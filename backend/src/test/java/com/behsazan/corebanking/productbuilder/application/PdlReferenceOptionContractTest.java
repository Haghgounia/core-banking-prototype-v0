package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.SelectOption;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PdlReferenceOptionContractTest {
    private static SelectOption check(String code) { return new SelectOption(code, code, code); }
    private static SelectOption ref(String code, String label) { return new SelectOption(code, code, label); }

    @Test
    void jointOwnershipTranslatesLegacyDpsNumericCodesToPdlConstraintDomain() {
        List<SelectOption> options = PdlReferenceOptionService.mapJointOptions("OWNERSHIP_TYPE_CODE",
                List.of(check("SINGLE"), check("JOINT")),
                List.of(ref("1", "انفرادی"), ref("2", "مشترک"), ref("3", "صغیر با ولی")));
        assertThat(options.stream().map(SelectOption::code).toList()).containsExactly("SINGLE", "JOINT");
        assertThat(options.stream().map(SelectOption::label).toList()).containsExactly("انفرادی", "مشترک");
    }

    @Test
    void signingAndProfitDistributionTranslateOnlyExactEquivalences() {
        Map<String, String> signing = PdlReferenceOptionService.mapJointOptions("SIGNING_RULE_CODE",
                List.of(check("ANY_TO_SIGN"), check("BOTH_TO_SIGN"), check("N_OF_M")),
                List.of(ref("1", "امضای هر یک"), ref("2", "امضای همه"), ref("3", "امضای هر دو نفر")))
                .stream().collect(Collectors.toMap(SelectOption::code, SelectOption::label));
        assertThat(signing).containsExactlyInAnyOrderEntriesOf(Map.of(
                "ANY_TO_SIGN", "امضای هر یک", "BOTH_TO_SIGN", "امضای همه"));
        Map<String, String> profits = PdlReferenceOptionService.mapJointOptions("PROFIT_DISTRIBUTION_CODE",
                List.of(check("EQUAL"), check("OWNERSHIP_SHARE"), check("CUSTOM_PERCENT")),
                List.of(ref("1", "مساوی"), ref("2", "براساس سهم مالکیت"), ref("3", "درصد سفارشی"), ref("4", "مالک اصلی")))
                .stream().collect(Collectors.toMap(SelectOption::code, SelectOption::label));
        assertThat(profits).containsExactlyInAnyOrderEntriesOf(Map.of(
                "EQUAL", "مساوی", "OWNERSHIP_SHARE", "براساس سهم مالکیت", "CUSTOM_PERCENT", "درصد سفارشی"));
    }

    @Test
    void doesNotPersistNumericCodeWithoutAnActualPdlConstraint() {
        assertThat(PdlReferenceOptionService.mapJointOptions("OWNERSHIP_TYPE_CODE", List.of(),
                List.of(ref("2", "مشترک")))).isEmpty();
    }

    @Test
    void alreadySemanticDpsCodesArePreservedIfAllowedByPdl() {
        assertThat(PdlReferenceOptionService.mapJointOptions("OWNERSHIP_TYPE_CODE",
                List.of(check("SINGLE"), check("JOINT")), List.of(ref("JOINT", "مشترک"))))
                .extracting(SelectOption::code).containsExactly("JOINT");
    }
    @Test
    void preservesLegacyNumericCodeWhenThatIsWhatActualPdlCheckAccepts() {
        assertThat(PdlReferenceOptionService.mapJointOptions("OWNERSHIP_TYPE_CODE",
                List.of(check("1"), check("2")), List.of(ref("1", "انفرادی"), ref("2", "مشترک"))))
                .extracting(SelectOption::code).containsExactly("1", "2");
    }

}
