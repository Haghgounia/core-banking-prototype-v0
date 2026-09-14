package com.behsazan.corebanking.deposit.account.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class DepositAccountModels {
    private DepositAccountModels() {
    }

    public record AccountLifecycleResponse(
            long accountId,
            String accountNo,
            long openingRequestId,
            Long productVersionId,
            String currencyCode,
            BigDecimal openingAmount,
            String accountStatusCode,
            OffsetDateTime createdAt,
            OffsetDateTime activatedAt,
            boolean idempotentReplay
    ) {
    }
}
