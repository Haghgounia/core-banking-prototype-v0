package com.behsazan.corebanking.deposit.account.application;

import com.behsazan.corebanking.deposit.account.domain.DepositAccountModels.AccountLifecycleResponse;
import com.behsazan.corebanking.deposit.account.error.DepositAccountLifecycleException;
import com.behsazan.corebanking.deposit.account.error.DepositAccountNotFoundException;
import com.behsazan.corebanking.deposit.account.oracle.DepositAccountRepository;
import com.behsazan.corebanking.deposit.account.oracle.DepositAccountRepository.AccountRow;
import com.behsazan.corebanking.deposit.account.oracle.DepositAccountRepository.OpeningLink;
import com.behsazan.corebanking.deposit.opening.audit.application.DepositOpeningAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

@Service
public class DepositAccountLifecycleService {
    private final DepositAccountRepository repository;
    private final DepositOpeningAuditService auditService;

    public DepositAccountLifecycleService(
            DepositAccountRepository repository,
            DepositOpeningAuditService auditService
    ) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional
    public AccountLifecycleResponse createAccount(long openingRequestId, String actor, String correlationId) {
        OpeningLink opening = repository.lockOpening(openingRequestId)
                .orElseThrow(() -> new DepositAccountNotFoundException(
                        "پرونده افتتاح با شناسه " + openingRequestId + " یافت نشد."
                ));

        if (opening.createdAccountId() != null) {
            AccountRow existing = repository.findAccount(opening.createdAccountId())
                    .orElseThrow(() -> new DepositAccountLifecycleException(
                            "Integration Reference حساب روی پرونده افتتاح ثبت شده، اما حساب متناظر یافت نشد.",
                            Map.of("DEPOSIT_OPENING_REQUEST.CREATED_ACCOUNT_ID", "شناسه حساب به رکورد معتبر DEPOSIT_ACCOUNT متصل نیست.")
                    ));
            return response(existing, true);
        }

        if (!"APPROVED".equals(upper(opening.requestStatusCode()))) {
            throw new DepositAccountLifecycleException(
                    "ایجاد حساب فقط برای Opening تأییدشده مجاز است.",
                    Map.of("DEPOSIT_OPENING_REQUEST.REQUEST_STATUS_CODE", "وضعیت مورد انتظار APPROVED است.")
            );
        }

        long accountId = repository.nextAccountId();
        String accountNo = technicalPrototypeAccountNo(accountId);
        repository.insertAccount(accountId, accountNo, opening, actor);
        if (repository.linkCreatedAccount(openingRequestId, accountId, actor) != 1) {
            throw new DepositAccountLifecycleException(
                    "اتصال حساب ایجادشده به پرونده Opening انجام نشد.",
                    Map.of("DEPOSIT_OPENING_REQUEST.CREATED_ACCOUNT_ID", "پرونده همزمان تغییر کرده است.")
            );
        }
        repository.insertLifecycleEvent(
                repository.nextLifecycleEventId(), accountId, openingRequestId,
                "CREATE", null, "PENDING_ACTIVATION", actor, correlationId
        );
        auditService.recordAccountLinked(openingRequestId, accountId, actor, correlationId);

        AccountRow created = repository.findAccount(accountId)
                .orElseThrow(() -> new DepositAccountLifecycleException(
                        "حساب ایجاد شد اما Read-back آن ناموفق بود.",
                        Map.of("DEPOSIT_ACCOUNT.ACCOUNT_ID", "Read-back حساب ایجادشده انجام نشد.")
                ));
        return response(created, false);
    }

