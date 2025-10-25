package com.bankabc.onboarding.service;

import com.bankabc.onboarding.exception.DefaultApiError;
import com.bankabc.onboarding.exception.ErrorTypes;
import com.bankabc.onboarding.openapi.model.OnboardingStartRequest;
import com.bankabc.onboarding.openapi.model.OnboardingStartResponse;
import com.bankabc.onboarding.openapi.model.OnboardingStatusResponse;
import com.bankabc.onboarding.entity.Onboarding;
import com.bankabc.onboarding.entity.Onboarding.OnboardingStatus;
import com.bankabc.onboarding.mapper.OnboardingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing BPMN process instances for onboarding workflows.
 * Handles process start, status tracking, and message correlation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BpmnProcessService {

    private final RuntimeService runtimeService;
    private final OnboardingService onboardingService;
    private final OnboardingMapper onboardingMapper;
    private final WorkflowConfigurationService workflowConfigurationService;

    /**
     * Start a new onboarding process instance.
     *
     * @param request The onboarding start request
     * @return The process start response
     */
    @Transactional
    public OnboardingStartResponse startOnboardingProcess(OnboardingStartRequest request) {
        log.info("Starting onboarding process for customer");
        
        // Get process definition key from configuration
        String processDefinitionKey = workflowConfigurationService.getProcessDefinitionKey();
        
        try {
            
            
            // Convert request to entity and save
            Onboarding onboarding = onboardingMapper.toEntity(request);
            onboarding = onboardingService.saveOnboarding(onboarding);
            
            // Create process variables with only essential data
            Map<String, Object> variables = new HashMap<>();
            variables.put("onboardingId", onboarding.getId());
            
            // Start process instance
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    processDefinitionKey, 
                    variables
            );
          
            String processInstanceId = processInstance.getId();
            log.info("Started onboarding process with instance ID: {}", processInstanceId);
            
            // Update onboarding with process instance ID and set initial status
            onboarding.setProcessInstanceId(processInstanceId);
            onboarding.setProcessDefinitionKey(processDefinitionKey);
            onboarding.setStatus(OnboardingStatus.INFO_COLLECTED);
            onboarding = onboardingService.saveOnboarding(onboarding);
            
            return new OnboardingStartResponse()
                    .processInstanceId(processInstanceId)
                    .status(OnboardingStatus.INFO_COLLECTED.name())
                    .message(workflowConfigurationService.getProcessStartSuccessMessage())
                    .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .nextStep(workflowConfigurationService.getNextStepId(onboarding.getStatus()))
                    .nextStepDescription(workflowConfigurationService.getNextStepDescription(onboarding.getStatus()));
                    
        } catch (ProcessEngineException e) {
            log.error("Process engine error starting onboarding process", e);
            throw new DefaultApiError(
                HttpStatus.BAD_REQUEST,
                ErrorTypes.PROCESS_START_FAILED.name(),
                "Failed to start process due to engine error: " + e.getMessage(),
                Map.of(
                    "processDefinitionKey", processDefinitionKey,
                    "customerName", "[REDACTED]",
                    "originalError", e.getMessage()
                )
            );
        } catch (Exception e) {
            log.error("Unexpected error starting onboarding process", e);
            throw new DefaultApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorTypes.PROCESS_START_FAILED.name(),
                ErrorTypes.PROCESS_START_FAILED.getMessage(),
                Map.of(
                    "processDefinitionKey", processDefinitionKey,
                    "customerName", "[REDACTED]",
                    "originalError", e.getMessage()
                )
            );
        }
    }

    /**
     * Correlate document upload message to resume the process.
     *
     * @param processInstanceId The process instance ID
     * @param uploadedDocuments Map of uploaded document paths
     * @return The document upload response
     */
    @Transactional
    public OnboardingStatusResponse correlateDocumentUpload(String processInstanceId, Map<String, String> uploadedDocuments) {
        log.info("Correlating document upload for process instance: {}", processInstanceId);
        
        try {
            // Set document variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("uploadedDocuments", uploadedDocuments);
            
            // Get message name from configuration
            String messageName = workflowConfigurationService.getMessageName("wait-documents");
            if (messageName == null) {
                messageName = "DocumentUploadedMessage"; // fallback
            }
            
            // Correlate message to resume process
            runtimeService.createMessageCorrelation(messageName)
                    .processInstanceId(processInstanceId)
                    .setVariables(variables)
                    .correlate();
            
            log.info("Successfully correlated document upload for process instance: {}", processInstanceId);
            
            // Get updated onboarding status
            return getOnboardingStatus(processInstanceId);
            
        } catch (org.camunda.bpm.engine.delegate.BpmnError e) {
            // Handle upload validation errors specifically
            if ("UPLOAD_VALIDATION_FAILED".equals(e.getErrorCode())) {
                log.warn("Upload validation failed for process instance: {} - {}", processInstanceId, e.getMessage());
                throw new DefaultApiError(
                    HttpStatus.BAD_REQUEST,
                    "FILE_VALIDATION_FAILED",
                    e.getMessage(),
                    Map.of(
                        "processInstanceId", processInstanceId,
                        "errorCode", e.getErrorCode(),
                        "validationError", e.getMessage()
                    )
                );
            }
            
            // Other BPMN errors are handled by boundary events - don't re-throw, just log and continue
            log.info("BpmnError handled by boundary event in correlateDocumentUpload: {} - {}", e.getErrorCode(), e.getMessage());
            // Don't re-throw - let the process continue and return status after error handling
            return getOnboardingStatus(processInstanceId);
        } catch (ProcessEngineException e) {
            log.error("Process engine error correlating document upload for process instance: {}", processInstanceId, e);
            String messageName = workflowConfigurationService.getMessageName("wait-documents");
            if (messageName == null) {
                messageName = "DocumentUploadedMessage"; // fallback
            }
            throw new DefaultApiError(
                HttpStatus.BAD_REQUEST,
                ErrorTypes.DOCUMENT_UPLOAD_FAILED.name(),
                "Failed to correlate document upload due to process engine error: " + e.getMessage(),
                Map.of(
                    "processInstanceId", processInstanceId,
                    "messageName", messageName,
                    "originalError", e.getMessage()
                )
            );
        } catch (Exception e) {
            log.error("Unexpected error correlating document upload for process instance: {} - Exception type: {}", processInstanceId, e.getClass().getName(), e);
            String messageName = workflowConfigurationService.getMessageName("wait-documents");
            if (messageName == null) {
                messageName = "DocumentUploadedMessage"; // fallback
            }
            throw new DefaultApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorTypes.DOCUMENT_UPLOAD_FAILED.name(),
                ErrorTypes.DOCUMENT_UPLOAD_FAILED.getMessage(),
                Map.of(
                    "processInstanceId", processInstanceId,
                    "messageName", messageName,
                    "originalError", e.getMessage()
                )
            );
        }
    }

    /**
     * Get the current status of an onboarding process.
     *
     * @param processInstanceId The process instance ID
     * @return The onboarding status response
     */
    public OnboardingStatusResponse getOnboardingStatus(String processInstanceId) {
        log.debug("Getting onboarding status for process instance: {}", processInstanceId);
        
        try {
            // Find onboarding by process instance ID
            Onboarding onboarding = onboardingService.findByProcessInstanceIdOrThrow(processInstanceId);
            
            
            // Determine current and next steps
            String currentStep = workflowConfigurationService.getCurrentStepDescription(onboarding.getStatus());
            String nextStep = workflowConfigurationService.getNextStepId(onboarding.getStatus());
            
            return new OnboardingStatusResponse()
                    .processInstanceId(processInstanceId)
                    .status(OnboardingStatusResponse.StatusEnum.fromValue(onboarding.getStatus().name()))
                    .message(workflowConfigurationService.getStatusMessage(onboarding.getStatus()))
                    .accountNumber(onboarding.getAccountNumber())
                    .createdAt(onboarding.getCreatedAt())
                    .updatedAt(onboarding.getUpdatedAt())
                    .completedAt(onboarding.getCompletedAt())
                    .kycVerified(onboarding.getKycVerified())
                    .addressVerified(onboarding.getAddressVerified())
                    .currentStep(currentStep)
                    .nextStep(nextStep);
        } catch (DefaultApiError e) {
            // Re-throw DefaultApiError as-is
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting onboarding status for process instance: {}", processInstanceId, e);
            throw new DefaultApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorTypes.INTERNAL_SERVER_ERROR.name(),
                ErrorTypes.INTERNAL_SERVER_ERROR.getMessage(),
                Map.of(
                    "processInstanceId", processInstanceId,
                    "originalError", e.getMessage()
                )
            );
        }
    }

    /**
     * Check if a process instance is active.
     *
     * @param processInstanceId The process instance ID
     * @return true if the process is active
     */
    public boolean isProcessActive(String processInstanceId) {
        try {
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            return processInstance != null;
        } catch (Exception e) {
            log.error("Error checking if process is active: {}", processInstanceId, e);
            return false;
        }
    }



    
}
