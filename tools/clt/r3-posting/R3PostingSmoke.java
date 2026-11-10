import com.behsazan.corebanking.collateral.workflow.CollateralPostingReadinessPolicy;
import com.behsazan.corebanking.collateral.workflow.CollateralWorkflowPolicy.Kind;

/** No database or framework. Pure bank decision safety smoke. */
public class R3PostingSmoke {
    private static int passed;
    private static final String H = "a".repeat(64);
    static void require(boolean test, String label) {
        if (!test) throw new AssertionError(label);
        passed++;
    }
    static CollateralPostingReadinessPolicy.Facts f(Kind kind,String status,String version,
            String digest,long created,long submitted,long reviewed,long events,
            long conflicts,String collateralStatus,String typeStatus,
            String current,String supersedes,boolean ref) {
        return new CollateralPostingReadinessPolicy.Facts(kind,"77","88",status,version,H,digest,
                created,submitted,reviewed,events,conflicts,collateralStatus,typeStatus,current,supersedes,ref);
    }
    public static void main(String[] args) {
        for (Kind kind: Kind.values()) {
            var good=CollateralPostingReadinessPolicy.evaluate(f(kind,"REVIEWED","3",H,1,1,1,3,0,
                    "ACTIVE","ACTIVE",null,null,true));
            require(good.stageIntegrityVerified(),kind+" stage good");
            require(!good.finalPostingEligible(),kind+" no final permit");
            require(!good.finalPostingEnabled(),kind+" DML disabled");
            require(!good.policyApproved(),kind+" no approved policy");
            require(good.mandatoryApprovalGates().stream().anyMatch(x->x.startsWith("G-09")),kind+" integration gap");
            require(good.integrityBlockers().isEmpty(),kind+" no false blocker");
            var invalid=CollateralPostingReadinessPolicy.evaluate(f(kind,"SUBMITTED","2","b".repeat(64),
                    0,0,0,0,2,"RELEASED","INACTIVE",null,null,true));
            for (String code: new String[]{"STAGE_REVIEWED","STAGE_VERSION","PAYLOAD_FINGERPRINT",
                    "CREATED_AUDIT","SUBMITTED_AUDIT","INDEPENDENT_REVIEW_AUDIT","AUDIT_EVENT_COUNT",
                    "COLLATERAL_NOT_RELEASED","COLLATERAL_TYPE_ACTIVE","NO_PHYSICAL_DUPLICATE"}) {
                require(invalid.integrityBlockers().contains(code),kind+" must reject "+code);
            }
            require(!invalid.stageIntegrityVerified(),kind+" not consistent");
        }
        var v=CollateralPostingReadinessPolicy.evaluate(f(Kind.VALUATION,"REVIEWED","3",H,
                1,1,1,3,0,"ACTIVE","ACTIVE","8","9",false));
        require(v.integrityBlockers().contains("VALUATION_SUPERSESSION_STABLE"),"stale pointer");
        require(v.checks().stream().anyMatch(c->c.result().equals("UNKNOWN")),"missing ref unknown");
        var unknown=CollateralPostingReadinessPolicy.evaluate(f(Kind.VALUATION,"REVIEWED","3",H,
                1,1,1,3,0,"ACTIVE","ACTIVE",null,null,false));
        require(!unknown.stageIntegrityVerified(),"unknown ref does not certify integrity");
        require(!unknown.finalPostingEligible(),"no false posting");
        try {
            CollateralPostingReadinessPolicy.evaluate(f(Kind.SPONSOR,"REVIEWED","3",H,
                    -1,1,1,3,0,"ACTIVE","ACTIVE",null,null,true));
            throw new AssertionError("negative accepted");
        } catch (IllegalArgumentException ok) {passed++;}
        System.out.println("CLT_R3_POSTING_POLICY_PASS="+passed+"/"+passed);
    }
}
