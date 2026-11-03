package com.behsazan.corebanking.collateral.introduction;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionReviewModels.Review;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionReviewPolicy.CheckedDecision;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

/** Only isolated staging tables are writable; CLT.COLLATERAL is never touched. */
@Repository
public class CollateralIntroductionReviewRepository {
    private static final String PROJECTION = """
            SELECT REVIEW_ID, DRAFT_ID, REVIEW_STATUS, MAKER_SUBJECT, BRANCH_CODE,
                   SUBMITTED_AT, CHECKER_SUBJECT, DECIDED_AT, DECISION_REASON, RECORD_VERSION
              FROM CLT.CLT_INTRODUCTION_REVIEW
            """;
    private static Review map(ResultSet rs, int i) throws SQLException {
        var decided = rs.getTimestamp("DECIDED_AT");
        return new Review(rs.getBigDecimal("REVIEW_ID").toPlainString(),
                rs.getBigDecimal("DRAFT_ID").toPlainString(),
                rs.getString("REVIEW_STATUS"), rs.getString("MAKER_SUBJECT"),
                rs.getString("BRANCH_CODE"), rs.getTimestamp("SUBMITTED_AT").toLocalDateTime().toString(),
                rs.getString("CHECKER_SUBJECT"), decided == null ? null : decided.toLocalDateTime().toString(),
                rs.getString("DECISION_REASON"), rs.getBigDecimal("RECORD_VERSION").toPlainString(),
                "NOT_REGISTERED");
    }
    public void requireSchema(CollateralIntroductionWriterPool pool) {
        Integer columns = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS
                 WHERE OWNER = 'CLT' AND TABLE_NAME = 'CLT_INTRODUCTION_REVIEW'
                   AND COLUMN_NAME IN ('REVIEW_ID','DRAFT_ID','REVIEW_STATUS','MAKER_SUBJECT',
                      'BRANCH_CODE','IDEMPOTENCY_KEY','DECISION_KEY','CHECKER_SUBJECT',
                      'DECISION_REASON','DECIDED_AT','RECORD_VERSION','DRAFT_HASH')
                """, Integer.class);
        Integer events = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS
                 WHERE OWNER = 'CLT' AND TABLE_NAME = 'CLT_INTRODUCTION_REVIEW_EVENT'
                   AND COLUMN_NAME IN ('EVENT_ID','REVIEW_ID','EVENT_CODE','ACTOR_SUBJECT',
                      'BRANCH_CODE','CORRELATION_ID')
                """, Integer.class);
        if (columns == null || columns != 12 || events == null || events != 6)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLT_REVIEW_SCHEMA_NOT_APPROVED_OR_INCOMPLETE");
    }
    public Review byDraft(CollateralIntroductionWriterPool pool, String draftId, String maker, String branch) {
        List<Review> list = pool.jdbc().query(PROJECTION +
                " WHERE DRAFT_ID = ? AND MAKER_SUBJECT = ? AND BRANCH_CODE = ?",
                CollateralIntroductionReviewRepository::map, new BigDecimal(draftId), maker, branch);
        return list.isEmpty() ? null : list.getFirst();
    }
    public String draftHash(CollateralIntroductionWriterPool pool, String reviewId, String branch) {
        return pool.jdbc().queryForObject("""
                SELECT DRAFT_HASH FROM CLT.CLT_INTRODUCTION_REVIEW
                 WHERE REVIEW_ID = ? AND BRANCH_CODE = ?
                """, String.class, new BigDecimal(reviewId));
    }
    public String submissionKey(CollateralIntroductionWriterPool pool, String reviewId) {
        return pool.jdbc().queryForObject("""
                SELECT IDEMPOTENCY_KEY FROM CLT.CLT_INTRODUCTION_REVIEW WHERE REVIEW_ID = ?
                """, String.class, new BigDecimal(reviewId));
    }
    public Review byIdInBranch(CollateralIntroductionWriterPool pool, String reviewId, String branch) {
        List<Review> list = pool.jdbc().query(PROJECTION +
                " WHERE REVIEW_ID = ? AND BRANCH_CODE = ?",
                CollateralIntroductionReviewRepository::map, new BigDecimal(reviewId), branch);
        return list.isEmpty() ? null : list.getFirst();
    }
    public List<Review> own(CollateralIntroductionWriterPool pool, String maker, String branch) {
        return pool.jdbc().query(PROJECTION + """
                WHERE MAKER_SUBJECT = ? AND BRANCH_CODE = ?
                 ORDER BY SUBMITTED_AT DESC, REVIEW_ID DESC FETCH FIRST 100 ROWS ONLY
                """, CollateralIntroductionReviewRepository::map, maker, branch);
    }
    public List<Review> pending(CollateralIntroductionWriterPool pool, String branch) {
        return pool.jdbc().query(PROJECTION + """
                WHERE BRANCH_CODE = ? AND REVIEW_STATUS = 'PENDING_REVIEW'
                 ORDER BY SUBMITTED_AT, REVIEW_ID FETCH FIRST 100 ROWS ONLY
                """, CollateralIntroductionReviewRepository::map, branch);
    }
    public Review insert(CollateralIntroductionWriterPool pool, String draftId, String maker,
            String branch, String key, String digest, String correlation) {
        BigDecimal id = pool.jdbc().queryForObject(
                "SELECT CLT.SEQ_CLT_INTRO_REVIEW.NEXTVAL FROM DUAL", BigDecimal.class);
        if (id == null) throw new IllegalStateException("CLT_REVIEW_SEQUENCE_NOT_AVAILABLE");
        int inserted = pool.jdbc().update("""
                INSERT INTO CLT.CLT_INTRODUCTION_REVIEW
                   (REVIEW_ID,DRAFT_ID,MAKER_SUBJECT,BRANCH_CODE,IDEMPOTENCY_KEY,
                    DRAFT_HASH,REVIEW_STATUS,RECORD_VERSION)
                SELECT ?, d.DRAFT_ID,d.MAKER_SUBJECT,d.BRANCH_CODE,?,d.REQUEST_HASH,
                       'PENDING_REVIEW',1
                  FROM CLT.CLT_INTRODUCTION_DRAFT d
                 WHERE d.DRAFT_ID = ? AND d.MAKER_SUBJECT = ? AND d.BRANCH_CODE = ?
                   AND d.STATUS = 'DRAFT' AND d.REQUEST_HASH = ?
                """, id, key, new BigDecimal(draftId), maker, branch, digest);
        if (inserted != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "CLT_REVIEW_DRAFT_CHANGED");
        event(pool, id, "SUBMITTED", maker, branch, correlation);
        return byIdInBranch(pool, id.toPlainString(), branch);
    }
    public int decide(CollateralIntroductionWriterPool pool, String reviewId,
            String checker, String branch, String key, CheckedDecision decision, String correlation) {
        int changed = pool.jdbc().update("""
                UPDATE CLT.CLT_INTRODUCTION_REVIEW
                   SET REVIEW_STATUS = ?, CHECKER_SUBJECT = ?, DECISION_REASON = ?,
                       DECISION_KEY = ?, DECIDED_AT = SYSTIMESTAMP, RECORD_VERSION = 2
                 WHERE REVIEW_ID = ? AND BRANCH_CODE = ? AND REVIEW_STATUS = 'PENDING_REVIEW'
                   AND RECORD_VERSION = 1 AND MAKER_SUBJECT <> ?
                """, decision.decision(), checker, decision.reason(), key,
                new BigDecimal(reviewId), branch, checker);
        if (changed == 1)
            event(pool, new BigDecimal(reviewId), decision.decision(), checker, branch, correlation);
        return changed;
    }
    public boolean exactDecisionReplay(CollateralIntroductionWriterPool pool, Review current,
            String checker, String key, CheckedDecision request) {
        Integer n = pool.jdbc().queryForObject("""
                SELECT COUNT(*) FROM CLT.CLT_INTRODUCTION_REVIEW
                 WHERE REVIEW_ID = ? AND CHECKER_SUBJECT = ? AND DECISION_KEY = ?
                   AND REVIEW_STATUS = ? AND DECISION_REASON = ? AND RECORD_VERSION = 2
                """, Integer.class, new BigDecimal(current.reviewId()), checker, key,
                request.decision(), request.reason());
        return n != null && n == 1;
    }
    private void event(CollateralIntroductionWriterPool pool, BigDecimal id, String code,
            String actor, String branch, String correlation) {
        pool.jdbc().update("""
                INSERT INTO CLT.CLT_INTRODUCTION_REVIEW_EVENT
                   (EVENT_ID,REVIEW_ID,EVENT_CODE,ACTOR_SUBJECT,BRANCH_CODE,CORRELATION_ID)
                VALUES (CLT.SEQ_CLT_INTRO_REVIEW_EVENT.NEXTVAL,?,?,?,?,?)
                """, id, code, actor, branch, correlation);
    }
}
