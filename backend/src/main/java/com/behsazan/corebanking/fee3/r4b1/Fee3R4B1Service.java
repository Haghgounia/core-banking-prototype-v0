package com.behsazan.corebanking.fee3.r4b1;

import com.behsazan.corebanking.fee3.oracle.Fee3Repository;
import com.behsazan.corebanking.fee3.r3.Fee3R3Service;
import com.behsazan.corebanking.fee3.r4.Fee3R4ReferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

/** Read-only simulation; preflight and Oracle snapshot proof are mandatory before arithmetic. */
@Service
public class Fee3R4B1Service {
    private final Fee3Repository oracle;
    private final Fee3R3Service validation;
    private final Fee3R4ReferenceRepository reference;
    public Fee3R4B1Service(Fee3Repository oracle,Fee3R3Service validation,
                            Fee3R4ReferenceRepository reference){
        this.oracle=oracle;this.validation=validation;this.reference=reference;
    }
    @Transactional(readOnly=true)
    public Fee3R4B1ServiceContract.Evidence preview(Fee3R4B1ServiceContract.Request request){
        LocalDate day=oracle.bankDate();
        String violation=Fee3R4B1ServiceContract.guard(request,day);
        if(violation!=null)return Fee3R4B1ServiceContract.blocked(request,day,violation);
        try{
            // R3 preflight is read-only: active operation, scope, source and validation policy.
            // It does not certify or enable the Maker/Checker workflow.
            var report=validation.preflight(request.draft());
            if(!report.submissionReady()){
                String first=report.issues().isEmpty()?"R3_PREFLIGHT_NOT_READY":
                    "R3_PREFLIGHT_"+report.issues().get(0).code();
                return Fee3R4B1ServiceContract.blocked(request,day,first);
            }
            var version=reference.resolve(request.draft());
            var result=Fee3R4B1ServiceContract.calculate(request);
            return Fee3R4B1ServiceContract.finish(request,day,version,result);
        }catch(IllegalArgumentException error){
            String code=error.getMessage();
            if(code==null||!code.matches("[A-Z0-9_]{3,100}"))code="R4B1_UNVERIFIED_CALCULATION";
            return Fee3R4B1ServiceContract.blocked(request,day,code);
        }
    }
}
