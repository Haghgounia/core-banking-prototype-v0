import com.behsazan.corebanking.iam.s1b.IamS1bPolicy;
import java.util.List;

/** Offline executable negative tests; no Spring, Oracle, JWT signing or network needed. */
public class PolicySelfTest {
    private static int n = 0;
    static void check(boolean condition, String detail) {
        if (!condition) throw new AssertionError("IAM_S1B_POLICY_FAIL " + detail);
        n++;
    }
    public static void main(String[] args) {
        var scope = List.of("iam.users.read");
        var domains = List.of("WF_BANK", "WF_BRANCH");
        var contexts = List.of("WORKFORCE");
        check(IamS1bPolicy.canRead("sub", scope, domains, contexts, "WF_BANK", "WORKFORCE"), "allowed signed claims");
        check(!IamS1bPolicy.canRead("sub", scope, domains, contexts, "CUSTOMER", "WORKFORCE"), "cross domain");
        check(!IamS1bPolicy.canRead("sub", scope, domains, contexts, "WF_BANK", "CUSTOMER"), "cross context");
        check(!IamS1bPolicy.canRead(null, scope, domains, contexts, "WF_BANK", "WORKFORCE"), "no actor");
        check(!IamS1bPolicy.canRead(" ", scope, domains, contexts, "WF_BANK", "WORKFORCE"), "blank actor");
        check(!IamS1bPolicy.canRead("sub", List.of(), domains, contexts, "WF_BANK", "WORKFORCE"), "no scope");
        check(!IamS1bPolicy.canRead("sub", List.of("iam.users.write"), domains, contexts, "WF_BANK", "WORKFORCE"), "wrong scope");
        check(!IamS1bPolicy.canRead("sub", scope, List.of("*"), contexts, "WF_BANK", "WORKFORCE"), "wildcard domain");
        check(!IamS1bPolicy.canRead("sub", scope, domains, List.of("*"), "WF_BANK", "WORKFORCE"), "wildcard context");
        check(!IamS1bPolicy.canRead("sub", scope, null, contexts, "WF_BANK", "WORKFORCE"), "null domains");
        check(!IamS1bPolicy.canRead("sub", scope, domains, null, "WF_BANK", "WORKFORCE"), "null contexts");
        check(IamS1bPolicy.safeStringClaim("WF_BANK").isEmpty(), "string not trusted as collection");
        check(IamS1bPolicy.safeStringClaim(List.of("WF_BANK", 4)).isEmpty(), "mixed list denied");
        check(IamS1bPolicy.safeStringClaim(List.of("WF_BANK")).equals(List.of("WF_BANK")), "valid list");
        check("%a!%!_!!%".equals(IamS1bPolicy.escapedLike("a%_!")), "LIKE escaping");
        check("user".equals(IamS1bPolicy.normalizedUsernameFragment(" USER ")), "username lower");
        check("é".equals(IamS1bPolicy.normalizedUsernameFragment("E\u0301")), "NFKC normalization");
        check(IamS1bPolicy.normalizedUsernameFragment("   ") == null, "blank search");
        check("WF_BANK".equals(IamS1bPolicy.domainCode("WF_BANK")), "valid domain");
        try { IamS1bPolicy.domainCode("OR 1=1"); throw new AssertionError("injection"); }
        catch (IllegalArgumentException expected) { n++; }
        try { IamS1bPolicy.normalizedUsernameFragment("line\nbreak"); throw new AssertionError("control"); }
        catch (IllegalArgumentException expected) { n++; }
        System.out.println("IAM_S1B_POLICY_JAVA_PASS=" + n + "/" + n + "; NO_JWT_BYPASS; NO_ORACLE_IO");
    }
}
