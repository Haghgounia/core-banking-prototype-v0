package com.behsazan.corebanking.deposit.account.error;

import java.util.Map;

public class DepositAccountLifecycleException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public DepositAccountLifecycleException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors == null ? Map.of() : Map.copyOf(fieldErrors);
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
