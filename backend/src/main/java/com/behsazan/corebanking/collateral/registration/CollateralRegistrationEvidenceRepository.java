package com.behsazan.corebanking.collateral.registration;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceReaderPool;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Read-only Oracle evidence. Metadata is observed, not inferred from an exported SQL file. */
@Repository
public class CollateralRegistrationEvidenceRepository {
    public record Evidence(boolean number19Unique, boolean postedStatus, boolean introductionType) {}
    private final ObjectProvider<CollateralReferenceReaderPool> reader;
    public CollateralRegistrationEvidenceRepository(ObjectProvider<CollateralReferenceReaderPool> reader) {
        this.reader = reader;
    }
    public Evidence inspect() {
        JdbcTemplate jdbc = reader.getObject().jdbc();
        List<String> shape = jdbc.query("""
                SELECT DATA_TYPE || ':' || DATA_PRECISION || ':' || DATA_SCALE
                  FROM ALL_TAB_COLUMNS WHERE OWNER = 'CLT' AND TABLE_NAME = 'COLLATERAL'
                   AND COLUMN_NAME = 'COLLATERAL_NUMBER'
                """, (rs, i) -> rs.getString(1));
        Integer uq = jdbc.queryForObject("""
                SELECT COUNT(*) FROM ALL_CONSTRAINTS c JOIN ALL_CONS_COLUMNS cc
                   ON c.OWNER = cc.OWNER AND c.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
                 WHERE c.OWNER = 'CLT' AND c.TABLE_NAME = 'COLLATERAL'
                   AND c.CONSTRAINT_TYPE IN ('U','P') AND c.STATUS = 'ENABLED'
                   AND cc.COLUMN_NAME = 'COLLATERAL_NUMBER'
                   AND (SELECT COUNT(*) FROM ALL_CONS_COLUMNS x
                         WHERE x.OWNER = c.OWNER AND x.CONSTRAINT_NAME = c.CONSTRAINT_NAME) = 1
                """, Integer.class);
        Integer posted = jdbc.queryForObject("""
                SELECT COUNT(*) FROM CLT.REF_COLL_TXN_STATUS
                 WHERE TRANSACTION_STATUS_CODE = 'POSTED' AND STATUS = 'ACTIVE'
                """, Integer.class);
        Integer introduction = jdbc.queryForObject("""
                SELECT COUNT(*) FROM CLT.REF_COLL_TXN_TYPE
                 WHERE TRANSACTION_TYPE_CODE = 'INTRODUCTION' AND STATUS = 'ACTIVE'
                """, Integer.class);
        return new Evidence(shape.size() == 1 && "NUMBER:19:0".equals(shape.getFirst())
                && uq != null && uq == 1, posted != null && posted == 1,
                introduction != null && introduction == 1);
    }
}
