package com.behsazan.corebanking.collateral.unallocation;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Bank-approved domain -> CLT.MODULE_ID mapping; all entries absent by default. */
@Component
public final class CollateralUnallocationDomainBinding {
    private final Map<String,String> mappings;
    public CollateralUnallocationDomainBinding(
            @Value("${core-banking.collateral.unallocation.module-ids.loan:}") String loan,
            @Value("${core-banking.collateral.unallocation.module-ids.guarantee:}") String guarantee,
            @Value("${core-banking.collateral.unallocation.module-ids.letter-of-credit:}") String credit,
            @Value("${core-banking.collateral.unallocation.module-ids.green-license:}") String green) {
        mappings = Map.of("LOAN", loan.trim(), "GUARANTEE", guarantee.trim(),
                "LETTER_OF_CREDIT", credit.trim(), "GREEN_LICENSE", green.trim());
    }
    public String approvedModule(String domain) {
        String candidate = mappings.getOrDefault(domain, "");
        if (candidate.isBlank()) return null;
        if (!candidate.matches("[1-9][0-9]{0,9}"))
            throw new SecurityException("CLT_R5A_INVALID_MODULE_MAPPING");
        Set<String> seen = new HashSet<>();
        for (String value : mappings.values()) {
            if (value.isBlank()) continue;
            if (!value.matches("[1-9][0-9]{0,9}") || !seen.add(value))
                throw new SecurityException("CLT_R5A_NON_UNIQUE_OR_INVALID_MODULE_MAPPING");
        }
        return candidate;
    }
}
