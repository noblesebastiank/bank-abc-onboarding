package com.bankabc.onboarding.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import com.bankabc.onboarding.openapi.model.DocumentUploadResponse;
import com.bankabc.onboarding.openapi.model.OnboardingStartRequest;
import com.bankabc.onboarding.openapi.model.OnboardingStartResponse;
import com.bankabc.onboarding.openapi.model.OnboardingStatusResponse;
import com.bankabc.onboarding.service.BpmnProcessService;
import com.bankabc.onboarding.service.FileStorageService;
import com.bankabc.onboarding.service.FileValidationService;
import com.bankabc.onboarding.service.OnboardingService;
import com.bankabc.onboarding.service.WorkflowConfigurationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;


/**
 * REST endpoint integration tests for OnboardingWorkflowController.
 * Tests the actual HTTP endpoints using MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OnboardingWorkflowControllerRestIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BpmnProcessService bpmnProcessService;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private FileValidationService fileValidationService;

    @MockBean
    private OnboardingService onboardingService;

    @MockBean
    private WorkflowConfigurationService workflowConfigurationService;

    private OnboardingStartRequest validRequest;
    private OnboardingStartResponse startResponse;
    private OnboardingStatusResponse statusResponse;
    private DocumentUploadResponse uploadResponse;

    @BeforeEach
    void setUp() {
        // Generate unique SSN for each test
        UUID uuid = UUID.randomUUID();
        int hash = Math.abs(uuid.hashCode());
        String uniqueSsn = String.format("%03d-%02d-%04d",
            hash % 1000,
            (hash / 1000) % 100,
            (hash / 100000) % 10000);

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
                .ssn(uniqueSsn);

        startResponse = new OnboardingStartResponse()
                .processInstanceId("test-process-123")
                .status("INFO_COLLECTED")
                .message("Onboarding process started successfully")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .nextStep("document_upload")
                .nextStepDescription("Please upload your passport and photo documents");

        statusResponse = new OnboardingStatusResponse()
                .processInstanceId("test-process-123")
                .status(OnboardingStatusResponse.StatusEnum.INFO_COLLECTED)
                .message("Information collected successfully")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .currentStep("Information collection completed")
                .nextStep("document_upload");

        uploadResponse = new DocumentUploadResponse()
                .processInstanceId("test-process-123")
                .status("DOCUMENTS_UPLOADED")
                .message("Documents uploaded successfully")
                .uploadedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .nextStep("kyc_verification")
                .nextStepDescription("Documents processed, verification in progress")
                .passportUploaded(true)
                .photoUploaded(true);
    }

    @Test
    void startOnboardingWorkflow_ValidRequest_ReturnsAccepted() throws Exception {
        // Given
        when(bpmnProcessService.startOnboardingProcess(any(OnboardingStartRequest.class)))
                .thenReturn(startResponse);
        when(onboardingService.existsBySsn(anyString())).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/v1/onboarding/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.processInstanceId").value("test-process-123"))
                .andExpect(jsonPath("$.status").value("INFO_COLLECTED"))
                .andExpect(jsonPath("$.message").value("Onboarding process started successfully"))
                .andExpect(jsonPath("$.nextStep").value("document_upload"))
                .andExpect(jsonPath("$.nextStepDescription").value("Please upload your passport and photo documents"));

        verify(bpmnProcessService).startOnboardingProcess(any(OnboardingStartRequest.class));
    }

    @Test
    void startOnboardingWorkflow_DuplicateSsn_ReturnsConflict() throws Exception {
        // Given
        when(onboardingService.existsBySsn(anyString())).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/v1/onboarding/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorName").value("CUSTOMER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Customer with SSN already exists"))
                .andExpect(jsonPath("$.additionalDetails.ssn").value(validRequest.getSsn()));

        verify(bpmnProcessService, never()).startOnboardingProcess(any(OnboardingStartRequest.class));
    }

    @Test
    void startOnboardingWorkflow_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        OnboardingStartRequest invalidRequest = new OnboardingStartRequest()
                .firstName("") // Empty first name
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

        // When & Then
        mockMvc.perform(post("/api/v1/onboarding/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(bpmnProcessService, never()).startOnboardingProcess(any(OnboardingStartRequest.class));
    }

    @Test
    void uploadDocuments_ValidFiles_ReturnsOk() throws Exception {
        // Given
        String processInstanceId = "test-process-123";
        MockMultipartFile passport = new MockMultipartFile(
                "passport", "passport.pdf", "application/pdf", "passport content".getBytes());
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "photo.jpg", "image/jpeg", "photo content".getBytes());

        when(bpmnProcessService.isProcessActive(processInstanceId)).thenReturn(true);
        doNothing().when(fileValidationService).validatePassport(any());
        doNothing().when(fileValidationService).validatePhoto(any());
        when(fileStorageService.storeFile(any(), eq("passport"))).thenReturn("/path/to/passport.pdf");
        when(fileStorageService.storeFile(any(), eq("photo"))).thenReturn("/path/to/photo.jpg");
        when(bpmnProcessService.correlateDocumentUpload(eq(processInstanceId), any(Map.class)))
                .thenReturn(statusResponse);
        when(workflowConfigurationService.getNextStepDescription(any()))
                .thenReturn("Documents processed, verification in progress");

        // When & Then
        mockMvc.perform(multipart("/api/v1/onboarding/{processInstanceId}/documents", processInstanceId)
                .file(passport)
                .file(photo))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.processInstanceId").value(processInstanceId))
                .andExpect(jsonPath("$.status").value("INFO_COLLECTED"))
                .andExpect(jsonPath("$.message").value("Documents uploaded successfully"))
                .andExpect(jsonPath("$.passportUploaded").value(true))
                .andExpect(jsonPath("$.photoUploaded").value(true))
                .andExpect(jsonPath("$.nextStep").value("document_upload"));

        verify(fileValidationService).validatePassport(any());
        verify(fileValidationService).validatePhoto(any());
        verify(fileStorageService).storeFile(any(), eq("passport"));
        verify(fileStorageService).storeFile(any(), eq("photo"));
        verify(bpmnProcessService).correlateDocumentUpload(eq(processInstanceId), any(Map.class));
    }

    @Test
    void uploadDocuments_ProcessNotFound_ReturnsNotFound() throws Exception {
        // Given
        String processInstanceId = "non-existent-process";
        MockMultipartFile passport = new MockMultipartFile(
                "passport", "passport.pdf", "application/pdf", "passport content".getBytes());
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "photo.jpg", "image/jpeg", "photo content".getBytes());

        when(bpmnProcessService.isProcessActive(processInstanceId)).thenReturn(false);

        // When & Then
        mockMvc.perform(multipart("/api/v1/onboarding/{processInstanceId}/documents", processInstanceId)
                .file(passport)
                .file(photo))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorName").value("ONBOARDING_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Process instance not found or not active"))
                .andExpect(jsonPath("$.additionalDetails.processInstanceId").value(processInstanceId));

        verify(fileValidationService, never()).validatePassport(any());
        verify(fileValidationService, never()).validatePhoto(any());
        verify(fileStorageService, never()).storeFile(any(), anyString());
    }

    @Test
    void uploadDocuments_EmptyFiles_ReturnsBadRequest() throws Exception {
        // Given
        String processInstanceId = "test-process-123";
        MockMultipartFile emptyPassport = new MockMultipartFile(
                "passport", "passport.pdf", "application/pdf", new byte[0]);
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "photo.jpg", "image/jpeg", "photo content".getBytes());

        when(bpmnProcessService.isProcessActive(processInstanceId)).thenReturn(true);

        // When & Then
        mockMvc.perform(multipart("/api/v1/onboarding/{processInstanceId}/documents", processInstanceId)
                .file(emptyPassport)
                .file(photo))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorName").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Both passport and photo files are required and cannot be empty"));

        verify(fileValidationService, never()).validatePassport(any());
        verify(fileValidationService, never()).validatePhoto(any());
        verify(fileStorageService, never()).storeFile(any(), anyString());
    }

    @Test
    void uploadDocuments_FileValidationFails_ReturnsInternalServerError() throws Exception {
        // Given
        String processInstanceId = "test-process-123";
        MockMultipartFile passport = new MockMultipartFile(
                "passport", "passport.pdf", "application/pdf", "passport content".getBytes());
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "photo.jpg", "image/jpeg", "photo content".getBytes());

        when(bpmnProcessService.isProcessActive(processInstanceId)).thenReturn(true);
        doThrow(new RuntimeException("Invalid passport format"))
                .when(fileValidationService).validatePassport(any());

        // When & Then
        mockMvc.perform(multipart("/api/v1/onboarding/{processInstanceId}/documents", processInstanceId)
                .file(passport)
                .file(photo))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorName").value("DOCUMENT_UPLOAD_FAILED"))
                .andExpect(jsonPath("$.message").value("Failed to store documents: Invalid passport format"));

        verify(fileValidationService).validatePassport(any());
        verify(fileStorageService, never()).storeFile(any(), anyString());
    }

    @Test
    void getOnboardingStatus_ValidProcessId_ReturnsOk() throws Exception {
        // Given
        String processInstanceId = "test-process-123";
        when(bpmnProcessService.getOnboardingStatus(processInstanceId)).thenReturn(statusResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/onboarding/{processInstanceId}/status", processInstanceId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.processInstanceId").value(processInstanceId))
                .andExpect(jsonPath("$.status").value("INFO_COLLECTED"))
                .andExpect(jsonPath("$.message").value("Information collected successfully"))
                .andExpect(jsonPath("$.currentStep").value("Information collection completed"))
                .andExpect(jsonPath("$.nextStep").value("document_upload"));

        verify(bpmnProcessService).getOnboardingStatus(processInstanceId);
    }

    @Test
    void getOnboardingStatus_ProcessNotFound_ReturnsNotFound() throws Exception {
        // Given
        String processInstanceId = "non-existent-process";
        when(bpmnProcessService.getOnboardingStatus(processInstanceId))
                .thenThrow(new RuntimeException("Process not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/onboarding/{processInstanceId}/status", processInstanceId))
                .andExpect(status().isInternalServerError());

        verify(bpmnProcessService).getOnboardingStatus(processInstanceId);
    }

    @Test
    void startOnboardingWorkflow_MissingRequiredFields_ReturnsBadRequest() throws Exception {
        // Given
        OnboardingStartRequest invalidRequest = new OnboardingStartRequest()
                .firstName("Emma")
                // Missing lastName
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

        // When & Then
        mockMvc.perform(post("/api/v1/onboarding/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(bpmnProcessService, never()).startOnboardingProcess(any(OnboardingStartRequest.class));
    }

    @Test
    void startOnboardingWorkflow_InvalidEmailFormat_ReturnsBadRequest() throws Exception {
        // Given
        OnboardingStartRequest invalidRequest = new OnboardingStartRequest()
                .firstName("Emma")
                .lastName("de Vries")
                .gender(OnboardingStartRequest.GenderEnum.F)
                .dob(LocalDate.of(1990, 5, 20))
                .phone("+31612345678")
                .email("invalid-email") // Invalid email format
                .nationality("Dutch")
                .street("Keizersgracht 1")
                .city("Amsterdam")
                .postalCode("1015CD")
                .country("Netherlands")
                .ssn("123-45-6789");

        // When & Then
        mockMvc.perform(post("/api/v1/onboarding/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(bpmnProcessService, never()).startOnboardingProcess(any(OnboardingStartRequest.class));
    }

    @Test
    void startOnboardingWorkflow_InvalidSsnFormat_ReturnsBadRequest() throws Exception {
        // Given
        OnboardingStartRequest invalidRequest = new OnboardingStartRequest()
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
                .ssn("123456789"); // Invalid SSN format (missing dashes)

        // When & Then
        mockMvc.perform(post("/api/v1/onboarding/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(bpmnProcessService, never()).startOnboardingProcess(any(OnboardingStartRequest.class));
    }


    @Test
    void startOnboardingWorkflow_EmptyFirstName_ReturnsBadRequest() throws Exception {
        // Given
        OnboardingStartRequest invalidRequest = new OnboardingStartRequest()
                .firstName("") // Empty first name
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

        // When & Then
        mockMvc.perform(post("/api/v1/onboarding/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(bpmnProcessService, never()).startOnboardingProcess(any(OnboardingStartRequest.class));
    }

    @Test
    void startOnboardingWorkflow_InvalidPhoneFormat_ReturnsBadRequest() throws Exception {
        // Given
        OnboardingStartRequest invalidRequest = new OnboardingStartRequest()
                .firstName("Emma")
                .lastName("de Vries")
                .gender(OnboardingStartRequest.GenderEnum.F)
                .dob(LocalDate.of(1990, 5, 20))
                .phone("123456789") // Invalid phone format (missing +)
                .email("emma.devries@example.com")
                .nationality("Dutch")
                .street("Keizersgracht 1")
                .city("Amsterdam")
                .postalCode("1015CD")
                .country("Netherlands")
                .ssn("123-45-6789");

        // When & Then
        mockMvc.perform(post("/api/v1/onboarding/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(bpmnProcessService, never()).startOnboardingProcess(any(OnboardingStartRequest.class));
    }
}
