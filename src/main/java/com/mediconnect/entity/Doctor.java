package com.mediconnect.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String specialty;

    @Column(name = "consultation_fee", nullable = false)
    private Double consultationFee;

    @Column(name = "hospital_branch")
    private String hospitalBranch;

    public Doctor() {}

    public Doctor(Long id, String name, String specialty, Double consultationFee, String hospitalBranch) {
        this.id = id;
        this.name = name;
        this.specialty = specialty;
        this.consultationFee = consultationFee;
        this.hospitalBranch = hospitalBranch;
    }

    public static DoctorBuilder builder() {
        return new DoctorBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public Double getConsultationFee() { return consultationFee; }
    public void setConsultationFee(Double consultationFee) { this.consultationFee = consultationFee; }

    public String getHospitalBranch() { return hospitalBranch; }
    public void setHospitalBranch(String hospitalBranch) { this.hospitalBranch = hospitalBranch; }

    public static class DoctorBuilder {
        private Long id;
        private String name;
        private String specialty;
        private Double consultationFee;
        private String hospitalBranch;

        public DoctorBuilder id(Long id) { this.id = id; return this; }
        public DoctorBuilder name(String name) { this.name = name; return this; }
        public DoctorBuilder specialty(String specialty) { this.specialty = specialty; return this; }
        public DoctorBuilder consultationFee(Double consultationFee) { this.consultationFee = consultationFee; return this; }
        public DoctorBuilder hospitalBranch(String hospitalBranch) { this.hospitalBranch = hospitalBranch; return this; }

        public Doctor build() {
            return new Doctor(id, name, specialty, consultationFee, hospitalBranch);
        }
    }
}
