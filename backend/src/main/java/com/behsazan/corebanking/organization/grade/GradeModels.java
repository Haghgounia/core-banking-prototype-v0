package com.behsazan.corebanking.organization.grade;

import java.time.LocalDate;
import java.util.List;

/** ORG-GRADE P3/P4 API contract. Dates are Gregorian ISO-8601 on the wire; UI renders Solar Hijri. */
public final class GradeModels {
    private GradeModels() {}

    public record Level(long id, long organizationId, long organizationUnitTypeId,
                        String gradeSchemeCode, String gradeCode, String nameFa,
                        String nameEn, int displayOrder, boolean active, long rowVersion) {}

    public record LevelInput(long organizationId, long organizationUnitTypeId,
                             String gradeSchemeCode, String gradeCode, String nameFa,
                             String nameEn, int displayOrder, boolean active, long rowVersion) {}

    public record Unit(long id, long organizationId, long organizationUnitTypeId,
                       String unitCode, String nameFa, String statusCode, String currentGrade) {}

    public record Organization(long id, String organizationCode, String nameFa) {}

    public record Period(long id, long organizationUnitId, long gradeLevelId,
                         String gradeCode, String gradeNameFa, LocalDate effectiveFrom,
                         LocalDate effectiveTo, String changeReasonCode, String recordState,
                         String descriptionFa, String createdBy, long rowVersion) {}

    /** Only fields required to assign a grade in the prototype. */
    public record AssignmentInput(long organizationUnitId, long gradeLevelId,
                                  LocalDate effectiveFrom, LocalDate effectiveTo,
                                  String descriptionFa) {}
    public record Coverage(long activeBranches, long gradedBranches, long missingBranches) {}
    public record PeriodPlan(Long closePredecessorId, Long closePredecessorVersion) {}
}
