package com.behsazan.corebanking.calendar2.reference.oracle;

import com.behsazan.corebanking.calendar2.reference.application.Calendar2ReferenceRegistry;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.CanonicalDayFilterMeta;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.CanonicalDaySummary;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.FieldDescriptor;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.FieldType;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.LookupOption;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.RecordResponse;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.TableDescriptor;
import com.behsazan.corebanking.shared.model.PageResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

@Repository
public class Calendar2ReferenceRepository {
    private final JdbcClient jdbcClient;
    private final Calendar2ReferenceRegistry registry;

    public Calendar2ReferenceRepository(JdbcClient jdbcClient, Calendar2ReferenceRegistry registry) {
        this.jdbcClient = jdbcClient;
        this.registry = registry;
    }

    public PageResponse<Map<String, Object>> search(TableDescriptor descriptor, String text, int page, int size,
                                                     String sortBy, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String where = searchWhere(descriptor, text);
        Map<String, Object> params = new LinkedHashMap<>();
        if (text != null && !text.isBlank() && !descriptor.searchableFields().isEmpty()) {
            params.put("searchText", "%" + text.trim().toUpperCase(Locale.ROOT) + "%");
        }

        long total = jdbcClient.sql("SELECT COUNT(*) FROM " + table(descriptor) + " T" + where)
                .params(params).query(Long.class).single();

        List<FieldDescriptor> selected = descriptor.gridFields();
        String sql = "SELECT " + selectList(selected, "T") + " FROM " + table(descriptor) + " T" + where
                + " ORDER BY " + orderBy(descriptor, sortBy) + " " + normalizedDirection(direction)
                + " OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY";
        params.put("offset", safePage * safeSize);
        params.put("pageSize", safeSize);

        List<Map<String, Object>> rows = jdbcClient.sql(sql).params(params).query((rs, rowNum) -> {
            Map<String, Object> row = mapFields(rs, selected);
            row.put("_key", keyFromValues(descriptor, row));
            return row;
        }).list();
        return new PageResponse<>(rows, total, safePage, safeSize);
    }

    public PageResponse<CanonicalDaySummary> searchCanonicalDays(String text, Integer solarYear, Integer solarCentury,
                                                                    int page, int size, String sortBy, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");

        if (text != null && !text.isBlank()) {
            where.append(" AND (UPPER(TO_CHAR(D.DAY_ID)) LIKE :searchText")
                    .append(" OR UPPER(TO_CHAR(D.EPOCH_DAY)) LIKE :searchText")
                    .append(" OR UPPER(TO_CHAR(D.CANONICAL_DATE,'YYYY-MM-DD')) LIKE :searchText")
                    .append(" OR UPPER(D.ISO_DATE_TEXT) LIKE :searchText")
                    .append(" OR UPPER(W.NAME_FA) LIKE :searchText")
                    .append(" OR UPPER(PM.NAME_FA) LIKE :searchText")
                    .append(" OR UPPER(TO_CHAR(PCD.YEAR_NO)) LIKE :searchText")
                    .append(" OR UPPER(TO_CHAR(PCD.MONTH_NO)) LIKE :searchText")
                    .append(" OR UPPER(TO_CHAR(PCD.DAY_NO)) LIKE :searchText)");
            params.put("searchText", "%" + text.trim().toUpperCase(Locale.ROOT) + "%");
        }
        if (solarYear != null) {
            where.append(" AND PCD.YEAR_NO = :solarYear");
            params.put("solarYear", solarYear);
        }
        if (solarCentury != null && solarCentury > 0) {
            int startYear = (solarCentury - 1) * 100 + 1;
            int endYear = solarCentury * 100;
            where.append(" AND PCD.YEAR_NO BETWEEN :centuryStartYear AND :centuryEndYear");
            params.put("centuryStartYear", startYear);
            params.put("centuryEndYear", endYear);
        }

        String cte = canonicalDayContextCte();
        String from = canonicalDayFrom();
        long total = jdbcClient.sql(cte + " SELECT COUNT(*) " + from + where)
                .params(params).query(Long.class).single();

        String sql = cte + """
                SELECT D.DAY_ID, D.EPOCH_DAY, D.CANONICAL_DATE, D.ISO_DATE_TEXT,
                       D.WEEKDAY_ID, W.NAME_FA AS WEEKDAY_NAME,
                       PCD.YEAR_NO AS SOLAR_YEAR, PCD.MONTH_NO AS SOLAR_MONTH_NO, PCD.DAY_NO AS SOLAR_DAY_NO,
                       PM.NAME_FA AS SOLAR_MONTH_NAME,
                       D.ISO_WEEK_NO, D.ISO_WEEK_YEAR
                """ + from + where + " ORDER BY " + canonicalDaySort(sortBy) + " " + normalizedDirection(direction)
                + " OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY";
        params.put("offset", safePage * safeSize);
        params.put("pageSize", safeSize);
        List<CanonicalDaySummary> rows = jdbcClient.sql(sql).params(params).query((rs, rowNum) -> new CanonicalDaySummary(
                rs.getLong("DAY_ID"), rs.getLong("EPOCH_DAY"), rs.getDate("CANONICAL_DATE").toLocalDate(),
                rs.getString("ISO_DATE_TEXT"), rs.getLong("WEEKDAY_ID"), rs.getString("WEEKDAY_NAME"),
                nullableInteger(rs, "SOLAR_YEAR"), nullableInteger(rs, "SOLAR_MONTH_NO"), nullableInteger(rs, "SOLAR_DAY_NO"),
                rs.getString("SOLAR_MONTH_NAME"), nullableInteger(rs, "ISO_WEEK_NO"), nullableInteger(rs, "ISO_WEEK_YEAR")
        )).list();
        return new PageResponse<>(rows, total, safePage, safeSize);
    }

