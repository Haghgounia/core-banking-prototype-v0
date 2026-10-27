import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.r3.Fee3PreviewEngine;
import com.behsazan.corebanking.fee3.r3.Fee3SubmissionPolicy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class Fee3R3EngineProbe {
    private static int checks=0;
    private static BigDecimal n(String value){return new BigDecimal(value);}
    private static void eq(Object got,Object want){checks++;if(!want.equals(got))throw new AssertionError("expected="+want+" got="+got);}
    private static void yes(boolean good){checks++;if(!good)throw new AssertionError("condition failed at check "+checks);}
    private static RuleDraft rule(String method,String rateSource,String rate,String fixed){
        return new RuleDraft(1,method,"REQUEST_AMOUNT",rateSource,rate==null?null:n(rate),
            fixed==null?null:n(fixed),null,null,null,null,null,Map.of());
    }
    private static DraftRequest draft(String method,String tax,String rounding,RuleDraft r,Map<String,Object> context){
        boolean guarantee=context.containsKey("guaranteePolicy");
        ItemDraft item=new ItemDraft("Test tariff",guarantee?"GUARANTEE":"DEPOSIT",
            guarantee?"GUARANTEE_ISSUE":"ACCOUNT_OPENING",guarantee?"GUARANTEE_FEE":"SERVICE_FEE",
            null,"TARIFF","UAT only");
        VersionDraft version=new VersionDraft(LocalDate.of(2026,10,26),null,"BANK_INTERNAL",null,
            method,tax,rounding,null,Map.of(),"IRR","IRR",context);
        return new DraftRequest(null,null,null,null,item,version,
            new ScopeDraft(List.of("BRANCH"),List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of(),"IRR"),
            List.of(r),List.of(new TestDraft("test",Map.of("amount",100),n("0"))));
    }
    private static Fee3PreviewEngine.PreviewResult calc(DraftRequest d,Map<String,Object> inputs){
        return Fee3PreviewEngine.calculate(new Fee3PreviewEngine.PreviewRequest(d,inputs));
    }
    public static void main(String[] args){
        DraftRequest fixed=draft("FIXED","EXEMPT","HALF_UP",rule("FIXED","RULE",null,"25000"),Map.of());
        var a=calc(fixed,Map.of());eq(a.status(),"SIMULATED");eq(a.feeIrr(),n("25000"));
        eq(a.totalIrr(),n("25000"));yes(!a.persisted());yes(!a.financialPosting());
        DraftRequest percent=draft("PERCENT","EXEMPT","HALF_UP",rule("PERCENT","RULE","2.5",null),Map.of());
        eq(calc(percent,Map.of("amount",10000000)).feeIrr(),n("250000"));
        DraftRequest mixed=draft("MIXED","EXEMPT","HALF_UP",rule("MIXED","RULE","2", "10000"),Map.of());
        eq(calc(mixed,Map.of("amount",10000000)).feeIrr(),n("210000"));
        DraftRequest requestRate=draft("PERCENT","EXEMPT","HALF_UP",rule("PERCENT","REQUEST",null,null),Map.of());
        eq(calc(requestRate,Map.of("amount",10000000,"contractRate",3)).feeIrr(),n("300000"));
        eq(calc(requestRate,Map.of("amount",10000000)).status(),"BLOCKED");
        eq(calc(percent,Map.of("amount","1,000")).status(),"BLOCKED");
        eq(calc(percent,Map.of("amount",-1)).status(),"BLOCKED");
        eq(calc(percent,Map.of("amount",0)).status(),"BLOCKED");
        eq(calc(percent,Map.of("amount",1000000000000000000L)).feeIrr(),n("25000000000000000"));
        DraftRequest unreviewed=draft("FIXED","UNREVIEWED","HALF_UP",rule("FIXED","RULE",null,"25000"),Map.of());
        eq(calc(unreviewed,Map.of()).status(),"BLOCKED");
        DraftRequest invalidRound=draft("FIXED","EXEMPT","SOME_OTHER",rule("FIXED","RULE",null,"25000"),Map.of());
        eq(calc(invalidRound,Map.of()).status(),"BLOCKED");
        DraftRequest formula=draft("FORMULA","EXEMPT","HALF_UP",rule("FORMULA","RULE",null,null),Map.of());
        eq(calc(formula,Map.of("amount",100)).status(),"BLOCKED");
        eq(Fee3SubmissionPolicy.check(fixed).submissionReady(),true);
        eq(Fee3SubmissionPolicy.check(formula).submissionReady(),false);
        eq(Fee3SubmissionPolicy.check(fixed).approvalReady(),false);
        yes(Fee3SubmissionPolicy.differentActors("maker","checker"));
        yes(!Fee3SubmissionPolicy.differentActors("maker","MAKER"));
        yes(Fee3SubmissionPolicy.validTransition("SUBMITTED","REVIEW","APPROVE"));
        yes(!Fee3SubmissionPolicy.validTransition("DRAFT","REVIEW","APPROVE"));
        yes(!Fee3SubmissionPolicy.validTransition("SUBMITTED","DRAFT","REJECT"));
        Map<String,Object> policy=Map.of("collateralType","MIXED_PORTFOLIO",
            "feeBasis","GUARANTEE_PORTFOLIO_ALLOCATED",
            "portfolioRules",List.of(
                Map.of("collateralType","CASH_100","rateSource","EXEMPT","coveragePercent",100),
                Map.of("collateralType","PROMISSORY_NOTE","rateSource","CONFIG","ratePercent",2.25,"coveragePercent",120),
                Map.of("collateralType","MOVABLE_PROPERTY","rateSource","CONFIG","ratePercent",1,"coveragePercent",100)));
        Map<String,Object> rowInput=Map.of("amount",100000000,"portfolio",List.of(
            Map.of("collateralType","CASH_100","sharePercent",15),
            Map.of("collateralType","PROMISSORY_NOTE","sharePercent",50),
            Map.of("collateralType","MOVABLE_PROPERTY","sharePercent",35)));
        DraftRequest guarantee=draft("PERCENT","EXEMPT","HALF_UP",rule("PERCENT","RULE","1",null),
            Map.of("guaranteePolicy",policy));
        var portfolio=calc(guarantee,rowInput);
        eq(portfolio.status(),"SIMULATED");eq(portfolio.feeIrr(),n("1475000"));
        eq(portfolio.breakdown().size(),3);
        Map<String,Object> coveredPolicy=Map.of("collateralType","MIXED_PORTFOLIO",
            "feeBasis","GUARANTEE_PORTFOLIO_COLLATERAL_VALUE","portfolioRules",policy.get("portfolioRules"));
        eq(calc(draft("PERCENT","EXEMPT","HALF_UP",rule("PERCENT","RULE","1",null),
            Map.of("guaranteePolicy",coveredPolicy)),rowInput).feeIrr(),n("1700000"));
        eq(calc(guarantee,Map.of("amount",100000000,"portfolio",List.of(
            Map.of("collateralType","CASH_100","sharePercent",10),
            Map.of("collateralType","PROMISSORY_NOTE","sharePercent",50)))).status(),"BLOCKED");
        eq(calc(guarantee,Map.of("amount",100000000,"portfolio",List.of(
            Map.of("collateralType","CHEQUE","sharePercent",100)))).status(),"BLOCKED");
        eq(calc(guarantee,Map.of("amount",100000000,"portfolio",List.of(
            Map.of("collateralType","CASH_100","sharePercent",50),
            Map.of("collateralType","CASH_100","sharePercent",50)))).status(),"BLOCKED");
        DraftRequest single=draft("PERCENT","EXEMPT","HALF_UP",rule("PERCENT","RULE","2",null),
            Map.of("guaranteePolicy",Map.of("collateralType","PROMISSORY_NOTE","feeBasis","GUARANTEE_AMOUNT","portfolioRules",List.of())));
        eq(calc(single,Map.of("amount",10000000)).feeIrr(),n("200000"));
        DraftRequest blockedDaily=draft("DAILY","EXEMPT","HALF_UP",rule("DAILY","RULE","2",null),Map.of());
        eq(calc(blockedDaily,Map.of("amount",10000000,"days",100)).status(),"BLOCKED");
        System.out.println("FEE3_R3_ENGINE_POLICY_CHECKS_PASS="+checks);
    }
}
