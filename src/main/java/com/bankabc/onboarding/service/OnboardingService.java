package com.bankabc.onboarding.service;

import com.bankabc.onboarding.entity.Onboarding;
import com.bankabc.onboarding.exception.DefaultApiError;
import com.bankabc.onboarding.exception.ErrorTypes;
import com.bankabc.onboarding.repository.OnboardingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Main service orchestrating the complete onboarding process.
 * Coordinates verification, account creation, and notification services.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OnboardingService {

    private final OnboardingRepository onboardingRepository;

    /**
     * Saves onboarding entity to database.
     * 
     * @param onboarding the onboarding entity to save
     * @return the saved onboarding entity
     */
    public Onboarding saveOnboarding(Onboarding onboarding) {
        return onboardingRepository.save(onboarding);
    }

    /**
     * Finds onboarding by ID.
     * 
     * @param id the onboarding ID
     * @return Optional containing the onboarding entity
     */
    public Optional<Onboarding> findById(Long id) {
        return onboardingRepository.findById(UUID.fromString(id.toString()));
    }

    /**
     * Finds onboarding by UUID.
     * 
     * @param id the onboarding UUID
     * @return Optional containing the onboarding entity
     */
    public Optional<Onboarding> findById(UUID id) {
        return onboardingRepository.findById(id);
    }

    /**
     * Finds onboarding by process instance ID.
     * 
     * @param processInstanceId the BPMN process instance ID
     * @return Optional containing the onboarding entity
     */
    public Optional<Onboarding> findByProcessInstanceId(String processInstanceId) {
        return onboardingRepository.findByProcessInstanceId(processInstanceId);
    }

    /**
     * Checks if customer already exists by SSN.
     * 
     * @param ssn the social security number
     * @return true if customer exists
     */
    public boolean existsBySsn(String ssn) {
        return onboardingRepository.existsBySsn(ssn);
    }

    /**
     * Finds onboarding by ID and throws DefaultApiError if not found.
     * 
     * @param id the onboarding ID
     * @return the onboarding entity
     * @throws DefaultApiError if onboarding not found
     */
    public Onboarding findByIdOrThrow(Long id) {
        return findById(id)
                .orElseThrow(() -> new DefaultApiError(
                    HttpStatus.NOT_FOUND,
                    ErrorTypes.ONBOARDING_NOT_FOUND.name(),
                    ErrorTypes.ONBOARDING_NOT_FOUND.getMessage(),
                    Map.of("id", id.toString())
                ));
    }

    /**
     * Finds onboarding by process instance ID and throws DefaultApiError if not found.
     * 
     * @param processInstanceId the BPMN process instance ID
     * @return the onboarding entity
     * @throws DefaultApiError if onboarding not found
     */
    public Onboarding findByProcessInstanceIdOrThrow(String processInstanceId) {
        return findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new DefaultApiError(
                    HttpStatus.NOT_FOUND,
                    ErrorTypes.ONBOARDING_NOT_FOUND.name(),
                    ErrorTypes.ONBOARDING_NOT_FOUND.getMessage(),
                    Map.of("processInstanceId", processInstanceId)
                ));
    }

}
