import com.behsazan.corebanking.collateral.sponsor.CollateralSponsorPolicy;
import java.util.List;

/** Pure-JDK smoke: independent from Spring/Oracle/IdP and safe for offline execution. */
public class SponsorPolicySmoke {
    static int pass = 0;
    static void yes(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        pass++;
    }
    static boolean bad(Runnable work) {
        try { work.run(); return false; }
        catch (IllegalArgumentException expected) { return true; }
    }
    public static void main(String[] args) {
        var policy = CollateralSponsorPolicy.class;
        yes(CollateralSponsorPolicy.id("9999999999999999999").length() == 19, "NUMBER19_EXACT");
        yes(bad(() -> CollateralSponsorPolicy.id("1e3")), "NO_EXPONENT");
        yes(bad(() -> CollateralSponsorPolicy.id("01")), "NO_LEADING_ZERO");
        yes(bad(() -> CollateralSponsorPolicy.id("10000000000000000000")), "NO_OVERFLOW");
        yes(bad(() -> CollateralSponsorPolicy.id("0")), "NO_ZERO");
        yes(bad(() -> CollateralSponsorPolicy.id("-1")), "NO_NEGATIVE");
        var c = CollateralSponsorPolicy.candidate("15", null, "reason for staged request");
        yes(c.depositNumber() == null, "OPTIONAL_DEPOSIT");
        yes(c.justification().length() > 8, "REASON_REQUIRED");
        yes(bad(() -> CollateralSponsorPolicy.candidate("15", "invalid!'", "reason for staged request")), "DEPOSIT_FORMAT");
        yes(bad(() -> CollateralSponsorPolicy.candidate("15", null, "short")), "REASON_MIN");
        yes(bad(() -> CollateralSponsorPolicy.candidate("15", null, "x".repeat(501))), "REASON_MAX");
        yes(bad(() -> CollateralSponsorPolicy.key("short")), "IDEMPOTENCY_MIN");
        yes(CollateralSponsorPolicy.key("sponsor-request-12345").length() == 21, "IDEMPOTENCY_OK");
        yes(CollateralSponsorPolicy.correlation(null) == null, "CORRELATION_OPTIONAL");
        yes(bad(() -> CollateralSponsorPolicy.correlation("a b")), "CORRELATION_FORMAT");
        yes(CollateralSponsorPolicy.digest(c.canonical()).length() == 64, "DIGEST_LENGTH");
        yes(!CollateralSponsorPolicy.digest(c.canonical()).equals(CollateralSponsorPolicy.digest(c.canonical()+"x")), "DIGEST_DELTA");
        var ok = CollateralSponsorPolicy.blockers("11", "12", "ACTIVE", "Y", "ACTIVE", false, true);
        yes(ok.isEmpty(), "ACTIVE_SAME_BRANCH_VALID");
        yes(CollateralSponsorPolicy.blockers("11", "12", "PLEDGED", "Y", "ACTIVE", false, true).isEmpty(), "PLEDGED_ALLOWED");
        yes(CollateralSponsorPolicy.blockers("11", "12", "PARTIAL_RELEASE", "Y", "ACTIVE", false, true).isEmpty(), "PARTIAL_RELEASE_ALLOWED");
        var conflict = CollateralSponsorPolicy.blockers("11", "11", "RELEASED", "N", "ACTIVE", true, false);
        yes(conflict.size() == 5, "INDEPENDENT_BLOCKERS");
        yes(conflict.contains("CLT_SPONSOR_SAME_AS_COLLATERAL_OWNER"), "OWNER_DIFF");
        yes(conflict.contains("CLT_SPONSOR_ALREADY_REGISTERED"), "DUPLICATE");
        yes(conflict.contains("CLT_SPONSOR_PARTY_NOT_VERIFIED_BY_CIF"), "CIF_REQUIRED");
        yes(conflict.contains("CLT_SPONSOR_NOT_APPLICABLE_TO_TYPE"), "TYPE_APPLICABLE");
        yes(conflict.contains("CLT_SPONSOR_COLLATERAL_STATUS_NOT_ALLOWED"), "STATUS_GATE");
        yes(CollateralSponsorPolicy.blockers("11", "12", "ACTIVE", "Y", "RETIRED", false, true)
                .contains("CLT_SPONSOR_TYPE_NOT_ACTIVE"), "TYPE_ACTIVE");
        yes(!CollateralSponsorPolicy.class.getName().contains("R3A"), "STABLE_NAMING");
        // No maximum, no debtor/loan/case check in intake, no min-count invented.
        yes(policy.getDeclaredMethods().length >= 5, "POLICY_COMPILATION");
        System.out.println("CLT_R3A_SPONSOR_POLICY_SMOKE_PASS=" + pass);
    }
}
