package com.behsazan.corebanking.collateral.registry;

import java.util.List;
import java.util.Map;

public final class CollateralRegistryModels {
    private CollateralRegistryModels() {}
    public record Page(List<Map<String, Object>> items, long totalElements,
                       int page, int size, String source) {}
    public record Readiness(boolean registryReadEnabled, boolean registryWritesEnabled,
                            String authorizationMode, String numberContract,
                            String state) {}
}
