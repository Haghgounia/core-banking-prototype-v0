package com.behsazan.corebanking.calendar2.personalv2;

import com.behsazan.corebanking.calendar2.personalv2.PersonalV2Models.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.SqlParameterValue;
import java.sql.Types;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** R2-B1 Oracle adapter. Only CAL2 personal tables are mutated. */
@Repository
public class PersonalV2Repository {
    private static final Pattern SAFE = Pattern.compile("[A-Z][A-Z0-9_]{0,127}");
    private final JdbcClient jdbc;
    private final String schema;

    public PersonalV2Repository(JdbcClient jdbc, @Value("${core-banking.schemas.calendar2:CAL2}") String schema) {
        String value = schema == null ? "" : schema.trim().toUpperCase(java.util.Locale.ROOT);
        if (!SAFE.matcher(value).matches()) throw new IllegalArgumentException("Unsafe Oracle schema name");
        this.jdbc = jdbc;
        this.schema = value;
    }
    private String t(String table) { return schema + ".USER_CALENDAR_" + table; }
    private String seq(String name) { return schema + ".SEQ_CAL2_" + name; }
    private String dayTable() { return schema + ".CANONICAL_DAY"; }

    public boolean dayExists(BigDecimal id) {
        return jdbc.sql("SELECT COUNT(*) FROM " + dayTable() + " WHERE DAY_ID = :day")
                .param("day", id).query(Integer.class).single() == 1;
    }
    public LocalDate dayDate(BigDecimal dayId) {
        return jdbc.sql("SELECT CANONICAL_DATE FROM " + dayTable() + " WHERE DAY_ID=:day")
                .param("day",dayId).query((rs,n)->rs.getDate(1).toLocalDate()).single();
    }
    public boolean calendarVariantExists(BigDecimal id) {
        return jdbc.sql("SELECT COUNT(*) FROM " + schema + ".CALENDAR_VARIANT WHERE CALENDAR_VARIANT_ID=:id AND ACTIVE_FLAG='Y'")
                .param("id",id).query(Integer.class).single() == 1;
    }

    private String projection() {
        return """
             SELECT E.USER_CALENDAR_ENTRY_ID, O.USER_CALENDAR_OCCURRENCE_ID, O.DAY_ID,
                    D.ISO_DATE_TEXT, E.ENTRY_TYPE, E.TITLE, E.DESCRIPTION, E.ENTRY_STATUS,
                    E.VERSION_NO, O.VERSION_NO AS OCC_VERSION_NO, O.OCCURRENCE_STATUS,
                    O.LOCAL_START_MINUTE AS START_MINUTE,
                    COALESCE((SELECT MAX(R.LOCAL_ALERT_MINUTE) FROM %s R
                     WHERE R.USER_CALENDAR_ENTRY_ID=E.USER_CALENDAR_ENTRY_ID AND R.IS_ACTIVE='Y'
                       AND R.ALERT_PATTERN<>'AT_DUE'),
                    (SELECT MAX(R.LOCAL_ALERT_MINUTE) FROM %s R
                     WHERE R.USER_CALENDAR_ENTRY_ID=E.USER_CALENDAR_ENTRY_ID AND R.IS_ACTIVE='Y'
                       AND R.ALERT_PATTERN='AT_DUE')) AS ALERT_MINUTE,
                    (SELECT MAX(CASE WHEN R.ALERT_PATTERN='DAILY_WINDOW' THEN R.WINDOW_START_DAYS
                                     WHEN R.ALERT_PATTERN='SINGLE_OFFSET' AND R.OFFSET_UNIT='CALENDAR_DAY' THEN R.OFFSET_VALUE
                                     ELSE 0 END)
                     FROM %s R WHERE R.USER_CALENDAR_ENTRY_ID=E.USER_CALENDAR_ENTRY_ID AND R.IS_ACTIVE='Y') AS ADVANCE_DAYS,
                    (SELECT MAX(CASE WHEN R.ALERT_PATTERN='DAILY_WINDOW' THEN 'DAILY'
                                     WHEN R.ALERT_PATTERN='SINGLE_OFFSET' THEN 'ONCE' END)
                     FROM %s R WHERE R.USER_CALENDAR_ENTRY_ID=E.USER_CALENDAR_ENTRY_ID AND R.IS_ACTIVE='Y') AS REPEAT_POLICY,
                    (SELECT MAX(R.FREQUENCY) FROM %s R WHERE R.USER_CALENDAR_ENTRY_ID=E.USER_CALENDAR_ENTRY_ID
                        AND R.RULE_STATUS='ACTIVE') AS RECURRENCE_FREQUENCY
               FROM %s E JOIN %s O ON O.USER_CALENDAR_ENTRY_ID=E.USER_CALENDAR_ENTRY_ID
               JOIN %s D ON D.DAY_ID=O.DAY_ID
              WHERE E.OWNER_ISSUER=:issuer AND E.OWNER_SUBJECT_ID=:subject
                AND E.ENTRY_STATUS='ACTIVE' AND O.OCCURRENCE_STATUS IN ('OPEN','DONE')
            """.formatted(t("REMINDER_RULES"),t("REMINDER_RULES"),t("REMINDER_RULES"),t("REMINDER_RULES"),t("RECURRENCE_RULES"),
                t("ENTRIES"),t("OCCURRENCES"),dayTable());
    }

