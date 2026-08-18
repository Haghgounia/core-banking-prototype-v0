package com.behsazan.corebanking.cif.application;

import com.behsazan.corebanking.cif.domain.CifModels.*;
import com.behsazan.corebanking.cif.domain.CifModels.CifDashboardSummary;
import com.behsazan.corebanking.cif.domain.CifModels.ExternalInquiryRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyConsentRecord;
import com.behsazan.corebanking.cif.domain.CifModels.PartyConsentRequest;
import com.behsazan.corebanking.cif.domain.CifModels.CommunicationPreferenceRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyGeneralPreferenceRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyStatusChangeRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyMergeRequest;
import com.behsazan.corebanking.cif.domain.CifModels.ContactPointAddressRequest;
import com.behsazan.corebanking.cif.domain.CifModels.ContactPointRequest;
import com.behsazan.corebanking.cif.domain.CifModels.CreatePartyRequest;
import com.behsazan.corebanking.cif.domain.CifModels.FinancialProfileRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyAssetLiabilityRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyEmploymentRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyIncomeSourceRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyLicenseRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyClassificationRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyRelationshipRequest;
import com.behsazan.corebanking.cif.domain.CifModels.BeneficialOwnershipRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyAuthorityRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyRoleRecord;
import com.behsazan.corebanking.cif.domain.CifModels.PartyRoleRequest;
import com.behsazan.corebanking.cif.domain.CifModels.KycCaseRequest;
import com.behsazan.corebanking.cif.domain.CifModels.OrganizationRequest;
import com.behsazan.corebanking.cif.domain.CifModels.Party360Response;
import com.behsazan.corebanking.cif.domain.CifModels.PartyAddressRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyDocumentRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyIdentifierRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyNameRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyOnboardingRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartySummary;
import com.behsazan.corebanking.cif.domain.CifModels.PersonRequest;
import com.behsazan.corebanking.cif.domain.CifModels.RiskAssessmentRequest;
import com.behsazan.corebanking.cif.domain.CifModels.ScreeningResultRequest;
import com.behsazan.corebanking.cif.domain.CifModels.UpdatePartyRequest;
import com.behsazan.corebanking.cif.error.CifNotFoundException;
import com.behsazan.corebanking.cif.error.CifValidationException;
import com.behsazan.corebanking.cif.oracle.CifRepository;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.LookupOption;
import com.behsazan.corebanking.shared.model.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CifService {
    private static final Set<String> PARTY_TYPES = Set.of("PERSON", "ORGANIZATION");
    private static final Set<String> YES_NO = Set.of("Y", "N");
    private static final Set<String> DUE_DILIGENCE = Set.of("SDD", "CDD", "EDD");
    private static final Set<String> RELATIONSHIP_TYPES = Set.of(
            "SPOUSE", "PARENT", "CHILD", "LEGAL_REPRESENTATIVE", "GUARDIAN",
            "DIRECTOR", "BOARD_MEMBER", "SIGNATORY", "BENEFICIAL_OWNER", "PARENT_COMPANY", "AFFILIATE"
    );
    private static final Set<String> FAMILY_RELATIONSHIP_TYPES = Set.of("SPOUSE", "PARENT", "CHILD");
    private static final Set<String> ORGANIZATION_RELATIONSHIP_TYPES = Set.of("PARENT_COMPANY", "AFFILIATE");
    private static final Set<String> ROLES_REQUIRING_PRINCIPAL = Set.of(
            "ATTORNEY", "GUARDIAN", "EXECUTOR", "TRUSTEE", "LEGAL_REPRESENTATIVE", "DIRECTOR",
            "SIGNATORY", "GUARANTOR", "PLEDGOR", "BENEFICIAL_OWNER", "BENEFICIARY"
    );
    private static final Set<String> AUTHORITY_DOCUMENT_ROLES = Set.of(
            "ATTORNEY", "GUARDIAN", "EXECUTOR", "TRUSTEE", "LEGAL_REPRESENTATIVE", "SIGNATORY"
    );
    private static final Set<String> MERGE_REASONS = Set.of("ثبت تکراری", "اصلاح شناسه هویتی");
    private static final Set<String> MERGE_CONFLICT_RESOLUTIONS = Set.of("حفظ اطلاعات تأییدشده", "بررسی دستی");

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
    public PartyReadinessSummary readiness(long partyId) {
        Party360Response response = find(partyId);
        LocalDate today = LocalDate.now();
        boolean primaryName = response.names().stream().anyMatch(n -> "Y".equals(n.isPrimary())
                && (n.validFrom() == null || !n.validFrom().isAfter(today))
                && (n.validTo() == null || !n.validTo().isBefore(today)));
        boolean primaryIdentifier = response.identifiers().stream().anyMatch(i -> "Y".equals(i.isPrimary()) && "Y".equals(i.isActive())
                && (i.validFrom() == null || !i.validFrom().isAfter(today))
                && (i.validTo() == null || !i.validTo().isBefore(today)));
        boolean profile = "PERSON".equals(response.party().partyTypeCode()) ? response.person() != null : response.organization() != null;
        boolean identityComplete = profile && primaryName && primaryIdentifier;

        Set<Long> activeCustomerRoleIds = response.roles().stream()
                .filter(r -> "CUSTOMER".equals(r.roleTypeCode()))
                .filter(r -> !Set.of("CLOSED", "REVOKED", "EXPIRED", "INACTIVE").contains(r.statusCode()))
                .filter(r -> r.validFrom() == null || !r.validFrom().isAfter(today))
                .filter(r -> r.validTo() == null || !r.validTo().isBefore(today))
                .map(PartyRoleRecord::partyRoleId)
                .collect(java.util.stream.Collectors.toSet());
        boolean customerRole = !activeCustomerRoleIds.isEmpty();
        PartyCustomerRecord currentCustomer = response.customers().stream()
                .filter(c -> activeCustomerRoleIds.contains(c.partyRoleId()))
                .filter(c -> "Y".equals(c.isCurrent()))
                .filter(c -> c.validFrom() == null || !c.validFrom().isAfter(today))
                .filter(c -> c.validTo() == null || !c.validTo().isBefore(today))
                .findFirst().orElse(null);
        String customerNo = currentCustomer == null ? null : currentCustomer.customerNo();

        boolean addressComplete = !response.addresses().isEmpty();
        boolean contactComplete = !response.contacts().isEmpty();
        boolean financialComplete = !response.financialProfiles().isEmpty();
        boolean customerRelationshipComplete = !customerRole || (currentCustomer != null && !blank(customerNo));
        boolean kycComplete = response.kycCases().stream().anyMatch(k -> !blank(k.finalRiskLevelCode()) && !blank(k.decisionCode()));
        boolean consentComplete = response.consents().stream().anyMatch(c -> "GRANT".equals(c.customerDecisionCode())
                && "GRANTED".equals(c.consentStatusCode()) && (c.validTo() == null || !c.validTo().isBefore(today)));

        List<PartyReadinessItem> items = List.of(
                readinessItem("IDENTITY", "هویت پایه و شناسه اصلی", true, identityComplete,
                        response.names().size() + response.identifiers().size(), "/cif/parties/" + partyId,
                        identityComplete ? "پروفایل، نام اصلی و شناسه اصلی موجود است." : "پروفایل، نام اصلی یا شناسه اصلی جاری ناقص است."),
                readinessItem("CONTACT_ADDRESS", "نشانی و راه تماس", customerRole, addressComplete && contactComplete,
                        response.addresses().size() + response.contacts().size(), "/cif/parties/" + partyId + "/onboarding/contact-address",
                        "نشانی و تماس برای Party دارای نقش مشتری بانک الزامی است."),
                readinessItem("FINANCIAL", "اطلاعات مالی و شغلی/اقتصادی", customerRole, financialComplete,
                        response.financialProfiles().size(), "/cif/parties/" + partyId + "/onboarding/financial-employment",
                        "حداقل یک نمایه مالی برای نقش مشتری بانک لازم است."),
                readinessItem("CUSTOMER_ROLE", "نقش مشتری و رابطه بانکی", customerRole, customerRelationshipComplete,
                        response.roles().size() + response.customers().size(), "/cif/parties/" + partyId + "/onboarding/roles",
                        customerRole ? "Customer Role باید رابطه جاری و شماره مشتری داشته باشد." : "این Party نقش مشتری بانک ندارد و شماره مشتری برای آن الزامی نیست."),
                readinessItem("KYC", "KYC، ریسک و تصمیم", customerRole, kycComplete,
                        response.kycCases().size(), "/cif/parties/" + partyId + "/onboarding/kyc-risk",
                        "برای مشتری بانک حداقل یک KYC دارای سطح ریسک نهایی و تصمیم لازم است."),
                readinessItem("CONSENT", "رضایت معتبر", customerRole, consentComplete,
                        response.consents().size(), "/cif/parties/" + partyId + "/onboarding/consents-preferences",
                        "برای مشتری بانک حداقل یک رضایت اعطاشده و معتبر لازم است.")
        );

        List<String> blockers = new java.util.ArrayList<>();
        if ("MERGED".equals(response.party().lifecycleStatusCode()) || response.party().mergedIntoPartyId() != null) {
            blockers.add("Party ادغام شده است و پرونده مبدأ قابل Finalize مجدد نیست.");
        }
        for (PartyReadinessItem item : items) {
            if (item.required() && !item.complete()) blockers.add(item.label());
        }
        int requiredTotal = (int) items.stream().filter(PartyReadinessItem::required).count();
        int requiredCompleted = (int) items.stream().filter(i -> i.required() && i.complete()).count();
        return new PartyReadinessSummary(customerRole, customerNo, blockers.isEmpty(), requiredCompleted, requiredTotal, items, List.copyOf(blockers));
    }

    private static PartyReadinessItem readinessItem(String code, String label, boolean required, boolean complete,
                                                    int recordCount, String actionPath, String detail) {
        return new PartyReadinessItem(code, label, required, complete, recordCount, actionPath, detail);
    }

    @Transactional(readOnly = true)
    public List<LookupOption> classificationValues(String typeCode, String text, int limit) {
        if (blank(typeCode)) return List.of();
        return repository.classificationValueLookup(upper(typeCode), text, Math.min(Math.max(limit, 1), 200));
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
        checkDateOrder("validFrom", raw.validFrom(), "validTo", raw.validTo(), errors);
        reject(errors);

        String lifecycleStatus = defaultText(raw.lifecycleStatusCode(), "ACTIVE");
        String statusReason = defaultText(raw.statusReasonCode(), "NEW_REGISTRATION");
        if (!repository.activeReferenceCodeExists("REF_PARTY_LIFECYCLE_STATUS", "LIFECYCLE_STATUS_CODE", lifecycleStatus)) {
            errors.put("lifecycleStatusCode", "وضعیت چرخه عمر در داده مرجع فعال یافت نشد.");
        }
        if (!repository.activeStatusReasonExists(statusReason)) {
            errors.put("statusReasonCode", "دلیل وضعیت در داده مرجع فعال یافت نشد.");
        }
        reject(errors);

        CreatePartyRequest request = new CreatePartyRequest(
                partyType,
                raw.primaryName().trim(),
                lifecycleStatus,
                statusReason,
                defaultText(raw.verificationStatusCode(), "UNVERIFIED"),
                defaultText(raw.dataQualityStatusCode(), "INCOMPLETE"),
                trimToNull(raw.creationSourceCode()),
                raw.validFrom() == null ? LocalDate.now() : raw.validFrom(),
                raw.validTo(),
                trimToNull(raw.legalFormCode()),
                trimToNull(raw.registeredName())
        );

        long partyId = repository.insertParty(request, actor);
        repository.insertStatusHistory(partyId, request.lifecycleStatusCode(), request.statusReasonCode(),
                request.validFrom(), null, "وضعیت اولیه Party", actor);
        PartyNameRequest primaryName = normalizeName(new PartyNameRequest(
                "LEGAL", "fa", "Arab", null, null, null, null, null,
                request.primaryName(), request.primaryName(), request.primaryName(), request.primaryName(),
                null, "Y", request.validFrom(), request.validTo(), request.verificationStatusCode(),
                request.creationSourceCode(), null, null
        ));
        repository.insertName(partyId, primaryName, actor);

        if ("PERSON".equals(partyType)) {
            repository.insertPerson(partyId, defaultPersonRequest(), actor);
        } else {
            String registeredName = blank(request.registeredName()) ? request.primaryName() : request.registeredName();
            OrganizationRequest organization = new OrganizationRequest(
                    registeredName, null, upper(request.legalFormCode()), null, null,
                    null, null, null, null, "N", null, null, null, null, null, null, null
            );
            repository.insertOrganization(partyId, organization, actor);
        }
        return find(partyId);
    }

    @Transactional
    public Party360Response onboardParty(PartyOnboardingRequest raw, String actor) {
        String partyType = upper(raw.party().partyTypeCode());
        Map<String, String> errors = new LinkedHashMap<>();
        if (!PARTY_TYPES.contains(partyType)) {
            errors.put("party.partyTypeCode", "نوع پارتی فقط PERSON یا ORGANIZATION است.");
        } else if ("PERSON".equals(partyType) && raw.person() == null) {
            errors.put("person", "اطلاعات شخص حقیقی برای Party از نوع PERSON الزامی است.");
        } else if ("ORGANIZATION".equals(partyType) && raw.organization() == null) {
            errors.put("organization", "اطلاعات شخص حقوقی برای Party از نوع ORGANIZATION الزامی است.");
        }
        reject(errors);

        CreatePartyRequest source = raw.party();
        OrganizationRequest organization = raw.organization();
        CreatePartyRequest createRequest = new CreatePartyRequest(
                source.partyTypeCode(), source.primaryName(), source.lifecycleStatusCode(), source.statusReasonCode(),
                source.verificationStatusCode(), source.dataQualityStatusCode(), source.creationSourceCode(),
                source.validFrom(), source.validTo(),
                "ORGANIZATION".equals(partyType)
                        ? defaultText(source.legalFormCode(), organization.legalFormCode())
                        : source.legalFormCode(),
                "ORGANIZATION".equals(partyType)
                        ? (blank(source.registeredName()) ? organization.registeredName() : source.registeredName())
                        : source.registeredName()
        );

        Party360Response created = createParty(createRequest, actor);
        long partyId = created.party().partyId();

        if (raw.primaryNameDetails() != null) {
            var currentPrimaryName = created.names().stream()
                    .filter(name -> "Y".equalsIgnoreCase(name.isPrimary()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Primary PARTY_NAME was not created."));
            PartyNameRequest name = raw.primaryNameDetails();
            updateName(partyId, currentPrimaryName.partyNameId(), new PartyNameRequest(
                    "LEGAL", name.languageCode(), name.scriptCode(), name.prefixText(), name.givenName(),
                    name.middleName(), name.familyName(), name.suffixText(), name.fullName(), name.displayName(),
                    name.sortName(), name.normalizedName(), name.phoneticKey(), "Y",
                    name.validFrom() == null ? created.party().validFrom() : name.validFrom(),
                    name.validTo() == null ? created.party().validTo() : name.validTo(),
                    defaultText(name.verificationStatusCode(), created.party().verificationStatusCode()),
                    blank(name.sourceCode()) ? created.party().creationSourceCode() : name.sourceCode(),
                    name.sourceReference(), currentPrimaryName.recordVersion()
            ), actor);
        }

        if ("PERSON".equals(partyType)) {
            PersonRequest person = raw.person();
            upsertPerson(partyId, new PersonRequest(
                    person.birthDate(), person.genderCode(), person.birthCountryCode(), person.birthPlaceId(),
                    person.birthPlaceText(), person.fatherGivenName(), person.motherGivenName(),
                    person.maritalStatusCode(), person.deathDate(), person.legalCapacityCode(),
                    person.primaryLanguageCode(), person.dataQualityStatusCode(), person.verificationStatusCode(),
                    person.residenceStatusCode(), person.physicalAbility(), person.lifeStatusCode(),
                    person.nationalityCountryCode(), created.person().recordVersion()
            ), actor);
        } else {
            OrganizationRequest org = raw.organization();
            upsertOrganization(partyId, new OrganizationRequest(
                    org.registeredName(), org.tradeName(), org.legalFormCode(), org.registrationNo(),
                    org.registrationPlaceCode(), org.incorporationDate(), org.dissolutionDate(),
                    org.economicSectorCode(), org.isicCode(), org.listedCompanyFlag(),
                    org.registrationCountryCode(), org.activityStatusCode(), org.mainActivityDescription(),
                    org.employeeCount(), org.enterpriseSizeCode(), org.ownershipTypeCode(),
                    created.organization().recordVersion()
            ), actor);
        }

        PartyIdentifierRequest identifier = raw.primaryIdentifier();
        createIdentifier(partyId, new PartyIdentifierRequest(
                identifier.identifierTypeCode(), identifier.identifierValue(), identifier.normalizedIdentifierValue(),
                identifier.issuingCountryCode(), identifier.issuingAuthorityCode(), identifier.issuerCode(),
                identifier.issueDate(), identifier.expiryDate(), "Y", "Y",
                defaultText(identifier.verificationStatusCode(), "UNVERIFIED"),
                identifier.verificationSourceCode(), identifier.verificationMethodCode(), identifier.verifiedAt(),
                identifier.validFrom() == null ? created.party().validFrom() : identifier.validFrom(),
                identifier.validTo(), null
        ), actor);

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
        if (!lifecycle.equals(current.party().lifecycleStatusCode())) {
            errors.put("lifecycleStatusCode", "تغییر چرخه عمر فقط از عملیات اختصاصی «تغییر وضعیت Party» مجاز است.");
        }
        if (!java.util.Objects.equals(raw.mergedIntoPartyId(), current.party().mergedIntoPartyId())) {
            errors.put("mergedIntoPartyId", "مقصد ادغام فقط از عملیات اختصاصی Merge قابل تغییر است.");
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
                trimToNull(raw.residenceStatusCode()), trimToNull(raw.physicalAbility()), upper(raw.lifeStatusCode()),
                upperOrNull(raw.nationalityCountryCode()), raw.recordVersion()
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
        if (raw.employeeCount() != null && raw.employeeCount() < 0) {
            errors.put("employeeCount", "تعداد کارکنان نمی‌تواند منفی باشد.");
        }
        reject(errors);
        OrganizationRequest request = new OrganizationRequest(
                raw.registeredName().trim(), trimToNull(raw.tradeName()), upper(raw.legalFormCode()),
                trimToNull(raw.registrationNo()), trimToNull(raw.registrationPlaceCode()), raw.incorporationDate(),
                raw.dissolutionDate(), trimToNull(raw.economicSectorCode()), trimToNull(raw.isicCode()), listed,
                upperOrNull(raw.registrationCountryCode()), upperOrNull(raw.activityStatusCode()),
                trimToNull(raw.mainActivityDescription()), raw.employeeCount(), upperOrNull(raw.enterpriseSizeCode()),
                upperOrNull(raw.ownershipTypeCode()), raw.recordVersion()
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
        if (repository.identifierExists(request.identifierTypeCode(), request.identifierValue(), request.issuerCode(), request.validFrom(), null)) {
            throw validation("identifierValue", "این شناسه با نوع، صادرکننده و تاریخ شروع اعتبار یکسان قبلاً ثبت شده است.");
        }
        if ("Y".equals(request.isPrimary()) && repository.hasPrimaryIdentifier(partyId, null)) {
            throw validation("isPrimary", "شناسه اصلی Party قبلاً ایجاد شده است؛ شناسه جدید فقط می‌تواند تکمیلی باشد.");
        }
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
        boolean currentPrimary = repository.identifierIsPrimary(partyId, id);
        if (currentPrimary && !"Y".equals(request.isPrimary())) {
            throw validation("isPrimary", "شناسه اصلی از این API قابل تنزل به شناسه تکمیلی نیست.");
        }
        if (!currentPrimary && "Y".equals(request.isPrimary()) && repository.hasPrimaryIdentifier(partyId, id)) {
            throw validation("isPrimary", "برای جایگزینی شناسه اصلی باید فرایند اصلاح هویت جداگانه اجرا شود.");
        }
        if (repository.identifierExists(request.identifierTypeCode(), request.identifierValue(), request.issuerCode(), request.validFrom(), id)) {
            throw validation("identifierValue", "این شناسه با نوع، صادرکننده و تاریخ شروع اعتبار یکسان قبلاً ثبت شده است.");
        }
        if ("Y".equals(request.isPrimary())) repository.clearPrimaryIdentifiers(partyId, id, actor);
        ensureUpdated(repository.updateIdentifier(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteIdentifier(long partyId, long id) {
        find(partyId);
        if (repository.identifierIsPrimary(partyId, id)) {
            throw validation("partyIdentifierId", "شناسه اصلی Party از این عملیات قابل حذف نیست.");
        }
        ensureDeleted(repository.deleteIdentifier(partyId, id));
        return find(partyId);
    }

    @Transactional
    public Party360Response createAddress(long partyId, PartyAddressRequest raw, String actor) {
        find(partyId);
        PartyAddressRequest request = normalizeAddress(raw);
        validateAddress(request);
        if ("Y".equals(request.isPrimary())) repository.clearPrimaryAddresses(partyId, request.addressTypeCode(), null, actor);
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
        if ("Y".equals(request.isPrimary())) repository.clearPrimaryAddresses(partyId, request.addressTypeCode(), id, actor);
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
        if ("Y".equals(request.isPrimary())) repository.clearPrimaryContacts(partyId, request.contactTypeCode(), null, actor);
        repository.insertContact(partyId, request, actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateContact(long partyId, long id, ContactPointRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        ContactPointRequest request = normalizeContact(raw);
        validateContact(request);
        if ("Y".equals(request.isPrimary())) repository.clearPrimaryContacts(partyId, request.contactTypeCode(), id, actor);
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
    public Party360Response createContactAddressAssociation(long partyId, ContactPointAddressRequest raw, String actor) {
        find(partyId);
        ContactPointAddressRequest request = normalizeContactAddressAssociation(raw);
        validateContactAddressAssociation(partyId, request);
        repository.insertContactAddressAssociation(partyId, request, actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateContactAddressAssociation(long partyId, long id, ContactPointAddressRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        ContactPointAddressRequest request = normalizeContactAddressAssociation(raw);
        validateContactAddressAssociation(partyId, request);
        ensureUpdated(repository.updateContactAddressAssociation(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteContactAddressAssociation(long partyId, long id) {
        find(partyId);
        ensureDeleted(repository.deleteContactAddressAssociation(partyId, id));
        return find(partyId);
    }

    @Transactional
    public Party360Response createFinancialProfile(long partyId, FinancialProfileRequest raw, String actor) {
        find(partyId);
        FinancialProfileRequest request = normalizeFinancialProfile(raw);
        validateFinancialProfile(request);
        if (repository.financialProfileExistsForDate(partyId, request.asOfDate(), null)) {
            throw validation("asOfDate", "برای این Party در تاریخ مرجع انتخاب‌شده قبلاً پروفایل مالی ثبت شده است.");
        }
        repository.insertFinancialProfile(partyId, request, actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateFinancialProfile(long partyId, long id, FinancialProfileRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        FinancialProfileRequest request = normalizeFinancialProfile(raw);
        validateFinancialProfile(request);
        if (repository.financialProfileExistsForDate(partyId, request.asOfDate(), id)) {
            throw validation("asOfDate", "برای این Party در تاریخ مرجع انتخاب‌شده قبلاً پروفایل مالی ثبت شده است.");
        }
        ensureUpdated(repository.updateFinancialProfile(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteFinancialProfile(long partyId, long id) {
        find(partyId);
        ensureDeleted(repository.deleteFinancialProfile(partyId, id));
        return find(partyId);
    }

    @Transactional
    public Party360Response createEmployment(long partyId, PartyEmploymentRequest raw, String actor) {
        find(partyId);
        if (!repository.partyIsPerson(partyId)) throw validation("partyId", "سابقه اشتغال فقط برای Party از نوع PERSON قابل ثبت است.");
        PartyEmploymentRequest request = normalizeEmployment(raw);
        validateEmployment(partyId, request);
        repository.insertEmployment(partyId, request, actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateEmployment(long partyId, long id, PartyEmploymentRequest raw, String actor) {
        find(partyId);
        if (!repository.partyIsPerson(partyId)) throw validation("partyId", "سابقه اشتغال فقط برای Party از نوع PERSON قابل ثبت است.");
        requireVersion(raw.recordVersion());
        PartyEmploymentRequest request = normalizeEmployment(raw);
        validateEmployment(partyId, request);
        ensureUpdated(repository.updateEmployment(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteEmployment(long partyId, long id) {
        find(partyId);
        ensureDeleted(repository.deleteEmployment(partyId, id));
        return find(partyId);
    }

    @Transactional
    public Party360Response createIncomeSource(long partyId, PartyIncomeSourceRequest raw, String actor) {
        find(partyId);
        PartyIncomeSourceRequest request = normalizeIncomeSource(raw);
        validateIncomeSource(request);
        repository.insertIncomeSource(partyId, request, actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateIncomeSource(long partyId, long id, PartyIncomeSourceRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        PartyIncomeSourceRequest request = normalizeIncomeSource(raw);
        validateIncomeSource(request);
        ensureUpdated(repository.updateIncomeSource(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteIncomeSource(long partyId, long id) {
        find(partyId);
        ensureDeleted(repository.deleteIncomeSource(partyId, id));
        return find(partyId);
    }

    @Transactional
    public Party360Response createAssetLiability(long partyId, PartyAssetLiabilityRequest raw, String actor) {
        find(partyId);
        PartyAssetLiabilityRequest request = normalizeAssetLiability(raw);
        repository.insertAssetLiability(partyId, request, actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateAssetLiability(long partyId, long id, PartyAssetLiabilityRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        PartyAssetLiabilityRequest request = normalizeAssetLiability(raw);
        ensureUpdated(repository.updateAssetLiability(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteAssetLiability(long partyId, long id) {
        find(partyId);
        ensureDeleted(repository.deleteAssetLiability(partyId, id));
        return find(partyId);
    }

    @Transactional
    public Party360Response createLicense(long partyId, PartyLicenseRequest raw, String actor) {
        find(partyId);
        PartyLicenseRequest request = normalizeLicense(raw);
        validateLicense(partyId, request);
        if (repository.licenseExists(request.licenseTypeCode(), request.licenseNumber(), null)) {
            throw validation("licenseNumber", "این نوع و شماره مجوز قبلاً ثبت شده است.");
        }
        repository.insertLicense(partyId, request, actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateLicense(long partyId, long id, PartyLicenseRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        PartyLicenseRequest request = normalizeLicense(raw);
        validateLicense(partyId, request);
        if (repository.licenseExists(request.licenseTypeCode(), request.licenseNumber(), id)) {
            throw validation("licenseNumber", "این نوع و شماره مجوز قبلاً ثبت شده است.");
        }
        ensureUpdated(repository.updateLicense(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteLicense(long partyId, long id) {
        find(partyId);
        ensureDeleted(repository.deleteLicense(partyId, id));
        return find(partyId);
    }

    @Transactional
    public Party360Response createClassification(long partyId, PartyClassificationRequest raw, String actor) {
        find(partyId);
        PartyClassificationRequest request = normalizeClassification(raw);
        validateClassification(partyId, null, request);
        repository.insertClassification(partyId, request, actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateClassification(long partyId, long id, PartyClassificationRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        PartyClassificationRequest request = normalizeClassification(raw);
        validateClassification(partyId, id, request);
        ensureUpdated(repository.updateClassification(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteClassification(long partyId, long id) {
        find(partyId);
        ensureDeleted(repository.deleteClassification(partyId, id));
        return find(partyId);
    }


    @Transactional
    public Party360Response createRelationship(long partyId, PartyRelationshipRequest raw, String actor) {
        find(partyId);
        PartyRelationshipRequest request = normalizeRelationship(raw);
        validateRelationship(partyId, null, request);
        repository.insertRelationship(partyId, request, actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateRelationship(long partyId, long id, PartyRelationshipRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        PartyRelationshipRequest request = normalizeRelationship(raw);
        validateRelationship(partyId, id, request);
        ensureUpdated(repository.updateRelationship(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteRelationship(long partyId, long id) {
        find(partyId);
        ensureDeleted(repository.deleteRelationship(partyId, id));
        return find(partyId);
    }

    @Transactional
    public Party360Response createBeneficialOwnership(long partyId, BeneficialOwnershipRequest raw, String actor) {
        find(partyId);
        BeneficialOwnershipRequest request = normalizeBeneficialOwnership(raw);
        validateBeneficialOwnership(partyId, null, request);
        repository.insertBeneficialOwnership(partyId, request, actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateBeneficialOwnership(long partyId, long id, BeneficialOwnershipRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        BeneficialOwnershipRequest request = normalizeBeneficialOwnership(raw);
        validateBeneficialOwnership(partyId, id, request);
        ensureUpdated(repository.updateBeneficialOwnership(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteBeneficialOwnership(long partyId, long id) {
        find(partyId);
        ensureDeleted(repository.deleteBeneficialOwnership(partyId, id));
        return find(partyId);
    }

    @Transactional
    public Party360Response createAuthority(long partyId, PartyAuthorityRequest raw, String actor) {
        find(partyId);
        PartyAuthorityRequest request = normalizeAuthority(raw);
        validateAuthority(partyId, null, request);
        repository.insertAuthority(partyId, request, actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateAuthority(long partyId, long id, PartyAuthorityRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        PartyAuthorityRequest request = normalizeAuthority(raw);
        validateAuthority(partyId, id, request);
        ensureUpdated(repository.updateAuthority(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteAuthority(long partyId, long id) {
        find(partyId);
        ensureDeleted(repository.deleteAuthority(partyId, id));
        return find(partyId);
    }

    @Transactional
    public Party360Response createRole(long partyId, PartyRoleRequest raw, String actor) {
        find(partyId);
        PartyRoleRequest request = normalizeRole(raw);
        validateRole(partyId, null, request);
        long roleId = repository.insertRole(partyId, request, actor);
        if ("CUSTOMER".equals(request.roleTypeCode())) {
            repository.insertCustomerForRole(partyId, roleId, request, actor);
        }
        return find(partyId);
    }

    @Transactional
    public Party360Response updateRole(long partyId, long id, PartyRoleRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        PartyRoleRecord existing = repository.findRole(partyId, id)
                .orElseThrow(() -> new CifNotFoundException("نقش Party یافت نشد."));
        PartyRoleRequest request = normalizeRole(raw);
        if (!existing.roleTypeCode().equals(request.roleTypeCode())) {
            throw validation("roleTypeCode", "نوع نقش پس از ایجاد قابل تغییر نیست؛ نقش جدید را جداگانه ثبت کنید.");
        }
        validateRole(partyId, id, request);
        ensureUpdated(repository.updateRole(partyId, id, request, actor));
        if ("CUSTOMER".equals(request.roleTypeCode())) {
            repository.updateCustomerForRole(partyId, id, request, actor);
        }
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteRole(long partyId, long id) {
        find(partyId);
        if (repository.customerForRoleExists(partyId, id)) {
            throw validation("statusCode", "نقش مشتری بانک حذف فیزیکی نمی‌شود؛ وضعیت یا تاریخ پایان آن را ویرایش کنید.");
        }
        ensureDeleted(repository.deleteRole(partyId, id));
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
        if (repository.kycCaseHasDependents(partyId, id)) {
            throw validation("kycCaseId", "پرونده KYC دارای ارزیابی ریسک، نتیجه Screening یا مدرک وابسته است؛ ابتدا وابستگی‌ها را مدیریت کنید.");
        }
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

    @Transactional
    public Party360Response createExternalInquiry(long partyId, ExternalInquiryRequest raw, String actor) {
        find(partyId);
        ExternalInquiryRequest request = normalizeExternalInquiry(raw);
        validateExternalInquiry(request);
        repository.insertExternalInquiry(partyId, request, actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateExternalInquiry(long partyId, long id, ExternalInquiryRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        ExternalInquiryRequest request = normalizeExternalInquiry(raw);
        validateExternalInquiry(request);
        ensureUpdated(repository.updateExternalInquiry(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteExternalInquiry(long partyId, long id) {
        find(partyId);
        ensureDeleted(repository.deleteExternalInquiry(partyId, id));
        return find(partyId);
    }

    @Transactional
    public Party360Response createConsent(long partyId, PartyConsentRequest raw, String actor) {
        find(partyId);
        PartyConsentRequest request = normalizeConsent(raw);
        validateConsent(partyId, null, request);
        repository.insertConsent(partyId, request, derivedConsentStatus(request), actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateConsent(long partyId, long id, PartyConsentRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        PartyConsentRecord existing = repository.findConsent(partyId, id)
                .orElseThrow(() -> new CifNotFoundException("رضایت ثبت‌شده یافت نشد."));
        if ("REVOKED".equals(existing.consentStatusCode())) {
            throw validation("consentStatusCode", "رضایت لغوشده قابل ویرایش نیست؛ رکورد جدید ثبت کنید.");
        }
        PartyConsentRequest request = normalizeConsent(raw);
        if (!existing.consentTypeCode().equals(request.consentTypeCode()) || !existing.purposeCode().equals(request.purposeCode())) {
            throw validation("consentTypeCode", "نوع و هدف رضایت پس از ثبت قابل تغییر نیست؛ برای هدف جدید رکورد جداگانه ثبت کنید.");
        }
        if (existing.customerDecisionCode() != null && !existing.customerDecisionCode().equals(request.customerDecisionCode())) {
            throw validation("customerDecisionCode", "تصمیم مشتری پس از ثبت تغییر نمی‌کند؛ رضایت اعطاشده را لغو و تصمیم جدید را جداگانه ثبت کنید.");
        }
        validateConsent(partyId, id, request);
        ensureUpdated(repository.updateConsent(partyId, id, request, derivedConsentStatus(request), actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response revokeConsent(long partyId, long id, String actor) {
        find(partyId);
        PartyConsentRecord existing = repository.findConsent(partyId, id)
                .orElseThrow(() -> new CifNotFoundException("رضایت ثبت‌شده یافت نشد."));
        if (!"GRANTED".equals(existing.consentStatusCode())) {
            throw validation("consentStatusCode", "فقط رضایت فعال و اعطاشده قابل لغو است.");
        }
        ensureUpdated(repository.revokeConsent(partyId, id, existing.recordVersion(), actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response createCommunicationPreference(long partyId, CommunicationPreferenceRequest raw, String actor) {
        find(partyId);
        CommunicationPreferenceRequest request = normalizeCommunicationPreference(raw);
        validateCommunicationPreference(partyId, null, request);
        repository.insertCommunicationPreference(partyId, request, actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateCommunicationPreference(long partyId, long id, CommunicationPreferenceRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        CommunicationPreferenceRequest request = normalizeCommunicationPreference(raw);
        validateCommunicationPreference(partyId, id, request);
        ensureUpdated(repository.updateCommunicationPreference(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteCommunicationPreference(long partyId, long id) {
        find(partyId);
        ensureDeleted(repository.deleteCommunicationPreference(partyId, id));
        return find(partyId);
    }

    @Transactional
    public Party360Response createGeneralPreference(long partyId, PartyGeneralPreferenceRequest raw, String actor) {
        find(partyId);
        PartyGeneralPreferenceRequest request = normalizeGeneralPreference(raw);
        validateGeneralPreference(partyId, null, request);
        repository.insertGeneralPreference(partyId, request, actor);
        return find(partyId);
    }

    @Transactional
    public Party360Response updateGeneralPreference(long partyId, long id, PartyGeneralPreferenceRequest raw, String actor) {
        find(partyId);
        requireVersion(raw.recordVersion());
        PartyGeneralPreferenceRequest request = normalizeGeneralPreference(raw);
        validateGeneralPreference(partyId, id, request);
        ensureUpdated(repository.updateGeneralPreference(partyId, id, request, actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response deleteGeneralPreference(long partyId, long id) {
        find(partyId);
        ensureDeleted(repository.deleteGeneralPreference(partyId, id));
        return find(partyId);
    }

    @Transactional
    public Party360Response changePartyStatus(long partyId, PartyStatusChangeRequest raw, String actor) {
        Party360Response current = find(partyId);
        requireVersion(raw.partyRecordVersion());
        String lifecycle = upper(raw.lifecycleStatusCode());
        String reason = upper(raw.statusReasonCode());
        Map<String, String> errors = new LinkedHashMap<>();
        if ("MERGED".equals(current.party().lifecycleStatusCode())) {
            errors.put("lifecycleStatusCode", "Party ادغام‌شده از فرایند تغییر وضعیت قابل بازگشت نیست.");
        }
        if ("MERGED".equals(lifecycle)) {
            errors.put("lifecycleStatusCode", "وضعیت MERGED فقط از فرایند اختصاصی ادغام Party قابل ثبت است.");
        }
        if (lifecycle.equals(current.party().lifecycleStatusCode()) && reason.equals(defaultText(current.party().statusReasonCode(), ""))) {
            errors.put("lifecycleStatusCode", "وضعیت و دلیل جدید با وضعیت جاری یکسان است.");
        }
        LocalDate effectiveDate = raw.effectiveDate();
        if (effectiveDate.isAfter(LocalDate.now())) errors.put("effectiveDate", "در این نسخه تغییر وضعیت آینده‌دار پشتیبانی نمی‌شود.");
        if (current.party().validFrom() != null && effectiveDate.isBefore(current.party().validFrom())) {
            errors.put("effectiveDate", "تاریخ اثر نمی‌تواند قبل از شروع اعتبار Party باشد.");
        }
        if (!repository.activeReferenceCodeExists("REF_PARTY_LIFECYCLE_STATUS", "LIFECYCLE_STATUS_CODE", lifecycle)) {
            errors.put("lifecycleStatusCode", "وضعیت چرخه عمر در داده مرجع فعال یافت نشد.");
        }
        if (!repository.activeStatusReasonExists(reason)) {
            errors.put("statusReasonCode", "دلیل وضعیت در داده مرجع فعال یافت نشد.");
        }
        var open = current.statusHistory().stream().filter(x -> x.validTo() == null).findFirst().orElse(null);
        if (open != null && effectiveDate.isBefore(open.validFrom())) {
            errors.put("effectiveDate", "تاریخ اثر نمی‌تواند قبل از شروع وضعیت جاری باشد.");
        }
        reject(errors);

        if (current.statusHistory().isEmpty() && current.party().validFrom() != null && effectiveDate.isAfter(current.party().validFrom())) {
            String initialReason = blank(current.party().statusReasonCode()) ? "NEW_REGISTRATION" : current.party().statusReasonCode();
            if (!repository.activeStatusReasonExists(initialReason)) initialReason = reason;
            repository.insertStatusHistory(partyId, current.party().lifecycleStatusCode(), initialReason,
                    current.party().validFrom(), effectiveDate, "بازسازی وضعیت جاری پیش از اولین تغییر ثبت‌شده", actor);
        } else {
            repository.closeOpenStatusHistory(partyId, effectiveDate, actor);
        }
        repository.insertStatusHistory(partyId, lifecycle, reason, effectiveDate, null, trimToNull(raw.descriptionText()), actor);
        ensureUpdated(repository.updatePartyStatus(partyId, lifecycle, reason, raw.partyRecordVersion(), actor));
        return find(partyId);
    }

    @Transactional
    public Party360Response mergeParty(long sourcePartyId, PartyMergeRequest raw, String actor) {
        Party360Response source = find(sourcePartyId);
        Party360Response target = find(raw.targetPartyId());
        requireVersion(raw.partyRecordVersion());
        Map<String, String> errors = new LinkedHashMap<>();
        if (sourcePartyId == raw.targetPartyId()) errors.put("targetPartyId", "Party مبدأ و مقصد ادغام نمی‌توانند یکسان باشند.");
        if ("MERGED".equals(source.party().lifecycleStatusCode())) errors.put("partyId", "Party مبدأ قبلاً ادغام شده است.");
        if ("MERGED".equals(target.party().lifecycleStatusCode())) errors.put("targetPartyId", "Party مقصد نباید خودش ادغام‌شده باشد.");
        if (!source.party().partyTypeCode().equals(target.party().partyTypeCode())) errors.put("targetPartyId", "ادغام فقط بین دو Party از یک نوع مجاز است.");
        if (!"Y".equalsIgnoreCase(source.party().isCurrent())) errors.put("partyId", "Party مبدأ باید رکورد جاری باشد.");
        if (!"Y".equalsIgnoreCase(target.party().isCurrent())) errors.put("targetPartyId", "Party مقصد باید رکورد جاری باشد.");
        String mergeReason = raw.mergeReasonCode().trim();
        String conflictResolution = trimToNull(raw.conflictResolutionCode());
        if (!MERGE_REASONS.contains(mergeReason)) errors.put("mergeReasonCode", "دلیل ادغام باید یکی از گزینه‌های فرم عملیاتی مبنا باشد.");
        if (conflictResolution != null && !MERGE_CONFLICT_RESOLUTIONS.contains(conflictResolution)) errors.put("conflictResolutionCode", "روش رفع تعارض باید یکی از گزینه‌های فرم عملیاتی مبنا باشد.");
        if (!repository.activeReferenceCodeExists("REF_PARTY_LIFECYCLE_STATUS", "LIFECYCLE_STATUS_CODE", "MERGED")) {
            errors.put("lifecycleStatusCode", "کد MERGED در داده مرجع چرخه عمر فعال نیست.");
        }
        if (!repository.activeStatusReasonExists("DUPLICATE_MERGED")) {
            errors.put("statusReasonCode", "کد DUPLICATE_MERGED در داده مرجع دلیل وضعیت فعال نیست.");
        }
        reject(errors);

        LocalDate effectiveDate = LocalDate.now();
        if (source.statusHistory().isEmpty() && source.party().validFrom() != null && effectiveDate.isAfter(source.party().validFrom())) {
            String initialReason = blank(source.party().statusReasonCode()) ? "NEW_REGISTRATION" : source.party().statusReasonCode();
            if (!repository.activeStatusReasonExists(initialReason)) initialReason = "DUPLICATE_MERGED";
            repository.insertStatusHistory(sourcePartyId, source.party().lifecycleStatusCode(), initialReason,
                    source.party().validFrom(), effectiveDate, "بازسازی وضعیت جاری پیش از ادغام", actor);
        } else {
            repository.closeOpenStatusHistory(sourcePartyId, effectiveDate, actor);
        }
        repository.insertMergeHistory(sourcePartyId, raw.targetPartyId(), mergeReason, conflictResolution, actor);
        // The supplied operational form explicitly requires valid names, identifiers and classifications
        // to follow the canonical target. Historical/expired rows remain on the merged source for audit.
        repository.transferValidNames(sourcePartyId, raw.targetPartyId(), actor);
        repository.transferValidIdentifiers(sourcePartyId, raw.targetPartyId(), actor);
        repository.transferValidClassifications(sourcePartyId, raw.targetPartyId(), actor);
        repository.insertStatusHistory(sourcePartyId, "MERGED", "DUPLICATE_MERGED", effectiveDate, null,
                "ادغام در Party مقصد " + raw.targetPartyId(), actor);
        ensureUpdated(repository.markPartyMerged(sourcePartyId, raw.targetPartyId(), "DUPLICATE_MERGED", raw.partyRecordVersion(), actor));
        return find(sourcePartyId);
    }

    private static PersonRequest defaultPersonRequest() {
        return new PersonRequest(
                null, null, null, null, null, null, null, null, null,
                "FULL", null, "INCOMPLETE", "UNVERIFIED", null, null, "ALIVE", null, null
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
                upper(raw.countryCode()), trimToNull(raw.provinceCode()), trimToNull(raw.countyCode()),
                trimToNull(raw.cityCode()), trimToNull(raw.districtCode()), trimToNull(raw.postalCode()),
                raw.addressLine1().trim(), trimToNull(raw.addressLine2()), trimToNull(raw.neighborhoodText()),
                trimToNull(raw.mainStreetText()), trimToNull(raw.sideStreetText()), trimToNull(raw.plaqueNo()),
                trimToNull(raw.floorNo()), trimToNull(raw.unitNo()), trimToNull(raw.addressDetail()),
                upperOrNull(raw.tenureTypeCode()), upperOrNull(raw.verificationStatusCode()),
                upperOrNull(raw.sourceCode()), raw.partyAddressRecordVersion(), raw.addressRecordVersion()
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
        String verified = upper(raw.isVerified());
        return new ContactPointRequest(
                upper(raw.contactTypeCode()), value,
                blank(raw.normalizedValue()) ? normalizeIdentifierValue(value) : raw.normalizedValue().trim(),
                upper(raw.purposeCode()), upper(raw.isPrimary()), verified, raw.verifiedAt(),
                raw.validFrom() == null ? LocalDate.now() : raw.validFrom(), raw.validTo(),
                trimToNull(raw.countryDialCode()), trimToNull(raw.areaCode()), trimToNull(raw.extensionNo()),
                upperOrNull(raw.ownerTypeCode()),
                blank(raw.verificationStatusCode()) ? ("Y".equals(verified) ? "VERIFIED" : "UNVERIFIED") : upper(raw.verificationStatusCode()),
                upperOrNull(raw.verificationMethodCode()), raw.recordVersion()
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

    private static ContactPointAddressRequest normalizeContactAddressAssociation(ContactPointAddressRequest raw) {
        return new ContactPointAddressRequest(
                raw.contactPointId(), raw.partyAddressId(), upper(raw.associationTypeCode()),
                upper(raw.isPrimaryForAddress()), raw.validFrom() == null ? LocalDate.now() : raw.validFrom(),
                raw.validTo(), raw.recordVersion()
        );
    }

    private void validateContactAddressAssociation(long partyId, ContactPointAddressRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        checkFlag("isPrimaryForAddress", r.isPrimaryForAddress(), errors);
        checkDateOrder("validFrom", r.validFrom(), "validTo", r.validTo(), errors);
        if (!repository.contactBelongsToParty(partyId, r.contactPointId())) {
            errors.put("contactPointId", "راه تماس انتخاب‌شده متعلق به این Party نیست.");
        }
        if (!repository.partyAddressBelongsToParty(partyId, r.partyAddressId())) {
            errors.put("partyAddressId", "نشانی انتخاب‌شده متعلق به این Party نیست.");
        }
        reject(errors);
    }

    private static FinancialProfileRequest normalizeFinancialProfile(FinancialProfileRequest raw) {
        return new FinancialProfileRequest(
                raw.asOfDate(), raw.annualIncome(), raw.totalAssets(), raw.totalLiabilities(), upper(raw.currencyCode()),
                upperOrNull(raw.sourceOfFundsCode()), upperOrNull(raw.sourceOfWealthCode()), raw.expectedMonthlyTurnover(),
                upperOrNull(raw.taxStatusCode()), upper(raw.verificationStatusCode()), raw.netMonthlyIncome(),
                raw.otherMonthlyIncome(), raw.expectedMonthlyTxnCount(), trimToNull(raw.fundsCountriesText()),
                upperOrNull(raw.financialRelationPurposeCode()), raw.realEstateValue(), raw.investmentValue(),
                raw.totalMonthlyInstallment(), raw.estimatedNetWorth(), upperOrNull(raw.financialCapacityCode()), raw.recordVersion()
        );
    }

    private static void validateFinancialProfile(FinancialProfileRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (r.expectedMonthlyTxnCount() != null && r.expectedMonthlyTxnCount() < 0) {
            errors.put("expectedMonthlyTxnCount", "تعداد تراکنش ماهانه نمی‌تواند منفی باشد.");
        }
        reject(errors);
    }

    private static PartyEmploymentRequest normalizeEmployment(PartyEmploymentRequest raw) {
        String status = upper(raw.jobStatus());
        return new PartyEmploymentRequest(
                raw.employerPartyId(), trimToNull(raw.employerName()), upper(raw.occupationCode()), trimToNull(raw.jobTitle()),
                upperOrNull(raw.economicSectorCode()), upperOrNull(raw.isicCode()), raw.monthlyIncome(),
                upperOrNull(raw.incomeCurrencyCode()), trimToNull(raw.familyRange()), status, trimToNull(raw.employeeRange()),
                raw.validFrom(), raw.validTo(), blank(raw.employmentStatusCode()) ? status : upper(raw.employmentStatusCode()),
                upperOrNull(raw.occupationGroupCode()), trimToNull(raw.employerIdentifier()), upperOrNull(raw.contractTypeCode()),
                trimToNull(raw.insuranceNo()), trimToNull(raw.taxCode()), raw.recordVersion()
        );
    }

    private void validateEmployment(long partyId, PartyEmploymentRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        checkDateOrder("validFrom", r.validFrom(), "validTo", r.validTo(), errors);
        boolean hasEmployerParty = r.employerPartyId() != null;
        boolean hasEmployerName = !blank(r.employerName());
        if (hasEmployerParty == hasEmployerName) {
            errors.put("employerPartyId", "دقیقاً یکی از Party کارفرما یا نام کارفرما باید ثبت شود.");
        }
        if (r.employerPartyId() != null && !repository.partyExists(r.employerPartyId())) {
            errors.put("employerPartyId", "Party کارفرما در CIF یافت نشد.");
        }
        if (r.employerPartyId() != null && r.employerPartyId() == partyId) {
            errors.put("employerPartyId", "Party نمی‌تواند کارفرمای خودش باشد.");
        }
        reject(errors);
    }

    private static PartyIncomeSourceRequest normalizeIncomeSource(PartyIncomeSourceRequest raw) {
        return new PartyIncomeSourceRequest(upper(raw.sourceTypeCode()), raw.monthlyAmount(), upper(raw.currencyCode()),
                upper(raw.documentedFlag()), upper(raw.statusCode()), raw.recordVersion());
    }

    private static void validateIncomeSource(PartyIncomeSourceRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        checkFlag("documentedFlag", r.documentedFlag(), errors);
        reject(errors);
    }

    private static PartyAssetLiabilityRequest normalizeAssetLiability(PartyAssetLiabilityRequest raw) {
        return new PartyAssetLiabilityRequest(upper(raw.itemTypeCode()), trimToNull(raw.descriptionText()), raw.amount(),
                upper(raw.currencyCode()), raw.assessmentDate(), upper(raw.statusCode()), raw.recordVersion());
    }

    private static PartyLicenseRequest normalizeLicense(PartyLicenseRequest raw) {
        return new PartyLicenseRequest(upper(raw.licenseTypeCode()), raw.licenseNumber().trim(), raw.issuerPartyId(),
                trimToNull(raw.issuerName()), raw.issueDate(), raw.expiryDate(), upper(raw.statusCode()),
                trimToNull(raw.documentRef()), raw.recordVersion());
    }

    private void validateLicense(long partyId, PartyLicenseRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        checkDateOrder("issueDate", r.issueDate(), "expiryDate", r.expiryDate(), errors);
        if (r.issuerPartyId() != null && !repository.partyExists(r.issuerPartyId())) {
            errors.put("issuerPartyId", "Party مرجع صادرکننده در CIF یافت نشد.");
        }
        reject(errors);
    }

    private static PartyClassificationRequest normalizeClassification(PartyClassificationRequest raw) {
        return new PartyClassificationRequest(
                upper(raw.classificationTypeCode()), upper(raw.classificationValueCode()), upper(raw.assignmentReasonCode()),
                raw.validFrom(), raw.validTo(), trimToNull(raw.descriptionText()), raw.recordVersion()
        );
    }

    private void validateClassification(long partyId, Long exceptId, PartyClassificationRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        checkDateOrder("validFrom", r.validFrom(), "validTo", r.validTo(), errors);
        if (!repository.classificationReferenceExists(r.classificationTypeCode(), r.classificationValueCode(), r.assignmentReasonCode())) {
            errors.put("classificationValueCode", "نوع، مقدار یا علت تخصیص با داده مرجع فعال CIF سازگار نیست.");
        }
        if (repository.classificationDuplicateExists(partyId, r.classificationTypeCode(), r.classificationValueCode(), r.validFrom(), exceptId)) {
            errors.put("validFrom", "برای این Party همین نوع و مقدار طبقه‌بندی در تاریخ شروع انتخاب‌شده قبلاً ثبت شده است.");
        }
        reject(errors);
    }


    private static PartyRelationshipRequest normalizeRelationship(PartyRelationshipRequest raw) {
        return new PartyRelationshipRequest(
                raw.relatedPartyId(), upper(raw.relationshipTypeCode()), raw.ownershipPercent(),
                trimToNull(raw.positionTitle()), upperOrNull(raw.signingRightCode()), raw.authorityLimitAmount(),
                raw.startDate(), raw.endDate(), raw.evidenceDocumentId(), upperOrNull(raw.verificationStatusCode()), raw.recordVersion()
        );
    }

    private void validateRelationship(long partyId, Long exceptId, PartyRelationshipRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        checkDateOrder("startDate", r.startDate(), "endDate", r.endDate(), errors);
        if (!RELATIONSHIP_TYPES.contains(r.relationshipTypeCode())) {
            errors.put("relationshipTypeCode", "نوع رابطه انتخاب‌شده در دامنه عملیاتی فعلی پشتیبانی نمی‌شود.");
        }
        if (r.relatedPartyId() == null) {
            errors.put("relatedPartyId", "Party مرتبط الزامی است.");
        } else if (r.relatedPartyId() == partyId) {
            errors.put("relatedPartyId", "ثبت رابطه یک Party با خودش مجاز نیست.");
        } else if (!repository.partyExists(r.relatedPartyId())) {
            errors.put("relatedPartyId", "Party مرتبط در CIF یافت نشد.");
        } else {
            if (FAMILY_RELATIONSHIP_TYPES.contains(r.relationshipTypeCode())
                    && (!repository.partyIsPerson(partyId) || !repository.partyIsPerson(r.relatedPartyId()))) {
                errors.put("relationshipTypeCode", "رابطه همسر/والد/فرزند فقط میان دو Party از نوع شخص حقیقی مجاز است.");
            }
            if (ORGANIZATION_RELATIONSHIP_TYPES.contains(r.relationshipTypeCode())
                    && (!repository.partyIsOrganization(partyId) || !repository.partyIsOrganization(r.relatedPartyId()))) {
                errors.put("relationshipTypeCode", "رابطه شرکت مادر/وابسته فقط میان دو Party از نوع شخص حقوقی مجاز است.");
            }
        }
        if ("BENEFICIAL_OWNER".equals(r.relationshipTypeCode())
                && (r.ownershipPercent() == null || r.ownershipPercent().signum() <= 0)) {
            errors.put("ownershipPercent", "برای رابطه مالک واقعی، درصد مالکیت/کنترل باید بیشتر از صفر باشد.");
        }
        if (!repository.documentBelongsToParty(partyId, r.evidenceDocumentId())) {
            errors.put("evidenceDocumentId", "مدرک انتخاب‌شده متعلق به Party جاری نیست.");
        }
        if (r.relatedPartyId() != null && r.startDate() != null
                && repository.relationshipDuplicateExists(partyId, r.relatedPartyId(), r.relationshipTypeCode(), r.startDate(), exceptId)) {
            errors.put("startDate", "همین نوع رابطه با Party انتخاب‌شده در تاریخ شروع موردنظر قبلاً ثبت شده است.");
        }
        reject(errors);
    }

    private static PartyRoleRequest normalizeRole(PartyRoleRequest raw) {
        return new PartyRoleRequest(
                upper(raw.roleTypeCode()), upperOrNull(raw.contextTypeCode()), trimToNull(raw.contextId()),
                raw.validFrom() == null ? LocalDate.now() : raw.validFrom(), raw.validTo(), upper(raw.statusCode()),
                raw.principalPartyId(), upperOrNull(raw.relationshipTypeCode()), upperOrNull(raw.authorityBasisCode()),
                trimToNull(raw.authorityDocumentNo()), trimToNull(raw.authorityIssuer()), trimToNull(raw.authorityScopeText()),
                trimToNull(raw.assignmentReasonText()), trimToNull(raw.descriptionText()), raw.recordVersion()
        );
    }

    private void validateRole(long partyId, Long exceptId, PartyRoleRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        checkDateOrder("validFrom", r.validFrom(), "validTo", r.validTo(), errors);
        if (!repository.roleReferenceExists(r.roleTypeCode(), r.contextTypeCode(), r.statusCode())) {
            errors.put("roleTypeCode", "نوع نقش، نوع زمینه یا وضعیت نقش با داده مرجع فعال CIF سازگار نیست.");
        }
        if ((r.contextTypeCode() == null) != (r.contextId() == null)) {
            errors.put("contextId", "نوع زمینه و شناسه زمینه باید همزمان ثبت یا خالی شوند.");
        }
        if (r.principalPartyId() != null) {
            if (r.principalPartyId() == partyId) errors.put("principalPartyId", "Party نمی‌تواند اصیل مرتبط خودش باشد.");
            else if (!repository.partyExists(r.principalPartyId())) errors.put("principalPartyId", "Party مرتبط/اصیل در CIF یافت نشد.");
        }
        if (ROLES_REQUIRING_PRINCIPAL.contains(r.roleTypeCode()) && r.principalPartyId() == null) {
            errors.put("principalPartyId", "برای این نوع نقش، Party مرتبط/اصیل الزامی است.");
        }
        if (AUTHORITY_DOCUMENT_ROLES.contains(r.roleTypeCode()) && blank(r.authorityDocumentNo())) {
            errors.put("authorityDocumentNo", "برای نقش مبتنی بر اختیار، شماره و نوع سند اختیار الزامی است.");
        }
        if ("CUSTOMER".equals(r.roleTypeCode())) {
            if (r.principalPartyId() != null) errors.put("principalPartyId", "نقش مشتری بانک رابطه مستقیم با بانک است و Party اصیل ندارد.");
            if (isCurrentCustomerRequest(r)) {
                boolean anotherCurrent = exceptId == null
                        ? repository.currentCustomerExists(partyId)
                        : repository.currentCustomerExistsForOtherRole(partyId, exceptId);
                if (anotherCurrent) errors.put("roleTypeCode", "برای این Party قبلاً رابطه جاری مشتری بانک وجود دارد.");
            }
        }
        if (repository.roleDuplicateExists(partyId, r.roleTypeCode(), r.contextTypeCode(), r.contextId(), r.validFrom(), exceptId)) {
            errors.put("validFrom", "این نقش با همین زمینه و تاریخ شروع قبلاً برای Party ثبت شده است.");
        }
        reject(errors);
    }


    private static boolean isCurrentCustomerRequest(PartyRoleRequest r) {
        LocalDate today = LocalDate.now();
        if (Set.of("CLOSED", "REVOKED", "EXPIRED", "INACTIVE").contains(r.statusCode())) return false;
        if (r.validFrom() != null && r.validFrom().isAfter(today)) return false;
        return r.validTo() == null || !r.validTo().isBefore(today);
    }

    private static BeneficialOwnershipRequest normalizeBeneficialOwnership(BeneficialOwnershipRequest raw) {
        return new BeneficialOwnershipRequest(
                raw.beneficialOwnerPartyId(), raw.directOwnershipPercent(), raw.indirectOwnershipPercent(), raw.controlPercent(),
                upperOrNull(raw.controlBasisCode()), upper(raw.isUltimateOwner()), trimToNull(raw.ownershipPath()),
                raw.validFrom(), raw.validTo(), trimToNull(raw.evidenceRef()), raw.recordVersion()
        );
    }

    private void validateBeneficialOwnership(long partyId, Long exceptId, BeneficialOwnershipRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!repository.partyIsOrganization(partyId)) errors.put("beneficialOwnerPartyId", "ثبت مالک ذی‌نفع فقط برای Party از نوع شخص حقوقی مجاز است.");
        if (r.beneficialOwnerPartyId() == null) errors.put("beneficialOwnerPartyId", "Party مالک یا کنترل‌کننده الزامی است.");
        else if (r.beneficialOwnerPartyId() == partyId) errors.put("beneficialOwnerPartyId", "شخص حقوقی نمی‌تواند مالک ذی‌نفع خودش باشد.");
        else if (!repository.partyExists(r.beneficialOwnerPartyId())) errors.put("beneficialOwnerPartyId", "Party مالک یا کنترل‌کننده در CIF یافت نشد.");
        if (r.directOwnershipPercent() == null && r.indirectOwnershipPercent() == null && r.controlPercent() == null) {
            errors.put("directOwnershipPercent", "حداقل یکی از درصد مالکیت مستقیم، غیرمستقیم یا کنترل باید وارد شود.");
        }
        checkFlag("isUltimateOwner", r.isUltimateOwner(), errors);
        checkDateOrder("validFrom", r.validFrom(), "validTo", r.validTo(), errors);
        if (r.beneficialOwnerPartyId() != null && r.validFrom() != null
                && repository.beneficialOwnershipDuplicateExists(partyId, r.beneficialOwnerPartyId(), r.validFrom(), exceptId)) {
            errors.put("validFrom", "برای این مالک ذی‌نفع در تاریخ شروع انتخاب‌شده قبلاً رکورد ثبت شده است.");
        }
        reject(errors);
    }

    private static PartyAuthorityRequest normalizeAuthority(PartyAuthorityRequest raw) {
        return new PartyAuthorityRequest(
                raw.authorizedPartyId(), upper(raw.authorityTypeCode()), upper(raw.scopeCode()), raw.maxAmount(),
                upperOrNull(raw.currencyCode()), raw.validFrom(), raw.validTo(), raw.documentRef().trim(), raw.recordVersion()
        );
    }

    private void validateAuthority(long partyId, Long exceptId, PartyAuthorityRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (r.authorizedPartyId() == null) errors.put("authorizedPartyId", "Party دارنده اختیار الزامی است.");
        else if (r.authorizedPartyId() == partyId) errors.put("authorizedPartyId", "اعطاکننده و دارنده اختیار نمی‌توانند یک Party باشند.");
        else if (!repository.partyExists(r.authorizedPartyId())) errors.put("authorizedPartyId", "Party دارنده اختیار در CIF یافت نشد.");
        if ((r.maxAmount() == null) != (r.currencyCode() == null)) {
            errors.put("maxAmount", "سقف مبلغ و کد ارز باید همزمان ثبت یا هر دو خالی باشند.");
        }
        checkDateOrder("validFrom", r.validFrom(), "validTo", r.validTo(), errors);
        if (r.authorizedPartyId() != null && r.validFrom() != null
                && repository.authorityDuplicateExists(partyId, r.authorizedPartyId(), r.authorityTypeCode(), r.scopeCode(), r.validFrom(), exceptId)) {
            errors.put("validFrom", "همین اختیار برای Party انتخاب‌شده، نوع، دامنه و تاریخ شروع قبلاً ثبت شده است.");
        }
        reject(errors);
    }

    private static KycCaseRequest normalizeKyc(KycCaseRequest raw) {
        return new KycCaseRequest(
                upper(raw.kycTypeCode()), upper(raw.dueDiligenceLevelCode()), upper(raw.statusCode()),
                raw.openedAt() == null ? LocalDateTime.now() : raw.openedAt(), raw.completedAt(), raw.reviewedAt(),
                raw.nextReviewDate(), upperOrNull(raw.finalRiskLevelCode()), upperOrNull(raw.decisionCode()),
                trimToNull(raw.decisionReason()), trimToNull(raw.approvedBy()), upperOrNull(raw.relationPurposeCode()),
                upperOrNull(raw.expectedActivityLevelCode()), upperOrNull(raw.geographicScopeCode()),
                trimToNull(raw.activityCountriesText()), trimToNull(raw.requestedProductsText()),
                upperOrNull(raw.preferredServiceChannelCode()), upperOrNull(raw.pepStatusCode()),
                upperOrNull(raw.highRiskCountryFlag()), upperOrNull(raw.eddRequiredFlag()), raw.recordVersion()
        );
    }

    private static void validateKyc(KycCaseRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!DUE_DILIGENCE.contains(r.dueDiligenceLevelCode())) errors.put("dueDiligenceLevelCode", "سطح بررسی باید SDD، CDD یا EDD باشد.");
        checkDateTimeOrder("openedAt", r.openedAt(), "completedAt", r.completedAt(), errors);
        checkDateTimeOrder("openedAt", r.openedAt(), "reviewedAt", r.reviewedAt(), errors);
        if (r.highRiskCountryFlag() != null) checkFlag("highRiskCountryFlag", r.highRiskCountryFlag(), errors);
        if (r.eddRequiredFlag() != null) checkFlag("eddRequiredFlag", r.eddRequiredFlag(), errors);
        reject(errors);
    }

    private PartyDocumentRequest normalizeDocument(PartyDocumentRequest raw) {
        return new PartyDocumentRequest(
                raw.kycCaseId(), upper(raw.documentTypeCode()), raw.documentNumber().trim(), trimToNull(raw.issuerCode()),
                raw.issueDate(), raw.expiryDate(), upper(raw.verificationStatusCode()), raw.verifiedAt(),
                raw.contentHash().trim(), raw.storageRef().trim(), raw.mimeType().trim(),
                trimToNull(raw.issuingAuthorityText()), upperOrNull(raw.controlStatusCode()),
                trimToNull(raw.descriptionText()), raw.recordVersion()
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

    private static ExternalInquiryRequest normalizeExternalInquiry(ExternalInquiryRequest raw) {
        return new ExternalInquiryRequest(
                upper(raw.inquiryTypeCode()), upper(raw.providerCode()), raw.referenceNo().trim(),
                upperOrNull(raw.inquiryResultCode()), raw.requestedAt() == null ? LocalDateTime.now() : raw.requestedAt(),
                raw.respondedAt(), raw.expiryAt(), trimToNull(raw.payloadRef()), trimToNull(raw.payloadHash()),
                raw.recordVersion()
        );
    }

    private static void validateExternalInquiry(ExternalInquiryRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        if ((r.respondedAt() == null) != (r.inquiryResultCode() == null)) {
            errors.put("respondedAt", "زمان پاسخ و نتیجه استعلام باید همزمان ثبت یا هر دو خالی باشند.");
        }
        if (r.respondedAt() != null && r.respondedAt().isBefore(r.requestedAt())) {
            errors.put("respondedAt", "زمان پاسخ نمی‌تواند قبل از زمان درخواست باشد.");
        }
        if (r.expiryAt() != null && (r.respondedAt() == null || r.expiryAt().isBefore(r.respondedAt()))) {
            errors.put("expiryAt", "پایان اعتبار فقط پس از دریافت پاسخ و در زمانی مساوی یا بعد از پاسخ قابل ثبت است.");
        }
        if ((r.payloadRef() == null) != (r.payloadHash() == null)) {
            errors.put("payloadRef", "مرجع Payload و Hash باید همزمان ثبت یا هر دو خالی باشند.");
        }
        reject(errors);
    }

    private static PartyConsentRequest normalizeConsent(PartyConsentRequest raw) {
        return new PartyConsentRequest(
                upper(raw.consentTypeCode()), upper(raw.purposeCode()), upper(raw.customerDecisionCode()),
                upper(raw.captureChannelCode()), raw.declaredAt(), raw.validTo(), raw.consentTextVersionCode().trim(),
                trimToNull(raw.scopeText()), trimToNull(raw.scopeLimitationText()), trimToNull(raw.evidenceRef()),
                upper(raw.sourceCode()), raw.recordVersion()
        );
    }

    private void validateConsent(long partyId, Long exceptId, PartyConsentRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!Set.of("GRANT", "REJECT").contains(r.customerDecisionCode())) {
            errors.put("customerDecisionCode", "تصمیم مشتری باید GRANT یا REJECT باشد.");
        }
        if (r.declaredAt().isAfter(LocalDateTime.now().plusMinutes(1))) errors.put("declaredAt", "زمان اعلام رضایت نمی‌تواند در آینده باشد.");
        if (r.validTo() != null && r.validTo().isBefore(r.declaredAt().toLocalDate())) errors.put("validTo", "پایان اعتبار نمی‌تواند قبل از تاریخ اعلام باشد.");
        if (!repository.activeReferenceCodeExists("REF_PARTY_CONSENT_TYPE", "CONSENT_TYPE_CODE", r.consentTypeCode())) errors.put("consentTypeCode", "نوع رضایت در داده مرجع فعال یافت نشد.");
        if (!repository.activeReferenceCodeExists("REF_PARTY_CONSENT_PURPOSE", "PARTY_CONSENT_PURPOSE_CODE", r.purposeCode())) errors.put("purposeCode", "هدف رضایت در داده مرجع فعال یافت نشد.");
        if (!repository.activeReferenceCodeExists("REF_SOURCE_SYSTEM", "SOURCE_SYSTEM_CODE", r.sourceCode())) errors.put("sourceCode", "منبع ثبت در داده مرجع فعال یافت نشد.");
        if ("GRANT".equals(r.customerDecisionCode()) && repository.consentDuplicateExists(partyId, r.consentTypeCode(), r.purposeCode(), exceptId)) {
            errors.put("purposeCode", "برای این نوع و هدف، یک رضایت فعال دیگر وجود دارد؛ ابتدا آن را لغو یا منقضی کنید.");
        }
        if (("MARKETING".equals(r.consentTypeCode()) || "THIRD_PARTY_SHARING".equals(r.consentTypeCode())) && blank(r.scopeText())) {
            errors.put("scopeText", "برای رضایت بازاریابی یا اشتراک‌گذاری، حداقل یک دامنه باید ثبت شود.");
        }
        reject(errors);
    }

    private static String derivedConsentStatus(PartyConsentRequest r) {
        if ("REJECT".equals(r.customerDecisionCode())) return "PENDING";
        if (r.validTo() != null && r.validTo().isBefore(LocalDate.now())) return "EXPIRED";
        return "GRANTED";
    }

    private static CommunicationPreferenceRequest normalizeCommunicationPreference(CommunicationPreferenceRequest raw) {
        String optOut = blank(raw.marketingOptOutFlag()) ? "N" : upper(raw.marketingOptOutFlag());
        String allowed = upper(raw.allowedFlag());
        if ("Y".equals(optOut)) allowed = "N";
        return new CommunicationPreferenceRequest(
                upper(raw.channelCode()), upper(raw.purposeCode()), allowed, trimToNull(raw.preferredTimeFrom()),
                trimToNull(raw.preferredTimeTo()), upperOrNull(raw.languageCode()), upperOrNull(raw.allowedDaysCode()),
                trimToNull(raw.timeZoneCode()), optOut, raw.recordVersion()
        );
    }

    private void validateCommunicationPreference(long partyId, Long exceptId, CommunicationPreferenceRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        checkFlag("allowedFlag", r.allowedFlag(), errors);
        checkFlag("marketingOptOutFlag", r.marketingOptOutFlag(), errors);
        if (r.preferredTimeFrom() != null && !r.preferredTimeFrom().matches("(?:[01]\\d|2[0-3]):[0-5]\\d")) errors.put("preferredTimeFrom", "زمان شروع باید HH:mm باشد.");
        if (r.preferredTimeTo() != null && !r.preferredTimeTo().matches("(?:[01]\\d|2[0-3]):[0-5]\\d")) errors.put("preferredTimeTo", "زمان پایان باید HH:mm باشد.");
        if (r.preferredTimeFrom() != null && r.preferredTimeTo() != null && r.preferredTimeTo().compareTo(r.preferredTimeFrom()) <= 0) errors.put("preferredTimeTo", "زمان پایان باید بعد از زمان شروع باشد.");
        if (!repository.activeReferenceCodeExists("REF_CHANNEL", "CHANNEL_CODE", r.channelCode())) errors.put("channelCode", "کانال ارتباطی در داده مرجع فعال یافت نشد.");
        if (!repository.activeReferenceCodeExists("REF_COMMUNICATION_PURPOSE", "COMMUNICATION_PURPOSE_CODE", r.purposeCode())) errors.put("purposeCode", "هدف ارتباط در داده مرجع فعال یافت نشد.");
        if (repository.communicationPreferenceDuplicateExists(partyId, r.channelCode(), r.purposeCode(), exceptId)) errors.put("purposeCode", "ترجیح همین کانال و هدف قبلاً ثبت شده است.");
        reject(errors);
    }

    private static PartyGeneralPreferenceRequest normalizeGeneralPreference(PartyGeneralPreferenceRequest raw) {
        return new PartyGeneralPreferenceRequest(
                upper(raw.preferenceTypeCode()), raw.preferenceValue().trim(), raw.validFrom(), raw.validTo(),
                upper(raw.sourceCode()), raw.recordVersion()
        );
    }

    private void validateGeneralPreference(long partyId, Long exceptId, PartyGeneralPreferenceRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        checkDateTimeOrder("validFrom", r.validFrom(), "validTo", r.validTo(), errors);
        if (!repository.activeReferenceCodeExists("REF_PREFERENCE_TYPE", "PREFERENCE_TYPE_CODE", r.preferenceTypeCode())) errors.put("preferenceTypeCode", "نوع ترجیح در داده مرجع فعال یافت نشد.");
        if (!repository.activeReferenceCodeExists("REF_SOURCE_SYSTEM", "SOURCE_SYSTEM_CODE", r.sourceCode())) errors.put("sourceCode", "منبع ترجیح در داده مرجع فعال یافت نشد.");
        if (repository.generalPreferenceOverlapExists(partyId, r.preferenceTypeCode(), r.validFrom(), r.validTo(), exceptId)) errors.put("validFrom", "برای این نوع ترجیح یک بازه زمانی همپوشان وجود دارد.");
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
