package com.behsazan.corebanking.shared.model;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        long totalElements,
        int page,
        int size
) {
}
