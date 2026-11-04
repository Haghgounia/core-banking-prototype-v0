package com.behsazan.corebanking.collateral.unallocation;

import com.behsazan.corebanking.collateral.unallocation.CollateralUnallocationModels.*;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceReaderPool;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

/** Bounded SELECT-only evidence, always anchored to parent CLT.COLLATERAL + authenticated branch. */
@Repository
public class CollateralUnallocationRepository {
    private final ObjectProvider<CollateralReferenceReaderPool> reader;
    public CollateralUnallocationRepository(ObjectProvider<CollateralReferenceReaderPool> reader) {
        this.reader=reader;
    }
    private JdbcTemplate db(){return reader.getObject().jdbc();}
    private static String num(BigDecimal n){return n==null?null:n.toPlainString();}
    private static String time(Timestamp n){return n==null?null:n.toLocalDateTime().toString();}
    private static AllocationLine allocation(ResultSet r,int row) throws SQLException {
        return new AllocationLine(num(r.getBigDecimal("COLLATERAL_ALLOCATION_ID")),
                num(r.getBigDecimal("COLLATERAL_ID")),num(r.getBigDecimal("MODULE_ID")),
                num(r.getBigDecimal("FILE_NUMBER")),num(r.getBigDecimal("ALLOCATE_AMOUNT")),
                r.getString("CURRENCY_CODE").trim(),"YES".equals(r.getString("USED")),
                num(r.getBigDecimal("RECORD_VERSION")),time(r.getTimestamp("CREATED_AT")),
                time(r.getTimestamp("UPDATED_AT")));
    }
    private static final String ALLOCATION_COLUMNS="""
            SELECT a.COLLATERAL_ALLOCATION_ID, a.COLLATERAL_ID, a.MODULE_ID, a.FILE_NUMBER,
                   a.ALLOCATE_AMOUNT, a.USED, a.CURRENCY_CODE, a.RECORD_VERSION,
                   a.CREATED_AT, a.UPDATED_AT
              FROM CLT.COLLATERAL_ALLOCATION a
              JOIN CLT.COLLATERAL c ON c.COLLATERAL_ID = a.COLLATERAL_ID
             WHERE c.COLLATERAL_ID = ? AND c.BRANCH_CODE = ?
               AND a.BRANCH_CODE = c.BRANCH_CODE
            """;
    public List<AllocationLine> allocations(String collateralId,String branch,int limit) {
        return db().query(ALLOCATION_COLUMNS+" ORDER BY a.COLLATERAL_ALLOCATION_ID DESC FETCH FIRST ? ROWS ONLY",
                CollateralUnallocationRepository::allocation,
                new BigDecimal(collateralId),branch,limit);
    }
    public AllocationLine allocationById(String collateralId,String allocationId,String branch) {
        List<AllocationLine> rows=db().query(ALLOCATION_COLUMNS+
                " AND a.COLLATERAL_ALLOCATION_ID=? FETCH FIRST 1 ROWS ONLY",
                CollateralUnallocationRepository::allocation,
                new BigDecimal(collateralId),branch,new BigDecimal(allocationId));
        if(rows.size()!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CLT_R5A_ALLOCATION_NOT_FOUND_IN_AUTHORIZED_BRANCH");
        return rows.getFirst();
    }
    private static TransactionLine transaction(ResultSet r,int row) throws SQLException {
        return new TransactionLine(num(r.getBigDecimal("COLLATERAL_TRANSACTION_ID")),
                num(r.getBigDecimal("COLLATERAL_ALLOCATION_ID")),
                r.getString("COLLATERAL_TRANSACTION_NUMBER"),r.getString("TRANSACTION_TYPE_CODE"),
                r.getString("TRANSACTION_STATUS_CODE"),r.getString("TRANSACTION_DIRECTION_CODE"),
                num(r.getBigDecimal("TRANSACTION_AMOUNT")),r.getString("CURRENCY_CODE").trim(),
                time(r.getTimestamp("EFFECTIVE_AT")),num(r.getBigDecimal("ORIGINAL_TRANSACTION_ID")),
                r.getString("REVERSAL_REASON_CODE"));
    }
    public List<TransactionLine> transactions(String collateralId,String allocationId,
            String branch,int limit) {
        return db().query("""
                SELECT t.COLLATERAL_TRANSACTION_ID, t.COLLATERAL_ALLOCATION_ID,
                       t.COLLATERAL_TRANSACTION_NUMBER, t.TRANSACTION_TYPE_CODE,
                       t.TRANSACTION_STATUS_CODE, t.TRANSACTION_DIRECTION_CODE,
                       t.TRANSACTION_AMOUNT, t.CURRENCY_CODE, t.EFFECTIVE_AT,
                       t.ORIGINAL_TRANSACTION_ID, t.REVERSAL_REASON_CODE
                  FROM CLT.COLLATERAL_TRANSACTION t
                  JOIN CLT.COLLATERAL_ALLOCATION a
                    ON a.COLLATERAL_ALLOCATION_ID=t.COLLATERAL_ALLOCATION_ID
                  JOIN CLT.COLLATERAL c ON c.COLLATERAL_ID=a.COLLATERAL_ID
                 WHERE c.COLLATERAL_ID=? AND c.BRANCH_CODE=?
                   AND a.BRANCH_CODE=c.BRANCH_CODE
                   AND a.COLLATERAL_ALLOCATION_ID=?
                 ORDER BY t.COLLATERAL_TRANSACTION_ID DESC
                 FETCH FIRST ? ROWS ONLY
                """, CollateralUnallocationRepository::transaction,
                new BigDecimal(collateralId),branch,new BigDecimal(allocationId),limit);
    }
}
