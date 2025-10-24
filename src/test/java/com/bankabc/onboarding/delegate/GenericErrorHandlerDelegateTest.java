package com.bankabc.onboarding.delegate;

import com.bankabc.onboarding.entity.Onboarding;
import com.bankabc.onboarding.entity.Onboarding.OnboardingStatus;
import com.bankabc.onboarding.service.OnboardingService;
import com.bankabc.onboarding.service.NotificationService;
import com.bankabc.onboarding.service.WorkflowConfigurationService;
import com.bankabc.onboarding.util.DelegateUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for GenericErrorHandlerDelegate.
 */
@ExtendWith(MockitoExtension.class)
class GenericErrorHandlerDelegateTest {

    @Mock
    private OnboardingService onboardingService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private WorkflowConfigurationService workflowConfigurationService;

    @Mock
    private DelegateUtils delegateUtils;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private GenericErrorHandlerDelegate genericErrorHandlerDelegate;

    private Onboarding testOnboarding;
    private JsonNode mockStepConfig;

    @BeforeEach
    void setUp() {
        testOnboarding = new Onboarding();
        testOnboarding.setId(UUID.randomUUID());
        testOnboarding.setFirstName("John");
        testOnboarding.setLastName("Doe");
        testOnboarding.setEmail("john.doe@example.com");
        testOnboarding.setPhone("+1234567890");
        testOnboarding.setStatus(OnboardingStatus.KYC_IN_PROGRESS);
    }

    @Test
    void testExecute_WithStepConfiguration() throws Exception {
        // Given
        JsonNode mockStepConfig = mock(JsonNode.class);
        JsonNode errorHandling = mock(JsonNode.class);
        JsonNode errorTypeNode = mock(JsonNode.class);
        JsonNode defaultMessageNode = mock(JsonNode.class);
        
        when(mockStepConfig.path("errorHandling")).thenReturn(errorHandling);
        when(errorHandling.path("errorType")).thenReturn(errorTypeNode);
        when(errorTypeNode.asText(null)).thenReturn("KYC_VERIFICATION_FAILED");
        when(errorHandling.path("defaultMessage")).thenReturn(defaultMessageNode);
        when(defaultMessageNode.asText(null)).thenReturn("Identity verification failed");
        
        when(execution.getVariable("failedStepId")).thenReturn("kyc-verification");
        when(execution.getVariable("errorMessage")).thenReturn(null);
        when(delegateUtils.getOnboarding(execution)).thenReturn(testOnboarding);
        when(workflowConfigurationService.getStepConfiguration("kyc-verification")).thenReturn(mockStepConfig);
        when(notificationService.sendFailureNotifications(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);

        // When
        genericErrorHandlerDelegate.execute(execution);

        // Then
        verify(notificationService).sendFailureNotifications(
                "john.doe@example.com",
                "+1234567890",
                "KYC_VERIFICATION_FAILED",
                "Identity verification failed",
                "Customer"
        );
        verify(execution).setVariable("errorType", "KYC_VERIFICATION_FAILED");
        verify(execution).setVariable("errorMessage", "Identity verification failed");
        verify(execution).setVariable("notificationSent", true);
    }

    @Test
    void testExecute_WithCustomErrorMessage() throws Exception {
        // Given
        String customErrorMessage = "Custom KYC error message";
        JsonNode mockStepConfig = mock(JsonNode.class);
        JsonNode errorHandling = mock(JsonNode.class);
        JsonNode errorTypeNode = mock(JsonNode.class);
        
        when(mockStepConfig.path("errorHandling")).thenReturn(errorHandling);
        when(errorHandling.path("errorType")).thenReturn(errorTypeNode);
        when(errorTypeNode.asText(null)).thenReturn("KYC_VERIFICATION_FAILED");
        
        when(execution.getVariable("failedStepId")).thenReturn("kyc-verification");
        when(execution.getVariable("errorMessage")).thenReturn(customErrorMessage);
        when(delegateUtils.getOnboarding(execution)).thenReturn(testOnboarding);
        when(workflowConfigurationService.getStepConfiguration("kyc-verification")).thenReturn(mockStepConfig);
        when(notificationService.sendFailureNotifications(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);

        // When
        genericErrorHandlerDelegate.execute(execution);

        // Then
        verify(notificationService).sendFailureNotifications(
                "john.doe@example.com",
                "+1234567890",
                "KYC_VERIFICATION_FAILED",
                customErrorMessage,
                "Customer"
        );
    }

    @Test
    void testExecute_WithLegacyFallback() throws Exception {
        // Given
        when(execution.getVariable("failedStepId")).thenReturn(null);
        when(execution.getVariable("kycResult")).thenReturn("FAILED");
        when(execution.getVariable("errorMessage")).thenReturn(null); // Add this line
        when(delegateUtils.getOnboarding(execution)).thenReturn(testOnboarding);
        when(notificationService.sendFailureNotifications(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);

        // When
        genericErrorHandlerDelegate.execute(execution);

        // Then
        verify(notificationService).sendFailureNotifications(
                "john.doe@example.com",
                "+1234567890",
                "KYC_VERIFICATION_FAILED",
                "Identity verification failed",
                "Customer"
        );
    }

    @Test
    void testExecute_OnboardingNotFound() {
        // Given
        when(delegateUtils.getOnboarding(execution)).thenThrow(new RuntimeException("Onboarding not found"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            genericErrorHandlerDelegate.execute(execution);
        });
        
        verify(notificationService, never()).sendFailureNotifications(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testExecute_MissingOnboardingId() {
        // Given
        when(delegateUtils.getOnboarding(execution)).thenThrow(new RuntimeException("Onboarding ID not found"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            genericErrorHandlerDelegate.execute(execution);
        });
        
        verify(notificationService, never()).sendFailureNotifications(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testExecute_NotificationFailure() throws Exception {
        // Given
        JsonNode mockStepConfig = mock(JsonNode.class);
        JsonNode errorHandling = mock(JsonNode.class);
        JsonNode errorTypeNode = mock(JsonNode.class);
        JsonNode defaultMessageNode = mock(JsonNode.class);
        
        when(mockStepConfig.path("errorHandling")).thenReturn(errorHandling);
        when(errorHandling.path("errorType")).thenReturn(errorTypeNode);
        when(errorTypeNode.asText(null)).thenReturn("KYC_VERIFICATION_FAILED");
        when(errorHandling.path("defaultMessage")).thenReturn(defaultMessageNode);
        when(defaultMessageNode.asText(null)).thenReturn("Identity verification failed");
        
        when(execution.getVariable("failedStepId")).thenReturn("kyc-verification");
        when(execution.getVariable("errorMessage")).thenReturn(null);
        when(delegateUtils.getOnboarding(execution)).thenReturn(testOnboarding);
        when(workflowConfigurationService.getStepConfiguration("kyc-verification")).thenReturn(mockStepConfig);
        when(notificationService.sendFailureNotifications(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(false);

        // When
        genericErrorHandlerDelegate.execute(execution);

        // Then
        verify(execution).setVariable("notificationSent", false);
    }
}
