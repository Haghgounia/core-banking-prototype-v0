package com.behsazan.corebanking.cif.application;

import com.behsazan.corebanking.cif.domain.CifModels.CifDashboardSummary;
import com.behsazan.corebanking.cif.domain.CifModels.ContactPointRequest;
import com.behsazan.corebanking.cif.domain.CifModels.CreatePartyRequest;
import com.behsazan.corebanking.cif.domain.CifModels.KycCaseRequest;
import com.behsazan.corebanking.cif.domain.CifModels.OrganizationRequest;
import com.behsazan.corebanking.cif.domain.CifModels.Party360Response;
import com.behsazan.corebanking.cif.domain.CifModels.PartyAddressRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyDocumentRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyIdentifierRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyNameRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartySummary;
import com.behsazan.corebanking.cif.domain.CifModels.PersonRequest;
import com.behsazan.corebanking.cif.domain.CifModels.RiskAssessmentRequest;
import com.behsazan.corebanking.cif.domain.CifModels.ScreeningResultRequest;
import com.behsazan.corebanking.cif.domain.CifModels.UpdatePartyRequest;
import com.behsazan.corebanking.cif.error.CifNotFoundException;
import com.behsazan.corebanking.cif.error.CifValidationException;
import com.behsazan.corebanking.cif.oracle.CifRepository;
import com.behsazan.corebanking.shared.model.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CifService {
    private static final Set<String> PARTY_TYPES = Set.of("PERSON", "ORGANIZATION");
    private static final Set<String> YES_NO = Set.of("Y", "N");
    private static final Set<String> DUE_DILIGENCE = Set.of("SDD", "CDD", "EDD");

    private final CifRepository repository;

    public CifService(CifRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<PartySummary> search(String text, String partyType, String status, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return repository.searchParties(text, partyType, status, safePage, safeSize);
    }

    @Transactional(readOnly = true)
    public Party360Response find(long partyId) {
        Party360Response response = repository.loadParty360(partyId);
        if (response == null) {
            throw new CifNotFoundException("پارتی با شناسه " + partyId + " یافت نشد.");
        }
        return response;
    }

    @Transactional(readOnly = true)
    public CifDashboardSummary dashboardSummary() {
        return repository.dashboardSummary();
    }

    @Transactional
    public Party360Response createParty(CreatePartyRequest raw, String actor) {
        String partyType = upper(raw.partyTypeCode());
        Map<String, String> errors = new LinkedHashMap<>();
        if (!PARTY_TYPES.contains(partyType)) {
            errors.put("partyTypeCode", "نوع پارتی فقط PERSON یا ORGANIZATION است.");
        }
        if ("ORGANIZATION".equals(partyType) && blank(raw.legalFormCode())) {
            errors.put("legalFormCode", "نوع شخصیت حقوقی الزامی است.");
        }
        if ("ORGANIZATION".equals(partyType) && blank(raw.registeredName())
                && raw.primaryName() != null && raw.primaryName().trim().length() > 300) {
            errors.put("registeredName", "نام ثبتی شخص حقوقی حداکثر ۳۰۰ کاراکتر است.");
        }
        reject(errors);

        CreatePartyRequest request = new CreatePartyRequest(
                partyType,
                raw.primaryName().trim(),
                defaultText(raw.lifecycleStatusCode(), "ACTIVE"),
                defaultText(raw.verificationStatusCode(), "UNVERIFIED"),
                defaultText(raw.dataQualityStatusCode(), "INCOMPLETE"),
                trimToNull(raw.creationSourceCode()),
                trimToNull(raw.legalFormCode()),
                trimToNull(raw.registeredName())
        );

        long partyId = repository.insertParty(request, actor);
        PartyNameRequest primaryName = normalizeName(new PartyNameRequest(
                "LEGAL", "fa", "Arab", null, null, null, null, null,
                request.primaryName(), request.primaryName(), request.primaryName(), request.primaryName(),
                null, "Y", LocalDate.now(), null, request.verificationStatusCode(),
                request.creationSourceCode(), null, null
        ));
        repository.insertName(partyId, primaryName, actor);

        if ("PERSON".equals(partyType)) {
            repository.insertPerson(partyId, defaultPersonRequest(), actor);
        } else {
            String registeredName = blank(request.registeredName()) ? request.primaryName() : request.registeredName();
            OrganizationRequest organization = new OrganizationRequest(
                    registeredName, null, upper(request.legalFormCode()), null, null,
                    null, null, null, null, "N", null
            );
            repository.insertOrganization(partyId, organization, actor);
        }
        return find(partyId);
    }

    @Transactional
    public Party360Response updateParty(long partyId, UpdatePartyRequest raw, String actor) {
        Party360Response current = find(partyId);
        requireVersion(raw.recordVersion());
        String lifecycle = upper(raw.lifecycleStatusCode());
        String isCurrent = upper(raw.isCurrent());
        Map<String, String> errors = new LinkedHashMap<>();
        checkFlag("isCurrent", isCurrent, errors);
        checkDateOrder("validFrom", raw.validFrom(), "validTo", raw.validTo(), errors);
        if ("MERGED".equals(lifecycle)) {
            if (raw.mergedIntoPartyId() == null) errors.put("mergedIntoPartyId", "برای وضعیت MERGED پارتی مقصد الزامی است.");
            if (raw.mergedIntoPartyId() != null && raw.mergedIntoPartyId() == partyId) errors.put("mergedIntoPartyId", "پارتی نمی‌تواند در خودش ادغام شود.");
        } else if (raw.mergedIntoPartyId() != null) {
            errors.put("mergedIntoPartyId", "پارتی مقصد فقط برای وضعیت MERGED قابل ثبت است.");
        }
        reject(errors);

        UpdatePartyRequest request = new UpdatePartyRequest(
                lifecycle,
                trimToNull(raw.statusReasonCode()),
                upper(raw.verificationStatusCode()),
                upper(raw.dataQualityStatusCode()),
                trimToNull(raw.creationSourceCode()),
                raw.mergedIntoPartyId(),
                raw.validFrom() == null ? current.party().validFrom() : raw.validFrom(),
                raw.validTo(),
                isCurrent,
                raw.recordVersion()
        );
        ensureUpdated(repository.updateParty(partyId, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response upsertPerson(long partyId, PersonRequest raw, String actor) {
        Party360Response current = find(partyId);
        if (!"PERSON".equals(current.party().partyTypeCode())) {
            throw validation("partyTypeCode", "این پارتی از نوع PERSON نیست.");
        }
        Map<String, String> errors = new LinkedHashMap<>();
        checkDateOrder("birthDate", raw.birthDate(), "deathDate", raw.deathDate(), errors);
        reject(errors);
        PersonRequest request = new PersonRequest(
                raw.birthDate(), trimToNull(raw.genderCode()), upperOrNull(raw.birthCountryCode()), raw.birthPlaceId(),
                trimToNull(raw.birthPlaceText()), trimToNull(raw.fatherGivenName()), trimToNull(raw.motherGivenName()),
                trimToNull(raw.maritalStatusCode()), raw.deathDate(), upper(raw.legalCapacityCode()),
                trimToNull(raw.primaryLanguageCode()), upper(raw.dataQualityStatusCode()), upper(raw.verificationStatusCode()),
                trimToNull(raw.residenceStatusCode()), trimToNull(raw.physicalAbility()), upper(raw.lifeStatusCode()), raw.recordVersion()
        );
        if (current.person() == null) {
            repository.insertPerson(partyId, request, actor);
        } else {
            requireVersion(raw.recordVersion());
            ensureUpdated(repository.updatePerson(partyId, request, actor));
        }
        return find(partyId);
    }

    @Transactional
    public Party360Response upsertOrganization(long partyId, OrganizationRequest raw, String actor) {
        Party360Response current = find(partyId);
        if (!"ORGANIZATION".equals(current.party().partyTypeCode())) {
            throw validation("partyTypeCode", "این پارتی از نوع ORGANIZATION نیست.");
        }
        Map<String, String> errors = new LinkedHashMap<>();
        checkDateOrder("incorporationDate", raw.incorporationDate(), "dissolutionDate", raw.dissolutionDate(), errors);
        String listed = upper(raw.listedCompanyFlag());
        checkFlag("listedCompanyFlag", listed, errors);
        reject(errors);
        OrganizationRequest request = new OrganizationRequest(
                raw.registeredName().trim(), trimToNull(raw.tradeName()), upper(raw.legalFormCode()),
                trimToNull(raw.registrationNo()), trimToNull(raw.registrationPlaceCode()), raw.incorporationDate(),
                raw.dissolutionDate(), trimToNull(raw.economicSectorCode()), trimToNull(raw.isicCode()), listed,
                raw.recordVersion()
        );
        if (current.organization() == null) {
            repository.insertOrganization(partyId, request, actor);
        } else {
            requireVersion(raw.recordVersion());
            ensureUpdated(repository.updateOrganization(partyId, request, actor));
        }
        return find(partyId);
    }

    @Transactional
    public Party360Response createName(long partyId, PartyNameRequest raw, String actor) {
        find(partyId);
        PartyNameRequest request = normalizeName(raw);
        validateName(request);
        if ("Y".equals(request.isPrimary())) repository.clearPrimaryNames(partyId, null, actor);
        repository.insertName(partyId, request, actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateName(long partyId, long id, PartyNameRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        PartyNameRequest request = normalizeName(raw);
        validateName(request);
        if ("Y".equals(request.isPrimary())) repository.clearPrimaryNames(partyId, id, actor);
        ensureUpdated(repository.updateName(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteName(long partyId, long id) {
        find(partyId);
        ensureDeleted(repository.deleteName(partyId, id));
        return find(partyId);
    }

    @Transactional
    public Party360Response createIdentifier(long partyId, PartyIdentifierRequest raw, String actor) {
        find(partyId);
        PartyIdentifierRequest request = normalizeIdentifier(raw);
        validateIdentifier(request);
        if ("Y".equals(request.isPrimary())) repository.clearPrimaryIdentifiers(partyId, null, actor);
        repository.insertIdentifier(partyId, request, actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateIdentifier(long partyId, long id, PartyIdentifierRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        PartyIdentifierRequest request = normalizeIdentifier(raw);
        validateIdentifier(request);
        if ("Y".equals(request.isPrimary())) repository.clearPrimaryIdentifiers(partyId, id, actor);
        ensureUpdated(repository.updateIdentifier(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteIdentifier(long partyId, long id) {
        find(partyId);
        ensureDeleted(repository.deleteIdentifier(partyId, id));
        return find(partyId);
    }

    @Transactional
    public Party360Response createAddress(long partyId, PartyAddressRequest raw, String actor) {
        find(partyId);
        PartyAddressRequest request = normalizeAddress(raw);
        validateAddress(request);
        if ("Y".equals(request.isPrimary())) repository.clearPrimaryAddresses(partyId, null, actor);
        repository.insertAddress(partyId, request, actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateAddress(long partyId, long id, PartyAddressRequest raw, String actor) {
        find(partyId);
        if (raw.partyAddressRecordVersion() == null || raw.addressRecordVersion() == null) {
            throw validation("recordVersion", "نسخه رکورد نشانی الزامی است.");
        }
        PartyAddressRequest request = normalizeAddress(raw);
        validateAddress(request);
        if ("Y".equals(request.isPrimary())) repository.clearPrimaryAddresses(partyId, id, actor);
        ensureUpdated(repository.updateAddress(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteAddress(long partyId, long id) {
        find(partyId);
        ensureDeleted(repository.deleteAddress(partyId, id));
        return find(partyId);
    }

    @Transactional
    public Party360Response createContact(long partyId, ContactPointRequest raw, String actor) {
        find(partyId);
        ContactPointRequest request = normalizeContact(raw);
        validateContact(request);
        if ("Y".equals(request.isPrimary())) repository.clearPrimaryContacts(partyId, null, actor);
        repository.insertContact(partyId, request, actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateContact(long partyId, long id, ContactPointRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        ContactPointRequest request = normalizeContact(raw);
        validateContact(request);
        if ("Y".equals(request.isPrimary())) repository.clearPrimaryContacts(partyId, id, actor);
        ensureUpdated(repository.updateContact(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteContact(long partyId, long id) {
        find(partyId);
        ensureDeleted(repository.deleteContact(partyId, id));
        return find(partyId);
    }

    @Transactional
    public Party360Response createKycCase(long partyId, KycCaseRequest raw, String actor) {
        find(partyId);
        KycCaseRequest request = normalizeKyc(raw);
        validateKyc(request);
        repository.insertKycCase(partyId, request, actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateKycCase(long partyId, long id, KycCaseRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        KycCaseRequest request = normalizeKyc(raw);
        validateKyc(request);
        ensureUpdated(repository.updateKycCase(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteKycCase(long partyId, long id) {
        find(partyId);
        ensureDeleted(repository.deleteKycCase(partyId, id));
        return find(partyId);
    }

    @Transactional
    public Party360Response createDocument(long partyId, PartyDocumentRequest raw, String actor) {
        find(partyId);
        PartyDocumentRequest request = normalizeDocument(raw);
        validateDocument(partyId, request);
        repository.insertDocument(partyId, request, actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateDocument(long partyId, long id, PartyDocumentRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        PartyDocumentRequest request = normalizeDocument(raw);
        validateDocument(partyId, request);
        ensureUpdated(repository.updateDocument(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteDocument(long partyId, long id) {
        find(partyId);
        ensureDeleted(repository.deleteDocument(partyId, id));
        return find(partyId);
    }

    @Transactional
    public Party360Response createRisk(long partyId, RiskAssessmentRequest raw, String actor) {
        find(partyId);
        RiskAssessmentRequest request = normalizeRisk(raw);
        validateRisk(partyId, request);
        repository.insertRisk(partyId, request, actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateRisk(long partyId, long id, RiskAssessmentRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        RiskAssessmentRequest request = normalizeRisk(raw);
        validateRisk(partyId, request);
        ensureUpdated(repository.updateRisk(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteRisk(long partyId, long id) {
        find(partyId);
        ensureDeleted(repository.deleteRisk(partyId, id));
        return find(partyId);
    }

    @Transactional
    public Party360Response createScreening(long partyId, ScreeningResultRequest raw, String actor) {
        find(partyId);
        ScreeningResultRequest request = normalizeScreening(raw);
        validateScreening(partyId, request);
        repository.insertScreening(partyId, request);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateScreening(long partyId, long id, ScreeningResultRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        ScreeningResultRequest request = normalizeScreening(raw);
        validateScreening(partyId, request);
        ensureUpdated(repository.updateScreening(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteScreening(long partyId, long id) {
        find(partyId);
        ensureDeleted(repository.deleteScreening(partyId, id));
        return find(partyId);
    }

    private static PersonRequest defaultPersonRequest() {
        return new PersonRequest(
                null, null, null, null, null, null, null, null, null,
                "FULL", null, "INCOMPLETE", "UNVERIFIED", null, null, "ALIVE", null
        );
    }

    private static PartyNameRequest normalizeName(PartyNameRequest raw) {
        String full = raw.fullName().trim();
        String display = blank(raw.displayName()) ? full : raw.displayName().trim();
        String sort = blank(raw.sortName()) ? display : raw.sortName().trim();
        String normalized = blank(raw.normalizedName()) ? normalizeText(full) : raw.normalizedName().trim();
        return new PartyNameRequest(
                upper(raw.nameTypeCode()), trimToNull(raw.languageCode()), trimToNull(raw.scriptCode()),
                trimToNull(raw.prefixText()), trimToNull(raw.givenName()), trimToNull(raw.middleName()),
                trimToNull(raw.familyName()), trimToNull(raw.suffixText()), full, display, sort, normalized,
                trimToNull(raw.phoneticKey()), upper(raw.isPrimary()),
                raw.validFrom() == null ? LocalDate.now() : raw.validFrom(), raw.validTo(),
                upper(raw.verificationStatusCode()), trimToNull(raw.sourceCode()), trimToNull(raw.sourceReference()),
                raw.recordVersion()
        );
    }

    private static void validateName(PartyNameRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        checkFlag("isPrimary", request.isPrimary(), errors);
        checkDateOrder("validFrom", request.validFrom(), "validTo", request.validTo(), errors);
        reject(errors);
    }

    private static PartyIdentifierRequest normalizeIdentifier(PartyIdentifierRequest raw) {
        String value = raw.identifierValue().trim();
        String normalized = blank(raw.normalizedIdentifierValue()) ? normalizeIdentifierValue(value) : raw.normalizedIdentifierValue().trim();
        return new PartyIdentifierRequest(
                upper(raw.identifierTypeCode()), value, normalized, upperOrNull(raw.issuingCountryCode()),
                trimToNull(raw.issuingAuthorityCode()), trimToNull(raw.issuerCode()), raw.issueDate(), raw.expiryDate(),
                upper(raw.isPrimary()), upper(raw.isActive()), upper(raw.verificationStatusCode()),
                trimToNull(raw.verificationSourceCode()), trimToNull(raw.verificationMethodCode()), raw.verifiedAt(),
                raw.validFrom() == null ? LocalDate.now() : raw.validFrom(), raw.validTo(), raw.recordVersion()
        );
    }

    private static void validateIdentifier(PartyIdentifierRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        checkFlag("isPrimary", r.isPrimary(), errors);
        checkFlag("isActive", r.isActive(), errors);
        checkDateOrder("issueDate", r.issueDate(), "expiryDate", r.expiryDate(), errors);
        checkDateOrder("validFrom", r.validFrom(), "validTo", r.validTo(), errors);
        reject(errors);
    }

    private static PartyAddressRequest normalizeAddress(PartyAddressRequest raw) {
        return new PartyAddressRequest(
                upper(raw.addressTypeCode()), upper(raw.isPrimary()),
                raw.validFrom() == null ? LocalDate.now() : raw.validFrom(), raw.validTo(),
                upper(raw.countryCode()), trimToNull(raw.provinceCode()), trimToNull(raw.cityCode()),
                trimToNull(raw.districtCode()), trimToNull(raw.postalCode()), raw.addressLine1().trim(),
                trimToNull(raw.addressLine2()), raw.partyAddressRecordVersion(), raw.addressRecordVersion()
        );
    }

    private static void validateAddress(PartyAddressRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        checkFlag("isPrimary", r.isPrimary(), errors);
        checkDateOrder("validFrom", r.validFrom(), "validTo", r.validTo(), errors);
        reject(errors);
    }

    private static ContactPointRequest normalizeContact(ContactPointRequest raw) {
        String value = raw.contactValue().trim();
        return new ContactPointRequest(
                upper(raw.contactTypeCode()), value,
                blank(raw.normalizedValue()) ? normalizeIdentifierValue(value) : raw.normalizedValue().trim(),
                upper(raw.purposeCode()), upper(raw.isPrimary()), upper(raw.isVerified()), raw.verifiedAt(),
                raw.validFrom() == null ? LocalDate.now() : raw.validFrom(), raw.validTo(), raw.recordVersion()
        );
    }

    private static void validateContact(ContactPointRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        checkFlag("isPrimary", r.isPrimary(), errors);
        checkFlag("isVerified", r.isVerified(), errors);
        checkDateOrder("validFrom", r.validFrom(), "validTo", r.validTo(), errors);
        if ("Y".equals(r.isVerified()) && r.verifiedAt() == null) errors.put("verifiedAt", "برای راه تماس تأییدشده زمان تأیید الزامی است.");
        if ("N".equals(r.isVerified()) && r.verifiedAt() != null) errors.put("verifiedAt", "برای راه تماس تأییدنشده زمان تأیید باید خالی باشد.");
        reject(errors);
    }

    private static KycCaseRequest normalizeKyc(KycCaseRequest raw) {
        return new KycCaseRequest(
                upper(raw.kycTypeCode()), upper(raw.dueDiligenceLevelCode()), upper(raw.statusCode()),
                raw.openedAt() == null ? LocalDateTime.now() : raw.openedAt(), raw.completedAt(), raw.reviewedAt(),
                raw.nextReviewDate(), upperOrNull(raw.finalRiskLevelCode()), upperOrNull(raw.decisionCode()),
                trimToNull(raw.decisionReason()), trimToNull(raw.approvedBy()), raw.recordVersion()
        );
    }

    private static void validateKyc(KycCaseRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!DUE_DILIGENCE.contains(r.dueDiligenceLevelCode())) errors.put("dueDiligenceLevelCode", "سطح بررسی باید SDD، CDD یا EDD باشد.");
        checkDateTimeOrder("openedAt", r.openedAt(), "completedAt", r.completedAt(), errors);
        checkDateTimeOrder("openedAt", r.openedAt(), "reviewedAt", r.reviewedAt(), errors);
        reject(errors);
    }

    private PartyDocumentRequest normalizeDocument(PartyDocumentRequest raw) {
        return new PartyDocumentRequest(
                raw.kycCaseId(), upper(raw.documentTypeCode()), raw.documentNumber().trim(), trimToNull(raw.issuerCode()),
                raw.issueDate(), raw.expiryDate(), upper(raw.verificationStatusCode()), raw.verifiedAt(),
                raw.contentHash().trim(), raw.storageRef().trim(), raw.mimeType().trim(), raw.recordVersion()
        );
    }

    private void validateDocument(long partyId, PartyDocumentRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        checkDateOrder("issueDate", r.issueDate(), "expiryDate", r.expiryDate(), errors);
        if (!repository.kycCaseBelongsToParty(partyId, r.kycCaseId())) errors.put("kycCaseId", "پرونده KYC انتخاب‌شده متعلق به این پارتی نیست.");
        reject(errors);
    }

    private RiskAssessmentRequest normalizeRisk(RiskAssessmentRequest raw) {
        return new RiskAssessmentRequest(
                raw.kycCaseId(), upper(raw.riskTypeCode()), raw.scoreValue(), upper(raw.ratingCode()),
                upper(raw.decisionCode()), upper(raw.modelCode()), raw.modelVersion().trim(),
                raw.assessmentDate() == null ? LocalDateTime.now() : raw.assessmentDate(), raw.validTo(),
                trimToNull(raw.explanation()), raw.recordVersion()
        );
    }

    private void validateRisk(long partyId, RiskAssessmentRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        checkDateTimeOrder("assessmentDate", r.assessmentDate(), "validTo", r.validTo(), errors);
        if (!repository.kycCaseBelongsToParty(partyId, r.kycCaseId())) errors.put("kycCaseId", "پرونده KYC انتخاب‌شده متعلق به این پارتی نیست.");
        reject(errors);
    }

    private ScreeningResultRequest normalizeScreening(ScreeningResultRequest raw) {
        return new ScreeningResultRequest(
                raw.kycCaseId(), upper(raw.screeningTypeCode()), upper(raw.sourceListCode()), upper(raw.providerCode()),
                trimToNull(raw.providerReferenceNo()), trimToNull(raw.matchedName()), raw.matchScore(),
                upper(raw.initialDecisionCode()), upperOrNull(raw.finalDecisionCode()), upper(raw.falsePositiveFlag()),
                raw.screenedAt() == null ? LocalDateTime.now() : raw.screenedAt(), raw.reviewedAt(),
                trimToNull(raw.reviewedBy()), trimToNull(raw.payloadRef()), raw.recordVersion()
        );
    }

    private void validateScreening(long partyId, ScreeningResultRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        checkFlag("falsePositiveFlag", r.falsePositiveFlag(), errors);
        if ((r.reviewedAt() == null) != (r.reviewedBy() == null)) errors.put("reviewedAt", "زمان و کاربر بازبینی باید همزمان ثبت یا خالی شوند.");
        if (!repository.kycCaseBelongsToParty(partyId, r.kycCaseId())) errors.put("kycCaseId", "پرونده KYC انتخاب‌شده متعلق به این پارتی نیست.");
        reject(errors);
    }

    private static void checkDateOrder(String fromName, LocalDate from, String toName, LocalDate to, Map<String, String> errors) {
        if (from != null && to != null && to.isBefore(from)) errors.put(toName, "تاریخ پایان نمی‌تواند قبل از تاریخ شروع باشد.");
    }

    private static void checkDateTimeOrder(String fromName, LocalDateTime from, String toName, LocalDateTime to, Map<String, String> errors) {
        if (from != null && to != null && to.isBefore(from)) errors.put(toName, "زمان پایان نمی‌تواند قبل از زمان شروع باشد.");
    }

    private static void checkFlag(String field, String value, Map<String, String> errors) {
        if (!YES_NO.contains(value)) errors.put(field, "مقدار باید Y یا N باشد.");
    }

    private static void requireVersion(Long version) {
        if (version == null || version < 1) throw validation("recordVersion", "نسخه رکورد معتبر نیست.");
    }

    private static void ensureUpdated(int updated) {
        if (updated == 0) throw validation("recordVersion", "رکورد توسط کاربر دیگری تغییر کرده یا دیگر وجود ندارد. اطلاعات را دوباره بارگذاری کنید.");
    }

    private static void ensureDeleted(int deleted) {
        if (deleted == 0) throw new CifNotFoundException("رکورد درخواستی یافت نشد.");
    }

    private static void reject(Map<String, String> errors) {
        if (!errors.isEmpty()) throw new CifValidationException("اطلاعات فرم CIF معتبر نیست.", errors);
    }

    private static CifValidationException validation(String field, String message) {
        return new CifValidationException("اطلاعات فرم CIF معتبر نیست.", Map.of(field, message));
    }

    private static String defaultText(String value, String fallback) {
        return blank(value) ? fallback : upper(value);
    }

    private static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String upperOrNull(String value) {
        return blank(value) ? null : upper(value);
    }

    private static String trimToNull(String value) {
        return blank(value) ? null : value.trim();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalizeIdentifierValue(String value) {
        return value == null ? null : value.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private static String normalizeText(String value) {
        if (value == null) return null;
        return value.trim().replace('ي', 'ی').replace('ك', 'ک').replaceAll("\\s+", " ");
    }
}
