package com.bankabc.onboarding.controller;

import com.bankabc.onboarding.openapi.model.OnboardingStartRequest;
import com.bankabc.onboarding.openapi.model.OnboardingStartResponse;
import com.bankabc.onboarding.openapi.model.OnboardingStatusResponse;
import com.bankabc.onboarding.openapi.model.DocumentUploadResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full REST API integration tests for OnboardingWorkflowController.
 * Tests the complete end-to-end flow through REST endpoints.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class OnboardingWorkflowControllerFullRestIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private OnboardingStartRequest validRequest;

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
    }

    @Test
    void completeOnboardingFlow_ThroughRestEndpoints_Success() throws Exception {
        // Step 1: Start onboarding process
        MvcResult startResult = mockMvc.perform(post("/api/v1/onboarding/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.processInstanceId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("INFO_COLLECTED"))
                .andExpect(jsonPath("$.nextStep").value("document_upload"))
                .andReturn();

        // Extract process instance ID from response
        String responseContent = startResult.getResponse().getContentAsString();
        OnboardingStartResponse startResponse = objectMapper.readValue(responseContent, OnboardingStartResponse.class);
        String processInstanceId = startResponse.getProcessInstanceId();
        assertNotNull(processInstanceId);

        // Step 2: Check initial status
        mockMvc.perform(get("/api/v1/onboarding/{processInstanceId}/status", processInstanceId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.processInstanceId").value(processInstanceId))
                .andExpect(jsonPath("$.status").value("INFO_COLLECTED"));

        // Step 3: Upload documents
        MockMultipartFile passport = new MockMultipartFile(
                "passport", "passport.pdf", "application/pdf", "passport content".getBytes());
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "photo.jpg", "image/jpeg", "photo content".getBytes());

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/onboarding/{processInstanceId}/documents", processInstanceId)
                .file(passport)
                .file(photo))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.processInstanceId").value(processInstanceId))
                .andExpect(jsonPath("$.passportUploaded").value(true))
                .andExpect(jsonPath("$.photoUploaded").value(true))
                .andReturn();

        // Extract upload response
        String uploadContent = uploadResult.getResponse().getContentAsString();
        DocumentUploadResponse uploadResponse = objectMapper.readValue(uploadContent, DocumentUploadResponse.class);
        assertEquals(processInstanceId, uploadResponse.getProcessInstanceId());
        assertTrue(uploadResponse.getPassportUploaded());
        assertTrue(uploadResponse.getPhotoUploaded());

        // Step 4: Check status after document upload
        mockMvc.perform(get("/api/v1/onboarding/{processInstanceId}/status", processInstanceId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.processInstanceId").value(processInstanceId))
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void startOnboarding_WithInvalidData_ReturnsValidationErrors() throws Exception {
        // Test with empty first name
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

        mockMvc.perform(post("/api/v1/onboarding/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadDocuments_WithInvalidProcessId_ReturnsNotFound() throws Exception {
        // Given
        String nonExistentProcessId = "non-existent-process-123";
        MockMultipartFile passport = new MockMultipartFile(
                "passport", "passport.pdf", "application/pdf", "passport content".getBytes());
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "photo.jpg", "image/jpeg", "photo content".getBytes());

        // When & Then
        mockMvc.perform(multipart("/api/v1/onboarding/{processInstanceId}/documents", nonExistentProcessId)
                .file(passport)
                .file(photo))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorName").value("ONBOARDING_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Process not found"));
    }

    @Test
    void getOnboardingStatus_WithInvalidProcessId_ReturnsError() throws Exception {
        // Given
        String nonExistentProcessId = "non-existent-process-123";

        // When & Then
        mockMvc.perform(get("/api/v1/onboarding/{processInstanceId}/status", nonExistentProcessId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void startOnboarding_WithDuplicateSsn_ReturnsConflict() throws Exception {
        // First, start an onboarding process
        mockMvc.perform(post("/api/v1/onboarding/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isAccepted());

        // Then try to start another process with the same SSN
        mockMvc.perform(post("/api/v1/onboarding/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorName").value("CUSTOMER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Customer with this SSN already exists"));
    }

    @Test
    void uploadDocuments_WithEmptyFiles_ReturnsBadRequest() throws Exception {
        // First, start an onboarding process
        MvcResult startResult = mockMvc.perform(post("/api/v1/onboarding/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isAccepted())
                .andReturn();

        String responseContent = startResult.getResponse().getContentAsString();
        OnboardingStartResponse startResponse = objectMapper.readValue(responseContent, OnboardingStartResponse.class);
        String processInstanceId = startResponse.getProcessInstanceId();

        // Then try to upload empty files
        MockMultipartFile emptyPassport = new MockMultipartFile(
                "passport", "passport.pdf", "application/pdf", new byte[0]);
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "photo.jpg", "image/jpeg", "photo content".getBytes());

        mockMvc.perform(multipart("/api/v1/onboarding/{processInstanceId}/documents", processInstanceId)
                .file(emptyPassport)
                .file(photo))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorName").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Both passport and photo files are required and cannot be empty"));
    }

    @Test
    void startOnboarding_WithInvalidEmailFormat_ReturnsBadRequest() throws Exception {
        OnboardingStartRequest invalidRequest = new OnboardingStartRequest()
                .firstName("Emma")
                .lastName("de Vries")
                .gender(OnboardingStartRequest.GenderEnum.F)
                .dob(LocalDate.of(1990, 5, 20))
                .phone("+31612345678")
                .email("invalid-email-format") // Invalid email
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
    void startOnboarding_WithInvalidPhoneFormat_ReturnsBadRequest() throws Exception {
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

        mockMvc.perform(post("/api/v1/onboarding/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startOnboarding_WithInvalidSsnFormat_ReturnsBadRequest() throws Exception {
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

        mockMvc.perform(post("/api/v1/onboarding/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
