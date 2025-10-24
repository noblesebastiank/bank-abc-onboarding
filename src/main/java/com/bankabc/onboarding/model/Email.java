package com.bankabc.onboarding.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.util.Map;

/**
 * Model class for email data structure.
 * Supports both simple and HTML emails with attachments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Email {
    
    /**
     * Primary recipient email address
     */
    private String to;
    
    /**
     * List of primary recipients (for bulk emails)
     */
    private String[] toList;
    
    /**
     * CC recipient email address
     */
    private String cc;
    
    /**
     * List of CC recipients
     */
    private String[] ccList;
    
    /**
     * BCC recipient email address
     */
    private String bcc;
    
    /**
     * List of BCC recipients
     */
    private String[] bccList;
    
    /**
     * Sender email address
     */
    private String from;
    
    /**
     * Email subject
     */
    private String subject;
    
    /**
     * Email body content
     */
    private String body;
    
    /**
     * Template name for Freemarker template processing
     */
    private String templateName;
    
    /**
     * Template variables for Freemarker processing
     */
    private Map<String, Object> templateVariables;
    
    /**
     * Whether the email content is HTML
     */
    private boolean html;
    
    /**
     * Attachment file
     */
    private File attachment;
    
    /**
     * Attachment file name (if different from actual file name)
     */
    private String attachmentName;
}
