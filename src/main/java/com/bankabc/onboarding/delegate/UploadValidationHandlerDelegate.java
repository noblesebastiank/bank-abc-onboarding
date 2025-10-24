package com.bankabc.onboarding.delegate;

// DISABLED: Upload validation moved to FileValidationService

import com.bankabc.onboarding.entity.Onboarding;
import com.bankabc.onboarding.entity.Onboarding.OnboardingStatus;
import com.bankabc.onboarding.service.OnboardingService;
import com.bankabc.onboarding.util.DelegateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Handler for upload validation failures.
 * Sets status back to WAITING_FOR_DOCUMENTS and throws BPMN error for controller to handle.
 */
@Component("uploadValidationHandlerDelegate")
@RequiredArgsConstructor
@Slf4j
public class UploadValidationHandlerDelegate implements JavaDelegate {

    private final DelegateUtils delegateUtils;
    private final OnboardingService onboardingService;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        UUID onboardingId = delegateUtils.getOnboardingId(execution);
        log.error("=== UPLOAD VALIDATION HANDLER INVOKED === for onboardingId: {}", onboardingId);
        log.error("Process instance ID: {}", execution.getProcessInstanceId());
        log.error("Activity ID: {}", execution.getCurrentActivityId());
        log.error("This delegate is being executed!");

        try {
            // Extract validation error details from DMN result
            Object validationResult = execution.getVariable("result");
            String errorCode = "VALIDATION_FAILED";
            String errorMessage = "Document validation failed. Please check file requirements and try again.";
            
            if (validationResult instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> resultList = (java.util.List<Map<String, Object>>) validationResult;
                if (!resultList.isEmpty()) {
                    Map<String, Object> firstResult = resultList.get(0);
                    errorCode = (String) firstResult.get("Error Code");
                    errorMessage = (String) firstResult.get("Error Message");
                }
            }
            
            // Set error details in process variables for the controller to access
            execution.setVariable("validationErrorCode", errorCode);
            execution.setVariable("validationErrorMessage", errorMessage);
            
            // Update onboarding status back to WAITING_FOR_DOCUMENTS
            Onboarding onboarding = onboardingService.findById(onboardingId)
                .orElseThrow(() -> new RuntimeException("Onboarding not found: " + onboardingId));
            
            onboarding.setStatus(OnboardingStatus.WAITING_FOR_DOCUMENTS);
            onboardingService.saveOnboarding(onboarding);
            
            log.info("Set onboarding status back to WAITING_FOR_DOCUMENTS for onboardingId: {}", onboardingId);
            
            // Throw BPMN error to be caught by the controller
            throw new BpmnError("UPLOAD_VALIDATION_FAILED", errorMessage);
            
        } catch (BpmnError e) {
            // Re-throw BPMN errors
            throw e;
        } catch (Exception e) {
            log.error("Error handling upload validation failure for onboardingId: {}", onboardingId, e);
            String fallbackMessage = "Document validation failed. Please check file requirements and try again.";
            execution.setVariable("validationErrorCode", "VALIDATION_FAILED");
            execution.setVariable("validationErrorMessage", fallbackMessage);
            throw new BpmnError("UPLOAD_VALIDATION_FAILED", fallbackMessage);
        }
    }
}
