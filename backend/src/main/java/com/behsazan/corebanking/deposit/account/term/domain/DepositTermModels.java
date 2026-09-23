package com.behsazan.corebanking.deposit.account.term.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class DepositTermModels {
    private DepositTermModels() {}

    public record TermContract(long termContractId,long accountId,long productVersionId,String termCode,long termValue,String termUnitCode,
                               LocalDate startDate,LocalDate maturityDate,BigDecimal principalAmount,BigDecimal contractRate,long renewalCount,
                               Long maturitySettlementAccountId,String maturitySettlementAccountReference,String maturityInstructionSourceCode,
                               String maturityInstructionCode,String statusCode,long recordVersion) {}
    public record MaturityEvent(long maturityEventId,long termContractId,long accountId,LocalDate maturityDate,String instructionCode,
                                BigDecimal principalAmount,BigDecimal profitAmount,String actionResultCode,Long relatedRenewalId,
                                Long settlementTransactionId,OffsetDateTime processedAt) {}
    public record Renewal(long termRenewalId,long termContractId,long renewalNo,long previousProductVersionId,long newProductVersionId,
                          LocalDate fromMaturityDate,LocalDate newStartDate,LocalDate newMaturityDate,BigDecimal previousRate,BigDecimal newRate,
                          BigDecimal renewedPrincipal,Long approvalRequestId,String approvalStatusCode,String approverUserId,String statusCode,long recordVersion) {}
    public record PartialWithdrawal(long partialWithdrawalId,long termContractId,long accountId,BigDecimal requestedAmount,BigDecimal effectiveAmount,
                                    BigDecimal principalBefore,BigDecimal principalAfter,BigDecimal profitRecalcAmount,BigDecimal adjustmentAmount,
                                    Long transactionId,LocalDate effectiveDate,Long approvalRequestId,String approvalStatusCode,String approverUserId,
                                    String statusCode,long recordVersion) {}
    public record TermConversion(long termConversionId,long termContractId,long accountId,long fromProductVersionId,long toProductVersionId,
                                 LocalDate effectiveDate,BigDecimal principalBefore,BigDecimal principalAfter,BigDecimal accruedProfitAmount,
                                 BigDecimal adjustmentAmount,Long transactionId,Long approvalRequestId,String approvalStatusCode,String approverUserId,
                                 String statusCode,long recordVersion) {}
    public record EarlyTermination(long earlyTerminationId,long termContractId,long accountId,LocalDate terminationDate,BigDecimal principalAmount,
                                   BigDecimal accruedProfitAmount,BigDecimal earlyTerminationRate,BigDecimal adjustmentAmount,BigDecimal taxAmount,
                                   BigDecimal feeAmount,BigDecimal netSettlementAmount,Long settlementTransactionId,Long approvalRequestId,
                                   String statusCode,long recordVersion,Long closureId,String closureStatusCode) {}
    public record TermSettlement(long termSettlementId,long termContractId,String settlementTypeCode,BigDecimal grossPrincipal,BigDecimal grossProfit,
                                 BigDecimal taxAmount,BigDecimal feeAmount,BigDecimal adjustmentAmount,BigDecimal netAmount,
                                 String destinationAccountReference,Long transactionId,String statusCode) {}
    public record TermOperationsView(TermContract contract,List<MaturityEvent> maturityEvents,List<Renewal> renewals,
                                     List<PartialWithdrawal> partialWithdrawals,List<TermConversion> conversions,
                                     List<EarlyTermination> earlyTerminations,List<TermSettlement> settlements) {}

    public record UpdateMaturityInstructionRequest(String instructionCode,String instructionSourceCode,Long settlementAccountId,String settlementAccountReference,long expectedRecordVersion) {}
    public record RenewalRequest(Long newProductVersionId,LocalDate newMaturityDate,BigDecimal newRate,String approverUserId,String orgUnitCode) {}
    public record PartialWithdrawalRequest(BigDecimal amount,String approverUserId,String orgUnitCode) {}
    public record ConversionRequest(long toProductVersionId,LocalDate effectiveDate,String approverUserId,String orgUnitCode) {}
    public record EarlyTerminationRequest(LocalDate terminationDate,String settlementAccountReference,String approverUserId,String orgUnitCode) {}
    public record TermActionResponse(TermOperationsView state,long entityId,boolean idempotentReplay) {}
}
