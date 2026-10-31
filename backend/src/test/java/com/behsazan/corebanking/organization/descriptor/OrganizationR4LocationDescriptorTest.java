package com.behsazan.corebanking.organization.descriptor;

import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceTableDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OrganizationR4LocationDescriptorTest {
    @Test
    void genericSourceTablesAreNotPublishedAfterCanonicalCutover() {
        List<ReferenceTableDescriptor> entries = new OrganizationDescriptorProvider().descriptors();
        assertEquals(24, entries.size());
        assertFalse(entries.stream().anyMatch(d -> "LOCATIONS".equals(d.tableName())));
        assertFalse(entries.stream().anyMatch(d -> "POSTAL_ADDRESSES".equals(d.tableName())));
        assertTrue(entries.stream().anyMatch(d -> "ORGANIZATION_UNIT_LOCATIONS".equals(d.tableName())));
    }

    @Test
    void servicePointsUseCanonicalLocationAndNoLegacyLookup() {
        List<ReferenceTableDescriptor> entries = new OrganizationDescriptorProvider().descriptors();
        ReferenceTableDescriptor sp = entries.stream()
                .filter(d -> "points-of-service".equals(d.resource())).findFirst().orElseThrow();
        assertEquals("SERVICE_POINTS", sp.tableName());
        assertTrue(sp.optionalField("organizationUnitLocationId").isPresent());
        assertEquals("ORGANIZATION_UNIT_LOCATION_ID", sp.field("organizationUnitLocationId").columnName());
        assertEquals("organization-unit-locations", sp.field("organizationUnitLocationId").lookupResource());
        assertTrue(sp.field("organizationUnitLocationId").required());
        assertTrue(sp.optionalField("locationId").isEmpty());
        ReferenceTableDescriptor canonical = entries.stream()
                .filter(d -> "organization-unit-locations".equals(d.resource())).findFirst().orElseThrow();
        assertEquals("addressFa", canonical.nameApiName());
        assertEquals("ADDRESS_FA", canonical.field(canonical.nameApiName()).columnName());
        assertTrue(canonical.optionalField("locationId").isEmpty());
    }
}
