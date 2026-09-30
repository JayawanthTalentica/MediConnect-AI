package com.mediconnect.repository;

import com.mediconnect.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findBySpecialtyIgnoreCase(String specialty);
    List<Doctor> findBySpecialtyContainingIgnoreCase(String specialty);
}
