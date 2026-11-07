package com.behsazan.corebanking.collateral.reference;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceModels.ReferenceReadMatrix;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CLT-RF1: local, loopback-restricted UAT diagnostics only, under the existing
 * independent CLT reference-read security chain. No production endpoint and
 * no alternate path around reference.read authority. All queries are SELECT.
 */
@RestController
@RequestMapping("/api/v1/collateral/reference")
@ConditionalOnProperty(name = "CLT_LOCAL_UAT_READONLY_ENABLED", havingValue = "true")
public final class CollateralReferenceLocalMatrixController {
    private final CollateralReferenceRepository repository;
    public CollateralReferenceLocalMatrixController(CollateralReferenceRepository repository) {
        this.repository = repository;
    }
    @GetMapping("/uat-matrix")
    ReferenceReadMatrix matrix() { return repository.readinessMatrix(); }
}
