package com.behsazan.corebanking.referencedata.descriptor;

import com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorRegistry;
import com.behsazan.corebanking.referencedata.geography.descriptor.GeographyDescriptorProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceDescriptorRegistryTest {
    private final ReferenceDescriptorRegistry registry = new ReferenceDescriptorRegistry(List.of(new GeographyDescriptorProvider()));

    @Test
    void registersTheSixGeographyResources() {
        assertThat(registry.all()).extracting("resource")
                .containsExactly("provinces", "counties", "districts", "cities", "rural-districts", "villages");
    }

    @Test
    void preservesTheGeographyParentChain() {
        assertThat(registry.require("counties").parent().resource()).isEqualTo("provinces");
        assertThat(registry.require("districts").parent().resource()).isEqualTo("counties");
        assertThat(registry.require("cities").parent().resource()).isEqualTo("districts");
        assertThat(registry.require("rural-districts").parent().resource()).isEqualTo("districts");
        assertThat(registry.require("villages").parent().resource()).isEqualTo("rural-districts");
    }
}
