package com.behsazan.corebanking.referencedata.education.web;

import com.behsazan.corebanking.referencedata.management.domain.LookupOption;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Read-only lookup projection backed by canonical CIF tables. */
@RestController
@RequestMapping("/api/v1/education/lookups")
public class EduLookupController {
    private final JdbcClient jdbc;
    private static final Map<String, String> QUERIES = Map.of(
        "fields", "SELECT EDUCATION_FIELDS_ID AS ID_VALUE, FIELD_CODE AS CODE_VALUE, NAME_FA AS LABEL_VALUE FROM CIF.EDUCATION_FIELDS WHERE ACTIVE_FLAG = 'Y' AND IS_SELECTABLE = 'Y' AND (:text IS NULL OR UPPER(FIELD_CODE) LIKE :text OR UPPER(NAME_FA) LIKE :text) ORDER BY DISPLAY_ORDER, NAME_FA FETCH FIRST :rowLimit ROWS ONLY",
        "levels", "SELECT EDUCATION_LEVELS_ID AS ID_VALUE, LEVEL_CODE AS CODE_VALUE, NAME_FA AS LABEL_VALUE FROM CIF.EDUCATION_LEVELS WHERE ACTIVE_FLAG = 'Y' AND IS_SELECTABLE = 'Y' AND (:text IS NULL OR UPPER(LEVEL_CODE) LIKE :text OR UPPER(NAME_FA) LIKE :text) ORDER BY LEVEL_ORDER, NAME_FA FETCH FIRST :rowLimit ROWS ONLY",
        "institutions", "SELECT I.EDUCATION_INSTITUTIONS_ID AS ID_VALUE, I.INSTITUTION_CODE AS CODE_VALUE, CASE WHEN P.EDUCATION_INSTITUTIONS_ID IS NULL THEN I.NAME_FA ELSE P.NAME_FA || ' / ' || I.NAME_FA END AS LABEL_VALUE FROM CIF.EDUCATION_INSTITUTIONS I LEFT JOIN CIF.EDUCATION_INSTITUTIONS P ON P.EDUCATION_INSTITUTIONS_ID = I.PARENT_INSTITUTIONS_ID WHERE I.ACTIVE_FLAG = 'Y' AND I.INSTITUTION_STATUS_CODE IN ('ACTIVE','HISTORICAL','RENAMED','NEEDS_REVIEW') AND (:text IS NULL OR UPPER(I.INSTITUTION_CODE) LIKE :text OR UPPER(I.NAME_FA) LIKE :text OR UPPER(P.NAME_FA || ' / ' || I.NAME_FA) LIKE :text) ORDER BY LABEL_VALUE FETCH FIRST :rowLimit ROWS ONLY",
        "field-levels", "SELECT FL.EDUCATION_FIELD_LEVELS_ID AS ID_VALUE, F.FIELD_CODE || ':' || L.LEVEL_CODE AS CODE_VALUE, F.NAME_FA || ' / ' || L.NAME_FA AS LABEL_VALUE FROM CIF.EDUCATION_FIELD_LEVELS FL JOIN CIF.EDUCATION_FIELDS F ON F.EDUCATION_FIELDS_ID = FL.EDUCATION_FIELDS_ID JOIN CIF.EDUCATION_LEVELS L ON L.EDUCATION_LEVELS_ID = FL.EDUCATION_LEVELS_ID WHERE FL.ACTIVE_FLAG = 'Y' AND F.ACTIVE_FLAG = 'Y' AND F.IS_SELECTABLE = 'Y' AND L.ACTIVE_FLAG = 'Y' AND (:text IS NULL OR UPPER(F.FIELD_CODE) LIKE :text OR UPPER(L.LEVEL_CODE) LIKE :text OR UPPER(F.NAME_FA) LIKE :text OR UPPER(L.NAME_FA) LIKE :text) ORDER BY F.NAME_FA, L.NAME_FA FETCH FIRST :rowLimit ROWS ONLY"
    );

    public EduLookupController(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/{type}")
    public List<LookupOption> find(@PathVariable String type,
                                   @RequestParam(required = false) String text,
                                   @RequestParam(defaultValue = "100") int limit) {
        String sql = QUERIES.get(type);
        if (sql == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Unknown EDU lookup type");
        }
        String filter = text == null || text.isBlank() ? null : "%" + text.trim().toUpperCase(java.util.Locale.ROOT) + "%";
        return jdbc.sql(sql)
            .param("text", filter, java.sql.Types.VARCHAR)
            .param("rowLimit", Math.min(Math.max(limit, 1), 500))
            .query((rs, rowNum) -> new LookupOption(
                rs.getLong("ID_VALUE"), rs.getString("CODE_VALUE"), rs.getString("LABEL_VALUE")))
            .list();
    }
}
