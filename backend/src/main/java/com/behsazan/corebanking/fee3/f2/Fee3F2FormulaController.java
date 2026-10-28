package com.behsazan.corebanking.fee3.f2;

import org.springframework.web.bind.annotation.*;

/** POST is a read-only UAT preview, NOT fee assessment, activation or posting. */
@RestController
@RequestMapping("/api/v1/fee3/r4/services/formula-allowlist")
public class Fee3F2FormulaController {
    private final Fee3F2FormulaService service;
    public Fee3F2FormulaController(Fee3F2FormulaService service){this.service=service;}
    @PostMapping("/preview")
    public Fee3F2FormulaContract.Evidence preview(@RequestBody Fee3F2FormulaContract.Request body){
        return service.preview(body);
    }
}
