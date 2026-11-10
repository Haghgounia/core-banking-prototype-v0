import com.behsazan.corebanking.collateral.uat.CollateralBusinessUatEvidencePolicy;
import com.behsazan.corebanking.collateral.uat.CollateralBusinessUatEvidencePolicy.Scenario;
import java.util.ArrayList;
import java.util.List;

/** Executable pure contract test, Java 21, no Spring or Oracle dependency. */
public class CollateralBusinessUatEvidenceSmoke {
    private static int checks;
    private static void yes(boolean value,String name) {if(!value)throw new AssertionError(name);checks++;}
    private static void rejects(Runnable action,String name) {
        try{action.run();throw new AssertionError("EXPECTED_FAILURE "+name);}
        catch(IllegalArgumentException ex){checks++;}
    }
    private static List<Scenario> input(){
        List<Scenario> result=new ArrayList<>();
        for(int i=1;i<=16;i++)for(String x:List.of("P","N"))
            result.add(new Scenario(String.format("BUC-%02d-%s",i,x),"NOT_RUN",""));
        return result;
    }
    public static void main(String[] args){
        List<Scenario> rows=input();
        yes(rows.size()==32,"catalog 32");
        List<Scenario> sorted=CollateralBusinessUatEvidencePolicy.normalize(rows);
        yes(sorted.size()==32,"normalize size");
        yes("BUC-01-N".equals(sorted.get(0).id()),"stable sort");
        yes("BUC-16-P".equals(sorted.get(31).id()),"stable tail");
        yes(CollateralBusinessUatEvidencePolicy.CASE_SET.startsWith("BUC"),"stable naming");
        yes(CollateralBusinessUatEvidencePolicy.CONTRACT.contains("ORACLE"),"stable schema contract");
        for(int i=1;i<=16;i++)for(String x:List.of("P","N")){
            String id=String.format("BUC-%02d-%s",i,x);
            yes(id.equals(CollateralBusinessUatEvidencePolicy.scenario(id)),"scenario "+id);
        }
        for(String bad:List.of("R6-BUC-01-P","BUC-17-N","BUC-00-P","BUC-01-UNKNOWN","BUC-1-P","BUC-01-X","", "BUC-01-P' OR 1=1"))
            rejects(()->CollateralBusinessUatEvidencePolicy.scenario(bad),"bad id "+bad);
        rejects(()->CollateralBusinessUatEvidencePolicy.scenario(null),"null id");
        for(String status:List.of("NOT_RUN","PASS","FAIL","BLOCKED"))
            yes(status.equals(CollateralBusinessUatEvidencePolicy.status(status)),"valid status");
        for(String bad:List.of("APPROVED","BANK_PASS","UNKNOWN","","PASS "))
            rejects(()->CollateralBusinessUatEvidencePolicy.status(bad),"bad status "+bad);
        rejects(()->CollateralBusinessUatEvidencePolicy.status(null),"null status");
        yes("شاهد تست".equals(CollateralBusinessUatEvidencePolicy.note("  شاهد تست  ")),"trim note");
        yes(CollateralBusinessUatEvidencePolicy.note("\nTest\t").equals("Test"),"allowed whitespace");
        yes(CollateralBusinessUatEvidencePolicy.note("x".repeat(280)).length()==280,"max note");
        rejects(()->CollateralBusinessUatEvidencePolicy.note("x".repeat(281)),"too long");
        rejects(()->CollateralBusinessUatEvidencePolicy.note(null),"null note");
        rejects(()->CollateralBusinessUatEvidencePolicy.note("abc\u0000"),"control char");
        yes(CollateralBusinessUatEvidencePolicy.revision(0L)==0,"first revision");
        yes(CollateralBusinessUatEvidencePolicy.revision(999_999_999L)==999_999_999L,"max revision");
        rejects(()->CollateralBusinessUatEvidencePolicy.revision(null),"null revision");
        rejects(()->CollateralBusinessUatEvidencePolicy.revision(-1L),"negative revision");
        rejects(()->CollateralBusinessUatEvidencePolicy.revision(1_000_000_000L),"too high revision");
        rejects(()->CollateralBusinessUatEvidencePolicy.normalize(null),"missing result array");
        rejects(()->CollateralBusinessUatEvidencePolicy.normalize(rows.subList(0,31)),"missing scenario");
        var repeated=new ArrayList<>(rows);repeated.set(31,rows.get(0));
        rejects(()->CollateralBusinessUatEvidencePolicy.normalize(repeated),"duplicate scenario");
        var unknown=new ArrayList<>(rows);unknown.set(31,new Scenario("BUC-99-N","PASS",""));
        rejects(()->CollateralBusinessUatEvidencePolicy.normalize(unknown),"unknown scenario");
        var invalid=new ArrayList<>(rows);invalid.set(31,new Scenario(rows.get(31).id(),"APPROVED",""));
        rejects(()->CollateralBusinessUatEvidencePolicy.normalize(invalid),"forged approved state");
        var tooLong=new ArrayList<>(rows);tooLong.set(31,new Scenario(rows.get(31).id(),"PASS","x".repeat(281)));
        rejects(()->CollateralBusinessUatEvidencePolicy.normalize(tooLong),"too long evidence");
        var nullRow=new ArrayList<>(rows);nullRow.set(31,null);
        rejects(()->CollateralBusinessUatEvidencePolicy.normalize(nullRow),"null scenario record");
        yes(CollateralBusinessUatEvidencePolicy.fillMissing(List.of()).size()==32,"reconstruct empty");
        yes(CollateralBusinessUatEvidencePolicy.fillMissing(List.of(new Scenario("BUC-08-P","FAIL","سبب")))
            .stream().anyMatch(r->"BUC-08-P".equals(r.id())&&"FAIL".equals(r.status())),"preserve saved result");
        yes(CollateralBusinessUatEvidencePolicy.normalize(rows).equals(CollateralBusinessUatEvidencePolicy.normalize(sorted)),"idempotent normalization");
        System.out.println("CLT_BUC_SUC_UAT_ORACLE_POLICY_PASS="+checks+"/"+checks);
    }
}
