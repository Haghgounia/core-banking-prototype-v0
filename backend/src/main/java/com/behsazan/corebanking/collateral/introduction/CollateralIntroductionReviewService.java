package com.behsazan.corebanking.collateral.introduction;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionReviewModels.Review;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionReviewModels.ReviewSnapshot;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionReviewModels.ReviewReadiness;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionReviewPolicy.Decision;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Maker/Checker review of immutable staging drafts; NOT a registration permission. */
@Service
public final class CollateralIntroductionReviewService {
    private final CollateralIntroductionService introduction;
    private final CollateralIntroductionRepository drafts;
    private final CollateralIntroductionReviewRepository reviews;
    private final CollateralIntroductionProperties properties;
    private final ObjectProvider<CollateralIntroductionWriterPool> writer;

    public CollateralIntroductionReviewService(CollateralIntroductionService introduction,
            CollateralIntroductionRepository drafts,
            CollateralIntroductionReviewRepository reviews,
            CollateralIntroductionProperties properties,
            ObjectProvider<CollateralIntroductionWriterPool> writer) {
        this.introduction = introduction;
        this.drafts = drafts;
        this.reviews = reviews;
        this.properties = properties;
        this.writer = writer;
    }
    private boolean enabled() {
        return properties.draftWritesEnabled() && properties.reviewWorkflowEnabled();
    }
    private CollateralIntroductionWriterPool pool() {
        if (!enabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "CLT_REVIEW_WORKFLOW_DISABLED");
        CollateralIntroductionWriterPool p = writer.getObject();
        reviews.requireSchema(p);
        return p;
    }
    private void requireScope(Authentication auth, String authority) {
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream()
                .noneMatch(a -> authority.equals(a.getAuthority())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CLT_REVIEW_SCOPE_REQUIRED");
    }
    public ReviewReadiness readiness(Authentication auth) {
        introduction.actor(auth);
        if (!enabled()) return new ReviewReadiness(false, false, false, "REVIEW_DISABLED");
        try {
            reviews.requireSchema(writer.getObject());
            return new ReviewReadiness(true, true, false, "DRAFT_REVIEW_ONLY");
        } catch (RuntimeException ex) {
            return new ReviewReadiness(true, false, false, "REVIEW_SCHEMA_NOT_READY");
        }
    }
    public Review submit(String draftId, Authentication auth, String idempotencyKey, String correlationId) {
        requireScope(auth, "SCOPE_clt.collateral.introduce");
        var who = introduction.actor(auth);
        var pool = pool();
        String id = CollateralIntroductionPolicy.id(draftId);
        String key = CollateralIntroductionPolicy.key(idempotencyKey);
        String trace = CollateralIntroductionPolicy.correlation(correlationId);
        var draft = drafts.find(pool, id, who.subject(), who.branch());
        if (draft == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CLT_REVIEW_OWN_DRAFT_NOT_FOUND");
        if (!"DRAFT".equals(draft.status()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CLT_REVIEW_DRAFT_NOT_MUTABLE");
        var existing = reviews.byDraft(pool, id, who.subject(), who.branch());
        if (existing != null) return replayOrConflict(pool, existing, key);
        // Revalidate persisted bytes, creation audit, current CIF/type/attribute/range rules.
        // A changed or stale stage record must never reach an independent checker queue.
        var integrity = introduction.inspectDraft(auth, id);
        if (!integrity.eligibleForReview())
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "CLT_R2_DRAFT_REVIEW_PREFLIGHT_BLOCKED:" + String.join(",", integrity.blockers()));
        // Guard the proposal review against textually identical assets proposed by
        // another owner. Same-owner revisions stay eligible; G-14 remains unresolved.
        var candidate = introduction.inspectAssetCandidate(auth, id);
        if (!candidate.eligibleForReviewSubmission())
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "CLT_ASSET_CANDIDATE_OTHER_OWNER_CONFLICT");
        String digest = drafts.previousHash(pool, id);
        try {
            return pool.transaction().execute(tx -> {
                Review concurrent = reviews.byDraft(pool, id, who.subject(), who.branch());
                if (concurrent != null) return replayOrConflict(pool, concurrent, key);
                return reviews.insert(pool, id, who.subject(), who.branch(), key, digest, trace);
            });
        } catch (DuplicateKeyException conflict) {
            Review concurrent = reviews.byDraft(pool, id, who.subject(), who.branch());
            if (concurrent != null) return replayOrConflict(pool, concurrent, key);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CLT_REVIEW_CONCURRENT_SUBMISSION");
        }
    }
    private Review replayOrConflict(CollateralIntroductionWriterPool pool, Review review, String key) {
        if (!key.equals(reviews.submissionKey(pool, review.reviewId())))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CLT_REVIEW_ALREADY_SUBMITTED");
        return review;
    }
    public List<Review> mine(Authentication auth) {
        requireScope(auth, "SCOPE_clt.collateral.introduce");
        var who = introduction.actor(auth);
        return reviews.own(pool(), who.subject(), who.branch());
    }
    public List<Review> pending(Authentication auth) {
        requireScope(auth, "SCOPE_clt.collateral.review");
        var who = introduction.actor(auth);
        return reviews.pending(pool(), who.branch());
    }
    public ReviewSnapshot snapshot(String reviewId, Authentication auth) {
        requireScope(auth, "SCOPE_clt.collateral.review");
        var who = introduction.actor(auth);
        String id = CollateralIntroductionPolicy.id(reviewId);
        var p = pool();
        Review review = reviews.byIdInBranch(p, id, who.branch());
        if (review == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CLT_REVIEW_NOT_FOUND_IN_BRANCH");
        try { CollateralIntroductionReviewPolicy.separateActors(review.makerSubject(), who.subject()); }
        catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage()); }
        var draft = drafts.findForReviewer(p, id, who.branch(), who.subject());
        if (draft == null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "CLT_REVIEW_STAGED_DRAFT_MISSING");
        String currentHash = drafts.previousHash(p, draft.draftId());
        String savedHash = reviews.draftHash(p, id, who.branch());
        if (!currentHash.equals(savedHash)) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "CLT_REVIEW_DRAFT_FINGERPRINT_CHANGED");
        try {
            var reconstructed = new com.behsazan.corebanking.collateral.introduction.CollateralIntroductionModels.Request(
                    draft.partyId(), draft.collateralTypeId(), draft.estimateAmount(),
                    draft.currencyCode(), draft.validFrom(), draft.validTo(),
                    draft.depositNumber(), draft.remarks(), draft.attributes(),
                    draft.assetReference(), draft.itemRanges());
            if (!savedHash.equals(CollateralIntroductionPolicy.sha256(
                    CollateralIntroductionPolicy.normalize(reconstructed).canonical()))
                    || !drafts.matchingCreationAudit(p, draft.draftId(), review.makerSubject(),
                        who.branch(), savedHash)
                    || !drafts.persistedRangesMatch(p, draft.draftId(), draft.itemRanges()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "CLT_REVIEW_STORED_SNAPSHOT_INVALID");
        } catch (IllegalArgumentException invalid) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CLT_REVIEW_STORED_SNAPSHOT_INVALID");
        }
        return new ReviewSnapshot(id, draft, savedHash, false);
    }
    public Review decide(String reviewId, Decision request, Authentication auth,
            String idempotencyKey, String correlationId) {
        requireScope(auth, "SCOPE_clt.collateral.review");
        var who = introduction.actor(auth);
        String id = CollateralIntroductionPolicy.id(reviewId);
        var decision = CollateralIntroductionReviewPolicy.normalize(request);
        String key = CollateralIntroductionPolicy.key(idempotencyKey);
        String trace = CollateralIntroductionPolicy.correlation(correlationId);
        var p = pool();
        return p.transaction().execute(tx -> {
            Review current = reviews.byIdInBranch(p, id, who.branch());
            if (current == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "CLT_REVIEW_NOT_FOUND_IN_BRANCH");
            try { CollateralIntroductionReviewPolicy.separateActors(current.makerSubject(), who.subject()); }
            catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage()); }
            String savedHash = reviews.draftHash(p, id, who.branch());
            if (!savedHash.equals(drafts.previousHash(p, current.draftId())))
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "CLT_REVIEW_DRAFT_FINGERPRINT_CHANGED");
            if (!request.draftHash().equals(savedHash))
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "CLT_REVIEW_SNAPSHOT_HASH_MISMATCH");
            if (!"PENDING_REVIEW".equals(current.status())) {
                if (reviews.exactDecisionReplay(p, current, who.subject(), key, decision)) return current;
                throw new ResponseStatusException(HttpStatus.CONFLICT, "CLT_REVIEW_ALREADY_DECIDED");
            }
            try { CollateralIntroductionReviewPolicy.pending(current.status(), current.recordVersion()); }
            catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage()); }
            // Re-run the full server-side checks within the decision transaction. A
            // RETURNED decision is always available for legitimately defective drafts.
            if ("REVIEWED".equals(decision.decision()))
                requireFreshSnapshotForApproval(p, current, id, who.branch(), who.subject(), auth);
            if (reviews.decide(p, id, who.subject(), who.branch(), key, decision, trace) != 1)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "CLT_REVIEW_CONCURRENT_DECISION");
            return reviews.byIdInBranch(p, id, who.branch());
        });
    }

    private void requireFreshSnapshotForApproval(CollateralIntroductionWriterPool p, Review review,
            String reviewId, String branch, String checker, Authentication authentication) {
        var draft = drafts.findForReviewer(p, reviewId, branch, checker);
        if (draft == null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "CLT_REVIEW_STAGED_DRAFT_MISSING");
        String savedHash = reviews.draftHash(p, reviewId, branch);
        boolean hashValid = false, auditValid = false, rangesValid = false, liveValid = false;
        boolean stageChecked = false, crossOwner = false;
        boolean hasCandidate = CollateralAssetCandidatePolicy.lookup(draft.assetReference()) != null;
        try {
            var storedRequest = new com.behsazan.corebanking.collateral.introduction.CollateralIntroductionModels.Request(
                    draft.partyId(), draft.collateralTypeId(), draft.estimateAmount(),
                    draft.currencyCode(), draft.validFrom(), draft.validTo(),
                    draft.depositNumber(), draft.remarks(), draft.attributes(),
                    draft.assetReference(), draft.itemRanges());
            var normalized = CollateralIntroductionPolicy.normalize(storedRequest);
            hashValid = savedHash != null && savedHash.equals(drafts.previousHash(p, draft.draftId()))
                    && savedHash.equals(CollateralIntroductionPolicy.sha256(normalized.canonical()));
            auditValid = hashValid && drafts.matchingCreationAudit(p, draft.draftId(),
                    review.makerSubject(), branch, savedHash);
            rangesValid = drafts.persistedRangesMatch(p, draft.draftId(), draft.itemRanges());
            if (hashValid && auditValid && rangesValid) {
                liveValid = introduction.validate(storedRequest, authentication).eligibleForDraft();
            }
            if (hasCandidate) {
                stageChecked = drafts.candidateColumnReady(p);
                if (stageChecked) crossOwner = drafts.stageCandidateMatches(p, draft).otherOwner() > 0;
            }
        } catch (RuntimeException invalidOrUnavailable) {
            // A partially unavailable Oracle/CIF dependency never permits REVIEWED.
            hashValid = false;
        }
        var result = CollateralReviewDecisionGuard.assess(
                new CollateralReviewDecisionGuard.Evidence(
                        "DRAFT".equals(draft.status()) && "1".equals(draft.recordVersion()),
                        hashValid, auditValid, rangesValid, liveValid, stageChecked,
                        crossOwner, hasCandidate));
        if (!result.reviewAllowed())
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "CLT_REVIEW_APPROVAL_PREFLIGHT_BLOCKED:" + String.join(",", result.blockers()));
    }

}
