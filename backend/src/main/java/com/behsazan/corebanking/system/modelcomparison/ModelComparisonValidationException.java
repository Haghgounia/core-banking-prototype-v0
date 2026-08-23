package com.behsazan.corebanking.system.modelcomparison;

public class ModelComparisonValidationException extends RuntimeException {
    public ModelComparisonValidationException(String message) {
        super(message);
    }

    public ModelComparisonValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
