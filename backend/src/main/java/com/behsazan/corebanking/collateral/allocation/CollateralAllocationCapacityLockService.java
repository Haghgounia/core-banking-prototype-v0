package com.behsazan.corebanking.collateral.allocation;

import com.behsazan.corebanking.collateral.allocation.CollateralAllocationCapacityLockModels.*;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionService;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import com.behsazan.corebanking.collateral.capacity.CollateralCapacityPolicy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** R4D: diagnostic Oracle critical-section proof; NO allocation writer, no reservation. */
@Service
public class CollateralAllocationCapacityLockService {
    private final CollateralAllocationCapacityLockProperties lock;
    private final CollateralAllocationIntakeProperties intake;
    private final ObjectProvider<CollateralAllocationIntakeWriterPool> writers;
    private final CollateralAllocationIntakeRepository journal;
    private final CollateralAllocationCapacityLockRepository repository;
    private final CollateralAllocationConsumerBinding consumer;
    private final CollateralIntroductionService identity;

    public CollateralAllocationCapacityLockService(CollateralAllocationCapacityLockProperties lock,
            CollateralAllocationIntakeProperties intake,
            ObjectProvider<CollateralAllocationIntakeWriterPool> writers,
            CollateralAllocationIntakeRepository journal,
            CollateralAllocationCapacityLockRepository repository,
            CollateralAllocationConsumerBinding consumer, CollateralIntroductionService identity) {
        this.lock=lock;this.intake=intake;this.writers=writers;this.journal=journal;
        this.repository=repository;this.consumer=consumer;this.identity=identity;
    }
    private JwtAuthenticationToken authenticated(Authentication auth,String authority) {
        if(!(auth instanceof JwtAuthenticationToken jwt)||!jwt.isAuthenticated())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"CLT_R4D_JWT_REQUIRED");
        if(jwt.getAuthorities().stream().noneMatch(a->authority.equals(a.getAuthority())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"CLT_R4D_SCOPE_REQUIRED");
        return jwt;
    }
    public Readiness readiness(Authentication auth) {
        authenticated(auth,"SCOPE_clt.collateral.read");identity.actor(auth);
        boolean enabled=lock.probeEnabled()&&intake.requestWritesEnabled();
        return new Readiness(enabled,intake.requestWritesEnabled(),false,false,
                enabled?"DIAGNOSTIC_ONLY_LOCK_DOES_NOT_RESERVE_OR_ALLOCATE":
                        "LOCK_PROBE_DISABLED_DEFAULT_DENY");
    }
    public Probe inspect(String collateralId,String requestId,String domain,Authentication auth) {
        JwtAuthenticationToken jwt=authenticated(auth,"SCOPE_clt.allocation.request");
        if(!lock.probeEnabled()||!intake.requestWritesEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_R4D_LOCK_PROBE_DISABLED");
        String checkedDomain=CollateralCapacityPolicy.domain(domain);
        String client=CollateralAllocationIntakePolicy.clientId(
                consumer.authenticatedClient(jwt,checkedDomain));
        String branch=identity.actor(jwt).branch();
        String collateral=CollateralRegistryPolicy.id(collateralId).toPlainString();
        String request=CollateralRegistryPolicy.id(requestId).toPlainString();
        var writer=writers.getObject();
        journal.requireSchema(writer);
        Probe probe=writer.transaction().execute(status->{
            // Lock order for all compliant future writers: COLLATERAL parent FIRST,
            // then immutable request, then allocations/debtors/valuation, all on ONE connection.
            String version=repository.lockParent(writer.jdbc(),collateral,branch);
            var stored=journal.byId(writer,request,collateral,branch,checkedDomain,client);
            if(stored==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "CLT_R4D_REQUEST_NOT_FOUND_FOR_AUTHENTICATED_CONSUMER");
            var snapshot=repository.inspectUnderParentLock(writer.jdbc(),collateral,branch,
                    request,version);
            var decision=CollateralAllocationCapacityLockPolicy.inspect(snapshot,
                    stored.requestStatus(),stored.amount(),stored.currency(),stored.debtorCount());
            return new Probe(stored.id(),collateral,checkedDomain,version,
                    snapshot.capacity().asOfDate(),stored.currency(),stored.amount(),
                    decision.allocatedSameCurrency(),decision.arithmeticResidual(),
                    decision.hypotheticalShortfall(),true,decision.debtorRosterWellFormed(),
                    decision.valuationPeriodProven(),false,false,false,
                    decision.evidenceLevel(),decision.blockers());
        });
        if(probe==null)throw new IllegalStateException("CLT_R4D_LOCK_TRANSACTION_RETURNED_NULL");
        // The parent row lock has ALREADY BEEN RELEASED by the transaction boundary.
        // Never return a 'capacity reserved' or 'lock currently held' representation.
        return probe;
    }
}
