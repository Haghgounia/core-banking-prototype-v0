package com.behsazan.corebanking.fee3.f1;

import org.springframework.web.bind.annotation.*;

/** POST is deliberately read-only; neither a fee charge nor a tariff publication endpoint. */
@RestController
@RequestMapping("/api/v1/fee3/r4/services/usage")
public class Fee3F1UsageController {
    private final Fee3F1UsageService service;
    public Fee3F1UsageController(Fee3F1UsageService service) { this.service = service; }
    @PostMapping("/preview")
    public Fee3F1UsageContract.Evidence preview(@RequestBody Fee3F1UsageContract.Request body) {
        return service.preview(body);
    }
}
