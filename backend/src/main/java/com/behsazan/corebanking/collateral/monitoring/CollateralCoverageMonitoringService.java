package com.behsazan.corebanking.collateral.monitoring;

import com.behsazan.corebanking.collateral.monitoring.CollateralCoverageMonitoringPolicy.Finding;
import java.util.List;
import org.springframework.stereotype.Service;

/** BUC-14: branch-scoped risk worklist from actual active CLT allocations. */
@Service
public class CollateralCoverageMonitoringService {
    public record CoveragePage(String source, String branchCode, int page, int size,
            boolean hasNextPage, int recordedShortfallCountOnPage, List<Finding> items,
            boolean allocationAuthorized, String evidenceLevel) {}

    private final CollateralCoverageMonitoringRepository repository;
    public CollateralCoverageMonitoringService(CollateralCoverageMonitoringRepository repository) {
        this.repository = repository;
    }
    public CoveragePage report(String verifiedBranch, int page, int size) {
        CollateralCoverageMonitoringPolicy.page(page);
        CollateralCoverageMonitoringPolicy.size(size);
        var fetched = repository.page(verifiedBranch, page, size);
        boolean more = fetched.size() > size;
        var items = fetched.stream().limit(size).map(CollateralCoverageMonitoringPolicy::evaluate).toList();
        long shortfalls = items.stream().filter(f -> "RECORDED_SHORTFALL".equals(f.riskStatus())).count();
        return new CoveragePage("ORACLE_CLT_ACTIVE_ALLOCATIONS", verifiedBranch, page, size, more,
                Math.toIntExact(shortfalls), items, false,
                "BUC14_READ_ONLY_SINGLE_STATEMENT_RECORDED_VALUE_MONITORING; NO_FX_OR_APPROVED_FACTORS; NOT_NEW_ALLOCATION_AUTHORIZATION");
    }
}
