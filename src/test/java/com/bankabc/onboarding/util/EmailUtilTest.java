package com.bankabc.onboarding.util;

import com.bankabc.onboarding.model.Email;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailUtil.
 */
@ExtendWith(MockitoExtension.class)
class EmailUtilTest {

    @Mock
    private Configuration freemarkerConfig;

    @Mock
    private Template template;

    private Email testEmail;
    private EmailUtil emailUtil;

    @BeforeEach
    void setUp() {
        testEmail = Email.builder()
            .to("test@example.com")
            .subject("Test Subject")
            .body("Test Content")
            .build();
        
        // Create EmailUtil instance and set configuration values for testing
        emailUtil = new EmailUtil(freemarkerConfig);
        ReflectionTestUtils.setField(emailUtil, "bankWebsiteUrl", "https://test.bankabc.com");
        ReflectionTestUtils.setField(emailUtil, "supportEmail", "test-support@bankabc.com");
        ReflectionTestUtils.setField(emailUtil, "bankPhoneNumber", "1-800-TEST-ABC");
    }

    @Test
    void testBuildEmailBody_WithDirectBody_ReturnsBody() {
        // Given
        String expectedBody = "Test Email Body";
        testEmail.setBody(expectedBody);

        // When
        String result = emailUtil.buildEmailBody(testEmail);

        // Then
        assertEquals(expectedBody, result);
    }

    @Test
    void testBuildEmailBody_WithTemplateName_Success() throws Exception {
        // Given
        String templateName = "test-template.ftl";
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("name", "John Doe");

        testEmail.setTemplateName(templateName);
        testEmail.setTemplateVariables(templateVariables);
        testEmail.setBody(null);

        when(freemarkerConfig.getTemplate(templateName)).thenReturn(template);
        when(freemarkerConfig.getTemplate("Header.ftl")).thenReturn(template);
        when(freemarkerConfig.getTemplate("Footer.ftl")).thenReturn(template);

        // When
        String result = emailUtil.buildEmailBody(testEmail);

        // Then
        assertNotNull(result);
        verify(freemarkerConfig).getTemplate(templateName);
    }

    @Test
    void testBuildEmailBody_WithTemplateName_IOException_ReturnsEmptyString() throws Exception {
        // Given
        String templateName = "test-template.ftl";
        testEmail.setTemplateName(templateName);
        testEmail.setBody(null);

        // When
        String result = emailUtil.buildEmailBody(testEmail);

        // Then
        assertEquals("", result);
    }

    @Test
    void testBuildEmailBody_WithTemplateName_TemplateException_ReturnsEmptyString() throws Exception {
        // Given
        String templateName = "test-template.ftl";
        testEmail.setTemplateName(templateName);
        testEmail.setBody(null);

        // When
        String result = emailUtil.buildEmailBody(testEmail);

        // Then
        assertEquals("", result);
    }

    @Test
    void testBuildEmailBody_WithNullEmail_ReturnsEmptyString() {
        // When
        String result = emailUtil.buildEmailBody(null);

        // Then
        assertEquals("", result);
    }

    @Test
    void testBuildEmailBody_WithNoTemplateOrBody_ReturnsEmptyString() {
        // Given
        testEmail.setTemplateName(null);
        testEmail.setBody(null);

        // When
        String result = emailUtil.buildEmailBody(testEmail);

        // Then
        assertEquals("", result);
    }

    @Test
    void testBuildSimpleEmailBody_Success() {
        // Given
        String subject = "Test Subject";
        String content = "Test Content";
        String customerName = "John Doe";

        // When
        String result = EmailUtil.buildSimpleEmailBody(subject, content, customerName);

        // Then
        assertNotNull(result);
        assertTrue(result.contains(subject));
        assertTrue(result.contains(content));
        assertTrue(result.contains(customerName));
        assertTrue(result.contains("Bank ABC Team"));
    }

    @Test
    void testBuildHtmlEmailBody_Success() {
        // Given
        String subject = "Test Subject";
        String content = "Test Content";
        String customerName = "John Doe";

        // When
        String result = EmailUtil.buildHtmlEmailBody(subject, content, customerName);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("<html>"));
        assertTrue(result.contains("<head>"));
        assertTrue(result.contains("<body>"));
        assertTrue(result.contains(subject));
        assertTrue(result.contains(content));
        assertTrue(result.contains(customerName));
        assertTrue(result.contains("Bank ABC Team"));
    }

    @Test
    void testBuildHtmlEmailBody_ContainsProperHtmlStructure() {
        // Given
        String subject = "Test Subject";
        String content = "Test Content";
        String customerName = "John Doe";

        // When
        String result = EmailUtil.buildHtmlEmailBody(subject, content, customerName);

        // Then
        assertTrue(result.contains("<!DOCTYPE html>"));
        assertTrue(result.contains("<meta charset=\"UTF-8\">"));
        assertTrue(result.contains("<title>"));
        assertTrue(result.contains("<style>"));
        assertTrue(result.contains("font-family: Arial, sans-serif"));
        assertTrue(result.contains("</html>"));
    }

    @Test
    void testBuildHeaderFooterTemplate_Success() throws Exception {
        // Given
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("subject", "Test Subject");
        
        when(freemarkerConfig.getTemplate("Header.ftl")).thenReturn(template);
        when(freemarkerConfig.getTemplate("Footer.ftl")).thenReturn(template);

        // When
        String[] result = emailUtil.buildHeaderFooterTemplate(templateModel);

        // Then
        assertNotNull(result);
        assertEquals(2, result.length);
        assertNotNull(result[0]); // Header
        assertNotNull(result[1]); // Footer
        verify(freemarkerConfig).getTemplate("Header.ftl");
        verify(freemarkerConfig).getTemplate("Footer.ftl");
    }

    @Test
    void testBuildHeaderFooterTemplate_WithException_ReturnsEmptyStrings() throws Exception {
        // Given
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("subject", "Test Subject");
        
        when(freemarkerConfig.getTemplate("Header.ftl")).thenThrow(new RuntimeException("Template not found"));
        when(freemarkerConfig.getTemplate("Footer.ftl")).thenThrow(new RuntimeException("Template not found"));

        // When
        String[] result = emailUtil.buildHeaderFooterTemplate(templateModel);

        // Then
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals("", result[0]); // Header should be empty
        assertEquals("", result[1]); // Footer should be empty
    }
}
