package com.bankabc.onboarding.service;

import com.bankabc.onboarding.exception.DefaultApiError;
import com.bankabc.onboarding.model.Email;
import com.bankabc.onboarding.util.EmailUtil;
import freemarker.template.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.internet.MimeMessage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailService.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private Configuration freemarkerConfig;

    @Mock
    private EmailUtil emailUtil;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private Email testEmail;

    @BeforeEach
    void setUp() {
        testEmail = Email.builder()
            .to("test@example.com")
            .subject("Test Subject")
            .body("Test Content")
            .html(false)
            .build();
    }

    @Test
    void testSendMail_WithSimpleEmail_Success() throws Exception {
        // Given
        testEmail.setHtml(false);
        when(emailUtil.buildEmailBody(any(Email.class))).thenReturn("Test Content");

        // When
        emailService.sendMail(testEmail);

        // Then
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendMail_WithHtmlEmail_Success() throws Exception {
        // Given
        testEmail.setHtml(true);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(emailUtil.buildEmailBody(any(Email.class))).thenReturn("Test HTML Content");

        // When
        emailService.sendMail(testEmail);

        // Then
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendMail_WithNullEmail_ThrowsException() {
        // When & Then
        assertThrows(DefaultApiError.class, () -> {
            emailService.sendMail(null);
        });
    }

    @Test
    void testSendMail_WithEmptyRecipient_ThrowsException() {
        // Given
        testEmail.setTo("");

        // When & Then
        assertThrows(DefaultApiError.class, () -> {
            emailService.sendMail(testEmail);
        });
    }

    @Test
    void testSendMailToRecipientList_WithSimpleEmail_Success() throws Exception {
        // Given
        testEmail.setToList(new String[]{"test1@example.com", "test2@example.com"});
        testEmail.setHtml(false);

        // When
        emailService.sendMail(testEmail);

        // Then
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendMailToRecipientList_WithHtmlEmail_Success() throws Exception {
        // Given
        testEmail.setToList(new String[]{"test1@example.com", "test2@example.com"});
        testEmail.setHtml(true);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(emailUtil.buildEmailBody(any(Email.class))).thenReturn("Test HTML Content");

        // When
        emailService.sendMail(testEmail);

        // Then
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendMailToRecipientList_WithEmptyRecipientList_ThrowsException() {
        // Given
        testEmail.setToList(null);
        testEmail.setTo(null);

        // When & Then
        assertThrows(DefaultApiError.class, () -> {
            emailService.sendMail(testEmail);
        });
    }

    @Test
    void testSendSimpleEmail_Success() {
        // Given
        String to = "test@example.com";
        String subject = "Test Subject";
        String content = "Test Content";
        boolean isHtml = false;

        // When
        boolean result = emailService.sendSimpleEmail(to, subject, content, isHtml);

        // Then
        assertTrue(result);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendSimpleEmail_WithException_ReturnsFalse() throws Exception {
        // Given
        String to = "test@example.com";
        String subject = "Test Subject";
        String content = "Test Content";
        boolean isHtml = false;
        
        doThrow(new RuntimeException("Mail sending failed"))
            .when(mailSender).send(any(SimpleMailMessage.class));

        // When
        boolean result = emailService.sendSimpleEmail(to, subject, content, isHtml);

        // Then
        assertFalse(result);
    }

    @Test
    void testSendTextEmail_Success() {
        // Given
        String to = "test@example.com";
        String subject = "Test Subject";
        String content = "Test Content";

        // When
        boolean result = emailService.sendTextEmail(to, subject, content);

        // Then
        assertTrue(result);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendHtmlEmail_Success() {
        // Given
        String to = "test@example.com";
        String subject = "Test Subject";
        String content = "Test Content";
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        boolean result = emailService.sendHtmlEmail(to, subject, content);

        // Then
        assertTrue(result);
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendHtmlEmail_WithException_ReturnsFalse() throws Exception {
        // Given
        String to = "test@example.com";
        String subject = "Test Subject";
        String content = "Test Content";
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("Mail sending failed"))
            .when(mailSender).send(any(MimeMessage.class));

        // When
        boolean result = emailService.sendHtmlEmail(to, subject, content);

        // Then
        assertFalse(result);
    }
}
