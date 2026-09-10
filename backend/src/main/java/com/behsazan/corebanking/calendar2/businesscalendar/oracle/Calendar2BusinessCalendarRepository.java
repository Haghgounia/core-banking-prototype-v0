package com.behsazan.corebanking.calendar2.businesscalendar.oracle;

import com.behsazan.corebanking.calendar2.reference.oracle.Calendar2SqlNames;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class Calendar2BusinessCalendarRepository {
    private final JdbcClient jdbcClient;
    private final String schema;

    public Calendar2BusinessCalendarRepository(JdbcClient jdbcClient,
                                               @Value("${core-banking.schemas.calendar2:CAL2}") String schema) {
        this.jdbcClient = jdbcClient;
        this.schema = schema.trim().toUpperCase();
    }

    public boolean businessCalendarExists(long businessCalendarId) {
        Long count = jdbcClient.sql("SELECT COUNT(*) FROM " + table("BUSINESS_CALENDAR")
                        + " WHERE BUSINESS_CALENDAR_ID = :id AND ACTIVE_FLAG = 'Y'")
                .param("id", businessCalendarId).query(Long.class).single();
        return count != null && count > 0;
    }

    public LocalDate[] resolveRange(long businessCalendarId, LocalDate requestedFrom, LocalDate requestedTo) {
        String sql = "SELECT BC.VALID_FROM, BC.VALID_TO, "
                + "(SELECT MIN(X.RANGE_START) FROM ("
                + " SELECT S.EFFECTIVE_FROM RANGE_START FROM " + table("BUSINESS_CALENDAR_SCHEDULE") + " S"
                + " WHERE S.BUSINESS_CALENDAR_ID = :id AND S.ACTIVE_FLAG='Y' AND S.STATUS='ACTIVE'"
                + " UNION ALL SELECT E.EXCEPTION_DATE FROM " + table("BUSINESS_CALENDAR_EXCEPTION") + " E"
                + " WHERE E.BUSINESS_CALENDAR_ID = :id AND E.ACTIVE_FLAG='Y') X) POLICY_FROM, "
                + "(SELECT MAX(X.RANGE_END) FROM ("
                + " SELECT S.EFFECTIVE_TO RANGE_END FROM " + table("BUSINESS_CALENDAR_SCHEDULE") + " S"
                + " WHERE S.BUSINESS_CALENDAR_ID = :id AND S.ACTIVE_FLAG='Y' AND S.STATUS='ACTIVE'"
                + " UNION ALL SELECT E.EXCEPTION_DATE FROM " + table("BUSINESS_CALENDAR_EXCEPTION") + " E"
                + " WHERE E.BUSINESS_CALENDAR_ID = :id AND E.ACTIVE_FLAG='Y') X) POLICY_TO "
                + "FROM " + table("BUSINESS_CALENDAR") + " BC WHERE BC.BUSINESS_CALENDAR_ID=:id";
        LocalDate[] data = jdbcClient.sql(sql).param("id", businessCalendarId).query((rs, rowNum) -> new LocalDate[]{
                rs.getDate("VALID_FROM") == null ? null : rs.getDate("VALID_FROM").toLocalDate(),
                rs.getDate("VALID_TO") == null ? null : rs.getDate("VALID_TO").toLocalDate(),
                rs.getDate("POLICY_FROM") == null ? null : rs.getDate("POLICY_FROM").toLocalDate(),
                rs.getDate("POLICY_TO") == null ? null : rs.getDate("POLICY_TO").toLocalDate()
        }).single();
        LocalDate from = requestedFrom != null ? requestedFrom : data[2];
        LocalDate to = requestedTo != null ? requestedTo : data[3];
        if (from != null && data[0] != null && from.isBefore(data[0])) from = data[0];
        if (to != null && data[1] != null && to.isAfter(data[1])) to = data[1];
        return new LocalDate[]{from, to};
    }

    public long incompleteActiveScheduleDays(long businessCalendarId, LocalDate from, LocalDate to) {
        String sql = "SELECT COUNT(*) FROM " + table("BUSINESS_CALENDAR_SCHEDULE") + " S "
                + "CROSS JOIN " + table("WEEKDAY") + " W "
                + "LEFT JOIN " + table("BUSINESS_CALENDAR_SCHEDULE_DAY") + " D "
                + "ON D.BUSINESS_CALENDAR_SCHEDULE_ID=S.BUSINESS_CALENDAR_SCHEDULE_ID "
                + "AND D.WEEKDAY_ID=W.WEEKDAY_ID AND D.ACTIVE_FLAG='Y' "
                + "WHERE S.BUSINESS_CALENDAR_ID=:id AND S.ACTIVE_FLAG='Y' AND S.STATUS='ACTIVE' "
                + "AND S.EFFECTIVE_FROM <= :toDate AND S.EFFECTIVE_TO >= :fromDate "
                + "AND (D.BUSINESS_CALENDAR_SCHEDULE_DAY_ID IS NULL "
                + "OR (D.DAY_STATUS IN ('OPEN','PARTIAL') AND (D.STAFF_START_TIME IS NULL OR D.STAFF_END_TIME IS NULL "
                + "OR D.CUSTOMER_OPEN_TIME IS NULL OR D.CUSTOMER_CLOSE_TIME IS NULL)))";
        Long count = jdbcClient.sql(sql)
                .param("id", businessCalendarId)
                .param("fromDate", Date.valueOf(from))
                .param("toDate", Date.valueOf(to))
                .query(Long.class).single();
        return count == null ? 0 : count;
    }

    public int rebuild(long businessCalendarId, LocalDate from, LocalDate to) {
        String target = table("BUSINESS_CALENDAR_DAY");
        jdbcClient.sql("LOCK TABLE " + target + " IN SHARE ROW EXCLUSIVE MODE").update();
        long baseId = jdbcClient.sql("SELECT NVL(MAX(BUSINESS_CALENDAR_DAY_ID),0) FROM " + target)
                .query(Long.class).single();

        String sql = """
                MERGE INTO %s T
                USING (
                  WITH DAYS AS (
                    SELECT D.DAY_ID, D.CANONICAL_DATE, D.WEEKDAY_ID
                      FROM %s D
                     WHERE D.CANONICAL_DATE BETWEEN :fromDate AND :toDate
                  ),
                  SCHEDULE_CANDIDATE AS (
                    SELECT D.DAY_ID, D.CANONICAL_DATE,
                           S.BUSINESS_CALENDAR_SCHEDULE_ID AS SCHEDULE_ID,
                           S.SOURCE_ID,
                           SD.DAY_STATUS, SD.STAFF_START_TIME, SD.STAFF_END_TIME,
                           SD.CUSTOMER_OPEN_TIME, SD.CUSTOMER_CLOSE_TIME,
                           SD.IS_BUSINESS_DAY, SD.IS_SETTLEMENT_DAY, SD.IS_CLEARING_DAY, SD.IS_PROCESSING_DAY,
                           ROW_NUMBER() OVER (
                             PARTITION BY D.DAY_ID
                             ORDER BY S.PRIORITY_NO DESC, S.EFFECTIVE_FROM DESC, S.BUSINESS_CALENDAR_SCHEDULE_ID DESC
                           ) AS RN
                      FROM DAYS D
                      JOIN %s S
                        ON S.BUSINESS_CALENDAR_ID = :businessCalendarId
                       AND S.ACTIVE_FLAG = 'Y'
                       AND S.STATUS = 'ACTIVE'
                       AND D.CANONICAL_DATE BETWEEN S.EFFECTIVE_FROM AND S.EFFECTIVE_TO
                      JOIN %s SD
                        ON SD.BUSINESS_CALENDAR_SCHEDULE_ID = S.BUSINESS_CALENDAR_SCHEDULE_ID
                       AND SD.WEEKDAY_ID = D.WEEKDAY_ID
                       AND SD.ACTIVE_FLAG = 'Y'
                  ),
                  BASE_RULE AS (
                    SELECT * FROM SCHEDULE_CANDIDATE WHERE RN = 1
                  ),
                  EXCEPTION_RULE AS (
                    SELECT E.BUSINESS_CALENDAR_EXCEPTION_ID AS EXCEPTION_ID,
                           E.EXCEPTION_DATE, E.EXCEPTION_TYPE, E.DAY_STATUS,
                           E.STAFF_START_TIME, E.STAFF_END_TIME, E.CUSTOMER_OPEN_TIME, E.CUSTOMER_CLOSE_TIME,
                           E.IS_BUSINESS_DAY, E.IS_SETTLEMENT_DAY, E.IS_CLEARING_DAY, E.IS_PROCESSING_DAY,
                           E.REASON_CODE, E.SOURCE_ID
                      FROM %s E
                     WHERE E.BUSINESS_CALENDAR_ID = :businessCalendarId
                       AND E.ACTIVE_FLAG = 'Y'
                       AND E.EXCEPTION_DATE BETWEEN :fromDate AND :toDate
                  ),
                  HOLIDAY_RULE AS (
                    SELECT EO.DAY_ID,
                           MAX(EO.SOURCE_ID) KEEP (DENSE_RANK LAST ORDER BY EO.EVENT_OCCURRENCE_ID) AS SOURCE_ID
                      FROM %s EO
                      JOIN DAYS D ON D.DAY_ID = EO.DAY_ID
                     WHERE EO.HOLIDAY_FLAG = 'Y'
                     GROUP BY EO.DAY_ID
                  ),
                  DECISION_INPUT AS (
                    SELECT D.DAY_ID, D.CANONICAL_DATE,
                           B.SCHEDULE_ID, B.SOURCE_ID AS SCHEDULE_SOURCE_ID,
                           B.DAY_STATUS AS BASE_STATUS,
                           B.STAFF_START_TIME AS BASE_STAFF_START, B.STAFF_END_TIME AS BASE_STAFF_END,
                           B.CUSTOMER_OPEN_TIME AS BASE_CUSTOMER_OPEN, B.CUSTOMER_CLOSE_TIME AS BASE_CUSTOMER_CLOSE,
                           B.IS_BUSINESS_DAY AS BASE_BUSINESS, B.IS_SETTLEMENT_DAY AS BASE_SETTLEMENT,
                           B.IS_CLEARING_DAY AS BASE_CLEARING, B.IS_PROCESSING_DAY AS BASE_PROCESSING,
                           E.EXCEPTION_ID, E.EXCEPTION_TYPE, E.DAY_STATUS AS EXCEPTION_STATUS,
                           E.STAFF_START_TIME AS EX_STAFF_START, E.STAFF_END_TIME AS EX_STAFF_END,
                           E.CUSTOMER_OPEN_TIME AS EX_CUSTOMER_OPEN, E.CUSTOMER_CLOSE_TIME AS EX_CUSTOMER_CLOSE,
                           E.IS_BUSINESS_DAY AS EX_BUSINESS, E.IS_SETTLEMENT_DAY AS EX_SETTLEMENT,
                           E.IS_CLEARING_DAY AS EX_CLEARING, E.IS_PROCESSING_DAY AS EX_PROCESSING,
                           E.REASON_CODE AS EX_REASON_CODE, E.SOURCE_ID AS EX_SOURCE_ID,
                           H.DAY_ID AS HOLIDAY_DAY_ID, H.SOURCE_ID AS HOLIDAY_SOURCE_ID
                      FROM DAYS D
                      LEFT JOIN BASE_RULE B ON B.DAY_ID = D.DAY_ID
                      LEFT JOIN EXCEPTION_RULE E ON E.EXCEPTION_DATE = D.CANONICAL_DATE
                      LEFT JOIN HOLIDAY_RULE H ON H.DAY_ID = D.DAY_ID
                  ),
                  DECIDED AS (
                    SELECT X.*,
                           CASE
                             WHEN X.EXCEPTION_ID IS NOT NULL AND X.EXCEPTION_STATUS IS NOT NULL THEN X.EXCEPTION_STATUS
                             WHEN X.HOLIDAY_DAY_ID IS NOT NULL THEN 'CLOSED'
                             WHEN X.SCHEDULE_ID IS NOT NULL THEN X.BASE_STATUS
                             ELSE 'UNCLASSIFIED'
                           END AS FINAL_STATUS,
                           CASE
                             WHEN X.EXCEPTION_ID IS NOT NULL THEN 'EXCEPTION'
                             WHEN X.HOLIDAY_DAY_ID IS NOT NULL THEN 'HOLIDAY'
                             WHEN X.SCHEDULE_ID IS NOT NULL THEN 'SCHEDULE'
                             ELSE 'DEFAULT'
                           END AS RESOLUTION_SOURCE
                      FROM DECISION_INPUT X
                  ),
                  RESOLVED AS (
                    SELECT D.DAY_ID, D.CANONICAL_DATE, D.SCHEDULE_ID, D.EXCEPTION_ID, D.RESOLUTION_SOURCE, D.FINAL_STATUS,
                           CASE WHEN D.FINAL_STATUS = 'CLOSED' THEN NULL
                                WHEN COALESCE(D.EX_STAFF_START, D.BASE_STAFF_START) IS NULL THEN NULL
                                ELSE CAST(TRUNC(D.CANONICAL_DATE) AS TIMESTAMP) + NUMTODSINTERVAL(TO_NUMBER(SUBSTR(COALESCE(D.EX_STAFF_START, D.BASE_STAFF_START),1,2))*60 + TO_NUMBER(SUBSTR(COALESCE(D.EX_STAFF_START, D.BASE_STAFF_START),4,2)), 'MINUTE') END AS STAFF_START_TIME,
                           CASE WHEN D.FINAL_STATUS = 'CLOSED' THEN NULL
                                WHEN COALESCE(D.EX_STAFF_END, D.BASE_STAFF_END) IS NULL THEN NULL
                                ELSE CAST(TRUNC(D.CANONICAL_DATE) AS TIMESTAMP) + NUMTODSINTERVAL(TO_NUMBER(SUBSTR(COALESCE(D.EX_STAFF_END, D.BASE_STAFF_END),1,2))*60 + TO_NUMBER(SUBSTR(COALESCE(D.EX_STAFF_END, D.BASE_STAFF_END),4,2)), 'MINUTE') END AS STAFF_END_TIME,
                           CASE WHEN D.FINAL_STATUS = 'CLOSED' THEN NULL
                                WHEN COALESCE(D.EX_CUSTOMER_OPEN, D.BASE_CUSTOMER_OPEN) IS NULL THEN NULL
                                ELSE CAST(TRUNC(D.CANONICAL_DATE) AS TIMESTAMP) + NUMTODSINTERVAL(TO_NUMBER(SUBSTR(COALESCE(D.EX_CUSTOMER_OPEN, D.BASE_CUSTOMER_OPEN),1,2))*60 + TO_NUMBER(SUBSTR(COALESCE(D.EX_CUSTOMER_OPEN, D.BASE_CUSTOMER_OPEN),4,2)), 'MINUTE') END AS OPEN_TIME,
                           CASE WHEN D.FINAL_STATUS = 'CLOSED' THEN NULL
                                WHEN COALESCE(D.EX_CUSTOMER_CLOSE, D.BASE_CUSTOMER_CLOSE) IS NULL THEN NULL
                                ELSE CAST(TRUNC(D.CANONICAL_DATE) AS TIMESTAMP) + NUMTODSINTERVAL(TO_NUMBER(SUBSTR(COALESCE(D.EX_CUSTOMER_CLOSE, D.BASE_CUSTOMER_CLOSE),1,2))*60 + TO_NUMBER(SUBSTR(COALESCE(D.EX_CUSTOMER_CLOSE, D.BASE_CUSTOMER_CLOSE),4,2)), 'MINUTE') END AS CLOSE_TIME,
                           CASE WHEN D.FINAL_STATUS = 'CLOSED' THEN 'N' ELSE COALESCE(D.EX_BUSINESS, D.BASE_BUSINESS, 'N') END AS IS_BUSINESS_DAY,
                           CASE WHEN D.FINAL_STATUS = 'CLOSED' THEN 'N' ELSE COALESCE(D.EX_SETTLEMENT, D.BASE_SETTLEMENT, 'N') END AS IS_SETTLEMENT_DAY,
                           CASE WHEN D.FINAL_STATUS = 'CLOSED' THEN 'N' ELSE COALESCE(D.EX_CLEARING, D.BASE_CLEARING, 'N') END AS IS_CLEARING_DAY,
                           CASE WHEN D.FINAL_STATUS = 'CLOSED' THEN 'N' ELSE COALESCE(D.EX_PROCESSING, D.BASE_PROCESSING, 'N') END AS IS_PROCESSING_DAY,
                           CASE
                             WHEN D.EXCEPTION_ID IS NOT NULL THEN COALESCE(D.EX_REASON_CODE, D.EXCEPTION_TYPE)
                             WHEN D.HOLIDAY_DAY_ID IS NOT NULL THEN 'PUBLIC_HOLIDAY'
                             WHEN D.SCHEDULE_ID IS NOT NULL AND D.FINAL_STATUS = 'CLOSED' THEN 'WEEKEND'
                             WHEN D.SCHEDULE_ID IS NULL THEN 'PENDING_RULE_EVALUATION'
                             ELSE NULL
                           END AS REASON_CODE,
                           CASE
                             WHEN D.EXCEPTION_ID IS NOT NULL THEN COALESCE(D.EX_SOURCE_ID, D.HOLIDAY_SOURCE_ID, D.SCHEDULE_SOURCE_ID)
                             WHEN D.HOLIDAY_DAY_ID IS NOT NULL THEN COALESCE(D.HOLIDAY_SOURCE_ID, D.SCHEDULE_SOURCE_ID)
                             ELSE D.SCHEDULE_SOURCE_ID
                           END AS SOURCE_ID
                      FROM DECIDED D
                  )
                  SELECT :baseId + ROW_NUMBER() OVER (ORDER BY R.CANONICAL_DATE) AS NEW_ID,
                         R.DAY_ID, R.FINAL_STATUS, R.OPEN_TIME, R.CLOSE_TIME, R.STAFF_START_TIME, R.STAFF_END_TIME,
                         R.IS_BUSINESS_DAY, R.IS_SETTLEMENT_DAY, R.IS_CLEARING_DAY, R.IS_PROCESSING_DAY,
                         R.REASON_CODE, R.SOURCE_ID, R.RESOLUTION_SOURCE, R.SCHEDULE_ID, R.EXCEPTION_ID
                    FROM RESOLVED R
                ) R
                ON (T.BUSINESS_CALENDAR_ID = :businessCalendarId AND T.DAY_ID = R.DAY_ID)
                WHEN MATCHED THEN UPDATE SET
                  T.DAY_STATUS = R.FINAL_STATUS,
                  T.OPEN_TIME = R.OPEN_TIME,
                  T.CLOSE_TIME = R.CLOSE_TIME,
                  T.STAFF_START_TIME = R.STAFF_START_TIME,
                  T.STAFF_END_TIME = R.STAFF_END_TIME,
                  T.IS_BUSINESS_DAY = R.IS_BUSINESS_DAY,
                  T.IS_SETTLEMENT_DAY = R.IS_SETTLEMENT_DAY,
                  T.IS_CLEARING_DAY = R.IS_CLEARING_DAY,
                  T.IS_PROCESSING_DAY = R.IS_PROCESSING_DAY,
                  T.REASON_CODE = R.REASON_CODE,
                  T.SOURCE_ID = R.SOURCE_ID,
                  T.RESOLUTION_SOURCE = R.RESOLUTION_SOURCE,
                  T.SCHEDULE_ID = R.SCHEDULE_ID,
                  T.EXCEPTION_ID = R.EXCEPTION_ID,
                  T.RESOLVED_AT = SYSTIMESTAMP
                WHEN NOT MATCHED THEN INSERT (
                  BUSINESS_CALENDAR_DAY_ID, BUSINESS_CALENDAR_ID, DAY_ID, DAY_STATUS, OPEN_TIME, CLOSE_TIME,
                  STAFF_START_TIME, STAFF_END_TIME, IS_BUSINESS_DAY, IS_SETTLEMENT_DAY, IS_CLEARING_DAY, IS_PROCESSING_DAY,
                  REASON_CODE, SOURCE_ID, RESOLUTION_SOURCE, SCHEDULE_ID, EXCEPTION_ID, RESOLVED_AT
                ) VALUES (
                  R.NEW_ID, :businessCalendarId, R.DAY_ID, R.FINAL_STATUS, R.OPEN_TIME, R.CLOSE_TIME,
                  R.STAFF_START_TIME, R.STAFF_END_TIME, R.IS_BUSINESS_DAY, R.IS_SETTLEMENT_DAY, R.IS_CLEARING_DAY, R.IS_PROCESSING_DAY,
                  R.REASON_CODE, R.SOURCE_ID, R.RESOLUTION_SOURCE, R.SCHEDULE_ID, R.EXCEPTION_ID, SYSTIMESTAMP
                )
                """.formatted(
                target,
                table("CANONICAL_DAY"),
                table("BUSINESS_CALENDAR_SCHEDULE"),
                table("BUSINESS_CALENDAR_SCHEDULE_DAY"),
                table("BUSINESS_CALENDAR_EXCEPTION"),
                table("EVENT_OCCURRENCE")
        );

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("businessCalendarId", businessCalendarId);
        params.put("fromDate", Date.valueOf(from));
        params.put("toDate", Date.valueOf(to));
        params.put("baseId", baseId);
        return jdbcClient.sql(sql).params(params).update();
    }

    public long[] resolutionCounts(long businessCalendarId, LocalDate from, LocalDate to) {
        String sql = "SELECT COUNT(*) TOTAL_COUNT, "
                + "SUM(CASE WHEN B.RESOLUTION_SOURCE='SCHEDULE' THEN 1 ELSE 0 END) SCHEDULE_COUNT, "
                + "SUM(CASE WHEN B.RESOLUTION_SOURCE='HOLIDAY' THEN 1 ELSE 0 END) HOLIDAY_COUNT, "
                + "SUM(CASE WHEN B.RESOLUTION_SOURCE='EXCEPTION' THEN 1 ELSE 0 END) EXCEPTION_COUNT, "
                + "SUM(CASE WHEN B.RESOLUTION_SOURCE='DEFAULT' THEN 1 ELSE 0 END) DEFAULT_COUNT "
                + "FROM " + table("BUSINESS_CALENDAR_DAY") + " B JOIN " + table("CANONICAL_DAY") + " D ON D.DAY_ID=B.DAY_ID "
                + "WHERE B.BUSINESS_CALENDAR_ID=:id AND D.CANONICAL_DATE BETWEEN :fromDate AND :toDate";
        return jdbcClient.sql(sql)
                .param("id", businessCalendarId)
                .param("fromDate", Date.valueOf(from))
                .param("toDate", Date.valueOf(to))
                .query((rs, rowNum) -> new long[]{
                        rs.getLong("TOTAL_COUNT"), rs.getLong("SCHEDULE_COUNT"), rs.getLong("HOLIDAY_COUNT"),
                        rs.getLong("EXCEPTION_COUNT"), rs.getLong("DEFAULT_COUNT")
                }).single();
    }

    private String table(String tableName) {
        return Calendar2SqlNames.qualified(schema, tableName);
    }
}
