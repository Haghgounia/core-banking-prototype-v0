package com.behsazan.corebanking.productbuilder.application;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PdlCatalogTest {
    @Test
    void containsAllFiftyUnifiedProductBuilderTables() {
        assertThat(PdlCatalog.entries()).hasSize(50);
        assertThat(PdlCatalog.contains("PRODUCT")).isTrue();
        assertThat(PdlCatalog.contains("PRODUCT_VERSION")).isTrue();
        assertThat(PdlCatalog.contains("DEPOSIT_PRODUCT_PROFILE")).isTrue();
        assertThat(PdlCatalog.contains("LOAN_PRODUCT_PROFILE")).isTrue();
        assertThat(PdlCatalog.contains("CODE_SET")).isTrue();
    }

    @Test
    void preservesEaPackageDistribution() {
        Map<String, Long> counts = PdlCatalog.entries().stream()
                .collect(Collectors.groupingBy(PdlCatalog.Entry::packageCode, Collectors.counting()));
        assertThat(counts).containsExactlyInAnyOrderEntriesOf(Map.of(
                "01", 5L,
                "02", 9L,
                "03", 13L,
                "04", 6L,
                "05", 17L
        ));
    }
}
