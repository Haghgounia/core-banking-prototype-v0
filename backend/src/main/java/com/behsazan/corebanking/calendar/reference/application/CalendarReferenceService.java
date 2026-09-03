package com.behsazan.corebanking.calendar.reference.application;

import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.CatalogResponse;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.FieldDescriptor;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.FieldType;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.LookupOption;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.RecordResponse;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.SolarYearContext;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.TableDescriptor;
import com.behsazan.corebanking.calendar.reference.oracle.CalendarReferenceRepository;
import com.behsazan.corebanking.shared.error.ReferenceNotFoundException;
import com.behsazan.corebanking.shared.error.ReferenceValidationException;
import com.behsazan.corebanking.shared.model.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CalendarReferenceService {
    private final CalendarReferenceRegistry registry;
    private final CalendarReferenceRepository repository;

    public CalendarReferenceService(CalendarReferenceRegistry registry, CalendarReferenceRepository repository) {
        this.registry = registry;
        this.repository = repository;
    }

    public CatalogResponse catalog() {
        return registry.catalog();
    }

    public TableDescriptor descriptor(String resource) {
        return registry.require(resource);
    }

    public SolarYearContext solarYearContext() {
        return repository.solarYearContext();
    }

    public PageResponse<Map<String, Object>> search(String resource, String text, Integer solarYear, int page, int size,
                                                     String sortBy, String direction) {
        TableDescriptor descriptor = registry.require(resource);
        return switch (resource) {
            case "calendar-days" -> repository.searchCalendarDays(text, solarYear, page, size, sortBy, direction);
            case "business-calendar-days" -> repository.searchBusinessCalendarDays(text, solarYear, page, size, sortBy, direction);
            case "occasion-rules" -> repository.searchOccasionRules(text, page, size, sortBy, direction);
            case "occasion-occurrences" -> repository.searchOccasionOccurrences(text, solarYear, page, size, sortBy, direction);
            case "calendar-day-occasions" -> repository.searchCalendarDayOccasions(text, solarYear, page, size, sortBy, direction);
            default -> repository.search(descriptor, text, solarYear, page, size, sortBy, direction);
        };
    }

    public RecordResponse find(String resource, String key) {
        TableDescriptor descriptor = registry.require(resource);
        return repository.find(descriptor, key)
                .orElseThrow(() -> new ReferenceNotFoundException("رکورد تقویم یافت نشد."));
    }

    public List<LookupOption> lookup(String resource, String text, int limit) {
        registry.require(resource);
        return repository.lookup(resource, text, limit);
    }

    @Transactional
    public RecordResponse create(String resource, Map<String, Object> values) {
        TableDescriptor descriptor = registry.require(resource);
        if (!descriptor.allowCreate()) {
            throw validation("این جدول از طریق فرم فقط‌خواندنی است و ایجاد رکورد در آن مجاز نیست.", "_form");
        }
        validate(descriptor, values, true);
        return repository.insert(descriptor, values);
    }

    @Transactional
    public RecordResponse update(String resource, String key, Map<String, Object> values) {
        TableDescriptor descriptor = registry.require(resource);
        if (!descriptor.allowUpdate()) {
            throw validation("ویرایش این جدول از طریق فرم مجاز نیست.", "_form");
        }
        validate(descriptor, values, false);
        return repository.update(descriptor, key, values)
                .orElseThrow(() -> new ReferenceNotFoundException("رکورد تقویم برای ویرایش یافت نشد."));
    }

    @Transactional
    public void delete(String resource, String key) {
        TableDescriptor descriptor = registry.require(resource);
        if (!descriptor.allowDelete()) {
            throw validation("حذف رکورد از این جدول مجاز نیست.", "_form");
        }
        if (!repository.delete(descriptor, key)) {
            throw new ReferenceNotFoundException("رکورد تقویم برای حذف یافت نشد.");
        }
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
            if (field.type() == FieldType.TIME && !blank(value) && !value.toString().matches("(?:[01]\\d|2[0-3]):[0-5]\\d")) {
                errors.put(field.apiName(), "زمان باید با قالب HH:mm وارد شود.");
            }
        }

        checkComparableRange(values, "validFromDate", "validToDate", "بازه اعتبار تقویم کاری", errors);
        checkComparableRange(values, "validFrom", "validTo", "بازه اعتبار", errors);
        checkNumericRange(values, "effectiveFromYear", "effectiveToYear", "بازه سال قاعده مناسبت", errors);
        checkNumericRange(values, "startDayId", "endDayId", "بازه رخداد مناسبت", errors);

        if (!errors.isEmpty()) throw new ReferenceValidationException("اطلاعات فرم تقویم را اصلاح کنید.", errors);
    }

    private static void checkComparableRange(Map<String, Object> values, String from, String to, String label,
                                             Map<String, String> errors) {
        Object left = values.get(from);
        Object right = values.get(to);
        if (blank(left) || blank(right)) return;
        if (left.toString().compareTo(right.toString()) > 0) {
            errors.put(to, label + " معتبر نیست؛ مقدار پایان نمی‌تواند قبل از شروع باشد.");
        }
    }

    private static void checkNumericRange(Map<String, Object> values, String from, String to, String label,
                                          Map<String, String> errors) {
        Object left = values.get(from);
        Object right = values.get(to);
        if (blank(left) || blank(right)) return;
        try {
            if (new BigDecimal(left.toString()).compareTo(new BigDecimal(right.toString())) > 0) {
                errors.put(to, label + " معتبر نیست؛ مقدار پایان نمی‌تواند کمتر از شروع باشد.");
            }
        } catch (NumberFormatException ignored) {
            errors.put(to, "مقدار عددی معتبر وارد کنید.");
        }
    }

    private static boolean blank(Object value) {
        return value == null || (value instanceof String s && s.isBlank());
    }

    private static ReferenceValidationException validation(String message, String field) {
        return new ReferenceValidationException(message, Map.of(field, message));
    }
}
