import com.behsazan.corebanking.collateral.introduction.CollateralNumberingFeasibilityPolicy;

/** Executable smoke for exactly the production G17 policy class, no Spring/Oracle. */
public final class PolicySmoke {
    private static int n;
    private static void require(boolean b, String msg) {
        if (!b) throw new AssertionError("G17_SMOKE_FAIL:"+msg);
        n++;
    }
    public static void main(String[] args) {
        var exact = CollateralNumberingFeasibilityPolicy.assess("12", "123456789", true, 19);
        require(exact.requiredDigits()==19, "exact-number19");
        require(exact.physicallyRepresentable(), "physical-shape-only");
        require(!exact.policyApproved(), "issuer-stays-disabled");
        require(exact.blockers().contains("G17_SCHEME_NOT_APPROVED_NO_FINAL_ISSUANCE"), "unapproved");
        require(exact.spareDigits()==0, "remaining-space");
        require(CollateralNumberingFeasibilityPolicy.assess("1234", "12345678", true, 19)
          .blockers().contains("G17_NUMBER19_PHYSICAL_OVERFLOW"), "overflow");
        require(!CollateralNumberingFeasibilityPolicy.assess("012", "12345", true, 19)
          .physicallyRepresentable(), "leading-zero-Oracle-number");
        require(!CollateralNumberingFeasibilityPolicy.assess("X", "123", true, 19)
          .physicallyRepresentable(), "nonnumeric-type");
        require(!CollateralNumberingFeasibilityPolicy.assess("22", "CIF-1", true, 19)
          .physicallyRepresentable(), "nonnumeric-cif");
        require(!CollateralNumberingFeasibilityPolicy.assess("22", null, false, 19)
          .physicallyRepresentable(), "not-party-id-substitution");
        require(!CollateralNumberingFeasibilityPolicy.assess("22", "123", true, -1)
          .physicalColumnVerified(), "missing-oracle-metadata");
        require(CollateralNumberingFeasibilityPolicy.assess("22", "123", true, 18)
          .blockers().contains("G17_ORACLE_COLLATERAL_NUMBER_COLUMN_MISMATCH"), "physical-drift");
        require(CollateralNumberingFeasibilityPolicy.PHYSICAL_DIGITS==19, "oracle-contract");
        require(CollateralNumberingFeasibilityPolicy.SEQUENCE_DIGITS==3, "proposal-sequence-only");
        System.out.println("CLT_R2_G14_G17_JAVA_SMOKE_PASS="+n+"/"+n);
    }
}
