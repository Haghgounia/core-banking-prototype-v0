package com.behsazan.corebanking.fee3.r4c1;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.r3.Fee3PreviewEngine;
import com.behsazan.corebanking.fee3.r4.Fee3R4CalculationContract;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** R4-C1: no-write, client-evidence UNVERIFIED loan-profit draft simulation only.
 * Formula text is NEVER interpreted, and year fractions are NEVER accepted from the caller.
 */
public final class Fee3R4C1LoanProfitContract {
    private Fee3R4C1LoanProfitContract() {}
    public static final String CONTRACT="FEE3_LOAN_PROFIT_R4C1_V1";
    private static final BigDecimal LIMIT=new BigDecimal("1000000000000000000");
    private static final MathContext MC=MathContext.DECIMAL128;
    private static final BigDecimal HUNDRED=new BigDecimal("100");
    private static final String JALALI="JALALI_ACT_ACT", MONTHS="CONTRACT_MONTHS";
    private static final String DAILY_FORMULA="TPL_E624746F262F6E";
    private static final String MONTHLY_FORMULA="TPL_7F2FE580EDB351";
    private static final Set<String> CONTRACT_TYPES=Set.of("MURABAHA","INSTALLMENT_SALE","IJARA_TAMLIK","ISTISNA");
    private static final Set<String> ROUNDINGS=Set.of("HALF_UP","HALF_EVEN","DOWN","UP","FLOOR","CEIL");
    public record Request(DraftRequest draft, Map<String,Object> inputs, LocalDate effectiveOn) {}
    /** A compact certified-by-source calendar mapping, NEVER submitted as an HTTP input. */
    public record YearSlice(int jalaliYear, LocalDate fromInclusive, LocalDate toExclusive,
                            int days, int daysInYear, String calendarDatasetEvidence) {}
    public record Evidence(String contractVersion,String status,boolean persisted,boolean financialPosting,
        boolean published,boolean approved,String simulationMode,
        Fee3R4CalculationContract.VersionReference versionReference,
        LocalDate oracleBusinessDate,LocalDate effectiveOn,String currency,String roundingCode,
        int roundingScale,String snapshotSha256,Fee3PreviewEngine.PreviewResult calculation,
        List<String> diagnostics) {}

