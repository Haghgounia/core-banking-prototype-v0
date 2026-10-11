package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.ColumnDescriptor;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TableDescriptor;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanTypeClassificationR23Test {
    private static Map<String, Object> row(int group, int obligation) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("LOAN_GROUP", group);
        record.put("OBLIGATION_TYPE", obligation);
        return record;
    }

    @Test void fourGroupsAndTwoCommitmentsPlusNotApplicableAreStable() {
        assertThat(LoanTypeClassification.loanGroupOptions()).extracting(opt -> opt.value())
                .containsExactly(1, 2, 3, 4);
        assertThat(LoanTypeClassification.obligationOptions()).extracting(opt -> opt.value())
                .containsExactly(0, 1, 2);
    }

    @Test void normalGroupsAcceptOnlyNotApplicableAndSetPausedInterestToZero() {
        for (int group : new int[]{1, 2, 3}) {
            Map<String, Object> record = row(group, 0);
            LoanTypeClassification.validateForSave(record, null);
            assertThat(record.get("INTEREST_METHOD")).isEqualTo(0);
        }
    }

    @Test void bothCommitmentTypesAreValidOnlyForCommitmentGroup() {
        for (int obligation : new int[]{1, 2}) {
            Map<String, Object> record = row(4, obligation);
            LoanTypeClassification.validateForSave(record, null);
            assertThat(record.get("INTEREST_METHOD")).isEqualTo(0);
        }
        assertThatThrownBy(() -> LoanTypeClassification.validateForSave(row(4, 0), null))
                .isInstanceOf(ProductBuilderValidationException.class);
        assertThatThrownBy(() -> LoanTypeClassification.validateForSave(row(2, 1), null))
                .isInstanceOf(ProductBuilderValidationException.class);
    }

    @Test void unknownOrFractionalCodesAreRejected() {
        assertThatThrownBy(() -> LoanTypeClassification.validateForSave(row(5, 0), null))
                .isInstanceOf(ProductBuilderValidationException.class);
        assertThatThrownBy(() -> LoanTypeClassification.validateForSave(row(4, 3), null))
                .isInstanceOf(ProductBuilderValidationException.class);
        Map<String, Object> invalid = row(1, 0);
        invalid.put("LOAN_GROUP", 1.5);
        assertThatThrownBy(() -> LoanTypeClassification.validateForSave(invalid, null))
                .isInstanceOf(ProductBuilderValidationException.class);
    }

    @Test void pausedInterestRejectsNewNonZero() {
        Map<String, Object> invalid = row(1, 0);
        invalid.put("INTEREST_METHOD", 2);
        assertThatThrownBy(() -> LoanTypeClassification.validateForSave(invalid, null))
                .isInstanceOf(ProductBuilderValidationException.class);
    }

    @Test void existingInterestIsPreservedButCannotChange() {
        Map<String, Object> previous = row(1, 0);
        previous.put("INTEREST_METHOD", 2);
        Map<String, Object> current = row(1, 0);
        current.put("INTEREST_METHOD", 2);
        LoanTypeClassification.validateForSave(current, previous);
        assertThat(current.get("INTEREST_METHOD")).isEqualTo(2);
        current.put("INTEREST_METHOD", 0);
        assertThatThrownBy(() -> LoanTypeClassification.validateForSave(current, previous))
                .isInstanceOf(ProductBuilderValidationException.class);
    }

    @Test void descriptorExposesNumericControlledOptions() {
        var provider = new PdlReferenceOptionService(null, null, null);
        List<ColumnDescriptor> cols = List.of(
                col("LOAN_GROUP"), col("OBLIGATION_TYPE"), col("INTEREST_METHOD"));
        var descriptor = provider.enrich(new TableDescriptor("PDL", "LOAN_TYPE", "نوع عقد", "05", "Reference Data", "LOAN_TYPE_ID", cols));
        assertThat(descriptor.columns()).allMatch(ColumnDescriptor::referenceControlled);
        assertThat(descriptor.columns().get(0).options()).extracting(opt -> opt.value()).containsExactly(1, 2, 3, 4);
        assertThat(descriptor.columns().get(1).options()).extracting(opt -> opt.value()).containsExactly(0, 1, 2);
        assertThat(descriptor.columns().get(2).options()).extracting(opt -> opt.value()).containsExactly(0);
    }

    private static ColumnDescriptor col(String name) {
        return new ColumnDescriptor(name, name, "NUMBER", null, 1, 0, false, false, false, null, null, false, null,
                List.of(), false, null);
    }
}
