package com.behsazan.corebanking.posting.streaming;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Prevents activation if additive PST Oracle columns or receipt storage are unavailable. */
@Component
@ConditionalOnProperty(prefix = "core-banking.event-stream", name = "enabled", havingValue = "true")
public class EventStreamSchemaGuard implements SmartInitializingSingleton {
    private final JdbcTemplate jdbc;
    private final boolean producer;
    private final boolean consumer;

    public EventStreamSchemaGuard(JdbcTemplate jdbc,
            @Value("${core-banking.event-stream.producer-enabled:false}") boolean producer,
            @Value("${core-banking.event-stream.consumer-enabled:false}") boolean consumer) {
        this.jdbc = jdbc;
        this.producer = producer;
        this.consumer = consumer;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (producer) {
            require("POSTING_OUTBOX_EVENTS", Set.of(
                "IDENTIFIER", "EVENT_IDENTIFIER", "AGGREGATE_REFERENCE", "EVENT_TYPE", "PAYLOAD_HASH",
                "DELIVERY_STATE", "CREATED_AT", "STREAM_PAYLOAD", "ATTEMPT_COUNT", "NEXT_ATTEMPT_AT",
                "LEASE_TOKEN", "LEASE_UNTIL", "PUBLISHED_AT", "LAST_ERROR"));
            requireUnique("POSTING_OUTBOX_EVENTS", Set.of("EVENT_IDENTIFIER"));
        }
        if (consumer) {
            require("POSTING_INBOX_RECEIPTS", Set.of(
                    "IDENTIFIER", "SOURCE_DOMAIN", "SOURCE_EVENT_IDENTIFIER", "ACCOUNTING_PURPOSE", "PAYLOAD_HASH", "RECEIVED_AT"));
            requireUnique("POSTING_INBOX_RECEIPTS", Set.of("SOURCE_DOMAIN", "SOURCE_EVENT_IDENTIFIER", "ACCOUNTING_PURPOSE"));
        }
    }

    /** Exact enabled validated unique keys are mandatory for replay safety. */
    private void requireUnique(String table, Set<String> expectedColumns) {
        Map<String, Set<String>> candidates = new LinkedHashMap<>();
        jdbc.query("SELECT C.CONSTRAINT_NAME, CC.COLUMN_NAME "
                        + "FROM ALL_CONSTRAINTS C JOIN ALL_CONS_COLUMNS CC "
                        + "ON C.OWNER=CC.OWNER AND C.CONSTRAINT_NAME=CC.CONSTRAINT_NAME "
                        + "AND C.TABLE_NAME=CC.TABLE_NAME "
                        + "WHERE C.OWNER='PST' AND C.TABLE_NAME=? "
                        + "AND C.CONSTRAINT_TYPE='U' AND C.STATUS='ENABLED' AND C.VALIDATED='VALIDATED'",
                (rs, i) -> new String[]{rs.getString(1), rs.getString(2)}, table)
                .forEach(entry -> candidates.computeIfAbsent(entry[0], k -> new HashSet<>()).add(entry[1]));
        if (candidates.values().stream().noneMatch(expectedColumns::equals))
            throw new IllegalStateException("Event streaming refused: exact unique key missing in PST." + table);
    }

    private void require(String table, Set<String> requiredColumns) {
        Set<String> found = new HashSet<>(jdbc.query(
                "SELECT COLUMN_NAME FROM ALL_TAB_COLUMNS WHERE OWNER='PST' AND TABLE_NAME=?",
                (rs, row) -> rs.getString(1), table));
        if (!found.containsAll(requiredColumns)) {
            var missing = new HashSet<>(requiredColumns);
            missing.removeAll(found);
            throw new IllegalStateException("Event streaming refused: PST." + table
                    + " schema is not approved/complete: " + missing);
        }
    }
}
