package com.behsazan.corebanking.iam.s1b;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Pure policy: use only claims from a VERIFIED, audience-bound JWT. Never trust request headers. */
public final class IamS1bPolicy {
    private static final Pattern DOMAIN = Pattern.compile("[A-Z][A-Z0-9_]{1,49}");
    private static final Pattern CONTEXT = Pattern.compile("[A-Z][A-Z0-9_]{1,39}");

    private IamS1bPolicy() { }

    public static String domainCode(String candidate) {
        if (candidate == null || !DOMAIN.matcher(candidate).matches())
            throw new IllegalArgumentException("IAM_S1B_INVALID_DOMAIN_CODE");
        return candidate;
    }

    public static String normalizedUsernameFragment(String candidate) {
        if (candidate == null || candidate.isBlank()) return null;
        String normalized = java.text.Normalizer.normalize(candidate.strip(), java.text.Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        if (normalized.length() > 80 || normalized.chars().anyMatch(c -> Character.isISOControl(c)))
            throw new IllegalArgumentException("IAM_S1B_INVALID_SEARCH");
        return normalized;
    }

    public static String escapedLike(String term) {
        if (term == null) return null;
        return "%" + term.replace("!", "!!").replace("%", "!%")
                .replace("_", "!_") + "%";
    }

    /** Deliberately exact case and membership; wildcard grants and case-folding are prohibited. */
    public static boolean canRead(String subject, Collection<String> tokenScopes,
                                  Collection<String> tokenDomains, Collection<String> tokenContexts,
                                  String domain, String context) {
        return subject != null && !subject.isBlank()
                && tokenScopes != null && tokenScopes.contains("iam.users.read")
                && tokenDomains != null && tokenDomains.contains(domain)
                && tokenContexts != null && tokenContexts.contains(context)
                && DOMAIN.matcher(domain).matches() && CONTEXT.matcher(context).matches();
    }

    public static List<String> safeStringClaim(Object raw) {
        if (!(raw instanceof List<?> list) || list.size() > 32) return List.of();
        for (Object v : list) if (!(v instanceof String s) || s.isBlank()) return List.of();
        return list.stream().map(String.class::cast).toList();
    }
}
