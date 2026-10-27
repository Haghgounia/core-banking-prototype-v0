package com.behsazan.corebanking.fee3.r4c3;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fee3/r4/qard")
public class Fee3R4C3Controller {
    private final Fee3R4C3Service service;
    public Fee3R4C3Controller(Fee3R4C3Service service){this.service=service;}
    @PostMapping("/preview")
    public Fee3R4C3QardContract.Evidence preview(@RequestBody Fee3R4C3QardContract.Request request){
        return service.preview(request);
    }
}
