package com.mediconnect.controller;

import com.mediconnect.entity.Appointment;
import com.mediconnect.entity.Doctor;
import com.mediconnect.repository.AppointmentRepository;
import com.mediconnect.repository.DoctorRepository;
import com.mediconnect.repository.PatientRepository;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/clinic")
@CrossOrigin(origins = "*")
public class ClinicApiController {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;

    private final List<SseEmitter> sseEmitters = new CopyOnWriteArrayList<>();

    public ClinicApiController(DoctorRepository doctorRepository, AppointmentRepository appointmentRepository, PatientRepository patientRepository) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/doctors")
    public ResponseEntity<List<Doctor>> getDoctors() {
        return ResponseEntity.ok(doctorRepository.findAll());
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<Appointment>> getAppointments() {
        return ResponseEntity.ok(appointmentRepository.findAll());
    }

    @GetMapping("/events/stream")
    public SseEmitter streamEvents() {
        SseEmitter emitter = new SseEmitter(180_000L);
        sseEmitters.add(emitter);

        emitter.onCompletion(() -> sseEmitters.remove(emitter));
        emitter.onTimeout(() -> sseEmitters.remove(emitter));

        return emitter;
    }

    @EventListener
    public void onAppointmentBooked(Appointment appointment) {
        String payload = String.format(
            "{\"type\":\"APPOINTMENT_BOOKED\",\"id\":%d,\"patient\":\"%s\",\"doctor\":\"%s\",\"time\":\"%s\"}",
            appointment.getId(), appointment.getPatient().getName(),
            appointment.getDoctor().getName(), appointment.getSlot().getSlotTime()
        );

        for (SseEmitter emitter : sseEmitters) {
            try {
                emitter.send(SseEmitter.event().name("kafka-event").data(payload));
            } catch (IOException e) {
                sseEmitters.remove(emitter);
            }
        }
    }
}
