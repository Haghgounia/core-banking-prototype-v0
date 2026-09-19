package com.behsazan.corebanking.deposit.account.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public final class DepositAccountModels {
    private DepositAccountModels() {
    }

    public record AccountLifecycleResponse(
            long accountId,
            String accountNo,
            long openingRequestId,
            Long productVersionId,
            Long openedProductVersionId,
            Long currentProductVersionId,
            String ownershipTypeCode,
            String currencyCode,
            BigDecimal openingAmount,
            LocalDate openedOn,
            String accountStatusCode,
            String openingActivationStatusCode,
            OffsetDateTime activationDeadlineAt,
            String activationPolicyVersion,
            BigDecimal ledgerBalance,
            BigDecimal availableBalance,
            String debitCapabilityCode,
            OffsetDateTime createdAt,
            OffsetDateTime activatedAt,
            boolean idempotentReplay
    ) {
    }
}