    public List<Entry> entries(Owner owner, LocalDate from, LocalDate to) {
        return jdbc.sql(projection() + " AND D.CANONICAL_DATE>=:fromDate AND D.CANONICAL_DATE<:exclusive ORDER BY D.CANONICAL_DATE, E.USER_CALENDAR_ENTRY_ID")
                .param("issuer",owner.issuer()).param("subject",owner.subjectId())
                .param("fromDate",Date.valueOf(from)).param("exclusive",Date.valueOf(to.plusDays(1)))
                .query((rs,n)->mapEntry(rs)).list();
    }
    public Optional<Entry> entry(Owner owner, long id) {
        return jdbc.sql(projection() + " AND E.USER_CALENDAR_ENTRY_ID=:id ORDER BY O.DAY_ID FETCH FIRST 1 ROWS ONLY")
                .param("issuer",owner.issuer()).param("subject",owner.subjectId()).param("id",id)
                .query((rs,n)->mapEntry(rs)).optional();
    }
    public Optional<Entry> occurrence(Owner owner,long occurrenceId) {
        return jdbc.sql(projection() + " AND O.USER_CALENDAR_OCCURRENCE_ID=:id")
                .param("issuer",owner.issuer()).param("subject",owner.subjectId()).param("id",occurrenceId)
                .query((rs,n)->mapEntry(rs)).optional();
    }
    private static Integer nullableInt(ResultSet rs,String column) throws SQLException {
        int v=rs.getInt(column);return rs.wasNull()?null:v;
    }
    private static String minuteText(Integer value) {
        return value==null?null:String.format(java.util.Locale.ROOT,"%02d:%02d",value/60,value%60);
    }
    private static Entry mapEntry(ResultSet rs) throws SQLException {
        int minute = rs.getInt("ALERT_MINUTE");
        String hhmm = rs.wasNull() ? null : String.format(java.util.Locale.ROOT,"%02d:%02d",minute/60,minute%60);
        int days = rs.getInt("ADVANCE_DAYS");
        return new Entry(rs.getString("USER_CALENDAR_ENTRY_ID"),rs.getString("USER_CALENDAR_OCCURRENCE_ID"),
                rs.getString("DAY_ID"),rs.getString("ISO_DATE_TEXT"),rs.getString("ENTRY_TYPE"),
                rs.getString("TITLE"),rs.getString("DESCRIPTION"),minuteText(nullableInt(rs, "START_MINUTE")),hhmm,days,
                rs.getString("REPEAT_POLICY")==null?"DAILY":rs.getString("REPEAT_POLICY"),
                rs.getString("OCCURRENCE_STATUS"),rs.getLong("VERSION_NO"),rs.getLong("OCC_VERSION_NO"),
                rs.getString("RECURRENCE_FREQUENCY"));
    }
    public long next(String sequence) {
        long id = jdbc.sql("SELECT " + seq(sequence) + ".NEXTVAL FROM DUAL").query(Long.class).single();
        if(id<1 || id>999_999_999_999L) throw new IllegalStateException("NUMBER(12) sequence exhaustion: " + sequence);
        return id;
    }
    public void insertEntry(Owner owner,long id, BigDecimal day,String type,String title,String description,
                            Integer minute,String timeZone) {
        jdbc.sql("""
            INSERT INTO %s (USER_CALENDAR_ENTRY_ID,OWNER_ISSUER,OWNER_SUBJECT_ID,ENTRY_TYPE,TITLE,DESCRIPTION,
              START_DAY_ID,ALL_DAY_FLAG,START_LOCAL_MINUTE,TIME_ZONE_ID,ENTRY_STATUS,SOURCE_KIND,
              VERSION_NO,CREATED_AT_UTC,UPDATED_AT_UTC,CREATED_BY_SUBJECT_ID,UPDATED_BY_SUBJECT_ID)
            VALUES (:id,:issuer,:subject,:type,:title,:description,:day,:allDay,:minute,:zone,'ACTIVE','USER',
              1,SYSTIMESTAMP,SYSTIMESTAMP,:subject,:subject)
            """.formatted(t("ENTRIES"))).param("id",id).param("issuer",owner.issuer())
                .param("subject",owner.subjectId()).param("type",type).param("title",title)
                .param("description",new SqlParameterValue(Types.CLOB,description)).param("day",day)
                .param("allDay",minute==null?"Y":"N").param("minute",minute)
                .param("zone",timeZone).update();
    }
    public int updateEntry(Owner owner,long id,long version,BigDecimal day,String type,String title,
                           String description,Integer minute,String zone) {
        return jdbc.sql("""
            UPDATE %s SET START_DAY_ID=:day,ENTRY_TYPE=:type,TITLE=:title,DESCRIPTION=:description,
                ALL_DAY_FLAG=:allDay,START_LOCAL_MINUTE=:minute,TIME_ZONE_ID=:zone,
                VERSION_NO=VERSION_NO+1, UPDATED_AT_UTC=SYSTIMESTAMP,UPDATED_BY_SUBJECT_ID=:subject
            WHERE USER_CALENDAR_ENTRY_ID=:id AND OWNER_ISSUER=:issuer AND OWNER_SUBJECT_ID=:subject
              AND ENTRY_STATUS='ACTIVE' AND VERSION_NO=:version
            """.formatted(t("ENTRIES"))).param("day",day).param("type",type).param("title",title)
                .param("description",new SqlParameterValue(Types.CLOB,description)).param("allDay",minute==null?"Y":"N")
                .param("minute",minute).param("zone",zone).param("id",id).param("issuer",owner.issuer())
                .param("subject",owner.subjectId()).param("version",version).update();
    }
    public int cancelEntry(Owner owner,long id,long version) {
        return jdbc.sql("""
             UPDATE %s SET ENTRY_STATUS='CANCELLED',DELETED_AT_UTC=SYSTIMESTAMP,
               VERSION_NO=VERSION_NO+1,UPDATED_AT_UTC=SYSTIMESTAMP, UPDATED_BY_SUBJECT_ID=:subject
             WHERE USER_CALENDAR_ENTRY_ID=:id AND OWNER_ISSUER=:issuer AND OWNER_SUBJECT_ID=:subject
             AND ENTRY_STATUS='ACTIVE' AND VERSION_NO=:version
            """.formatted(t("ENTRIES"))).param("subject",owner.subjectId()).param("issuer",owner.issuer())
                .param("id",id).param("version",version).update();
    }
    public long insertOccurrence(long entryId, BigDecimal dayId, String slot, Integer localMinute,String dueUtc,Long recurrenceId) {
        long id=next("UCO");
        jdbc.sql("""
            INSERT INTO %s (USER_CALENDAR_OCCURRENCE_ID,USER_CALENDAR_ENTRY_ID,RECURRENCE_RULE_ID,ORIGINAL_SLOT_KEY,
              ORIGINAL_DAY_ID,DAY_ID,LOCAL_START_MINUTE,DUE_AT_UTC,OCCURRENCE_STATUS,
              ORIGIN_RULE_VERSION_NO,IS_OVERRIDDEN,VERSION_NO,CREATED_AT_UTC,UPDATED_AT_UTC)
            VALUES (:id,:entry,:recurrence,:slot,:day,:day,:minute,
                CASE WHEN :due IS NULL THEN NULL ELSE TO_TIMESTAMP_TZ(:due,'YYYY-MM-DD"T"HH24:MI:SSTZH:TZM') END,
                'OPEN',:ruleVersion,'N',1,SYSTIMESTAMP,SYSTIMESTAMP)
            """.formatted(t("OCCURRENCES")))
                .param("id",id).param("entry",entryId).param("recurrence",recurrenceId)
                .param("ruleVersion",recurrenceId==null?0:1).param("slot",slot).param("day",dayId)
                .param("minute",localMinute).param("due",dueUtc).update();
        return id;
    }
    public Optional<Long> occurrenceBySlot(long entry,String slot) {
        return jdbc.sql("SELECT USER_CALENDAR_OCCURRENCE_ID FROM " + t("OCCURRENCES") +
                " WHERE USER_CALENDAR_ENTRY_ID=:entry AND ORIGINAL_SLOT_KEY=:slot")
                .param("entry",entry).param("slot",slot).query(Long.class).optional();
    }
    public int updateOneOccurrence(Owner owner,long id,long entry,BigDecimal day,Integer minute,String dueUtc) {
        return jdbc.sql("""
            UPDATE %s SET DAY_ID=:day,LOCAL_START_MINUTE=:minute,
                DUE_AT_UTC=CASE WHEN :due IS NULL THEN NULL ELSE TO_TIMESTAMP_TZ(:due,'YYYY-MM-DD"T"HH24:MI:SSTZH:TZM') END,
                IS_OVERRIDDEN='Y',VERSION_NO=VERSION_NO+1,UPDATED_AT_UTC=SYSTIMESTAMP
            WHERE USER_CALENDAR_ENTRY_ID=:entry AND OCCURRENCE_STATUS='OPEN'
              AND USER_CALENDAR_OCCURRENCE_ID=:id AND EXISTS
               (SELECT 1 FROM %s E WHERE E.USER_CALENDAR_ENTRY_ID=:entry
                 AND E.OWNER_ISSUER=:issuer AND E.OWNER_SUBJECT_ID=:subject)
            """.formatted(t("OCCURRENCES"),t("ENTRIES")))
                .param("day",day).param("minute",minute).param("due",dueUtc)
                .param("entry",entry).param("id",id).param("issuer",owner.issuer())
                .param("subject",owner.subjectId()).update();
    }
    public int setOccurrenceCompleted(Owner owner,long id,long version,boolean done) {
        return jdbc.sql("""
            UPDATE %s O SET OCCURRENCE_STATUS=:status,
              COMPLETED_AT_UTC=CASE WHEN :done=1 THEN SYSTIMESTAMP ELSE NULL END,
              COMPLETED_BY_SUBJECT_ID=CASE WHEN :done=1 THEN :subject ELSE NULL END,
              VERSION_NO=VERSION_NO+1,UPDATED_AT_UTC=SYSTIMESTAMP
            WHERE O.USER_CALENDAR_OCCURRENCE_ID=:id AND O.VERSION_NO=:version
              AND O.OCCURRENCE_STATUS IN ('OPEN','DONE') AND EXISTS
                (SELECT 1 FROM %s E WHERE E.USER_CALENDAR_ENTRY_ID=O.USER_CALENDAR_ENTRY_ID
                   AND E.OWNER_ISSUER=:issuer AND E.OWNER_SUBJECT_ID=:subject AND E.ENTRY_TYPE='TASK'
                   AND E.ENTRY_STATUS='ACTIVE')
            """.formatted(t("OCCURRENCES"),t("ENTRIES")))
                .param("status",done?"DONE":"OPEN").param("done",done?1:0)
                .param("subject",owner.subjectId()).param("id",id).param("version",version)
                .param("issuer",owner.issuer()).update();
    }
    public void cancelPendingAlertsForEntry(long entryId) {
        jdbc.sql("""
            UPDATE %s SET ALERT_STATUS='CANCELLED',VERSION_NO=VERSION_NO+1,UPDATED_AT_UTC=SYSTIMESTAMP
            WHERE USER_CALENDAR_ENTRY_ID=:entry AND ALERT_STATUS IN ('SCHEDULED','SNOOZED','AVAILABLE')
            """.formatted(t("ALERTS"))).param("entry",entryId).update();
    }
    public void cancelAlertsForOccurrence(long occurrenceId) {
        jdbc.sql("""
            UPDATE %s SET ALERT_STATUS='CANCELLED',VERSION_NO=VERSION_NO+1,UPDATED_AT_UTC=SYSTIMESTAMP
             WHERE USER_CALENDAR_OCCURRENCE_ID=:occ AND ALERT_STATUS IN ('SCHEDULED','SNOOZED','AVAILABLE')
            """.formatted(t("ALERTS"))).param("occ",occurrenceId).update();
    }
    public void disableReminders(long entryId) {
        jdbc.sql("UPDATE " + t("REMINDER_RULES") + " SET IS_ACTIVE='N',UPDATED_AT_UTC=SYSTIMESTAMP WHERE USER_CALENDAR_ENTRY_ID=:entry AND IS_ACTIVE='Y'")
                .param("entry",entryId).update();
    }
    public long createReminder(long entry,String pattern,String relation,Integer offset,String unit,
                               Integer start,Integer end,Integer step,Integer localMinute,String subject) {
        long id=next("URR");
        jdbc.sql("""
             INSERT INTO %s (USER_CALENDAR_REMINDER_RULE_ID,USER_CALENDAR_ENTRY_ID,RULE_VERSION_NO,ALERT_PATTERN,
              TRIGGER_RELATION,OFFSET_VALUE,OFFSET_UNIT,WINDOW_START_DAYS,WINDOW_END_DAYS,
              WINDOW_INTERVAL_DAYS,LOCAL_ALERT_MINUTE,CHANNEL_CODE,ACK_STOP_POLICY,IS_ACTIVE,
              CREATED_AT_UTC,UPDATED_AT_UTC,CREATED_BY_SUBJECT_ID)
             VALUES (:id,:entry,1,:pattern,:relation,:offset,:unit,:start,:end,:step,:minute,
               'IN_APP','NONE','Y',SYSTIMESTAMP,SYSTIMESTAMP,:subject)
            """.formatted(t("REMINDER_RULES")))
                .param("id",id).param("entry",entry).param("pattern",pattern).param("relation",relation)
                .param("offset",offset).param("unit",unit).param("start",start).param("end",end)
                .param("step",step).param("minute",localMinute).param("subject",subject).update();
        return id;
    }
    public List<Reminder> reminders(long entry) {
        return jdbc.sql("""
            SELECT USER_CALENDAR_REMINDER_RULE_ID,RULE_VERSION_NO,ALERT_PATTERN,OFFSET_VALUE,OFFSET_UNIT,
             WINDOW_START_DAYS,WINDOW_END_DAYS,WINDOW_INTERVAL_DAYS,LOCAL_ALERT_MINUTE
            FROM %s WHERE USER_CALENDAR_ENTRY_ID=:entry AND IS_ACTIVE='Y'
            """.formatted(t("REMINDER_RULES"))).param("entry",entry)
            .query((rs,n)->new Reminder(rs.getLong(1),rs.getInt(2),rs.getString(3),nullableInt(rs,4),
                    rs.getString(5),nullableInt(rs,6),nullableInt(rs,7),nullableInt(rs,8),nullableInt(rs,9))).list();
    }
    private static Integer nullableInt(ResultSet rs,int column) throws SQLException {
        int n=rs.getInt(column);return rs.wasNull()?null:n;
    }
    public record Reminder(long id,int version,String pattern,Integer offset,String unit,Integer startDays,
                           Integer endDays,Integer intervalDays,Integer alertMinute) {}

