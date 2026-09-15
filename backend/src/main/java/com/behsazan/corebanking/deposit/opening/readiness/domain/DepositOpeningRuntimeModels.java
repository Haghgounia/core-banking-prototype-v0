package com.behsazan.corebanking.deposit.opening.readiness.domain;

import java.time.OffsetDateTime;
import java.util.List;

public final class DepositOpeningRuntimeModels {
    private DepositOpeningRuntimeModels() {}

    public record ProductVersionContract(
            long productVersionId,
            long productId,
            String productClassCode,
            String productFamilyCode,
            String defaultCurrencyCode,
            String versionStatusCode,
            String originationStatusCode,
            String recordStatusCode,
            Integer isCurrent,
            java.time.LocalDate validFrom,
            java.time.LocalDate validTo
    ) {}

    public record RuntimeValidationResponse(
            boolean valid,
            String productFamilyCode,
            String versionStatusCode,
            String originationStatusCode,
            String recordStatusCode
    ) {}

    public record ReadinessCheck(String code, String title, String status, String detail) {}

    public record ReadinessReport(
            String status,
            OffsetDateTime checkedAt,
            List<ReadinessCheck> checks
    ) {}

    public record RollbackProbeResult(
            boolean success,
            long probeBatchId,
            String idempotencyKey,
            boolean visibleInsideTransaction,
            boolean remainedAfterRollback,
            String detail
    ) {}
}
