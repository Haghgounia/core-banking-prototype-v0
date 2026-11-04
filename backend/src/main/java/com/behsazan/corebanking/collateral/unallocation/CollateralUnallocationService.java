package com.behsazan.corebanking.collateral.unallocation;

import com.behsazan.corebanking.collateral.allocation.CollateralAllocationConsumerBinding;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionService;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryRepository;
import com.behsazan.corebanking.collateral.unallocation.CollateralUnallocationModels.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** R5A read-only allocation history and fail-closed unallocation candidate preflight. */
@Service
public class CollateralUnallocationService {
    private final CollateralUnallocationRepository repository;
    private final CollateralRegistryRepository registry;
    private final CollateralIntroductionService identity;
    private final CollateralAllocationConsumerBinding consumer;
    private final CollateralUnallocationDomainBinding modules;
    private final CollateralUnallocationProperties properties;

    public CollateralUnallocationService(CollateralUnallocationRepository repository,
            CollateralRegistryRepository registry, CollateralIntroductionService identity,
            CollateralAllocationConsumerBinding consumer, CollateralUnallocationDomainBinding modules,
            CollateralUnallocationProperties properties) {
        this.repository=repository;this.registry=registry;this.identity=identity;
        this.consumer=consumer;this.modules=modules;this.properties=properties;
    }
    private JwtAuthenticationToken auth(Authentication auth,String scope) {
        if(!(auth instanceof JwtAuthenticationToken jwt)||!jwt.isAuthenticated())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"CLT_R5A_JWT_REQUIRED");
        if(jwt.getAuthorities().stream().noneMatch(a->scope.equals(a.getAuthority())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"CLT_R5A_SCOPE_REQUIRED");
        return jwt;
    }
    private String id(String raw){return CollateralRegistryPolicy.id(raw).toPlainString();}
    private String accessible(String collateralId,String branch) {
        if(!properties.historyEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"CLT_R5A_HISTORY_READS_DISABLED");
        String id=id(collateralId);
        registry.byId(branch,id); // branch-bound 404: missing and unauthorized indistinguishable
        return id;
    }
    public Readiness readiness(Authentication authentication) {
        JwtAuthenticationToken jwt=auth(authentication,"SCOPE_clt.collateral.read");
        identity.actor(jwt);
        return new Readiness(properties.historyEnabled(),false,false,false,false,
                properties.historyEnabled()?"R5A_READ_ONLY_HISTORY_WITH_NO_RELEASE_AUTHORIZATION":
                    "R5A_HISTORY_DISABLED");
    }
    public List<AllocationLine> allocations(String collateralId,int limit,Authentication authentication){
        JwtAuthenticationToken jwt=auth(authentication,"SCOPE_clt.collateral.read");
        String branch=identity.actor(jwt).branch();
        String id=accessible(collateralId,branch);
        return repository.allocations(id,branch,CollateralUnallocationPolicy.limit(limit));
    }
    public List<TransactionLine> transactions(String collateralId,String allocationId,int limit,
            Authentication authentication){
        JwtAuthenticationToken jwt=auth(authentication,"SCOPE_clt.collateral.read");
        String branch=identity.actor(jwt).branch();
        String id=accessible(collateralId,branch);
        String allocation=id(allocationId);
        repository.allocationById(id,allocation,branch);
        return repository.transactions(id,allocation,branch,CollateralUnallocationPolicy.limit(limit));
    }
    public Preflight preview(String collateralId,String allocationId,
            PreviewRequest request,String idempotencyKey,String ifMatch,Authentication authentication){
        JwtAuthenticationToken jwt=auth(authentication,"SCOPE_clt.collateral.read");
        String branch=identity.actor(jwt).branch();
        return evaluate(collateralId,allocationId,request,idempotencyKey,ifMatch,
                branch,"BROWSER_ANALYST_NOT_CONSUMER",false);
    }
    public Preflight servicePreflight(String collateralId,String allocationId,
            PreviewRequest request,String idempotencyKey,String ifMatch,Authentication authentication){
        JwtAuthenticationToken jwt=auth(authentication,"SCOPE_clt.unallocation.preflight");
        CollateralUnallocationPolicy.Normalized normalized=CollateralUnallocationPolicy.normalize(request,ifMatch);
        // Verify signed client_id+gty and bank-managed domain binding BEFORE any database access.
        final String client;
        try {client=consumer.authenticatedClient(jwt,normalized.consumerDomain());}
        catch(SecurityException e){throw new ResponseStatusException(HttpStatus.FORBIDDEN,e.getMessage());}
        String branch=identity.actor(jwt).branch();
        return evaluateNormalized(collateralId,allocationId,normalized,idempotencyKey,branch,client,true);
    }
    private Preflight evaluate(String collateralId,String allocationId,
            PreviewRequest request,String key,String ifMatch,String branch,String caller,boolean service){
        return evaluateNormalized(collateralId,allocationId,
                CollateralUnallocationPolicy.normalize(request,ifMatch),key,branch,caller,service);
    }
    private Preflight evaluateNormalized(String rawCollateral,String rawAllocation,
            CollateralUnallocationPolicy.Normalized request,String rawKey,String branch,
            String caller,boolean service){
        String key=CollateralUnallocationPolicy.idempotencyKey(rawKey);
        // Service domain mapping is checked BEFORE any CLT SELECT; absent mapping means deny.
        final String module;
        try {module=modules.approvedModule(request.consumerDomain());}
        catch(SecurityException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "CLT_R5A_DOMAIN_MODULE_MAPPING_CONFIGURATION_INVALID");}
        if(service && module==null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "CLT_R5A_CONSUMER_MODULE_NOT_CONFIGURED");
        String collateral=accessible(rawCollateral,branch);
        String allocation=id(rawAllocation);
        AllocationLine stored=repository.allocationById(collateral,allocation,branch);
        // Do not disclose USED/version/case details of another domain's allocation.
        if(service && !module.equals(stored.moduleId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "CLT_R5A_ALLOCATION_NOT_FOUND_FOR_CONSUMER");
        List<String> blockers=CollateralUnallocationPolicy.blockers(stored,request,module,service);
        String digest=CollateralUnallocationPolicy.digest(collateral,allocation,branch,caller,request);
        return new Preflight(collateral,allocation,request.consumerDomain(),request.fileNumber(),
                request.requestedAmount(),request.currencyCode(),request.releaseMode(),key,digest,
                stored.recordVersion(),stored.used(),stored.recordVersion().equals(request.expectedVersion()),
                module!=null&&module.equals(stored.moduleId()),service,false,false,false,false,
                blockers,"DENIED_NO_FINANCIAL_UNALLOCATION",
                "R5A_NON_ATOMIC_READ_ONLY_NO_CONSUMER_ATTESTATION_NO_DURABLE_IDEMPOTENCY");
    }
}
