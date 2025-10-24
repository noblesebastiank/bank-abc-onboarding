package com.bankabc.onboarding.service;

import com.bankabc.onboarding.exception.DefaultApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for validating uploaded files according to business rules.
 * Handles validation for passport and photo documents.
 */
@Service
@Slf4j
public class FileValidationService {

    private static final long PASSPORT_MAX_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long PHOTO_MAX_SIZE = 2 * 1024 * 1024; // 2MB

    /**
     * Validates passport file according to business rules.
     *
     * @param passport The passport file to validate
     * @throws DefaultApiError if validation fails
     */
    public void validatePassport(MultipartFile passport) {
        if (passport.isEmpty()) {
            throw new DefaultApiError(
                HttpStatus.BAD_REQUEST,
                "INVALID_FILE",
                "Passport file is required and cannot be empty"
            );
        }

        String originalFilename = passport.getOriginalFilename();
        log.debug("Validating passport file: {}", originalFilename);

        // Check file extension
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new DefaultApiError(
                HttpStatus.BAD_REQUEST,
                "INVALID_FILE_TYPE",
                "Passport must be a PDF file"
            );
        }

        // Check file size
        if (passport.getSize() > PASSPORT_MAX_SIZE) {
            throw new DefaultApiError(
                HttpStatus.BAD_REQUEST,
                "FILE_TOO_LARGE",
                "Passport file size must not exceed 5MB"
            );
        }

        // Check MIME type
        String mimeType = passport.getContentType();
        if (mimeType == null || !mimeType.equals("application/pdf")) {
            throw new DefaultApiError(
                HttpStatus.BAD_REQUEST,
                "INVALID_MIME_TYPE",
                "Passport must have correct MIME type (application/pdf)"
            );
        }

        log.info("Passport validation passed for file: {}", originalFilename);
    }

    /**
     * Validates photo file according to business rules.
     *
     * @param photo The photo file to validate
     * @throws DefaultApiError if validation fails
     */
    public void validatePhoto(MultipartFile photo) {
        if (photo.isEmpty()) {
            throw new DefaultApiError(
                HttpStatus.BAD_REQUEST,
                "INVALID_FILE",
                "Photo file is required and cannot be empty"
            );
        }

        String originalFilename = photo.getOriginalFilename();
        log.debug("Validating photo file: {}", originalFilename);

        // Check file extension
        if (originalFilename == null) {
            throw new DefaultApiError(
                HttpStatus.BAD_REQUEST,
                "INVALID_FILE",
                "Photo file must have a valid filename"
            );
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!extension.matches("jpg|jpeg|png")) {
            throw new DefaultApiError(
                HttpStatus.BAD_REQUEST,
                "INVALID_FILE_TYPE",
                "Photo must be JPG, JPEG, or PNG file"
            );
        }

        // Check file size
        if (photo.getSize() > PHOTO_MAX_SIZE) {
            throw new DefaultApiError(
                HttpStatus.BAD_REQUEST,
                "FILE_TOO_LARGE",
                "Photo file size must not exceed 2MB"
            );
        }

        // Check MIME type
        String mimeType = photo.getContentType();
        if (mimeType == null || !mimeType.matches("image/jpeg|image/png")) {
            throw new DefaultApiError(
                HttpStatus.BAD_REQUEST,
                "INVALID_MIME_TYPE",
                "Photo must have correct MIME type (image/jpeg or image/png)"
            );
        }

        log.info("Photo validation passed for file: {}", originalFilename);
    }

    /**
     * Extracts file extension from filename.
     *
     * @param filename The filename to extract extension from
     * @return The file extension in lowercase, or empty string if no extension
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
