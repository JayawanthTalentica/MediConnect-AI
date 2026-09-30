package com.mediconnect.repository;

import com.mediconnect.entity.AppointmentSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {
    List<AppointmentSlot> findByDoctorIdAndIsBooked(Long doctorId, Boolean isBooked);

    @Query("SELECT s FROM AppointmentSlot s WHERE LOWER(s.doctor.specialty) LIKE LOWER(CONCAT('%', :specialty, '%')) AND s.isBooked = false AND s.slotTime >= :startTime ORDER BY s.slotTime ASC")
    List<AppointmentSlot> findAvailableSlotsBySpecialty(@Param("specialty") String specialty, @Param("startTime") LocalDateTime startTime);
}
