package com.behsazan.corebanking.collateral.sponsor;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionService;
import com.behsazan.corebanking.collateral.introduction.CollateralPartyEligibility;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceProperties;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryProperties;
import com.behsazan.corebanking.collateral.sponsor.CollateralSponsorModels.*;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Sponsor identity belongs to CLT, but no debtor or consuming case is bound at intake. */
@Service
public class CollateralSponsorService {
    private final CollateralSponsorRepository sponsors;
    private final CollateralSponsorRequestRepository requests;
    private final CollateralIntroductionService introduction;
    private final CollateralPartyEligibility party;
    private final CollateralSponsorProperties properties;
    private final CollateralRegistryProperties registry;
    private final CollateralReferenceProperties reference;
    private final ObjectProvider<CollateralSponsorWriterPool> writers;

    public CollateralSponsorService(CollateralSponsorRepository sponsors,
            CollateralSponsorRequestRepository requests, CollateralIntroductionService introduction,
            CollateralPartyEligibility party, CollateralSponsorProperties properties,
            CollateralRegistryProperties registry, CollateralReferenceProperties reference,
            ObjectProvider<CollateralSponsorWriterPool> writers) {
        this.sponsors = sponsors;
        this.requests = requests;
        this.introduction = introduction;
        this.party = party;
        this.properties = properties;
        this.registry = registry;
        this.reference = reference;
        this.writers = writers;
    }
    private CollateralIntroductionService.Actor actor(Authentication authentication) {
        return introduction.actor(authentication);
    }
    private void requireRead() {
        if (!registry.readsEnabled() || !reference.readsEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_SPONSOR_READS_DISABLED");
    }
    private static void scope(Authentication auth, String expected) {
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream()
                .noneMatch(a -> expected.equals(a.getAuthority())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "CLT_SPONSOR_AUTHORITY_REQUIRED");
    }
    private CollateralSponsorWriterPool writer() {
        if (!properties.requestWritesEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_SPONSOR_REQUEST_WRITES_DISABLED");
        CollateralSponsorWriterPool pool = writers.getObject();
        requests.requireSchema(pool);
        return pool;
    }
    public Readiness readiness(Authentication authentication) {
        actor(authentication);
        boolean canRead = registry.readsEnabled() && reference.readsEnabled();
        if (!properties.requestWritesEnabled())
            return new Readiness(canRead, false, false, false, false,
                    "R3A_READ_AND_VALIDATE_ONLY");
        boolean verified;
        try { requests.requireSchema(writers.getObject()); verified = true; }
        catch (RuntimeException ex) { verified = false; }
        return new Readiness(canRead, true, verified, false, false,
                verified ? "R3A_STAGING_REQUESTS_ONLY" : "R3A_SCHEMA_NOT_READY");
    }
    public SponsorPage list(String collateralId, int page, int size, Authentication authentication) {
        scope(authentication, "SCOPE_clt.collateral.read");
        var who = actor(authentication);
        requireRead();
        return sponsors.list(sponsors.parent(who.branch(), CollateralSponsorPolicy.id(collateralId)), page, size);
    }
    public Validation validate(String collateralId, CandidateRequest request, Authentication authentication) {
        return validateWithScope(collateralId, request, authentication, "SCOPE_clt.collateral.introduce");
    }
    /** Internal Stage screening: an independent reviewer need not hold maker's introduce scope. */
    public Validation screenForReview(String collateralId, CandidateRequest request,
            Authentication authentication) {
        return validateWithScope(collateralId, request, authentication, "SCOPE_clt.collateral.review");
    }
    private Validation validateWithScope(String collateralId, CandidateRequest request,
            Authentication authentication, String requiredScope) {
        scope(authentication, requiredScope);
        var who = actor(authentication);
        requireRead();
        if (request == null) throw new IllegalArgumentException("CLT_SPONSOR_BODY_REQUIRED");
        String id = CollateralSponsorPolicy.id(collateralId);
        var candidate = CollateralSponsorPolicy.candidate(request.partyId(),
                request.depositNumber(), request.justification());
        return validateCandidate(id, candidate, who.branch());
    }
    private Validation validateCandidate(String collateralId, CollateralSponsorPolicy.Candidate value,
            String branch) {
        var collateral = sponsors.parent(branch, collateralId);
        boolean duplicate = sponsors.registered(branch, collateralId, value.partyId());
        boolean eligible = false;
        try { eligible = party.eligible(value.partyId()); }
        catch (RuntimeException ignored) { /* Fail closed on CIF unavailable, invalid or stale Party. */ }
        List<String> blockers = CollateralSponsorPolicy.blockers(collateral.ownerPartyId(),
                value.partyId(), collateral.collateralStatus(), collateral.sponsorRequired(),
                collateral.typeStatus(), duplicate, eligible);
        return new Validation(collateralId, value.partyId(), blockers.isEmpty(), false, blockers,
                "G-11_MINIMUM_COUNT_UNDEFINED", "SUC-08_ALLOCATION_ONLY");
    }
    public RequestRecord request(String collateralId, CandidateRequest request,
            Authentication authentication, String idem, String correlation) {
        scope(authentication, "SCOPE_clt.collateral.introduce");
        var who = actor(authentication);
        requireRead();
        if (request == null) throw new IllegalArgumentException("CLT_SPONSOR_BODY_REQUIRED");
        String id = CollateralSponsorPolicy.id(collateralId);
        var value = CollateralSponsorPolicy.candidate(request.partyId(),
                request.depositNumber(), request.justification());
        String key = CollateralSponsorPolicy.key(idem);
        String trace = CollateralSponsorPolicy.correlation(correlation);
        String hash = CollateralSponsorPolicy.digest(id + "\u001f" + value.canonical());
        var pool = writer();
        var existing = requests.byKey(pool, who.branch(), who.subject(), key);
        if (existing != null) return replay(existing, hash);
        Validation checked = validateCandidate(id, value, who.branch());
        if (!checked.eligibleForRequest())
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "CLT_SPONSOR_REQUEST_BLOCKED:" + String.join(",", checked.blockers()));
        try {
            return pool.transaction().execute(tx -> {
                var previous = requests.byKey(pool, who.branch(), who.subject(), key);
                if (previous != null) return replay(previous, hash);
                return requests.create(pool, value, id, who.branch(), who.subject(), key, hash, trace);
            });
        } catch (DuplicateKeyException conflict) {
            var winner = requests.byKey(pool, who.branch(), who.subject(), key);
            if (winner != null) return replay(winner, hash);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CLT_SPONSOR_PENDING_REQUEST_ALREADY_EXISTS");
        }
    }
    private static RequestRecord replay(CollateralSponsorRequestRepository.Keyed saved, String hash) {
        if (!hash.equals(saved.hash()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CLT_SPONSOR_IDEMPOTENCY_PAYLOAD_CONFLICT");
        return saved.request();
    }
    public RequestPage mine(Authentication authentication, int page, int size) {
        scope(authentication, "SCOPE_clt.collateral.introduce");
        var who = actor(authentication);
        CollateralRegistryPolicy.pagination(page, size);
        return requests.mine(writer(), who.branch(), who.subject(), page, size);
    }
}
