package com.behsazan.corebanking.collateral.reporting;

import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** BUC-16: exact registration lookup and authorization before branch-scoped history query. */
@Service
public class CollateralTimelineService {
    public record Timeline(String collateralId, String collateralNumber, String branchCode,
            int page, int size, List<CollateralTimelineRepository.Event> events, boolean hasMore, boolean exhaustiveAudit,
            String evidenceLevel) {}

    private final CollateralRegistryRepository registry;
    private final CollateralTimelineRepository repository;
    public CollateralTimelineService(CollateralRegistryRepository registry, CollateralTimelineRepository repository) {
        this.registry = registry;
        this.repository = repository;
    }
    private static String id(Object raw) {
        if (raw instanceof String value) return CollateralRegistryPolicy.id(value).toPlainString();
        if (raw instanceof BigDecimal value) return CollateralRegistryPolicy.id(value.toPlainString()).toPlainString();
        throw new IllegalArgumentException("CLT_TIMELINE_REGISTRY_ID_MISSING");
    }
    public Timeline byRegistration(String verifiedBranch, String registrationNumber, int page, int size) {
        if (size < 1 || size > 100 || page < 0 || page > 100_000)
            throw new IllegalArgumentException("CLT_TIMELINE_PAGE_OR_SIZE_INVALID");
        String branch = CollateralRegistryPolicy.branch(verifiedBranch);
        Map<String,Object> parent = registry.byRegistration(branch, registrationNumber);
        String collateralId = id(parent.get("COLLATERAL_ID"));
        String number = id(parent.get("COLLATERAL_NUMBER"));
        var received = repository.events(branch, collateralId, page, size);
        boolean more = received.size() > size;
        return new Timeline(collateralId, number, branch, page, size,
                List.copyOf(received.subList(0, Math.min(received.size(), size))), more, false,
                "BUC16_PERSISTED_CLT_ROWS_ONLY; ALLOCATION_UPDATE_IS_LAST_EDIT_NOT_EVENT_SOURCED_HISTORY; NOT_COMPLETE_AUDIT_LOG");
    }
}
