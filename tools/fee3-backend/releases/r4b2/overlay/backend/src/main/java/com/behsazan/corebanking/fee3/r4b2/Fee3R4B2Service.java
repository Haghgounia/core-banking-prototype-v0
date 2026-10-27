package com.behsazan.corebanking.fee3.r4b2;

import com.behsazan.corebanking.fee3.application.Fee3DraftPolicy;
import com.behsazan.corebanking.fee3.application.Fee3Problem;
import com.behsazan.corebanking.fee3.oracle.Fee3Repository;
import com.behsazan.corebanking.fee3.r3.Fee3SubmissionPolicy;
import com.behsazan.corebanking.fee3.r4.Fee3R4ReferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

/** Read-only preflight + Oracle draft snapshot proof. Generic R3 Maker/Checker is NOT enabled. */
@Service
public class Fee3R4B2Service {
    private final Fee3Repository oracle;
    private final Fee3R4ReferenceRepository reference;
    public Fee3R4B2Service(Fee3Repository oracle,Fee3R4ReferenceRepository reference){
        this.oracle=oracle;this.reference=reference;
    }
    @Transactional(readOnly=true)
    public Fee3R4B2GuaranteeContract.Evidence preview(Fee3R4B2GuaranteeContract.Request req){
        LocalDate day=oracle.bankDate();
        String violation=Fee3R4B2GuaranteeContract.guard(req,day);
        if(violation!=null)return Fee3R4B2GuaranteeContract.blocked(req,day,violation);
        try {
            var draft=req.draft();
            var operation=oracle.activeOperation(draft.item().module(),draft.item().operation());
            if(operation.isEmpty())return Fee3R4B2GuaranteeContract.blocked(req,day,
                "ACTIVE_ORACLE_GUARANTEE_OPERATION_REQUIRED");
            // Full R3 business checks (including active catalog/nullable scope semantics) remain enforced.
            Fee3DraftPolicy.check(draft,day,operation.get().allowedAmountKinds());
            // The generic R3 check assumes a *single* rate for PERCENT, which does not hold for
            // guarantee portfolio per-collateral rates. Only this precisely identified issue
            // is exempted here, and the specialized contract must already have proved every row.
            var governance=Fee3SubmissionPolicy.check(draft);
            boolean portfolio="MIXED_PORTFOLIO".equals(((java.util.Map<?,?>)
                draft.version().uiContext().get("guaranteePolicy")).get("collateralType"));
            List<Fee3SubmissionPolicy.Issue> unresolved=governance.issues().stream().filter(issue->
                !(portfolio&&"PERCENT".equals(draft.version().method())&&
                   "RULE_RATE_REQUIRED".equals(issue.code()))).toList();
            if(!unresolved.isEmpty())return Fee3R4B2GuaranteeContract.blocked(req,day,
                "R3_PREFLIGHT_"+unresolved.get(0).code());
            if(draft.version().sourceId()!=null){
                String status=oracle.sourceStatus(draft.version().sourceId());
                if(status==null || "CBI_MANDATORY".equals(draft.version().origin())&&
                   !"VERIFIED".equals(status))
                    return Fee3R4B2GuaranteeContract.blocked(req,day,
                        "SOURCE_NOT_FOUND_OR_REGULATORY_SOURCE_UNVERIFIED");
            }
            var version=reference.resolve(draft);
            var result=Fee3R4B2GuaranteeContract.calculate(req);
            return Fee3R4B2GuaranteeContract.finish(req,day,version,result);
        }catch(Fee3Problem ex){
            return Fee3R4B2GuaranteeContract.blocked(req,day,"BUSINESS_"+ex.errorCode());
        }catch(IllegalArgumentException|ArithmeticException ex){
            String code=ex.getMessage();
            return Fee3R4B2GuaranteeContract.blocked(req,day,
                code!=null&&code.matches("[A-Z0-9_]{3,100}")?code:"R4B2_UNVERIFIED_CALCULATION");
        }
    }
}
