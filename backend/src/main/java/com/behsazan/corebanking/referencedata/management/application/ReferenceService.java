package com.behsazan.corebanking.referencedata.management.application;

import com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorRegistry;
import com.behsazan.corebanking.referencedata.descriptor.domain.FieldType;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceFieldDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceTableDescriptor;
import com.behsazan.corebanking.referencedata.management.domain.AncestorValue;
import com.behsazan.corebanking.referencedata.management.domain.LookupOption;
import com.behsazan.corebanking.referencedata.management.domain.ReferenceRecordResponse;
import com.behsazan.corebanking.referencedata.management.domain.ReferenceSearchQuery;
import com.behsazan.corebanking.shared.error.ReferenceNotFoundException;
import com.behsazan.corebanking.shared.error.ReferenceValidationException;
import com.behsazan.corebanking.shared.model.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReferenceService {
    private static final Logger log = LoggerFactory.getLogger(ReferenceService.class);

    private final ReferenceDescriptorRegistry registry;
    private final ReferenceRepository repository;

    public ReferenceService(ReferenceDescriptorRegistry registry, ReferenceRepository repository) {
        this.registry = registry;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> search(String resource, ReferenceSearchQuery query) {
        return repository.search(registry.require(resource), query);
    }

    @Transactional(readOnly = true)
    public ReferenceRecordResponse findById(String resource, long id) {
        ReferenceTableDescriptor descriptor = registry.require(resource);
        Map<String, Object> values = requireRecord(descriptor, id);
        return new ReferenceRecordResponse(id, values, ancestors(descriptor, values));
    }

    @Transactional
    public ReferenceRecordResponse create(String resource, Map<String, Object> request, long actorId) {
        ReferenceTableDescriptor descriptor = registry.require(resource);
        Map<String, Object> values = validate(descriptor, request);
        long id = repository.insert(descriptor, values, actorId);
        return findById(resource, id);
    }

    @Transactional
    public ReferenceRecordResponse update(String resource, long id, Map<String, Object> request, long actorId) {
        ReferenceTableDescriptor descriptor = registry.require(resource);
        Map<String, Object> values = validate(descriptor, request);
        descriptor.optionalField("recordVersion").ifPresent(field -> {
            Object raw = request.get("recordVersion");
            if (raw == null) {
                throw new ReferenceValidationException(
                        "نسخه رکورد برای ویرایش ارسال نشده است.",
                        Map.of("recordVersion", "صفحه را دوباره بارگذاری کنید.")
                );
            }
            try {
                values.put("recordVersion", new BigDecimal(raw.toString()));
            } catch (NumberFormatException exception) {
                throw new ReferenceValidationException(
                        "نسخه رکورد معتبر نیست.",
                        Map.of("recordVersion", "صفحه را دوباره بارگذاری کنید.")
                );
            }
        });
        if (!repository.update(descriptor, id, values, actorId)) {
            if (descriptor.optionalField("recordVersion").isPresent()) {
                throw new ReferenceValidationException(
                        "رکورد توسط کاربر دیگری تغییر کرده یا دیگر وجود ندارد.",
                        Map.of("recordVersion", "اطلاعات را دوباره بارگذاری کنید.")
                );
            }
            throw notFound(descriptor, id);
        }
        return findById(resource, id);
    }

    @Transactional
    public void delete(String resource, long id) {
        ReferenceTableDescriptor descriptor = registry.require(resource);
        if (!repository.delete(descriptor, id)) {
            throw notFound(descriptor, id);
        }
    }

    @Transactional(readOnly = true)
    public List<LookupOption> lookup(String resource, Long parentId, String text, int limit) {
        return repository.lookup(registry.require(resource), parentId, text, limit);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> dashboardCounts() {
        Map<String, Long> result = new LinkedHashMap<>();
        RuntimeException firstFailure = null;
        for (ReferenceTableDescriptor descriptor : registry.all()) {
            try {
                result.put(descriptor.resource(), repository.count(descriptor));
            } catch (RuntimeException exception) {
                if (firstFailure == null) firstFailure = exception;
                log.warn("Dashboard count failed for {}.{} ({})",
                        descriptor.schemaName(), descriptor.tableName(), descriptor.resource(), exception);
            }
        }
        if (result.isEmpty() && firstFailure != null) throw firstFailure;
        return result;
    }

    private Map<String, Object> validate(ReferenceTableDescriptor descriptor, Map<String, Object> request) {
        Map<String, String> errors = new LinkedHashMap<>();
        Map<String, Object> normalized = new LinkedHashMap<>();

        for (String key : request.keySet()) {
            if (descriptor.optionalField(key).isEmpty()) {
                errors.put(key, "فیلد ناشناخته است.");
            }
        }

        for (ReferenceFieldDescriptor field : descriptor.editableFields()) {
            Object raw = request.containsKey(field.apiName()) ? request.get(field.apiName()) : field.defaultValue();
            Object value = normalize(field, raw, errors);
            if (field.required() && isEmpty(value)) {
                errors.put(field.apiName(), "مقدار این فیلد الزامی است.");
            }
            normalized.put(field.apiName(), value);
        }

        validateCrossFieldRules(descriptor, normalized, errors);
        if (!errors.isEmpty()) {
            throw new ReferenceValidationException("اطلاعات فرم را اصلاح کنید.", errors);
        }
        return normalized;
    }

    private static void validateCrossFieldRules(
            ReferenceTableDescriptor descriptor,
            Map<String, Object> values,
            Map<String, String> errors
    ) {
        if (descriptor.optionalField("validFrom").isPresent()
                && descriptor.optionalField("validTo").isPresent()) {
            LocalDate validFrom = (LocalDate) values.get("validFrom");
            LocalDate validTo = (LocalDate) values.get("validTo");
            if (validFrom != null && validTo != null && validTo.isBefore(validFrom)) {
                errors.put("validTo", "پایان اعتبار نباید قبل از شروع اعتبار باشد.");
            }
        }
        if (descriptor.optionalField("parentCode").isPresent()) {
            Object code = values.get("code");
            Object parentCode = values.get("parentCode");
            if (code != null && parentCode != null
                    && code.toString().equalsIgnoreCase(parentCode.toString())) {
                errors.put("parentCode", "کد والد نمی‌تواند با کد رکورد یکسان باشد.");
            }
        }
    }

    private Object normalize(ReferenceFieldDescriptor field, Object raw, Map<String, String> errors) {
        if (raw == null) {
            return null;
        }
        try {
            return switch (field.type()) {
                case TEXT -> normalizeText(field, raw.toString(), errors);
                case NUMBER, LOOKUP -> new BigDecimal(raw.toString());
                case BOOLEAN -> normalizeBoolean(raw);
                case SELECT -> normalizeSelect(field, raw);
                case DATE -> raw instanceof LocalDate value ? value : LocalDate.parse(raw.toString());
                case TIMESTAMP -> raw;
            };
        } catch (RuntimeException exception) {
            errors.put(field.apiName(), "نوع مقدار معتبر نیست.");
            return null;
        }
    }

    private static String normalizeText(ReferenceFieldDescriptor field, String raw, Map<String, String> errors) {
        String value = raw.trim();
        if (field.maxLength() != null && value.length() > field.maxLength()) {
            errors.put(field.apiName(), "حداکثر طول مجاز " + field.maxLength() + " کاراکتر است.");
        }
        return value.isEmpty() ? null : value;
    }

    private static Boolean normalizeBoolean(Object raw) {
        if (raw instanceof Boolean value) {
            return value;
        }
        String value = raw.toString();
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    private static Object normalizeSelect(ReferenceFieldDescriptor field, Object raw) {
        BigDecimal value = new BigDecimal(raw.toString());
        boolean accepted = field.options().isEmpty() || field.options().stream()
                .map(option -> new BigDecimal(option.value().toString()))
                .anyMatch(optionValue -> optionValue.compareTo(value) == 0);
        if (!accepted) {
            throw new IllegalArgumentException("Unsupported option");
        }
        return value;
    }

    private List<AncestorValue> ancestors(ReferenceTableDescriptor descriptor, Map<String, Object> values) {
        List<AncestorValue> result = new ArrayList<>();
        ReferenceTableDescriptor currentDescriptor = descriptor;
        Map<String, Object> currentValues = values;

        while (currentDescriptor.parent() != null) {
            Object parentRaw = currentValues.get(currentDescriptor.parent().apiField());
            if (parentRaw == null) {
                break;
            }
            long parentId = new BigDecimal(parentRaw.toString()).longValueExact();
            ReferenceTableDescriptor parentDescriptor = registry.require(currentDescriptor.parent().resource());
            Map<String, Object> parentValues = requireRecord(parentDescriptor, parentId);
            String label = String.valueOf(parentValues.get(parentDescriptor.nameApiName()));
            result.add(new AncestorValue(parentDescriptor.resource(), parentId, label));
            currentDescriptor = parentDescriptor;
            currentValues = parentValues;
        }

        Collections.reverse(result);
        return result;
    }

    private Map<String, Object> requireRecord(ReferenceTableDescriptor descriptor, long id) {
        return repository.findById(descriptor, id).orElseThrow(() -> notFound(descriptor, id));
    }

    private static ReferenceNotFoundException notFound(ReferenceTableDescriptor descriptor, long id) {
        return new ReferenceNotFoundException(descriptor.title() + " با شناسه " + id + " یافت نشد.");
    }

    private static boolean isEmpty(Object value) {
        return value == null || value instanceof String text && text.isBlank();
    }
}
