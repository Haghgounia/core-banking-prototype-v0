package com.behsazan.corebanking.posting.readiness;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class PostingReadinessModels {
    private PostingReadinessModels() {}

    public record Finding(String category, String objectName, String status, String description) {}

    public record ReadinessReport(
            String phase,
            String status,
            boolean readOnly,
            boolean realFinancialPostingAllowed,
            int expectedTableCount,
            int visibleTableCount,
            int expectedSequenceCount,
            int visibleSequenceCount,
            int expectedIndexCount,
            int validIndexCount,
            int expectedConstraintCount,
            int validConstraintCount,
            Map<String, Long> configurationRowCounts,
            List<Finding> findings,
            String postingGateExplanation,
            Instant collectedAt
    ) {}
}
