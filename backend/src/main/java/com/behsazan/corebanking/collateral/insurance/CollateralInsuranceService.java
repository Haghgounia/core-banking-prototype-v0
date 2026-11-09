package com.behsazan.corebanking.collateral.insurance;

import com.behsazan.corebanking.collateral.insurance.CollateralInsuranceModels.*;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionService;
import com.behsazan.corebanking.collateral.introduction.CollateralPartyEligibility;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceProperties;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryProperties;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** R3B: reads, preflight and optional staging DRAFT; no operational policy mutations. */
@Service
public class CollateralInsuranceService {
    private static final List<String> UNRESOLVED = List.of(
            "G-04_REQUIRED_INSURANCE_TYPES_NOT_PERSISTED",
            "G-06_APPROVAL_CANCELLATION_LIFECYCLE_NOT_PERSISTED",
            "G-09_CONTRACTED_INSURER_API_UNAVAILABLE");
    private final CollateralInsuranceRepository repository;
    private final CollateralInsuranceRequestRepository requests;
    private final CollateralIntroductionService introduction;
    private final CollateralPartyEligibility ownerParty;
    private final CollateralRegistryProperties registry;
    private final CollateralReferenceProperties reference;
    private final CollateralInsuranceProperties properties;
    private final ObjectProvider<CollateralInsuranceWriterPool> writers;
    public CollateralInsuranceService(CollateralInsuranceRepository repository,
            CollateralInsuranceRequestRepository requests, CollateralIntroductionService introduction,
            CollateralPartyEligibility ownerParty,
            CollateralRegistryProperties registry, CollateralReferenceProperties reference,
            CollateralInsuranceProperties properties, ObjectProvider<CollateralInsuranceWriterPool> writers) {
        this.repository = repository;
        this.requests = requests;
        this.introduction = introduction;
        this.ownerParty = ownerParty;
        this.registry = registry;
        this.reference = reference;
        this.properties = properties;
        this.writers = writers;
    }
    private CollateralIntroductionService.Actor actor(Authentication auth) { return introduction.actor(auth); }
    private static void scope(Authentication auth, String authority) {
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream()
                .noneMatch(a -> authority.equals(a.getAuthority())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CLT_INSURANCE_AUTHORITY_REQUIRED");
    }
    private void requireRead() {
        if (!reference.readsEnabled() || !registry.readsEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_INSURANCE_READS_DISABLED");
    }
    private CollateralInsuranceWriterPool writer() {
        if (!properties.requestWritesEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_INSURANCE_STAGING_WRITES_DISABLED");
        var pool = writers.getObject();
        requests.requireSchema(pool);
        return pool;
    }
    public Readiness readiness(Authentication auth) {
        actor(auth);
        boolean enabled = reference.readsEnabled() && registry.readsEnabled();
        if (!properties.requestWritesEnabled()) return new Readiness(enabled, false,
                false, false, false, "R3B_READ_VALIDATE_ONLY");
        boolean schema;
        try { requests.requireSchema(writers.getObject()); schema = true; }
        catch (RuntimeException ex) { schema = false; }
        return new Readiness(enabled, true, schema, false, false,
                schema ? "R3B_STAGING_REQUESTS_ONLY" : "R3B_STAGING_SCHEMA_NOT_READY");
    }
    public PolicyPage list(String collateralId, int page, int size, Authentication auth) {
        scope(auth, "SCOPE_clt.collateral.read");
        var who = actor(auth);
        requireRead();
        return repository.list(repository.parent(who.branch(), CollateralInsurancePolicy.id(collateralId)), page, size);
    }
    public Options options(String collateralId, Authentication auth) {
        scope(auth, "SCOPE_clt.collateral.read");
        var who = actor(auth);
        requireRead();
        return repository.options(repository.parent(who.branch(), CollateralInsurancePolicy.id(collateralId)));
    }
    public List<RenewalOption> renewalOptions(String collateralId, String typeId, Authentication auth) {
        scope(auth, "SCOPE_clt.collateral.read");
        var who = actor(auth);
        requireRead();
        return repository.renewalOptions(repository.parent(who.branch(),
                CollateralInsurancePolicy.id(collateralId)), CollateralInsurancePolicy.id(typeId));
    }
    public Validation validate(String collateralId, CandidateRequest request, Authentication auth) {
        return validateWithScope(collateralId, request, auth, "SCOPE_clt.collateral.introduce");
    }
    /** Re-check current insurer, type, policy period and renewal at independent Stage review. */
    public Validation screenForReview(String collateralId, CandidateRequest request, Authentication auth) {
        return validateWithScope(collateralId, request, auth, "SCOPE_clt.collateral.review");
    }
    private Validation validateWithScope(String collateralId, CandidateRequest request,
            Authentication auth, String requiredScope) {
        scope(auth, requiredScope);
        var who = actor(auth);
        requireRead();
        var candidate = CollateralInsurancePolicy.candidate(request);
        var parent = repository.parent(who.branch(), CollateralInsurancePolicy.id(collateralId));
        return validateCandidate(parent, candidate);
    }
    private Validation validateCandidate(CollateralInsuranceRepository.Parent parent,
            CollateralInsurancePolicy.Candidate c) {
        boolean companyActive = repository.activeReference("company", c.companyId());
        boolean typeActive = repository.activeReference("type", c.insuranceTypeId());
        boolean currencyActive = repository.currencyActive(c.currencyCode());
        boolean ownerEligible = false;
        try { ownerEligible = ownerParty.eligible(parent.ownerPartyId()); }
        catch (RuntimeException ignored) { /* CIF outage/stale owner cannot be approved. */ }
        boolean numberDuplicate = repository.alreadyNumber(c.companyId(), c.insuranceNumber());
        boolean overlap = repository.overlaps(parent, c.insuranceTypeId(), c.validFrom(), c.validTo());
        boolean renewalValid = true;
        if (c.previousInsuranceId() != null) {
            var previous = repository.previous(parent, c.previousInsuranceId());
            renewalValid = previous != null && CollateralInsurancePolicy.validRenewal(
                    parent.collateralId(), c.insuranceTypeId(), previous.collateralId(),
                    previous.insuranceTypeId(), previous.validTo(), c.validFrom(), previous.renewed());
        }
        List<String> blockers = CollateralInsurancePolicy.blockers(parent.insurable(), parent.typeActive(),
                parent.collateralStatus(), companyActive, typeActive, currencyActive, ownerEligible,
                numberDuplicate, overlap, renewalValid);
        return new Validation(parent.collateralId(), blockers.isEmpty(), false, false, blockers,
                UNRESOLVED, "STAGING_ONLY_NO_FINAL_INSURANCE_DML");
    }
    public RequestRecord request(String collateralId, CandidateRequest body, Authentication auth,
            String idem, String correlation) {
        scope(auth, "SCOPE_clt.collateral.introduce");
        var who = actor(auth);
        requireRead();
        var c = CollateralInsurancePolicy.candidate(body);
        String id = CollateralInsurancePolicy.id(collateralId);
        String key = CollateralInsurancePolicy.key(idem);
        String trace = CollateralInsurancePolicy.correlation(correlation);
        String hash = CollateralInsurancePolicy.digest(id + "\u001f" + c.canonical());
        var pool = writer();
        var previous = requests.byKey(pool, who.branch(), who.subject(), key);
        if (previous != null) return replay(previous, hash);
        var parent = repository.parent(who.branch(), id);
        var check = validateCandidate(parent, c);
        if (!check.eligibleForRequest()) throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "CLT_INSURANCE_REQUEST_BLOCKED:" + String.join(",", check.blockers()));
        try {
            return pool.transaction().execute(tx -> {
                var existing = requests.byKey(pool, who.branch(), who.subject(), key);
                if (existing != null) return replay(existing, hash);
                return requests.create(pool, c, parent, who.branch(), who.subject(), key, hash, trace);
            });
        } catch (DuplicateKeyException ex) {
            var winner = requests.byKey(pool, who.branch(), who.subject(), key);
            if (winner != null) return replay(winner, hash);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CLT_INSURANCE_REQUEST_CONFLICT");
        }
    }
    private static RequestRecord replay(CollateralInsuranceRequestRepository.Keyed found, String expected) {
        if (!expected.equals(found.hash())) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "CLT_INSURANCE_IDEMPOTENCY_PAYLOAD_CONFLICT");
        return found.request();
    }
    public RequestPage mine(Authentication auth, int page, int size) {
        scope(auth, "SCOPE_clt.collateral.introduce");
        var who = actor(auth);
        CollateralRegistryPolicy.pagination(page, size);
        return requests.mine(writer(), who.branch(), who.subject(), page, size);
    }
}
