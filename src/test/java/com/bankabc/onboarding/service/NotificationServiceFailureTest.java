package com.bankabc.onboarding.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for NotificationService failure notification functionality.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceFailureTest {

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void testSendFailureEmailNotification() {
        // Given
        String email = "test@example.com";
        String failureType = "KYC_VERIFICATION_FAILED";
        String errorMessage = "Identity verification failed";
        String customerName = "John Doe";

        // When
        boolean result = notificationService.sendFailureEmailNotification(email, failureType, errorMessage, customerName);

        // Then
        assertTrue(result, "Failure email notification should be sent successfully");
    }

    @Test
    void testSendFailureSmsNotification() {
        // Given
        String phone = "+1234567890";
        String failureType = "ADDRESS_VERIFICATION_FAILED";
        String errorMessage = "Address verification failed";

        // When
        boolean result = notificationService.sendFailureSmsNotification(phone, failureType, errorMessage);

        // Then
        assertTrue(result, "Failure SMS notification should be sent successfully");
    }

    @Test
    void testSendFailureNotifications() {
        // Given
        String email = "test@example.com";
        String phone = "+1234567890";
        String failureType = "ACCOUNT_CREATION_FAILED";
        String errorMessage = "Account creation failed";
        String customerName = "Jane Smith";

        // When
        boolean result = notificationService.sendFailureNotifications(email, phone, failureType, errorMessage, customerName);

        // Then
        assertTrue(result, "Both failure notifications should be sent successfully");
    }

    @Test
    void testSendFailureNotificationsWithInvalidEmail() {
        // Given
        String email = "invalid-email";
        String phone = "+1234567890";
        String failureType = "GENERAL_FAILURE";
        String errorMessage = "General failure";
        String customerName = "Test User";

        // When
        boolean result = notificationService.sendFailureNotifications(email, phone, failureType, errorMessage, customerName);

        // Then
        assertTrue(result, "Notifications should still be sent even with invalid email format");
    }

    @Test
    void testSendFailureNotificationsWithNullParameters() {
        // Given
        String email = "test@example.com";
        String phone = "+1234567890";
        String failureType = "DOCUMENT_UPLOAD_FAILED";
        String errorMessage = null;
        String customerName = null;

        // When
        boolean result = notificationService.sendFailureNotifications(email, phone, failureType, errorMessage, customerName);

        // Then
        assertTrue(result, "Notifications should handle null parameters gracefully");
    }
}
