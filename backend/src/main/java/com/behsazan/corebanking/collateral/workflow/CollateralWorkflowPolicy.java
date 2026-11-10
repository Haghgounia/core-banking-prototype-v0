package com.behsazan.corebanking.collateral.workflow;

import java.math.BigDecimal;
import java.util.Set;

/** Pure R3 staging transitions; REVIEWED is NOT sponsor/insurance/valuation approval. */
public final class CollateralWorkflowPolicy {
    private CollateralWorkflowPolicy() {}

    public enum Kind {
        SPONSOR("COLLATERAL_SPONSOR_REQUEST", "SPONSOR_REQUEST_ID",
                "COLLATERAL_SPONSOR_REQUEST_EVENT", "SPONSOR_REQUEST_EVENT_ID",
                "SEQ_COLLATERAL_SPONSOR_REQUEST_EVENT"),
        INSURANCE("COLLATERAL_INSURANCE_REQUEST", "INSURANCE_REQUEST_ID",
                "COLLATERAL_INSURANCE_REQUEST_EVENT", "INSURANCE_REQUEST_EVENT_ID",
                "SEQ_COLLATERAL_INSURANCE_REQUEST_EVENT"),
        VALUATION("COLLATERAL_VALUATION_REQUEST", "VALUATION_REQUEST_ID",
                "COLLATERAL_VALUATION_REQUEST_EVENT", "VALUATION_REQUEST_EVENT_ID",
                "SEQ_COLLATERAL_VALUATION_REQUEST_EVENT");
        public final String table;
        public final String idColumn;
        public final String eventTable;
        public final String eventIdColumn;
        public final String eventSequence;
        Kind(String table, String idColumn, String eventTable, String eventIdColumn, String eventSequence) {
            this.table = table;
            this.idColumn = idColumn;
            this.eventTable = eventTable;
            this.eventIdColumn = eventIdColumn;
            this.eventSequence = eventSequence;
        }
        public static Kind parse(String value) {
            return switch (value) {
                case "sponsor" -> SPONSOR;
                case "insurance" -> INSURANCE;
                case "valuation" -> VALUATION;
                default -> throw new IllegalArgumentException("CLT_R3_WORKFLOW_KIND_INVALID");
            };
        }
    }
    public enum Action { SUBMIT, WITHDRAW, REVIEW, REJECT }

    /** Validate live domain evidence at maker submit and again at positive independent review.
     * Reject/withdraw must remain available when a business prerequisite has become invalid.
     */
    public static boolean requiresCurrentDomainValidation(Action action) {
        return action == Action.SUBMIT || action == Action.REVIEW;
    }

    public static String id(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}"))
            throw new IllegalArgumentException("CLT_R3_WORKFLOW_ID_INVALID");
        return new BigDecimal(value).toPlainString();
    }
    public static BigDecimal version(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,9}"))
            throw new IllegalArgumentException("CLT_R3_WORKFLOW_VERSION_INVALID");
        return new BigDecimal(value);
    }
    public static String reason(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.length() < 8 || normalized.length() > 500
                || (text != null && text.chars().anyMatch(Character::isISOControl)))
            throw new IllegalArgumentException("CLT_R3_WORKFLOW_REASON_INVALID");
        return normalized;
    }
    public static String correlation(String text) {
        if (text == null || text.isBlank()) return null;
        if (!text.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,99}"))
            throw new IllegalArgumentException("CLT_R3_WORKFLOW_CORRELATION_INVALID");
        return text;
    }
    public static String next(String status, Action action, boolean isMaker, boolean reviewerAuthorized) {
        return switch (action) {
            case SUBMIT -> {
                if (!isMaker) throw new IllegalArgumentException("CLT_R3_WORKFLOW_MAKER_REQUIRED");
                if (!"DRAFT".equals(status)) throw new IllegalStateException("CLT_R3_WORKFLOW_NOT_DRAFT");
                yield "SUBMITTED";
            }
            case WITHDRAW -> {
                if (!isMaker) throw new IllegalArgumentException("CLT_R3_WORKFLOW_MAKER_REQUIRED");
                if (!Set.of("DRAFT", "SUBMITTED").contains(status))
                    throw new IllegalStateException("CLT_R3_WORKFLOW_NOT_WITHDRAWABLE");
                yield "WITHDRAWN";
            }
            case REVIEW, REJECT -> {
                if (!reviewerAuthorized || isMaker)
                    throw new IllegalArgumentException("CLT_R3_WORKFLOW_INDEPENDENT_REVIEWER_REQUIRED");
                if (!"SUBMITTED".equals(status))
                    throw new IllegalStateException("CLT_R3_WORKFLOW_NOT_SUBMITTED");
                yield action == Action.REVIEW ? "REVIEWED" : "REJECTED";
            }
        };
    }
    public static String event(Action action) {
        return switch (action) {
            case SUBMIT -> "REQUEST_SUBMITTED";
            case WITHDRAW -> "REQUEST_WITHDRAWN";
            case REVIEW -> "REQUEST_REVIEWED";
            case REJECT -> "REQUEST_REJECTED";
        };
    }
}
