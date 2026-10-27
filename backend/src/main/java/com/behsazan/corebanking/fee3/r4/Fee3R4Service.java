package com.behsazan.corebanking.fee3.r4;

import com.behsazan.corebanking.fee3.oracle.Fee3Repository;
import com.behsazan.corebanking.fee3.r3.Fee3R3Service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

/** New read-only endpoint, isolated from R3 API contract, workflow and persistence. */
@Service
public class Fee3R4Service {
    private final Fee3Repository repo;
    private final Fee3R4ReferenceRepository references;
    private final Fee3R3Service r3;
    public Fee3R4Service(Fee3Repository repo,Fee3R4ReferenceRepository references,Fee3R3Service r3){
        this.repo=repo;this.references=references;this.r3=r3;
    }
    @Transactional(readOnly=true)
    public Fee3R4CalculationContract.Evidence preview(Fee3R4CalculationContract.Request request){
        LocalDate day=repo.bankDate();
        String problem=Fee3R4CalculationContract.guard(request,day);
        if(problem!=null)return Fee3R4CalculationContract.blocked(request,day,problem);
        try{
            Fee3R4CalculationContract.VersionReference source=references.resolve(request.draft());
            var calculated=r3.preview(new com.behsazan.corebanking.fee3.r3.Fee3PreviewEngine.PreviewRequest(
                request.draft(),request.inputs()));
            return Fee3R4CalculationContract.finish(request,day,source,calculated);
        }catch(IllegalArgumentException ex){
            String code=ex.getMessage();
            if(code==null||!code.matches("[A-Z0-9_]{3,100}"))code="R4A_REFERENCE_UNVERIFIED";
            return Fee3R4CalculationContract.blocked(request,day,code);
        }
    }
}
