package com.behsazan.corebanking.collateral.amendment;

import com.behsazan.corebanking.collateral.amendment.CollateralAmendmentModels.*;
import com.behsazan.corebanking.collateral.amendment.CollateralAmendmentModels.Record;
import com.behsazan.corebanking.collateral.amendment.CollateralAmendmentRepository.Source;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionPolicy;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.web.server.ResponseStatusException;

/** G-01: post-registration amendment request and independent decision with atomic Oracle audit. */
@Service
public class CollateralAmendmentService {
    private final CollateralAmendmentRepository repo;
    private final TransactionTemplate transaction;
    private final Environment env;

    public CollateralAmendmentService(CollateralAmendmentRepository repo,DataSource source,Environment env) {
        this.repo=repo;
        this.transaction=new TransactionTemplate(new DataSourceTransactionManager(source));
        this.transaction.setTimeout(20);
        this.env=env;
    }
    private record Actor(String subject,String branch) {}
    private Actor actor(Authentication authentication) {
        // Reuse the application's existing, internally derived prototype principal or production principal.
        // Do not accept an identity, branch or permission via the request body or custom user headers.
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"CLT_G01_ACTOR_REQUIRED");
        String subject;
        try { subject=CollateralIntroductionPolicy.actor(jwt.getToken().getSubject()); }
        catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"CLT_G01_ACTOR_INVALID");}
        Object raw=jwt.getToken().getClaims().get("branch_code");
        if (!(raw instanceof String branch))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"CLT_G01_BRANCH_REQUIRED");
        return new Actor(subject,CollateralRegistryPolicy.branch(branch));
    }
    private boolean enabled() {return env.getProperty("core-banking.collateral.amendments.writes-enabled",Boolean.class,false);}
    private void requireReady() {
        if (!enabled())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"CLT_G01_WRITES_DISABLED");
        if (!repo.schemaReady())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"CLT_G01_SCHEMA_MIGRATION_REQUIRED");
    }
    public Readiness readiness(Authentication auth) {
        actor(auth);
        boolean schema;
        try{schema=repo.schemaReady();}catch(RuntimeException e){schema=false;}
        return new Readiness(schema,enabled(),false,true,
                schema?(enabled()?"G01_AMENDMENTS_READY":"G01_WRITES_DISABLED"):"G01_SCHEMA_MIGRATION_REQUIRED");
    }
    private static ResponseStatusException bad(String code) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,code);
    }
    private static String hash(String text){return CollateralIntroductionPolicy.sha256(text);}
    private static String checkedKey(String key) {
        try{return CollateralIntroductionPolicy.key(key);}
        catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage());}
    }
    private static String trace(String correlation) {
        try{return CollateralIntroductionPolicy.correlation(correlation);}
        catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage());}
    }
    private static String number(String id) {
        try{return CollateralAmendmentPolicy.id(id);}
        catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage());}
    }
    private static String version(String id) {
        try{return CollateralAmendmentPolicy.version(id);}
        catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage());}
    }
    private Source source(String id,String branch,boolean lock) {
        Source source=lock?repo.sourceForUpdate(id,branch):repo.source(id,branch);
        if (source==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"CLT_G01_COLLATERAL_NOT_FOUND");
        return source;
    }
    private void eligible(Source source) {
        if (!CollateralAmendmentPolicy.amendableStatus(source.status()))throw bad("CLT_G01_ACTIVE_COLLATERAL_REQUIRED");
        if (repo.hasAnyDependency(source.collateralId()))throw bad("CLT_G01_OPERATIONAL_DEPENDENCY_EXISTS");
    }
    public ListResult list(String collateralId,Authentication auth) {
        Actor who=actor(auth);
        String id=number(collateralId);
        // Branch-scoped existence check before exposing request history.
        source(id,who.branch(),false);
        requireReady();
        return new ListResult(repo.list(id,who.branch()),"ORACLE_CLT_G01_AMENDMENTS");
    }
    /** Proposal never mutates CLT.COLLATERAL; all evidence is persisted in a single transaction. */
    public Record propose(String collateralId,Proposal proposal,Authentication auth,String idempotencyKey,String correlationId) {
        Actor who=actor(auth);
        String id=number(collateralId),key=checkedKey(idempotencyKey),corr=trace(correlationId);
        if(proposal==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"CLT_G01_REQUEST_REQUIRED");
        String expected=version(proposal.expectedRecordVersion());
        String reason;
        try{reason=CollateralAmendmentPolicy.reason(proposal.reason());}
        catch(IllegalArgumentException e){throw bad(e.getMessage());}
        requireReady();
        try{
            return transaction.execute(status->{
                Source current=source(id,who.branch(),true);
                // Canonical idempotency is based on submitted intent, before checking current data.
                // Thus a successful, already-applied request is replayed without creating a new amendment.
                if(proposal.changes()==null)throw bad("CLT_G01_NO_CHANGES");
                String canonical=id+"\n"+expected+"\n"+reason+"\n"
                        +repo.json(new java.util.TreeMap<>(proposal.changes()));
                String digest=hash(canonical);
                Record existing=repo.byIdempotency(who.subject(),who.branch(),key);
                if(existing!=null){
                    if(existing.collateralId().equals(id) && repo.requestHash(existing.amendmentId()).equals(digest))return existing;
                    throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_G01_IDEMPOTENCY_REUSED");
                }
                Map<String,String> after;
                try{after=CollateralAmendmentPolicy.normalize(current.values(),proposal.changes());}
                catch(IllegalArgumentException e){throw bad(e.getMessage());}
                if(!current.version().equals(expected))
                    throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_G01_BASE_VERSION_CHANGED");
                eligible(current);
                if(repo.hasPending(id))
                    throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_G01_PENDING_AMENDMENT_EXISTS");
                return repo.insert(id,who.branch(),who.subject(),expected,current.values(),after,
                        proposal.changes(),reason,key,digest,corr);
            });
        }catch(DataIntegrityViolationException e){throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_G01_CONCURRENT_REQUEST");}
    }
    /** Separate checker: approve applies only four allowed columns, then records immutable audit. */
    public Record decide(String collateralId,String amendmentId,Decision request,Authentication auth,
                         String idempotencyKey,String correlationId) {
        Actor who=actor(auth);
        String id=number(collateralId),aid=number(amendmentId),key=checkedKey(idempotencyKey),corr=trace(correlationId);
        if(request==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"CLT_G01_DECISION_REQUIRED");
        String decision = request.decision();
        if(!"APPROVE".equals(decision) && !"REJECT".equals(decision))throw bad("CLT_G01_DECISION_INVALID");
        String reason;
        try{reason=CollateralAmendmentPolicy.reason(request.reason());}
        catch(IllegalArgumentException e){throw bad(e.getMessage());}
        String expected=version(request.expectedAmendmentVersion());
        if(auth.getAuthorities().stream().noneMatch(a->"SCOPE_clt.collateral.review".equals(a.getAuthority())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"CLT_G01_REVIEWER_SCOPE_REQUIRED");
        requireReady();
        return transaction.execute(status->{
            // Parent first: immutable row lock ordering across submission and decision.
            Source current=source(id,who.branch(),true);
            Record pending=repo.byId(aid,id,who.branch(),true);
            if(pending==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"CLT_G01_REQUEST_NOT_FOUND");
            if(pending.makerSubject().equals(who.subject()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,"CLT_G01_MAKER_CANNOT_APPROVE");
            if(!"PENDING_REVIEW".equals(pending.status())){
                if(repo.exactDecisionReplay(aid,who.subject(),key)
                        && (("APPROVE".equals(decision)&&"APPROVED".equals(pending.status()))
                            || ("REJECT".equals(decision)&&"REJECTED".equals(pending.status())))
                        && Objects.equals(pending.decisionReason(),reason)) return pending;
                throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_G01_ALREADY_DECIDED");
            }
            if(!pending.recordVersion().equals(expected))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_G01_REQUEST_VERSION_CHANGED");
            // A stale request must still be rejectable without mutating the original collateral.
            if("APPROVE".equals(decision)) {
                if(!current.version().equals(pending.baseRecordVersion()))
                    throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_G01_BASE_VERSION_CHANGED");
                eligible(current);
            }
            String statusCode="APPROVE".equals(decision)?"APPROVED":"REJECTED";
            String applied=null;
            if("APPROVE".equals(decision)){
                // Verify a persisted proposal's request digest, independent of stored snapshots.
                String proposalDigest=hash(id+"\n"+pending.baseRecordVersion()+"\n"
                        +pending.reason()+"\n"+repo.json(new java.util.TreeMap<>(pending.changes())));
                if(!Objects.equals(proposalDigest,repo.requestHash(aid)))
                    throw bad("CLT_G01_REQUEST_HASH_MISMATCH");
                // Snapshot must still match current version and exact allowed fields.
                if(!Objects.equals(current.values(),pending.before()))
                    throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_G01_BASE_SNAPSHOT_CHANGED");
                Map<String,String> reconstructed;
                try{reconstructed=CollateralAmendmentPolicy.normalize(pending.before(),pending.changes());}
                catch(IllegalArgumentException e){throw bad("CLT_G01_STORED_PROPOSAL_INVALID");}
                if(!Objects.equals(reconstructed,pending.after()))throw bad("CLT_G01_PROPOSAL_TAMPERED");
                if(repo.apply(current,pending.after(),who.subject())!=1)
                    throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_G01_CONCURRENT_APPLY");
                applied=new java.math.BigDecimal(current.version()).add(java.math.BigDecimal.ONE).toPlainString();
            }
            if(repo.decide(aid,who.branch(),who.subject(),statusCode,reason,key,applied)!=1)
                throw new ResponseStatusException(HttpStatus.CONFLICT,"CLT_G01_CONCURRENT_DECISION");
            repo.audit(aid,"APPROVE".equals(decision)?"AMENDMENT_APPROVED":"AMENDMENT_REJECTED",
                    who.subject(),corr,repo.requestHash(aid));
            return repo.byId(aid,id,who.branch(),false);
        });
    }
}