    @Transactional
    public AccountLifecycleResponse activateAccount(long openingRequestId, String actor, String correlationId) {
        OpeningLink opening = repository.lockOpening(openingRequestId)
                .orElseThrow(() -> new DepositAccountNotFoundException(
                        "پرونده افتتاح با شناسه " + openingRequestId + " یافت نشد."
                ));

        if (opening.createdAccountId() == null) {
            throw new DepositAccountLifecycleException(
                    "برای این Opening هنوز حسابی ایجاد نشده است.",
                    Map.of("DEPOSIT_OPENING_REQUEST.CREATED_ACCOUNT_ID", "ابتدا عملیات ایجاد حساب را اجرا کنید.")
            );
        }
        AccountRow account = repository.lockAccount(opening.createdAccountId())
                .orElseThrow(() -> new DepositAccountLifecycleException(
                        "حساب متصل به Opening یافت نشد.",
                        Map.of("DEPOSIT_OPENING_REQUEST.CREATED_ACCOUNT_ID", "Integration Reference نامعتبر است.")
                ));
        if (!"PENDING_ACTIVATION".equals(upper(account.accountStatusCode()))) {
            throw new DepositAccountLifecycleException(
                    "Activation فقط برای حساب PENDING_ACTIVATION مجاز است.",
                    Map.of("DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE", "وضعیت جاری: " + account.accountStatusCode())
            );
        }
        if (!"APPROVED".equals(upper(opening.requestStatusCode()))) {
            throw new DepositAccountLifecycleException(
                    "Opening متصل به حساب باید پیش از Activation در وضعیت APPROVED باشد.",
                    Map.of("DEPOSIT_OPENING_REQUEST.REQUEST_STATUS_CODE", "وضعیت مورد انتظار APPROVED است.")
            );
        }

        if (repository.activateAccount(account.accountId(), actor) != 1) {
            throw new DepositAccountLifecycleException(
                    "Transition حساب به ACTIVE انجام نشد.",
                    Map.of("DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE", "Transition همزمان یا نامعتبر رخ داده است.")
            );
        }
        repository.insertLifecycleEvent(
                repository.nextLifecycleEventId(), account.accountId(), openingRequestId,
                "ACTIVATE", "PENDING_ACTIVATION", "ACTIVE", actor, correlationId
        );
        if (repository.completeOpening(openingRequestId, actor) != 1) {
            throw new DepositAccountLifecycleException(
                    "حساب فعال شد اما پرونده Opening قابل انتقال به COMPLETED نبود.",
                    Map.of("DEPOSIT_OPENING_REQUEST.REQUEST_STATUS_CODE", "Transition APPROVED -> COMPLETED نامعتبر است.")
            );
        }
        auditService.recordOpeningCompleted(openingRequestId, actor, correlationId);

        AccountRow activated = repository.findAccount(account.accountId())
                .orElseThrow(() -> new DepositAccountLifecycleException(
                        "حساب فعال شد اما Read-back آن ناموفق بود.",
                        Map.of("DEPOSIT_ACCOUNT.ACCOUNT_ID", "Read-back حساب فعال‌شده انجام نشد.")
                ));
        return response(activated, false);
    }

    @Transactional(readOnly = true)
    public AccountLifecycleResponse getAccount(long openingRequestId) {
        OpeningLink opening = repository.findOpening(openingRequestId)
                .orElseThrow(() -> new DepositAccountNotFoundException(
                        "پرونده افتتاح با شناسه " + openingRequestId + " یافت نشد."
                ));
        if (opening.createdAccountId() == null) {
            throw new DepositAccountNotFoundException("برای این Opening هنوز حسابی ایجاد نشده است.");
        }
        AccountRow account = repository.findAccount(opening.createdAccountId())
                .orElseThrow(() -> new DepositAccountNotFoundException("حساب متصل به Opening یافت نشد."));
        return response(account, false);
    }

    private static AccountLifecycleResponse response(AccountRow row, boolean replay) {
        return new AccountLifecycleResponse(
                row.accountId(), row.accountNo(), row.openingRequestId(), row.productVersionId(),
                row.currencyCode(), row.openingAmount(), row.accountStatusCode(),
                row.createdAt(), row.activatedAt(), replay
        );
    }

    private static String technicalPrototypeAccountNo(long accountId) {
        return "DPA" + String.format(Locale.ROOT, "%017d", accountId);
    }

    private static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
