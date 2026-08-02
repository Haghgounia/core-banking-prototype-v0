package com.behsazan.corebanking.referencedata.management.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ReferenceRecordResponse(
        long id,
        Map<String, Object> values,
        List<AncestorValue> ancestors
) {
    public ReferenceRecordResponse {
        values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        ancestors = List.copyOf(ancestors);
    }
}
