package com.behsazan.corebanking.cif.error;

import java.util.Map;

public class CifValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public CifValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
