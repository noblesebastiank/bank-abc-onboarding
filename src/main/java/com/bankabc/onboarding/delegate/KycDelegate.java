package com.bankabc.onboarding.delegate;

import com.bankabc.onboarding.entity.Onboarding;
import com.bankabc.onboarding.entity.Onboarding.OnboardingStatus;
import com.bankabc.onboarding.exception.DefaultApiError;
import com.bankabc.onboarding.exception.ErrorTypes;
import com.bankabc.onboarding.service.OnboardingService;
import com.bankabc.onboarding.service.VerificationService;
import com.bankabc.onboarding.constants.ApplicationConstants;
import com.bankabc.onboarding.util.DelegateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Component("kycDelegate")
@RequiredArgsConstructor
@Slf4j
public class KycDelegate implements JavaDelegate {

    private final VerificationService verificationService;
    private final OnboardingService onboardingService;
    private final DelegateUtils delegateUtils;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String processInstanceId = execution.getProcessInstanceId();
        log.info("Executing KycDelegate for processInstanceId: {}", processInstanceId);

        try {
            Onboarding onboarding = delegateUtils.getOnboarding(execution);
            UUID onboardingId = onboarding.getId();

            // Update onboarding status
            onboarding.setStatus(OnboardingStatus.KYC_IN_PROGRESS);
            onboardingService.saveOnboarding(onboarding);

            // Perform actual verification
            boolean kycResult = verificationService.performKycVerification(
                onboarding.getFirstName(),
                onboarding.getLastName(),
                onboarding.getDateOfBirth(),
                onboarding.getSsn(),
                onboarding.getPassportPath(),
                onboarding.getPhotoPath()
            );

            if (kycResult) {
                onboarding.setStatus(OnboardingStatus.KYC_COMPLETED);
                onboarding.setKycVerified(true);
                onboardingService.saveOnboarding(onboarding);

                execution.setVariable(ApplicationConstants.ProcessVariables.KYC_RESULT, ApplicationConstants.Workflow.KYC_RESULT_SUCCESS);
                execution.setVariable(ApplicationConstants.ProcessVariables.KYC_VERIFIED, ApplicationConstants.Workflow.KYC_VERIFIED_TRUE);
                execution.setVariable(ApplicationConstants.ProcessVariables.STATUS, OnboardingStatus.KYC_COMPLETED.name());

                log.info("✅ KYC verification successful for onboardingId: {}", onboardingId);

            } else {
                // Don't set global status to FAILED - let document upload step remain successful
                onboarding.setKycVerified(false);
                onboardingService.saveOnboarding(onboarding);

                execution.setVariable(ApplicationConstants.ProcessVariables.KYC_RESULT, ApplicationConstants.Workflow.KYC_RESULT_FAILED);
                execution.setVariable(ApplicationConstants.ProcessVariables.KYC_VERIFIED, ApplicationConstants.Workflow.KYC_VERIFIED_FALSE);
                execution.setVariable(ApplicationConstants.ProcessVariables.ERROR_TYPE, ErrorTypes.KYC_VERIFICATION_FAILED.name());
                execution.setVariable(ApplicationConstants.ProcessVariables.ERROR_MESSAGE, ApplicationConstants.Workflow.KYC_VERIFICATION_FAILED_MSG);
                execution.setVariable(ApplicationConstants.ProcessVariables.FAILED_STEP_ID, ApplicationConstants.Workflow.FAILED_STEP_KYC);

                log.warn("KYC verification failed for onboardingId: {}", onboardingId);

                // Trigger BPMN error boundary event
                throw new BpmnError("GENERIC_ERROR", ApplicationConstants.Workflow.KYC_VERIFICATION_FAILED_MSG);
            }

        } catch (BpmnError e) {
            // Re-throw BPMN errors to trigger boundary events
            throw e;
        } catch (Exception e) {
            log.error("Error during KYC verification (processInstanceId: {})", processInstanceId, e);
            execution.setVariable(ApplicationConstants.ProcessVariables.KYC_RESULT, ApplicationConstants.Workflow.KYC_RESULT_ERROR);
            execution.setVariable(ApplicationConstants.ProcessVariables.ERROR_MESSAGE, "KYC verification error: " + e.getMessage());
            execution.setVariable(ApplicationConstants.ProcessVariables.FAILED_STEP_ID, ApplicationConstants.Workflow.FAILED_STEP_KYC);
            // Convert regular exceptions to BPMN errors to trigger boundary events
            throw new BpmnError("GENERIC_ERROR", "KYC verification error: " + e.getMessage());
        }
    }
}
