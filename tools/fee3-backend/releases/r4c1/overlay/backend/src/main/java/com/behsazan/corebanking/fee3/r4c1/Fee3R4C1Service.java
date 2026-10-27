package com.behsazan.corebanking.fee3.r4c1;

import com.behsazan.corebanking.fee3.application.Fee3DraftPolicy;
import com.behsazan.corebanking.fee3.application.Fee3Problem;
import com.behsazan.corebanking.fee3.oracle.Fee3Repository;
import com.behsazan.corebanking.fee3.r3.Fee3SubmissionPolicy;
import com.behsazan.corebanking.fee3.r4.Fee3R4ReferenceRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class Fee3R4C1Service {
    private final Fee3Repository oracle;
    private final Fee3R4ReferenceRepository references;
    private final Fee3R4C1JalaliDayRepository calendar;
    public Fee3R4C1Service(Fee3Repository oracle,Fee3R4ReferenceRepository refs,
                           Fee3R4C1JalaliDayRepository calendar){
        this.oracle=oracle;this.references=refs;this.calendar=calendar;
    }
    @Transactional(readOnly=true)
    public Fee3R4C1LoanProfitContract.Evidence preview(Fee3R4C1LoanProfitContract.Request request){
        LocalDate day=oracle.bankDate();
        String violation=Fee3R4C1LoanProfitContract.guard(request,day);
        if(violation!=null)return Fee3R4C1LoanProfitContract.blocked(request,day,violation);
        try{
            var draft=request.draft();
            var operation=oracle.activeOperation(draft.item().module(),draft.item().operation());
            if(operation.isEmpty()||!operation.get().allowedAmountKinds().contains("FINANCING_PROFIT"))
                return Fee3R4C1LoanProfitContract.blocked(request,day,"ORACLE_LOAN_OPERATION_NOT_ACTIVE");
            Fee3DraftPolicy.check(draft,day,operation.get().allowedAmountKinds());
            var governance=Fee3SubmissionPolicy.check(draft);
            if(!governance.submissionReady()){
                String first=governance.issues().isEmpty()?"R3_PREFLIGHT_REQUIRED":
                    "R3_PREFLIGHT_"+governance.issues().get(0).code();
                return Fee3R4C1LoanProfitContract.blocked(request,day,first);
            }
            // The formula library is an allowlist of static approved-for-UAT semantics only.
            // Never execute FORMULA_EXPRESSION or any arbitrary expression provided by the client.
            String formula=draft.rules().get(0).formulaCode();
            if(oracle.formulas().stream().noneMatch(f->f.code().equals(formula)&&f.active()&&
                "FORMULA".equals(f.method())))
                return Fee3R4C1LoanProfitContract.blocked(request,day,"ORACLE_FORMULA_REFERENCE_NOT_ACTIVE");
            if(draft.version().sourceId()!=null&&
                !"VERIFIED".equals(oracle.sourceStatus(draft.version().sourceId())))
                return Fee3R4C1LoanProfitContract.blocked(request,day,"DRAFT_SOURCE_NOT_VERIFIED");
            var proof=references.resolve(draft);
            List<Fee3R4C1LoanProfitContract.YearSlice> slices=List.of();
            if("JALALI_ACT_ACT".equals(((java.util.Map<?,?>)draft.version().uiContext()
                .get("loanPolicy")).get("dayCountBasis"))){
                LocalDate first=LocalDate.parse((String)request.inputs().get("periodStart"));
                LocalDate end=LocalDate.parse((String)request.inputs().get("periodEndExclusive"));
                slices=calendar.yearSlices(first,end);
            }
            var calculation=Fee3R4C1LoanProfitContract.calculate(request,slices);
            return Fee3R4C1LoanProfitContract.finish(request,day,proof,calculation);
        }catch(Fee3Problem ex){
            return Fee3R4C1LoanProfitContract.blocked(request,day,"BUSINESS_"+ex.errorCode());
        }catch(DataAccessException ex){
            // Do not leak SQL, identifiers, or connection metadata in API error details.
            return Fee3R4C1LoanProfitContract.blocked(request,day,"ORACLE_CAL2_READONLY_REFERENCE_UNAVAILABLE");
        }catch(IllegalArgumentException|ArithmeticException ex){
            String code=ex.getMessage();
            return Fee3R4C1LoanProfitContract.blocked(request,day,
                code!=null&&code.matches("[A-Z0-9_]{3,100}")?code:"R4C1_CALCULATION_UNVERIFIED");
        }
    }
}
