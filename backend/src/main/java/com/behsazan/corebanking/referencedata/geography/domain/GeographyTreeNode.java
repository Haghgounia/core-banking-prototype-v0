package com.behsazan.corebanking.referencedata.geography.domain;

public record GeographyTreeNode(
        String resource,
        long id,
        String code,
        String label,
        boolean hasChildren
) {
}
