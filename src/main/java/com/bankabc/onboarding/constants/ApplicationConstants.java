package com.bankabc.onboarding.constants;

/**
 * Application constants for the Bank ABC Onboarding system.
 * Centralizes all hardcoded values to improve maintainability.
 */
public final class ApplicationConstants {

    private ApplicationConstants() {
        // Utility class - prevent instantiation
    }

    // ============================================================================
    // DATE/TIME FORMATS
    // ============================================================================
    
    /**
     * ISO 8601 datetime format with timezone offset.
     * Used for all OffsetDateTime fields in JSON serialization.
     */
    public static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    
    /**
     * ISO 8601 date format.
     * Used for LocalDate fields in JSON serialization.
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    // ============================================================================
    // API RESPONSE STATUS VALUES
    // ============================================================================
    
    /**
     * Status values for API responses.
     */
    public static final class Status {
        private Status() {
            // Utility class - prevent instantiation
        }
        
        public static final String SUCCESS = "SUCCESS";
        public static final String FAILED = "FAILED";
        public static final String PARTIAL = "PARTIAL";
        public static final String ERROR = "ERROR";
        public static final String PENDING = "PENDING";
    }

    // ============================================================================
    // PROCESS RESULTS
    // ============================================================================
    
    /**
     * Process execution result values.
     */
    public static final class ProcessResult {
        private ProcessResult() {
            // Utility class - prevent instantiation
        }
        
        public static final String SUCCESS = "SUCCESS";
        public static final String FAILED = "FAILED";
        public static final String ERROR = "ERROR";
    }

    // ============================================================================
    // ERROR TYPES
    // ============================================================================
    
    /**
     * Error type constants for failure handling.
     */
    public static final class ErrorType {
        private ErrorType() {
            // Utility class - prevent instantiation
        }
        
        public static final String KYC_VERIFICATION_FAILED = "KYC_VERIFICATION_FAILED";
        public static final String ADDRESS_VERIFICATION_FAILED = "ADDRESS_VERIFICATION_FAILED";
        public static final String ACCOUNT_CREATION_FAILED = "ACCOUNT_CREATION_FAILED";
        public static final String DOCUMENT_UPLOAD_FAILED = "DOCUMENT_UPLOAD_FAILED";
        public static final String GENERAL_FAILURE = "GENERAL_FAILURE";
        public static final String INVALID_REQUEST = "INVALID_REQUEST";
        public static final String CUSTOMER_ALREADY_EXISTS = "CUSTOMER_ALREADY_EXISTS";
        public static final String ONBOARDING_NOT_FOUND = "ONBOARDING_NOT_FOUND";
    }

    // ============================================================================
    // BANKING CONSTANTS
    // ============================================================================
    
    /**
     * Banking-related constants.
     */
    public static final class Banking {
        private Banking() {
            // Utility class - prevent instantiation
        }
        
        public static final String BANK_CODE = "BANK";
        public static final String COUNTRY_CODE = "NL";
        public static final String BANK_NAME = "Bank ABC";
    }

    // ============================================================================
    // NOTIFICATION CONSTANTS
    // ============================================================================
    
    /**
     * Notification-related constants.
     */
    public static final class Notification {
        private Notification() {
            // Utility class - prevent instantiation
        }
        
        public static final String SUCCESS_RESULT = "SUCCESS";
        public static final String PARTIAL_RESULT = "PARTIAL";
        public static final String ERROR_RESULT = "ERROR";
        
        // Email subjects
        public static final String ACCOUNT_CREATED_SUBJECT = "Account Created Successfully";
        public static final String KYC_FAILED_SUBJECT = "Identity Verification Required";
        public static final String ADDRESS_FAILED_SUBJECT = "Address Verification Required";
        public static final String ACCOUNT_FAILED_SUBJECT = "Account Creation Issue";
        public static final String DOCUMENT_FAILED_SUBJECT = "Document Upload Issue";
        public static final String GENERAL_FAILED_SUBJECT = "Onboarding Process Update";
    }

    // ============================================================================
    // MAPPING CONSTANTS
    // ============================================================================
    
    /**
     * Constants used in entity mapping.
     */
    public static final class Mapping {
        private Mapping() {
            // Utility class - prevent instantiation
        }
        
        public static final String INITIATED_STATUS = "INITIATED";
        public static final String FALSE_VALUE = "false";
    }

    // ============================================================================
    // MESSAGE TEMPLATES
    // ============================================================================
    
    /**
     * Message templates for notifications and responses.
     */
    public static final class Messages {
        private Messages() {
            // Utility class - prevent instantiation
        }
        
        // API Response Messages
        public static final String CUSTOMER_ALREADY_EXISTS = "Customer with SSN already exists";
        public static final String PROCESS_NOT_FOUND = "Process instance not found or not active";
        public static final String DOCUMENTS_UPLOADED_SUCCESS = "Documents uploaded successfully";
        
        // Error Messages
        public static final String KYC_VERIFICATION_FAILED_MSG = "Identity verification failed";
        public static final String ADDRESS_VERIFICATION_FAILED_MSG = "Address verification failed";
        public static final String ACCOUNT_CREATION_FAILED_MSG = "Account creation failed";
        public static final String DOCUMENT_UPLOAD_FAILED_MSG = "Document upload failed";
        public static final String GENERAL_FAILURE_MSG = "Onboarding process failed";
        
