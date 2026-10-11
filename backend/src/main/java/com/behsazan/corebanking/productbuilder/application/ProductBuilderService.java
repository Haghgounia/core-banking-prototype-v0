package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.CatalogResponse;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.EligibilityRuleSaveRequest;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.EligibilityRuleSaveResponse;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.PackageCatalogItem;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.ProductWorkspace;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.ProductVersionDefaults;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.SelectOption;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TableCatalogItem;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TableDescriptor;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TablePage;
import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProductBuilderService {
    private final PdlProductBuilderRepository repository;
    private final ProductBuilderBusinessValidator businessValidator;
    private final PdlReferenceOptionService referenceOptionService;
    private final ProductRuleGovernanceService governance;
    private final ProductGovernanceWriteGuard writeGuard;

    public ProductBuilderService(PdlProductBuilderRepository repository,
                                 ProductBuilderBusinessValidator businessValidator,
                                 PdlReferenceOptionService referenceOptionService,
                                 ProductRuleGovernanceService governance,
                                 ProductGovernanceWriteGuard writeGuard) {
        this.repository = repository;
        this.businessValidator = businessValidator;
        this.referenceOptionService = referenceOptionService;
        this.governance = governance;
        this.writeGuard = writeGuard;
    }

    /** Bank-facing families are GROUPS with at least one ACTIVE contract in PDL.LOAN_TYPE.
     *  Civil participation itself is an individual LOAN_TYPE, not a family code.
     */
    @Transactional(readOnly = true)
    public List<SelectOption> loanProductFamilies() {
        Set<Integer> available = new java.util.HashSet<>(repository.activeLoanGroupCodes());
        return java.util.Arrays.stream(LoanTypeClassification.LoanGroup.values())
                .filter(group -> available.contains(group.code()))
                .map(group -> new SelectOption(group.productFamilyCode(), group.productFamilyCode(),
                        group == LoanTypeClassification.LoanGroup.COMMITMENT ? "تعهدات اعتباری" : "تسهیلات " + group.title()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> activeLoanTypesForCode() {
        return repository.activeLoanTypesForCode();
    }

    @Transactional(readOnly = true)
    public List<String> loanEligibilityEmploymentTypes(long ruleId) {
        Map<String, Object> parent = findById("LOAN_ELIGIBILITY_EXTENSION", ruleId);
        List<String> saved = repository.loanEligibilityEmploymentTypes(ruleId);
        if (!saved.isEmpty()) return saved;
        // Keep a single-code historical row editable prior to first multi-choice save.
        String legacy = text(parent.get("EMPLOYMENT_TYPE_CODE"));
        return legacy.isBlank() ? List.of() : List.of(legacy);
    }

    /** Replaces the normalized employment choices atomically with its eligibility extension. */
    @Transactional
    public Map<String, Object> saveLoanEligibilityEmployment(Long id, Map<String, Object> body,
                                                              List<?> requestedCodes, String actor) {
        if (requestedCodes == null) throw new ProductBuilderValidationException("فهرست انواع اشتغال باید صریحاً ارسال شود.");
        var descriptor = descriptor("LOAN_ELIGIBILITY_EXTENSION");
        var codeColumn = descriptor.columns().stream().filter(c -> "EMPLOYMENT_TYPE_CODE".equals(c.name()))
                .findFirst().orElseThrow(() -> new ProductBuilderValidationException("ستون نوع اشتغال در قرارداد PDL موجود نیست."));
        java.util.Set<String> allowed = new java.util.LinkedHashSet<>();
        for (var option : codeColumn.options()) allowed.add(option.code());
        if (allowed.isEmpty())
            throw new ProductBuilderValidationException("داده مرجع انواع اشتغال در دسترس نیست؛ ثبت افزونه اهلیت متوقف شد.");
        List<String> selected = LoanEmploymentSelectionPolicy.normalize(requestedCodes, allowed);
        Map<String,Object> values = new LinkedHashMap<>(body);
        // Historic single-value column remains a backward-compatible first code; the child table is authoritative.
        values.put("EMPLOYMENT_TYPE_CODE", selected.isEmpty() ? null : selected.get(0));
        Map<String,Object> row = id == null ? create("LOAN_ELIGIBILITY_EXTENSION", values, actor)
                : update("LOAN_ELIGIBILITY_EXTENSION", id, values, actor);
        long ruleId = number(row.get("ELIGIBILITY_RULE_ID"));
        repository.replaceLoanEligibilityEmploymentTypes(ruleId, selected, actorName(actor));
        return row;
    }

    public CatalogResponse catalog() {
        Map<String, List<TableCatalogItem>> grouped = new LinkedHashMap<>();
        Map<String, String> packageTitles = new LinkedHashMap<>();
        long totalRows = 0;
        for (PdlCatalog.Entry entry : PdlCatalog.entries()) {
            long count = repository.count(entry.tableName());
            totalRows += count;
            grouped.computeIfAbsent(entry.packageCode(), k -> new ArrayList<>())
                    .add(new TableCatalogItem(entry.tableName(), entry.title(), entry.packageCode(), entry.packageTitle(), count));
            packageTitles.put(entry.packageCode(), entry.packageTitle());
        }
        List<PackageCatalogItem> packages = grouped.entrySet().stream().map(group -> {
            long rows = group.getValue().stream().mapToLong(TableCatalogItem::rowCount).sum();
            return new PackageCatalogItem(group.getKey(), packageTitles.get(group.getKey()), rows, List.copyOf(group.getValue()));
        }).toList();
        return new CatalogResponse(repository.schemaName(), totalRows, packages);
    }

    public TableDescriptor descriptor(String table) {
        return referenceOptionService.enrich(repository.descriptor(table));
    }

    public TablePage search(String table, String text, int page, int size, String filterColumn, String filterValue) {
        return repository.search(table, text, page, size, filterColumn, filterValue);
    }

    public Map<String, Object> findById(String table, long id) {
        return repository.findById(table, id)
                .orElseThrow(() -> new ProductBuilderValidationException("PDL row not found: " + table + "/" + id));
    }

    /** A multi-selection is one atomic transaction; never serialize IDs into ORG_UNIT_ID. */
    @Transactional
    public List<Map<String, Object>> saveOrgScopes(Map<String, Object> values, List<?> orgUnitIds,
                                                    Long editingId, String actor) {
        if (orgUnitIds == null || orgUnitIds.isEmpty() || orgUnitIds.size() > 500)
            throw new ProductBuilderValidationException("بین ۱ تا ۵۰۰ واحد سازمانی مجاز را انتخاب کنید.");
        long versionId = number(values.get("PRODUCT_VERSION_ID"));
        if (versionId <= 0) throw new ProductBuilderValidationException("نسخه محصول برای محدوده سازمانی الزامی است.");
        // Verify parent exists before making any changes.
        findById("PRODUCT_VERSION", versionId);
        java.util.LinkedHashSet<Long> unique = new java.util.LinkedHashSet<>();
        for (Object raw : orgUnitIds) {
            long id;
            try { id = new java.math.BigDecimal(String.valueOf(raw)).longValueExact(); }
            catch (RuntimeException e) { throw new ProductBuilderValidationException("شناسه واحد سازمانی باید عدد صحیح معتبر باشد."); }
            if (id <= 0 || !unique.add(id)) throw new ProductBuilderValidationException("واحدهای انتخابی نباید خالی یا تکراری باشند.");
        }
        Map<String, Object> editing = editingId == null ? null : findById("PRODUCT_ORG_SCOPE", editingId);
        if (editing != null && number(editing.get("PRODUCT_VERSION_ID")) != versionId)
            throw new ProductBuilderValidationException("محدوده سازمانی فقط در نسخه محصول خودش قابل ویرایش است.");
        if (editing != null && "0".equals(String.valueOf(editing.get("IS_ALLOWED"))) && unique.size() > 1)
            throw new ProductBuilderValidationException("رکورد قدیمی منع عرضه را نمی‌توان به چند واحد گسترش داد.");
        java.util.Set<Long> occupied = new java.util.HashSet<>();
        int loaded = 0;
        for (int page = 0; page < 50; page++) {
            TablePage current = repository.search("PRODUCT_ORG_SCOPE", null, page, 200,
                    "PRODUCT_VERSION_ID", String.valueOf(versionId));
            for (var row : current.items()) {
                if (editingId != null && number(row.get("ORG_SCOPE_ID")) == editingId) continue;
                occupied.add(number(row.get("ORG_UNIT_ID")));
            }
            loaded += current.items().size();
            if (loaded >= current.totalElements()) break;
            if (current.items().isEmpty() || page == 49)
                throw new ProductBuilderValidationException("فهرست محدوده‌های قبلی کامل بازیابی نشد؛ ثبت متوقف شد.");
        }
        if (unique.stream().anyMatch(occupied::contains))
            throw new ProductBuilderValidationException("حداقل یک واحد انتخابی قبلاً برای همین نسخه ثبت شده است؛ رکورد موجود را ویرایش کنید.");
        Map<String, Object> fields = new LinkedHashMap<>(values);
        fields.remove("ORG_UNIT_CODE");
        // Keep the existing unit on its own row when editing; additions become new rows.
        if (editing != null) {
            long oldUnit = number(editing.get("ORG_UNIT_ID"));
            if (unique.remove(oldUnit)) {
                var ordered = new java.util.LinkedHashSet<Long>();
                ordered.add(oldUnit);
                ordered.addAll(unique);
                unique = ordered;
            }
        }
        List<Map<String,Object>> result = new java.util.ArrayList<>();
        boolean first = true;
        for (long unitId : unique) {
            Map<String, Object> record = new LinkedHashMap<>(fields);
            record.put("ORG_UNIT_ID", unitId);
            if (first && editingId != null) result.add(update("PRODUCT_ORG_SCOPE", editingId, record, actor));
            else result.add(create("PRODUCT_ORG_SCOPE", record, actor));
            first = false;
        }
        return result;
    }

    @Transactional
    public Map<String, Object> create(String table, Map<String, Object> values, String actor) {
        String resolvedActor = actorName(actor);
        if ("PRODUCT_VERSION".equals(normalizeTable(table)) && values.containsKey("RULE_POLICY_VERSION_ID")) {
            throw new ProductBuilderValidationException("اتصال سیاست فقط از مسیر اختصاصی Rule Governance Lite مجاز است.");
        }
        if ("PRODUCT_VERSION".equals(normalizeTable(table))) {
            String requestedStatus = text(values.get("VERSION_STATUS_CODE"));
            if (!requestedStatus.isBlank() && !"DRAFT".equals(requestedStatus)) {
                throw new ProductBuilderValidationException("نسخه جدید ابتدا باید به صورت پیش‌نویس ثبت شود. تصویب فقط پس از بازبینی حاکمیت مجاز است.");
            }
            if ("OPEN".equals(text(values.get("ORIGINATION_STATUS_CODE")))) {
                throw new ProductBuilderValidationException("افتتاح محصول برای نسخه ثبت‌نشده مجاز نیست؛ ابتدا پیش‌نویس و قواعد آن را تکمیل کنید.");
            }
        }
        writeGuard.assertCreateAllowed(table, values);
        Map<String, Object> prepared = prepareCreateValues(table, values, resolvedActor);
        LoanBooleanDefaults.apply(normalizeTable(table), prepared);
        if ("PRODUCT".equals(normalizeTable(table))) {
            String domain = text(prepared.get("PRODUCT_CLASS_CODE"));
            if ("LOAN".equals(domain)) {
                if (!text(prepared.get("PRODUCT_CODE")).isBlank())
                    throw new ProductBuilderValidationException("کد محصول تسهیلات فقط پس از نهایی‌سازی موفق مرحله ششم توسط سامانه صادر می‌شود.");
                if (!"DRAFT".equals(text(prepared.getOrDefault("PRODUCT_STATUS_CODE", "DRAFT"))))
                    throw new ProductBuilderValidationException("محصول تسهیلات بدون کد باید در وضعیت پیش‌نویس باقی بماند.");
                if (prepared.containsKey("LOAN_TYPE_ID"))
                    throw new ProductBuilderValidationException("عقد محصول باید در پروفایل تسهیلات نسخه ثبت شود؛ صدور کد در مرحله نهایی انجام می‌شود.");
                prepared.put("PRODUCT_CODE", null); // R29 conditional nullable PRODUCT_CODE migration.
            } else if ("DEPOSIT".equals(domain) && text(prepared.get("PRODUCT_CODE")).isBlank()) {
                throw new ProductBuilderValidationException("کد محصول سپرده الزامی است.");
            }
        }
        if ("PRODUCT_ELIGIBILITY_CRITERION".equals(normalizeTable(table))) {
            referenceOptionService.validateEligibilityCriterion(prepared);
            validateGenderCriterionScope(prepared);
        }
        if ("LOAN_TYPE".equals(normalizeTable(table))) LoanTypeClassification.validateForSave(prepared, null);
        validateLoanProductFamily(table, prepared, null);
        referenceOptionService.validateChangedValues(descriptor(table), prepared, null);
        validateVersionScopedCreate(table, prepared);
        validateRelationshipTargets(table, prepared);
        validateLoanReferenceRows(table, prepared, null);
        validateCivilPartnershipContribution(table, prepared);
        validateLoanTermUnit(table, prepared, null);
        validateLoanRepaymentContract(table, prepared);
        validateInquiryRequiredForCreate(table, prepared);
        validateLoanReferenceMaintenance(table, prepared, null);
        validateChannelOperationDomain(table, prepared, null);
        businessValidator.validate(table, prepared);
        if ("LOAN_PRODUCT_COLLATERAL_RULE".equals(normalizeTable(table)))
            reorderLoanCollateral(prepared, null, resolvedActor);
        long id = repository.insert(table, prepared, resolvedActor);
        return findById(table, id);
    }

    @Transactional
    public Map<String, Object> update(String table, long id, Map<String, Object> values, String actor) {
        String resolvedActor = actorName(actor);
        if ("PRODUCT_VERSION".equals(normalizeTable(table)) && values.containsKey("RULE_POLICY_VERSION_ID")) {
            throw new ProductBuilderValidationException("تغییر سیاست نسخه تنها از مسیر اختصاصی مجاز است.");
        }
        Map<String, Object> existing = findById(table, id);
        Map<String, Object> prepared = prepareUpdateValues(table, existing, values, resolvedActor);
        if ("PRODUCT".equals(normalizeTable(table))) {
            String oldDomain = text(existing.get("PRODUCT_CLASS_CODE"));
            String newDomain = text(prepared.getOrDefault("PRODUCT_CLASS_CODE", oldDomain));
            if (!oldDomain.equals(newDomain) && ("LOAN".equals(oldDomain) || "LOAN".equals(newDomain)))
                throw new ProductBuilderValidationException("تبدیل دامنه سپرده/تسهیلات مجاز نیست؛ محصول با کد سیستمی مستقل بسازید.");
        }
        if ("PRODUCT".equals(normalizeTable(table)) && "LOAN".equals(text(existing.get("PRODUCT_CLASS_CODE")))) {
            if (repository.registeredLoanType(id).isPresent() && prepared.containsKey("PRODUCT_FAMILY_CODE")
                    && !text(prepared.get("PRODUCT_FAMILY_CODE")).equals(text(existing.get("PRODUCT_FAMILY_CODE"))))
                throw new ProductBuilderValidationException("خانواده محصول دارای کد سیستمی قابل تغییر نیست.");
            if (text(existing.get("PRODUCT_CODE")).isBlank()
                    && !"DRAFT".equals(text(prepared.getOrDefault("PRODUCT_STATUS_CODE", existing.get("PRODUCT_STATUS_CODE")))))
                throw new ProductBuilderValidationException("تا پیش از صدور کد نهایی، وضعیت محصول تسهیلات باید پیش‌نویس باشد.");
            if (prepared.containsKey("LOAN_TYPE_ID") || (prepared.containsKey("PRODUCT_CODE")
                    && !text(prepared.get("PRODUCT_CODE")).equals(text(existing.get("PRODUCT_CODE")))))
                throw new ProductBuilderValidationException("کد محصول و عقد سازنده پس از ایجاد محصول تسهیلات قابل تغییر نیستند.");
            prepared.remove("LOAN_TYPE_ID");
            prepared.remove("PRODUCT_CODE");
        }
        Map<String, Object> merged = new LinkedHashMap<>(existing);
        merged.putAll(prepared);
        writeGuard.assertUpdateAllowed(table, existing, prepared);
        if ("PRODUCT_ELIGIBILITY_CRITERION".equals(normalizeTable(table))) {
            referenceOptionService.validateEligibilityCriterion(merged);
            validateGenderCriterionScope(merged);
        }
        if ("LOAN_TYPE".equals(normalizeTable(table))) {
            LoanTypeClassification.validateForSave(merged, existing);
            if (number(merged.get("LOAN_GROUP")) != number(existing.get("LOAN_GROUP"))
                    && repository.loanTypeHasProfiles(id)) {
                throw new ProductBuilderValidationException("گروه عقد دارای پروفایل تسهیلات قابل تغییر نیست؛ نسخه‌های محصول به این گروه وابسته‌اند.");
            }
        }
        validateLoanProductFamily(table, merged, existing);
        referenceOptionService.validateChangedValues(descriptor(table), prepared, existing);
        if ("PRODUCT_VERSION".equals(normalizeTable(table))) {
            governance.assertPromotionAllowed(id, existing, merged);
        }
        validateRelationshipTargets(table, merged);
        validateLoanReferenceRows(table, merged, existing);
        validateCivilPartnershipContribution(table, merged);
        validateLoanTermUnit(table, merged, existing);
        validateLoanRepaymentContract(table, merged);
        validateLoanReferenceMaintenance(table, merged, id);
        validateChannelOperationDomain(table, merged, existing);
        businessValidator.validate(table, merged);
        if ("LOAN_PRODUCT_COLLATERAL_RULE".equals(normalizeTable(table)))
            reorderLoanCollateral(merged, id, resolvedActor);
        // Keep the ordinal in the update payload when the server computed its rank.
        if ("LOAN_PRODUCT_COLLATERAL_RULE".equals(normalizeTable(table)))
            prepared.put("PRIORITY_NO", merged.get("PRIORITY_NO"));
        if (!repository.update(table, id, prepared, resolvedActor)) {
            throw new ProductBuilderValidationException("PDL row not found: " + table + "/" + id);
        }
        return findById(table, id);
    }

    /** Only the approved, current loan configuration may receive a code. Issuance,
     * product status and registry entry commit or roll back together. The parent
     * PRODUCT row lock guarantees that repeat/concurrent requests are idempotent.
     */
    @Transactional
    public Map<String, Object> finalizeLoanProduct(long productId, long versionId, String actor) {
        String resolvedActor = actorName(actor);
        repository.lockProduct(productId);
        Map<String, Object> product = findById("PRODUCT", productId);
        if (!"LOAN".equals(text(product.get("PRODUCT_CLASS_CODE"))))
            throw new ProductBuilderValidationException("صدور سیستمی کد در مرحله نهایی فقط برای محصول تسهیلات مجاز است.");
        if (number(product.get("IS_DELETED")) != 0)
            throw new ProductBuilderValidationException("برای محصول حذف‌شده صدور کد مجاز نیست.");
        repository.lockProductVersion(versionId);
        Map<String, Object> version = findById("PRODUCT_VERSION", versionId);
        if (number(version.get("PRODUCT_ID")) != productId)
            throw new ProductBuilderValidationException("نسخه انتخاب‌شده متعلق به این محصول تسهیلات نیست.");
        String existingCode = text(product.get("PRODUCT_CODE"));
        if (!existingCode.isBlank()) {
            // R28/legacy issued codes remain immutable and must not consume another serial.
            return Map.of("productId", productId, "productCode", existingCode, "newlyIssued", false);
        }
        LoanCodeIssuanceGate.requireDraft(text(product.get("PRODUCT_STATUS_CODE")));
        LoanCodeIssuanceGate.requireApprovedCurrent(text(version.get("VERSION_STATUS_CODE")),
                number(version.get("IS_CURRENT")) == 1, text(version.get("RECORD_STATUS_CODE")));
        if (repository.currentProductVersionCount(productId) != 1L)
            throw new ProductBuilderValidationException("نهایی‌سازی مستلزم دقیقاً یک نسخه جاری برای محصول است؛ وضعیت نسخه‌های دیگر را اصلاح کنید.");
        var profiles = repository.search("LOAN_PRODUCT_PROFILE", null, 0, 2,
                "PRODUCT_VERSION_ID", String.valueOf(versionId));
        LoanCodeIssuanceGate.requireSingleProfile(profiles.totalElements());
        if (profiles.items().size() != 1)
            throw new ProductBuilderValidationException("پروفایل نسخه به‌طور کامل بازیابی نشد؛ صدور کد متوقف شد.");
        Map<String, Object> profile = profiles.items().get(0);
        if (profile.containsKey("RECORD_STATUS_CODE")
                && !"ACTIVE".equals(text(profile.get("RECORD_STATUS_CODE"))))
            throw new ProductBuilderValidationException("پروفایل تسهیلات نسخه مصوب باید فعال باشد.");
        long loanTypeId = requiredLoanId(profile, "LOAN_TYPE_ID");
        Map<String, Object> loanType = findById("LOAN_TYPE", loanTypeId);
        int expectedGroup = LoanTypeClassification.groupForProductFamily(text(product.get("PRODUCT_FAMILY_CODE")));
        if (expectedGroup == 0 || expectedGroup != number(loanType.get("LOAN_GROUP"))
                || !"ACTIVE".equals(text(loanType.get("RECORD_STATUS_CODE"))))
            throw new ProductBuilderValidationException("عقد پروفایل باید فعال و متعلق به خانواده محصول تسهیلات باشد.");
        // Validate the product-version/profile currency contract again at issuance.
        loanProfileCurrency(versionId, loanTypeId);
        validateLoanProductFamily("PRODUCT", product, null);
        validateLoanReferenceRows("LOAN_PRODUCT_PROFILE", profile, null);
        businessValidator.validate("PRODUCT", product);
        businessValidator.validate("LOAN_PRODUCT_PROFILE", profile);
        var readiness = governance.report(versionId);
        LoanCodeIssuanceGate.requireGovernance(readiness.canApprove(), readiness.blockers());
        // All readiness checks precede serial allocation. No code is consumed on failure.
        PdlProductBuilderRepository.GeneratedLoanCode issued =
                repository.allocateLoanProductCode(loanTypeId, text(loanType.get("LOAN_TYPE_CODE")));
        boolean changed = repository.update("PRODUCT", productId,
                Map.of("PRODUCT_CODE", issued.code(), "PRODUCT_STATUS_CODE", "INACTIVE"), resolvedActor);
        if (!changed) throw new ProductBuilderValidationException("محصول هنگام نهایی‌سازی یافت نشد.");
        repository.registerLoanProductCode(productId, issued, resolvedActor);
        // INACTIVE deliberately separates successful design finalization from branch offering.
        return Map.of("productId", productId, "productCode", issued.code(), "newlyIssued", true);
    }

    @Transactional
    public void delete(String table, long id, String actor) {
        writeGuard.assertDeleteAllowed(table, findById(table, id));
        Map<String, Object> current = findById(table, id);
        if ("LOAN_PRODUCT_COLLATERAL_RULE".equals(normalizeTable(table)))
            repository.lockCollateralProductVersion(requiredLoanId(current, "PRODUCT_VERSION_ID"));
        if (!repository.delete(table, id, actorName(actor))) {
            throw new ProductBuilderValidationException("PDL row not found: " + table + "/" + id);
        }
        if ("LOAN_PRODUCT_COLLATERAL_RULE".equals(normalizeTable(table)))
            normalizeLoanCollateralPriorities(requiredLoanId(current, "PRODUCT_VERSION_ID"), actorName(actor));
    }


    private static final Set<String> LOAN_CHANNEL_OPERATIONS = Set.of(
            "APPLICATION", "DISBURSEMENT", "REPAYMENT", "SERVICING", "CLOSURE", "INQUIRY");
    private static final Set<String> DEPOSIT_CHANNEL_OPERATIONS = Set.of(
            "OPENING", "SERVICING", "CLOSURE", "INQUIRY", "TRANSFER_OUT",
            "CASH_WITHDRAWAL", "CHEQUE", "ALL_OPERATIONS");

    /** Domain-specific operations are not interchangeable between deposits and loans. */
    private void validateChannelOperationDomain(String table, Map<String,Object> values, Map<String,Object> old) {
        if (!"PRODUCT_CHANNEL_OPERATION".equals(normalizeTable(table))) return;
        String code = text(values.get("OPERATION_CODE"));
        if (old != null && code.equals(text(old.get("OPERATION_CODE")))
                && number(old.get("CHANNEL_RULE_ID")) == number(values.get("CHANNEL_RULE_ID")))
            return; // Historical code in its original channel rule; not portable to another domain.
        long parentId = requiredLoanId(values, "CHANNEL_RULE_ID");
        Map<String,Object> channelRule = findById("PRODUCT_CHANNEL_RULE", parentId);
        long versionId = requiredLoanId(channelRule, "PRODUCT_VERSION_ID");
        Map<String,Object> version = findById("PRODUCT_VERSION", versionId);
        Map<String,Object> product = findById("PRODUCT", requiredLoanId(version, "PRODUCT_ID"));
        String domain = text(product.get("PRODUCT_CLASS_CODE"));
        Set<String> allowed = "LOAN".equals(domain) ? LOAN_CHANNEL_OPERATIONS
                : "DEPOSIT".equals(domain) ? DEPOSIT_CHANNEL_OPERATIONS : Set.of();
        if (!allowed.contains(code))
            throw new ProductBuilderValidationException("عملیات «" + code + "» برای دامنه "
                    + ("LOAN".equals(domain) ? "تسهیلات" : "سپرده")
                    + " مجاز نیست؛ عملیات را از فهرست تخصصی همان دامنه انتخاب کنید.");
    }

    private List<Map<String,Object>> loanCollateralRows(long versionId) {
        List<Map<String,Object>> result = new ArrayList<>();
        for (int page = 0; page < 100; page++) {
            TablePage batch = repository.search("LOAN_PRODUCT_COLLATERAL_RULE", null, page, 200,
                    "PRODUCT_VERSION_ID", String.valueOf(versionId));
            result.addAll(batch.items());
            if (result.size() >= batch.totalElements()) break;
            if (batch.items().isEmpty() || page == 99)
                throw new ProductBuilderValidationException("فهرست قواعد وثیقه کامل بارگذاری نشد؛ تعیین اولویت متوقف شد.");
        }
        result.sort(java.util.Comparator
                .comparingLong((Map<String,Object> row) -> number(row.get("PRIORITY_NO")))
                .thenComparingLong(row -> number(row.get("LOAN_COLLATERAL_RULE_ID"))));
        return result;
    }

    private void normalizeLoanCollateralPriorities(long versionId, String actor) {
        List<Map<String,Object>> rows = loanCollateralRows(versionId);
        for (int i = 0; i < rows.size(); i++) {
            Map<String,Object> row = rows.get(i);
            if (number(row.get("PRIORITY_NO")) != i + 1)
                repository.update("LOAN_PRODUCT_COLLATERAL_RULE", number(row.get("LOAN_COLLATERAL_RULE_ID")),
                        Map.of("PRIORITY_NO", i + 1), actor);
        }
    }

    /** Insert/move into 1..N without duplicate ranks, serialized under the parent version row. */
    private void reorderLoanCollateral(Map<String,Object> values, Long editingId, String actor) {
        long versionId = requiredLoanId(values, "PRODUCT_VERSION_ID");
        repository.lockCollateralProductVersion(versionId);
        List<Map<String,Object>> siblings = loanCollateralRows(versionId);
        long collateralId = requiredLoanId(values, "LOAN_PRODUCT_COLLATERAL_ID");
        if (editingId != null) {
            Map<String,Object> current = findById("LOAN_PRODUCT_COLLATERAL_RULE", editingId);
            if (number(current.get("PRODUCT_VERSION_ID")) != versionId)
                throw new ProductBuilderValidationException("تغییر نسخه قاعده وثیقه مجاز نیست.");
        }
        for (Map<String,Object> row : siblings) {
            if (editingId != null && number(row.get("LOAN_COLLATERAL_RULE_ID")) == editingId) continue;
            if (number(row.get("LOAN_PRODUCT_COLLATERAL_ID")) == collateralId)
                throw new ProductBuilderValidationException("این نوع وثیقه قبلاً برای نسخه محصول تعریف شده است؛ رکورد موجود را ویرایش کنید.");
        }
        siblings.removeIf(row -> editingId != null && number(row.get("LOAN_COLLATERAL_RULE_ID")) == editingId);
        long requested = number(values.get("PRIORITY_NO")); // zero: automatic next ordinal
        int index = requested < 1 ? siblings.size() : (int)Math.min(requested - 1, siblings.size());
        for (int i = 0; i < siblings.size(); i++) {
            int rank = i < index ? i + 1 : i + 2;
            Map<String,Object> row = siblings.get(i);
            if (number(row.get("PRIORITY_NO")) != rank)
                repository.update("LOAN_PRODUCT_COLLATERAL_RULE", number(row.get("LOAN_COLLATERAL_RULE_ID")),
                        Map.of("PRIORITY_NO", rank), actor);
        }
        values.put("PRIORITY_NO", index + 1);
    }

    /** Protects loan reference data against duplicate codes/pairs and inactive parents.
     *  PDL has PK/FK constraints but no uniqueness constraint on business code or type/plan pair. */
    private void validateLoanReferenceMaintenance(String table, Map<String, Object> values, Long editingId) {
        String target = normalizeTable(table);
        if ("LOAN_TYPE".equals(target) || "PLAN_TYPE".equals(target) || "LOAN_PRODUCT_COLLATERAL".equals(target)) {
            String key = "LOAN_PRODUCT_COLLATERAL".equals(target) ? "PRODUCT_COLLATERAL_CODE" : target + "_CODE";
            String code = text(values.get(key)).trim();
            if (code.isBlank()) throw new ProductBuilderValidationException(
                "LOAN_PRODUCT_COLLATERAL".equals(target) ? "کد نوع وثیقه الزامی است." : "کد نوع عقد یا طرح الزامی است.");
            if (repository.loanReferenceCodeExists(target, key, code, editingId))
                throw new ProductBuilderValidationException("کد «" + code + "» قبلاً ثبت شده است. رکورد موجود را ویرایش کنید.");
        }
        if (!"LOAN_TYPE_PLAN_TYPE".equals(target)) return;
        long loanType = requiredLoanId(values, "LOAN_TYPE_ID");
        long planType = requiredLoanId(values, "PLAN_TYPE_ID");
        if (loanType <= 0 || planType <= 0) throw new ProductBuilderValidationException("انتخاب نوع عقد و طرح معتبر الزامی است.");
        // Serializing per loan type prevents concurrent inserts of the same relation pair.
        repository.lockLoanTypeForMapping(loanType);
        // Deactivation remains possible even if a parent is later retired; activation must have active parents.
        if ("ACTIVE".equals(String.valueOf(values.get("RECORD_STATUS_CODE")))) {
            for (var relation : Map.of("LOAN_TYPE", loanType, "PLAN_TYPE", planType).entrySet()) {
                Map<String, Object> reference = repository.findById(relation.getKey(), relation.getValue())
                    .orElseThrow(() -> new ProductBuilderValidationException("رکورد مرجع عقد/طرح یافت نشد."));
                if (!"ACTIVE".equals(String.valueOf(reference.get("RECORD_STATUS_CODE"))))
                    throw new ProductBuilderValidationException("ثبت رابطه فقط با عقد و طرح فعال مجاز است.");
            }
        }
        if (repository.loanTypePlanPairExists(loanType, planType, editingId))
            throw new ProductBuilderValidationException("این رابطه عقد و طرح قبلاً ثبت شده است؛ همان رکورد را فعال یا ویرایش کنید.");
    }

    /** A loan PRODUCT family represents the active LOAN_GROUP, not the individual loan contract. */
    private void validateLoanProductFamily(String table, Map<String, Object> values, Map<String, Object> previous) {
        if (!"PRODUCT".equals(normalizeTable(table))) return;
        String domain = text(values.get("PRODUCT_CLASS_CODE"));
        if (previous != null && !domain.equals(text(previous.get("PRODUCT_CLASS_CODE")))
                && repository.loanProductHasProfiles(number(previous.get("PRODUCT_ID")))) {
            throw new ProductBuilderValidationException("دامنه محصول دارای پروفایل تسهیلات قابل تغییر نیست.");
        }
        if (!"LOAN".equals(domain)) return;
        String family = text(values.get("PRODUCT_FAMILY_CODE"));
        if (previous != null) {
            long id = number(previous.get("PRODUCT_ID"));
            if (repository.loanProductHasProfiles(id)
                    && (!family.equals(text(previous.get("PRODUCT_FAMILY_CODE")))
                    || !text(values.get("DEFAULT_CURRENCY_CODE")).equals(text(previous.get("DEFAULT_CURRENCY_CODE"))))) {
                throw new ProductBuilderValidationException("پس از ثبت پروفایل تسهیلات، خانواده و ارز اصلی محصول نباید بی‌صدا تغییر کنند؛ ابتدا فرایند اصلاح نسخه را مشخص کنید.");
            }
            if ("LOAN".equals(text(previous.get("PRODUCT_CLASS_CODE")))
                    && family.equals(text(previous.get("PRODUCT_FAMILY_CODE")))) return; // historical loan codes remain readable/editable
        }
        int groupCode = LoanTypeClassification.groupForProductFamily(family);
        if (groupCode == 0 || !repository.activeLoanGroupExists(groupCode)) {
            throw new ProductBuilderValidationException("خانواده محصول تسهیلات باید از گروه دارای نوع عقد فعال در اطلاعات پایه انتخاب شود.");
        }
    }

    /** Source-of-truth currency and group for a loan profile: PRODUCT -> VERSION -> LOAN_TYPE. */
    private String loanProfileCurrency(long versionId, long loanTypeId) {
        if (versionId <= 0) throw new ProductBuilderValidationException("نسخه محصول تسهیلات مشخص نشده است.");
        Map<String, Object> version = findById("PRODUCT_VERSION", versionId);
        Map<String, Object> product = findById("PRODUCT", number(version.get("PRODUCT_ID")));
        if (!"LOAN".equals(text(product.get("PRODUCT_CLASS_CODE"))))
            throw new ProductBuilderValidationException("پروفایل تسهیلات باید به نسخه یک محصول تسهیلات تعلق داشته باشد.");
        if (loanTypeId <= 0) throw new ProductBuilderValidationException("انتخاب نوع عقد معتبر الزامی است.");
        Map<String, Object> type = findById("LOAN_TYPE", loanTypeId);
        repository.registeredLoanType(number(product.get("PRODUCT_ID"))).ifPresent(original -> {
            if (original != loanTypeId)
                throw new ProductBuilderValidationException("عقد پروفایل با عقد سازنده کد محصول متفاوت است؛ کد محصول دائمی و تغییرناپذیر است.");
        });
        int group = LoanTypeClassification.groupForProductFamily(text(product.get("PRODUCT_FAMILY_CODE")));
        if (group != 0 && group != number(type.get("LOAN_GROUP")))
            throw new ProductBuilderValidationException("نوع عقد انتخابی با خانواده محصول سازگار نیست؛ خانواده بر اساس گروه نوع عقد تعیین می‌شود.");
        if (group == 0 && !LoanTypeClassification.isLegacyRetailFamily(text(product.get("PRODUCT_FAMILY_CODE"))))
            throw new ProductBuilderValidationException("خانواده محصول تسهیلات معتبر نیست.");
        String currency = text(product.get("DEFAULT_CURRENCY_CODE"));
        if (!currency.matches("[A-Z]{3}")) throw new ProductBuilderValidationException("ارز پیش‌فرض محصول باید کد سه‌حرفی معتبر باشد.");
        return currency;
    }

    private void validateLoanReferenceRows(String table, Map<String, Object> values, Map<String, Object> previous) {
        Map<String, String> foreigns;
        if ("LOAN_PRODUCT_PROFILE".equals(normalizeTable(table))) {
            foreigns = Map.of("LOAN_TYPE_ID", "LOAN_TYPE", "LOAN_USAGE_ID", "LOAN_USAGE",
                    "PLAN_TYPE_ID", "PLAN_TYPE", "ECONOMIC_SUB_SECTION_ID", "ECONOMIC_SUB_SECTION",
                    "OPERATION_PARAMETER_ID", "FACILITY_OPERATION_PARAMETER");
        } else if ("LOAN_PRODUCT_COLLATERAL_RULE".equals(normalizeTable(table))) {
            foreigns = Map.of("LOAN_PRODUCT_COLLATERAL_ID", "LOAN_PRODUCT_COLLATERAL");
        } else return;
        for (var entry : foreigns.entrySet()) {
            Object value = values.get(entry.getKey());
            if (value == null || value.toString().isBlank()) continue;
            if (previous != null && java.util.Objects.equals(value.toString(), String.valueOf(previous.get(entry.getKey())))) continue;
            long id;
            try { id = new java.math.BigDecimal(value.toString()).longValueExact(); }
            catch (NumberFormatException | ArithmeticException ex) { throw new ProductBuilderValidationException("شناسه مرجع تسهیلات باید عدد صحیح معتبر باشد."); }
            if (id <= 0) throw new ProductBuilderValidationException("شناسه مرجع تسهیلات نامعتبر است.");
            Map<String, Object> ref = repository.findById(entry.getValue(), id)
                    .orElseThrow(() -> new ProductBuilderValidationException("گزینه انتخاب‌شده در جدول مرجع " + entry.getValue() + " یافت نشد."));
            if (ref.containsKey("RECORD_STATUS_CODE") && !"ACTIVE".equals(String.valueOf(ref.get("RECORD_STATUS_CODE"))))
                throw new ProductBuilderValidationException("گزینه انتخابی در اطلاعات پایه تسهیلات فعال نیست.");
        }
        if ("LOAN_PRODUCT_PROFILE".equals(normalizeTable(table))) {
            long type = requiredLoanId(values, "LOAN_TYPE_ID"), plan = requiredLoanId(values, "PLAN_TYPE_ID");
            String masterCurrency = loanProfileCurrency(requiredLoanId(values, "PRODUCT_VERSION_ID"), type);
            if (!masterCurrency.equals(text(values.get("CURRENCY_TYPE_CODE")))) {
                throw new ProductBuilderValidationException("ارز پروفایل باید از ارز پیش‌فرض هویت محصول به‌صورت سیستمی تأمین شود.");
            }
            if (!repository.loanTypePlanAllowed(type, plan))
                throw new ProductBuilderValidationException("طرح انتخاب‌شده برای نوع عقد تسهیلات در جدول LOAN_TYPE_PLAN_TYPE مجاز نیست.");
            Object sub = values.get("ECONOMIC_SUB_SECTION_ID");
            if (sub != null && !sub.toString().isBlank()) {
                long subId = requiredLoanId(values, "ECONOMIC_SUB_SECTION_ID");
                if (!repository.loanEconomicSubSectionAllowed(plan, subId))
                    throw new ProductBuilderValidationException("زیربخش اقتصادی انتخاب‌شده به طرح تسهیلات جاری تعلق ندارد.");
            }
        }
    }

    private void validateInquiryRequiredForCreate(String table, Map<String, Object> values) {
        if ("PRODUCT_REQUIRED_INQUIRY".equals(normalizeTable(table))
                && !"1".equals(String.valueOf(values.get("IS_MANDATORY")))) {
            throw new ProductBuilderValidationException("در فرم استعلام‌های الزامی، تنها استعلام اجباری قابل ثبت است؛ استعلام غیرالزامی را در این فهرست اضافه نکنید.");
        }
    }

    private void validateLoanTermUnit(String table, Map<String, Object> values, Map<String, Object> existing) {
        if (!"LOAN_PRODUCT_REPAYMENT_RULE".equals(normalizeTable(table))) return;
        String unit = text(values.get("TERM_UNIT_CODE"));
        if (unit.isBlank()) return;
        if (!referenceOptionService.matchesDurationUnit(unit, "DAY")
                && !referenceOptionService.matchesDurationUnit(unit, "MONTH")
                && (existing == null || !unit.equals(text(existing.get("TERM_UNIT_CODE"))))) {
            throw new ProductBuilderValidationException("واحد مدت در مرجع DPS برای روز یا ماه معتبر نیست؛ ابتدا اطلاعات پایه واحدهای مدت را بررسی کنید.");
        }
    }

    private void validateLoanRepaymentContract(String table, Map<String, Object> values) {
        if (!"LOAN_PRODUCT_REPAYMENT_RULE".equals(normalizeTable(table))) return;
        long versionId = number(values.get("PRODUCT_VERSION_ID"));
        if (versionId <= 0) throw new ProductBuilderValidationException("نسخه محصول تسهیلات مشخص نشده است.");
        TablePage profiles = repository.search("LOAN_PRODUCT_PROFILE", null, 0, 2,
                "PRODUCT_VERSION_ID", String.valueOf(versionId));
        if (profiles.totalElements() != 1 || profiles.items().size() != 1)
            throw new ProductBuilderValidationException("ابتدا پروفایل و نوع عقد نسخه تسهیلات را ثبت کنید.");
        long loanTypeId = number(profiles.items().get(0).get("LOAN_TYPE_ID"));
        Map<String, Object> loanType = findById("LOAN_TYPE", loanTypeId);
        long group = number(loanType.get("LOAN_GROUP"));
        if (group == 1 || group == 2) {
            if (values.get("MIN_TERM_DURATION") != null || values.get("MAX_TERM_DURATION") != null
                    || !text(values.get("TERM_UNIT_CODE")).isBlank())
                throw new ProductBuilderValidationException("در قرض‌الحسنه و عقود مبادله‌ای، تعداد و فاصله اقساط ملاک است؛ مدت تسهیلات به‌صورت مستقل ثبت نشود.");
            boolean hasCount = values.get("MIN_INSTALLMENT_COUNT") != null || values.get("MAX_INSTALLMENT_COUNT") != null;
            if (hasCount && values.get("INSTALLMENT_INTERVAL") == null)
                throw new ProductBuilderValidationException("برای تعداد اقساط، فاصله اقساط به ماه را نیز تعیین کنید.");
            if (!referenceOptionService.matchesDurationUnit(text(values.get("INSTALLMENT_INTERVAL_UNIT_CODE")), "MONTH"))
                throw new ProductBuilderValidationException("واحد فاصله اقساط این عقد باید «ماه» از مرجع معتبر DPS باشد.");
        } else if (group == 3) {
            for (String key : List.of("MIN_INSTALLMENT_COUNT", "MAX_INSTALLMENT_COUNT", "INSTALLMENT_INTERVAL",
                    "INSTALLMENT_INTERVAL_UNIT_CODE", "INSTALLMENT_FREQUENCY_CODE")) {
                if (values.get(key) != null && !text(values.get(key)).isBlank())
                    throw new ProductBuilderValidationException("در عقود مشارکتی، تعداد و فاصله اقساط کاربرد ندارد؛ مدت تسهیلات به روز تعریف می‌شود.");
            }
            if (!referenceOptionService.matchesDurationUnit(text(values.get("TERM_UNIT_CODE")), "DAY"))
                throw new ProductBuilderValidationException("واحد مدت تسهیلات مشارکتی باید «روز» از مرجع معتبر DPS باشد.");
        }
    }

    /** Contribution is a ratio in Oracle, but belongs only to a real civil partnership contract.
     *  A participatory family alone is not sufficient. No schema changes or test fixtures. */
    private void validateCivilPartnershipContribution(String table, Map<String,Object> values) {
        if (!"LOAN_FINANCIAL_EXTENSION".equals(normalizeTable(table))) return;
        if (values.get("MIN_CUSTOMER_CONTRIBUTION") == null && values.get("MAX_CUSTOMER_CONTRIBUTION") == null) return;
        long versionId = requiredLoanId(values, "PRODUCT_VERSION_ID");
        TablePage profiles = repository.search("LOAN_PRODUCT_PROFILE", null, 0, 2, "PRODUCT_VERSION_ID", String.valueOf(versionId));
        if (profiles.totalElements() != 1 || profiles.items().size() != 1)
            throw new ProductBuilderValidationException("برای ثبت درصد آورده، ابتدا پروفایل تسهیلات این نسخه و عقد مشارکت مدنی را مشخص کنید.");
        long typeId = requiredLoanId(profiles.items().get(0), "LOAN_TYPE_ID");
        Map<String,Object> contract = findById("LOAN_TYPE", typeId);
        if (!LoanCivilPartnershipPolicy.matches(contract))
            throw new ProductBuilderValidationException("درصد سهم / آورده مشتری فقط برای عقد مشارکت مدنی قابل ثبت است.");
    }

    private static long requiredLoanId(Map<String, Object> values, String key) {
        try { return new java.math.BigDecimal(String.valueOf(values.get(key))).longValueExact(); }
        catch (NumberFormatException | ArithmeticException e) { throw new ProductBuilderValidationException("نوع عقد و طرح تسهیلات باید از اطلاعات پایه انتخاب شوند."); }
    }

    public List<SelectOption> lookup(String table, String column, String text, int limit) {
        return repository.lookup(table, column, text, limit);
    }

    public EligibilityRuleSaveResponse eligibilityRule(long id) {
        Map<String, Object> rule = findById("PRODUCT_ELIGIBILITY_RULE", id);
        TablePage page = repository.search("PRODUCT_ELIGIBILITY_CRITERION", null, 0, 500,
                "ELIGIBILITY_RULE_ID", String.valueOf(id));
        return new EligibilityRuleSaveResponse(rule, page.items());
    }

    @Transactional
    public EligibilityRuleSaveResponse saveEligibilityRule(Long id, EligibilityRuleSaveRequest request, String actor) {
        if (request == null || request.rule() == null) {
            throw new ProductBuilderValidationException("اطلاعات قاعده اهلیت الزامی است.");
        }
        String resolvedActor = actorName(actor);
        List<Map<String, Object>> requestedCriteria = request.criteria() == null ? List.of() : request.criteria();
        Map<String, Object> existing = id == null ? null : findById("PRODUCT_ELIGIBILITY_RULE", id);
        Map<String, Object> prepared = existing == null
                ? new LinkedHashMap<>(request.rule())
                : new LinkedHashMap<>(request.rule());
        applyEligibilityLegacyProjection(prepared, existing, requestedCriteria);

        long ruleId;
        if (existing == null) {
            writeGuard.assertCreateAllowed("PRODUCT_ELIGIBILITY_RULE", prepared);
            referenceOptionService.validateChangedValues(descriptor("PRODUCT_ELIGIBILITY_RULE"), prepared, null);
            businessValidator.validate("PRODUCT_ELIGIBILITY_RULE", prepared);
            ruleId = repository.insert("PRODUCT_ELIGIBILITY_RULE", prepared, resolvedActor);
        } else {
            writeGuard.assertUpdateAllowed("PRODUCT_ELIGIBILITY_RULE", existing, prepared);
            referenceOptionService.validateChangedValues(descriptor("PRODUCT_ELIGIBILITY_RULE"), prepared, existing);
            Map<String, Object> merged = new LinkedHashMap<>(existing);
            merged.putAll(prepared);
            businessValidator.validate("PRODUCT_ELIGIBILITY_RULE", merged);
            if (!repository.update("PRODUCT_ELIGIBILITY_RULE", id, prepared, resolvedActor)) {
                throw new ProductBuilderValidationException("قاعده اهلیت یافت نشد: " + id);
            }
            ruleId = id;
        }

        TablePage current = repository.search("PRODUCT_ELIGIBILITY_CRITERION", null, 0, 1000,
                "ELIGIBILITY_RULE_ID", String.valueOf(ruleId));
        for (Map<String, Object> row : current.items()) {
            long criterionId = number(row.get("ELIGIBILITY_CRITERION_ID"));
            if (criterionId > 0) repository.delete("PRODUCT_ELIGIBILITY_CRITERION", criterionId, resolvedActor);
        }

        List<Map<String, Object>> normalized = requestedCriteria.stream()
                .<Map<String, Object>>map(row -> new LinkedHashMap<>(row))
                .peek(row -> row.put("ELIGIBILITY_RULE_ID", ruleId))
                .sorted((left, right) -> Integer.compare(criterionSort(left), criterionSort(right)))
                .toList();

        for (Map<String, Object> criterion : normalized) {
            referenceOptionService.validateEligibilityCriterion(criterion);
            validateGenderCriterionScopeAgainstRequest(criterion, normalized);
            businessValidator.validate("PRODUCT_ELIGIBILITY_CRITERION", criterion);
            repository.insert("PRODUCT_ELIGIBILITY_CRITERION", criterion, resolvedActor);
        }
        return eligibilityRule(ruleId);
    }

    private void applyEligibilityLegacyProjection(Map<String, Object> values, Map<String, Object> existing,
                                                   List<Map<String, Object>> criteria) {
        TableDescriptor descriptor = descriptor("PRODUCT_ELIGIBILITY_RULE");
        Map<String, String> projectionColumns = Map.of(
                "CUSTOMER_TYPE", "CUSTOMER_TYPE_CODE",
                "CUSTOMER_SEGMENT", "CUSTOMER_SEGMENT_CODE",
                "CUSTOMER_STATUS", "CUSTOMER_STATUS_CODE",
                "GENDER", "GENDER_CODE",
                "RESIDENCY_STATUS", "RESIDENCY_STATUS_CODE",
                "NATIONALITY_SCOPE", "NATIONALITY_SCOPE_CODE"
        );
        for (Map.Entry<String, String> projection : projectionColumns.entrySet()) {
            var column = descriptor.columns().stream().filter(c -> projection.getValue().equals(c.name())).findFirst().orElse(null);
            if (column == null) continue;
            List<String> selected = criteria.stream()
                    .filter(row -> projection.getKey().equals(text(row.get("CRITERION_TYPE_CODE"))))
                    .filter(row -> "IN".equals(text(row.get("OPERATOR_CODE"))) || "EQ".equals(text(row.get("OPERATOR_CODE"))))
                    .map(row -> row.get("CRITERION_VALUE_CODE") == null ? "" : row.get("CRITERION_VALUE_CODE").toString().trim())
                    .filter(v -> !v.isBlank())
                    .toList();
            if (!selected.isEmpty()) {
                values.put(projection.getValue(), selected.get(0));
                continue;
            }
            if (column.nullable()) {
                values.put(projection.getValue(), null);
                continue;
            }
            if (existing != null && existing.get(projection.getValue()) != null) {
                values.put(projection.getValue(), existing.get(projection.getValue()));
                continue;
            }
            String unrestricted = column.options().stream()
                    .filter(o -> Set.of("ALL", "ANY", "3").contains(text(o.code())) || Set.of("ALL", "ANY", "3").contains(text(o.value())))
                    .map(o -> o.value().toString()).findFirst().orElse(null);
            if (unrestricted != null) {
                values.put(projection.getValue(), unrestricted);
            } else if (!column.options().isEmpty()) {
                values.put(projection.getValue(), column.options().get(0).value());
            }
        }

        descriptor.columns().stream()
                .filter(c -> Set.of("PRIORITY_NO", "RULE_PRIORITY", "PRIORITY").contains(c.name()))
                .findFirst()
                .ifPresent(column -> {
                    if (!values.containsKey(column.name()) || values.get(column.name()) == null || values.get(column.name()).toString().isBlank()) {
                        values.put(column.name(), 100);
                    }
                });
    }

    private void validateGenderCriterionScopeAgainstRequest(Map<String, Object> criterion, List<Map<String, Object>> allCriteria) {
        if (!"GENDER".equals(text(criterion.get("CRITERION_TYPE_CODE")))) return;
        List<String> customerTypes = allCriteria.stream()
                .filter(row -> "CUSTOMER_TYPE".equals(text(row.get("CRITERION_TYPE_CODE"))))
                .map(row -> text(row.get("CRITERION_VALUE_CODE")))
                .filter(v -> !v.isBlank())
                .distinct().toList();
        if (customerTypes.isEmpty() || customerTypes.stream().anyMatch(v -> !Set.of("PERSON", "INDIVIDUAL").contains(v))) {
            throw new ProductBuilderValidationException("معیار جنسیت فقط برای دامنه صرفاً «شخص حقیقی» قابل ثبت است.");
        }
    }

    private static int criterionSort(Map<String, Object> row) {
        return switch (text(row.get("CRITERION_TYPE_CODE"))) {
            case "CUSTOMER_TYPE" -> 10;
            case "CUSTOMER_SEGMENT" -> 20;
            case "CUSTOMER_STATUS" -> 30;
            case "GENDER" -> 40;
            case "RESIDENCY_STATUS" -> 50;
            case "NATIONALITY_SCOPE" -> 60;
            case "CUSTOMER_TENURE" -> 70;
            case "ACCOUNT_TENURE" -> 80;
            default -> 100;
        };
    }

    public ProductWorkspace productWorkspace(long productId) {
        Map<String, Object> product = findById("PRODUCT", productId);
        TablePage versionPage = repository.search("PRODUCT_VERSION", null, 0, 200, "PRODUCT_ID", String.valueOf(productId));
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("PRODUCT_VERSION", versionPage.totalElements());
        counts.put("PRODUCT_LEGACY_MAPPING", repository.search("PRODUCT_LEGACY_MAPPING", null, 0, 1, "PRODUCT_ID", String.valueOf(productId)).totalElements());
        return new ProductWorkspace(product, versionPage.items(), counts);
    }

    public ProductVersionDefaults productVersionDefaults(long productId, String actor) {
        findById("PRODUCT", productId);
        return new ProductVersionDefaults(
                repository.previewNextProductVersionNo(productId),
                repository.currentDatabaseDate().toString(),
                repository.currentDatabaseDateTime().toString(),
                actorName(actor)
        );
    }

    private void validateGenderCriterionScope(Map<String, Object> criterion) {
        if (!"GENDER".equals(text(criterion.get("CRITERION_TYPE_CODE")))) return;
        long ruleId = number(criterion.get("ELIGIBILITY_RULE_ID"));
        if (ruleId <= 0) throw new ProductBuilderValidationException("قاعده اهلیت والد برای معیار جنسیت الزامی است.");
        TablePage criteria = repository.search("PRODUCT_ELIGIBILITY_CRITERION", null, 0, 500,
                "ELIGIBILITY_RULE_ID", String.valueOf(ruleId));
        List<String> customerTypes = criteria.items().stream()
                .filter(row -> "CUSTOMER_TYPE".equals(text(row.get("CRITERION_TYPE_CODE"))))
                .filter(row -> !row.containsKey("IS_ACTIVE") || number(row.get("IS_ACTIVE")) != 0)
                .map(row -> text(row.get("CRITERION_VALUE_CODE")))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (customerTypes.isEmpty() || customerTypes.stream().anyMatch(value -> !Set.of("PERSON", "INDIVIDUAL").contains(value))) {
            throw new ProductBuilderValidationException(
                    "معیار جنسیت فقط زمانی مجاز است که دامنه نوع مشتری صرفاً «شخص حقیقی» باشد."
            );
        }
    }

    /** A profile and an opening policy are owned by one product version. Use edit, not a duplicate INSERT. */
    private void validateVersionScopedCreate(String table, Map<String, Object> values) {
        String name = normalizeTable(table);
        if (!Set.of("DEPOSIT_PRODUCT_PROFILE", "DEPOSIT_PRODUCT_OPENING_RULE").contains(name)) return;
        long version = number(values.get("PRODUCT_VERSION_ID"));
        if (version <= 0) throw new ProductBuilderValidationException("نسخه محصول معتبر انتخاب نشده است.");
        if (repository.search(name, null, 0, 1, "PRODUCT_VERSION_ID", String.valueOf(version)).totalElements() > 0) {
            throw new ProductBuilderValidationException(name.equals("DEPOSIT_PRODUCT_PROFILE")
                    ? "برای این نسخه محصول پروفایل سپرده قبلاً ثبت شده است. رکورد موجود را ویرایش کنید یا نسخه محصول دیگری را برای کپی انتخاب کنید."
                    : "قاعده افتتاح برای این نسخه محصول قبلاً ثبت شده است. به جای افزودن رکورد تکراری، قاعده موجود را ویرایش کنید.");
        }
    }

    private void validateRelationshipTargets(String table, Map<String, Object> values) {
        if (!"PRODUCT_RELATIONSHIP".equals(normalizeTable(table))) return;
        long sourceVersion = number(values.get("SOURCE_PRODUCT_VERSION_ID"));
        long targetProduct = number(values.get("TARGET_PRODUCT_ID"));
        if (sourceVersion <= 0 || targetProduct <= 0)
            throw new ProductBuilderValidationException("نسخه محصول مبدأ و محصول مقصد الزامی هستند.");
        Map<String, Object> source = findById("PRODUCT_VERSION", sourceVersion);
        findById("PRODUCT", targetProduct);
        if (number(source.get("PRODUCT_ID")) == targetProduct)
            throw new ProductBuilderValidationException("محصول مقصد نباید همان محصول مبدأ باشد.");
        long targetVersion = number(values.get("TARGET_PRODUCT_VERSION_ID"));
        if (targetVersion > 0) {
            Map<String, Object> target = findById("PRODUCT_VERSION", targetVersion);
            if (number(target.get("PRODUCT_ID")) != targetProduct)
                throw new ProductBuilderValidationException("نسخه انتخاب‌شده متعلق به محصول مقصد نیست.");
        }
    }

    private Map<String, Object> prepareCreateValues(String table, Map<String, Object> values, String actor) {
        Map<String, Object> prepared = new LinkedHashMap<>(values);
        if ("PRODUCT_ORG_SCOPE".equals(normalizeTable(table))) {
            resolveOrgUnitCode(prepared);
            return prepared;
        }
        if ("LOAN_PRODUCT_PROFILE".equals(normalizeTable(table))) {
            String currency = loanProfileCurrency(number(prepared.get("PRODUCT_VERSION_ID")), number(prepared.get("LOAN_TYPE_ID")));
            String entered = text(prepared.get("CURRENCY_TYPE_CODE"));
            if (!entered.isBlank() && !currency.equals(entered)) {
                throw new ProductBuilderValidationException("ارز پروفایل قابل انتخاب مستقل نیست؛ ارز از هویت محصول تأمین می‌شود.");
            }
            prepared.put("CURRENCY_TYPE_CODE", currency);
            return prepared;
        }
        if ("DEPOSIT_PRODUCT_PROFILE".equals(normalizeTable(table))) {
            var identity = depositIdentity(number(prepared.get("PRODUCT_VERSION_ID")));
            String enteredGroup = text(prepared.get("DEPOSIT_GROUP_CODE"));
            String enteredType = text(prepared.get("DEPOSIT_TYPE_CODE"));
            if ((!enteredGroup.isBlank() && !enteredGroup.equals(identity[0])) ||
                    (!enteredType.isBlank() && !enteredType.equals(identity[1]))) {
                throw new ProductBuilderValidationException("گروه یا نوع سپرده با خانواده نسخه محصول سازگار نیست.");
            }
            prepared.put("DEPOSIT_GROUP_CODE", identity[0]);
            prepared.put("DEPOSIT_TYPE_CODE", identity[1]);
            return prepared;
        }
        if (!"PRODUCT_VERSION".equals(normalizeTable(table))) return prepared;

        long productId = number(prepared.get("PRODUCT_ID"));
        if (productId <= 0) throw new ProductBuilderValidationException("PRODUCT_ID برای ثبت نسخه محصول الزامی است.");
        prepared.remove("APPROVED_AT");
        prepared.remove("APPROVED_BY");
        prepared.put("VERSION_NO", repository.lockAndNextProductVersionNo(productId));
        applyApprovalAudit(prepared, null, actor);
        return prepared;
    }

    private Map<String, Object> prepareUpdateValues(String table, Map<String, Object> existing,
                                                     Map<String, Object> values, String actor) {
        Map<String, Object> prepared = new LinkedHashMap<>(values);
        if ("PRODUCT_ORG_SCOPE".equals(normalizeTable(table))) {
            Map<String, Object> scope = new LinkedHashMap<>(existing);
            scope.putAll(prepared);
            resolveOrgUnitCode(scope);
            prepared.put("ORG_UNIT_CODE", scope.get("ORG_UNIT_CODE"));
            return prepared;
        }
        if ("LOAN_PRODUCT_PROFILE".equals(normalizeTable(table))) {
            if (prepared.containsKey("PRODUCT_VERSION_ID")
                    && number(prepared.get("PRODUCT_VERSION_ID")) != number(existing.get("PRODUCT_VERSION_ID")))
                throw new ProductBuilderValidationException("نسخه پروفایل موجود ثابت است؛ از گزینه کپی به نسخه دیگر استفاده کنید.");
            String canonicalCurrency = loanProfileCurrency(number(existing.get("PRODUCT_VERSION_ID")),
                    number(prepared.getOrDefault("LOAN_TYPE_ID", existing.get("LOAN_TYPE_ID"))));
            if (!canonicalCurrency.equals(text(existing.get("CURRENCY_TYPE_CODE"))))
                throw new ProductBuilderValidationException("ارز پروفایل موجود با هویت محصول مغایرت دارد؛ اصلاح کنترل‌شده داده لازم است.");
            if (prepared.containsKey("CURRENCY_TYPE_CODE")
                    && !canonicalCurrency.equals(text(prepared.get("CURRENCY_TYPE_CODE"))))
                throw new ProductBuilderValidationException("ارز پروفایل مستقلاً قابل تغییر نیست.");
            prepared.remove("CURRENCY_TYPE_CODE");
            return prepared;
        }
        if ("DEPOSIT_PRODUCT_PROFILE".equals(normalizeTable(table))) {
            // Identity is immutable after creation, regardless of client-side disabled fields.
            for (String code : List.of("DEPOSIT_GROUP_CODE", "DEPOSIT_TYPE_CODE")) {
                if (prepared.containsKey(code) && !text(prepared.get(code)).equals(text(existing.get(code)))) {
                    throw new ProductBuilderValidationException("گروه و نوع سپرده پس از ثبت قابل تغییر نیستند.");
                }
                prepared.remove(code);
            }
            return prepared;
        }
        if (!"PRODUCT_VERSION".equals(normalizeTable(table))) return prepared;

        prepared.remove("VERSION_NO");
        prepared.remove("APPROVED_AT");
        prepared.remove("APPROVED_BY");
        applyApprovalAudit(prepared, text(existing.get("VERSION_STATUS_CODE")), actor);
        return prepared;
    }

    /** Never trust a typed ORG_UNIT_CODE; derive it from governed organizational selection. */
    private void resolveOrgUnitCode(Map<String, Object> values) {
        long orgUnitId = number(values.get("ORG_UNIT_ID"));
        values.put("ORG_UNIT_CODE", orgUnitId > 0 ? referenceOptionService.governedOrgUnitCode(orgUnitId) : null);
    }

    /** The FIX96 DPS product-family classification is the authoritative server-side identity. */
    private String[] depositIdentity(long versionId) {
        if (versionId <= 0) throw new ProductBuilderValidationException("نسخه محصول برای پروفایل سپرده الزامی است.");
        Map<String, Object> version = findById("PRODUCT_VERSION", versionId);
        Map<String, Object> product = findById("PRODUCT", number(version.get("PRODUCT_ID")));
        String family = text(product.get("PRODUCT_FAMILY_CODE"));
        return switch (family) {
            case "SAVINGS", "QARD_SAVINGS" -> new String[]{"QARD_HASAN", "SAVINGS"};
            case "CURRENT", "CURRENT_ACCOUNT" -> new String[]{"CURRENT", "CURRENT_NO_INTEREST"};
            case "SHORT_TERM_DEPOSIT" -> new String[]{"SHORT_TERM", "SHORT_TERM_DEPOSIT"};
            case "LONG_TERM_DEPOSIT" -> new String[]{"LONG_TERM", "LONG_TERM_DEPOSIT"};
            case "CERTIFICATE_OF_DEPOSIT" -> new String[]{"CERTIFICATE", "CERTIFICATE_OF_DEPOSIT"};
            default -> throw new ProductBuilderValidationException(
                    "برای خانواده این محصول، نگاشت گروه و نوع سپرده تعریف نشده است. اطلاعات پایه را اصلاح کنید.");
        };
    }

    private void applyApprovalAudit(Map<String, Object> values, String previousStatus, String actor) {
        String requestedStatus = values.containsKey("VERSION_STATUS_CODE")
                ? text(values.get("VERSION_STATUS_CODE")) : previousStatus;
        if ("APPROVED".equals(requestedStatus) && !"APPROVED".equals(previousStatus)) {
            values.put("APPROVED_AT", repository.currentDatabaseDateTime());
            values.put("APPROVED_BY", actor);
        } else if ("DRAFT".equals(requestedStatus) && "APPROVED".equals(previousStatus)) {
            values.put("APPROVED_AT", null);
            values.put("APPROVED_BY", null);
        }
    }

    private static String normalizeTable(String table) {
        return table == null ? "" : table.trim().toUpperCase();
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString().trim().toUpperCase();
    }

    private static long number(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(value.toString().trim()); }
        catch (NumberFormatException ex) { return 0; }
    }

    private static String actorName(String actor) {
        return actor == null || actor.isBlank() ? "prototype-ui" : actor.trim();
    }
}
