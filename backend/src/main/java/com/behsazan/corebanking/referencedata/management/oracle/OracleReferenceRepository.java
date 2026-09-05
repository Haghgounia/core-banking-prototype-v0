package com.behsazan.corebanking.referencedata.management.oracle;

import com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorRegistry;
import com.behsazan.corebanking.referencedata.descriptor.domain.FieldType;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceFieldDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceTableDescriptor;
import com.behsazan.corebanking.referencedata.management.application.ReferenceRepository;
import com.behsazan.corebanking.referencedata.management.domain.LookupOption;
import com.behsazan.corebanking.referencedata.management.domain.ReferenceSearchQuery;
import com.behsazan.corebanking.shared.model.PageResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Types;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

@Repository
public class OracleReferenceRepository implements ReferenceRepository {
    private final JdbcClient jdbcClient;
    private final ReferenceDescriptorRegistry registry;

    public OracleReferenceRepository(JdbcClient jdbcClient, ReferenceDescriptorRegistry registry) {
        this.jdbcClient = jdbcClient;
        this.registry = registry;
    }

    @Override
    public PageResponse<Map<String, Object>> search(ReferenceTableDescriptor descriptor, ReferenceSearchQuery query) {
        QueryParts parts = queryParts(descriptor, query);
        long total = jdbcClient.sql("SELECT COUNT(*) " + parts.fromAndWhere())
                .params(parts.parameters())
                .query(Long.class)
                .single();

        List<ReferenceFieldDescriptor> selected = descriptor.gridFields();
        String select = selectList(selected, "T");
        if (descriptor.parent() != null) {
            select += ", P." + parentDescriptor(descriptor).field(parentDescriptor(descriptor).nameApiName()).columnName()
                    + " AS \"parentName\"";
        }

        String orderBy = orderBy(descriptor, query.sortBy());
        String sql = "SELECT " + select + " " + parts.fromAndWhere()
                + " ORDER BY " + orderBy + " " + query.direction().toUpperCase()
                + " OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY";

        Map<String, Object> params = new LinkedHashMap<>(parts.parameters());
        params.put("offset", query.offset());
        params.put("pageSize", query.size());

        List<Map<String, Object>> rows = jdbcClient.sql(sql)
                .params(params)
                .query((rs, rowNum) -> mapFields(rs, selected, descriptor.parent() != null))
                .list();

        return new PageResponse<>(rows, total, query.page(), query.size());
    }

    @Override
    public Optional<Map<String, Object>> findById(ReferenceTableDescriptor descriptor, long id) {
        String sql = "SELECT " + selectList(descriptor.fields(), "T")
                + " FROM " + table(descriptor) + " T"
                + " WHERE T." + OracleSqlNames.identifier(descriptor.idColumnName()) + " = :id";

        return jdbcClient.sql(sql)
                .param("id", id)
                .query((rs, rowNum) -> mapFields(rs, descriptor.fields(), false))
                .optional();
    }

    @Override
    public long insert(ReferenceTableDescriptor descriptor, Map<String, Object> values, long actorId) {
        long id = jdbcClient.sql("SELECT " + OracleSqlNames.qualified(descriptor.schemaName(), descriptor.sequenceName())
                        + ".NEXTVAL FROM DUAL")
                .query(Long.class)
                .single();

        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();

        columns.add(OracleSqlNames.identifier(descriptor.idColumnName()));
        placeholders.add(":generatedId");
        params.addValue("generatedId", id, Types.NUMERIC);

        for (ReferenceFieldDescriptor field : descriptor.editableFields()) {
            columns.add(OracleSqlNames.identifier(field.columnName()));
            placeholders.add(":" + field.apiName());
            params.addValue(field.apiName(), databaseValue(field, values.get(field.apiName())), sqlType(field));
        }

        descriptor.optionalField("createdBy").ifPresent(field -> {
            columns.add(OracleSqlNames.identifier(field.columnName()));
            placeholders.add(":createdBy");
            params.addValue("createdBy", actorValue(field, actorId), sqlType(field));
        });
        descriptor.optionalField("createdDate").ifPresent(field -> {
            columns.add(OracleSqlNames.identifier(field.columnName()));
            placeholders.add("SYSTIMESTAMP");
        });

        String sql = "INSERT INTO " + table(descriptor)
                + " (" + String.join(", ", columns) + ")"
                + " VALUES (" + String.join(", ", placeholders) + ")";
        jdbcClient.sql(sql).paramSource(params).update();
        return id;
    }

