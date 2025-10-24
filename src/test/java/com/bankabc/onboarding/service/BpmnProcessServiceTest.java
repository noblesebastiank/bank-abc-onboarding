package com.bankabc.onboarding.service;

import com.bankabc.onboarding.exception.DefaultApiError;
import com.bankabc.onboarding.exception.ErrorTypes;
import com.bankabc.onboarding.openapi.model.OnboardingStartRequest;
import com.bankabc.onboarding.openapi.model.OnboardingStartResponse;
import com.bankabc.onboarding.openapi.model.OnboardingStatusResponse;
import com.bankabc.onboarding.entity.Onboarding;
import com.bankabc.onboarding.entity.Onboarding.OnboardingStatus;
import com.bankabc.onboarding.mapper.OnboardingMapper;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("unchecked")
class BpmnProcessServiceTest {

    @Mock
    private RuntimeService runtimeService;
    @Mock
    private RepositoryService repositoryService;
    @Mock
    private ProcessDefinitionQuery processDefinitionQuery;
    @Mock
    private OnboardingService onboardingService;
    @Mock
    private ProcessInstanceQuery processInstanceQuery;
    @Mock
    private MessageCorrelationBuilder messageCorrelationBuilder;
    @Mock
    private OnboardingMapper onboardingMapper;
    @Mock
    private WorkflowConfigurationService workflowConfigurationService;

    @InjectMocks
    private BpmnProcessService bpmnProcessService;

    private OnboardingStartRequest validRequest;
    private Onboarding onboardingEntity;
    private ProcessInstance processInstance;

