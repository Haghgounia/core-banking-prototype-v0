package com.behsazan.corebanking.fee3.r4b1;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.r3.Fee3PreviewEngine;
import com.behsazan.corebanking.fee3.r4.Fee3R4CalculationContract;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** R4-B1: safe, draft-only BRACKET/PROGRESSIVE service fee preview. No financial side effects. */
public final class Fee3R4B1ServiceContract {
    private Fee3R4B1ServiceContract() {}
    public static final String CONTRACT = "FEE3_SERVICE_FEE_R4B1_V1";
    private static final BigDecimal MAX = new BigDecimal("1000000000000000000");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int MAX_ROWS = 50;
    private static final Set<String> METHODS = Set.of("BRACKET", "PROGRESSIVE");
    private static final Set<String> CONFIG_KEYS = Set.of("basisCode", "basisExplicit", "tiers", "brackets",
        "rateSource", "fixedAmount", "fixedPart", "ratePercent", "unitPrice", "freeUnits", "dayBasis",
        "formula", "formulaParams", "timeEnabled", "minOutput", "maxOutput", "matrix");
    private static final Set<String> TIER_KEYS = Set.of("upTo", "ratePercent", "fixedAmount", "calcType");
    private static final Set<String> BRACKET_KEYS = Set.of("from", "to", "ratePercent", "fixedAmount",
        "calcType", "inclusiveUpper");

    public record Request(DraftRequest draft, Map<String,Object> inputs, LocalDate effectiveOn) {}
    public record Evidence(String contractVersion, String status, boolean persisted,
                           boolean financialPosting, boolean published, boolean approved,
                           String simulationMode, Fee3R4CalculationContract.VersionReference versionReference,
                           LocalDate oracleBusinessDate, LocalDate effectiveOn, String currency,
                           String roundingCode, int roundingScale, String snapshotSha256,
                           Fee3PreviewEngine.PreviewResult calculation, List<String> diagnostics) {}

