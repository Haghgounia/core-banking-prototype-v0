package com.behsazan.corebanking.fee3.r4c2;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fee3/r4")
public class Fee3R4C2Controller {
    private final Fee3R4C2Service service;
    public Fee3R4C2Controller(Fee3R4C2Service service){this.service=service;}
    @PostMapping("/loans/models/preview")
    public Fee3R4C2LoanModelContract.Evidence preview(
        @RequestBody Fee3R4C2LoanModelContract.Request req){return service.preview(req);}
}
