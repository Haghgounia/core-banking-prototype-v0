package com.behsazan.corebanking.fee3.r4c3;

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
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/** R4-C3: Qard al-hasana fee UAT only. NEVER authorization to demand, debit or post.
 * Actual schedule, contract rate and previous charge history are NOT integrated.
 * No arbitrary Oracle or caller formula text is executed.
 */
public final class Fee3R4C3QardContract {
    private Fee3R4C3QardContract() {}
    public static final String CONTRACT="FEE3_QARD_FEE_R4C3_V1";
    private static final BigDecimal HUNDRED=new BigDecimal("100");
    private static final BigDecimal LIMIT=new BigDecimal("1000000000000000000");
    private static final MathContext MC=MathContext.DECIMAL128;
    private static final String FORMULA="TPL_E624746F262F6E";
    private static final Set<String> ROUNDINGS=Set.of("HALF_UP","HALF_EVEN","DOWN","UP","FLOOR","CEIL");
    public record Spec(String model,String qardChargeTiming,String yearStartBasis) {}
    public static final Map<String,Spec> SPECS=Map.of(
        "ON_ANNIVERSARY",new Spec("ON_ANNIVERSARY","ON_ANNIVERSARY","UNDECIDED"),
        "FIRST_INSTALLMENT_OF_PERIOD",new Spec("FIRST_INSTALLMENT_OF_PERIOD","FIRST_INSTALLMENT_OF_PERIOD","UNDECIDED"),
        "JALALI_YEAR_START",new Spec("JALALI_YEAR_START","START_OF_YEAR","JALALI_CALENDAR_YEAR"),
        "CONTRACT_YEAR_START",new Spec("CONTRACT_YEAR_START","START_OF_YEAR","LOAN_CONTRACT_YEAR")
    );
    public record Request(DraftRequest draft,Map<String,Object> inputs,LocalDate effectiveOn) {}
    public record EventDay(LocalDate isoDate,int jalaliYear,int jalaliMonth,int jalaliDay,
        int ordinal,int daysInYear,String calendarDatasetEvidence) {}
    public record Calculation(String status,String timingModel,String resultKind,
        BigDecimal yearFraction,BigDecimal rawIrr,BigDecimal roundedIrr,
        String candidateAssessmentKey,String duplicateCheckStatus,
        int eventJalaliYear,int eventJalaliMonth,int eventJalaliDay,int eventJalaliOrdinal,
        String eventCalendarDatasetEvidence,
        List<Map<String,Object>> breakdown,List<String> diagnostics) {}
    public record Evidence(String contractVersion,String status,boolean persisted,
        boolean financialPosting,boolean published,boolean approved,String simulationMode,
        String timingModel,Fee3R4CalculationContract.VersionReference versionReference,
        LocalDate oracleBusinessDate,LocalDate effectiveOn,String currency,
        String roundingCode,int roundingScale,String snapshotSha256,
        Calculation calculation,List<String> diagnostics) {}
    private static IllegalArgumentException bad(String code){return new IllegalArgumentException(code);}
    private static BigDecimal decimal(Object o){
        if(!(o instanceof Number||o instanceof String)||o instanceof Boolean)return null;
        try{return new BigDecimal(String.valueOf(o));}
        catch(NumberFormatException ex){return null;}
    }
    private static int scale(BigDecimal x){return Math.max(0,x.stripTrailingZeros().scale());}
    private static boolean money(Object o){
        BigDecimal x=decimal(o);
        return x!=null&&x.signum()>0&&x.compareTo(LIMIT)<=0&&scale(x)<=2;
    }
    private static boolean rate(Object o){
        BigDecimal x=decimal(o);
        return x!=null&&x.signum()>=0&&x.compareTo(HUNDRED)<=0&&scale(x)<=8;
    }
    private static boolean evidence(Object o){
        return o instanceof String s&&s.matches("[A-Za-z0-9_./:-]{3,100}");
    }
    private static RoundingMode rounding(String s){return switch(s){
        case "HALF_UP"->RoundingMode.HALF_UP;
        case "HALF_EVEN"->RoundingMode.HALF_EVEN;
        case "DOWN","FLOOR"->RoundingMode.FLOOR;
        case "UP","CEIL"->RoundingMode.CEILING;
        default->throw bad("ROUNDING_UNSUPPORTED");
    };}
    public static Spec spec(Request r){
        if(r==null||r.draft()==null||r.draft().version()==null||
           r.draft().version().uiContext()==null)return null;
        Object model=r.draft().version().uiContext().get("loanModelProfile");
        return model instanceof String s?SPECS.get(s):null;
    }
    private static LocalDate date(Object value,String code){
        if(!(value instanceof String text)||!text.matches("\\d{4}-\\d{2}-\\d{2}"))throw bad(code);
        try{return LocalDate.parse(text);}catch(RuntimeException ex){throw bad(code);}
    }
    public static String guard(Request req,LocalDate bankDate){
        if(bankDate==null)return "ORACLE_BANK_DATE_REQUIRED";
        if(req==null||req.draft()==null||req.draft().item()==null||
           req.draft().version()==null||req.draft().scope()==null)return "DRAFT_REQUIRED";
        var d=req.draft();var v=d.version();var i=d.item();var scope=d.scope();
        var s=spec(req);
        if(s==null)return "QARD_TIMING_MODEL_UNSUPPORTED";
        LocalDate effective=req.effectiveOn()==null?bankDate:req.effectiveOn();
        if(!effective.equals(bankDate))return "QARD_EVENT_MUST_MATCH_BANK_DAY";
        if(v.validFrom()==null||v.validFrom().isAfter(effective)||
            v.validTo()!=null&&v.validTo().isBefore(effective))return "OUTSIDE_DRAFT_VALIDITY";
        if(!"LOAN".equals(i.module())||!"QARD_FEE".equals(i.operation())||
           !"QARD_FEE".equals(i.amountKind())||!"CREDIT".equals(i.ownerDomain()))
            return "NOT_QARD_FEE";
        if(!"BANK_INTERNAL".equals(v.origin())||!"FORMULA".equals(v.method()))
            return "QARD_RATE_OR_FORMULA_SOURCE_UNSUPPORTED";
        if(!"OUT_OF_SCOPE".equals(v.taxMode())||v.financialPolicy()!=null&&
           !v.financialPolicy().isEmpty())return "QARD_TAX_OR_FINANCIAL_POLICY_UNREVIEWED";
        if(!"IRR".equals(v.feeCurrency())||!"IRR".equals(v.serviceCurrency())||
           !"IRR".equals(scope.currency()))return "IRR_ONLY";
        if(!ROUNDINGS.contains(v.rounding()))return "ROUNDING_UNSUPPORTED";
        if(!List.of("QARD_HASANA").equals(scope.contracts()))return "QARD_SCOPE_REQUIRED";
        Map<String,Object> context=v.uiContext();
        if(!List.of("QARD_HASANA").equals(context.get("loanContractTypes"))||
           context.get("loanCalculationEvent")!=null||
           Boolean.TRUE.equals(context.get("loanFormulaContextReviewRequired")))
            return "QARD_CONTRACT_CONTEXT_UNREVIEWED";
        if(!(context.get("loanPolicy") instanceof Map<?,?> policy)||
           !Boolean.TRUE.equals(policy.get("contractEvidenceRequired"))||
           !Boolean.TRUE.equals(policy.get("contractInputsRequired"))||
           !"JALALI_ACT_ACT".equals(policy.get("dayCountBasis"))||
           !"CHARGE_ALL_DAYS".equals(policy.get("holidayPolicy"))||
           policy.containsKey("businessCalendarRef")||
           !s.qardChargeTiming().equals(policy.get("qardChargeTiming"))||
           !s.yearStartBasis().equals(policy.get("yearStartBasis")))
            return "QARD_TIMING_POLICY_MISMATCH";
        if(d.rules()==null||d.rules().size()!=1||d.rules().get(0)==null)
            return "SINGLE_FORMULA_RULE_REQUIRED";
        var rule=d.rules().get(0);
        if(rule.order()!=1||!"FORMULA".equals(rule.method())||
           !"REQUEST_AMOUNT".equals(rule.base())||!"REQUEST".equals(rule.rateSource())||
           rule.rate()!=null||rule.fixed()!=null||rule.min()!=null||rule.max()!=null||
           rule.coverage()!=null||rule.collateral()!=null)return "QARD_RULE_UNSUPPORTED";
        if(!FORMULA.equals(rule.formulaCode()))return "QARD_FORMULA_NOT_CERTIFIED";
        if(rule.parameters()!=null&&!rule.parameters().isEmpty())
            return "CLIENT_FORMULA_PARAMETERS_FORBIDDEN";
        Map<String,Object> in=req.inputs();
        if(in==null)return "QARD_INPUTS_REQUIRED";
        Set<String> exact=Set.of("contractReference","contractEvidenceReference",
            "rateEvidenceReference","scheduleEvidenceReference","loanContractStartDate",
            "periodStart","periodEndExclusive","eventDate","amount","contractRate");
        Set<String> expected=s.model().equals("FIRST_INSTALLMENT_OF_PERIOD")?
            new java.util.HashSet<>(exact):exact;
        if(s.model().equals("FIRST_INSTALLMENT_OF_PERIOD"))expected.add("firstInstallmentDate");
        if(!in.keySet().equals(expected))return "QARD_UNREVIEWED_INPUT_FIELDS";
        if(!money(in.get("amount")))return "QARD_AMOUNT_NUMBER22_2";
        if(!rate(in.get("contractRate")))return "QARD_RATE_NUMBER18_8";
        if(!evidence(in.get("contractReference"))||!evidence(in.get("contractEvidenceReference"))||
           !evidence(in.get("rateEvidenceReference"))||!evidence(in.get("scheduleEvidenceReference")))
            return "QARD_CONTRACT_SCHEDULE_EVIDENCE_REQUIRED";
        LocalDate from,to,event,origin,first=null;
        try{
            from=date(in.get("periodStart"),"QARD_DATE_INVALID");
            to=date(in.get("periodEndExclusive"),"QARD_DATE_INVALID");
            event=date(in.get("eventDate"),"QARD_DATE_INVALID");
            origin=date(in.get("loanContractStartDate"),"QARD_DATE_INVALID");
            if(s.model().equals("FIRST_INSTALLMENT_OF_PERIOD"))
                first=date(in.get("firstInstallmentDate"),"QARD_DATE_INVALID");
        }catch(IllegalArgumentException ex){return ex.getMessage();}
        if(!to.isAfter(from)||ChronoUnit.DAYS.between(from,to)>731||
           origin.isAfter(from)||event.isBefore(from)||!event.isBefore(to))
            return "QARD_PERIOD_OR_CONTRACT_DATES_INVALID";
        if(!event.equals(bankDate))return "QARD_EVENT_NOT_AT_BANK_BUSINESS_DATE";
        if(s.model().equals("FIRST_INSTALLMENT_OF_PERIOD")){
            if(!first.equals(event))return "FIRST_INSTALLMENT_SCHEDULE_MISMATCH";
        }
        if(!s.model().equals("FIRST_INSTALLMENT_OF_PERIOD")&&!from.equals(event))
            return "QARD_EVENT_MUST_START_PERIOD";
        if(d.calcItemId()==null ^ d.tariffVersionId()==null)return "PARTIAL_ORACLE_REFERENCE";
        if(d.calcItemId()==null&&(d.expectedItemRecordVersion()!=null||
            d.expectedVersionRecordVersion()!=null))return "UNSAVED_ROW_VERSION_FORBIDDEN";
        if(d.calcItemId()!=null&&(d.calcItemId()<=0||d.tariffVersionId()<=0||
            d.expectedItemRecordVersion()==null||d.expectedItemRecordVersion()<=0||
            d.expectedVersionRecordVersion()==null||d.expectedVersionRecordVersion()<=0))
            return "ORACLE_REFERENCE_INCOMPLETE";
        return null;
    }
    private static void checkDay(EventDay day,LocalDate expected){
        if(day==null||!expected.equals(day.isoDate())||day.jalaliYear()<1200||
           day.jalaliYear()>1700||day.jalaliMonth()<1||day.jalaliMonth()>12||
           day.jalaliDay()<1||day.jalaliDay()>31||
           day.daysInYear()!=365&&day.daysInYear()!=366||
           day.ordinal()<1||day.ordinal()>day.daysInYear()||
           day.calendarDatasetEvidence()==null||
           !day.calendarDatasetEvidence().matches("CAL2:PERSIAN:[1-9][0-9]*:[1-9][0-9]*"))
            throw bad("QARD_CAL2_EVENT_MAPPING_UNTRUSTED");
    }
    private static String sha(String value){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException ex){throw new IllegalStateException("SHA256_UNAVAILABLE",ex);}
    }
    public static Calculation calculate(Request req,
        List<Fee3R4C1LoanProfitContract.YearSlice> slices,
        EventDay originDay,EventDay eventDay){
        Spec spec=spec(req);
        if(spec==null)throw bad("QARD_TIMING_MODEL_UNSUPPORTED");
        Map<String,Object> in=req.inputs();
        LocalDate from=date(in.get("periodStart"),"QARD_DATE_INVALID");
        LocalDate to=date(in.get("periodEndExclusive"),"QARD_DATE_INVALID");
        LocalDate event=date(in.get("eventDate"),"QARD_DATE_INVALID");
        LocalDate origin=date(in.get("loanContractStartDate"),"QARD_DATE_INVALID");
        checkDay(eventDay,event);checkDay(originDay,origin);
        // Event mapping must use the same active Persian CAL2 variant as the period.
        String[] eventTokens=eventDay.calendarDatasetEvidence().split(":");
        String[] originTokens=originDay.calendarDatasetEvidence().split(":");
        if(!eventTokens[2].equals(originTokens[2]))throw bad("QARD_CAL2_VARIANT_CHANGED");
        if(eventDay.jalaliYear()<originDay.jalaliYear())throw bad("QARD_EVENT_BEFORE_ORIGINATION");
        String eventEvidence=eventDay.calendarDatasetEvidence();
        boolean covered=slices!=null&&slices.stream().anyMatch(row->
            row.jalaliYear()==eventDay.jalaliYear() &&
            row.calendarDatasetEvidence().equals(eventEvidence)&&
            !event.isBefore(row.fromInclusive())&&event.isBefore(row.toExclusive()));
        if(!covered)throw bad("QARD_EVENT_DAY_NOT_COVERED_BY_CAL2_PERIOD");
        switch(spec.model()){
            case "ON_ANNIVERSARY" -> {
                if(eventDay.jalaliYear()<=originDay.jalaliYear()||
                   eventDay.jalaliMonth()!=originDay.jalaliMonth()||
                   eventDay.jalaliDay()!=originDay.jalaliDay())
                    throw bad("QARD_ANNIVERSARY_NOT_CAL2_VERIFIED");
            }
            case "FIRST_INSTALLMENT_OF_PERIOD" -> {
                // Only validates caller-asserted date; actual loan schedule is not connected.
                if(!event.equals(date(in.get("firstInstallmentDate"),"QARD_DATE_INVALID")))
                    throw bad("QARD_FIRST_INSTALLMENT_DATE_MISMATCH");
            }
            case "JALALI_YEAR_START" -> {
                if(eventDay.ordinal()!=1||eventDay.jalaliMonth()!=1||eventDay.jalaliDay()!=1)
                    throw bad("QARD_NOT_JALALI_NEW_YEAR");
            }
            case "CONTRACT_YEAR_START" -> {
                if(eventDay.jalaliMonth()!=originDay.jalaliMonth()||
                   eventDay.jalaliDay()!=originDay.jalaliDay())
                    throw bad("QARD_NOT_START_OF_CONTRACT_YEAR");
            }
            default -> throw bad("QARD_TIMING_MODEL_UNSUPPORTED");
        }
        BigDecimal factor=Fee3R4C1LoanProfitContract.yearFactor(from,to,slices);
        BigDecimal raw=decimal(in.get("amount")).multiply(decimal(in.get("contractRate")),MC)
            .divide(HUNDRED,MC).multiply(factor,MC);
        if(raw.signum()<0||raw.compareTo(LIMIT)>0)throw bad("QARD_RESULT_UNSAFE");
        BigDecimal rounded=raw.setScale(0,rounding(req.draft().version().rounding()));
        List<Map<String,Object>> trace=new ArrayList<>();
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
        // Deliberately stable across amended amount/rate: an identity candidate, not an authoritative check.
        String key=sha(CONTRACT+"|"+in.get("contractReference")+"|"+spec.model()+"|"+
            from+"|"+to+"|"+event);
        return new Calculation("SIMULATED",spec.model(),"QARD_SERVICE_FEE_NOT_DEMANDED",
            factor,raw,rounded,key,"NOT_VERIFIED_NO_AUTHORITATIVE_CHARGE_HISTORY",
            eventDay.jalaliYear(),eventDay.jalaliMonth(),eventDay.jalaliDay(),
            eventDay.ordinal(),eventDay.calendarDatasetEvidence(),
            List.copyOf(trace),List.of("SCHEDULE_EVIDENCE_CLIENT_ASSERTED_ONLY",
                "RATE_EVIDENCE_CLIENT_ASSERTED_ONLY","NO_DUPLICATE_CERTIFICATION",
                "NOT_A_CHARGE_AUTHORIZATION","NO_ORACLE_WRITE","NO_POSTING"));
    }
    public static Evidence blocked(Request req,LocalDate day,String code){
        var v=req==null||req.draft()==null?null:req.draft().version();
        var s=spec(req);
        return new Evidence(CONTRACT,"BLOCKED",false,false,false,false,"UNVERIFIED_UAT_DRAFT",
            s==null?null:s.model(),null,day,req==null||req.effectiveOn()==null?day:req.effectiveOn(),
            "IRR",v==null?null:v.rounding(),0,null,null,List.of(code));
    }
    public static Evidence finish(Request req,LocalDate day,
        Fee3R4CalculationContract.VersionReference reference,Calculation calc){
        if(reference==null||reference.active()||
           !Set.of("UNSAVED","DRAFT").contains(reference.status())||
           calc==null||!"SIMULATED".equals(calc.status())||
           calc.rawIrr()==null||calc.roundedIrr()==null||
           calc.breakdown()==null||calc.breakdown().isEmpty()||
           calc.rawIrr().signum()<0||calc.roundedIrr().signum()<0||
           calc.rawIrr().compareTo(LIMIT)>0||calc.roundedIrr().compareTo(LIMIT)>0||
           calc.roundedIrr().scale()!=0||
           !"NOT_VERIFIED_NO_AUTHORITATIVE_CHARGE_HISTORY".equals(calc.duplicateCheckStatus()))
            return blocked(req,day,"QARD_UNSAFE_SIMULATION");
        LocalDate effective=req.effectiveOn()==null?day:req.effectiveOn();
        String fingerprint=Fee3R4CalculationContract.fingerprint(
            new Fee3R4CalculationContract.Request(req.draft(),req.inputs(),req.effectiveOn()),
            reference,effective);
        StringBuilder text=new StringBuilder(CONTRACT).append('|').append(fingerprint)
            .append('|').append(calc.timingModel()).append('|').append(calc.yearFraction().toPlainString())
            .append('|').append(calc.rawIrr().toPlainString()).append('|')
            .append(calc.roundedIrr().toPlainString()).append('|')
            .append(calc.candidateAssessmentKey()).append('|')
            .append(calc.eventJalaliYear()).append('/').append(calc.eventJalaliMonth())
            .append('/').append(calc.eventJalaliDay()).append('|')
            .append(calc.eventCalendarDatasetEvidence());
        for(var line:calc.breakdown())line.forEach((key,value)->text.append('|').append(key)
            .append('=').append(value));
        String hash=sha(text.toString());
        return new Evidence(CONTRACT,"SIMULATED",false,false,false,false,"UNVERIFIED_UAT_DRAFT",
            calc.timingModel(),reference,day,effective,"IRR",req.draft().version().rounding(),0,
            hash,calc,List.of("UNPUBLISHED_DRAFT_ONLY","CONTRACT_RATE_UNVERIFIED",
                "LOAN_SCHEDULE_UNVERIFIED","PREVIOUS_CHARGE_HISTORY_NOT_CHECKED",
                "NO_DUPLICATE_CERTIFICATION","NOT_A_CHARGE_AUTHORIZATION",
                "NOT_A_TAX_DETERMINATION","NO_ORACLE_WRITE","NO_ACCOUNTING_POSTING"));
    }
}
