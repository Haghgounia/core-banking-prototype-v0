import com.behsazan.corebanking.collateral.valuation.CollateralValuationModels.CandidateRequest;
import com.behsazan.corebanking.collateral.valuation.CollateralValuationPolicy;

/** Pure Java21 checks. Never claims Spring, IAM, Oracle or end-to-end validation. */
public final class ValuationPolicySmoke {
    private static int passed;
    private static void ok(boolean valid, String name) {
        if (!valid) throw new AssertionError("FAILED:" + name);
        passed++;
    }
    private static void deny(Runnable run, String name) {
        try {run.run(); throw new AssertionError("EXPECTED_REJECTION:" + name);}
        catch (IllegalArgumentException ex) {
            ok(ex.getMessage().startsWith("CLT_VALUATION_"), name);
        }
    }
    private static CandidateRequest request() {
        return new CandidateRequest("INITIAL", "MARKET_COMPARISON", "2026-11-03",
                "9999999999999999.9999", "IRR", "1000.5000", "900.00", "12.25",
                "9999999999999999999", null, "EV-2026-111", "2026-11-03", "2027-11-03",
                null, "درخواست ارزیابی اولیه با گزارش کارشناس");
    }
    private static CandidateRequest withAmount(String amount) {
        var x = request(); return new CandidateRequest(x.typeCode(),x.methodCode(),x.valuationDate(),
                amount,x.currencyCode(),x.forcedSaleValue(),x.eligibleValue(),x.haircutPercent(),
                x.valuerPartyId(),x.valuerName(),x.valuationReference(),x.effectiveFrom(),
                x.effectiveTo(),x.supersedesValuationId(),x.justification());
    }
    private static CandidateRequest withPeriod(String evalDate, String from, String to) {
        var x = request(); return new CandidateRequest(x.typeCode(),x.methodCode(),evalDate,
                x.valuationAmount(),x.currencyCode(),x.forcedSaleValue(),x.eligibleValue(),x.haircutPercent(),
                x.valuerPartyId(),x.valuerName(),x.valuationReference(),from,to,x.supersedesValuationId(),x.justification());
    }
    public static void main(String[] args) {
        var n = CollateralValuationPolicy.normalize(request());
        ok(n.typeCode().equals("INITIAL"),"type");
        ok(n.methodCode().equals("MARKET_COMPARISON"),"method");
        ok(n.valuationAmount().equals("9999999999999999.9999"),"NUMBER20_4 exact");
        ok(n.forcedSaleValue().equals("1000.5"),"force decimal normalized");
        ok(n.eligibleValue().equals("900"),"eligible decimal normalized");
        ok(n.haircutPercent().equals("12.25"),"haircut decimal exact");
        ok(n.valuerPartyId().equals("9999999999999999999"),"valuer id19 exact");
        ok(n.valuationReference().equals("EV-2026-111"),"document reference");
        ok(n.effectiveFrom().equals("2026-11-03"),"effective from");
        ok(n.effectiveTo().equals("2027-11-03"),"effective to");
        ok(n.canonical().contains("MARKET_COMPARISON"),"stable canonical");
        ok(CollateralValuationPolicy.digest(n.canonical()).length()==64,"sha256 fingerprint");
        ok(!CollateralValuationPolicy.digest(n.canonical()).equals(
                CollateralValuationPolicy.digest(n.canonical()+"a")),"fingerprint sensitivity");
        ok(CollateralValuationPolicy.id("9999999999999999999").length()==19,"id19");
        ok(CollateralValuationPolicy.requestKey("valuation-abc-123").length()>8,"idempotency key");
        ok(CollateralValuationPolicy.correlation(null).equals("NO_CORRELATION"),"default correlation");
        ok(CollateralValuationPolicy.date("2024-02-29").getDayOfMonth()==29,"leap date");
        ok(CollateralValuationPolicy.date("2026-11-03").getYear()==2026,"gregorian date");
        var effective=CollateralValuationPolicy.recordedPeriod("50","50","FINAL",
                "2026-01-01","2026-11-03","2026-11-03",true);
        ok(effective.withinRecordedPeriod(),"expiry boundary inclusive");
        ok(effective.state().equals("WITHIN_RECORDED_EFFECTIVE_PERIOD"),"recorded date evidence only");
        var expired=CollateralValuationPolicy.recordedPeriod("50","50","FINAL",
                "2026-01-01","2026-11-03","2026-11-04",true);
        ok(expired.state().equals("EXPIRED"),"day after expiry");
        ok(!expired.withinRecordedPeriod(),"expired not current");
        ok(CollateralValuationPolicy.recordedPeriod("50","50","FINAL",
                "2026-01-01",null,"2026-11-03",true).blockers().contains(
                "CLT_VALUATION_EFFECTIVE_TO_UNSPECIFIED"),"missing date not treated as infinite");
        ok(!CollateralValuationPolicy.recordedPeriod("50","50","SUPERSEDED",
                "2026-01-01","2027-01-01","2026-11-03",true).withinRecordedPeriod(),"superseded not final");
        ok(!CollateralValuationPolicy.recordedPeriod("50","50","APPROVED",
                "2026-01-01","2027-01-01","2026-11-03",true).withinRecordedPeriod(),"UI approved not Oracle final");
        ok(!CollateralValuationPolicy.recordedPeriod("50","51","FINAL",
                "2026-01-01","2027-01-01","2026-11-03",true).withinRecordedPeriod(),"pointer mismatch");
        ok(!CollateralValuationPolicy.recordedPeriod(null,null,null,
                null,null,"2026-11-03",false).withinRecordedPeriod(),"pointer missing");
        ok(!CollateralValuationPolicy.recordedPeriod("50","50","FINAL",
                "2026-01-01","2027-01-01","2026-11-03",false).withinRecordedPeriod(),"currency mismatch");
        ok(!CollateralValuationPolicy.recordedPeriod("50","50","FINAL",
                "2026-11-04","2027-11-03","2026-11-03",true).withinRecordedPeriod(),"future effectiveness");
        ok(CollateralValuationPolicy.candidateBlockers(true,true,true,true,true,"ACTIVE",null,null,false).isEmpty(),
                "new staging request");
        ok(CollateralValuationPolicy.candidateBlockers(true,true,true,true,true,"PLEDGED","1","1",true).isEmpty(),
                "revaluation staging with current FINAL");
        ok(CollateralValuationPolicy.candidateBlockers(true,true,true,true,true,"PARTIAL_RELEASE","1","1",true).isEmpty(),
                "partial-release history remains");
        ok(CollateralValuationPolicy.candidateBlockers(true,true,true,true,true,"RELEASED",null,null,false).size()==1,
                "released rejected");
        ok(CollateralValuationPolicy.candidateBlockers(true,true,true,true,true,"ACTIVE","5",null,false).contains(
                "CLT_VALUATION_CURRENT_MUST_BE_REFERENCED"),"missing previous rejected");
        ok(CollateralValuationPolicy.candidateBlockers(true,true,true,true,true,"ACTIVE",null,"5",false).contains(
                "CLT_VALUATION_SUPERSEDES_WITHOUT_CURRENT"),"unjustified supersession rejected");
        ok(CollateralValuationPolicy.candidateBlockers(true,true,true,true,true,"ACTIVE","4","5",false).contains(
                "CLT_VALUATION_SUPERSEDES_NOT_CURRENT_FINAL"),"stale reference rejected");
        ok(CollateralValuationPolicy.candidateBlockers(false,true,true,true,true,"ACTIVE",null,null,false).contains(
                "CLT_VALUATION_TYPE_INACTIVE"),"inactive type rejected");
        ok(CollateralValuationPolicy.candidateBlockers(true,false,true,true,true,"ACTIVE",null,null,false).contains(
                "CLT_VALUATION_METHOD_INACTIVE"),"inactive method rejected");
        ok(CollateralValuationPolicy.candidateBlockers(true,true,false,true,true,"ACTIVE",null,null,false).contains(
                "CLT_VALUATION_CURRENCY_INACTIVE"),"inactive currency rejected");
        ok(CollateralValuationPolicy.candidateBlockers(true,true,true,false,true,"ACTIVE",null,null,false).contains(
                "CLT_VALUATION_OWNER_NOT_VERIFIED_BY_CIF"),"CIF owner must be verified");
        ok(CollateralValuationPolicy.candidateBlockers(true,true,true,true,false,"ACTIVE",null,null,false).contains(
                "CLT_VALUATION_COLLATERAL_TYPE_INACTIVE"),"inactive collateral type rejected");
        deny(()->CollateralValuationPolicy.id("00012"),"id leading zeros");
        deny(()->CollateralValuationPolicy.id("10000000000000000000"),"id20 rejected");
        deny(()->CollateralValuationPolicy.id("1e10"),"id scientific");
        deny(()->CollateralValuationPolicy.requestKey("short"),"idempotency short");
        deny(()->CollateralValuationPolicy.requestKey("unsafe/key/with/slash"),"idempotency unsafe");
        deny(()->CollateralValuationPolicy.correlation("a b"),"correlation invalid");
        deny(()->CollateralValuationPolicy.date("2026-02-29"),"date invalid leap");
        deny(()->CollateralValuationPolicy.normalize(withAmount("10000000000000000")),"amount 17 integer digits");
        deny(()->CollateralValuationPolicy.normalize(withAmount("1e10")),"scientific amount denied");
        deny(()->CollateralValuationPolicy.normalize(withAmount("-3")),"negative amount denied");
        deny(()->CollateralValuationPolicy.normalize(withAmount("10.12345")),"amount 5 decimals denied");
        deny(()->CollateralValuationPolicy.normalize(withPeriod("2026-11-03","2026-11-02","2027-11-03")),
                "effective before evaluation denied");
        deny(()->CollateralValuationPolicy.normalize(withPeriod("2026-11-03","2026-11-03","2026-11-02")),
                "reverse effective denied");
        deny(()->CollateralValuationPolicy.normalize(withPeriod("2026-02-30","2026-11-03","2027-11-03")),
                "invalid valuation day denied");
        var x=request();
        deny(()->CollateralValuationPolicy.normalize(new CandidateRequest(x.typeCode(),x.methodCode(),x.valuationDate(),
                x.valuationAmount(),x.currencyCode(),x.forcedSaleValue(),x.eligibleValue(),"100.01",x.valuerPartyId(),
                x.valuerName(),x.valuationReference(),x.effectiveFrom(),x.effectiveTo(),null,x.justification())),
                "haircut >100 denied");
        deny(()->CollateralValuationPolicy.normalize(new CandidateRequest(x.typeCode(),x.methodCode(),x.valuationDate(),
                x.valuationAmount(),x.currencyCode(),x.forcedSaleValue(),x.eligibleValue(),"10.123",x.valuerPartyId(),
                x.valuerName(),x.valuationReference(),x.effectiveFrom(),x.effectiveTo(),null,x.justification())),
                "haircut scale3 denied");
        deny(()->CollateralValuationPolicy.normalize(new CandidateRequest(x.typeCode(),x.methodCode(),x.valuationDate(),
                x.valuationAmount(),x.currencyCode(),x.forcedSaleValue(),x.eligibleValue(),x.haircutPercent(),null,
                null,x.valuationReference(),x.effectiveFrom(),x.effectiveTo(),null,x.justification())),
                "no valuer denied");
        deny(()->CollateralValuationPolicy.normalize(new CandidateRequest(x.typeCode(),x.methodCode(),x.valuationDate(),
                x.valuationAmount(),x.currencyCode(),x.forcedSaleValue(),x.eligibleValue(),x.haircutPercent(),x.valuerPartyId(),
                x.valuerName(),x.valuationReference(),x.effectiveFrom(),x.effectiveTo(),"10000000000000000000",x.justification())),
                "previous id overflow");
        deny(()->CollateralValuationPolicy.normalize(new CandidateRequest("APPROVED",x.methodCode(),x.valuationDate(),
                x.valuationAmount(),"irR",x.forcedSaleValue(),x.eligibleValue(),x.haircutPercent(),x.valuerPartyId(),
                x.valuerName(),x.valuationReference(),x.effectiveFrom(),x.effectiveTo(),null,x.justification())),
                "currency lowercase denied");
        System.out.println("CLT_R3C_VALUATION_POLICY_SMOKE_PASS="+passed);
    }
}
