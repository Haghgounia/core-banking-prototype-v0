package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TablePage;
import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** HF9 integration: assert the actual report and approval gate, not just pure tier checks. */
class ProductPricingPublicationHF7A3HF9Test {
    private static final java.util.Set<String> OPTIONAL_CHILDREN = java.util.Set.of(
            "PRODUCT_CHANNEL_OPERATION", "PRODUCT_ELIGIBILITY_CRITERION", "LOAN_ELIGIBILITY_EXTENSION",
            "DEPOSIT_PRODUCT_CLOSURE_PRECHECK", "DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",
            "DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE", "CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE");
    private static Map<String,Object> row(String table) {
        return new LinkedHashMap<>(ProductRuleGovernanceFixture.row(table));
    }
    private static class Case {
        final PdlProductBuilderRepository repo=mock(PdlProductBuilderRepository.class);
        final ProductRuleGovernanceService service = new ProductRuleGovernanceService(repo,
                mock(RuleGovernanceLiteService.class),new ProductBuilderBusinessValidator());
        final Map<String,List<Map<String,Object>>> data=new LinkedHashMap<>();
        // Capture the real repository calls made by the report, independent of Mockito verify overloads.
        final List<String> tierFkLookups=new ArrayList<>();
        boolean honorTierForeignKey;
        Case() {
            when(repo.findById("PRODUCT_VERSION",55L)).thenReturn(Optional.of(Map.of("PRODUCT_ID",9L,
                    "VALID_FROM","2026-10-10","VALID_TO","2026-10-30","VERSION_STATUS_CODE","DRAFT")));
            when(repo.findById("PRODUCT",9L)).thenReturn(Optional.of(Map.of("PRODUCT_CLASS_CODE","DEPOSIT",
                    "PRODUCT_FAMILY_CODE","CURRENT_ACCOUNT","PRODUCT_STATUS_CODE","DRAFT", "DEFAULT_CURRENCY_CODE","IRR")));
            when(repo.findById("PRODUCT",99L)).thenReturn(Optional.of(Map.of("PRODUCT_ID",99L)));
            when(repo.search(anyString(),isNull(),anyInt(),anyInt(),anyString(),anyString())).thenAnswer(call -> {
                String table=call.getArgument(0);
                int page=call.getArgument(2), size=call.getArgument(3);
                if ("PRODUCT_RATE_TIER".equals(table)) {
                    tierFkLookups.add(page + "|" + size + "|" + call.getArgument(4) + "|" + call.getArgument(5));
                }
                List<Map<String,Object>> records="PRODUCT_VERSION_MODULE".equals(table) || OPTIONAL_CHILDREN.contains(table)
                        ? List.of(): List.of(row(table));
                if(data.containsKey(table))records=data.get(table);
                // Optional real-repository FK filtering: the adversarial wrong-FK
                // test intentionally uses unfiltered rows to prove fail-closed.
                if(honorTierForeignKey && "PRODUCT_RATE_TIER".equals(table)) {
                    String key=call.getArgument(4), value=call.getArgument(5);
                    records=records.stream().filter(r->value.equals(String.valueOf(r.get(key)))).toList();
                }
                int first=Math.min(page*size,records.size());
                int last=Math.min(first+size,records.size());
                return new TablePage(records.subList(first,last),records.size(),page,size);
            });
            var pricing=row("PRODUCT_PRICING_RULE");pricing.put("PRICING_METHOD_CODE","TIERED");pricing.put("IS_TIERED_RATE",1);
            put("PRODUCT_PRICING_RULE",pricing);
            var comp=row("PRODUCT_PRICING_COMPONENT");comp.put("MIN_AMOUNT",0);
            put("PRODUCT_PRICING_COMPONENT",comp);
            var first=row("PRODUCT_RATE_TIER");first.put("MIN_AMOUNT",0);first.put("MAX_AMOUNT",100);
            var second=row("PRODUCT_RATE_TIER");second.put("TIER_NO",2);second.put("MIN_AMOUNT",100);
            put("PRODUCT_RATE_TIER",first,second);
        }
        void put(String table,Map<String,Object>... records){data.put(table,new ArrayList<>(List.of(records)));}
        List<Map<String,Object>> rows(String table){return data.get(table);}
        ProductRuleGovernanceService.GovernanceReport report(){return service.report(55L);}
        void noWrites() {
            verify(repo,never()).insert(anyString(),anyMap(),anyString());
            verify(repo,never()).update(anyString(),anyLong(),anyMap(),anyString());
        }
        void denied(String state){
            var report=report();
            assertFalse(report.canApprove());assertFalse(report.canOpen());
            assertTrue(report.effectiveRules().stream().anyMatch(r->r.code().equals("PRODUCT_PRICING_RULE")
                    && r.status().equals(state) && r.blocking()));
            noWrites();
        }
    }
    @Test void validHalfOpenTiersAllowDesignApprovalButNotOrigination() {
        var c=new Case();
        assertTrue(c.report().canApprove());
        assertFalse(c.report().canOpen());
        c.noWrites();
    }
    @Test void overlappedAmountsBlockRealReportAndPromotion() {
        var c=new Case();c.rows("PRODUCT_RATE_TIER").get(0).put("MAX_AMOUNT",110);
        c.denied("CONFLICT");
        assertThrows(ProductBuilderValidationException.class,()->c.service.assertPromotionAllowed(55L,
                Map.of("VERSION_STATUS_CODE","DRAFT"),Map.of("VERSION_STATUS_CODE","APPROVED")));
    }
    @Test void uncoveredAmountBlocksRealReport() {
        var c=new Case();c.rows("PRODUCT_RATE_TIER").get(1).put("MIN_AMOUNT",110);
        c.denied("CONFLICT");
    }
    @Test void secondComponentCannotBorrowCoverageFromFirst() {
        var c=new Case();
        var child=row("PRODUCT_PRICING_COMPONENT");child.put("PRICING_COMPONENT_ID",815L);
        c.put("PRODUCT_PRICING_COMPONENT",c.rows("PRODUCT_PRICING_COMPONENT").get(0),child);
        // Mirror the real repository: component 815 must not borrow component 814's tiers.
        c.honorTierForeignKey=true;
        var result=c.report();
        // The pre-Maven HF8 compiler has reduced JUnit and Mockito stubs.
        // Retain strict assertions using signatures supported by both HF8 and real JUnit.
        assertFalse(result.canApprove());
        assertFalse(result.canOpen());
        var pricing=result.effectiveRules().stream()
                .filter(r->"PRODUCT_PRICING_RULE".equals(r.code()))
                .findFirst().orElseThrow();
        if (!"CONFLICT".equals(pricing.status()))
            throw new AssertionError("Expected CONFLICT for uncovered component 815: " + pricing);
        assertEquals("CONFLICT", pricing.status());
        assertTrue(pricing.blocking());
        if (pricing.reason()==null || !pricing.reason().contains("815"))
            throw new AssertionError("Missing component 815 was not independently audited: " + pricing);
        assertTrue(pricing.reason().contains("815"));
        // Stronger than the old verify(...eq(...)) check: assert that both
        // distinct component IDs were actually queried, with the exact FK and page.
        if (!c.tierFkLookups.contains("0|200|PRICING_COMPONENT_ID|814")
                || !c.tierFkLookups.contains("0|200|PRICING_COMPONENT_ID|815"))
            throw new AssertionError("Missing independent 814/815 FK lookups: " + c.tierFkLookups);
        assertTrue(c.tierFkLookups.contains("0|200|PRICING_COMPONENT_ID|815"));
        c.noWrites();
    }
    @Test void incompleteDurationDomainFailsClosedInRealReport() {
        var c=new Case();var tier=c.rows("PRODUCT_RATE_TIER").get(0);
        tier.put("MIN_TERM_VALUE",1);tier.put("MAX_TERM_VALUE",12);tier.put("TERM_UNIT_CODE","MONTH");
        c.denied("INVALID_SEMANTICS");
    }
    @Test void inconsistentPricingMethodCannotBePublished() {
        var c=new Case();c.rows("PRODUCT_PRICING_RULE").get(0).put("PRICING_METHOD_CODE","FIXED");
        c.denied("INVALID_SEMANTICS");
    }
    @Test void invalidTierForeignKeyBlocksRealReport() {
        var c=new Case();c.rows("PRODUCT_RATE_TIER").get(1).put("PRICING_COMPONENT_ID",999L);
        c.denied("INVALID_SEMANTICS");
    }
}
