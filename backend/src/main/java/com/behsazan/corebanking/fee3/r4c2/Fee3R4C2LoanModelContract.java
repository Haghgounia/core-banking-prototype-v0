package com.behsazan.corebanking.fee3.r4c2;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.r4.Fee3R4CalculationContract;
import com.behsazan.corebanking.fee3.r4c1.Fee3R4C1LoanProfitContract;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * R4-C2: strictly whitelisted, NO-WRITE UAT arithmetic for participatory contracts,
 * debt purchase and Salam. No profit recognition, contractual verification, tax or posting.
 * The formulas below are explicit Java code: Oracle formula text and caller expressions
 * are NEVER executed. An active formula reference is vocabulary, not approval.
 */
public final class Fee3R4C2LoanModelContract {
    private Fee3R4C2LoanModelContract() {}
    public static final String CONTRACT = "FEE3_LOAN_MODELS_R4C2_V1";
    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal LIMIT = new BigDecimal("1000000000000000000");
    private static final String JALALI_FORMULA = "TPL_E624746F262F6E";
    private static final Set<String> ROUNDINGS = Set.of("HALF_UP","HALF_EVEN","FLOOR","DOWN","UP","CEIL");
    private static final Set<String> INPUT_EVIDENCE = Set.of("contractReference","calculationEvidenceReference");

    public record Request(DraftRequest draft, Map<String,Object> inputs, LocalDate effectiveOn) {}
    public record Calculation(String status,String modelProfile,String resultKind,
        BigDecimal rawIrr,BigDecimal roundedIrr,List<Map<String,Object>> breakdown,
        List<String> diagnostics) {}
    public record Evidence(String contractVersion,String status,boolean persisted,
        boolean financialPosting,boolean published,boolean approved,String simulationMode,
        String modelProfile,String resultKind,
        Fee3R4CalculationContract.VersionReference versionReference,
        LocalDate oracleBusinessDate,LocalDate effectiveOn,String currency,
        String roundingCode,int roundingScale,String snapshotSha256,
        Calculation calculation,List<String> diagnostics) {}
    public record Spec(String profile,String event,String formula,String dayCount,
        Set<String> contracts,String resultKind,boolean estimated) {}

    private static final Map<String,Spec> SPECS = Map.of(
        "PARTICIPATION_REALIZED",new Spec("PARTICIPATION_REALIZED","REALIZED_PROFIT",
            "PARTICIPATION_REALIZED","CONTRACT_MONTHS",Set.of("MUSHARAKA_MADANI","MUDARABA"),
            "BANK_SHARE_OF_REPORTED_REALIZED_PROFIT_UNVERIFIED",false),
        "PARTICIPATION_ESTIMATE",new Spec("PARTICIPATION_ESTIMATE","ESTIMATED_PROFIT",
            JALALI_FORMULA,"JALALI_ACT_ACT",Set.of("MUSHARAKA_MADANI","MUDARABA"),
            "PARTICIPATION_PERIOD_ESTIMATE_NOT_REALIZED",true),
        "DEBT_PURCHASE_SPREAD",new Spec("DEBT_PURCHASE_SPREAD","PURCHASE_SPREAD",
            "DEBT_PURCHASE_SPREAD","CONTRACT_MONTHS",Set.of("DEBT_PURCHASE"),
            "GROSS_DEBT_PURCHASE_SPREAD_NOT_RECOGNIZED_PROFIT",false),
        "SALAM_SPREAD",new Spec("SALAM_SPREAD","SALAM_SETTLEMENT",
            "SALAM_PRICE_SPREAD","CONTRACT_MONTHS",Set.of("SALAM"),
            "GROSS_SALAM_SPREAD_NOT_RECOGNIZED_PROFIT",false),
        "SALAM_ESTIMATE",new Spec("SALAM_ESTIMATE","ESTIMATED_PROFIT",
            JALALI_FORMULA,"JALALI_ACT_ACT",Set.of("SALAM"),
            "SALAM_PERIOD_ESTIMATE_NOT_REALIZED",true)
    );

