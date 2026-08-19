package com.behsazan.corebanking.cif.reference.application;

import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PartyReferenceMetadataRegistryTest {
    private final PartyReferenceMetadataRegistry registry = new PartyReferenceMetadataRegistry(JsonMapper.builder().build(), "CIF");

    @Test
    void loadsCompletedReferenceCatalogIncludingTenureExtension() {
        var catalog = registry.catalog();
        assertThat(catalog.tableCount()).isEqualTo(97);
        assertThat(catalog.packages()).extracting("name")
                .contains("Identity and Party", "Contact and Geography", "Compliance and Risk", "Organization and Product", "Workflow and Interaction", "Analytics and Recommendation");
    }

    @Test
    void preservesCompositeClassificationKeyAndRelation() {
        var descriptor = registry.descriptor("ref-classification-value");
        assertThat(descriptor.primaryKey()).containsExactly("CLASSIFICATION_TYPE_CODE", "CLASSIFICATION_VALUE_CODE");
        assertThat(descriptor.relation()).isNotNull();
        assertThat(descriptor.relation().targetResource()).isEqualTo("ref-classification-type");
    }

    @Test
    void exposesGenderReferenceWithSourceColumns() {
        var descriptor = registry.descriptor("ref-gender");
        assertThat(descriptor.schemaName()).isEqualTo("CIF");
        assertThat(descriptor.tableName()).isEqualTo("REF_GENDER");
        assertThat(descriptor.primaryKey()).containsExactly("GENDER_CODE");
        assertThat(descriptor.columns()).hasSize(10);
    }
    @Test
    void exposesOperationalComplianceLookups() {
        assertThat(registry.descriptor("ref-verification-status").tableName()).isEqualTo("REF_VERIFICATION_STATUS");
        assertThat(registry.descriptor("ref-kyc-type").tableName()).isEqualTo("REF_KYC_TYPE");
        assertThat(registry.descriptor("ref-risk-level").tableName()).isEqualTo("REF_RISK_LEVEL");
        assertThat(registry.descriptor("ref-screening-provider").tableName()).isEqualTo("REF_SCREENING_PROVIDER");
        assertThat(registry.descriptor("ref-tenure-type").tableName()).isEqualTo("REF_TENURE_TYPE");
    }

    @Test
    void exposesOrganizationAndProductLookups() {
        assertThat(registry.descriptor("ref-legal-form").tableName()).isEqualTo("REF_LEGAL_FORM");
        assertThat(registry.descriptor("ref-economic-sector").tableName()).isEqualTo("REF_ECONOMIC_SECTOR");
        assertThat(registry.descriptor("ref-isic-activity").tableName()).isEqualTo("REF_ISIC_ACTIVITY");
        assertThat(registry.descriptor("ref-product-type").tableName()).isEqualTo("REF_PRODUCT_TYPE");
        assertThat(registry.descriptor("ref-product-relationship-role").tableName()).isEqualTo("REF_PRODUCT_RELATIONSHIP_ROLE");
    }

    @Test
    void exposesWorkflowAndJourneyLookups() {
        assertThat(registry.descriptor("ref-workflow-status").tableName()).isEqualTo("REF_WORKFLOW_STATUS");
        assertThat(registry.descriptor("ref-complaint-type").tableName()).isEqualTo("REF_COMPLAINT_TYPE");
        assertThat(registry.descriptor("ref-interaction-type").tableName()).isEqualTo("REF_INTERACTION_TYPE");
        assertThat(registry.descriptor("ref-journey-stage").relation()).isNotNull();
        assertThat(registry.descriptor("ref-journey-stage").relation().targetResource()).isEqualTo("ref-journey");
        assertThat(registry.descriptor("ref-journey-event-type").relation().targetResource()).isEqualTo("ref-journey-stage");
    }

    @Test
    void exposesAnalyticsAndRecommendationLookups() {
        assertThat(registry.descriptor("ref-metric").tableName()).isEqualTo("REF_METRIC");
        assertThat(registry.descriptor("ref-metric-unit").tableName()).isEqualTo("REF_METRIC_UNIT");
        assertThat(registry.descriptor("ref-model").tableName()).isEqualTo("REF_MODEL");
        assertThat(registry.descriptor("ref-recommendation-status").tableName()).isEqualTo("REF_RECOMMENDATION_STATUS");
        assertThat(registry.descriptor("ref-recommendation-type").tableName()).isEqualTo("REF_RECOMMENDATION_TYPE");
        assertThat(registry.descriptor("ref-score-band").tableName()).isEqualTo("REF_SCORE_BAND");
        assertThat(registry.descriptor("ref-score-type").tableName()).isEqualTo("REF_SCORE_TYPE");
    }

}
