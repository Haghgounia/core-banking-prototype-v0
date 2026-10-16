package com.behsazan.corebanking.posting.configuration;

import java.util.List;
import java.util.Map;

/** Read-only projection abstraction, replaceable by an in-memory test fixture. */
public interface PostingConfigurationRepository {
    Long count(String qualifiedTable);
    List<Map<String, Object>> select(String query, Map<String, ?> parameters);
}
