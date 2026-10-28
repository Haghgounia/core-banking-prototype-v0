package com.behsazan.corebanking.fee3.f2;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.r3.Fee3PreviewEngine;
import com.behsazan.corebanking.fee3.r4.Fee3R4CalculationContract;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FEE3-F2: strict, eight-code allowlisted SERVICE_FEE formula arithmetic.
 * ONLY a read-only draft UAT preview. No expression parser, eval, writes, tariffs,
 * publication, fee assessment, tax determination or banking transaction.
 */
public final class Fee3F2FormulaContract {
    private Fee3F2FormulaContract() {}
    public static final String CONTRACT = "FEE3_F2_FORMULA_ALLOWLIST_PREVIEW_V1";
    private static final BigDecimal MAX = new BigDecimal("1000000000000000000");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal CARD_UNIT = new BigDecimal("10000000");
    private static final BigDecimal VALUATION_LIMIT = new BigDecimal("400000000000");
    private static final BigDecimal COLLATERAL_BASE = new BigDecimal("2587500");
    private static final BigDecimal COLLATERAL_RATE = new BigDecimal("0.002");
    private static final Set<String> ROUNDINGS = Set.of("HALF_UP","HALF_EVEN","FLOOR","DOWN","CEIL","UP");
    private static final Set<String> CONFIG_KEYS = Set.of("basisCode","brackets","dayBasis","fixedAmount",
        "fixedPart","formula","formulaParams","freeUnits","matrix","maxOutput","minOutput",
        "ratePercent","rateSource","tiers","unitPrice","valuationRule1404");
    private static final Set<String> RULE_META = Set.of("originalConfig","sourceRecordCode","sourceVersion",
        "requiresCurrentApproval","rateFromContract","loanCalculationEvent","loanContractTypes",
        "loanModelProfile","loanPolicy","portfolioRule","portfolioAppliedMinOnce");

    private enum Type { CHEQUE_PAPER, CARD_OLD, CARD_NEW, ACH, PAYROLL, VALUATION, CHEQUE_ELECTRONIC }
    private record Profile(String calcCode, int versionNo, String operation, String formulaCode,
                           String exactExpression, Type type, BigDecimal threshold, String officialRow) {}
    private static final Map<String,Profile> PROFILES;
    static {
        Map<String,Profile> m = new LinkedHashMap<>();
        register(m,"FCE-000051",1,"CHEQUEBOOK_ISSUE","FCE_EXPR_3721E6C16512A004",
            "75000 + printingCost + stampDuty",Type.CHEQUE_PAPER,null,null);
        register(m,"FCE-000157",1,"CARD_TO_CARD","FCE_EXPR_B963F129BCA3C7F3",
            "9000 + max(0,ceil((amount-10000000)/10000000))*3200",Type.CARD_OLD,null,null);
        register(m,"FCE-000157",2,"CARD_TO_CARD","FCE_EXPR_7025358841EFCDF6",
            "11000 + max(0,ceil((amount-10000000)/10000000))*3500",Type.CARD_NEW,null,null);
        register(m,"FCE-000160",1,"ACH_TRANSFER","FCE_EXPR_67AA0A60D5EEF44D",
            "15000 + usage * 1500",Type.ACH,null,null);
        register(m,"FCE-000164",1,"PAYROLL_BATCH","FCE_EXPR_39E716F39BF0EAAE",
            "4500 + usage * 450",Type.PAYROLL,null,null);
        register(m,"FCE-000291",1,"COLLATERAL_VALUATION_REAL_ESTATE","FCE_EXPR_101E3E73C3939534",
            "2587500 + max(0, amount - 20000000000) * 0.002",Type.VALUATION,
            new BigDecimal("20000000000"),"7-2");
        for (String[] info : new String[][] {
            {"FCE-000292","COLLATERAL_VALUATION_MACHINERY","7-1"},
            {"FCE-000293","COLLATERAL_VALUATION_FARMLAND","7-3"},
            {"FCE-000294","COLLATERAL_VALUATION_SURPLUS","7-7"}
        }) {
            register(m,info[0],1,info[1],"FCE_EXPR_228383EB0C8B0B93",
                "2587500 + max(0, amount - 2000000000) * 0.002",Type.VALUATION,
                new BigDecimal("2000000000"),info[2]);
        }
        register(m,"FCE-000296",1,"CHEQUEBOOK_ISSUE_PAPER","FCE_EXPR_3721E6C16512A004",
            "75000 + printingCost + stampDuty",Type.CHEQUE_PAPER,null,null);
        register(m,"FCE-000297",1,"CHEQUEBOOK_ISSUE_ELECTRONIC","FCE_EXPR_9E1A70F17DCB90E8",
            "1.2 * approvedOperatingCost + stampDuty",Type.CHEQUE_ELECTRONIC,null,null);
        PROFILES=Map.copyOf(m);
    }
    private static void register(Map<String,Profile> m,String code,int no,String operation,
            String formula,String expression,Type type,BigDecimal threshold,String row) {
        String key=code+"#"+no;
        if(m.put(key,new Profile(code,no,operation,formula,expression,type,threshold,row))!=null)
            throw new IllegalStateException("DUPLICATE_F2_PROFILE");
    }
    public static int knownVersions() { return PROFILES.size(); }
    public static int uniqueFormulaCodes() {
        return (int)PROFILES.values().stream().map(Profile::formulaCode).distinct().count();
    }
    public record Request(DraftRequest draft,Map<String,Object> inputs,LocalDate effectiveOn) {}
    public record Evidence(String contractVersion,String status,boolean persisted,boolean financialPosting,
        boolean published,boolean approved,String simulationMode,
        Fee3R4CalculationContract.VersionReference versionReference,LocalDate oracleBusinessDate,
        LocalDate effectiveOn,String currency,String roundingCode,int roundingScale,
        String snapshotSha256,Fee3PreviewEngine.PreviewResult calculation,List<String> diagnostics) {}

