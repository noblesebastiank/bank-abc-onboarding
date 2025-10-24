package com.bankabc.onboarding.delegate;

import com.bankabc.onboarding.entity.Onboarding;
import com.bankabc.onboarding.entity.Onboarding.OnboardingStatus;
import com.bankabc.onboarding.exception.DefaultApiError;
import com.bankabc.onboarding.exception.ErrorTypes;
import com.bankabc.onboarding.service.NotificationService;
import com.bankabc.onboarding.service.OnboardingService;
import com.bankabc.onboarding.service.VerificationService;
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
        when(execution.getVariable("onboardingId")).thenReturn(testOnboardingId);
        when(onboardingService.findById((UUID) testOnboardingId)).thenReturn(Optional.of(testOnboarding));
        when(verificationService.performKycVerification(
                "John", "Doe", LocalDate.of(1990, 1, 1), 
                "123-45-6789", "/path/to/passport.pdf", "/path/to/photo.jpg"))
                .thenReturn(true);

        // When
        kycDelegate.execute(execution);

        // Then
        verify(onboardingService, times(2)).saveOnboarding(testOnboarding);
        assertEquals(OnboardingStatus.KYC_COMPLETED, testOnboarding.getStatus());
        assertTrue(testOnboarding.getKycVerified());
        
        verify(execution).setVariable("kycResult", "SUCCESS");
        verify(execution).setVariable("kycVerified", true);
        verify(execution).setVariable("status", OnboardingStatus.KYC_COMPLETED.name());
    }

    @Test
    void execute_KycVerificationFailure_UpdatesStatusAndThrowsException() throws Exception {
        // Given
        when(execution.getProcessInstanceId()).thenReturn(processInstanceId);
        when(execution.getVariable("onboardingId")).thenReturn(testOnboardingId);
        when(onboardingService.findById((UUID) testOnboardingId)).thenReturn(Optional.of(testOnboarding));
        when(verificationService.performKycVerification(
                "John", "Doe", LocalDate.of(1990, 1, 1), 
                "123-45-6789", "/path/to/passport.pdf", "/path/to/photo.jpg"))
                .thenReturn(false);

        // When & Then
        DefaultApiError exception = assertThrows(DefaultApiError.class, () -> {
            kycDelegate.execute(execution);
        });

        // Verify exception details
        assertEquals(ErrorTypes.KYC_VERIFICATION_FAILED.name(), exception.getErrorName());
        assertEquals(ErrorTypes.KYC_VERIFICATION_FAILED.getMessage(), exception.getMessage());
        
        // Verify onboarding was updated
        verify(onboardingService, times(2)).saveOnboarding(testOnboarding);
        assertEquals(OnboardingStatus.FAILED, testOnboarding.getStatus());
        assertFalse(testOnboarding.getKycVerified());
        
        // Verify BPMN variables were set
        verify(execution).setVariable("kycResult", "FAILED");
        verify(execution).setVariable("kycVerified", false);
        verify(execution).setVariable("status", OnboardingStatus.FAILED.name());
        verify(execution).setVariable("errorType", ErrorTypes.KYC_VERIFICATION_FAILED.name());
        verify(execution).setVariable("errorMessage", "KYC verification failed");
        verify(execution).setVariable("failedStepId", "kyc-verification");
    }

    @Test
    void execute_MissingOnboardingId_ThrowsException() throws Exception {
        // Given
        when(execution.getProcessInstanceId()).thenReturn(processInstanceId);
        when(execution.getVariable("onboardingId")).thenReturn(null);

        // When & Then
        DefaultApiError exception = assertThrows(DefaultApiError.class, () -> {
            kycDelegate.execute(execution);
        });

        assertEquals(ErrorTypes.INVALID_REQUEST.name(), exception.getErrorName());
        assertEquals("Onboarding ID not found in process variables", exception.getMessage());
        verify(onboardingService, never()).findById(any(UUID.class));
    }

    @Test
    void execute_OnboardingNotFound_ThrowsException() throws Exception {
        // Given
        when(execution.getProcessInstanceId()).thenReturn(processInstanceId);
        when(execution.getVariable("onboardingId")).thenReturn(testOnboardingId);
        when(onboardingService.findById(testOnboardingId)).thenReturn(Optional.empty());

        // When & Then
        DefaultApiError exception = assertThrows(DefaultApiError.class, () -> {
            kycDelegate.execute(execution);
        });

        assertEquals(ErrorTypes.ONBOARDING_NOT_FOUND.name(), exception.getErrorName());
        assertTrue(exception.getMessage().contains(testOnboardingId.toString()));
    }

    @Test
    void execute_VerificationServiceThrowsException_HandlesGracefully() throws Exception {
        // Given
        when(execution.getProcessInstanceId()).thenReturn(processInstanceId);
        when(execution.getVariable("onboardingId")).thenReturn(testOnboardingId);
        when(onboardingService.findById((UUID) testOnboardingId)).thenReturn(Optional.of(testOnboarding));
        when(verificationService.performKycVerification(any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Verification service error"));

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            kycDelegate.execute(execution);
        });

        // Verify error handling
        verify(execution).setVariable("kycResult", "ERROR");
        verify(execution).setVariable("errorMessage", "KYC verification error: Verification service error");
        verify(execution).setVariable("failedStepId", "KycVerificationTask");
        
        // Verify onboarding status was updated to KYC_IN_PROGRESS before the error
        verify(onboardingService).saveOnboarding(argThat(onboarding -> 
                onboarding.getStatus() == OnboardingStatus.KYC_IN_PROGRESS));
    }

    @Test
    void execute_OnboardingServiceThrowsException_HandlesGracefully() throws Exception {
        // Given
        when(execution.getProcessInstanceId()).thenReturn(processInstanceId);
        when(execution.getVariable("onboardingId")).thenReturn(testOnboardingId);
        when(onboardingService.findById(testOnboardingId)).thenThrow(new RuntimeException("Database error"));

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            kycDelegate.execute(execution);
        });

        // Verify error handling
        verify(execution).setVariable("kycResult", "ERROR");
        verify(execution).setVariable("errorMessage", "KYC verification error: Database error");
        verify(execution).setVariable("failedStepId", "KycVerificationTask");
    }

    @Test
    void execute_WithNullPassportPath_HandlesGracefully() throws Exception {
        // Given
        testOnboarding.setPassportPath(null);
        when(execution.getProcessInstanceId()).thenReturn(processInstanceId);
        when(execution.getVariable("onboardingId")).thenReturn(testOnboardingId);
        when(onboardingService.findById((UUID) testOnboardingId)).thenReturn(Optional.of(testOnboarding));
        when(verificationService.performKycVerification(
                "John", "Doe", LocalDate.of(1990, 1, 1), 
                "123-45-6789", null, "/path/to/photo.jpg"))
                .thenReturn(true);

        // When
        kycDelegate.execute(execution);

        // Then
        verify(verificationService).performKycVerification(
                "John", "Doe", LocalDate.of(1990, 1, 1), 
                "123-45-6789", null, "/path/to/photo.jpg");
        verify(onboardingService, times(2)).saveOnboarding(testOnboarding);
        assertEquals(OnboardingStatus.KYC_COMPLETED, testOnboarding.getStatus());
    }

    @Test
    void execute_WithNullPhotoPath_HandlesGracefully() throws Exception {
        // Given
        testOnboarding.setPhotoPath(null);
        when(execution.getProcessInstanceId()).thenReturn(processInstanceId);
        when(execution.getVariable("onboardingId")).thenReturn(testOnboardingId);
        when(onboardingService.findById((UUID) testOnboardingId)).thenReturn(Optional.of(testOnboarding));
        when(verificationService.performKycVerification(
                "John", "Doe", LocalDate.of(1990, 1, 1), 
                "123-45-6789", "/path/to/passport.pdf", null))
                .thenReturn(true);

        // When
        kycDelegate.execute(execution);

        // Then
        verify(verificationService).performKycVerification(
                "John", "Doe", LocalDate.of(1990, 1, 1), 
                "123-45-6789", "/path/to/passport.pdf", null);
        verify(onboardingService, times(2)).saveOnboarding(testOnboarding);
        assertEquals(OnboardingStatus.KYC_COMPLETED, testOnboarding.getStatus());
    }
}
