package com.behsazan.corebanking.productbuilder.application;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PdlCatalogTest {
    @Test
    void containsFiftyBusinessTablesPlusThreeCodeInfrastructureTables() {
        assertThat(PdlCatalog.entries()).hasSize(53);
        assertThat(PdlCatalog.contains("PRODUCT")).isTrue();
        assertThat(PdlCatalog.contains("PRODUCT_VERSION")).isTrue();
        assertThat(PdlCatalog.contains("DEPOSIT_PROFIT_PAYMENT_RULE")).isTrue();
        assertThat(PdlCatalog.contains("CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE")).isTrue();
        assertThat(PdlCatalog.contains("CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE")).isTrue();
        assertThat(PdlCatalog.contains("CODE_SET")).isTrue();
    }

    @Test
    void preservesUnifiedProductBuilderBusinessDistributionAndSeparatesCodeInfrastructure() {
        Map<String, Long> counts = PdlCatalog.entries().stream()
                .collect(Collectors.groupingBy(PdlCatalog.Entry::packageCode, Collectors.counting()));
        assertThat(counts).containsExactlyInAnyOrderEntriesOf(Map.of(
                "01", 5L,
                "02", 9L,
                "03", 14L,
                "04", 6L,
                "05", 14L,
                "11", 2L,
                "90", 3L
        ));
        long businessCount = PdlCatalog.entries().stream()
                .filter(entry -> !entry.packageCode().equals("90"))
                .count();
        assertThat(businessCount).isEqualTo(50L);
    }
}
