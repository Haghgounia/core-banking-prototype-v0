import com.behsazan.corebanking.fee3.application.Fee3DraftPolicy;
import com.behsazan.corebanking.fee3.application.Fee3Problem;
import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class Fee3R2PolicyProbe {
    private static final LocalDate TODAY=LocalDate.of(2026,10,26);
    private static int ok=0;
    static DraftRequest draft(String service,String fee,Map<String,Object> ui,Map<String,Object> finance) {
        return new DraftRequest(null,null,null,null,
            new ItemDraft("کارمزد آزمایشی","DEPOSIT","ACCOUNT_OPENING","SERVICE_FEE",null,"TARIFF",null),
            new VersionDraft(TODAY,null,"BANK_INTERNAL",null,"FIXED","UNREVIEWED","HALF_UP",null,finance,service,fee,ui),
            new ScopeDraft(List.of("BRANCH"),List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of(),"IRR"),
            List.of(),List.of());
    }
    static void pass(DraftRequest request) {
        Fee3DraftPolicy.check(request,TODAY,List.of("SERVICE_FEE"));ok++;
    }
    static void fail(DraftRequest request) {
        try{Fee3DraftPolicy.check(request,TODAY,List.of("SERVICE_FEE"));throw new AssertionError("Expected rejection");}
        catch(Fee3Problem ex){ok++;}
    }
    public static void main(String[] args){
        pass(draft("IRR","IRR",Map.of(),Map.of()));
        pass(draft("IRR","IRR",Map.of("loanPolicy",Map.of("dayCountBasis","ACT_365")),Map.of()));
        fail(draft("EUR","IRR",Map.of(),Map.of()));
        fail(draft("IRR","EUR",Map.of(),Map.of()));
        fail(draft("IRR","IRR",Map.of("anyNewKey",true),Map.of()));
        fail(draft("IRR","IRR",Map.of(),Map.of("_fee3UiV1",Map.of("contractVersion",1))));
        fail(draft("IRR","IRR",null,Map.of()));
        fail(draft("IRR","IRR",Map.of(),Map.of("deals",List.of(Map.of("from","2026-10-25")))));
        fail(draft("IRR","IRR",Map.of(),Map.of("discountGroups",List.of(Map.of("from","2026-10-27","to","2026-10-26")))));
        pass(draft("IRR","IRR",Map.of(),Map.of("discountGroups",List.of(Map.of("from","2026-10-27","to","2026-10-29")))));
        System.out.println("FEE3_BE_R2_POLICY_PASS="+ok);
    }
}
