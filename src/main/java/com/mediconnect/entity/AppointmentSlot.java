package com.mediconnect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_slots")
public class AppointmentSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "slot_time", nullable = false)
    private LocalDateTime slotTime;

    @Column(name = "is_booked", nullable = false)
    private Boolean isBooked;

    public AppointmentSlot() {}

    public AppointmentSlot(Long id, Doctor doctor, LocalDateTime slotTime, Boolean isBooked) {
        this.id = id;
        this.doctor = doctor;
        this.slotTime = slotTime;
        this.isBooked = isBooked;
    }

    public static AppointmentSlotBuilder builder() {
        return new AppointmentSlotBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }

    public LocalDateTime getSlotTime() { return slotTime; }
    public void setSlotTime(LocalDateTime slotTime) { this.slotTime = slotTime; }

    public Boolean getIsBooked() { return isBooked; }
    public void setIsBooked(Boolean isBooked) { this.isBooked = isBooked; }

    public static class AppointmentSlotBuilder {
        private Long id;
        private Doctor doctor;
        private LocalDateTime slotTime;
        private Boolean isBooked;

        public AppointmentSlotBuilder id(Long id) { this.id = id; return this; }
        public AppointmentSlotBuilder doctor(Doctor doctor) { this.doctor = doctor; return this; }
        public AppointmentSlotBuilder slotTime(LocalDateTime slotTime) { this.slotTime = slotTime; return this; }
        public AppointmentSlotBuilder isBooked(Boolean isBooked) { this.isBooked = isBooked; return this; }

        public AppointmentSlot build() {
            return new AppointmentSlot(id, doctor, slotTime, isBooked);
        }
    }
}
