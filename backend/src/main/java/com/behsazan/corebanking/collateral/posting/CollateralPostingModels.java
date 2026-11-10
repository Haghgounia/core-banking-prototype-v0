package com.behsazan.corebanking.collateral.posting;

/** Persistent permit IDs and all Oracle money/NUMBER(19) values are JSON strings. */
public final class CollateralPostingModels {
    private CollateralPostingModels() {}
    /** No client-provided amount, approval, status, policy factor, or debtor roster. */
    public record CommitRequest(String permitId) {}
    public record Readiness(boolean allocationPostingEnabled,boolean unallocationPostingEnabled,
            boolean oraclePermitSchemaReady,boolean coreSchemaReady,
            boolean bankMappingsReady,boolean localPrototype,
            boolean operationallyReady,String state) {}
    public record Receipt(String permitId,String operation,String collateralId,
            String allocationId,String transactionId,String transactionNumber,
            String amount,String remainingAllocationAmount,String currencyCode,
            String parentRecordVersion,String status,boolean replayed,String evidence) {}
}
