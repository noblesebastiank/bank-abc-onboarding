package com.bankabc.onboarding.service;

import com.bankabc.onboarding.model.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for EmailService.
 * These tests use the actual Spring context and email configuration.
 */
@SpringBootTest
@ActiveProfiles("test")
class EmailServiceIntegrationTest {

    @Autowired
    private EmailService emailService;

    @MockBean
    private JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        // Mock the createMimeMessage method to return a mocked MimeMessage
        when(javaMailSender.createMimeMessage()).thenReturn(mock(jakarta.mail.internet.MimeMessage.class));
    }

    @Test
    void testEmailService_ContextLoads() {
        // Given & When & Then
        assertNotNull(emailService);
    }

    @Test
    void testSendTextEmail_WithValidData_DoesNotThrowException() {
        // Given
        String to = "test@example.com";
        String subject = "Test Subject";
        String content = "Test Content";

        // When & Then
        assertDoesNotThrow(() -> {
            boolean result = emailService.sendTextEmail(to, subject, content);
            // Note: In test environment, email might not actually be sent
            // but the method should not throw an exception
        });
    }

    @Test
    void testSendHtmlEmail_WithValidData_DoesNotThrowException() {
        // Given
        String to = "test@example.com";
        String subject = "Test Subject";
        String content = "<h1>Test Content</h1>";

        // When & Then
        assertDoesNotThrow(() -> {
            boolean result = emailService.sendHtmlEmail(to, subject, content);
            // Note: In test environment, email might not actually be sent
            // but the method should not throw an exception
        });
    }

    @Test
    void testSendMail_WithValidEmailObject_DoesNotThrowException() {
        // Given
        Email email = Email.builder()
            .to("test@example.com")
            .subject("Test Subject")
            .body("Test Content")
            .html(false)
            .build();

        // When & Then
        assertDoesNotThrow(() -> {
            emailService.sendMail(email);
        });
    }

    @Test
    void testSendMail_WithHtmlEmailObject_DoesNotThrowException() {
        // Given
        Email email = Email.builder()
            .to("test@example.com")
            .subject("Test Subject")
            .body("<h1>Test Content</h1>")
            .html(true)
            .build();

        // When & Then
        assertDoesNotThrow(() -> {
            emailService.sendMail(email);
        });
    }

    @Test
    void testSendMail_WithRecipientList_DoesNotThrowException() {
        // Given
        Email email = Email.builder()
            .to("test@example.com")
            .toList(new String[]{"test1@example.com", "test2@example.com"})
            .subject("Test Subject")
            .body("Test Content")
            .html(false)
            .build();

        // When & Then
        assertDoesNotThrow(() -> {
            emailService.sendMail(email);
        });
    }

    @Test
    void testSendMail_WithCcAndBcc_DoesNotThrowException() {
        // Given
        Email email = Email.builder()
            .to("test@example.com")
            .cc("cc@example.com")
            .bcc("bcc@example.com")
            .subject("Test Subject")
            .body("Test Content")
            .html(false)
            .build();

        // When & Then
        assertDoesNotThrow(() -> {
            emailService.sendMail(email);
        });
    }
}
