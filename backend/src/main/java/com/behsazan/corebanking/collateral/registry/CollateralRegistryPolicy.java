package com.behsazan.corebanking.collateral.registry;

import java.math.BigDecimal;

/** Pure validation: never parse Oracle NUMBER(19) identifiers through float or JS Number. */
public final class CollateralRegistryPolicy {
    private CollateralRegistryPolicy() {}
    public static BigDecimal id(String text) {
        if (text == null || !text.matches("[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException("CLT_REGISTRY_ID_INVALID");
        }
        return new BigDecimal(text);
    }
    public static String branch(String text) {
        if (text == null || !text.matches("[A-Za-z0-9_-]{1,30}")) {
            throw new IllegalArgumentException("CLT_REGISTRY_BRANCH_CLAIM_INVALID");
        }
        return text;
    }
    /** Accept exact business codes only; no SQL text, wildcard or unreviewed normalization. */
    public static String groupCode(String text) {
        if (text == null || !text.matches("[A-Za-z0-9_-]{1,5}")) {
            throw new IllegalArgumentException("CLT_REGISTRY_GROUP_CODE_INVALID");
        }
        return text;
    }
    public static void pagination(int page, int size) {
        if (page < 0 || page > 1000 || size < 1 || size > 50) {
            throw new IllegalArgumentException("CLT_REGISTRY_PAGE_INVALID");
        }
    }
}
