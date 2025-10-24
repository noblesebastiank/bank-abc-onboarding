package com.bankabc.onboarding.delegate;

import com.bankabc.onboarding.entity.Onboarding;
import com.bankabc.onboarding.entity.Onboarding.OnboardingStatus;
import com.bankabc.onboarding.exception.DefaultApiError;
import com.bankabc.onboarding.exception.ErrorTypes;
import com.bankabc.onboarding.service.NotificationService;
import com.bankabc.onboarding.service.OnboardingService;
import com.bankabc.onboarding.service.VerificationService;
import com.bankabc.onboarding.util.DelegateUtils;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycDelegateTest {

    @Mock
    private VerificationService verificationService;

    @Mock
    private OnboardingService onboardingService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DelegateUtils delegateUtils;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private KycDelegate kycDelegate;

    private Onboarding testOnboarding;
    private UUID testOnboardingId;
    private String processInstanceId;

    @BeforeEach
    void setUp() {
        testOnboardingId = UUID.randomUUID();
        processInstanceId = "test-process-instance-123";
        
        testOnboarding = new Onboarding();
        testOnboarding.setId(testOnboardingId);
        testOnboarding.setFirstName("John");
        testOnboarding.setLastName("Doe");
        testOnboarding.setDateOfBirth(LocalDate.of(1990, 1, 1));
        testOnboarding.setSsn("123-45-6789");
        testOnboarding.setPassportPath("/path/to/passport.pdf");
        testOnboarding.setPhotoPath("/path/to/photo.jpg");
        testOnboarding.setStatus(OnboardingStatus.INFO_COLLECTED);
    }

    @Test
    void execute_KycVerificationSuccess_UpdatesStatusAndSetsVariables() throws Exception {
        // Given
        when(execution.getProcessInstanceId()).thenReturn(processInstanceId);
        when(delegateUtils.getOnboarding(execution)).thenReturn(testOnboarding);
        when(verificationService.performKycVerification(
                "John", "Doe", LocalDate.of(1990, 1, 1), 
                "123-45-6789"))
                .thenReturn(true);

        // When
        kycDelegate.execute(execution);

        // Then
        verify(onboardingService, times(2)).saveOnboarding(testOnboarding);
        assertEquals(OnboardingStatus.KYC_COMPLETED, testOnboarding.getStatus());
        assertTrue(testOnboarding.getKycVerified());
        
        verify(execution).setVariable("kycResult", "SUCCESS");
        verify(execution).setVariable("kycVerified", "true");
        verify(execution).setVariable("status", OnboardingStatus.KYC_COMPLETED.name());
    }

    @Test
    void execute_KycVerificationFailure_UpdatesStatusAndThrowsException() throws Exception {
        // Given
        when(execution.getProcessInstanceId()).thenReturn(processInstanceId);
        when(delegateUtils.getOnboarding(execution)).thenReturn(testOnboarding);
        when(verificationService.performKycVerification(
                "John", "Doe", LocalDate.of(1990, 1, 1), 
                "123-45-6789"))
                .thenReturn(false);

        // When & Then
        BpmnError exception = assertThrows(BpmnError.class, () -> {
            kycDelegate.execute(execution);
        });

        // Verify exception details
        assertEquals("KYC_VERIFICATION_FAILED", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("KYC verification failed"));
        
        // Verify onboarding was updated
        verify(onboardingService, times(2)).saveOnboarding(testOnboarding);
        assertEquals(OnboardingStatus.KYC_IN_PROGRESS, testOnboarding.getStatus());
        assertFalse(testOnboarding.getKycVerified());
        
        // Verify BPMN variables were set
        verify(execution).setVariable("kycResult", "FAILED");
        verify(execution).setVariable("kycVerified", "false");
        verify(execution).setVariable("errorType", ErrorTypes.KYC_VERIFICATION_FAILED.name());
        verify(execution).setVariable("errorMessage", "KYC verification failed");
        verify(execution).setVariable("failedStepId", "kyc-verification");
    }

    @Test
    void execute_MissingOnboardingId_ThrowsException() throws Exception {
        // Given
        when(execution.getProcessInstanceId()).thenReturn(processInstanceId);
        when(delegateUtils.getOnboarding(execution)).thenThrow(new DefaultApiError(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                ErrorTypes.INVALID_REQUEST.name(),
                "Onboarding ID not found in process variables",
                java.util.Map.of("processInstanceId", processInstanceId)
        ));

        // When & Then
        BpmnError exception = assertThrows(BpmnError.class, () -> {
            kycDelegate.execute(execution);
        });

        assertEquals("KYC_VERIFICATION_FAILED", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Onboarding ID not found"));
    }

    @Test
    void execute_OnboardingNotFound_ThrowsException() throws Exception {
        // Given
        when(execution.getProcessInstanceId()).thenReturn(processInstanceId);
        when(delegateUtils.getOnboarding(execution)).thenThrow(new DefaultApiError(
                org.springframework.http.HttpStatus.NOT_FOUND,
                ErrorTypes.ONBOARDING_NOT_FOUND.name(),
                "Onboarding not found: " + testOnboardingId,
                java.util.Map.of("onboardingId", testOnboardingId.toString())
        ));

        // When & Then
        BpmnError exception = assertThrows(BpmnError.class, () -> {
            kycDelegate.execute(execution);
        });

        assertEquals("KYC_VERIFICATION_FAILED", exception.getErrorCode());
        assertTrue(exception.getMessage().contains(testOnboardingId.toString()));
    }

    @Test
    void execute_VerificationServiceThrowsException_HandlesGracefully() throws Exception {
        // Given
        when(execution.getProcessInstanceId()).thenReturn(processInstanceId);
        when(delegateUtils.getOnboarding(execution)).thenReturn(testOnboarding);
        when(verificationService.performKycVerification(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Verification service error"));

        // When & Then
        BpmnError exception = assertThrows(BpmnError.class, () -> {
            kycDelegate.execute(execution);
        });

        // Verify error handling
        verify(execution).setVariable("kycResult", "ERROR");
        verify(execution).setVariable("errorMessage", "KYC verification error: Verification service error");
        verify(execution).setVariable("failedStepId", "kyc-verification");
        
        // Verify onboarding status was updated to KYC_IN_PROGRESS before the error
        verify(onboardingService).saveOnboarding(argThat(onboarding -> 
                onboarding.getStatus() == OnboardingStatus.KYC_IN_PROGRESS));
    }

    @Test
    void execute_OnboardingServiceThrowsException_HandlesGracefully() throws Exception {
        // Given
        when(execution.getProcessInstanceId()).thenReturn(processInstanceId);
        when(delegateUtils.getOnboarding(execution)).thenThrow(new RuntimeException("Database error"));

        // When & Then
        BpmnError exception = assertThrows(BpmnError.class, () -> {
            kycDelegate.execute(execution);
        });

        // Verify error handling
        verify(execution).setVariable("kycResult", "ERROR");
        verify(execution).setVariable("errorMessage", "KYC verification error: Database error");
        verify(execution).setVariable("failedStepId", "kyc-verification");
    }




}