    public static Spec spec(Request r) {
        if(r==null||r.draft()==null||r.draft().version()==null||
           r.draft().version().uiContext()==null)return null;
        Object profile=r.draft().version().uiContext().get("loanModelProfile");
        return profile instanceof String p?SPECS.get(p):null;
    }
    private static BigDecimal decimal(Object val) {
        if(!(val instanceof Number||val instanceof String) || val instanceof Boolean)return null;
        try {return new BigDecimal(String.valueOf(val));}
        catch(NumberFormatException ex){return null;}
    }
    private static int scale(BigDecimal val){return Math.max(0,val.stripTrailingZeros().scale());}
    private static boolean money(Object val,boolean positive){
        BigDecimal b=decimal(val);
        return b!=null&&(positive?b.signum()>0:b.signum()>=0)&&b.compareTo(LIMIT)<=0&&scale(b)<=2;
    }
    private static boolean percent(Object val,boolean positive){
        BigDecimal b=decimal(val);
        return b!=null&&(positive?b.signum()>0:b.signum()>=0)&&b.compareTo(HUNDRED)<=0&&scale(b)<=8;
    }
    private static boolean evidence(Object val){
        return val instanceof String s&&!s.isBlank()&&s.matches("[A-Za-z0-9_./:-]{3,100}");
    }
    private static RoundingMode rounding(String val){return switch(val){
        case "HALF_UP"->RoundingMode.HALF_UP;
        case "HALF_EVEN"->RoundingMode.HALF_EVEN;
        case "FLOOR","DOWN"->RoundingMode.FLOOR;
        case "CEIL","UP"->RoundingMode.CEILING;
        default -> throw new IllegalArgumentException("UNSUPPORTED_ROUNDING");
    };}

