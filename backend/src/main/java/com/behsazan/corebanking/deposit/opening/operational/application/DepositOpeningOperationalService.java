package com.behsazan.corebanking.deposit.opening.operational.application;

import com.behsazan.corebanking.deposit.account.error.DepositAccountLifecycleException;
import com.behsazan.corebanking.deposit.account.error.DepositAccountNotFoundException;
import com.behsazan.corebanking.deposit.account.oracle.DepositAccountRepository;
import com.behsazan.corebanking.deposit.account.oracle.DepositAccountRepository.AccountRow;
import com.behsazan.corebanking.deposit.account.oracle.DepositAccountRepository.OpeningLink;
import com.behsazan.corebanking.deposit.opening.operational.domain.DepositOpeningOperationalModels.*;
import com.behsazan.corebanking.deposit.opening.operational.oracle.DepositOpeningOperationalRepository;
import com.behsazan.corebanking.deposit.opening.operational.oracle.DepositOpeningOperationalRepository.CheckDefinition;
import com.behsazan.corebanking.deposit.opening.operational.oracle.DepositOpeningOperationalRepository.FundingRow;
import com.behsazan.corebanking.deposit.opening.operational.oracle.DepositOpeningOperationalRepository.ObligationRow;
import com.behsazan.corebanking.deposit.opening.operational.oracle.DepositOpeningOperationalRepository.TermReadiness;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class DepositOpeningOperationalService {
    private static final Set<String> EVIDENCE_STATUSES = Set.of("PASS", "FAIL", "WAIVED", "NOT_APPLICABLE", "PENDING");
    private static final Set<String> EXTERNAL_REQUIRED = Set.of(
            "CBI_SIAH_REGISTRATION", "FINAL_COMPLIANCE_RECHECK", "RESTRICTIONS_READY"
    );

    private final DepositAccountRepository accountRepository;
    private final DepositOpeningOperationalRepository repository;

    public DepositOpeningOperationalService(
            DepositAccountRepository accountRepository,
            DepositOpeningOperationalRepository repository
    ) {
        this.accountRepository = accountRepository;
        this.repository = repository;
    }

    @Transactional
    public SettlementResponse settle(
            long openingRequestId,
            SettlementRequest request,
            String actor,
            String correlationId
    ) {
        String settlementReference = trim(request == null ? null : request.settlementReference());
        if (settlementReference == null) {
            throw lifecycle("مرجع Settlement الزامی است.",
                    "SETTLEMENT_REFERENCE", "مرجع Posting/Settlement معتبر ارسال کنید.");
        }

        OpeningLink opening = requireOpeningForUpdate(openingRequestId);
        AccountRow account = requirePendingAccount(opening);
        if (!"APPROVED".equals(upper(opening.requestStatusCode()))) {
            throw lifecycle("Settlement فقط برای Opening تأییدشده مجاز است.",
                    "DEPOSIT_OPENING_REQUEST.REQUEST_STATUS_CODE", "وضعیت مورد انتظار APPROVED است.");
        }

        List<FundingRow> fundings = repository.listFundings(openingRequestId);
        List<ObligationRow> obligations = repository.listObligations(openingRequestId);
        if (fundings.isEmpty()) {
            throw lifecycle("برنامه تأمین وجه برای Opening ثبت نشده است.",
                    "DEPOSIT_OPENING_FUNDING", "حداقل یک منبع تأمین وجه لازم است.");
        }
        if (obligations.isEmpty()) {
            throw lifecycle("تعهدات مالی Opening ثبت نشده است.",
                    "DEPOSIT_OPENING_OBLIGATION", "حداقل تعهد INITIAL_BALANCE باید وجود داشته باشد.");
        }

        BigDecimal fundingTotal = sumFunding(fundings);
        BigDecimal obligationTotal = sumObligations(obligations);
        BigDecimal openingBalance = obligations.stream()
                .filter(o -> "INITIAL_BALANCE".equals(upper(o.obligationTypeCode())))
                .map(ObligationRow::finalAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (openingBalance.signum() < 0) {
            throw lifecycle("تعهد مانده افتتاح نامعتبر است.",
                    "DEPOSIT_OPENING_OBLIGATION.FINAL_AMOUNT", "مبلغ INITIAL_BALANCE نمی‌تواند منفی باشد.");
        }
        if (fundingTotal.compareTo(obligationTotal) < 0) {
            throw lifecycle("جمع منابع برای تسویه تعهدات افتتاح کافی نیست.",
                    "DEPOSIT_OPENING_FUNDING.FUNDING_AMOUNT",
                    "planned=" + fundingTotal + ", obligations=" + obligationTotal);
        }

        boolean alreadySettled = fundings.stream().allMatch(f -> "SUCCESS".equals(upper(f.fundingStatusCode())))
                && obligations.stream().allMatch(o -> Set.of("SETTLED", "WAIVED").contains(upper(o.settlementStatusCode())));
        if (alreadySettled) {
            return new SettlementResponse(
                    openingRequestId, account.accountId(), account.accountStatusCode(), opening.activationStatusCode(),
                    obligationTotal, openingBalance, 0, 0, 0, true
            );
        }

        if (repository.countAllocations(openingRequestId) > 0) {
            throw lifecycle("Allocation قبلی برای Opening وجود دارد ولی Settlement کامل نیست.",
                    "DEPOSIT_OPENING_FUND_ALLOC", "برای جلوگیری از Double Posting ابتدا وضعیت قبلی بررسی شود.");
        }

        int fundingRows = 0;
        for (FundingRow funding : fundings) {
            fundingRows += repository.settleFunding(
                    funding.fundingId(), settlementReference + "-F" + funding.fundingId(), actor
            );
        }

        int obligationRows = 0;
        for (ObligationRow obligation : obligations) {
            boolean waived = obligation.finalAmount() == null || obligation.finalAmount().signum() == 0;
            obligationRows += repository.settleObligation(
                    obligation.obligationId(), waived,
                    settlementReference + "-O" + obligation.obligationId(), actor
            );
        }

        int allocations = allocate(fundings, obligations, settlementReference, actor);
        if (accountRepository.updateBalances(account.accountId(), openingBalance, openingBalance, actor) != 1) {
            throw lifecycle("به‌روزرسانی مانده افتتاح روی حساب انجام نشد.",
                    "DEPOSIT_ACCOUNT.LEDGER_BALANCE", "حساب باید PENDING_ACTIVATION باشد.");
        }
        accountRepository.updateActivationStatus(openingRequestId, "PENDING_READINESS", actor);

        return new SettlementResponse(
                openingRequestId, account.accountId(), account.accountStatusCode(), "PENDING_READINESS",
                obligationTotal, openingBalance, fundingRows, obligationRows, allocations, false
        );
    }

    @Transactional
    public ActivationReadinessResponse evaluateReadiness(
            long openingRequestId,
            ReadinessEvaluationRequest request,
            String actor,
            String correlationId
    ) {
        OpeningLink opening = requireOpeningForUpdate(openingRequestId);
        AccountRow account = requirePendingAccount(opening);
        Map<String, ReadinessEvidence> evidence = normalizeEvidence(request == null ? null : request.evidence());

        List<CheckDefinition> definitions = repository.activationDefinitions();
        if (definitions.isEmpty()) {
            throw lifecycle("Activation check catalog خالی است.",
                    "REF_DEP_OPEN_CHECK", "Phase 10A reference data باید اعمال شده باشد.");
        }

        Map<String, Eval> evaluated = new LinkedHashMap<>();
        TermReadiness term = repository.termReadiness(openingRequestId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        for (CheckDefinition definition : definitions) {
            String code = definition.checkCode();
            ReadinessEvidence supplied = evidence.get(code);
            Eval result = evaluateOne(opening, account, definition, supplied, term, now);
            evaluated.put(code, result);
            repository.insertReadinessCheck(
                    openingRequestId,
                    definition,
                    repository.nextAttemptNo(openingRequestId, code),
                    result.status(), result.reference(), result.validUntil(), result.sourceEvaluationReference(),
                    result.waiverReason(), actor
            );
        }

        List<String> blockers = new ArrayList<>();
        for (CheckDefinition definition : definitions) {
            Eval result = evaluated.get(definition.checkCode());
            if (definition.requiredFlag() == 1
                    && "ACCOUNT_ACTIVATION".equals(upper(definition.blockingScopeCode()))
                    && !Set.of("PASS", "WAIVED", "NOT_APPLICABLE").contains(upper(result.status()))) {
                blockers.add(definition.titleFa() + " [" + definition.checkCode() + "]");
            }
        }

        String activationStatus = blockers.isEmpty() ? "READY" : "BLOCKED";
        accountRepository.updateActivationStatus(openingRequestId, activationStatus, actor);
        return buildResponse(openingRequestId, account, activationStatus, blockers, now);
    }

    @Transactional(readOnly = true)
    public ActivationReadinessResponse getReadiness(long openingRequestId) {
        OpeningLink opening = accountRepository.findOpening(openingRequestId)
                .orElseThrow(() -> new DepositAccountNotFoundException(
                        "پرونده افتتاح با شناسه " + openingRequestId + " یافت نشد."
                ));
        if (opening.createdAccountId() == null) {
            throw new DepositAccountNotFoundException("برای این Opening هنوز حسابی ایجاد نشده است.");
        }
        AccountRow account = accountRepository.findAccount(opening.createdAccountId())
                .orElseThrow(() -> new DepositAccountNotFoundException("حساب متصل به Opening یافت نشد."));
        List<ReadinessCheckView> checks = repository.latestActivationChecks(openingRequestId);
        List<String> blockers = checks.stream()
                .filter(ReadinessCheckView::required)
                .filter(c -> "ACCOUNT_ACTIVATION".equals(upper(c.blockingScopeCode())))
                .filter(c -> !Set.of("PASS", "WAIVED", "NOT_APPLICABLE").contains(upper(c.resultStatusCode())))
                .map(c -> c.titleFa() + " [" + c.checkCode() + "]")
                .toList();
        return response(openingRequestId, account, opening.activationStatusCode(), blockers, checks, OffsetDateTime.now(ZoneOffset.UTC));
    }

    private Eval evaluateOne(
            OpeningLink opening,
            AccountRow account,
            CheckDefinition definition,
            ReadinessEvidence supplied,
            TermReadiness term,
            OffsetDateTime now
    ) {
        String code = definition.checkCode();
        if ("OPENING_CREATE_GATE".equals(code)) {
            boolean pass = repository.unresolvedRequiredAccountCreationChecks(opening.openingRequestId()) == 0;
            return internal(pass, "CREATE-GATE:" + (pass ? "PASS" : "BLOCKED"));
        }
        if ("ACTIVATION_WINDOW_VALID".equals(code)) {
            boolean pass = account.activationDeadlineAt() != null && !account.activationDeadlineAt().isBefore(now);
            return internal(pass, account.activationDeadlineAt() == null ? "NO-DEADLINE" : account.activationDeadlineAt().toString());
        }
        if ("FINANCIAL_SETTLEMENT".equals(code)) {
            boolean pass = repository.allMandatoryObligationsSettled(opening.openingRequestId());
            return internal(pass, pass ? "MANDATORY-OBLIGATIONS-SETTLED" : "UNSETTLED-OBLIGATIONS");
        }
        if ("MIN_OPENING_BALANCE".equals(code)) {
            boolean pass = account.ledgerBalance() != null && opening.openingAmount() != null
                    && account.ledgerBalance().compareTo(opening.openingAmount()) >= 0;
            return internal(pass, "BALANCE=" + account.ledgerBalance());
        }
        if ("FUNDING_SOURCE_VALIDATION".equals(code)) {
            boolean pass = repository.fundingSourcesValid(opening.openingRequestId());
            return internal(pass, pass ? "FUNDING-SOURCES-VALID" : "FUNDING-SOURCE-BLOCKED");
        }
        if ("SIGNATORY_MANDATE_READY".equals(code)) {
            boolean pass = repository.signatoryMandateReady(opening.openingRequestId(), opening.ownershipTypeCode());
            return internal(pass, pass ? "SIGNATORY-READY" : "SIGNATORY-MISSING");
        }
        if ("PRODUCT_OPERATIONAL_SETUP".equals(code)) {
            boolean pass = opening.productVersionId() != null && opening.productVersionId() > 0;
            return internal(pass, "PRODUCT_VERSION_ID=" + opening.productVersionId());
        }
        if ("TERM_PROFIT_INSTRUCTIONS".equals(code)) {
            if (term == null || term.termCount() == 0) return notApplicable("ON_DEMAND_OR_NON_TERM");
            boolean pass = term.profitCount() > 0 && term.maturityCount() > 0;
            return internal(pass, "TERM=" + term.termCount() + ",PROFIT=" + term.profitCount() + ",MATURITY=" + term.maturityCount());
        }
        if ("JOINT_COMMERCIAL_CONFIRMATION".equals(code)) {
            boolean applicable = "JOINT".equals(upper(opening.ownershipTypeCode()))
                    && "COMMERCIAL_ACCOUNT".equals(upper(repository.jointBasisCode(opening.openingRequestId())));
            if (!applicable) return notApplicable("NOT_JOINT_COMMERCIAL");
            return suppliedOrPending(supplied, code, now);
        }
        if ("LEGAL_ENTITY_NATIONAL_ID_READY".equals(code)) {
            // The current Opening Aggregate does not persist legal-entity formation status.
            // Explicit evidence makes the condition applicable; otherwise it remains N/A rather than fabricated.
            return supplied == null ? notApplicable("APPLICABILITY_NOT_PERSISTED") : suppliedOrPending(supplied, code, now);
        }
        if ("ACCOUNT_OPENED_SMS".equals(code)) {
            return supplied == null ? pending("POST_ACTIVATION_EVIDENCE_REQUIRED") : suppliedOrPending(supplied, code, now);
        }
        if (EXTERNAL_REQUIRED.contains(code)) {
            return suppliedOrPending(supplied, code, now);
        }
        return supplied == null ? pending("EVIDENCE_REQUIRED:" + code) : suppliedOrPending(supplied, code, now);
    }

    private ActivationReadinessResponse buildResponse(
            long openingRequestId,
            AccountRow account,
            String activationStatus,
            List<String> blockers,
            OffsetDateTime evaluatedAt
    ) {
        List<ReadinessCheckView> checks = repository.latestActivationChecks(openingRequestId);
        return response(openingRequestId, account, activationStatus, blockers, checks, evaluatedAt);
    }

    private static ActivationReadinessResponse response(
            long openingRequestId,
            AccountRow account,
            String activationStatus,
            List<String> blockers,
            List<ReadinessCheckView> checks,
            OffsetDateTime evaluatedAt
    ) {
        boolean debitRestricted = checks.stream()
                .filter(c -> "DEBIT_CAPABILITY".equals(upper(c.blockingScopeCode())))
                .anyMatch(c -> !Set.of("PASS", "WAIVED", "NOT_APPLICABLE").contains(upper(c.resultStatusCode())));
        return new ActivationReadinessResponse(
                openingRequestId, account.accountId(), account.accountStatusCode(), activationStatus,
                debitRestricted, List.copyOf(blockers), List.copyOf(checks), evaluatedAt
        );
    }

    private int allocate(
            List<FundingRow> fundings,
            List<ObligationRow> obligations,
            String settlementReference,
            String actor
    ) {
        Map<Long, BigDecimal> remaining = new LinkedHashMap<>();
        for (FundingRow funding : fundings) remaining.put(funding.fundingId(), funding.amount());
        int count = 0;
        for (ObligationRow obligation : obligations) {
            BigDecimal need = obligation.finalAmount() == null ? BigDecimal.ZERO : obligation.finalAmount();
            if (need.signum() <= 0) continue;
            for (FundingRow funding : fundings) {
                if (need.signum() <= 0) break;
                BigDecimal available = remaining.getOrDefault(funding.fundingId(), BigDecimal.ZERO);
                if (available.signum() <= 0) continue;
                BigDecimal amount = need.min(available);
                repository.insertAllocation(
                        repository.nextFundAllocationId(), funding.fundingId(), obligation.obligationId(), amount,
                        settlementReference + "-A" + funding.fundingId() + "-" + obligation.obligationId(), actor
                );
                count++;
                remaining.put(funding.fundingId(), available.subtract(amount));
                need = need.subtract(amount);
            }
            if (need.signum() > 0) {
                throw lifecycle("Allocation منابع برای تعهد کامل نشد.",
                        "DEPOSIT_OPENING_FUND_ALLOC.ALLOCATED_AMOUNT", "remaining=" + need);
            }
        }
        return count;
    }

    private OpeningLink requireOpeningForUpdate(long openingRequestId) {
        return accountRepository.lockOpening(openingRequestId)
                .orElseThrow(() -> new DepositAccountNotFoundException(
                        "پرونده افتتاح با شناسه " + openingRequestId + " یافت نشد."
                ));
    }

    private AccountRow requirePendingAccount(OpeningLink opening) {
        if (opening.createdAccountId() == null) {
            throw lifecycle("برای این Opening هنوز حسابی ایجاد نشده است.",
                    "DEPOSIT_OPENING_REQUEST.CREATED_ACCOUNT_ID", "ابتدا Create Account را اجرا کنید.");
        }
        AccountRow account = accountRepository.lockAccount(opening.createdAccountId())
                .orElseThrow(() -> new DepositAccountNotFoundException("حساب متصل به Opening یافت نشد."));
        if (!"PENDING_ACTIVATION".equals(upper(account.accountStatusCode()))) {
            throw lifecycle("عملیات فقط روی حساب PENDING_ACTIVATION مجاز است.",
                    "DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE", account.accountStatusCode());
        }
        return account;
    }

    private static Map<String, ReadinessEvidence> normalizeEvidence(List<ReadinessEvidence> values) {
        Map<String, ReadinessEvidence> result = new HashMap<>();
        if (values == null) return result;
        for (ReadinessEvidence value : values) {
            if (value == null || trim(value.checkCode()) == null) continue;
            String code = upper(value.checkCode());
            String status = upper(value.resultStatusCode());
            if (!EVIDENCE_STATUSES.contains(status)) {
                throw lifecycle("وضعیت Evidence معتبر نیست.",
                        "EVIDENCE.RESULT_STATUS_CODE", String.valueOf(value.resultStatusCode()));
            }
            result.put(code, new ReadinessEvidence(
                    code, status, trim(value.resultReference()), value.validUntil(),
                    trim(value.sourceEvaluationReference()), trim(value.waiverReason())
            ));
        }
        return result;
    }

    private static Eval suppliedOrPending(ReadinessEvidence supplied, String code, OffsetDateTime now) {
        if (supplied == null) return pending("EVIDENCE_REQUIRED:" + code);
        if ("PASS".equals(upper(supplied.resultStatusCode()))
                && supplied.validUntil() != null
                && supplied.validUntil().isBefore(now)) {
            return new Eval(
                    "FAIL",
                    supplied.resultReference() == null ? "EXPIRED_EVIDENCE:" + code : supplied.resultReference(),
                    supplied.validUntil(),
                    supplied.sourceEvaluationReference() == null ? "EVIDENCE_EXPIRED" : supplied.sourceEvaluationReference(),
                    "Evidence validity expired before Activation Readiness evaluation."
            );
        }
        return new Eval(
                supplied.resultStatusCode(), supplied.resultReference(), supplied.validUntil(),
                supplied.sourceEvaluationReference(), supplied.waiverReason()
        );
    }

    private static Eval internal(boolean pass, String reference) {
        return new Eval(pass ? "PASS" : "FAIL", reference, null, "INTERNAL_EVALUATION", null);
    }

    private static Eval notApplicable(String reference) {
        return new Eval("NOT_APPLICABLE", reference, null, "INTERNAL_APPLICABILITY", null);
    }

    private static Eval pending(String reference) {
        return new Eval("PENDING", reference, null, "EXTERNAL_EVIDENCE", null);
    }

    private static BigDecimal sumFunding(List<FundingRow> values) {
        return values.stream().map(FundingRow::amount).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumObligations(List<ObligationRow> values) {
        return values.stream().map(ObligationRow::finalAmount).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static DepositAccountLifecycleException lifecycle(String message, String field, String detail) {
        return new DepositAccountLifecycleException(message, Map.of(field, detail == null ? "نامعتبر" : detail));
    }

    private static String trim(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private record Eval(
            String status,
            String reference,
            OffsetDateTime validUntil,
            String sourceEvaluationReference,
            String waiverReason
    ) {
    }
}
