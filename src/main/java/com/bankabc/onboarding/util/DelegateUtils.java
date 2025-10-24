package com.bankabc.onboarding.util;

import com.bankabc.onboarding.entity.Onboarding;
import com.bankabc.onboarding.exception.DefaultApiError;
import com.bankabc.onboarding.exception.ErrorTypes;
import com.bankabc.onboarding.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Utility class for common delegate operations.
 * Provides reusable methods for delegate validation and data retrieval.
 */
@Component
@RequiredArgsConstructor
public final class DelegateUtils {

    private final OnboardingService onboardingService;

    /**
     * Retrieves and validates the onboarding ID from process variables.
     * 
     * @param execution the delegate execution context
     * @return the validated onboarding ID
     * @throws DefaultApiError if onboarding ID is not found or invalid
     */
    public UUID getOnboardingId(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        UUID onboardingId = (UUID) execution.getVariable("onboardingId");
        
        if (onboardingId == null) {
            throw new DefaultApiError(
                HttpStatus.BAD_REQUEST,
                ErrorTypes.INVALID_REQUEST.name(),
                "Onboarding ID not found in process variables",
                Map.of("processInstanceId", processInstanceId)
            );
        }
        
        return onboardingId;
    }

    /**
     * Retrieves the onboarding entity by ID with validation.
     * 
     * @param execution the delegate execution context
     * @return the onboarding entity
     * @throws DefaultApiError if onboarding ID is not found or onboarding entity doesn't exist
     */
    public Onboarding getOnboarding(DelegateExecution execution) {
        UUID onboardingId = getOnboardingId(execution);
        
        return onboardingService.findById(onboardingId)
            .orElseThrow(() -> new DefaultApiError(
                HttpStatus.NOT_FOUND,
                ErrorTypes.ONBOARDING_NOT_FOUND.name(),
                "Onboarding not found: " + onboardingId,
                Map.of("onboardingId", onboardingId.toString())
            ));
    }

    /**
     * Retrieves the onboarding entity by ID with validation.
     * 
     * @param execution the delegate execution context
     * @param onboardingId the onboarding ID to validate and retrieve
     * @return the onboarding entity
     * @throws DefaultApiError if onboarding entity doesn't exist
     */
    public Onboarding getOnboarding(DelegateExecution execution, UUID onboardingId) {
        return onboardingService.findById(onboardingId)
            .orElseThrow(() -> new DefaultApiError(
                HttpStatus.NOT_FOUND,
                ErrorTypes.ONBOARDING_NOT_FOUND.name(),
                "Onboarding not found: " + onboardingId,
                Map.of("onboardingId", onboardingId.toString())
            ));
    }
}
