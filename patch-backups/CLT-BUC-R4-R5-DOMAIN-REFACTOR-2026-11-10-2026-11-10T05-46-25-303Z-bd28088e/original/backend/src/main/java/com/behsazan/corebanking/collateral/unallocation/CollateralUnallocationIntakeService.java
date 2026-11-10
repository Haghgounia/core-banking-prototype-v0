package com.behsazan.corebanking.collateral.unallocation;

import com.behsazan.corebanking.collateral.allocation.CollateralAllocationConsumerBinding;
import com.behsazan.corebanking.collateral.allocation.CollateralAllocationIntakePolicy;
import com.behsazan.corebanking.collateral.capacity.CollateralCapacityPolicy;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionService;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryRepository;
import com.behsazan.corebanking.collateral.unallocation.CollateralUnallocationIntakeModels.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** R5B: records and cancels ONLY a staging request. Never updates core allocation or capacity. */
@Service
public class CollateralUnallocationIntakeService {
    private final CollateralUnallocationIntakeProperties properties;
    private final ObjectProvider<CollateralUnallocationIntakeWriterPool> writers;
    private final CollateralUnallocationIntakeRepository journal;
    private final CollateralUnallocationRepository allocationRepository;
    private final CollateralRegistryRepository registry;
    private final CollateralIntroductionService identity;
    private final CollateralAllocationConsumerBinding consumers;
    private final CollateralUnallocationDomainBinding modules;