    public static String guard(Request req,LocalDate bankDate) {
        if(bankDate==null)return "ORACLE_BANK_DATE_REQUIRED";
        if(req==null||req.draft()==null||req.draft().item()==null||
            req.draft().version()==null||req.draft().scope()==null)return "DRAFT_REQUIRED";
        var d=req.draft();var v=d.version();var i=d.item();var sc=d.scope();
        Spec s=spec(req);
        if(s==null)return "MODEL_NOT_WHITELISTED";
        LocalDate effective=req.effectiveOn()==null?bankDate:req.effectiveOn();
        if(effective.isBefore(bankDate))return "HISTORICAL_EFFECTIVE_UNSUPPORTED";
        if(v.validFrom()==null||v.validFrom().isAfter(effective)||
           v.validTo()!=null&&v.validTo().isBefore(effective))return "OUTSIDE_DRAFT_VALIDITY";
        if(!"LOAN".equals(i.module())||!"LOAN_PROFIT".equals(i.operation())||
           !"FINANCING_PROFIT".equals(i.amountKind())||!"LOAN".equals(i.ownerDomain()))
            return "NOT_LOAN_PROFIT";
        if(!"CONTRACT_RATE_REGULATED".equals(v.origin())||!"FORMULA".equals(v.method()))
            return "LOAN_FORMULA_ORIGIN_REQUIRED";
        if(!"OUT_OF_SCOPE".equals(v.taxMode())||v.financialPolicy()!=null&&
           !v.financialPolicy().isEmpty())return "TAX_AND_FINANCIAL_POLICY_OUT_OF_SCOPE";
        if(!"IRR".equals(v.serviceCurrency())||!"IRR".equals(v.feeCurrency())||
           !"IRR".equals(sc.currency()))return "IRR_ONLY";
        if(!ROUNDINGS.contains(v.rounding()))return "UNSUPPORTED_ROUNDING";
        Map<String,Object> context=v.uiContext();
        if(!s.event().equals(context.get("loanCalculationEvent"))||
           Boolean.TRUE.equals(context.get("loanFormulaContextReviewRequired")))
            return "LOAN_MODEL_EVENT_MISMATCH_OR_PENDING";
        Object types=context.get("loanContractTypes");
        if(!(types instanceof List<?> list)||list.size()!=1||
           !(list.get(0) instanceof String contract)||!s.contracts().contains(contract))
            return "CONTRACT_TYPE_MODEL_MISMATCH";
        if(sc.contracts()==null||!sc.contracts().equals(List.of(contract)))
            return "CONTRACT_SCOPE_MISMATCH";
        if(!(context.get("loanPolicy") instanceof Map<?,?> policy)||
           !Boolean.TRUE.equals(policy.get("contractEvidenceRequired"))||
           !"CHARGE_ALL_DAYS".equals(policy.get("holidayPolicy"))||
           !s.dayCount().equals(policy.get("dayCountBasis"))||
           policy.containsKey("businessCalendarRef")||
           Boolean.FALSE.equals(policy.get("contractInputsRequired")))
            return "LOAN_POLICY_UNREVIEWED";
        if(d.rules()==null||d.rules().size()!=1||d.rules().get(0)==null)
            return "SINGLE_RULE_REQUIRED";
        RuleDraft rule=d.rules().get(0);
        if(rule.order()!=1||!"FORMULA".equals(rule.method())||
           !"REQUEST_AMOUNT".equals(rule.base())||!"REQUEST".equals(rule.rateSource())||
           rule.rate()!=null||rule.fixed()!=null||rule.min()!=null||rule.max()!=null||
           rule.coverage()!=null||rule.collateral()!=null)return "UNSUPPORTED_MODEL_RULE";
        if(!s.formula().equals(rule.formulaCode()))return "MODEL_FORMULA_MISMATCH";
        if(rule.parameters()!=null&&(
           rule.parameters().containsKey("_fceEditableExpressionV1")||
           rule.parameters().containsKey("yearFraction")||
           rule.parameters().containsKey("contractMonths")||
           rule.parameters().containsKey("rate")||
           rule.parameters().containsKey("sharePercent")))
            return "CLIENT_FORMULA_OVERRIDE_FORBIDDEN";
        if(req.inputs()==null)return "LOAN_INPUTS_REQUIRED";
        Set<String> expected=switch(s.profile()){
            case "PARTICIPATION_REALIZED" -> Set.of("contractReference","calculationEvidenceReference",
                "eventDate","realizedProfit","sharePercent","realizationStatementReference","shareAgreementReference");
            case "DEBT_PURCHASE_SPREAD" -> Set.of("contractReference","calculationEvidenceReference",
                "eventDate","faceAmount","purchasePrice","faceAmountEvidenceReference","purchasePriceEvidenceReference");
            case "SALAM_SPREAD" -> Set.of("contractReference","calculationEvidenceReference",
                "eventDate","saleAmount","purchaseAmount","saleEvidenceReference","purchaseEvidenceReference");
            case "PARTICIPATION_ESTIMATE","SALAM_ESTIMATE" -> Set.of("contractReference",
                "calculationEvidenceReference","amount","contractRate","rateEvidenceReference",
                "periodStart","periodEndExclusive","estimateBasisReference");
            default -> throw new IllegalStateException("WHITELIST_MISSING");
        };
        if(!req.inputs().keySet().equals(expected))return "UNREVIEWED_INPUT_FIELDS";
        for(String k:INPUT_EVIDENCE)if(!evidence(req.inputs().get(k)))
            return "CONTRACT_EVIDENCE_REQUIRED";
        if(s.estimated()){
            if(!money(req.inputs().get("amount"),true))return "ESTIMATE_AMOUNT_NUMBER_22_2";
            if(!percent(req.inputs().get("contractRate"),false))return "ESTIMATE_RATE_NUMBER_18_8";
            if(!evidence(req.inputs().get("rateEvidenceReference"))||
               !evidence(req.inputs().get("estimateBasisReference")))
                return "ESTIMATE_CONTRACT_EVIDENCE_REQUIRED";
            LocalDate from,to;
            try{
                from=LocalDate.parse((String)req.inputs().get("periodStart"));
                to=LocalDate.parse((String)req.inputs().get("periodEndExclusive"));
            }catch(RuntimeException ex){return "INVALID_ESTIMATE_ISO_PERIOD";}
            if(!to.isAfter(from)||ChronoUnit.DAYS.between(from,to)>3653||
               from.isBefore(v.validFrom())||v.validTo()!=null&&to.isAfter(v.validTo().plusDays(1)))
                return "ESTIMATE_PERIOD_UNSUPPORTED";
        }else{
            if(!evidence(req.inputs().get("eventDate"))){
                // ISO dates are narrower than generic evidence strings; validate below.
                return "EVENT_DATE_REQUIRED";
            }
            LocalDate event;
            try{event=LocalDate.parse((String)req.inputs().get("eventDate"));}
            catch(RuntimeException ex){return "INVALID_EVENT_DATE";}
            if(!event.equals(effective)||event.isAfter(bankDate))return "EVENT_NOT_AT_BANK_BUSINESS_DATE";
            if("PARTICIPATION_REALIZED".equals(s.profile())){
                if(!money(req.inputs().get("realizedProfit"),false)||
                   !percent(req.inputs().get("sharePercent"),true))
                    return "REALIZED_PROFIT_OR_SHARE_INVALID";
                if(!evidence(req.inputs().get("realizationStatementReference"))||
                   !evidence(req.inputs().get("shareAgreementReference")))
                    return "REALIZATION_EVIDENCE_REQUIRED";
            }else if("DEBT_PURCHASE_SPREAD".equals(s.profile())){
                if(!money(req.inputs().get("faceAmount"),true)||
                   !money(req.inputs().get("purchasePrice"),true))
                    return "DEBT_PURCHASE_MONEY_INVALID";
                if(!evidence(req.inputs().get("faceAmountEvidenceReference"))||
                   !evidence(req.inputs().get("purchasePriceEvidenceReference")))
                    return "DEBT_PURCHASE_EVIDENCE_REQUIRED";
                if(decimal(req.inputs().get("faceAmount")).compareTo(
                    decimal(req.inputs().get("purchasePrice")))<0)
                    return "NEGATIVE_SPREAD_NEEDS_LOSS_POLICY";
            }else{
                if(!money(req.inputs().get("saleAmount"),true)||
                   !money(req.inputs().get("purchaseAmount"),true))
                    return "SALAM_MONEY_INVALID";
                if(!evidence(req.inputs().get("saleEvidenceReference"))||
                   !evidence(req.inputs().get("purchaseEvidenceReference")))
                    return "SALAM_EVIDENCE_REQUIRED";
                if(decimal(req.inputs().get("saleAmount")).compareTo(
                    decimal(req.inputs().get("purchaseAmount")))<0)
                    return "NEGATIVE_SPREAD_NEEDS_LOSS_POLICY";
            }
        }
        if(d.calcItemId()==null ^ d.tariffVersionId()==null)return "PARTIAL_ORACLE_REFERENCE";
        if(d.calcItemId()==null&&(d.expectedItemRecordVersion()!=null||
           d.expectedVersionRecordVersion()!=null))return "UNSAVED_ROW_VERSION_FORBIDDEN";
        if(d.calcItemId()!=null&&(d.calcItemId()<=0||d.tariffVersionId()<=0||
           d.expectedItemRecordVersion()==null||d.expectedItemRecordVersion()<=0||
           d.expectedVersionRecordVersion()==null||d.expectedVersionRecordVersion()<=0))
            return "ORACLE_REFERENCE_INCOMPLETE";
        return null;
    }

