package com.behsazan.corebanking.cif.reference.application;

import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.CatalogResponse;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.ColumnDefinition;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.LookupOption;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.RecordResponse;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.RowResponse;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.TableDefinition;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.TableDescriptor;
import com.behsazan.corebanking.cif.reference.oracle.PartyReferenceRepository;
import com.behsazan.corebanking.shared.error.ReferenceNotFoundException;
import com.behsazan.corebanking.shared.error.ReferenceValidationException;
import com.behsazan.corebanking.shared.model.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PartyReferenceService {
    private final PartyReferenceMetadataRegistry registry;
    private final PartyReferenceRepository repository;

    public PartyReferenceService(PartyReferenceMetadataRegistry registry, PartyReferenceRepository repository) {
        this.registry = registry;
        this.repository = repository;
    }

    public CatalogResponse catalog() {
        return registry.catalog();
    }

    public TableDescriptor descriptor(String resource) {
        return registry.descriptor(resource);
    }

    public PageResponse<RowResponse> search(String resource, String text, Boolean active, int page, int size, String sortBy, String direction) {
        return repository.search(registry.require(resource), text, active, page, size, sortBy, direction);
    }

    public RecordResponse find(String resource, String key) {
        TableDefinition table = registry.require(resource);
        RowResponse row = repository.find(table, key).orElseThrow(() -> notFound(table));
        return new RecordResponse(row.key(), row.values());
    }

    @Transactional
    public RecordResponse create(String resource, Map<String, Object> request) {
        TableDefinition table = registry.require(resource);
        Map<String, Object> values = normalize(table, request, true);
        RowResponse row = repository.insert(table, values);
        return new RecordResponse(row.key(), row.values());
    }

    @Transactional
    public RecordResponse update(String resource, String key, Map<String, Object> request) {
        TableDefinition table = registry.require(resource);
        Map<String, Object> values = normalize(table, request, false);
        RowResponse row = repository.update(table, key, values)
                .orElseThrow(() -> new ReferenceValidationException(
                        "رکورد تغییر کرده یا دیگر وجود ندارد. اطلاعات را دوباره بارگذاری کنید.",
                        Map.of("RECORD_VERSION", "نسخه رکورد معتبر نیست.")));
        return new RecordResponse(row.key(), row.values());
    }

    @Transactional
    public void delete(String resource, String key) {
        TableDefinition table = registry.require(resource);
        if (!repository.delete(table, key)) throw notFound(table);
    }

    public List<LookupOption> lookup(String resource, String text, int limit) {
        return repository.lookup(registry.require(resource), text, limit);
    }

    private static Map<String, Object> normalize(TableDefinition table, Map<String, Object> request, boolean create) {
        Map<String, String> errors = new LinkedHashMap<>();
        Map<String, Object> values = new LinkedHashMap<>();
        for (String requestField : request.keySet()) {
            if (!table.hasColumn(requestField)) errors.put(requestField, "فیلد ناشناخته است.");
        }
        for (ColumnDefinition column : table.columns()) {
            Object raw;
            if (!create && table.pk().contains(column.name())) {
                raw = request.get(column.name());
            } else if (request.containsKey(column.name())) {
                raw = request.get(column.name());
            } else {
                raw = column.defaultValue();
            }
            Object value = normalizeColumn(column, raw, errors);
            if (column.required() && empty(value)) errors.put(column.name(), "مقدار این فیلد الزامی است.");
            values.put(column.name(), value);
        }
        validateRules(table, values, errors);
        if (!errors.isEmpty()) throw new ReferenceValidationException("اطلاعات فرم را اصلاح کنید.", errors);
        return values;
    }

    private static Object normalizeColumn(ColumnDefinition column, Object raw, Map<String, String> errors) {
        if (raw == null || raw instanceof String s && s.isBlank()) return null;
        try {
            return switch (column.type()) {
                case "NUMBER" -> new BigDecimal(raw.toString());
                case "DATE" -> raw instanceof LocalDate date ? date : LocalDate.parse(raw.toString());
                case "VARCHAR2" -> {
                    String value = raw.toString().trim();
                    if (column.maxLength() > 0 && value.length() > column.maxLength()) {
                        errors.put(column.name(), "حداکثر طول مجاز " + column.maxLength() + " کاراکتر است.");
                    }
                    yield value.isBlank() ? null : value;
                }
                default -> raw;
            };
        } catch (RuntimeException exception) {
            errors.put(column.name(), "نوع مقدار معتبر نیست.");
            return null;
        }
    }

    private static void validateRules(TableDefinition table, Map<String, Object> values, Map<String, String> errors) {
        BigDecimal active = number(values.get("IS_ACTIVE"));
        if (active != null && active.compareTo(BigDecimal.ZERO) != 0 && active.compareTo(BigDecimal.ONE) != 0) {
            errors.put("IS_ACTIVE", "وضعیت فعال فقط می‌تواند ۰ یا ۱ باشد.");
        }
        BigDecimal sort = number(values.get("SORT_ORDER"));
        if (sort != null && sort.signum() < 0) errors.put("SORT_ORDER", "ترتیب نمایش باید صفر یا بیشتر باشد.");
        BigDecimal version = number(values.get("RECORD_VERSION"));
        if (version != null && version.compareTo(BigDecimal.ONE) < 0) errors.put("RECORD_VERSION", "نسخه رکورد باید حداقل ۱ باشد.");
        LocalDate from = (LocalDate) values.get("VALID_FROM");
        LocalDate to = (LocalDate) values.get("VALID_TO");
        if (from != null && to != null && to.isBefore(from)) errors.put("VALID_TO", "تاریخ پایان اعتبار نمی‌تواند قبل از شروع باشد.");
        for (String pk : table.pk()) if (empty(values.get(pk))) errors.put(pk, "مقدار کلید اصلی کامل نیست.");
    }

    private static BigDecimal number(Object value) {
        return value instanceof BigDecimal b ? b : value == null ? null : new BigDecimal(value.toString());
    }

    private static boolean empty(Object value) {
        return value == null || value instanceof String s && s.isBlank();
    }

    private static ReferenceNotFoundException notFound(TableDefinition table) {
        return new ReferenceNotFoundException("رکورد مورد نظر در " + table.title() + " یافت نشد.");
    }
}
