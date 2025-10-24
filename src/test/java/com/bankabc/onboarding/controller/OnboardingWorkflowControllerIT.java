package com.bankabc.onboarding.controller;

import com.bankabc.onboarding.entity.Onboarding;
import com.bankabc.onboarding.entity.Onboarding.OnboardingStatus;
import com.bankabc.onboarding.openapi.model.OnboardingStartRequest;
import com.bankabc.onboarding.service.BpmnProcessService;
import com.bankabc.onboarding.service.OnboardingService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql(scripts = "/cleanup-database.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OnboardingWorkflowControllerIT {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private BpmnProcessService bpmnProcessService;

    @Autowired
    private OnboardingService onboardingService;

    private OnboardingStartRequest validRequest;

    @BeforeEach
    void setUp() {
        // Generate unique SSN for each test
        // Format: XXX-XX-XXXX (11 characters total)
        // Use current time in nanoseconds to ensure absolute uniqueness
        long nanoTime = System.nanoTime();
        String uniqueSsn = String.format("%03d-%02d-%04d", 
            (int)(nanoTime % 1000),
            (int)((nanoTime / 1000) % 100),
            (int)((nanoTime / 100000) % 10000));
            
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
    void completeOnboardingWorkflow_SuccessFlow_CompletesAllSteps() throws Exception {
        // Given: Start the BPMN process
        var startResponse = bpmnProcessService.startOnboardingProcess(validRequest);
        String processInstanceId = startResponse.getProcessInstanceId();
        
        // Verify process started
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        assertNotNull(processInstance);
        
        // Step 1: Verify CollectInfoTask completed
        waitForTaskCompletion("CollectInfoTask", processInstanceId);
        Onboarding onboarding = onboardingService.findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new AssertionError("Onboarding entity not found"));
        assertEquals(OnboardingStatus.INFO_COLLECTED, onboarding.getStatus());
        
        // Step 2: Wait a moment for process to reach document upload wait state, then simulate document upload correlation
        Thread.sleep(1000); // Give the process time to reach the intermediate catch event
        
        Map<String, String> documentData = new HashMap<>();
        documentData.put("passport", "https://bankabc-documents.s3.amazonaws.com/onboarding/test/passport.pdf");
        documentData.put("photo", "https://bankabc-documents.s3.amazonaws.com/onboarding/test/photo.jpg");
        
        bpmnProcessService.correlateDocumentUpload(processInstanceId, documentData);
        
        // Step 3: Complete UploadDocumentsTask
        waitForTaskCompletion("UploadDocumentsTask", processInstanceId);
        
        // Step 4: Complete KYC Verification
        completeTaskWithStringVariables("KycVerificationTask", processInstanceId, Map.of(
            "kycResult", "PASSED",
            "kycVerified", "true"
        ));
        
        // Verify KYC status
        onboarding = onboardingService.findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new AssertionError("Onboarding entity not found"));
        assertTrue(onboarding.getKycVerified());
        
        // Step 5: Complete Address Verification
        completeTaskWithStringVariables("AddressVerificationTask", processInstanceId, Map.of(
            "addressResult", "PASSED",
            "addressVerified", "true"
        ));
        
        // Verify address verification status
        onboarding = onboardingService.findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new AssertionError("Onboarding entity not found"));
        assertTrue(onboarding.getAddressVerified());
        
        // Step 6: Complete Account Creation
        completeTaskWithStringVariables("AccountCreationTask", processInstanceId, Map.of(
            "accountCreated", "true"
        ));
        
        // Verify account creation
        onboarding = onboardingService.findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new AssertionError("Onboarding entity not found"));
        assertNotNull(onboarding.getAccountNumber());
        assertTrue(onboarding.getAccountNumber().startsWith("NL"));
        
        // Step 7: Complete Customer Notification
        completeTaskWithStringVariables("NotifyCustomerTask", processInstanceId, Map.of(
            "notificationSent", "true"
        ));
        
        // Wait for process to complete
        waitForProcessCompletion(processInstanceId);
        
        // Verify final state
        onboarding = onboardingService.findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new AssertionError("Onboarding entity not found"));
        assertEquals(OnboardingStatus.COMPLETED, onboarding.getStatus());
        assertTrue(onboarding.getKycVerified());
        assertTrue(onboarding.getAddressVerified());
        assertNotNull(onboarding.getAccountNumber());
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

    private void completeTaskWithStringVariables(String taskName, String processInstanceId, Map<String, String> variables) {
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(taskName)
                .list();
        
        if (!tasks.isEmpty()) {
            Task task = tasks.get(0);
            // Convert Map<String, String> to Map<String, Object> for Camunda
            Map<String, Object> objectVariables = new HashMap<>();
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                objectVariables.put(entry.getKey(), entry.getValue());
            }
            taskService.complete(task.getId(), objectVariables);
        }
    }

    private void waitForProcessCompletion(String processInstanceId) throws InterruptedException {
        int maxAttempts = 30;
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            
            if (processInstance == null) {
                return; // Process completed
            }
            
            Thread.sleep(100);
            attempt++;
        }
        
        throw new AssertionError("Process did not complete within expected time");
    }
}