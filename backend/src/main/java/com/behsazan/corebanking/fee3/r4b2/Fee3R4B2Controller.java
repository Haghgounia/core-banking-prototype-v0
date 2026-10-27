package com.behsazan.corebanking.fee3.r4b2;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fee3/r4")
public class Fee3R4B2Controller {
    private final Fee3R4B2Service service;
    public Fee3R4B2Controller(Fee3R4B2Service service){this.service=service;}
    @PostMapping("/guarantees/preview")
    public Fee3R4B2GuaranteeContract.Evidence preview(@RequestBody Fee3R4B2GuaranteeContract.Request req){
        return service.preview(req);
    }
}
