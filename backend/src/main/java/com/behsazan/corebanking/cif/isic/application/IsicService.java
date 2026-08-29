package com.behsazan.corebanking.cif.isic.application;

import com.behsazan.corebanking.cif.isic.domain.IsicModels.ActivityDetail;
import com.behsazan.corebanking.cif.isic.domain.IsicModels.ActivityLookup;
import com.behsazan.corebanking.cif.isic.domain.IsicModels.ActivityRequest;
import com.behsazan.corebanking.cif.isic.domain.IsicModels.ActivityRow;
import com.behsazan.corebanking.cif.isic.domain.IsicModels.ReleaseLookup;
import com.behsazan.corebanking.cif.isic.domain.IsicModels.ReleaseRequest;
import com.behsazan.corebanking.cif.isic.domain.IsicModels.ReleaseRow;
import com.behsazan.corebanking.cif.isic.oracle.IsicRepository;
import com.behsazan.corebanking.shared.error.ReferenceNotFoundException;
import com.behsazan.corebanking.shared.error.ReferenceValidationException;
import com.behsazan.corebanking.shared.model.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class IsicService {
    private static final String ACTOR = "ISIC_UI";
    private static final Set<String> DATASET_STATUSES = Set.of("DRAFT", "PARTIAL", "COMPLETE", "RETIRED");
    private static final Set<String> LEVELS = Set.of("SECTION", "DIVISION", "GROUP", "CLASS", "NATIONAL_SUBCLASS");
    private static final Set<String> TRANSLATIONS = Set.of("OFFICIAL", "BANK_VERIFIED", "BANK_TRANSLATED", "NOT_AVAILABLE");
    private static final Pattern COUNTRY = Pattern.compile("^[A-Z]{2}$");
    private static final Pattern SECTION = Pattern.compile("^[A-U]$");
    private static final Pattern DIVISION = Pattern.compile("^[0-9]{2}$");
    private static final Pattern GROUP = Pattern.compile("^[0-9]{3}$");
    private static final Pattern CLASS = Pattern.compile("^[0-9]{4}$");
    private static final Pattern NATIONAL = Pattern.compile("^[0-9A-Z./-]+$");

    private final IsicRepository repository;

    public IsicService(IsicRepository repository) { this.repository = repository; }

    public PageResponse<ReleaseRow> searchReleases(String text, Boolean active, int page, int size, String sortBy, String direction) {
        return repository.searchReleases(text, active, page, size, sortBy, direction);
    }

    public List<ReleaseLookup> releaseLookup(boolean includeInactive) { return repository.releaseLookup(includeInactive); }
    public ReleaseRow findRelease(long id) { return repository.findRelease(id).orElseThrow(() -> notFound("نسخه ISIC")); }

    @Transactional
    public ReleaseRow createRelease(ReleaseRequest request) {
        ReleaseRequest normalized = normalizeRelease(request, false);
        long id = repository.insertRelease(normalized, ACTOR);
        return findRelease(id);
    }

    @Transactional
    public ReleaseRow updateRelease(long id, ReleaseRequest request) {
        ReleaseRequest normalized = normalizeRelease(request, true);
        if (!repository.updateRelease(id, normalized, ACTOR)) throw stale();
        return findRelease(id);
    }

    @Transactional
    public void deleteRelease(long id) {
        if (!repository.deleteRelease(id)) throw notFound("نسخه ISIC");
    }

    public PageResponse<ActivityRow> searchActivities(Long releaseId, Long parentActivityId, String levelCode, String text,
                                                      Boolean active, Boolean selectable, int page, int size, String sortBy, String direction) {
        return repository.searchActivities(releaseId, parentActivityId, upper(levelCode), text, active, selectable, page, size, sortBy, direction);
    }

    public ActivityDetail findActivity(long id) { return repository.findActivity(id).orElseThrow(() -> notFound("فعالیت ISIC")); }

    public List<ActivityLookup> activityLookup(long releaseId, String text, boolean selectableOnly, int limit) {
        if (!repository.releaseExists(releaseId)) throw notFound("نسخه ISIC");
        return repository.activityLookup(releaseId, text, selectableOnly, limit);
    }

    @Transactional
    public ActivityDetail createActivity(ActivityRequest request) {
        ActivityRequest normalized = normalizeActivity(request, false, null);
        long id = repository.insertActivity(normalized, ACTOR);
        return findActivity(id);
    }

    @Transactional
    public ActivityDetail updateActivity(long id, ActivityRequest request) {
        ActivityRequest normalized = normalizeActivity(request, true, id);
        if (!repository.updateActivity(id, normalized, ACTOR)) throw stale();
        return findActivity(id);
    }

    @Transactional
    public void deleteActivity(long id) {
        if (!repository.deleteActivity(id)) throw notFound("فعالیت ISIC");
    }

    private ReleaseRequest normalizeRelease(ReleaseRequest r, boolean update) {
        Map<String, String> errors = new LinkedHashMap<>();
        String classification = upper(text(r.classificationCode()));
        if (classification == null) classification = "ISIC";
        String revision = required(r.revisionCode(), "revisionCode", errors);
        String variant = upper(required(r.variantCode(), "variantCode", errors));
        String country = upper(text(r.countryCode()));
        if (country != null && !COUNTRY.matcher(country).matches()) errors.put("countryCode", "کد کشور باید ISO دوحرفی باشد.");
        String nameFa = required(r.nameFa(), "nameFa", errors);
        String nameEn = required(r.nameEn(), "nameEn", errors);
        String authority = required(r.sourceAuthority(), "sourceAuthority", errors);
        String status = upper(text(r.datasetStatusCode()));
        if (status == null) status = "DRAFT";
        if (!DATASET_STATUSES.contains(status)) errors.put("datasetStatusCode", "وضعیت Dataset معتبر نیست.");
        validateDates(r.validFrom(), r.validTo(), errors);
        if (update && (r.recordVersion() == null || r.recordVersion() < 1)) errors.put("recordVersion", "نسخه رکورد معتبر نیست.");
        if (!errors.isEmpty()) throw new ReferenceValidationException("اطلاعات نسخه ISIC را اصلاح کنید.", errors);
        return new ReleaseRequest(classification, revision, variant, country, nameFa, nameEn, authority, text(r.sourceUri()),
                r.publicationDate(), status, Boolean.TRUE.equals(r.current()), r.active() == null || Boolean.TRUE.equals(r.active()),
                r.validFrom(), r.validTo(), r.recordVersion());
    }

    private ActivityRequest normalizeActivity(ActivityRequest r, boolean update, Long currentId) {
        Map<String, String> errors = new LinkedHashMap<>();
        Long releaseId = r.isicReleaseId();
        if (releaseId == null || !repository.releaseExists(releaseId)) errors.put("isicReleaseId", "نسخه ISIC معتبر انتخاب نشده است.");

        String level = upper(text(r.levelCode()));
        if (!LEVELS.contains(level)) errors.put("levelCode", "سطح ISIC معتبر نیست.");
        int levelNo = levelNo(level);
        String code = upper(required(r.isicCode(), "isicCode", errors));
        validateCode(level, code, errors);

        Long parentId = r.parentActivityId();
        if ("SECTION".equals(level) && parentId != null) errors.put("parentActivityId", "سطح بخش والد ندارد.");
        if (level != null && !"SECTION".equals(level) && parentId == null) errors.put("parentActivityId", "برای این سطح انتخاب والد الزامی است.");
        if (releaseId != null && parentId != null) {
            ActivityDetail parent = repository.findActivityInRelease(releaseId, parentId).orElse(null);
            if (parent == null) {
                errors.put("parentActivityId", "والد انتخاب‌شده متعلق به همین نسخه ISIC نیست.");
            } else {
                if (currentId != null && currentId.equals(parent.isicActivityId())) errors.put("parentActivityId", "یک رکورد نمی‌تواند والد خودش باشد.");
                if (!validParent(level, parent.levelCode())) errors.put("parentActivityId", "سطح والد با سطح رکورد سازگار نیست.");
                if (levelNo > 0 && parent.levelNo() != null && !"NATIONAL_SUBCLASS".equals(level) && parent.levelNo() != levelNo - 1) {
                    errors.put("parentActivityId", "سطح والد باید دقیقاً یک سطح بالاتر باشد.");
                }
            }
        }

        String nameFa = required(r.nameFa(), "nameFa", errors);
        String nameEn = required(r.nameEn(), "nameEn", errors);
        String translation = upper(text(r.translationStatusCode()));
        if (translation == null) translation = "BANK_TRANSLATED";
        if (!TRANSLATIONS.contains(translation)) errors.put("translationStatusCode", "وضعیت ترجمه معتبر نیست.");
        if ("NOT_AVAILABLE".equals(translation)) errors.put("translationStatusCode", "در مدل جدید عنوان فارسی الزامی است؛ وضعیت فاقد ترجمه مجاز نیست.");

        boolean selectable = "CLASS".equals(level) || "NATIONAL_SUBCLASS".equals(level);
        validateDates(r.validFrom(), r.validTo(), errors);
        if (r.sortOrder() != null && r.sortOrder() < 0) errors.put("sortOrder", "ترتیب نمایش نمی‌تواند منفی باشد.");
        if (update && (r.recordVersion() == null || r.recordVersion() < 1)) errors.put("recordVersion", "نسخه رکورد معتبر نیست.");
        if (!errors.isEmpty()) throw new ReferenceValidationException("اطلاعات فعالیت ISIC را اصلاح کنید.", errors);

        return new ActivityRequest(releaseId, parentId, code, level, levelNo, nameFa, nameEn,
                text(r.descriptionFa()), text(r.descriptionEn()), text(r.inclusionsFa()), text(r.inclusionsEn()),
                text(r.exclusionsFa()), text(r.exclusionsEn()), translation, selectable,
                r.active() == null || Boolean.TRUE.equals(r.active()), r.validFrom(), r.validTo(),
                r.sortOrder() == null ? 0 : r.sortOrder(), r.recordVersion());
    }

    private static int levelNo(String level) {
        if (level == null) return 0;
        return switch (level) {
            case "SECTION" -> 1;
            case "DIVISION" -> 2;
            case "GROUP" -> 3;
            case "CLASS" -> 4;
            case "NATIONAL_SUBCLASS" -> 5;
            default -> 0;
        };
    }

    private static void validateCode(String level, String code, Map<String, String> errors) {
        if (level == null || code == null) return;
        boolean valid = switch (level) {
            case "SECTION" -> SECTION.matcher(code).matches();
            case "DIVISION" -> DIVISION.matcher(code).matches();
            case "GROUP" -> GROUP.matcher(code).matches();
            case "CLASS" -> CLASS.matcher(code).matches();
            case "NATIONAL_SUBCLASS" -> NATIONAL.matcher(code).matches();
            default -> false;
        };
        if (!valid) errors.put("isicCode", "قالب کد با سطح انتخاب‌شده سازگار نیست.");
    }

    private static boolean validParent(String child, String parent) {
        if (child == null || parent == null) return false;
        return switch (child) {
            case "DIVISION" -> "SECTION".equals(parent);
            case "GROUP" -> "DIVISION".equals(parent);
            case "CLASS" -> "GROUP".equals(parent);
            case "NATIONAL_SUBCLASS" -> "CLASS".equals(parent) || "NATIONAL_SUBCLASS".equals(parent);
            case "SECTION" -> false;
            default -> false;
        };
    }

    private static void validateDates(LocalDate from, LocalDate to, Map<String, String> errors) {
        if (from != null && to != null && to.isBefore(from)) errors.put("validTo", "پایان اعتبار نمی‌تواند قبل از شروع اعتبار باشد.");
    }

    private static String required(String value, String field, Map<String, String> errors) {
        String v = text(value);
        if (v == null) errors.put(field, "مقدار این فیلد الزامی است.");
        return v;
    }

    private static String text(String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    private static String upper(String value) { return value == null ? null : value.toUpperCase(Locale.ROOT); }
    private static ReferenceNotFoundException notFound(String entity) { return new ReferenceNotFoundException(entity + " یافت نشد."); }
    private static ReferenceValidationException stale() {
        return new ReferenceValidationException("رکورد تغییر کرده یا دیگر وجود ندارد. اطلاعات را دوباره بارگذاری کنید.",
                Map.of("recordVersion", "نسخه رکورد معتبر نیست."));
    }
}
