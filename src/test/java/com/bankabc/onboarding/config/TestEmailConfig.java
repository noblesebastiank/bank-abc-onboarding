package com.bankabc.onboarding.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import jakarta.mail.internet.MimeMessage;

import static org.mockito.Mockito.*;

/**
 * Test configuration for email services.
 * Provides mock mail senders to prevent actual email sending during tests.
 */
@TestConfiguration
public class TestEmailConfig {

    @Bean
    @Primary
    public MailSender mockMailSender() {
        return mock(MailSender.class);
    }

    @Bean
    @Primary
    public JavaMailSender mockJavaMailSender() {
        JavaMailSender mockSender = mock(JavaMailSender.class);
        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(mockSender.createMimeMessage()).thenReturn(mockMimeMessage);
        return mockSender;
    }
}
