package com.behsazan.corebanking.organization.grade;

import com.behsazan.corebanking.organization.grade.GradeModels.Period;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class GradePolicyTest {
    private static Period period(long id, String status, String start, String end, long version) {
        return new Period(id, 10, 20, "GRADE_1", "درجه یک",
                LocalDate.parse(start), end == null ? null : LocalDate.parse(end),
                "INITIAL", status, null, "prototype", version);
    }
    @Test void initialGradeHasNoPredecessor() {
        var plan = GradePolicy.plan(period(1, "ACTIVE", "2026-01-01", null, 0), List.of());
        assertThat(plan.closePredecessorId()).isNull();
    }
    @Test void promotionClosesPreviousOpenPeriod() {
        var plan = GradePolicy.plan(period(2, "ACTIVE", "2026-07-01", null, 0),
                List.of(period(1, "ACTIVE", "2026-01-01", null, 5)));
        assertThat(plan.closePredecessorId()).isEqualTo(1L);
        assertThat(plan.closePredecessorVersion()).isEqualTo(5L);
    }
    @Test void duplicateStartRejected() {
        assertThatThrownBy(() -> GradePolicy.plan(period(2, "ACTIVE", "2026-01-01", null, 0),
                List.of(period(1, "ACTIVE", "2026-01-01", null, 0))))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void futureApprovedPeriodPreventsOverlappingOpenGrade() {
        assertThatThrownBy(() -> GradePolicy.plan(period(2, "ACTIVE", "2026-01-01", null, 0),
                List.of(period(1, "ACTIVE", "2026-09-01", null, 0))))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void finiteTemporaryGradeCannotSilentlyCutOpenPredecessor() {
        assertThatThrownBy(() -> GradePolicy.plan(period(2, "ACTIVE", "2026-07-01", "2026-08-01", 0),
                List.of(period(1, "ACTIVE", "2026-01-01", null, 0))))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void adjoiningPeriodsAllowed() {
        var plan = GradePolicy.plan(period(2, "ACTIVE", "2026-07-01", null, 0),
                List.of(period(1, "ACTIVE", "2026-01-01", "2026-07-01", 0)));
        assertThat(plan.closePredecessorId()).isNull();
    }
    @Test void invalidDateRejected() {
        assertThatThrownBy(() -> GradePolicy.dates(LocalDate.parse("2026-02-02"), LocalDate.parse("2026-02-02")))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void mismatchedOrganizationOrNonBranchRejected() {
        assertThatThrownBy(() -> GradePolicy.eligibility(1, 1, 2, 1, "BRANCH"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GradePolicy.eligibility(1, 2, 1, 2, "COUNTER"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
