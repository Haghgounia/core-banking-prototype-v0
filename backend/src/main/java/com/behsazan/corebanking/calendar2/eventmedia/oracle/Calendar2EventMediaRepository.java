package com.behsazan.corebanking.calendar2.eventmedia.oracle;

import com.behsazan.corebanking.calendar2.eventmedia.domain.Calendar2EventMediaModels.EventMediaContent;
import com.behsazan.corebanking.calendar2.eventmedia.domain.Calendar2EventMediaModels.EventMediaMetadata;
import com.behsazan.corebanking.calendar2.eventmedia.domain.Calendar2EventMediaModels.TodayEventMedia;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Repository
public class Calendar2EventMediaRepository {
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Z][A-Z0-9_]{0,127}");

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;
    private final String schema;

    public Calendar2EventMediaRepository(
            JdbcClient jdbcClient,
            JdbcTemplate jdbcTemplate,
            @Value("${core-banking.schemas.calendar2:CAL2}") String schemaName
    ) {
        this.jdbcClient = jdbcClient;
        this.jdbcTemplate = jdbcTemplate;
        this.schema = safeIdentifier(schemaName == null ? null : schemaName.trim().toUpperCase());
    }

    public void lockActiveEvent(long eventId) {
        Long value = jdbcClient.sql("SELECT EVENT_ID FROM " + table("EVENT") + " WHERE EVENT_ID=:eventId AND ACTIVE_FLAG='Y' FOR UPDATE")
                .param("eventId", eventId)
                .query(Long.class)
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("رویداد فعال موردنظر یافت نشد."));
        if (value <= 0) throw new IllegalArgumentException("رویداد فعال موردنظر یافت نشد.");
    }

    public long nextId() {
        return jdbcClient.sql("SELECT " + sequence("SEQ_CAL2_EVENT_MEDIA") + ".NEXTVAL FROM DUAL")
                .query(Long.class).single();
    }

    public void deactivateActive(long eventId, String actor) {
        jdbcClient.sql("UPDATE " + table("EVENT_MEDIA") + " SET ACTIVE_FLAG='N', DEACTIVATED_AT=SYSTIMESTAMP, DEACTIVATED_BY=:actor WHERE EVENT_ID=:eventId AND ACTIVE_FLAG='Y'")
                .param("actor", actor)
                .param("eventId", eventId)
                .update();
    }

    public void insert(long id, long eventId, int displayYearNo, String displayCalendarCode,
                       String fileName, String mimeType, byte[] content, String captionFa,
                       String altTextFa, String actor) {
        String sql = "INSERT INTO " + table("EVENT_MEDIA") + " (EVENT_MEDIA_ID,EVENT_ID,DISPLAY_YEAR_NO,DISPLAY_CALENDAR_CODE,FILE_NAME,MIME_TYPE,FILE_SIZE_BYTES,IMAGE_CONTENT,CAPTION_FA,ALT_TEXT_FA,ACTIVE_FLAG,CREATED_AT,CREATED_BY) VALUES (?,?,?,?,?,?,?,?,?,?,'Y',SYSTIMESTAMP,?)";
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, id);
            ps.setLong(2, eventId);
            ps.setInt(3, displayYearNo);
            ps.setString(4, displayCalendarCode);
            ps.setString(5, fileName);
            ps.setString(6, mimeType);
            ps.setLong(7, content.length);
            ps.setBinaryStream(8, new ByteArrayInputStream(content), content.length);
            ps.setString(9, captionFa);
            ps.setString(10, altTextFa);
            ps.setString(11, actor);
            return ps;
        });
    }

    public Optional<EventMediaMetadata> findMetadata(long mediaId) {
        String sql = metadataSelect() + " WHERE M.EVENT_MEDIA_ID=:mediaId";
        return jdbcClient.sql(sql).param("mediaId", mediaId).query(this::mapMetadata).optional();
    }

    public List<EventMediaMetadata> history(long eventId) {
        String sql = metadataSelect() + " WHERE M.EVENT_ID=:eventId ORDER BY M.CREATED_AT DESC, M.EVENT_MEDIA_ID DESC";
        return jdbcClient.sql(sql).param("eventId", eventId).query(this::mapMetadata).list();
    }

    public List<TodayEventMedia> today() {
        String sql = """
                SELECT M.EVENT_MEDIA_ID, M.EVENT_ID, E.EVENT_CODE, E.NAME_FA AS EVENT_NAME,
                       ET.NAME_FA AS EVENT_TYPE_NAME, M.DISPLAY_YEAR_NO, M.DISPLAY_CALENDAR_CODE,
                       M.FILE_NAME, M.MIME_TYPE, M.FILE_SIZE_BYTES, M.CAPTION_FA, M.ALT_TEXT_FA
                  FROM %s M
                  JOIN %s E ON E.EVENT_ID=M.EVENT_ID
                  JOIN %s ET ON ET.EVENT_TYPE_ID=E.EVENT_TYPE_ID
                 WHERE M.ACTIVE_FLAG='Y'
                   AND E.ACTIVE_FLAG='Y'
                   AND EXISTS (
                       SELECT 1
                         FROM %s EO
                         JOIN %s D ON D.DAY_ID=EO.DAY_ID
                        WHERE EO.EVENT_ID=E.EVENT_ID
                          AND D.CANONICAL_DATE=TRUNC(SYSDATE)
                          AND NVL(EO.DATA_STATUS,'ACTIVE') <> 'CANCELLED'
                   )
                 ORDER BY CASE WHEN E.OFFICIAL_FLAG='Y' THEN 0 ELSE 1 END, E.NAME_FA, M.EVENT_MEDIA_ID DESC
                """.formatted(table("EVENT_MEDIA"), table("EVENT"), table("EVENT_TYPE"), table("EVENT_OCCURRENCE"), table("CANONICAL_DAY"));
        return jdbcClient.sql(sql).query((rs, rowNum) -> new TodayEventMedia(
                rs.getLong("EVENT_MEDIA_ID"), rs.getLong("EVENT_ID"), rs.getString("EVENT_CODE"),
                rs.getString("EVENT_NAME"), rs.getString("EVENT_TYPE_NAME"), rs.getInt("DISPLAY_YEAR_NO"),
                rs.getString("DISPLAY_CALENDAR_CODE"), rs.getString("FILE_NAME"), rs.getString("MIME_TYPE"),
                rs.getLong("FILE_SIZE_BYTES"), rs.getString("CAPTION_FA"), rs.getString("ALT_TEXT_FA")
        )).list();
    }

    public Optional<EventMediaContent> content(long mediaId) {
        String sql = "SELECT IMAGE_CONTENT,MIME_TYPE,FILE_NAME FROM " + table("EVENT_MEDIA") + " WHERE EVENT_MEDIA_ID=:mediaId";
        return jdbcClient.sql(sql).param("mediaId", mediaId).query((rs, rowNum) ->
                new EventMediaContent(rs.getBytes("IMAGE_CONTENT"), rs.getString("MIME_TYPE"), rs.getString("FILE_NAME"))).optional();
    }

    private String metadataSelect() {
        return "SELECT M.EVENT_MEDIA_ID,M.EVENT_ID,E.EVENT_CODE,E.NAME_FA AS EVENT_NAME,M.DISPLAY_YEAR_NO,M.DISPLAY_CALENDAR_CODE,M.FILE_NAME,M.MIME_TYPE,M.FILE_SIZE_BYTES,M.CAPTION_FA,M.ALT_TEXT_FA,M.ACTIVE_FLAG,M.CREATED_AT,M.CREATED_BY,M.DEACTIVATED_AT,M.DEACTIVATED_BY FROM "
                + table("EVENT_MEDIA") + " M JOIN " + table("EVENT") + " E ON E.EVENT_ID=M.EVENT_ID";
    }

    private EventMediaMetadata mapMetadata(ResultSet rs, int rowNum) throws SQLException {
        return new EventMediaMetadata(
                rs.getLong("EVENT_MEDIA_ID"), rs.getLong("EVENT_ID"), rs.getString("EVENT_CODE"), rs.getString("EVENT_NAME"),
                rs.getInt("DISPLAY_YEAR_NO"), rs.getString("DISPLAY_CALENDAR_CODE"), rs.getString("FILE_NAME"),
                rs.getString("MIME_TYPE"), rs.getLong("FILE_SIZE_BYTES"), rs.getString("CAPTION_FA"), rs.getString("ALT_TEXT_FA"),
                "Y".equalsIgnoreCase(rs.getString("ACTIVE_FLAG")), localDateTime(rs, "CREATED_AT"), rs.getString("CREATED_BY"),
                localDateTime(rs, "DEACTIVATED_AT"), rs.getString("DEACTIVATED_BY")
        );
    }

    private static LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private String table(String objectName) { return schema + "." + safeIdentifier(objectName); }
    private String sequence(String objectName) { return schema + "." + safeIdentifier(objectName); }

    private static String safeIdentifier(String value) {
        if (value == null || !SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Unsafe Oracle identifier: " + value);
        }
        return value;
    }
}
