package com.mediconnect.repository;

import com.mediconnect.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<Appointment> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);
}
