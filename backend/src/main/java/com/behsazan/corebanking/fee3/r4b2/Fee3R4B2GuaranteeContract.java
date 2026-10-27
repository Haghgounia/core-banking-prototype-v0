package com.behsazan.corebanking.fee3.r4b2;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.r3.Fee3PreviewEngine;
import com.behsazan.corebanking.fee3.r4.Fee3R4CalculationContract;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** R4-B2: exact-decimal, guarantee ISSUE-only, unsaved/DRAFT UAT preview; no posting, publication, or DML. */
public final class Fee3R4B2GuaranteeContract {
    private Fee3R4B2GuaranteeContract() {}
    public static final String CONTRACT = "FEE3_GUARANTEE_R4B2_V1";
    private static final BigDecimal MAX = new BigDecimal("1000000000000000000");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal MAX_PERCENT = new BigDecimal("1000");
    private static final Set<String> TYPES = Set.of("CASH_100","TERM_DEPOSIT","GOLD",
        "DOMESTIC_BANK_GUARANTEE","EXPORT_GUARANTEE_FUND","FOREIGN_BANK_GUARANTEE",
        "LISTED_SHARES","LIQUID_REAL_ESTATE","OTHER_REAL_ESTATE","VESSEL","CHEQUE",
        "PROMISSORY_NOTE","OTHER_COLLATERAL","MOVABLE_PROPERTY");
    private static final String PORTFOLIO="MIXED_PORTFOLIO";
    private static final String ALLOCATED="GUARANTEE_PORTFOLIO_ALLOCATED";
    private static final String COVERED="GUARANTEE_PORTFOLIO_COLLATERAL_VALUE";
    private static final String SINGLE_AMOUNT="GUARANTEE_AMOUNT";
    private static final String SINGLE_COVERED="GUARANTEE_COLLATERAL_COVERED_VALUE";
    private static final Set<String> POLICY_KEYS=Set.of("collateralType","feeBasis","portfolioRules");
    private static final Set<String> TARIFF_ROW_KEYS=Set.of("collateralType","rateSource","ratePercent","coveragePercent");
    private static final Set<String> TEST_ROW_KEYS=Set.of("collateralType","sharePercent");
    private static final Set<String> METHODS=Set.of("PERCENT","DAILY");
    private static final Set<String> SOURCES=Set.of("CONFIG","REQUEST","EXEMPT");
    private static final Set<String> DAYS=Set.of("ACT_ACT","ACT_365","ACT_360");

    public record Request(DraftRequest draft,Map<String,Object> inputs,LocalDate effectiveOn) {}
    public record Evidence(String contractVersion,String status,boolean persisted,boolean financialPosting,
        boolean published,boolean approved,String simulationMode,
        Fee3R4CalculationContract.VersionReference versionReference,
        LocalDate oracleBusinessDate,LocalDate effectiveOn,String currency,String roundingCode,
        int roundingScale,String snapshotSha256,Fee3PreviewEngine.PreviewResult calculation,
        List<String> diagnostics) {}

