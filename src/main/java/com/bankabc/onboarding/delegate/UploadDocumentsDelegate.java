package com.bankabc.onboarding.delegate;

import com.bankabc.onboarding.entity.Onboarding;
import com.bankabc.onboarding.entity.Onboarding.OnboardingStatus;
import com.bankabc.onboarding.exception.DefaultApiError;
import com.bankabc.onboarding.service.OnboardingService;
import com.bankabc.onboarding.util.DelegateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Component("uploadDocumentsDelegate")
@RequiredArgsConstructor
@Slf4j
public class UploadDocumentsDelegate implements JavaDelegate {

    private final OnboardingService onboardingService;
    private final DelegateUtils delegateUtils;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) throws Exception {
        UUID onboardingId = delegateUtils.getOnboardingId(execution);

        @SuppressWarnings("unchecked")
        Map<String, String> uploadedDocuments =
            (Map<String, String>) execution.getVariable("uploadedDocuments");

        if (uploadedDocuments == null || uploadedDocuments.isEmpty()) {
            throw new DefaultApiError(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "No documents found in process variables");
        }

        // Ensure passport + photo are present
        if (!uploadedDocuments.containsKey("passport") || !uploadedDocuments.containsKey("photo")) {
            throw new DefaultApiError(HttpStatus.BAD_REQUEST, "MISSING_DOCUMENT",
                "Both passport and photo are required");
        }

        // Extract metadata for both files
        FileMetadata passport = extractFileMetadata(uploadedDocuments.get("passport"));
        FileMetadata photo = extractFileMetadata(uploadedDocuments.get("photo"));

        // Set process variables for DMN
        execution.setVariable("documentType", "passport");
        execution.setVariable("fileExtension", passport.getExtension());
        execution.setVariable("fileSizeMB", passport.getSizeMB());
        execution.setVariable("mimeType", passport.getMimeType());

        execution.setVariable("photoExtension", photo.getExtension());
        execution.setVariable("photoSizeMB", photo.getSizeMB());
        execution.setVariable("photoMimeType", photo.getMimeType());
        execution.setVariable("photoPath", uploadedDocuments.get("photo"));

        log.info("=== DMN VARIABLES SET ===");
        log.info("Document type: {}", execution.getVariable("documentType"));
        log.info("File extension: {}", execution.getVariable("fileExtension"));
        log.info("File size MB: {}", execution.getVariable("fileSizeMB"));
        log.info("MIME type: {}", execution.getVariable("mimeType"));
        log.info("Photo extension: {}", execution.getVariable("photoExtension"));
        log.info("Photo size MB: {}", execution.getVariable("photoSizeMB"));
        log.info("Photo MIME type: {}", execution.getVariable("photoMimeType"));

        // Update onboarding entity
        Onboarding onboarding = onboardingService.findById(onboardingId)
            .orElseThrow(() -> new DefaultApiError(HttpStatus.NOT_FOUND, "ONBOARDING_NOT_FOUND",
                "Onboarding not found: " + onboardingId));

        onboarding.setPassportPath(uploadedDocuments.get("passport"));
        onboarding.setPhotoPath(uploadedDocuments.get("photo"));
        onboarding.setDocumentUploadedAt(OffsetDateTime.now(ZoneOffset.UTC));
        onboarding.setStatus(OnboardingStatus.DOCUMENTS_UPLOADED);

        onboardingService.saveOnboarding(onboarding);

        execution.setVariable("status", OnboardingStatus.DOCUMENTS_UPLOADED.name());

        log.info("Documents uploaded for onboarding ID: {}", onboardingId);
        log.info("=== UPLOAD DELEGATE COMPLETED - DMN VALIDATION SHOULD EXECUTE NEXT ===");
    }

    // -----------------------
    // Helper: Extract file metadata
    // -----------------------
    private FileMetadata extractFileMetadata(String filePath) {
        try {
            Path path = Paths.get(filePath);
            File file = path.toFile();

            if (!file.exists()) {
                throw new IllegalArgumentException("File does not exist: " + filePath);
            }

            // File extension
            String fileName = file.getName();
            String extension = "";
            int lastDotIndex = fileName.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
                extension = fileName.substring(lastDotIndex + 1).toLowerCase();
            }

            // File size in MB
            double sizeMB = file.length() / (1024.0 * 1024.0);

            // MIME type detection
            String mimeType = Files.probeContentType(path);
            if (mimeType == null) {
                mimeType = getMimeTypeFromExtension(extension);
            }

            return new FileMetadata(extension, sizeMB, mimeType);

        } catch (Exception e) {
            log.error("Error extracting file metadata for: {}", filePath, e);
            return new FileMetadata("unknown", 0.0, "unknown");
        }
    }

    private String getMimeTypeFromExtension(String extension) {
        return switch (extension.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> "application/octet-stream";
        };
    }

    // -----------------------
    // Inner class to hold metadata
    // -----------------------
    private static class FileMetadata {
        private final String extension;
        private final double sizeMB;
        private final String mimeType;

        public FileMetadata(String extension, double sizeMB, String mimeType) {
            this.extension = extension;
            this.sizeMB = sizeMB;
            this.mimeType = mimeType;
        }

        public String getExtension() { return extension; }
        public double getSizeMB() { return sizeMB; }
        public String getMimeType() { return mimeType; }
    }
}
