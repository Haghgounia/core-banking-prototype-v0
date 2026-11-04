import com.behsazan.corebanking.collateral.unallocation.CollateralUnallocationModels.*;
import com.behsazan.corebanking.collateral.unallocation.CollateralUnallocationPolicy;
import java.util.List;
import java.util.UUID;

/** R5A pure contract: does not start Spring, use JDBC, issue SQL, or authorize release. */
public class UnallocationPolicySmoke {
  private static int passed=0;
  private static void check(boolean test,String name){if(!test)throw new AssertionError(name);passed++;}
  private static void invalid(Runnable call,String name){
    boolean rejected=false;try{call.run();}catch(IllegalArgumentException e){rejected=true;}
    check(rejected,name);
  }
  private static AllocationLine allocation(boolean used,String amount,String currency,String module,String version,String file){
    return new AllocationLine("123","99",module,file,amount,currency,used,version,"2026-11-04T08:00:00",null);
  }
  private static CollateralUnallocationPolicy.Normalized normalized(String amount,String currency,String mode,String version){
    return CollateralUnallocationPolicy.normalize(new PreviewRequest("LOAN","567",amount,currency,mode,"CASE_REF_001"),"\""+version+"\"");
  }
  private static boolean contains(AllocationLine record,CollateralUnallocationPolicy.Normalized n,String module,boolean service,String reason){
    return CollateralUnallocationPolicy.blockers(record,n,module,service).contains(reason);
  }
  public static void main(String[] args){
    var good=allocation(true,"100.0000","IRR","2","4","567");
    var n=normalized("100","IRR","FULL","4");
    var result=CollateralUnallocationPolicy.blockers(good,n,"2",true);
    check(result.size()>=5,"unconditional blockers");
    check(!result.contains("CLT_R5A_ALLOCATION_ALREADY_NOT_USED"),"active snapshot");
    check(!result.contains("CLT_R5A_CURRENCY_MISMATCH_NO_FX_ASSUMPTION"),"currency matched");
    check(!result.contains("CLT_R5A_DOMAIN_MODULE_ID_NOT_APPROVED_OR_MISMATCH"),"domain mapping matched");
    check(!result.contains("CLT_R5A_ALLOCATION_VERSION_MISMATCH"),"etag matched");
    check(!result.contains("CLT_R5A_CONSUMER_FILE_NUMBER_MISMATCH"),"consumer file matched");
    check(!result.contains("CLT_R5A_REQUEST_EXCEEDS_ALLOCATION"),"money equality independent of scale");
    check(!result.contains("CLT_R5A_PARTIAL_UNALLOCATION_POLICY_NOT_APPROVED"),"full candidate");
    check(result.contains("G-09_CONSUMER_OBLIGATION_STATUS_NOT_ATTESTED"),"consumer attestation missing");
    check(result.contains("G-13_BRANCH_AND_CROSS_BRANCH_RELEASE_AUTHORIZATION_NOT_APPROVED"),"branch approvals missing");
    check(result.contains("CLT_R5A_FINANCIAL_UNALLOCATION_AND_AUDIT_WRITES_DISABLED"),"NO finance");
    check(result.contains("CLT_R5A_DURABLE_UNALLOCATION_IDEMPOTENCY_NOT_IMPLEMENTED"),"idempotency not durable");
    check(!CollateralUnallocationPolicy.everAuthorizesUnallocation(),"ALWAYS DENY");
    check(contains(allocation(false,"100","IRR","2","4","567"),n,"2",true,"CLT_R5A_ALLOCATION_ALREADY_NOT_USED"),"USED NO not active");
    check(contains(good,n,"",true,"CLT_R5A_DOMAIN_MODULE_ID_NOT_APPROVED_OR_MISMATCH"),"unconfigured mapping");
    check(contains(good,n,"3",true,"CLT_R5A_DOMAIN_MODULE_ID_NOT_APPROVED_OR_MISMATCH"),"wrong module");
    check(contains(good,n,null,true,"CLT_R5A_DOMAIN_MODULE_ID_NOT_APPROVED_OR_MISMATCH"),"null module");
    check(contains(good,n,"2",false,"CLT_R5A_CONSUMER_CLIENT_NOT_VERIFIED"),"analyst not consumer");
    check(contains(good,normalized("101","IRR","FULL","4"),"2",true,"CLT_R5A_REQUEST_EXCEEDS_ALLOCATION"),"over release");
    check(contains(good,normalized("90","IRR","FULL","4"),"2",true,"CLT_R5A_PARTIAL_UNALLOCATION_POLICY_NOT_APPROVED"),"partial amount");
    check(contains(good,normalized("100","IRR","PARTIAL","4"),"2",true,"CLT_R5A_PARTIAL_UNALLOCATION_POLICY_NOT_APPROVED"),"partial mode");
    check(contains(good,normalized("100","USD","FULL","4"),"2",true,"CLT_R5A_CURRENCY_MISMATCH_NO_FX_ASSUMPTION"),"foreign mismatch");
    check(contains(good,normalized("100","IRR","FULL","5"),"2",true,"CLT_R5A_ALLOCATION_VERSION_MISMATCH"),"stale ETag");
    check(contains(allocation(true,"100","IRR","2","4","666"),n,"2",true,"CLT_R5A_CONSUMER_FILE_NUMBER_MISMATCH"),"wrong case");
    invalid(()->CollateralUnallocationPolicy.version("\"0001\""),"reject noncanonical strong ETag");
    check(CollateralUnallocationPolicy.limit(1)==1,"min limit");
    check(CollateralUnallocationPolicy.limit(50)==50,"max limit");
    check(CollateralUnallocationPolicy.id("9999999999999999999").equals("9999999999999999999"),"number19");
    check(CollateralUnallocationPolicy.idempotencyKey("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa").length()==36,"uuid");
    check(CollateralUnallocationPolicy.normalize(new PreviewRequest("LOAN","567","1.2300","IRR","FULL","REQ-123"),"\"1\"").requestedAmount().equals("1.23"),"normalized decimal");
    var digest1=CollateralUnallocationPolicy.digest("99","123","11","analyst",n);
    var digest2=CollateralUnallocationPolicy.digest("99","123","11","analyst",n);
    check(digest1.equals(digest2),"stable digest");
    check(digest1.matches("[0-9a-f]{64}"),"digest sha256");
    check(!digest1.equals(CollateralUnallocationPolicy.digest("99","124","11","analyst",n)),"allocation bound");
    check(!digest1.equals(CollateralUnallocationPolicy.digest("98","123","11","analyst",n)),"collateral bound");
    check(!digest1.equals(CollateralUnallocationPolicy.digest("99","123","12","analyst",n)),"branch bound");
    check(!digest1.equals(CollateralUnallocationPolicy.digest("99","123","11","consumer",n)),"caller bound");
    check(!digest1.equals(CollateralUnallocationPolicy.digest("99","123","11","analyst",normalized("100","USD","FULL","4"))),"currency bound");
    check(!digest1.equals(CollateralUnallocationPolicy.digest("99","123","11","analyst",normalized("100","IRR","FULL","5"))),"version bound");
    for(String key:List.of("", "123", "a", "AAAAAAAA-AAAA-4AAA-8AAA-AAAAAAAAAAAA"))
      invalid(()->CollateralUnallocationPolicy.idempotencyKey(key),"UUID rejected "+key);
    for(String v:List.of("1","*","W/\"1\"","\"0\"","\"-1\"","\"12345678901\"","\"1.1\""))
      invalid(()->CollateralUnallocationPolicy.version(v),"ETag rejected "+v);
    for(String id:List.of("0","-1","1.0","12345678901234567890"," 1 "))
      invalid(()->CollateralUnallocationPolicy.id(id),"id rejected "+id);
    for(String amount:List.of("0","-1","1.12345","1,000","1e2","1.000000","99999999999999999"))
      invalid(()->CollateralUnallocationPolicy.normalize(new PreviewRequest("LOAN","567",amount,"IRR","FULL","REQ"),"\"1\""),"amount rejected "+amount);
    invalid(()->CollateralUnallocationPolicy.normalize(new PreviewRequest("LOAN","567","1","irr","FULL","REQ"),"\"1\""),"lower currency");
    invalid(()->CollateralUnallocationPolicy.normalize(new PreviewRequest("OTHER","567","1","IRR","FULL","REQ"),"\"1\""),"domain not allowlisted");
    invalid(()->CollateralUnallocationPolicy.normalize(new PreviewRequest("LOAN","0","1","IRR","FULL","REQ"),"\"1\""),"file zero");
    invalid(()->CollateralUnallocationPolicy.normalize(new PreviewRequest("LOAN","567","1","IRR","ALL","REQ"),"\"1\""),"mode unknown");
    invalid(()->CollateralUnallocationPolicy.normalize(new PreviewRequest("LOAN","567","1","IRR","FULL","white space"),"\"1\""),"reason whitespace");
    invalid(()->CollateralUnallocationPolicy.normalize(new PreviewRequest("LOAN","567","1","IRR","FULL","x".repeat(101)),"\"1\""),"reason too long");
    invalid(()->CollateralUnallocationPolicy.limit(0),"limit zero");
    invalid(()->CollateralUnallocationPolicy.limit(51),"limit >50");
    check(!CollateralUnallocationPolicy.blockers(good,n,"2",true).isEmpty(),"positive evidence no grant");
    check(!CollateralUnallocationPolicy.blockers(good,n,"2",true).stream().allMatch(s->!s.startsWith("CLT_R5A_")),"hard denials");
    System.out.println("CLT_R5A_UNALLOCATION_POLICY_SMOKE_PASS="+passed+"; ORACLE_EXECUTION=NONE");
  }
}
