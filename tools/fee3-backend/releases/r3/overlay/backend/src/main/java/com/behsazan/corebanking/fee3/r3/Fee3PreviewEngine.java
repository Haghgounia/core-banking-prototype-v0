package com.behsazan.corebanking.fee3.r3;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministic, side-effect-free and deliberately bounded UAT arithmetic. Not bank posting. */
public final class Fee3PreviewEngine {
    private Fee3PreviewEngine() {}
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal MAX = new BigDecimal("1000000000000000000");
    private static final Set<String> COLLATERALS = Set.of("CASH_100","TERM_DEPOSIT","GOLD",
        "DOMESTIC_BANK_GUARANTEE","EXPORT_GUARANTEE_FUND","FOREIGN_BANK_GUARANTEE",
        "LISTED_SHARES","LIQUID_REAL_ESTATE","OTHER_REAL_ESTATE","VESSEL","CHEQUE",
        "PROMISSORY_NOTE","OTHER_COLLATERAL","MOVABLE_PROPERTY");
    public record PreviewRequest(DraftRequest draft, Map<String,Object> inputs) {}
    public record PreviewResult(String status, boolean financialPosting, boolean persisted,
                                String method, BigDecimal rawIrr, BigDecimal feeIrr,
                                BigDecimal taxIrr, BigDecimal totalIrr,
                                List<Map<String,Object>> breakdown, List<String> diagnostics) {}

