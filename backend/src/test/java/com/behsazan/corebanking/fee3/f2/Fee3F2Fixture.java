package com.behsazan.corebanking.fee3.f2;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Synthetic UAT-only fixtures; NOT a sourced/verified current bank rate. */
final class Fee3F2Fixture {
    private Fee3F2Fixture(){}
    static final LocalDate DAY=LocalDate.of(2026,10,28);
    record Case(String code,int version,String operation,String formula,String expr,
                String type,String officialRow,String threshold) {}
    static Case p(String code,int version){
        return switch(code+"#"+version){
            case "FCE-000051#1" -> new Case(code,version,"CHEQUEBOOK_ISSUE","FCE_EXPR_3721E6C16512A004",
                "75000 + printingCost + stampDuty","PAPER",null,null);
            case "FCE-000157#1" -> new Case(code,version,"CARD_TO_CARD","FCE_EXPR_B963F129BCA3C7F3",
                "9000 + max(0,ceil((amount-10000000)/10000000))*3200","CARD",null,null);
            case "FCE-000157#2" -> new Case(code,version,"CARD_TO_CARD","FCE_EXPR_7025358841EFCDF6",
                "11000 + max(0,ceil((amount-10000000)/10000000))*3500","CARD",null,null);
            case "FCE-000160#1" -> new Case(code,version,"ACH_TRANSFER","FCE_EXPR_67AA0A60D5EEF44D",
                "15000 + usage * 1500","USAGE",null,null);
            case "FCE-000164#1" -> new Case(code,version,"PAYROLL_BATCH","FCE_EXPR_39E716F39BF0EAAE",
                "4500 + usage * 450","USAGE",null,null);
            case "FCE-000291#1" -> new Case(code,version,"COLLATERAL_VALUATION_REAL_ESTATE",
                "FCE_EXPR_101E3E73C3939534",
                "2587500 + max(0, amount - 20000000000) * 0.002","VALUATION","7-2","20000000000");
            case "FCE-000292#1" -> valuation(code,"COLLATERAL_VALUATION_MACHINERY","7-1");
            case "FCE-000293#1" -> valuation(code,"COLLATERAL_VALUATION_FARMLAND","7-3");
            case "FCE-000294#1" -> valuation(code,"COLLATERAL_VALUATION_SURPLUS","7-7");
            case "FCE-000296#1" -> new Case(code,version,"CHEQUEBOOK_ISSUE_PAPER",
                "FCE_EXPR_3721E6C16512A004","75000 + printingCost + stampDuty","PAPER",null,null);
            case "FCE-000297#1" -> new Case(code,version,"CHEQUEBOOK_ISSUE_ELECTRONIC",
                "FCE_EXPR_9E1A70F17DCB90E8","1.2 * approvedOperatingCost + stampDuty","ELECTRONIC",null,null);
            default -> throw new IllegalArgumentException("TEST_CASE_NOT_ALLOWED");
        };
    }
    private static Case valuation(String code,String operation,String row){
        return new Case(code,1,operation,"FCE_EXPR_228383EB0C8B0B93",
            "2587500 + max(0, amount - 2000000000) * 0.002","VALUATION",row,"2000000000");
    }
    static Fee3F2FormulaContract.Request create(String code,int version,Map<String,Object> inputs){
        Case p=p(code,version);
        Map<String,Object> cfg=new LinkedHashMap<>();
        cfg.put("basisCode","REQUEST_AMOUNT");cfg.put("rateSource","CONFIG");
        cfg.put("dayBasis","ACT_ACT");cfg.put("freeUnits",0);cfg.put("formula",p.expr());
        cfg.put("brackets",List.of());cfg.put("matrix",List.of());cfg.put("tiers",List.of());
        List<Map<String,Object>> specs=new ArrayList<>();
        if("PAPER".equals(p.type())){
            specs.add(spec("printingCost"));specs.add(spec("stampDuty"));
        }else if("ELECTRONIC".equals(p.type())){
            specs.add(spec("approvedOperatingCost"));specs.add(spec("stampDuty"));
        }else if("VALUATION".equals(p.type())){
            specs.add(spec("officialExpertFeeForExcess"));
            cfg.put("valuationRule1404",Map.of("baseFee",2587500,"marginalRate",0.002,
                "maximumAutoValue",400000000000L,"officialRow",p.officialRow(),
                "requireExpertFeeForExcess",true,"threshold",new BigDecimal(p.threshold())));
        }
        cfg.put("formulaParams",specs);
        Map<String,Object> params=new LinkedHashMap<>();params.put("originalConfig",cfg);
        params.put("sourceRecordCode",code);params.put("sourceVersion",version);
        params.put("requiresCurrentApproval",true);params.put("rateFromContract",false);
        RuleDraft rule=new RuleDraft(1,"FORMULA","REQUEST_AMOUNT","RULE",null,null,"",null,
            null,null,p.formula(),params);
        DraftRequest draft=new DraftRequest(null,null,null,null,
            new ItemDraft("F2 Engineering Only","PAYMENT",p.operation(),"SERVICE_FEE",null,"PAYMENT",null),
            new VersionDraft(DAY,null,"BANK_INTERNAL",null,"FORMULA","EXEMPT","HALF_UP",
                "F2 offline engineering fixture",Map.of(),"IRR","IRR",Map.of()),
            new ScopeDraft(List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of(),"IRR"),
            List.of(rule),List.of(new TestDraft("F2 deterministic",inputs,null)));
        return new Fee3F2FormulaContract.Request(draft,inputs,null);
    }
    private static Map<String,Object> spec(String name){return Map.of("name",name,"type","MONEY","label","engineering");}
    static Fee3F2FormulaContract.Request inputs(Fee3F2FormulaContract.Request r,Map<String,Object> values){
        return new Fee3F2FormulaContract.Request(r.draft(),values,r.effectiveOn());
    }
    static Fee3F2FormulaContract.Request change(Fee3F2FormulaContract.Request r,Map<String,Object> policy,
        String tax,String rounding,LocalDate from,LocalDate to){
        DraftRequest d=r.draft();VersionDraft v=d.version();
        VersionDraft nv=new VersionDraft(from,to,v.origin(),v.sourceId(),v.method(),tax,rounding,
            v.changeReason(),policy,v.serviceCurrency(),v.feeCurrency(),v.uiContext());
        return new Fee3F2FormulaContract.Request(new DraftRequest(d.calcItemId(),d.tariffVersionId(),
            d.expectedItemRecordVersion(),d.expectedVersionRecordVersion(),d.item(),nv,d.scope(),
            d.rules(),d.tests()),r.inputs(),r.effectiveOn());
    }
    static Map<String,Object> config(Fee3F2FormulaContract.Request req){
        @SuppressWarnings("unchecked") var v=(Map<String,Object>)req.draft().rules().get(0).parameters().get("originalConfig");
        return v;
    }
}
