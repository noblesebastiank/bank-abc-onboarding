package com.bankabc.onboarding.mapper;

import com.bankabc.onboarding.entity.Onboarding;
import org.mapstruct.Mapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * MapStruct mapper for Onboarding status-related operations.
 * 
 * This mapper handles status updates and specialized mappings
 * for the onboarding process workflow.
 */
@Mapper(componentModel = "spring")
public interface OnboardingStatusMapper {

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
     * Updates the onboarding status to COMPLETED.
     * 
     * @param entity the onboarding entity to update
     */
    default void setCompleted(Onboarding entity) {
        updateStatus(entity, Onboarding.OnboardingStatus.COMPLETED, null, null, null);
    }

}

