package com.behsazan.corebanking.collateral.unallocation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Read-only CLT history can be used only under the existing reference/registry read gates. */
@Component
public final class CollateralUnallocationProperties {
    private final boolean historyEnabled;
    public CollateralUnallocationProperties(
            @Value("${core-banking.collateral.unallocation.history-enabled:true}") boolean enabled) {
        this.historyEnabled = enabled;
    }
    public boolean historyEnabled() {return historyEnabled;}
    public boolean financialWritesEnabled() {return false;}
}
