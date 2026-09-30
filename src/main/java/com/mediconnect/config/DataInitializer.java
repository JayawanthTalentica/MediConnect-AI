package com.mediconnect.config;

import com.mediconnect.entity.*;
import com.mediconnect.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentSlotRepository slotRepository;
    private final InsurancePolicyRepository insurancePolicyRepository;

    public DataInitializer(PatientRepository patientRepository, DoctorRepository doctorRepository,
                           AppointmentSlotRepository slotRepository, InsurancePolicyRepository insurancePolicyRepository) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.slotRepository = slotRepository;
        this.insurancePolicyRepository = insurancePolicyRepository;
    }

    @Override
    public void run(String... args) {
        if (patientRepository.count() > 0) {
            log.info("✅ Database already populated with seed data. Skipping DataInitializer.");
            return;
        }

        log.info("🌱 Initializing MediConnect Database Seed Data & Policy Documents...");


        Patient p1 = Patient.builder()
                .name("Jayawanth Kumar")
                .email("jayawanth@example.com")
                .insurancePlan("Aetna Silver")
                .medicalHistory("History of migraine, mild penicillin sensitivity")
                .build();

        Patient p2 = Patient.builder()
                .name("Sophia Martinez")
                .email("sophia@example.com")
                .insurancePlan("BlueCross Gold")
                .medicalHistory("Hypertension")
                .build();

        patientRepository.saveAll(List.of(p1, p2));

        Doctor d1 = Doctor.builder()
                .name("Dr. Sarah Jenkins")
                .specialty("Neurology")
                .consultationFee(180.00)
                .hospitalBranch("Central Hospital - Downtown")
                .build();

        Doctor d2 = Doctor.builder()
                .name("Dr. Michael Chang")
                .specialty("Cardiology")
                .consultationFee(220.00)
                .hospitalBranch("St. Jude Heart Center")
                .build();

        Doctor d3 = Doctor.builder()
                .name("Dr. Emily Watson")
                .specialty("Dermatology")
                .consultationFee(150.00)
                .hospitalBranch("Westside Skin Clinic")
                .build();

        doctorRepository.saveAll(List.of(d1, d2, d3));

        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.HOURS);

        AppointmentSlot s1 = AppointmentSlot.builder().doctor(d1).slotTime(tomorrow.withHour(10)).isBooked(false).build();
        AppointmentSlot s2 = AppointmentSlot.builder().doctor(d1).slotTime(tomorrow.withHour(14)).isBooked(false).build();
        AppointmentSlot s3 = AppointmentSlot.builder().doctor(d2).slotTime(tomorrow.withHour(11)).isBooked(false).build();
        AppointmentSlot s4 = AppointmentSlot.builder().doctor(d2).slotTime(tomorrow.withHour(15)).isBooked(false).build();
        AppointmentSlot s5 = AppointmentSlot.builder().doctor(d3).slotTime(tomorrow.withHour(9)).isBooked(false).build();

        slotRepository.saveAll(List.of(s1, s2, s3, s4, s5));

        InsurancePolicy pol1 = InsurancePolicy.builder()
                .planName("Aetna Silver")
                .specialty("Neurology")
                .coveragePercentage(90)
                .copayAmount(20.00)
                .policyDetails("Aetna Silver covers Neurology specialist visits at 90% after a $20 copay. Requires pre-authorization for MRI brain scans.")
                .build();

        InsurancePolicy pol2 = InsurancePolicy.builder()
                .planName("Aetna Silver")
                .specialty("Cardiology")
                .coveragePercentage(85)
                .copayAmount(30.00)
                .policyDetails("Aetna Silver covers Cardiology consultations at 85% with a $30 copay.")
                .build();

        InsurancePolicy pol3 = InsurancePolicy.builder()
                .planName("Aetna Silver")
                .specialty("Dermatology")
                .coveragePercentage(80)
                .copayAmount(25.00)
                .policyDetails("Aetna Silver covers Dermatology appointments at 80% with a $25 copay.")
                .build();

        InsurancePolicy pol4 = InsurancePolicy.builder()
                .planName("BlueCross Gold")
                .specialty("Neurology")
                .coveragePercentage(100)
                .copayAmount(10.00)
                .policyDetails("BlueCross Gold offers full 100% coverage for Neurology consultations with a $10 copay per visit.")
                .build();

        InsurancePolicy pol5 = InsurancePolicy.builder()
                .planName("BlueCross Gold")
                .specialty("Cardiology")
                .coveragePercentage(95)
                .copayAmount(15.00)
                .policyDetails("BlueCross Gold covers Cardiology at 95% with a $15 copay.")
                .build();

        InsurancePolicy pol6 = InsurancePolicy.builder()
                .planName("BlueCross Gold")
                .specialty("Dermatology")
                .coveragePercentage(100)
                .copayAmount(15.00)
                .policyDetails("BlueCross Gold covers Dermatology consultations at 100% with a $15 copay.")
                .build();

        InsurancePolicy pol7 = InsurancePolicy.builder()
                .planName("UnitedHealth Choice")
                .specialty("Neurology")
                .coveragePercentage(80)
                .copayAmount(35.00)
                .policyDetails("UnitedHealth Choice covers Neurology at 80% after a $35 specialist copay.")
                .build();

        insurancePolicyRepository.saveAll(List.of(pol1, pol2, pol3, pol4, pol5, pol6, pol7));

        log.info("✅ Database successfully populated: 2 Patients, 3 Doctors, 5 Slots, 7 Insurance Policies!");
    }
}
