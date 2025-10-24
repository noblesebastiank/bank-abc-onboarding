package com.bankabc.onboarding.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

import com.bankabc.onboarding.exception.DefaultApiError;
import com.bankabc.onboarding.exception.ErrorTypes;
import com.bankabc.onboarding.exception.GlobalExceptionHandler;
import com.bankabc.onboarding.openapi.model.OnboardingStartRequest;
import com.bankabc.onboarding.openapi.model.OnboardingStartResponse;
import com.bankabc.onboarding.openapi.model.OnboardingStatusResponse;
import com.bankabc.onboarding.service.BpmnProcessService;
import com.bankabc.onboarding.service.FileStorageService;
import com.bankabc.onboarding.service.FileValidationService;
import com.bankabc.onboarding.service.OnboardingService;
import com.bankabc.onboarding.service.WorkflowConfigurationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class OnboardingWorkflowControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BpmnProcessService bpmnProcessService;

    @Mock
    private OnboardingService onboardingService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private WorkflowConfigurationService workflowConfigurationService;

    @Mock
    private FileValidationService fileValidationService;

    @InjectMocks
    private OnboardingWorkflowController controller;

    private OnboardingStartRequest validRequest;
    private OnboardingStartResponse startResponse;
    private OnboardingStatusResponse statusResponse;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        
        // Configure ObjectMapper for Java 8 time support
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // File validation is now handled by DMN decision table

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

        startResponse = new OnboardingStartResponse()
                .processInstanceId("12345")
                .status("INFO_COLLECTED")
                .message("Onboarding process started successfully")
                .createdAt(OffsetDateTime.now())
                .nextStep("document_upload")
                .nextStepDescription("Please upload your passport and photo documents");

        statusResponse = new OnboardingStatusResponse()
                .processInstanceId("12345")
                .status(OnboardingStatusResponse.StatusEnum.KYC_IN_PROGRESS)
                .message("KYC verification in progress")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .kycVerified(false)
                .addressVerified(false)
                .currentStep("KYC verification in progress")
                .nextStep("kyc_verification");
    }

    @Test
    void startOnboarding_ValidRequest_ReturnsAccepted() throws Exception {
        when(onboardingService.existsBySsn(anyString())).thenReturn(false);
        when(bpmnProcessService.startOnboardingProcess(any(OnboardingStartRequest.class)))
                .thenReturn(startResponse);

        mockMvc.perform(post("/api/v1/onboarding/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.processInstanceId").value("12345"))
                .andExpect(jsonPath("$.status").value("INFO_COLLECTED"))
                .andExpect(jsonPath("$.nextStep").value("document_upload"));
    }

    @Test
    void startOnboarding_CustomerExists_ReturnsConflict() throws Exception {
        when(onboardingService.existsBySsn(anyString())).thenReturn(true);

        mockMvc.perform(post("/api/v1/onboarding/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorName").value("CUSTOMER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Customer with SSN already exists"));
    }

    @Test
    void uploadDocuments_ValidFiles_ReturnsOk() throws Exception {
        MockMultipartFile passport = new MockMultipartFile(
                "passport", "passport.pdf", "application/pdf", "passport content".getBytes());
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "photo.jpg", "image/jpeg", "photo content".getBytes());

        when(bpmnProcessService.isProcessActive(anyString())).thenReturn(true);
        doNothing().when(fileValidationService).validatePassport(any());
        doNothing().when(fileValidationService).validatePhoto(any());
        when(fileStorageService.storeFile(any(), anyString())).thenReturn("/path/to/file");
        when(bpmnProcessService.correlateDocumentUpload(anyString(), any()))
                .thenReturn(statusResponse);
        when(workflowConfigurationService.getNextStepDescription(any()))
                .thenReturn("Dynamic next step description");

        mockMvc.perform(multipart("/api/v1/onboarding/12345/documents")
                        .file(passport)
                        .file(photo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processInstanceId").value("12345"))
                .andExpect(jsonPath("$.passportUploaded").value(true))
                .andExpect(jsonPath("$.photoUploaded").value(true));
    }

    @Test
    void uploadDocuments_ProcessNotActive_ReturnsNotFound() throws Exception {
        MockMultipartFile passport = new MockMultipartFile(
                "passport", "passport.pdf", "application/pdf", "passport content".getBytes());
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "photo.jpg", "image/jpeg", "photo content".getBytes());

        when(bpmnProcessService.isProcessActive(anyString())).thenReturn(false);

        mockMvc.perform(multipart("/api/v1/onboarding/12345/documents")
                        .file(passport)
                        .file(photo))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorName").value("ONBOARDING_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Process instance not found or not active"));
    }

    @Test
    void getOnboardingStatus_ValidProcessId_ReturnsOk() throws Exception {
        when(bpmnProcessService.getOnboardingStatus(anyString())).thenReturn(statusResponse);

        mockMvc.perform(get("/api/v1/onboarding/12345/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processInstanceId").value("12345"))
                .andExpect(jsonPath("$.status").value("KYC_IN_PROGRESS"))
                .andExpect(jsonPath("$.currentStep").value("KYC verification in progress"));
    }

    // Health check test removed - Actuator endpoints not available in unit test context
    // Use integration tests for Actuator endpoint testing

    @Test
    void startOnboarding_InvalidRequest_ReturnsBadRequest() throws Exception {
        OnboardingStartRequest invalidRequest = new OnboardingStartRequest()
                .firstName("") // Invalid: empty first name
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

        mockMvc.perform(post("/api/v1/onboarding/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startOnboarding_ProcessEngineError_ReturnsBadRequest() throws Exception {
        when(onboardingService.existsBySsn(anyString())).thenReturn(false);
        when(bpmnProcessService.startOnboardingProcess(any(OnboardingStartRequest.class)))
                .thenThrow(new DefaultApiError(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    ErrorTypes.PROCESS_START_FAILED.name(),
                    "Failed to start process due to engine error: Process definition not found",
                    Map.of("processDefinitionKey", "onboarding-process", "originalError", "Process definition not found")
                ));

        mockMvc.perform(post("/api/v1/onboarding/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorName").value(ErrorTypes.PROCESS_START_FAILED.name()))
                .andExpect(jsonPath("$.message").value("Failed to start process due to engine error: Process definition not found"))
                .andExpect(jsonPath("$.additionalDetails.processDefinitionKey").value("onboarding-process"))
                .andExpect(jsonPath("$.additionalDetails.originalError").value("Process definition not found"));
    }

    @Test
    void startOnboarding_ProcessStartFails_ReturnsInternalServerError() throws Exception {
        when(onboardingService.existsBySsn(anyString())).thenReturn(false);
        when(bpmnProcessService.startOnboardingProcess(any(OnboardingStartRequest.class)))
                .thenThrow(new DefaultApiError(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorTypes.PROCESS_START_FAILED.name(),
                    ErrorTypes.PROCESS_START_FAILED.getMessage(),
                    Map.of("processDefinitionKey", "onboarding-process", "originalError", "Unexpected system error")
                ));

        mockMvc.perform(post("/api/v1/onboarding/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorName").value(ErrorTypes.PROCESS_START_FAILED.name()))
                .andExpect(jsonPath("$.message").value(ErrorTypes.PROCESS_START_FAILED.getMessage()))
                .andExpect(jsonPath("$.additionalDetails.processDefinitionKey").value("onboarding-process"))
                .andExpect(jsonPath("$.additionalDetails.originalError").value("Unexpected system error"));
    }

    @Test
    void getOnboardingStatus_ProcessNotFound_ReturnsNotFound() throws Exception {
        when(bpmnProcessService.getOnboardingStatus(anyString()))
                .thenThrow(new DefaultApiError(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    ErrorTypes.ONBOARDING_NOT_FOUND.name(),
                    ErrorTypes.ONBOARDING_NOT_FOUND.getMessage(),
                    Map.of("processInstanceId", "12345")
                ));

        mockMvc.perform(get("/api/v1/onboarding/12345/status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorName").value(ErrorTypes.ONBOARDING_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").value(ErrorTypes.ONBOARDING_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.additionalDetails.processInstanceId").value("12345"));
    }

    // File validation tests removed - validation is now handled by DMN decision table
}
