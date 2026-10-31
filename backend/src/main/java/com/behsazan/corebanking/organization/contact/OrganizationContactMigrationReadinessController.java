package com.behsazan.corebanking.organization.contact;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * R5 migration observability, available even while storage-mode=legacy.
 * Exposes counts only, never the contents of phones, addresses or emails.
 * V2 is historical migration evidence only, never the operational master.
 * New OUCP-only writes can intentionally make V2 parity diverge; this endpoint
 * must not be used as an operational readiness gate after R5-C2 cutover.
 */
@RestController
@RequestMapping("/api/v1/organization/unit-contacts/migration")
public class OrganizationContactMigrationReadinessController {
    private final NamedParameterJdbcTemplate jdbc;

    public OrganizationContactMigrationReadinessController(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record MigrationReadiness(long legacyUnitAssociations,
                                     long canonicalV2Rows,
                                     long missingLegacyLinks,
                                     long orphanV2LegacyLinks,
                                     long sourceBusinessDrift,
                                     long nativeV2Rows,
                                     long servicePointContactAssociations,
                                     boolean sourceCoverageComplete) {}

    @GetMapping("/readiness")
    @Transactional(readOnly = true)
    public MigrationReadiness readiness() {
        Long legacy = count("SELECT COUNT(*) FROM ORG.ORGANIZATION_UNIT_CONTACT_POINTS");
        Long v2 = count("SELECT COUNT(*) FROM ORG.CONTACT_POINTS_V2");
        Long missing = count("""
                SELECT COUNT(*) FROM ORG.ORGANIZATION_UNIT_CONTACT_POINTS a
                 WHERE NOT EXISTS (SELECT 1 FROM ORG.CONTACT_POINTS_V2 v
                   WHERE v.LEGACY_ASSOCIATION_ID=a.ORGANIZATION_UNIT_CONTACT_POINT_ID)
                """);
        Long orphan = count("""
                SELECT COUNT(*) FROM ORG.CONTACT_POINTS_V2 v
                 WHERE v.LEGACY_ASSOCIATION_ID IS NOT NULL
                   AND NOT EXISTS (SELECT 1 FROM ORG.ORGANIZATION_UNIT_CONTACT_POINTS a
                    WHERE a.ORGANIZATION_UNIT_CONTACT_POINT_ID=v.LEGACY_ASSOCIATION_ID)
                """);
        Long drift = count("""
                SELECT COUNT(*)
                  FROM ORG.CONTACT_POINTS_V2 v
                  JOIN ORG.ORGANIZATION_UNIT_CONTACT_POINTS a
                    ON a.ORGANIZATION_UNIT_CONTACT_POINT_ID=v.LEGACY_ASSOCIATION_ID
                 WHERE v.LEGACY_CONTACT_POINT_ID<>a.CONTACT_POINT_ID
                    OR v.ORGANIZATION_UNIT_ID<>a.ORGANIZATION_UNIT_ID
                    OR v.CONTACT_ROLE_ID<>a.CONTACT_ROLE_ID
                    OR v.PRIMARY_FLAG<>a.PRIMARY_FLAG
                    OR DECODE(v.EFFECTIVE_FROM,a.EFFECTIVE_FROM,0,1)=1
                    OR DECODE(v.EFFECTIVE_TO,a.EFFECTIVE_TO,0,1)=1
                    OR DECODE(v.CONTACT_TYPE_CODE,a.CONTACT_TYPE_CODE,0,1)=1
                    OR DECODE(v.CONTACT_VALUE,a.CONTACT_VALUE,0,1)=1
                    OR DECODE(v.DISPLAY_NAME_FA,a.DISPLAY_NAME_FA,0,1)=1
                    OR DECODE(v.SOURCE_CODE,a.SOURCE_CODE,0,1)=1
                """);
        Long nativeRows = count("SELECT COUNT(*) FROM ORG.CONTACT_POINTS_V2 WHERE LEGACY_ASSOCIATION_ID IS NULL");
        Long service = count("SELECT COUNT(*) FROM ORG.SERVICE_POINT_CONTACT_POINTS");
        return new MigrationReadiness(legacy, v2, missing, orphan, drift, nativeRows, service,
                missing == 0 && orphan == 0 && drift == 0);
    }

    private Long count(String sql) {
        return jdbc.queryForObject(sql, Map.of(), Long.class);
    }
}
