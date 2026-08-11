package com.behsazan.corebanking.cif.reference.oracle;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

final class PartyReferenceKeyCodec {
    private static final char SEPARATOR = '\u001F';

    private PartyReferenceKeyCodec() {
    }

    static String encode(List<String> values) {
        String joined = String.join(String.valueOf(SEPARATOR), values);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(joined.getBytes(StandardCharsets.UTF_8));
    }

    static List<String> decode(String key, int expectedParts) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(key), StandardCharsets.UTF_8);
            List<String> values = List.of(decoded.split(String.valueOf(SEPARATOR), -1));
            if (values.size() != expectedParts) {
                throw new IllegalArgumentException("Invalid key part count");
            }
            return values;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid party reference record key", exception);
        }
    }
}
