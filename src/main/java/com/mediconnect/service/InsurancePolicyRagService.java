package com.mediconnect.service;

import com.mediconnect.entity.InsurancePolicy;
import com.mediconnect.repository.InsurancePolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InsurancePolicyRagService {

    private static final Logger log = LoggerFactory.getLogger(InsurancePolicyRagService.class);

    private final InsurancePolicyRepository policyRepository;

    public InsurancePolicyRagService(InsurancePolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    public String searchInsuranceCoverage(String planName, String specialty) {
        log.info("RAG Querying Insurance Policy for plan: '{}', specialty: '{}'", planName, specialty);

        Optional<InsurancePolicy> policyOpt = policyRepository.findByPlanNameIgnoreCaseAndSpecialtyIgnoreCase(planName, specialty);

        if (policyOpt.isPresent()) {
            InsurancePolicy p = policyOpt.get();
            return String.format(
                "Policy Match Found [%s - %s]: Coverage is %d%% with a $%s copay per visit. Details: %s",
                p.getPlanName(), p.getSpecialty(), p.getCoveragePercentage(), p.getCopayAmount(), p.getPolicyDetails()
            );
        }

        List<InsurancePolicy> policies = policyRepository.findByPlanNameContainingIgnoreCase(planName);
        if (!policies.isEmpty()) {
            StringBuilder sb = new StringBuilder("Related Policies Found for " + planName + ":\n");
            for (InsurancePolicy p : policies) {
                sb.append(String.format("- %s (%s): %d%% covered, $%s copay. Details: %s\n",
                    p.getPlanName(), p.getSpecialty(), p.getCoveragePercentage(), p.getCopayAmount(), p.getPolicyDetails()));
            }
            return sb.toString();
        }

        return "No specific insurance policy found for plan '" + planName + "' and specialty '" + specialty + "'. Standard out-of-network rates apply.";
    }
}
