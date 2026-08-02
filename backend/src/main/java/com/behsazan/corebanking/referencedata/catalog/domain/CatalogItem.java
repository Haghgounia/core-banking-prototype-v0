package com.behsazan.corebanking.referencedata.catalog.domain;

public record CatalogItem(
        String resource,
        String category,
        String title,
        String icon,
        CatalogStatus status,
        String parentResource
) {
}
