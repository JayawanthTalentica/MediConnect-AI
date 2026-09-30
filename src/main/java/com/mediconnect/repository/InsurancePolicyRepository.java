package com.mediconnect.repository;

import com.mediconnect.entity.InsurancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, Long> {
    Optional<InsurancePolicy> findByPlanNameIgnoreCaseAndSpecialtyIgnoreCase(String planName, String specialty);
    List<InsurancePolicy> findByPlanNameContainingIgnoreCase(String planName);
}
