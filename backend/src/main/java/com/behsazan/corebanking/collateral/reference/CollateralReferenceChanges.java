package com.behsazan.corebanking.collateral.reference;

import java.util.List;
import java.util.Map;

/** Stable DTOs: phase labels belong in release documents, never in Java public class names. */
public final class CollateralReferenceChanges {
    private CollateralReferenceChanges() { }

    public record Proposal(String table, String operation, String targetId,
                           Long expectedVersion, Map<String, Object> values, String reason) { }
    public record Decision(String decision, String reason) { }
    public record Change(String changeId, String table, String operation, String targetId,
                         Long expectedVersion, Map<String, Object> values, String reason,
                         String status, String maker, String checker, String createdAt,
                         String decidedAt, String correlationId) { }
    public record Changes(List<Change> items, String source) { }
    public record Governance(boolean writesEnabled, boolean schemaMigrationAutomatic,
                             boolean referencePolicySeedAutomatic, String stage) { }
}
