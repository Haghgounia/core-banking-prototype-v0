package com.behsazan.corebanking.deposit.account.error;

public class DepositAccountNotFoundException extends RuntimeException {
    public DepositAccountNotFoundException(String message) {
        super(message);
    }
}
