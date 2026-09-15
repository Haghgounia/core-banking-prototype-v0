package com.behsazan.corebanking.deposit.account.servicing.domain;

import com.behsazan.corebanking.deposit.account.operations.domain.DepositAccountOperationsModels.AccountDetails;

public final class DepositAccountServicingModels {
    private DepositAccountServicingModels() {
    }

    public record CloseAccountRequest(
            long expectedRecordVersion
    ) {
    }

    public record CloseAccountResponse(
            AccountDetails account,
            boolean idempotentReplay
    ) {
    }
}
