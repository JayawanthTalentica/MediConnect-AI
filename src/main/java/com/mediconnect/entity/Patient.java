package com.mediconnect.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "insurance_plan", nullable = false)
    private String insurancePlan;

    @Column(name = "medical_history", length = 1000)
    private String medicalHistory;

    public Patient() {}

    public Patient(Long id, String name, String email, String insurancePlan, String medicalHistory) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.insurancePlan = insurancePlan;
        this.medicalHistory = medicalHistory;
    }

    public static PatientBuilder builder() {
        return new PatientBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getInsurancePlan() { return insurancePlan; }
    public void setInsurancePlan(String insurancePlan) { this.insurancePlan = insurancePlan; }

    public String getMedicalHistory() { return medicalHistory; }
    public void setMedicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; }

    public static class PatientBuilder {
        private Long id;
        private String name;
        private String email;
        private String insurancePlan;
        private String medicalHistory;

        public PatientBuilder id(Long id) { this.id = id; return this; }
        public PatientBuilder name(String name) { this.name = name; return this; }
        public PatientBuilder email(String email) { this.email = email; return this; }
        public PatientBuilder insurancePlan(String insurancePlan) { this.insurancePlan = insurancePlan; return this; }
        public PatientBuilder medicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; return this; }

        public Patient build() {
            return new Patient(id, name, email, insurancePlan, medicalHistory);
        }
    }
}