    public CanonicalDayFilterMeta canonicalDayFilterMeta() {
        String sql = canonicalDayContextCte() + """
                SELECT MAX(CASE WHEN D.CANONICAL_DATE = TRUNC(SYSDATE) THEN PCD.YEAR_NO END) AS CURRENT_SOLAR_YEAR,
                       MIN(PCD.YEAR_NO) AS MIN_SOLAR_YEAR,
                       MAX(PCD.YEAR_NO) AS MAX_SOLAR_YEAR
                """ + canonicalDayFrom();
        return jdbcClient.sql(sql).query((rs, rowNum) -> new CanonicalDayFilterMeta(
                nullableInteger(rs, "CURRENT_SOLAR_YEAR"),
                nullableInteger(rs, "MIN_SOLAR_YEAR"),
                nullableInteger(rs, "MAX_SOLAR_YEAR")
        )).single();
    }

    public Optional<RecordResponse> find(TableDescriptor descriptor, String encodedKey) {
        ParsedKey key = parseKey(descriptor, encodedKey);
        String sql = "SELECT " + selectList(descriptor.fields(), "T") + " FROM " + table(descriptor) + " T WHERE " + key.where();
        return jdbcClient.sql(sql).paramSource(key.params()).query((rs, rowNum) -> {
            Map<String, Object> values = mapFields(rs, descriptor.fields());
            return new RecordResponse(keyFromValues(descriptor, values), values);
        }).optional();
    }

    public RecordResponse insert(TableDescriptor descriptor, Map<String, Object> values) {
        LinkedHashMap<String, Object> normalized = new LinkedHashMap<>(values);
        if (descriptor.autoNumericPrimaryKey()) {
            FieldDescriptor key = descriptor.keyFields().getFirst();
            jdbcClient.sql("LOCK TABLE " + table(descriptor) + " IN SHARE ROW EXCLUSIVE MODE").update();
            long nextId = jdbcClient.sql("SELECT NVL(MAX(" + Calendar2SqlNames.identifier(key.columnName()) + "), 0) + 1 FROM " + table(descriptor))
                    .query(Long.class).single();
            normalized.put(key.apiName(), nextId);
        }

        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();
        for (FieldDescriptor field : descriptor.fields()) {
            if (field.readOnly() && !(field.key() && descriptor.autoNumericPrimaryKey())) continue;
            Object value = normalized.get(field.apiName());
            if (value == null && field.defaultValue() != null) value = field.defaultValue();
            columns.add(Calendar2SqlNames.identifier(field.columnName()));
            placeholders.add(":" + field.apiName());
            params.addValue(field.apiName(), databaseValue(field, value), sqlType(field));
        }
        jdbcClient.sql("INSERT INTO " + table(descriptor) + " (" + String.join(", ", columns) + ") VALUES ("
                        + String.join(", ", placeholders) + ")")
                .paramSource(params).update();
        return find(descriptor, keyFromValues(descriptor, normalized)).orElseThrow();
    }