    public static String guard(Request request,LocalDate bankDay) {
        if(bankDay==null)return "ORACLE_BUSINESS_DATE_MISSING";
        if(request==null || request.draft()==null || request.draft().item()==null ||
           request.draft().version()==null || request.draft().scope()==null)return "DRAFT_INCOMPLETE";
        DraftRequest draft=request.draft();ItemDraft item=draft.item();VersionDraft version=draft.version();
        if(!"SERVICE_FEE".equals(item.amountKind()))return "SERVICE_FEE_ONLY";
        if(version.method()==null||!METHODS.contains(version.method()))return "SERVICE_METHOD_NOT_IN_R4B1";
        if(!"IRR".equals(version.feeCurrency()) || !"IRR".equals(version.serviceCurrency()) ||
           !"IRR".equals(draft.scope().currency()))return "IRR_ONLY";
        LocalDate effective=request.effectiveOn()==null?bankDay:request.effectiveOn();
        if(effective.isBefore(bankDay))return "HISTORICAL_SIMULATION_NOT_ALLOWED";
        if(version.validFrom()==null || effective.isBefore(version.validFrom()) ||
            version.validTo()!=null&&effective.isAfter(version.validTo()) ||
            version.validTo()!=null&&version.validTo().isBefore(version.validFrom()))
            return "OUTSIDE_DRAFT_VALIDITY";
        if(version.rounding()==null || !Set.of("HALF_UP","HALF_EVEN","FLOOR","DOWN","CEIL","UP").contains(version.rounding()))
            return "ROUNDING_POLICY_UNSUPPORTED";
        if(draft.rules()==null||draft.rules().size()!=1||draft.rules().get(0)==null)
            return "EXACTLY_ONE_RULE_REQUIRED";
        RuleDraft rule=draft.rules().get(0);
        if(rule.order()!=1||!version.method().equals(rule.method()))return "RULE_METHOD_OR_ORDER_MISMATCH";
        if(!"REQUEST_AMOUNT".equals(rule.base()))return "ONLY_EXPLICIT_REQUEST_AMOUNT_BASIS";
        if(!"RULE".equals(rule.rateSource())||rule.rate()!=null||rule.fixed()!=null ||
           rule.collateral()!=null&&!rule.collateral().isBlank() || rule.coverage()!=null)
            return "ADVANCED_TIER_RATE_FIELDS_UNSUPPORTED";
        if(rule.formulaCode()!=null&&!rule.formulaCode().isBlank())return "FORMULA_NOT_CERTIFIED";
        if(!optionalMoney(rule.min())||!optionalMoney(rule.max()))return "CAP_PRECISION_OR_RANGE_INVALID";
        if(rule.min()!=null&&rule.max()!=null&&rule.min().compareTo(rule.max())>0)
            return "MIN_FEE_EXCEEDS_MAX_FEE";
        if(draft.calcItemId()==null ^ draft.tariffVersionId()==null)
            return "PARTIAL_ORACLE_REFERENCE";
        if(draft.calcItemId()==null&&(draft.expectedItemRecordVersion()!=null||
             draft.expectedVersionRecordVersion()!=null))return "UNPERSISTED_RECORD_VERSION_UNEXPECTED";
        if(draft.calcItemId()!=null && (draft.calcItemId()<=0||draft.tariffVersionId()<=0||
             draft.expectedItemRecordVersion()==null||draft.expectedItemRecordVersion()<=0||
             draft.expectedVersionRecordVersion()==null||draft.expectedVersionRecordVersion()<=0))
             return "INVALID_ORACLE_REFERENCE_VERSIONS";
        if(version.uiContext()!=null&&!version.uiContext().isEmpty())return "SPECIALIZED_CONTEXT_UNREVIEWED";
        if(request.inputs()==null||!request.inputs().keySet().equals(Set.of("amount")) ||
           !money(request.inputs().get("amount"),false))return "EXPLICIT_POSITIVE_IRR_AMOUNT_REQUIRED";
        Map<String,Object> policy=version.financialPolicy()==null?Map.of():version.financialPolicy();
        if(!Set.of("taxPercent").containsAll(policy.keySet()))return "FINANCIAL_ADJUSTMENTS_NOT_CERTIFIED";
        if("TAXABLE".equals(version.taxMode())){
            if(!percentage(policy.get("taxPercent"),false,HUNDRED))return "EXPLICIT_TAX_RATE_REQUIRED";
        } else if(version.taxMode()!=null&&Set.of("EXEMPT","NONE","NON_TAXABLE").contains(version.taxMode())){
            if(policy.containsKey("taxPercent")&&policy.get("taxPercent")!=null)
                return "TAX_RATE_WITH_NON_TAXABLE_MODE";
        } else return "TAX_POLICY_UNREVIEWED";
        if(rule.parameters()==null||!rule.parameters().keySet().equals(Set.of("originalConfig")))
            return "REVIEWED_ORIGINAL_CONFIG_REQUIRED";
        Object rawConfig=rule.parameters().get("originalConfig");
        if(!(rawConfig instanceof Map<?,?> config))return "REVIEWED_ORIGINAL_CONFIG_REQUIRED";
        if(!CONFIG_KEYS.containsAll(config.keySet()))return "UNKNOWN_SERVICE_CONFIG_FIELD";
        if(!Boolean.TRUE.equals(config.get("basisExplicit")) ||
           !"REQUEST_AMOUNT".equals(config.get("basisCode")))return "EXPLICIT_MONEY_BASIS_REQUIRED";
        if(config.get("rateSource")!=null&&!"CONFIG".equals(config.get("rateSource")))
            return "DYNAMIC_TIER_RATES_UNSUPPORTED";
        if(present(config.get("fixedAmount"))||present(config.get("fixedPart"))||
           present(config.get("ratePercent"))||present(config.get("unitPrice"))||
           nonEmpty(config.get("formula"))||Boolean.TRUE.equals(config.get("timeEnabled"))||
           ((config.get("matrix")!=null && !(config.get("matrix") instanceof List<?>)) ||
            nonEmptyList(config.get("matrix"))) ||
           ((config.get("formulaParams")!=null && !(config.get("formulaParams") instanceof List<?>)) ||
            nonEmptyList(config.get("formulaParams"))) ||
           presentNonZero(config.get("freeUnits")))return "UNSUPPORTED_SERVICE_CONFIG_MODIFIER";
        if(config.containsKey("minOutput")&&present(config.get("minOutput"))&&
           !equalsNumber(rule.min(),config.get("minOutput")))return "MINIMUM_INCONSISTENT";
        if(config.containsKey("maxOutput")&&present(config.get("maxOutput"))&&
           !equalsNumber(rule.max(),config.get("maxOutput")))return "MAXIMUM_INCONSISTENT";
        if("BRACKET".equals(version.method())){
            if(config.get("tiers")!=null && !(config.get("tiers") instanceof List<?>))return "MIXED_TIER_MODE_FORBIDDEN";
            if(nonEmptyList(config.get("tiers")))return "MIXED_TIER_MODE_FORBIDDEN";
            return bracketStructure(config.get("brackets"));
        }
        if(config.get("brackets")!=null && !(config.get("brackets") instanceof List<?>))return "MIXED_TIER_MODE_FORBIDDEN";
        if(nonEmptyList(config.get("brackets")))return "MIXED_TIER_MODE_FORBIDDEN";
        return tierStructure(config.get("tiers"));
    }

