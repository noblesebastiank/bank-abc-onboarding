package com.bankabc.onboarding.delegate;

import com.bankabc.onboarding.constants.ApplicationConstants;
import com.bankabc.onboarding.entity.Onboarding;
import com.bankabc.onboarding.entity.Onboarding.OnboardingStatus;
import com.bankabc.onboarding.service.OnboardingService;
import com.bankabc.onboarding.service.VerificationService;
import com.bankabc.onboarding.util.DelegateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Delegate for address verification in the onboarding process.
 * Performs address verification via external service.
 */
@Component("addressVerificationDelegate")
@RequiredArgsConstructor
@Slf4j
public class AddressVerificationDelegate implements JavaDelegate {

    private final VerificationService verificationService;
    private final OnboardingService onboardingService;
    private final DelegateUtils delegateUtils;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String processInstanceId = execution.getProcessInstanceId();
        log.info("Executing AddressVerificationDelegate for processInstanceId: {}", processInstanceId);

        try {
            Onboarding onboarding = delegateUtils.getOnboarding(execution);
            UUID onboardingId = onboarding.getId();

            onboarding.setStatus(OnboardingStatus.ADDRESS_VERIFICATION_IN_PROGRESS);

            // Perform address verification
            boolean addressVerified = verificationService.verifyAddress(
                onboarding.getStreet(),
                onboarding.getCity(),
                onboarding.getPostalCode(),
                onboarding.getCountry()
            );

            if (!addressVerified) {
                // Don't set global status to FAILED - let previous steps remain successful
                onboarding.setAddressVerified(false);
                execution.setVariable(ApplicationConstants.ProcessVariables.ADDRESS_RESULT, ApplicationConstants.Workflow.KYC_RESULT_FAILED);
                execution.setVariable(ApplicationConstants.ProcessVariables.ERROR_MESSAGE, ApplicationConstants.Workflow.ADDRESS_VERIFICATION_FAILED_MSG);
                execution.setVariable(ApplicationConstants.ProcessVariables.FAILED_STEP_ID, ApplicationConstants.Workflow.FAILED_STEP_ADDRESS);

                log.warn("Address verification failed for onboardingId: {}", onboardingId);

                // BPMN error for business-level handling
                throw new BpmnError("ADDRESS_VERIFICATION_FAILED", ApplicationConstants.Workflow.ADDRESS_VERIFICATION_FAILED_MSG);
            }

            // Address verified successfully
            onboarding.setStatus(OnboardingStatus.ADDRESS_VERIFICATION_COMPLETED);
            onboarding.setAddressVerified(true);
            onboardingService.saveOnboarding(onboarding);

            execution.setVariable(ApplicationConstants.ProcessVariables.ADDRESS_RESULT, ApplicationConstants.Workflow.KYC_RESULT_SUCCESS);
            execution.setVariable(ApplicationConstants.ProcessVariables.STATUS, OnboardingStatus.ADDRESS_VERIFICATION_COMPLETED.name());
            execution.setVariable(ApplicationConstants.ProcessVariables.VERIFIED_BY, "AddressVerificationDelegate");

            log.info(" Address verification successful for onboardingId: {}", onboardingId);


        } catch (BpmnError e) {
            execution.setVariable(ApplicationConstants.ProcessVariables.FAILED_STEP_ID, ApplicationConstants.ProcessVariables.STEP_ADDRESS_VERIFICATION);
            throw e; // handled by BPMN boundary event
        } catch (Exception e) {
            log.error("System error in AddressVerificationDelegate for processInstanceId: {}", processInstanceId, e);
            execution.setVariable(ApplicationConstants.ProcessVariables.ERROR_MESSAGE, "System error: " + e.getMessage());
            execution.setVariable(ApplicationConstants.ProcessVariables.ERROR_TYPE, e.getClass().getSimpleName());
            execution.setVariable(ApplicationConstants.ProcessVariables.FAILED_STEP_ID, ApplicationConstants.ProcessVariables.STEP_ADDRESS_VERIFICATION);
            // Convert regular exceptions to BPMN errors to trigger boundary events
            throw new BpmnError("ADDRESS_VERIFICATION_FAILED", "Address verification error: " + e.getMessage());
        }
    }
}
