package com.behsazan.corebanking.referencedata.descriptor;

import com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorRegistry;
import com.behsazan.corebanking.referencedata.education.descriptor.EducationDescriptorProvider;
import com.behsazan.corebanking.referencedata.employment.descriptor.EmploymentDescriptorProvider;
import com.behsazan.corebanking.referencedata.general.descriptor.GeneralReferenceDescriptorProvider;
import com.behsazan.corebanking.referencedata.geography.descriptor.GeographyDescriptorProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExtendedReferenceDescriptorRegistryTest {
    private final ReferenceDescriptorRegistry registry = new ReferenceDescriptorRegistry(List.of(
            new GeographyDescriptorProvider(),
            new GeneralReferenceDescriptorProvider(),
            new EmploymentDescriptorProvider(),
            new EducationDescriptorProvider()
    ));

    @Test
    void registersAllActiveReferenceResources() {
        assertThat(registry.all()).hasSize(20);
        assertThat(registry.all()).extracting("resource").contains(
                "continents", "languages", "currencies", "countries", "blood-types", "banks", "foreign-cities",
                "job-groups", "jobs",
                "education-groups", "education-subgroups", "education-degrees", "education-fields", "education-universities"
        );
    }

    @Test
    void usesGeoSchemaForEveryActiveReferenceTable() {
        assertThat(registry.all())
                .extracting("schemaName")
                .containsOnly("GEO");
    }

    @Test
    void classifiesContinentsCountriesAndForeignCitiesAsGeography() {
        assertThat(registry.require("continents").category()).isEqualTo("GEOGRAPHY");
        assertThat(registry.require("countries").category()).isEqualTo("GEOGRAPHY");
        assertThat(registry.require("foreign-cities").category()).isEqualTo("GEOGRAPHY");
        assertThat(registry.require("languages").category()).isEqualTo("GENERAL");
    }

    @Test
    void preservesNewParentChainsAndLookupResources() {
        assertThat(registry.require("countries").parent().resource()).isEqualTo("continents");
        assertThat(registry.require("banks").parent().resource()).isEqualTo("countries");
        assertThat(registry.require("foreign-cities").parent().resource()).isEqualTo("countries");
        assertThat(registry.require("jobs").parent().resource()).isEqualTo("job-groups");
        assertThat(registry.require("education-fields").parent().resource()).isEqualTo("education-subgroups");
        assertThat(registry.require("education-universities").field("provinceId").lookupResource()).isEqualTo("provinces");
        assertThat(registry.require("countries").field("currencyId").lookupResource()).isEqualTo("currencies");
    }
}