    @BeforeEach
    void setUp() {
        
        validRequest = new OnboardingStartRequest()
                .firstName("Emma")
                .lastName("de Vries")
                .gender(OnboardingStartRequest.GenderEnum.F)
                .dob(LocalDate.of(1990, 5, 20))
                .phone("+31612345678")
                .email("emma.devries@example.com")
                .nationality("Dutch")
                .street("Keizersgracht 1")
                .city("Amsterdam")
                .postalCode("1015CD")
                .country("Netherlands")
                .ssn("123-45-6789");

        onboardingEntity = Onboarding.builder()
                .id(java.util.UUID.randomUUID())
                .firstName("Emma")
                .lastName("de Vries")
                .gender(Onboarding.Gender.F)
                .dateOfBirth(LocalDate.of(1990, 5, 20))
                .phone("+31612345678")
                .email("emma.devries@example.com")
                .nationality("Dutch")
                .street("Keizersgracht 1")
                .city("Amsterdam")
                .postalCode("1015CD")
                .country("Netherlands")
                .ssn("123-45-6789")
                .status(OnboardingStatus.KYC_IN_PROGRESS)
                .processInstanceId("12345")
                .kycVerified(false)
                .addressVerified(false)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        processInstance = mock(ProcessInstance.class);
        when(processInstance.getId()).thenReturn("12345");
        
        // Mock RepositoryService
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionKey(anyString())).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.latestVersion()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.count()).thenReturn(1L);
        
        // Mock WorkflowConfigurationService
        when(workflowConfigurationService.getProcessDefinitionKey()).thenReturn("onboarding-process");
        when(workflowConfigurationService.getNextStepId(any(OnboardingStatus.class))).thenReturn("upload-documents");
        when(workflowConfigurationService.getNextStepDescription(any(OnboardingStatus.class))).thenReturn("Please upload your documents");
        when(workflowConfigurationService.getStatusMessage(any(OnboardingStatus.class))).thenReturn("Status message");
        when(workflowConfigurationService.getCurrentStepDescription(any(OnboardingStatus.class))).thenReturn("Current step description");
        when(workflowConfigurationService.getProcessStartSuccessMessage()).thenReturn("Process started successfully");
        when(workflowConfigurationService.getMessageName("wait-documents")).thenReturn("DocumentUploadedMessage");
        
        // Mock mapper behavior
        when(onboardingMapper.toEntity(any(OnboardingStartRequest.class))).thenReturn(onboardingEntity);
        when(onboardingMapper.toStatusDto(any(Onboarding.class))).thenReturn(new OnboardingStatusResponse()
                .processInstanceId("12345")
                .status(OnboardingStatusResponse.StatusEnum.KYC_IN_PROGRESS)
                .message("KYC verification in progress")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .kycVerified(false)
                .addressVerified(false)
                .currentStep("KYC verification in progress")
                .nextStep("kyc_verification")
                );
        
        // Mock WorkflowConfigurationService
        when(workflowConfigurationService.getNextStepDescription(any(OnboardingStatus.class)))
                .thenReturn("Dynamic next step description");
        when(workflowConfigurationService.getCurrentStepDescription(any(OnboardingStatus.class)))
                .thenReturn("Dynamic current step description");
        when(workflowConfigurationService.getNextStepId(any(OnboardingStatus.class)))
                .thenReturn("upload-documents");
    }

    @Test
    void startOnboardingProcess_ValidRequest_ReturnsResponse() {
        // Mock service save operations - return the onboarding with ID set
        Onboarding savedOnboarding = Onboarding.builder()
                .id(java.util.UUID.randomUUID())
                .firstName("Emma")
                .lastName("de Vries")
                .gender(Onboarding.Gender.F)
                .dateOfBirth(LocalDate.of(1990, 5, 20))
                .phone("+31612345678")
                .email("emma.devries@example.com")
                .nationality("Dutch")
                .street("Keizersgracht 1")
                .city("Amsterdam")
                .postalCode("1015CD")
                .country("Netherlands")
                .ssn("123-45-6789")
                .status(OnboardingStatus.INITIATED)
                .kycVerified(false)
                .addressVerified(false)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        when(onboardingService.saveOnboarding(any(Onboarding.class))).thenReturn(savedOnboarding);
        
        // Mock repository service to return valid process definition
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionKey(anyString())).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.latestVersion()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.count()).thenReturn(1L);
        
        // Mock runtime service
        when(runtimeService.startProcessInstanceByKey(anyString(), any(Map.class)))
                .thenReturn(processInstance);

        OnboardingStartResponse response = bpmnProcessService.startOnboardingProcess(validRequest);

        assertNotNull(response);
        assertEquals("12345", response.getProcessInstanceId());
        assertEquals("INFO_COLLECTED", response.getStatus());
        assertNotNull(response.getMessage());
        assertNotNull(response.getCreatedAt());
        assertEquals("upload-documents", response.getNextStep());
        assertEquals("Dynamic next step description", response.getNextStepDescription());

        // Verify service was called twice (initial save and update with process instance ID)
        verify(onboardingService, times(2)).saveOnboarding(any(Onboarding.class));
        
        // Verify process was started with onboardingId variable
        verify(runtimeService).startProcessInstanceByKey(eq("onboarding-process"), any(Map.class));
        
        // Verify WorkflowConfigurationService was called for next step description with the actual status
        verify(workflowConfigurationService).getNextStepDescription(any(OnboardingStatus.class));
    }

    @Test
    void startOnboardingProcess_ProcessEngineException_ThrowsBadRequestError() {
        // Mock service save operations
        Onboarding savedOnboarding = Onboarding.builder()
                .id(java.util.UUID.randomUUID())
                .firstName("Emma")
                .lastName("de Vries")
                .gender(Onboarding.Gender.F)
                .dateOfBirth(LocalDate.of(1990, 5, 20))
                .phone("+31612345678")
                .email("emma.devries@example.com")
                .nationality("Dutch")
                .street("Keizersgracht 1")
                .city("Amsterdam")
                .postalCode("1015CD")
                .country("Netherlands")
                .ssn("123-45-6789")
                .status(OnboardingStatus.INITIATED)
                .kycVerified(false)
                .addressVerified(false)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        when(onboardingService.saveOnboarding(any(Onboarding.class))).thenReturn(savedOnboarding);
        
        // Mock repository service to return valid process definition
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionKey(anyString())).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.latestVersion()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.count()).thenReturn(1L);
        
        // Mock runtime service to throw ProcessEngineException
        when(runtimeService.startProcessInstanceByKey(anyString(), any(Map.class)))
                .thenThrow(new org.camunda.bpm.engine.ProcessEngineException("Process definition not found"));

        DefaultApiError exception = assertThrows(DefaultApiError.class, () -> {
            bpmnProcessService.startOnboardingProcess(validRequest);
        });
        
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, exception.getHttpStatus());
        assertEquals(ErrorTypes.PROCESS_START_FAILED.name(), exception.getErrorName());
        assertTrue(exception.getMessage().contains("Failed to start process due to engine error"));
        assertNotNull(exception.getAdditionalDetails());
        assertTrue(exception.getAdditionalDetails().containsKey("processDefinitionKey"));
        assertTrue(exception.getAdditionalDetails().containsKey("customerName"));
        assertTrue(exception.getAdditionalDetails().containsKey("originalError"));
    }

    @Test
    void startOnboardingProcess_UnexpectedException_ThrowsInternalServerError() {
        // Mock service save operations
        Onboarding savedOnboarding = Onboarding.builder()
                .id(java.util.UUID.randomUUID())
                .firstName("Emma")
                .lastName("de Vries")
                .gender(Onboarding.Gender.F)
                .dateOfBirth(LocalDate.of(1990, 5, 20))
                .phone("+31612345678")
                .email("emma.devries@example.com")
                .nationality("Dutch")
                .street("Keizersgracht 1")
                .city("Amsterdam")
                .postalCode("1015CD")
                .country("Netherlands")
                .ssn("123-45-6789")
                .status(OnboardingStatus.INITIATED)
                .kycVerified(false)
                .addressVerified(false)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        when(onboardingService.saveOnboarding(any(Onboarding.class))).thenReturn(savedOnboarding);
        
        // Mock repository service to return valid process definition
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionKey(anyString())).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.latestVersion()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.count()).thenReturn(1L);
        
        // Mock runtime service to throw unexpected exception
        when(runtimeService.startProcessInstanceByKey(anyString(), any(Map.class)))
                .thenThrow(new RuntimeException("Unexpected system error"));

        DefaultApiError exception = assertThrows(DefaultApiError.class, () -> {
            bpmnProcessService.startOnboardingProcess(validRequest);
        });
        
        assertEquals(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
        assertEquals(ErrorTypes.PROCESS_START_FAILED.name(), exception.getErrorName());
        assertEquals(ErrorTypes.PROCESS_START_FAILED.getMessage(), exception.getMessage());
        assertNotNull(exception.getAdditionalDetails());
        assertTrue(exception.getAdditionalDetails().containsKey("processDefinitionKey"));
        assertTrue(exception.getAdditionalDetails().containsKey("customerName"));
        assertTrue(exception.getAdditionalDetails().containsKey("originalError"));
    }

    @Test
    void correlateDocumentUpload_ValidProcess_ReturnsStatusResponse() {
        Map<String, String> uploadedDocuments = new HashMap<>();
        uploadedDocuments.put("passport", "/path/to/passport.pdf");
        uploadedDocuments.put("photo", "/path/to/photo.jpg");

        when(runtimeService.createMessageCorrelation("DocumentUploadedMessage"))
                .thenReturn(messageCorrelationBuilder);
        when(messageCorrelationBuilder.processInstanceId("12345"))
                .thenReturn(messageCorrelationBuilder);
        when(messageCorrelationBuilder.setVariables(any(Map.class)))
                .thenReturn(messageCorrelationBuilder);
        doNothing().when(messageCorrelationBuilder).correlate();
        when(onboardingService.findByProcessInstanceId("12345"))
                .thenReturn(Optional.of(onboardingEntity));
        when(onboardingService.findByProcessInstanceIdOrThrow("12345"))
                .thenReturn(onboardingEntity);

        OnboardingStatusResponse response = bpmnProcessService.correlateDocumentUpload("12345", uploadedDocuments);

        assertNotNull(response);
        assertEquals("12345", response.getProcessInstanceId());
        assertEquals(OnboardingStatusResponse.StatusEnum.KYC_IN_PROGRESS, response.getStatus());

        verify(runtimeService).createMessageCorrelation("DocumentUploadedMessage");
    }

    @Test
    void correlateDocumentUpload_ProcessEngineException_ThrowsBadRequestError() {
        Map<String, String> uploadedDocuments = new HashMap<>();
        uploadedDocuments.put("passport", "/path/to/passport.pdf");
        uploadedDocuments.put("photo", "/path/to/photo.jpg");

        when(runtimeService.createMessageCorrelation("DocumentUploadedMessage"))
                .thenThrow(new org.camunda.bpm.engine.ProcessEngineException("Process instance not found"));

        DefaultApiError exception = assertThrows(DefaultApiError.class, () -> {
            bpmnProcessService.correlateDocumentUpload("12345", uploadedDocuments);
        });
        
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, exception.getHttpStatus());
        assertEquals(ErrorTypes.DOCUMENT_UPLOAD_FAILED.name(), exception.getErrorName());
        assertTrue(exception.getMessage().contains("Failed to correlate document upload due to process engine error"));
        assertNotNull(exception.getAdditionalDetails());
        assertTrue(exception.getAdditionalDetails().containsKey("processInstanceId"));
        assertTrue(exception.getAdditionalDetails().containsKey("messageName"));
        assertTrue(exception.getAdditionalDetails().containsKey("originalError"));
        assertEquals("12345", exception.getAdditionalDetails().get("processInstanceId"));
        assertEquals("DocumentUploadedMessage", exception.getAdditionalDetails().get("messageName"));
    }

    @Test
    void correlateDocumentUpload_UnexpectedException_ThrowsInternalServerError() {
        Map<String, String> uploadedDocuments = new HashMap<>();
        uploadedDocuments.put("passport", "/path/to/passport.pdf");
        uploadedDocuments.put("photo", "/path/to/photo.jpg");

        when(runtimeService.createMessageCorrelation("DocumentUploadedMessage"))
                .thenThrow(new RuntimeException("Unexpected system error"));

        DefaultApiError exception = assertThrows(DefaultApiError.class, () -> {
            bpmnProcessService.correlateDocumentUpload("12345", uploadedDocuments);
        });
        
        assertEquals(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
        assertEquals(ErrorTypes.DOCUMENT_UPLOAD_FAILED.name(), exception.getErrorName());
        assertEquals(ErrorTypes.DOCUMENT_UPLOAD_FAILED.getMessage(), exception.getMessage());
        assertNotNull(exception.getAdditionalDetails());
        assertTrue(exception.getAdditionalDetails().containsKey("processInstanceId"));
        assertTrue(exception.getAdditionalDetails().containsKey("messageName"));
        assertTrue(exception.getAdditionalDetails().containsKey("originalError"));
        assertEquals("12345", exception.getAdditionalDetails().get("processInstanceId"));
        assertEquals("DocumentUploadedMessage", exception.getAdditionalDetails().get("messageName"));
    }

    @Test
    void getOnboardingStatus_ValidProcessId_ReturnsStatusResponse() {
        when(onboardingService.findByProcessInstanceId("12345"))
                .thenReturn(Optional.of(onboardingEntity));
        when(onboardingService.findByProcessInstanceIdOrThrow("12345"))
                .thenReturn(onboardingEntity);

        OnboardingStatusResponse response = bpmnProcessService.getOnboardingStatus("12345");

        assertNotNull(response);
        assertEquals("12345", response.getProcessInstanceId());
        assertEquals(OnboardingStatusResponse.StatusEnum.KYC_IN_PROGRESS, response.getStatus());
        assertEquals("Dynamic current step description", response.getCurrentStep());
        assertEquals("upload-documents", response.getNextStep());
        
        // Verify WorkflowConfigurationService was called for dynamic descriptions
        verify(workflowConfigurationService).getCurrentStepDescription(OnboardingStatus.KYC_IN_PROGRESS);
        verify(workflowConfigurationService).getNextStepId(OnboardingStatus.KYC_IN_PROGRESS);
    }

    @Test
    void getOnboardingStatus_ProcessNotFound_ThrowsDefaultApiError() {
        when(onboardingService.findByProcessInstanceId("12345"))
                .thenReturn(Optional.empty());
        when(onboardingService.findByProcessInstanceIdOrThrow("12345"))
                .thenThrow(new DefaultApiError(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    ErrorTypes.ONBOARDING_NOT_FOUND.name(),
                    ErrorTypes.ONBOARDING_NOT_FOUND.getMessage(),
                    Map.of("processInstanceId", "12345")
                ));

        DefaultApiError exception = assertThrows(DefaultApiError.class, () -> {
            bpmnProcessService.getOnboardingStatus("12345");
        });
        
        assertEquals(ErrorTypes.ONBOARDING_NOT_FOUND.name(), exception.getErrorName());
        assertEquals(ErrorTypes.ONBOARDING_NOT_FOUND.getMessage(), exception.getMessage());
        assertNotNull(exception.getAdditionalDetails());
        assertTrue(exception.getAdditionalDetails().containsKey("processInstanceId"));
        assertEquals("12345", exception.getAdditionalDetails().get("processInstanceId"));
    }

    @Test
    void isProcessActive_ActiveProcess_ReturnsTrue() {
        when(runtimeService.createProcessInstanceQuery())
                .thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceId("12345"))
                .thenReturn(processInstanceQuery);
        when(processInstanceQuery.singleResult())
                .thenReturn(processInstance);

        boolean isActive = bpmnProcessService.isProcessActive("12345");

        assertTrue(isActive);
    }

    @Test
    void isProcessActive_InactiveProcess_ReturnsFalse() {
        when(runtimeService.createProcessInstanceQuery())
                .thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceId("12345"))
                .thenReturn(processInstanceQuery);
        when(processInstanceQuery.singleResult())
                .thenReturn(null);

        boolean isActive = bpmnProcessService.isProcessActive("12345");

        assertFalse(isActive);
    }

    @Test
    void isProcessActive_Exception_ReturnsFalse() {
        when(runtimeService.createProcessInstanceQuery())
                .thenThrow(new RuntimeException("Query failed"));

        boolean isActive = bpmnProcessService.isProcessActive("12345");

        assertFalse(isActive);
    }
}