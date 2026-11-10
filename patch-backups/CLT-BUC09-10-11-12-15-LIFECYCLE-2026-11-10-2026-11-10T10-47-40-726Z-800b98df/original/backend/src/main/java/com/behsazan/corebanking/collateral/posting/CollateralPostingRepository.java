package com.behsazan.corebanking.collateral.posting;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

/**
 * Only shared-DataSource JDBC; every query and mutation is enlisted in the caller's
 * TransactionTemplate. The parent collateral row is the canonical lock ordering root.
 */
@Repository
public class CollateralPostingRepository {
    public record Parent(String id,String branch,String status,String version,String currency,
            LocalDate validFrom,LocalDate validTo,String valuationId,String valuationStatus,
            String valuationCurrency,LocalDate valuationFrom,LocalDate valuationTo,
            BigDecimal eligibleValue,String typeStatus,boolean statusActive,
            boolean sponsorRequired,boolean insurable,boolean exclusiveUsage) {}
    public record Permit(String id,String operation,String collateralId,String ownerBranch,
            String consumerBranch,String clientId,String domain,String moduleId,String fileNumber,
            String caseReference,BigDecimal amount,String currency,String collateralVersion,
            String allocationVersion,String allocationId,BigDecimal approvedLimit,
            String valuationId,String originalTransactionId,String sourceRequestId,String sourceDigest,
            String obligationReference,String policyReference,String debtorDigest,String transactionNumber,
            String releaseMode,String maker,String checker,String issuer,boolean obligationAttested,
            boolean debtorsAttested,boolean policyAttested,String status,LocalDateTime expiresAt,
            String key,String poster,String postedTransactionId,String postedAllocationId,
            String postedParentVersion,BigDecimal postedRemainingAmount) {}
    public record Exposure(BigDecimal activeAmount,long activeCount,long mismatchedCurrencies,
            long invalidUsage,long otherBranch,long pendingTransactionCount) {}
    public record Allocation(String id,String collateralId,String moduleId,String fileNumber,
            String currency,BigDecimal amount,boolean used,String version) {}
    private static String num(BigDecimal x){return x==null?null:x.toPlainString();}
    private static LocalDate date(Date x){return x==null?null:x.toLocalDate();}
    private static LocalDateTime time(Timestamp x){return x==null?null:x.toLocalDateTime();}
    private static boolean yes(String x){return "Y".equals(x);}
    private static BigDecimal n(String text){return new BigDecimal(text);}
    private static ResponseStatusException deny(String code){return new ResponseStatusException(HttpStatus.CONFLICT,code);}

