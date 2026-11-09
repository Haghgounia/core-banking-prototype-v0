package com.behsazan.corebanking.collateral.registration;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionModels;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionPolicy;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionProperties;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionRepository;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionReviewRepository;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionService;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionWriterPool;
import com.behsazan.corebanking.collateral.registration.CollateralRegistrationGatePolicy.Evidence;
import com.behsazan.corebanking.collateral.registration.CollateralRegistrationReadinessModels.Report;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Maker-only check of the exact persisted reviewed draft; no registration transaction. */
@Service
public final class CollateralRegistrationReadinessService {
    private final CollateralIntroductionService introduction;
    private final CollateralIntroductionRepository drafts;
    private final CollateralIntroductionReviewRepository reviews;
    private final CollateralIntroductionProperties settings;
    private final ObjectProvider<CollateralIntroductionWriterPool> writer;
    private final CollateralRegistrationEvidenceRepository evidence;

    public CollateralRegistrationReadinessService(CollateralIntroductionService introduction,
            CollateralIntroductionRepository drafts, CollateralIntroductionReviewRepository reviews,
            CollateralIntroductionProperties settings,
            ObjectProvider<CollateralIntroductionWriterPool> writer,
            CollateralRegistrationEvidenceRepository evidence) {
        this.introduction = introduction;
        this.drafts = drafts;
        this.reviews = reviews;
        this.settings = settings;
        this.writer = writer;
        this.evidence = evidence;
    }

    public Report report(String reviewId, Authentication authentication) {
        var who = introduction.actor(authentication);
        if (authentication.getAuthorities().stream().noneMatch(
                a -> "SCOPE_clt.collateral.introduce".equals(a.getAuthority())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CLT_REGISTRATION_MAKER_SCOPE_REQUIRED");
        if (!settings.draftWritesEnabled() || !settings.reviewWorkflowEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_REGISTRATION_REVIEW_STORAGE_DISABLED");
        String id = CollateralIntroductionPolicy.id(reviewId);
        CollateralIntroductionWriterPool pool = writer.getObject();
        reviews.requireSchema(pool);
        var review = reviews.byIdInBranch(pool, id, who.branch());
        // Do not reveal the existence of another user's reviewed draft.
        if (review == null || !who.subject().equals(review.makerSubject()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "CLT_REGISTRATION_OWN_REVIEW_NOT_FOUND");
        var draft = drafts.find(pool, review.draftId(), who.subject(), who.branch());
        String savedHash = reviews.draftHash(pool, id, who.branch());
        boolean storedMatches = false, recomputedMatches = false;
        boolean creationAuditValid = false, persistedSerialRowsValid = false;
        boolean assetStageChecked = false, crossOwnerStageConflict = false;
        boolean currentValid = false;
        boolean countable = false, serialsValid = false;
        String serialCount = "0";
        List<String> validationReasons = List.of("CLT_CURRENT_VALIDATION_NOT_PROVEN");
        String candidate = null, amount = null, currency = null;
        if (draft != null) {
            candidate = draft.assetReference();
            amount = draft.estimateAmount();
            currency = draft.currencyCode();
            try {
                storedMatches = savedHash != null && savedHash.equals(
                        drafts.previousHash(pool, draft.draftId()));
                var request = new CollateralIntroductionModels.Request(draft.partyId(),
                        draft.collateralTypeId(), draft.estimateAmount(), draft.currencyCode(),
                        draft.validFrom(), draft.validTo(), draft.depositNumber(), draft.remarks(),
                        draft.attributes(), draft.assetReference(), draft.itemRanges());
                var normalized = CollateralIntroductionPolicy.normalize(request);
                recomputedMatches = savedHash != null && savedHash.equals(
                        CollateralIntroductionPolicy.sha256(normalized.canonical()));
                creationAuditValid = storedMatches && recomputedMatches &&
                        drafts.matchingCreationAudit(pool, draft.draftId(), who.subject(),
                                who.branch(), savedHash);
                persistedSerialRowsValid = drafts.persistedRangesMatch(pool,
                        draft.draftId(), draft.itemRanges());
                if (draft.assetReference() != null && !draft.assetReference().isBlank()) {
                    assetStageChecked = drafts.candidateColumnReady(pool);
                    if (assetStageChecked)
                        crossOwnerStageConflict = drafts.stageCandidateMatches(pool, draft).otherOwner() > 0;
                }
                // Always recheck the live CIF party and governed CLT references.
                try {
                    var live = introduction.validate(request, authentication);
                    currentValid = live.eligibleForDraft();
                    validationReasons = live.blockers();
                } catch (RuntimeException unavailable) {
                    validationReasons = List.of("CLT_CURRENT_CIF_OR_CLT_VALIDATION_UNAVAILABLE");
                }
                try {
                    var rules = drafts.rules(draft.collateralTypeId());
                    countable = rules.countable();
                    var rows = com.behsazan.corebanking.collateral.introduction.CollateralSerialRangePolicy
                            .assess(countable, draft.currencyCode(), draft.estimateAmount(), draft.itemRanges());
                    serialsValid = persistedSerialRowsValid;
                    serialCount = Integer.toString(rows.itemCount());
                } catch (RuntimeException unavailable) { serialsValid = false; }
            } catch (RuntimeException invalidDraft) {
                // Malformed stored payload is evidence of a blocking condition, never approval.
                validationReasons = List.of("CLT_PERSISTED_DRAFT_CANNOT_BE_RECONSTRUCTED");
            }
        }
        boolean number19 = false, posted = false, introductionType = false;
        try {
            var db = evidence.inspect();
            number19 = db.number19Unique();
            posted = db.postedStatus();
            introductionType = db.introductionType();
        } catch (RuntimeException unavailable) {
            // Lack of dictionary or reference access is a blocker, not a positive assumption.
        }
        return CollateralRegistrationGatePolicy.assess(new Evidence(
                id, review.draftId(), review.status(), review.makerSubject(),
                review.checkerSubject(), who.branch(), savedHash, draft != null,
                storedMatches, recomputedMatches, currentValid, validationReasons,
                number19, posted, introductionType, countable, serialsValid,
                serialCount, candidate, amount, currency, creationAuditValid,
                persistedSerialRowsValid, assetStageChecked, crossOwnerStageConflict));
    }
}
