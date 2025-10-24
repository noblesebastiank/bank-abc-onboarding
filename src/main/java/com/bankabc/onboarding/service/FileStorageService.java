package com.bankabc.onboarding.service;

import com.bankabc.onboarding.exception.DefaultApiError;
import com.bankabc.onboarding.exception.ErrorTypes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Service for handling file storage operations for document uploads.
 * Supports local file storage with configurable directory and file type validation.
 */
@Service
@Slf4j
public class FileStorageService {

    @Value("${app.file-storage.path:/tmp/onboarding-documents}")
    private String storagePath;

    @Value("${app.file-storage.max-size:10485760}") // 10MB default
    private long maxFileSize;


    /**
     * Store uploaded file to local storage.
     *
     * @param file The uploaded file
     * @param documentType The type of document (passport, photo)
     * @return The file path where the file was stored
     * @throws IOException if file storage fails
     */
    public String storeFile(MultipartFile file, String documentType) throws IOException {
        log.info("Storing file: {} of type: {}", file.getOriginalFilename(), documentType);
        
        // Validate file
        validateFile(file, documentType);
        
        // Create storage directory if it doesn't exist
        Path storageDir = Paths.get(storagePath);
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
            log.info("Created storage directory: {}", storagePath);
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = String.format("%s_%s_%s%s", 
                documentType, 
                UUID.randomUUID().toString(), 
                System.currentTimeMillis(),
                fileExtension);
        
        // Store file
        Path targetPath = storageDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        String filePath = targetPath.toString();
        log.info("File stored successfully: {}", filePath);
        
        return filePath;
    }

    /**
     * Basic file validation for storage (file existence and size only).
     * Detailed validation is handled by DMN decision table.
     *
     * @param file The uploaded file
     * @param documentType The type of document
     * @throws DefaultApiError if file is invalid
     */
    public void validateFile(MultipartFile file, String documentType) {
        if (file.isEmpty()) {
            throw new DefaultApiError(
                HttpStatus.BAD_REQUEST,
                ErrorTypes.FILE_VALIDATION_FAILED.name(),
                ErrorTypes.FILE_VALIDATION_FAILED.getMessage(),
                Map.of(
                    "fileName", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown",
                    "documentType", documentType,
                    "validationError", "File is empty"
                )
            );
        }
        
        if (file.getSize() > maxFileSize) {
            throw new DefaultApiError(
                HttpStatus.BAD_REQUEST,
                ErrorTypes.FILE_VALIDATION_FAILED.name(),
                ErrorTypes.FILE_VALIDATION_FAILED.getMessage(),
                Map.of(
                    "fileName", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown",
                    "documentType", documentType,
                    "fileSize", String.valueOf(file.getSize()),
                    "maxFileSize", String.valueOf(maxFileSize),
                    "validationError", "File size exceeds maximum allowed size"
                )
            );
        }
        
        log.debug("Basic file validation passed for: {} with type: {}", file.getOriginalFilename(), documentType);
    }


    /**
     * Get file extension from filename.
     *
     * @param filename The filename
     * @return The file extension including the dot
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }


}
