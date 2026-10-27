package com.behsazan.corebanking.fee3.r4;

import org.springframework.web.bind.annotation.*;

/** R4-A is explicitly an unapproved simulation; POST does not mutate state. */
@RestController
@RequestMapping("/api/v1/fee3/r4")
public class Fee3R4Controller {
    private final Fee3R4Service service;
    public Fee3R4Controller(Fee3R4Service service){this.service=service;}
    @PostMapping("/preview")
    public Fee3R4CalculationContract.Evidence preview(@RequestBody Fee3R4CalculationContract.Request request){
        return service.preview(request);
    }
}
