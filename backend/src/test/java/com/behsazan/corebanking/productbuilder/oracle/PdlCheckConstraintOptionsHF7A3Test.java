package com.behsazan.corebanking.productbuilder.oracle;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.SelectOption;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class PdlCheckConstraintOptionsHF7A3Test {
    @Test void parsesOracleQuotedIdentifierInList() {
        var allowed = PdlProductBuilderRepository.parseCheckOptions(List.of(
                "(\"OWNERSHIP_TYPE_CODE\" IN ('INDIVIDUAL','JOINT'))",
                "\"SIGNING_RULE_CODE\" IN ('ANY_TO_SIGN','BOTH_TO_SIGN')"));
        assertThat(allowed.get("OWNERSHIP_TYPE_CODE").stream().map(SelectOption::code).toList())
            .containsExactly("INDIVIDUAL","JOINT");
        assertThat(allowed.get("SIGNING_RULE_CODE").stream().map(SelectOption::code).toList())
            .containsExactly("ANY_TO_SIGN","BOTH_TO_SIGN");
    }
    @Test void parsesAlternativesFromEqualityCheckAndQuotedLiteral() {
        var allowed = PdlProductBuilderRepository.parseCheckOptions(List.of(
                "(\"OWNERSHIP_TYPE_CODE\" = 'SINGLE' OR \"OWNERSHIP_TYPE_CODE\" = 'JOINT')"));
        assertThat(allowed.get("OWNERSHIP_TYPE_CODE").stream().map(SelectOption::code).toList())
            .containsExactly("SINGLE","JOINT");
    }
    @Test void intersectsTwoIndependentCheckConstraints() {
        var allowed = PdlProductBuilderRepository.parseCheckOptions(List.of(
                "\"OWNERSHIP_TYPE_CODE\" IN ('SINGLE','JOINT')",
                "OWNERSHIP_TYPE_CODE IN ('JOINT')"));
        assertThat(allowed.get("OWNERSHIP_TYPE_CODE").stream().map(SelectOption::code).toList())
            .containsExactly("JOINT");
    }
    @Test void handlesEscapedSqlStringAndNumericCodes() {
        var allowed = PdlProductBuilderRepository.parseCheckOptions(List.of(
                "\"VALUE_CODE\" IN ('O''BRIEN','FOO')",
                "\"OWNER_COUNT\" IN (1,2)"));
        assertThat(allowed.get("VALUE_CODE").stream().map(SelectOption::code).toList())
            .containsExactly("O'BRIEN","FOO");
        assertThat(allowed.get("OWNER_COUNT").stream().map(SelectOption::code).toList())
            .containsExactly("1","2");
    }
}
