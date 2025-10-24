package com.bankabc.onboarding.mapper;

import com.bankabc.onboarding.entity.Onboarding;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * MapStruct mapper for Onboarding status-related operations.
 * 
 * This mapper handles status updates and specialized mappings
 * for the onboarding process workflow.
 */
@Mapper(componentModel = "spring")
public interface OnboardingStatusMapper {

    OnboardingStatusMapper INSTANCE = Mappers.getMapper(OnboardingStatusMapper.class);

    /**
     * Updates the onboarding status and related fields.
     * 
     * @param entity the onboarding entity to update
     * @param status the new status
     * @param kycVerified the KYC verification status
     * @param kycNotes the KYC verification notes
     * @param accountNumber the account number (if applicable)
     */
    default void updateStatus(Onboarding entity, 
                             Onboarding.OnboardingStatus status, 
                             Boolean kycVerified, 
                             String kycNotes, 
                             String accountNumber) {
        entity.setStatus(status);
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        
        if (kycVerified != null) {
            entity.setKycVerified(kycVerified);
        }
        
        if (kycNotes != null) {
            entity.setKycVerificationNotes(kycNotes);
        }
        
        if (accountNumber != null) {
            entity.setAccountNumber(accountNumber);
        }
        
        if (status == Onboarding.OnboardingStatus.COMPLETED) {
            entity.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
    }

    /**
     * Updates the onboarding status to VERIFICATION_IN_PROGRESS.
     * 
     * @param entity the onboarding entity to update
     */
    default void setVerificationInProgress(Onboarding entity) {
        updateStatus(entity, Onboarding.OnboardingStatus.KYC_IN_PROGRESS, null, null, null);
    }

    /**
     * Updates the onboarding status to VERIFICATION_COMPLETED.
     * 
     * @param entity the onboarding entity to update
     * @param kycVerified the KYC verification result
     * @param kycNotes the KYC verification notes
     */
    default void setVerificationCompleted(Onboarding entity, Boolean kycVerified, String kycNotes) {
        updateStatus(entity, Onboarding.OnboardingStatus.KYC_COMPLETED, kycVerified, kycNotes, null);
    }

    /**
     * Updates the onboarding status to ACCOUNT_CREATED.
     * 
     * @param entity the onboarding entity to update
     * @param accountNumber the generated account number
     */
    default void setAccountCreated(Onboarding entity, String accountNumber) {
        updateStatus(entity, Onboarding.OnboardingStatus.ACCOUNT_CREATED, null, null, accountNumber);
    }

    /**
     * Updates the onboarding status to COMPLETED.
     * 
     * @param entity the onboarding entity to update
     */
    default void setCompleted(Onboarding entity) {
        updateStatus(entity, Onboarding.OnboardingStatus.COMPLETED, null, null, null);
    }

    /**
     * Updates the onboarding status to FAILED.
     * 
     * @param entity the onboarding entity to update
     * @param errorMessage the error message
     */
    default void setFailed(Onboarding entity, String errorMessage) {
        updateStatus(entity, Onboarding.OnboardingStatus.FAILED, null, errorMessage, null);
    }
}

