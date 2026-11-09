package com.behsazan.corebanking.collateral.reference;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Optional CLT insurer website field, validated separately from the fixed reference catalog. */
final class CltInsuranceWebsitePolicy {
    private CltInsuranceWebsitePolicy() { }

    static String normalized(Object raw) {
        if (raw == null) return null; // Nullable field: clear via normal CRUD UPDATE.
        if (!(raw instanceof String value))
            throw new IllegalArgumentException("CLT_INSURER_WEBSITE_TEXT_REQUIRED");
        if (value.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("CLT_INSURER_WEBSITE_CONTROL_CHAR");
        String site = value.trim();
        if (site.isEmpty() || site.codePointCount(0, site.length()) > 500
                || site.chars().anyMatch(Character::isWhitespace)
                || site.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("CLT_INSURER_WEBSITE_INVALID_LENGTH_OR_WHITESPACE");
        try {
            URI uri = new URI(site);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost();
            if (!("https".equals(scheme) || "http".equals(scheme))
                    || uri.isOpaque() || uri.getRawUserInfo() != null
                    || host == null || host.indexOf('.') < 1 || host.endsWith(".")
                    || uri.getPort() > 65535 || uri.getPort() == 0
                    || "localhost".equalsIgnoreCase(host))
                throw new IllegalArgumentException("CLT_INSURER_WEBSITE_INVALID_URL");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("CLT_INSURER_WEBSITE_INVALID_URL", e);
        }
        return site;
    }
}
