package com.bankabc.onboarding.exception;

import lombok.Getter;

/**
 * Enum containing all error types and their messages for the onboarding service.
 */
@Getter
public enum ErrorTypes {

    CUSTOMER_ALREADY_EXISTS("Customer already exists with this SSN"),
    ONBOARDING_NOT_FOUND("Onboarding process not found"),
    ORGANIZATION_NOT_FOUND("Organization not found"),
    KYC_VERIFICATION_FAILED("KYC verification failed"),
    ACCOUNT_CREATION_FAILED("Account creation failed"),
    DOCUMENT_UPLOAD_FAILED("Document upload failed"),
    ADDRESS_VERIFICATION_FAILED("Address verification failed"),
    FILE_VALIDATION_FAILED("File validation failed"),
    PROCESS_START_FAILED("Failed to start onboarding process"),
    NOTIFICATION_FAILED("Customer notification failed"),
    INVALID_REQUEST("Invalid request data"),
    INTERNAL_SERVER_ERROR("Internal server error");

    private final String message;

    ErrorTypes(String message) {
        this.message = message;
    }

}

