package com.behsazan.corebanking.deposit.account.operations.application;

import com.behsazan.corebanking.deposit.account.error.DepositAccountNotFoundException;
import com.behsazan.corebanking.deposit.account.operations.domain.DepositAccountOperationsModels.AccountDetails;
import com.behsazan.corebanking.deposit.account.operations.domain.DepositAccountOperationsModels.AccountSearchResponse;
import com.behsazan.corebanking.deposit.account.operations.domain.DepositAccountOperationsModels.AccountSummary;
import com.behsazan.corebanking.deposit.account.operations.oracle.DepositAccountOperationsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
public class DepositAccountOperationsService {
    private static final Set<String> STATUSES = Set.of("PENDING_ACTIVATION", "ACTIVE", "CLOSED");
    private static final Set<String> FAMILIES = Set.of("QARD_SAVINGS", "CURRENT_ACCOUNT", "SHORT_TERM_DEPOSIT", "LONG_TERM_DEPOSIT");

    private final DepositAccountOperationsRepository repository;

    public DepositAccountOperationsService(DepositAccountOperationsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AccountSearchResponse search(
            String accountNo,
            String status,
            Long openingRequestId,
            Long partyId,
            String productFamilyCode,
            int offset,
            int limit
    ) {
        String normalizedStatus = normalizeOptional(status);
        String normalizedFamily = normalizeOptional(productFamilyCode);
        if (normalizedStatus != null && !STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("ACCOUNT_STATUS_CODE نامعتبر است: " + normalizedStatus);
        }
        if (normalizedFamily != null && !FAMILIES.contains(normalizedFamily)) {
            throw new IllegalArgumentException("PRODUCT_FAMILY_CODE خارج از چهار خانواده سپرده است: " + normalizedFamily);
        }
        if (openingRequestId != null && openingRequestId <= 0) {
            throw new IllegalArgumentException("openingRequestId باید مثبت باشد.");
        }
        if (partyId != null && partyId <= 0) {
            throw new IllegalArgumentException("partyId باید مثبت باشد.");
        }
        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        var items = repository.search(accountNo, normalizedStatus, openingRequestId, partyId, normalizedFamily, safeOffset, safeLimit);
        long total = repository.count(accountNo, normalizedStatus, openingRequestId, partyId, normalizedFamily);
        return new AccountSearchResponse(items, total, safeOffset, safeLimit);
    }

    @Transactional(readOnly = true)
    public AccountDetails get(long accountId) {
        if (accountId <= 0) throw new IllegalArgumentException("accountId باید مثبت باشد.");
        AccountSummary account = repository.find(accountId)
                .orElseThrow(() -> new DepositAccountNotFoundException("حساب سپرده با شناسه " + accountId + " یافت نشد."));
        return new AccountDetails(
                account,
                repository.owners(account.openingRequestId()),
                repository.events(accountId)
        );
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
