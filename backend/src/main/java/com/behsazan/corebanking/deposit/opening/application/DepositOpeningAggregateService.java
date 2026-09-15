package com.behsazan.corebanking.deposit.opening.application;

import com.behsazan.corebanking.deposit.opening.domain.DepositOpeningModels.*;
import com.behsazan.corebanking.deposit.opening.audit.application.DepositOpeningAuditService;
import com.behsazan.corebanking.deposit.opening.audit.domain.DepositOpeningAuditModels.OpeningCreateAuditResult;
import com.behsazan.corebanking.deposit.opening.error.DepositOpeningValidationException;
import com.behsazan.corebanking.deposit.opening.oracle.DepositOpeningAggregateRepository;
import com.behsazan.corebanking.deposit.opening.oracle.DepositOpeningAggregateRepository.ExistingRequest;
import com.behsazan.corebanking.deposit.opening.readiness.application.DepositOpeningRuntimeValidator;
import com.behsazan.corebanking.deposit.opening.readiness.domain.DepositOpeningRuntimeModels.RuntimeValidationResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DepositOpeningAggregateService {
    private final DepositOpeningAggregateRepository repository;
    private final DepositOpeningAuditService auditService;
    private final DepositOpeningRuntimeValidator runtimeValidator;

    public DepositOpeningAggregateService(
            DepositOpeningAggregateRepository repository,
            DepositOpeningAuditService auditService,
            DepositOpeningRuntimeValidator runtimeValidator
    ) {
        this.repository = repository;
        this.auditService = auditService;
        this.runtimeValidator = runtimeValidator;
    }

    @Transactional
    public PersistedAggregateResponse create(AggregateRequest aggregate, String actor, String correlationId) {
        AggregateRequest request = normalize(aggregate);
        validate(request);
        runtimeValidator.validateAggregate(request);

        OpeningRequest root = request.request();
        ExistingRequest existing = repository.findByIdempotencyKey(root.idempotencyKey()).orElse(null);
        if (existing != null) {
            if (!existing.requestNo().equals(root.requestNo())) {
                throw new DepositOpeningValidationException(
                        "کلید Idempotency قبلاً برای درخواست دیگری استفاده شده است.",
                        Map.of("DEPOSIT_OPENING_REQUEST.IDEMPOTENCY_KEY", "این کلید قبلاً برای شماره درخواست دیگری ثبت شده است.")
                );
            }
            return new PersistedAggregateResponse(
                    existing.openingRequestId(), existing.requestNo(), existing.idempotencyKey(),
                    existing.requestStatusCode(), true, Map.of()
            );
        }

        long requestId = repository.nextRequestId();
        Map<String, Integer> rows = new LinkedHashMap<>();
        try {
            put(rows, "DEPOSIT_OPENING_REQUEST", repository.insertRequest(requestId, root, actor));
        } catch (DuplicateKeyException ex) {
            ExistingRequest concurrent = repository.findByIdempotencyKey(root.idempotencyKey()).orElse(null);
            if (concurrent == null) throw ex;
            if (!concurrent.requestNo().equals(root.requestNo())) {
                throw new DepositOpeningValidationException(
                        "کلید Idempotency همزمان برای درخواست دیگری ثبت شده است.",
                        Map.of("DEPOSIT_OPENING_REQUEST.IDEMPOTENCY_KEY", "کلید Idempotency باید به همان REQUEST_NO بازپخش شود.")
                );
            }
            return new PersistedAggregateResponse(
                    concurrent.openingRequestId(), concurrent.requestNo(), concurrent.idempotencyKey(),
                    concurrent.requestStatusCode(), true, Map.of()
            );
        }

        int partySequence = 1;
        for (OpeningParty party : safe(request.parties())) {
            put(rows, "DEPOSIT_OPENING_PARTY", repository.insertParty(requestId, party, partySequence++, actor));
        }

        if (request.signatureRule() != null) {
            put(rows, "DEPOSIT_OPENING_SIGNATURE_RULE",
                    repository.insertSignatureRule(requestId, request.signatureRule(), root.requestedOpeningDate(), actor));
        }

        Map<Long, Long> persistedSignatoryIds = new HashMap<>();
        int signatorySequence = 1;
        for (Signatory signatory : safe(request.signatories())) {
            long persistedId = repository.nextSignatoryId();
            put(rows, "DEPOSIT_OPENING_SIGNATORY",
                    repository.insertSignatory(persistedId, requestId, signatory, signatorySequence++, root.requestedOpeningDate(), actor));
            if (signatory.openingSignatoryId() != null) {
                persistedSignatoryIds.put(signatory.openingSignatoryId(), persistedId);
            }
        }
        for (SignatoryAuthority authority : safe(request.signatoryAuthorities())) {
            Long persistedSignatoryId = persistedSignatoryIds.get(authority.openingSignatoryId());
            if (persistedSignatoryId == null) {
                throw new DepositOpeningValidationException(
                        "اختیار صاحب امضا به Signatory معتبر متصل نیست.",
                        Map.of("DEPOSIT_OPENING_SIGNATORY_AUTHORITY.OPENING_SIGNATORY_ID", "شناسه موقت صاحب امضا در Payload پیدا نشد.")
                );
            }
            put(rows, "DEPOSIT_OPENING_SIGNATORY_AUTHORITY",
                    repository.insertSignatoryAuthority(persistedSignatoryId, authority, root.requestedOpeningDate(), actor));
        }

        for (AuthorizedUser value : safe(request.authorizedUsers())) {
            put(rows, "DEPOSIT_OPENING_AUTHORIZED_USER",
                    repository.insertAuthorizedUser(requestId, value, root.requestedOpeningDate(), actor));
        }
        for (Delegation value : safe(request.delegations())) {
            put(rows, "DEPOSIT_OPENING_DELEGATION",
                    repository.insertDelegation(requestId, value, root.requestedOpeningDate(), actor));
        }
        for (Beneficiary value : safe(request.beneficiaries())) {
            put(rows, "DEPOSIT_OPENING_BENEFICIARY",
                    repository.insertBeneficiary(requestId, value, root.requestedOpeningDate(), actor));
        }

        Long termId = null;
        if (request.term() != null) {
            termId = repository.nextTermId();
            LocalDate maturityDate = resolveMaturityDate(request.term());
            put(rows, "DEPOSIT_OPENING_TERM",
                    repository.insertTerm(termId, requestId, request.term(), maturityDate, actor));
        }
        if (request.maturityInstruction() != null) {
            if (termId == null) {
                throw new DepositOpeningValidationException(
                        "دستور سررسید بدون مدت سپرده قابل ثبت نیست.",
                        Map.of("DEPOSIT_OPENING_MATURITY_INSTRUCTION", "ابتدا DEPOSIT_OPENING_TERM باید وجود داشته باشد.")
                );
            }
            put(rows, "DEPOSIT_OPENING_MATURITY_INSTRUCTION",
                    repository.insertMaturityInstruction(termId, request.maturityInstruction(), actor));
        }

        if (request.profitInstruction() != null) {
            put(rows, "DEPOSIT_OPENING_PROFIT_INSTRUCTION",
                    repository.insertProfitInstruction(requestId, request.profitInstruction(), actor));
        }
        for (WithdrawalMedia value : safe(request.withdrawalMedia())) {
            put(rows, "DEPOSIT_OPENING_WITHDRAWAL_MEDIA", repository.insertWithdrawalMedia(requestId, value, actor));
        }
        for (ServiceSelection value : safe(request.serviceSelections())) {
            put(rows, "DEPOSIT_OPENING_SERVICE_SELECTION", repository.insertServiceSelection(requestId, value, actor));
        }
        for (PaymentInstrument value : safe(request.paymentInstruments())) {
            put(rows, "DEPOSIT_OPENING_PAYMENT_INSTRUMENT", repository.insertPaymentInstrument(requestId, value, actor));
        }
        for (PricingOverrideRequest value : safe(request.pricingOverrideRequests())) {
            put(rows, "DEPOSIT_OPENING_PRICING_OVERRIDE_REQUEST",
                    repository.insertPricingOverrideRequest(requestId, value, root.requestedOpeningDate(), actor));
        }
        if (request.taxStatus() != null) {
            put(rows, "DEPOSIT_OPENING_TAX_STATUS",
                    repository.insertTaxStatus(requestId, request.taxStatus(), root.requestedOpeningDate(), actor));
        }
        for (RewardEnrollment value : safe(request.rewardEnrollments())) {
            put(rows, "DEPOSIT_OPENING_REWARD_ENROLLMENT", repository.insertRewardEnrollment(requestId, value, actor));
        }

        if (request.funding() != null) {
            put(rows, "DEPOSIT_OPENING_FUNDING", repository.insertFunding(requestId, request.funding(), actor));
        }
        for (OpeningCheck value : safe(request.checks())) {
            put(rows, "DEPOSIT_OPENING_CHECK", repository.insertCheck(requestId, value, actor));
        }
        for (OpeningDocument value : safe(request.documents())) {
            put(rows, "DEPOSIT_OPENING_DOCUMENT", repository.insertDocument(requestId, value, actor));
        }
        if (request.termsAcceptance() != null) {
            TermsAcceptance acceptance = request.termsAcceptance();
            if (acceptance.acceptedByPartyId() == null) {
                Long primaryPartyId = safe(request.parties()).stream()
                        .filter(p -> Integer.valueOf(1).equals(p.isPrimary()))
                        .map(OpeningParty::partyId)
                        .findFirst()
                        .orElse(null);
                acceptance = new TermsAcceptance(
                        acceptance.termsVersionCode(), primaryPartyId, acceptance.acceptanceSourceCode(),
                        acceptance.channelCode(), acceptance.acceptanceStatusCode(), acceptance.evidenceReference()
                );
            }
            put(rows, "DEPOSIT_OPENING_TERMS_ACCEPTANCE",
                    repository.insertTermsAcceptance(requestId, acceptance, actor));
        }
        if (request.decision() != null) {
            put(rows, "DEPOSIT_OPENING_DECISION", repository.insertDecision(requestId, request.decision(), actor));
        }

        OpeningCreateAuditResult audit = auditService.recordOpeningCreated(requestId, root, actor, correlationId);
        put(rows, "DEPOSIT_OPENING_AUDIT_EVENT", 1);
        put(rows, "DEPOSIT_OPENING_STATUS_HISTORY", audit.statusHistoryRows());
        if (audit.snapshotId() != null) put(rows, "DEPOSIT_OPENING_SNAPSHOT", 1);

        return new PersistedAggregateResponse(
                requestId, root.requestNo(), root.idempotencyKey(), root.requestStatusCode(), false, Map.copyOf(rows)
        );
    }

    @Transactional(readOnly = true)
    public RuntimeValidationResponse validateRuntime(AggregateRequest aggregate) {
        AggregateRequest request = normalize(aggregate);
        validate(request);
        return runtimeValidator.validateAggregate(request);
    }

    private static AggregateRequest normalize(AggregateRequest value) {
        if (value == null) return null;
        OpeningRequest root = value.request();
        if (root == null) return value;
        OpeningRequest normalizedRoot = new OpeningRequest(
                trim(root.requestNo()), trim(root.idempotencyKey()), root.productVersionId(),
                upper(root.requestTypeCode()), upper(root.ownershipTypeCode()), upper(root.currencyCode()),
                upper(root.openingChannelCode()), trim(root.orgUnitCode()), root.requestedOpeningDate(),
                root.openingAmount(), upper(root.sourceOfFundsCode()), upper(root.purposeCode()),
                upper(root.requestStatusCode())
        );
        return new AggregateRequest(
                normalizedRoot,
                value.parties(), value.signatureRule(), value.signatories(), value.signatoryAuthorities(),
                value.authorizedUsers(), value.delegations(), value.beneficiaries(),
                value.term(), value.maturityInstruction(), value.profitInstruction(),
                value.withdrawalMedia(), value.serviceSelections(), value.paymentInstruments(),
                value.pricingOverrideRequests(), value.taxStatus(), value.rewardEnrollments(),
                value.funding(), value.checks(), value.documents(), value.termsAcceptance(), value.decision()
        );
    }

    private static void validate(AggregateRequest aggregate) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (aggregate == null || aggregate.request() == null) {
            throw new DepositOpeningValidationException(
                    "ریشه درخواست افتتاح حساب ارسال نشده است.",
                    Map.of("DEPOSIT_OPENING_REQUEST", "اطلاعات اصلی درخواست الزامی است.")
            );
        }

        OpeningRequest root = aggregate.request();
        required(errors, "DEPOSIT_OPENING_REQUEST.REQUEST_NO", root.requestNo(), "شماره درخواست الزامی است.");
        required(errors, "DEPOSIT_OPENING_REQUEST.IDEMPOTENCY_KEY", root.idempotencyKey(), "کلید Idempotency الزامی است.");
        required(errors, "DEPOSIT_OPENING_REQUEST.REQUEST_TYPE_CODE", root.requestTypeCode(), "نوع درخواست الزامی است.");
        required(errors, "DEPOSIT_OPENING_REQUEST.OWNERSHIP_TYPE_CODE", root.ownershipTypeCode(), "نوع مالکیت الزامی است.");
        required(errors, "DEPOSIT_OPENING_REQUEST.CURRENCY_CODE", root.currencyCode(), "ارز الزامی است.");
        required(errors, "DEPOSIT_OPENING_REQUEST.OPENING_CHANNEL_CODE", root.openingChannelCode(), "کانال افتتاح الزامی است.");
        required(errors, "DEPOSIT_OPENING_REQUEST.ORG_UNIT_CODE", root.orgUnitCode(), "واحد سازمانی الزامی است.");
        required(errors, "DEPOSIT_OPENING_REQUEST.REQUEST_STATUS_CODE", root.requestStatusCode(), "وضعیت درخواست الزامی است.");
        if (root.productVersionId() == null || root.productVersionId() <= 0) {
            errors.put("DEPOSIT_OPENING_REQUEST.PRODUCT_VERSION_ID", "نسخه محصول معتبر نیست.");
        }
        if (root.openingAmount() == null || root.openingAmount().compareTo(BigDecimal.ZERO) < 0) {
            errors.put("DEPOSIT_OPENING_REQUEST.OPENING_AMOUNT", "مبلغ افتتاح نمی‌تواند منفی باشد.");
        }
        if (root.requestNo() != null && root.requestNo().length() > 40) {
            errors.put("DEPOSIT_OPENING_REQUEST.REQUEST_NO", "شماره درخواست حداکثر ۴۰ کاراکتر است.");
        }
        if (root.idempotencyKey() != null && root.idempotencyKey().length() > 80) {
            errors.put("DEPOSIT_OPENING_REQUEST.IDEMPOTENCY_KEY", "کلید Idempotency حداکثر ۸۰ کاراکتر است.");
        }

        List<OpeningParty> parties = safe(aggregate.parties());
        Set<Long> openingPartyIds = new HashSet<>();
        if (parties.isEmpty()) {
            errors.put("DEPOSIT_OPENING_PARTY", "حداقل یک مالک/Party باید ثبت شود.");
        } else {
            long primaryCount = parties.stream().filter(p -> Integer.valueOf(1).equals(p.isPrimary())).count();
            if (primaryCount != 1) {
                errors.put("DEPOSIT_OPENING_PARTY.IS_PRIMARY", "دقیقاً یک مالک اصلی باید تعیین شود.");
            }
            long ownerCount = parties.stream().filter(p -> "OWNER".equals(upper(p.roleCode())) || "CO_OWNER".equals(upper(p.roleCode()))).count();
            if ("JOINT".equals(root.ownershipTypeCode()) && ownerCount < 2) {
                errors.put("DEPOSIT_OPENING_PARTY.ROLE_CODE", "برای مالکیت مشترک حداقل دو مالک لازم است.");
            }
            if ("INDIVIDUAL".equals(root.ownershipTypeCode()) && ownerCount < 1) {
                errors.put("DEPOSIT_OPENING_PARTY.ROLE_CODE", "برای مالکیت انفرادی حداقل یک مالک لازم است.");
            }
            BigDecimal totalShare = parties.stream()
                    .filter(p -> "OWNER".equals(upper(p.roleCode())) || "CO_OWNER".equals(upper(p.roleCode())))
                    .map(OpeningParty::ownershipPercent)
                    .filter(v -> v != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (ownerCount > 0 && totalShare.subtract(new BigDecimal("100")).abs().compareTo(new BigDecimal("0.01")) > 0) {
                errors.put("DEPOSIT_OPENING_PARTY.OWNERSHIP_PERCENT", "جمع درصد مالکیت مالکان باید ۱۰۰ باشد.");
            }
            for (OpeningParty party : parties) {
                if (party.partyId() == null || party.partyId() <= 0) {
                    errors.put("DEPOSIT_OPENING_PARTY.PARTY_ID", "Party ID باید مثبت باشد.");
                } else if (!openingPartyIds.add(party.partyId())) {
                    errors.put("DEPOSIT_OPENING_PARTY.PARTY_ID", "Party تکراری در Aggregate مجاز نیست.");
                }
                flag(errors, "DEPOSIT_OPENING_PARTY.IS_PRIMARY", party.isPrimary());
            }
        }

        List<SignatoryAuthority> authorities = safe(aggregate.signatoryAuthorities());
        Set<Long> signatoryClientIds = new HashSet<>();
        Set<Long> signatoryPartyIds = new HashSet<>();
        for (Signatory signatory : safe(aggregate.signatories())) {
            if (signatory.partyId() == null || signatory.partyId() <= 0) {
                errors.put("DEPOSIT_OPENING_SIGNATORY.PARTY_ID", "Party صاحب امضا باید معتبر باشد.");
            } else {
                signatoryPartyIds.add(signatory.partyId());
                if (!openingPartyIds.contains(signatory.partyId())) {
                    errors.put("DEPOSIT_OPENING_SIGNATORY.PARTY_ID", "Party صاحب امضا باید در DEPOSIT_OPENING_PARTY همین درخواست وجود داشته باشد.");
                }
            }
            required(errors, "DEPOSIT_OPENING_SIGNATORY.SIGNATORY_ROLE_CODE", signatory.signatoryRoleCode(), "نقش صاحب امضا الزامی است.");
            flag(errors, "DEPOSIT_OPENING_SIGNATORY.IS_PRIMARY_SIGNATORY", signatory.isPrimarySignatory());
            if (signatory.openingSignatoryId() != null && !signatoryClientIds.add(signatory.openingSignatoryId())) {
                errors.put("DEPOSIT_OPENING_SIGNATORY.OPENING_SIGNATORY_ID", "شناسه موقت صاحب امضا باید در Payload یکتا باشد.");
            }
        }
        if (!authorities.isEmpty()) {
            if (signatoryClientIds.isEmpty()) {
                errors.put("DEPOSIT_OPENING_SIGNATORY.OPENING_SIGNATORY_ID", "برای اتصال Authority، شناسه موقت صاحب امضا در Payload الزامی است.");
            }
            for (SignatoryAuthority authority : authorities) {
                if (authority.openingSignatoryId() == null || !signatoryClientIds.contains(authority.openingSignatoryId())) {
                    errors.put("DEPOSIT_OPENING_SIGNATORY_AUTHORITY.OPENING_SIGNATORY_ID", "Authority باید به شناسه موقت یکی از Signatoryهای همین Payload متصل باشد.");
                }
                required(errors, "DEPOSIT_OPENING_SIGNATORY_AUTHORITY.OPERATION_CODE", authority.operationCode(), "عملیات مجاز صاحب امضا الزامی است.");
                if (authority.maxAmount() != null && authority.maxAmount().compareTo(BigDecimal.ZERO) < 0) {
                    errors.put("DEPOSIT_OPENING_SIGNATORY_AUTHORITY.MAX_AMOUNT", "سقف اختیار نمی‌تواند منفی باشد.");
                }
                flag(errors, "DEPOSIT_OPENING_SIGNATORY_AUTHORITY.REQUIRES_COSIGN", authority.requiresCosign());
            }
        }

        for (AuthorizedUser value : safe(aggregate.authorizedUsers())) {
            if (value.partyId() == null || value.partyId() <= 0) errors.put("DEPOSIT_OPENING_AUTHORIZED_USER.PARTY_ID", "Party کاربر مجاز باید معتبر باشد.");
            required(errors, "DEPOSIT_OPENING_AUTHORIZED_USER.ACCESS_ROLE_CODE", value.accessRoleCode(), "نقش دسترسی الزامی است.");
            required(errors, "DEPOSIT_OPENING_AUTHORIZED_USER.CHANNEL_SCOPE_CODE", value.channelScopeCode(), "دامنه کانال الزامی است.");
        }

        for (Delegation value : safe(aggregate.delegations())) {
            if (value.grantorPartyId() == null || value.grantorPartyId() <= 0) errors.put("DEPOSIT_OPENING_DELEGATION.GRANTOR_PARTY_ID", "اعطا کننده باید معتبر باشد.");
            if (value.delegatePartyId() == null || value.delegatePartyId() <= 0) errors.put("DEPOSIT_OPENING_DELEGATION.DELEGATE_PARTY_ID", "نماینده باید معتبر باشد.");
            if (value.grantorPartyId() != null && value.grantorPartyId().equals(value.delegatePartyId())) errors.put("DEPOSIT_OPENING_DELEGATION.DELEGATE_PARTY_ID", "اعطا کننده و نماینده نمی‌توانند یک Party باشند.");
            required(errors, "DEPOSIT_OPENING_DELEGATION.DELEGATION_TYPE_CODE", value.delegationTypeCode(), "نوع نمایندگی الزامی است.");
            required(errors, "DEPOSIT_OPENING_DELEGATION.AUTHORITY_SCOPE_CODE", value.authorityScopeCode(), "دامنه اختیار الزامی است.");
            required(errors, "DEPOSIT_OPENING_DELEGATION.DOCUMENT_REFERENCE", value.documentReference(), "مرجع سند وکالت/نمایندگی الزامی است.");
        }

        for (Beneficiary value : safe(aggregate.beneficiaries())) {
            if (value.beneficiaryPartyId() == null || value.beneficiaryPartyId() <= 0) errors.put("DEPOSIT_OPENING_BENEFICIARY.BENEFICIARY_PARTY_ID", "Party ذی‌نفع باید معتبر باشد.");
            required(errors, "DEPOSIT_OPENING_BENEFICIARY.BENEFICIARY_TYPE_CODE", value.beneficiaryTypeCode(), "نوع ذی‌نفع الزامی است.");
            if (value.sharePercent() != null && (value.sharePercent().compareTo(BigDecimal.ZERO) < 0 || value.sharePercent().compareTo(new BigDecimal("100")) > 0)) {
                errors.put("DEPOSIT_OPENING_BENEFICIARY.SHARE_PERCENT", "درصد سهم ذی‌نفع باید بین صفر و ۱۰۰ باشد.");
            }
        }

        if (aggregate.term() == null && aggregate.maturityInstruction() != null) {
            errors.put("DEPOSIT_OPENING_MATURITY_INSTRUCTION", "دستور سررسید بدون مدت سپرده مجاز نیست.");
        }
        if (aggregate.term() != null) {
            OpeningTerm term = aggregate.term();
            if (term.termValue() == null || term.termValue() <= 0) {
                errors.put("DEPOSIT_OPENING_TERM.TERM_VALUE", "مقدار مدت باید بزرگ‌تر از صفر باشد.");
            }
            required(errors, "DEPOSIT_OPENING_TERM.TERM_UNIT_CODE", term.termUnitCode(), "واحد مدت الزامی است.");
            if (term.startDate() == null && root.requestedOpeningDate() == null) {
                errors.put("DEPOSIT_OPENING_TERM.START_DATE", "تاریخ شروع مدت الزامی است.");
            }
        }

        for (PaymentInstrument value : safe(aggregate.paymentInstruments())) {
            required(errors, "DEPOSIT_OPENING_PAYMENT_INSTRUMENT.INSTRUMENT_TYPE_CODE", value.instrumentTypeCode(), "نوع ابزار پرداخت الزامی است.");
            if (value.requestedQuantity() != null && value.requestedQuantity() <= 0) errors.put("DEPOSIT_OPENING_PAYMENT_INSTRUMENT.REQUESTED_QUANTITY", "تعداد ابزار باید بزرگ‌تر از صفر باشد.");
            if (value.linkedPartyId() != null && value.linkedPartyId() <= 0) errors.put("DEPOSIT_OPENING_PAYMENT_INSTRUMENT.LINKED_PARTY_ID", "Party ابزار پرداخت معتبر نیست.");
        }

        for (PricingOverrideRequest value : safe(aggregate.pricingOverrideRequests())) {
            required(errors, "DEPOSIT_OPENING_PRICING_OVERRIDE_REQUEST.OVERRIDE_TYPE_CODE", value.overrideTypeCode(), "نوع درخواست Override الزامی است.");
            required(errors, "DEPOSIT_OPENING_PRICING_OVERRIDE_REQUEST.AUTHORITY_LEVEL_CODE", value.authorityLevelCode(), "سطح اختیار الزامی است.");
            if (value.requestedValue() == null) errors.put("DEPOSIT_OPENING_PRICING_OVERRIDE_REQUEST.REQUESTED_VALUE", "مقدار درخواستی الزامی است.");
            if ("APPROVED".equals(upper(value.requestStatusCode())) && blank(value.approvalReference())) {
                errors.put("DEPOSIT_OPENING_PRICING_OVERRIDE_REQUEST.APPROVAL_REFERENCE", "برای Override تأییدشده مرجع تأیید الزامی است.");
            }
        }

        if (aggregate.taxStatus() != null) {
            TaxStatus value = aggregate.taxStatus();
            required(errors, "DEPOSIT_OPENING_TAX_STATUS.TAX_RESIDENCY_CODE", value.taxResidencyCode(), "اقامت مالیاتی الزامی است.");
            required(errors, "DEPOSIT_OPENING_TAX_STATUS.TAX_STATUS_SOURCE_CODE", value.taxStatusSourceCode(), "منبع وضعیت مالیاتی الزامی است.");
            flag(errors, "DEPOSIT_OPENING_TAX_STATUS.WITHHOLDING_APPLICABLE", value.withholdingApplicable());
        }

        for (RewardEnrollment value : safe(aggregate.rewardEnrollments())) {
            if (value.programId() == null || value.programId() <= 0) errors.put("DEPOSIT_OPENING_REWARD_ENROLLMENT.PROGRAM_ID", "شناسه برنامه جایزه معتبر نیست.");
            required(errors, "DEPOSIT_OPENING_REWARD_ENROLLMENT.CONSENT_REFERENCE", value.consentReference(), "مرجع رضایت برای عضویت برنامه جایزه الزامی است.");
        }

        for (OpeningDocument value : safe(aggregate.documents())) {
            required(errors, "DEPOSIT_OPENING_DOCUMENT.DOCUMENT_TYPE_CODE", value.documentTypeCode(), "نوع مدرک الزامی است.");
            required(errors, "DEPOSIT_OPENING_DOCUMENT.DOCUMENT_STATUS_CODE", value.documentStatusCode(), "وضعیت مدرک الزامی است.");
            if (("RECEIVED".equals(upper(value.documentStatusCode())) || "VERIFIED".equals(upper(value.documentStatusCode()))) && blank(value.documentReference())) {
                errors.put("DEPOSIT_OPENING_DOCUMENT.DOCUMENT_REFERENCE", "برای مدرک دریافت/تأییدشده، مرجع مدرک الزامی است.");
            }
        }

        if ("APPROVED".equals(root.requestStatusCode())) {
            if (root.requestedOpeningDate() == null) {
                errors.put("DEPOSIT_OPENING_REQUEST.REQUESTED_OPENING_DATE", "تاریخ افتتاح برای درخواست تأییدشده الزامی است.");
            }
            if (aggregate.funding() == null || !"SUCCESS".equals(upper(aggregate.funding().fundingStatusCode()))) {
                errors.put("DEPOSIT_OPENING_FUNDING.FUNDING_STATUS_CODE", "برای درخواست تأییدشده، تأمین وجه باید SUCCESS باشد.");
            }
            if (safe(aggregate.checks()).isEmpty() || safe(aggregate.checks()).stream().anyMatch(c -> !"PASS".equals(upper(c.resultStatusCode())))) {
                errors.put("DEPOSIT_OPENING_CHECK.RESULT_STATUS_CODE", "برای درخواست تأییدشده، همه کنترل‌ها باید PASS باشند.");
            }
            if (safe(aggregate.documents()).stream().anyMatch(d -> "MISSING".equals(upper(d.documentStatusCode())) || "REJECTED".equals(upper(d.documentStatusCode())))) {
                errors.put("DEPOSIT_OPENING_DOCUMENT.DOCUMENT_STATUS_CODE", "در درخواست تأییدشده مدرک MISSING/REJECTED مجاز نیست.");
            }
            if (aggregate.termsAcceptance() == null || !"ACCEPTED".equals(upper(aggregate.termsAcceptance().acceptanceStatusCode()))) {
                errors.put("DEPOSIT_OPENING_TERMS_ACCEPTANCE", "پذیرش شروط برای درخواست تأییدشده الزامی است.");
            }
            if (aggregate.decision() == null || !"APPROVE".equals(upper(aggregate.decision().decisionCode()))) {
                errors.put("DEPOSIT_OPENING_DECISION.DECISION_CODE", "تصمیم نهایی درخواست تأییدشده باید APPROVE باشد.");
            }
        }

        if (!errors.isEmpty()) {
            throw new DepositOpeningValidationException("Aggregate افتتاح حساب معتبر نیست.", errors);
        }
    }

    private static LocalDate resolveMaturityDate(OpeningTerm term) {
        if (term.maturityDate() != null) return term.maturityDate();
        LocalDate start = term.startDate();
        if (start == null || term.termValue() == null || term.termUnitCode() == null) return null;
        return switch (upper(term.termUnitCode())) {
            case "DAY" -> start.plusDays(term.termValue());
            case "MONTH" -> start.plusMonths(term.termValue());
            case "YEAR" -> start.plusYears(term.termValue());
            default -> null;
        };
    }

    private static void put(Map<String, Integer> rows, String table, int count) {
        rows.merge(table, count, Integer::sum);
    }

    private static void required(Map<String, String> errors, String field, String value, String message) {
        if (blank(value)) errors.put(field, message);
    }

    private static void flag(Map<String, String> errors, String field, Integer value) {
        if (value != null && value != 0 && value != 1) errors.put(field, "مقدار پرچم باید صفر یا یک باشد.");
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static <T> List<T> safe(List<T> value) {
        return value == null ? List.of() : value;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String upper(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    public static String correlationId(String supplied) {
        if (supplied != null && !supplied.isBlank()) return supplied.trim();
        return UUID.randomUUID().toString();
    }
}
