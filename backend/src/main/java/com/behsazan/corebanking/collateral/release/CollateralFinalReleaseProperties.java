package com.behsazan.corebanking.collateral.release;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Read-only R5C; cannot enable final status or ledger writes by configuration. */
@Component
public final class CollateralFinalReleaseProperties {
    private final boolean readsEnabled;
    public CollateralFinalReleaseProperties(
            @Value("${core-banking.collateral.final-release.evidence-reads-enabled:true}") boolean enabled){
        this.readsEnabled=enabled;
    }
    public boolean readsEnabled(){return readsEnabled;}
    public boolean finalReleaseWritesEnabled(){return false;}
}