    /** Normalized UTC key in Oracle timestamp-with-time-zone; uniqueness prevents duplicate slots. */
    public void insertAlert(long occurrence,long entry,Reminder rule,String slot,String atUtc) {
        int exists=jdbc.sql("""
            SELECT COUNT(*) FROM %s WHERE USER_CALENDAR_OCCURRENCE_ID=:occ AND REMINDER_RULE_ID=:rule
               AND RULE_VERSION_NO=:rv AND SCHEDULED_SLOT_KEY=:slot
            """.formatted(t("ALERTS")))
             .param("occ",occurrence).param("rule",rule.id()).param("rv",rule.version())
             .param("slot",slot).query(Integer.class).single();
        if(exists>0)return;
        long id=next("UCA");
        try {
            jdbc.sql("""
             INSERT INTO %s (USER_CALENDAR_ALERT_ID,USER_CALENDAR_OCCURRENCE_ID,USER_CALENDAR_ENTRY_ID,
              REMINDER_RULE_ID,RULE_VERSION_NO,SCHEDULED_SLOT_KEY,PLANNED_AT_UTC,NEXT_ELIGIBLE_AT_UTC,
              ALERT_STATUS,SNOOZE_COUNT,ATTEMPT_COUNT,VERSION_NO,CREATED_AT_UTC,UPDATED_AT_UTC)
             VALUES (:id,:occ,:entry,:rule,:rv,:slot,
              TO_TIMESTAMP_TZ(:at,'YYYY-MM-DD"T"HH24:MI:SSTZH:TZM'),
              TO_TIMESTAMP_TZ(:at,'YYYY-MM-DD"T"HH24:MI:SSTZH:TZM'),
              'SCHEDULED',0,0,1,SYSTIMESTAMP,SYSTIMESTAMP)
             """.formatted(t("ALERTS"))).param("id",id).param("occ",occurrence).param("entry",entry)
                    .param("rule",rule.id()).param("rv",rule.version()).param("slot",slot)
                    .param("at",atUtc).update();
        } catch (DuplicateKeyException ignored) {
            // Another generator already produced this exact immutable slot; no re-delivery.
        }
    }
    public List<Alert> availableAlerts(Owner owner,LocalDate today) {
        return jdbc.sql("""
            SELECT A.USER_CALENDAR_ALERT_ID,A.USER_CALENDAR_OCCURRENCE_ID,A.USER_CALENDAR_ENTRY_ID,
              O.DAY_ID,D.ISO_DATE_TEXT,E.TITLE,A.SCHEDULED_SLOT_KEY,A.ALERT_STATUS,
              R.LOCAL_ALERT_MINUTE
            FROM %s A
            JOIN %s E ON E.USER_CALENDAR_ENTRY_ID=A.USER_CALENDAR_ENTRY_ID
            JOIN %s O ON O.USER_CALENDAR_OCCURRENCE_ID=A.USER_CALENDAR_OCCURRENCE_ID
            JOIN %s D ON D.DAY_ID=O.DAY_ID
            JOIN %s R ON R.USER_CALENDAR_REMINDER_RULE_ID=A.REMINDER_RULE_ID
            WHERE E.OWNER_ISSUER=:issuer AND E.OWNER_SUBJECT_ID=:subject
              AND E.ENTRY_STATUS='ACTIVE' AND O.OCCURRENCE_STATUS='OPEN'
              AND A.ALERT_STATUS='AVAILABLE'
            ORDER BY A.AVAILABLE_AT_UTC DESC FETCH FIRST 100 ROWS ONLY
            """.formatted(t("ALERTS"),t("ENTRIES"),t("OCCURRENCES"),dayTable(),t("REMINDER_RULES")))
                .param("issuer",owner.issuer()).param("subject",owner.subjectId())
                .query((rs,n)->{
                    LocalDate due=LocalDate.parse(rs.getString("ISO_DATE_TEXT"));
                    int advance=(int)java.time.temporal.ChronoUnit.DAYS.between(today,due);
                    Integer minute=nullableInt(rs,9);
                    return new Alert(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),
                        due.toString(),rs.getString(6),rs.getString(7).startsWith("ADV:")?"ADVANCE":"DUE",
                        advance,minute==null?"09:00":String.format(java.util.Locale.ROOT,"%02d:%02d",minute/60,minute%60),rs.getString(8));
                }).list();
    }
    public int acknowledge(Owner owner,long alert) {
        return jdbc.sql("""
            UPDATE %s A SET ALERT_STATUS='ACKNOWLEDGED',ACKNOWLEDGED_AT_UTC=SYSTIMESTAMP,
                UPDATED_AT_UTC=SYSTIMESTAMP,VERSION_NO=VERSION_NO+1
            WHERE A.USER_CALENDAR_ALERT_ID=:id AND A.ALERT_STATUS='AVAILABLE'
              AND EXISTS (SELECT 1 FROM %s E WHERE E.USER_CALENDAR_ENTRY_ID=A.USER_CALENDAR_ENTRY_ID
               AND E.OWNER_ISSUER=:issuer AND E.OWNER_SUBJECT_ID=:subject AND E.ENTRY_STATUS='ACTIVE')
            """.formatted(t("ALERTS"),t("ENTRIES")))
                .param("id",alert).param("issuer",owner.issuer()).param("subject",owner.subjectId()).update();
    }
    public int snooze(Owner owner,long alert,int minutes) {
        return jdbc.sql("""
            UPDATE %s A SET ALERT_STATUS='SNOOZED',SNOOZED_UNTIL_UTC=SYSTIMESTAMP+NUMTODSINTERVAL(:mins,'MINUTE'),
               NEXT_ELIGIBLE_AT_UTC=SYSTIMESTAMP+NUMTODSINTERVAL(:mins,'MINUTE'),SNOOZE_COUNT=SNOOZE_COUNT+1,
               VERSION_NO=VERSION_NO+1,UPDATED_AT_UTC=SYSTIMESTAMP
            WHERE A.USER_CALENDAR_ALERT_ID=:id AND A.ALERT_STATUS='AVAILABLE'
              AND EXISTS (SELECT 1 FROM %s E WHERE E.USER_CALENDAR_ENTRY_ID=A.USER_CALENDAR_ENTRY_ID
               AND E.OWNER_ISSUER=:issuer AND E.OWNER_SUBJECT_ID=:subject AND E.ENTRY_STATUS='ACTIVE')
            """.formatted(t("ALERTS"),t("ENTRIES")))
                .param("mins",minutes).param("id",alert).param("issuer",owner.issuer())
                .param("subject",owner.subjectId()).update();
    }
    public int releaseEligibleAlerts() {
        // Missed old advances should not flood the inbox after an outage.
        jdbc.sql("""
            UPDATE %s SET ALERT_STATUS='EXPIRED',UPDATED_AT_UTC=SYSTIMESTAMP,VERSION_NO=VERSION_NO+1
            WHERE ALERT_STATUS IN ('SCHEDULED','SNOOZED') AND SCHEDULED_SLOT_KEY LIKE 'ADV:%%'
              AND PLANNED_AT_UTC < SYSTIMESTAMP - INTERVAL '1' DAY
            """.formatted(t("ALERTS"))).update();
        return jdbc.sql("""
            UPDATE %s A SET ALERT_STATUS='AVAILABLE',AVAILABLE_AT_UTC=SYSTIMESTAMP,
                 VERSION_NO=VERSION_NO+1,UPDATED_AT_UTC=SYSTIMESTAMP,LEASE_TOKEN=NULL,LEASE_UNTIL_UTC=NULL
            WHERE A.ALERT_STATUS IN ('SCHEDULED','SNOOZED') AND A.NEXT_ELIGIBLE_AT_UTC<=SYSTIMESTAMP
             AND EXISTS(SELECT 1 FROM %s E JOIN %s O ON O.USER_CALENDAR_ENTRY_ID=E.USER_CALENDAR_ENTRY_ID
               WHERE E.USER_CALENDAR_ENTRY_ID=A.USER_CALENDAR_ENTRY_ID
                 AND O.USER_CALENDAR_OCCURRENCE_ID=A.USER_CALENDAR_OCCURRENCE_ID
                 AND E.ENTRY_STATUS='ACTIVE' AND O.OCCURRENCE_STATUS='OPEN')
            """.formatted(t("ALERTS"),t("ENTRIES"),t("OCCURRENCES"))).update();
    }
    public long idempotencyExisting(Owner owner,String key,byte[] sha) {
        return jdbc.sql("""
            SELECT REQUEST_SHA256,REQUEST_STATUS,RESOURCE_ID FROM %s
            WHERE OWNER_ISSUER=:issuer AND OWNER_SUBJECT_ID=:subject
              AND OPERATION_CODE='CREATE_ENTRY' AND IDEMPOTENCY_KEY=:key
            """.formatted(t("REQUEST_KEYS"))).param("issuer",owner.issuer())
                .param("subject",owner.subjectId()).param("key",key)
                .query((rs,n)->{
                    if(!java.security.MessageDigest.isEqual(sha,rs.getBytes(1))) return -1L;
                    if(!"COMPLETED".equals(rs.getString(2))) return -1L;
                    return Long.parseLong(rs.getString(3));
                }).optional().orElse(0L);
    }
    public void claimIdempotency(Owner owner,String key,byte[] sha) {
        jdbc.sql("""
            INSERT INTO %s (USER_CALENDAR_REQUEST_KEY_ID,OWNER_ISSUER,OWNER_SUBJECT_ID,OPERATION_CODE,
              IDEMPOTENCY_KEY,REQUEST_SHA256,REQUEST_STATUS,CREATED_AT_UTC,EXPIRES_AT_UTC)
            VALUES (:id,:issuer,:subject,'CREATE_ENTRY',:key,:sha,'IN_PROGRESS',SYSTIMESTAMP,SYSTIMESTAMP+INTERVAL '7' DAY)
            """.formatted(t("REQUEST_KEYS"))).param("id",next("URK"))
                .param("issuer",owner.issuer()).param("subject",owner.subjectId()).param("key",key)
                .param("sha",sha).update();
    }
    public void finishIdempotency(Owner owner,String key,long entry) {
        jdbc.sql("""
            UPDATE %s SET REQUEST_STATUS='COMPLETED',RESOURCE_KIND='ENTRY',RESOURCE_ID=:entry,
               RESPONSE_STATUS=201,COMPLETED_AT_UTC=SYSTIMESTAMP
            WHERE OWNER_ISSUER=:issuer AND OWNER_SUBJECT_ID=:subject
              AND OPERATION_CODE='CREATE_ENTRY' AND IDEMPOTENCY_KEY=:key
            """.formatted(t("REQUEST_KEYS"))).param("issuer",owner.issuer()).param("subject",owner.subjectId())
                .param("key",key).param("entry",Long.toString(entry)).update();
    }

    public record RecurringEntry(long id,long ruleId,BigDecimal startDay,String zone,String frequency,
            int interval,BigDecimal variant,Integer targetDay,Integer targetMonth,String weekdayMask,String policy,
            Integer localMinute) {}
    public List<RecurringEntry> activeRecurrences() {
        return jdbc.sql("""
            SELECT E.USER_CALENDAR_ENTRY_ID,R.USER_CALENDAR_RECURRENCE_RULE_ID,E.START_DAY_ID,E.TIME_ZONE_ID,R.FREQUENCY,R.INTERVAL_NO,
               R.CALENDAR_VARIANT_ID,R.DAY_OF_MONTH,R.MONTH_OF_YEAR,R.BY_WEEKDAYS_MASK,R.DAY_RESOLUTION_POLICY,
               E.START_LOCAL_MINUTE
            FROM %s E JOIN %s R ON R.USER_CALENDAR_ENTRY_ID=E.USER_CALENDAR_ENTRY_ID
            WHERE E.ENTRY_STATUS='ACTIVE' AND R.RULE_STATUS='ACTIVE'
            ORDER BY E.USER_CALENDAR_ENTRY_ID
            """.formatted(t("ENTRIES"),t("RECURRENCE_RULES")))
                .query((rs,n)->new RecurringEntry(rs.getLong(1),rs.getLong(2),rs.getBigDecimal(3),rs.getString(4),rs.getString(5),
                        rs.getInt(6),rs.getBigDecimal(7),nullableInt(rs,8),nullableInt(rs,9),
                        rs.getString(10),rs.getString(11),nullableInt(rs,12))).list();
    }
    public long insertRecurrence(long entry,BigDecimal variant,String frequency,int interval,
                                 Integer dayOfMonth,Integer monthOfYear,String weekdays,String policy,String subject) {
        long recurrenceId=next("UCR");
        jdbc.sql("""
            INSERT INTO %s (USER_CALENDAR_RECURRENCE_RULE_ID,USER_CALENDAR_ENTRY_ID,CALENDAR_VARIANT_ID,
              RULE_VERSION_NO,FREQUENCY,INTERVAL_NO,DAY_OF_MONTH,MONTH_OF_YEAR,BY_WEEKDAYS_MASK,
              DAY_RESOLUTION_POLICY,RULE_STATUS,CREATED_AT_UTC,CREATED_BY_SUBJECT_ID)
            VALUES (:id,:entry,:variant,1,:freq,:interval,:dom,:month,:weekdays,:policy,'ACTIVE',SYSTIMESTAMP,:subject)
            """.formatted(t("RECURRENCE_RULES"))).param("id",recurrenceId)
                .param("entry",entry).param("variant",variant).param("freq",frequency)
                .param("interval",interval).param("dom",dayOfMonth).param("month",monthOfYear)
                .param("weekdays",weekdays).param("policy",policy).param("subject",subject).update();
        return recurrenceId;
    }
    public record VariantDay(BigDecimal dayId,LocalDate date,int year,int month,int day) {}
    public Optional<VariantDay> variantDate(BigDecimal variant,BigDecimal day) {
        return jdbc.sql("""
            SELECT C.DAY_ID,D.CANONICAL_DATE,C.YEAR_NO,C.MONTH_NO,C.DAY_NO
            FROM %s.CALENDAR_DATE C JOIN %s D ON D.DAY_ID=C.DAY_ID
            WHERE C.CALENDAR_VARIANT_ID=:variant AND C.DAY_ID=:day
            """.formatted(schema,dayTable())).param("variant",variant).param("day",day)
                .query((rs,n)->mapVariant(rs)).optional();
    }
    public List<VariantDay> variantDays(BigDecimal variant,LocalDate from,LocalDate to) {
        return jdbc.sql("""
            SELECT C.DAY_ID,D.CANONICAL_DATE,C.YEAR_NO,C.MONTH_NO,C.DAY_NO
            FROM %s.CALENDAR_DATE C JOIN %s D ON D.DAY_ID=C.DAY_ID
            WHERE C.CALENDAR_VARIANT_ID=:variant AND D.CANONICAL_DATE >= :fromDate
              AND D.CANONICAL_DATE < :toDate ORDER BY D.CANONICAL_DATE
            """.formatted(schema,dayTable()))
                .param("variant",variant).param("fromDate",Date.valueOf(from))
                .param("toDate",Date.valueOf(to.plusDays(1)))
                .query((rs,n)->mapVariant(rs)).list();
    }
    private static VariantDay mapVariant(ResultSet rs)throws SQLException {
        return new VariantDay(rs.getBigDecimal(1),rs.getDate(2).toLocalDate(),rs.getInt(3),rs.getInt(4),rs.getInt(5));
    }
    public record Occurrence(long id,long entryId,BigDecimal dayId,LocalDate date,String zone,
                             Integer startMinute,String status) {}
    public List<Occurrence> occurrencesForEntry(long entryId) {
        return jdbc.sql("""
            SELECT O.USER_CALENDAR_OCCURRENCE_ID,O.USER_CALENDAR_ENTRY_ID,O.DAY_ID,D.CANONICAL_DATE,
              E.TIME_ZONE_ID,O.LOCAL_START_MINUTE,O.OCCURRENCE_STATUS
            FROM %s O JOIN %s E ON E.USER_CALENDAR_ENTRY_ID=O.USER_CALENDAR_ENTRY_ID
                JOIN %s D ON D.DAY_ID=O.DAY_ID
            WHERE O.USER_CALENDAR_ENTRY_ID=:entry AND O.OCCURRENCE_STATUS='OPEN'
            """.formatted(t("OCCURRENCES"),t("ENTRIES"),dayTable()))
                .param("entry",entryId)
                .query((rs,n)->new Occurrence(rs.getLong(1),rs.getLong(2),rs.getBigDecimal(3),
                     rs.getDate(4).toLocalDate(),rs.getString(5),nullableInt(rs,6),rs.getString(7))).list();
    }
    public boolean hasRecurrence(long entryId) {
        return jdbc.sql("SELECT COUNT(*) FROM " + t("RECURRENCE_RULES") +
                " WHERE USER_CALENDAR_ENTRY_ID=:entry AND RULE_STATUS='ACTIVE'")
                .param("entry",entryId).query(Integer.class).single()>0;
    }
    public boolean hasAcknowledgedAlerts(long entryId) {
        return jdbc.sql("SELECT COUNT(*) FROM " + t("ALERTS") +
                " WHERE USER_CALENDAR_ENTRY_ID=:entry AND ALERT_STATUS IN ('ACKNOWLEDGED','DISMISSED')")
                .param("entry",entryId).query(Integer.class).single()>0;
    }

}