    private static String bracketStructure(Object input){
        if(!(input instanceof List<?> rows)||rows.isEmpty()||rows.size()>MAX_ROWS)
            return "BRACKET_ROWS_MISSING_OR_TOO_MANY";
        BigDecimal prior=BigDecimal.ZERO;boolean open=false;
        for(int n=0;n<rows.size();n++){
            Object row=rows.get(n);
            if(!(row instanceof Map<?,?> r)||!BRACKET_KEYS.containsAll(r.keySet()))
                return "UNREVIEWED_BRACKET_ROW";
            if(!"LEGACY".equals(r.get("calcType"))&&r.get("calcType")!=null ||
               r.get("inclusiveUpper")!=null&&!Boolean.FALSE.equals(r.get("inclusiveUpper")))
                return "BRACKET_MODE_NOT_CERTIFIED";
            BigDecimal from=amount(r.get("from"));
            if(from==null||from.compareTo(prior)!=0)return "BRACKET_GAP_OR_OVERLAP";
            if(!rowPricing(r))return "BRACKET_RATE_OR_FIXED_INVALID";
            BigDecimal end=amount(r.get("to"));
            if(present(r.get("to")) && (end==null||end.compareTo(from)<=0))
                return "BRACKET_UPPER_BOUND_INVALID";
            if(!present(r.get("to"))){
                if(n!=rows.size()-1)return "OPEN_BRACKET_MUST_BE_LAST";
                open=true;
            } else prior=end;
        }
        return open?null:"BRACKET_LAST_RANGE_MUST_BE_OPEN";
    }
    private static String tierStructure(Object input){
        if(!(input instanceof List<?> rows)||rows.isEmpty()||rows.size()>MAX_ROWS)
            return "PROGRESSIVE_ROWS_MISSING_OR_TOO_MANY";
        BigDecimal prior=BigDecimal.ZERO;boolean open=false;
        for(int n=0;n<rows.size();n++){
            Object row=rows.get(n);
            if(!(row instanceof Map<?,?> r)||!TIER_KEYS.containsAll(r.keySet()))
                return "UNREVIEWED_PROGRESSIVE_ROW";
            if(!"LEGACY".equals(r.get("calcType"))&&r.get("calcType")!=null)
                return "TIER_MODE_NOT_CERTIFIED";
            if(!rowPricing(r))return "TIER_RATE_OR_FIXED_INVALID";
            BigDecimal upper=amount(r.get("upTo"));
            if(present(r.get("upTo")) && (upper==null||upper.compareTo(prior)<=0))
                return "TIER_UPPER_BOUND_INVALID";
            if(!present(r.get("upTo"))){
                if(n!=rows.size()-1)return "OPEN_TIER_MUST_BE_LAST";
                open=true;
            } else prior=upper;
        }
        return open?null:"PROGRESSIVE_LAST_TIER_MUST_BE_OPEN";
    }
    private static boolean rowPricing(Map<?,?> row){
        return percentage(row.get("ratePercent"),false,new BigDecimal("1000"))&&
               money(row.get("fixedAmount"),true);
    }
    private static boolean present(Object o){return o!=null&&!(o instanceof String s&&s.isBlank());}
    private static boolean nonEmpty(Object o){return o!=null && !String.valueOf(o).isBlank();}
    private static boolean nonEmptyList(Object o){return o instanceof List<?> l&&!l.isEmpty();}
    private static boolean presentNonZero(Object o){
        if(!present(o))return false;
        BigDecimal n=number(o);return n==null||n.signum()!=0;
    }
    private static BigDecimal number(Object o){
        if(!(o instanceof Number||o instanceof String))return null;
        try{return new BigDecimal(String.valueOf(o));}catch(NumberFormatException e){return null;}
    }
    private static BigDecimal amount(Object o){return money(o,true)?number(o):null;}
    private static boolean money(Object o,boolean allowZero){
        BigDecimal n=number(o);
        return n!=null&&(allowZero?n.signum()>=0:n.signum()>0)&&n.compareTo(MAX)<=0&&
               Math.max(0,n.stripTrailingZeros().scale())<=2;
    }
    private static boolean optionalMoney(BigDecimal o){return o==null||money(o,true);}
    private static boolean percentage(Object o,boolean allowNull,BigDecimal maximum){
        if(o==null)return allowNull;
        BigDecimal n=number(o);
        return n!=null&&n.signum()>=0&&n.compareTo(maximum)<=0&&
               Math.max(0,n.stripTrailingZeros().scale())<=8;
    }
    private static boolean equalsNumber(BigDecimal expected,Object other){
        BigDecimal n=number(other);return expected!=null&&n!=null&&expected.compareTo(n)==0;
    }

