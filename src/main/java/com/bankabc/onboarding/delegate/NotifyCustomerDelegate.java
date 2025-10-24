package com.bankabc.onboarding.delegate;

import com.bankabc.onboarding.entity.Onboarding;
import com.bankabc.onboarding.entity.Onboarding.OnboardingStatus;
import com.bankabc.onboarding.exception.DefaultApiError;
import com.bankabc.onboarding.exception.ErrorTypes;
import com.bankabc.onboarding.service.NotificationService;
import com.bankabc.onboarding.service.OnboardingService;
import com.bankabc.onboarding.constants.ApplicationConstants;
import com.bankabc.onboarding.util.DelegateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Component("notifyCustomerDelegate")
@RequiredArgsConstructor
@Slf4j
public class NotifyCustomerDelegate implements JavaDelegate {

    private final NotificationService notificationService;
    private final OnboardingService onboardingService;
    private final DelegateUtils delegateUtils;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) throws Exception {
        String processInstanceId = execution.getProcessInstanceId();
        log.info("Executing NotifyCustomerDelegate for processInstanceId: {}", processInstanceId);

        try {
            Onboarding onboarding = delegateUtils.getOnboarding(execution);
            UUID onboardingId = onboarding.getId();

            // Mark notification step started
            onboarding.setStatus(OnboardingStatus.NOTIFICATION_SENT);
            onboardingService.saveOnboarding(onboarding);

            // Send notifications
            boolean emailSent = sendEmailNotification(onboarding, onboardingId);
            boolean smsSent = sendSmsNotification(onboarding, onboardingId);

            // Decide final outcome
            if (emailSent && smsSent) {
                onboarding.setStatus(OnboardingStatus.COMPLETED);
                execution.setVariable(ApplicationConstants.ProcessVariables.NOTIFICATION_RESULT, ApplicationConstants.Workflow.KYC_RESULT_SUCCESS);
                log.info("All notifications sent successfully for onboardingId: {}", onboardingId);
            } else if (emailSent || smsSent) {
                onboarding.setStatus(OnboardingStatus.COMPLETED);
                execution.setVariable(ApplicationConstants.ProcessVariables.NOTIFICATION_RESULT, ApplicationConstants.Workflow.KYC_RESULT_FAILED);
                log.warn("Partial notification success for onboardingId: {} (emailSent={}, smsSent={})",
                    onboardingId, emailSent, smsSent);
            } else {
                onboarding.setStatus(OnboardingStatus.FAILED);
                execution.setVariable(ApplicationConstants.ProcessVariables.NOTIFICATION_RESULT, ApplicationConstants.Workflow.KYC_RESULT_FAILED);
                execution.setVariable(ApplicationConstants.ProcessVariables.ERROR_TYPE, ErrorTypes.NOTIFICATION_FAILED.name());
                execution.setVariable(ApplicationConstants.ProcessVariables.ERROR_MESSAGE, "Both email and SMS notifications failed");
                log.error("All notifications failed for onboardingId: {}", onboardingId);

                throw new DefaultApiError(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorTypes.NOTIFICATION_FAILED.name(),
                    "Unable to send any customer notifications",
                    Map.of("onboardingId", onboardingId.toString())
                );
            }


            onboardingService.saveOnboarding(onboarding);

            // Update process variables for BPMN
            execution.setVariable(ApplicationConstants.ProcessVariables.STATUS, onboarding.getStatus().name());
            execution.setVariable(ApplicationConstants.ProcessVariables.EMAIL_SENT, emailSent);
            execution.setVariable(ApplicationConstants.ProcessVariables.SMS_SENT, smsSent);
            execution.setVariable(ApplicationConstants.ProcessVariables.NOTIFICATION_TIMESTAMP, OffsetDateTime.now().toString());

        } catch (Exception e) {
            log.error("Error in NotifyCustomerDelegate for processInstanceId: {}", processInstanceId, e);
            execution.setVariable(ApplicationConstants.ProcessVariables.NOTIFICATION_RESULT, ApplicationConstants.Workflow.KYC_RESULT_ERROR);
            execution.setVariable(ApplicationConstants.ProcessVariables.ERROR_MESSAGE, e.getMessage());
            throw e; // propagate for BPMN boundary event
        }
    }

    private String buildEmailContent(Onboarding onboarding) {
        return String.format("""
                Dear %s %s,
                
                🎉 Congratulations! Your Bank ABC account has been successfully created.

                Account Details:
                • Account Number: %s
                • Email: %s
                • Phone: %s

                You can now start using your new account for online banking.

                Best regards,
                Bank ABC Team
                """,
            onboarding.getFirstName(),
            onboarding.getLastName(),
            onboarding.getAccountNumber(),
            onboarding.getEmail(),
            onboarding.getPhone()
        );
    }

    private String buildSmsContent(Onboarding onboarding) {
        return String.format("🎉 Welcome to Bank ABC! Your account %s has been created successfully. " +
            "You can now log in to online banking.", onboarding.getAccountNumber());
    }

    /**
     * Sends email notification to the customer.
     * 
     * @param onboarding the onboarding entity
     * @param onboardingId the onboarding ID for logging
     * @return true if email was sent successfully, false otherwise
     */
    private boolean sendEmailNotification(Onboarding onboarding, UUID onboardingId) {
        try {
            return notificationService.sendEmailNotification(
                onboarding.getEmail(),
                "Account Created Successfully",
                buildEmailContent(onboarding)
            );
        } catch (Exception e) {
            log.warn("Failed to send email for onboardingId: {}", onboardingId, e);
            return false;
        }
    }

    /**
     * Sends SMS notification to the customer.
     * 
     * @param onboarding the onboarding entity
     * @param onboardingId the onboarding ID for logging
     * @return true if SMS was sent successfully, false otherwise
     */
    private boolean sendSmsNotification(Onboarding onboarding, UUID onboardingId) {
        try {
            return notificationService.sendSmsNotification(
                onboarding.getPhone(),
                buildSmsContent(onboarding)
            );
        } catch (Exception e) {
            log.warn("Failed to send SMS for onboardingId: {}", onboardingId, e);
            return false;
        }
    }
}
