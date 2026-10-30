package com.behsazan.corebanking.fee3.r3;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** FCE V13.8.84: numerical trace is UAT-only, never bank authorization. */
class Fee3F7DocumentedDomainPreviewTest {
    private static final LocalDate DAY = LocalDate.of(2025,11,2);
    static DraftRequest draft(String domain,String op,String kind,String method,String expression,
                              String fixed,Map<String,Object> params,LocalDate from) {
        var item=new ItemDraft("QA",domain,op,kind,"","","QA");
        var version=new VersionDraft(from,null,"CBI_MANDATORY",null,method,"UNREVIEWED",
            "HALF_UP","QA",Map.of(),"IRR","IRR",Map.of());
        var rule=new RuleDraft(1,method,"REQUEST_AMOUNT","RULE",null,
            fixed==null?null:new BigDecimal(fixed),"",null,null,null,null,params);
        return new DraftRequest(null,null,null,null,item,version,null,List.of(rule),List.of());
    }
    static Map<String,Object> evidence() {
        return new HashMap<>(Map.of("regulatoryReference","REF1404","sourceEvidenceReference","SRC1404"));
    }
    static Fee3F7DomainSimulationService.Result calc(DraftRequest draft,Map<String,Object> inputs,LocalDate date) {
        return Fee3F7DocumentedDomainPreview.simulate(new Fee3F7DomainSimulationService.Request(draft,inputs,date));
    }
    static void verifyPretax(Fee3F7DomainSimulationService.Result r,String value) {
        assertEquals("PRETAX_ONLY",r.status());assertEquals(value,r.amountIrr());
        assertNull(r.taxIrr());assertNull(r.totalIrr());
        assertFalse(r.persisted());assertFalse(r.financialPosting());
        assertFalse(r.approved());assertFalse(r.published());
        assertFalse(r.breakdown().isEmpty());assertNotNull(r.snapshotSha256());
    }
    @Test void chequeFixedRequiresDocuments() {
        var d=draft("DEPOSIT","CHEQUEBOOK_REQUEST","SERVICE_FEE","FIXED",null,"9000",Map.of(),DAY);
        verifyPretax(calc(d,evidence(),DAY),"9000");
        assertEquals("BLOCKED",calc(d,Map.of(),DAY).status());
    }
    @Test void paperChequeFormulaRequiresStampEvidence() {
        var params=Map.<String,Object>of("_fceEditableExpressionV1","75000 + printingCost + stampDuty",
            "originalConfig",Map.of("formulaParams",List.of(
                Map.of("name","printingCost","type","MONEY","label","Printing"),
                Map.of("name","stampDuty","type","MONEY","label","Stamp"))));
        var d=draft("DEPOSIT","CHEQUEBOOK_ISSUE_PAPER","SERVICE_FEE","FORMULA",null,null,params,DAY);
        var inputs=evidence();inputs.putAll(Map.of("printingCost","25000","stampDuty","4000",
            "invoiceReference","INV111","stampDutyReference","DOC222"));
        verifyPretax(calc(d,inputs,DAY),"104000");
        inputs.remove("stampDutyReference");
        assertEquals("BLOCKED",calc(d,inputs,DAY).status());
    }
    @Test void lcOpenRequiresContractAndEffectiveDate() {
        var d=draft("LC","LC_OPEN","LC_FEE","FIXED",null,"2000",Map.of(),DAY);
        var inputs=evidence();inputs.put("contractReference","LC123");
        verifyPretax(calc(d,inputs,DAY),"2000");
        assertEquals("BLOCKED",calc(d,evidence(),DAY).status());
        assertEquals("BLOCKED",calc(d,inputs,DAY.minusDays(1)).status());
    }
    @Test void valuation1404DoesNotBecomeCurrentTariff() {
        var config=Map.<String,Object>of("baseFee",2587500,"threshold",20000000000L,
            "maximumAutoValue",400000000000L,"marginalRate",0.002,"officialRow","7-2");
        var d=draft("GENERAL","COLLATERAL_VALUATION_REAL_ESTATE","SERVICE_FEE","FORMULA",null,null,
            Map.of("originalConfig",Map.of("valuationRule1404",config)),LocalDate.of(2025,3,21));
        var inputs=evidence();inputs.put("expertReportReference","EXP139");inputs.put("amount","25000000000");
        verifyPretax(calc(d,inputs,DAY),"12587500");
        assertEquals("BLOCKED",calc(d,inputs,LocalDate.of(2026,10,29)).status());
        inputs.put("amount","500000000000");
        assertEquals("BLOCKED",calc(d,inputs,DAY).status());
        inputs.put("officialExpertFeeForExcess","6000000");inputs.put("excessExpertFeeReference","EXC320");
        verifyPretax(calc(d,inputs,DAY),"768587500");
    }
    @Test void unrelatedDomainCannotReuseDocumentaryRoutes() {
        var d=draft("LC","CHEQUEBOOK_REQUEST","LC_FEE","FIXED",null,"9000",Map.of(),DAY);
        assertEquals("BLOCKED",calc(d,evidence(),DAY).status());
    }
}
