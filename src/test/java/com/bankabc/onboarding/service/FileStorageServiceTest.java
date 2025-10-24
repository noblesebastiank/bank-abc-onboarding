package com.bankabc.onboarding.service;

import com.bankabc.onboarding.exception.DefaultApiError;
import com.bankabc.onboarding.exception.ErrorTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @InjectMocks
    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        // Set test values for @Value fields
        ReflectionTestUtils.setField(fileStorageService, "storagePath", "/tmp/test-documents");
        ReflectionTestUtils.setField(fileStorageService, "maxFileSize", 10485760L); // 10MB
    }

    @Test
    void validateFile_EmptyFile_ThrowsDefaultApiError() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "passport", "passport.pdf", "application/pdf", new byte[0]);

        DefaultApiError exception = assertThrows(DefaultApiError.class, () -> {
            fileStorageService.validateFile(emptyFile, "passport");
        });

        assertEquals(ErrorTypes.FILE_VALIDATION_FAILED.name(), exception.getErrorName());
        assertEquals(ErrorTypes.FILE_VALIDATION_FAILED.getMessage(), exception.getMessage());
        assertNotNull(exception.getAdditionalDetails());
        assertTrue(exception.getAdditionalDetails().containsKey("fileName"));
        assertTrue(exception.getAdditionalDetails().containsKey("documentType"));
        assertTrue(exception.getAdditionalDetails().containsKey("validationError"));
        assertEquals("File is empty", exception.getAdditionalDetails().get("validationError"));
    }

    @Test
    void validateFile_FileTooLarge_ThrowsDefaultApiError() {
        // Create a file larger than max size (10MB)
        byte[] largeContent = new byte[10485761]; // 10MB + 1 byte
        MockMultipartFile largeFile = new MockMultipartFile(
                "passport", "passport.pdf", "application/pdf", largeContent);

        DefaultApiError exception = assertThrows(DefaultApiError.class, () -> {
            fileStorageService.validateFile(largeFile, "passport");
        });

        assertEquals(ErrorTypes.FILE_VALIDATION_FAILED.name(), exception.getErrorName());
        assertEquals(ErrorTypes.FILE_VALIDATION_FAILED.getMessage(), exception.getMessage());
        assertNotNull(exception.getAdditionalDetails());
        assertTrue(exception.getAdditionalDetails().containsKey("fileSize"));
        assertTrue(exception.getAdditionalDetails().containsKey("maxFileSize"));
        assertTrue(exception.getAdditionalDetails().containsKey("validationError"));
        assertEquals("File size exceeds maximum allowed size", exception.getAdditionalDetails().get("validationError"));
    }

    @Test
    void validateFile_InvalidContentType_ThrowsDefaultApiError() {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "passport", "passport.txt", "text/plain", "passport content".getBytes());

        DefaultApiError exception = assertThrows(DefaultApiError.class, () -> {
            fileStorageService.validateFile(invalidFile, "passport");
        });

        assertEquals(ErrorTypes.FILE_VALIDATION_FAILED.name(), exception.getErrorName());
        assertEquals(ErrorTypes.FILE_VALIDATION_FAILED.getMessage(), exception.getMessage());
        assertNotNull(exception.getAdditionalDetails());
        assertTrue(exception.getAdditionalDetails().containsKey("contentType"));
        assertTrue(exception.getAdditionalDetails().containsKey("allowedTypes"));
        assertTrue(exception.getAdditionalDetails().containsKey("validationError"));
        assertEquals("Invalid file type for passport", exception.getAdditionalDetails().get("validationError"));
    }

    @Test
    void validateFile_InvalidImageType_ThrowsDefaultApiError() {
        MockMultipartFile invalidImage = new MockMultipartFile(
                "photo", "photo.txt", "text/plain", "photo content".getBytes());

        DefaultApiError exception = assertThrows(DefaultApiError.class, () -> {
            fileStorageService.validateFile(invalidImage, "photo");
        });

        assertEquals(ErrorTypes.FILE_VALIDATION_FAILED.name(), exception.getErrorName());
        assertEquals(ErrorTypes.FILE_VALIDATION_FAILED.getMessage(), exception.getMessage());
        assertNotNull(exception.getAdditionalDetails());
        assertTrue(exception.getAdditionalDetails().containsKey("contentType"));
        assertTrue(exception.getAdditionalDetails().containsKey("allowedTypes"));
        assertTrue(exception.getAdditionalDetails().containsKey("validationError"));
        assertEquals("Invalid file type for photo", exception.getAdditionalDetails().get("validationError"));
    }

    @Test
    void validateFile_UnknownDocumentType_ThrowsDefaultApiError() {
        MockMultipartFile file = new MockMultipartFile(
                "document", "document.pdf", "application/pdf", "content".getBytes());

        DefaultApiError exception = assertThrows(DefaultApiError.class, () -> {
            fileStorageService.validateFile(file, "unknown");
        });

        assertEquals(ErrorTypes.FILE_VALIDATION_FAILED.name(), exception.getErrorName());
        assertEquals(ErrorTypes.FILE_VALIDATION_FAILED.getMessage(), exception.getMessage());
        assertNotNull(exception.getAdditionalDetails());
        assertTrue(exception.getAdditionalDetails().containsKey("documentType"));
        assertTrue(exception.getAdditionalDetails().containsKey("validationError"));
        assertEquals("Unknown document type", exception.getAdditionalDetails().get("validationError"));
    }

    @Test
    void validateFile_NullContentType_ThrowsDefaultApiError() {
        MockMultipartFile file = new MockMultipartFile(
                "passport", "passport.pdf", null, "content".getBytes());

        DefaultApiError exception = assertThrows(DefaultApiError.class, () -> {
            fileStorageService.validateFile(file, "passport");
        });

        assertEquals(ErrorTypes.FILE_VALIDATION_FAILED.name(), exception.getErrorName());
        assertEquals(ErrorTypes.FILE_VALIDATION_FAILED.getMessage(), exception.getMessage());
        assertNotNull(exception.getAdditionalDetails());
        assertTrue(exception.getAdditionalDetails().containsKey("validationError"));
        assertEquals("File content type cannot be determined", exception.getAdditionalDetails().get("validationError"));
    }

    @Test
    void validateFile_ValidPassportFile_DoesNotThrow() {
        MockMultipartFile validFile = new MockMultipartFile(
                "passport", "passport.pdf", "application/pdf", "passport content".getBytes());

        assertDoesNotThrow(() -> {
            fileStorageService.validateFile(validFile, "passport");
        });
    }

    @Test
    void validateFile_ValidPhotoFile_DoesNotThrow() {
        MockMultipartFile validFile = new MockMultipartFile(
                "photo", "photo.jpg", "image/jpeg", "photo content".getBytes());

        assertDoesNotThrow(() -> {
            fileStorageService.validateFile(validFile, "photo");
        });
    }
}
