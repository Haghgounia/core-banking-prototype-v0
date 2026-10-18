package com.behsazan.corebanking.posting.cash;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** Optional bridge: when disabled, existing DPS2 behavior is unchanged. */
@Service
public final class DepositCashPostingBridge {
    private final ObjectProvider<CashPostingService> posting;
    public DepositCashPostingBridge(ObjectProvider<CashPostingService> posting) { this.posting = posting; }
    public boolean isEnabled() { return posting.getIfAvailable() != null; }
    public void record(CashPostingPolicy.Instruction instruction) {
        CashPostingService service = posting.getIfAvailable();
        if (service != null) service.commit(instruction);
    }
}
