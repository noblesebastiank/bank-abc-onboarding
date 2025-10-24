package com.bankabc.onboarding.delegate;

import java.util.UUID;

import com.bankabc.onboarding.entity.Onboarding;
import com.bankabc.onboarding.entity.Onboarding.OnboardingStatus;
import com.bankabc.onboarding.service.AccountService;
import com.bankabc.onboarding.service.OnboardingService;
import com.bankabc.onboarding.constants.ApplicationConstants;
import com.bankabc.onboarding.util.DelegateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Delegate for bank account creation in the onboarding process.
 */
@Component("accountCreationDelegate")
@RequiredArgsConstructor
@Slf4j
public class AccountCreationDelegate implements JavaDelegate {

    private final AccountService accountService;
    private final OnboardingService onboardingService;
    private final DelegateUtils delegateUtils;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Executing AccountCreationDelegate for process instance: {}",
            execution.getProcessInstanceId());

        try {
            Onboarding onboarding = delegateUtils.getOnboarding(execution);
            UUID onboardingId = onboarding.getId();

            onboarding.setStatus(OnboardingStatus.ACCOUNT_CREATION_IN_PROGRESS);

            String accountNumber = accountService.createAccount(
                onboarding.getFirstName(),
                onboarding.getLastName(),
                onboarding.getEmail(),
                onboarding.getPhone(),
                onboarding.getDateOfBirth(),
                onboarding.getSsn()
            );

            if (accountNumber == null || accountNumber.isEmpty()) {
                // Don't set global status to FAILED - let previous steps remain successful
                execution.setVariable(ApplicationConstants.ProcessVariables.ERROR_MESSAGE, ApplicationConstants.Workflow.ACCOUNT_CREATION_FAILED_MSG);
                execution.setVariable(ApplicationConstants.ProcessVariables.FAILED_STEP_ID, ApplicationConstants.Workflow.FAILED_STEP_ACCOUNT);
                log.warn("Account creation failed for onboardingId: {}", onboardingId);

                // Business-level BPMN error — will trigger boundary event
                throw new BpmnError("ACCOUNT_CREATION_FAILED", ApplicationConstants.Workflow.ACCOUNT_CREATION_FAILED_MSG);
            }

            onboarding.setAccountNumber(accountNumber);
            onboarding.setStatus(OnboardingStatus.ACCOUNT_CREATED);
            onboardingService.saveOnboarding(onboarding);

            execution.setVariable(ApplicationConstants.ProcessVariables.ACCOUNT_NUMBER, accountNumber);
            execution.setVariable(ApplicationConstants.ProcessVariables.STATUS, OnboardingStatus.ACCOUNT_CREATED.name());
            log.info("Account created successfully for onboardingId: {}, accountNumber: {}",
                onboardingId, accountNumber);

        } catch (BpmnError e) {
            execution.setVariable(ApplicationConstants.ProcessVariables.FAILED_STEP_ID, ApplicationConstants.ProcessVariables.STEP_ACCOUNT_CREATION);
            throw e; // handled by BPMN
        } catch (Exception e) {
            log.error("System error during account creation for processInstanceId: {}", execution.getProcessInstanceId(), e);
            execution.setVariable(ApplicationConstants.ProcessVariables.ERROR_MESSAGE, "System error: " + e.getMessage());
            execution.setVariable(ApplicationConstants.ProcessVariables.FAILED_STEP_ID, ApplicationConstants.ProcessVariables.STEP_ACCOUNT_CREATION);
            // Convert regular exceptions to BPMN errors to trigger boundary events
            throw new BpmnError("GENERIC_ERROR", "Account creation error: " + e.getMessage());
        }
    }
}
