package com.behsazan.corebanking.referencedata.catalog.domain;

import java.util.List;

public record CatalogResponse(List<CatalogItem> items) {
    public CatalogResponse {
        items = List.copyOf(items);
    }
}
