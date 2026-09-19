package com.behsazan.corebanking.deposit.opening.operational.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class DepositOpeningOperationalModels {
    private DepositOpeningOperationalModels() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SettlementRequest(
            @JsonProperty("SETTLEMENT_REFERENCE") String settlementReference
    ) {
    }

    public record SettlementResponse(
            long openingRequestId,
            long accountId,
            String accountStatusCode,
            String activationStatusCode,
            BigDecimal obligationTotal,
            BigDecimal settledOpeningBalance,
            int fundingRowsSettled,
            int obligationRowsSettled,
            int allocationRowsCreated,
            boolean idempotentReplay
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReadinessEvidence(
            @JsonProperty("CHECK_CODE") String checkCode,
            @JsonProperty("RESULT_STATUS_CODE") String resultStatusCode,
            @JsonProperty("RESULT_REFERENCE") String resultReference,
            @JsonProperty("VALID_UNTIL") OffsetDateTime validUntil,
            @JsonProperty("SOURCE_EVALUATION_REFERENCE") String sourceEvaluationReference,
            @JsonProperty("WAIVER_REASON") String waiverReason
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReadinessEvaluationRequest(
            @JsonProperty("EVIDENCE") List<ReadinessEvidence> evidence
    ) {
    }

    public record ReadinessCheckView(
            String checkCode,
            String titleFa,
            String checkTypeCode,
            String checkPhaseCode,
            String blockingScopeCode,
            boolean required,
            String resultStatusCode,
            String resultReference,
            OffsetDateTime checkedAt,
            OffsetDateTime validUntil,
            String sourceEvaluationReference,
            String waiverReason
    ) {
    }

    public record ActivationReadinessResponse(
            long openingRequestId,
            long accountId,
            String accountStatusCode,
            String activationStatusCode,
            boolean debitCapabilityRestricted,
            List<String> blockers,
            List<ReadinessCheckView> checks,
            OffsetDateTime evaluatedAt
    ) {
    }
}
