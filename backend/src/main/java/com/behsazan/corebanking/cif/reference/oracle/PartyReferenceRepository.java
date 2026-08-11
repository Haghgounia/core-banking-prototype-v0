package com.behsazan.corebanking.cif.reference.oracle;

import com.behsazan.corebanking.cif.reference.application.PartyReferenceMetadataRegistry;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.ColumnDefinition;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.LookupOption;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.RowResponse;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.TableDefinition;
import com.behsazan.corebanking.shared.model.PageResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PartyReferenceRepository {
    private final JdbcClient jdbcClient;
    private final PartyReferenceMetadataRegistry registry;

    public PartyReferenceRepository(JdbcClient jdbcClient, PartyReferenceMetadataRegistry registry) {
        this.jdbcClient = jdbcClient;
        this.registry = registry;
    }

    public PageResponse<RowResponse> search(
            TableDefinition table,
            String text,
            Boolean active,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();

        if (text != null && !text.isBlank()) {
            List<String> search = table.columns().stream()
                    .filter(column -> "VARCHAR2".equals(column.type()))
                    .filter(column -> table.pk().contains(column.name())
                            || "NAME_FA".equals(column.name())
                            || "NAME_EN".equals(column.name())
                            || "DESCRIPTION_FA".equals(column.name()))
                    .map(column -> "UPPER(T." + id(column.name()) + ") LIKE :text")
                    .toList();
            if (!search.isEmpty()) {
                where.add("(" + String.join(" OR ", search) + ")");
                params.put("text", "%" + text.trim().toUpperCase() + "%");
            }
        }
        if (active != null && table.hasColumn("IS_ACTIVE")) {
            where.add("T.IS_ACTIVE = :active");
            params.put("active", active ? 1 : 0);
        }

        String from = " FROM " + table(table) + " T" + (where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where));
        long total = jdbcClient.sql("SELECT COUNT(*)" + from).params(params).query(Long.class).single();

        int normalizedSize = Math.min(Math.max(size, 1), 200);
        int normalizedPage = Math.max(page, 0);
        String orderColumn = safeSortColumn(table, sortBy);
        String orderDirection = "desc".equalsIgnoreCase(direction) ? "DESC" : "ASC";
        Map<String, Object> pageParams = new LinkedHashMap<>(params);
        pageParams.put("offset", normalizedPage * normalizedSize);
        pageParams.put("pageSize", normalizedSize);

        String sql = "SELECT " + selectList(table) + from
                + " ORDER BY T." + id(orderColumn) + " " + orderDirection
                + " OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY";
        List<RowResponse> rows = jdbcClient.sql(sql).params(pageParams)
                .query((rs, rowNum) -> toRow(table, rs))
                .list();
        return new PageResponse<>(rows, total, normalizedPage, normalizedSize);
    }

    public Optional<RowResponse> find(TableDefinition table, String key) {
        KeyParts parts = decodeKey(table, key);
        String sql = "SELECT " + selectList(table) + " FROM " + table(table) + " T WHERE " + parts.where();
        return jdbcClient.sql(sql).params(parts.params())
                .query((rs, rowNum) -> toRow(table, rs))
                .optional();
    }

    public RowResponse insert(TableDefinition table, Map<String, Object> values) {
        List<String> columns = table.columns().stream().map(ColumnDefinition::name).toList();
        String sql = "INSERT INTO " + table(table)
                + " (" + columns.stream().map(PartyReferenceRepository::id).reduce((a,b)->a+", "+b).orElseThrow() + ")"
                + " VALUES (" + columns.stream().map(c -> ":" + c).reduce((a,b)->a+", "+b).orElseThrow() + ")";
        jdbcClient.sql(sql).paramSource(databaseParams(table, values)).update();
        String key = keyFromValues(table, values);
        return find(table, key).orElseThrow();
    }

    public Optional<RowResponse> update(TableDefinition table, String key, Map<String, Object> values) {
        KeyParts parts = decodeKey(table, key);
        List<String> assignments = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();
        parts.params().forEach((name, value) -> params.addValue(name, value, Types.VARCHAR));

        for (ColumnDefinition column : table.columns()) {
            if (table.pk().contains(column.name()) || "RECORD_VERSION".equals(column.name())) {
                continue;
            }
            assignments.add(id(column.name()) + " = :" + column.name());
            params.addValue(column.name(), databaseValue(column, values.get(column.name())), sqlType(column));
        }

        String where = parts.where();
        if (table.hasColumn("RECORD_VERSION")) {
            assignments.add("RECORD_VERSION = RECORD_VERSION + 1");
            ColumnDefinition recordVersion = table.requireColumn("RECORD_VERSION");
            params.addValue("EXPECTED_RECORD_VERSION", databaseValue(recordVersion, values.get("RECORD_VERSION")), sqlType(recordVersion));
            where += " AND RECORD_VERSION = :EXPECTED_RECORD_VERSION";
        }

        int updated = jdbcClient.sql("UPDATE " + table(table) + " SET " + String.join(", ", assignments) + " WHERE " + where)
                .paramSource(params).update();
        if (updated == 0) return Optional.empty();
        return find(table, key);
    }

    public boolean delete(TableDefinition table, String key) {
        KeyParts parts = decodeKey(table, key);
        return jdbcClient.sql("DELETE FROM " + table(table) + " WHERE " + parts.where())
                .params(parts.params()).update() == 1;
    }

    public List<LookupOption> lookup(TableDefinition table, String text, int limit) {
        String codeColumn = table.pk().get(0);
        String labelColumn = table.hasColumn("NAME_FA") ? "NAME_FA" : table.hasColumn("NAME_EN") ? "NAME_EN" : codeColumn;
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();
        if (table.hasColumn("IS_ACTIVE")) where.add("T.IS_ACTIVE = 1");
        if (text != null && !text.isBlank()) {
            where.add("(UPPER(T." + id(codeColumn) + ") LIKE :text OR UPPER(T." + id(labelColumn) + ") LIKE :text)");
            params.put("text", "%" + text.trim().toUpperCase() + "%");
        }
        params.put("limit", Math.min(Math.max(limit, 1), 5000));
        String order = table.hasColumn("SORT_ORDER") ? "T.SORT_ORDER, T." + id(labelColumn) : "T." + id(labelColumn);
        String sql = "SELECT T." + id(codeColumn) + " AS CODE_VALUE, T." + id(labelColumn) + " AS LABEL_VALUE"
                + " FROM " + table(table) + " T"
                + (where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where))
                + " ORDER BY " + order + " FETCH FIRST :limit ROWS ONLY";
        return jdbcClient.sql(sql).params(params).query((rs, rowNum) -> {
            String code = rs.getString("CODE_VALUE");
            return new LookupOption(code, code, rs.getString("LABEL_VALUE"));
        }).list();
    }

    private MapSqlParameterSource databaseParams(TableDefinition table, Map<String, Object> values) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        table.columns().forEach(column ->
                params.addValue(column.name(), databaseValue(column, values.get(column.name())), sqlType(column)));
        return params;
    }

    private static int sqlType(ColumnDefinition column) {
        return switch (column.type()) {
            case "NUMBER" -> Types.NUMERIC;
            case "DATE" -> Types.DATE;
            default -> Types.VARCHAR;
        };
    }

    private static Object databaseValue(ColumnDefinition column, Object value) {
        if (value == null) return null;
        return switch (column.type()) {
            case "NUMBER" -> value instanceof BigDecimal b ? b : new BigDecimal(value.toString());
            case "DATE" -> value instanceof LocalDate d ? Date.valueOf(d) : Date.valueOf(value.toString());
            default -> value.toString();
        };
    }

    private RowResponse toRow(TableDefinition table, ResultSet rs) throws SQLException {
        Map<String, Object> values = new LinkedHashMap<>();
        for (ColumnDefinition column : table.columns()) {
            Object value = switch (column.type()) {
                case "NUMBER" -> rs.getBigDecimal(column.name());
                case "DATE" -> {
                    Date date = rs.getDate(column.name());
                    yield date == null ? null : date.toLocalDate();
                }
                default -> rs.getString(column.name());
            };
            values.put(column.name(), value);
        }
        return new RowResponse(keyFromValues(table, values), values);
    }

    private static String selectList(TableDefinition table) {
        return table.columns().stream().map(c -> "T." + id(c.name())).reduce((a,b)->a+", "+b).orElseThrow();
    }

    private static String safeSortColumn(TableDefinition table, String sortBy) {
        if (sortBy != null && table.hasColumn(sortBy)) return sortBy;
        if (table.hasColumn("SORT_ORDER")) return "SORT_ORDER";
        return table.pk().get(0);
    }

    private String table(TableDefinition table) {
        return PartyReferenceSqlNames.qualified(registry.schemaName(), table.name());
    }

    private static String id(String value) {
        return PartyReferenceSqlNames.identifier(value);
    }

    private static String keyFromValues(TableDefinition table, Map<String, Object> values) {
        List<String> parts = table.pk().stream().map(pk -> String.valueOf(values.get(pk))).toList();
        return PartyReferenceKeyCodec.encode(parts);
    }

    private static KeyParts decodeKey(TableDefinition table, String key) {
        List<String> values = PartyReferenceKeyCodec.decode(key, table.pk().size());
        Map<String, Object> params = new LinkedHashMap<>();
        List<String> conditions = new ArrayList<>();
        for (int i = 0; i < table.pk().size(); i++) {
            String column = table.pk().get(i);
            String param = "PK" + i;
            conditions.add(id(column) + " = :" + param);
            params.put(param, values.get(i));
        }
        return new KeyParts(String.join(" AND ", conditions), params);
    }

    private record KeyParts(String where, Map<String, Object> params) {
    }
}
