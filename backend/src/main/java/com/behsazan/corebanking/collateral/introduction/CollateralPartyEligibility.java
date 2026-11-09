package com.behsazan.corebanking.collateral.introduction;

import com.behsazan.corebanking.cif.application.CifService;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Cross-domain read goes through the existing CIF application contract, never CIF tables. */
@Component
public final class CollateralPartyEligibility {
    private final CifService cif;
    public CollateralPartyEligibility(CifService cif) { this.cif = cif; }
    /** The CIF customer number is not the technical Party ID. Fail closed if not current/ready. */
    public String currentVerifiedCustomerNumber(String partyId) {
        long id;
        try { id = new BigDecimal(partyId).longValueExact(); }
        catch (ArithmeticException e) { return null; }
        var status = cif.readiness(id);
        if (!status.customerRole() || !status.readyForFinalization()) return null;
        String number = status.customerNo();
        return number == null || number.isBlank() ? null : number.strip();
    }
    public boolean eligible(String partyId) {
        long id;
        try { id = new BigDecimal(partyId).longValueExact(); }
        catch (ArithmeticException e) { return false; }
        var party = cif.find(id).party();
        String status = party == null ? null : party.lifecycleStatusCode();
        if (party == null || party.mergedIntoPartyId() != null || !"Y".equals(party.isCurrent())
                || (status != null && java.util.Set.of("MERGED", "CLOSED", "INACTIVE", "SUSPENDED")
                    .contains(status))) return false;
        return cif.readiness(id).readyForFinalization();
    }
}
