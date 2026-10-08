package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.SelectOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Approved FEE definition versions suitable for dormancy maintenance charges.
 * The ID persisted to PDL.MAINTENANCE_FEE_PLAN_ID is the live FEE_DEFINITION_VERSION_ID.
 * No mock sample ID from the reference HTML is ever emitted.
 */
@Service
public class PdlDormancyFeePlanReference {
    private final JdbcClient jdbc;
    private final String schema;

    public PdlDormancyFeePlanReference(JdbcClient jdbc,
            @Value("${core-banking.schemas.fee:FEE}") String schema) {
        this.jdbc = jdbc;
        String resolved = schema == null ? "FEE" : schema.trim().toUpperCase(Locale.ROOT);
        if (!resolved.matches("[A-Z][A-Z0-9_$#]*")) throw new IllegalArgumentException("Invalid FEE schema name");
        this.schema = resolved;
    }

    public List<SelectOption> options() {
        String sql = "SELECT V.FEE_DEFINITION_VERSION_ID AS ID, D.NAME_FA AS TITLE, V.VERSION_NO AS VERSION_NO "
                + "FROM " + schema + ".FEE_DEFINITION_VERSION V "
                + "JOIN " + schema + ".FEE_DEFINITION D ON D.FEE_DEFINITION_ID = V.FEE_DEFINITION_ID "
                + "WHERE V.STATUS_CODE = 'ACTIVE' AND D.IS_ACTIVE = 'Y' "
                + "AND V.EFFECTIVE_FROM <= TRUNC(SYSDATE) "
                + "AND (V.EFFECTIVE_TO IS NULL OR V.EFFECTIVE_TO >= TRUNC(SYSDATE)) "
                + "AND (UPPER(D.FEE_CODE) LIKE '%DORM%' OR UPPER(D.FEE_CODE) LIKE '%INACT%' "
                + "OR UPPER(NVL(D.CATEGORY_CODE, '-')) LIKE '%DORM%' "
                + "OR D.NAME_FA LIKE '%راکد%' OR V.FEE_PLAN_NAME LIKE '%راکد%' "
                + "OR UPPER(NVL(V.FEE_PLAN_TYPE_CODE, '-')) LIKE '%DORM%') "
                + "ORDER BY D.NAME_FA, V.VERSION_NO DESC";
        try {
            return jdbc.sql(sql).query((rs, index) -> {
                long id = rs.getLong("ID");
                String title = rs.getString("TITLE");
                String label = (title != null && !title.isBlank() ? title : "برنامه بدون نام")
                        + " — نسخه " + rs.getString("VERSION_NO");
                return new SelectOption(id, String.valueOf(id), label);
            }).list();
        } catch (DataAccessException notAccessible) {
            // Do not invent a fee ID when FEE has no configured active maintenance program
            // or the datasource lacks permission to read governed FEE tables.
            return List.of();
        }
    }
}
