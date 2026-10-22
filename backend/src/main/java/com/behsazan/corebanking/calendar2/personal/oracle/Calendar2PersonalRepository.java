package com.behsazan.corebanking.calendar2.personal.oracle;

import com.behsazan.corebanking.calendar2.personal.domain.Calendar2PersonalModels.Item;
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

    public Calendar2PersonalRepository(JdbcClient jdbc, @Value("${core-banking.schemas.calendar2:CAL2}") String schema) {
        String owner = schema == null ? "" : schema.trim().toUpperCase(java.util.Locale.ROOT);
        if (!SAFE.matcher(owner).matches()) throw new IllegalArgumentException("Invalid CAL2 owner");
        this.jdbc = jdbc;
        this.table = owner + ".USER_CALENDAR_ITEMS";
        this.canonicalDay = owner + ".CANONICAL_DAY";
    }

    public boolean dayExists(long dayId) {
        return jdbc.sql("SELECT COUNT(*) FROM " + canonicalDay + " WHERE DAY_ID = :dayId")
                .param("dayId", dayId).query(Integer.class).single() == 1;
    }

    public List<Item> monthItems(String actor, LocalDate from, LocalDate to) {
        return jdbc.sql(select() + " WHERE I.ACTOR_KEY = :actor AND D.CANONICAL_DATE >= :fromDay AND D.CANONICAL_DATE < :toExclusive ORDER BY D.CANONICAL_DATE, I.CREATED_AT")
                .param("actor", actor).param("fromDay", Date.valueOf(from)).param("toExclusive", Date.valueOf(to.plusDays(1)))
                .query((rs, row) -> item(rs)).list();
    }

    public List<Item> openReminders(String actor, LocalDate today) {
        return jdbc.sql(select() + " WHERE I.ACTOR_KEY = :actor AND I.ITEM_TYPE = 'REMINDER' AND I.STATUS = 'OPEN' AND D.CANONICAL_DATE <= :horizon ORDER BY D.CANONICAL_DATE DESC, I.CREATED_AT")
                .param("actor", actor).param("horizon", Date.valueOf(today.plusDays(30)))
                .query((rs, row) -> item(rs)).list();
    }

    public Optional<Item> find(String actor, String itemId) {
        return jdbc.sql(select() + " WHERE I.ACTOR_KEY = :actor AND I.ITEM_ID = :itemId")
                .param("actor", actor).param("itemId", itemId).query((rs, row) -> item(rs)).optional();
    }

    public void insert(String actor, String id, long dayId, String type, String title, String description, String time, int advance, String repeat) {
        jdbc.sql("""
                INSERT INTO %s (ITEM_ID, ACTOR_KEY, DAY_ID, ITEM_TYPE, TITLE, DESCRIPTION, REMINDER_TIME,
                                  ADVANCE_DAYS, REPEAT_POLICY, STATUS, FIRST_ADVANCE_ACK, CREATED_AT, UPDATED_AT)
                VALUES (:id, :actor, :dayId, :type, :title, :description, :time,
                        :advance, :repeat, 'OPEN', 'N', SYSTIMESTAMP, SYSTIMESTAMP)
                """.formatted(table))
                .param("id", id).param("actor", actor).param("dayId", dayId).param("type", type)
                .param("title", title).param("description", description).param("time", time)
                .param("advance", advance).param("repeat", repeat).update();
    }

    public int update(String actor, String id, long dayId, String type, String title, String description, String time, int advance, String repeat) {
        return jdbc.sql("""
                UPDATE %s SET DAY_ID = :dayId, ITEM_TYPE = :type, TITLE = :title, DESCRIPTION = :description,
                    REMINDER_TIME = :time, ADVANCE_DAYS = :advance, REPEAT_POLICY = :repeat,
                    STATUS = 'OPEN', FIRST_ADVANCE_ACK = 'N', LAST_ALERT_ACK_DATE = NULL, UPDATED_AT = SYSTIMESTAMP
                WHERE ITEM_ID = :id AND ACTOR_KEY = :actor
                """.formatted(table))
                .param("id", id).param("actor", actor).param("dayId", dayId).param("type", type)
                .param("title", title).param("description", description).param("time", time)
                .param("advance", advance).param("repeat", repeat).update();
    }

    public int complete(String actor, String id, boolean completed) {
        return jdbc.sql("UPDATE " + table + " SET STATUS = :status, FIRST_ADVANCE_ACK = 'N', LAST_ALERT_ACK_DATE = NULL, UPDATED_AT = SYSTIMESTAMP WHERE ITEM_ID = :id AND ACTOR_KEY = :actor")
                .param("status", completed ? "DONE" : "OPEN").param("id", id).param("actor", actor).update();
    }

    public int acknowledge(String actor, String id, LocalDate today, boolean advancedOnce) {
        return jdbc.sql("""
                UPDATE %s SET LAST_ALERT_ACK_DATE = :ackDate,
                    FIRST_ADVANCE_ACK = CASE WHEN :advancedOnce = 1 THEN 'Y' ELSE FIRST_ADVANCE_ACK END,
                    UPDATED_AT = SYSTIMESTAMP
                WHERE ITEM_ID = :id AND ACTOR_KEY = :actor AND STATUS = 'OPEN'
                """.formatted(table))
                .param("ackDate", Date.valueOf(today)).param("advancedOnce", advancedOnce ? 1 : 0)
                .param("id", id).param("actor", actor).update();
    }

    public int delete(String actor, String id) {
        return jdbc.sql("DELETE FROM " + table + " WHERE ITEM_ID = :id AND ACTOR_KEY = :actor")
                .param("id", id).param("actor", actor).update();
    }

    private String select() {
        return """
                SELECT I.ITEM_ID, I.DAY_ID, D.ISO_DATE_TEXT, I.ITEM_TYPE, I.TITLE, I.DESCRIPTION,
                       I.REMINDER_TIME, I.ADVANCE_DAYS, I.REPEAT_POLICY, I.STATUS, I.FIRST_ADVANCE_ACK,
                       TO_CHAR(I.LAST_ALERT_ACK_DATE, 'YYYY-MM-DD') AS LAST_ALERT_ACK_DATE
                  FROM %s I JOIN %s D ON D.DAY_ID = I.DAY_ID
                """.formatted(table, canonicalDay);
    }

    private static Item item(ResultSet rs) throws SQLException {
        return new Item(rs.getString("ITEM_ID"), rs.getLong("DAY_ID"), rs.getString("ISO_DATE_TEXT"),
                rs.getString("ITEM_TYPE"), rs.getString("TITLE"), rs.getString("DESCRIPTION"),
                rs.getString("REMINDER_TIME"), rs.getInt("ADVANCE_DAYS"), rs.getString("REPEAT_POLICY"),
                rs.getString("STATUS"), rs.getString("LAST_ALERT_ACK_DATE"), "Y".equals(rs.getString("FIRST_ADVANCE_ACK")));
    }
}
