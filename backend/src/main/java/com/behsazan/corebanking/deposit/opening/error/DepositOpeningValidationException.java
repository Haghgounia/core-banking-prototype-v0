package com.behsazan.corebanking.deposit.opening.error;

import java.util.Map;

public class DepositOpeningValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public DepositOpeningValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
