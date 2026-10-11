package com.behsazan.corebanking.productbuilder.application;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** PB-R24: loan contract grouping, applicant wildcard, classification pattern rules. */
class LoanProductContractR24Test {
    private final ProductBuilderBusinessValidator validator = new ProductBuilderBusinessValidator();

    @Test void businessGroupsMapToFamiliesButNeverIndividualContracts() {
        assertThat(LoanTypeClassification.groupForProductFamily("QARD_HASAN_LOAN")).isEqualTo(1);
        assertThat(LoanTypeClassification.groupForProductFamily("EXCHANGE_LOAN")).isEqualTo(2);
        assertThat(LoanTypeClassification.groupForProductFamily("PARTICIPATORY_LOAN")).isEqualTo(3);
        assertThat(LoanTypeClassification.groupForProductFamily("COMMITMENT_LOAN")).isEqualTo(4);
        assertThat(LoanTypeClassification.groupForProductFamily("MUDARABA")).isZero();
        assertThat(LoanTypeClassification.isLegacyRetailFamily("RETAIL_LOAN")).isTrue();
    }

    @Test void applicantNatureAllIsValid() {
        validator.validate("LOAN_PRODUCT_PROFILE", Map.of("PARTY_NATURE_CODE", "ALL"));
        validator.validate("LOAN_PRODUCT_PROFILE", Map.of("PARTY_NATURE_CODE", "NATURAL"));
        validator.validate("LOAN_PRODUCT_PROFILE", Map.of("PARTY_NATURE_CODE", "LEGAL"));
    }

    @Test void invalidApplicantNatureIsRejected() {
        for (String invalid : new String[]{"", "UNKNOWN", "BOTH", "PERSON"}) {
            assertThatThrownBy(() -> validator.validate("LOAN_PRODUCT_PROFILE", Map.of("PARTY_NATURE_CODE", invalid)))
                    .isInstanceOf(ProductBuilderValidationException.class);
        }
    }

    private Map<String, Object> validPattern() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("OVERDUE_DURATION", 1);
        row.put("PASTDUE_DURATION", 30);
        row.put("DOUBTFUL_DURATION", 90);
        row.put("TRANSFER_CATEGORY_METHOD", 0);
        row.put("TRANSFER_PRINCIPLE", 0);
        row.put("TRANSFER_INTEREST", 1);
        row.put("TRANSFER_PENALTY", 0);
        row.put("TRANSFER_COMMISSION", 1);
        row.put("PARAMETER_STATUS_CODE", "DRAFT");
        return row;
    }

    @Test void classificationPatternAcceptsOracleSizedValues() {
        validator.validate("FACILITY_OPERATION_PARAMETER", validPattern());
        var row = validPattern();
        row.put("OVERDUE_DURATION", 99);
        row.put("TRANSFER_CATEGORY_METHOD", 9);
        validator.validate("FACILITY_OPERATION_PARAMETER", row);
    }

    @Test void patternRejectsZeroAndOutOfRangeDurations() {
        for (String key : new String[]{"OVERDUE_DURATION", "PASTDUE_DURATION", "DOUBTFUL_DURATION"}) {
            for (int invalid : new int[]{0, 100, -1}) {
                var row = validPattern();
                row.put(key, invalid);
                assertThatThrownBy(() -> validator.validate("FACILITY_OPERATION_PARAMETER", row))
                        .isInstanceOf(ProductBuilderValidationException.class);
            }
        }
    }

    @Test void transferMethodMustFitOracleNumberOneWithoutInventedSemantics() {
        for (Object invalid : new Object[]{-1, 10, 1.5, null}) {
            var row = validPattern();
            row.put("TRANSFER_CATEGORY_METHOD", invalid);
            assertThatThrownBy(() -> validator.validate("FACILITY_OPERATION_PARAMETER", row))
                    .isInstanceOf(ProductBuilderValidationException.class);
        }
    }

    @Test void transferFlagsMustBeBinary() {
        for (String key : new String[]{"TRANSFER_PRINCIPLE", "TRANSFER_INTEREST", "TRANSFER_PENALTY", "TRANSFER_COMMISSION"}) {
            var row = validPattern();
            row.put(key, 2);
            assertThatThrownBy(() -> validator.validate("FACILITY_OPERATION_PARAMETER", row))
                    .isInstanceOf(ProductBuilderValidationException.class);
        }
    }

    @Test void operationalPatternStatusIsRequired() {
        var row = validPattern();
        row.put("PARAMETER_STATUS_CODE", "");
        assertThatThrownBy(() -> validator.validate("FACILITY_OPERATION_PARAMETER", row))
                .isInstanceOf(ProductBuilderValidationException.class);
    }
}
