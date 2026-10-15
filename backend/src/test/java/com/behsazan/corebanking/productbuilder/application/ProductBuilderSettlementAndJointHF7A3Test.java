package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.SelectOption;
import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductBuilderSettlementAndJointHF7A3Test {
    private final ProductBuilderBusinessValidator validator = new ProductBuilderBusinessValidator();
    private static Map<String,Object> settlement(String method,String destination) {
        Map<String,Object> row = new LinkedHashMap<>();
        row.put("SETTLEMENT_COMPONENT_CODE", "COMMISSION");
        row.put("SETTLEMENT_METHOD_CODE", method);
        row.put("DESTINATION_CODE", destination);
        row.put("SEQUENCE_NO", 1);
        row.put("IS_MANDATORY", 1);
        return row;
    }
    private static Map<String,Object> joint(String ownership,int min,int max,int signers,String profit) {
        Map<String,Object> row = new LinkedHashMap<>();
        row.put("OWNERSHIP_TYPE_CODE", ownership);
        row.put("MIN_JOINT_OWNER_COUNT", min);
        row.put("MAX_JOINT_OWNER_COUNT", max);
        row.put("SIGNING_RULE_CODE", "ANY_TO_SIGN");
        row.put("MIN_REQUIRED_SIGNER_COUNT", signers);
        row.put("PROFIT_DISTRIBUTION_CODE", profit);
        return row;
    }
    @Test void deductionAndWaiverHaveNoDestination() {
        validator.validate("DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE", settlement("DEDUCT", null));
        validator.validate("DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE", settlement("WAIVE", null));
        for (String method: List.of("DEDUCT","WAIVE"))
            assertThatThrownBy(() -> validator.validate("DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",settlement(method,"CUSTOMER_ACCOUNT")))
                .isInstanceOf(ProductBuilderValidationException.class);
    }
    @Test void payAndTransferRequireCompatibleDestinations() {
        validator.validate("DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",settlement("PAY","CASH"));
        validator.validate("DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",settlement("TRANSFER","BANK_ACCOUNT"));
        for (String method: List.of("PAY","TRANSFER"))
            assertThatThrownBy(() -> validator.validate("DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",settlement(method,null)))
                .isInstanceOf(ProductBuilderValidationException.class);
        assertThatThrownBy(() -> validator.validate("DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",settlement("TRANSFER","CASH")))
            .isInstanceOf(ProductBuilderValidationException.class);
        assertThatThrownBy(() -> validator.validate("DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",settlement("PAY","BANK_ACCOUNT")))
            .isInstanceOf(ProductBuilderValidationException.class);
        assertThatThrownBy(() -> validator.validate("DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",settlement("UNREVIEWED",null)))
            .isInstanceOf(ProductBuilderValidationException.class);
    }
    @Test void individualOwnershipHasOneOwnerSignerAndNoProfitAllocation() {
        validator.validate("DEPOSIT_PRODUCT_JOINT_RULE",joint("SINGLE",1,1,1,null));
        validator.validate("DEPOSIT_PRODUCT_JOINT_RULE",joint("INDIVIDUAL",1,1,1,null));
        validator.validate("DEPOSIT_PRODUCT_JOINT_RULE",joint("1",1,1,1,null));
        assertThatThrownBy(() -> validator.validate("DEPOSIT_PRODUCT_JOINT_RULE",joint("SINGLE",1,2,1,null)))
            .isInstanceOf(ProductBuilderValidationException.class);
        assertThatThrownBy(() -> validator.validate("DEPOSIT_PRODUCT_JOINT_RULE",joint("SINGLE",1,1,1,"EQUAL")))
            .isInstanceOf(ProductBuilderValidationException.class);
        assertThatThrownBy(() -> validator.validate("DEPOSIT_PRODUCT_JOINT_RULE",joint("JOINT",1,2,1,null)))
            .isInstanceOf(ProductBuilderValidationException.class);
        validator.validate("DEPOSIT_PRODUCT_JOINT_RULE",joint("JOINT",2,4,2,"EQUAL"));
    }
    @Test void mappedIndividualOptionPersistsOnlyCodePermittedByActualCheck() {
        List<SelectOption> actual = PdlReferenceOptionService.mapJointOptions("OWNERSHIP_TYPE_CODE",
            List.of(new SelectOption("INDIVIDUAL","INDIVIDUAL","INDIVIDUAL"),new SelectOption("JOINT","JOINT","JOINT")),
            List.of(new SelectOption("1","1","\u0627\u0646\u0641\u0631\u0627\u062f\u06cc"),new SelectOption("2","2","\u0645\u0634\u062a\u0631\u06a9")));
        assertThat(actual.stream().map(SelectOption::code).toList()).containsExactly("INDIVIDUAL","JOINT");
    }
}
