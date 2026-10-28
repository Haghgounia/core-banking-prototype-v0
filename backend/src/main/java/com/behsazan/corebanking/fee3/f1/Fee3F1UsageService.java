package com.behsazan.corebanking.fee3.f1;

import com.behsazan.corebanking.fee3.oracle.Fee3Repository;
import com.behsazan.corebanking.fee3.r3.Fee3R3Service;
import com.behsazan.corebanking.fee3.r4.Fee3R4ReferenceRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dedicated read-only USAGE simulation; Oracle preflight and immutable draft proof required. */
@Service
public class Fee3F1UsageService {
    private final Fee3Repository oracle;
    private final Fee3R3Service validation;
    private final Fee3R4ReferenceRepository reference;
    public Fee3F1UsageService(Fee3Repository oracle, Fee3R3Service validation,
                              Fee3R4ReferenceRepository reference) {
        this.oracle = oracle; this.validation = validation; this.reference = reference;
    }
    @Transactional(readOnly = true)
    public Fee3F1UsageContract.Evidence preview(Fee3F1UsageContract.Request request) {
        LocalDate bankDay = oracle.bankDate();
        String reason = Fee3F1UsageContract.guard(request, bankDay);
        if (reason != null) return Fee3F1UsageContract.blocked(request, bankDay, reason);
        try {
            var preflight = validation.preflight(request.draft());
            if (!preflight.submissionReady()) {
                String first = preflight.issues().isEmpty() ? "R3_PREFLIGHT_NOT_READY"
                    : "R3_PREFLIGHT_" + preflight.issues().get(0).code();
                return Fee3F1UsageContract.blocked(request, bankDay, first);
            }
            var source = reference.resolve(request.draft());
            var amount = Fee3F1UsageContract.calculate(request);
            return Fee3F1UsageContract.finish(request, bankDay, source, amount);
        } catch (IllegalArgumentException error) {
            String code = error.getMessage();
            if (code == null || !code.matches("[A-Z0-9_]{3,100}")) code = "USAGE_UNVERIFIED_CALCULATION";
            return Fee3F1UsageContract.blocked(request, bankDay, code);
        }
    }
}
