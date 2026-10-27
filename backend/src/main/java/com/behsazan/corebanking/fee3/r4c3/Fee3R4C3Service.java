package com.behsazan.corebanking.fee3.r4c3;

import com.behsazan.corebanking.fee3.application.Fee3DraftPolicy;
import com.behsazan.corebanking.fee3.application.Fee3Problem;
import com.behsazan.corebanking.fee3.oracle.Fee3Repository;
import com.behsazan.corebanking.fee3.r3.Fee3SubmissionPolicy;
import com.behsazan.corebanking.fee3.r4.Fee3R4ReferenceRepository;
import com.behsazan.corebanking.fee3.r4c1.Fee3R4C1JalaliDayRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

/** Read-only Qard fee calculation. No loan schedule/history/collection mutation. */
@Service
public class Fee3R4C3Service {
    private final Fee3Repository oracle;
    private final Fee3R4ReferenceRepository references;
    private final Fee3R4C1JalaliDayRepository dayRepo;
    private final Fee3R4C3JalaliEventRepository eventRepo;
    public Fee3R4C3Service(Fee3Repository oracle,Fee3R4ReferenceRepository references,
       Fee3R4C1JalaliDayRepository dayRepo,Fee3R4C3JalaliEventRepository eventRepo){
        this.oracle=oracle;this.references=references;this.dayRepo=dayRepo;this.eventRepo=eventRepo;
    }
    @Transactional(readOnly=true)
    public Fee3R4C3QardContract.Evidence preview(Fee3R4C3QardContract.Request request){
        LocalDate bankDate=oracle.bankDate();
        String error=Fee3R4C3QardContract.guard(request,bankDate);
        if(error!=null)return Fee3R4C3QardContract.blocked(request,bankDate,error);
        try{
            var draft=request.draft();
            var op=oracle.activeOperation("LOAN","QARD_FEE");
            if(op.isEmpty()||!op.get().allowedAmountKinds().contains("QARD_FEE"))
                return Fee3R4C3QardContract.blocked(request,bankDate,"ORACLE_QARD_OPERATION_NOT_ACTIVE");
            Fee3DraftPolicy.check(draft,bankDate,op.get().allowedAmountKinds());
            var governance=Fee3SubmissionPolicy.check(draft);
            if(!governance.submissionReady())return Fee3R4C3QardContract.blocked(request,bankDate,
                "R3_PREFLIGHT_"+(governance.issues().isEmpty()?"UNREADY":governance.issues().get(0).code()));
            if(oracle.formulas().stream().noneMatch(f->"TPL_E624746F262F6E".equals(f.code())&&
                f.active()&&"FORMULA".equals(f.method())))
                return Fee3R4C3QardContract.blocked(request,bankDate,"ORACLE_QARD_FORMULA_NOT_ACTIVE");
            if(draft.version().sourceId()!=null&&
               !"VERIFIED".equals(oracle.sourceStatus(draft.version().sourceId())))
                return Fee3R4C3QardContract.blocked(request,bankDate,"DRAFT_SOURCE_UNVERIFIED");
            var proof=references.resolve(draft);
            LocalDate start=LocalDate.parse((String)request.inputs().get("periodStart"));
            LocalDate end=LocalDate.parse((String)request.inputs().get("periodEndExclusive"));
            LocalDate origin=LocalDate.parse((String)request.inputs().get("loanContractStartDate"));
            LocalDate event=LocalDate.parse((String)request.inputs().get("eventDate"));
            var slices=dayRepo.yearSlices(start,end);
            var startDay=eventRepo.eventDay(origin);
            var eventDay=eventRepo.eventDay(event);
            var result=Fee3R4C3QardContract.calculate(request,slices,startDay,eventDay);
            return Fee3R4C3QardContract.finish(request,bankDate,proof,result);
        }catch(Fee3Problem ex){
            return Fee3R4C3QardContract.blocked(request,bankDate,"BUSINESS_"+ex.errorCode());
        }catch(DataAccessException ex){
            return Fee3R4C3QardContract.blocked(request,bankDate,"ORACLE_CAL2_REFERENCE_UNAVAILABLE");
        }catch(IllegalArgumentException|ArithmeticException ex){
            String msg=ex.getMessage();
            return Fee3R4C3QardContract.blocked(request,bankDate,
                msg!=null&&msg.matches("[A-Z0-9_]{3,100}")?msg:"QARD_CALCULATION_UNVERIFIED");
        }
    }
}
