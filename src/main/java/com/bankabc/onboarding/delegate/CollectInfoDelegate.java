package com.bankabc.onboarding.delegate;

import com.bankabc.onboarding.entity.Onboarding;
import com.bankabc.onboarding.entity.Onboarding.OnboardingStatus;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Delegate for collecting customer information at the start of onboarding.
 */
@Component("collectInfoDelegate")
@RequiredArgsConstructor
@Slf4j
public class CollectInfoDelegate implements JavaDelegate {

    private final OnboardingService onboardingService;
    private final DelegateUtils delegateUtils;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) throws Exception {
        String processInstanceId = execution.getProcessInstanceId();
        log.info("Executing CollectInfoDelegate for processInstanceId: {}", processInstanceId);

        try {
            // Fetch onboarding record
            Onboarding onboarding = delegateUtils.getOnboarding(execution);
            UUID onboardingId = onboarding.getId();

            // Update onboarding state
            onboarding.setStatus(OnboardingStatus.INFO_COLLECTED);
            onboardingService.saveOnboarding(onboarding);

            // Set relevant process variables
            execution.setVariable(ApplicationConstants.ProcessVariables.STATUS, OnboardingStatus.INFO_COLLECTED.name());
            execution.setVariable(ApplicationConstants.ProcessVariables.CUSTOMER_EMAIL, onboarding.getEmail());
            execution.setVariable(ApplicationConstants.ProcessVariables.CUSTOMER_PHONE, onboarding.getPhone());
            execution.setVariable(ApplicationConstants.ProcessVariables.INFO_COLLECTED_AT, OffsetDateTime.now(ZoneOffset.UTC));
            execution.setVariable(ApplicationConstants.ProcessVariables.STEP_ID, ApplicationConstants.ProcessVariables.STEP_COLLECT_INFO);
            execution.setVariable(ApplicationConstants.ProcessVariables.STEP_STATUS, ApplicationConstants.Workflow.KYC_RESULT_SUCCESS);

            log.info("Customer info collected for onboardingId: {} | processInstanceId: {}", onboardingId, processInstanceId);

        } catch (BpmnError e) {
            execution.setVariable(ApplicationConstants.ProcessVariables.FAILED_STEP_ID, ApplicationConstants.ProcessVariables.STEP_COLLECT_INFO_TASK);
            // Re-throw for BPMN boundary error handling
            throw e;
        } catch (Exception e) {
            log.error("Unexpected system error during CollectInfoDelegate: {}", processInstanceId, e);
            execution.setVariable(ApplicationConstants.ProcessVariables.STEP_STATUS, ApplicationConstants.Workflow.KYC_RESULT_ERROR);
            execution.setVariable(ApplicationConstants.ProcessVariables.ERROR_MESSAGE, e.getMessage());
            execution.setVariable(ApplicationConstants.ProcessVariables.FAILED_STEP_ID, ApplicationConstants.ProcessVariables.STEP_COLLECT_INFO_TASK);
            throw e; // triggers retry by Camunda job executor
        }
    }
}
