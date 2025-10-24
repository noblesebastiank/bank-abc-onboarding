package com.bankabc.onboarding.util;

import com.bankabc.onboarding.model.Email;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.util.Map;

/**
 * Utility class for building email content using Freemarker templates.
 * Supports header/footer templates and Spring's FreeMarkerTemplateUtils.
 */
@Slf4j
@Component
public final class EmailUtil {

    public static final String HEADER_TEMPLATE = "Header.ftl";
    public static final String FOOTER_TEMPLATE = "Footer.ftl";
    public static final String THREAD_NAME_PREFIX = "Email-async-";
    
    @Value("${bank.website.url:https://www.bankabc.com}")
    private String bankWebsiteUrl;
    
    @Value("${bank.support.email:support@bankabc.com}")
    private String supportEmail;
    
    @Value("${bank.phone.number:1-800-BANK-ABC}")
    private String bankPhoneNumber;
    
    private final Configuration freemarkerConfig;

    /**
     * Builds email body content using Freemarker template or plain text.
     * 
     * @param email the email object containing template information
     * @return the processed email body content
     */
    public String buildEmailBody(Email email) {
        if (email == null) {
            log.warn("Email object is null, returning empty string");
            return "";
        }

        // If template name is provided, use Freemarker template processing
        if (StringUtils.isNotBlank(email.getTemplateName()) && freemarkerConfig != null) {
            try {
                return buildEmailBodyFromTemplate(email);
            } catch (Exception e) {
                log.error("Error building email body from template: {}", email.getTemplateName(), e);
                return email.getBody() != null ? email.getBody() : "";
            }
        }

        // If body is provided directly, use it
        if (StringUtils.isNotBlank(email.getBody())) {
            return email.getBody();
        }

        // Fallback to empty string
        log.warn("No template name or body provided for email, returning empty string");
        return "";
    }

    /**
     * Builds email body from Freemarker template with header/footer support.
     * 
     * @param email the email object
     * @return the processed template content
     * @throws Exception if template processing fails
     */
    private String buildEmailBodyFromTemplate(Email email) throws Exception {
        log.info("Building template - Starts: {}", email.getTemplateName());
        
        StringBuilder sb = new StringBuilder();
        String templateName = email.getTemplateName();
        Map<String, Object> templateModel = email.getTemplateVariables();
        
        if (templateModel == null) {
            log.warn("No template variables provided for template: {}", templateName);
            templateModel = Map.of();
        }
        
        // Add common template variables
        addCommonTemplateVariables(templateModel, templateName);
        
        // Process main template
        Template template = freemarkerConfig.getTemplate(templateName);
        String mainContent = FreeMarkerTemplateUtils.processTemplateIntoString(template, templateModel);
        
        // Check if we should use header/footer templates
        if (shouldUseHeaderFooter(templateName)) {
            String[] headerFooter = buildHeaderFooterTemplate(templateModel);
            sb.append(headerFooter[0]); // Header
            sb.append(mainContent);
            sb.append(headerFooter[1]); // Footer
        } else {
            sb.append(mainContent);
        }
        
        log.info("Building template - Ends: {}", templateName);
        return sb.toString();
    }

    /**
     * Builds a simple email body with basic formatting.
     * 
     * @param subject the email subject
     * @param content the email content
     * @param customerName the customer name
     * @return formatted email body
     */
    public static String buildSimpleEmailBody(String subject, String content, String customerName) {
        return String.format("""
                Subject: %s
                
                Dear %s,
                
                %s
                
                Best regards,
                Bank ABC Team
                """, subject, customerName, content);
    }

    /**
     * Builds an HTML email body with basic formatting.
     * 
     * @param subject the email subject
     * @param content the email content
     * @param customerName the customer name
     * @return formatted HTML email body
     */
    public static String buildHtmlEmailBody(String subject, String content, String customerName) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>%s</title>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #f4f4f4; padding: 20px; text-align: center; }
                        .content { padding: 20px; }
                        .footer { background-color: #f4f4f4; padding: 20px; text-align: center; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2>%s</h2>
                        </div>
                        <div class="content">
                            <p>Dear %s,</p>
                            <div>%s</div>
                        </div>
                        <div class="footer">
                            <p>Best regards,<br>Bank ABC Team</p>
                        </div>
                    </div>
                </body>
                </html>
                """, subject, subject, customerName, content);
    }

    /**
     * Builds header and footer templates.
     * 
     * @param templateModel the template variables
     * @return array with header and footer content
     * @throws Exception if template processing fails
     */
    public String[] buildHeaderFooterTemplate(Map<String, Object> templateModel) throws Exception {
        String header = "";
        String footer = "";
        
        try {
            Template templateHeader = freemarkerConfig.getTemplate(HEADER_TEMPLATE);
            header = FreeMarkerTemplateUtils.processTemplateIntoString(templateHeader, templateModel);
        } catch (Exception e) {
            log.warn("Header template not found or error processing: {}", HEADER_TEMPLATE, e);
        }
        
        try {
            Template templateFooter = freemarkerConfig.getTemplate(FOOTER_TEMPLATE);
            footer = FreeMarkerTemplateUtils.processTemplateIntoString(templateFooter, templateModel);
        } catch (Exception e) {
            log.warn("Footer template not found or error processing: {}", FOOTER_TEMPLATE, e);
        }
        
        return new String[]{header, footer};
    }

    /**
     * Determines if header/footer templates should be used for the given template.
     * 
     * @param templateName the template name
     * @return true if header/footer should be used
     */
    private static boolean shouldUseHeaderFooter(String templateName) {
        // Use header/footer for all templates except standalone ones
        return !templateName.toLowerCase().contains("standalone") && 
               !templateName.toLowerCase().contains("complete");
    }

    /**
     * Adds common template variables that are available to all templates.
     * 
     * @param templateModel the template variables map
     * @param templateName the template name
     */
    private void addCommonTemplateVariables(Map<String, Object> templateModel, String templateName) {
        // Add bank information
        if (bankWebsiteUrl != null) {
            templateModel.put("bankWebsiteUrl", bankWebsiteUrl);
        }
        if (supportEmail != null) {
            templateModel.put("supportEmail", supportEmail);
        }
        if (bankPhoneNumber != null) {
            templateModel.put("bankPhoneNumber", bankPhoneNumber);
        }
        
        // Add current timestamp
        templateModel.put("currentDate", java.time.LocalDate.now().toString());
        templateModel.put("currentTime", java.time.LocalTime.now().toString());
        
        // Template-specific variables
        checkTemplateSpecificVariables(templateName, templateModel);
    }

    /**
     * Adds template-specific variables based on template name.
     * 
     * @param templateName the template name
     * @param templateModel the template variables map
     */
    private void checkTemplateSpecificVariables(String templateName, Map<String, Object> templateModel) {
        if (templateName.equals("failure-notification.ftl")) {
            // Add failure-specific variables if not already present
            if (!templateModel.containsKey("bankName")) {
                templateModel.put("bankName", "Bank ABC");
            }
            if (!templateModel.containsKey("supportPhone")) {
                templateModel.put("supportPhone", "1-800-BANK-ABC");
            }
        }
        // Add more template-specific logic as needed
    }


    /**
     * Constructor for dependency injection.
     */
    public EmailUtil(Configuration freemarkerConfig) {
        this.freemarkerConfig = freemarkerConfig;
    }
}
