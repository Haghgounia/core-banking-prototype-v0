package com.behsazan.corebanking.cif.error;

public class CifNotFoundException extends RuntimeException {
    public CifNotFoundException(String message) {
        super(message);
    }
}
