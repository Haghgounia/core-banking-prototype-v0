package com.behsazan.corebanking.fee3.r4c1;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fee3/r4")
public class Fee3R4C1Controller {
    private final Fee3R4C1Service service;
    public Fee3R4C1Controller(Fee3R4C1Service service){this.service=service;}
    @PostMapping("/loans/profit/preview")
    public Fee3R4C1LoanProfitContract.Evidence preview(
        @RequestBody Fee3R4C1LoanProfitContract.Request request){
        return service.preview(request);
    }
}
