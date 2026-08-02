package com.behsazan.corebanking.deposit.productfactory.reference;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DepositProductReferenceDescriptorProviderTest {
    private final DepositProductReferenceDescriptorProvider provider =
            new DepositProductReferenceDescriptorProvider();

    @Test
    void exposesAllFiftyDpsReferenceTablesUnderDepositProductReferenceCategory() {
        assertThat(provider.descriptors()).hasSize(50);
        assertThat(provider.descriptors())
                .allSatisfy(descriptor -> {
                    assertThat(descriptor.schemaName()).isEqualTo("DPS");
                    assertThat(descriptor.category())
                            .isEqualTo(DepositProductReferenceDescriptorProvider.CATEGORY);
                    assertThat(descriptor.tableName()).startsWith("REF_");
                    assertThat(descriptor.sequenceName()).isEqualTo("SEQ_" + descriptor.tableName());
                    assertThat(descriptor.field("code").required()).isTrue();
                    assertThat(descriptor.field("nameFa").required()).isTrue();
                    assertThat(descriptor.field("createdBy").readOnly()).isTrue();
                });
    }

    @Test
    void preservesTableSpecificColumnShapes() {
        var channels = provider.descriptors().stream()
                .filter(item -> item.resource().equals("dps-channels"))
                .findFirst().orElseThrow();
        assertThat(channels.optionalField("parentCode")).isPresent();
        assertThat(channels.optionalField("validFrom")).isPresent();
        assertThat(channels.optionalField("recordVersion")).isPresent();

        var genders = provider.descriptors().stream()
                .filter(item -> item.resource().equals("dps-genders"))
                .findFirst().orElseThrow();
        assertThat(genders.optionalField("validFrom")).isEmpty();

        var holdTypes = provider.descriptors().stream()
                .filter(item -> item.resource().equals("dps-hold-types"))
                .findFirst().orElseThrow();
        assertThat(holdTypes.optionalField("recordVersion")).isEmpty();
    }
}
