import com.behsazan.corebanking.collateral.release.CollateralReleasePostingPolicy;
import java.time.LocalDateTime;
import java.util.List;

/** No Oracle required; tests release authorization safety, not banking acceptance. */
public final class ReleasePostingPolicySmoke {
    private static int count=0;
    private static void check(boolean state,String label){count++;if(!state)throw new AssertionError(label);}
    static final class Inputs {
        String expected="4",actual="4",permitVersion="4",status="ACTIVE",source="ACTIVE",target="RELEASED",
          configured="RELEASED",branch="1001",permitBranch="1001",permitStatus="APPROVED",
          maker="maker",checker="checker",poster="poster",approval="APP-1",legal="LEGAL-1",
          consumer="CONS-1",unallocation="UNAL-1",lastHistoryStatus="ACTIVE";
        boolean validTerminal=true;
        long active=0,foreign=0,invalid=0,unresolved=0,sequence=2;
        LocalDateTime expires=LocalDateTime.of(2027,1,1,0,0),now=LocalDateTime.of(2026,11,10,8,0);
        CollateralReleasePostingPolicy.Facts toFacts(){return new CollateralReleasePostingPolicy.Facts(
          expected,actual,permitVersion,status,source,target,configured,validTerminal,
          branch,permitBranch,active,foreign,invalid,unresolved,permitStatus,maker,checker,
          poster,approval,legal,consumer,unallocation,expires,now,lastHistoryStatus,sequence);
        }
    }
    private static void reject(String code,java.util.function.Consumer<Inputs> mutation){
        var inputs=new Inputs();mutation.accept(inputs);
        var issues=CollateralReleasePostingPolicy.blockers(inputs.toFacts());
        check(issues.contains(code),"should block "+code+": "+issues);
    }
    public static void main(String[] args){
        check(CollateralReleasePostingPolicy.blockers(new Inputs().toFacts()).isEmpty(),"base eligible");
        reject("RELEASE_PERMIT_NOT_APPROVED",i->i.permitStatus="DRAFT");
        reject("RELEASE_PERMIT_NOT_APPROVED",i->i.permitStatus="POSTED");
        reject("RELEASE_INDEPENDENT_ACTORS_REQUIRED",i->i.maker="checker");
        reject("RELEASE_INDEPENDENT_ACTORS_REQUIRED",i->i.poster="maker");
        reject("RELEASE_INDEPENDENT_ACTORS_REQUIRED",i->i.poster="checker");
        reject("RELEASE_INDEPENDENT_ACTORS_REQUIRED",i->i.maker="");
        reject("RELEASE_INDEPENDENT_ACTORS_REQUIRED",i->i.checker=null);
        reject("RELEASE_INDEPENDENT_ACTORS_REQUIRED",i->i.poster="");
        reject("RELEASE_BRANCH_MISMATCH",i->i.permitBranch="1002");
        reject("RELEASE_BRANCH_MISMATCH",i->i.branch="");
        reject("RELEASE_COLLATERAL_VERSION_CHANGED",i->i.expected="3");
        reject("RELEASE_COLLATERAL_VERSION_CHANGED",i->i.actual="5");
        reject("RELEASE_COLLATERAL_VERSION_CHANGED",i->i.permitVersion="3");
        reject("RELEASE_SOURCE_STATUS_NOT_APPROVED",i->i.status="BLOCKED");
        reject("RELEASE_SOURCE_STATUS_NOT_APPROVED",i->i.source="");
        reject("RELEASE_TARGET_STATUS_NOT_APPROVED",i->i.validTerminal=false);
        reject("RELEASE_TARGET_STATUS_NOT_APPROVED",i->i.target="ACTIVE");
        reject("RELEASE_TARGET_STATUS_NOT_APPROVED",i->i.configured="OTHER");
        reject("RELEASE_TARGET_STATUS_NOT_APPROVED",i->i.configured="");
        reject("RELEASE_ACTIVE_ALLOCATIONS",i->i.active=1);
        reject("RELEASE_FOREIGN_BRANCH_ALLOCATIONS",i->i.foreign=1);
        reject("RELEASE_UNRECOGNIZED_ALLOCATION_STATE",i->i.invalid=1);
        reject("RELEASE_UNRESOLVED_TRANSACTIONS",i->i.unresolved=1);
        reject("RELEASE_INVALID_EVIDENCE_COUNTS",i->i.active=-1);
        reject("RELEASE_INVALID_EVIDENCE_COUNTS",i->i.foreign=-1);
        reject("RELEASE_INVALID_EVIDENCE_COUNTS",i->i.invalid=-1);
        reject("RELEASE_INVALID_EVIDENCE_COUNTS",i->i.unresolved=-1);
        reject("RELEASE_SIGNED_APPROVAL_EVIDENCE_INCOMPLETE",i->i.approval="");
        reject("RELEASE_SIGNED_APPROVAL_EVIDENCE_INCOMPLETE",i->i.legal=null);
        reject("RELEASE_SIGNED_APPROVAL_EVIDENCE_INCOMPLETE",i->i.consumer="");
        reject("RELEASE_SIGNED_APPROVAL_EVIDENCE_INCOMPLETE",i->i.unallocation="");
        reject("RELEASE_PERMIT_EXPIRED",i->i.expires=i.now);
        reject("RELEASE_PERMIT_EXPIRED",i->i.expires=i.now.minusDays(1));
        reject("RELEASE_PERMIT_EXPIRED",i->i.now=null);
        reject("RELEASE_STATUS_HISTORY_CHAIN_INCONSISTENT",i->i.sequence=0);
        reject("RELEASE_STATUS_HISTORY_CHAIN_INCONSISTENT",i->i.lastHistoryStatus="PENDING");
        for(String good:List.of("1","9999999999999999999","4038")){
            check(good.equals(CollateralReleasePostingPolicy.decimalId(good)),"valid id"+good);
        }
        for(String bad:List.of("0","-1","01","1.0","10000000000000000000","", "abc")){
            try{CollateralReleasePostingPolicy.decimalId(bad);check(false,"bad id accepted");}
            catch(IllegalArgumentException okay){count++;}
        }
        for(String good:List.of("1","9999999999","10")){
            check(good.equals(CollateralReleasePostingPolicy.version(good)),"valid version");
        }
        for(String bad:List.of("0","01","-1","10000000000","")){
            try{CollateralReleasePostingPolicy.version(bad);check(false,"bad version accepted");}
            catch(IllegalArgumentException okay){count++;}
        }
        for(String good:List.of("RELEASED","A_01","STATUS_2")){
            check(good.equals(CollateralReleasePostingPolicy.code(good)),"valid code");
        }
        for(String bad:List.of("released","-","R A","","A")){
            try{CollateralReleasePostingPolicy.code(bad);check(false,"bad status accepted");}
            catch(IllegalArgumentException okay){count++;}
        }
        for(String good:List.of("operator","employee-12","a@example.org")){
            check(good.equals(CollateralReleasePostingPolicy.actor(good)),"valid actor");
        }
        for(String bad:List.of("","space operator","/operator")){
            try{CollateralReleasePostingPolicy.actor(bad);check(false,"bad actor accepted");}
            catch(IllegalArgumentException okay){count++;}
        }
        String uuid="123e4567-e89b-42d3-a456-426614174000";
        check(uuid.equals(CollateralReleasePostingPolicy.key(uuid)),"idempotent UUID");
        check("5".equals(CollateralReleasePostingPolicy.etag("\"5\"")),"strong ETag");
        try{CollateralReleasePostingPolicy.etag("W/\"5\"");check(false,"weak ETag accepted");}
        catch(IllegalArgumentException okay){count++;}
        try{CollateralReleasePostingPolicy.key("reused");check(false,"invalid key accepted");}
        catch(IllegalArgumentException okay){count++;}
        System.out.println("CLT_BUC11_RELEASE_POLICY_PASS="+count+"/"+count);
    }
}
