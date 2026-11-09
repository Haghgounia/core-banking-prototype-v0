package com.behsazan.corebanking.collateral.valuation;

import com.behsazan.corebanking.collateral.valuation.CollateralValuationModels.*;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionService;
import com.behsazan.corebanking.collateral.introduction.CollateralPartyEligibility;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceProperties;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** SVC-08 / BUC-08: history, evidence and opt-in DRAFT requests only; no final write. */
@Service
public class CollateralValuationService {
    private static final List<String> UNRESOLVED = List.of(
            "G-07_UI_APPROVED_VS_ORACLE_FINAL_NOT_APPROVED",
            "G-18_APPRAISABILITY_AND_INTERVAL_UNIT_MISSING_FROM_PHYSICAL_TYPE",
            "G-09_VALUER_AUTHORITY_AND_FX_CONTRACT_NOT_VERIFIED");
    private final CollateralValuationRepository repo;
    private final CollateralValuationRequestRepository requests;
    private final CollateralIntroductionService introduction;
    private final CollateralPartyEligibility ownerParty;
    private final CollateralReferenceProperties reference;
    private final CollateralRegistryProperties registry;
    private final CollateralValuationProperties properties;
    private final ObjectProvider<CollateralValuationWriterPool> writers;
    public CollateralValuationService(CollateralValuationRepository repo,
            CollateralValuationRequestRepository requests, CollateralIntroductionService introduction,
            CollateralPartyEligibility ownerParty, CollateralReferenceProperties reference,
            CollateralRegistryProperties registry, CollateralValuationProperties properties,
            ObjectProvider<CollateralValuationWriterPool> writers) {
        this.repo = repo; this.requests = requests; this.introduction = introduction;
        this.ownerParty = ownerParty; this.reference = reference; this.registry = registry;
        this.properties = properties; this.writers = writers;
    }
    private CollateralIntroductionService.Actor actor(Authentication auth) { return introduction.actor(auth); }
    private static void requireAuthority(Authentication auth, String authority) {
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream()
                .noneMatch(x -> authority.equals(x.getAuthority())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CLT_VALUATION_AUTHORITY_REQUIRED");
    }
    private void readGate() {
        if (!reference.readsEnabled() || !registry.readsEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "CLT_VALUATION_READS_DISABLED");
    }
    private CollateralValuationWriterPool writer() {
        if (!properties.requestWritesEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_VALUATION_STAGING_WRITES_DISABLED");
        CollateralValuationWriterPool pool = writers.getObject();
        requests.requireSchema(pool);
        return pool;
    }
    public Readiness readiness(Authentication auth) {
        requireAuthority(auth, "SCOPE_clt.collateral.read"); actor(auth);
        boolean enabled = reference.readsEnabled() && registry.readsEnabled();
        if (!properties.requestWritesEnabled())
            return new Readiness(enabled, false, false, false, false, "R3C_READ_VALIDATE_ONLY");
        boolean ready;
        try { requests.requireSchema(writers.getObject()); ready = true; }
        catch (RuntimeException ex) { ready = false; }
        return new Readiness(enabled, true, ready, false, false,
                ready ? "R3C_STAGING_REQUESTS_ONLY" : "R3C_STAGING_SCHEMA_UNAVAILABLE");
    }
    public ValuationPage history(String collateralId, Authentication auth, int page, int size) {
        requireAuthority(auth, "SCOPE_clt.collateral.read");
        var who = actor(auth); readGate();
        return repo.page(repo.parent(who.branch(), CollateralValuationPolicy.id(collateralId)), page, size);
    }
    public Options options(String collateralId, Authentication auth) {
        requireAuthority(auth, "SCOPE_clt.collateral.read");
        var who = actor(auth); readGate();
        return repo.options(repo.parent(who.branch(), CollateralValuationPolicy.id(collateralId)));
    }
    public CurrentEvidence current(String collateralId, Authentication auth) {
        requireAuthority(auth, "SCOPE_clt.collateral.read");
        var who = actor(auth); readGate();
        var parent = repo.parent(who.branch(), CollateralValuationPolicy.id(collateralId));
        var row = repo.current(parent);
        String asOf = repo.oracleTehranDate();
        var recorded = CollateralValuationPolicy.recordedPeriod(parent.currentValuationId(),
                row == null ? null : row.valuationId(), row == null ? null : row.statusCode(),
                row == null ? null : row.effectiveFrom(), row == null ? null : row.effectiveTo(),
                asOf, row != null && parent.currencyCode().equals(row.currencyCode()));
        return new CurrentEvidence(parent.collateralId(), parent.collateralNumber(), asOf,
                parent.currentValuationId(), row, recorded.state(), recorded.withinRecordedPeriod(),
                false, recorded.blockers(), UNRESOLVED,
                "ORACLE_CLOCK_ASIA_TEHRAN_NOT_OFFICIAL_BUSINESS_DATE");
    }
    public Validation validate(String collateralId, CandidateRequest input, Authentication auth) {
        return validateWithScope(collateralId, input, auth, "SCOPE_clt.collateral.introduce");
    }
    /** Review-time re-evaluation of canonical reference and current supersession evidence. */
    public Validation screenForReview(String collateralId, CandidateRequest input, Authentication auth) {
        return validateWithScope(collateralId, input, auth, "SCOPE_clt.collateral.review");
    }
    private Validation validateWithScope(String collateralId, CandidateRequest input,
            Authentication auth, String requiredScope) {
        requireAuthority(auth, requiredScope);
        var who = actor(auth); readGate();
        var value = CollateralValuationPolicy.normalize(input);
        var parent = repo.parent(who.branch(), CollateralValuationPolicy.id(collateralId));
        return validateValue(parent, value);
    }
    private Validation validateValue(CollateralValuationRepository.Parent parent,
            CollateralValuationPolicy.Candidate value) {
        boolean ownerEligible;
        try { ownerEligible = ownerParty.eligible(parent.ownerPartyId()); }
        catch (RuntimeException ex) { ownerEligible = false; /* CIF outage fails closed. */ }
        boolean currentFinal = value.supersedesValuationId() != null
                && repo.currentIsFinal(parent, value.supersedesValuationId());
        List<String> blockers = CollateralValuationPolicy.candidateBlockers(
                repo.activeReference("type", value.typeCode()),
                repo.activeReference("method", value.methodCode()),
                repo.activeReference("currency", value.currencyCode()),
                ownerEligible, parent.typeActive(), parent.collateralStatus(),
                parent.currentValuationId(), value.supersedesValuationId(), currentFinal);
        List<String> gaps = new ArrayList<>(UNRESOLVED);
        if (!parent.currencyCode().equals(value.currencyCode()))
            gaps.add("G-09_CURRENCY_CONVERSION_NOT_AUTHORIZED_FOR_ALLOCATION");
        return new Validation(parent.collateralId(), blockers.isEmpty(), false, false,
                blockers, gaps, "STAGING_ONLY_NO_CLT_VALUATION_HISTORY_DML");
    }
    public RequestRecord request(String collateralId, CandidateRequest body,
            Authentication auth, String key, String correlation) {
        requireAuthority(auth, "SCOPE_clt.collateral.introduce");
        var who = actor(auth); readGate();
        var value = CollateralValuationPolicy.normalize(body);
        String id = CollateralValuationPolicy.id(collateralId);
        String idem = CollateralValuationPolicy.requestKey(key);
        String trace = CollateralValuationPolicy.correlation(correlation);
        String hash = CollateralValuationPolicy.digest(id + "\u001f" + value.canonical());
        var pool = writer();
        var prior = requests.byKey(pool, who.branch(), who.subject(), idem);
        if (prior != null) return replay(prior, hash);
        var parent = repo.parent(who.branch(), id);
        var validation = validateValue(parent, value);
        if (!validation.eligibleForStagingRequest())
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "CLT_VALUATION_STAGING_REQUEST_BLOCKED:" + String.join(",", validation.blockers()));
        try {
            return pool.transaction().execute(tx -> {
                var found = requests.byKey(pool, who.branch(), who.subject(), idem);
                if (found != null) return replay(found, hash);
                return requests.create(pool, value, parent, who.branch(), who.subject(), idem, hash, trace);
            });
        } catch (DuplicateKeyException ex) {
            var winner = requests.byKey(pool, who.branch(), who.subject(), idem);
            if (winner != null) return replay(winner, hash);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CLT_VALUATION_REQUEST_CONFLICT");
        }
    }
    private static RequestRecord replay(CollateralValuationRequestRepository.Keyed found, String expected) {
        if (!expected.equals(found.hash())) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "CLT_VALUATION_IDEMPOTENCY_PAYLOAD_CONFLICT");
        return found.request();
    }
    public RequestPage mine(Authentication auth, int page, int size) {
        requireAuthority(auth, "SCOPE_clt.collateral.introduce");
        var who = actor(auth);
        CollateralRegistryPolicy.pagination(page, size);
        return requests.mine(writer(), who.branch(), who.subject(), page, size);
    }
}
