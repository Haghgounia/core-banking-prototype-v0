package com.behsazan.corebanking.deposit.account.transaction.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class DepositTransactionModels {
    private DepositTransactionModels() {}

    public record InitiateTransactionRequest(
            String transactionTypeCode, BigDecimal amount, String currencyCode,
            LocalDate valueDate, LocalDate bookingDate, String channelCode,
            String destinationAccountReference, String transferTypeCode,
            String destinationBankCode, String paymentPurposeCode,
            String cashManagementTxnRef, String cashDeskCode, String tellerId,
            String sourceOfFundsCode) {}

    public record ReversalRequest(String reasonCode) {}

    /**
     * Internal-only Step 05 contract used by documented operational flows (Term/Profit/etc.).
     * It deliberately is not exposed by the public transaction controller.
     */
    public record DerivedTransactionLeg(Long accountId,String accountReference,String debitCreditCode,BigDecimal amount) {}
    public record DerivedTransactionRequest(
            long contextAccountId,String transactionTypeCode,BigDecimal amount,String currencyCode,
            LocalDate valueDate,LocalDate bookingDate,String debitCreditCode,String externalReference,
            String authorizationTypeCode,String requestedBy,String decidedBy,String authorizationReasonCode,
            List<DerivedTransactionLeg> legs) {}

    public record TransactionRecord(long transactionId,String referenceNo,String idempotencyKey,long accountId,
                                    String transactionTypeCode,BigDecimal amount,String currencyCode,
                                    LocalDate valueDate,LocalDate bookingDate,String channelCode,
                                    String debitCreditCode,String transactionStatusCode,Long originalTransactionId,
                                    String externalReference,OffsetDateTime createdAt,String createdBy,long recordVersion) {}
    public record ValidationRecord(long transactionValidationId,long transactionId,String validationCode,
                                   String validationTypeCode,String resultCode,String resultMessage,
                                   String ruleReference,OffsetDateTime validatedAt) {}
    public record AuthorizationRecord(long transactionAuthorizationId,long transactionId,int authorizationLevel,
                                      String authorizationTypeCode,String requestedBy,String decisionCode,
                                      String decidedBy,OffsetDateTime decidedAt,String reasonCode) {}
    public record TransactionLeg(long transactionLegId,long transactionId,int legNo,Long accountId,
                                 String accountReference,String debitCreditCode,BigDecimal amount,
                                 String currencyCode,String postingReference) {}
    public record TransferDetail(long transferDetailId,long transactionId,String transferTypeCode,
                                 String sourceAccountReference,String destinationAccountReference,
                                 String destinationBankCode,String paymentPurposeCode,String routingReference) {}
    public record CashDetail(long cashTransactionDetailId,long transactionId,String cashOperationCode,
                             String cashDeskCode,String tellerId,String cashCountReference,
                             String sourceOfFundsCode,String cashManagementTxnRef,String tellerSessionRef) {}
    public record ReversalRecord(long transactionReversalId,long originalTransactionId,long reversalTransactionId,
                                 String reversalReasonCode,String reversalStatusCode,Long approvalRequestId,
                                 OffsetDateTime requestedAt,OffsetDateTime postedAt) {}
    public record TransactionView(TransactionRecord transaction,List<ValidationRecord> validations,
                                  List<AuthorizationRecord> authorizations,List<TransactionLeg> legs,
                                  TransferDetail transferDetail,CashDetail cashDetail,ReversalRecord reversal) {}
    public record TransactionListView(List<TransactionView> transactions) {}
    public record TransactionActionResponse(TransactionView transaction, boolean idempotentReplay) {}
}
