package com.behsazan.corebanking.collateral.capacity;

import com.behsazan.corebanking.collateral.capacity.CollateralCapacityModels.*;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionService;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceProperties;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryProperties;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** R4A evidence & what-if ONLY. Allocations, holds and financial authorizations are NOT implemented. */
@Service
public class CollateralCapacityService {
    private final CollateralCapacityRepository repository;
    private final CollateralIntroductionService identity;
    private final CollateralReferenceProperties reference;
    private final CollateralRegistryProperties registry;

    public CollateralCapacityService(CollateralCapacityRepository repository,
            CollateralIntroductionService identity, CollateralReferenceProperties reference,
            CollateralRegistryProperties registry) {
        this.repository = repository;
        this.identity = identity;
        this.reference = reference;
        this.registry = registry;
    }
    private void check(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream()
                .noneMatch(a -> "SCOPE_clt.collateral.read".equals(a.getAuthority())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CLT_CAPACITY_READ_AUTHORITY_REQUIRED");
        if (!reference.readsEnabled() || !registry.readsEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "CLT_CAPACITY_READS_DISABLED");
    }
    public CapacityEvidence evidence(String id, Authentication auth) {
        check(auth);
        var actor = identity.actor(auth); // Jwt subject & branch from verified token; never request parameter
        return evidenceForVerifiedBranch(id, actor.branch());
    }
    /** Internal reuse after a separate authenticated service/client gate; never expose this as an HTTP route. */
    public CapacityEvidence evidenceForVerifiedBranch(String id, String verifiedBranch) {
        if (!reference.readsEnabled() || !registry.readsEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "CLT_CAPACITY_READS_DISABLED");
        var p = repository.loadParent(verifiedBranch, CollateralRegistryPolicy.id(id).toPlainString());
        List<AllocationBucket> allocations = repository.activeAllocations(p);
        String day = repository.oracleTehranDate();
        var computation = CollateralCapacityPolicy.calculate(new CollateralCapacityPolicy.Input(
                p.collateralStatus(), p.statusReferenceActive(),
                "ACTIVE".equals(p.typeParameters().status()),
                p.typeParameters().sponsorRequired(), p.typeParameters().insurable(),
                p.typeParameters().exclusiveUsage(), p.currencyCode(),
                p.currentValuationId(), p.valuationStatus(), p.valuationCurrency(),
                p.valuationEffectiveFrom(), p.valuationEffectiveTo(), day,
                p.collateralValidFrom(), p.collateralValidTo(), p.eligibleValue(), allocations));
        return new CapacityEvidence(p.collateralId(), p.collateralNumber(),
                p.collateralStatus(), p.currencyCode(), day,
                "ORACLE_SYSTIMESTAMP_ASIA_TEHRAN_NOT_BANK_BUSINESS_DATE",
                p.currentValuationId(), p.valuationStatus(), p.valuationCurrency(),
                p.valuationEffectiveFrom(), p.valuationEffectiveTo(),
                CollateralCapacityPolicy.decimal(p.eligibleValue()),
                computation.sameCurrencyAllocated(), computation.residual(), computation.shortfall(),
                computation.activeCount(), allocations, p.typeParameters(),
                computation.periodProven(), false, null, computation.blockers(),
                CollateralCapacityPolicy.POLICY_GAPS,
                "READ_ONLY_NON_TRANSACTIONAL_ARITHMETIC_NOT_ALLOCATION_AUTHORIZATION");
    }
    public PreviewResult preview(String id, PreviewRequest request, Authentication auth) {
        check(auth);
        if (request == null) throw new IllegalArgumentException("CLT_CAPACITY_PREVIEW_BODY_REQUIRED");
        String domain = CollateralCapacityPolicy.domain(request.consumerDomain());
        BigDecimal amount = CollateralCapacityPolicy.positiveMoney(request.requestedAmount());
        String currency = CollateralCapacityPolicy.currency(request.currencyCode());
        CapacityEvidence current = evidence(id, auth);
        List<String> reasons = new ArrayList<>(current.blockers());
        if (!currency.equals(current.currencyCode()))
            reasons.add("CLT_CAPACITY_REQUEST_CURRENCY_DIFFERS_FX_NOT_VERIFIED");
        // Raw comparison excludes FX and all policy-specific factors; never treated as approval.
        String hypotheticalShortfall = currency.equals(current.currencyCode())
                && current.arithmeticResidual() != null
                ? CollateralCapacityPolicy.hypotheticalShortfall(current.arithmeticResidual(), amount.toPlainString())
                : null;
        reasons.add("CLT_CAPACITY_CONSUMER_CASE_DEBTORS_AND_MINIMUM_COVERAGE_NOT_VERIFIED");
        return new PreviewResult(current.collateralId(), domain, amount.toPlainString(), currency,
                hypotheticalShortfall, false, null, List.copyOf(reasons),
                "WHAT_IF_ONLY_NOT_APPROVED_NO_ALLOCATION_WRITE");
    }
}
