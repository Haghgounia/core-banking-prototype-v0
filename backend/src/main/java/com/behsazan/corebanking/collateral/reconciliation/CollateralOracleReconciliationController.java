package com.behsazan.corebanking.collateral.reconciliation;

import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Read-only follow-up to the existing BUC UAT dictionary probe. */
@RestController
@RequestMapping("/api/v1/collaterals/uat")
public final class CollateralOracleReconciliationController {
    private final CollateralOracleReconciliationService service;
    public CollateralOracleReconciliationController(CollateralOracleReconciliationService service) {
        this.service = service;
    }
    @GetMapping("/oracle-reconciliation")
    public CollateralOracleReconciliationService.Report reconcile(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated()
                || jwt.getToken().getSubject() == null || jwt.getToken().getSubject().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "CLT_RECONCILIATION_SIGNED_PRINCIPAL_REQUIRED");
        }
        if (authentication.getAuthorities().stream().noneMatch(
                a -> "SCOPE_clt.collateral.read".equals(a.getAuthority()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CLT_RECONCILIATION_READ_SCOPE_REQUIRED");
        }
        Object branch = jwt.getToken().getClaims().get("branch_code");
        if (!(branch instanceof String value))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CLT_RECONCILIATION_BRANCH_REQUIRED");
        try {return service.inspect(CollateralRegistryPolicy.branch(value));}
        catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CLT_RECONCILIATION_BRANCH_INVALID");
        }
    }
}
