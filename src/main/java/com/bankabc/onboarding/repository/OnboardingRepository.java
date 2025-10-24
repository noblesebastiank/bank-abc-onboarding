package com.bankabc.onboarding.repository;

import com.bankabc.onboarding.entity.Onboarding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Onboarding entity operations.
 * Provides CRUD operations and custom queries for onboarding data.
 */
@Repository
public interface OnboardingRepository extends JpaRepository<Onboarding, UUID> {


    /**
     * Checks if a customer with the given SSN already exists.
     * 
     * @param ssn the social security number
     * @return true if customer exists, false otherwise
     */
    boolean existsBySsn(String ssn);



    /**
     * Finds onboarding by BPMN process instance ID.
     * 
     * @param processInstanceId the BPMN process instance ID
     * @return Optional containing onboarding if found
     */
    Optional<Onboarding> findByProcessInstanceId(String processInstanceId);
}

