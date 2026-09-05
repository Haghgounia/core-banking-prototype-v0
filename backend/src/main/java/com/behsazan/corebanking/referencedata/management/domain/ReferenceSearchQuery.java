package com.behsazan.corebanking.referencedata.management.domain;

import java.util.LinkedHashMap;
import java.util.Map;

public record ReferenceSearchQuery(
        String text,
        Long parentId,
        Boolean active,
        Map<String, String> filters,
        int page,
        int size,
        String sortBy,
        String direction
) {
    public ReferenceSearchQuery {
        text = text == null || text.isBlank() ? null : text.trim();
        filters = filters == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(filters));
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 100);
        sortBy = sortBy == null || sortBy.isBlank() ? null : sortBy;
        direction = "desc".equalsIgnoreCase(direction) ? "desc" : "asc";
    }

    public int offset() {
        return page * size;
    }
}
