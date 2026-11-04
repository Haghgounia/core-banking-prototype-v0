package com.behsazan.corebanking.collateral.release;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionService;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryRepository;
import com.behsazan.corebanking.collateral.release.CollateralFinalReleaseModels.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Branch-bound evidence only; NO approved status, no legal hold assertion, no financial change. */
@Service
public class CollateralFinalReleaseService {
    private final CollateralFinalReleaseRepository repository;
    private final CollateralRegistryRepository registry;
    private final CollateralIntroductionService identity;
    private final CollateralFinalReleaseProperties properties;
    public CollateralFinalReleaseService(CollateralFinalReleaseRepository repository,
            CollateralRegistryRepository registry,CollateralIntroductionService identity,
            CollateralFinalReleaseProperties properties){
        this.repository=repository;this.registry=registry;this.identity=identity;this.properties=properties;
    }
    private String branch(Authentication auth){
        if(!(auth instanceof JwtAuthenticationToken jwt)||!jwt.isAuthenticated())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"CLT_R5C_JWT_REQUIRED");
        if(jwt.getAuthorities().stream().noneMatch(a->"SCOPE_clt.collateral.read".equals(a.getAuthority())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"CLT_R5C_READ_SCOPE_REQUIRED");
        return identity.actor(jwt).branch();
    }
    private String accessible(String rawId,String branch){
        if(!properties.readsEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"CLT_R5C_EVIDENCE_READS_DISABLED");
        String id=CollateralFinalReleasePolicy.positiveId(rawId);
        registry.byId(branch,id); // missing and branch-unauthorized are identical 404
        return id;
    }
    public Readiness readiness(Authentication auth){
        branch(auth);
        return new Readiness(properties.readsEnabled(),false,false,false,false,
                "DENIED_FINAL_RELEASE_NOT_AUTHORIZED");
    }
    private Evidence evidenceFor(String id,String branch){
        Snapshot s=repository.snapshot(id,branch);
        var f=new CollateralFinalReleasePolicy.Facts(s.activeAllocationCount(),s.totalAllocationCount(),
                s.crossBranchAllocationCount(),s.invalidUsageCount(),s.pendingTransactionCount(),
                s.releaseInquiryRequired(),s.terminalStatusReported(),null,s.recordVersion());
        return new Evidence(s,CollateralFinalReleasePolicy.blockers(f),
                "DENIED_FINAL_RELEASE_NOT_AUTHORIZED",false,
                "NON_ATOMIC_CLT_READ_ONLY_NOT_LEGAL_OR_CONSUMER_ATTESTATION");
    }
    public Evidence evidence(String rawId,Authentication auth){
        String branch=branch(auth);
        return evidenceFor(accessible(rawId,branch),branch);
    }
    public List<StatusEvent> history(String rawId,int limit,Authentication auth){
        String branch=branch(auth);
        String id=accessible(rawId,branch);
        return repository.statusHistory(id,branch,CollateralFinalReleasePolicy.limit(limit));
    }
    public PreviewResult preview(String rawId,PreviewRequest request,String ifMatch,
            String idempotencyKey,Authentication auth){
        String branch=branch(auth);
        String id=accessible(rawId,branch);
        if(request==null)throw new IllegalArgumentException("CLT_R5C_REQUEST_REQUIRED");
        String reason=CollateralFinalReleasePolicy.reason(request.reasonReference());
        String expected=CollateralFinalReleasePolicy.strongEtag(ifMatch);
        String key=CollateralFinalReleasePolicy.idempotencyKey(idempotencyKey);
        Evidence evidence=evidenceFor(id,branch);
        Snapshot s=evidence.snapshot();
        var facts=new CollateralFinalReleasePolicy.Facts(s.activeAllocationCount(),s.totalAllocationCount(),
                s.crossBranchAllocationCount(),s.invalidUsageCount(),s.pendingTransactionCount(),
                s.releaseInquiryRequired(),s.terminalStatusReported(),expected,s.recordVersion());
        return new PreviewResult(id,expected,s.recordVersion(),reason,
                CollateralFinalReleasePolicy.digest(id,branch,expected,reason,key),
                false,false,false,CollateralFinalReleasePolicy.blockers(facts),
                "DENIED_NO_FINAL_RELEASE_OR_STATUS_MUTATION");
    }
}
