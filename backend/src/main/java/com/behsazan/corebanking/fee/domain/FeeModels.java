package com.behsazan.corebanking.fee.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public final class FeeModels {
    private FeeModels() {}

    public record LookupOption(String code, String nameFa) {}

    public record FeePrototypeMetadata(
            List<LookupOption> categories,
            List<LookupOption> activities,
            List<LookupOption> transactionTypes,
            List<LookupOption> channels,
            List<LookupOption> currencies,
            List<LookupOption> customerSegments,
            List<LookupOption> organizations,
            List<LookupOption> calculationStrategies,
            List<LookupOption> adjustmentTypes,
            List<LookupOption> beneficiaryRoles,
            List<LookupOption> collectionMethods,
            List<LookupOption> timingModes
    ) {}

    public record CalculateFeeRequest(
            @NotBlank String feeCode,
            @NotBlank String strategy,
            @NotNull @PositiveOrZero BigDecimal basisAmount,
            @NotBlank String currencyCode,
            BigDecimal fixedAmount,
            BigDecimal rate,
            BigDecimal minFeeAmount,
            BigDecimal maxFeeAmount,
            BigDecimal discountRate
    ) {}

    public record CalculationStep(int sequence, String title, String explanation, BigDecimal amount) {}

    public record CalculateFeeResponse(
            String feeCode,
            String strategy,
            BigDecimal basisAmount,
            String currencyCode,
            BigDecimal grossFeeAmount,
            BigDecimal adjustmentAmount,
            BigDecimal netFeeAmount,
            List<CalculationStep> steps
    ) {}
}
