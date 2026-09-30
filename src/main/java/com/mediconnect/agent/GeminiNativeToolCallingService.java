package com.mediconnect.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class GeminiNativeToolCallingService {

    private static final Logger log = LoggerFactory.getLogger(GeminiNativeToolCallingService.class);

    private final ClinicAiTools clinicAiTools;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${spring.ai.openai.api-key:${GEMINI_API_KEY:}}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model:gemini-3.5-flash-lite}")
    private String model;

    public GeminiNativeToolCallingService(ClinicAiTools clinicAiTools, ObjectMapper objectMapper) {
        this.clinicAiTools = clinicAiTools;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.contains("demo-placeholder-key");
    }

    public String generateChatResponse(String systemPrompt, List<String> history, String userPrompt) throws Exception {
        String effectiveModel = (model != null && !model.isBlank()) ? model.replace("models/", "") : "gemini-3.5-flash-lite";
        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                effectiveModel, apiKey.trim());

        // Build root payload
        ObjectNode root = objectMapper.createObjectNode();

        // System instruction
        ObjectNode systemInstruction = objectMapper.createObjectNode();
        ArrayNode systemParts = systemInstruction.putArray("parts");
        systemParts.addObject().put("text", systemPrompt);
        root.set("system_instruction", systemInstruction);

        // Tools declaration
        ArrayNode toolsArray = root.putArray("tools");
        ObjectNode toolsObj = toolsArray.addObject();
        ArrayNode functionDecls = toolsObj.putArray("function_declarations");

        // Tool 1: getPatientProfile
        ObjectNode tool1 = functionDecls.addObject();
        tool1.put("name", "getPatientProfile");
        tool1.put("description", "Look up patient details, registered insurance policy name, email, and medical history from the clinic database by patient name (e.g. 'Jayawanth', 'Sophia', 'Adam'), email, or ID");
        ObjectNode p1 = tool1.putObject("parameters");
        p1.put("type", "OBJECT");
        ObjectNode props1 = p1.putObject("properties");
        props1.putObject("patientIdentifier").put("type", "STRING").put("description", "Patient name (e.g. 'Adam', 'Jayawanth'), email, or numeric ID");
        p1.putArray("required").add("patientIdentifier");

        // Tool 2: calculatePatientPayment
        ObjectNode tool2 = functionDecls.addObject();
        tool2.put("name", "calculatePatientPayment");
        tool2.put("description", "Calculate the patient's exact out-of-pocket payment, insurance copay, and coverage breakdown for a medical specialty visit based on their insurance plan in DB");
        ObjectNode p2 = tool2.putObject("parameters");
        p2.put("type", "OBJECT");
        ObjectNode props2 = p2.putObject("properties");
        props2.putObject("patientOrPlan").put("type", "STRING").put("description", "Patient name (e.g. 'Adam', 'Jayawanth') or Insurance Plan name (e.g. 'Aetna Silver', 'BlueCross Gold')");
        props2.putObject("specialty").put("type", "STRING").put("description", "Medical specialty (e.g. 'Neurology', 'Cardiology')");
        props2.putObject("consultationFee").put("type", "NUMBER").put("description", "Doctor consultation fee (e.g. 180.00)");
        ArrayNode req2 = p2.putArray("required");
        req2.add("patientOrPlan");
        req2.add("specialty");

        // Tool 3: findDoctorsBySpecialty
        ObjectNode tool3 = functionDecls.addObject();
        tool3.put("name", "findDoctorsBySpecialty");
        tool3.put("description", "Search available doctor appointment slots by medical specialty (e.g., Neurology, Cardiology, Dermatology, Pediatrics)");
        ObjectNode p3 = tool3.putObject("parameters");
        p3.put("type", "OBJECT");
        ObjectNode props3 = p3.putObject("properties");
        props3.putObject("specialty").put("type", "STRING").put("description", "Medical specialty needed (e.g. Neurology, Cardiology, Pediatrics, Dermatology)");
        p3.putArray("required").add("specialty");

        // Tool 4: bookAppointmentSlot
        ObjectNode tool4 = functionDecls.addObject();
        tool4.put("name", "bookAppointmentSlot");
        tool4.put("description", "Book a doctor appointment slot for a patient using patient name, email, or ID and the selected slot ID");
        ObjectNode p4 = tool4.putObject("parameters");
        p4.put("type", "OBJECT");
        ObjectNode props4 = p4.putObject("properties");
        props4.putObject("patientIdentifier").put("type", "STRING").put("description", "Patient name (e.g. 'Adam', 'Jayawanth'), email, or ID");
        props4.putObject("slotId").put("type", "INTEGER").put("description", "ID of the appointment slot to book (e.g. 1, 2, 6)");
        ArrayNode req4 = p4.putArray("required");
        req4.add("patientIdentifier");
        req4.add("slotId");

        // Tool 5: checkInsuranceCoverage
        ObjectNode tool5 = functionDecls.addObject();
        tool5.put("name", "checkInsuranceCoverage");
        tool5.put("description", "Search insurance policy guidelines, copay amounts, and coverage percentages for a given plan name (or patient name) and specialty");
        ObjectNode p5 = tool5.putObject("parameters");
        p5.put("type", "OBJECT");
        ObjectNode props5 = p5.putObject("properties");
        props5.putObject("planName").put("type", "STRING").put("description", "Name of the insurance plan (e.g. 'Aetna Silver', 'BlueCross Gold') or Patient Name");
        props5.putObject("specialty").put("type", "STRING").put("description", "Medical specialty requested (e.g. 'Neurology', 'Cardiology')");
        ArrayNode req5 = p5.putArray("required");
        req5.add("planName");
        req5.add("specialty");

        // Tool 6: getPatientAppointments
        ObjectNode tool6 = functionDecls.addObject();
        tool6.put("name", "getPatientAppointments");
        tool6.put("description", "Retrieve current and past appointments for a specific patient by name, email, or patient ID");
        ObjectNode p6 = tool6.putObject("parameters");
        p6.put("type", "OBJECT");
        ObjectNode props6 = p6.putObject("properties");
        props6.putObject("patientIdentifier").put("type", "STRING").put("description", "Patient name (e.g. 'Adam', 'Jayawanth', 'Sophia'), email, or patient ID");
        p6.putArray("required").add("patientIdentifier");

        // Tool 7: registerPatientProfile
        ObjectNode tool7 = functionDecls.addObject();
        tool7.put("name", "registerPatientProfile");
        tool7.put("description", "Register a new patient profile with their name, insurance policy plan, email, and medical history in the clinic database");
        ObjectNode p7 = tool7.putObject("parameters");
        p7.put("type", "OBJECT");
        ObjectNode props7 = p7.putObject("properties");
        props7.putObject("name").put("type", "STRING").put("description", "Full name of the patient (e.g. 'Adam', 'Sophia')");
        props7.putObject("insurancePlan").put("type", "STRING").put("description", "Insurance plan name (e.g. 'Aetna Silver', 'BlueCross Gold', 'UnitedHealth Choice', 'Self-Pay')");
        props7.putObject("email").put("type", "STRING").put("description", "Email address of the patient");
        props7.putObject("medicalHistory").put("type", "STRING").put("description", "Medical history or symptoms notes");
        p7.putArray("required").add("name");

        // Tool 8: listAllInsurancePlans
        ObjectNode tool8 = functionDecls.addObject();
        tool8.put("name", "listAllInsurancePlans");
        tool8.put("description", "List all supported in-network insurance policies, copays, and coverage rates across the clinic network");
        ObjectNode p8 = tool8.putObject("parameters");
        p8.put("type", "OBJECT");
        ObjectNode props8 = p8.putObject("properties");
        props8.putObject("specialty").put("type", "STRING").put("description", "Optional medical specialty filter (e.g. 'Neurology', 'Cardiology')");

        // Tool 9: searchPdfPolicyDocuments
        ObjectNode tool9 = functionDecls.addObject();
        tool9.put("name", "searchPdfPolicyDocuments");
        tool9.put("description", "Search uploaded PDF policy document chunks for specific insurance rules, FAQs, copays, or pre-authorization details");
        ObjectNode p9 = tool9.putObject("parameters");
        p9.put("type", "OBJECT");
        ObjectNode props9 = p9.putObject("properties");
        props9.putObject("query").put("type", "STRING").put("description", "Query or keyword to search uploaded PDF policy documents for (e.g. 'Neurology copay', 'Pre-authorization')");
        p9.putArray("required").add("query");


        // Contents array
        ArrayNode contents = root.putArray("contents");

        // Add user prompt (with history if any)
        String fullUserText = (history != null && !history.isEmpty())
                ? "Conversation History:\n" + String.join("\n", history) + "\n\nCurrent User Query: " + userPrompt
                : userPrompt;

        ObjectNode userTurn = contents.addObject();
        userTurn.put("role", "user");
        userTurn.putArray("parts").addObject().put("text", fullUserText);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Multi-turn tool execution loop (max 5 iterations)
        for (int turn = 0; turn < 5; turn++) {
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(root), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Gemini API returned HTTP " + response.getStatusCode());
            }

            JsonNode responseJson = objectMapper.readTree(response.getBody());
            JsonNode candidate = responseJson.path("candidates").path(0);
            JsonNode content = candidate.path("content");
            JsonNode parts = content.path("parts");

            // Append model's response (preserves thought_signature and functionCall exactly)
            contents.add(content);

            boolean hasFunctionCall = false;
            ArrayNode toolResponseParts = objectMapper.createArrayNode();

            for (JsonNode part : parts) {
                if (part.has("functionCall")) {
                    hasFunctionCall = true;
                    JsonNode funcCall = part.path("functionCall");
                    String funcName = funcCall.path("name").asText();
                    JsonNode args = funcCall.path("args");

                    log.info("🤖 Gemini Autonomous Tool Call: {} with args {}", funcName, args);
                    String result = executeTool(funcName, args);

                    ObjectNode respPart = toolResponseParts.addObject();
                    ObjectNode funcResp = respPart.putObject("functionResponse");
                    funcResp.put("name", funcName);
                    funcResp.putObject("response").put("result", result);
                }
            }

            if (!hasFunctionCall) {
                // Final answer reached
                StringBuilder finalText = new StringBuilder();
                for (JsonNode part : parts) {
                    if (part.has("text")) {
                        finalText.append(part.path("text").asText());
                    }
                }
                String finalResult = finalText.toString().trim();
                return finalResult.isEmpty() ? null : finalResult;
            }

            // Append function response turn with role "user"
            ObjectNode toolResponseTurn = contents.addObject();
            toolResponseTurn.put("role", "user");
            toolResponseTurn.set("parts", toolResponseParts);
        }

        return null;
    }

    private String executeTool(String funcName, JsonNode args) {
        try {
            switch (funcName) {
                case "getPatientProfile": {
                    String patientIdentifier = args.path("patientIdentifier").asText("Patient");
                    var resp = clinicAiTools.getPatientProfile().apply(new ClinicAiTools.PatientProfileRequest(patientIdentifier));
                    return resp.patientDetails();
                }
                case "calculatePatientPayment": {
                    String patientOrPlan = args.path("patientOrPlan").asText("Aetna Silver");
                    String specialty = args.path("specialty").asText("Neurology");
                    Double fee = args.has("consultationFee") ? args.path("consultationFee").asDouble(180.00) : 180.00;
                    var resp = clinicAiTools.calculatePatientPayment().apply(new ClinicAiTools.PaymentCalculationRequest(patientOrPlan, specialty, fee));
                    return resp.paymentSummary();
                }
                case "findDoctorsBySpecialty": {
                    String specialty = args.path("specialty").asText("Neurology");
                    var resp = clinicAiTools.findDoctorsBySpecialty().apply(new ClinicAiTools.DoctorSearchRequest(specialty));
                    return resp.result();
                }
                case "bookAppointmentSlot": {
                    String patientIdentifier = args.has("patientIdentifier") ? args.path("patientIdentifier").asText() :
                            (args.has("patientId") ? String.valueOf(args.path("patientId").asLong()) : "Patient");
                    Long slotId = args.has("slotId") ? args.path("slotId").asLong(1L) : 1L;
                    var resp = clinicAiTools.bookAppointmentSlot().apply(new ClinicAiTools.BookAppointmentRequest(patientIdentifier, slotId));
                    return resp.status() + ": " + resp.message();
                }
                case "checkInsuranceCoverage": {
                    String plan = args.path("planName").asText("Aetna Silver");
                    String specialty = args.path("specialty").asText("Neurology");
                    var resp = clinicAiTools.checkInsuranceCoverage().apply(new ClinicAiTools.InsurancePolicyRequest(plan, specialty));
                    return resp.policyInformation();
                }
                case "getPatientAppointments": {
                    String patientIdentifier = args.has("patientIdentifier") ? args.path("patientIdentifier").asText() :
                            (args.has("patientId") ? String.valueOf(args.path("patientId").asLong()) : "Patient");
                    var resp = clinicAiTools.getPatientAppointments().apply(new ClinicAiTools.PatientAppointmentsRequest(patientIdentifier));
                    return resp.appointmentsSummary();
                }
                case "registerPatientProfile": {
                    String name = args.path("name").asText("Patient");
                    String plan = args.has("insurancePlan") ? args.path("insurancePlan").asText("Aetna Silver") : "Aetna Silver";
                    String email = args.has("email") ? args.path("email").asText() : null;
                    String history = args.has("medicalHistory") ? args.path("medicalHistory").asText() : "Self-registered";
                    var resp = clinicAiTools.registerPatientProfile().apply(new ClinicAiTools.RegisterPatientRequest(name, plan, email, history));
                    return resp.status() + ": " + resp.message();
                }
                case "listAllInsurancePlans": {
                    String specialty = args.has("specialty") ? args.path("specialty").asText() : null;
                    var resp = clinicAiTools.listAllInsurancePlans().apply(new ClinicAiTools.ListInsurancePlansRequest(specialty));
                    return resp.plansSummary();
                }
                case "searchPdfPolicyDocuments": {
                    String query = args.path("query").asText("policy rules");
                    var resp = clinicAiTools.searchPdfPolicyDocuments().apply(new ClinicAiTools.SearchPdfDocumentsRequest(query));
                    return resp.searchResults();
                }

                default:
                    return "Unknown tool: " + funcName;
            }
        } catch (Exception e) {
            log.error("Tool execution failed for {}: {}", funcName, e.getMessage(), e);
            return "Error executing " + funcName + ": " + e.getMessage();
        }
    }
}
