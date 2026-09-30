-- ============================================================
-- Flyway Migration: V2__seed_data.sql
-- Description: Seed initial patient, doctor, slot, and policy data
-- ============================================================

-- 1. Insert Initial Patients
INSERT INTO patients (name, email, insurance_plan, medical_history)
VALUES 
  ('Jayawanth Kumar', 'jayawanth@example.com', 'Aetna Silver', 'History of migraine, mild penicillin sensitivity'),
  ('Sophia Martinez', 'sophia@example.com', 'BlueCross Gold', 'Hypertension')
ON CONFLICT (email) DO NOTHING;

-- 2. Insert Initial Doctors
INSERT INTO doctors (name, specialty, consultation_fee, hospital_branch)
VALUES 
  ('Dr. Sarah Jenkins', 'Neurology', 180.00, 'Central Hospital - Downtown'),
  ('Dr. Michael Chang', 'Cardiology', 220.00, 'St. Jude Heart Center'),
  ('Dr. Emily Watson', 'Dermatology', 150.00, 'Westside Skin Clinic');

-- 3. Insert Initial Appointment Slots
INSERT INTO appointment_slots (doctor_id, slot_time, is_booked)
VALUES 
  (1, CURRENT_DATE + INTERVAL '1 day 10 hours', false),
  (1, CURRENT_DATE + INTERVAL '1 day 14 hours', false),
  (2, CURRENT_DATE + INTERVAL '1 day 11 hours', false),
  (2, CURRENT_DATE + INTERVAL '1 day 15 hours', false),
  (3, CURRENT_DATE + INTERVAL '1 day 9 hours', false);

-- 4. Insert Initial Insurance Policies
INSERT INTO insurance_policies (plan_name, specialty, coverage_percentage, copay_amount, policy_details)
VALUES 
  ('Aetna Silver', 'Neurology', 90, 20.00, 'Aetna Silver covers Neurology specialist visits at 90% after a $20 copay. Requires pre-authorization for MRI brain scans.'),
  ('Aetna Silver', 'Cardiology', 85, 30.00, 'Aetna Silver covers Cardiology consultations at 85% with a $30 copay.'),
  ('BlueCross Gold', 'Neurology', 100, 10.00, 'BlueCross Gold offers full 100% coverage for Neurology consultations with a $10 copay per visit.');
