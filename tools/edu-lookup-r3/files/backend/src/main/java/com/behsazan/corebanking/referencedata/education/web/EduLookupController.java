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

/** Read-only lookup projection backed by the four verified EDU views. */
@RestController
@RequestMapping("/api/v1/education/lookups")
public class EduLookupController {
    private final JdbcClient jdbc;
    private static final Map<String, String> QUERIES = Map.of(
        "fields", "SELECT EDUCATION_FIELDS_ID AS ID_VALUE, FIELD_CODE AS CODE_VALUE, NAME_FA AS LABEL_VALUE FROM EDU.V_EDUCATION_FIELD_LOOKUP WHERE (:text IS NULL OR UPPER(FIELD_CODE) LIKE :text OR UPPER(NAME_FA) LIKE :text) ORDER BY DISPLAY_ORDER, NAME_FA FETCH FIRST :rowLimit ROWS ONLY",
        "levels", "SELECT EDUCATION_LEVELS_ID AS ID_VALUE, LEVEL_CODE AS CODE_VALUE, NAME_FA AS LABEL_VALUE FROM EDU.V_EDUCATION_LEVEL_LOOKUP WHERE (:text IS NULL OR UPPER(LEVEL_CODE) LIKE :text OR UPPER(NAME_FA) LIKE :text) ORDER BY LEVEL_ORDER, NAME_FA FETCH FIRST :rowLimit ROWS ONLY",
        "institutions", "SELECT EDUCATION_INSTITUTIONS_ID AS ID_VALUE, INSTITUTION_CODE AS CODE_VALUE, DISPLAY_NAME_FA AS LABEL_VALUE FROM EDU.V_EDUCATION_INSTITUTION_LOOKUP WHERE (:text IS NULL OR UPPER(INSTITUTION_CODE) LIKE :text OR UPPER(DISPLAY_NAME_FA) LIKE :text) ORDER BY DISPLAY_NAME_FA FETCH FIRST :rowLimit ROWS ONLY",
        "field-levels", "SELECT EDUCATION_FIELD_LEVELS_ID AS ID_VALUE, FIELD_CODE || ':' || LEVEL_CODE AS CODE_VALUE, FIELD_NAME_FA || ' / ' || LEVEL_NAME_FA AS LABEL_VALUE FROM EDU.V_EDUCATION_FIELD_LEVEL_LOOKUP WHERE (:text IS NULL OR UPPER(FIELD_CODE) LIKE :text OR UPPER(LEVEL_CODE) LIKE :text OR UPPER(FIELD_NAME_FA) LIKE :text OR UPPER(LEVEL_NAME_FA) LIKE :text) ORDER BY FIELD_NAME_FA, LEVEL_NAME_FA FETCH FIRST :rowLimit ROWS ONLY"
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
