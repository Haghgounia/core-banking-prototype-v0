package com.behsazan.corebanking.collateral.release;

import java.util.List;

/** Oracle NUMBER(19) values are JSON strings, never JavaScript floating point numbers. */
public final class CollateralReleasePostingModels {
    private CollateralReleasePostingModels() {}
    public record Readiness(boolean postingEnabled, boolean bankPermitSchemaReady,
            boolean targetStatusConfigured, boolean prototypePostingBlocked,
            String decision) {}
    public record PermitView(String permitId, String status, String sourceStatus,
            String targetStatus, String baseVersion, String approvalReference,
            String expiresAt, String postedAt, String postedVersion) {}
    public record Permits(List<PermitView> items, String source) {}
    public record FinalizeCommand(String permitId) {}
    public record Receipt(String collateralId, String permitId, String status,
            String previousStatus, String finalStatus, String previousVersion,
            String finalVersion, String statusHistoryId, String approvalReference,
            String postedAt, boolean replayed, boolean capacityReleased,
            String decision) {}
}
