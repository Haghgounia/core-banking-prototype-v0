package com.behsazan.corebanking.deposit.opening.operational.integration;

import com.behsazan.corebanking.deposit.opening.operational.domain.DepositOpeningOperationalModels.ReadinessEvidence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PrototypeActivationEvidenceAdapter {
    private final boolean enabled;
    private final long finalComplianceValidityHours;

    public PrototypeActivationEvidenceAdapter(
            @Value("${core-banking.deposit-opening.prototype-activation-evidence.enabled:true}") boolean enabled,
            @Value("${core-banking.deposit-opening.prototype-activation-evidence.final-compliance-validity-hours:24}") long finalComplianceValidityHours
    ) {
        this.enabled = enabled;
        this.finalComplianceValidityHours = Math.max(1L, finalComplianceValidityHours);
    }

    public boolean enabled() {
        return enabled;
    }

    public Map<String, ReadinessEvidence> evidence(long openingRequestId, OffsetDateTime evaluatedAt) {
        if (!enabled) return Map.of();

        Map<String, ReadinessEvidence> values = new LinkedHashMap<>();
        values.put("CBI_SIAH_REGISTRATION", pass(
                "CBI_SIAH_REGISTRATION",
                "PROTO-SIAH-OPENING-" + openingRequestId,
                null
        ));
        values.put("FINAL_COMPLIANCE_RECHECK", pass(
                "FINAL_COMPLIANCE_RECHECK",
                "PROTO-COMPLIANCE-OPENING-" + openingRequestId,
                evaluatedAt.plusHours(finalComplianceValidityHours)
        ));
        values.put("RESTRICTIONS_READY", pass(
                "RESTRICTIONS_READY",
                "PROTO-RESTRICTIONS-OPENING-" + openingRequestId,
                null
        ));
        return values;
    }

    private static ReadinessEvidence pass(String code, String reference, OffsetDateTime validUntil) {
        return new ReadinessEvidence(
                code,
                "PASS",
                reference,
                validUntil,
                "PROTOTYPE_ADAPTER:" + code,
                null
        );
    }
}
