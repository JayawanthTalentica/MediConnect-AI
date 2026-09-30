-- ============================================================
-- Flyway Migration: V1__init_schema.sql
-- Description: Create initial schema tables for MediConnect AI
-- ============================================================

-- 1. Patients Table
CREATE TABLE IF NOT EXISTS patients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    insurance_plan VARCHAR(255) NOT NULL,
    medical_history VARCHAR(1000)
);

-- 2. Doctors Table
CREATE TABLE IF NOT EXISTS doctors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    specialty VARCHAR(255) NOT NULL,
    consultation_fee DOUBLE PRECISION NOT NULL,
    hospital_branch VARCHAR(255)
);

-- 3. Appointment Slots Table
CREATE TABLE IF NOT EXISTS appointment_slots (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    slot_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    is_booked BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_slots_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
);

-- 4. Appointments Table
CREATE TABLE IF NOT EXISTS appointments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    slot_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
    CONSTRAINT fk_appointments_slot FOREIGN KEY (slot_id) REFERENCES appointment_slots(id)
);

-- 5. Insurance Policies Table (RAG Metadata)
CREATE TABLE IF NOT EXISTS insurance_policies (
    id BIGSERIAL PRIMARY KEY,
    plan_name VARCHAR(255) NOT NULL,
    specialty VARCHAR(255) NOT NULL,
    coverage_percentage INTEGER NOT NULL,
    copay_amount DOUBLE PRECISION NOT NULL,
    policy_details VARCHAR(2000) NOT NULL
);
