package com.behsazan.corebanking.iam.s1b;

import java.util.*;
import java.util.regex.Pattern;

/** Pure, fail-closed classification of *verified* JWT domain/context arrays. Signature validation is separate. */
public final class IamS1bTokenClaimPolicy {
    private static final Pattern DOMAIN = Pattern.compile("[A-Z][A-Z0-9_]{1,49}");
    private static final Pattern CONTEXT = Pattern.compile("[A-Z][A-Z0-9_]{1,39}");
    private IamS1bTokenClaimPolicy() { }

    public static boolean validClaims(Map<String, Object> claims) {
        if (claims == null) return false;
        return validArray(claims.get("iam_domains"), DOMAIN)
                && validArray(claims.get("iam_contexts"), CONTEXT);
    }

    private static boolean validArray(Object raw, Pattern allowed) {
        if (!(raw instanceof List<?> values) || values.size() > 32) return false;
        Set<String> seen = new HashSet<>();
        for (Object value : values) {
            if (!(value instanceof String s) || !allowed.matcher(s).matches() || !seen.add(s)) return false;
        }
        return true; // Empty arrays are valid but grant nothing (403 on every domain read).
    }
}
