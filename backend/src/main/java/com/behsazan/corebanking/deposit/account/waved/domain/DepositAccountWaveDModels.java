package com.behsazan.corebanking.deposit.account.waved.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class DepositAccountWaveDModels {
    private DepositAccountWaveDModels(){}

    public record ReconciliationRunRequest(LocalDate businessDate,String reconciliationTypeCode,String sourceSystemCode,String targetLedgerCode){}
    public record ReconciliationItemRequest(String sourceReference,String targetReference,BigDecimal sourceAmount,BigDecimal targetAmount,String discrepancyTypeCode,String severityCode){}
    public record ExceptionRequest(Long transactionId,String exceptionTypeCode,String reasonCode,String severityCode,String queueCode,OffsetDateTime slaDueAt){}
    public record AssignmentRequest(String queueCode,String assigneeReference,OffsetDateTime slaDueAt){}
    public record CorrectionRequest(Long originalTransactionId,Long exceptionCaseId,String correctionTypeCode,String reasonCode,LocalDate requestedEffectiveDate,String approverUserId){}
    public record RootCauseRequest(String rootCauseCode,String description,String correctiveAction,String preventiveAction,String ownerReference,LocalDate targetDate){}
    public record CorrespondentRequest(String accountTypeCode,long correspondentBankPartyId,String correspondentBic,String externalAccountReference,String settlementCurrencyCode,String statementSourceCode,String reconciliationFrequencyCode,Boolean autoMatchAllowed,BigDecimal toleranceAmount){}
    public record RewardEnrollmentRequest(long programId,String consentReference){}

    public record ReconciliationRun(long reconciliationRunId,LocalDate businessDate,String reconciliationTypeCode,String sourceSystemCode,String targetLedgerCode,String statusCode,BigDecimal totalSourceAmount,BigDecimal totalTargetAmount,BigDecimal differenceAmount,OffsetDateTime startedAt,OffsetDateTime completedAt){}
    public record ReconciliationItem(long reconciliationItemId,long reconciliationRunId,Long accountId,String sourceReference,String targetReference,BigDecimal sourceAmount,BigDecimal targetAmount,BigDecimal differenceAmount,String matchStatusCode,String discrepancyTypeCode){}
    public record Discrepancy(long discrepancyId,long reconciliationItemId,String discrepancyTypeCode,BigDecimal expectedAmount,BigDecimal actualAmount,BigDecimal differenceAmount,String severityCode,String statusCode,Long exceptionCaseId){}
    public record SuspenseOpenItem(long suspenseOpenItemId,Long accountId,String suspenseAccountCode,String sourceReference,BigDecimal amount,String currencyCode,String statusCode,String resolutionReference){}
    public record ReconciliationView(List<ReconciliationRun> runs,List<ReconciliationItem> items,List<Discrepancy> discrepancies,List<SuspenseOpenItem> suspenseItems){}

    public record ExceptionCase(long exceptionCaseId,String caseNo,Long accountId,Long transactionId,String exceptionTypeCode,String reasonCode,String severityCode,String queueCode,String statusCode,String resolutionCode){}
    public record ExceptionAssignment(long exceptionAssignmentId,long exceptionCaseId,String queueCode,String assigneeReference,OffsetDateTime assignedAt,OffsetDateTime slaDueAt,OffsetDateTime completedAt,String statusCode){}
    public record Correction(long correctionRequestId,String requestNo,long accountId,Long originalTransactionId,Long exceptionCaseId,String correctionTypeCode,String reasonCode,LocalDate requestedEffectiveDate,String requestedBy,String statusCode){}
    public record CorrectionAuthorization(long correctionAuthorizationId,long correctionRequestId,long authorizationLevel,String decisionCode,String decidedBy,OffsetDateTime decidedAt,String decisionReasonCode){}
    public record CorrectionEntry(long correctionEntryId,long correctionRequestId,Long originalTransactionId,Long correctiveTransactionId,String entryTypeCode,LocalDate effectiveDate,BigDecimal amount,String postingReference,String auditReference){}
    public record RootCause(long rootCauseAnalysisId,long exceptionCaseId,String rootCauseCode,String description,String correctiveAction,String preventiveAction,String ownerReference,LocalDate targetDate,String statusCode){}
    public record ExceptionView(List<ExceptionCase> cases,List<ExceptionAssignment> assignments,List<Correction> corrections,List<CorrectionAuthorization> authorizations,List<CorrectionEntry> entries,List<RootCause> rootCauses){}

    public record Correspondent(long extensionId,long accountId,String accountTypeCode,long correspondentBankPartyId,String correspondentBic,String externalAccountReference,String settlementCurrencyCode,String reconciliationStatusCode,Long profileId,String statementSourceCode,String reconciliationFrequencyCode,Boolean autoMatchAllowed,BigDecimal toleranceAmount,OffsetDateTime lastReconciledAt){}
    public record CorrespondentView(List<Correspondent> accounts){}

    public record RewardProgram(long programId,String programCode,String programName,String programTypeCode,Long productVersionId,LocalDate effectiveFrom,LocalDate effectiveTo,String statusCode){}
    public record RewardEnrollment(long rewardEnrollmentId,long programId,long accountId,OffsetDateTime enrolledAt,String eligibilityStatusCode,String statusCode,String consentReference){}
    public record LotteryEntry(long lotteryEntryId,long lotteryDrawId,long rewardEnrollmentId,BigDecimal eligibleBalance,long entryCount,String entryStatusCode){}
    public record LotteryWinner(long lotteryWinnerId,long lotteryDrawId,long lotteryEntryId,String prizeCode,String prizeDescription,BigDecimal prizeAmount,String winnerStatusCode,Long paymentTransactionId,String paymentReference){}
    public record RewardView(List<RewardProgram> programs,List<RewardEnrollment> enrollments,List<LotteryEntry> entries,List<LotteryWinner> winners){}

    public record WaveDView(ReconciliationView reconciliation,ExceptionView exceptions,CorrespondentView correspondent,RewardView rewards){}
}
