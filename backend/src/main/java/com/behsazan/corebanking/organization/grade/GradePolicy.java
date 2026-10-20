package com.behsazan.corebanking.organization.grade;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import com.behsazan.corebanking.organization.grade.GradeModels.PeriodPlan;
import com.behsazan.corebanking.organization.grade.GradeModels.Period;

/** Pure, dependency-free policy for effective dated grade decisions. */
public final class GradePolicy {
    private GradePolicy() {}

    public static void dates(LocalDate from, LocalDate to) {
        if (from == null) throw new IllegalArgumentException("تاریخ شروع اعتبار اجباری است.");
        if (to != null && !to.isAfter(from))
            throw new IllegalArgumentException("تاریخ پایان باید پس از تاریخ شروع باشد.");
    }

    public static void eligibility(long unitOrganizationId, long unitTypeId,
                                   long gradeOrganizationId, long gradeUnitTypeId,
                                   String gradeUnitTypeCode) {
        if (!Objects.equals(unitOrganizationId, gradeOrganizationId) || unitTypeId != gradeUnitTypeId)
            throw new IllegalArgumentException("سازمان یا نوع واحد با درجه انتخابی سازگار نیست.");
        if (!"BRANCH".equals(gradeUnitTypeCode))
            throw new IllegalArgumentException("در این نسخه درجه‌بندی فقط برای شعبه مجاز است.");
    }

    public static PeriodPlan plan(Period proposed, List<Period> alreadyApproved) {
        dates(proposed.effectiveFrom(), proposed.effectiveTo());
        Period predecessor = null;
        for (Period old : alreadyApproved) {
            if (!("ACTIVE".equals(old.recordState()) || "APPROVED".equals(old.recordState())) || old.id() == proposed.id()) continue;
            if (old.organizationUnitId() != proposed.organizationUnitId())
                throw new IllegalArgumentException("سوابق دوره باید متعلق به همان واحد باشند.");
            LocalDate from = proposed.effectiveFrom();
            LocalDate to = proposed.effectiveTo();
            if (old.effectiveFrom().isEqual(from))
                throw new IllegalArgumentException("برای این تاریخ شروع، درجه مصوب دیگری وجود دارد.");
            boolean overlaps = (to == null || old.effectiveFrom().isBefore(to)) &&
                    (old.effectiveTo() == null || old.effectiveTo().isAfter(from));
            if (!overlaps) continue;
            if (old.effectiveFrom().isBefore(from) && predecessor == null) {
                // The previous period is allowed to be shortened only at the new start.
                // Never silently discard a tail of a finite period (requires splitting).
                if (to != null && (old.effectiveTo() == null || old.effectiveTo().isAfter(to))) {
                    if (old.effectiveTo() != null)
                        throw new IllegalArgumentException("درجه پیشنهادی داخل دوره مصوب قبلی است؛ اصلاح تاریخچه نیازمند فرایند جداگانه است.");
                    // If previous is open and proposed is finite, clipping would create a gap after proposed.
                    throw new IllegalArgumentException("برای دوره موقت باید جانشین درجه قبلی مشخص شود؛ درج دوره محدود روی دوره باز مجاز نیست.");
                }
                predecessor = old;
            } else {
                throw new IllegalArgumentException("دوره درجه‌بندی با دوره مصوب دیگری هم‌پوشانی دارد.");
            }
        }
        return new PeriodPlan(predecessor == null ? null : predecessor.id(),
                                predecessor == null ? null : predecessor.rowVersion());
    }
}
