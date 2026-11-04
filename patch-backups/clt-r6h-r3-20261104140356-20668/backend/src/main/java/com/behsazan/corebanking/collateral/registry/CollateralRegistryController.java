package com.behsazan.corebanking.collateral.registry;

import com.behsazan.corebanking.collateral.registry.CollateralRegistryModels.*;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** R2A read-only contract. No introduction, amendment or release write endpoints. */
@RestController
@RequestMapping("/api/v1/collaterals")
public final class CollateralRegistryController {
    private final CollateralRegistryRepository repository;
    private final CollateralRegistryProperties registry;

    public CollateralRegistryController(CollateralRegistryRepository repository,
                                        CollateralRegistryProperties registry) {
        this.repository = repository;
        this.registry = registry;
    }
    private String branch(Authentication authentication) {
        if (!registry.readsEnabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "CLT_REGISTRY_READS_DISABLED");
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated()
                || jwt.getToken().getSubject() == null || jwt.getToken().getSubject().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "CLT_REGISTRY_JWT_REQUIRED");
        }
        Object claim = jwt.getToken().getClaims().get("branch_code");
        if (!(claim instanceof String branch)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "CLT_REGISTRY_BRANCH_CLAIM_REQUIRED");
        try { return CollateralRegistryPolicy.branch(branch); }
        catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "CLT_REGISTRY_BRANCH_CLAIM_INVALID"); }
    }

    @GetMapping("/readiness")
    Readiness readiness(Authentication authentication) {
        branch(authentication);
        return new Readiness(registry.readsEnabled(), false, "JWT_BRANCH_ONLY",
                "DECIMAL_STRING_MAX_19", "R2A_READ_ONLY");
    }

    @GetMapping
    Page list(Authentication authentication,
              @RequestParam(required = false) String registrationNumber,
              @RequestParam(defaultValue = "0") int page,
              @RequestParam(defaultValue = "20") int size) {
        return repository.list(branch(authentication), registrationNumber, page, size);
    }

    @GetMapping("/by-registration/{number}")
    Map<String, Object> byRegistration(Authentication authentication, @PathVariable String number) {
        return repository.byRegistration(branch(authentication), number);
    }

    @GetMapping("/types")
    List<Map<String, Object>> types(Authentication authentication) {
        branch(authentication);
        return repository.activeTypes();
    }

    @GetMapping("/types/{typeId}/attributes")
    List<Map<String, Object>> attributes(Authentication authentication, @PathVariable String typeId) {
        branch(authentication);
        return repository.typeAttributes(typeId);
    }

    @GetMapping("/{collateralId}")
    Map<String, Object> byId(Authentication authentication, @PathVariable String collateralId) {
        return repository.byId(branch(authentication), collateralId);
    }
}
