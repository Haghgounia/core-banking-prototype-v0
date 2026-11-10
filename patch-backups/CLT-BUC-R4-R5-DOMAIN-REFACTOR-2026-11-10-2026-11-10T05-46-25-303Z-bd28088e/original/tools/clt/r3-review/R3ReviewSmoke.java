import com.behsazan.corebanking.collateral.workflow.CollateralR3WorkflowPolicy;
import com.behsazan.corebanking.collateral.sponsor.CollateralSponsorPolicy;
import com.behsazan.corebanking.collateral.insurance.CollateralInsurancePolicy;
import com.behsazan.corebanking.collateral.insurance.CollateralInsuranceModels;
import com.behsazan.corebanking.collateral.valuation.CollateralValuationPolicy;
import com.behsazan.corebanking.collateral.valuation.CollateralValuationModels;
import java.util.List;

/** No Spring, Oracle or network required. Deterministic Java 21 policy regression checks. */
public final class R3ReviewSmoke {
    private static int checks;
    private static void ok(boolean actual, String name) {
        checks++;
        if (!actual) throw new AssertionError("FAIL " + name);
    }
    private static void same(Object actual, Object expected, String name) {
        ok(expected.equals(actual), name + " expected=" + expected + " got=" + actual);
    }
    private static void rejects(Runnable action, String name) {
        boolean rejected = false;
        try { action.run(); } catch (IllegalArgumentException | IllegalStateException ex) { rejected = true; }
        ok(rejected, name);
    }
    public static void main(String[] args) {
        var action = CollateralR3WorkflowPolicy.Action.class;
        for (var kind : CollateralR3WorkflowPolicy.Kind.values()) {
            ok(kind.table.startsWith("COLLATERAL_"), "fixed Stage enum " + kind);
            ok(kind.eventTable.startsWith("COLLATERAL_"), "fixed event enum " + kind);
            same(CollateralR3WorkflowPolicy.Kind.parse(kind.name().toLowerCase()), kind, "parse " + kind);
        }
        ok(CollateralR3WorkflowPolicy.requiresCurrentDomainValidation(CollateralR3WorkflowPolicy.Action.SUBMIT), "submit revalidate");
        ok(CollateralR3WorkflowPolicy.requiresCurrentDomainValidation(CollateralR3WorkflowPolicy.Action.REVIEW), "review revalidate");
        ok(!CollateralR3WorkflowPolicy.requiresCurrentDomainValidation(CollateralR3WorkflowPolicy.Action.REJECT), "reject not blocked by ineligibility");
        ok(!CollateralR3WorkflowPolicy.requiresCurrentDomainValidation(CollateralR3WorkflowPolicy.Action.WITHDRAW), "withdraw not blocked by ineligibility");
        same(CollateralR3WorkflowPolicy.next("DRAFT", CollateralR3WorkflowPolicy.Action.SUBMIT,true,false), "SUBMITTED", "submit");
        same(CollateralR3WorkflowPolicy.next("SUBMITTED", CollateralR3WorkflowPolicy.Action.REVIEW,false,true), "REVIEWED", "independent reviewer");
        same(CollateralR3WorkflowPolicy.next("SUBMITTED", CollateralR3WorkflowPolicy.Action.REJECT,false,true), "REJECTED", "independent reject");
        same(CollateralR3WorkflowPolicy.next("SUBMITTED", CollateralR3WorkflowPolicy.Action.WITHDRAW,true,false), "WITHDRAWN", "maker withdrawal");
        rejects(() -> CollateralR3WorkflowPolicy.next("SUBMITTED", CollateralR3WorkflowPolicy.Action.REVIEW,true,true), "maker cannot review own Stage");
        rejects(() -> CollateralR3WorkflowPolicy.next("REJECTED", CollateralR3WorkflowPolicy.Action.REVIEW,false,true), "no review of rejected Stage");
        rejects(() -> CollateralR3WorkflowPolicy.next("SUBMITTED", CollateralR3WorkflowPolicy.Action.REVIEW,false,false), "review scope required");
        rejects(() -> CollateralR3WorkflowPolicy.reason("too"), "reason must be detailed");
        same(CollateralR3WorkflowPolicy.version("1").toPlainString(), "1", "record version");
        var sponsor = CollateralSponsorPolicy.candidate("123", null, "Approved staged candidate");
        same(sponsor.partyId(), "123", "sponsor Party decimal");
        ok(CollateralSponsorPolicy.blockers("42", "123", "ACTIVE", "Y", "ACTIVE", false,true).isEmpty(), "eligible sponsor intake");
        ok(CollateralSponsorPolicy.blockers("42", "42", "ACTIVE", "Y", "ACTIVE", false,true).contains("CLT_SPONSOR_SAME_AS_COLLATERAL_OWNER"), "sponsor owner prohibited");
        ok(CollateralSponsorPolicy.blockers("42", "123", "RELEASED", "Y", "ACTIVE", false,true).contains("CLT_SPONSOR_COLLATERAL_STATUS_NOT_ALLOWED"), "released collateral blocked");
        ok(CollateralSponsorPolicy.blockers("42", "123", "ACTIVE", "Y", "ACTIVE", false,false).contains("CLT_SPONSOR_PARTY_NOT_VERIFIED_BY_CIF"), "CIF fail-closed");
        var insurance = CollateralInsurancePolicy.candidate(new CollateralInsuranceModels.CandidateRequest(
                "2", "1", "10", "ACC1", "POLICY1", "100000", "100", null,
                "2026-10-01", "2027-09-30", null, "IRR", "Independent review requested"));
        same(insurance.companyId(), "1", "insurance company");
        ok(CollateralInsurancePolicy.blockers(true,true,"ACTIVE",true,true,true,true,false,false,true).isEmpty(), "eligible insurance request");
        ok(CollateralInsurancePolicy.blockers(true,true,"ACTIVE",false,true,true,true,false,false,true).contains("CLT_INSURANCE_COMPANY_INACTIVE_OR_UNKNOWN"), "inactive insurer blocked");
        ok(CollateralInsurancePolicy.blockers(true,true,"ACTIVE",true,true,true,true,true,false,true).contains("CLT_INSURANCE_COMPANY_NUMBER_DUPLICATE"), "duplicate policy blocked");
        ok(CollateralInsurancePolicy.blockers(true,true,"ACTIVE",true,true,true,true,false,true,true).contains("CLT_INSURANCE_SAME_TYPE_PERIOD_OVERLAP"), "period overlap blocked");
        ok(CollateralInsurancePolicy.validRenewal("1","2","1","2","2026-10-01","2026-10-02",false), "renewal adjacent");
        ok(!CollateralInsurancePolicy.validRenewal("1","2","1","2","2026-10-01","2026-10-05",false), "renewal gap rejected");
        var valuation = CollateralValuationPolicy.normalize(new CollateralValuationModels.CandidateRequest(
                "INITIAL", "EXPERT", "2026-10-01", "100000", "IRR", null,
                "90000", "10", "123", null, null,
                "2026-10-01", "2027-10-01", null, "Business approval pending"));
        same(valuation.typeCode(), "INITIAL", "valuation type");
        ok(CollateralValuationPolicy.candidateBlockers(true,true,true,true,true,"ACTIVE",null,null,false).isEmpty(), "eligible valuation draft");
        ok(CollateralValuationPolicy.candidateBlockers(false,true,true,true,true,"ACTIVE",null,null,false).contains("CLT_VALUATION_TYPE_INACTIVE"), "valuation type inactive");
        ok(CollateralValuationPolicy.candidateBlockers(true,true,true,true,true,"ACTIVE","7",null,false).contains("CLT_VALUATION_CURRENT_MUST_BE_REFERENCED"), "current valuation reference mandatory");
        ok(CollateralValuationPolicy.candidateBlockers(true,true,true,false,true,"ACTIVE",null,null,false).contains("CLT_VALUATION_OWNER_NOT_VERIFIED_BY_CIF"), "valuation CIF fail-closed");
        var recorded = CollateralValuationPolicy.recordedPeriod("7","7","FINAL","2026-01-01","2026-12-31","2026-11-09",true);
        ok(recorded.withinRecordedPeriod(), "final recorded period evidence");
        var expired = CollateralValuationPolicy.recordedPeriod("7","7","FINAL","2026-01-01","2026-02-01","2026-11-09",true);
        ok(!expired.withinRecordedPeriod(), "expired not usable");
        same(expired.state(), "EXPIRED", "expiry state");
        System.out.println("CLT_R3_REVIEW_POLICY_SMOKE_PASS=" + checks + "/" + checks);
    }
}
