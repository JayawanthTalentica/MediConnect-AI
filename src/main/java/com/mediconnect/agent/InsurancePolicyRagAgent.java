package com.mediconnect.agent;

import com.mediconnect.service.ClinicService;
import com.mediconnect.service.InsurancePolicyRagService;
import com.mediconnect.service.PdfDocumentRagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InsurancePolicyRagAgent {

    private static final Logger log = LoggerFactory.getLogger(InsurancePolicyRagAgent.class);

    private final ClinicService clinicService;
    private final InsurancePolicyRagService databaseRagService;
    private final PdfDocumentRagService pdfDocumentRagService;

    public InsurancePolicyRagAgent(ClinicService clinicService,
                                   InsurancePolicyRagService databaseRagService,
                                   PdfDocumentRagService pdfDocumentRagService) {
        this.clinicService = clinicService;
        this.databaseRagService = databaseRagService;
        this.pdfDocumentRagService = pdfDocumentRagService;
    }

    public String handlePolicyAndFaqTask(String prompt, String patientName) {
        log.info("🤖 [SPECIALIST AGENT: InsurancePolicyRagAgent] Processing policy/FAQ query for prompt: '{}'", prompt);
        String lower = prompt.toLowerCase();

        StringBuilder response = new StringBuilder("📋 **Insurance Policy & FAQ Specialist Agent**\n\n");

        // 1. Search PDF Vector Store first if uploaded documents exist
        String pdfResults = pdfDocumentRagService.searchPdfDocuments(prompt);
        if (pdfResults != null && !pdfResults.contains("No uploaded PDF policy documents")) {
            response.append(pdfResults).append("\n---\n\n");
        }

        // 2. Search Database Policy Records
        String planName = extractInsurancePlan(lower);
        String specialty = extractSpecialty(lower);
        if (specialty == null) specialty = "Neurology";

        String targetLookup = (patientName != null) ? patientName : (planName != null ? planName : "Aetna Silver");

        // Payment & Copay calculation
        Double fee = 180.00;
        if (specialty.equalsIgnoreCase("Cardiology")) fee = 220.00;
        if (specialty.equalsIgnoreCase("Dermatology")) fee = 150.00;

        String paymentSummary = clinicService.calculatePatientPayment(targetLookup, specialty, fee);
        response.append(paymentSummary);

        return response.toString();
    }

    private String extractInsurancePlan(String prompt) {
        if (prompt.contains("bluecross") || prompt.contains("gold")) return "BlueCross Gold";
        if (prompt.contains("kaiser")) return "Kaiser Premier";
        if (prompt.contains("aetna") || prompt.contains("silver")) return "Aetna Silver";
        if (prompt.contains("united")) return "UnitedHealth Choice";
        return null;
    }

    private String extractSpecialty(String prompt) {
        if (prompt.contains("headache") || prompt.contains("neuro") || prompt.contains("brain")) return "Neurology";
        if (prompt.contains("heart") || prompt.contains("chest") || prompt.contains("cardio")) return "Cardiology";
        if (prompt.contains("skin") || prompt.contains("rash") || prompt.contains("derma")) return "Dermatology";
        if (prompt.contains("child") || prompt.contains("baby") || prompt.contains("pedia")) return "Pediatrics";
        return null;
    }
}