    private static Profile profile(Request request) {
        if(request==null||request.draft()==null||request.draft().rules()==null||
            request.draft().rules().size()!=1||request.draft().rules().get(0)==null)return null;
        var p=request.draft().rules().get(0).parameters();
        if(p==null)return null;
        Object no=p.get("sourceVersion");
        BigDecimal n=decimal(no);
        if(n==null||n.scale()>0||n.signum()<=0)return null;
        return PROFILES.get(String.valueOf(p.get("sourceRecordCode"))+"#"+n.toPlainString());
    }
    public static String guard(Request req,LocalDate bankDate) {
        if(bankDate==null)return "ORACLE_BUSINESS_DATE_REQUIRED";
        if(req==null||req.draft()==null||req.draft().item()==null||req.draft().version()==null||
           req.draft().scope()==null)return "DRAFT_INCOMPLETE";
        var d=req.draft();var v=d.version();var i=d.item();
        if(!"SERVICE_FEE".equals(i.amountKind()))return "SERVICE_FEE_ONLY";
        if(!"FORMULA".equals(v.method()))return "FORMULA_METHOD_REQUIRED";
        if(!"IRR".equals(v.feeCurrency())||!"IRR".equals(v.serviceCurrency())||
           !"IRR".equals(d.scope().currency()))return "IRR_ONLY_NO_FX";
        LocalDate on=req.effectiveOn()==null?bankDate:req.effectiveOn();
        if(on.isBefore(bankDate))return "HISTORICAL_PREVIEW_NOT_ALLOWED";
        if(v.validFrom()==null||on.isBefore(v.validFrom())||
           (v.validTo()!=null&&(v.validTo().isBefore(v.validFrom())||on.isAfter(v.validTo()))))
            return "OUTSIDE_TARIFF_VALIDITY";
        if(v.rounding()==null||!ROUNDINGS.contains(v.rounding()))return "ROUNDING_POLICY_UNREVIEWED";
        if(v.origin()==null||!Set.of("BANK_INTERNAL","CBI_MANDATORY").contains(v.origin()))
            return "TARIFF_ORIGIN_UNREVIEWED";
        if("CBI_MANDATORY".equals(v.origin())&&v.sourceId()==null)return "CBI_SOURCE_REQUIRED";
        if(v.uiContext()!=null&&!v.uiContext().isEmpty())return "SPECIAL_CONTEXT_UNCERTIFIED";
        if(d.rules()==null||d.rules().size()!=1||d.rules().get(0)==null)
            return "EXACTLY_ONE_RULE_REQUIRED";
        var r=d.rules().get(0);
        if(r.order()!=1||!"FORMULA".equals(r.method())||!"REQUEST_AMOUNT".equals(r.base())||
           !"RULE".equals(r.rateSource()))return "FORMULA_RULE_MISMATCH";
        if(r.rate()!=null||r.fixed()!=null||r.coverage()!=null||
           (r.collateral()!=null&&!r.collateral().isBlank())||r.min()!=null||r.max()!=null)
            return "FORMULA_RULE_HAS_UNREVIEWED_MODIFIERS";
        if((d.calcItemId()==null)!=(d.tariffVersionId()==null))return "PARTIAL_ORACLE_REFERENCE";
        if(d.calcItemId()==null&&(d.expectedItemRecordVersion()!=null||d.expectedVersionRecordVersion()!=null))
            return "UNSAVED_RECORD_VERSION_FORBIDDEN";
        if(d.calcItemId()!=null&&(d.calcItemId()<=0||d.tariffVersionId()<=0||
            d.expectedItemRecordVersion()==null||d.expectedItemRecordVersion()<=0||
            d.expectedVersionRecordVersion()==null||d.expectedVersionRecordVersion()<=0))
            return "INVALID_ORACLE_VERSION_REFERENCE";
        var p=profile(req);
        if(p==null)return "UNREVIEWED_FORMULA_PROFILE";
        if(!p.operation().equals(i.operation())||!p.formulaCode().equals(r.formulaCode()))
            return "FORMULA_CODE_OPERATION_MISMATCH";
        if(r.parameters().keySet().stream().anyMatch(k->!RULE_META.contains(k)))
            return "UNKNOWN_RULE_METADATA";
        if(r.parameters().get("requiresCurrentApproval")!=null&&
            !Boolean.TRUE.equals(r.parameters().get("requiresCurrentApproval")))
            return "FORMULA_APPROVAL_METADATA_MISMATCH";
        if(r.parameters().get("rateFromContract")!=null&&
            !Boolean.FALSE.equals(r.parameters().get("rateFromContract")))
            return "CONTRACT_RATE_MODIFIER_UNSUPPORTED";
        for(String key:List.of("loanCalculationEvent","loanContractTypes","loanModelProfile",
                "loanPolicy","portfolioRule","portfolioAppliedMinOnce"))
            if(r.parameters().get(key)!=null)return "SPECIAL_FORMULA_CONTEXT_UNREVIEWED";
        if(!(r.parameters().get("originalConfig") instanceof Map<?,?> cfg))
            return "FORMULA_CONFIG_MISSING";
        for(Object key:cfg.keySet())if(!(key instanceof String s)||!CONFIG_KEYS.contains(s))
            return "UNKNOWN_FORMULA_CONFIG_FIELD";
        if(!"REQUEST_AMOUNT".equals(cfg.get("basisCode"))||
           !"CONFIG".equals(cfg.get("rateSource"))||
           !p.exactExpression().equals(cfg.get("formula")))
            return "UNVERIFIED_FORMULA_EXPRESSION";
        if(!empty(cfg.get("brackets"))||!empty(cfg.get("matrix"))||!empty(cfg.get("tiers"))||
           nonNull(cfg.get("fixedAmount"))||nonNull(cfg.get("fixedPart"))||
           nonNull(cfg.get("ratePercent"))||nonNull(cfg.get("unitPrice"))||
           nonNull(cfg.get("maxOutput"))||nonNull(cfg.get("minOutput"))||
           !isZero(cfg.get("freeUnits")))return "EXTRA_PRICING_MODIFIER_UNREVIEWED";
        if(!"ACT_ACT".equals(cfg.get("dayBasis")))return "UNEXPECTED_LEGACY_DAY_BASIS";
        if(!validFormulaParameters(cfg.get("formulaParams"),p.type()))
            return "FORMULA_PARAMETER_SCHEMA_MISMATCH";
        if(p.type()==Type.VALUATION){
            if(!(cfg.get("valuationRule1404") instanceof Map<?,?> rule)||
               !rule.keySet().equals(Set.of("baseFee","marginalRate","maximumAutoValue",
                   "officialRow","requireExpertFeeForExcess","threshold"))||
               !same(rule.get("baseFee"),COLLATERAL_BASE)||
               !same(rule.get("marginalRate"),COLLATERAL_RATE)||
               !same(rule.get("maximumAutoValue"),VALUATION_LIMIT)||
               !same(rule.get("threshold"),p.threshold())||
               !p.officialRow().equals(rule.get("officialRow"))||
               !Boolean.TRUE.equals(rule.get("requireExpertFeeForExcess")))
                return "VALUATION_POLICY_METADATA_MISMATCH";
        } else if(cfg.get("valuationRule1404")!=null)return "UNEXPECTED_VALUATION_POLICY";
        Map<String,Object> policy=v.financialPolicy()==null?Map.of():v.financialPolicy();
        if(policy.keySet().stream().anyMatch(k->!"taxPercent".equals(k)))
            return "FINANCIAL_POLICY_UNREVIEWED";
        if("TAXABLE".equals(v.taxMode())) {
            if(!percent(policy.get("taxPercent")))return "EXPLICIT_TAX_PERCENT_REQUIRED";
        }else if(v.taxMode()!=null&&Set.of("EXEMPT","NONE","NON_TAXABLE").contains(v.taxMode())){
            if(policy.get("taxPercent")!=null)return "TAX_PERCENT_NOT_ALLOWED_FOR_EXEMPT";
        }else return "TAX_POLICY_UNREVIEWED";
        if(req.inputs()==null||!req.inputs().keySet().equals(expectedInputs(p.type())))
            return "FORMULA_INPUT_CONTRACT_MISMATCH";
        for(var entry:req.inputs().entrySet()){
            BigDecimal n=decimal(entry.getValue());
            if(n==null||n.signum()<0||n.compareTo(MAX)>0||
               Math.max(0,n.stripTrailingZeros().scale())>2)return "FORMULA_INPUT_NOT_VALID_IRR_OR_COUNT";
            if("usage".equals(entry.getKey())){
                if(n.stripTrailingZeros().scale()>0||n.signum()==0)
                    return "USAGE_MUST_BE_POSITIVE_INTEGER";
            }else if("amount".equals(entry.getKey())&&n.signum()==0)
                return "TRANSFER_OR_VALUATION_AMOUNT_REQUIRED";
        }
        if(p.type()==Type.VALUATION&&
           decimal(req.inputs().get("amount")).compareTo(VALUATION_LIMIT)>0)
            return "CERTIFIED_EXPERT_FEE_REQUIRED_ABOVE_AUTO_LIMIT";
        return null;
    }
    private static Set<String> expectedInputs(Type t){
        return switch(t){
            case CHEQUE_PAPER -> Set.of("printingCost","stampDuty");
            case CHEQUE_ELECTRONIC -> Set.of("approvedOperatingCost","stampDuty");
            case CARD_OLD,CARD_NEW,VALUATION -> Set.of("amount");
            case ACH,PAYROLL -> Set.of("usage");
        };
    }
    private static boolean validFormulaParameters(Object raw,Type type){
        if(!(raw instanceof List<?> list))return false;
        Set<String> required=switch(type){
            case CHEQUE_PAPER -> Set.of("printingCost","stampDuty");
            case CHEQUE_ELECTRONIC -> Set.of("approvedOperatingCost","stampDuty");
            case VALUATION -> Set.of("officialExpertFeeForExcess");
            default -> Set.of();
        };
        if(list.size()!=required.size())return false;
        java.util.HashSet<String> found=new java.util.HashSet<>();
        for(Object value:list){
            if(!(value instanceof Map<?,?> r)||!(r.get("name") instanceof String name)||
               !"MONEY".equals(r.get("type"))||!r.keySet().equals(Set.of("name","type","label"))||
               !(r.get("label") instanceof String))return false;
            found.add(name);
        }
        return required.equals(found);
    }
    private static boolean empty(Object raw){return raw instanceof List<?> list&&list.isEmpty();}
    private static boolean nonNull(Object raw){return raw!=null;}
    private static boolean isZero(Object v){return v!=null&&same(v,BigDecimal.ZERO);}
    private static boolean same(Object raw,BigDecimal target){
        BigDecimal b=decimal(raw);return b!=null&&b.compareTo(target)==0;
    }
    private static BigDecimal decimal(Object raw){
        if(!(raw instanceof Number||raw instanceof String))return null;
        String s=raw.toString();if(s.length()>44)return null;
        try{return new BigDecimal(s);}catch(NumberFormatException ex){return null;}
    }
    private static boolean percent(Object raw){
        BigDecimal n=decimal(raw);return n!=null&&n.signum()>=0&&n.compareTo(HUNDRED)<=0&&
            Math.max(0,n.stripTrailingZeros().scale())<=8;
    }
    private static RoundingMode rounding(String code){
        return switch(code){
            case "HALF_UP" -> RoundingMode.HALF_UP;
            case "HALF_EVEN" -> RoundingMode.HALF_EVEN;
            case "FLOOR","DOWN" -> RoundingMode.FLOOR;
            case "CEIL","UP" -> RoundingMode.CEILING;
            default -> throw new IllegalArgumentException("ROUNDING_UNSUPPORTED");
        };
    }

