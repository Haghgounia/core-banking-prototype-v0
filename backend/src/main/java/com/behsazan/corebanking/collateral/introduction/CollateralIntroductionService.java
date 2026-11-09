package com.behsazan.corebanking.collateral.introduction;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionModels.*;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionPolicy.Normalized;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionRepository.AttributeRule;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionRepository.TypeRules;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceProperties;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryProperties;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Bank-safe split: preflight and persisted DRAFT; no create/confirm in CLT.COLLATERAL. */
@Service
public final class CollateralIntroductionService {
    public record Actor(String subject, String branch) {}

    private final CollateralIntroductionRepository repository;
    private final CollateralPartyEligibility party;
    private final CollateralIntroductionProperties properties;
    private final CollateralRegistryProperties registry;
    private final CollateralReferenceProperties reference;
    private final ObjectProvider<CollateralIntroductionWriterPool> writer;

    public CollateralIntroductionService(CollateralIntroductionRepository repository,
            CollateralPartyEligibility party, CollateralIntroductionProperties properties,
            CollateralRegistryProperties registry, CollateralReferenceProperties reference,
            ObjectProvider<CollateralIntroductionWriterPool> writer) {
        this.repository = repository;
        this.party = party;
        this.properties = properties;
        this.registry = registry;
        this.reference = reference;
        this.writer = writer;
    }
    public Actor actor(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "CLT_INTRODUCTION_JWT_REQUIRED");
        String subject = CollateralIntroductionPolicy.actor(jwt.getToken().getSubject());
        Object branch = jwt.getToken().getClaims().get("branch_code");
        if (!(branch instanceof String value))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CLT_INTRODUCTION_BRANCH_REQUIRED");
        return new Actor(subject, CollateralRegistryPolicy.branch(value));
    }
    public Readiness readiness(Authentication authentication) {
        actor(authentication);
        boolean ready = false;
        if (properties.draftWritesEnabled() && registry.readsEnabled() && reference.readsEnabled()) {
            try {
                CollateralIntroductionWriterPool staging = writer.getIfAvailable();
                ready = staging != null && repository.draftSchemaReady(staging);
            } catch (RuntimeException unavailable) { ready = false; }
        }
        return new Readiness(registry.readsEnabled() && reference.readsEnabled(),
                ready, false, false,
                ready ? "R2_DRAFT_STAGING_READY" : "R2_DRAFT_SCHEMA_NOT_READY",
                "G-17_NOT_APPROVED");
    }
    private void requireRead() {
        if (!registry.readsEnabled() || !reference.readsEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "CLT_INTRODUCTION_READS_DISABLED");
    }
    public Validation validate(Request request, Authentication authentication) {
        actor(authentication);
        requireRead();
        Normalized normalized = CollateralIntroductionPolicy.normalize(request);
        return validateNormalized(normalized);
    }
    private Validation validateNormalized(Normalized n) {
        List<String> blockers = new ArrayList<>();
        try {
            if (!party.eligible(n.partyId())) blockers.add("CLT_PARTY_ELIGIBILITY_NOT_VERIFIED");
        } catch (RuntimeException ex) {
            // Do not treat a CIF outage or unknown Party as an approved identity.
            blockers.add("CLT_PARTY_ELIGIBILITY_NOT_VERIFIED");
        }
        TypeRules type = repository.rules(n.collateralTypeId());
        if (!type.active()) blockers.add("CLT_TYPE_NOT_ACTIVE");
        try {
            CollateralSerialRangePolicy.assess(type.countable(), n.currencyCode(),
                    n.estimateAmount(), n.itemRanges());
        } catch (IllegalArgumentException e) { blockers.add(e.getMessage()); }
        if (!repository.currencyActive(n.currencyCode())) blockers.add("CLT_CURRENCY_NOT_ACTIVE");
        Map<String, AttributeRule> rules = new HashMap<>();
        for (AttributeRule rule : type.attributes()) rules.put(rule.id(), rule);
        Map<String, Integer> seen = new HashMap<>();
        Set<String> pairs = new HashSet<>();
        for (AttributeInput input : n.attributes()) {
            AttributeRule rule = rules.get(input.attributeId());
            if (rule == null) { blockers.add("CLT_ATTRIBUTE_NOT_CONFIGURED:" + input.attributeId()); continue; }
            seen.merge(input.attributeId(), 1, Integer::sum);
            if (!pairs.add(input.attributeId() + "\u0000" + input.value()))
                blockers.add("CLT_ATTRIBUTE_DUPLICATE_VALUE:" + input.attributeId());
            if (!rule.manualEntryAllowed())
                blockers.add("CLT_ATTRIBUTE_MANUAL_ENTRY_DISABLED:" + input.attributeId());
            try {
                CollateralIntroductionPolicy.validateValue(rule.dataType(), input.value(), rule.listValue());
            } catch (IllegalArgumentException e) { blockers.add(e.getMessage() + ":" + input.attributeId()); }
        }
        for (AttributeRule rule : type.attributes()) {
            if (rule.required() && !seen.containsKey(rule.id()))
                blockers.add("CLT_ATTRIBUTE_REQUIRED:" + rule.id());
            if (!rule.repeatable() && seen.getOrDefault(rule.id(), 0) > 1)
                blockers.add("CLT_ATTRIBUTE_NOT_REPEATABLE:" + rule.id());
        }
        return new Validation(blockers.isEmpty(), false, List.copyOf(blockers),
                n.partyId(), n.collateralTypeId(), type.attributes().size(), "FINAL_REGISTRATION_DISABLED");
    }
    public RegistrationReview registrationPreflight(Request request, Authentication authentication) {
        actor(authentication);
        requireRead();
        Normalized normalized = CollateralIntroductionPolicy.normalize(request);
        Validation draft = validateNormalized(normalized);
        // The complete operational workflow is never activated from a client flag.
        // G-03, G-14 and G-17 require approved DB and business decisions.
        var type = repository.rules(normalized.collateralTypeId());
        int count = 0;
        String total = "0";
        try {
            var assessment = CollateralSerialRangePolicy.assess(type.countable(),
                    normalized.currencyCode(), normalized.estimateAmount(), normalized.itemRanges());
            count = assessment.itemCount();
            total = assessment.totalAmountIRR();
        } catch (IllegalArgumentException ignored) {
            // An invalid range appears in draftBlockers, not as an approved item count.
        }
        return new RegistrationReview(draft.eligibleForDraft(), false, draft.blockers(),
                List.of("G-03_FINAL_RANGE_PERSISTENCE_NOT_APPROVED",
                        "G-14_STABLE_ASSET_IDENTITY_NOT_APPROVED",
                        "G-17_REGISTRATION_NUMBERING_NOT_APPROVED",
                        "G-01_AMENDMENT_STORAGE_OPEN"),
                normalized.itemRanges().size(), Integer.toString(count), total,
                "NO_SERVER_ISSUER_APPROVED", "DRAFT_STAGE_ONLY_NO_COLLATERAL_DML");
    }
    private CollateralIntroductionWriterPool writer() {
        if (!properties.draftWritesEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_INTRODUCTION_DRAFT_WRITES_DISABLED");
        CollateralIntroductionWriterPool staging = writer.getIfAvailable();
        if (staging == null || !repository.draftSchemaReady(staging))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_INTRODUCTION_DRAFT_SCHEMA_NOT_READY");
        return staging;
    }
    public Draft createDraft(Request request, Authentication authentication,
                             String idempotencyKey, String correlationId) {
        Actor who = actor(authentication);
        requireRead();
        String key = CollateralIntroductionPolicy.key(idempotencyKey);
        String trace = CollateralIntroductionPolicy.correlation(correlationId);
        Normalized normalized = CollateralIntroductionPolicy.normalize(request);
        CollateralIntroductionWriterPool pool = writer();
        String digest = CollateralIntroductionPolicy.sha256(normalized.canonical());
        // A replay retains its original result even if a reference value or CIF status
        // changed since creation. The payload must still match exactly.
        Draft replay = repository.findByKey(pool, key, who.subject(), who.branch());
        if (replay != null) return replayOrConflict(pool, replay, digest);
        Validation check = validateNormalized(normalized);
        if (!check.eligibleForDraft())
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "CLT_INTRODUCTION_PREFLIGHT_BLOCKED:" + String.join(",", check.blockers()));
        try {
            return pool.transaction().execute(tx -> {
                Draft earlier = repository.findByKey(pool, key, who.subject(), who.branch());
                if (earlier != null) return replayOrConflict(pool, earlier, digest);
                return repository.insert(pool, normalized, who.subject(), who.branch(), key, digest, trace);
            });
        } catch (DuplicateKeyException conflict) {
            // Race on unique maker/branch/idempotency key; after rollback, read the winner.
            Draft winner = repository.findByKey(pool, key, who.subject(), who.branch());
            if (winner != null) return replayOrConflict(pool, winner, digest);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CLT_INTRODUCTION_CONCURRENT_REQUEST");
        }
    }
    private Draft replayOrConflict(CollateralIntroductionWriterPool pool, Draft previous, String fingerprint) {
        String storedHash = repository.previousHash(pool, previous.draftId());
        if (!fingerprint.equals(storedHash))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CLT_INTRODUCTION_IDEMPOTENCY_CONFLICT");
        return previous;
    }
    /** R2 operational worklist; this does not expose other makers or create final records. */
    public DraftPage mine(Authentication authentication, int page, int size) {
        Actor who = actor(authentication);
        requireRead();
        if (page < 0 || page > 1000 || size < 1 || size > 50)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CLT_DRAFT_PAGE_INVALID");
        return repository.listMine(writer(), who.subject(), who.branch(), page, size);
    }

    public Draft getDraft(Authentication authentication, String draftId) {
        Actor who = actor(authentication);
        requireRead();
        CollateralIntroductionWriterPool pool = writer();
        Draft found = repository.find(pool, CollateralIntroductionPolicy.id(draftId),
                who.subject(), who.branch());
        if (found == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CLT_INTRODUCTION_DRAFT_NOT_FOUND");
        return found;
    }
}