    public static PreviewResult calculate(PreviewRequest request) {
        String method=request==null||request.draft()==null||request.draft().version()==null?
            "UNKNOWN":request.draft().version().method();
        try {return calculateChecked(request,method);}
        catch (IllegalArgumentException | ArithmeticException ex) {
            return new PreviewResult("BLOCKED",false,false,method,null,null,null,null,
                List.of(),List.of(ex.getMessage()==null?"UNSUPPORTED_CONTRACT":ex.getMessage()));
        }
    }
    private static PreviewResult calculateChecked(PreviewRequest request,String method) {
        if(request==null||request.draft()==null||request.draft().item()==null||
           request.draft().version()==null)throw bad("MISSING_DRAFT");
        DraftRequest draft=request.draft(); VersionDraft v=draft.version(); ItemDraft item=draft.item();
        Map<String,Object> inputs=request.inputs()==null?Map.of():request.inputs();
        if(!"IRR".equals(v.feeCurrency())||!"IRR".equals(v.serviceCurrency()))throw bad("CURRENCY_CONVERSION_UNSUPPORTED");
        if(method==null||!Set.of("FIXED","PERCENT","MIXED").contains(method))throw bad("METHOD_NOT_CERTIFIED_FOR_SERVER_PREVIEW");
        if(draft.rules()==null||draft.rules().size()!=1)throw bad("EXACTLY_ONE_RULE_REQUIRED");
        RuleDraft rule=draft.rules().get(0);
        if(rule==null)throw bad("NULL_RULE");
        if(!method.equals(rule.method()))throw bad("RULE_METHOD_MISMATCH");
        if(rule.formulaCode()!=null&&!rule.formulaCode().isBlank() ||
           rule.parameters()!=null&&rule.parameters().containsKey("_fceEditableExpressionV1"))
           throw bad("FORMULA_EXECUTION_FORBIDDEN");
        Map<String,Object> policy=v.financialPolicy()==null?Map.of():v.financialPolicy();
        for(String key:List.of("partsEnabled","adjustmentsEnabled","dealsEnabled","allocationEnabled"))
            if(Boolean.TRUE.equals(policy.get(key)))throw bad("COMPLEX_FINANCIAL_POLICY_UNSUPPORTED");
        RoundingMode rounding=rounding(v.rounding());
        BigDecimal raw;
        List<Map<String,Object>> trace=new ArrayList<>();
        Map<String,Object> context=v.uiContext()==null?Map.of():v.uiContext();
        if(context.get("guaranteePolicy") instanceof Map<?,?> g) {
            if(!"GUARANTEE".equals(item.module())||!"GUARANTEE_ISSUE".equals(item.operation())||
               !"GUARANTEE_FEE".equals(item.amountKind()))throw bad("GUARANTEE_CONTEXT_MISMATCH");
            raw=guarantee(rule,g,method,inputs,trace);
        } else {
            if(Set.of("GUARANTEE_FEE","QARD_FEE","FINANCING_PROFIT","LATE_DAMAGE").contains(item.amountKind()))
                throw bad("SPECIALIZED_BANKING_MODEL_REQUIRES_CERTIFIED_ENGINE");
            raw=simple(rule,method,inputs);
        }
        if(raw.compareTo(BigDecimal.ZERO)<0||raw.compareTo(MAX)>0)throw bad("FEE_OUT_OF_RANGE");
        BigDecimal capped=raw;
        if(rule.min()!=null)capped=capped.max(nonnegative(rule.min(),"MIN_FEE"));
        if(rule.max()!=null)capped=capped.min(nonnegative(rule.max(),"MAX_FEE"));
        if(rule.min()!=null&&rule.max()!=null&&rule.min().compareTo(rule.max())>0)throw bad("MIN_GREATER_THAN_MAX");
        BigDecimal fee=capped.setScale(0,rounding);
        BigDecimal tax=BigDecimal.ZERO;
        if("TAXABLE".equals(v.taxMode())){
            BigDecimal percent=bounded(policy.get("taxPercent"),"TAX_PERCENT",HUNDRED,true);
            tax=fee.multiply(percent).divide(HUNDRED,12,RoundingMode.HALF_EVEN).setScale(0,rounding);
        } else if(v.taxMode()==null||!Set.of("EXEMPT","NONE","NON_TAXABLE").contains(v.taxMode())){
            throw bad("TAX_POLICY_NOT_REVIEWED");
        }
        if(fee.add(tax).compareTo(MAX)>0)throw bad("TOTAL_OUT_OF_RANGE");
        return new PreviewResult("SIMULATED",false,false,method,raw,fee,tax,fee.add(tax),
            List.copyOf(trace),List.of("UAT_SIMULATION_ONLY","NO_ORACLE_MUTATION","NO_ACCOUNTING_POSTING"));
    }
    private static BigDecimal simple(RuleDraft rule,String method,Map<String,Object> inputs) {
        if("FIXED".equals(method)){
            if(rule.fixed()==null)throw bad("FIXED_AMOUNT_REQUIRED");
            return nonnegative(rule.fixed(),"FIXED_AMOUNT");
        }
        BigDecimal amount=positive(inputs.get("amount"),"AMOUNT",MAX);
        BigDecimal rate=rate(rule,inputs);
        BigDecimal variable=amount.multiply(rate).divide(HUNDRED,12,RoundingMode.HALF_EVEN);
        if("PERCENT".equals(method))return variable;
        if(rule.fixed()==null)throw bad("MIXED_FIXED_AMOUNT_REQUIRED");
        return variable.add(nonnegative(rule.fixed(),"FIXED_AMOUNT"));
    }
    private static BigDecimal guarantee(RuleDraft rule,Map<?,?> guarantee,String method,
                                        Map<String,Object> inputs,List<Map<String,Object>> trace) {
        String type=code(guarantee.get("collateralType"));
        String basis=code(guarantee.get("feeBasis"));
        BigDecimal amount=positive(inputs.get("amount"),"GUARANTEE_AMOUNT",MAX);
        if("MIXED_PORTFOLIO".equals(type)){
            if(!"PERCENT".equals(method))throw bad("PORTFOLIO_PERCENT_ONLY_IN_R3");
            if(!Set.of("GUARANTEE_PORTFOLIO_ALLOCATED","GUARANTEE_PORTFOLIO_COLLATERAL_VALUE").contains(basis))
                throw bad("INVALID_PORTFOLIO_BASIS");
            Object rawRules=guarantee.get("portfolioRules"),rawRows=inputs.get("portfolio");
            if(!(rawRules instanceof List<?> rules)||!(rawRows instanceof List<?> rows) ||
               rules.isEmpty()||rules.size()>50||rows.isEmpty()||rows.size()>50)
                throw bad("PORTFOLIO_RULES_AND_SHARES_REQUIRED");
            Map<String,Map<?,?>> tariffs=new HashMap<>();
            for(Object obj:rules){
                if(!(obj instanceof Map<?,?> r))throw bad("INVALID_PORTFOLIO_RULE");
                String c=code(r.get("collateralType"));
                if(!COLLATERALS.contains(c)||tariffs.putIfAbsent(c,r)!=null)throw bad("DUPLICATE_OR_UNKNOWN_COLLATERAL");
                BigDecimal cov=positive(r.get("coveragePercent"),"COVERAGE_PERCENT",new BigDecimal("1000"));
                if("CASH_100".equals(c)&&cov.compareTo(HUNDRED)<0)throw bad("CASH_COVERAGE_BELOW_100");
                String source=code(r.get("rateSource"));
                if(!Set.of("CONFIG","REQUEST","EXEMPT").contains(source))throw bad("INVALID_RATE_SOURCE");
                if("CONFIG".equals(source))bounded(r.get("ratePercent"),"CONFIG_RATE",new BigDecimal("1000"),true);
                else if(r.get("ratePercent")!=null)throw bad("RATE_NOT_OWNED_BY_CONFIG");
            }
            Set<String> selected=new HashSet<>();BigDecimal shares=BigDecimal.ZERO;BigDecimal total=BigDecimal.ZERO;
            for(Object obj:rows){
                if(!(obj instanceof Map<?,?> row))throw bad("INVALID_COLLATERAL_SHARE");
                String c=code(row.get("collateralType"));
                Map<?,?> tariff=tariffs.get(c);
                if(tariff==null||!selected.add(c))throw bad("UNKNOWN_OR_DUPLICATE_COLLATERAL_SHARE");
                BigDecimal share=positive(row.get("sharePercent"),"SHARE_PERCENT",HUNDRED);
                shares=shares.add(share);
                BigDecimal cov=positive(tariff.get("coveragePercent"),"COVERAGE_PERCENT",new BigDecimal("1000"));
                String source=code(tariff.get("rateSource"));
                BigDecimal r="EXEMPT".equals(source)?BigDecimal.ZERO:
                    "REQUEST".equals(source)?bounded(inputs.get("contractRate"),"CONTRACT_RATE",new BigDecimal("1000"),true):
                    bounded(tariff.get("ratePercent"),"CONFIG_RATE",new BigDecimal("1000"),true);
                BigDecimal allocated=amount.multiply(share).divide(HUNDRED,12,RoundingMode.HALF_EVEN);
                BigDecimal covered=allocated.multiply(cov).divide(HUNDRED,12,RoundingMode.HALF_EVEN);
                BigDecimal base="GUARANTEE_PORTFOLIO_COLLATERAL_VALUE".equals(basis)?covered:allocated;
                BigDecimal fee=base.multiply(r).divide(HUNDRED,12,RoundingMode.HALF_EVEN);
                total=total.add(fee);
                Map<String,Object> entry=new LinkedHashMap<>();
                entry.put("collateralType",c);entry.put("sharePercent",share);entry.put("coveragePercent",cov);
                entry.put("allocatedIrr",allocated);entry.put("coveredIrr",covered);entry.put("feeBaseIrr",base);
                entry.put("ratePercent",r);entry.put("source",source);entry.put("rawFeeIrr",fee);
                trace.add(entry);
            }
            if(shares.compareTo(HUNDRED)!=0)throw bad("PORTFOLIO_SHARES_MUST_EQUAL_100");
            return total;
        }
        if(!COLLATERALS.contains(type))throw bad("INVALID_SINGLE_COLLATERAL");
        if(!Set.of("GUARANTEE_AMOUNT","GUARANTEE_COLLATERAL_COVERED_VALUE").contains(basis))
            throw bad("INVALID_GUARANTEE_BASIS");
        if("CASH_100".equals(type))positive(inputs.get("cashCoveragePercent"),"CASH_COVERAGE",new BigDecimal("1000"));
        BigDecimal base=amount;
        if("GUARANTEE_COLLATERAL_COVERED_VALUE".equals(basis)){
            BigDecimal cov=positive(inputs.get("CASH_100".equals(type)?"cashCoveragePercent":"collateralCoveragePercent"),
                "COLLATERAL_COVERAGE",new BigDecimal("1000"));
            if("CASH_100".equals(type)&&cov.compareTo(HUNDRED)<0)throw bad("CASH_COVERAGE_BELOW_100");
            base=amount.multiply(cov).divide(HUNDRED,12,RoundingMode.HALF_EVEN);
        }
        if("FIXED".equals(method)){
            if(!"GUARANTEE_AMOUNT".equals(basis))throw bad("FIXED_WITH_COVERAGE_FORBIDDEN");
            if(rule.fixed()==null)throw bad("FIXED_AMOUNT_REQUIRED");
            return nonnegative(rule.fixed(),"FIXED_AMOUNT");
        }
        if(!"PERCENT".equals(method))throw bad("GUARANTEE_METHOD_NOT_SUPPORTED");
        BigDecimal rate=rate(rule,inputs);
        return base.multiply(rate).divide(HUNDRED,12,RoundingMode.HALF_EVEN);
    }
    private static String code(Object value){return value instanceof String s?s:"";}
    private static BigDecimal rate(RuleDraft rule,Map<String,Object> inputs) {
        if("RULE".equals(rule.rateSource()))return bounded(rule.rate(),"RULE_RATE",new BigDecimal("1000"),true);
        if("REQUEST".equals(rule.rateSource())){
            if(rule.rate()!=null)throw bad("REQUEST_RATE_MUST_NOT_BE_PERSISTED");
            return bounded(inputs.get("contractRate"),"CONTRACT_RATE",new BigDecimal("1000"),true);
        }
        throw bad("UNKNOWN_RATE_SOURCE");
    }
    private static BigDecimal positive(Object value,String name,BigDecimal maximum){
        BigDecimal n=bounded(value,name,maximum,false);
        if(n.compareTo(BigDecimal.ZERO)==0)throw bad(name+"_MUST_BE_POSITIVE");
        return n;
    }
    private static BigDecimal nonnegative(BigDecimal value,String name){
        if(value==null||value.compareTo(BigDecimal.ZERO)<0||value.compareTo(MAX)>0)throw bad(name+"_OUT_OF_RANGE");
        return value;
    }
    private static BigDecimal bounded(Object value,String name,BigDecimal maximum,boolean allowZero){
        if(value==null)throw bad(name+"_REQUIRED");
        BigDecimal n;
        try {
            if(value instanceof BigDecimal b)n=b;
            else if(value instanceof Number || value instanceof String)n=new BigDecimal(String.valueOf(value));
            else throw bad(name+"_INVALID_TYPE");
        }catch(NumberFormatException ex){throw bad(name+"_INVALID_NUMBER");}
        if(n.compareTo(BigDecimal.ZERO)<0||(!allowZero&&n.signum()==0)||n.compareTo(maximum)>0||n.scale()>12)
            throw bad(name+"_OUT_OF_RANGE");
        return n;
    }
    private static RoundingMode rounding(String mode){
        if(mode==null)throw bad("ROUNDING_REQUIRED");
        return switch(mode){
            case "HALF_UP" -> RoundingMode.HALF_UP;
            case "HALF_EVEN" -> RoundingMode.HALF_EVEN;
            case "DOWN", "FLOOR" -> RoundingMode.FLOOR;
            case "UP", "CEIL" -> RoundingMode.CEILING;
            default -> throw bad("ROUNDING_POLICY_UNSUPPORTED");
        };
    }
    private static IllegalArgumentException bad(String code){return new IllegalArgumentException(code);}
}
