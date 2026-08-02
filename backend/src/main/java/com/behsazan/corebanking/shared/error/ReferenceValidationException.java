package com.behsazan.corebanking.shared.error;

import java.util.Map;

public class ReferenceValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public ReferenceValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