    public Parent lockParent(JdbcTemplate jdbc,String id,String branch){
        var rows=jdbc.query("""
                SELECT COLLATERAL_ID,BRANCH_CODE,COLLATERAL_STATUS_CODE,RECORD_VERSION,CURRENCY_CODE,
                       VALID_FROM,VALID_TO,CURRENT_VALUATION_HISTORY_ID
                  FROM CLT.COLLATERAL
                 WHERE COLLATERAL_ID=? AND BRANCH_CODE=? FOR UPDATE WAIT 2
                """,(r,i)->new Parent(num(r.getBigDecimal(1)),r.getString(2),r.getString(3),
                        num(r.getBigDecimal(4)),r.getString(5),date(r.getDate(6)),date(r.getDate(7)),
                        num(r.getBigDecimal(8)),null,null,null,null,null,null,false,false,false,false),
                n(id),branch);
        if(rows.size()!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"POSTING_COLLATERAL_NOT_FOUND");
        return rows.getFirst();
    }
    public Parent currentFacts(JdbcTemplate jdbc,Parent locked){
        var rows=jdbc.query("""
                SELECT c.COLLATERAL_ID,c.BRANCH_CODE,c.COLLATERAL_STATUS_CODE,c.RECORD_VERSION,
                       c.CURRENCY_CODE,c.VALID_FROM,c.VALID_TO,c.CURRENT_VALUATION_HISTORY_ID,
                       v.VALUATION_STATUS_CODE,v.CURRENCY_CODE AS V_CURRENCY,
                       v.EFFECTIVE_FROM,v.EFFECTIVE_TO,v.ELIGIBLE_VALUE,
                       t.STATUS AS T_STATUS,t.SPONSOR_REQUIRED,t.INSURABLE,t.EXCLUSIVE_USAGE,
                       s.IS_ACTIVE_STATUS_FLAG
                  FROM CLT.COLLATERAL c
                  JOIN CLT.COLLATERAL_TYPE t ON t.COLLATERAL_TYPE_ID=c.COLLATERAL_TYPE_ID
                  JOIN CLT.REF_COLLATERAL_STATUS s ON s.COLLATERAL_STATUS_CODE=c.COLLATERAL_STATUS_CODE
                  LEFT JOIN CLT.COLLATERAL_VALUATION_HISTORY v
                    ON v.COLLATERAL_VALUATION_HISTORY_ID=c.CURRENT_VALUATION_HISTORY_ID
                   AND v.COLLATERAL_ID=c.COLLATERAL_ID
                 WHERE c.COLLATERAL_ID=? AND c.BRANCH_CODE=? AND c.RECORD_VERSION=?
                """,(r,i)->new Parent(num(r.getBigDecimal(1)),r.getString(2),r.getString(3),
                        num(r.getBigDecimal(4)),r.getString(5),date(r.getDate(6)),date(r.getDate(7)),
                        num(r.getBigDecimal(8)),r.getString(9),r.getString(10),date(r.getDate(11)),
                        date(r.getDate(12)),r.getBigDecimal(13),r.getString(14),yes(r.getString(18)),
                        yes(r.getString(15)),yes(r.getString(16)),yes(r.getString(17))),
                n(locked.id()),locked.branch(),n(locked.version()));
        if(rows.size()!=1)throw deny("POSTING_PARENT_CHANGED_UNDER_LOCK");
        return rows.getFirst();
    }
    private static Permit permit(ResultSet r,int i)throws SQLException{
        return new Permit(num(r.getBigDecimal("PERMIT_ID")),r.getString("OPERATION_CODE"),
                num(r.getBigDecimal("COLLATERAL_ID")),r.getString("BRANCH_CODE"),
                r.getString("CONSUMER_BRANCH_CODE"),r.getString("CLIENT_ID"),r.getString("CONSUMER_DOMAIN"),
                num(r.getBigDecimal("MODULE_ID")),num(r.getBigDecimal("FILE_NUMBER")),
                r.getString("CASE_REFERENCE"),r.getBigDecimal("AMOUNT"),r.getString("CURRENCY_CODE"),
                num(r.getBigDecimal("EXPECTED_COLLATERAL_VERSION")),
                num(r.getBigDecimal("EXPECTED_ALLOCATION_VERSION")),
                num(r.getBigDecimal("ALLOCATION_ID")),r.getBigDecimal("APPROVED_CAPACITY_LIMIT"),
                num(r.getBigDecimal("VALUATION_HISTORY_ID")),
                num(r.getBigDecimal("ORIGINAL_TRANSACTION_ID")),num(r.getBigDecimal("SOURCE_REQUEST_ID")),
                r.getString("SOURCE_DIGEST"),r.getString("OBLIGATION_REFERENCE"),
                r.getString("COVERAGE_POLICY_REFERENCE"),r.getString("DEBTOR_ROSTER_DIGEST"),
                r.getString("TRANSACTION_NUMBER"),r.getString("RELEASE_MODE"),
                r.getString("MAKER_SUBJECT"),r.getString("CHECKER_SUBJECT"),r.getString("ISSUER_SUBJECT"),
                yes(r.getString("OBLIGATION_ATTESTED")),yes(r.getString("DEBTORS_ATTESTED")),
                yes(r.getString("POLICY_ATTESTED")),r.getString("PERMIT_STATUS"),
                time(r.getTimestamp("VALID_UNTIL")),r.getString("IDEMPOTENCY_KEY"),
                r.getString("POSTER_SUBJECT"),num(r.getBigDecimal("POSTED_TRANSACTION_ID")),
                num(r.getBigDecimal("POSTED_ALLOCATION_ID")),num(r.getBigDecimal("POSTED_PARENT_VERSION")),
                r.getBigDecimal("POSTED_REMAINING_AMOUNT"));
    }
    public Permit lockPermit(JdbcTemplate jdbc,String id,String collateralId,String branch){
        var rows=jdbc.query("""
                SELECT PERMIT_ID,OPERATION_CODE,COLLATERAL_ID,BRANCH_CODE,CONSUMER_BRANCH_CODE,
                       CLIENT_ID,CONSUMER_DOMAIN,MODULE_ID,FILE_NUMBER,CASE_REFERENCE,AMOUNT,CURRENCY_CODE,
                       EXPECTED_COLLATERAL_VERSION,EXPECTED_ALLOCATION_VERSION,ALLOCATION_ID,
                       APPROVED_CAPACITY_LIMIT,VALUATION_HISTORY_ID,ORIGINAL_TRANSACTION_ID,
                       SOURCE_REQUEST_ID,SOURCE_DIGEST,OBLIGATION_REFERENCE,COVERAGE_POLICY_REFERENCE,
                       DEBTOR_ROSTER_DIGEST,TRANSACTION_NUMBER,RELEASE_MODE,MAKER_SUBJECT,CHECKER_SUBJECT,
                       ISSUER_SUBJECT,OBLIGATION_ATTESTED,DEBTORS_ATTESTED,POLICY_ATTESTED,
                       PERMIT_STATUS,VALID_UNTIL,IDEMPOTENCY_KEY,POSTER_SUBJECT,
                       POSTED_TRANSACTION_ID,POSTED_ALLOCATION_ID,POSTED_PARENT_VERSION,
                       POSTED_REMAINING_AMOUNT
                  FROM CLT.CLT_COVERAGE_POST_PERMIT
                 WHERE PERMIT_ID=? AND COLLATERAL_ID=? AND BRANCH_CODE=? FOR UPDATE WAIT 2
                """,CollateralPostingRepository::permit,n(id),n(collateralId),branch);
        if(rows.size()!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"POSTING_PERMIT_NOT_FOUND");
        return rows.getFirst();
    }
    public boolean matchesReviewedStage(JdbcTemplate db,Permit p){
        if(p.sourceRequestId()==null || p.sourceDigest()==null || p.sourceDigest().length()!=64)return false;
        String sql;
        Object[] args;
        if("ALLOCATE".equals(p.operation())) {
            sql="""
                SELECT COUNT(*) FROM CLT.CLT_ALLOCATION_REQUEST
                 WHERE REQUEST_ID=? AND COLLATERAL_ID=? AND BRANCH_CODE=?
                   AND CONSUMER_DOMAIN=? AND CASE_REFERENCE=? AND FILE_NUMBER=? AND AMOUNT=? AND CURRENCY_CODE=?
                   AND PAYLOAD_DIGEST=? AND REQUEST_STATUS='RECORDED_NOT_ALLOCATED'
                   AND DEBTOR_COUNT>0 AND SPONSOR_CONFLICT=0
                """;
            args=new Object[]{n(p.sourceRequestId()),n(p.collateralId()),p.ownerBranch(),p.domain(),p.caseReference(),
                    n(p.fileNumber()),p.amount(),p.currency(),p.sourceDigest()};
        } else if("UNALLOCATE".equals(p.operation())) {
            sql="""
                SELECT COUNT(*) FROM CLT.CLT_UNALLOC_REQ
                 WHERE REQUEST_ID=? AND COLLATERAL_ID=? AND ALLOCATION_ID=? AND BRANCH_CODE=?
                   AND CONSUMER_DOMAIN=? AND MODULE_ID=? AND FILE_NUMBER=?
                   AND REQUESTED_AMOUNT=? AND CURRENCY_CODE=? AND RELEASE_MODE=?
                   AND PAYLOAD_DIGEST=? AND REQUEST_STATUS='RECORDED_NOT_UNALLOCATED'
                   AND EXPECTED_ALLOC_VERSION=?
                """;
            args=new Object[]{n(p.sourceRequestId()),n(p.collateralId()),n(p.allocationId()),
                    p.ownerBranch(),p.domain(),n(p.moduleId()),n(p.fileNumber()),p.amount(),
                    p.currency(),p.releaseMode(),p.sourceDigest(),n(p.allocationVersion())};
        } else return false;
        Integer count=db.queryForObject(sql,Integer.class,args);
        return Integer.valueOf(1).equals(count);
    }
    public Exposure exposure(JdbcTemplate jdbc,String id,String branch,String currency){
        var rows=jdbc.query("""
                SELECT COALESCE(SUM(CASE WHEN USED='YES' AND CURRENCY_CODE=?
                         THEN ALLOCATE_AMOUNT ELSE 0 END),0) AS ACTIVE_AMOUNT,
                       COALESCE(SUM(CASE WHEN USED='YES' THEN 1 ELSE 0 END),0) AS ACTIVE_COUNT,
                       COALESCE(SUM(CASE WHEN USED='YES' AND (CURRENCY_CODE IS NULL OR CURRENCY_CODE<>?) THEN 1 ELSE 0 END),0) AS FX_COUNT,
                       COALESCE(SUM(CASE WHEN USED NOT IN ('YES','NO') OR USED IS NULL
                         OR ALLOCATE_AMOUNT IS NULL OR ALLOCATE_AMOUNT<=0
                         OR MODULE_ID IS NULL OR FILE_NUMBER IS NULL
                         THEN 1 ELSE 0 END),0) AS BAD_COUNT,
                       COALESCE(SUM(CASE WHEN BRANCH_CODE<>? THEN 1 ELSE 0 END),0) AS OTHER_BRANCH
                  FROM CLT.COLLATERAL_ALLOCATION WHERE COLLATERAL_ID=?
                """,(r,i)->new long[]{r.getLong("ACTIVE_COUNT"),r.getLong("FX_COUNT"),
                        r.getLong("BAD_COUNT"),r.getLong("OTHER_BRANCH")},
                currency,currency,branch,n(id));
        if(rows.size()!=1)throw deny("POSTING_ALLOCATION_EXPOSURE_UNAVAILABLE");
        BigDecimal amount=jdbc.queryForObject("""
                SELECT COALESCE(SUM(CASE WHEN USED='YES' AND CURRENCY_CODE=? THEN ALLOCATE_AMOUNT ELSE 0 END),0)
                  FROM CLT.COLLATERAL_ALLOCATION WHERE COLLATERAL_ID=?
                """,BigDecimal.class,currency,n(id));
        Long pending=jdbc.queryForObject("""
                SELECT COUNT(*) FROM CLT.COLLATERAL_TRANSACTION
                 WHERE COLLATERAL_ID=? AND (TRANSACTION_STATUS_CODE IS NULL
                        OR TRANSACTION_STATUS_CODE IN ('PENDING','PENDING_APPROVAL'))
                """,Long.class,n(id));
        long[] row=rows.getFirst();
        return new Exposure(amount,row[0],row[1],row[2],row[3],pending==null?-1:pending);
    }
    public Allocation lockAllocation(JdbcTemplate db,String id,String collateralId,String branch){
        var rows=db.query("""
                SELECT COLLATERAL_ALLOCATION_ID,COLLATERAL_ID,MODULE_ID,FILE_NUMBER,
                       CURRENCY_CODE,ALLOCATE_AMOUNT,USED,RECORD_VERSION
                  FROM CLT.COLLATERAL_ALLOCATION
                 WHERE COLLATERAL_ALLOCATION_ID=? AND COLLATERAL_ID=? AND BRANCH_CODE=?
                 FOR UPDATE WAIT 2
                """,(r,i)->new Allocation(num(r.getBigDecimal(1)),num(r.getBigDecimal(2)),
                        num(r.getBigDecimal(3)),num(r.getBigDecimal(4)),r.getString(5),
                        r.getBigDecimal(6),"YES".equals(r.getString(7)),num(r.getBigDecimal(8))),
                n(id),n(collateralId),branch);
        if(rows.size()!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"POSTING_ALLOCATION_NOT_FOUND");
        return rows.getFirst();
    }
    public boolean matchesOriginalTransaction(JdbcTemplate db,Permit p,String allocationTxnType,
            String postedTxnStatus){
        if(p.originalTransactionId()==null || p.allocationId()==null)return false;
        Integer c=db.queryForObject("""
                SELECT COUNT(*) FROM CLT.COLLATERAL_TRANSACTION
                 WHERE COLLATERAL_TRANSACTION_ID=? AND COLLATERAL_ID=?
                   AND COLLATERAL_ALLOCATION_ID=?
                   AND TRANSACTION_TYPE_CODE=? AND TRANSACTION_STATUS_CODE=?
                """,Integer.class,n(p.originalTransactionId()),n(p.collateralId()),
                n(p.allocationId()),allocationTxnType,postedTxnStatus);
        return Integer.valueOf(1).equals(c);
    }
    public LocalDate bankCalendarDay(JdbcTemplate db){
        Date date=db.queryForObject("""
                SELECT TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Tehran' AS DATE)) FROM DUAL
                """,Date.class);
        if(date==null)throw deny("POSTING_ORACLE_DATE_UNAVAILABLE");
        return date.toLocalDate();
    }
    public LocalDateTime oracleNow(JdbcTemplate db){
        Timestamp time=db.queryForObject("SELECT CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Tehran' AS TIMESTAMP) FROM DUAL",Timestamp.class);
        if(time==null)throw deny("POSTING_ORACLE_CLOCK_UNAVAILABLE");
        return time.toLocalDateTime();
    }
    public String nextId(JdbcTemplate db,String sequence){
        String allowed=CollateralPostingPolicy.sequence(sequence);
        BigDecimal id=db.queryForObject("SELECT CLT."+allowed+".NEXTVAL FROM DUAL",BigDecimal.class);
        if(id==null || id.signum()<=0)throw deny("POSTING_SEQUENCE_NOT_AVAILABLE");
        return id.toPlainString();
    }
    public void insertAllocation(JdbcTemplate db,String allocationId,Permit p,String poster){
        int count=db.update("""
                INSERT INTO CLT.COLLATERAL_ALLOCATION
                   (COLLATERAL_ALLOCATION_ID,COLLATERAL_ID,BRANCH_CODE,MODULE_ID,
                    FILE_NUMBER,ALLOCATE_AMOUNT,CURRENCY_CODE,USED,RECORD_VERSION,
                    CREATED_AT,CREATED_BY,UPDATED_AT)
                VALUES (?,?,?,?,?,?,?,'YES',1,SYSTIMESTAMP,?,SYSTIMESTAMP)
                """,n(allocationId),n(p.collateralId()),p.ownerBranch(),n(p.moduleId()),
                n(p.fileNumber()),p.amount(),p.currency(),poster);
        if(count!=1)throw deny("POSTING_ALLOCATION_INSERT_NOT_EXACTLY_ONE");
    }
    public void updateAllocation(JdbcTemplate db,Allocation a,BigDecimal remaining,String poster){
        boolean full=remaining.signum()==0;
        int count=db.update("""
                UPDATE CLT.COLLATERAL_ALLOCATION
                   SET ALLOCATE_AMOUNT=?,USED=?,RECORD_VERSION=RECORD_VERSION+1,
                       UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=?
                 WHERE COLLATERAL_ALLOCATION_ID=? AND COLLATERAL_ID=?
                   AND RECORD_VERSION=? AND USED='YES' AND ALLOCATE_AMOUNT=?
                """,full?a.amount():remaining,full?"NO":"YES",poster,n(a.id()),n(a.collateralId()),
                n(a.version()),a.amount());
        if(count!=1)throw deny("POSTING_ALLOCATION_CAS_LOST");
    }
    public void insertTransaction(JdbcTemplate db,String transactionId,String allocationId,
            Permit p,String type,String status,String direction,String sourceSystem,
            String idempotencyKey,String poster){
        Integer exists=db.queryForObject("""
                SELECT COUNT(*) FROM CLT.COLLATERAL_TRANSACTION
                 WHERE COLLATERAL_TRANSACTION_NUMBER=?
                """,Integer.class,p.transactionNumber());
        if(exists==null || exists!=0)throw deny("POSTING_BANK_TRANSACTION_NUMBER_ALREADY_USED");
        int count=db.update("""
                INSERT INTO CLT.COLLATERAL_TRANSACTION
                   (COLLATERAL_TRANSACTION_ID,COLLATERAL_ID,COLLATERAL_ALLOCATION_ID,
                    COLLATERAL_TRANSACTION_NUMBER,TRANSACTION_TYPE_CODE,TRANSACTION_STATUS_CODE,
                    TRANSACTION_DIRECTION_CODE,TRANSACTION_AMOUNT,CURRENCY_CODE,EFFECTIVE_AT,
                    ORIGINAL_TRANSACTION_ID,SOURCE_SYSTEM_CODE,IDEMPOTENCY_KEY,CREATED_BY,
                    OWNER_BRANCH_CODE,EXECUTING_BRANCH_CODE,REQUEST_ID,TRANSACTION_AT,CREATED_AT,RECORD_VERSION)
                VALUES (?,?,?,?,?,?,?,?,?,SYSTIMESTAMP,?,?,?,?,?,?,?,SYSTIMESTAMP,SYSTIMESTAMP,1)
                """,n(transactionId),n(p.collateralId()),n(allocationId),p.transactionNumber(),
                type,status,direction,p.amount(),p.currency(),
                p.originalTransactionId()==null?null:n(p.originalTransactionId()),
                sourceSystem,idempotencyKey,poster,p.ownerBranch(),p.consumerBranch(),p.sourceRequestId());
        if(count!=1)throw deny("POSTING_TRANSACTION_INSERT_NOT_EXACTLY_ONE");
    }
    /** The first posted state is immutable historical evidence, not an inferred allocation edit. */
    public void insertInitialTransactionStatus(JdbcTemplate db,String transactionId,String status,String poster){
        int count=db.update("""
                INSERT INTO CLT.COLLATERAL_TXN_STATUS_HISTORY
                   (COLLATERAL_TRANSACTION_ID,STATUS_SEQUENCE_NO,FROM_STATUS_CODE,TO_STATUS_CODE,
                    CHANGED_AT,CHANGED_BY,CREATED_AT,CREATED_BY,RECORD_VERSION)
                VALUES (?,1,NULL,?,SYSTIMESTAMP,?,SYSTIMESTAMP,?,1)
                """,n(transactionId),status,poster,poster);
        if(count!=1)throw deny("POSTING_TRANSACTION_STATUS_HISTORY_INSERT_NOT_EXACTLY_ONE");
    }
    public String bumpParent(JdbcTemplate db,Parent parent,String subject){
        int changed=db.update("""
                UPDATE CLT.COLLATERAL
                   SET RECORD_VERSION=RECORD_VERSION+1,UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=?
                 WHERE COLLATERAL_ID=? AND BRANCH_CODE=? AND RECORD_VERSION=?
                """,subject,n(parent.id()),parent.branch(),n(parent.version()));
        if(changed!=1)throw deny("POSTING_PARENT_CAS_LOST");
        return n(parent.version()).add(BigDecimal.ONE).toPlainString();
    }
    public void consumePermit(JdbcTemplate db,Permit permit,String key,String poster,
            String transactionId,String allocationId,String version,BigDecimal remaining){
        int changed=db.update("""
                UPDATE CLT.CLT_COVERAGE_POST_PERMIT
                   SET PERMIT_STATUS='POSTED',IDEMPOTENCY_KEY=?,POSTER_SUBJECT=?,
                       POSTED_TRANSACTION_ID=?,POSTED_ALLOCATION_ID=?,
                       POSTED_PARENT_VERSION=?,POSTED_REMAINING_AMOUNT=?,POSTED_AT=SYSTIMESTAMP
                 WHERE PERMIT_ID=? AND PERMIT_STATUS='APPROVED' AND IDEMPOTENCY_KEY IS NULL
                """,key,poster,n(transactionId),n(allocationId),n(version),remaining,n(permit.id()));
        if(changed!=1)throw deny("POSTING_PERMIT_CONSUMPTION_CAS_LOST");
    }
}
