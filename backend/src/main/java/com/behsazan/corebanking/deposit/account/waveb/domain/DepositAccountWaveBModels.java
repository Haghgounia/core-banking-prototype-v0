package com.behsazan.corebanking.deposit.account.waveb.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class DepositAccountWaveBModels {
    private DepositAccountWaveBModels(){}

    public record StatementTemplateRequest(String templateCode,String templateName,String formatCode,String languageCode,Integer versionNo,LocalDate effectiveFrom,LocalDate effectiveTo,String templateBody){}
    public record StatementGenerateRequest(long statementTemplateId,LocalDate dateFrom,LocalDate dateTo,String requestChannelCode,String deliveryChannelCode,String deliveryTarget){}
    public record StatementDeliveryRequest(String channelCode,String destination){}
    public record AccountLimitRequest(String limitTypeCode,String transactionTypeCode,String channelCode,BigDecimal amountLimit,Long countLimit,String sourceCode,OffsetDateTime effectiveFrom,OffsetDateTime effectiveTo,String originSystemCode,String originModuleCode,String originRequestRef,String revocationPolicyCode){}
    public record TransactionRestrictionRequest(String transactionTypeCode,String channelCode,String directionCode,String restrictionModeCode,BigDecimal maxAmount,String sourceCode,String authorityReference,OffsetDateTime validFrom,OffsetDateTime validTo,String originSystemCode,String originModuleCode,String originRequestRef,String releasePolicyCode){}
    public record ControlReleaseRequest(String actionSourceSystemCode,String actionSourceModuleCode,String authorityReference){}
    public record MaturityRunRequest(LocalDate businessDate,String runModeCode){}

    public record StatementTemplate(long statementTemplateId,String templateCode,String templateName,String formatCode,String languageCode,long versionNo,LocalDate effectiveFrom,LocalDate effectiveTo,String statusCode,long recordVersion){}
    public record StatementItem(long statementItemId,long statementId,Long transactionId,long sequenceNo,LocalDate bookingDate,LocalDate valueDate,String description,BigDecimal debitAmount,BigDecimal creditAmount,BigDecimal runningBalance){}
    public record StatementDelivery(long statementDeliveryId,long statementId,String channelCode,String destination,long attemptNo,String deliveryStatusCode,OffsetDateTime sentAt,OffsetDateTime deliveredAt,String providerReference,String errorCode,String errorMessage){}
    public record Statement(long statementId,long statementRequestId,long accountId,LocalDate dateFrom,LocalDate dateTo,String requestChannelCode,String deliveryChannelCode,String deliveryTarget,BigDecimal openingBalance,BigDecimal closingBalance,String formatCode,String statementStatusCode,OffsetDateTime generatedAt,List<StatementItem> items,List<StatementDelivery> deliveries){}

    public record AccountLimit(long accountLimitId,long accountId,String limitTypeCode,String transactionTypeCode,String channelCode,BigDecimal amountLimit,Long countLimit,String periodUnitCode,Integer periodValue,String sourceCode,OffsetDateTime effectiveFrom,OffsetDateTime effectiveTo,String statusCode,String originSystemCode,String originModuleCode,String originRequestRef,String revocationPolicyCode,Long approvalRequestId,long recordVersion){}
    public record AccountLimitUsage(long accountLimitUsageId,long accountLimitId,long accountId,OffsetDateTime periodStart,OffsetDateTime periodEnd,BigDecimal usedAmount,long usedCount,Long lastTransactionId,long recordVersion){}
    public record TransactionRestriction(long accountTxRestrictionId,long accountId,String transactionTypeCode,String channelCode,String directionCode,String restrictionModeCode,BigDecimal maxAmount,String sourceCode,String authorityReference,OffsetDateTime validFrom,OffsetDateTime validTo,String statusCode,String originSystemCode,String originModuleCode,String originRequestRef,String releasePolicyCode,Long approvalRequestId,long recordVersion){}
    public record LimitControlsView(List<AccountLimit> limits,List<AccountLimitUsage> usages,List<TransactionRestriction> restrictions){}

    public record MaturityProcessItem(long maturityProcessItemId,long maturityProcessRunId,long accountId,long termContractId,Long maturityEventId,String actionCode,String itemStatusCode,String errorCode,String errorMessage,OffsetDateTime processedAt){}
    public record MaturityProcessRun(long maturityProcessRunId,LocalDate businessDate,String runModeCode,String statusCode,long totalCount,long processedCount,long failedCount,OffsetDateTime startedAt,OffsetDateTime finishedAt,List<MaturityProcessItem> items){}

    public record WaveBView(List<StatementTemplate> statementTemplates,List<Statement> statements,LimitControlsView limitControls,List<MaturityProcessRun> maturityRuns){}
    public record WaveBActionResponse(WaveBView view,long entityId,boolean replayed){}
    public record MaturityRunResponse(MaturityProcessRun run,boolean replayed){}
}
