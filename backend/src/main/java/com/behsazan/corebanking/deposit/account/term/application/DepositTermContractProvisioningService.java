package com.behsazan.corebanking.deposit.account.term.application;

import com.behsazan.corebanking.deposit.account.error.DepositAccountLifecycleException;
import com.behsazan.corebanking.deposit.account.term.oracle.DepositTermRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class DepositTermContractProvisioningService {
    private static final Set<String> TERM_FAMILIES = Set.of("SHORT_TERM_DEPOSIT", "LONG_TERM_DEPOSIT");
    private final DepositTermRepository repository;

    public DepositTermContractProvisioningService(DepositTermRepository repository) {
        this.repository = repository;
    }

    public void ensureForActivation(long accountId, long openingRequestId, long productVersionId, String actor) {
        String family = repository.productFamily(productVersionId)
                .map(DepositTermContractProvisioningService::upper)
                .orElseThrow(() -> new DepositAccountLifecycleException(
                        "خانواده محصول حساب برای Activation قابل تشخیص نیست.",
                        Map.of("DEPOSIT_ACCOUNT.CURRENT_PRODUCT_VERSION_ID", String.valueOf(productVersionId))
                ));
        if (!TERM_FAMILIES.contains(family)) {
            return;
        }
        if (repository.term(accountId).isPresent()) {
            return;
        }
        int created = repository.provisionContractFromOpening(accountId, openingRequestId, productVersionId, actor);
        if (created != 1) {
            throw new DepositAccountLifecycleException(
                    "Activation حساب مدت‌دار بدون Opening Term معتبر مجاز نیست.",
                    Map.of(
                            "DEPOSIT_OPENING_TERM.OPENING_REQUEST_ID", String.valueOf(openingRequestId),
                            "PRODUCT_FAMILY_CODE", family,
                            "DEPOSIT_TERM_CONTRACT.ACCOUNT_ID", "قرارداد عملیاتی مدت باید از داده Opening ایجاد شود."
                    )
            );
        }
    }

    private static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
