package com.behsazan.corebanking.calendar2.reference.application;

import com.behsazan.corebanking.calendar2.eventrecurrence.application.Calendar2EventRecurrenceService;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.CanonicalDayFilterMeta;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.CanonicalDaySummary;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.CatalogResponse;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.FieldDescriptor;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.FieldType;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.LookupOption;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.RecordResponse;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.TableDescriptor;
import com.behsazan.corebanking.calendar2.reference.oracle.Calendar2ReferenceRepository;
import com.behsazan.corebanking.shared.error.ReferenceNotFoundException;
import com.behsazan.corebanking.shared.error.ReferenceValidationException;
import com.behsazan.corebanking.shared.model.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class Calendar2ReferenceService {
    private final Calendar2ReferenceRegistry registry;
    private final Calendar2ReferenceRepository repository;
    private final Calendar2EventRecurrenceService recurrenceService;

    public Calendar2ReferenceService(Calendar2ReferenceRegistry registry, Calendar2ReferenceRepository repository,
                                     Calendar2EventRecurrenceService recurrenceService) {
        this.registry = registry;
        this.repository = repository;
        this.recurrenceService = recurrenceService;
    }

    public CatalogResponse catalog() { return registry.catalog(); }
    public TableDescriptor descriptor(String resource) { return registry.require(resource); }

    public PageResponse<Map<String, Object>> search(String resource, String text, int page, int size, String sortBy, String direction) {
        return repository.search(registry.require(resource), text, page, size, sortBy, direction);
    }

    public PageResponse<CanonicalDaySummary> searchCanonicalDays(String text, Integer solarYear, Integer solarCentury,
                                                                    int page, int size, String sortBy, String direction) {
        registry.require("canonical-days");
        return repository.searchCanonicalDays(text, solarYear, solarCentury, page, size, sortBy, direction);
    }

    public CanonicalDayFilterMeta canonicalDayFilterMeta() {
        registry.require("canonical-days");
        return repository.canonicalDayFilterMeta();
    }

    public RecordResponse find(String resource, String key) {
        return repository.find(registry.require(resource), key)
                .orElseThrow(() -> new ReferenceNotFoundException("رکورد CAL2 یافت نشد."));
    }

    public List<LookupOption> lookup(String resource, String text, int limit) {
        registry.require(resource);
        return repository.lookup(resource, text, limit);
    }

    @Transactional
    public RecordResponse create(String resource, Map<String, Object> values) {
        TableDescriptor descriptor = registry.require(resource);
        if (!descriptor.allowCreate()) throw validation("این جدول CAL2 فقط‌خواندنی است.", "_form");
        validate(descriptor, values, true);
        validateEventOccurrenceCreate(descriptor, values);
        RecordResponse saved = repository.insert(descriptor, values);
        rebuildRecurrenceRuleIfNeeded(descriptor, saved);
        return saved;
    }

    @Transactional
    public RecordResponse update(String resource, String key, Map<String, Object> values) {
        TableDescriptor descriptor = registry.require(resource);
        if (!descriptor.allowUpdate()) throw validation("ویرایش این جدول CAL2 مجاز نیست.", "_form");
        validateEventOccurrenceMutation(descriptor, key, false, values);
        validate(descriptor, values, false);
        validateEventOccurrenceCreate(descriptor, values);
        RecordResponse saved = repository.update(descriptor, key, values)
                .orElseThrow(() -> new ReferenceNotFoundException("رکورد CAL2 برای ویرایش یافت نشد."));
        rebuildRecurrenceRuleIfNeeded(descriptor, saved);
        return saved;
    }

    @Transactional
    public void delete(String resource, String key) {
        TableDescriptor descriptor = registry.require(resource);
        if (!descriptor.allowDelete()) throw validation("حذف رکورد از این جدول CAL2 مجاز نیست.", "_form");
        validateEventOccurrenceMutation(descriptor, key, true, null);
        if (!repository.delete(descriptor, key)) throw new ReferenceNotFoundException("رکورد CAL2 برای حذف یافت نشد.");
    }

    private void validateEventOccurrenceCreate(TableDescriptor descriptor, Map<String, Object> values) {
        if (!"event-occurrences".equals(descriptor.resource())) return;
        String source = text(values.get("occurrenceSource"));
        if ("GENERATED".equalsIgnoreCase(source)) {
            throw validation("رخداد تولیدشده فقط توسط موتور قواعد مناسبت ساخته می‌شود.", "occurrenceSource");
        }
    }

    private void validateEventOccurrenceMutation(TableDescriptor descriptor, String key, boolean delete, Map<String, Object> values) {
        if (!"event-occurrences".equals(descriptor.resource())) return;
        RecordResponse existing = repository.find(descriptor, key)
                .orElseThrow(() -> new ReferenceNotFoundException("رخداد مناسبت یافت نشد."));
        String source = text(existing.values().get("occurrenceSource"));
        if ("GENERATED".equalsIgnoreCase(source)) {
            throw validation("رخدادهای تولیدشده قابل ویرایش یا حذف مستقیم نیستند؛ قاعده مناسبت را ویرایش یا بازسازی کنید.", "_form");
        }
        if ("OFFICIAL".equalsIgnoreCase(source)) {
            if (delete) {
                throw validation("رخداد رسمی از این فرم حذف نمی‌شود؛ در صورت نیاز وضعیت آن را اصلاح کنید.", "_form");
            }
            String requestedSource = values == null ? null : text(values.get("occurrenceSource"));
            if (requestedSource != null && !"OFFICIAL".equalsIgnoreCase(requestedSource)) {
                throw validation("منشأ رخداد رسمی قابل تنزل به دستی نیست.", "occurrenceSource");
            }
        }
    }

    private void rebuildRecurrenceRuleIfNeeded(TableDescriptor descriptor, RecordResponse saved) {
        if (!"event-recurrence-rules".equals(descriptor.resource())) return;
        recurrenceService.rebuild(Long.parseLong(saved.key()));
    }

    private static void validate(TableDescriptor descriptor, Map<String, Object> values, boolean create) {
        LinkedHashMap<String, String> errors = new LinkedHashMap<>();
        for (FieldDescriptor field : descriptor.fields()) {
            if (field.readOnly()) continue;
            if (!create && field.key()) continue;
            Object value = values.get(field.apiName());
            if (field.required() && blank(value)) {
                errors.put(field.apiName(), "مقدار «" + field.label() + "» الزامی است.");
                continue;
            }
            if (value == null) continue;
            if (field.maxLength() != null && value.toString().length() > field.maxLength()) {
                errors.put(field.apiName(), "طول «" + field.label() + "» بیش از " + field.maxLength() + " کاراکتر است.");
            }
            if (field.type() == FieldType.SELECT && !field.options().isEmpty()) {
                boolean valid = field.options().stream().anyMatch(option -> String.valueOf(option.value()).equals(String.valueOf(value)));
                if (!valid) errors.put(field.apiName(), "مقدار انتخاب‌شده برای «" + field.label() + "» معتبر نیست.");
            }
        }
        range(values, "validFrom", "validTo", "بازه اعتبار", errors);
        range(values, "startTime", "endTime", "بازه زمانی", errors);
        range(values, "openTime", "closeTime", "ساعات کاری", errors);
        if ("event-recurrence-rules".equals(descriptor.resource())) validateEventRecurrenceRule(values, errors);
        if (!errors.isEmpty()) throw new ReferenceValidationException("اطلاعات فرم CAL2 را اصلاح کنید.", errors);
    }

    private static void validateEventRecurrenceRule(Map<String, Object> values, Map<String, String> errors) {
        String ruleType = text(values.get("ruleType"));
        Integer year = integer(values.get("yearNo"));
        Integer month = integer(values.get("monthNo"));
        Integer day = integer(values.get("dayNo"));
        Integer startYear = integer(values.get("startYearNo"));
        Integer endYear = integer(values.get("endYearNo"));

        if (month != null && (month < 1 || month > 13)) errors.put("monthNo", "ماه باید بین ۱ تا ۱۳ باشد.");
        if (day != null && (day < 1 || day > 31)) errors.put("dayNo", "روز باید بین ۱ تا ۳۱ باشد.");

        if ("ONE_TIME_DATE".equals(ruleType)) {
            if (year == null) errors.put("yearNo", "برای قاعده یک‌باره، سال وقوع الزامی است.");
            if (startYear != null || endYear != null) {
                errors.put("startYearNo", "برای قاعده یک‌باره، سال شروع/پایان وارد نکنید.");
            }
        } else if ("ANNUAL_FIXED_DATE".equals(ruleType)) {
            if (year != null) errors.put("yearNo", "برای قاعده سالانه، سال وقوع را خالی بگذارید.");
            if (startYear != null && endYear != null && startYear > endYear) {
                errors.put("endYearNo", "سال پایان نمی‌تواند قبل از سال شروع باشد.");
            }
        }
    }

    private static Integer integer(Object value) {
        if (blank(value)) return null;
        try { return Integer.valueOf(value.toString()); }
        catch (NumberFormatException ex) { return null; }
    }

    private static String text(Object value) { return value == null ? null : value.toString(); }

    private static void range(Map<String, Object> values, String from, String to, String label, Map<String, String> errors) {
        Object left = values.get(from);
        Object right = values.get(to);
        if (blank(left) || blank(right)) return;
        if (left.toString().compareTo(right.toString()) > 0) errors.put(to, label + " معتبر نیست؛ پایان قبل از شروع است.");
    }

    private static boolean blank(Object value) { return value == null || (value instanceof String s && s.isBlank()); }
    private static ReferenceValidationException validation(String message, String field) {
        return new ReferenceValidationException(message, Map.of(field, message));
    }
}
