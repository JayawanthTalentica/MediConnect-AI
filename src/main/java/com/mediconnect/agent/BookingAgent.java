package com.mediconnect.agent;

import com.mediconnect.entity.Appointment;
import com.mediconnect.entity.AppointmentSlot;
import com.mediconnect.service.ClinicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingAgent {

    private static final Logger log = LoggerFactory.getLogger(BookingAgent.class);

    private final ClinicService clinicService;

    public BookingAgent(ClinicService clinicService) {
        this.clinicService = clinicService;
    }

    public String handleBookingTask(String prompt, String patientName) {
        log.info("🤖 [SPECIALIST AGENT: BookingAgent] Executing booking task for prompt: '{}'", prompt);
        String lower = prompt.toLowerCase();

        // 1. View Appointments
        if (lower.contains("history") || lower.contains("my appointment") || lower.contains("show appointment")) {
            String identifier = (patientName != null) ? patientName : "1";
            List<Appointment> list = clinicService.getPatientAppointmentsByIdentifier(identifier);
            if (list.isEmpty()) {
                return String.format("📋 **Patient Appointments History:**\nNo booked appointments found%s.",
                        patientName != null ? " for **" + patientName + "**" : "");
            }
            String summary = list.stream()
                    .map(a -> String.format("- **Appointment #%d** | Patient: %s | Doctor: %s (%s) | Status: %s | Time: %s",
                            a.getId(), a.getPatient().getName(), a.getDoctor().getName(),
                            a.getDoctor().getSpecialty(), a.getStatus(), a.getSlot().getSlotTime()))
                    .collect(Collectors.joining("\n"));
            return "📋 **Confirmed Appointments:**\n\n" + summary;
        }

        // 2. Book Slot
        if (lower.contains("book") || lower.contains("confirm") || lower.contains("slot")) {
            Long slotId = extractSlotId(lower);
            String identifier = (patientName != null) ? patientName : "Jayawanth Kumar";

            try {
                var appointment = clinicService.bookAppointmentForPatient(identifier, slotId);
                return String.format(
                        "✅ **Appointment Confirmed! (Processed by Booking Agent)**\n\n" +
                                "- **Appointment ID:** #%d\n" +
                                "- **Patient:** %s\n" +
                                "- **Doctor:** %s (%s)\n" +
                                "- **Time:** %s\n" +
                                "- **Hospital Branch:** %s\n" +
                                "- **Status:** CONFIRMED\n\n" +
                                "📢 *Published to Kafka topic `appointment-events` & SSE Dashboard Feed!*",
                        appointment.getId(), appointment.getPatient().getName(), appointment.getDoctor().getName(),
                        appointment.getDoctor().getSpecialty(), appointment.getSlot().getSlotTime(),
                        appointment.getDoctor().getHospitalBranch());
            } catch (Exception e) {
                return "⚠️ **Booking Notice:** " + e.getMessage() + ". Please check available open slots on the dashboard.";
            }
        }

        // 3. Search Available Doctor Slots
        String specialty = extractSpecialty(lower);
        if (specialty == null) specialty = "Neurology";

        List<AppointmentSlot> openSlots = clinicService.findAvailableSlots(specialty);
        if (openSlots.isEmpty()) {
            return "👨‍⚕️ **Booking Agent:** No open slots available for specialty: " + specialty;
        }

        StringBuilder sb = new StringBuilder("👨‍⚕️ **Booking Agent — Available " + specialty + " Specialists & Slots:**\n\n");
        for (AppointmentSlot slot : openSlots) {
            sb.append(String.format("- **Slot ID #%d** | %s | Fee: $%s | Time: %s (%s)\n",
                    slot.getId(), slot.getDoctor().getName(), slot.getDoctor().getConsultationFee(),
                    slot.getSlotTime(), slot.getDoctor().getHospitalBranch()));
        }
        sb.append("\n👉 *Reply with **'Book Slot #ID'** to confirm your reservation.*");
        return sb.toString();
    }

    private Long extractSlotId(String prompt) {
        for (String word : prompt.split("\\s+")) {
            String cleaned = word.replaceAll("[^0-9]", "");
            if (!cleaned.isEmpty()) {
                try {
                    return Long.parseLong(cleaned);
                } catch (Exception ignored) {}
            }
        }
        return 1L;
    }

    private String extractSpecialty(String prompt) {
        if (prompt.contains("headache") || prompt.contains("neuro") || prompt.contains("brain")) return "Neurology";
        if (prompt.contains("heart") || prompt.contains("chest") || prompt.contains("cardio")) return "Cardiology";
        if (prompt.contains("skin") || prompt.contains("rash") || prompt.contains("derma")) return "Dermatology";
        if (prompt.contains("child") || prompt.contains("baby") || prompt.contains("pedia")) return "Pediatrics";
        return null;
    }
}
