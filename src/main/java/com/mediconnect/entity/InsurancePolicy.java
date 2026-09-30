package com.mediconnect.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "insurance_policies")
public class InsurancePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_name", nullable = false)
    private String planName;

    @Column(name = "specialty", nullable = false)
    private String specialty;

    @Column(name = "coverage_percentage", nullable = false)
    private Integer coveragePercentage;

    @Column(name = "copay_amount", nullable = false)
    private Double copayAmount;

    @Column(length = 2000, nullable = false)
    private String policyDetails;

    public InsurancePolicy() {}

    public InsurancePolicy(Long id, String planName, String specialty, Integer coveragePercentage, Double copayAmount, String policyDetails) {
        this.id = id;
        this.planName = planName;
        this.specialty = specialty;
        this.coveragePercentage = coveragePercentage;
        this.copayAmount = copayAmount;
        this.policyDetails = policyDetails;
    }

    public static InsurancePolicyBuilder builder() {
        return new InsurancePolicyBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public Integer getCoveragePercentage() { return coveragePercentage; }
    public void setCoveragePercentage(Integer coveragePercentage) { this.coveragePercentage = coveragePercentage; }

    public Double getCopayAmount() { return copayAmount; }
    public void setCopayAmount(Double copayAmount) { this.copayAmount = copayAmount; }

    public String getPolicyDetails() { return policyDetails; }
    public void setPolicyDetails(String policyDetails) { this.policyDetails = policyDetails; }

    public static class InsurancePolicyBuilder {
        private Long id;
        private String planName;
        private String specialty;
        private Integer coveragePercentage;
        private Double copayAmount;
        private String policyDetails;

        public InsurancePolicyBuilder id(Long id) { this.id = id; return this; }
        public InsurancePolicyBuilder planName(String planName) { this.planName = planName; return this; }
        public InsurancePolicyBuilder specialty(String specialty) { this.specialty = specialty; return this; }
        public InsurancePolicyBuilder coveragePercentage(Integer coveragePercentage) { this.coveragePercentage = coveragePercentage; return this; }
        public InsurancePolicyBuilder copayAmount(Double copayAmount) { this.copayAmount = copayAmount; return this; }
        public InsurancePolicyBuilder policyDetails(String policyDetails) { this.policyDetails = policyDetails; return this; }

        public InsurancePolicy build() {
            return new InsurancePolicy(id, planName, specialty, coveragePercentage, copayAmount, policyDetails);
        }
    }
}
