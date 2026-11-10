package com.behsazan.corebanking.collateral.registry;

import com.behsazan.corebanking.collateral.registry.CollateralRegistryModels.*;
import com.behsazan.corebanking.collateral.localuat.CltLocalUatProperties;
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
    private final CltLocalUatProperties localUat;
    private final com.behsazan.corebanking.collateral.registration.CollateralRegistrationReadbackService readback;

    public CollateralRegistryController(CollateralRegistryRepository repository,
                                        CollateralRegistryProperties registry, CltLocalUatProperties localUat,
            com.behsazan.corebanking.collateral.registration.CollateralRegistrationReadbackService readback) {
        this.repository = repository;
        this.registry = registry;
        this.localUat = localUat;
        this.readback = readback;
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
        return new Readiness(registry.readsEnabled(), false,
                localUat.enabled() ? "LOCAL_UAT_FIXED_BRANCH" : "JWT_BRANCH_ONLY",
                "DECIMAL_STRING_MAX_19", "R2A_READ_ONLY");
    }

    @GetMapping
    Page list(Authentication authentication,
              @RequestParam(required = false) String registrationNumber,
              @RequestParam(required = false) String groupCode,
              @RequestParam(required = false) String partyId,
              @RequestParam(required = false) String typeId,
              @RequestParam(required = false) String statusCode,
              @RequestParam(defaultValue = "0") int page,
              @RequestParam(defaultValue = "20") int size) {
        if (groupCode != null) {
            try { CollateralRegistryPolicy.groupCode(groupCode); }
            catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CLT_REGISTRY_GROUP_CODE_INVALID");
            }
        }
        try {
            if (partyId != null && !partyId.isBlank()) CollateralRegistryPolicy.id(partyId);
            if (typeId != null && !typeId.isBlank()) CollateralRegistryPolicy.id(typeId);
            if (statusCode != null && !statusCode.isBlank()) CollateralRegistryPolicy.statusCode(statusCode);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CLT_REGISTRY_FILTER_INVALID");
        }
        return repository.list(branch(authentication), registrationNumber, groupCode,
                partyId, typeId, statusCode, page, size);
    }

    @GetMapping("/by-registration/{number}")
    Map<String, Object> byRegistration(Authentication authentication, @PathVariable String number) {
        return repository.byRegistration(branch(authentication), number);
    }

    /** BUC-13: branch-scoped full readback, no registration or approval mutation. */
    @GetMapping("/by-registration/{number}/verified-readback")
    com.behsazan.corebanking.collateral.registration.CollateralRegistrationReadbackModels.Report
    verifiedReadback(Authentication authentication, @PathVariable String number) {
        return readback.byNumber(branch(authentication), number);
    }

    /** CLT_R6I_ACTIVE_GROUPS_20261104: safe, global technical catalog with a read-scope gate. */
    @GetMapping(value = "/groups", name = "CLT_R6I_ACTIVE_GROUPS_20261104")
    List<Map<String, Object>> groups(Authentication authentication) {
        branch(authentication);
        return repository.activeGroups();
    }

    /** Reference-driven lifecycle choices; not hardcoded UI state. */
    @GetMapping("/statuses")
    List<Map<String, Object>> statuses(Authentication authentication) {
        branch(authentication);
        return repository.activeStatuses();
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
