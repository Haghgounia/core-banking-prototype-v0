package com.behsazan.corebanking.deposit.account.servicing.application;

import com.behsazan.corebanking.deposit.account.error.DepositAccountLifecycleException;
import com.behsazan.corebanking.deposit.account.error.DepositAccountNotFoundException;
import com.behsazan.corebanking.deposit.account.operations.application.DepositAccountOperationsService;
import com.behsazan.corebanking.deposit.account.servicing.domain.DepositAccountServicingModels.CloseAccountRequest;
import com.behsazan.corebanking.deposit.account.servicing.domain.DepositAccountServicingModels.CloseAccountResponse;
import com.behsazan.corebanking.deposit.account.servicing.oracle.DepositAccountServicingRepository;
import com.behsazan.corebanking.deposit.account.servicing.oracle.DepositAccountServicingRepository.AccountLockRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

@Service
public class DepositAccountServicingService {
    private final DepositAccountServicingRepository repository;
    private final DepositAccountOperationsService operationsService;

    public DepositAccountServicingService(
            DepositAccountServicingRepository repository,
            DepositAccountOperationsService operationsService
    ) {
        this.repository = repository;
        this.operationsService = operationsService;
    }

    @Transactional
    public CloseAccountResponse close(
            long accountId,
            CloseAccountRequest request,
            String actor,
            String correlationId
    ) {
        if (accountId <= 0) throw new IllegalArgumentException("accountId باید مثبت باشد.");
        if (request == null || request.expectedRecordVersion() <= 0) {
            throw new IllegalArgumentException("expectedRecordVersion باید مثبت باشد.");
        }

        AccountLockRow account = repository.lockAccount(accountId)
                .orElseThrow(() -> new DepositAccountNotFoundException(
                        "حساب سپرده با شناسه " + accountId + " یافت نشد."
                ));

        String status = upper(account.accountStatusCode());
        if ("CLOSED".equals(status)) {
            return new CloseAccountResponse(operationsService.get(accountId), true);
        }
        if (!"ACTIVE".equals(status)) {
            throw new DepositAccountLifecycleException(
                    "بستن حساب فقط برای حساب ACTIVE مجاز است.",
                    Map.of("DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE", "وضعیت جاری: " + account.accountStatusCode())
            );
        }
        if (account.recordVersion() != request.expectedRecordVersion()) {
            throw new DepositAccountLifecycleException(
                    "نسخه حساب تغییر کرده است؛ اطلاعات حساب را تازه‌سازی کنید.",
                    Map.of(
                            "DEPOSIT_ACCOUNT.RECORD_VERSION",
                            "نسخه مورد انتظار " + request.expectedRecordVersion()
                                    + " اما نسخه جاری " + account.recordVersion() + " است."
                    )
            );
        }

        if (repository.closeAccount(accountId, request.expectedRecordVersion(), actor) != 1) {
            throw new DepositAccountLifecycleException(
                    "Transition حساب از ACTIVE به CLOSED انجام نشد.",
                    Map.of("DEPOSIT_ACCOUNT.RECORD_VERSION", "حساب همزمان تغییر کرده است؛ مجدداً استعلام کنید.")
            );
        }
        repository.insertCloseEvent(
                repository.nextLifecycleEventId(),
                account.accountId(),
                account.openingRequestId(),
                actor,
                correlationId
        );

        return new CloseAccountResponse(operationsService.get(accountId), false);
    }

    private static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