    public static Calculation calculate(Request req,
        List<Fee3R4C1LoanProfitContract.YearSlice> slices) {
        Spec spec=spec(req);
        if(spec==null)throw new IllegalArgumentException("MODEL_NOT_WHITELISTED");
        Map<String,Object> in=req.inputs();
        BigDecimal raw;
        List<Map<String,Object>> trace=new ArrayList<>();
        if(spec.estimated()){
            LocalDate from=LocalDate.parse((String)in.get("periodStart"));
            LocalDate to=LocalDate.parse((String)in.get("periodEndExclusive"));
            BigDecimal factor=Fee3R4C1LoanProfitContract.yearFactor(from,to,slices);
            for(var row:slices){
                Map<String,Object> detail=new LinkedHashMap<>();
                detail.put("jalaliYear",row.jalaliYear());
                detail.put("fromInclusive",row.fromInclusive());
                detail.put("toExclusive",row.toExclusive());
                detail.put("days",row.days());
                detail.put("yearDenominator",row.daysInYear());
                detail.put("calendarDatasetEvidence",row.calendarDatasetEvidence());
                detail.put("yearFraction",BigDecimal.valueOf(row.days()).divide(
                    BigDecimal.valueOf(row.daysInYear()),MC));
                trace.add(detail);
            }
            raw=decimal(in.get("amount")).multiply(decimal(in.get("contractRate")),MC)
                .divide(HUNDRED,MC).multiply(factor,MC);
        }else{
            if(slices!=null&&!slices.isEmpty())
                throw new IllegalArgumentException("CAL2_SEGMENTS_FOR_NON_ESTIMATE_FORBIDDEN");
            Map<String,Object> detail=new LinkedHashMap<>();
            detail.put("eventDate",in.get("eventDate"));
            if("PARTICIPATION_REALIZED".equals(spec.profile())){
                raw=decimal(in.get("realizedProfit")).multiply(decimal(in.get("sharePercent")),MC)
                    .divide(HUNDRED,MC);
                detail.put("reportedRealizedProfitIrr",decimal(in.get("realizedProfit")));
                detail.put("contractSharePercent",decimal(in.get("sharePercent")));
            }else if("DEBT_PURCHASE_SPREAD".equals(spec.profile())){
                raw=decimal(in.get("faceAmount")).subtract(decimal(in.get("purchasePrice")),MC);
                detail.put("faceAmountIrr",decimal(in.get("faceAmount")));
                detail.put("purchasePriceIrr",decimal(in.get("purchasePrice")));
            }else if("SALAM_SPREAD".equals(spec.profile())){
                raw=decimal(in.get("saleAmount")).subtract(decimal(in.get("purchaseAmount")),MC);
                detail.put("realizedSaleAmountIrr",decimal(in.get("saleAmount")));
                detail.put("purchaseAmountIrr",decimal(in.get("purchaseAmount")));
            }else throw new IllegalArgumentException("MODEL_NOT_WHITELISTED");
            trace.add(detail);
        }
        if(raw.signum()<0||raw.compareTo(LIMIT)>0)
            throw new IllegalArgumentException("MODEL_RESULT_OUTSIDE_SAFE_LIMIT");
        BigDecimal rounded=raw.setScale(0,rounding(req.draft().version().rounding()));
        return new Calculation("SIMULATED",spec.profile(),spec.resultKind(),raw,rounded,
            List.copyOf(trace),List.of("ONLY_GROSS_OR_ESTIMATE_UAT","CLIENT_EVIDENCE_UNVERIFIED",
                "NO_ORACLE_WRITE","NO_PROFIT_RECOGNITION","NO_POSTING"));
    }
    public static Evidence blocked(Request req,LocalDate bankDate,String error){
        var v=req==null||req.draft()==null?null:req.draft().version();
        LocalDate effective=req==null||req.effectiveOn()==null?bankDate:req.effectiveOn();
        Spec s=spec(req);
        return new Evidence(CONTRACT,"BLOCKED",false,false,false,false,"UNVERIFIED_UAT_DRAFT",
            s==null?null:s.profile(),s==null?null:s.resultKind(),null,bankDate,effective,
            "IRR",v==null?null:v.rounding(),0,null,null,List.of(error));
    }
    public static Evidence finish(Request req,LocalDate bankDate,
        Fee3R4CalculationContract.VersionReference source,Calculation calc) {
        Spec selected=spec(req);
        if(source==null||source.active()||!Set.of("UNSAVED","DRAFT").contains(source.status())||
           selected==null||calc==null||!"SIMULATED".equals(calc.status())||
           !selected.profile().equals(calc.modelProfile())||
           !selected.resultKind().equals(calc.resultKind())||
           calc.rawIrr()==null||calc.roundedIrr()==null||calc.breakdown()==null||
           calc.breakdown().isEmpty()||calc.rawIrr().signum()<0||calc.roundedIrr().signum()<0||
           calc.rawIrr().compareTo(LIMIT)>0||calc.roundedIrr().compareTo(LIMIT)>0||
           scale(calc.roundedIrr())>0)
            return blocked(req,bankDate,"UNSAFE_LOAN_MODEL_SIMULATION");
        LocalDate effective=req.effectiveOn()==null?bankDate:req.effectiveOn();
        String hash;
        try{
            String fingerprint=Fee3R4CalculationContract.fingerprint(
                new Fee3R4CalculationContract.Request(req.draft(),req.inputs(),req.effectiveOn()),
                source,effective);
            StringBuilder data=new StringBuilder(CONTRACT).append('|').append(fingerprint)
                .append('|').append(calc.modelProfile()).append('|').append(calc.resultKind())
                .append('|').append(calc.rawIrr().toPlainString()).append('|')
                .append(calc.roundedIrr().toPlainString());
            for(var row:calc.breakdown()){
                // Fixed insertion-order maps originate only from this class; no caller map is appended.
                row.forEach((key,val)->data.append('|').append(key).append('=').append(val));
            }
            hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(data.toString().getBytes(StandardCharsets.UTF_8)));
        }catch(NoSuchAlgorithmException ex){throw new IllegalStateException("SHA256_UNAVAILABLE",ex);}
        Spec spec=spec(req);
        return new Evidence(CONTRACT,"SIMULATED",false,false,false,false,
            effective.isAfter(bankDate)?"FUTURE_UNVERIFIED_UAT":"UNVERIFIED_UAT_DRAFT",
            spec.profile(),spec.resultKind(),source,bankDate,effective,
            "IRR",req.draft().version().rounding(),0,hash,calc,
            List.of("UNPUBLISHED_DRAFT_ONLY","CLIENT_CONTRACT_EVIDENCE_UNVERIFIED",
                "NOT_REALIZED_PROFIT_CERTIFICATION","NOT_AN_ACCOUNTING_ACCRUAL",
                "NOT_A_TAX_DETERMINATION","NO_ORACLE_WRITE","NO_ACCOUNTING_POSTING"));
    }
}
