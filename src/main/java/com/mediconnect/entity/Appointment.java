package com.mediconnect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "slot_id", nullable = false)
    private AppointmentSlot slot;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Appointment() {}

    public Appointment(Long id, Patient patient, Doctor doctor, AppointmentSlot slot, String status, LocalDateTime createdAt) {
        this.id = id;
        this.patient = patient;
        this.doctor = doctor;
        this.slot = slot;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static AppointmentBuilder builder() {
        return new AppointmentBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }

    public AppointmentSlot getSlot() { return slot; }
    public void setSlot(AppointmentSlot slot) { this.slot = slot; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class AppointmentBuilder {
        private Long id;
        private Patient patient;
        private Doctor doctor;
        private AppointmentSlot slot;
        private String status;
        private LocalDateTime createdAt;

        public AppointmentBuilder id(Long id) { this.id = id; return this; }
        public AppointmentBuilder patient(Patient patient) { this.patient = patient; return this; }
        public AppointmentBuilder doctor(Doctor doctor) { this.doctor = doctor; return this; }
        public AppointmentBuilder slot(AppointmentSlot slot) { this.slot = slot; return this; }
        public AppointmentBuilder status(String status) { this.status = status; return this; }
        public AppointmentBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Appointment build() {
            return new Appointment(id, patient, doctor, slot, status, createdAt);
        }
    }
}
