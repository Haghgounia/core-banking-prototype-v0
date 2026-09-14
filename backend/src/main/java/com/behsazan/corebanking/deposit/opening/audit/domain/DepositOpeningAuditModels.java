package com.behsazan.corebanking.deposit.opening.audit.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class DepositOpeningAuditModels {
    private DepositOpeningAuditModels() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChangeSetCreateRequest(
            String changeTypeCode,
            String changeReasonCode,
            String changeReasonNote
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChangeSetActionRequest(String note) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FieldMutationRequest(
            String entityName,
            Long entityId,
            String fieldName,
            Object newValue
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApplyChangeSetRequest(
            List<FieldMutationRequest> changes,
            String operationName,
            String approvalReference
    ) {
    }

    public record OpeningAuditHeader(
            long openingRequestId,
            String requestNo,
            String requestStatusCode,
            long recordVersion,
            Long createdAccountId
    ) {
    }

    public record ChangeSetView(
            long changeSetId,
            long openingRequestId,
            long changeNo,
            String changeTypeCode,
            String changeStatusCode,
            String changeReasonCode,
            String changeReasonNote,
            String requestedBy,
            OffsetDateTime requestedAt,
            String approvedBy,
            OffsetDateTime approvedAt,
            Long baseRequestVersion,
            Long resultRequestVersion,
            String correlationId
    ) {
    }

    public record AuditFieldChangeView(
            long fieldChangeId,
            long auditEventId,
            String fieldName,
            String fieldChangeTypeCode,
            String oldValue,
            String newValue,
            String oldValueHash,
            String newValueHash,
            String dataTypeCode,
            boolean sensitive,
            boolean valueMasked
    ) {
    }

    public record AuditEventView(
            long auditEventId,
            long openingRequestId,
            Long changeSetId,
            String entityName,
            String entityKey,
            String eventTypeCode,
            String actorTypeCode,
            String actorId,
            String actorRoleCode,
            String channelCode,
            String orgUnitCode,
            String sourceSystemCode,
            String operationName,
            OffsetDateTime eventAt,
            String reasonCode,
            String reasonNote,
            String approvalReference,
            String correlationId,
            Long requestVersionBefore,
            Long requestVersionAfter,
            List<AuditFieldChangeView> fieldChanges
    ) {
    }

    public record StatusHistoryView(
            long statusHistoryId,
            long openingRequestId,
            String fromStatusCode,
            String toStatusCode,
            Long changeSetId,
            Long auditEventId,
            String changeReasonCode,
            String changeNote,
            String changedBy,
            OffsetDateTime changedAt,
            String correlationId
    ) {
    }

    public record SnapshotView(
            long snapshotId,
            long openingRequestId,
            Long changeSetId,
            String snapshotTypeCode,
            long requestVersionNo,
            String snapshotFormatCode,
            String snapshotHash,
            String hashAlgorithmCode,
            String correlationId,
            OffsetDateTime createdAt,
            String createdBy
    ) {
    }

    public record SnapshotDocument(
            SnapshotView metadata,
            String snapshotData
    ) {
    }

    public record AuditTrailResponse(
            OpeningAuditHeader opening,
            List<ChangeSetView> changeSets,
            List<AuditEventView> auditEvents,
            List<StatusHistoryView> statusHistory,
            List<SnapshotView> snapshots
    ) {
    }

    public record ChangeSetApplyResponse(
            ChangeSetView changeSet,
            List<Long> auditEventIds,
            long preChangeSnapshotId,
            long postChangeSnapshotId,
            long requestVersion,
            List<AuditFieldChangeView> fieldChanges
    ) {
    }

    public record OpeningCreateAuditResult(
            long auditEventId,
            int statusHistoryRows,
            Long snapshotId
    ) {
    }

    public record AuditMutationResult(
            long auditEventId,
            int fieldChangeRows,
            Long snapshotId
    ) {
    }

    public record MutableFieldState(
            OpeningAuditHeader header,
            Map<String, Object> values
    ) {
    }

    public record EntityFieldMetadata(
            String entityName,
            String fieldName,
            String dataType,
            Integer charLength,
            Integer precision,
            Integer scale,
            boolean nullable
    ) {
    }

    public record EntityRowState(
            String entityName,
            long entityId,
            long recordVersion,
            Map<String, Object> values
    ) {
    }
}
