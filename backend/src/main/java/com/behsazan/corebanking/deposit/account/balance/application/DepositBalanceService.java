package com.behsazan.corebanking.deposit.account.balance.application;

import com.behsazan.corebanking.deposit.account.balance.domain.DepositBalanceModels.*;
import com.behsazan.corebanking.deposit.account.balance.oracle.DepositBalanceRepository;
import com.behsazan.corebanking.deposit.account.balance.oracle.DepositBalanceRepository.AccountFinancialRow;
import com.behsazan.corebanking.deposit.account.balance.oracle.DepositBalanceRepository.ReservationLockRow;
import com.behsazan.corebanking.deposit.account.error.DepositAccountLifecycleException;
import com.behsazan.corebanking.deposit.account.error.DepositAccountNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class DepositBalanceService {
    private static final Set<String> DCS = Set.of("DEBIT","CREDIT");
    private static final Set<String> RESERVATION_TYPES = Set.of("DEBIT_AUTHORIZATION","FEE","TAX","CLOSURE","OTHER");
    private final DepositBalanceRepository repository;

    public DepositBalanceService(DepositBalanceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public BalanceView get(long accountId) {
        requireAccount(accountId, false);
        BalanceSnapshot balance = repository.findBalance(accountId, false)
                .orElseThrow(() -> new DepositAccountLifecycleException(
                        "مانده عملیاتی حساب ایجاد نشده است؛ Phase 11D migration را اجرا کنید.",
                        Map.of("DEPOSIT_ACCOUNT_BALANCE.ACCOUNT_ID", "Balance row برای حساب موجود نیست.")));
        return new BalanceView(balance, repository.listSubledger(accountId, 100), repository.listReservations(accountId));
    }

    @Transactional(readOnly = true)
    public BalanceSnapshot currentBalance(long accountId) {
        requireAccount(accountId, false);
        return repository.findBalance(accountId, false)
                .orElseThrow(() -> new DepositAccountLifecycleException(
                        "مانده عملیاتی حساب یافت نشد.",
                        Map.of("DEPOSIT_ACCOUNT_BALANCE.ACCOUNT_ID", "Phase 11D balance row الزامی است.")));
    }

    @Transactional
    public BalanceSnapshot initializeAccount(long accountId, String currencyCode, String actor) {
        AccountFinancialRow account = requireAccount(accountId, true);
        BalanceSnapshot existing = repository.findBalance(accountId, true).orElse(null);
        if (existing != null) return existing;
        String currency = upper(currencyCode == null ? account.currencyCode() : currencyCode);
        if (!upper(account.currencyCode()).equals(currency)) throw new IllegalArgumentException("ارز Balance باید با ارز حساب یکسان باشد.");
        repository.insertBalance(repository.nextBalanceId(), accountId, currency, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, null, actor);
        return repository.findBalance(accountId, false).orElseThrow();
    }

    @Transactional
    public BalanceSnapshot initializeOpeningBalance(long accountId, BigDecimal openingBalance, String currencyCode,
                                                    long openingRequestId, LocalDate openedOn,
                                                    String settlementReference, String actor) {
        if (openingBalance == null || openingBalance.signum() < 0) throw new IllegalArgumentException("Opening balance نامعتبر است.");
        AccountFinancialRow account = requireAccount(accountId, true);
        String currency = upper(currencyCode == null ? account.currencyCode() : currencyCode);
        if (!upper(account.currencyCode()).equals(currency)) throw new IllegalArgumentException("ارز Opening Balance با ارز حساب یکسان نیست.");
        BalanceSnapshot balance = ensureBalanceLocked(account, actor);
        if (openingBalance.signum() == 0) return refreshLocked(accountId, balance, balance.ledgerBalance(), null, actor);
        if (balance.ledgerBalance().signum() != 0 || balance.lastSubledgerEntryId() != null) {
            if (balance.ledgerBalance().compareTo(openingBalance) == 0) return balance;
            throw new DepositAccountLifecycleException("Opening Balance قبلاً مقداردهی شده است.",
                    Map.of("DEPOSIT_ACCOUNT_BALANCE.LEDGER_BALANCE", "مقدار موجود با Settlement جدید یکسان نیست."));
        }
        long entryId = repository.nextSubledgerId();
        String postingReference = "OPENING-" + openingRequestId + "-" + shortHash(settlementReference == null ? "" : settlementReference);
        LocalDate date = openedOn == null ? LocalDate.now(ZoneOffset.UTC) : openedOn;
        repository.insertSubledger(entryId, accountId, null, null, 1, postingReference,
                "DEPOSIT_OPENING_REQUEST", openingRequestId, "CREDIT", openingBalance, currency,
                date, date, null, settlementReference, actor);
        return refreshLocked(accountId, balance, openingBalance, entryId, actor);
    }

    @Transactional
    public BalanceSnapshot refreshBalance(long accountId, String actor) {
        AccountFinancialRow account = requireAccount(accountId, true);
        repository.expireReservations(accountId, actor);
        BalanceSnapshot balance = ensureBalanceLocked(account, actor);
        return refreshLocked(accountId, balance, balance.ledgerBalance(), null, actor);
    }

    @Transactional
    public PostEntryResponse post(long accountId, PostEntryRequest request, String actor, String correlationId, String idempotencyKey) {
        if (request == null) throw new IllegalArgumentException("اطلاعات Posting الزامی است.");
        String dc = upper(request.debitCreditCode());
        if (!DCS.contains(dc)) throw new IllegalArgumentException("DEBIT_CREDIT_CODE نامعتبر است: " + dc);
        BigDecimal amount = request.amount();
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("مبلغ Posting باید مثبت باشد.");
        String postingReference = required(request.postingReference(), "POSTING_REFERENCE");
        if (postingReference.length() > 80) throw new IllegalArgumentException("POSTING_REFERENCE حداکثر 80 کاراکتر است.");
        String sourceType = upper(required(request.sourceEntityType(), "SOURCE_ENTITY_TYPE"));
        if (sourceType.length() > 60) throw new IllegalArgumentException("SOURCE_ENTITY_TYPE حداکثر 60 کاراکتر است.");
        if (request.sourceEntityId() == null || request.sourceEntityId() <= 0) throw new IllegalArgumentException("SOURCE_ENTITY_ID باید مثبت باشد.");
        String currency = upper(required(request.currencyCode(), "CURRENCY_CODE"));
        LocalDate bookingDate = request.bookingDate() == null ? LocalDate.now(ZoneOffset.UTC) : request.bookingDate();
        LocalDate valueDate = request.valueDate() == null ? bookingDate : request.valueDate();
        boolean hasTransactionTrace = request.transactionId() != null || request.transactionLegId() != null || request.entrySequenceNo() != null;
        if (hasTransactionTrace) {
            if (request.transactionId() == null || request.transactionId() <= 0) throw new IllegalArgumentException("TRANSACTION_ID باید مثبت باشد.");
            if (request.transactionLegId() == null || request.transactionLegId() <= 0) throw new IllegalArgumentException("TRANSACTION_LEG_ID باید مثبت باشد.");
            if (request.entrySequenceNo() == null || request.entrySequenceNo() <= 0) throw new IllegalArgumentException("ENTRY_SEQUENCE_NO باید مثبت باشد.");
        }
        String payload = String.join("|", dc, amount.toPlainString(), currency, bookingDate.toString(), valueDate.toString(), postingReference,
                sourceType, String.valueOf(request.sourceEntityId()), Objects.toString(request.reversalOfEntryId(), ""), Objects.toString(request.glPostingReference(), ""),
                Objects.toString(request.transactionId(), ""), Objects.toString(request.transactionLegId(), ""), Objects.toString(request.entrySequenceNo(), ""));
        String hash = hash(payload);
        if (replayOrClaim(idempotencyKey, accountId, "SUBLEDGER_POST", hash, actor, correlationId)) {
            long entryId = parseReference(repository.findIdempotency(idempotencyKey).orElseThrow().resultReference(), "SUBLEDGER:");
            return new PostEntryResponse(get(accountId), entryId, true);
        }

        AccountFinancialRow account = requireAccount(accountId, true);
        String status = upper(account.accountStatusCode());
        if ("DEBIT".equals(dc) && !"ACTIVE".equals(status))
            throw lifecycle("Debit Posting فقط برای حساب ACTIVE مجاز است.", "DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE", status);
        if ("CREDIT".equals(dc) && Set.of("PENDING_ACTIVATION","CLOSED").contains(status))
            throw lifecycle("Credit Posting برای وضعیت جاری حساب مجاز نیست.", "DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE", status);
        if (!upper(account.currencyCode()).equals(currency)) throw new IllegalArgumentException("ارز Posting باید با ارز حساب یکسان باشد.");

        repository.expireReservations(accountId, actor);
        BalanceSnapshot balance = ensureBalanceLocked(account, actor);
        BalanceMath before = math(accountId, balance.ledgerBalance());
        if ("DEBIT".equals(dc)) {
            if (repository.activeDebitBlockHoldCount(accountId) > 0) throw lifecycle("Debit به علت Hold فعال مجاز نیست.", "DEPOSIT_ACCOUNT_HOLD", "FULL/DEBIT_ONLY");
            if (before.available().compareTo(amount) < 0) throw lifecycle("مانده قابل برداشت کافی نیست.", "DEPOSIT_ACCOUNT_BALANCE.AVAILABLE_BALANCE", before.available().toPlainString());
        } else if (repository.activeCreditBlockHoldCount(accountId) > 0) {
            throw lifecycle("Credit به علت Hold فعال مجاز نیست.", "DEPOSIT_ACCOUNT_HOLD", "FULL/CREDIT_ONLY");
        }
        if (request.reversalOfEntryId() != null) {
            SubledgerEntry original = repository.findSubledger(request.reversalOfEntryId()).orElseThrow(() -> new IllegalArgumentException("Subledger entry مرجع Reversal یافت نشد."));
            if (original.accountId() != accountId) throw new IllegalArgumentException("Reversal entry متعلق به حساب دیگری است.");
            if (original.amount().compareTo(amount) != 0 || !original.currencyCode().equals(currency) || original.debitCreditCode().equals(dc))
                throw new IllegalArgumentException("Reversal باید مبلغ/ارز برابر و جهت معکوس Entry اصلی داشته باشد.");
        }

        long entryId = repository.nextSubledgerId();
        repository.insertSubledger(entryId, accountId, request.transactionId(), request.transactionLegId(),
                request.entrySequenceNo() == null ? 1 : request.entrySequenceNo(), postingReference, sourceType, request.sourceEntityId(), dc,
                amount, currency, bookingDate, valueDate, request.reversalOfEntryId(), trimToNull(request.glPostingReference()), actor);
        BigDecimal newLedger = "CREDIT".equals(dc) ? balance.ledgerBalance().add(amount) : balance.ledgerBalance().subtract(amount);
        refreshLocked(accountId, balance, newLedger, entryId, actor);
        repository.completeIdempotency(idempotencyKey, "SUBLEDGER:" + entryId);
        return new PostEntryResponse(get(accountId), entryId, false);
    }

    @Transactional
    public void attachTransactionTrace(long subledgerEntryId,long transactionId,long transactionLegId) {
        if (subledgerEntryId<=0 || transactionId<=0 || transactionLegId<=0) throw new IllegalArgumentException("شناسه Trace تراکنش نامعتبر است.");
        if (repository.attachTransactionTrace(subledgerEntryId,transactionId,transactionLegId)!=1)
            throw new DepositAccountLifecycleException("اتصال Subledger به Transaction/Leg ناموفق بود.",Map.of("DEPOSIT_SUBLEDGER_ENTRY.SUBLEDGER_ENTRY_ID",String.valueOf(subledgerEntryId)));
    }

    @Transactional
    public ReservationActionResponse createReservation(long accountId, CreateReservationRequest request, String actor, String correlationId, String idempotencyKey) {
        if (request == null) throw new IllegalArgumentException("اطلاعات Reservation الزامی است.");
        String type = upper(request.reservationTypeCode());
        if (!RESERVATION_TYPES.contains(type)) throw new IllegalArgumentException("RESERVATION_TYPE_CODE نامعتبر است: " + type);
        BigDecimal amount = request.amount();
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("مبلغ Reservation باید مثبت باشد.");
        String reference = required(request.reservationReference(), "RESERVATION_REFERENCE");
        if (reference.length() > 80) throw new IllegalArgumentException("RESERVATION_REFERENCE حداکثر 80 کاراکتر است.");
        String currency = upper(required(request.currencyCode(), "CURRENCY_CODE"));
        if (request.expiresAt() != null && !request.expiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC)))
            throw new IllegalArgumentException("EXPIRES_AT باید در آینده باشد.");
        validateReservationOwner(request);
        String payload = type+"|"+amount.toPlainString()+"|"+currency+"|"+reference+"|"+Objects.toString(request.expiresAt(),"")+"|"+
                Objects.toString(request.transactionId(),"")+"|"+Objects.toString(request.ownerEntityType(),"")+"|"+Objects.toString(request.ownerEntityId(),"")+"|"+Objects.toString(request.ownerReference(),"");
        String hash = hash(payload);
        if (replayOrClaim(idempotencyKey, accountId, "BALANCE_RESERVE", hash, actor, correlationId)) {
            long reservationId = parseReference(repository.findIdempotency(idempotencyKey).orElseThrow().resultReference(), "RESERVATION:");
            return new ReservationActionResponse(get(accountId), reservationId, true);
        }

        AccountFinancialRow account = requireAccount(accountId, true);
        if (!"ACTIVE".equals(upper(account.accountStatusCode()))) throw lifecycle("Reservation فقط برای حساب ACTIVE مجاز است.", "DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE", account.accountStatusCode());
        if (!upper(account.currencyCode()).equals(currency)) throw new IllegalArgumentException("ارز Reservation باید با ارز حساب یکسان باشد.");
        repository.expireReservations(accountId, actor);
        BalanceSnapshot balance = ensureBalanceLocked(account, actor);
        BalanceMath before = math(accountId, balance.ledgerBalance());
        if (repository.activeDebitBlockHoldCount(accountId) > 0) throw lifecycle("Reservation به علت Hold بدهکارکننده مجاز نیست.", "DEPOSIT_ACCOUNT_HOLD", "FULL/DEBIT_ONLY");
        if (before.available().compareTo(amount) < 0) throw lifecycle("مانده قابل رزرو کافی نیست.", "DEPOSIT_ACCOUNT_BALANCE.AVAILABLE_BALANCE", before.available().toPlainString());

        long reservationId = repository.nextReservationId();
        repository.insertReservation(reservationId, accountId, request.transactionId(), reference, type, amount, currency,
                request.expiresAt(), upper(trimToNull(request.ownerEntityType())), request.ownerEntityId(), trimToNull(request.ownerReference()), actor);
        refreshLocked(accountId, balance, balance.ledgerBalance(), null, actor);
        repository.completeIdempotency(idempotencyKey, "RESERVATION:" + reservationId);
        return new ReservationActionResponse(get(accountId), reservationId, false);
    }

    @Transactional
    public ReservationActionResponse releaseReservation(long accountId, long reservationId, ReleaseReservationRequest request,
                                                         String actor, String correlationId, String idempotencyKey) {
        String reason = upper(required(request == null ? null : request.reasonCode(), "RELEASE_REASON_CODE"));
        if (reason.length() > 40) throw new IllegalArgumentException("RELEASE_REASON_CODE حداکثر 40 کاراکتر است.");
        String hash = hash("BALANCE_RELEASE|"+accountId+"|"+reservationId+"|"+reason);
        if (replayOrClaim(idempotencyKey, accountId, "BALANCE_RELEASE", hash, actor, correlationId))
            return new ReservationActionResponse(get(accountId), reservationId, true);

        AccountFinancialRow account = requireAccount(accountId, true);
        repository.expireReservations(accountId, actor);
        BalanceSnapshot balance = ensureBalanceLocked(account, actor);
        ReservationLockRow row = repository.lockReservation(accountId, reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation موردنظر برای این حساب یافت نشد."));
        if (!"ACTIVE".equals(upper(row.statusCode()))) throw new IllegalArgumentException("فقط Reservation فعال قابل Release است.");
        if (repository.releaseReservation(reservationId, reason, actor) != 1)
            throw lifecycle("Reservation همزمان تغییر کرده است.", "DEPOSIT_BALANCE_RESERVATION.RECORD_VERSION", String.valueOf(row.recordVersion()));
        refreshLocked(accountId, balance, balance.ledgerBalance(), null, actor);
        repository.completeIdempotency(idempotencyKey, "RESERVATION:" + reservationId);
        return new ReservationActionResponse(get(accountId), reservationId, false);
    }

    private BalanceSnapshot ensureBalanceLocked(AccountFinancialRow account, String actor) {
        BalanceSnapshot existing = repository.findBalance(account.accountId(), true).orElse(null);
        if (existing != null) return existing;
        BigDecimal ledger = account.legacyLedgerBalance() == null ? BigDecimal.ZERO : account.legacyLedgerBalance();
        BalanceMath m = math(account.accountId(), ledger);
        repository.insertBalance(repository.nextBalanceId(), account.accountId(), account.currencyCode(), ledger, m.available(), m.blocked(), m.pendingDebit(), null, actor);
        return repository.findBalance(account.accountId(), true).orElseThrow();
    }

    private BalanceSnapshot refreshLocked(long accountId, BalanceSnapshot balance, BigDecimal ledger, Long lastEntryId, String actor) {
        BalanceMath m = math(accountId, ledger);
        if (repository.updateBalance(accountId, balance.recordVersion(), ledger, m.available(), m.blocked(), m.pendingDebit(), lastEntryId, actor) != 1)
            throw lifecycle("Balance همزمان تغییر کرده است.", "DEPOSIT_ACCOUNT_BALANCE.RECORD_VERSION", String.valueOf(balance.recordVersion()));
        return repository.findBalance(accountId, false).orElseThrow();
    }

    private BalanceMath math(long accountId, BigDecimal ledger) {
        BigDecimal partial = repository.activePartialHoldTotal(accountId);
        BigDecimal reservations = repository.activeReservationTotal(accountId);
        boolean debitBlocked = repository.activeDebitBlockHoldCount(accountId) > 0;
        BigDecimal blocked = debitBlocked ? ledger.max(BigDecimal.ZERO) : partial;
        BigDecimal available = debitBlocked ? BigDecimal.ZERO : ledger.subtract(partial).subtract(reservations).max(BigDecimal.ZERO);
        return new BalanceMath(blocked, reservations, available);
    }

    private AccountFinancialRow requireAccount(long accountId, boolean lock) {
        if (accountId <= 0) throw new IllegalArgumentException("accountId باید مثبت باشد.");
        return repository.findAccount(accountId, lock).orElseThrow(() -> new DepositAccountNotFoundException("حساب سپرده با شناسه "+accountId+" یافت نشد."));
    }

    private boolean replayOrClaim(String key,long accountId,String operation,String payloadHash,String actor,String correlation) {
        String k = trimToNull(key);
        if (k == null) throw new IllegalArgumentException("X-Idempotency-Key الزامی است.");
        var existing = repository.findIdempotency(k);
        if (existing.isPresent()) {
            var x = existing.get();
            if (!Objects.equals(x.accountId(),accountId) || !operation.equals(x.operationType()) || !payloadHash.equals(x.payloadHash()))
                throw new IllegalArgumentException("Idempotency-Key قبلاً برای درخواست دیگری استفاده شده است.");
            if ("COMPLETED".equals(x.processingStatus())) return true;
            throw new IllegalArgumentException("درخواست با این Idempotency-Key در حال پردازش است.");
        }
        repository.insertIdempotency(k,accountId,operation,payloadHash,actor,k,correlation);
        return false;
    }

    private static void validateReservationOwner(CreateReservationRequest r) {
        boolean tx = r.transactionId() != null;
        boolean ownerType = trimToNull(r.ownerEntityType()) != null;
        boolean ownerIdentity = r.ownerEntityId() != null || trimToNull(r.ownerReference()) != null;
        if (tx && (ownerType || ownerIdentity)) throw new IllegalArgumentException("Reservation تراکنشی نباید Owner Entity داشته باشد.");
        if (!tx && (!ownerType || !ownerIdentity)) throw new IllegalArgumentException("Reservation غیرتراکنشی به OWNER_ENTITY_TYPE و Owner ID/Reference نیاز دارد.");
    }

    private static DepositAccountLifecycleException lifecycle(String message,String field,String detail){return new DepositAccountLifecycleException(message,Map.of(field,detail==null?"—":detail));}
    private static String required(String v,String field){String x=trimToNull(v);if(x==null)throw new IllegalArgumentException(field+" الزامی است.");return x;}
    private static String trimToNull(String v){return v==null||v.isBlank()?null:v.trim();}
    private static String upper(String v){String x=trimToNull(v);return x==null?null:x.toUpperCase(Locale.ROOT);}
    private static String hash(String value){try{MessageDigest d=MessageDigest.getInstance("SHA-256");return HexFormat.of().formatHex(d.digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private static String shortHash(String value){return hash(value).substring(0,16).toUpperCase(Locale.ROOT);}
    private static long parseReference(String value,String prefix){if(value==null||!value.startsWith(prefix))throw new IllegalStateException("Idempotency result reference نامعتبر است.");return Long.parseLong(value.substring(prefix.length()));}
    private record BalanceMath(BigDecimal blocked,BigDecimal pendingDebit,BigDecimal available){}
}
