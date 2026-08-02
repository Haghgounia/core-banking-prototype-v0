package com.behsazan.corebanking.referencedata.management.domain;

public record ReferenceSearchQuery(
        String text,
        Long parentId,
        Boolean active,
        int page,
        int size,
        String sortBy,
        String direction
) {
    public ReferenceSearchQuery {
        text = text == null || text.isBlank() ? null : text.trim();
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 100);
        sortBy = sortBy == null || sortBy.isBlank() ? null : sortBy;
        direction = "desc".equalsIgnoreCase(direction) ? "desc" : "asc";
    }

    public int offset() {
        return page * size;
    }
}
