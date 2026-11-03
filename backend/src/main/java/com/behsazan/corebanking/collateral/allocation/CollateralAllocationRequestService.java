package com.behsazan.corebanking.collateral.allocation;

import com.behsazan.corebanking.collateral.allocation.CollateralAllocationRequestModels.*;
import com.behsazan.corebanking.collateral.capacity.CollateralCapacityModels.CapacityEvidence;
import com.behsazan.corebanking.collateral.capacity.CollateralCapacityPolicy;
import com.behsazan.corebanking.collateral.capacity.CollateralCapacityService;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionService;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** R4B protocol checks only: NO request persistence, NO capacity reservation, NO allocation DML. */
@Service
public class CollateralAllocationRequestService {
    private final CollateralAllocationRequestRepository repository;
    private final CollateralCapacityService capacity;
    private final CollateralIntroductionService identity;
    private final CollateralAllocationConsumerBinding consumers;

    public CollateralAllocationRequestService(CollateralAllocationRequestRepository repository,
            CollateralCapacityService capacity, CollateralIntroductionService identity,
            CollateralAllocationConsumerBinding consumers) {
        this.repository = repository; this.capacity = capacity;
        this.identity = identity; this.consumers = consumers;
    }
    private JwtAuthenticationToken token(Authentication auth, String scope) {
        if (!(auth instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "CLT_ALLOCATION_JWT_REQUIRED");
        if (jwt.getAuthorities().stream().noneMatch(a -> scope.equals(a.getAuthority())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CLT_ALLOCATION_SCOPE_REQUIRED");
        return jwt;
    }
    public Preflight preview(String collateralId, Request request, String key, Authentication auth) {
        JwtAuthenticationToken jwt = token(auth, "SCOPE_clt.collateral.read");
        String branch = identity.actor(jwt).branch();
        return evaluate(collateralId, request, key, branch, "BROWSER_ANALYST_NOT_CONSUMER", false);
    }
    public Preflight servicePreflight(String collateralId, Request request, String key, Authentication auth) {
        JwtAuthenticationToken jwt = token(auth, "SCOPE_clt.allocation.preflight");
        CollateralAllocationRequestPolicy.Normalized normalized = CollateralAllocationRequestPolicy.normalize(request);
        // Fail BEFORE database access when service client has not been admitted by bank configuration.
        final String client;
        try { client = consumers.authenticatedClient(jwt, normalized.consumerDomain()); }
        catch (SecurityException e) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage()); }
        String branch = identity.actor(jwt).branch();
        return evaluateValidated(collateralId, normalized, key, branch, client, true);
    }
    private Preflight evaluate(String id, Request request, String key, String branch,
            String caller, boolean verified) {
        return evaluateValidated(id, CollateralAllocationRequestPolicy.normalize(request),
                key, branch, caller, verified);
    }
    private Preflight evaluateValidated(String collateralId,
            CollateralAllocationRequestPolicy.Normalized n, String key, String branch,
            String caller, boolean verified) {
        String uuid = CollateralAllocationRequestPolicy.key(key);
        String id = CollateralRegistryPolicy.id(collateralId).toPlainString();
        CapacityEvidence evidence = capacity.evidenceForVerifiedBranch(id, branch);
        Set<String> conflicts = repository.sponsorDebtorMatches(id, branch, n.debtors());
        // Capacity calculation is non-serializable; cannot be used as a reservation or approval.
        String shortfall = n.currencyCode().equals(evidence.currencyCode())
                && evidence.arithmeticResidual() != null
                ? CollateralCapacityPolicy.hypotheticalShortfall(evidence.arithmeticResidual(), n.amount())
                : null;
        List<String> blockers = new ArrayList<>(CollateralAllocationRequestPolicy.blockers(
                verified, !conflicts.isEmpty(), n.currencyCode(), evidence.currencyCode(), shortfall));
        blockers.addAll(evidence.blockers());
        return new Preflight(id, n.consumerDomain(), n.caseReference(), n.fileNumber(),
                n.amount(), n.currencyCode(), uuid,
                CollateralAllocationRequestPolicy.digest(branch, caller, id, n),
                verified ? "JWT_CLIENT_ID_DOMAIN_AND_GRANT_MATCHED" : "ANALYST_PREVIEW_NOT_VERIFIED_CONSUMER",
                shortfall, !conflicts.isEmpty(), false, false, false, false, false,
                List.copyOf(blockers), "DENIED_NO_OPERATIONAL_AUTHORIZATION",
                "R4B_READ_ONLY_NON_ATOMIC_NO_PERSISTENCE");
    }
}
