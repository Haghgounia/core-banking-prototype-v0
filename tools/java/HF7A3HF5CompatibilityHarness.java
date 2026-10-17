package com.behsazan.corebanking.productbuilder.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Non-JUnit, read-only regression harness for HF5 source contract. */
public final class HF7A3HF5CompatibilityHarness {
    private static final LocalDate FROM = LocalDate.parse("2026-10-10");
    private static final LocalDate TO = LocalDate.parse("2026-10-20");
    private static final ProductBuilderBusinessValidator VALIDATOR = new ProductBuilderBusinessValidator();
    private static int passed = 0;

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        passed++;
        System.out.println("PASS | " + label);
    }
    private static ProductRuleHierarchyAudit.AuditResult audit(String parentTable,
            List<? extends Map<String, ?>> parents, String domain,
            ProductRuleHierarchyAudit.ChildLoader loader) {
        return ProductRuleHierarchyAudit.audit(parentTable, parents, FROM, TO, domain, loader, VALIDATOR);
    }
    private static Map<String,Object> channelChild() {
        return Map.of("CHANNEL_RULE_ID",81L, "OPERATION_CODE","OPEN_ACCOUNT", "IS_ALLOWED",1,
                "REQUIRES_ADDITIONAL_AUTH",0);
    }

    public static void main(String[] args) {
        var badFlag = ProductRuleEvidenceEvaluator.evaluate("DEPOSIT_PRODUCT_ALLOWED_TERM",
                List.of(Map.of("IS_ACTIVE",9)), FROM, TO);
        check(badFlag.unrecognizedStatuses() == 1 && badFlag.conflict(), "invalid IS_ACTIVE rejects publication");
        var decimalActive = ProductRuleEvidenceEvaluator.evaluate("DEPOSIT_PRODUCT_ALLOWED_TERM",
                List.of(Map.of("IS_ACTIVE",new BigDecimal("1.0"))), FROM, TO);
        check(decimalActive.effectiveCount() == 1, "Oracle NUMBER(1) decimal one recognized active");
        check(ProductRuleEvidenceEvaluator.evaluate("DEPOSIT_PRODUCT_ALLOWED_TERM",
                List.of(Map.of("IS_ACTIVE",new BigDecimal("0.0"))), FROM, TO).effectiveCount() == 0,
                "Oracle NUMBER(1) zero recognized inactive");
        check(ProductRuleEvidenceEvaluator.evaluate("PRODUCT_REQUIRED_DOCUMENT", List.of(
                Map.of("DOCUMENT_TYPE_ID",17,"REQUIREMENT_STAGE_CODE","OPENING","RULE_STATUS_CODE","ACTIVE"),
                Map.of("DOCUMENT_TYPE_ID",17,"REQUIREMENT_STAGE_CODE","OPENING","RULE_STATUS_CODE","ACTIVE")),
                FROM, TO).conflict(), "duplicate document per stage denied");
        check(ProductRuleEvidenceEvaluator.evaluate("PRODUCT_REQUIRED_INQUIRY", List.of(
                Map.of("INQUIRY_TYPE_CODE","KYC","REQUIREMENT_STAGE_CODE","APPROVAL","RULE_STATUS_CODE","ACTIVE"),
                Map.of("INQUIRY_TYPE_CODE","KYC","REQUIREMENT_STAGE_CODE","APPROVAL","RULE_STATUS_CODE","ACTIVE")),
                FROM, TO).conflict(), "duplicate inquiry per stage denied");
        check(!ProductRuleEvidenceEvaluator.evaluate("PRODUCT_REQUIRED_DOCUMENT", List.of(
                Map.of("DOCUMENT_TYPE_ID",17,"REQUIREMENT_STAGE_CODE","OPENING"),
                Map.of("DOCUMENT_TYPE_ID",17,"REQUIREMENT_STAGE_CODE","APPROVAL")), FROM, TO).conflict(),
                "documents in distinct stages are allowed");
        check(!ProductRuleEvidenceEvaluator.evaluate("PRODUCT_REQUIRED_INQUIRY", List.of(
                Map.of("INQUIRY_TYPE_CODE","KYC","REQUIREMENT_STAGE_CODE","OPENING"),
                Map.of("INQUIRY_TYPE_CODE","AML","REQUIREMENT_STAGE_CODE","OPENING")), FROM, TO).conflict(),
                "distinct inquiries do not conflict");

        var overlappingTierOne = Map.<String,Object>of("TIER_NO",1,"RATE_VALUE","0.1",
                "VALID_FROM","2026-10-10","VALID_TO","2026-10-15");
        var overlappingTierTwo = Map.<String,Object>of("TIER_NO",1,"RATE_VALUE","0.1",
                "VALID_FROM","2026-10-15","VALID_TO","2026-10-20");
        var disjointTierTwo = Map.<String,Object>of("TIER_NO",1,"RATE_VALUE","0.1",
                "VALID_FROM","2026-10-16","VALID_TO","2026-10-20");
        check(ProductRuleEvidenceEvaluator.evaluate("PRODUCT_RATE_TIER", List.of(
                overlappingTierOne, overlappingTierTwo), FROM, TO).conflict(),
                "same tier number overlapping at inclusive date is a conflict");
        check(!ProductRuleEvidenceEvaluator.evaluate("PRODUCT_RATE_TIER", List.of(
                overlappingTierOne, disjointTierTwo), FROM, TO).conflict(),
                "same tier number in disjoint periods is allowed");

        var deniedChannel = audit("PRODUCT_CHANNEL_RULE",
                List.of(Map.of("CHANNEL_RULE_ID",81L,"IS_ALLOWED",0)),"DEPOSIT",
                (table, fk, id) -> List.of(channelChild()));
        check(deniedChannel.invalidCount() == 1 && deniedChannel.firstProblem().contains("غیرمجاز"),
                "denied channel cannot grant child operation");
        check(audit("PRODUCT_CHANNEL_RULE",List.of(Map.of("CHANNEL_RULE_ID",81L,"IS_ALLOWED",1)), "DEPOSIT",
                (table,fk,id) -> List.of(channelChild())).valid(), "allowed channel with valid child is accepted");
        check(audit("PRODUCT_CHANNEL_RULE",List.of(Map.of("CHANNEL_RULE_ID",81L,"IS_ALLOWED",1)), "DEPOSIT",
                (table,fk,id) -> List.of()).valid(), "optional child absence does not invent authorization");
        check(audit("PRODUCT_CHANNEL_RULE",List.of(Map.of("CHANNEL_RULE_ID",81L,"IS_ALLOWED",1)), "DEPOSIT",
                (table,fk,id) -> List.of(Map.of("CHANNEL_RULE_ID",99L,"OPERATION_CODE","OPEN_ACCOUNT",
                        "IS_ALLOWED",1,"REQUIRES_ADDITIONAL_AUTH",0))).invalidCount() > 0,
                "wrong parent link is invalid");
        check(audit("PRODUCT_CHANNEL_RULE",List.of(Map.of("CHANNEL_RULE_ID",81L,"IS_ALLOWED",1)), "DEPOSIT",
                (table,fk,id) -> { throw new IllegalStateException("down"); }).invalidCount() > 0,
                "child lookup failure is fail closed");
        check(audit("PRODUCT_CHANNEL_RULE",List.of(Map.of("CHANNEL_RULE_ID",81L,"IS_ALLOWED",1)), "DEPOSIT",
                (table,fk,id) -> List.of(Map.of("CHANNEL_RULE_ID",81L,"OPERATION_CODE","OPEN_ACCOUNT",
                        "IS_ALLOWED",9,"REQUIRES_ADDITIONAL_AUTH",0))).invalidCount() > 0,
                "invalid child permission rejected");

        var malformedLoan = Map.<String,Object>of("ELIGIBILITY_RULE_ID",91L,
                "REQUIRED_GUARANTOR_COUNT",2.5,"CBI_TRACKING_REQUIRED",0,
                "CREDIT_INQUIRY_REQUIRED",0,"AML_CHECK_REQUIRED",0);
        check(audit("PRODUCT_ELIGIBILITY_RULE",List.of(Map.of("ELIGIBILITY_RULE_ID",91L)), "LOAN",
                (table,fk,id)->"LOAN_ELIGIBILITY_EXTENSION".equals(table) ? List.of(malformedLoan) : List.of()).invalidCount()>0,
                "invalid optional loan eligibility extension rejected");
        check(audit("PRODUCT_ELIGIBILITY_RULE",List.of(Map.of("ELIGIBILITY_RULE_ID",91L)), "DEPOSIT",
                (table,fk,id)-> {
                    if (!"PRODUCT_ELIGIBILITY_CRITERION".equals(table))
                        throw new AssertionError("deposit may load only common eligibility criteria");
                    return List.of();
                }).valid(), "deposit loads common criteria but never loan-only extension");
        check(audit("PRODUCT_ELIGIBILITY_RULE",List.of(Map.of("ELIGIBILITY_RULE_ID",91L)), "LOAN",
                (table,fk,id)->List.of()).valid(), "optional loan extension may be absent");

        var invalidClosure = Map.<String,Object>of("CLOSURE_RULE_ID",99L,"CHECK_TYPE_CODE","HOLD",
                "FAILURE_ACTION_CODE","BLOCK","IS_MANDATORY",1,"SEQUENCE_NO",0);
        check(audit("DEPOSIT_PRODUCT_CLOSURE_RULE",List.of(Map.of("CLOSURE_RULE_ID",99L)), "DEPOSIT",
                (table,fk,id)->table.equals("DEPOSIT_PRODUCT_CLOSURE_PRECHECK")
                        ?List.of(invalidClosure):List.of()).invalidCount()>0,
                "invalid closure precheck sequence rejected");
        check(audit("DEPOSIT_PRODUCT_CLOSURE_RULE",List.of(Map.of("CLOSURE_RULE_ID",99L)), "DEPOSIT",
                (table,fk,id)->List.of()).valid(), "optional closure children may be absent");
        check(audit("DEPOSIT_PRODUCT_CLOSURE_RULE",List.of(Map.of("CLOSURE_RULE_ID",99L)), "LOAN",
                (table,fk,id)-> { throw new AssertionError("must not query deposit closure"); }).valid(),
                "loan domain does not load deposit closure children");

        check(!LoanCivilPartnershipPolicy.matches(Map.of("LOAN_GROUP",3,"LOAN_TYPE_CODE","MUDARABA")),
                "mudaraba is not civil partnership");
        check(LoanCivilPartnershipPolicy.matches(Map.of("LOAN_GROUP",3,"LOAN_TYPE_CODE","CIVIL_PARTNERSHIP")),
                "civil partnership contract classified correctly");
        check(!LoanCivilPartnershipPolicy.matches(Map.of("LOAN_GROUP",2,"LOAN_TYPE_CODE","CIVIL_PARTNERSHIP")),
                "wrong group cannot masquerade as civil partnership");

        var termChild = new LinkedHashMap<String,Object>(Map.of("TERM_RULE_ID",7L,"TERM_UNIT_CODE","DAY",
                "TERM_VALUE",30,"IS_ACTIVE",1,"EFFECTIVE_FROM_DATE","2026-10-10","EFFECTIVE_TO_DATE","2026-10-11"));
        var termParent = List.<Map<String,Object>>of(Map.of("TERM_RULE_ID",7L,"IS_ACTIVE",1));
        check(ProductRuleHierarchyAudit.allowedTerms(termParent,FROM,TO,
                (table,fk,id)->List.of(termChild),VALIDATOR).conflict(), "term child date gap rejected");
        termChild.put("EFFECTIVE_TO_DATE","2026-10-20");
        check(ProductRuleHierarchyAudit.allowedTerms(termParent,FROM,TO,
                (table,fk,id)->List.of(termChild),VALIDATOR).valid(), "complete term child accepted");

        var tier = new LinkedHashMap<String,Object>(Map.of("PRICING_COMPONENT_ID",12L,
                "TIER_NO",1,"RATE_VALUE",new BigDecimal("0.15"),"VALID_FROM","2026-10-10","VALID_TO","2026-10-11"));
        var p = List.<Map<String,Object>>of(Map.of("PRICING_RULE_ID",11L,"IS_TIERED_RATE",1,"PRICING_METHOD_CODE","TIERED"));
        var c = Map.<String,Object>of("PRICING_RULE_ID",11L,"PRICING_COMPONENT_ID",12L);
        ProductRuleHierarchyAudit.ChildLoader rateLoader = (table,fk,id)->
                table.equals("PRODUCT_PRICING_COMPONENT") ? List.of(c) : List.of(tier);
        check(ProductRuleHierarchyAudit.pricingTiers(p,FROM,TO,rateLoader,VALIDATOR).conflict(),
                "tiered rate gap rejected");
        tier.put("VALID_TO","2026-10-20");
        check(ProductRuleHierarchyAudit.pricingTiers(p,FROM,TO,rateLoader,VALIDATOR).valid(),
                "complete tiered pricing accepted");

        System.out.println("PDL_R30_HF7A3_HF5_JAVA_PASS=" + passed + " FAIL=0");
    }
}
