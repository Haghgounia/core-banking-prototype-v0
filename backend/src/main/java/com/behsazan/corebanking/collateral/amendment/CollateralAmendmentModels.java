package com.behsazan.corebanking.collateral.amendment;

import java.util.List;
import java.util.Map;

/** G-01: an amendment is a proposed change to an existing registered collateral. */
public final class CollateralAmendmentModels {
    private CollateralAmendmentModels() {}
    public record Proposal(String expectedRecordVersion, String reason, Map<String, String> changes) {}
    public record Decision(String expectedAmendmentVersion, String decision, String reason) {}
    public record Record(String amendmentId, String collateralId, String status, String makerSubject,
            String checkerSubject, String reason, String decisionReason, String baseRecordVersion,
            String appliedRecordVersion, String recordVersion, String createdAt, String decidedAt,
            Map<String, String> before, Map<String, String> after, Map<String, String> changes) {}
    public record Readiness(boolean schemaReady, boolean writesEnabled, boolean originalMutationOnSubmission,
            boolean separateCheckerRequired, String state) {}
    public record ListResult(List<Record> items, String source) {}
}