    public Optional<RecordResponse> update(TableDescriptor descriptor, String encodedKey, Map<String, Object> values) {
        ParsedKey key = parseKey(descriptor, encodedKey);
        List<String> assignments = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();
        for (FieldDescriptor field : descriptor.fields()) {
            if (field.readOnly() || field.key()) continue;
            assignments.add(Calendar2SqlNames.identifier(field.columnName()) + " = :" + field.apiName());
            Object value = values.get(field.apiName());
            if (value == null && field.defaultValue() != null) value = field.defaultValue();
            params.addValue(field.apiName(), databaseValue(field, value), sqlType(field));
        }
        key.params().getValues().forEach(params::addValue);
        if (assignments.isEmpty()) return find(descriptor, encodedKey);
        int changed = jdbcClient.sql("UPDATE " + table(descriptor) + " SET " + String.join(", ", assignments) + " WHERE " + key.where())
                .paramSource(params).update();
        return changed == 1 ? find(descriptor, encodedKey) : Optional.empty();
    }

    public boolean delete(TableDescriptor descriptor, String encodedKey) {
        ParsedKey key = parseKey(descriptor, encodedKey);
        return jdbcClient.sql("DELETE FROM " + table(descriptor) + " WHERE " + key.where())
                .paramSource(key.params()).update() == 1;
    }

