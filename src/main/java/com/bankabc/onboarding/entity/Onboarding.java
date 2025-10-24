package com.bankabc.onboarding.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.bankabc.onboarding.constants.ApplicationConstants;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Onboarding entity representing a customer onboarding process.
 * Stores all customer information, onboarding status, and account details.
 * 
 * Future enhancements:
 * - Add document upload fields for ID proof and photo
 * - Add audit trail for compliance
 * - Add encryption for sensitive data (SSN)
 */
@Entity
@Table(name = "onboarding", 
       indexes = {
           @Index(name = "idx_ssn", columnList = "ssn")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Onboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // Personal Information
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 1)
    private Gender gender;

    @Column(name = "date_of_birth", nullable = false)
    @JsonFormat(pattern = ApplicationConstants.DATE_FORMAT)
    private LocalDate dateOfBirth;

    // Contact Information
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "nationality", nullable = false, length = 50)
    private String nationality;

    // Address Information
    @Column(name = "street", nullable = false, length = 100)
    private String street;

    @Column(name = "city", nullable = false, length = 50)
    private String city;

    @Column(name = "postal_code", nullable = false, length = 10)
    private String postalCode;

    @Column(name = "country", nullable = false, length = 50)
    private String country;

    // Identification
    @Column(name = "ssn", nullable = false, unique = true, length = 11)
    private String ssn;

    // Onboarding Process Information
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private OnboardingStatus status = OnboardingStatus.INITIATED;

    @Column(name = "account_number", length = 18)
    private String accountNumber;

    @Column(name = "kyc_verified")
    @Builder.Default
    private Boolean kycVerified = false;

    @Column(name = "kyc_verification_notes", length = 500)
    private String kycVerificationNotes;

    @Column(name = "address_verified")
    @Builder.Default
    private Boolean addressVerified = false;

    @Column(name = "address_verification_notes", length = 500)
    private String addressVerificationNotes;

    // BPMN Process Information
    @Column(name = "process_instance_id", length = 100)
    private String processInstanceId;

    @Column(name = "process_definition_key", length = 100)
    private String processDefinitionKey;

    // Document Information
    @Column(name = "passport_path", length = 500)
    private String passportPath;

    @Column(name = "photo_path", length = 500)
    private String photoPath;

    @Column(name = "document_uploaded_at")
    @JsonFormat(pattern = ApplicationConstants.DATETIME_FORMAT)
    private OffsetDateTime documentUploadedAt;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(pattern = ApplicationConstants.DATETIME_FORMAT)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @JsonFormat(pattern = ApplicationConstants.DATETIME_FORMAT)
    private OffsetDateTime updatedAt;

    @Column(name = "completed_at")
    @JsonFormat(pattern = ApplicationConstants.DATETIME_FORMAT)
    private OffsetDateTime completedAt;

    /**
     * JPA lifecycle method to set timestamps before persisting.
     * Ensures all timestamps are in UTC regardless of database server timezone.
     */
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    /**
     * JPA lifecycle method to update timestamp before updating.
     * Ensures all timestamps are in UTC regardless of database server timezone.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Enum representing customer gender.
     */
    public enum Gender {
        M("Male"),
        F("Female"),
        O("Other");

        private final String description;

        Gender(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Enum representing the different states of the onboarding process.
     */
    public enum OnboardingStatus {
        INITIATED("Onboarding process initiated"),
        INFO_COLLECTED("Customer information collected"),
        WAITING_FOR_DOCUMENTS("Waiting for document upload"),
        DOCUMENTS_UPLOADED("Documents uploaded successfully"),
        KYC_IN_PROGRESS("KYC verification in progress"),
        KYC_COMPLETED("KYC verification completed"),
        ADDRESS_VERIFICATION_IN_PROGRESS("Address verification in progress"),
        ADDRESS_VERIFICATION_COMPLETED("Address verification completed"),
        ACCOUNT_CREATION_IN_PROGRESS("Account creation in progress"),
        ACCOUNT_CREATED("Bank account created"),
        NOTIFICATION_SENT("Customer notification sent"),
        COMPLETED("Onboarding completed successfully"),
        FAILED("Onboarding failed");

        private final String description;

        OnboardingStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}

