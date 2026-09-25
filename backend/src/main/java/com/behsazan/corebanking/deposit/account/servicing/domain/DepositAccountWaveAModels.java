package com.behsazan.corebanking.deposit.account.servicing.domain;

import com.behsazan.corebanking.deposit.account.operations.domain.DepositAccountOperationsModels.AccountDetails;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class DepositAccountWaveAModels {
    private DepositAccountWaveAModels() {}

    public record AccountAttribute(long accountAttributeId,long accountId,String attributeCode,String attributeValue,String valueTypeCode,LocalDate validFrom,LocalDate validTo,long recordVersion) {}
    public record ConditionOverride(long accountConditionOverrideId,long accountId,String conditionCode,String baseValue,String overrideValue,String valueTypeCode,LocalDate validFrom,LocalDate validTo,String reasonCode,Long approvalRequestId,String trackingNo,String approvalStatusCode,String approverUserId,long recordVersion) {}
    public record SignatoryAuthority(long signatoryAuthorityId,long signatoryId,long partyId,String signatoryRoleCode,String operationCode,String channelCode,BigDecimal maxAmount,String currencyCode,boolean requiresCosign,LocalDate validFrom,LocalDate validTo,String statusCode) {}
    public record ProductVersionCandidate(long productVersionId) {}
    public record ProductChange(long accountProductHistoryId,long accountId,long fromProductVersionId,long toProductVersionId,LocalDate effectiveDate,String changeReasonCode,Long approvalRequestId,String trackingNo,String approvalStatusCode,String approverUserId,String migrationStatusCode) {}
    public record ActivationCheck(long activationCheckId,long activationRunId,long accountId,String requirementCode,String requirementTypeCode,String blockingScopeCode,boolean required,String resultStatusCode,String sourceSystemCode,String sourceReference,String details,OffsetDateTime evaluatedAt,OffsetDateTime validUntil) {}
    public record ActivationRun(long activationRunId,long accountId,Long openingRequestId,long runNo,String correlationId,String triggerCode,String runStatusCode,long mandatoryCheckCount,long passedCheckCount,long blockingCheckCount,boolean ready,OffsetDateTime requestedAt,OffsetDateTime evaluatedAt,List<ActivationCheck> checks) {}
    public record BulkActionItem(long bulkActionItemId,long bulkActionId,long accountId,String itemStatusCode,String errorCode,String errorMessage,OffsetDateTime executedAt) {}
    public record BulkAction(long bulkActionId,String bulkActionNo,String actionTypeCode,String reasonCode,long totalCount,long successCount,long failedCount,String actionStatusCode,OffsetDateTime requestedAt,List<BulkActionItem> items) {}

    public record ServicingControlsView(
            AccountDetails account,
            List<AccountAttribute> attributes,
            List<ConditionOverride> conditionOverrides,
            List<SignatoryAuthority> signatoryAuthorities,
            List<ProductVersionCandidate> productVersionCandidates,
            List<ProductChange> productChanges,
            ActivationRun latestActivationRun,
            List<BulkAction> recentBulkActions
    ) {}

    public record AttributeRequest(String attributeCode,String attributeValue,String valueTypeCode,LocalDate validFrom,String reasonCode) {}
    public record ConditionOverrideRequest(String conditionCode,String baseValue,String overrideValue,String valueTypeCode,LocalDate validFrom,LocalDate validTo,String reasonCode,String approverUserId,String orgUnitCode) {}
    public record ProductChangeRequest(long toProductVersionId,LocalDate effectiveDate,String changeReasonCode,String approverUserId,String orgUnitCode) {}
    public record SignatoryAuthorityRequest(long partyId,String signatoryRoleCode,String operationCode,String channelCode,BigDecimal maxAmount,String currencyCode,Boolean requiresCosign,LocalDate validFrom) {}
    public record ActivationRunRequest(String triggerCode) {}
    public record BulkActionRequest(String actionTypeCode,List<Long> accountIds,String reasonCode,String holdTypeCode,BigDecimal holdAmount,String currencyCode,String holdReasonCode,Map<Long,Long> releaseHoldIds) {}

    public record WaveAActionResponse(ServicingControlsView state,long entityId,boolean idempotentReplay) {}
    public record ActivationActionResponse(ServicingControlsView state,long activationRunId,boolean idempotentReplay) {}
    public record BulkActionResponse(BulkAction bulkAction,boolean idempotentReplay) {}
}
