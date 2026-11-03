package com.behsazan.corebanking.collateral.sponsor;

import java.util.List;

public final class CollateralSponsorModels {
    private CollateralSponsorModels() {}
    public record SponsorRow(String sponsorId, String collateralId, String partyId,
            String depositNumber, String branchCode, String createdAt,
            String recordVersion, String evidenceStatus) {}
    public record SponsorPage(String collateralId, String collateralNumber,
            boolean sponsorRequired, String minimumRequiredStatus,
            Long validConfirmedCount, long totalElements, int page, int size,
            List<SponsorRow> items, String source) {}
    public record CandidateRequest(String partyId, String depositNumber, String justification) {}
    public record Validation(String collateralId, String partyId,
            boolean eligibleForRequest, boolean eligibleForFinalSponsorRegistration,
            List<String> blockers, String minimumRequiredStatus, String debtorCheckStage) {}
    public record RequestRecord(String requestId, String collateralId, String partyId,
            String depositNumber, String justification, String status, String branchCode,
            String makerSubject, String createdAt, String recordVersion,
            String source) {}
    public record RequestPage(long totalElements, int page, int size, List<RequestRecord> items) {}
    public record Readiness(boolean readsEnabled, boolean requestWritesEnabled,
            boolean stagingSchemaVerified, boolean finalSponsorRegistrationEnabled,
            boolean sponsorLifecycleApproved, String stage) {}
}
