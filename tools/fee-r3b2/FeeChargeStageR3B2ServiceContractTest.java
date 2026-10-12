package com.behsazan.corebanking.fee.admin.application;

import com.behsazan.corebanking.fee.admin.oracle.FeeAdminRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Pure unit tests: zero Oracle/HTTP connections, zero persistent data. */
class FeeChargeStageR3B2ServiceContractTest {
    private static Map<String, Object> stage(long version, long rule) {
        return Map.of("FEE_DEFINITION_VERSION_ID", version, "CALCULATION_RULE_ID", rule,
                "STAGE_NO", 1, "STAGE_CODE", "R3B2_TEST", "NAME_FA", "مرحله آزمایشی",
                "TRIGGER_CODE", "R3B2_EVENT");
    }

    @Test void correctVersionAllowsStageWithoutOptionalComponent() {
        FeeAdminRepository repo = mock(FeeAdminRepository.class);
        when(repo.lockStageParentVersion("FEE_CALCULATION_RULE", 101)).thenReturn(Optional.of(16L));
        when(repo.insert(eq("FEE_CHARGE_STAGE"), any(), anyString())).thenReturn(901L);
        when(repo.findById("FEE_CHARGE_STAGE", 901L)).thenReturn(Optional.of(Map.of("CHARGE_STAGE_ID", 901L)));
        var result = new FeeAdminService(repo).create("FEE_CHARGE_STAGE", stage(16, 101), "r3b2-uat");
        assertThat(result.get("CHARGE_STAGE_ID")).isEqualTo(901L);
        verify(repo, never()).lockStageParentVersion(eq("FEE_CHARGE_COMPONENT"), anyLong());
        verify(repo).insert(eq("FEE_CHARGE_STAGE"), any(), eq("r3b2-uat"));
    }

    @Test void mismatchedRuleNeverReachesInsert() {
        FeeAdminRepository repo = mock(FeeAdminRepository.class);
        when(repo.lockStageParentVersion("FEE_CALCULATION_RULE", 101)).thenReturn(Optional.of(17L));
        assertThatThrownBy(() -> new FeeAdminService(repo).create("FEE_CHARGE_STAGE", stage(16, 101), "qa"))
                .isInstanceOf(FeeAdminValidationException.class).hasMessageContaining("قاعده");
        verify(repo, never()).insert(eq("FEE_CHARGE_STAGE"), any(), anyString());
    }

    @Test void mismatchedComponentNeverReachesInsert() {
        FeeAdminRepository repo = mock(FeeAdminRepository.class);
        when(repo.lockStageParentVersion("FEE_CALCULATION_RULE", 101)).thenReturn(Optional.of(16L));
        when(repo.lockStageParentVersion("FEE_CHARGE_COMPONENT", 202)).thenReturn(Optional.of(17L));
        var values = new HashMap<>(stage(16, 101));
        values.put("CHARGE_COMPONENT_ID", 202);
        assertThatThrownBy(() -> new FeeAdminService(repo).create("FEE_CHARGE_STAGE", values, "qa"))
                .isInstanceOf(FeeAdminValidationException.class).hasMessageContaining("مؤلفه");
        verify(repo, never()).insert(eq("FEE_CHARGE_STAGE"), any(), anyString());
    }

    @Test void missingParentNeverReachesInsert() {
        FeeAdminRepository repo = mock(FeeAdminRepository.class);
        when(repo.lockStageParentVersion("FEE_CALCULATION_RULE", 101)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new FeeAdminService(repo).create("FEE_CHARGE_STAGE", stage(16, 101), "qa"))
                .isInstanceOf(FeeAdminValidationException.class).hasMessageContaining("وجود ندارد");
        verify(repo, never()).insert(eq("FEE_CHARGE_STAGE"), any(), anyString());
    }

    @Test void partialUpdatePreservesOwnersAndCanClearOptionalComponent() {
        FeeAdminRepository repo = mock(FeeAdminRepository.class);
        when(repo.lockChargeStage(901L)).thenReturn(Optional.of(Map.of(
                "FEE_DEFINITION_VERSION_ID", 16L, "CALCULATION_RULE_ID", 101L, "CHARGE_COMPONENT_ID", 202L)));
        when(repo.lockStageParentVersion("FEE_CALCULATION_RULE", 101)).thenReturn(Optional.of(16L));
        when(repo.update(eq("FEE_CHARGE_STAGE"), eq(901L), any(), anyString())).thenReturn(true);
        when(repo.findById("FEE_CHARGE_STAGE", 901L)).thenReturn(Optional.of(Map.of("CHARGE_STAGE_ID", 901L)));
        Map<String, Object> patch = new HashMap<>();
        patch.put("CHARGE_COMPONENT_ID", null);
        new FeeAdminService(repo).update("FEE_CHARGE_STAGE", 901L, patch, "qa");
        verify(repo, never()).lockStageParentVersion(eq("FEE_CHARGE_COMPONENT"), anyLong());
        verify(repo).update(eq("FEE_CHARGE_STAGE"), eq(901L), any(), anyString());
    }

    @Test void referencedParentCannotMoveAndUpdateDoesNotRun() {
        FeeAdminRepository repo = mock(FeeAdminRepository.class);
        when(repo.lockStageParentVersion("FEE_CALCULATION_RULE", 101L)).thenReturn(Optional.of(16L));
        when(repo.stageReferencesParent("FEE_CALCULATION_RULE", 101L)).thenReturn(true);
        assertThatThrownBy(() -> new FeeAdminService(repo).update("FEE_CALCULATION_RULE", 101L,
                Map.of("FEE_DEFINITION_VERSION_ID", 17L), "qa"))
                .isInstanceOf(FeeAdminValidationException.class);
        verify(repo, never()).update(eq("FEE_CALCULATION_RULE"), anyLong(), any(), anyString());
    }

    @Test void serviceWritesAreDeclaredTransactional() throws NoSuchMethodException {
        for (Method method : new Method[]{
                FeeAdminService.class.getMethod("create", String.class, Map.class, String.class),
                FeeAdminService.class.getMethod("update", String.class, long.class, Map.class, String.class)}) {
            assertThat(method.isAnnotationPresent(Transactional.class)).isTrue();
        }
    }
}
