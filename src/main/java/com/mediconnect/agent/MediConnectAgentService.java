package com.mediconnect.agent;

import com.mediconnect.entity.Appointment;
import com.mediconnect.entity.AppointmentSlot;
import com.mediconnect.service.ChatMemoryService;
import com.mediconnect.service.ClinicService;
import com.mediconnect.service.InsurancePolicyRagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MediConnectAgentService {

    private static final Logger log = LoggerFactory.getLogger(MediConnectAgentService.class);

    @Value("${mediconnect.ai.prefer-ollama:true}")
    private boolean preferOllama;

    private final ChatClient.Builder chatClientBuilder;
    private final ChatMemoryService chatMemoryService;
    private final ClinicService clinicService;
    private final InsurancePolicyRagService ragService;

    private final ClinicAiTools clinicAiTools;
    private final GeminiNativeToolCallingService geminiNativeToolCallingService;
    private final AgentRouterService agentRouterService;
    private final BookingAgent bookingAgent;
    private final InsurancePolicyRagAgent insurancePolicyRagAgent;

    private static final String SYSTEM_PROMPT = """
            You are MediConnect AI, an intelligent clinical assistant for a healthcare network.

            Your primary role is to assist patients and doctors with:
            1. Looking up patient details and registered insurance policies from the clinic database.
            2. Checking medical insurance policy coverage, copays, and out-of-pocket payment calculations.
            3. Searching available doctor appointment slots by specialty.
            4. Booking appointment slots on behalf of patients.
            5. Retrieving appointment history.
            6. Registering new patient profiles in the clinic database.
            7. Searching uploaded PDF policy manuals for specific coverage rules and FAQs.

            GUIDELINES:
            - Be empathetic, professional, and clear.
            - Never assume a fixed patient ID or assume all patients have the same plan.
            - When a patient provides their name, look up their registered insurance policy and medical record.
            - Provide exact breakdowns of consultation fees, insurance coverage %, copays, and out-of-pocket amounts.
            """;

    public MediConnectAgentService(
            @Autowired(required = false) ChatClient.Builder chatClientBuilder,
            ChatMemoryService chatMemoryService,
            ClinicService clinicService,
            InsurancePolicyRagService ragService,
            ClinicAiTools clinicAiTools,
            GeminiNativeToolCallingService geminiNativeToolCallingService,
            AgentRouterService agentRouterService,
            BookingAgent bookingAgent,
            InsurancePolicyRagAgent insurancePolicyRagAgent) {

        this.chatClientBuilder = chatClientBuilder;
        this.chatMemoryService = chatMemoryService;
        this.clinicService = clinicService;
        this.ragService = ragService;
        this.clinicAiTools = clinicAiTools;
        this.geminiNativeToolCallingService = geminiNativeToolCallingService;
        this.agentRouterService = agentRouterService;
        this.bookingAgent = bookingAgent;
        this.insurancePolicyRagAgent = insurancePolicyRagAgent;
    }

    // ============================================================
    // MAIN CHAT METHOD
    // ============================================================

    public String processUserChat(
            String sessionId,
            String userPrompt) {

        log.info(
                "Processing Chat Session '{}' | Prompt: '{}'",
                sessionId,
                userPrompt);

        // --------------------------------------------------------
        // Save user message
        // --------------------------------------------------------

        chatMemoryService.appendMessage(
                sessionId,
                "User",
                userPrompt);

        // --------------------------------------------------------
        // Retrieve conversation history
        // --------------------------------------------------------

        List<String> history = chatMemoryService.getHistory(sessionId);

        String formattedHistory = String.join("\n", history);

        String responseContent = null;

        // --------------------------------------------------------
        // 1. Primary Path: Cloud Gemini Native REST API (Native Tool Calling & DB Execution)
        // --------------------------------------------------------

        if (!preferOllama && geminiNativeToolCallingService != null && geminiNativeToolCallingService.isAvailable()) {
            try {
                log.info("Executing Autonomous Gemini Agent with Native Tool Calling & Thought Signature Retention...");

                responseContent = geminiNativeToolCallingService.generateChatResponse(
                        SYSTEM_PROMPT,
                        history,
                        userPrompt);

            } catch (Exception e) {
                log.warn("Gemini execution notice: {}. Falling back to Ollama / Java reasoning engine.", e.getMessage());
            }
        }

        // --------------------------------------------------------
        // 2. Secondary Path: Local Ollama LLM (via Spring AI ChatClient)
        // --------------------------------------------------------

        if ((responseContent == null || responseContent.isBlank()) && chatClientBuilder != null) {
            try {
                log.info("🦙 Executing Local Ollama / Spring AI ChatModel...");

                ChatClient chatClient = chatClientBuilder.build();

                responseContent = chatClient.prompt()
                        .system(SYSTEM_PROMPT)
                        .user(String.format(
                                """
                                Conversation History:
                                %s

                                Current User Query:
                                %s
                                """,
                                formattedHistory,
                                userPrompt))
                        .functions("getPatientProfile", "calculatePatientPayment", "findDoctorsBySpecialty",
                                "bookAppointmentSlot", "checkInsuranceCoverage", "getPatientAppointments",
                                "registerPatientProfile", "listAllInsurancePlans")
                        .call()
                        .content();

            } catch (Exception e) {
                log.warn("🦙 Ollama Local LLM notice: {}. Falling back to Java reasoning engine.", e.getMessage());
            }
        }

        // --------------------------------------------------------
        // 3. Tertiary Path: MediConnect Multi-Agent Fallback Engine
        // --------------------------------------------------------

        if (responseContent == null || responseContent.isBlank()) {
            log.info("Using MediConnect Multi-Agent Fallback Engine.");
            responseContent = executeAgentFallbackReasoning(userPrompt, formattedHistory);
        }



        // --------------------------------------------------------
        // Save AI response
        // --------------------------------------------------------

        chatMemoryService.appendMessage(
                sessionId,
                "MediConnect AI",
                responseContent);

        return responseContent;
    }

    // ============================================================
    // MULTI-AGENT REASONING & ROUTING ENGINE
    // ============================================================

    private String executeAgentFallbackReasoning(
            String prompt,
            String history) {

        String patientName = extractPatientName(prompt, history);
        AgentRouterService.AgentType targetAgent = agentRouterService.routeRequest(prompt);

        log.info("🤖 Multi-Agent Orchestrator: Routing prompt '{}' to Specialist Agent [{}]", prompt, targetAgent);

        switch (targetAgent) {
            case BOOKING_AGENT:
                return bookingAgent.handleBookingTask(prompt, patientName);
            case POLICY_RAG_AGENT:
                return insurancePolicyRagAgent.handlePolicyAndFaqTask(prompt, patientName);
            case GENERAL_GREETING_AGENT:
            default:
                String greetingName = (patientName != null) ? " " + patientName : "";
                return String.format("""
                        👋 **Hello%s! Welcome to MediConnect AI.**

                        I am your personal clinical healthcare assistant powered by specialized AI sub-agents:

                        📅 **Booking Agent:**
                        Search specialist doctors, available slot times, and book appointment slots.
                        *Example: "I have a headache, what doctors are available?"* or *"Book slot #1 for Adam"*

                        📄 **Insurance Policy & PDF FAQ Agent (RAG):**
                        Calculate patient out-of-pocket costs, check copays, and search uploaded PDF policy manuals.
                        *Example: "How much should I pay under Aetna Silver?"* or upload PDF policy files on the dashboard!

                        How can I assist you today?
                        """, greetingName);
        }
    }

    private String extractPatientName(String prompt, String history) {
        String combined = (prompt + " " + (history != null ? history : "")).trim();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?i)(?:my name is|i am|name is|i'm|im|patient:?)\\s+([a-zA-Z]+)");
        java.util.regex.Matcher m = p.matcher(combined);
        if (m.find()) {
            String candidate = m.group(1).trim();
            if (!candidate.equalsIgnoreCase("a") && !candidate.equalsIgnoreCase("the")
                    && !candidate.equalsIgnoreCase("looking") && !candidate.equalsIgnoreCase("booking")
                    && !candidate.equalsIgnoreCase("having") && !candidate.equalsIgnoreCase("feeling")) {
                return candidate.substring(0, 1).toUpperCase() + candidate.substring(1).toLowerCase();
            }
        }
        if (combined.toLowerCase().contains("jayawanth")) return "Jayawanth";
        if (combined.toLowerCase().contains("sophia")) return "Sophia";
        if (combined.toLowerCase().contains("adam")) return "Adam";
        return null;
    }

    // ============================================================
    // MEDICAL QUERY DETECTION
    // ============================================================

    private boolean isMedicalQuery(String prompt) {

        String[] keywords = {
                "headache",
                "pain",
                "fever",
                "sick",
                "head",
                "dizzy",
                "chest",
                "heart",
                "rash",
                "skin",
                "cough",
                "doctor",
                "specialist",
                "appointment",
                "slot",
                "consult",
                "treatment",
                "clinic",
                "hospital",
                "coverage",
                "insurance"
        };

        for (String keyword : keywords) {

            if (prompt.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    // ============================================================
    // SLOT ID EXTRACTION
    // ============================================================

    private Long extractSlotId(String prompt) {

        for (String word : prompt.split("\\s+")) {

            String cleaned = word.replaceAll("[^0-9]", "");

            if (!cleaned.isEmpty()) {

                try {

                    return Long.parseLong(cleaned);

                } catch (Exception ignored) {
                    // Continue searching
                }
            }
        }

        return 1L;
    }

    // ============================================================
    // SPECIALTY EXTRACTION
    // ============================================================

    private String extractSpecialty(String prompt) {

        // --------------------------------------------------------
        // Neurology
        // --------------------------------------------------------

        if (prompt.contains("headache") ||
                prompt.contains("head ache") ||
                prompt.contains("migraine") ||
                prompt.contains("brain") ||
                prompt.contains("neuro") ||
                prompt.contains("dizzy") ||
                prompt.contains("head")) {

            return "Neurology";
        }

        // --------------------------------------------------------
        // Cardiology
        // --------------------------------------------------------

        if (prompt.contains("heart") ||
                prompt.contains("chest") ||
                prompt.contains("cardio") ||
                prompt.contains("blood pressure") ||
                prompt.contains("palpitation") ||
                prompt.contains("hypertension")) {

            return "Cardiology";
        }

        // --------------------------------------------------------
        // Dermatology
        // --------------------------------------------------------

        if (prompt.contains("skin") ||
                prompt.contains("rash") ||
                prompt.contains("derma") ||
                prompt.contains("acne") ||
                prompt.contains("itching") ||
                prompt.contains("mole")) {

            return "Dermatology";
        }

        // --------------------------------------------------------
        // Pediatrics
        // --------------------------------------------------------

        if (prompt.contains("child") ||
                prompt.contains("baby") ||
                prompt.contains("pedia") ||
                prompt.contains("kid") ||
                prompt.contains("toddler")) {

            return "Pediatrics";
        }

        return null;
    }

    // ============================================================
    // INSURANCE PLAN EXTRACTION
    // ============================================================

    private String extractInsurancePlan(String prompt) {

        if (prompt.contains("bluecross") ||
                prompt.contains("gold")) {

            return "BlueCross Gold";
        }

        if (prompt.contains("kaiser")) {

            return "Kaiser Premier";
        }

        if (prompt.contains("aetna") ||
                prompt.contains("silver")) {

            return "Aetna Silver";
        }

        return null;
    }
}