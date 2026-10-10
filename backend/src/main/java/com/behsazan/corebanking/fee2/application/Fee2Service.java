package com.behsazan.corebanking.fee2.application;

import com.behsazan.corebanking.fee2.domain.Fee2Models.CalculationConfigRequest;
import com.behsazan.corebanking.fee2.domain.Fee2Models.CatalogResponse;
import com.behsazan.corebanking.fee2.domain.Fee2Models.GroupCatalogItem;
import com.behsazan.corebanking.fee2.domain.Fee2Models.SelectOption;
import com.behsazan.corebanking.fee2.domain.Fee2Models.TableCatalogItem;
import com.behsazan.corebanking.fee2.domain.Fee2Models.TableDescriptor;
import com.behsazan.corebanking.fee2.domain.Fee2Models.TablePage;
import com.behsazan.corebanking.fee2.domain.Fee2Models.StudioCatalogItem;
import com.behsazan.corebanking.fee2.domain.Fee2Models.StudioCreateRequest;
import com.behsazan.corebanking.fee2.domain.Fee2Models.StudioCreateResponse;
import com.behsazan.corebanking.fee2.domain.Fee2Models.StudioSummary;
import com.behsazan.corebanking.fee2.oracle.Fee2Repository;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class Fee2Service {
    private static final Set<String> VERSION_CHILDREN = Set.of("FEE_BINDING", "FEE_CONDITION", "FEE_MODIFIER", "FEE_TAX", "FEE_SHARE");
    private static final Map<String,String> NEXT_STATUS = Map.of(
            "DRAFT", "READY",
            "READY", "APPROVED",
            "APPROVED", "ACTIVE",
            "ACTIVE", "RETIRED"
    );

    private final Fee2Repository repository;
    private final JsonMapper jsonMapper;
    private final Fee2CalculationConfigValidator calculationValidator = new Fee2CalculationConfigValidator();

    public Fee2Service(Fee2Repository repository, JsonMapper jsonMapper) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    public CatalogResponse catalog() {
        Map<String,List<TableCatalogItem>> grouped = new LinkedHashMap<>();
        Map<String,String> titles = new LinkedHashMap<>();
        long totalRows = 0;
        int available = 0;
        for (Fee2Catalog.Entry entry : Fee2Catalog.entries()) {
            boolean exists = repository.tableExists(entry.tableName());
            long count = exists ? repository.count(entry.tableName()) : 0;
            if (exists) available++;
            totalRows += count;
            grouped.computeIfAbsent(entry.groupCode(), key -> new ArrayList<>()).add(new TableCatalogItem(
                    entry.tableName(), entry.title(), entry.groupCode(), entry.groupTitle(), entry.description(), count, exists, entry.editable()));
            titles.put(entry.groupCode(), entry.groupTitle());
        }
        List<GroupCatalogItem> groups = grouped.entrySet().stream().map(entry -> new GroupCatalogItem(
                entry.getKey(), titles.get(entry.getKey()), entry.getValue().stream().mapToLong(TableCatalogItem::rowCount).sum(),
                (int) entry.getValue().stream().filter(TableCatalogItem::available).count(), List.copyOf(entry.getValue())
        )).toList();
        return new CatalogResponse(repository.schemaName(), Fee2Catalog.entries().size(), available, totalRows, groups);
    }

    public TableDescriptor descriptor(String table) { return repository.descriptor(table); }

    public TablePage search(String table, String text, int page, int size, String filterColumn, String filterValue) {
        return repository.search(table, text, page, size, filterColumn, filterValue);
    }

    public Map<String,Object> findById(String table, String id) {
        return repository.findById(table,id).orElseThrow(() -> new Fee2ValidationException("رکورد یافت نشد: " + table + "/" + id));
    }

    public List<SelectOption> lookup(String table, String text, int limit) { return repository.lookup(table,text,limit); }

    public StudioSummary studioSummary(String scopeId) { return repository.studioSummary(scopeId); }

    public List<StudioCatalogItem> studioCatalog(String scopeId) { return repository.studioCatalog(scopeId); }

    @Transactional
    public StudioCreateResponse createStudioFee(StudioCreateRequest request, String actor) {
        if (request == null) throw new Fee2ValidationException("اطلاعات ایجاد کارمزد ارسال نشده است.");
        String scopeId = required(request.scopeId(), "دامنه کارمزد انتخاب نشده است.");
        String feeCode = upperRequired(request.feeCode(), "کد کارمزد الزامی است.");
        String nameFa = required(request.nameFa(), "عنوان فارسی کارمزد الزامی است.");
        String feeTypeCode = upperRequired(request.feeTypeCode(), "نوع کارمزد الزامی است.");
        String calculationType = upperRequired(request.calculationType(), "روش محاسبه انتخاب نشده است.");
        String effectiveFrom = required(request.effectiveFrom(), "شروع اعتبار نسخه الزامی است.");
        String serviceCode = upperRequired(request.serviceCode(), "کد خدمت برای نحوه اعمال الزامی است.");
        String eventType = upperRequired(request.eventType(), "نوع رویداد برای نحوه اعمال الزامی است.");
        String currency = request.currency() == null || request.currency().isBlank() ? "IRR" : request.currency().trim().toUpperCase(Locale.ROOT);
        String roundingMode = request.roundingMode() == null || request.roundingMode().isBlank() ? "HALF_UP" : request.roundingMode().trim().toUpperCase(Locale.ROOT);
        BigDecimal roundingQuantum = request.roundingQuantum() == null ? BigDecimal.ONE : request.roundingQuantum();
        String dayBasis = normalizeNullable(request.dayBasis());
        Map<String,Object> operatorConfig = request.operatorConfig() == null ? Map.of() : request.operatorConfig();

        calculationValidator.validate(calculationType, request.fixedAmount(), request.rateValue(), request.minAmount(), request.maxAmount(),
                roundingMode, roundingQuantum, dayBasis, operatorConfig);
        if (Boolean.TRUE.equals(request.taxEnabled()) && (request.taxRate() == null || request.taxRate().signum() < 0)) {
            throw new Fee2ValidationException("برای مالیات فعال، نرخ مالیات معتبر الزامی است.");
        }

        String normalizedActor = actorName(actor);
        Map<String,Object> definition = new LinkedHashMap<>();
        definition.put("SCOPE_ID", scopeId);
        definition.put("FEE_CODE", feeCode);
        definition.put("FEE_TYPE_CODE", feeTypeCode);
        definition.put("CATEGORY_CODE", upperNullable(request.categoryCode()));
        definition.put("NAME_FA", nameFa);
        definition.put("NAME_EN", normalizeNullable(request.nameEn()));
        definition.put("DESCRIPTION_FA", normalizeNullable(request.descriptionFa()));
        definition.put("OWNER_UNIT", normalizeNullable(request.ownerUnit()));
        String definitionId = repository.insert("FEE_DEFINITION", definition, normalizedActor);

        Map<String,Object> version = new LinkedHashMap<>();
        version.put("FEE_ID", definitionId);
        version.put("BINDING_LEVEL", request.bindingLevel() == null || request.bindingLevel().isBlank() ? "GENERAL" : request.bindingLevel().trim().toUpperCase(Locale.ROOT));
        version.put("PRIORITY", request.priority() == null ? 100 : request.priority());
        version.put("CALCULATION_TYPE", calculationType);
        version.put("CURRENCY", currency);
        version.put("BASIS_CODE", upperNullable(request.basisCode()));
        version.put("BASIS_UNIT", upperNullable(request.basisUnit()));
        version.put("FIXED_AMOUNT", request.fixedAmount());
        version.put("RATE_VALUE", request.rateValue());
        version.put("MIN_AMOUNT", request.minAmount());
        version.put("MAX_AMOUNT", request.maxAmount());
        version.put("ROUNDING_MODE", roundingMode);
        version.put("ROUNDING_QUANTUM", roundingQuantum);
        version.put("PERIOD_POLICY", upperNullable(request.periodPolicy()));
        version.put("DAY_BASIS", dayBasis);
        version.put("EFFECTIVE_FROM", effectiveFrom);
        version.put("CHANGE_REASON", "ایجاد اولیه از استودیوی FEE2");
        String versionId = repository.insert("FEE_VERSION", version, normalizedActor);

        CalculationConfigRequest calculation = new CalculationConfigRequest(0L, calculationType, upperNullable(request.basisCode()),
                upperNullable(request.basisUnit()), request.fixedAmount(), request.rateValue(), request.minAmount(), request.maxAmount(),
                roundingMode, roundingQuantum, upperNullable(request.periodPolicy()), dayBasis, operatorConfig);
        if (!repository.updateCalculation(versionId, calculation, writeConfig(operatorConfig), normalizedActor)) {
            throw new Fee2ValidationException("تنظیمات روش محاسبه نسخه اولیه ذخیره نشد.");
        }

        Map<String,Object> binding = new LinkedHashMap<>();
        binding.put("FEE_VERSION_ID", versionId);
        binding.put("SERVICE_CODE", serviceCode);
        binding.put("EVENT_TYPE", eventType);
        binding.put("OPERATION_CODE", upperNullable(request.operationCode()));
        binding.put("SEQUENCE_NO", 1);
        binding.put("ENABLED_FLAG", "Y");
        String bindingId = repository.insert("FEE_BINDING", binding, normalizedActor);

        String taxId = null;
        if (Boolean.TRUE.equals(request.taxEnabled())) {
            Map<String,Object> tax = new LinkedHashMap<>();
            tax.put("FEE_VERSION_ID", versionId);
            tax.put("TAX_CODE", request.taxCode() == null || request.taxCode().isBlank() ? "VAT" : request.taxCode().trim().toUpperCase(Locale.ROOT));
            tax.put("SEQUENCE_NO", 1);
            tax.put("TAX_TYPE", request.taxType() == null || request.taxType().isBlank() ? "EXCLUSIVE" : request.taxType().trim().toUpperCase(Locale.ROOT));
            tax.put("RATE_VALUE", request.taxRate());
            tax.put("BASIS_TYPE", request.taxBasisType() == null || request.taxBasisType().isBlank() ? "NET" : request.taxBasisType().trim().toUpperCase(Locale.ROOT));
            tax.put("ROUNDING_MODE", "HALF_UP");
            tax.put("ROUNDING_QUANTUM", BigDecimal.ONE);
            tax.put("ENABLED_FLAG", "Y");
            taxId = repository.insert("FEE_TAX", tax, normalizedActor);
        }
        return new StudioCreateResponse(definitionId, versionId, bindingId, taxId);
    }

    @Transactional
    public Map<String,Object> create(String table, Map<String,Object> values, String actor) {
        Fee2Catalog.Entry entry = Fee2Catalog.require(table);
        requireEditable(entry);
        if (VERSION_CHILDREN.contains(entry.tableName())) requireDraftVersion(stringValue(values,"FEE_VERSION_ID"));
        String id = repository.insert(entry.tableName(), values, actorName(actor));
        return findById(entry.tableName(), id);
    }

    @Transactional
    public Map<String,Object> update(String table, String id, Map<String,Object> values, String actor) {
        Fee2Catalog.Entry entry = Fee2Catalog.require(table);
        requireEditable(entry);
        Map<String,Object> current = findById(entry.tableName(), id);
        if ("FEE_VERSION".equals(entry.tableName())) {
            requireVersionStatus(current,"DRAFT","فقط نسخه پیش‌نویس قابل ویرایش است.");
            rejectParentChange(values, "FEE_ID", current.get("FEE_ID"), "تعریف کارمزد نسخه پس از ایجاد قابل تغییر نیست.");
        }
        if (VERSION_CHILDREN.contains(entry.tableName())) {
            requireDraftVersion(String.valueOf(current.get("FEE_VERSION_ID")));
            rejectParentChange(values, "FEE_VERSION_ID", current.get("FEE_VERSION_ID"), "انتقال جزء نسخه به نسخه دیگر مجاز نیست.");
        }
        Map<String,Object> payload = new LinkedHashMap<>(values);
        if (current.containsKey("ROW_VERSION")) payload.put("ROW_VERSION", current.get("ROW_VERSION"));
        if (!repository.update(entry.tableName(), id, payload, actorName(actor))) {
            throw new Fee2ValidationException("رکورد هم‌زمان توسط کاربر دیگری تغییر کرده است؛ فرم را تازه‌سازی کنید.");
        }
        return findById(entry.tableName(), id);
    }

    @Transactional
    public void delete(String table, String id) {
        Fee2Catalog.Entry entry = Fee2Catalog.require(table);
        requireEditable(entry);
        Map<String,Object> current = findById(entry.tableName(),id);
        if ("FEE_VERSION".equals(entry.tableName())) requireVersionStatus(current,"DRAFT","فقط نسخه پیش‌نویس قابل حذف است.");
        if (VERSION_CHILDREN.contains(entry.tableName())) requireDraftVersion(String.valueOf(current.get("FEE_VERSION_ID")));
        if (!repository.delete(entry.tableName(),id)) throw new Fee2ValidationException("رکورد یافت نشد: " + entry.tableName()+"/"+id);
    }

    @Transactional
    public Map<String,Object> updateCalculation(String id, CalculationConfigRequest request, String actor) {
        if (request == null || request.rowVersion() == null) {
            throw new Fee2ValidationException("نسخه رکورد برای کنترل همزمانی ارسال نشده است.");
        }
        Map<String,Object> current = findById("FEE_VERSION", id);
        requireVersionStatus(current, "DRAFT", "منطق محاسبه فقط در نسخه پیش‌نویس قابل تغییر است.");
        calculationValidator.validate(
                request.calculationType(), request.fixedAmount(), request.rateValue(),
                request.minAmount(), request.maxAmount(), request.roundingMode(),
                request.roundingQuantum(), request.dayBasis(), request.operatorConfig()
        );
        String configJson = writeConfig(request.operatorConfig());
        if (!repository.updateCalculation(id, request, configJson, actorName(actor))) {
            throw new Fee2ValidationException("نسخه کارمزد هم‌زمان تغییر کرده است؛ صفحه را تازه‌سازی کنید.");
        }
        return findById("FEE_VERSION", id);
    }

    @Transactional
    public Map<String,Object> transitionVersion(String id, String requestedTarget, String comment, String actor) {
        Map<String,Object> current = findById("FEE_VERSION",id);
        String currentStatus = String.valueOf(current.get("STATUS")).toUpperCase(Locale.ROOT);
        String expectedTarget = NEXT_STATUS.get(currentStatus);
        String target = requestedTarget == null ? "" : requestedTarget.trim().toUpperCase(Locale.ROOT);
        if (expectedTarget == null || !expectedTarget.equals(target)) {
            throw new Fee2ValidationException("گذار وضعیت مجاز نیست. وضعیت جاری «" + currentStatus + "» و وضعیت بعدی مجاز «" + (expectedTarget == null ? "-" : expectedTarget) + "» است.");
        }
        if ("READY".equals(target)) validateReady(current, id);
        String normalizedActor = actorName(actor);
        if (!repository.transitionVersion(id,currentStatus,target,normalizedActor)) {
            throw new Fee2ValidationException("وضعیت نسخه هم‌زمان تغییر کرده است؛ صفحه را تازه‌سازی کنید.");
        }
        if ("APPROVED".equals(target)) repository.addApproval(id,"APPROVE","APPROVED",normalizedActor,comment);
        return findById("FEE_VERSION",id);
    }


    private void validateReady(Map<String,Object> version, String versionId) {
        if (repository.countEnabledBindings(versionId) == 0) {
            throw new Fee2ValidationException("برای ارسال نسخه به بررسی، حداقل یک نحوه اعمال فعال لازم است.");
        }
        calculationValidator.validate(
                string(version.get("CALCULATION_TYPE")), decimal(version.get("FIXED_AMOUNT")), decimal(version.get("RATE_VALUE")),
                decimal(version.get("MIN_AMOUNT")), decimal(version.get("MAX_AMOUNT")), string(version.get("ROUNDING_MODE")),
                decimal(version.get("ROUNDING_QUANTUM")), string(version.get("DAY_BASIS")), readConfig(version.get("CALC_CONFIG_JSON"))
        );
    }

    private String writeConfig(Map<String,Object> config) {
        try {
            return jsonMapper.writeValueAsString(config == null ? Map.of() : config);
        } catch (JacksonException exception) {
            throw new Fee2ValidationException("تنظیمات روش محاسبه قابل ذخیره‌سازی نیست.");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> readConfig(Object raw) {
        if (raw == null || raw.toString().isBlank()) return Map.of();
        try {
            Object parsed = jsonMapper.readValue(raw.toString(), Map.class);
            return parsed instanceof Map<?,?> map ? (Map<String,Object>) map : Map.of();
        } catch (JacksonException exception) {
            throw new Fee2ValidationException("تنظیمات ذخیره‌شده روش محاسبه JSON معتبر نیست.");
        }
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal decimal) return decimal;
        String text = value.toString().trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) return null;
        try { return new BigDecimal(text.replace(",", "")); }
        catch (NumberFormatException exception) { return null; }
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    private static void rejectParentChange(Map<String,Object> values, String column, Object currentValue, String message) {
        Object requested = values.get(column);
        if (requested == null) return;
        if (!String.valueOf(currentValue).equals(String.valueOf(requested))) throw new Fee2ValidationException(message);
    }

    private void requireDraftVersion(String versionId) {
        if (versionId == null || versionId.isBlank() || "null".equalsIgnoreCase(versionId)) {
            throw new Fee2ValidationException("نسخه کارمزد انتخاب نشده است.");
        }
        Map<String,Object> version = findById("FEE_VERSION",versionId);
        requireVersionStatus(version,"DRAFT","جزئیات نسخه فقط در وضعیت پیش‌نویس قابل تغییر است.");
    }

    private static void requireVersionStatus(Map<String,Object> row, String status, String message) {
        if (!status.equalsIgnoreCase(String.valueOf(row.get("STATUS")))) throw new Fee2ValidationException(message);
    }
    private static void requireEditable(Fee2Catalog.Entry entry) {
        if (!entry.editable()) throw new Fee2ValidationException("این فرم فقط خواندنی است: " + entry.title());
    }
    private static String stringValue(Map<String,Object> values, String column) {
        Object direct = values.get(column);
        if (direct == null) {
            String[] parts = column.toLowerCase(Locale.ROOT).split("_");
            StringBuilder camel = new StringBuilder(parts[0]);
            for (int i=1;i<parts.length;i++) camel.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
            direct = values.get(camel.toString());
        }
        return direct == null ? null : direct.toString();
    }
    private static String required(String value, String message) {
        if (value == null || value.isBlank()) throw new Fee2ValidationException(message);
        return value.trim();
    }
    private static String upperRequired(String value, String message) { return required(value, message).toUpperCase(Locale.ROOT); }
    private static String normalizeNullable(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String upperNullable(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
    private static String actorName(String actor) { return actor == null || actor.isBlank() ? "prototype-ui" : actor.trim(); }
}
