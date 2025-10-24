package com.bankabc.onboarding.service;

import com.bankabc.onboarding.model.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.bankabc.onboarding.constants.ApplicationConstants;

/**
 * Service for handling notifications during the onboarding process.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {


    private final EmailService emailService;

    /**
     * Sends email notification.
     * 
     * @param email the recipient email address
     * @param subject the email subject
     * @param content the email content
     * @return true if sent successfully, false otherwise
     */
    public boolean sendEmailNotification(String email, String subject, String content) {
        log.info("Sending email notification with subject: {}", subject);
        
        try {
            // Create Email object
            Email emailObj = Email.builder()
                .to(email)
                .subject(subject)
                .body(content)
                .html(true) // Use HTML for better formatting
                .build();
            
            // Send email using EmailService
            emailService.sendMail(emailObj);
            
            log.info("Email notification sent successfully to: {}", email);
            return true;
            
        } catch (Exception e) {
            log.error("Error sending email notification to: {}", email, e);
            return false;
        }
    }

    /**
     * Sends SMS notification.
     * 
     * @param phone the recipient phone number
     * @param message the SMS message
     * @return true if sent successfully, false otherwise
     */
    public boolean sendSmsNotification(String phone, String message) {
        log.info("Sending SMS notification");
        
        try {
            // Log SMS for demo purposes
            log.info("SMS NOTIFICATION SENT:\nMessage: {}", message);
            
            // Future: Send actual SMS
            // return smsService.sendSms(phone, message);
            
            return true;
            
        } catch (Exception e) {
            log.error("Error sending SMS notification", e);
            return false;
        }
    }

    /**
     * Sends failure notification email to customer.
     * 
     * @param email the recipient email address
     * @param failureType the type of failure that occurred
     * @param errorMessage the specific error message
     * @param customerName the customer's name
     * @return true if sent successfully, false otherwise
     */
    public boolean sendFailureEmailNotification(String email, String failureType, String errorMessage, String customerName) {
        log.info("Sending failure email notification for failure type: {}", failureType);
        
        try {
            String subject = "Onboarding Process Update - " + getFailureSubject(failureType);
            String content = buildFailureEmailContent(failureType, errorMessage, customerName);
            
            // Create Email object with HTML content
            Email emailObj = Email.builder()
                .to(email)
                .subject(subject)
                .body(content)
                .html(true)
                .build();
            
            // Send email using EmailService
            emailService.sendMail(emailObj);
            
            log.info("Failure email notification sent successfully to: {}", email);
            return true;
            
        } catch (Exception e) {
            log.error("Error sending failure email notification to: {}", email, e);
            return false;
        }
    }

    /**
     * Sends failure notification SMS to customer.
     * 
     * @param phone the recipient phone number
     * @param failureType the type of failure that occurred
     * @param errorMessage the specific error message
     * @return true if sent successfully, false otherwise
     */
    public boolean sendFailureSmsNotification(String phone, String failureType, String errorMessage) {
        log.info("Sending failure SMS notification for failure type: {}", failureType);
        
        try {
            String message = buildFailureSmsContent(failureType, errorMessage);
            
            // Log SMS for demo purposes
            log.info("FAILURE SMS NOTIFICATION SENT:\nMessage: {}", message);
            
            // Future: Send actual SMS
            // return smsService.sendSms(phone, message);
            
            return true;
            
        } catch (Exception e) {
            log.error("Error sending failure SMS notification", e);
            return false;
        }
    }

    /**
     * Sends comprehensive failure notifications (both email and SMS).
     * 
     * @param email the recipient email address
     * @param phone the recipient phone number
     * @param failureType the type of failure that occurred
     * @param errorMessage the specific error message
     * @param customerName the customer's name
     * @return true if both notifications sent successfully, false otherwise
     */
    public boolean sendFailureNotifications(String email, String phone, String failureType, 
                                         String errorMessage, String customerName) {
        log.info("Sending comprehensive failure notifications for failure type: {}", failureType);
        
        boolean emailSent = sendFailureEmailNotification(email, failureType, errorMessage, customerName);
        boolean smsSent = sendFailureSmsNotification(phone, failureType, errorMessage);
        
        return emailSent && smsSent;
    }

    /**
     * Gets the appropriate subject line for failure type.
     * 
     * @param failureType the type of failure
     * @return the subject line
     */
    private String getFailureSubject(String failureType) {
        return switch (failureType.toUpperCase()) {
            case ApplicationConstants.ErrorType.KYC_VERIFICATION_FAILED -> ApplicationConstants.Notification.KYC_FAILED_SUBJECT;
            case ApplicationConstants.ErrorType.ADDRESS_VERIFICATION_FAILED -> ApplicationConstants.Notification.ADDRESS_FAILED_SUBJECT;
            case ApplicationConstants.ErrorType.ACCOUNT_CREATION_FAILED -> ApplicationConstants.Notification.ACCOUNT_FAILED_SUBJECT;
            case ApplicationConstants.ErrorType.DOCUMENT_UPLOAD_FAILED -> ApplicationConstants.Notification.DOCUMENT_FAILED_SUBJECT;
            case ApplicationConstants.ErrorType.GENERAL_FAILURE -> ApplicationConstants.Notification.GENERAL_FAILED_SUBJECT;
            default -> ApplicationConstants.Notification.GENERAL_FAILED_SUBJECT;
        };
    }

    /**
     * Builds failure email content based on failure type.
     * 
     * @param failureType the type of failure
     * @param errorMessage the specific error message
     * @param customerName the customer's name
     * @return the email content
     */
    private String buildFailureEmailContent(String failureType, String errorMessage, String customerName) {
        return switch (failureType.toUpperCase()) {
            case ApplicationConstants.ErrorType.KYC_VERIFICATION_FAILED -> String.format("""
                    Dear %s,
                    
                    We encountered an issue during your identity verification process.
                    
                    Issue: %s
                    
                    Next Steps:
                    - Please ensure your passport and photo documents are clear and readable
                    - Make sure all personal information matches your official documents
                    - Contact our support team if you need assistance
                    
                    You can restart the verification process by logging into your account.
                    
                    If you have any questions, please contact our support team.
                    
                    Best regards,
                    Bank ABC Team
                    """, customerName, errorMessage);
                    
            case ApplicationConstants.ErrorType.ADDRESS_VERIFICATION_FAILED -> String.format("""
                    Dear %s,
                    
                    We were unable to verify your residential address.
                    
                    Issue: %s
                    
                    Next Steps:
                    - Please ensure your address information is complete and accurate
                    - Verify that your address is a residential address (not a P.O. Box)
                    - Contact our support team if you need assistance updating your address
                    
                    You can update your address information by logging into your account.
                    
                    If you have any questions, please contact our support team.
                    
                    Best regards,
                    Bank ABC Team
                    """, customerName, errorMessage);
                    
            case ApplicationConstants.ErrorType.ACCOUNT_CREATION_FAILED -> String.format("""
                    Dear %s,
                    
                    We encountered a technical issue while creating your bank account.
                    
                    Issue: %s
                    
                    Next Steps:
                    - Our technical team has been notified of this issue
                    - We will attempt to resolve this within 24 hours
                    - You will receive another notification once your account is created
                    - Contact our support team if you need immediate assistance
                    
                    We apologize for any inconvenience caused.
                    
                    Best regards,
                    Bank ABC Team
                    """, customerName, errorMessage);
                    
            case ApplicationConstants.ErrorType.DOCUMENT_UPLOAD_FAILED -> String.format("""
                    Dear %s,
                    
                    We encountered an issue processing your uploaded documents.
                    
                    Issue: %s
                    
                    Next Steps:
                    - Please ensure your documents are in the correct format (JPEG, PNG, or PDF)
                    - Make sure file sizes are under 10MB
                    - Ensure documents are clear and all text is readable
                    - Try uploading your documents again
                    
                    You can upload your documents by logging into your account.
                    
                    If you have any questions, please contact our support team.
                    
                    Best regards,
                    Bank ABC Team
                    """, customerName, errorMessage);
                    
            default -> String.format("""
                    Dear %s,
                    
                    We encountered an issue during your onboarding process.
                    
                    Issue: %s
                    
                    Next Steps:
                    - Please review your information and try again
                    - Contact our support team if you need assistance
                    - You can restart the process by logging into your account
                    
                    If you have any questions, please contact our support team.
                    
                    Best regards,
                    Bank ABC Team
                    """, customerName, errorMessage);
        };
    }

    /**
     * Builds failure SMS content based on failure type.
     * 
     * @param failureType the type of failure
     * @param errorMessage the specific error message
     * @return the SMS content
     */
    private String buildFailureSmsContent(String failureType, String errorMessage) {
        return switch (failureType.toUpperCase()) {
            case ApplicationConstants.ErrorType.KYC_VERIFICATION_FAILED -> ApplicationConstants.Messages.KYC_FAILED_SMS;
            case ApplicationConstants.ErrorType.ADDRESS_VERIFICATION_FAILED -> ApplicationConstants.Messages.ADDRESS_FAILED_SMS;
            case ApplicationConstants.ErrorType.ACCOUNT_CREATION_FAILED -> ApplicationConstants.Messages.ACCOUNT_FAILED_SMS;
            case ApplicationConstants.ErrorType.DOCUMENT_UPLOAD_FAILED -> ApplicationConstants.Messages.DOCUMENT_FAILED_SMS;
            default -> 
                "Bank ABC: Onboarding process issue. Please contact support or try again later.";
        };
    }
}

