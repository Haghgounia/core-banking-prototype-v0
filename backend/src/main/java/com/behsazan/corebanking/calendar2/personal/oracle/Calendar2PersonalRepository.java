package com.behsazan.corebanking.calendar2.personal.oracle;

import com.behsazan.corebanking.calendar2.personal.domain.Calendar2PersonalModels.Entry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Repository
public class Calendar2PersonalRepository {
    private static final Pattern SAFE = Pattern.compile("[A-Z][A-Z0-9_]{0,127}");
    private final JdbcClient jdbc;
    private final String table;
    private final String canonicalDay;
    private final String sequence;

    public Calendar2PersonalRepository(JdbcClient jdbc, @Value("${core-banking.schemas.calendar2:CAL2}") String schema) {
        String owner = schema == null ? "" : schema.trim().toUpperCase(java.util.Locale.ROOT);
        if (!SAFE.matcher(owner).matches()) throw new IllegalArgumentException("Invalid CAL2 owner");
        this.jdbc = jdbc;
        this.table = owner + ".USER_CALENDAR_ENTRIES";
        this.canonicalDay = owner + ".CANONICAL_DAY";
        this.sequence = owner + ".SEQ_CAL2_USER_CALENDAR_ENTRIES";
    }

    public boolean dayExists(long dayId) {
        return jdbc.sql("SELECT COUNT(*) FROM " + canonicalDay + " WHERE DAY_ID = :dayId")
                .param("dayId", dayId).query(Integer.class).single() == 1;
    }

    public List<Entry> monthEntries(String actor, LocalDate from, LocalDate to) {
        return jdbc.sql(select() + " WHERE I.ACTOR_KEY = :actor AND D.CANONICAL_DATE >= :fromDay AND D.CANONICAL_DATE < :toExclusive ORDER BY D.CANONICAL_DATE, I.CREATED_AT")
                .param("actor", actor).param("fromDay", Date.valueOf(from)).param("toExclusive", Date.valueOf(to.plusDays(1)))
                .query((rs, row) -> entry(rs)).list();
    }

    public List<Entry> openReminders(String actor, LocalDate today) {
        return jdbc.sql(select() + " WHERE I.ACTOR_KEY = :actor AND I.ENTRY_TYPE = 'REMINDER' AND I.STATUS = 'OPEN' AND D.CANONICAL_DATE <= :horizon ORDER BY D.CANONICAL_DATE DESC, I.CREATED_AT")
                .param("actor", actor).param("horizon", Date.valueOf(today.plusDays(30)))
                .query((rs, row) -> entry(rs)).list();
    }

    public Optional<Entry> find(String actor, long entryId) {
        return jdbc.sql(select() + " WHERE I.ACTOR_KEY = :actor AND I.USER_CALENDAR_ENTRY_ID = :entryId")
                .param("actor", actor).param("entryId", entryId).query((rs, row) -> entry(rs)).optional();
    }

    public long insert(String actor, long dayId, String type, String title, String description, String time, int advance, String repeat) {
        // Allocate the numeric key from Oracle, never from a JVM counter or UUID.
        // The table also has DEFAULT SEQ_CAL2_USER_CALENDAR_ENTRIES.NEXTVAL for other SQL clients.
        long id = jdbc.sql("SELECT " + sequence + ".NEXTVAL FROM DUAL").query(Long.class).single();
        jdbc.sql("""
                INSERT INTO %s (USER_CALENDAR_ENTRY_ID, ACTOR_KEY, DAY_ID, ENTRY_TYPE, TITLE, DESCRIPTION, REMINDER_TIME,
                                  ADVANCE_DAYS, REPEAT_POLICY, STATUS, FIRST_ADVANCE_ACK, CREATED_AT, UPDATED_AT)
                VALUES (:id, :actor, :dayId, :type, :title, :description, :time,
                        :advance, :repeat, 'OPEN', 'N', SYSTIMESTAMP, SYSTIMESTAMP)
                """.formatted(table))
                .param("id", id).param("actor", actor).param("dayId", dayId).param("type", type)
                .param("title", title).param("description", description).param("time", time)
                .param("advance", advance).param("repeat", repeat).update();
        return id;
    }

