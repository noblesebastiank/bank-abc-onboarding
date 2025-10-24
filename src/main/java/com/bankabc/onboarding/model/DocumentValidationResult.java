package com.bankabc.onboarding.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model class representing the result of document validation using DMN.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentValidationResult {
    
    private boolean isValid;
    private String documentType;
    private String fileExtension;
    private Double fileSizeMB;
    private String mimeType;
    private String errorCode;
    private String errorMessage;
    
    /**
     * Creates a valid result for a document.
     */
    public static DocumentValidationResult valid(String documentType, String fileExtension, 
                                               Double fileSizeMB, String mimeType) {
        return DocumentValidationResult.builder()
                .isValid(true)
                .documentType(documentType)
                .fileExtension(fileExtension)
                .fileSizeMB(fileSizeMB)
                .mimeType(mimeType)
                .errorCode(null)
                .errorMessage(null)
                .build();
    }
    
    /**
     * Creates an invalid result for a document.
     */
    public static DocumentValidationResult invalid(String documentType, String fileExtension, 
                                                 Double fileSizeMB, String mimeType,
                                                 String errorCode, String errorMessage) {
        return DocumentValidationResult.builder()
                .isValid(false)
                .documentType(documentType)
                .fileExtension(fileExtension)
                .fileSizeMB(fileSizeMB)
                .mimeType(mimeType)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}