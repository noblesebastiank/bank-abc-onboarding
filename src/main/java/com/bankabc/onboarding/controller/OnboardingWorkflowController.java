package com.bankabc.onboarding.controller;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import com.bankabc.onboarding.constants.ApplicationConstants;
import com.bankabc.onboarding.exception.DefaultApiError;
import com.bankabc.onboarding.openapi.api.OnboardingWorkflowApi;
import com.bankabc.onboarding.openapi.model.DocumentUploadResponse;
import com.bankabc.onboarding.openapi.model.OnboardingStartRequest;
import com.bankabc.onboarding.openapi.model.OnboardingStartResponse;
import com.bankabc.onboarding.openapi.model.OnboardingStatusResponse;
import com.bankabc.onboarding.service.BpmnProcessService;
import com.bankabc.onboarding.service.FileStorageService;
import com.bankabc.onboarding.service.FileValidationService;
import com.bankabc.onboarding.service.OnboardingService;
import com.bankabc.onboarding.service.WorkflowConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

/**
 * REST controller for multi-step onboarding workflow operations.
 * Handles BPMN process integration for customer onboarding.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class OnboardingWorkflowController implements OnboardingWorkflowApi {

    private final BpmnProcessService bpmnProcessService;
    private final FileStorageService fileStorageService;
    private final FileValidationService fileValidationService;
    private final OnboardingService onboardingService;
    private final WorkflowConfigurationService workflowConfigurationService;

    /**
     * Start the onboarding process (Step 1: Collect customer information).
     * 
     * @param onboardingStartRequest The customer information request
     * @return Response with process instance ID and status
     */
    @Override
    public ResponseEntity<OnboardingStartResponse> startOnboardingWorkflow(@Valid OnboardingStartRequest onboardingStartRequest) {

        log.info("Starting onboarding process for customer 1");
        
        // Check if customer already exists
        if (onboardingService.existsBySsn(onboardingStartRequest.getSsn())) {
            log.warn("Customer with SSN already exists");
            throw new DefaultApiError(
                    HttpStatus.CONFLICT,
                    ApplicationConstants.ErrorType.CUSTOMER_ALREADY_EXISTS,
                    ApplicationConstants.Messages.CUSTOMER_ALREADY_EXISTS,
                    Map.of("ssn", onboardingStartRequest.getSsn()));
        }
        
        OnboardingStartResponse response = bpmnProcessService.startOnboardingProcess(onboardingStartRequest);
        log.info("Onboarding process started with instance ID: {}", response.getProcessInstanceId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Upload documents (Step 2: Document upload).
     * 
     * @param processInstanceId The BPMN process instance ID
     * @param passport The passport document file
     * @param photo The photo document file
     * @return Response with upload status
     */
    @Override
    public ResponseEntity<DocumentUploadResponse> uploadDocuments(
            String processInstanceId,
            MultipartFile passport,
            MultipartFile photo) {
        
        log.info("Uploading documents for process instance: {}", processInstanceId);
        
        // Validate process instance exists
        if (!bpmnProcessService.isProcessActive(processInstanceId)) {
            log.warn("Process instance not found or not active: {}", processInstanceId);
            throw new DefaultApiError(
                    HttpStatus.NOT_FOUND,
                    ApplicationConstants.ErrorType.ONBOARDING_NOT_FOUND,
                    ApplicationConstants.Messages.PROCESS_NOT_FOUND,
                    Map.of("processInstanceId", processInstanceId));
        }
        
        // Validate files are not empty
        if (passport.isEmpty() || photo.isEmpty()) {
            log.warn("Empty file(s) provided for process instance: {}", processInstanceId);
            throw new DefaultApiError(
                    HttpStatus.BAD_REQUEST,
                    ApplicationConstants.ErrorType.INVALID_REQUEST,
                    "Both passport and photo files are required and cannot be empty",
                    Map.of("processInstanceId", processInstanceId));
        }

        try {
            // VALIDATE FILES BEFORE STORING AND CORRELATING
            log.info("Validating uploaded files for process instance: {}", processInstanceId);
            fileValidationService.validatePassport(passport);
            fileValidationService.validatePhoto(photo);
            log.info("File validation passed for process instance: {}", processInstanceId);
            
            // Store documents
            String passportPath = fileStorageService.storeFile(passport, "passport");
            String photoPath = fileStorageService.storeFile(photo, "photo");
            
            // Create document map for process variables
            Map<String, String> uploadedDocuments = new HashMap<>();
            uploadedDocuments.put("passport", passportPath);
            uploadedDocuments.put("photo", photoPath);
            
            // Correlate message to resume process
            OnboardingStatusResponse statusResponse = bpmnProcessService.correlateDocumentUpload(processInstanceId, uploadedDocuments);
            
            DocumentUploadResponse response = new DocumentUploadResponse()
                    .processInstanceId(processInstanceId)
                    .status(statusResponse.getStatus().getValue())
                    .message(ApplicationConstants.Messages.DOCUMENTS_UPLOADED_SUCCESS)
                    .uploadedAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .nextStep(statusResponse.getNextStep())
                    .nextStepDescription(workflowConfigurationService.getNextStepDescription(
                            com.bankabc.onboarding.entity.Onboarding.OnboardingStatus.valueOf(statusResponse.getStatus().getValue())))
                    .passportUploaded(true)
                    .photoUploaded(true);
            
            log.info("Documents uploaded successfully for process instance: {}", processInstanceId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to store documents for process instance: {}", processInstanceId, e);
            throw new DefaultApiError(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ApplicationConstants.ErrorType.DOCUMENT_UPLOAD_FAILED,
                    "Failed to store documents: " + e.getMessage(),
                    Map.of("processInstanceId", processInstanceId, "originalError", e.getMessage()));
        }
    }

    /**
     * Get onboarding status.
     * 
     * @param processInstanceId The BPMN process instance ID
     * @return Response with current status and progress
     */
    @Override
    public ResponseEntity<OnboardingStatusResponse> getOnboardingStatus(String processInstanceId) {
        log.debug("Getting onboarding status for process instance: {}", processInstanceId);
        
        OnboardingStatusResponse response = bpmnProcessService.getOnboardingStatus(processInstanceId);
        log.debug("Retrieved onboarding status for process instance {}: {}", processInstanceId, response.getStatus());
        return ResponseEntity.ok(response);
    }




}
