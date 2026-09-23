package com.behsazan.corebanking.deposit.account.closure.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class DepositClosureModels {
    private DepositClosureModels() {}

    public record ClosureRequest(String closureTypeCode, String reasonCode, LocalDate effectiveDate,
                                 String settlementAccountReference, String approverUserId, String orgUnitCode) {}
    public record ApprovalDecisionRequest(String decisionReasonCode) {}
    public record ReopeningRequest(String reopenReasonCode, String approverUserId, String orgUnitCode) {}

    public record ClosureCheck(long closureCheckId, String checkCode, String resultStatusCode,
                               String resultMessage, OffsetDateTime evaluatedAt, String evidenceReference) {}
    public record ClosureSettlementItem(long closureSettlementItemId, int sequenceNo, String componentCode,
                                        BigDecimal grossAmount, String debitCreditCode, BigDecimal netAmount,
                                        Long transactionId, String settlementStatusCode) {}
    public record ClosureCase(long accountClosureId, long accountId, String closureTypeCode,
                              OffsetDateTime requestedAt, LocalDate effectiveDate, String settlementAccountReference,
                              String closureStatusCode, String reasonCode, Long approvalRequestId,
                              String approvalStatusCode, String approverUserId, String settlementTransactionReference,
                              List<ClosureCheck> checks, List<ClosureSettlementItem> settlementItems, long recordVersion) {}
    public record ReopeningCase(long accountReopeningId, long accountId, String reopenReasonCode,
                                OffsetDateTime requestedAt, String reopenStatusCode, Long approvalRequestId,
                                String approvalStatusCode, String approverUserId, OffsetDateTime executedAt,
                                long recordVersion) {}
    public record ClosureWorkflowView(List<ClosureCase> closures, List<ReopeningCase> reopenings) {}
    public record ClosureActionResponse(ClosureCase closure, boolean idempotentReplay) {}
    public record ReopeningActionResponse(ReopeningCase reopening, boolean idempotentReplay) {}
}
