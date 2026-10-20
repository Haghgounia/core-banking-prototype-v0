package com.behsazan.corebanking.organization.grade;

import com.behsazan.corebanking.organization.grade.GradeModels.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GradeServiceTest {
    @Test void canDirectlyAssignWithoutAnyApproval() {
        GradeRepository repo = mock(GradeRepository.class);
        GradeService service = new GradeService(repo);
        var from = LocalDate.of(2026, 10, 20);
        when(repo.requireUnit(10)).thenReturn(new Unit(10, 1, 1, "0101", "شعبه", "ACTIVE", null));
        when(repo.level(20)).thenReturn(new Level(20, 1, 1, "BRANCH_STANDARD", "GRADE_1", "درجه یک", null, 1, true, 0));
        when(repo.unitTypeCode(1)).thenReturn("BRANCH");
        when(repo.effective(10)).thenReturn(List.of());
        when(repo.nextPeriodId()).thenReturn(30L);
        var result = new Period(30, 10, 20, "GRADE_1", "درجه یک", from, null,
                "INITIAL", "ACTIVE", null, "PROTOTYPE", 0);
        when(repo.period(30)).thenReturn(result);
        assertThat(service.assign(new AssignmentInput(10, 20, from, null, null), "PROTOTYPE").recordState())
                .isEqualTo("ACTIVE");
        verify(repo).insertActivePeriod(eq(30L), any(AssignmentInput.class), eq("INITIAL"), eq("PROTOTYPE"));
    }
    @Test void rejectsInvalidDates() {
        GradeRepository repo = mock(GradeRepository.class);
        var date = LocalDate.of(2026, 10, 20);
        assertThatThrownBy(() -> new GradeService(repo).assign(new AssignmentInput(10, 20, date, date, null), "PROTOTYPE"))
                .isInstanceOf(GradeException.class);
        verify(repo, never()).insertActivePeriod(anyLong(), any(), anyString(), anyString());
    }
}
