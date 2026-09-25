package com.behsazan.corebanking.deposit.account.dashboard.application;

import com.behsazan.corebanking.deposit.account.dashboard.domain.DepositAccountDashboardModels.DashboardView;
import com.behsazan.corebanking.deposit.account.dashboard.oracle.DepositAccountDashboardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class DepositAccountDashboardService {
    private final DepositAccountDashboardRepository repository;

    public DepositAccountDashboardService(DepositAccountDashboardRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public DashboardView get() {
        return new DashboardView(
                OffsetDateTime.now(ZoneOffset.UTC),
                repository.accounts(),
                repository.holds(),
                repository.transactions(),
                repository.balances());
    }
}