    public static Fee3PreviewEngine.PreviewResult calculate(Request req){
        // Caller MUST first run guard(), R3 preflight and the Oracle reference proof.
        RuleDraft rule=req.draft().rules().get(0);
        String method=req.draft().version().method();
        Map<?,?> config=(Map<?,?>)rule.parameters().get("originalConfig");
        BigDecimal amount=number(req.inputs().get("amount"));
        BigDecimal raw=BigDecimal.ZERO;
        List<Map<String,Object>> trace=new ArrayList<>();
        if("BRACKET".equals(method)){
            List<?> rows=(List<?>)config.get("brackets");
            for(int i=0;i<rows.size();i++){
                Map<?,?> row=(Map<?,?>)rows.get(i);
                BigDecimal from=number(row.get("from"));
                BigDecimal to=present(row.get("to"))?number(row.get("to")):null;
                if(amount.compareTo(from)>=0&&(to==null||amount.compareTo(to)<0)){
                    raw=segment(amount,row);
                    trace.add(trace(i+1,from,to,amount,row,raw));
                    break;
                }
            }
        }else{
            List<?> rows=(List<?>)config.get("tiers");
            BigDecimal bottom=BigDecimal.ZERO;
            for(int i=0;i<rows.size();i++){
                Map<?,?> row=(Map<?,?>)rows.get(i);
                BigDecimal upper=present(row.get("upTo"))?number(row.get("upTo")):null;
                BigDecimal part=amount.min(upper==null?amount:upper).subtract(bottom);
                if(part.signum()>0){
                    BigDecimal sub=segment(part,row);raw=raw.add(sub);
                    trace.add(trace(i+1,bottom,upper,part,row,sub));
                }
                if(upper==null||amount.compareTo(upper)<=0)break;
                bottom=upper;
            }
        }
        if(raw.signum()<0||raw.compareTo(MAX)>0)throw bad("FEE_EXCEEDS_SAFE_LIMIT");
        BigDecimal capped=raw;
        if(rule.min()!=null)capped=capped.max(rule.min());
        if(rule.max()!=null)capped=capped.min(rule.max());
        RoundingMode mode=rounding(req.draft().version().rounding());
        BigDecimal fee=capped.setScale(0,mode);
        BigDecimal tax=BigDecimal.ZERO;
        if("TAXABLE".equals(req.draft().version().taxMode())){
            BigDecimal rate=number(req.draft().version().financialPolicy().get("taxPercent"));
            tax=fee.multiply(rate).divide(HUNDRED,12,RoundingMode.HALF_EVEN).setScale(0,mode);
        }
        BigDecimal total=fee.add(tax);
        if(total.compareTo(MAX)>0)throw bad("FEE_AND_TAX_TOTAL_EXCEEDS_SAFE_LIMIT");
        return new Fee3PreviewEngine.PreviewResult("SIMULATED",false,false,method,raw,fee,tax,total,
            List.copyOf(trace),List.of("UAT_SIMULATION_ONLY","SINGLE_REVIEWED_SERVICE_RULE",
                "NO_ORACLE_MUTATION","NO_ACCOUNTING_POSTING"));
    }
    private static BigDecimal segment(BigDecimal basis,Map<?,?> r){
        BigDecimal rate=number(r.get("ratePercent"));
        BigDecimal fixed=number(r.get("fixedAmount"));
        return basis.multiply(rate).divide(HUNDRED,12,RoundingMode.HALF_EVEN).add(fixed);
    }
    private static Map<String,Object> trace(int n,BigDecimal from,BigDecimal to,BigDecimal part,
                                             Map<?,?> row,BigDecimal component){
        Map<String,Object> t=new LinkedHashMap<>();
        t.put("ordinal",n);t.put("lowerInclusiveIrr",from);t.put("upperExclusiveIrr",to);
        t.put("chargedBasisIrr",part);t.put("ratePercent",number(row.get("ratePercent")));
        t.put("fixedComponentIrr",number(row.get("fixedAmount")));
        t.put("rawFeeIrr",component);return t;
    }
    private static RoundingMode rounding(String s){return switch(s){
        case "HALF_UP"->RoundingMode.HALF_UP;case "HALF_EVEN"->RoundingMode.HALF_EVEN;
        case "FLOOR","DOWN"->RoundingMode.FLOOR;case "CEIL","UP"->RoundingMode.CEILING;
        default->throw bad("ROUNDING_NOT_ALLOWED");};}
    private static IllegalArgumentException bad(String reason){return new IllegalArgumentException(reason);}