    @Override
    public boolean update(ReferenceTableDescriptor descriptor, long id, Map<String, Object> values, long actorId) {
        List<String> assignments = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();
        for (ReferenceFieldDescriptor field : descriptor.editableFields()) {
            assignments.add(OracleSqlNames.identifier(field.columnName()) + " = :" + field.apiName());
            params.addValue(field.apiName(), databaseValue(field, values.get(field.apiName())), sqlType(field));
        }

        descriptor.optionalField("lastModifiedBy").ifPresent(field -> {
            assignments.add(OracleSqlNames.identifier(field.columnName()) + " = :modifiedBy");
            params.addValue("modifiedBy", actorValue(field, actorId), sqlType(field));
        });
        descriptor.optionalField("lastModifiedDate").ifPresent(field ->
                assignments.add(OracleSqlNames.identifier(field.columnName()) + " = SYSTIMESTAMP")
        );

        StringBuilder where = new StringBuilder(OracleSqlNames.identifier(descriptor.idColumnName()) + " = :id");
        descriptor.optionalField("recordVersion").ifPresent(field -> {
            assignments.add(OracleSqlNames.identifier(field.columnName())
                    + " = " + OracleSqlNames.identifier(field.columnName()) + " + 1");
            params.addValue("expectedRecordVersion", values.get("recordVersion"), Types.NUMERIC);
            where.append(" AND ").append(OracleSqlNames.identifier(field.columnName()))
                    .append(" = :expectedRecordVersion");
        });
        params.addValue("id", id, Types.NUMERIC);

        String sql = "UPDATE " + table(descriptor)
                + " SET " + String.join(", ", assignments)
                + " WHERE " + where;
        return jdbcClient.sql(sql).paramSource(params).update() == 1;
    }

    @Override
    public boolean delete(ReferenceTableDescriptor descriptor, long id) {
        String sql = "DELETE FROM " + table(descriptor)
                + " WHERE " + OracleSqlNames.identifier(descriptor.idColumnName()) + " = :id";
        return jdbcClient.sql(sql).param("id", id).update() == 1;
    }

    @Override
    public List<LookupOption> lookup(ReferenceTableDescriptor descriptor, Long parentId, String text, int limit) {
        String idColumn = OracleSqlNames.identifier(descriptor.idColumnName());
        String codeColumn = OracleSqlNames.identifier(descriptor.field(descriptor.codeApiName()).columnName());
        String nameColumn = OracleSqlNames.identifier(descriptor.field(descriptor.nameApiName()).columnName());
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();

        descriptor.optionalField("isActive").ifPresent(field -> where.add("T." + field.columnName() + " = 1"));
        if (descriptor.parent() != null && parentId != null) {
            where.add("T." + descriptor.parent().columnName() + " = :parentId");
            params.put("parentId", parentId);
        }
        if (text != null && !text.isBlank()) {
            where.add("(UPPER(T." + codeColumn + ") LIKE :lookupText OR UPPER(T." + nameColumn + ") LIKE :lookupText)");
            params.put("lookupText", "%" + text.trim().toUpperCase() + "%");
        }
        params.put("lookupLimit", Math.min(Math.max(limit, 1), 5000));

        String sql = "SELECT T." + idColumn + " AS ID_VALUE, T." + codeColumn + " AS CODE_VALUE, T."
                + nameColumn + " AS LABEL_VALUE FROM " + table(descriptor) + " T"
                + (where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where))
                + " ORDER BY T." + nameColumn + " FETCH FIRST :lookupLimit ROWS ONLY";

