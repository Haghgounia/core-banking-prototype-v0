package com.behsazan.corebanking.organization.location.servicepoint;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * R4-A: metadata-only readiness for service-point -> canonical unit-location location link.
 * No query references legacy LOCATIONS/POSTAL_ADDRESSES by their physical table names.
 * Safe after the R4-B archival rename.
 */
@RestController
@RequestMapping("/api/v1/organization/service-point-location-readiness")
public class OrganizationServicePointLocationReadinessController {
    private final NamedParameterJdbcTemplate jdbc;

    public OrganizationServicePointLocationReadinessController(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Status(boolean canonicalColumnRequired,
                         boolean canonicalForeignKeyEnabled,
                         boolean legacyForeignKeyDetached,
                         boolean legacyColumnOptional,
                         boolean legacyNewWritesBlocked,
                         long servicePointCount,
                         long legacyLocationLinkCount,
                         boolean ready) {}

    @GetMapping
    @Transactional(readOnly = true)
    public Status readiness() {
        String sql = """
                SELECT
                  (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='ORG'
                     AND TABLE_NAME='SERVICE_POINTS' AND COLUMN_NAME='ORGANIZATION_UNIT_LOCATION_ID'
                     AND DATA_TYPE='NUMBER' AND DATA_PRECISION=8 AND DATA_SCALE=0 AND NULLABLE='N') AS CANONICAL_COL,
                  (SELECT COUNT(*) FROM ALL_CONSTRAINTS C
                   JOIN ALL_CONSTRAINTS P ON P.OWNER=C.R_OWNER
                     AND P.CONSTRAINT_NAME=C.R_CONSTRAINT_NAME
                   WHERE C.OWNER='ORG' AND C.TABLE_NAME='SERVICE_POINTS'
                     AND C.CONSTRAINT_NAME='FK_SP_R4_CANONICAL_LOCATION'
                     AND C.CONSTRAINT_TYPE='R' AND C.STATUS='ENABLED' AND C.VALIDATED='VALIDATED'
                     AND P.OWNER='ORG' AND P.TABLE_NAME='ORGANIZATION_UNIT_LOCATIONS') AS CANONICAL_FK,
                  (SELECT COUNT(*) FROM ALL_CONSTRAINTS WHERE OWNER='ORG'
                     AND TABLE_NAME='SERVICE_POINTS'
                     AND CONSTRAINT_NAME='FK_POINTS_OF_SERVICE_LOCATION_ID') AS OLD_FK,
                  (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='ORG'
                     AND TABLE_NAME='SERVICE_POINTS' AND COLUMN_NAME='LOCATION_ID'
                     AND NULLABLE='Y') AS OLD_NULLABLE,
                  (SELECT COUNT(*) FROM ALL_CONSTRAINTS WHERE OWNER='ORG'
                     AND TABLE_NAME='SERVICE_POINTS' AND CONSTRAINT_NAME='CK_SP_R4_NO_LEGACY'
                     AND STATUS='ENABLED' AND VALIDATED='VALIDATED') AS NO_LEGACY_CHECK,
                  (SELECT COUNT(*) FROM ORG.SERVICE_POINTS) AS SP_COUNT,
                  (SELECT COUNT(*) FROM ORG.SERVICE_POINTS WHERE LOCATION_ID IS NOT NULL) AS OLD_LINKS
                FROM DUAL
                """;
        return jdbc.queryForObject(sql, new MapSqlParameterSource(), (rs, n) -> {
            boolean column = rs.getInt("CANONICAL_COL") == 1;
            boolean fk = rs.getInt("CANONICAL_FK") == 1;
            boolean detached = rs.getInt("OLD_FK") == 0;
            boolean nullable = rs.getInt("OLD_NULLABLE") == 1;
            boolean blocked = rs.getInt("NO_LEGACY_CHECK") == 1;
            long points = rs.getLong("SP_COUNT");
            long oldLinks = rs.getLong("OLD_LINKS");
            return new Status(column, fk, detached, nullable, blocked, points, oldLinks,
                    column && fk && detached && nullable && blocked && oldLinks == 0);
        });
    }
}
