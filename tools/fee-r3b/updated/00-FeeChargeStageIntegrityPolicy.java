package com.behsazan.corebanking.fee.admin.application;

import java.math.BigDecimal;
import java.util.Map;

/** Validates version-owned stage links before any database write. */
public final class FeeChargeStageIntegrityPolicy {
    private FeeChargeStageIntegrityPolicy() {}

    public record StageLinks(long versionId, long ruleId, Long componentId) {}

    /** Omitted keys on PATCH-like updates retain their persisted values; explicit null does not. */
    public static StageLinks links(Map<String, Object> values, Map<String, Object> persisted) {
        long versionId = requirePositive(pick(values, persisted, "FEE_DEFINITION_VERSION_ID"), "نسخه کارمزد");
        long ruleId = requirePositive(pick(values, persisted, "CALCULATION_RULE_ID"), "قاعده محاسبه");
        Long componentId = optionalPositive(pick(values, persisted, "CHARGE_COMPONENT_ID"), "مؤلفه کارمزد");
        return new StageLinks(versionId, ruleId, componentId);
    }

    public static void verifyOwners(StageLinks links, long ruleVersionId, Long componentVersionId) {
        if (links.versionId() != ruleVersionId) {
            throw new FeeAdminValidationException("قاعده محاسبه انتخابی متعلق به نسخه این مرحله اخذ نیست.");
        }
        if (links.componentId() != null && (componentVersionId == null || links.versionId() != componentVersionId)) {
            throw new FeeAdminValidationException("مؤلفه کارمزد انتخابی متعلق به نسخه این مرحله اخذ نیست.");
        }
    }

    public static void verifyParentMove(long currentVersionId, Object proposedVersion,
                                        boolean referencedByStage, String parentLabel) {
        long proposed = requirePositive(proposedVersion, "نسخه جدید");
        if (referencedByStage && currentVersionId != proposed) {
            throw new FeeAdminValidationException("تغییر نسخه " + parentLabel
                    + " مجاز نیست؛ مرحله اخذ به این رکورد وابسته است.");
        }
    }

    public static long requirePositive(Object raw, String label) {
        if (raw == null || raw.toString().trim().isEmpty()) {
            throw new FeeAdminValidationException("انتخاب " + label + " الزامی است.");
        }
        try {
            BigDecimal value = new BigDecimal(raw.toString().trim());
            long id = value.longValueExact();
            if (id > 0) return id;
        } catch (NumberFormatException | ArithmeticException ignored) {
            // Invalid numeric identifier is handled by the consistent validation error below.
        }
        throw new FeeAdminValidationException("شناسه " + label + " باید یک عدد صحیح مثبت باشد.");
    }

    private static Long optionalPositive(Object raw, String label) {
        return raw == null || raw.toString().trim().isEmpty() ? null : requirePositive(raw, label);
    }

    private static Object pick(Map<String, Object> values, Map<String, Object> persisted, String key) {
        return values.containsKey(key) ? values.get(key) : persisted == null ? null : persisted.get(key);
    }
}