        return jdbcClient.sql(sql).params(params).query((rs, rowNum) ->
                new LookupOption(rs.getLong("ID_VALUE"), rs.getString("CODE_VALUE"), rs.getString("LABEL_VALUE"))
        ).list();
    }

    @Override
    public long count(ReferenceTableDescriptor descriptor) {
        return jdbcClient.sql("SELECT COUNT(*) FROM " + table(descriptor)).query(Long.class).single();
    }

    private QueryParts queryParts(ReferenceTableDescriptor descriptor, ReferenceSearchQuery query) {
        StringBuilder from = new StringBuilder("FROM ").append(table(descriptor)).append(" T");
        if (descriptor.parent() != null) {
            ReferenceTableDescriptor parent = parentDescriptor(descriptor);
            from.append(" LEFT JOIN ").append(table(parent)).append(" P ON T.")
                    .append(descriptor.parent().columnName()).append(" = P.").append(parent.idColumnName());
        }

        List<String> where = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();
        if (query.text() != null) {
            StringJoiner search = new StringJoiner(" OR ", "(", ")");
            for (ReferenceFieldDescriptor field : descriptor.searchableFields()) {
                search.add("UPPER(T." + OracleSqlNames.identifier(field.columnName()) + ") LIKE :searchText");
            }
            if (search.length() > 2) {
                where.add(search.toString());
                params.put("searchText", "%" + query.text().toUpperCase() + "%");
            }
        }
        if (descriptor.parent() != null && query.parentId() != null) {
            where.add("T." + OracleSqlNames.identifier(descriptor.parent().columnName()) + " = :parentId");
            params.put("parentId", query.parentId());
        }
        if (query.active() != null && descriptor.optionalField("isActive").isPresent()) {
            where.add("T.IS_ACTIVE = :active");
            params.put("active", query.active() ? 1 : 0);
        }
        int filterIndex = 0;
        for (Map.Entry<String, String> entry : query.filters().entrySet()) {
            Optional<ReferenceFieldDescriptor> candidate = descriptor.optionalField(entry.getKey());
            if (candidate.isEmpty()) continue;
            ReferenceFieldDescriptor field = candidate.get();
            if (field.readOnly()) continue;
            String paramName = "filter" + filterIndex++;
            where.add("T." + OracleSqlNames.identifier(field.columnName()) + " = :" + paramName);
            params.put(paramName, queryFilterValue(field, entry.getValue()));
        }

        return new QueryParts(from + (where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where)), params);
    }

    private static Object queryFilterValue(ReferenceFieldDescriptor field, String raw) {
        return switch (field.type()) {
            case NUMBER, SELECT, LOOKUP -> new BigDecimal(raw);
            case BOOLEAN -> ("true".equalsIgnoreCase(raw) || "1".equals(raw)) ? 1 : 0;
            case DATE -> Date.valueOf(raw);
            case TIMESTAMP -> Timestamp.valueOf(raw);
            case TEXT, STRING_SELECT -> raw;
        };
    }

    private String orderBy(ReferenceTableDescriptor descriptor, String requested) {
        if (requested == null || requested.isBlank()) {
            return "T." + descriptor.idColumnName();
        }
        if ("parentName".equals(requested) && descriptor.parent() != null) {
            ReferenceTableDescriptor parent = parentDescriptor(descriptor);
            return "P." + parent.field(parent.nameApiName()).columnName();
        }
        return descriptor.fields().stream()
                .filter(ReferenceFieldDescriptor::grid)
                .filter(field -> field.apiName().equals(requested))
                .findFirst()
                .map(field -> "T." + OracleSqlNames.identifier(field.columnName()))
                .orElse("T." + descriptor.idColumnName());
    }

    private ReferenceTableDescriptor parentDescriptor(ReferenceTableDescriptor descriptor) {
        return registry.require(descriptor.parent().resource());
    }

    private static String table(ReferenceTableDescriptor descriptor) {
        return OracleSqlNames.qualified(descriptor.schemaName(), descriptor.tableName());
    }


    private static String selectList(List<ReferenceFieldDescriptor> fields, String alias) {
        return fields.stream()
                .map(field -> alias + "." + OracleSqlNames.identifier(field.columnName())
                        + " AS \"" + field.apiName() + "\"")
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
    }

    private static Map<String, Object> mapFields(ResultSet rs, List<ReferenceFieldDescriptor> fields,
                                                  boolean includeParentName) throws SQLException {
        Map<String, Object> values = new LinkedHashMap<>();
        for (ReferenceFieldDescriptor field : fields) {
            values.put(field.apiName(), readValue(rs, field));
        }
        if (includeParentName) {
            values.put("parentName", rs.getString("parentName"));
        }
        return values;
    }

    private static Object readValue(ResultSet rs, ReferenceFieldDescriptor field) throws SQLException {
        return switch (field.type()) {
            case BOOLEAN -> {
                BigDecimal value = rs.getBigDecimal(field.apiName());
                yield value == null ? null : value.intValue() == 1;
            }
            case STRING_SELECT -> rs.getString(field.apiName());
            case NUMBER, SELECT, LOOKUP -> rs.getBigDecimal(field.apiName());
            case DATE -> {
                Date value = rs.getDate(field.apiName());
                yield value == null ? null : value.toLocalDate();
            }
            case TIMESTAMP -> {
                Timestamp value = rs.getTimestamp(field.apiName());
                yield value == null ? null : value.toLocalDateTime();
            }
            case TEXT -> rs.getString(field.apiName());
        };
    }

    private static int sqlType(ReferenceFieldDescriptor field) {
        return switch (field.type()) {
            case TEXT, STRING_SELECT -> Types.VARCHAR;
            case NUMBER, BOOLEAN, SELECT, LOOKUP -> Types.NUMERIC;
            case DATE -> Types.DATE;
            case TIMESTAMP -> Types.TIMESTAMP;
        };
    }

    private static Object actorValue(ReferenceFieldDescriptor field, long actorId) {
        return field.type() == FieldType.TEXT ? Long.toString(actorId) : BigDecimal.valueOf(actorId);
    }

    private static Object databaseValue(ReferenceFieldDescriptor field, Object value) {
        if (value == null) {
            return null;
        }
        return switch (field.type()) {
            case BOOLEAN -> Boolean.TRUE.equals(value) ? 1 : 0;
            case NUMBER, SELECT, LOOKUP -> value instanceof BigDecimal ? value : new BigDecimal(value.toString());
            case STRING_SELECT -> value.toString();
            case DATE -> value instanceof LocalDate localDate ? Date.valueOf(localDate) : Date.valueOf(value.toString());
            case TEXT, TIMESTAMP -> value;
        };
    }

    private record QueryParts(String fromAndWhere, Map<String, Object> parameters) {
    }
}
