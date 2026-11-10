import com.behsazan.corebanking.collateral.reintroduction.CollateralReintroductionPolicy;
import com.behsazan.corebanking.collateral.reintroduction.CollateralReintroductionPolicy.Facts;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.List;

public final class ReintroductionPolicySmoke {
  private static int assertions=0;
  static void check(boolean ok,String name){assertions++;if(!ok)throw new AssertionError(name);}
  static Facts normal(){
    LocalDateTime now=LocalDateTime.of(2026,11,10,12,0);
    return new Facts("100","100","RELEASED",true,true,true,0,0,
      "42","42",true,0,true,"12","12","100","REVIEWED",
      "a".repeat(64),"a".repeat(64),true,"230","230",true,true,true,true,
      "5555","6666","ACTIVE","ACTIVE".equals("ACTIVE"),"9","9",
      "maker","checker","poster","BANK-SIGN","CIF-ATTEST","ASSET-PROOF",
      "APPROVED",now.plusDays(1),now);
  }
  static Facts change(Facts source,String field,Object value){
    try{
      RecordComponent[] members=Facts.class.getRecordComponents();
      Object[] values=new Object[members.length];Class<?>[] types=new Class<?>[members.length];
      boolean found=false;
      for(int i=0;i<members.length;i++){
        RecordComponent m=members[i];types[i]=m.getType();values[i]=m.getAccessor().invoke(source);
        if(m.getName().equals(field)){values[i]=value;found=true;}
      }
      if(!found)throw new AssertionError("unknown field "+field);
      Constructor<Facts> ctor=Facts.class.getDeclaredConstructor(types);
      return ctor.newInstance(values);
    }catch(Exception ex){throw new IllegalStateException(ex);}
  }
  static void fails(String code,String field,Object value){
    var actual=CollateralReintroductionPolicy.blockers(change(normal(),field,value));
    check(actual.contains(code),field+" missing "+code+" "+actual);
  }
  public static void main(String[] args){
    check(CollateralReintroductionPolicy.blockers(normal()).isEmpty(),"fully-approved baseline");
    fails("REINTRO_PERMIT_NOT_APPROVED","permitStatus","DRAFT");
    fails("REINTRO_BRANCH_MISMATCH","previousBranch","101");
    fails("REINTRO_BRANCH_MISMATCH","reviewedBranch","101");
    fails("REINTRO_BRANCH_MISMATCH","branch",null);
    fails("REINTRO_FINAL_RELEASE_NOT_PROVEN","previousStatusIsTerminal",false);
    fails("REINTRO_FINAL_RELEASE_NOT_PROVEN","actualFinalReleasePosted",false);
    fails("REINTRO_FINAL_RELEASE_NOT_PROVEN","finalStatusHistoryMatches",false);
    fails("REINTRO_PREVIOUS_ALLOCATION_NOT_CLEARED","activeAllocationCount",1L);
    fails("REINTRO_PREVIOUS_ALLOCATION_NOT_CLEARED","unknownAllocationCount",1L);
    fails("REINTRO_PREVIOUS_ALLOCATION_NOT_CLEARED","activeAllocationCount",-1L);
    fails("REINTRO_STABLE_ASSET_NOT_VERIFIED","oldAssetId","43");
    fails("REINTRO_STABLE_ASSET_NOT_VERIFIED","assetIdentityVerified",false);
    fails("REINTRO_ASSET_ALREADY_CURRENT_OR_LINK_INVALID","otherCurrentAssetLinks",1L);
    fails("REINTRO_ASSET_ALREADY_CURRENT_OR_LINK_INVALID","currentLinkIsPrevious",false);
    fails("REINTRO_ASSET_TYPE_CHANGED","reviewedTypeId","13");
    fails("REINTRO_REVIEWED_DRAFT_NOT_CONSISTENT","reviewStatus","PENDING_REVIEW");
    fails("REINTRO_REVIEWED_DRAFT_NOT_CONSISTENT","reviewHash","different");
    fails("REINTRO_REVIEWED_DRAFT_NOT_CONSISTENT","draftStillImmutable",false);
    fails("REINTRO_OWNER_REVERIFICATION_REQUIRED","approvedPartyId","231");
    fails("REINTRO_OWNER_REVERIFICATION_REQUIRED","ownerReverified",false);
    fails("REINTRO_OWNER_REVERIFICATION_REQUIRED","ownerEvidenceReference",null);
    fails("REINTRO_INTRODUCTION_OR_SERIAL_RULES_REQUIRED","introductionRulesVerified",false);
    fails("REINTRO_INTRODUCTION_OR_SERIAL_RULES_REQUIRED","serialRangesVerified",false);
    fails("REINTRO_NEW_BANK_NUMBER_REQUIRED","bankRegistrationNumberAvailable",false);
    fails("REINTRO_NEW_BANK_NUMBER_REQUIRED","bankRegistrationNumber","5555");
    fails("REINTRO_NEW_BANK_NUMBER_REQUIRED","bankRegistrationNumber","bad-number");
    fails("REINTRO_NEW_ACTIVE_STATUS_NOT_APPROVED","initialStatusApprovedActive",false);
    fails("REINTRO_NEW_ACTIVE_STATUS_NOT_APPROVED","initialStatusCode","RELEASED");
    fails("REINTRO_RELEASED_COLLATERAL_VERSION_CHANGED","approvedSourceVersion","10");
    fails("REINTRO_THREE_PARTY_APPROVAL_REQUIRED","checker","maker");
    fails("REINTRO_THREE_PARTY_APPROVAL_REQUIRED","poster","maker");
    fails("REINTRO_THREE_PARTY_APPROVAL_REQUIRED","poster","checker");
    fails("REINTRO_BANK_APPROVAL_OR_IDENTITY_EVIDENCE_REQUIRED","approvalReference","");
    fails("REINTRO_BANK_APPROVAL_OR_IDENTITY_EVIDENCE_REQUIRED","identityEvidenceReference","");
    fails("REINTRO_APPROVAL_EXPIRED","expiresAt",LocalDateTime.of(2026,11,10,11,59));
    fails("REINTRO_APPROVAL_EXPIRED","oracleNow",null);
    for(String bad:List.of("0","00012","-1","1.2","99999999999999999999")){
      boolean rejected=false;
      try{CollateralReintroductionPolicy.requiredId(bad);}catch(IllegalArgumentException e){rejected=true;}
      check(rejected,"invalid id "+bad);
    }
    for(String bad:List.of("7","bad","\"0\"","\"01\"","\"12345678901\"")){
      boolean rejected=false;
      try{CollateralReintroductionPolicy.etag(bad);}catch(IllegalArgumentException e){rejected=true;}
      check(rejected,"invalid strong etag "+bad);
    }
    check("12".equals(CollateralReintroductionPolicy.etag("\"12\"")),"etag decode");
    check("12345678".equals(CollateralReintroductionPolicy.idempotency("12345678")),"idempotent key");
    check(CollateralReintroductionPolicy.blockers(normal()).size()==0,"no fabricated blocker");
    System.out.println("CLT_BUC12_POLICY_PASS="+assertions+"/"+assertions);
  }
}