    public List<LookupOption> lookup(String resource, String text, int limit) {
        if ("calendar-variants".equals(resource)) return calendarVariantLookup(text, limit);
        TableDescriptor descriptor = registry.require(resource);
        if (descriptor.keyFields().size() != 1) return List.of();
        FieldDescriptor valueField = descriptor.keyFields().getFirst();
        FieldDescriptor codeField = descriptor.field(descriptor.lookupCodeApiName());
        FieldDescriptor nameField = descriptor.field(descriptor.lookupNameApiName());
        int safeLimit = Math.min(Math.max(limit, 1), 500);
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();
        if (text != null && !text.isBlank()) {
            String codeExpr = textExpression(codeField, "T." + Calendar2SqlNames.identifier(codeField.columnName()));
            String nameExpr = textExpression(nameField, "T." + Calendar2SqlNames.identifier(nameField.columnName()));
            where.add("(UPPER(" + codeExpr + ") LIKE :text OR UPPER(" + nameExpr + ") LIKE :text)");
            params.put("text", "%" + text.trim().toUpperCase(Locale.ROOT) + "%");
        }
        descriptor.fields().stream().filter(f -> f.apiName().equals("activeFlag")).findFirst()
                .ifPresent(field -> where.add("T." + Calendar2SqlNames.identifier(field.columnName()) + " = 'Y'"));
        params.put("limit", safeLimit);
        String codeSelect = textExpression(codeField, "T." + Calendar2SqlNames.identifier(codeField.columnName()));
        String nameSelect = textExpression(nameField, "T." + Calendar2SqlNames.identifier(nameField.columnName()));
        String sql = "SELECT T." + Calendar2SqlNames.identifier(valueField.columnName()) + " AS VALUE_COL, "
                + codeSelect + " AS CODE_COL, " + nameSelect + " AS LABEL_COL FROM " + table(descriptor) + " T"
                + (where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where))
                + " ORDER BY " + nameSelect + " FETCH FIRST :limit ROWS ONLY";
        return jdbcClient.sql(sql).params(params).query((rs, rowNum) -> new LookupOption(
                readKeyValue(rs, "VALUE_COL", valueField), rs.getString("CODE_COL"), rs.getString("LABEL_COL")
        )).list();
    }

    private List<LookupOption> calendarVariantLookup(String text, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 500);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("limit", safeLimit);
        String where = " WHERE V.ACTIVE_FLAG = 'Y'";
        if (text != null && !text.isBlank()) {
            where += " AND (UPPER(V.VARIANT_CODE) LIKE :text OR UPPER(S.NAME_FA) LIKE :text OR UPPER(S.CALENDAR_CODE) LIKE :text)";
            params.put("text", "%" + text.trim().toUpperCase(Locale.ROOT) + "%");
        }
        String sql = "SELECT V.CALENDAR_VARIANT_ID AS VALUE_COL, V.VARIANT_CODE AS CODE_COL, "
                + "S.NAME_FA AS LABEL_COL FROM " + Calendar2SqlNames.qualified(registry.require("calendar-variants").schemaName(), "CALENDAR_VARIANT") + " V"
                + " JOIN " + Calendar2SqlNames.qualified(registry.require("calendar-systems").schemaName(), "CALENDAR_SYSTEM") + " S"
                + " ON S.CALENDAR_SYSTEM_ID = V.CALENDAR_SYSTEM_ID"
                + where + " ORDER BY S.NAME_FA, V.VARIANT_CODE FETCH FIRST :limit ROWS ONLY";
        return jdbcClient.sql(sql).params(params).query((rs, rowNum) -> new LookupOption(
                rs.getBigDecimal("VALUE_COL"), rs.getString("CODE_COL"), rs.getString("LABEL_COL")
        )).list();
    }

    private String canonicalDayContextCte() {
        return """
                WITH PERSIAN_CONTEXT AS (
                    SELECT MAX(CASE WHEN S.CALENDAR_CODE = 'PERSIAN' AND V.IS_DEFAULT = 'Y' THEN V.CALENDAR_VARIANT_ID END) AS PERSIAN_VARIANT_ID,
                           MAX(CASE WHEN S.CALENDAR_CODE = 'PERSIAN' THEN S.CALENDAR_SYSTEM_ID END) AS PERSIAN_SYSTEM_ID
                      FROM %s V
                      JOIN %s S ON S.CALENDAR_SYSTEM_ID = V.CALENDAR_SYSTEM_ID
                )
                """.formatted(cal2Table("CALENDAR_VARIANT"), cal2Table("CALENDAR_SYSTEM"));
    }

    private String canonicalDayFrom() {
        return " FROM " + cal2Table("CANONICAL_DAY") + " D"
                + " CROSS JOIN PERSIAN_CONTEXT PX"
                + " JOIN " + cal2Table("CALENDAR_DATE") + " PCD ON PCD.DAY_ID = D.DAY_ID AND PCD.CALENDAR_VARIANT_ID = PX.PERSIAN_VARIANT_ID"
                + " LEFT JOIN " + cal2Table("WEEKDAY") + " W ON W.WEEKDAY_ID = D.WEEKDAY_ID"
                + " LEFT JOIN " + cal2Table("CALENDAR_MONTH") + " PM ON PM.CALENDAR_SYSTEM_ID = PX.PERSIAN_SYSTEM_ID AND PM.MONTH_NO = PCD.MONTH_NO";
    }

    private static String canonicalDaySort(String requested) {
        if (requested == null || requested.isBlank()) return "D.CANONICAL_DATE";
        return switch (requested) {
            case "dayId" -> "D.DAY_ID";
            case "epochDay" -> "D.EPOCH_DAY";
            case "canonicalDate", "isoDateText" -> "D.CANONICAL_DATE";
            case "weekdayName" -> "W.IR_DISPLAY_ORDER";
            case "solarMonthName" -> "PCD.MONTH_NO";
            case "isoWeekNo" -> "D.ISO_WEEK_NO";
            case "isoWeekYear" -> "D.ISO_WEEK_YEAR";
            default -> "D.CANONICAL_DATE";
        };
    }

    private String cal2Table(String tableName) {
        return Calendar2SqlNames.qualified(registry.schemaName(), tableName);
    }

    private String searchWhere(TableDescriptor descriptor, String text) {
        if (text == null || text.isBlank() || descriptor.searchableFields().isEmpty()) return "";
        StringJoiner joiner = new StringJoiner(" OR ", " WHERE (", ")");
        for (FieldDescriptor field : descriptor.searchableFields()) {
            String col = "T." + Calendar2SqlNames.identifier(field.columnName());
            joiner.add("UPPER(" + textExpression(field, col) + ") LIKE :searchText");
        }
        return joiner.toString();
    }

    private String orderBy(TableDescriptor descriptor, String requested) {
        if (requested != null && !requested.isBlank()) {
            Optional<FieldDescriptor> field = descriptor.gridFields().stream().filter(item -> item.apiName().equals(requested)).findFirst();
            if (field.isPresent()) return "T." + Calendar2SqlNames.identifier(field.get().columnName());
        }
        return "T." + Calendar2SqlNames.identifier(descriptor.keyFields().getFirst().columnName());
    }

    private static String textExpression(FieldDescriptor field, String qualifiedColumn) {
        return switch (field.type()) {
            case DATE -> "TO_CHAR(" + qualifiedColumn + ",'YYYY-MM-DD')";
            case TIMESTAMP -> "TO_CHAR(" + qualifiedColumn + ",'YYYY-MM-DD HH24:MI:SS.FF6')";
            case NUMBER -> "TO_CHAR(" + qualifiedColumn + ")";
            case LOOKUP -> isNumericLookup(field) ? "TO_CHAR(" + qualifiedColumn + ")" : qualifiedColumn;
            default -> qualifiedColumn;
        };
    }

    private static String normalizedDirection(String direction) { return "DESC".equalsIgnoreCase(direction) ? "DESC" : "ASC"; }
    private static String table(TableDescriptor descriptor) { return Calendar2SqlNames.qualified(descriptor.schemaName(), descriptor.tableName()); }
    private static String selectList(List<FieldDescriptor> fields, String alias) {
        return fields.stream().map(field -> alias + "." + Calendar2SqlNames.identifier(field.columnName()) + " AS \"" + field.apiName() + "\"")
                .reduce((left, right) -> left + ", " + right).orElseThrow();
    }

    private static Map<String, Object> mapFields(ResultSet rs, List<FieldDescriptor> fields) throws SQLException {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        for (FieldDescriptor field : fields) values.put(field.apiName(), readValue(rs, field));
        return values;
    }

    private static Object readValue(ResultSet rs, FieldDescriptor field) throws SQLException {
        return switch (field.type()) {
            case BOOLEAN -> {
                String value = rs.getString(field.apiName());
                yield value == null ? null : "Y".equalsIgnoreCase(value);
            }
            case NUMBER -> rs.getBigDecimal(field.apiName());
            case DATE -> {
                Date date = rs.getDate(field.apiName());
                yield date == null ? null : date.toLocalDate();
            }
            case TIMESTAMP -> {
                Timestamp ts = rs.getTimestamp(field.apiName());
                yield ts == null ? null : ts.toLocalDateTime();
            }
            case LOOKUP -> isNumericLookup(field) ? rs.getBigDecimal(field.apiName()) : rs.getString(field.apiName());
            case TEXT, SELECT -> rs.getString(field.apiName());
        };
    }

    private static Integer nullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static Object readKeyValue(ResultSet rs, String columnAlias, FieldDescriptor field) throws SQLException {
        if (field.type() == FieldType.NUMBER || isNumericLookup(field)) return rs.getBigDecimal(columnAlias);
        return rs.getString(columnAlias);
    }

    private static int sqlType(FieldDescriptor field) {
        return switch (field.type()) {
            case NUMBER -> Types.NUMERIC;
            case DATE -> Types.DATE;
            case TIMESTAMP -> Types.TIMESTAMP;
            case BOOLEAN, TEXT, SELECT -> Types.VARCHAR;
            case LOOKUP -> isNumericLookup(field) ? Types.NUMERIC : Types.VARCHAR;
        };
    }

    private static Object databaseValue(FieldDescriptor field, Object value) {
        if (value == null || (value instanceof String s && s.isBlank())) return null;
        return switch (field.type()) {
            case BOOLEAN -> (value instanceof Boolean b ? b : Boolean.parseBoolean(value.toString())) ? "Y" : "N";
            case NUMBER -> value instanceof BigDecimal ? value : new BigDecimal(value.toString());
            case DATE -> value instanceof LocalDate d ? Date.valueOf(d) : Date.valueOf(value.toString());
            case TIMESTAMP -> {
                if (value instanceof LocalDateTime dt) yield Timestamp.valueOf(dt);
                yield Timestamp.valueOf(value.toString().replace('T', ' '));
            }
            case LOOKUP -> isNumericLookup(field)
                    ? (value instanceof BigDecimal ? value : new BigDecimal(value.toString())) : value.toString();
            case TEXT, SELECT -> value.toString();
        };
    }

    private static boolean isNumericLookup(FieldDescriptor field) {
        return field.type() == FieldType.LOOKUP && field.columnName().endsWith("_ID");
    }

    private static String keyFromValues(TableDescriptor descriptor, Map<String, Object> values) {
        return descriptor.keyFields().stream().map(field -> {
            Object value = values.get(field.apiName());
            if (value instanceof BigDecimal bd) return bd.stripTrailingZeros().toPlainString();
            return value == null ? "" : value.toString();
        }).reduce((left, right) -> left + "~" + right).orElseThrow();
    }

    private static ParsedKey parseKey(TableDescriptor descriptor, String encodedKey) {
        String[] parts = encodedKey.split("~", -1);
        List<FieldDescriptor> keys = descriptor.keyFields();
        if (parts.length != keys.size()) throw new IllegalArgumentException("Invalid CAL2 record key");
        StringJoiner where = new StringJoiner(" AND ");
        MapSqlParameterSource params = new MapSqlParameterSource();
        for (int i = 0; i < keys.size(); i++) {
            FieldDescriptor field = keys.get(i);
            String name = "key" + i;
            where.add(Calendar2SqlNames.identifier(field.columnName()) + " = :" + name);
            params.addValue(name, databaseValue(field, parts[i]), sqlType(field));
        }
        return new ParsedKey(where.toString(), params);
    }

    private record ParsedKey(String where, MapSqlParameterSource params) {}
}
