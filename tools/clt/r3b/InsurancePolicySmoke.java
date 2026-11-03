import com.behsazan.corebanking.collateral.insurance.CollateralInsuranceModels.CandidateRequest;
import com.behsazan.corebanking.collateral.insurance.CollateralInsurancePolicy;
import java.util.List;

/** Run by plain Java 21. This is a contract-level smoke, not an Oracle integration test. */
public final class InsurancePolicySmoke {
    private static int passed;
    private static void check(boolean ok, String description) {
        if (!ok) throw new AssertionError("FAILED:"+description);
        passed++;
    }
    private static void rejects(Runnable r, String code) {
        try {r.run(); throw new AssertionError("EXPECTED_ERROR:"+code);}
        catch (IllegalArgumentException e) {check(e.getMessage().startsWith("CLT_INSURANCE_"),code);}
    }
    public static void main(String[] args) {
        var c = new CandidateRequest("2","9999999999999999999","1234", "D-123", "POL-123",
                "9999999999999999.9999","200.0000","1.01","2026-11-03","2027-11-03",
                null,"IRR","درخواست بیمه جدید");
        var n = CollateralInsurancePolicy.candidate(c);
        check(n.companyId().equals("9999999999999999999"),"id19");
        check(n.insuranceAmount().equals("9999999999999999.9999"),"amount20");
        check(n.receivedAmount().equals("200"),"received decimal");
        check(n.taxAmount().equals("1.01"),"tax decimal");
        check(n.validFrom().equals("2026-11-03"),"dateISO");
        check(n.validTo().equals("2027-11-03"),"dateToISO");
        check(n.insurerCode().equals("2"),"customer issuance only");
        check(n.canonical().contains("POL-123"),"payload canonical");
        check(CollateralInsurancePolicy.digest(n.canonical()).length()==64,"sha256 len");
        check(!CollateralInsurancePolicy.digest(n.canonical()).equals(
                CollateralInsurancePolicy.digest(n.canonical()+"x")),"payload fingerprint");
        check(CollateralInsurancePolicy.overlaps("2026-01-01","2026-01-31","2026-01-31","2026-02-10"),"inclusive overlap");
        check(!CollateralInsurancePolicy.overlaps("2026-01-01","2026-01-31","2026-02-01","2026-02-28"),"adjacent allowed");
        check(CollateralInsurancePolicy.validRenewal("5","7","5","7","2026-11-02","2026-11-03",false),"renewal next day");
        check(!CollateralInsurancePolicy.validRenewal("5","7","5","7","2026-11-02","2026-11-04",false),"renewal gap rejected");
        check(!CollateralInsurancePolicy.validRenewal("5","7","5","8","2026-11-02","2026-11-03",false),"renewal type");
        check(!CollateralInsurancePolicy.validRenewal("5","7","4","7","2026-11-02","2026-11-03",false),"renewal collateral");
        check(!CollateralInsurancePolicy.validRenewal("5","7","5","7","2026-11-02","2026-11-03",true),"renewal already used");
        check(CollateralInsurancePolicy.blockers(true,true,"ACTIVE",true,true,true,true,false,false,true).isEmpty(),"staging valid");
        check(CollateralInsurancePolicy.blockers(true,true,"PLEDGED",true,true,true,true,false,false,true).isEmpty(),"pledged valid");
        check(CollateralInsurancePolicy.blockers(true,true,"PARTIAL_RELEASE",true,true,true,true,false,false,true).isEmpty(),"partial release valid");
        check(CollateralInsurancePolicy.blockers(true,true,"RELEASED",true,true,true,true,false,false,true).size()==1,"released denied");
        check(CollateralInsurancePolicy.blockers(false,true,"ACTIVE",true,true,true,true,false,false,true).size()==1,"non insurable denied");
        check(CollateralInsurancePolicy.blockers(true,false,"ACTIVE",true,true,true,true,false,false,true).size()==1,"retired type denied");
        check(CollateralInsurancePolicy.blockers(true,true,"ACTIVE",false,true,true,true,false,false,true).size()==1,"company inactive");
        check(CollateralInsurancePolicy.blockers(true,true,"ACTIVE",true,false,true,true,false,false,true).size()==1,"insurance type inactive");
        check(CollateralInsurancePolicy.blockers(true,true,"ACTIVE",true,true,false,true,false,false,true).size()==1,"currency inactive");
        check(CollateralInsurancePolicy.blockers(true,true,"ACTIVE",true,true,true,true,true,false,true).size()==1,"duplicate denied");
        check(CollateralInsurancePolicy.blockers(true,true,"ACTIVE",true,true,true,true,false,true,true).size()==1,"overlap denied");
        check(CollateralInsurancePolicy.blockers(true,true,"ACTIVE",true,true,true,true,false,false,false).size()==1,"invalid renewal denied");
        check(CollateralInsurancePolicy.blockers(true,true,"ACTIVE",true,true,true,false,false,false,true)
                .contains("CLT_INSURANCE_OWNER_PARTY_NOT_VERIFIED_BY_CIF"),"CIF owner invalid");
        rejects(()->CollateralInsurancePolicy.id("0001"),"leading zeros");
        rejects(()->CollateralInsurancePolicy.id("10000000000000000000"),"id overflow");
        rejects(()->CollateralInsurancePolicy.id("1e9"),"id scientific");
        rejects(()->CollateralInsurancePolicy.amount("-1",true,false),"negative amount");
        rejects(()->CollateralInsurancePolicy.amount("0",true,false),"zero cover");
        rejects(()->CollateralInsurancePolicy.amount("1.12345",true,false),"scale > 4");
        rejects(()->CollateralInsurancePolicy.amount("10000000000000000",true,false),"integer digits >16");
        rejects(()->CollateralInsurancePolicy.amount("10e5",true,false),"scientific reject");
        rejects(()->CollateralInsurancePolicy.key("a"),"short idem");
        rejects(()->CollateralInsurancePolicy.candidate(new CandidateRequest("1",c.companyId(),c.insuranceTypeId(),
                c.depositNumber(),c.insuranceNumber(),c.insuranceAmount(),c.receivedAmount(),c.taxAmount(),
                c.validFrom(),c.validTo(),null,"IRR",c.justification())),"bank-issued denied");
        rejects(()->CollateralInsurancePolicy.candidate(new CandidateRequest("2",c.companyId(),c.insuranceTypeId(),
                c.depositNumber(),c.insuranceNumber(),c.insuranceAmount(),c.receivedAmount(),c.taxAmount(),
                c.validFrom(),c.validFrom(),null,"IRR",c.justification())),"equal dates");
        rejects(()->CollateralInsurancePolicy.candidate(new CandidateRequest("2",c.companyId(),c.insuranceTypeId(),
                c.depositNumber(),c.insuranceNumber(),c.insuranceAmount(),c.receivedAmount(),c.taxAmount(),
                "2026-02-30",c.validTo(),null,"IRR",c.justification())),"invalid date");
        System.out.println("CLT_R3B_INSURANCE_POLICY_SMOKE_PASS="+passed);
    }
}
