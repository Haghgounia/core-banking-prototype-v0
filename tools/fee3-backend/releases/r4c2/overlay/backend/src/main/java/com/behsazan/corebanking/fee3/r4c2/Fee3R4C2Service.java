package com.behsazan.corebanking.fee3.r4c2;

import com.behsazan.corebanking.fee3.application.Fee3DraftPolicy;
import com.behsazan.corebanking.fee3.application.Fee3Problem;
import com.behsazan.corebanking.fee3.oracle.Fee3Repository;
import com.behsazan.corebanking.fee3.r3.Fee3SubmissionPolicy;
import com.behsazan.corebanking.fee3.r4.Fee3R4ReferenceRepository;
import com.behsazan.corebanking.fee3.r4c1.Fee3R4C1JalaliDayRepository;
import com.behsazan.corebanking.fee3.r4c1.Fee3R4C1LoanProfitContract;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

/** Only GET-equivalent reference checks plus a nonpersistent computational POST. */
@Service
public class Fee3R4C2Service {
    private final Fee3Repository oracle;
    private final Fee3R4ReferenceRepository references;
    private final Fee3R4C1JalaliDayRepository calendar;
    public Fee3R4C2Service(Fee3Repository oracle,Fee3R4ReferenceRepository references,
                           Fee3R4C1JalaliDayRepository calendar) {
        this.oracle=oracle;this.references=references;this.calendar=calendar;
    }
    @Transactional(readOnly=true)
    public Fee3R4C2LoanModelContract.Evidence preview(Fee3R4C2LoanModelContract.Request req) {
        LocalDate bankDay=oracle.bankDate();
        String error=Fee3R4C2LoanModelContract.guard(req,bankDay);
        if(error!=null)return Fee3R4C2LoanModelContract.blocked(req,bankDay,error);
        try {
            var draft=req.draft();var spec=Fee3R4C2LoanModelContract.spec(req);
            var op=oracle.activeOperation(draft.item().module(),draft.item().operation());
            if(op.isEmpty()||!op.get().allowedAmountKinds().contains("FINANCING_PROFIT"))
                return Fee3R4C2LoanModelContract.blocked(req,bankDay,"ORACLE_OPERATION_NOT_ACTIVE");
            Fee3DraftPolicy.check(draft,bankDay,op.get().allowedAmountKinds());
            var report=Fee3SubmissionPolicy.check(draft);
            if(!report.submissionReady())return Fee3R4C2LoanModelContract.blocked(req,bankDay,
                "R3_PREFLIGHT_"+(report.issues().isEmpty()?"INCOMPLETE":report.issues().get(0).code()));
            // Active formula CODE is a whitelisted name, never an executable expression.
            if(oracle.formulas().stream().noneMatch(f->spec.formula().equals(f.code())&&
                    f.active()&&"FORMULA".equals(f.method())))
                return Fee3R4C2LoanModelContract.blocked(req,bankDay,"ORACLE_FORMULA_CODE_INACTIVE");
            if(draft.version().sourceId()!=null&&
               !"VERIFIED".equals(oracle.sourceStatus(draft.version().sourceId())))
                return Fee3R4C2LoanModelContract.blocked(req,bankDay,"DRAFT_SOURCE_NOT_VERIFIED");
            var versionProof=references.resolve(draft);
            List<Fee3R4C1LoanProfitContract.YearSlice> slices=List.of();
            if(spec.estimated()){
                LocalDate from=LocalDate.parse((String)req.inputs().get("periodStart"));
                LocalDate end=LocalDate.parse((String)req.inputs().get("periodEndExclusive"));
                slices=calendar.yearSlices(from,end);
            }
            var result=Fee3R4C2LoanModelContract.calculate(req,slices);
            return Fee3R4C2LoanModelContract.finish(req,bankDay,versionProof,result);
        }catch(Fee3Problem ex){
            return Fee3R4C2LoanModelContract.blocked(req,bankDay,"BUSINESS_"+ex.errorCode());
        }catch(DataAccessException ex){
            return Fee3R4C2LoanModelContract.blocked(req,bankDay,"ORACLE_CAL2_REFERENCE_UNAVAILABLE");
        }catch(IllegalArgumentException|ArithmeticException ex){
            String code=ex.getMessage();
            return Fee3R4C2LoanModelContract.blocked(req,bankDay,
                code!=null&&code.matches("[A-Z0-9_]{3,100}")?code:"CALCULATION_REJECTED");
        }
    }
}
