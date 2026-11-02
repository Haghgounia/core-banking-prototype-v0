package com.behsazan.corebanking.iam.s1b;

import com.behsazan.corebanking.iam.s1b.IamS1bModels.*;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** No POST, PUT, PATCH or DELETE. The existing S1A mock UI remains isolated. */
@RestController
@RequestMapping("/api/v1/iam")
@ConditionalOnProperty(prefix = "core-banking.iam.s1b", name = "reads-enabled", havingValue = "true")
public class IamS1bReadController {
    private final IamS1bReadService service;
    public IamS1bReadController(IamS1bReadService service) { this.service = service; }

    @GetMapping("/readiness")
    public Readiness readiness(@RequestParam String domainCode, JwtAuthenticationToken actor) {
        return service.readiness(requireActor(actor), domainCode);
    }

    @GetMapping("/accounts")
    public AccountPage accounts(@RequestParam String domainCode,
                                @RequestParam(required = false) String search,
                                @RequestParam(defaultValue = "0") int offset,
                                @RequestParam(defaultValue = "25") int limit,
                                JwtAuthenticationToken actor) {
        return service.accounts(requireActor(actor), domainCode, search, offset, limit);
    }

    @GetMapping("/accounts/{accountId}")
    public Account account(@PathVariable long accountId, @RequestParam String domainCode,
                           JwtAuthenticationToken actor) {
        return service.account(requireActor(actor), domainCode, accountId);
    }

    @GetMapping("/accounts/{accountId}/identity-links")
    public List<IdentityLink> links(@PathVariable long accountId, @RequestParam String domainCode,
                                    JwtAuthenticationToken actor) {
        return service.links(requireActor(actor), domainCode, accountId);
    }

    private static JwtAuthenticationToken requireActor(JwtAuthenticationToken actor) {
        if (actor == null || !actor.isAuthenticated())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "IAM_S1B_VERIFIED_ACTOR_REQUIRED");
        return actor;
    }
}
