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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"test", "account-failure-test"})
@Transactional
class OnboardingWorkflowControllerAccountFailureIT {

    @Autowired
    private BpmnProcessService bpmnProcessService;

    @Autowired
    private OnboardingService onboardingService;

    @Autowired
    private TaskService taskService;

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
    void bpmnProcess_AccountCreationFailure_TriggersErrorHandling() throws Exception {
        // Given: Create a request with invalid data that will cause account creation to fail
        OnboardingStartRequest invalidRequest = new OnboardingStartRequest()
                .firstName("")  // Empty first name will cause account creation to fail
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
        
        var startResponse = bpmnProcessService.startOnboardingProcess(invalidRequest);
        String processInstanceId = startResponse.getProcessInstanceId();
        
        waitForTaskCompletion("CollectInfoTask", processInstanceId);
        
        // Wait a moment for process to reach document upload wait state
        Thread.sleep(1000); // Give the process time to reach the intermediate catch event
        
        Map<String, String> documentData = new HashMap<>();
        documentData.put("passport", "https://bankabc-documents.s3.amazonaws.com/onboarding/test/passport.pdf");
        documentData.put("photo", "https://bankabc-documents.s3.amazonaws.com/onboarding/test/photo.jpg");
        
        // The document upload correlation will trigger account creation which will fail
        // and cause the entire process to fail
        try {
            bpmnProcessService.correlateDocumentUpload(processInstanceId, documentData);
        } catch (Exception e) {
            // Expected - Account creation failure should cause document upload to fail
            System.out.println("Expected exception during document upload due to account creation failure: " + e.getMessage());
        }
        
        // Wait a bit more for error handling to complete
        Thread.sleep(3000);
        
        // Then: Verify error handling was triggered
        Onboarding onboarding = onboardingService.findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new AssertionError("Onboarding entity not found"));
        assertEquals(OnboardingStatus.FAILED, onboarding.getStatus());
        // KYC and address verification may have failed due to invalid data
        // The important thing is that the process failed and no account was created
        assertNull(onboarding.getAccountNumber()); // Account creation failed
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
            
            Thread.sleep(100);
            attempt++;
        }
        
        throw new AssertionError("Task " + taskName + " did not complete within expected time");
    }
}
