package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ProductGovernanceStopOnlyR19Test {
    private final ProductGovernanceWriteGuard guard = new ProductGovernanceWriteGuard(mock(PdlProductBuilderRepository.class));
    private final Map<String,Object> version = Map.of(
            "PRODUCT_VERSION_ID", 55L, "VERSION_STATUS_CODE", "APPROVED",
            "ORIGINATION_STATUS_CODE", "OPEN", "SERVICING_STATUS_CODE", "ACTIVE", "IS_CURRENT", 1,
            "RECORD_STATUS_CODE", "ACTIVE");

    @Test void bankCanImmediatelyStopOriginationOnApprovedProduct() {
        assertThatCode(() -> guard.assertUpdateAllowed("PRODUCT_VERSION", version,
                Map.of("ORIGINATION_STATUS_CODE", "SUSPENDED"))).doesNotThrowAnyException();
        assertThatCode(() -> guard.assertUpdateAllowed("PRODUCT_VERSION", version,
                Map.of("ORIGINATION_STATUS_CODE", "CLOSED", "SERVICING_STATUS_CODE", "SUSPENDED")))
                .doesNotThrowAnyException();
    }

    @Test void bankCannotReopenOrMutateApprovedRuleThroughGenericCrud() {
        assertThatThrownBy(() -> guard.assertUpdateAllowed("PRODUCT_VERSION", version,
                Map.of("ORIGINATION_STATUS_CODE", "ENABLED"))).isInstanceOf(ProductBuilderValidationException.class);
        assertThatThrownBy(() -> guard.assertUpdateAllowed("PRODUCT_VERSION", version,
                Map.of("VALID_FROM", "2026-10-10"))).isInstanceOf(ProductBuilderValidationException.class);
    }

    @Test void approvedVersionCanBeExpiredButNotRevertedToDraft() {
        assertThatCode(() -> guard.assertUpdateAllowed("PRODUCT_VERSION", version,
                Map.of("VERSION_STATUS_CODE", "EXPIRED"))).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.assertUpdateAllowed("PRODUCT_VERSION", version,
                Map.of("VERSION_STATUS_CODE", "DRAFT"))).isInstanceOf(ProductBuilderValidationException.class);
    }
}