    public int update(String actor, long id, long dayId, String type, String title, String description, String time, int advance, String repeat) {
        return jdbc.sql("""
                UPDATE %s SET DAY_ID = :dayId, ENTRY_TYPE = :type, TITLE = :title, DESCRIPTION = :description,
                    REMINDER_TIME = :time, ADVANCE_DAYS = :advance, REPEAT_POLICY = :repeat,
                    STATUS = 'OPEN', FIRST_ADVANCE_ACK = 'N', LAST_ALERT_ACK_DATE = NULL, UPDATED_AT = SYSTIMESTAMP
                WHERE USER_CALENDAR_ENTRY_ID = :id AND ACTOR_KEY = :actor
                """.formatted(table))
                .param("id", id).param("actor", actor).param("dayId", dayId).param("type", type)
                .param("title", title).param("description", description).param("time", time)
                .param("advance", advance).param("repeat", repeat).update();
    }

    public int complete(String actor, long id, boolean completed) {
        return jdbc.sql("UPDATE " + table + " SET STATUS = :status, FIRST_ADVANCE_ACK = 'N', LAST_ALERT_ACK_DATE = NULL, UPDATED_AT = SYSTIMESTAMP WHERE USER_CALENDAR_ENTRY_ID = :id AND ACTOR_KEY = :actor")
                .param("status", completed ? "DONE" : "OPEN").param("id", id).param("actor", actor).update();
    }

    public int acknowledge(String actor, long id, LocalDate today, boolean advancedOnce) {
        return jdbc.sql("""
                UPDATE %s SET LAST_ALERT_ACK_DATE = :ackDate,
                    FIRST_ADVANCE_ACK = CASE WHEN :advancedOnce = 1 THEN 'Y' ELSE FIRST_ADVANCE_ACK END,
                    UPDATED_AT = SYSTIMESTAMP
                WHERE USER_CALENDAR_ENTRY_ID = :id AND ACTOR_KEY = :actor AND STATUS = 'OPEN'
                """.formatted(table))
                .param("ackDate", Date.valueOf(today)).param("advancedOnce", advancedOnce ? 1 : 0)
                .param("id", id).param("actor", actor).update();
    }

    public int delete(String actor, long id) {
        return jdbc.sql("DELETE FROM " + table + " WHERE USER_CALENDAR_ENTRY_ID = :id AND ACTOR_KEY = :actor")
                .param("id", id).param("actor", actor).update();
    }

    private String select() {
        return """
                SELECT I.USER_CALENDAR_ENTRY_ID, I.DAY_ID, D.ISO_DATE_TEXT, I.ENTRY_TYPE, I.TITLE, I.DESCRIPTION,
                       I.REMINDER_TIME, I.ADVANCE_DAYS, I.REPEAT_POLICY, I.STATUS, I.FIRST_ADVANCE_ACK,
                       TO_CHAR(I.LAST_ALERT_ACK_DATE, 'YYYY-MM-DD') AS LAST_ALERT_ACK_DATE
                  FROM %s I JOIN %s D ON D.DAY_ID = I.DAY_ID
                """.formatted(table, canonicalDay);
    }

    private static Entry entry(ResultSet rs) throws SQLException {
        return new Entry(rs.getLong("USER_CALENDAR_ENTRY_ID"), rs.getLong("DAY_ID"), rs.getString("ISO_DATE_TEXT"),
                rs.getString("ENTRY_TYPE"), rs.getString("TITLE"), rs.getString("DESCRIPTION"),
                rs.getString("REMINDER_TIME"), rs.getInt("ADVANCE_DAYS"), rs.getString("REPEAT_POLICY"),
                rs.getString("STATUS"), rs.getString("LAST_ALERT_ACK_DATE"), "Y".equals(rs.getString("FIRST_ADVANCE_ACK")));
    }
}
