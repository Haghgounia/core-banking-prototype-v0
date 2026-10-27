import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.r4.Fee3R4CalculationContract;
import com.behsazan.corebanking.fee3.r4c1.Fee3R4C1LoanProfitContract;
import com.behsazan.corebanking.fee3.r4c2.Fee3R4C2LoanModelContract;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Standalone actual-Java arithmetic/guard suite, NO Oracle or Spring. */
public class Fee3R4C2Probe {
    private static int pass,fail;
    static final LocalDate TODAY=LocalDate.of(2026,10,27);
    private static void check(String title,boolean okay){
        if(okay){pass++;System.out.println("PASS | "+title);}
        else {fail++;System.out.println("FAIL | "+title);}
    }
    private static Map<String,Object> context(String profile,String contract){
        String event=switch(profile){
            case "PARTICIPATION_REALIZED"->"REALIZED_PROFIT";
            case "PARTICIPATION_ESTIMATE","SALAM_ESTIMATE"->"ESTIMATED_PROFIT";
            case "SALAM_SPREAD"->"SALAM_SETTLEMENT";
            case "DEBT_PURCHASE_SPREAD"->"PURCHASE_SPREAD";
            default -> throw new IllegalArgumentException();
        };
        Map<String,Object> policy=new LinkedHashMap<>();
        policy.put("dayCountBasis",profile.endsWith("ESTIMATE")?"JALALI_ACT_ACT":"CONTRACT_MONTHS");
        policy.put("holidayPolicy","CHARGE_ALL_DAYS");
        policy.put("contractEvidenceRequired",true);
        Map<String,Object> ctx=new LinkedHashMap<>();
        ctx.put("loanModelProfile",profile);
        ctx.put("loanCalculationEvent",event);
        ctx.put("loanContractTypes",List.of(contract));
        ctx.put("loanPolicy",policy);
        return ctx;
    }
    static Fee3R4C2LoanModelContract.Request fixture(String profile){
        String contract=switch(profile){
            case "PARTICIPATION_REALIZED","PARTICIPATION_ESTIMATE"->"MUSHARAKA_MADANI";
            case "DEBT_PURCHASE_SPREAD"->"DEBT_PURCHASE";
            case "SALAM_SPREAD","SALAM_ESTIMATE"->"SALAM";
            default ->throw new IllegalArgumentException();
        };
        String formula=switch(profile){
            case "PARTICIPATION_REALIZED"->"PARTICIPATION_REALIZED";
            case "DEBT_PURCHASE_SPREAD"->"DEBT_PURCHASE_SPREAD";
            case "SALAM_SPREAD"->"SALAM_PRICE_SPREAD";
            default ->"TPL_E624746F262F6E";
        };
        Map<String,Object> input=new LinkedHashMap<>();
        input.put("contractReference","UAT-LOAN-CONTRACT-UNVERIFIED");
        input.put("calculationEvidenceReference","UAT-EVENT-PROOF-UNVERIFIED");
        if(profile.endsWith("ESTIMATE")){
            input.put("amount","100000000.00");
            input.put("contractRate","18.00000000");
            input.put("rateEvidenceReference","UAT-RATE-UNVERIFIED");
            input.put("periodStart",TODAY.toString());
            input.put("periodEndExclusive",TODAY.plusDays(10).toString());
            input.put("estimateBasisReference","UAT-ESTIMATE-BASE-UNVERIFIED");
        }else{
            input.put("eventDate",TODAY.toString());
            switch(profile){
                case "PARTICIPATION_REALIZED"->{
                    input.put("realizedProfit","90000000.00");
                    input.put("sharePercent","40.00000000");
                    input.put("realizationStatementReference","UAT-REALIZATION-UNVERIFIED");
                    input.put("shareAgreementReference","UAT-SHARE-UNVERIFIED");
                }
                case "DEBT_PURCHASE_SPREAD"->{
                    input.put("faceAmount","100000000.00");input.put("purchasePrice","92000000.00");
                    input.put("faceAmountEvidenceReference","UAT-FACE-UNVERIFIED");
                    input.put("purchasePriceEvidenceReference","UAT-PURCHASE-UNVERIFIED");
                }
                case "SALAM_SPREAD"->{
                    input.put("saleAmount","105000000.00");input.put("purchaseAmount","90000000.00");
                    input.put("saleEvidenceReference","UAT-SALE-UNVERIFIED");
                    input.put("purchaseEvidenceReference","UAT-PURCHASE-UNVERIFIED");
                }
            }
        }
        VersionDraft v=new VersionDraft(TODAY,null,"CONTRACT_RATE_REGULATED",null,
            "FORMULA","OUT_OF_SCOPE","HALF_UP","UAT no write",Map.of(),"IRR","IRR",
            context(profile,contract));
        RuleDraft rule=new RuleDraft(1,"FORMULA","REQUEST_AMOUNT","REQUEST",null,null,
            null,null,null,null,formula,Map.of());
        DraftRequest d=new DraftRequest(null,null,null,null,
            new ItemDraft("R4-C2 UAT loan model", "LOAN","LOAN_PROFIT","FINANCING_PROFIT",
                null,"LOAN",null),v,
            new ScopeDraft(List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of("ALL"),
                List.of(contract),"IRR"),List.of(rule),
            List.of(new TestDraft("R4-C2 offline check",Map.of("amount","100000000"),null)));
        return new Fee3R4C2LoanModelContract.Request(d,input,null);
    }
    static Fee3R4C2LoanModelContract.Request inputs(Fee3R4C2LoanModelContract.Request r,
        String name,Object value){
        Map<String,Object> map=new LinkedHashMap<>(r.inputs());
        if(value==null)map.remove(name);else map.put(name,value);
        return new Fee3R4C2LoanModelContract.Request(r.draft(),map,r.effectiveOn());
    }
    static Fee3R4C2LoanModelContract.Request version(Fee3R4C2LoanModelContract.Request r,
        String profile,String contract,String formula,String tax,String rounding){
        var d=r.draft();var v=d.version();
        var n=new VersionDraft(v.validFrom(),v.validTo(),v.origin(),v.sourceId(),v.method(),
            tax,rounding,v.changeReason(),v.financialPolicy(),v.serviceCurrency(),v.feeCurrency(),
            context(profile,contract));
        var rule=d.rules().get(0);
        var changedRule=new RuleDraft(rule.order(),rule.method(),rule.base(),rule.rateSource(),
            rule.rate(),rule.fixed(),rule.collateral(),rule.coverage(),rule.min(),rule.max(),formula,
            rule.parameters());
        var draft=new DraftRequest(d.calcItemId(),d.tariffVersionId(),d.expectedItemRecordVersion(),
            d.expectedVersionRecordVersion(),d.item(),n,
            new ScopeDraft(d.scope().channels(),d.scope().customers(),d.scope().groups(),
                d.scope().products(),List.of(contract),d.scope().currency()),
            List.of(changedRule),d.tests());
        return new Fee3R4C2LoanModelContract.Request(draft,r.inputs(),r.effectiveOn());
    }
    static Fee3R4C2LoanModelContract.Request rule(Fee3R4C2LoanModelContract.Request r,
        String formula,BigDecimal fixed,Map<String,Object> params){
        var d=r.draft();var old=d.rules().get(0);
        var q=new RuleDraft(1,"FORMULA","REQUEST_AMOUNT","REQUEST",null,fixed,null,null,
            null,null,formula,params);
        var n=new DraftRequest(d.calcItemId(),d.tariffVersionId(),d.expectedItemRecordVersion(),
            d.expectedVersionRecordVersion(),d.item(),d.version(),d.scope(),List.of(q),d.tests());
        return new Fee3R4C2LoanModelContract.Request(n,r.inputs(),r.effectiveOn());
    }
    static List<Fee3R4C1LoanProfitContract.YearSlice> days(){
        return List.of(new Fee3R4C1LoanProfitContract.YearSlice(1405,TODAY,TODAY.plusDays(10),
            10,365,"CAL2:PERSIAN:UAT"));
    }
    private static void negative(String title,Fee3R4C2LoanModelContract.Request r,String expected){
        check(title,expected.equals(Fee3R4C2LoanModelContract.guard(r,TODAY)));
    }
    public static void main(String[] args){
        String[] profiles={"PARTICIPATION_REALIZED","PARTICIPATION_ESTIMATE",
            "DEBT_PURCHASE_SPREAD","SALAM_SPREAD","SALAM_ESTIMATE"};
        String[] totals={"36000000","493151","8000000","15000000","493151"};
        var ref=new Fee3R4CalculationContract.VersionReference("UNSAVED",null,null,null,
            null,"UNSAVED",false,0);
        for(int k=0;k<profiles.length;k++){
            String p=profiles[k];var r=fixture(p);
            check(p+" guard",Fee3R4C2LoanModelContract.guard(r,TODAY)==null);
            var calc=Fee3R4C2LoanModelContract.calculate(r,p.endsWith("ESTIMATE")?days():List.of());
            check(p+" exact total",calc.roundedIrr().compareTo(new BigDecimal(totals[k]))==0);
            check(p+" trace",calc.breakdown().size() == 1);
            check(p+" no approval",calc.diagnostics().contains("NO_PROFIT_RECOGNITION"));
            var evidence=Fee3R4C2LoanModelContract.finish(r,TODAY,ref,calc);
            check(p+" finish",evidence.status().equals("SIMULATED") &&
                !evidence.persisted()&&!evidence.approved()&&!evidence.published()&&
                !evidence.financialPosting()&&evidence.calculation()!=null);
            check(p+" sha256",evidence.snapshotSha256().matches("[0-9a-f]{64}"));
            check(p+" deterministic",evidence.snapshotSha256().equals(
                Fee3R4C2LoanModelContract.finish(r,TODAY,ref,calc).snapshotSha256()));
            check(p+" flags",evidence.diagnostics().contains("CLIENT_CONTRACT_EVIDENCE_UNVERIFIED"));
        }
        var realized=fixture("PARTICIPATION_REALIZED");
        var estimate=fixture("PARTICIPATION_ESTIMATE");
        var debt=fixture("DEBT_PURCHASE_SPREAD");
        var salam=fixture("SALAM_SPREAD");
        var salamEst=fixture("SALAM_ESTIMATE");
        negative("share above hundred",inputs(realized,"sharePercent","100.00000001"),
            "REALIZED_PROFIT_OR_SHARE_INVALID");
        negative("share greater than eight decimals",inputs(realized,"sharePercent","10.123456789"),
            "REALIZED_PROFIT_OR_SHARE_INVALID");
        negative("negative realized profit/loss separately",inputs(realized,"realizedProfit","-10"),
            "REALIZED_PROFIT_OR_SHARE_INVALID");
        negative("missing realized statement",inputs(realized,"realizationStatementReference",null),
            "UNREVIEWED_INPUT_FIELDS");
        negative("missing share agreement",inputs(realized,"shareAgreementReference",""),
            "REALIZATION_EVIDENCE_REQUIRED");
        negative("annual rate not a realized share",inputs(realized,"contractRate","18"),
            "UNREVIEWED_INPUT_FIELDS");
        negative("missing contract ref",inputs(realized,"contractReference",null),
            "UNREVIEWED_INPUT_FIELDS");
        negative("missing evidence ref",inputs(realized,"calculationEvidenceReference",""),
            "CONTRACT_EVIDENCE_REQUIRED");
        negative("future asserted realization",inputs(realized,"eventDate",TODAY.plusDays(1).toString()),
            "EVENT_NOT_AT_BANK_BUSINESS_DATE");
        negative("invalid realization date",inputs(realized,"eventDate","2026-02-30"),
            "INVALID_EVENT_DATE");
        negative("debt reverse spread",inputs(debt,"purchasePrice","101000000"),
            "NEGATIVE_SPREAD_NEEDS_LOSS_POLICY");
        negative("debt face monetary scale",inputs(debt,"faceAmount","100000000.123"),
            "DEBT_PURCHASE_MONEY_INVALID");
        negative("debt no face evidence",inputs(debt,"faceAmountEvidenceReference",""),
            "DEBT_PURCHASE_EVIDENCE_REQUIRED");
        negative("salam negative spread",inputs(salam,"saleAmount","89000000"),
            "NEGATIVE_SPREAD_NEEDS_LOSS_POLICY");
        negative("salam no sale evidence",inputs(salam,"saleEvidenceReference","invalid refs"),
            "SALAM_EVIDENCE_REQUIRED");
        negative("salam cannot assert client tax",inputs(salam,"taxPercent","10"),
            "UNREVIEWED_INPUT_FIELDS");
        negative("estimate nine digit rate",inputs(estimate,"contractRate","18.123456789"),
            "ESTIMATE_RATE_NUMBER_18_8");
        negative("estimate no rate evidence",inputs(estimate,"rateEvidenceReference",""),
            "ESTIMATE_CONTRACT_EVIDENCE_REQUIRED");
        negative("estimate invalid period",inputs(estimate,"periodEndExclusive",TODAY.toString()),
            "ESTIMATE_PERIOD_UNSUPPORTED");
        negative("estimate cannot inject factor",inputs(estimate,"yearFraction","0.9"),
            "UNREVIEWED_INPUT_FIELDS");
        negative("estimate cannot inject months",inputs(estimate,"contractMonths",12),
            "UNREVIEWED_INPUT_FIELDS");
        negative("model cannot use late formula",rule(estimate,"LOAN_LATE_HOLIDAY",null,Map.of()),
            "MODEL_FORMULA_MISMATCH");
        negative("model cannot use editable expression",rule(estimate,"TPL_E624746F262F6E",null,
            Map.of("_fceEditableExpressionV1","amount*rate")),"CLIENT_FORMULA_OVERRIDE_FORBIDDEN");
        negative("model cannot use fixed rule",rule(estimate,"TPL_E624746F262F6E",new BigDecimal("100"),Map.of()),
            "UNSUPPORTED_MODEL_RULE");
        negative("model event cross-use",version(realized,"PARTICIPATION_ESTIMATE",
            "MUSHARAKA_MADANI","PARTICIPATION_REALIZED","OUT_OF_SCOPE","HALF_UP"),
            "MODEL_FORMULA_MISMATCH");
        negative("salam estimate not realized spread",version(salamEst,"SALAM_SPREAD","SALAM",
            "TPL_E624746F262F6E","OUT_OF_SCOPE","HALF_UP"),"MODEL_FORMULA_MISMATCH");
        negative("unreviewed profit tax",version(debt,"DEBT_PURCHASE_SPREAD","DEBT_PURCHASE",
            "DEBT_PURCHASE_SPREAD","UNREVIEWED","HALF_UP"),"TAX_AND_FINANCIAL_POLICY_OUT_OF_SCOPE");
        negative("unreviewed rounding",version(debt,"DEBT_PURCHASE_SPREAD","DEBT_PURCHASE",
            "DEBT_PURCHASE_SPREAD","OUT_OF_SCOPE","INVALID"),"UNSUPPORTED_ROUNDING");
        negative("wrong contract for debt",version(debt,"DEBT_PURCHASE_SPREAD","SALAM",
            "DEBT_PURCHASE_SPREAD","OUT_OF_SCOPE","HALF_UP"),"CONTRACT_TYPE_MODEL_MISMATCH");
        // Hash is insensitive to JSON/map insertion order, but model/amount-sensitive.
        var in=new LinkedHashMap<String,Object>();
        var reversed=new ArrayList<>(realized.inputs().keySet());
        java.util.Collections.reverse(reversed);
        for(String key:reversed)in.put(key,realized.inputs().get(key));
        var replay=new Fee3R4C2LoanModelContract.Request(realized.draft(),in,null);
        var c=Fee3R4C2LoanModelContract.calculate(realized,List.of());
        check("map order does not affect SHA256",Fee3R4C2LoanModelContract.finish(realized,TODAY,ref,c)
            .snapshotSha256().equals(Fee3R4C2LoanModelContract.finish(replay,TODAY,ref,c).snapshotSha256()));
        check("different model has different SHA256",!Fee3R4C2LoanModelContract.finish(realized,TODAY,ref,c)
            .snapshotSha256().equals(Fee3R4C2LoanModelContract.finish(estimate,TODAY,ref,
                Fee3R4C2LoanModelContract.calculate(estimate,days())).snapshotSha256()));
        check("published ref blocked",Fee3R4C2LoanModelContract.finish(realized,TODAY,
            new Fee3R4CalculationContract.VersionReference("PERSISTED",1L,1L,1L,1L,
                "PUBLISHED",true,1),c).status().equals("BLOCKED"));
        check("missing ref blocked",Fee3R4C2LoanModelContract.finish(realized,TODAY,null,c)
            .status().equals("BLOCKED"));
        check("missing CAL2 slices fail closed",throwsError(()->Fee3R4C2LoanModelContract.calculate(estimate,List.of())));
        check("wrong CAL2 period fail closed",throwsError(()->Fee3R4C2LoanModelContract.calculate(estimate,
            List.of(new Fee3R4C1LoanProfitContract.YearSlice(1405,TODAY,TODAY.plusDays(9),
                9,365,"CAL2:PERSIAN:UAT")))));
        check("non-estimate rejects supplied CAL2",throwsError(()->Fee3R4C2LoanModelContract.calculate(realized,days())));
        System.out.println("FEE3_R4C2_PURE_JAVA_PASS="+pass+" FAIL="+fail);
        if(fail>0)System.exit(2);
    }
    private static boolean throwsError(Runnable action){try{action.run();return false;}
        catch(IllegalArgumentException ex){return true;}}
}
