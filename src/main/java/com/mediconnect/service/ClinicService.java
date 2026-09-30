package com.mediconnect.service;

import com.mediconnect.entity.*;
import com.mediconnect.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ClinicService {

    private static final Logger log = LoggerFactory.getLogger(ClinicService.class);

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentSlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final InsurancePolicyRepository insurancePolicyRepository;
    private final EventPublisherService eventPublisherService;

    public ClinicService(PatientRepository patientRepository, DoctorRepository doctorRepository,
                         AppointmentSlotRepository slotRepository, AppointmentRepository appointmentRepository,
                         InsurancePolicyRepository insurancePolicyRepository,
                         EventPublisherService eventPublisherService) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.insurancePolicyRepository = insurancePolicyRepository;
        this.eventPublisherService = eventPublisherService;
    }

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    public List<AppointmentSlot> findAvailableSlots(String specialty) {
        log.info("Searching available appointment slots for specialty: {}", specialty);
        return slotRepository.findAvailableSlotsBySpecialty(specialty, LocalDateTime.now());
    }

    public Optional<Patient> findPatient(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        String clean = identifier.trim();
        // Check if numeric ID
        if (clean.matches("^\\d+$")) {
            try {
                return patientRepository.findById(Long.parseLong(clean));
            } catch (Exception ignored) {}
        }
        if (clean.contains("@")) {
            return patientRepository.findByEmail(clean);
        }
        return patientRepository.findByNameContainingIgnoreCase(clean);
    }

    @Transactional
    public Patient registerPatient(String name, String email, String insurancePlan, String medicalHistory) {
        String cleanName = (name != null && !name.isBlank()) ? name.trim() : "Patient";
        String cleanEmail = (email != null && !email.isBlank()) ? email.trim() : cleanName.toLowerCase().replaceAll("[^a-z0-9]", "") + "@example.com";
        String cleanPlan = (insurancePlan != null && !insurancePlan.isBlank()) ? insurancePlan.trim() : "Aetna Silver";
        String cleanHistory = (medicalHistory != null) ? medicalHistory.trim() : "None noted";

        Optional<Patient> existing = findPatient(cleanEmail);
        if (existing.isPresent()) {
            return existing.get();
        }

        Patient patient = Patient.builder()
                .name(cleanName)
                .email(cleanEmail)
                .insurancePlan(cleanPlan)
                .medicalHistory(cleanHistory)
                .build();

        return patientRepository.save(patient);
    }

    public List<InsurancePolicy> getAllInsurancePolicies() {
        return insurancePolicyRepository.findAll();
    }

    public String calculatePatientPayment(String planOrPatient, String specialty, Double consultationFee) {
        String planName = null;
        String patientName = null;

        if (planOrPatient != null && !planOrPatient.isBlank()) {
            Optional<Patient> pOpt = findPatient(planOrPatient);
            if (pOpt.isPresent()) {
                Patient p = pOpt.get();
                planName = p.getInsurancePlan();
                patientName = p.getName();
            } else {
                String lower = planOrPatient.toLowerCase();
                if (lower.contains("aetna") || lower.contains("bluecross") || lower.contains("kaiser")
                        || lower.contains("united") || lower.contains("gold") || lower.contains("silver")
                        || lower.contains("bronze") || lower.contains("premier") || lower.contains("medicare")
                        || lower.contains("insurance") || lower.contains("plan")) {
                    planName = planOrPatient.trim();
                } else {
                    patientName = planOrPatient.trim();
                }
            }
        }

        String targetSpecialty = (specialty != null && !specialty.isBlank()) ? specialty.trim() : "Neurology";
        Double fee = (consultationFee != null && consultationFee > 0) ? consultationFee : 180.00;

        if (planName != null) {
            Optional<InsurancePolicy> policyOpt = insurancePolicyRepository.findByPlanNameIgnoreCaseAndSpecialtyIgnoreCase(planName, targetSpecialty);
            if (policyOpt.isEmpty()) {
                List<InsurancePolicy> matching = insurancePolicyRepository.findByPlanNameContainingIgnoreCase(planName);
                if (!matching.isEmpty()) {
                    policyOpt = matching.stream().filter(p -> p.getSpecialty().equalsIgnoreCase(targetSpecialty)).findFirst();
                    if (policyOpt.isEmpty()) {
                        policyOpt = Optional.of(matching.get(0));
                    }
                }
            }

            if (policyOpt.isPresent()) {
                InsurancePolicy policy = policyOpt.get();
                double copay = policy.getCopayAmount();
                int coveragePct = policy.getCoveragePercentage();
                double insurancePortion = (fee - copay) * (coveragePct / 100.0);
                double patientOutOfPocket = copay + Math.max(0.0, (fee - copay) * ((100 - coveragePct) / 100.0));

                return String.format(
                    "💰 **Payment & Copay Breakdown (%s - %s):**\n" +
                    (patientName != null ? "- **Patient:** " + patientName + "\n" : "") +
                    "- **Insurance Plan in Record:** %s\n" +
                    "- **Doctor Consultation Fee:** $%.2f\n" +
                    "- **Policy Coverage:** %d%%\n" +
                    "- **Insurance Copay Required:** $%.2f\n" +
                    "- **Insurance Company Covers:** $%.2f\n" +
                    "- **Total Patient Out-of-Pocket Payment:** **$%.2f**\n\n" +
                    "📋 *Policy Details: %s*",
                    policy.getPlanName(), targetSpecialty,
                    policy.getPlanName(), fee, coveragePct, copay, insurancePortion, patientOutOfPocket, policy.getPolicyDetails()
                );
            }
        }

        // If patient is not yet registered with an insurance plan, list in-network comparisons
        List<InsurancePolicy> allPolicies = insurancePolicyRepository.findAll();
        StringBuilder networkExamples = new StringBuilder();
        for (InsurancePolicy p : allPolicies) {
            if (p.getSpecialty().equalsIgnoreCase(targetSpecialty)) {
                double outOfPocket = p.getCopayAmount() + Math.max(0.0, (fee - p.getCopayAmount()) * ((100 - p.getCoveragePercentage()) / 100.0));
                networkExamples.append(String.format("  • **%s**: $%.2f out-of-pocket (Copay: $%.2f, %d%% covered)\n",
                        p.getPlanName(), outOfPocket, p.getCopayAmount(), p.getCoveragePercentage()));
            }
        }

        return String.format(
            "💰 **Consultation Pricing & In-Network Coverage for %s:**\n" +
            (patientName != null ? "- **Patient:** " + patientName + " *(Self-Pay / Not yet registered with an active insurance plan)*\n" : "") +
            "- **Full Self-Pay / Uninsured Fee:** **$%.2f**\n\n" +
            "🏥 **In-Network Insurance Rates for %s:**\n%s\n" +
            "💡 *If you have an active insurance plan (e.g. Aetna Silver, BlueCross Gold, or UnitedHealth), please provide your plan name and I will calculate your exact copay!*",
            targetSpecialty,
            fee,
            targetSpecialty,
            networkExamples.length() > 0 ? networkExamples.toString() : "  • Standard in-network rates available upon policy verification.\n"
        );
    }

    @Transactional
    public Appointment bookAppointment(Long patientId, Long slotId) {
        log.info("Booking appointment for patientId: {}, slotId: {}", patientId, slotId);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found with ID: " + patientId));

        return executeBooking(patient, slotId);
    }

    @Transactional
    public Appointment bookAppointmentForPatient(String patientIdentifier, Long slotId) {
        log.info("Booking appointment for patientIdentifier: '{}', slotId: {}", patientIdentifier, slotId);

        Patient patient = findPatient(patientIdentifier).orElseGet(() -> {
            log.info("Patient '{}' not found in database. Auto-registering new patient record.", patientIdentifier);
            return registerPatient(patientIdentifier, null, "Aetna Silver", "Self-registered via MediConnect AI Assistant");
        });

        return executeBooking(patient, slotId);
    }

    private Appointment executeBooking(Patient patient, Long slotId) {
        AppointmentSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment slot not found with ID: " + slotId));

        if (Boolean.TRUE.equals(slot.getIsBooked())) {
            throw new IllegalStateException("Slot ID " + slotId + " is already booked.");
        }

        slot.setIsBooked(true);
        slotRepository.save(slot);

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(slot.getDoctor())
                .slot(slot)
                .status("CONFIRMED")
                .createdAt(LocalDateTime.now())
                .build();

        Appointment savedAppointment = appointmentRepository.save(appointment);
        eventPublisherService.publishAppointmentBookedEvent(savedAppointment);

        return savedAppointment;
    }

    public List<Appointment> getPatientAppointments(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    public List<Appointment> getPatientAppointmentsByIdentifier(String patientIdentifier) {
        Optional<Patient> pOpt = findPatient(patientIdentifier);
        if (pOpt.isPresent()) {
            return appointmentRepository.findByPatientIdOrderByCreatedAtDesc(pOpt.get().getId());
        }
        return List.of();
    }
}
