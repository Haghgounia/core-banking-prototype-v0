package com.behsazan.corebanking.fee3.r3;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** F7 read-only domain contract; Oracle writes, approval, posting and collection are excluded. */
@RestController
@RequestMapping("/api/v1/fee3/simulations")
public final class Fee3F7DomainSimulationController {
    private final Fee3F7DomainSimulationService service;
    public Fee3F7DomainSimulationController(Fee3F7DomainSimulationService service){this.service=service;}
    @PostMapping("/domain-preview")
    public Fee3F7DomainSimulationService.Result preview(
            @RequestBody Fee3F7DomainSimulationService.Request request){
        return service.preview(request);
    }
}
