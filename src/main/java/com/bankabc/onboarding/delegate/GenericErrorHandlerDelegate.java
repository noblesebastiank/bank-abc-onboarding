package com.bankabc.onboarding.delegate;

import com.bankabc.onboarding.entity.Onboarding;
import com.bankabc.onboarding.entity.Onboarding.OnboardingStatus;
import com.bankabc.onboarding.service.NotificationService;
import com.bankabc.onboarding.service.OnboardingService;
import com.bankabc.onboarding.constants.ApplicationConstants;
import com.bankabc.onboarding.util.DelegateUtils;
import com.bankabc.onboarding.service.WorkflowConfigurationService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Centralized error handler for all onboarding process failures.
 * Determines the failure type, updates onboarding status, and notifies the customer.
 */
@Component("genericErrorHandlerDelegate")
@RequiredArgsConstructor
@Slf4j
public class GenericErrorHandlerDelegate implements JavaDelegate {

    private final OnboardingService onboardingService;
    private final NotificationService notificationService;
    private final WorkflowConfigurationService workflowConfigurationService;
    private final DelegateUtils delegateUtils;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        log.warn("Executing GenericErrorHandlerDelegate for processInstanceId: {}", processInstanceId);

        try {
            Onboarding onboarding = delegateUtils.getOnboarding(execution);
            UUID onboardingId = onboarding.getId();

            // Update onboarding status to FAILED when any step fails
            onboarding.setStatus(OnboardingStatus.FAILED);
            onboardingService.saveOnboarding(onboarding);

            String failedStepId = Optional.ofNullable((String) execution.getVariable("failedStepId"))
                .orElse("unknown-step");

            // Derive error details
            String errorType = determineErrorType(execution, failedStepId);
            String errorMessage = determineErrorMessage(execution, failedStepId, errorType);

            // Send notification to customer
            String customerName = "Customer";

            boolean notificationSent = false;
            try {
                notificationSent = notificationService.sendFailureNotifications(
                    onboarding.getEmail(),
                    onboarding.getPhone(),
                    errorType,
                    errorMessage,
                    customerName
                );
            } catch (Exception notifyEx) {
                log.error("Notification sending failed for onboardingId: {}", onboardingId, notifyEx);
            }

            if (notificationSent) {
                log.info("Error notification sent successfully for onboardingId: {} | Type: {}", onboardingId, errorType);
            } else {
                log.warn("Failed to send error notification for onboardingId: {} | Type: {}", onboardingId, errorType);
            }

            // Update BPMN variables
            execution.setVariable(ApplicationConstants.ProcessVariables.ERROR_TYPE, errorType);
            execution.setVariable(ApplicationConstants.ProcessVariables.ERROR_MESSAGE, errorMessage);
            execution.setVariable(ApplicationConstants.ProcessVariables.NOTIFICATION_SENT, notificationSent);
            execution.setVariable(ApplicationConstants.ProcessVariables.STATUS, OnboardingStatus.FAILED.name());

        } catch (Exception e) {
            log.error("Critical failure inside GenericErrorHandlerDelegate (processInstanceId: {})", processInstanceId, e);
            execution.setVariable(ApplicationConstants.ProcessVariables.ERROR_MESSAGE, "Error handling failed: " + e.getMessage());
            throw e;
        }
    }

    private String determineErrorType(DelegateExecution execution, String failedStepId) {
        try {
            JsonNode stepConfig = workflowConfigurationService.getStepConfiguration(failedStepId);
            return Optional.ofNullable(stepConfig)
                .map(cfg -> cfg.path("errorHandling").path("errorType").asText(null))
                .filter(type -> !type.isEmpty())
                .orElseGet(() -> determineErrorTypeLegacy(execution));
        } catch (Exception e) {
            log.warn("Unable to resolve errorType from config for stepId: {}", failedStepId, e);
            return "GENERAL_FAILURE";
        }
    }

    private String determineErrorMessage(DelegateExecution execution, String failedStepId, String errorType) {
        String existing = Optional.ofNullable((String) execution.getVariable("errorMessage")).orElse("");

        if (!existing.isBlank()) {
            return existing;
        }

        try {
            JsonNode stepConfig = workflowConfigurationService.getStepConfiguration(failedStepId);
            String defaultMessage = stepConfig.path("errorHandling").path("defaultMessage").asText(null);
            if (defaultMessage != null && !defaultMessage.isBlank()) {
                return defaultMessage;
            }
        } catch (Exception e) {
            log.debug("No default error message found in config for stepId: {}", failedStepId);
        }

        return determineErrorMessageLegacy(errorType);
    }

    private String determineErrorTypeLegacy(DelegateExecution execution) {
        if ("FAILED".equals(execution.getVariable("kycResult"))) return "KYC_VERIFICATION_FAILED";
        if ("FAILED".equals(execution.getVariable("addressResult"))) return "ADDRESS_VERIFICATION_FAILED";
        if ("FAILED".equals(execution.getVariable("accountCreationResult"))) return "ACCOUNT_CREATION_FAILED";
        if ("FAILED".equals(execution.getVariable("documentUploadResult"))) return "DOCUMENT_UPLOAD_FAILED";
        return "GENERAL_FAILURE";
    }

    private String determineErrorMessageLegacy(String errorType) {
        return switch (errorType) {
            case "KYC_VERIFICATION_FAILED" -> "Identity verification failed";
            case "ADDRESS_VERIFICATION_FAILED" -> "Address verification failed";
            case "ACCOUNT_CREATION_FAILED" -> "Account creation failed";
            case "DOCUMENT_UPLOAD_FAILED" -> "Document upload failed";
            default -> "Onboarding process failed due to an unexpected error";
        };
    }
}
