package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.referencedata.management.application.ReferenceService;
import com.behsazan.corebanking.referencedata.management.domain.ReferenceRecordResponse;
import com.behsazan.corebanking.cif.reference.application.PartyReferenceService;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PdlOrgScopeR18Test {
    private final ReferenceService referenceService = mock(ReferenceService.class);
    private final PdlReferenceOptionService options = new PdlReferenceOptionService(
            referenceService, mock(PartyReferenceService.class), mock(PdlDormancyFeePlanReference.class));

    @Test void derivesCanonicalCodeFromTheSelectedReferenceRecord() {
        when(referenceService.findById("dps-org-units", 12L)).thenReturn(new ReferenceRecordResponse(12L,
                Map.of("code", "BR-001", "nameFa", "شعبه مرکزی", "isActive", 1, "isCurrent", 1), List.of()));
        assertThat(options.governedOrgUnitCode(12)).isEqualTo("BR-001");
    }

    @Test void rejectsInactiveOrUnknownOrgUnitInsteadOfTrustingUserCode() {
        when(referenceService.findById("dps-org-units", 19L)).thenReturn(new ReferenceRecordResponse(19L,
                Map.of("code", "OLD", "isActive", 0, "isCurrent", 1), List.of()));
        assertThatThrownBy(() -> options.governedOrgUnitCode(19)).isInstanceOf(ProductBuilderValidationException.class);
        assertThatThrownBy(() -> options.governedOrgUnitCode(0)).isInstanceOf(ProductBuilderValidationException.class);
    }
}