        // SMS Messages
        public static final String KYC_FAILED_SMS = "Bank ABC: Identity verification failed. Please check your documents and try again. Contact support if needed.";
        public static final String ADDRESS_FAILED_SMS = "Bank ABC: Address verification failed. Please update your address information and try again.";
        public static final String ACCOUNT_FAILED_SMS = "Bank ABC: Account creation issue detected. Our team is working on it. You'll be notified once resolved.";
        public static final String DOCUMENT_FAILED_SMS = "Bank ABC: Document upload failed. Please check file format and size, then try again.";
    }

    // ============================================================================
    // WORKFLOW CONSTANTS
    // ============================================================================
    
    /**
     * Workflow-related constants for BPMN process execution.
     */
    public static final class Workflow {
        private Workflow() {
            // Utility class - prevent instantiation
        }
        
        // Process Results
        public static final String KYC_RESULT_SUCCESS = "SUCCESS";
        public static final String KYC_RESULT_FAILED = "FAILED";
        public static final String KYC_RESULT_ERROR = "ERROR";
        
        // Verification Status
        public static final String KYC_VERIFIED_TRUE = "true";
        public static final String KYC_VERIFIED_FALSE = "false";
        
        // Step Identifiers
        public static final String FAILED_STEP_KYC = "kyc-verification";
        public static final String FAILED_STEP_ADDRESS = "address-verification";
        public static final String FAILED_STEP_ACCOUNT = "account-creation";
        public static final String FAILED_STEP_DOCUMENTS = "document-upload";
        
        // Error Messages
        public static final String KYC_VERIFICATION_FAILED_MSG = "KYC verification failed";
        public static final String ADDRESS_VERIFICATION_FAILED_MSG = "Address verification failed";
        public static final String ACCOUNT_CREATION_FAILED_MSG = "Account creation failed";
        public static final String DOCUMENT_UPLOAD_FAILED_MSG = "Document upload failed";
    }

    // ============================================================================
    // PROCESS VARIABLES
    // ============================================================================
    
    /**
     * BPMN process variable names used across delegates.
     */
    public static final class ProcessVariables {
        private ProcessVariables() {
            // Utility class - prevent instantiation
        }
        
        // KYC Variables
        public static final String KYC_RESULT = "kycResult";
        public static final String KYC_VERIFIED = "kycVerified";
        
        // Address Variables
        public static final String ADDRESS_RESULT = "addressResult";
        
        // Account Variables
        public static final String ACCOUNT_NUMBER = "accountNumber";
        
        // Document Variables
        public static final String PASSPORT_PATH = "passportPath";
        public static final String PHOTO_PATH = "photoPath";
        
        // Status Variables
        public static final String STATUS = "status";
        public static final String STEP_STATUS = "stepStatus";
        public static final String STEP_ID = "stepId";
        
        // Customer Info Variables
        public static final String CUSTOMER_EMAIL = "customerEmail";
        public static final String CUSTOMER_PHONE = "customerPhone";
        public static final String INFO_COLLECTED_AT = "infoCollectedAt";
        
        // Error Variables
        public static final String ERROR_TYPE = "errorType";
        public static final String ERROR_MESSAGE = "errorMessage";
        public static final String FAILED_STEP_ID = "failedStepId";
        
        // Notification Variables
        public static final String NOTIFICATION_RESULT = "notificationResult";
        public static final String NOTIFICATION_SENT = "notificationSent";
        public static final String EMAIL_SENT = "emailSent";
        public static final String SMS_SENT = "smsSent";
        public static final String NOTIFICATION_TIMESTAMP = "notificationTimestamp";
        
        // Verification Variables
        public static final String VERIFIED_BY = "verifiedBy";
        
        // Step Identifiers
        public static final String STEP_COLLECT_INFO = "collect-info";
        public static final String STEP_UPLOAD_DOCUMENTS = "UploadDocumentsTask";
        public static final String STEP_KYC_VERIFICATION = "KycVerificationTask";
        public static final String STEP_ADDRESS_VERIFICATION = "AddressVerificationTask";
        public static final String STEP_ACCOUNT_CREATION = "AccountCreationTask";
        public static final String STEP_COLLECT_INFO_TASK = "CollectInfoTask";
    }

    // ============================================================================
    // EMAIL TEMPLATES
    // ============================================================================
    
    /**
     * Email template constants.
     */
    public static final class EmailTemplates {
        private EmailTemplates() {
            // Utility class - prevent instantiation
        }
        
        public static final String KYC_FAILED_TEMPLATE = """
                Dear %s,
                
                We encountered an issue with your identity verification during the onboarding process.
                
                Please ensure your documents are:
                - Clear and readable
                - Valid and not expired
                - Match the information provided
                
                You can retry the verification process or contact our support team for assistance.
                
                Best regards,
                Bank ABC Team
                """;
                
        public static final String ADDRESS_FAILED_TEMPLATE = """
                Dear %s,
                
                We were unable to verify your address information. Please update your address details and try again.
                
                Ensure your address is:
                - Complete and accurate
                - Current and up-to-date
                - Matches official records
                
                You can update your information and retry the verification.
                
                Best regards,
                Bank ABC Team
                """;
                
        public static final String ACCOUNT_FAILED_TEMPLATE = """
                Dear %s,
                
                We encountered a technical issue while creating your account. Our team is working to resolve this.
                
                You don't need to take any action - we'll notify you once your account is ready.
                
                We apologize for any inconvenience.
                
                Best regards,
                Bank ABC Team
                """;
                
        public static final String DOCUMENT_FAILED_TEMPLATE = """
                Dear %s,
                
                Your document upload failed. Please check the following:
                
                - File format is supported (JPG, PNG, PDF)
                - File size is under 10MB
                - Documents are clear and readable
                
                Please try uploading your documents again.
                
                Best regards,
                Bank ABC Team
                """;
    }
}
