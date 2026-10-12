package com.behsazan.corebanking.fee.admin.application;

import com.behsazan.corebanking.fee.admin.oracle.FeeAdminRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FeeChargeStageIntegrityPolicyTest {

    @Test void validLinksMayUseSameVersion() {
        var links = FeeChargeStageIntegrityPolicy.links(Map.of(
                "FEE_DEFINITION_VERSION_ID", new BigDecimal("16"),
                "CALCULATION_RULE_ID", 101,
                "CHARGE_COMPONENT_ID", 202), null);
        FeeChargeStageIntegrityPolicy.verifyOwners(links, 16, 16L);
        assertThat(links.versionId()).isEqualTo(16);
        assertThat(links.componentId()).isEqualTo(202L);
    }

    @Test void rejectsCrossVersionRuleAndComponent() {
        var links = FeeChargeStageIntegrityPolicy.links(Map.of(
                "FEE_DEFINITION_VERSION_ID", 16,
                "CALCULATION_RULE_ID", 101,
                "CHARGE_COMPONENT_ID", 202), null);
        assertThatThrownBy(() -> FeeChargeStageIntegrityPolicy.verifyOwners(links, 17, 16L))
                .isInstanceOf(FeeAdminValidationException.class).hasMessageContaining("قاعده");
        assertThatThrownBy(() -> FeeChargeStageIntegrityPolicy.verifyOwners(links, 16, 17L))
                .isInstanceOf(FeeAdminValidationException.class).hasMessageContaining("مؤلفه");
    }

    @Test void rejectsMissingFractionalAndNegativeIdentifiers() {
        for (Object value : new Object[]{0, -5, "1.2", "invalid", Double.NaN}) {
            assertThatThrownBy(() -> FeeChargeStageIntegrityPolicy.links(Map.of(
                    "FEE_DEFINITION_VERSION_ID", value, "CALCULATION_RULE_ID", 101), null))
                    .isInstanceOf(FeeAdminValidationException.class);
        }
    }

    @Test void partialEditMergesExistingLinksAndExplicitNullFails() {
        Map<String,Object> persisted = Map.of(
                "FEE_DEFINITION_VERSION_ID", 16, "CALCULATION_RULE_ID", 101, "CHARGE_COMPONENT_ID", 202);
        var links = FeeChargeStageIntegrityPolicy.links(Map.of("STAGE_CODE", "CONTRACT"), persisted);
        assertThat(links.ruleId()).isEqualTo(101);
        assertThat(links.componentId()).isEqualTo(202L);
        assertThatThrownBy(() -> FeeChargeStageIntegrityPolicy.links(
                java.util.Collections.singletonMap("CALCULATION_RULE_ID", null), persisted))
                .isInstanceOf(FeeAdminValidationException.class);
    }

    @Test void referencedParentMustNotMoveToAnotherVersion() {
        assertThatThrownBy(() -> FeeChargeStageIntegrityPolicy.verifyParentMove(16, 17, true, "قاعده"))
                .isInstanceOf(FeeAdminValidationException.class);
        FeeChargeStageIntegrityPolicy.verifyParentMove(16, 17, false, "قاعده");
        FeeChargeStageIntegrityPolicy.verifyParentMove(16, 16, true, "قاعده");
    }

    @Test void legacyGenericWritePathRejectsCrossVersionRuleBeforeInsert() {
        FeeAdminRepository repo = mock(FeeAdminRepository.class);
        when(repo.lockStageParentVersion("FEE_CALCULATION_RULE",101)).thenReturn(Optional.of(17L));
        var service = new FeeAdminService(repo);
        assertThatThrownBy(() -> service.create("FEE_CHARGE_STAGE", Map.of(
                "FEE_DEFINITION_VERSION_ID", 16, "CALCULATION_RULE_ID", 101), "qa"))
                .isInstanceOf(FeeAdminValidationException.class);
        verify(repo, never()).insert(eq("FEE_CHARGE_STAGE"), any(), anyString());
    }

    @Test void legacyGenericPartialUpdateRejectsCrossVersionComponent() {
        FeeAdminRepository repo = mock(FeeAdminRepository.class);
        when(repo.lockChargeStage(9501L)).thenReturn(Optional.of(Map.of(
                "FEE_DEFINITION_VERSION_ID", 16, "CALCULATION_RULE_ID", 101, "CHARGE_COMPONENT_ID", 202)));
        when(repo.lockStageParentVersion("FEE_CALCULATION_RULE",101)).thenReturn(Optional.of(16L));
        when(repo.lockStageParentVersion("FEE_CHARGE_COMPONENT",202)).thenReturn(Optional.of(17L));
        var service = new FeeAdminService(repo);
        assertThatThrownBy(() -> service.update("FEE_CHARGE_STAGE", 9501L, Map.of("STAGE_CODE", "NEW"), "qa"))
                .isInstanceOf(FeeAdminValidationException.class);
        verify(repo, never()).update(eq("FEE_CHARGE_STAGE"), eq(9501L), any(), anyString());
    }

    @Test void referencedRuleCannotBeMovedToAnotherVersion() {
        FeeAdminRepository repo = mock(FeeAdminRepository.class);
        when(repo.lockStageParentVersion("FEE_CALCULATION_RULE",101L)).thenReturn(Optional.of(16L));
        when(repo.stageReferencesParent("FEE_CALCULATION_RULE",101L)).thenReturn(true);
        var service = new FeeAdminService(repo);
        assertThatThrownBy(() -> service.update("FEE_CALCULATION_RULE", 101L,
                Map.of("FEE_DEFINITION_VERSION_ID", 17), "qa"))
                .isInstanceOf(FeeAdminValidationException.class);
        verify(repo, never()).update(eq("FEE_CALCULATION_RULE"), eq(101L), any(), anyString());
    }
}
