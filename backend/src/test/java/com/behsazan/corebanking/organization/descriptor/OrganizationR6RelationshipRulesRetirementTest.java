package com.behsazan.corebanking.organization.descriptor;

import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceTableDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OrganizationR6RelationshipRulesRetirementTest {
    @Test
    void oldRuleTableIsNotExposedButRelationshipsAndTypesRemain() {
        List<ReferenceTableDescriptor> descriptors = new OrganizationDescriptorProvider().descriptors();
        assertEquals(23, descriptors.size());
        assertFalse(descriptors.stream().anyMatch(d ->
                "ORGANIZATION_UNIT_RELATIONSHIP_RULES".equals(d.tableName())));
        assertFalse(descriptors.stream().anyMatch(d ->
                "organization-unit-relationship-rules".equals(d.resource())));
        assertFalse(descriptors.stream().anyMatch(d ->
                "organization-unit-contact-points".equals(d.resource())));
        assertTrue(descriptors.stream().anyMatch(d ->
                "ORGANIZATION_UNIT_RELATIONSHIPS".equals(d.tableName())));
        assertTrue(descriptors.stream().anyMatch(d ->
                "ORGANIZATION_UNIT_RELATIONSHIP_TYPES".equals(d.tableName())));
    }
}
