package com.behsazan.corebanking.collateral.allocation;

import com.behsazan.corebanking.collateral.allocation.CollateralAllocationIntakeModels.*;
import com.behsazan.corebanking.collateral.allocation.CollateralAllocationRequestModels.*;
import com.behsazan.corebanking.collateral.allocation.CollateralAllocationRequestPolicy.Normalized;
import com.behsazan.corebanking.collateral.capacity.CollateralCapacityService;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionService;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import com.behsazan.corebanking.collateral.prototype.CltAnalystStageIdentity;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** R4C: persistent, immutable intake only; no financial mutation, reservation or allocation. */
@Service
public class CollateralAllocationIntakeService {
    private final CollateralAllocationIntakeProperties properties;
    private final ObjectProvider<CollateralAllocationIntakeWriterPool> writers;
    private final CollateralAllocationIntakeRepository journal;
    private final CollateralAllocationRequestRepository sponsorRepository;
    private final CollateralAllocationConsumerBinding consumers;
    private final CollateralIntroductionService identity;
    private final CollateralCapacityService capacity;
    public CollateralAllocationIntakeService(CollateralAllocationIntakeProperties properties,
            ObjectProvider<CollateralAllocationIntakeWriterPool> writers,
            CollateralAllocationIntakeRepository journal,
            CollateralAllocationRequestRepository sponsorRepository,
            CollateralAllocationConsumerBinding consumers, CollateralIntroductionService identity,
            CollateralCapacityService capacity) {
        this.properties=properties;this.writers=writers;this.journal=journal;
        this.sponsorRepository=sponsorRepository;this.consumers=consumers;
        this.identity=identity;this.capacity=capacity;
    }
    private JwtAuthenticationToken token(Authentication auth, String scope) {
        if (!(auth instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"CLT_ALLOCATION_JWT_REQUIRED");
        if (jwt.getAuthorities().stream().noneMatch(a -> scope.equals(a.getAuthority())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"CLT_ALLOCATION_SCOPE_REQUIRED");
        return jwt;
    }
    private CollateralAllocationIntakeWriterPool pool() {
        if (!properties.requestWritesEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_ALLOCATION_INTAKE_DISABLED");
        CollateralAllocationIntakeWriterPool p=writers.getObject();
        journal.requireSchema(p);
        return p;
    }
    public Readiness readiness(Authentication auth) {
        token(auth,"SCOPE_clt.collateral.read");
        identity.actor(auth);
        if (!properties.requestWritesEnabled())
            return new Readiness(false,false,false,false,false,"INTAKE_DISABLED_DEFAULT_DENY");
        boolean schema;
        try {journal.requireSchema(writers.getObject()); schema=true;}
        catch (RuntimeException ex) {schema=false;}
        return new Readiness(true,schema,false,false,false,
                schema ? "STAGING_ONLY_NO_ALLOCATION" : "STAGING_SCHEMA_NOT_APPROVED");
    }
    private Receipt receipt(CollateralAllocationIntakeRepository.Stored r, boolean replay) {
        List<String> blockers=new ArrayList<>(CollateralAllocationIntakePolicy.OPERATIONAL_BLOCKERS);
        if (r.sponsorConflict()) blockers.add("CLT_ALLOCATION_DEBTOR_IS_COLLATERAL_SPONSOR");
        return new Receipt(r.id(),r.collateralId(),r.consumerDomain(),r.caseReference(),
                r.fileNumber(),r.amount(),r.currency(),r.requestStatus(),r.createdAt(),
                r.version(),r.debtorCount(),r.sponsorConflict(),r.digest(),
                replay,true,false,false,false,false,List.copyOf(blockers),
                CollateralAllocationIntakePolicy.EVIDENCE);
    }
    public Receipt submit(String rawCollateral, Request input, String rawKey, Authentication auth) {
        JwtAuthenticationToken jwt=token(auth,"SCOPE_clt.allocation.request");
        Normalized n=CollateralAllocationRequestPolicy.normalize(input);
        String client=CollateralAllocationIntakePolicy.clientId(
                consumers.authenticatedClient(jwt,n.consumerDomain()));
        var actor=identity.actor(jwt);
        String subject=CollateralAllocationIntakePolicy.actor(actor.subject());
        String id=CollateralRegistryPolicy.id(rawCollateral).toPlainString();
        String key=CollateralAllocationRequestPolicy.key(rawKey);
        String digest=CollateralAllocationRequestPolicy.digest(actor.branch(),client,id,n);
        CollateralAllocationIntakeWriterPool pool=pool();
        try {
            Receipt response=pool.transaction().execute(status -> {
                var prior=journal.byKey(pool,n.consumerDomain(),client,key,true);
                if(prior!=null) {
                    CollateralAllocationIntakePolicy.requireReplayMatch(
                            prior.digest(),digest,prior.requestStatus());
                    return receipt(prior,true);
                }
                // Branch-scoped evidence read: proves existence, NOT a transactionally locked capacity.
                capacity.evidenceForVerifiedBranch(id,actor.branch());
                boolean conflict=!sponsorRepository.sponsorDebtorMatches(
                        id,actor.branch(),n.debtors()).isEmpty();
                return receipt(journal.insert(pool,id,actor.branch(),subject,client,key,digest,n,conflict),false);
            });
            if(response==null) throw new IllegalStateException("CLT_ALLOCATION_INTAKE_TRANSACTION_EMPTY");
            return response;
        } catch (DuplicateKeyException duplicate) {
            // Another transaction committed the same unique key. Re-read only after its commit/rollback.
            Receipt response=pool.transaction().execute(status -> {
                var winner=journal.byKey(pool,n.consumerDomain(),client,key,true);
                if(winner==null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "CLT_ALLOCATION_INTAKE_CONCURRENT_CONFLICT");
                CollateralAllocationIntakePolicy.requireReplayMatch(
                        winner.digest(),digest,winner.requestStatus());
                return receipt(winner,true);
            });
            if(response==null) throw new IllegalStateException("CLT_ALLOCATION_INTAKE_REPLAY_EMPTY");
            return response;
        }
    }
    /** Persist a branch-bound ANALYST request, never a consumer-authorized allocation. */
    public Receipt submitAnalyst(String rawCollateral, Request input, String rawKey, Authentication auth) {
        JwtAuthenticationToken jwt = CltAnalystStageIdentity.require(auth);
        Normalized normalized = CollateralAllocationRequestPolicy.normalize(input);
        var actor = identity.actor(jwt);
        String branch = actor.branch();
        String subject = CollateralAllocationIntakePolicy.actor(actor.subject());
        String collateral = CollateralRegistryPolicy.id(rawCollateral).toPlainString();
        String client = CltAnalystStageIdentity.clientId();
        String key = CollateralAllocationRequestPolicy.key(rawKey);
        String digest = CollateralAllocationRequestPolicy.digest(branch, client, collateral, normalized);
        CollateralAllocationIntakeWriterPool pool = pool();
        try {
            Receipt result = pool.transaction().execute(tx -> {
                var existing = journal.byKey(pool, normalized.consumerDomain(), client, key, true);
                if (existing != null) {
                    CollateralAllocationIntakePolicy.requireReplayMatch(existing.digest(),digest,existing.requestStatus());
                    // Do not reveal another branch's request on key collision.
                    if (!branch.equals(existing.branchCode())
                            || !collateral.equals(existing.collateralId()))
                        throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_ALLOCATION_ANALYST_REPLAY_SCOPE_MISMATCH");
                    return receipt(existing,true);
                }
                capacity.evidenceForVerifiedBranch(collateral, branch);
                boolean conflict = !sponsorRepository.sponsorDebtorMatches(
                        collateral,branch,normalized.debtors()).isEmpty();
                return receipt(journal.insert(pool,collateral,branch,subject,client,key,digest,
                        normalized,conflict),false);
            });
            if (result == null) throw new IllegalStateException("CLT_ALLOCATION_ANALYST_STAGE_TX_EMPTY");
            return result;
        } catch (DuplicateKeyException duplicate) {
            Receipt replay = pool.transaction().execute(tx -> {
                var row = journal.byKey(pool,normalized.consumerDomain(),client,key,true);
                if (row == null || !collateral.equals(row.collateralId()))
                    throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_ALLOCATION_ANALYST_CONCURRENT_CONFLICT");
                CollateralAllocationIntakePolicy.requireReplayMatch(row.digest(),digest,row.requestStatus());
                return receipt(row,true);
            });
            if (replay == null) throw new IllegalStateException("CLT_ALLOCATION_ANALYST_REPLAY_EMPTY");
            return replay;
        }
    }

    public Receipt receiptById(String collateralId, String requestId,
            String domain, Authentication auth) {
        JwtAuthenticationToken jwt=token(auth,"SCOPE_clt.allocation.request");
        String checkedDomain=com.behsazan.corebanking.collateral.capacity.CollateralCapacityPolicy.domain(domain);
        String client=CollateralAllocationIntakePolicy.clientId(consumers.authenticatedClient(jwt,checkedDomain));
        String branch=identity.actor(jwt).branch();
        String id=CollateralRegistryPolicy.id(collateralId).toPlainString();
        String request=CollateralRegistryPolicy.id(requestId).toPlainString();
        var stored=journal.byId(pool(),request,id,branch,checkedDomain,client);
        if(stored==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"CLT_ALLOCATION_REQUEST_NOT_FOUND");
        return receipt(stored,false);
    }
    public List<JournalItem> journal(String collateralId, int limit, Authentication auth) {
        token(auth,"SCOPE_clt.collateral.read");
        String branch=identity.actor(auth).branch();
        String id=CollateralRegistryPolicy.id(collateralId).toPlainString();
        // Verify original collateral visibility even if intake table has stale or inconsistent rows.
        capacity.evidenceForVerifiedBranch(id,branch);
        return journal.journal(pool(),id,branch,CollateralAllocationIntakePolicy.limit(limit));
    }
}
