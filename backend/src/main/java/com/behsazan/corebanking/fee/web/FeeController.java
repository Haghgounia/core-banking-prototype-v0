package com.behsazan.corebanking.fee.web;

import com.behsazan.corebanking.fee.application.FeePrototypeService;
import com.behsazan.corebanking.fee.domain.FeeModels.CalculateFeeRequest;
import com.behsazan.corebanking.fee.domain.FeeModels.CalculateFeeResponse;
import com.behsazan.corebanking.fee.domain.FeeModels.FeePrototypeMetadata;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fees")
public class FeeController {
    private final FeePrototypeService service;
    public FeeController(FeePrototypeService service) { this.service = service; }

    @GetMapping("/prototype-metadata")
    FeePrototypeMetadata metadata() { return service.metadata(); }

    @PostMapping("/calculate")
    CalculateFeeResponse calculate(@Valid @RequestBody CalculateFeeRequest request) { return service.calculate(request); }
}