    /** Call only AFTER guard() and R3/Oracle snapshot validation; pure arithmetic. */
    public static Fee3PreviewEngine.PreviewResult calculate(Request req){
        Profile p=profile(req);
        if(p==null)throw new IllegalArgumentException("UNKNOWN_F2_PROFILE");
        BigDecimal raw;Map<String,Object> trace=new LinkedHashMap<>();
        BigDecimal amount=decimal(req.inputs().get("amount"));
        BigDecimal usage=decimal(req.inputs().get("usage"));
        switch(p.type()){
            case CHEQUE_PAPER -> {
                BigDecimal printing=decimal(req.inputs().get("printingCost"));
                BigDecimal stamp=decimal(req.inputs().get("stampDuty"));
                raw=new BigDecimal("75000").add(printing).add(stamp);
                trace.put("printingCostIrr",printing);trace.put("stampDutyIrr",stamp);
            }
            case CHEQUE_ELECTRONIC -> {
                BigDecimal operating=decimal(req.inputs().get("approvedOperatingCost"));
                BigDecimal stamp=decimal(req.inputs().get("stampDuty"));
                raw=operating.multiply(new BigDecimal("1.2")).add(stamp);
                trace.put("approvedOperatingCostIrr",operating);trace.put("stampDutyIrr",stamp);
            }
            case CARD_OLD,CARD_NEW -> {
                BigDecimal extra=amount.subtract(CARD_UNIT).max(BigDecimal.ZERO)
                    .divide(CARD_UNIT,0,RoundingMode.CEILING);
                BigDecimal base=p.type()==Type.CARD_OLD?new BigDecimal("9000"):new BigDecimal("11000");
                BigDecimal step=p.type()==Type.CARD_OLD?new BigDecimal("3200"):new BigDecimal("3500");
                raw=base.add(extra.multiply(step));
                trace.put("amountIrr",amount);trace.put("additionalBlocks",extra);
            }
            case ACH,PAYROLL -> {
                BigDecimal base=p.type()==Type.ACH?new BigDecimal("15000"):new BigDecimal("4500");
                BigDecimal per=p.type()==Type.ACH?new BigDecimal("1500"):new BigDecimal("450");
                raw=base.add(usage.multiply(per));
                trace.put("usageUnits",usage);trace.put("batchBaseIrr",base);
            }
            case VALUATION -> {
                if(amount.compareTo(VALUATION_LIMIT)>0)
                    throw new IllegalArgumentException("CERTIFIED_EXPERT_FEE_REQUIRED_ABOVE_AUTO_LIMIT");
                BigDecimal excess=amount.subtract(p.threshold()).max(BigDecimal.ZERO);
                raw=COLLATERAL_BASE.add(excess.multiply(COLLATERAL_RATE));
                trace.put("assetValueIrr",amount);trace.put("thresholdIrr",p.threshold());
                trace.put("excessIrr",excess);trace.put("autoValueLimitIrr",VALUATION_LIMIT);
            }
            default -> throw new IllegalArgumentException("UNKNOWN_FORMULA_TYPE");
        }
        if(raw.signum()<0||raw.compareTo(MAX)>0)throw new IllegalArgumentException("RAW_FEE_EXCEEDS_LIMIT");
        RoundingMode mode=rounding(req.draft().version().rounding());
        BigDecimal fee=raw.setScale(0,mode);
        BigDecimal tax=BigDecimal.ZERO;
        if("TAXABLE".equals(req.draft().version().taxMode())){
            BigDecimal rate=decimal(req.draft().version().financialPolicy().get("taxPercent"));
            tax=fee.multiply(rate).divide(HUNDRED,12,RoundingMode.HALF_EVEN).setScale(0,mode);
        }
        BigDecimal total=fee.add(tax);
        if(total.compareTo(MAX)>0)throw new IllegalArgumentException("TOTAL_EXCEEDS_LIMIT");
        trace.put("formulaCode",p.formulaCode());
        trace.put("referenceOnly",true);trace.put("rawFeeIrr",raw);
        return new Fee3PreviewEngine.PreviewResult("SIMULATED",false,false,"FORMULA",raw,fee,tax,total,
            List.of(trace),List.of("UAT_FORMULA_ALLOWLIST_ONLY","NO_DYNAMIC_EXPRESSION_EVALUATION",
                "UNVERIFIED_BANK_SOURCE_NOT_CERTIFIED","NO_ORACLE_WRITE","NO_POSTING"));
    }
    public static Evidence blocked(Request req,LocalDate bankDate,String code){
        var v=req==null||req.draft()==null?null:req.draft().version();
        LocalDate on=req==null||req.effectiveOn()==null?bankDate:req.effectiveOn();
        return new Evidence(CONTRACT,"BLOCKED",false,false,false,false,"UAT_DRAFT_SIMULATION",
            null,bankDate,on,"IRR",v==null?null:v.rounding(),0,null,null,List.of(code));
    }
    public static Evidence finish(Request req,LocalDate bankDate,
            Fee3R4CalculationContract.VersionReference ref,Fee3PreviewEngine.PreviewResult calc){
        if(ref==null||ref.active()||!("DRAFT".equals(ref.status())||"UNSAVED".equals(ref.status()))||
            calc==null||!"SIMULATED".equals(calc.status())||calc.persisted()||calc.financialPosting()||
            calc.feeIrr()==null||calc.taxIrr()==null||calc.totalIrr()==null||
            calc.feeIrr().scale()!=0||calc.totalIrr().scale()!=0)
            return blocked(req,bankDate,"UNSAFE_RESULT_OR_REFERENCE");
        LocalDate on=req.effectiveOn()==null?bankDate:req.effectiveOn();
        String digest;
        try {
            var core=new Fee3R4CalculationContract.Request(req.draft(),req.inputs(),req.effectiveOn());
            String fp=Fee3R4CalculationContract.fingerprint(core,ref,on);
            String payload=CONTRACT+"|"+fp+"|"+calc.rawIrr().toPlainString()+"|"+calc.totalIrr().toPlainString();
            digest=java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(payload.getBytes(StandardCharsets.UTF_8)));
        }catch(NoSuchAlgorithmException ex){throw new IllegalStateException("SHA256_UNAVAILABLE",ex);}
        return new Evidence(CONTRACT,"SIMULATED",false,false,false,false,
            on.isAfter(bankDate)?"FUTURE_DATED_UAT_SIMULATION":"UAT_DRAFT_SIMULATION",
            ref,bankDate,on,"IRR",req.draft().version().rounding(),0,digest,calc,
            List.of("UNAPPROVED_DRAFT_ONLY","NOT_A_CHARGE_AUTHORIZATION","NOT_A_TAX_DETERMINATION",
                "NO_ORACLE_WRITE","NO_ACCOUNTING_POSTING"));
    }
}
