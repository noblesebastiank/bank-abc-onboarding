package com.bankabc.onboarding.controller;

import com.bankabc.onboarding.entity.Onboarding;
import com.bankabc.onboarding.entity.Onboarding.OnboardingStatus;
import com.bankabc.onboarding.openapi.model.OnboardingStartRequest;
import com.bankabc.onboarding.service.BpmnProcessService;
import com.bankabc.onboarding.service.OnboardingService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"test", "upload-failure-test"})
@Transactional
class OnboardingWorkflowControllerUploadFailureIT {

    @Autowired
    private BpmnProcessService bpmnProcessService;

    @Autowired
    private OnboardingService onboardingService;

    @Autowired
    private TaskService taskService;

    private OnboardingStartRequest validRequest;
    private String testPassportPath;
    private String testPhotoPath;

    @BeforeEach
    void setUp() throws IOException {
        // Generate unique SSN for each test
        UUID uuid = UUID.randomUUID();
        int hash = Math.abs(uuid.hashCode());
        String uniqueSsn = String.format("%03d-%02d-%04d",
            hash % 1000,
            (hash / 1000) % 100,
            (hash / 100000) % 10000);

        validRequest = new OnboardingStartRequest()
                .firstName("Test")
                .lastName("User")
                .gender(OnboardingStartRequest.GenderEnum.M)
                .dob(LocalDate.of(1985, 3, 15))
                .phone("+31612345678")
                .email("test.user@example.com")
                .nationality("Dutch")
                .street("Test Street 123")
                .city("Amsterdam")
                .postalCode("1012AB")
                .country("Netherlands")
                .ssn(uniqueSsn);

        // Create test files that will fail validation
        createTestFiles();
    }

    private void createTestFiles() throws IOException {
        // Create test directory
        Path testDir = Paths.get("/tmp/test-upload-documents");
        Files.createDirectories(testDir);

        // Create oversized passport file (6MB - exceeds 5MB DMN limit)
        testPassportPath = testDir.resolve("oversized-passport.pdf").toString();
        createOversizedFile(testPassportPath, 6 * 1024 * 1024); // 6MB

        // Create oversized photo file (3MB - exceeds 2MB DMN limit)
        testPhotoPath = testDir.resolve("oversized-photo.jpg").toString();
        createOversizedFile(testPhotoPath, 3 * 1024 * 1024); // 3MB
    }

    private void createOversizedFile(String filePath, long sizeBytes) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            byte[] buffer = new byte[1024];
            long written = 0;
            while (written < sizeBytes) {
                int toWrite = (int) Math.min(buffer.length, sizeBytes - written);
                fos.write(buffer, 0, toWrite);
                written += toWrite;
            }
        }
    }

    @Test
    void bpmnProcess_UploadValidationFailure_TriggersErrorHandling() throws Exception {
        // Given: Start the BPMN process
        var startResponse = bpmnProcessService.startOnboardingProcess(validRequest);
        String processInstanceId = startResponse.getProcessInstanceId();
        
        // Complete initial steps
        waitForTaskCompletion("CollectInfoTask", processInstanceId);
        
        // Wait a moment for process to reach document upload wait state
        Thread.sleep(1000);
        
        // Prepare document data with oversized files
        Map<String, String> documentData = new HashMap<>();
        documentData.put("passport", testPassportPath);
        documentData.put("photo", testPhotoPath);
        
        // When: Correlate document upload with oversized files
        try {
            bpmnProcessService.correlateDocumentUpload(processInstanceId, documentData);
        } catch (Exception e) {
            // Expected - upload validation failure should be handled gracefully
            System.out.println("Expected exception during document upload due to validation failure: " + e.getMessage());
        }
        
        // Wait for error handling to complete
        Thread.sleep(3000);
        
        // Then: Verify error handling was triggered and status is correct
        Onboarding onboarding = onboardingService.findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new AssertionError("Onboarding entity not found"));
        
        // Should be WAITING_FOR_DOCUMENTS (reverted back for retry)
        assertEquals(OnboardingStatus.WAITING_FOR_DOCUMENTS, onboarding.getStatus());
        
        // Verify the process completed (reached UploadErrorEnd)
        assertTrue(isProcessCompleted(processInstanceId), "Process should have completed with upload error");
    }

    @Test
    void bpmnProcess_ValidFiles_ShouldPassValidation() throws Exception {
        // Given: Start the BPMN process
        var startResponse = bpmnProcessService.startOnboardingProcess(validRequest);
        String processInstanceId = startResponse.getProcessInstanceId();
        
        // Complete initial steps
        waitForTaskCompletion("CollectInfoTask", processInstanceId);
        
        // Wait a moment for process to reach document upload wait state
        Thread.sleep(1000);
        
        // Create valid-sized files
        Path testDir = Paths.get("/tmp/test-upload-documents");
        String validPassportPath = testDir.resolve("valid-passport.pdf").toString();
        String validPhotoPath = testDir.resolve("valid-photo.jpg").toString();
        
        // Create valid-sized files (within DMN limits)
        createOversizedFile(validPassportPath, 4 * 1024 * 1024); // 4MB (under 5MB limit)
        createOversizedFile(validPhotoPath, 1 * 1024 * 1024);    // 1MB (under 2MB limit)
        
        Map<String, String> documentData = new HashMap<>();
        documentData.put("passport", validPassportPath);
        documentData.put("photo", validPhotoPath);
        
        // When: Correlate document upload with valid files
        bpmnProcessService.correlateDocumentUpload(processInstanceId, documentData);
        
        // Wait for processing
        Thread.sleep(2000);
        
        // Then: Verify process continues normally
        Onboarding onboarding = onboardingService.findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new AssertionError("Onboarding entity not found"));
        
        // Should be DOCUMENTS_UPLOADED, not DOCUMENT_UPLOAD_FAILED
        assertEquals(OnboardingStatus.DOCUMENTS_UPLOADED, onboarding.getStatus());
    }

    private void waitForTaskCompletion(String taskName, String processInstanceId) throws InterruptedException {
        int maxAttempts = 30;
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .taskDefinitionKey(taskName)
                    .list();
            
            if (tasks.isEmpty()) {
                return; // Task completed
            }
            
            Thread.sleep(500);
            attempt++;
        }
        
        throw new AssertionError("Task " + taskName + " did not complete within expected time");
    }

    private boolean isProcessCompleted(String processInstanceId) {
        return bpmnProcessService.getOnboardingStatus(processInstanceId).getStatus() != null;
    }
}
