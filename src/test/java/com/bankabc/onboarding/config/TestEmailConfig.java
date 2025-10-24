package com.bankabc.onboarding.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test configuration for email integration tests.
 * Provides a mocked JavaMailSender to prevent actual email sending during tests.
 */
@TestConfiguration
public class TestEmailConfig {

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        JavaMailSender mockSender = mock(JavaMailSender.class);
        
        // Mock the createMimeMessage method to return a mocked MimeMessage
        when(mockSender.createMimeMessage()).thenReturn(mock(jakarta.mail.internet.MimeMessage.class));
        
        return mockSender;
    }
}