    public static Evidence blocked(Request r,LocalDate bankDay,String reason){
        VersionDraft v=r==null||r.draft()==null?null:r.draft().version();
        LocalDate effective=r==null||r.effectiveOn()==null?bankDay:r.effectiveOn();
        return new Evidence(CONTRACT,"BLOCKED",false,false,false,false,"UAT_DRAFT_SIMULATION",null,
            bankDay,effective,"IRR",v==null?null:v.rounding(),0,null,null,List.of(reason));
    }
    public static Evidence finish(Request r,LocalDate bankDay,
            Fee3R4CalculationContract.VersionReference ref,Fee3PreviewEngine.PreviewResult calc){
        if(ref==null||ref.active()||!("UNSAVED".equals(ref.status())||"DRAFT".equals(ref.status()))||
           calc==null||!"SIMULATED".equals(calc.status())||calc.financialPosting()||calc.persisted()||
           calc.feeIrr()==null||calc.rawIrr()==null||calc.taxIrr()==null||calc.totalIrr()==null)
            return blocked(r,bankDay,"UNSAFE_CALCULATION_OR_VERSION_STATE");
        LocalDate day=r.effectiveOn()==null?bankDay:r.effectiveOn();
        String sha;
        try{
            String baseline=Fee3R4CalculationContract.fingerprint(
                new Fee3R4CalculationContract.Request(r.draft(),r.inputs(),r.effectiveOn()),ref,day);
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(
                (CONTRACT+"|"+baseline+"|"+calc.rawIrr().toPlainString()+"|"+
                 calc.totalIrr().toPlainString()).getBytes(StandardCharsets.UTF_8));
            sha=java.util.HexFormat.of().formatHex(bytes);
        }catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA256_UNAVAILABLE",e);}
        return new Evidence(CONTRACT,"SIMULATED",false,false,false,false,
            day.isAfter(bankDay)?"FUTURE_DATED_UAT_SIMULATION":"UAT_DRAFT_SIMULATION",
            ref,bankDay,day,"IRR",r.draft().version().rounding(),0,sha,calc,
            List.of("UNPUBLISHED_DRAFT_ONLY","NOT_A_CHARGE_AUTHORIZATION","NO_ORACLE_WRITE",
                "NO_ACCOUNTING_POSTING","NOT_A_TAX_DETERMINATION"));
    }
}