    private static BigDecimal number(Object input){
        if(!(input instanceof Number || input instanceof String))return null;
        try{return new BigDecimal(String.valueOf(input));}
        catch(NumberFormatException exception){return null;}
    }
    private static int scale(BigDecimal n){return Math.max(0,n.stripTrailingZeros().scale());}
    private static boolean monetary(Object o, boolean positive){
        BigDecimal n=number(o);
        return n!=null&&(positive?n.signum()>0:n.signum()>=0)&&n.compareTo(MAX)<=0&&scale(n)<=2;
    }
    private static boolean percentage(Object o,boolean positive,int precision){
        BigDecimal n=number(o);
        return n!=null&&(positive?n.signum()>0:n.signum()>=0)&&n.compareTo(MAX_PERCENT)<=0&&scale(n)<=precision;
    }
    private static String round(String code){
        return code!=null&&Set.of("HALF_UP","HALF_EVEN","FLOOR","DOWN","CEIL","UP").contains(code)?null:
            "ROUNDING_POLICY_UNSUPPORTED";
    }
    private static RoundingMode rounding(String c){return switch(c){
        case "HALF_UP"->RoundingMode.HALF_UP;case "HALF_EVEN"->RoundingMode.HALF_EVEN;
        case "DOWN","FLOOR"->RoundingMode.FLOOR;case "UP","CEIL"->RoundingMode.CEILING;
        default->throw bad("ROUNDING_POLICY_UNSUPPORTED");};}
    private static IllegalArgumentException bad(String code){return new IllegalArgumentException(code);}
    private static Map<?,?> policy(Request req){
        Object value=req.draft().version().uiContext().get("guaranteePolicy");
        return value instanceof Map<?,?> m?m:null;
    }
    private static String policyGuard(Map<?,?> policy,Map<String,Object> inputs,String method){
        if(policy==null||!POLICY_KEYS.containsAll(policy.keySet()))return "TARIFF_GUARANTEE_POLICY_REQUIRED";
        String type=policy.get("collateralType") instanceof String s?s:"";
        String basis=policy.get("feeBasis") instanceof String s?s:"";
        Object raw=policy.get("portfolioRules");
        if(!(raw instanceof List<?> rules))return "TARIFF_COLLATERAL_RULES_REQUIRED";
        if(PORTFOLIO.equals(type)){
            if(!Set.of(ALLOCATED,COVERED).contains(basis))return "PORTFOLIO_BASIS_UNREVIEWED";
            if(rules.isEmpty()||rules.size()>50)return "PORTFOLIO_TARIFF_RULES_REQUIRED";
            Set<String> unique=new HashSet<>();boolean anyRequest=false;
            for(Object obj:rules){
                if(!(obj instanceof Map<?,?> row)||!TARIFF_ROW_KEYS.containsAll(row.keySet()))
                    return "UNREVIEWED_TARIFF_COLLATERAL_FIELD";
                String t=row.get("collateralType") instanceof String s?s:"";
                if(!TYPES.contains(t)||!unique.add(t))return "TARIFF_COLLATERAL_DUPLICATE_OR_UNKNOWN";
                String src=row.get("rateSource") instanceof String s?s:"";
                if(!SOURCES.contains(src))return "COLLATERAL_RATE_SOURCE_UNSUPPORTED";
                if(!percentage(row.get("coveragePercent"),true,4))return "TARIFF_COVERAGE_INVALID";
                if("CASH_100".equals(t) && number(row.get("coveragePercent")).compareTo(HUNDRED)<0)
                    return "CASH_100_COVERAGE_BELOW_100";
                if("CONFIG".equals(src)){
                    if(!percentage(row.get("ratePercent"),false,8))return "TARIFF_RATE_INVALID";
                }else if(row.get("ratePercent")!=null)return "NON_CONFIG_RATE_MUST_BE_NULL";
                if("REQUEST".equals(src))anyRequest=true;
            }
            if(!inputs.keySet().equals(anyRequest?Set.of("amount","portfolio","contractRate"):
                    Set.of("amount","portfolio")) &&
               !("DAILY".equals(method)&&inputs.keySet().equals(anyRequest?
                    Set.of("amount","portfolio","contractRate","periodStart","periodEndExclusive"):
                    Set.of("amount","portfolio","periodStart","periodEndExclusive"))))
                return "PORTFOLIO_INPUT_FIELDS_UNREVIEWED";
            if(anyRequest&&!percentage(inputs.get("contractRate"),false,8))return "CONTRACT_RATE_REQUIRED";
            Object positions=inputs.get("portfolio");
            if(!(positions instanceof List<?> rows)||rows.isEmpty()||rows.size()>50)
                return "PORTFOLIO_SHARES_REQUIRED";
            Set<String> selected=new HashSet<>();BigDecimal sum=BigDecimal.ZERO;
            for(Object obj:rows){
                if(!(obj instanceof Map<?,?> r)||!TEST_ROW_KEYS.containsAll(r.keySet()))
                    return "UNREVIEWED_REQUEST_COLLATERAL_FIELD";
                String chosen=r.get("collateralType") instanceof String s?s:"";
                if(!selected.add(chosen)||!unique.contains(chosen))return "PORTFOLIO_COLLATERAL_NOT_IN_TARIFF_OR_DUPLICATE";
                if(!percentage(r.get("sharePercent"),true,4)||
                    number(r.get("sharePercent")).compareTo(HUNDRED)>0)return "SHARE_PERCENT_INVALID";
                sum=sum.add(number(r.get("sharePercent")));
            }
            if(sum.compareTo(HUNDRED)!=0)return "PORTFOLIO_SHARES_MUST_TOTAL_100";
            return null;
        }
        if(!TYPES.contains(type)||!Set.of(SINGLE_AMOUNT,SINGLE_COVERED).contains(basis))
            return "SINGLE_COLLATERAL_OR_BASIS_UNREVIEWED";
        if(!rules.isEmpty())return "SINGLE_CANNOT_INCLUDE_PORTFOLIO_TARIFF";
        Set<String> allowed=Set.of("amount");
        if(SINGLE_COVERED.equals(basis)||"CASH_100".equals(type))allowed=Set.of("amount","collateralCoveragePercent");
        if(!inputs.keySet().containsAll(allowed))return "SINGLE_COVERAGE_INPUT_REQUIRED";
        if(inputs.containsKey("collateralCoveragePercent") &&
           !percentage(inputs.get("collateralCoveragePercent"),true,4))return "SINGLE_COVERAGE_INVALID";
        if("CASH_100".equals(type)&&number(inputs.get("collateralCoveragePercent")).compareTo(HUNDRED)<0)
            return "CASH_100_COVERAGE_BELOW_100";
        return null;
    }
    /** Exact-day proration; ACT_ACT divides each Gregorian-year segment by its actual 365/366 denominator. */
    public static BigDecimal fraction(LocalDate start,LocalDate end,String dayBasis){
        if(start==null||end==null||dayBasis==null||!DAYS.contains(dayBasis)||!end.isAfter(start)||
           ChronoUnit.DAYS.between(start,end)>36525)throw bad("EXPLICIT_VALID_PERIOD_REQUIRED");
        BigDecimal total=BigDecimal.ZERO;
        for(LocalDate d=start;d.isBefore(end);){
            LocalDate upper="ACT_ACT".equals(dayBasis)?(d.withDayOfYear(1).plusYears(1).isBefore(end)?d.withDayOfYear(1).plusYears(1):end):end;
            long days=ChronoUnit.DAYS.between(d,upper);
            int denominator=switch(dayBasis){case "ACT_360"->360;case "ACT_365"->365;
                default->d.isLeapYear()?366:365;};
            total=total.add(BigDecimal.valueOf(days).divide(BigDecimal.valueOf(denominator),
                24,RoundingMode.HALF_EVEN));
            d=upper;
        }
        return total;
    }
    public static String guard(Request req,LocalDate bankDay){
        if(bankDay==null)return "ORACLE_BUSINESS_DATE_MISSING";
        if(req==null||req.draft()==null||req.draft().item()==null||
           req.draft().version()==null||req.draft().scope()==null)return "DRAFT_INCOMPLETE";
        DraftRequest d=req.draft(); ItemDraft i=d.item(); VersionDraft v=d.version();
        if(!"GUARANTEE".equals(i.module())||!"GUARANTEE_ISSUE".equals(i.operation())||
           !"GUARANTEE_FEE".equals(i.amountKind()))return "GUARANTEE_ISSUE_ONLY";
        if(v.method()==null||!METHODS.contains(v.method()))return "GUARANTEE_METHOD_NOT_CERTIFIED";
        if(!"IRR".equals(v.serviceCurrency())||!"IRR".equals(v.feeCurrency())||
           !"IRR".equals(d.scope().currency()))return "IRR_ONLY";
        LocalDate effective=req.effectiveOn()==null?bankDay:req.effectiveOn();
        if(effective.isBefore(bankDay))return "HISTORICAL_SIMULATION_NOT_ALLOWED";
        if(v.validFrom()==null||effective.isBefore(v.validFrom())||
           (v.validTo()!=null&&(effective.isAfter(v.validTo())||v.validTo().isBefore(v.validFrom()))))
            return "OUTSIDE_DRAFT_VALIDITY";
        if(round(v.rounding())!=null)return "ROUNDING_POLICY_UNSUPPORTED";
        if(d.rules()==null||d.rules().size()!=1||d.rules().get(0)==null)
            return "EXACTLY_ONE_RULE_REQUIRED";
        RuleDraft r=d.rules().get(0);
        if(r.order()!=1||!v.method().equals(r.method()))return "RULE_METHOD_OR_ORDER_MISMATCH";
        if(r.formulaCode()!=null&&!r.formulaCode().isBlank()||r.fixed()!=null||r.collateral()!=null||r.coverage()!=null)
            return "UNREVIEWED_RULE_NUMERIC_OR_FORMULA_FIELD";
        if(r.rateSource()==null||!Set.of("RULE","REQUEST").contains(r.rateSource()))return "RULE_RATE_SOURCE_UNREVIEWED";
        if(r.min()!=null&&!monetary(r.min(),false)||r.max()!=null&&!monetary(r.max(),false)||
           r.min()!=null&&r.max()!=null&&r.min().compareTo(r.max())>0)return "FEE_CAP_INVALID";
        if(d.calcItemId()==null ^ d.tariffVersionId()==null)return "PARTIAL_ORACLE_REFERENCE";
        if(d.calcItemId()==null&&(d.expectedItemRecordVersion()!=null||
           d.expectedVersionRecordVersion()!=null))return "UNSAVED_WITH_RECORD_VERSION";
        if(d.calcItemId()!=null&&(d.calcItemId()<=0||d.tariffVersionId()<=0||
           d.expectedItemRecordVersion()==null||d.expectedItemRecordVersion()<=0||
           d.expectedVersionRecordVersion()==null||d.expectedVersionRecordVersion()<=0))
            return "INVALID_ORACLE_REFERENCE_VERSION";
        if(v.uiContext()==null||!v.uiContext().keySet().equals(Set.of("guaranteePolicy")))
            return "ONLY_REVIEWED_GUARANTEE_CONTEXT_ALLOWED";
        if(v.financialPolicy()!=null&&!v.financialPolicy().keySet().stream().allMatch("taxPercent"::equals))
            return "FINANCIAL_MODIFIERS_UNREVIEWED";
        Map<String,Object> fp=v.financialPolicy()==null?Map.of():v.financialPolicy();
        if(v.taxMode()==null)return "TAX_MODE_UNREVIEWED";
        if("TAXABLE".equals(v.taxMode())){
            if(!percentage(fp.get("taxPercent"),false,8)||
               number(fp.get("taxPercent")).compareTo(HUNDRED)>0)return "EXPLICIT_TAX_RATE_REQUIRED";
        }else if(Set.of("EXEMPT","NONE","NON_TAXABLE").contains(v.taxMode())){
            if(fp.containsKey("taxPercent"))return "TAX_RATE_WITH_NON_TAXABLE_MODE";
        }else return "TAX_MODE_UNREVIEWED";
        if(req.inputs()==null||!monetary(req.inputs().get("amount"),true))
            return "POSITIVE_GUARANTEE_AMOUNT_REQUIRED";
        if(r.parameters()==null||!r.parameters().keySet().equals(Set.of("originalConfig"))||
           !(r.parameters().get("originalConfig") instanceof Map<?,?> config)||
           !Set.of("dayBasis").containsAll(config.keySet()))return "REVIEWED_DAY_COUNT_CONFIG_REQUIRED";
        if("DAILY".equals(v.method())){
            if(!(config.get("dayBasis") instanceof String basis)||!DAYS.contains(basis))
                return "DAY_BASIS_REQUIRED";
            if(!(req.inputs().get("periodStart") instanceof String start)||
               !(req.inputs().get("periodEndExclusive") instanceof String end))
                return "PERIOD_DATES_REQUIRED";
            try{LocalDate first=LocalDate.parse(start),last=LocalDate.parse(end);
                if(first.isBefore(effective)||!last.isAfter(first)||
                   ChronoUnit.DAYS.between(first,last)>36525)return "PERIOD_OUTSIDE_SUPPORTED_RANGE";
            }catch(java.time.DateTimeException e){return "PERIOD_INVALID_ISO_DATE";}
        }else if(config.containsKey("dayBasis"))return "DAY_BASIS_FOR_NON_DAILY_METHOD";
        Map<?,?> p=policy(req);
        String violation=policyGuard(p,req.inputs(),v.method());
        if(violation!=null)return violation;
        if(PORTFOLIO.equals(p.get("collateralType"))){
            if(!"RULE".equals(r.rateSource())||r.rate()!=null)return "PORTFOLIO_HAS_NO_SINGLE_RATE";
            if(!String.valueOf(p.get("feeBasis")).equals(r.base()))return "PORTFOLIO_RULE_BASIS_MISMATCH";
        }else{
            if(!String.valueOf(p.get("feeBasis")).equals(r.base()))return "SINGLE_RULE_BASIS_MISMATCH";
            if("RULE".equals(r.rateSource())&&!percentage(r.rate(),false,8))
                return "SINGLE_TARIFF_RATE_REQUIRED";
            if("REQUEST".equals(r.rateSource())&&r.rate()!=null)
                return "SINGLE_REQUEST_RATE_MUST_NOT_BE_TARIFF_RATE";
            if("REQUEST".equals(r.rateSource())&&!percentage(req.inputs().get("contractRate"),false,8))
                return "SINGLE_CONTRACT_RATE_REQUIRED";
        }
        if(!PORTFOLIO.equals(p.get("collateralType"))){
            Set<String> expected=new HashSet<>(Set.of("amount"));
            if("CASH_100".equals(p.get("collateralType"))||SINGLE_COVERED.equals(p.get("feeBasis")))
                expected.add("collateralCoveragePercent");
            if("REQUEST".equals(r.rateSource()))expected.add("contractRate");
            if("DAILY".equals(v.method())){expected.add("periodStart");expected.add("periodEndExclusive");}
            if(!req.inputs().keySet().equals(expected))return "SINGLE_INPUT_FIELDS_UNREVIEWED";
        }
        return null;
    }
    private static BigDecimal rateFor(Map<?,?> row,BigDecimal requestRate){
        return switch((String)row.get("rateSource")){
            case "EXEMPT"->BigDecimal.ZERO;
            case "REQUEST"->requestRate;
            case "CONFIG"->number(row.get("ratePercent"));
            default->throw bad("COLLATERAL_RATE_SOURCE_UNSUPPORTED");};
    }
    private static Map<String,Object> trace(String type,BigDecimal share,BigDecimal coverage,
         BigDecimal allocated,BigDecimal face,BigDecimal basis,BigDecimal rate,String source,
         BigDecimal factor,BigDecimal fee){
        Map<String,Object> t=new LinkedHashMap<>();
        t.put("collateralType",type);t.put("sharePercent",share);t.put("coveragePercent",coverage);
        t.put("allocatedAmountIrr",allocated);t.put("collateralFaceValueIrr",face);
        t.put("feeBaseIrr",basis);t.put("ratePercent",rate);t.put("rateSource",source);
        t.put("yearFraction",factor);t.put("rawFeeIrr",fee);return t;
    }
    public static Fee3PreviewEngine.PreviewResult calculate(Request req){
        // MUST be called only after guard(), R3 preflight and SELECT-only Oracle snapshot proof.
        var d=req.draft();var v=d.version();var r=d.rules().get(0);var p=policy(req);
        BigDecimal amount=number(req.inputs().get("amount"));BigDecimal factor=BigDecimal.ONE;
        if("DAILY".equals(v.method())){
            var cfg=(Map<?,?>)r.parameters().get("originalConfig");
            factor=fraction(LocalDate.parse((String)req.inputs().get("periodStart")),
                LocalDate.parse((String)req.inputs().get("periodEndExclusive")),(String)cfg.get("dayBasis"));
        }
        List<Map<String,Object>> lines=new ArrayList<>();BigDecimal raw=BigDecimal.ZERO;
        if(PORTFOLIO.equals(p.get("collateralType"))){
            Map<String,Map<?,?>> tariff=new LinkedHashMap<>();
            for(Object obj:(List<?>)p.get("portfolioRules")){
                Map<?,?> row=(Map<?,?>)obj;tariff.put((String)row.get("collateralType"),row);
            }
            for(Object obj:(List<?>)req.inputs().get("portfolio")){
                Map<?,?> row=(Map<?,?>)obj;String type=(String)row.get("collateralType");
                BigDecimal share=number(row.get("sharePercent"));Map<?,?> policy=tariff.get(type);
                BigDecimal coverage=number(policy.get("coveragePercent"));
                BigDecimal alloc=amount.multiply(share).divide(HUNDRED);
                BigDecimal face=alloc.multiply(coverage).divide(HUNDRED);
                BigDecimal basis=COVERED.equals(p.get("feeBasis"))?face:alloc;
                BigDecimal rate=rateFor(policy,number(req.inputs().get("contractRate")));
                BigDecimal due=basis.multiply(rate).divide(HUNDRED).multiply(factor);
                raw=raw.add(due);
                lines.add(trace(type,share,coverage,alloc,face,basis,rate,
                    (String)policy.get("rateSource"),factor,due));
            }
        }else{
            String type=(String)p.get("collateralType");BigDecimal coverage=number(req.inputs().get("collateralCoveragePercent"));
            BigDecimal face=coverage==null?null:amount.multiply(coverage).divide(HUNDRED);
            BigDecimal basis=SINGLE_COVERED.equals(p.get("feeBasis"))?face:amount;
            BigDecimal rate="REQUEST".equals(r.rateSource())?
                number(req.inputs().get("contractRate")):r.rate();
            BigDecimal due=basis.multiply(rate).divide(HUNDRED).multiply(factor);
            raw=raw.add(due);
            lines.add(trace(type,HUNDRED,coverage,amount,face,basis,rate,r.rateSource(),factor,due));
        }
        if(raw.signum()<0||raw.compareTo(MAX)>0)throw bad("GUARANTEE_FEE_EXCEEDS_SAFE_LIMIT");
        BigDecimal capped=raw;if(r.min()!=null)capped=capped.max(r.min());
        if(r.max()!=null)capped=capped.min(r.max());
        RoundingMode mode=rounding(v.rounding());
        BigDecimal fee=capped.setScale(0,mode);
        BigDecimal tax=BigDecimal.ZERO;
        if("TAXABLE".equals(v.taxMode()))
            tax=fee.multiply(number(v.financialPolicy().get("taxPercent"))).divide(HUNDRED).setScale(0,mode);
        BigDecimal total=fee.add(tax);
        if(total.compareTo(MAX)>0)throw bad("GUARANTEE_TOTAL_EXCEEDS_SAFE_LIMIT");
        return new Fee3PreviewEngine.PreviewResult("SIMULATED",false,false,v.method(),raw,fee,tax,total,
            List.copyOf(lines),List.of("UAT_SIMULATION_ONLY","TARIFF_OWNED_COVERAGE",
                "NO_ORACLE_MUTATION","NO_ACCOUNTING_POSTING"));
    }
    public static Evidence blocked(Request req,LocalDate businessDay,String reason){
        var v=req==null||req.draft()==null?null:req.draft().version();
        return new Evidence(CONTRACT,"BLOCKED",false,false,false,false,"UAT_DRAFT_SIMULATION",null,
            businessDay,req==null||req.effectiveOn()==null?businessDay:req.effectiveOn(),"IRR",
            v==null?null:v.rounding(),0,null,null,List.of(reason));
    }
    public static Evidence finish(Request req,LocalDate businessDay,
        Fee3R4CalculationContract.VersionReference reference,Fee3PreviewEngine.PreviewResult calc){
        if(reference==null||reference.active()||
            !("DRAFT".equals(reference.status())||"UNSAVED".equals(reference.status()))||
            calc==null||!"SIMULATED".equals(calc.status())||calc.financialPosting()||calc.persisted()||
            calc.rawIrr()==null||calc.feeIrr()==null||calc.taxIrr()==null||calc.totalIrr()==null||
            scale(calc.feeIrr())>0||scale(calc.totalIrr())>0)
            return blocked(req,businessDay,"UNSAFE_VERSION_OR_CALCULATION_STATE");
        LocalDate effective=req.effectiveOn()==null?businessDay:req.effectiveOn();
        String hash;
        try{
            String baseline=Fee3R4CalculationContract.fingerprint(new
                Fee3R4CalculationContract.Request(req.draft(),req.inputs(),req.effectiveOn()),reference,effective);
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest((CONTRACT+"|"+baseline+"|"+
                calc.rawIrr().toPlainString()+"|"+calc.totalIrr().toPlainString())
                .getBytes(StandardCharsets.UTF_8));
            hash=java.util.HexFormat.of().formatHex(bytes);
        }catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA256_NOT_AVAILABLE",e);}
        return new Evidence(CONTRACT,"SIMULATED",false,false,false,false,
            effective.isAfter(businessDay)?"FUTURE_DATED_UAT_SIMULATION":"UAT_DRAFT_SIMULATION",
            reference,businessDay,effective,"IRR",req.draft().version().rounding(),0,hash,calc,
            List.of("UNPUBLISHED_DRAFT_ONLY","NOT_A_CHARGE_AUTHORIZATION","NO_ORACLE_WRITE",
                "NO_ACCOUNTING_POSTING","NOT_A_TAX_DETERMINATION"));
    }
}
