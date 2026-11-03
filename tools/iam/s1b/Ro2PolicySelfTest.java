import com.behsazan.corebanking.iam.s1b.IamS1bOracleReadPolicy;
import com.behsazan.corebanking.iam.s1b.IamS1bTokenClaimPolicy;
import java.util.*;

/** No framework, secret, JWT signing key or DB required. Pure negative-domain contract checks. */
public class Ro2PolicySelfTest {
  private static int checks;
  private static void check(String label, boolean value) {
    if (!value) throw new IllegalStateException("FAIL " + label);
    checks++;
    System.out.println("PASS RO2_" + checks + " " + label);
  }
  private static Map<String,Object> claims(Object domains,Object contexts) {
    return Map.of("iam_domains",domains,"iam_contexts",contexts);
  }
  public static void main(String[] args) {
    var valid=claims(List.of("DEMO_WF","INTERNAL"),List.of("WORKFORCE"));
    check("well formed signed-grant claims", IamS1bTokenClaimPolicy.validClaims(valid));
    check("empty claim arrays do not fabricate grants",IamS1bTokenClaimPolicy.validClaims(claims(List.of(),List.of())));
    check("missing claim denied",!IamS1bTokenClaimPolicy.validClaims(Map.of("iam_domains",List.of("DEMO_WF"))));
    check("claim must be list",!IamS1bTokenClaimPolicy.validClaims(claims("DEMO_WF",List.of("WORKFORCE"))));
    check("no wildcard domains",!IamS1bTokenClaimPolicy.validClaims(claims(List.of("*"),List.of("WORKFORCE"))));
    check("no wildcard context",!IamS1bTokenClaimPolicy.validClaims(claims(List.of("DEMO_WF"),List.of("*"))));
    check("no lowercase domain",!IamS1bTokenClaimPolicy.validClaims(claims(List.of("demo_wf"),List.of("WORKFORCE"))));
    check("no lowercase context",!IamS1bTokenClaimPolicy.validClaims(claims(List.of("DEMO_WF"),List.of("workforce"))));
    check("duplicate domain rejected",!IamS1bTokenClaimPolicy.validClaims(claims(List.of("DEMO_WF","DEMO_WF"),List.of("WORKFORCE"))));
    check("duplicate context rejected",!IamS1bTokenClaimPolicy.validClaims(claims(List.of("DEMO_WF"),List.of("WORKFORCE","WORKFORCE"))));
    check("null input rejected",!IamS1bTokenClaimPolicy.validClaims(null));
    check("no numeric domain",!IamS1bTokenClaimPolicy.validClaims(claims(List.of(1001),List.of("WORKFORCE"))));
    check("no whitespace domain",!IamS1bTokenClaimPolicy.validClaims(claims(List.of("DEMO WF"),List.of("WORKFORCE"))));
    check("no Unicode homoglyph",!IamS1bTokenClaimPolicy.validClaims(claims(List.of("DEMО_WF"),List.of("WORKFORCE"))));
    check("no oversized grants",!IamS1bTokenClaimPolicy.validClaims(claims(Collections.nCopies(33,"DEMO_WF"),List.of("WORKFORCE"))));
    var grants=IamS1bOracleReadPolicy.TABLES.stream().map(table->
            new IamS1bOracleReadPolicy.Grant("IAM",table,"SELECT")).toList();
    check("four direct IAM SELECT grants and CREATE SESSION",IamS1bOracleReadPolicy.leastPrivilege(List.of("CREATE SESSION"),List.of(),grants));
    check("READ privilege accepted",IamS1bOracleReadPolicy.leastPrivilege(List.of("CREATE SESSION"),List.of(),
            IamS1bOracleReadPolicy.TABLES.stream().map(t->new IamS1bOracleReadPolicy.Grant("IAM",t,"READ")).toList()));
    check("no CREATE SESSION rejected",!IamS1bOracleReadPolicy.leastPrivilege(List.of(),List.of(),grants));
    check("elevated system privilege denied",!IamS1bOracleReadPolicy.leastPrivilege(List.of("CREATE SESSION","UPDATE ANY TABLE"),List.of(),grants));
    check("role-enabled reader denied",!IamS1bOracleReadPolicy.leastPrivilege(List.of("CREATE SESSION"),List.of("CONNECT"),grants));
    check("one table missing rejected",!IamS1bOracleReadPolicy.leastPrivilege(List.of("CREATE SESSION"),List.of(),grants.subList(0,3)));
    var withUpdate=new ArrayList<>(grants);withUpdate.add(new IamS1bOracleReadPolicy.Grant("IAM","USER_ACCOUNTS","UPDATE"));
    check("table UPDATE grant denied",!IamS1bOracleReadPolicy.leastPrivilege(List.of("CREATE SESSION"),List.of(),withUpdate));
    var withOther=new ArrayList<>(grants);withOther.add(new IamS1bOracleReadPolicy.Grant("CIF","PARTIES","SELECT"));
    check("cross-schema grant denied",!IamS1bOracleReadPolicy.leastPrivilege(List.of("CREATE SESSION"),List.of(),withOther));
    var withOtherIam=new ArrayList<>(grants);withOtherIam.add(new IamS1bOracleReadPolicy.Grant("IAM","ACCESS_ROLES","SELECT"));
    check("unapproved additional IAM table denied",!IamS1bOracleReadPolicy.leastPrivilege(List.of("CREATE SESSION"),List.of(),withOtherIam));
    check("null collections denied",!IamS1bOracleReadPolicy.leastPrivilege(null,List.of(),grants));
    check("empty grants denied",!IamS1bOracleReadPolicy.leastPrivilege(List.of("CREATE SESSION"),List.of(),List.of()));
    System.out.println("IAM_S1B_RO2_PURE_POLICY_PASS="+checks+"/"+checks+"; NO_ORACLE_IO; NO_PRIVATE_KEYS");
  }
}
