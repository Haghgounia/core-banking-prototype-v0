package com.behsazan.corebanking.collateral.release;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceReaderPool;
import com.behsazan.corebanking.collateral.release.CollateralFinalReleaseModels.*;
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

/** CLT-only read evidence. No SELECT FOR UPDATE and no DML/DDL. */
@Repository
public class CollateralFinalReleaseRepository {
    private final ObjectProvider<CollateralReferenceReaderPool> reader;
    public CollateralFinalReleaseRepository(ObjectProvider<CollateralReferenceReaderPool> reader){this.reader=reader;}
    private JdbcTemplate db(){return reader.getObject().jdbc();}
    private static String number(BigDecimal b){return b==null?null:b.toPlainString();}
    private static String timestamp(Timestamp t){return t==null?null:t.toLocalDateTime().toString();}

    public Snapshot snapshot(String id, String branch) {
        List<Snapshot> header=db().query("""
                SELECT c.COLLATERAL_ID,c.COLLATERAL_NUMBER,c.BRANCH_CODE,c.COLLATERAL_STATUS_CODE,
                       c.RECORD_VERSION,t.COLLATERAL_TYPE_CODE,t.REQUIRED_RELEASE_INQUIRY,
                       s.IS_TERMINAL_STATUS_FLAG
                  FROM CLT.COLLATERAL c
                  JOIN CLT.COLLATERAL_TYPE t ON t.COLLATERAL_TYPE_ID=c.COLLATERAL_TYPE_ID
                  JOIN CLT.REF_COLLATERAL_STATUS s ON s.COLLATERAL_STATUS_CODE=c.COLLATERAL_STATUS_CODE
                 WHERE c.COLLATERAL_ID=? AND c.BRANCH_CODE=?
                """, (r,n)-> new Snapshot(number(r.getBigDecimal("COLLATERAL_ID")),
                number(r.getBigDecimal("COLLATERAL_NUMBER")),r.getString("BRANCH_CODE"),
                r.getString("COLLATERAL_STATUS_CODE"),r.getString("COLLATERAL_TYPE_CODE"),
                number(r.getBigDecimal("RECORD_VERSION")),
                "Y".equals(r.getString("REQUIRED_RELEASE_INQUIRY")),
                "Y".equals(r.getString("IS_TERMINAL_STATUS_FLAG")),0,0,0,0,0),
                new BigDecimal(id),branch);
        if(header.size()!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"CLT_R5C_COLLATERAL_NOT_FOUND");
        Snapshot s=header.getFirst();
        List<long[]> allocationCounts=db().query("""
                SELECT COUNT(*) AS TOTAL_COUNT,
                       COALESCE(SUM(CASE WHEN a.USED='YES' THEN 1 ELSE 0 END),0) AS ACTIVE_COUNT,
                       COALESCE(SUM(CASE WHEN a.BRANCH_CODE<>? THEN 1 ELSE 0 END),0) AS CROSS_BRANCH_COUNT,
                       COALESCE(SUM(CASE WHEN a.USED IS NULL OR a.USED NOT IN ('YES','NO') THEN 1 ELSE 0 END),0) AS UNKNOWN_COUNT
                  FROM CLT.COLLATERAL_ALLOCATION a
                 WHERE a.COLLATERAL_ID=?
                """, (r,n)->new long[]{r.getLong("TOTAL_COUNT"),r.getLong("ACTIVE_COUNT"),
                        r.getLong("CROSS_BRANCH_COUNT"),r.getLong("UNKNOWN_COUNT")},
                branch,new BigDecimal(id));
        if(allocationCounts.size()!=1)throw new IllegalStateException("CLT_R5C_ALLOCATION_COUNT_UNAVAILABLE");
        long[] ac=allocationCounts.getFirst();
        Long pending=db().queryForObject("""
                SELECT COUNT(*) FROM CLT.COLLATERAL_TRANSACTION t
                 WHERE t.COLLATERAL_ID=? AND t.TRANSACTION_STATUS_CODE='PENDING'
                """,Long.class,new BigDecimal(id));
        if(pending==null)throw new IllegalStateException("CLT_R5C_PENDING_TRANSACTION_COUNT_UNAVAILABLE");
        return new Snapshot(s.collateralId(),s.registrationNumber(),s.branchCode(),
                s.currentStatusCode(),s.typeCode(),s.recordVersion(),s.releaseInquiryRequired(),
                s.terminalStatusReported(),ac[0],ac[1],ac[2],ac[3],pending);
    }

    private static StatusEvent event(ResultSet r,int row) throws SQLException {
        return new StatusEvent(number(r.getBigDecimal("COLLATERAL_STATUS_HISTORY_ID")),
                number(r.getBigDecimal("STATUS_SEQUENCE_NO")),r.getString("FROM_STATUS_CODE"),
                r.getString("TO_STATUS_CODE"),r.getString("STATUS_REASON_CODE"),
                r.getString("SOURCE_EVENT_CODE"),r.getString("SOURCE_REFERENCE"),
                timestamp(r.getTimestamp("CHANGED_AT")));
    }
    public List<StatusEvent> statusHistory(String id,String branch,int limit){
        return db().query("""
                SELECT h.COLLATERAL_STATUS_HISTORY_ID,h.STATUS_SEQUENCE_NO,h.FROM_STATUS_CODE,
                       h.TO_STATUS_CODE,h.STATUS_REASON_CODE,h.SOURCE_EVENT_CODE,
                       h.SOURCE_REFERENCE,h.CHANGED_AT
                  FROM CLT.COLLATERAL_STATUS_HISTORY h
                  JOIN CLT.COLLATERAL c ON c.COLLATERAL_ID=h.COLLATERAL_ID
                 WHERE h.COLLATERAL_ID=? AND c.BRANCH_CODE=?
                 ORDER BY h.STATUS_SEQUENCE_NO DESC,h.COLLATERAL_STATUS_HISTORY_ID DESC
                 FETCH FIRST ? ROWS ONLY
                """,CollateralFinalReleaseRepository::event,new BigDecimal(id),branch,limit);
    }
}
