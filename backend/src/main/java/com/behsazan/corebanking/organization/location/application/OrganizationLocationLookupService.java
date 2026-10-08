package com.behsazan.corebanking.organization.location.application;

import com.behsazan.corebanking.referencedata.management.domain.LookupOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class OrganizationLocationLookupService {
    private static final Pattern ORACLE_IDENTIFIER = Pattern.compile("[A-Z][A-Z0-9_$#]*");
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    private final NamedParameterJdbcTemplate jdbc;
    private final String geoSchema;

    public OrganizationLocationLookupService(
            NamedParameterJdbcTemplate jdbc,
            @Value("${core-banking.schemas.reference-data:GEO}") String geoSchema
    ) {
        this.jdbc = jdbc;
        this.geoSchema = identifier(geoSchema, "GEO");
    }

    @Transactional(readOnly = true)
    public List<LookupOption> searchCities(Long provinceId, String text, Integer requestedLimit) {
        int limit = requestedLimit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(requestedLimit, MAX_LIMIT));
        String normalized = text == null ? "" : text.trim();
        String like = normalized.isBlank() ? null : "%" + normalized.toLowerCase(Locale.ROOT) + "%";

        String sql = """
                SELECT CITY_ID, CITY_CODE, CITY_NAME, PROVINCE_NAME
                FROM (
                    SELECT c.CITY_ID,
                           c.CITY_CODE,
                           c.CITY_NAME,
                           p.PROVINCE_NAME
                    FROM %s.CITIES c
                    JOIN %s.DISTRICTS d ON d.DISTRICT_ID = c.DISTRICT_ID
                    JOIN %s.COUNTIES co ON co.COUNTY_ID = d.COUNTY_ID
                    JOIN %s.PROVINCES p ON p.PROVINCE_ID = co.PROVINCE_ID
                    WHERE c.IS_ACTIVE = 1
                      AND (:provinceId IS NULL OR p.PROVINCE_ID = :provinceId)
                      AND (
                           :likeText IS NULL
                           OR LOWER(c.CITY_NAME) LIKE :likeText
                           OR LOWER(NVL(c.CITY_ENGLISH_NAME, '')) LIKE :likeText
                           OR LOWER(c.CITY_CODE) LIKE :likeText
                      )
                    ORDER BY p.PROVINCE_NAME, c.CITY_NAME, c.CITY_CODE
                )
                WHERE ROWNUM <= :limit
                """.formatted(geoSchema, geoSchema, geoSchema, geoSchema);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("provinceId", provinceId)
                .addValue("likeText", like)
                .addValue("limit", limit);

        return jdbc.query(sql, params, (rs, rowNum) -> {
            String cityName = rs.getString("CITY_NAME");
            String provinceName = rs.getString("PROVINCE_NAME");
            String label = provinceName == null || provinceName.isBlank()
                    ? cityName
                    : cityName + " — " + provinceName;
            return new LookupOption(
                    rs.getLong("CITY_ID"),
                    rs.getString("CITY_CODE"),
                    label
            );
        });
    }

    private static String identifier(String value, String fallback) {
        String normalized = value == null || value.isBlank() ? fallback : value.trim().toUpperCase(Locale.ROOT);
        if (!ORACLE_IDENTIFIER.matcher(normalized).matches()) {
            throw new IllegalStateException("Invalid Oracle schema configured for organization location lookup.");
        }
        return normalized;
    }
}
