package com.bankabc.onboarding.service;

import com.bankabc.onboarding.exception.DefaultApiError;
import com.bankabc.onboarding.exception.ErrorTypes;
import com.bankabc.onboarding.model.Email;
import com.bankabc.onboarding.util.EmailUtil;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * Service for sending emails using Spring Boot Mail and Freemarker templates.
 * Supports both simple text and HTML emails with attachments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailUtil emailUtil;

    /**
     * Functional interface to unify setting recipients and sender.
     */
    @FunctionalInterface
    private interface EmailSetter {
        void set(String[] to, String[] cc, String[] bcc, String from) throws Exception;
    }

    /**
     * Sends email asynchronously (single recipient or multiple recipients).
     */
    @Async
    public void sendMail(Email email) throws Exception {
        if (email == null) {
            log.error("Email object is null, cannot send email");
            throw new DefaultApiError(
                HttpStatus.BAD_REQUEST,
                "EMAIL_VALIDATION_ERROR",
                "Email object cannot be null"
            );
        }

      sendEmail(email, email.isHtml());
    }

    /**
     * Unified method for sending HTML or plain text emails.
     */
    void sendEmail(Email email, boolean isHtml) throws Exception {
        validateEmailContent(email);
        
        if (isHtml) {
            sendHtmlEmail(email);
        } else {
            sendPlainTextEmail(email);
        }
        
        cleanupAttachment(email);
    }

    /**
     * Sends HTML email with attachments support.
     */
    private void sendHtmlEmail(Email email) throws Exception {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        configureEmailRecipients(email, helper);
        helper.setSubject(email.getSubject());
        
        // Use template-based content if available, otherwise use simple body content
        String emailBody = emailUtil.buildEmailBody(email);
        if (emailBody == null || emailBody.isEmpty()) {
            emailBody = email.getBody();
        }
        helper.setText(emailBody, true);
        addAttachmentIfPresent(email, helper);

        mailSender.send(mimeMessage);
        logEmailSent("HTML", email);
    }

    /**
     * Sends plain text email.
     */
    private void sendPlainTextEmail(Email email)  {
        SimpleMailMessage mailMessage = new SimpleMailMessage();

        configureEmailRecipients(email, mailMessage);
        mailMessage.setSubject(email.getSubject());
        mailMessage.setText(emailUtil.buildEmailBody(email));

        mailSender.send(mailMessage);
        logEmailSent("Simple", email);
    }

    /**
     * Configures recipients for MimeMessageHelper.
     */
    private void configureEmailRecipients(Email email, MimeMessageHelper helper) throws Exception {
        setEmailRecipientsAndSender(email, (to, cc, bcc, from) -> {
            helper.setTo(to);
            if (cc != null) helper.setCc(cc);
            if (bcc != null) helper.setBcc(bcc);
            if (from != null) helper.setFrom(from);
        });
    }

    /**
     * Configures recipients for SimpleMailMessage.
     */
    private void configureEmailRecipients(Email email, SimpleMailMessage mailMessage) {
        try {
            setEmailRecipientsAndSender(email, (to, cc, bcc, from) -> {
                mailMessage.setTo(to);
                if (cc != null) mailMessage.setCc(cc);
                if (bcc != null) mailMessage.setBcc(bcc);
                if (from != null) mailMessage.setFrom(from);
            });
        } catch (Exception e) {
            throw new DefaultApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "EMAIL_CONFIGURATION_ERROR",
                "Failed to configure email recipients",
                e.getMessage()
            );
        }
    }

    /**
     * Adds attachment to MimeMessageHelper if present.
     */
    private void addAttachmentIfPresent(Email email, MimeMessageHelper helper) throws Exception {
        if (email.getAttachment() != null) {
            String attachmentName = StringUtils.isNotBlank(email.getAttachmentName())
                ? email.getAttachmentName()
                : email.getAttachment().getName();
            helper.addAttachment(attachmentName, email.getAttachment());
        }
    }

    /**
     * Logs email sent message.
     */
    private void logEmailSent(String emailType, Email email) {
        boolean hasRecipientList = ArrayUtils.isNotEmpty(email.getToList());
        String recipient = hasRecipientList ? "recipient list" : email.getTo();
        log.info("{} Email sent successfully to {}", emailType, recipient);
    }

    /**
     * Cleans up temporary attachment file.
     */
    private void cleanupAttachment(Email email) {
        if (email.getAttachment() != null) {
            File file = email.getAttachment();
            try {
                java.nio.file.Files.delete(file.toPath());
            } catch (java.nio.file.NoSuchFileException e) {
                log.debug("Attachment file already deleted: {}", file.getName());
            } catch (java.nio.file.AccessDeniedException e) {
                log.warn("Access denied when deleting attachment: {} - {}", file.getName(), e.getMessage());
            } catch (java.io.IOException e) {
                log.warn("Failed to delete attachment: {} - {}", file.getName(), e.getMessage());
            }
        }
    }

    /**
     * Sends a simple email with basic parameters.
     */
    public boolean sendSimpleEmail(String to, String subject, String content, boolean isHtml) {
        try {
            Email email = Email.builder()
                .to(to)
                .subject(subject)
                .body(content)
                .html(isHtml)
                .build();

            sendEmail(email, isHtml);
            return true;
        } catch (Exception e) {
            log.error("Error sending simple email to: {}", to, e);
            return false;
        }
    }

    public boolean sendTextEmail(String to, String subject, String content) {
        return sendSimpleEmail(to, subject, content, false);
    }

    public boolean sendHtmlEmail(String to, String subject, String content) {
        return sendSimpleEmail(to, subject, content, true);
    }

    /**
     * Sets recipients and sender using a functional interface.
     */
    private void setEmailRecipientsAndSender(Email email, EmailSetter setter) throws Exception {
        String[] to = determineRecipients(email.getToList(), email.getTo());
        if (to == null) {
            throw new DefaultApiError(
                HttpStatus.BAD_REQUEST,
                "EMAIL_VALIDATION_ERROR",
                "No recipient specified"
            );
        }

        String[] cc = determineRecipients(email.getCcList(), email.getCc());
        String[] bcc = determineRecipients(email.getBccList(), email.getBcc());
        String from = StringUtils.isNotBlank(email.getFrom()) ? email.getFrom() : null;

        setter.set(to, cc, bcc, from);
    }

    /**
     * Determines recipients from either list or single recipient.
     */
    private String[] determineRecipients(String[] recipientList, String singleRecipient) {
        if (ArrayUtils.isNotEmpty(recipientList)) {
            return recipientList;
        }
        if (StringUtils.isNotBlank(singleRecipient)) {
            return new String[]{singleRecipient};
        }
        return null;
    }

    /**
     * Validates email content to prevent SMTP issues.
     */
    private void validateEmailContent(Email email) {
        if (email == null) {
            throw new DefaultApiError(
                HttpStatus.BAD_REQUEST,
                "EMAIL_VALIDATION_ERROR",
                "Email object cannot be null"
            );
        }

        if (StringUtils.isBlank(email.getTo()) && ArrayUtils.isEmpty(email.getToList())) {
            throw new DefaultApiError(
                HttpStatus.BAD_REQUEST,
                "EMAIL_VALIDATION_ERROR",
                "Email recipient is required"
            );
        }

        if (StringUtils.isBlank(email.getSubject())) {
            throw new DefaultApiError(
                HttpStatus.BAD_REQUEST,
                "EMAIL_VALIDATION_ERROR",
                "Email subject is required"
            );
        }

        String emailBody = emailUtil.buildEmailBody(email);
        if (StringUtils.isBlank(emailBody)) {
            log.warn("Email body is empty, this might cause issues with SMTP server");
        }

        if (emailBody != null && emailBody.length() > 1_000_000) {
            log.warn("Email body is very large ({} characters)", emailBody.length());
        }

        if (email.getAttachment() != null) {
            File attachment = email.getAttachment();
            if (!attachment.exists()) {
                throw new DefaultApiError(
                    HttpStatus.BAD_REQUEST,
                    "EMAIL_VALIDATION_ERROR",
                    "Attachment file does not exist: " + attachment.getPath()
                );
            }
            if (attachment.length() > 10_485_760) {
                throw new DefaultApiError(
                    HttpStatus.BAD_REQUEST,
                    "EMAIL_VALIDATION_ERROR",
                    "Attachment file is too large: " + attachment.length() + " bytes"
                );
            }
        }

        log.debug("Email validation passed for recipient: {}", email.getTo());
    }
}
