package com.behsazan.corebanking.collateral.introduction;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionReviewModels.Review;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionReviewModels.ReviewSnapshot;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionReviewModels.ReviewReadiness;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionReviewPolicy.Decision;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/collaterals")
public final class CollateralIntroductionReviewController {
    private final CollateralIntroductionReviewService service;
    private final com.behsazan.corebanking.collateral.registration.CollateralRegistrationReadinessService registration;
    private final com.behsazan.corebanking.collateral.registration.CollateralRegistrationReadbackService readback;
    public CollateralIntroductionReviewController(CollateralIntroductionReviewService service,
            com.behsazan.corebanking.collateral.registration.CollateralRegistrationReadinessService registration,
            com.behsazan.corebanking.collateral.registration.CollateralRegistrationReadbackService readback) {
        this.service = service;
        this.registration = registration;
        this.readback = readback;
    }
    @GetMapping("/introduction-reviews/readiness")
    ReviewReadiness readiness(Authentication actor) { return service.readiness(actor); }

    @PostMapping("/introduction-drafts/{draftId}/submit-review")
    @ResponseStatus(HttpStatus.CREATED)
    Review submit(@PathVariable String draftId, Authentication actor,
                  @RequestHeader("Idempotency-Key") String idempotencyKey,
                  @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        return service.submit(draftId, actor, idempotencyKey, correlationId);
    }
    @GetMapping("/introduction-reviews/mine")
    List<Review> mine(Authentication actor) { return service.mine(actor); }

    @GetMapping("/introduction-reviews/pending")
    List<Review> pending(Authentication actor) { return service.pending(actor); }

    @GetMapping("/introduction-reviews/{reviewId}/draft")
    ReviewSnapshot snapshot(@PathVariable String reviewId, Authentication actor) {
        return service.snapshot(reviewId, actor);
    }

    /** Only the original maker can inspect readiness of their persisted, reviewed draft. */
    @GetMapping("/introduction-reviews/{reviewId}/finalization-readiness")
    com.behsazan.corebanking.collateral.registration.CollateralRegistrationReadinessModels.Report
    finalizationReadiness(@PathVariable String reviewId, Authentication actor) {
        return registration.report(reviewId, actor);
    }

    /** BUC-03: only the maker may resolve an Oracle registration for this review. */
    @GetMapping("/introduction-reviews/{reviewId}/registration-readback")
    com.behsazan.corebanking.collateral.registration.CollateralRegistrationReadbackModels.Report
    registrationReadback(@PathVariable String reviewId, Authentication actor) {
        return readback.byReview(actor, reviewId);
    }

    @PostMapping("/introduction-reviews/{reviewId}/decision")
    Review decide(@PathVariable String reviewId, @RequestBody Decision decision,
                  Authentication actor, @RequestHeader("Idempotency-Key") String idempotencyKey,
                  @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        return service.decide(reviewId, decision, actor, idempotencyKey, correlationId);
    }
}
