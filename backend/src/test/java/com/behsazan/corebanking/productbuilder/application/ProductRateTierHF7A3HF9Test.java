package com.behsazan.corebanking.productbuilder.application;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** HF9: read-only, real Java policy and hierarchy regression; never calls Oracle. */
class ProductRateTierHF7A3HF9Test {
    private static final LocalDate FROM = LocalDate.parse("2026-10-10");
    private static final LocalDate TO = LocalDate.parse("2026-10-30");
    private static final ProductBuilderBusinessValidator VALIDATOR = new ProductBuilderBusinessValidator();
    private static Map<String,Object> row(Object... args) {
        Map<String,Object> result = new LinkedHashMap<>();
        for (int i=0;i<args.length;i+=2) result.put((String) args[i],args[i+1]);
        return result;
    }
    private static Map<String,Object> pricing() {
        return row("PRICING_RULE_ID", 813L, "PRICING_METHOD_CODE", "TIERED", "IS_TIERED_RATE", 1,
                "PRICING_PURPOSE_CODE", "DEPOSIT_PROFIT", "EARLY_TERMINATION_ALLOWED", 0);
    }
    private static Map<String,Object> component(long id,Object... bounds) {
        Map<String,Object> c = row("PRICING_COMPONENT_ID",id,"PRICING_RULE_ID",813L);
        c.putAll(row(bounds)); return c;
    }
    private static Map<String,Object> tier(long componentId,int number,Object... bounds) {
        Map<String,Object> t = row("PRICING_COMPONENT_ID",componentId,"TIER_NO",number,"RATE_VALUE","0.12");
        t.putAll(row(bounds)); return t;
    }
    private static ProductRuleHierarchyAudit.Finding audit(Map<String,Object> parent,
                           List<Map<String,Object>> components,
                           Map<Long,List<Map<String,Object>>> allTiers) {
        return ProductRuleHierarchyAudit.pricingTiers(List.of(parent),FROM,TO,(table,key,id)-> {
            if ("PRODUCT_PRICING_COMPONENT".equals(table)) return components;
            return allTiers.getOrDefault(id,List.of());
        }, VALIDATOR);
    }
    private static ProductRuleHierarchyAudit.Finding audit(List<Map<String,Object>> tiers) {
        return audit(pricing(),List.of(component(814L)),Map.of(814L,tiers));
    }
    private static ProductRuleHierarchyAudit.Finding audit(List<Map<String,Object>> tiers,Object... limits) {
        return audit(pricing(),List.of(component(814L,limits)),Map.of(814L,tiers));
    }
    private static void status(String expected,ProductRuleHierarchyAudit.Finding finding) {
        assertEquals(expected, finding.status(),finding.reason());
    }
    @Test void oneUnboundedAmountTierIsUnambiguous() {
        assertTrue(audit(List.of(tier(814L,1))).valid());
    }
    @Test void adjacentHalfOpenAmountIntervalsAreValid() {
        var first=tier(814L,1,"MIN_AMOUNT","0.0000","MAX_AMOUNT","100.0000");
        var next=tier(814L,2,"MIN_AMOUNT","100.0000");
        assertTrue(audit(List.of(first,next),"MIN_AMOUNT","0.0000").valid());
    }
    @Test void exactUpperBoundarySelectsOnlyNextTier() {
        var first=tier(814L,1,"MIN_AMOUNT",0,"MAX_AMOUNT",100);
        var next=tier(814L,2,"MIN_AMOUNT",100);
        var tiers=List.of(first,next);
        assertEquals(1L,ProductPricingSingleRatePolicy.previewAmountOnlyRate(tiers,new BigDecimal("99.9999"),FROM).tierNo());
        assertEquals(2L,ProductPricingSingleRatePolicy.previewAmountOnlyRate(tiers,new BigDecimal("100.0000"),FROM).tierNo());
        assertEquals(new BigDecimal("0.12"),ProductPricingSingleRatePolicy.previewAmountOnlyRate(tiers,new BigDecimal("500"),FROM).rate());
    }
    @Test void nullMinimumDoesNotGetCoercedToZero() {
        var t=tier(814L,1,"MAX_AMOUNT",100);
        assertTrue(audit(List.of(t),"MAX_AMOUNT",100).valid());
        assertEquals(1L,ProductPricingSingleRatePolicy.previewAmountOnlyRate(List.of(t),new BigDecimal("-20"),FROM).tierNo());
    }
    @Test void nullMaximumIsUnbounded() {
        var t=tier(814L,1,"MIN_AMOUNT",20);
        assertTrue(audit(List.of(t),"MIN_AMOUNT",20).valid());
        assertEquals(1L,ProductPricingSingleRatePolicy.previewAmountOnlyRate(List.of(t),new BigDecimal("99999999999"),FROM).tierNo());
    }
    @Test void equalAmountBoundsAreEmptyAndInvalid() {
        status("INVALID_SEMANTICS",audit(List.of(tier(814L,1,"MIN_AMOUNT",100,"MAX_AMOUNT",100))));
        assertThrows(ProductBuilderValidationException.class,()->VALIDATOR.validate("PRODUCT_RATE_TIER",
                tier(814L,1,"MIN_AMOUNT",0,"MAX_AMOUNT",0)));
    }
    @Test void negativeAmountBoundaryStillRejected() {
        status("INVALID_SEMANTICS",audit(List.of(tier(814L,1,"MIN_AMOUNT",-1))));
    }
    @Test void partialAmountOverlapBlocksPublication() {
        status("CONFLICT",audit(List.of(tier(814L,1,"MIN_AMOUNT",0,"MAX_AMOUNT",110),
                tier(814L,2,"MIN_AMOUNT",100)),"MIN_AMOUNT",0));
    }
    @Test void interiorAmountGapBlocksPublication() {
        status("CONFLICT",audit(List.of(tier(814L,1,"MIN_AMOUNT",0,"MAX_AMOUNT",100),
                tier(814L,2,"MIN_AMOUNT",101)),"MIN_AMOUNT",0));
    }
    @Test void missingLowerCoverageBlocksPublication() {
        status("CONFLICT",audit(List.of(tier(814L,1,"MIN_AMOUNT",1)),"MIN_AMOUNT",0));
    }
    @Test void missingUpperCoverageBlocksPublication() {
        status("CONFLICT",audit(List.of(tier(814L,1,"MIN_AMOUNT",0,"MAX_AMOUNT",150)),
                "MIN_AMOUNT",0,"MAX_AMOUNT",200));
    }
    @Test void outsideComponentEnvelopeIsRejected() {
        status("INVALID_SEMANTICS",audit(List.of(tier(814L,1,"MIN_AMOUNT",0)),"MIN_AMOUNT",10));
    }
    @Test void eachComponentMustCarryItsOwnTierCoverage() {
        Map<Long,List<Map<String,Object>>> tiers = Map.of(814L,List.of(tier(814L,1)),815L,List.of());
        status("CONFLICT",audit(pricing(),List.of(component(814L),component(815L)),tiers));
    }
    @Test void separateValidComponentsAreNotFalselyInConflict() {
        Map<Long,List<Map<String,Object>>> tiers = Map.of(814L,List.of(tier(814L,1)),815L,List.of(tier(815L,1)));
        assertTrue(audit(pricing(),List.of(component(814L),component(815L)),tiers).valid());
    }
    @Test void oneMissingDateInTierCoverageFails() {
        var t=tier(814L,1,"VALID_FROM","2026-10-11");
        status("CONFLICT",audit(List.of(t)));
    }
    @Test void adjacentCalendarDayCutoverIsAllowed() {
        var a=tier(814L,1,"VALID_FROM","2026-10-10","VALID_TO","2026-10-20");
        var b=tier(814L,2,"VALID_FROM","2026-10-21","VALID_TO","2026-10-30");
        assertTrue(audit(List.of(a,b)).valid());
    }
    @Test void sameDayCutoverWithoutDistinctAmountOrTermIsAmbiguous() {
        var a=tier(814L,1,"VALID_FROM","2026-10-10","VALID_TO","2026-10-20");
        var b=tier(814L,2,"VALID_FROM","2026-10-20","VALID_TO","2026-10-30");
        status("CONFLICT",audit(List.of(a,b)));
    }
    @Test void inactiveTierNeverSuppliesApprovalCoverage() {
        var inactive=tier(814L,1);inactive.put("IS_ACTIVE",0);
        status("CONFLICT",audit(List.of(inactive)));
    }
    @Test void unrecognizedTierStatusIsNotSilentlyIgnored() {
        var invalid=tier(814L,1);invalid.put("IS_ACTIVE",9);
        status("INVALID_SEMANTICS",audit(List.of(invalid)));
    }
    @Test void incompleteTermBoundsAreRejectedWithoutInferredNullMeaning() {
        status("INVALID_SEMANTICS",audit(List.of(tier(814L,1,
                "MIN_TERM_VALUE",3,"TERM_UNIT_CODE","MONTH"))));
    }
    @Test void numericDurationCodeCannotBeGuessed() {
        status("INVALID_SEMANTICS",audit(List.of(tier(814L,1,
                "MIN_TERM_VALUE",3,"MAX_TERM_VALUE",12,"TERM_UNIT_CODE","2"))));
    }
    @Test void explicitDurationRequiresApprovedUniverseAndBoundarySemantics() {
        var result=audit(List.of(tier(814L,1,"MIN_TERM_VALUE",3,"MAX_TERM_VALUE",12,"TERM_UNIT_CODE","MONTH")));
        status("INVALID_SEMANTICS",result);
        assertTrue(result.reason().contains("مدت"));
    }
    @Test void mixedDurationUnitsWithOverlappingAmountsCannotBeCompared() {
        var a=tier(814L,1,"MIN_AMOUNT",0,"MAX_AMOUNT",100,"MIN_TERM_VALUE",1,
                "MAX_TERM_VALUE",12,"TERM_UNIT_CODE","MONTH");
        var b=tier(814L,2,"MIN_AMOUNT",0,"MAX_AMOUNT",100,"MIN_TERM_VALUE",1,
                "MAX_TERM_VALUE",365,"TERM_UNIT_CODE","DAY");
        status("INVALID_SEMANTICS",audit(List.of(a,b)));
    }
    @Test void amountOnlyAndDurationTierOverlapIsAConflict() {
        var a=tier(814L,1);
        var b=tier(814L,2,"MIN_TERM_VALUE",3,"MAX_TERM_VALUE",6,"TERM_UNIT_CODE","MONTH");
        status("CONFLICT",audit(List.of(a,b)));
    }
    @Test void pricingMethodTierFlagMismatchBlocksPublication() {
        var p=pricing();p.put("PRICING_METHOD_CODE","FIXED");
        status("INVALID_SEMANTICS",audit(p,List.of(component(814L)),Map.of(814L,List.of(tier(814L,1)))));
        p.put("IS_TIERED_RATE",0);p.put("PRICING_METHOD_CODE","TIERED");
        status("INVALID_SEMANTICS",audit(p,List.of(component(814L)),Map.of(814L,List.of(tier(814L,1)))));
    }
    @Test void wrongChildForeignKeyOrFailedLookupFailsClosed() {
        status("INVALID_SEMANTICS",audit(List.of(tier(999L,1))));
        var result=ProductRuleHierarchyAudit.pricingTiers(List.of(pricing()),FROM,TO,
                (table,key,id)->{throw new IllegalStateException("Read-only fetch failed");},VALIDATOR);
        status("CONFLICT",result);
    }
    @Test void rateValueOutsideUnitFractionIsRejected() {
        var t=tier(814L,1);t.put("RATE_VALUE","1.01");
        status("INVALID_SEMANTICS",audit(List.of(t)));
    }
    @Test void previewNeverInventsMissingAmountRate() {
        assertThrows(ProductBuilderValidationException.class,()->
                ProductPricingSingleRatePolicy.previewAmountOnlyRate(List.of(tier(814L,1,
                        "MIN_AMOUNT",100)),new BigDecimal("99.9999"),FROM));
    }
    @Test void previewDeniesMultipleCompetingRates() {
        assertThrows(ProductBuilderValidationException.class,()->
                ProductPricingSingleRatePolicy.previewAmountOnlyRate(List.of(tier(814L,1),tier(814L,2)),
                        new BigDecimal("100"),FROM));
    }
}
