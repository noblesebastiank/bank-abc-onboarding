package com.bankabc.onboarding.exception;

import com.bankabc.onboarding.openapi.model.DefaultApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Global exception handler for the onboarding service.
 * Handles all exceptions and provides structured error responses.
 */
@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DefaultApiException> handleValidationExceptions(final MethodArgumentNotValidException ex) {
        log.warn("Validation error occurred: {}", ex.getMessage());
        
        final List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        final DefaultApiException apiError = new DefaultApiException(
                HttpStatus.BAD_REQUEST.value(),
                errors
        );

        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<DefaultApiException> handleConstraintViolationException(final ConstraintViolationException ex) {
        log.warn("Constraint violation error occurred: {}", ex.getMessage());
        
        final List<String> errors = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();

        final DefaultApiException apiError = new DefaultApiException(
                HttpStatus.BAD_REQUEST.value(),
                errors
        );

        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(DefaultApiError.class)
    public ResponseEntity<DefaultApiErrorResponse> handleDefaultApiException(final DefaultApiError ex) {
        log.warn("Custom API error occurred: {} - {}", ex.getErrorName(), ex.getMessage());

        final var response = new DefaultApiErrorResponse();
        response.setErrorName(ex.getErrorName());
        response.setMessage(ex.getMessage());
        response.setTimestamp(ex.getTimestamp());
        response.setAdditionalDetails(ex.getAdditionalDetails());

        return ResponseEntity.status(ex.getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<DefaultApiErrorResponse> handleMaxUploadSizeExceededException(final MaxUploadSizeExceededException ex) {
        log.warn("File upload size exceeded: {}", ex.getMessage());

        final var response = new DefaultApiErrorResponse();
        response.setErrorName("FILE_TOO_LARGE");
        response.setMessage("File size exceeds the maximum allowed limit. Please upload a smaller file.");
        response.setTimestamp(OffsetDateTime.now());
        response.setAdditionalDetails(Map.of(
            "maxSize", "10MB",
            "errorType", "FILE_SIZE_EXCEEDED"
        ));

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DefaultApiErrorResponse> handleGenericException(final Exception ex) {
        log.error("Unexpected error occurred", ex);

        final var response = new DefaultApiErrorResponse();
        response.setErrorName(ErrorTypes.INTERNAL_SERVER_ERROR.name());
        response.setMessage(ErrorTypes.INTERNAL_SERVER_ERROR.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