    private static IllegalArgumentException bad(String code){return new IllegalArgumentException(code);}
    private static BigDecimal number(Object val){
        if(!(val instanceof Number||val instanceof String))return null;
        try {return new BigDecimal(String.valueOf(val));}
        catch(NumberFormatException ex){return null;}
    }
    private static int scale(BigDecimal val){return Math.max(0,val.stripTrailingZeros().scale());}
    private static boolean valid(BigDecimal x,boolean nonZero,int maxScale,BigDecimal max){
        return x!=null && (nonZero?x.signum()>0:x.signum()>=0) && x.compareTo(max)<=0 && scale(x)<=maxScale;
    }
    private static String string(Object value){return value instanceof String s?s:null;}
    private static boolean meaningful(Object value){
        return value instanceof String s && !s.isBlank() && s.length()<=100 && s.matches("[A-Za-z0-9_./:-]{3,100}");
    }
    private static String basis(Request req){
        Object p=req.draft().version().uiContext().get("loanPolicy");
        if(!(p instanceof Map<?,?> policy))return null;
        return string(policy.get("dayCountBasis"));
    }
    private static RoundingMode rounding(String value){return switch(value){
        case "HALF_UP" -> RoundingMode.HALF_UP;
        case "HALF_EVEN" -> RoundingMode.HALF_EVEN;
        case "DOWN","FLOOR" -> RoundingMode.FLOOR;
        case "UP","CEIL" -> RoundingMode.CEILING;
        default -> throw bad("UNSUPPORTED_ROUNDING");
    };}
    public static String guard(Request request,LocalDate bankDate){
        if(bankDate==null)return "ORACLE_BANK_DATE_REQUIRED";
        if(request==null||request.draft()==null||request.draft().item()==null||
           request.draft().version()==null||request.draft().scope()==null)return "DRAFT_REQUIRED";
        DraftRequest d=request.draft(); ItemDraft i=d.item(); VersionDraft v=d.version();
        LocalDate effective=request.effectiveOn()==null?bankDate:request.effectiveOn();
        if(effective.isBefore(bankDate))return "HISTORICAL_EFFECTIVE_DATE_UNSUPPORTED";
        if(v.validFrom()==null||v.validFrom().isAfter(effective)||
           v.validTo()!=null&&v.validTo().isBefore(effective))return "OUTSIDE_DRAFT_VALIDITY";
        if(!"LOAN".equals(i.module())||!"LOAN_PROFIT".equals(i.operation())||
           !"FINANCING_PROFIT".equals(i.amountKind()))return "NOT_FINANCING_PROFIT";
        if(!"CONTRACT_RATE_REGULATED".equals(v.origin()))return "CONTRACT_RATE_ORIGIN_REQUIRED";
        if(!"FORMULA".equals(v.method()))return "UNREVIEWED_PROFIT_METHOD";
        if(!"OUT_OF_SCOPE".equals(v.taxMode()))return "TAX_TREATMENT_NOT_REVIEWED_FOR_PROFIT";
        if(!"IRR".equals(v.serviceCurrency())||!"IRR".equals(v.feeCurrency())||
           !"IRR".equals(d.scope().currency()))return "IRR_ONLY";
        if(!ROUNDINGS.contains(v.rounding()))return "UNSUPPORTED_ROUNDING";
        if(v.financialPolicy()!=null&&v.financialPolicy().containsKey("taxPercent"))return "UNREVIEWED_TAX";
        if(v.uiContext()==null)return "LOAN_CONTEXT_REQUIRED";
        if(!"EXCHANGE_ACCRUAL".equals(v.uiContext().get("loanModelProfile"))||
           !"PERIOD_PROFIT".equals(v.uiContext().get("loanCalculationEvent")))
            return "NOT_EXCHANGE_PERIOD_PROFIT";
        if(Boolean.TRUE.equals(v.uiContext().get("loanFormulaContextReviewRequired")))
            return "LOAN_MODEL_REVIEW_PENDING";
        Object types=v.uiContext().get("loanContractTypes");
        if(!(types instanceof List<?> list)||list.size()!=1||!(list.get(0) instanceof String type)||
           !CONTRACT_TYPES.contains(type))return "CONTRACT_TYPE_UNREVIEWED";
        if(!(v.uiContext().get("loanPolicy") instanceof Map<?,?> policy)||
           !Boolean.TRUE.equals(policy.get("contractEvidenceRequired"))||
           !"CHARGE_ALL_DAYS".equals(policy.get("holidayPolicy"))||
           policy.containsKey("businessCalendarRef"))return "LOAN_POLICY_UNREVIEWED";
        String dayCount=basis(request);
        if(!Set.of(JALALI,MONTHS).contains(dayCount))return "UNSUPPORTED_DAY_COUNT";
        if("INSTALLMENT_SALE".equals(list.get(0))!=MONTHS.equals(dayCount))
            return "CONTRACT_TYPE_DAY_COUNT_MISMATCH";
        if(d.rules()==null||d.rules().size()!=1||d.rules().get(0)==null)return "SINGLE_RULE_REQUIRED";
        RuleDraft r=d.rules().get(0);
        if(r.order()!=1||!"FORMULA".equals(r.method())||!"REQUEST_AMOUNT".equals(r.base())||
           !"REQUEST".equals(r.rateSource())||r.rate()!=null||r.fixed()!=null||
           r.min()!=null||r.max()!=null||r.coverage()!=null||r.collateral()!=null)
            return "UNSUPPORTED_PROFIT_RULE";
        if(!((JALALI.equals(dayCount)&&DAILY_FORMULA.equals(r.formulaCode()))||
             (MONTHS.equals(dayCount)&&MONTHLY_FORMULA.equals(r.formulaCode()))))
            return "FORMULA_TEMPLATE_NOT_CERTIFIED";
        if(r.parameters()!=null&&(r.parameters().containsKey("_fceEditableExpressionV1")||
           r.parameters().containsKey("yearFraction")||r.parameters().containsKey("contractMonths")))
            return "CLIENT_FORMULA_OVERRIDE_FORBIDDEN";
        Map<String,Object> input=request.inputs();
        if(input==null)return "LOAN_INPUTS_REQUIRED";
        Set<String> keys=new HashSet<>(Set.of("amount","contractRate","contractReference","rateEvidenceReference"));
        if(JALALI.equals(dayCount)){keys.add("periodStart");keys.add("periodEndExclusive");}
        else keys.add("contractMonths");
        if(!input.keySet().equals(keys))return "UNREVIEWED_INPUT_FIELDS";
        if(!valid(number(input.get("amount")),true,2,LIMIT))return "PRINCIPAL_EXCEEDS_NUMBER_22_2";
        if(!valid(number(input.get("contractRate")),false,8,HUNDRED))return "REQUEST_RATE_NOT_NUMBER_18_8";
        if(!meaningful(input.get("contractReference"))||!meaningful(input.get("rateEvidenceReference")))
            return "CONTRACT_EVIDENCE_REFERENCE_REQUIRED";
        if(JALALI.equals(dayCount)){
            try {
                LocalDate start=LocalDate.parse(string(input.get("periodStart")));
                LocalDate end=LocalDate.parse(string(input.get("periodEndExclusive")));
                if(!end.isAfter(start)||ChronoUnit.DAYS.between(start,end)>3653||
                    (start.isBefore(v.validFrom())||v.validTo()!=null&&end.isAfter(v.validTo().plusDays(1))))
                    return "INVALID_JALALI_PERIOD";
            }catch(RuntimeException exception){return "INVALID_ISO_PERIOD";}
        }else{
            BigDecimal months=number(input.get("contractMonths"));
            if(months==null||months.scale()>0||months.signum()<=0||months.compareTo(new BigDecimal("600"))>0)
                return "CONTRACT_MONTHS_NOT_VALIDATED";
        }
        if(d.calcItemId()==null ^ d.tariffVersionId()==null)return "PARTIAL_ORACLE_ID";
        if(d.calcItemId()==null&&(d.expectedItemRecordVersion()!=null||
            d.expectedVersionRecordVersion()!=null))return "UNSAVED_RECORD_VERSION_FORBIDDEN";
        if(d.calcItemId()!=null&&(d.expectedItemRecordVersion()==null||
           d.expectedVersionRecordVersion()==null||d.expectedItemRecordVersion()<=0||
           d.expectedVersionRecordVersion()<=0))return "MISSING_ORACLE_RECORD_VERSION";
        return null;
    }
    /** Validate complete coverage and CAL2-derived year denominators before doing any arithmetic. */
    public static BigDecimal yearFactor(LocalDate from,LocalDate to,List<YearSlice> slices){
        if(from==null||to==null||!to.isAfter(from)||slices==null||slices.isEmpty())
            throw bad("JALALI_CALENDAR_COVERAGE_MISSING");
        LocalDate current=from;
        BigDecimal fraction=BigDecimal.ZERO;
        int previousYear=Integer.MIN_VALUE;
        for(YearSlice s:slices){
            if(s==null||s.jalaliYear()<=previousYear||s.fromInclusive()==null||s.toExclusive()==null||
               !s.fromInclusive().equals(current)||!s.toExclusive().isAfter(current)||
               s.days()!=ChronoUnit.DAYS.between(s.fromInclusive(),s.toExclusive())||
               s.daysInYear()!=365&&s.daysInYear()!=366||s.days()>s.daysInYear()||
               s.calendarDatasetEvidence()==null||s.calendarDatasetEvidence().isBlank())
                throw bad("INCOMPLETE_JALALI_YEAR_SEGMENT");
            fraction=fraction.add(BigDecimal.valueOf(s.days()).divide(
                BigDecimal.valueOf(s.daysInYear()),MC),MC);
            current=s.toExclusive();previousYear=s.jalaliYear();
        }
        if(!current.equals(to))throw bad("JALALI_CALENDAR_COVERAGE_MISSING");
        return fraction;
    }
    public static Fee3PreviewEngine.PreviewResult calculate(Request req,List<YearSlice> slices){
        String dc=basis(req);
        BigDecimal principal=number(req.inputs().get("amount"));
        BigDecimal rate=number(req.inputs().get("contractRate"));
        BigDecimal factor;
        List<Map<String,Object>> trace=new ArrayList<>();
        if(JALALI.equals(dc)){
            LocalDate start=LocalDate.parse((String)req.inputs().get("periodStart"));
            LocalDate end=LocalDate.parse((String)req.inputs().get("periodEndExclusive"));
            factor=yearFactor(start,end,slices);
            for(YearSlice slice:slices){
                Map<String,Object> row=new LinkedHashMap<>();
                row.put("jalaliYear",slice.jalaliYear());row.put("fromInclusive",slice.fromInclusive());
                row.put("toExclusive",slice.toExclusive());row.put("days",slice.days());
                row.put("yearDenominator",slice.daysInYear());
                row.put("calendarDatasetEvidence",slice.calendarDatasetEvidence());
                row.put("yearFraction",BigDecimal.valueOf(slice.days()).divide(
                    BigDecimal.valueOf(slice.daysInYear()),MC));
                trace.add(row);
            }
        }else if(MONTHS.equals(dc)){
            if(slices!=null&&!slices.isEmpty())throw bad("CALENDAR_SEGMENTS_NOT_FOR_MONTHS");
            BigDecimal months=number(req.inputs().get("contractMonths"));
            factor=months.divide(new BigDecimal("12"),MC);
            trace.add(Map.of("contractMonths",months,"yearFraction",factor,
                "source","UNVERIFIED_CLIENT_CONTRACT_MONTHS"));
        }else throw bad("UNSUPPORTED_DAY_COUNT");
        BigDecimal raw=principal.multiply(rate,MC).divide(HUNDRED,MC).multiply(factor,MC);
        if(raw.signum()<0||raw.compareTo(LIMIT)>0)throw bad("PROFIT_EXCEEDS_SAFE_LIMIT");
        BigDecimal profit=raw.setScale(0,rounding(req.draft().version().rounding()));
        return new Fee3PreviewEngine.PreviewResult("SIMULATED",false,false,"FORMULA",raw,
            profit,BigDecimal.ZERO,profit,List.copyOf(trace),List.of(
                "UNVERIFIED_CLIENT_CONTRACT_RATE","LOAN_PROFIT_NOT_SERVICE_FEE",
                "JALALI_YEAR_DENOMINATORS_FROM_CAL2_WHEN_USED","NO_POSTING"));
    }
    public static Evidence blocked(Request req,LocalDate bankDay,String reason){
        var version=req==null||req.draft()==null?null:req.draft().version();
        LocalDate effective=req==null||req.effectiveOn()==null?bankDay:req.effectiveOn();
        return new Evidence(CONTRACT,"BLOCKED",false,false,false,false,"UNVERIFIED_UAT_DRAFT",null,
            bankDay,effective,"IRR",version==null?null:version.rounding(),0,null,null,List.of(reason));
    }
    public static Evidence finish(Request req,LocalDate bankDay,
        Fee3R4CalculationContract.VersionReference ref,Fee3PreviewEngine.PreviewResult calculated){
        if(ref==null||ref.active()||!(Set.of("DRAFT","UNSAVED").contains(ref.status()))||
           calculated==null||!"SIMULATED".equals(calculated.status())||
           calculated.persisted()||calculated.financialPosting()||calculated.feeIrr()==null||
           scale(calculated.feeIrr())!=0)return blocked(req,bankDay,"UNSAFE_PROFIT_SIMULATION");
        LocalDate effective=req.effectiveOn()==null?bankDay:req.effectiveOn();
        String hash;
        try{
            var fp=Fee3R4CalculationContract.fingerprint(new Fee3R4CalculationContract.Request(
                req.draft(),req.inputs(),req.effectiveOn()),ref,effective);
            StringBuilder canonical=new StringBuilder(CONTRACT).append('|').append(fp).append('|')
                .append(calculated.rawIrr().toPlainString()).append('|')
                .append(calculated.feeIrr().toPlainString());
            for(var line:calculated.breakdown())canonical.append('|').append(line.toString());
            hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                canonical.toString().getBytes(StandardCharsets.UTF_8)));
        }catch(NoSuchAlgorithmException ex){throw new IllegalStateException("SHA256_MISSING",ex);}
        return new Evidence(CONTRACT,"SIMULATED",false,false,false,false,
            effective.isAfter(bankDay)?"FUTURE_UAT_SIMULATION":"UNVERIFIED_UAT_DRAFT",ref,
            bankDay,effective,"IRR",req.draft().version().rounding(),0,hash,calculated,
            List.of("UNPUBLISHED_DRAFT_ONLY","CLIENT_CONTRACT_RATE_UNVERIFIED",
                "NOT_A_PROFIT_ACCRUAL_AUTHORIZATION","NO_ORACLE_WRITE",
                "NO_ACCOUNTING_POSTING","NOT_A_TAX_DETERMINATION"));
    }
}
