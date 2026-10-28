package com.behsazan.corebanking.fee3.f2;

import com.behsazan.corebanking.fee3.oracle.Fee3Repository;
import com.behsazan.corebanking.fee3.r3.Fee3R3Service;
import com.behsazan.corebanking.fee3.r4.Fee3R4ReferenceRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Separate, SELECT-only, fail-closed F2 formula preview. All business approval gates remain OFF. */
@Service
public class Fee3F2FormulaService {
    private final Fee3Repository oracle;
    private final Fee3R3Service preflight;
    private final Fee3R4ReferenceRepository references;
    public Fee3F2FormulaService(Fee3Repository oracle,Fee3R3Service preflight,
                                Fee3R4ReferenceRepository references){
        this.oracle=oracle;this.preflight=preflight;this.references=references;
    }
    @Transactional(readOnly=true)
    public Fee3F2FormulaContract.Evidence preview(Fee3F2FormulaContract.Request request){
        LocalDate bankDay=oracle.bankDate();
        String local=Fee3F2FormulaContract.guard(request,bankDay);
        if(local!=null)return Fee3F2FormulaContract.blocked(request,bankDay,local);
        try {
            var review=preflight.preflight(request.draft());
            if(!review.submissionReady()){
                String code=review.issues().isEmpty()?"R3_PREFLIGHT_NOT_READY":
                    "R3_PREFLIGHT_"+review.issues().get(0).code();
                return Fee3F2FormulaContract.blocked(request,bankDay,code);
            }
            var ref=references.resolve(request.draft());
            var amount=Fee3F2FormulaContract.calculate(request);
            return Fee3F2FormulaContract.finish(request,bankDay,ref,amount);
        } catch(IllegalArgumentException ex){
            String code=ex.getMessage();
            if(code==null||!code.matches("[A-Z0-9_]{3,100}"))code="F2_FORMULA_PREVIEW_REJECTED";
            return Fee3F2FormulaContract.blocked(request,bankDay,code);
        }
    }
}
