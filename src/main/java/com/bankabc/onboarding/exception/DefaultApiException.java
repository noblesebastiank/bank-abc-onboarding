package com.bankabc.onboarding.exception;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@JsonIgnoreProperties(value = {"cause", "stackTrace", "suppressed", "localizedMessage", "message"})
public class DefaultApiException extends RuntimeException {
    private final int status;
    private final List<String> errors;
}