    public CollateralUnallocationIntakeService(CollateralUnallocationIntakeProperties properties,
            ObjectProvider<CollateralUnallocationIntakeWriterPool> writers,
            CollateralUnallocationIntakeRepository journal,
            CollateralUnallocationRepository allocationRepository,
            CollateralRegistryRepository registry, CollateralIntroductionService identity,
            CollateralAllocationConsumerBinding consumers, CollateralUnallocationDomainBinding modules) {
        this.properties=properties;this.writers=writers;this.journal=journal;
        this.allocationRepository=allocationRepository;this.registry=registry;
        this.identity=identity;this.consumers=consumers;this.modules=modules;
    }
    private JwtAuthenticationToken jwt(Authentication auth,String scope) {
        if(!(auth instanceof JwtAuthenticationToken token)||!token.isAuthenticated())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"CLT_R5B_JWT_REQUIRED");
        if(token.getAuthorities().stream().noneMatch(a->scope.equals(a.getAuthority())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"CLT_R5B_SCOPE_REQUIRED");
        return token;
    }
    private CollateralUnallocationIntakeWriterPool pool() {
        if(!properties.requestWritesEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"CLT_R5B_INTAKE_DEFAULT_DENY");
        CollateralUnallocationIntakeWriterPool p=writers.getObject();
        journal.requireSchema(p);return p;
    }
    private String numeric(String raw){return CollateralRegistryPolicy.id(raw).toPlainString();}
    public Readiness readiness(Authentication auth){
        jwt(auth,"SCOPE_clt.collateral.read");identity.actor(auth);
        if(!properties.requestWritesEnabled())
            return new Readiness(false,false,false,false,false,false,"R5B_STAGING_DISABLED_DEFAULT_DENY");
        boolean ready;
        try {journal.requireSchema(writers.getObject());ready=true;}
        catch(RuntimeException ex){ready=false;}
        return new Readiness(true,ready,ready,false,false,false,
                ready?"R5B_STAGING_ONLY_NO_FINANCIAL_RELEASE":"R5B_STAGING_SCHEMA_NOT_APPROVED");
    }
    private Receipt receipt(CollateralUnallocationIntakeRepository.Stored stored,boolean replay) {
        List<String> blockers=new ArrayList<>(CollateralUnallocationIntakePolicy.parseBlockers(stored.blockerCodes()));
        if(CollateralUnallocationIntakePolicy.CANCELLED.equals(stored.requestStatus()))
            blockers.add("CLT_R5B_REQUEST_CANCELLED_IN_STAGING");
        return new Receipt(stored.requestId(),stored.collateralId(),stored.allocationId(),
                stored.consumerDomain(),stored.fileNumber(),stored.amount(),stored.currency(),
                stored.releaseMode(),stored.reasonReference(),stored.requestStatus(),
                stored.recordVersion(),stored.expectedAllocationVersion(),stored.createdAt(),
                stored.updatedAt(),stored.payloadDigest(),replay,true,false,false,
                List.copyOf(blockers),CollateralUnallocationIntakePolicy.EVIDENCE);
    }
    private record Context(JwtAuthenticationToken token,String branch,String subject,
            String client,String module,String domain) {}
    private Context caller(Authentication auth,String rawDomain) {
        JwtAuthenticationToken token=jwt(auth,"SCOPE_clt.unallocation.request");
        String domain=CollateralCapacityPolicy.domain(rawDomain);
        // Signed client_id + client_credentials + bank allowlist BEFORE any Oracle access.
        final String client;
        try {client=consumers.authenticatedClient(token,domain);}
        catch(SecurityException ex){throw new ResponseStatusException(HttpStatus.FORBIDDEN,"CLT_R5B_CONSUMER_CLIENT_DENIED");}
        // Bank-approved CLT.MODULE_ID binding is mandatory for ANY staged request.
        final String module;
        try {module=modules.approvedModule(domain);}
        catch(SecurityException ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"CLT_R5B_MODULE_CONFIG_INVALID");}
        if(module==null)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"CLT_R5B_MODULE_NOT_APPROVED");
        var actor=identity.actor(token);
        return new Context(token,actor.branch(),CollateralUnallocationIntakePolicy.actor(actor.subject()),
                CollateralAllocationIntakePolicy.clientId(client),module,domain);
    }
    private void checkSnapshot(CollateralUnallocationModels.AllocationLine a,
            CollateralUnallocationPolicy.Normalized n,String module) {
        if(!module.equals(a.moduleId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"CLT_R5B_ALLOCATION_NOT_OWNED_BY_DOMAIN");
        if(!a.used())throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_R5B_ALLOCATION_NOT_ACTIVE");
        if(!a.recordVersion().equals(n.expectedVersion()))
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,"CLT_R5B_ALLOCATION_ETAG_STALE");
        if(!a.fileNumber().equals(n.fileNumber())||!a.currencyCode().equals(n.currencyCode()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_R5B_ALLOCATION_SNAPSHOT_MISMATCH");
        int compare=new BigDecimal(n.requestedAmount()).compareTo(new BigDecimal(a.allocatedAmount()));
        if(compare>0 || ("FULL".equals(n.releaseMode()) && compare!=0)
                || ("PARTIAL".equals(n.releaseMode()) && compare>=0))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_R5B_RELEASE_MODE_OR_AMOUNT_MISMATCH");
    }
    public Receipt submit(String rawCollateral,String rawAllocation,
            CollateralUnallocationModels.PreviewRequest request,String rawKey,String ifMatch,
            Authentication auth) {
        CollateralUnallocationPolicy.Normalized n=CollateralUnallocationPolicy.normalize(request,ifMatch);
        Context caller=caller(auth,n.consumerDomain());
        String collateral=numeric(rawCollateral),allocation=numeric(rawAllocation);
        String key=CollateralUnallocationPolicy.idempotencyKey(rawKey);
        String digest=CollateralUnallocationPolicy.digest(collateral,allocation,
                caller.branch(),caller.client(),n);
        registry.byId(caller.branch(),collateral); // Verify CURRENT branch before any replay visibility.
        var pool=pool();
        try {
            Receipt result=pool.transaction().execute(status -> {
                var prior=journal.byKey(pool,caller.domain(),caller.client(),key,true);
                if(prior!=null){CollateralUnallocationIntakePolicy.replay(prior.payloadDigest(),digest);
                    return receipt(prior,true);}
                // Separate read-only CLT connection: snapshot only, NOT a financial capacity lock.
                registry.byId(caller.branch(),collateral);
                var stored=allocationRepository.allocationById(collateral,allocation,caller.branch());
                checkSnapshot(stored,n,caller.module());
                List<String> blockers=new ArrayList<>(CollateralUnallocationPolicy.blockers(stored,n,caller.module(),true));
                blockers.addAll(CollateralUnallocationIntakePolicy.MANDATORY_BLOCKERS);
                return receipt(journal.insert(pool,collateral,allocation,caller.module(),caller.branch(),
                        caller.subject(),caller.client(),key,digest,n,
                        CollateralUnallocationIntakePolicy.snapshotBlockers(blockers)),false);
            });
            if(result==null)throw new IllegalStateException("CLT_R5B_EMPTY_TRANSACTION");
            return result;
        }catch(DuplicateKeyException ex){
            // Atomic database constraint wins on concurrent same-key requests; replay after rollback.
            Receipt replay=pool.transaction().execute(status -> {
                var prior=journal.byKey(pool,caller.domain(),caller.client(),key,true);
                if(prior==null)throw new CollateralUnallocationIntakePolicy.Conflict(
                        "CLT_R5B_CONCURRENT_REQUEST_CONFLICT");
                CollateralUnallocationIntakePolicy.replay(prior.payloadDigest(),digest);
                return receipt(prior,true);
            });
            if(replay==null)throw new IllegalStateException("CLT_R5B_REPLAY_MISSING");
            return replay;
        }
    }
    public Receipt byId(String rawCollateral,String rawAllocation,String rawRequest,String domain,Authentication auth){
        Context caller=caller(auth,domain);
        String collateral=numeric(rawCollateral),allocation=numeric(rawAllocation),request=numeric(rawRequest);
        registry.byId(caller.branch(),collateral); // Current ownership, not just staging branch.
        var stored=journal.byId(pool(),request,collateral,allocation,caller.branch(),
                caller.domain(),caller.client(),false);
        if(stored==null || !caller.module().equals(stored.moduleId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"CLT_R5B_REQUEST_NOT_FOUND");
        return receipt(stored,false);
    }
    public Receipt cancel(String rawCollateral,String rawAllocation,String rawRequest,
            CancelRequest body,String rawKey,String ifMatch,Authentication auth){
        if(body==null)throw new IllegalArgumentException("CLT_R5B_CANCEL_BODY_REQUIRED");
        String reason=CollateralUnallocationIntakePolicy.reason(body.reasonReference());
        Context caller=caller(auth,body.consumerDomain());
        String collateral=numeric(rawCollateral),allocation=numeric(rawAllocation),request=numeric(rawRequest);
        registry.byId(caller.branch(),collateral); // Recheck owner branch for cancellation.
        String actionKey=CollateralUnallocationPolicy.idempotencyKey(rawKey);
        String etag=CollateralUnallocationPolicy.version(ifMatch);
        String digest=CollateralUnallocationIntakePolicy.cancelDigest(request,caller.branch(),
                caller.client(),caller.domain(),etag,reason);
        var pool=pool();
        Receipt result=pool.transaction().execute(status -> {
            var stored=journal.byId(pool,request,collateral,allocation,caller.branch(),
                    caller.domain(),caller.client(),true);
            if(stored==null || !caller.module().equals(stored.moduleId()))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,"CLT_R5B_REQUEST_NOT_FOUND");
            // On retries, the original ETag and action key still replay the single cancel event.
            String prior=journal.cancelEventDigest(pool,request,actionKey);
            if(prior!=null){CollateralUnallocationIntakePolicy.replay(prior,digest);
                if(!CollateralUnallocationIntakePolicy.CANCELLED.equals(stored.requestStatus()))
                    throw new IllegalStateException("CLT_R5B_EVENT_STATUS_INCONSISTENT");
                return receipt(stored,true);}
            CollateralUnallocationIntakePolicy.canCancel(stored.requestStatus(),stored.recordVersion(),etag);
            return receipt(journal.cancel(pool,stored,actionKey,caller.subject(),digest,reason),false);
        });
        if(result==null)throw new IllegalStateException("CLT_R5B_CANCEL_TRANSACTION_EMPTY");
        return result;
    }
    public List<JournalItem> journal(String rawCollateral,int limit,Authentication auth){
        jwt(auth,"SCOPE_clt.collateral.read");
        String collateral=numeric(rawCollateral),branch=identity.actor(auth).branch();
        registry.byId(branch,collateral); // 404 for unauthorized branch, no cross-branch disclosure.
        return journal.journal(pool(),collateral,branch,CollateralUnallocationIntakePolicy.limit(limit));
    }
}
