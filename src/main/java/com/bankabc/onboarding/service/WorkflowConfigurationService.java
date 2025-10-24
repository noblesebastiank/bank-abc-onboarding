package com.bankabc.onboarding.service;

import com.bankabc.onboarding.entity.Onboarding.OnboardingStatus;
import com.bankabc.onboarding.exception.DefaultApiError;
import com.bankabc.onboarding.exception.ErrorTypes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing workflow configuration and dynamic step descriptions.
 * Loads configuration from onboarding.json and provides dynamic descriptions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowConfigurationService {

    private final ObjectMapper objectMapper;
    private Map<String, JsonNode> stepConfigurations = new HashMap<>();
    private Map<OnboardingStatus, String> nextStepMappings = new HashMap<>();
    private JsonNode workflowConfig;

    @PostConstruct
    public void loadConfiguration() {
        try {
            ClassPathResource resource = new ClassPathResource("onboarding.json");
            JsonNode rootNode = objectMapper.readTree(resource.getInputStream());
            
            // Load workflow configuration
            workflowConfig = rootNode.get("workflow");
            
            // Load step configurations
            JsonNode steps = rootNode.get("steps");
            if (steps != null && steps.isArray()) {
                for (JsonNode step : steps) {
                    String stepId = step.get("id").asText();
                    stepConfigurations.put(stepId, step);
                }
            }
            
            // Initialize status to next step mappings
            initializeNextStepMappings();
            
            log.info("Loaded workflow configuration with {} steps", stepConfigurations.size());
            
        } catch (IOException e) {
            log.error("Failed to load workflow configuration", e);
            throw new DefaultApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorTypes.INTERNAL_SERVER_ERROR.name(),
                "Failed to load workflow configuration: " + e.getMessage(),
                Map.of("originalError", e.getMessage())
            );
        }
    }

    /**
     * Get the next step description based on current status.
     * 
     * @param currentStatus The current onboarding status
     * @return Dynamic next step description
     */
    public String getNextStepDescription(OnboardingStatus currentStatus) {
        String nextStepId = nextStepMappings.get(currentStatus);
        if (nextStepId == null) {
            return getDefaultNextStepDescription(currentStatus);
        }
        
        JsonNode stepConfig = stepConfigurations.get(nextStepId);
        if (stepConfig != null) {
            // First try to get nextStepDescription, then fall back to description
            if (stepConfig.has("nextStepDescription")) {
                return stepConfig.get("nextStepDescription").asText();
            } else if (stepConfig.has("description")) {
                return stepConfig.get("description").asText();
            }
        }
        
        return getDefaultNextStepDescription(currentStatus);
    }

    /**
     * Get the current step description based on status.
     * 
     * @param currentStatus The current onboarding status
     * @return Dynamic current step description
     */
    public String getCurrentStepDescription(OnboardingStatus currentStatus) {
        return switch (currentStatus) {
            case INITIATED -> "Starting onboarding process";
            case INFO_COLLECTED -> "Information collected successfully";
            case WAITING_FOR_DOCUMENTS -> "Waiting for document upload";
            case DOCUMENTS_UPLOADED -> "Documents uploaded and processed";
            case KYC_IN_PROGRESS -> "KYC verification in progress";
            case KYC_COMPLETED -> "KYC verification completed";
            case ADDRESS_VERIFICATION_IN_PROGRESS -> "Address verification in progress";
            case ADDRESS_VERIFICATION_COMPLETED -> "Address verification completed";
            case ACCOUNT_CREATION_IN_PROGRESS -> "Creating bank account";
            case ACCOUNT_CREATED -> "Bank account created successfully";
            case NOTIFICATION_SENT -> "Customer notification sent";
            case COMPLETED -> "Onboarding completed successfully";
            case FAILED -> "Onboarding process failed";
        };
    }

    /**
     * Get the next step ID based on current status.
     * 
     * @param currentStatus The current onboarding status
     * @return Next step ID
     */
    public String getNextStepId(OnboardingStatus currentStatus) {
        return nextStepMappings.get(currentStatus);
    }

    /**
     * Get step configuration by ID.
     * 
     * @param stepId The step ID
     * @return Step configuration as JsonNode
     */
    public JsonNode getStepConfiguration(String stepId) {
        return stepConfigurations.get(stepId);
    }

    /**
     * Get the process definition key from configuration.
     * 
     * @return Process definition key
     */
    public String getProcessDefinitionKey() {
        if (workflowConfig != null && workflowConfig.has("processDefinitionKey")) {
            return workflowConfig.get("processDefinitionKey").asText();
        }
        return "onboarding-process"; // fallback
    }

    /**
     * Get message name for a specific step.
     * 
     * @param stepId The step ID
     * @return Message name for the step
     */
    public String getMessageName(String stepId) {
        JsonNode stepConfig = stepConfigurations.get(stepId);
        if (stepConfig != null && stepConfig.has("messageName")) {
            return stepConfig.get("messageName").asText();
        }
        return null;
    }

    /**
     * Get status message for a specific status.
     * 
     * @param status The onboarding status
     * @return Status message
     */
    public String getStatusMessage(OnboardingStatus status) {
        return switch (status) {
            case INITIATED -> "Starting onboarding process";
            case INFO_COLLECTED -> "Information collected successfully";
            case WAITING_FOR_DOCUMENTS -> "Waiting for document upload";
            case DOCUMENTS_UPLOADED -> "Documents uploaded and processed";

            case KYC_IN_PROGRESS -> "KYC verification in progress";
            case KYC_COMPLETED -> "KYC verification completed";
            case ADDRESS_VERIFICATION_IN_PROGRESS -> "Address verification in progress";
            case ADDRESS_VERIFICATION_COMPLETED -> "Address verification completed";
            case ACCOUNT_CREATION_IN_PROGRESS -> "Creating bank account";
            case ACCOUNT_CREATED -> "Bank account created successfully";
            case NOTIFICATION_SENT -> "Customer notification sent";
            case COMPLETED -> "Onboarding completed successfully";
            case FAILED -> "Onboarding process failed";
        };
    }

    /**
     * Get success message for process start.
     * 
     * @return Success message for process start
     */
    public String getProcessStartSuccessMessage() {
        return "Onboarding process started successfully";
    }

    /**
     * Initialize the mapping from status to next step.
     */
    private void initializeNextStepMappings() {
        nextStepMappings.put(OnboardingStatus.INITIATED, "collect-info");
        nextStepMappings.put(OnboardingStatus.INFO_COLLECTED, "upload-documents");
        nextStepMappings.put(OnboardingStatus.WAITING_FOR_DOCUMENTS, "upload-documents");
        nextStepMappings.put(OnboardingStatus.DOCUMENTS_UPLOADED, "kyc-verification");
        nextStepMappings.put(OnboardingStatus.KYC_IN_PROGRESS, "kyc-verification");
        nextStepMappings.put(OnboardingStatus.KYC_COMPLETED, "address-verification");
        nextStepMappings.put(OnboardingStatus.ADDRESS_VERIFICATION_IN_PROGRESS, "address-verification");
        nextStepMappings.put(OnboardingStatus.ADDRESS_VERIFICATION_COMPLETED, "account-creation");
        nextStepMappings.put(OnboardingStatus.ACCOUNT_CREATION_IN_PROGRESS, "account-creation");
        nextStepMappings.put(OnboardingStatus.ACCOUNT_CREATED, "notify-customer");
        nextStepMappings.put(OnboardingStatus.NOTIFICATION_SENT, "complete");
        nextStepMappings.put(OnboardingStatus.COMPLETED, null);
        nextStepMappings.put(OnboardingStatus.FAILED, null);
    }

    /**
     * Get default next step description when configuration is not available.
     * 
     * @param currentStatus The current onboarding status
     * @return Default description
     */
    private String getDefaultNextStepDescription(OnboardingStatus currentStatus) {
        return switch (currentStatus) {
            case INITIATED -> "Please provide your personal information";
            case INFO_COLLECTED -> "Please upload your passport and photo documents";
            case WAITING_FOR_DOCUMENTS -> "Please upload your passport and photo documents";
            case DOCUMENTS_UPLOADED -> "Documents processed, verification in progress";
            case KYC_IN_PROGRESS -> "KYC verification in progress";
            case KYC_COMPLETED -> "Address verification in progress";
            case ADDRESS_VERIFICATION_IN_PROGRESS -> "Address verification in progress";
            case ADDRESS_VERIFICATION_COMPLETED -> "Creating your bank account";
            case ACCOUNT_CREATION_IN_PROGRESS -> "Creating your bank account";
            case ACCOUNT_CREATED -> "Sending account details to you";
            case NOTIFICATION_SENT -> "Onboarding process completed";
            case COMPLETED -> "Onboarding completed successfully";
            case FAILED -> "Please contact support for assistance";
        };
    }
}
