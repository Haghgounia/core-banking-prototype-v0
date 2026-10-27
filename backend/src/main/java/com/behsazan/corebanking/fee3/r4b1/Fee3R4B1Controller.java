package com.behsazan.corebanking.fee3.r4b1;

import org.springframework.web.bind.annotation.*;

/** Dedicated UAT-only numerical preview; no transaction/approval endpoints. */
@RestController
@RequestMapping("/api/v1/fee3/r4")
public class Fee3R4B1Controller {
    private final Fee3R4B1Service service;
    public Fee3R4B1Controller(Fee3R4B1Service service){this.service=service;}
    @PostMapping("/services/preview")
    public Fee3R4B1ServiceContract.Evidence preview(@RequestBody Fee3R4B1ServiceContract.Request body){
        return service.preview(body);
    }
}
