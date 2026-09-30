package com.mediconnect.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.mediconnect.entity.Appointment;
import com.mediconnect.entity.AppointmentSlot;
import com.mediconnect.entity.Patient;
import com.mediconnect.service.ClinicService;
import com.mediconnect.service.InsurancePolicyRagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class ClinicAiTools {

    private static final Logger log = LoggerFactory.getLogger(ClinicAiTools.class);

    private final ClinicService clinicService;
    private final InsurancePolicyRagService ragService;
    private final com.mediconnect.service.PdfDocumentRagService pdfDocumentRagService;

    public ClinicAiTools(ClinicService clinicService,
                         InsurancePolicyRagService ragService,
                         com.mediconnect.service.PdfDocumentRagService pdfDocumentRagService) {
        this.clinicService = clinicService;
        this.ragService = ragService;
        this.pdfDocumentRagService = pdfDocumentRagService;
    }

    // ============================================================
    // 1. GET PATIENT PROFILE & INSURANCE PLAN FROM DB
    // ============================================================

    public record PatientProfileRequest(
        @JsonPropertyDescription("Patient name (e.g. 'Jayawanth', 'Sophia', 'Adam'), email address, or numeric ID")
        @JsonProperty(required = true) String patientIdentifier
    ) {}

    public record PatientProfileResponse(String patientDetails) {}

    @Bean
    @Description("Look up patient details, registered insurance policy, and medical history from the database by name, email, or patient ID")
    public Function<PatientProfileRequest, PatientProfileResponse> getPatientProfile() {
        return request -> {
            log.info("🤖 AI TOOL EXECUTED: getPatientProfile -> patientIdentifier: '{}'", request.patientIdentifier());
            Optional<Patient> patientOpt = clinicService.findPatient(request.patientIdentifier());

            if (patientOpt.isPresent()) {
                Patient p = patientOpt.get();
                String details = String.format(
                    "Patient Record Found:\n" +
                    "- Patient ID: #%d\n" +
                    "- Full Name: %s\n" +
                    "- Email: %s\n" +
                    "- Registered Insurance Plan: %s\n" +
                    "- Medical History: %s",
                    p.getId(), p.getName(), p.getEmail(), p.getInsurancePlan(), p.getMedicalHistory()
                );
                return new PatientProfileResponse(details);
            }

            return new PatientProfileResponse(String.format(
                "No patient record found in database for identifier '%s'. " +
                "The patient can be registered, or can proceed as self-pay or provide their insurance plan name (e.g. Aetna Silver, BlueCross Gold).",
                request.patientIdentifier()
            ));
        };
    }

    // ============================================================
    // 2. CALCULATE PATIENT PAYMENT & COPAYS
    // ============================================================

    public record PaymentCalculationRequest(
        @JsonPropertyDescription("Patient name (e.g. 'Jayawanth', 'Adam') or Insurance Plan name (e.g. 'Aetna Silver')")
        @JsonProperty(required = true) String patientOrPlan,

        @JsonPropertyDescription("Medical specialty (e.g. 'Neurology', 'Cardiology')")
        @JsonProperty(required = true) String specialty,

        @JsonPropertyDescription("Doctor consultation fee (e.g. 180.00)")
        Double consultationFee
    ) {}

    public record PaymentCalculationResponse(String paymentSummary) {}

    @Bean
    @Description("Calculate the patient's exact out-of-pocket payment, insurance copay, and insurance covered amount based on their policy in DB")
    public Function<PaymentCalculationRequest, PaymentCalculationResponse> calculatePatientPayment() {
        return request -> {
            log.info("🤖 AI TOOL EXECUTED: calculatePatientPayment -> patientOrPlan: '{}', specialty: '{}', fee: {}",
                    request.patientOrPlan(), request.specialty(), request.consultationFee());
            String summary = clinicService.calculatePatientPayment(request.patientOrPlan(), request.specialty(), request.consultationFee());
            return new PaymentCalculationResponse(summary);
        };
    }

    // ============================================================
    // 3. DOCTOR SEARCH BY SPECIALTY
    // ============================================================

    public record DoctorSearchRequest(
        @JsonPropertyDescription("Medical specialty needed (e.g., Neurology, Cardiology, Pediatrics, Dermatology)")
        @JsonProperty(required = true) String specialty
    ) {}

    public record DoctorSearchResponse(String result) {}

    @Bean
    @Description("Search available doctor appointment slots by medical specialty (e.g., Neurology, Cardiology)")
    public Function<DoctorSearchRequest, DoctorSearchResponse> findDoctorsBySpecialty() {
        return request -> {
            log.info("🤖 AI TOOL EXECUTED: findDoctorsBySpecialty -> specialty: '{}'", request.specialty());
            List<AppointmentSlot> slots = clinicService.findAvailableSlots(request.specialty());

            if (slots.isEmpty()) {
                return new DoctorSearchResponse("No available open slots found for specialty: " + request.specialty());
            }

            String summary = slots.stream()
                .map(s -> String.format("- Slot ID #%d | Doctor: %s (%s) | Hospital: %s | Fee: $%s | Time: %s",
                    s.getId(), s.getDoctor().getName(), s.getDoctor().getSpecialty(),
                    s.getDoctor().getHospitalBranch(), s.getDoctor().getConsultationFee(), s.getSlotTime()))
                .collect(Collectors.joining("\n"));

            return new DoctorSearchResponse("Available Appointment Slots:\n" + summary);
        };
    }

    // ============================================================
    // 4. BOOK APPOINTMENT
    // ============================================================

    public record BookAppointmentRequest(
        @JsonPropertyDescription("Patient name (e.g. 'Adam', 'Jayawanth'), email, or numeric patient ID")
        @JsonProperty(required = true) String patientIdentifier,

        @JsonPropertyDescription("ID of the appointment slot to book")
        @JsonProperty(required = true) Long slotId
    ) {}

    public record BookAppointmentResponse(String status, String message, Long appointmentId) {}

    @Bean
    @Description("Book a doctor appointment slot for a patient using patient name, ID, or email and slot ID")
    public Function<BookAppointmentRequest, BookAppointmentResponse> bookAppointmentSlot() {
        return request -> {
            log.info("🤖 AI TOOL EXECUTED: bookAppointmentSlot -> patientIdentifier: '{}', slotId: {}", request.patientIdentifier(), request.slotId());
            try {
                Appointment appointment = clinicService.bookAppointmentForPatient(request.patientIdentifier(), request.slotId());
                return new BookAppointmentResponse(
                    "SUCCESS",
                    String.format("Appointment #%d successfully confirmed for %s with %s on %s!",
                        appointment.getId(), appointment.getPatient().getName(), appointment.getDoctor().getName(), appointment.getSlot().getSlotTime()),
                    appointment.getId()
                );
            } catch (Exception e) {
                log.error("Failed to book appointment: {}", e.getMessage());
                return new BookAppointmentResponse("FAILED", "Booking failed: " + e.getMessage(), null);
            }
        };
    }

    // ============================================================
    // 5. CHECK INSURANCE COVERAGE RAG
    // ============================================================

    public record InsurancePolicyRequest(
        @JsonPropertyDescription("Name of the insurance plan (e.g. Aetna Silver, BlueCross Gold) or Patient Name")
        @JsonProperty(required = true) String planName,

        @JsonPropertyDescription("Medical specialty requested (e.g. Neurology, Cardiology)")
        @JsonProperty(required = true) String specialty
    ) {}

    public record InsurancePolicyResponse(String policyInformation) {}

    @Bean
    @Description("Search insurance policy guidelines, copay amounts, and coverage percentages for a given plan name (or patient name) and specialty")
    public Function<InsurancePolicyRequest, InsurancePolicyResponse> checkInsuranceCoverage() {
        return request -> {
            log.info("🤖 AI TOOL EXECUTED: checkInsuranceCoverage -> plan/patient: '{}', specialty: '{}'", request.planName(), request.specialty());
            String resolvedPlan = request.planName();
            Optional<Patient> pOpt = clinicService.findPatient(request.planName());
            if (pOpt.isPresent()) {
                resolvedPlan = pOpt.get().getInsurancePlan();
            }
            String info = ragService.searchInsuranceCoverage(resolvedPlan, request.specialty());
            return new InsurancePolicyResponse(info);
        };
    }

    // ============================================================
    // 6. PATIENT APPOINTMENTS HISTORY
    // ============================================================

    public record PatientAppointmentsRequest(
        @JsonPropertyDescription("Patient name (e.g. 'Jayawanth', 'Sophia', 'Adam'), email, or patient ID")
        @JsonProperty(required = true) String patientIdentifier
    ) {}

    public record PatientAppointmentsResponse(String appointmentsSummary) {}

    @Bean
    @Description("Retrieve current and past appointments for a specific patient by name, email, or patient ID")
    public Function<PatientAppointmentsRequest, PatientAppointmentsResponse> getPatientAppointments() {
        return request -> {
            log.info("🤖 AI TOOL EXECUTED: getPatientAppointments -> patientIdentifier: '{}'", request.patientIdentifier());
            List<Appointment> list = clinicService.getPatientAppointmentsByIdentifier(request.patientIdentifier());

            if (list.isEmpty()) {
                return new PatientAppointmentsResponse("No existing appointments found for patient: " + request.patientIdentifier());
            }

            String summary = list.stream()
                .map(a -> String.format("- Appointment #%d | Patient: %s | Doctor: %s (%s) | Status: %s | Time: %s",
                    a.getId(), a.getPatient().getName(), a.getDoctor().getName(), a.getDoctor().getSpecialty(), a.getStatus(), a.getSlot().getSlotTime()))
                .collect(Collectors.joining("\n"));

            return new PatientAppointmentsResponse("Patient Appointments History:\n" + summary);
        };
    }

    // ============================================================
    // 7. REGISTER PATIENT PROFILE
    // ============================================================

    public record RegisterPatientRequest(
        @JsonPropertyDescription("Full name of the patient (e.g. 'Adam Smith', 'Sophia')")
        @JsonProperty(required = true) String name,

        @JsonPropertyDescription("Insurance plan name (e.g. 'Aetna Silver', 'BlueCross Gold', 'UnitedHealth Choice', 'Self-Pay')")
        String insurancePlan,

        @JsonPropertyDescription("Email address of the patient")
        String email,

        @JsonPropertyDescription("Medical history or relevant symptoms notes")
        String medicalHistory
    ) {}

    public record RegisterPatientResponse(String status, String message, Long patientId) {}

    @Bean
    @Description("Register a new patient profile with their name, insurance policy plan, email, and medical history in the clinic database")
    public Function<RegisterPatientRequest, RegisterPatientResponse> registerPatientProfile() {
        return request -> {
            log.info("🤖 AI TOOL EXECUTED: registerPatientProfile -> name: '{}', plan: '{}'", request.name(), request.insurancePlan());
            try {
                Patient patient = clinicService.registerPatient(request.name(), request.email(), request.insurancePlan(), request.medicalHistory());
                return new RegisterPatientResponse(
                    "SUCCESS",
                    String.format("Patient '%s' successfully registered in database with ID #%d and Insurance Plan '%s'.",
                        patient.getName(), patient.getId(), patient.getInsurancePlan()),
                    patient.getId()
                );
            } catch (Exception e) {
                log.error("Failed to register patient: {}", e.getMessage());
                return new RegisterPatientResponse("FAILED", "Registration failed: " + e.getMessage(), null);
            }
        };
    }

    // ============================================================
    // 8. LIST ALL ACCEPTED INSURANCE PLANS
    // ============================================================

    public record ListInsurancePlansRequest(
        @JsonPropertyDescription("Optional specialty filter (e.g. 'Neurology') or empty for all")
        String specialty
    ) {}

    public record ListInsurancePlansResponse(String plansSummary) {}

    @Bean
    @Description("List all supported in-network insurance policies, copays, and coverage rates across the clinic network")
    public Function<ListInsurancePlansRequest, ListInsurancePlansResponse> listAllInsurancePlans() {
        return request -> {
            log.info("🤖 AI TOOL EXECUTED: listAllInsurancePlans");
            var policies = clinicService.getAllInsurancePolicies();
            if (policies.isEmpty()) {
                return new ListInsurancePlansResponse("No insurance policies found in database.");
            }
            String summary = policies.stream()
                .filter(p -> request.specialty() == null || request.specialty().isBlank() || p.getSpecialty().equalsIgnoreCase(request.specialty().trim()))
                .map(p -> String.format("- **%s** (%s): %d%% coverage, $%.2f copay | %s",
                    p.getPlanName(), p.getSpecialty(), p.getCoveragePercentage(), p.getCopayAmount(), p.getPolicyDetails()))
                .collect(Collectors.joining("\n"));

            return new ListInsurancePlansResponse("🏥 **Supported Clinic Insurance Policies:**\n" + summary);
        };
    }

    // ============================================================
    // 9. SEARCH PDF POLICY DOCUMENTS (VECTOR RAG)
    // ============================================================

    public record SearchPdfDocumentsRequest(
        @JsonPropertyDescription("Query or keyword to search uploaded PDF policy documents for (e.g. 'Neurology copay', 'Pre-authorization')")
        @JsonProperty(required = true) String query
    ) {}

    public record SearchPdfDocumentsResponse(String searchResults) {}

    @Bean
    @Description("Search uploaded PDF policy document chunks for specific insurance rules, FAQs, copays, or pre-authorization details")
    public Function<SearchPdfDocumentsRequest, SearchPdfDocumentsResponse> searchPdfPolicyDocuments() {
        return request -> {
            log.info("🤖 AI TOOL EXECUTED: searchPdfPolicyDocuments -> query: '{}'", request.query());
            String results = pdfDocumentRagService.searchPdfDocuments(request.query());
            return new SearchPdfDocumentsResponse(results);
        };
    }
}
