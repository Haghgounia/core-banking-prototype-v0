package com.behsazan.corebanking.deposit.opening.batch.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class DepositOpeningBatchModels {
    private DepositOpeningBatchModels() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BatchCreateRequest(
            String batchNo,
            String idempotencyKey,
            String sourceTypeCode,
            String sourceReference,
            String bulkOpeningBasisCode,
            String legalBasisReference,
            String cddApprovalReference,
            List<BatchItemCreateRequest> items
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BatchItemCreateRequest(
            Integer rowNo,
            String externalRowKey,
            Long partyId,
            Long productVersionId,
            String currencyCode,
            BigDecimal openingAmount
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BatchProcessRequest(
            String openingChannelCode,
            String orgUnitCode,
            LocalDate requestedOpeningDate
    ) {}

    public record BatchHeaderView(
            long openingBatchId,
            String batchNo,
            String idempotencyKey,
            String sourceTypeCode,
            String sourceReference,
            String bulkOpeningBasisCode,
            String legalBasisReference,
            String cddApprovalReference,
            int totalCount,
            int successCount,
            int failedCount,
            String batchStatusCode,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            long recordVersion
    ) {}

    public record BatchItemView(
            long openingBatchItemId,
            long openingBatchId,
            int rowNo,
            String externalRowKey,
            Long partyId,
            Long productVersionId,
            String currencyCode,
            BigDecimal openingAmount,
            String itemStatusCode,
            Long openingRequestId,
            Long accountId,
            String requestNo,
            String accountNo,
            String accountStatusCode,
            long recordVersion
    ) {}

    public record BatchErrorView(
            long openingBatchErrorId,
            long openingBatchItemId,
            String errorStageCode,
            String errorCode,
            String fieldName,
            String errorMessage,
            boolean retryable,
            LocalDateTime createdAt
    ) {}

    public record BatchView(
            BatchHeaderView batch,
            List<BatchItemView> items,
            List<BatchErrorView> errors,
            boolean idempotentReplay
    ) {}
}
