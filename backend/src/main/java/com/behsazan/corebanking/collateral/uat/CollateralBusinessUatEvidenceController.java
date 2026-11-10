package com.behsazan.corebanking.collateral.uat;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Analyst UAT evidence only; NO bank approval or financial operation endpoints. */
@RestController
@RequestMapping("/api/v1/collaterals/uat")
public class CollateralBusinessUatEvidenceController {
    private final CollateralBusinessUatEvidenceService service;
    public CollateralBusinessUatEvidenceController(CollateralBusinessUatEvidenceService service) {this.service=service;}
    @GetMapping("/business-evidence")
    public CollateralBusinessUatEvidenceService.EvidenceState get(Authentication auth) {return service.get(auth);}
    @GetMapping("/business-evidence/history")
    public java.util.List<CollateralBusinessUatEvidenceService.EvidenceEvent> history(Authentication auth) {
        return service.history(auth);
    }
    @PutMapping("/business-evidence")
    public CollateralBusinessUatEvidenceService.EvidenceState put(
            Authentication auth,@RequestBody CollateralBusinessUatEvidenceService.EvidenceWrite data) {
        return service.put(auth,data);
    }
}
