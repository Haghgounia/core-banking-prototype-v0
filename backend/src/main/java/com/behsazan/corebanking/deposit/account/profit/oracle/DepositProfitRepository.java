package com.behsazan.corebanking.deposit.account.profit.oracle;

import com.behsazan.corebanking.deposit.account.profit.domain.DepositProfitModels.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@Repository
public class DepositProfitRepository {
    public record AccountRow(long accountId,String status,String currencyCode,long openingRequestId,long productVersionId) {}
    public record ContractLock(long profitContractId,long accountId,long openingRequestId,long productVersionId,BigDecimal annualRate,
                               String calculationMethodCode,String dayCountBasisCode,String accrualFrequencyCode,String paymentFrequencyCode,
                               String paymentDestinationCode,String destinationAccountReference,LocalDate effectiveFrom,LocalDate effectiveTo,
                               BigDecimal accruedAmount,BigDecimal paidAmount,LocalDate lastAccrualDate,String statusCode,long recordVersion) {}
    public record PeriodLock(long profitPeriodId,long accountProfitProfileId,LocalDate periodStartDate,LocalDate periodEndDate,
                             String periodStatusCode,BigDecimal totalAccruedAmount,BigDecimal totalAdjustmentAmount,
                             BigDecimal payableAmount,BigDecimal paidAmount,long recordVersion) {}
    public record AdjustmentLock(long profitAdjustmentId,long accountId,long profitPeriodId,Long originalAccrualId,Long originalPaymentId,
                                 String adjustmentTypeCode,BigDecimal adjustmentAmount,String reasonCode,String adjustmentStatusCode,
                                 Long approvalRequestId,String approvalStatusCode,String approverUserId,long recordVersion) {}
    public record PaymentLock(long profitPaymentId,long profitPeriodId,long accountId,LocalDate paymentDueDate,LocalDate paymentDate,
                              BigDecimal paymentAmount,String paymentDestinationCode,String destinationAccountReference,
                              String paymentStatusCode,String postingReference,int attemptNo,long recordVersion) {}
    public record IdempotencyRow(Long accountId,String operationType,String payloadHash,String processingStatus,String resultReference) {}

    private final NamedParameterJdbcTemplate jdbc;
    private final String schema, productSchema;

    public DepositProfitRepository(NamedParameterJdbcTemplate jdbc,
                                   @Value("${core-banking.schemas.deposit-account:DPS2}") String schema,
                                   @Value("${core-banking.schemas.product-definition:PDL}") String productSchema) {
        this.jdbc=jdbc;this.schema=id(schema);this.productSchema=id(productSchema);
    }

    public Optional<AccountRow> account(long accountId,boolean lock){
        String sql="SELECT ACCOUNT_ID,ACCOUNT_STATUS_CODE,CURRENCY_CODE,OPENING_REQUEST_ID,CURRENT_PRODUCT_VERSION_ID FROM "+schema+".DEPOSIT_ACCOUNT WHERE ACCOUNT_ID=:id"+(lock?" FOR UPDATE":"");
        return jdbc.query(sql,new MapSqlParameterSource("id",accountId),(r,n)->new AccountRow(r.getLong(1),r.getString(2),r.getString(3),r.getLong(4),r.getLong(5))).stream().findFirst();
    }
    public Optional<AccountRow> accountByReference(String reference){
        if(reference==null||reference.isBlank())return Optional.empty();
        String sql="SELECT ACCOUNT_ID,ACCOUNT_STATUS_CODE,CURRENCY_CODE,OPENING_REQUEST_ID,CURRENT_PRODUCT_VERSION_ID FROM "+schema+".DEPOSIT_ACCOUNT WHERE ACCOUNT_NO=:ref";
        return jdbc.query(sql,new MapSqlParameterSource("ref",reference.trim()),(r,n)->new AccountRow(r.getLong(1),r.getString(2),r.getString(3),r.getLong(4),r.getLong(5))).stream().findFirst();
    }

    public Optional<String> productFamily(long productVersionId){
        String sql="SELECT P.PRODUCT_FAMILY_CODE FROM "+productSchema+".PRODUCT_VERSION PV JOIN "+productSchema+".PRODUCT P ON P.PRODUCT_ID=PV.PRODUCT_ID WHERE PV.PRODUCT_VERSION_ID=:id";
        return jdbc.query(sql,new MapSqlParameterSource("id",productVersionId),(r,n)->r.getString(1)).stream().findFirst();
    }
    public boolean openingProfitInstructionExists(long openingRequestId){
        Long v=jdbc.queryForObject("SELECT COUNT(*) FROM "+schema+".DEPOSIT_OPENING_PROFIT_INSTRUCTION WHERE OPENING_REQUEST_ID=:id",new MapSqlParameterSource("id",openingRequestId),Long.class);return v!=null&&v>0;
    }

    public int provisionProfileFromOpening(long accountId,long openingRequestId,String actor){
        String sql="""
            INSERT INTO %s.DEPOSIT_ACCOUNT_PROFIT_PROFILE(
              ACCOUNT_PROFIT_PROFILE_ID,ACCOUNT_ID,PRODUCT_PRICING_RULE_ID,PRODUCT_PRICING_COMPONENT_ID,PRODUCT_RATE_TIER_ID,
              PROFIT_PAYMENT_RULE_ID,RATE_VALUE,CALCULATION_METHOD_CODE,DAY_COUNT_BASIS_CODE,ACCRUAL_FREQUENCY_CODE,
              PAYMENT_FREQUENCY_CODE,PAYMENT_DAY_RULE_CODE,PAYMENT_DAY_NO,FIRST_PAYMENT_RULE_CODE,HOLIDAY_ADJUSTMENT_CODE,
              PAYMENT_DESTINATION_CODE,DESTINATION_ACCOUNT_ID,DESTINATION_SELECTED_BY_CUSTOMER,DESTINATION_ACCOUNT_REFERENCE,
              VALID_FROM,VALID_TO,STATUS_CODE,SOURCE_OPENING_PROFIT_INST_ID,CREATED_AT,CREATED_BY,RECORD_VERSION)
            SELECT %s.SEQ_DEPOSIT_ACCOUNT_PROFIT_PROFILE.NEXTVAL,:aid,P.PRICING_RULE_ID,P.PRICING_COMPONENT_ID,P.RATE_TIER_ID,
                   P.PROFIT_PAYMENT_RULE_ID,P.RATE_VALUE,NVL(P.CALCULATION_METHOD_CODE,'PERCENTAGE'),NVL(P.DAY_COUNT_BASIS_CODE,'ACT_365'),
                   NVL(P.ACCRUAL_FREQUENCY_CODE,'DAILY'),NVL(P.PAYMENT_FREQUENCY_CODE,'MATURITY'),P.PAYMENT_DAY_RULE_CODE,NULL,
                   P.FIRST_PAYMENT_RULE_CODE,P.HOLIDAY_ADJUSTMENT_CODE,NVL(P.PAYMENT_DESTINATION_CODE,'SAME_DEPOSIT'),
                   P.DESTINATION_ACCOUNT_ID,P.DESTINATION_SELECTED_BY_CUSTOMER,P.DESTINATION_ACCOUNT_REFERENCE,
                   NVL(OT.START_DATE,NVL(A.OPENED_ON,TRUNC(SYSDATE))),OT.MATURITY_DATE,'ACTIVE',P.OPENING_PROFIT_INSTRUCTION_ID,
                   SYSTIMESTAMP,:actor,1
              FROM %s.DEPOSIT_OPENING_PROFIT_INSTRUCTION P
              JOIN %s.DEPOSIT_ACCOUNT A ON A.ACCOUNT_ID=:aid AND A.OPENING_REQUEST_ID=P.OPENING_REQUEST_ID
              LEFT JOIN %s.DEPOSIT_OPENING_TERM OT ON OT.OPENING_REQUEST_ID=P.OPENING_REQUEST_ID
             WHERE P.OPENING_REQUEST_ID=:oid
               AND NOT EXISTS (SELECT 1 FROM %s.DEPOSIT_ACCOUNT_PROFIT_PROFILE X WHERE X.SOURCE_OPENING_PROFIT_INST_ID=P.OPENING_PROFIT_INSTRUCTION_ID)
            """.formatted(schema,schema,schema,schema,schema,schema);
        return jdbc.update(sql,new MapSqlParameterSource().addValue("aid",accountId).addValue("oid",openingRequestId).addValue("actor",actor));
    }

    public int provisionContractFromOpening(long accountId,long openingRequestId,long productVersionId,String actor){
        String sql="""
            INSERT INTO %s.DEPOSIT_PROFIT_CONTRACT(
              PROFIT_CONTRACT_ID,ACCOUNT_ID,OPENING_REQUEST_ID,PRODUCT_VERSION_ID,PRICING_RULE_ID,PRICING_COMPONENT_ID,RATE_TIER_ID,
              PROFIT_PAYMENT_RULE_ID,ANNUAL_RATE,CALCULATION_METHOD_CODE,DAY_COUNT_BASIS_CODE,ACCRUAL_FREQUENCY_CODE,PAYMENT_FREQUENCY_CODE,
              PAYMENT_DAY_RULE_CODE,FIRST_PAYMENT_RULE_CODE,HOLIDAY_ADJUSTMENT_CODE,PAYMENT_DESTINATION_CODE,DESTINATION_ACCOUNT_REFERENCE,
              EFFECTIVE_FROM,EFFECTIVE_TO,ACCRUED_AMOUNT,PAID_AMOUNT,LAST_ACCRUAL_DATE,STATUS_CODE,SOURCE_OPENING_PROFIT_INSTRUCTION_ID,
              CREATED_AT,CREATED_BY,RECORD_VERSION)
            SELECT %s.SEQ_DEP_PROFIT_CONTRACT.NEXTVAL,:aid,:oid,:pv,P.PRICING_RULE_ID,P.PRICING_COMPONENT_ID,P.RATE_TIER_ID,
                   P.PROFIT_PAYMENT_RULE_ID,P.RATE_VALUE,NVL(P.CALCULATION_METHOD_CODE,'PERCENTAGE'),NVL(P.DAY_COUNT_BASIS_CODE,'ACT_365'),
                   NVL(P.ACCRUAL_FREQUENCY_CODE,'DAILY'),NVL(P.PAYMENT_FREQUENCY_CODE,'MATURITY'),NVL(P.PAYMENT_DAY_RULE_CODE,'MATURITY_DATE'),
                   NVL(P.FIRST_PAYMENT_RULE_CODE,'MATURITY_ONLY'),NVL(P.HOLIDAY_ADJUSTMENT_CODE,'NEXT_BUSINESS_DAY'),
                   NVL(P.PAYMENT_DESTINATION_CODE,'SAME_DEPOSIT'),P.DESTINATION_ACCOUNT_REFERENCE,
                   NVL(OT.START_DATE,NVL(A.OPENED_ON,TRUNC(SYSDATE))),OT.MATURITY_DATE,0,0,
                   NVL(OT.START_DATE,NVL(A.OPENED_ON,TRUNC(SYSDATE))),'ACTIVE',P.OPENING_PROFIT_INSTRUCTION_ID,SYSTIMESTAMP,:actor,1
              FROM %s.DEPOSIT_OPENING_PROFIT_INSTRUCTION P
              JOIN %s.DEPOSIT_ACCOUNT A ON A.ACCOUNT_ID=:aid AND A.OPENING_REQUEST_ID=P.OPENING_REQUEST_ID
              LEFT JOIN %s.DEPOSIT_OPENING_TERM OT ON OT.OPENING_REQUEST_ID=P.OPENING_REQUEST_ID
             WHERE P.OPENING_REQUEST_ID=:oid AND P.RATE_VALUE IS NOT NULL AND P.RATE_VALUE>=0
               AND NOT EXISTS (SELECT 1 FROM %s.DEPOSIT_PROFIT_CONTRACT C WHERE C.ACCOUNT_ID=:aid AND C.STATUS_CODE='ACTIVE')
            """.formatted(schema,schema,schema,schema,schema,schema);
        return jdbc.update(sql,new MapSqlParameterSource().addValue("aid",accountId).addValue("oid",openingRequestId).addValue("pv",productVersionId).addValue("actor",actor));
    }
    public int syncTermRate(long accountId,String actor){
        String sql="UPDATE "+schema+".DEPOSIT_TERM_CONTRACT T SET CONTRACT_RATE=(SELECT C.ANNUAL_RATE FROM "+schema+".DEPOSIT_PROFIT_CONTRACT C WHERE C.ACCOUNT_ID=T.ACCOUNT_ID AND C.STATUS_CODE='ACTIVE'),UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1 WHERE T.ACCOUNT_ID=:aid AND T.STATUS_CODE='ACTIVE' AND T.CONTRACT_RATE IS NULL AND EXISTS(SELECT 1 FROM "+schema+".DEPOSIT_PROFIT_CONTRACT C WHERE C.ACCOUNT_ID=T.ACCOUNT_ID AND C.STATUS_CODE='ACTIVE')";
        return jdbc.update(sql,new MapSqlParameterSource().addValue("aid",accountId).addValue("actor",actor));
    }

    public Optional<ProfitProfile> profile(long accountId){
        String sql="SELECT ACCOUNT_PROFIT_PROFILE_ID,ACCOUNT_ID,PRODUCT_PRICING_RULE_ID,PRODUCT_PRICING_COMPONENT_ID,PRODUCT_RATE_TIER_ID,PROFIT_PAYMENT_RULE_ID,RATE_VALUE,CALCULATION_METHOD_CODE,DAY_COUNT_BASIS_CODE,ACCRUAL_FREQUENCY_CODE,PAYMENT_FREQUENCY_CODE,PAYMENT_DAY_RULE_CODE,PAYMENT_DAY_NO,FIRST_PAYMENT_RULE_CODE,HOLIDAY_ADJUSTMENT_CODE,PAYMENT_DESTINATION_CODE,DESTINATION_ACCOUNT_ID,DESTINATION_SELECTED_BY_CUSTOMER,DESTINATION_ACCOUNT_REFERENCE,VALID_FROM,VALID_TO,STATUS_CODE,SOURCE_OPENING_PROFIT_INST_ID,RECORD_VERSION FROM "+schema+".DEPOSIT_ACCOUNT_PROFIT_PROFILE WHERE ACCOUNT_ID=:id AND STATUS_CODE='ACTIVE' ORDER BY ACCOUNT_PROFIT_PROFILE_ID DESC FETCH FIRST 1 ROWS ONLY";
        return jdbc.query(sql,new MapSqlParameterSource("id",accountId),(r,n)->new ProfitProfile(r.getLong(1),r.getLong(2),r.getObject(3,Long.class),r.getObject(4,Long.class),r.getObject(5,Long.class),r.getObject(6,Long.class),r.getBigDecimal(7),r.getString(8),r.getString(9),r.getString(10),r.getString(11),r.getString(12),r.getObject(13,Integer.class),r.getString(14),r.getString(15),r.getString(16),r.getObject(17,Long.class),bool(r.getObject(18)),r.getString(19),date(r,20),date(r,21),r.getString(22),r.getObject(23,Long.class),r.getLong(24))).stream().findFirst();
    }
    public Optional<ProfitContract> contract(long accountId){
        String sql="SELECT PROFIT_CONTRACT_ID,ACCOUNT_ID,OPENING_REQUEST_ID,PRODUCT_VERSION_ID,PRICING_RULE_ID,PRICING_COMPONENT_ID,RATE_TIER_ID,PROFIT_PAYMENT_RULE_ID,ANNUAL_RATE,CALCULATION_METHOD_CODE,DAY_COUNT_BASIS_CODE,ACCRUAL_FREQUENCY_CODE,PAYMENT_FREQUENCY_CODE,PAYMENT_DAY_RULE_CODE,FIRST_PAYMENT_RULE_CODE,HOLIDAY_ADJUSTMENT_CODE,PAYMENT_DESTINATION_CODE,DESTINATION_ACCOUNT_REFERENCE,EFFECTIVE_FROM,EFFECTIVE_TO,ACCRUED_AMOUNT,PAID_AMOUNT,LAST_ACCRUAL_DATE,LAST_PAYMENT_DATE,STATUS_CODE,RECORD_VERSION FROM "+schema+".DEPOSIT_PROFIT_CONTRACT WHERE ACCOUNT_ID=:id AND STATUS_CODE='ACTIVE' ORDER BY PROFIT_CONTRACT_ID DESC FETCH FIRST 1 ROWS ONLY";
        return jdbc.query(sql,new MapSqlParameterSource("id",accountId),(r,n)->new ProfitContract(r.getLong(1),r.getLong(2),r.getLong(3),r.getLong(4),r.getObject(5,Long.class),r.getObject(6,Long.class),r.getObject(7,Long.class),r.getObject(8,Long.class),r.getBigDecimal(9),r.getString(10),r.getString(11),r.getString(12),r.getString(13),r.getString(14),r.getString(15),r.getString(16),r.getString(17),r.getString(18),date(r,19),date(r,20),nz(r.getBigDecimal(21)),nz(r.getBigDecimal(22)),date(r,23),date(r,24),r.getString(25),r.getLong(26))).stream().findFirst();
    }
    public Optional<ContractLock> lockContract(long accountId){
        String sql="SELECT PROFIT_CONTRACT_ID,ACCOUNT_ID,OPENING_REQUEST_ID,PRODUCT_VERSION_ID,ANNUAL_RATE,CALCULATION_METHOD_CODE,DAY_COUNT_BASIS_CODE,ACCRUAL_FREQUENCY_CODE,PAYMENT_FREQUENCY_CODE,PAYMENT_DESTINATION_CODE,DESTINATION_ACCOUNT_REFERENCE,EFFECTIVE_FROM,EFFECTIVE_TO,ACCRUED_AMOUNT,PAID_AMOUNT,LAST_ACCRUAL_DATE,STATUS_CODE,RECORD_VERSION FROM "+schema+".DEPOSIT_PROFIT_CONTRACT WHERE ACCOUNT_ID=:id AND STATUS_CODE='ACTIVE' AND PROFIT_CONTRACT_ID=(SELECT MAX(X.PROFIT_CONTRACT_ID) FROM "+schema+".DEPOSIT_PROFIT_CONTRACT X WHERE X.ACCOUNT_ID=:id AND X.STATUS_CODE='ACTIVE') FOR UPDATE";
        return jdbc.query(sql,new MapSqlParameterSource("id",accountId),(r,n)->new ContractLock(r.getLong(1),r.getLong(2),r.getLong(3),r.getLong(4),r.getBigDecimal(5),r.getString(6),r.getString(7),r.getString(8),r.getString(9),r.getString(10),r.getString(11),date(r,12),date(r,13),nz(r.getBigDecimal(14)),nz(r.getBigDecimal(15)),date(r,16),r.getString(17),r.getLong(18))).stream().findFirst();
    }

    public Optional<ProfitPeriod> periodForDate(long accountId,LocalDate date){
        String sql="SELECT P.PROFIT_PERIOD_ID,P.ACCOUNT_PROFIT_PROFILE_ID,P.PERIOD_START_DATE,P.PERIOD_END_DATE,P.PERIOD_STATUS_CODE,P.TOTAL_ACCRUED_AMOUNT,P.TOTAL_ADJUSTMENT_AMOUNT,P.PAYABLE_AMOUNT,P.PAID_AMOUNT,P.RECORD_VERSION FROM "+schema+".DEPOSIT_PROFIT_PERIOD P JOIN "+schema+".DEPOSIT_ACCOUNT_PROFIT_PROFILE F ON F.ACCOUNT_PROFIT_PROFILE_ID=P.ACCOUNT_PROFIT_PROFILE_ID WHERE F.ACCOUNT_ID=:aid AND :dt BETWEEN P.PERIOD_START_DATE AND P.PERIOD_END_DATE ORDER BY P.PROFIT_PERIOD_ID DESC FETCH FIRST 1 ROWS ONLY";
        return jdbc.query(sql,new MapSqlParameterSource().addValue("aid",accountId).addValue("dt",java.sql.Date.valueOf(date)),(r,n)->mapPeriod(r)).stream().findFirst();
    }
    public BigDecimal outstandingPayableAmount(long accountId){
        String sql="SELECT NVL(SUM(GREATEST(NVL(P.PAYABLE_AMOUNT,0)-NVL(P.PAID_AMOUNT,0),0)),0) FROM "+schema+".DEPOSIT_PROFIT_PERIOD P JOIN "+schema+".DEPOSIT_ACCOUNT_PROFIT_PROFILE F ON F.ACCOUNT_PROFIT_PROFILE_ID=P.ACCOUNT_PROFIT_PROFILE_ID WHERE F.ACCOUNT_ID=:aid AND P.PERIOD_STATUS_CODE<>'CLOSED'";
        BigDecimal v=jdbc.queryForObject(sql,new MapSqlParameterSource("aid",accountId),BigDecimal.class);return nz(v);
    }
    public Optional<PeriodLock> lockPayablePeriod(long accountId){
        String sql="SELECT P.PROFIT_PERIOD_ID,P.ACCOUNT_PROFIT_PROFILE_ID,P.PERIOD_START_DATE,P.PERIOD_END_DATE,P.PERIOD_STATUS_CODE,P.TOTAL_ACCRUED_AMOUNT,P.TOTAL_ADJUSTMENT_AMOUNT,P.PAYABLE_AMOUNT,P.PAID_AMOUNT,P.RECORD_VERSION FROM "+schema+".DEPOSIT_PROFIT_PERIOD P WHERE P.PROFIT_PERIOD_ID=(SELECT X.PROFIT_PERIOD_ID FROM (SELECT P2.PROFIT_PERIOD_ID FROM "+schema+".DEPOSIT_PROFIT_PERIOD P2 JOIN "+schema+".DEPOSIT_ACCOUNT_PROFIT_PROFILE F2 ON F2.ACCOUNT_PROFIT_PROFILE_ID=P2.ACCOUNT_PROFIT_PROFILE_ID WHERE F2.ACCOUNT_ID=:aid AND P2.PAYABLE_AMOUNT>P2.PAID_AMOUNT AND P2.PERIOD_STATUS_CODE<>'CLOSED' ORDER BY P2.PERIOD_END_DATE,P2.PROFIT_PERIOD_ID) X WHERE ROWNUM=1) FOR UPDATE";
        return jdbc.query(sql,new MapSqlParameterSource("aid",accountId),(r,n)->mapPeriodLock(r)).stream().findFirst();
    }
    public Optional<PeriodLock> lockPeriod(long periodId){
        String sql="SELECT PROFIT_PERIOD_ID,ACCOUNT_PROFIT_PROFILE_ID,PERIOD_START_DATE,PERIOD_END_DATE,PERIOD_STATUS_CODE,TOTAL_ACCRUED_AMOUNT,TOTAL_ADJUSTMENT_AMOUNT,PAYABLE_AMOUNT,PAID_AMOUNT,RECORD_VERSION FROM "+schema+".DEPOSIT_PROFIT_PERIOD WHERE PROFIT_PERIOD_ID=:id FOR UPDATE";
        return jdbc.query(sql,new MapSqlParameterSource("id",periodId),(r,n)->mapPeriodLock(r)).stream().findFirst();
    }
    public long createPeriod(long profileId,LocalDate from,LocalDate to,String actor){
        long id=next("SEQ_DEPOSIT_PROFIT_PERIOD");
        jdbc.update("INSERT INTO "+schema+".DEPOSIT_PROFIT_PERIOD(PROFIT_PERIOD_ID,ACCOUNT_PROFIT_PROFILE_ID,PERIOD_START_DATE,PERIOD_END_DATE,PERIOD_STATUS_CODE,TOTAL_ACCRUED_AMOUNT,TOTAL_ADJUSTMENT_AMOUNT,PAYABLE_AMOUNT,PAID_AMOUNT,CREATED_AT,CREATED_BY,RECORD_VERSION) VALUES(:id,:pid,:fd,:td,'OPEN',0,0,0,0,SYSTIMESTAMP,:actor,1)",new MapSqlParameterSource().addValue("id",id).addValue("pid",profileId).addValue("fd",java.sql.Date.valueOf(from)).addValue("td",java.sql.Date.valueOf(to)).addValue("actor",actor));
        return id;
    }

    public BigDecimal ledgerBalance(long accountId){BigDecimal v=jdbc.queryForObject("SELECT LEDGER_BALANCE FROM "+schema+".DEPOSIT_ACCOUNT_BALANCE WHERE ACCOUNT_ID=:id",new MapSqlParameterSource("id",accountId),BigDecimal.class);return nz(v);}
    public long next(String seq){Long v=jdbc.queryForObject("SELECT "+schema+"."+id(seq)+".NEXTVAL FROM DUAL",new MapSqlParameterSource(),Long.class);return Objects.requireNonNull(v);}

    public void insertAccrual(long id,long periodId,ContractLock c,LocalDate from,LocalDate to,int days,BigDecimal basis,BigDecimal amount,String actor){
        MapSqlParameterSource ps=new MapSqlParameterSource()
            .addValue("id",id).addValue("pid",periodId).addValue("cid",c.profitContractId()).addValue("aid",c.accountId())
            .addValue("fd",java.sql.Date.valueOf(from)).addValue("td",java.sql.Date.valueOf(to)).addValue("days",days)
            .addValue("basis",basis).addValue("rate",c.annualRate()).addValue("amount",amount)
            .addValue("dcb",c.dayCountBasisCode()).addValue("method",c.calculationMethodCode()).addValue("actor",actor);
        if(hasLegacyAccrualProjection()){
            BigDecimal denominator=BigDecimal.valueOf("ACT_360".equalsIgnoreCase(c.dayCountBasisCode())?360:365);
            BigDecimal dayFraction=BigDecimal.valueOf(days).divide(denominator,10,RoundingMode.HALF_UP);
            String sql="INSERT INTO "+schema+".DEPOSIT_PROFIT_ACCRUAL("+
                "PROFIT_ACCRUAL_ID,PROFIT_PERIOD_ID,ACCOUNT_ID,ACCRUAL_DATE,BALANCE_BASIS_AMOUNT,RATE_VALUE,DAY_FRACTION,ACCRUAL_AMOUNT,ACCRUAL_STATUS_CODE,CREATED_BY,"+
                "PROFIT_CONTRACT_ID,ACCRUAL_FROM_DATE,ACCRUAL_TO_DATE,DAY_COUNT,BASIS_AMOUNT,ANNUAL_RATE,ACCRUED_AMOUNT,DAY_COUNT_BASIS_CODE,CALCULATION_METHOD_CODE,STATUS_CODE,CALCULATED_AT,CALCULATED_BY) "+
                "VALUES(:id,:pid,:aid,:td,:basis,:rate,:dayFraction,:amount,'CALCULATED',:actor,:cid,:fd,:td,:days,:basis,:rate,:amount,:dcb,:method,'ACCRUED',SYSTIMESTAMP,:actor)";
            jdbc.update(sql,ps.addValue("dayFraction",dayFraction));
            return;
        }
        String sql="INSERT INTO "+schema+".DEPOSIT_PROFIT_ACCRUAL(PROFIT_ACCRUAL_ID,PROFIT_PERIOD_ID,PROFIT_CONTRACT_ID,ACCOUNT_ID,ACCRUAL_FROM_DATE,ACCRUAL_TO_DATE,DAY_COUNT,BASIS_AMOUNT,ANNUAL_RATE,ACCRUED_AMOUNT,DAY_COUNT_BASIS_CODE,CALCULATION_METHOD_CODE,STATUS_CODE,CALCULATED_AT,CALCULATED_BY) VALUES(:id,:pid,:cid,:aid,:fd,:td,:days,:basis,:rate,:amount,:dcb,:method,'ACCRUED',SYSTIMESTAMP,:actor)";
        jdbc.update(sql,ps);
    }
    public long insertAccrualDetail(long accrualId,BigDecimal basis,BigDecimal rate,BigDecimal amount,String note,String actor){
        long id=next("SEQ_DEPOSIT_PROFIT_ACCRUAL_DETAIL");
        jdbc.update("INSERT INTO "+schema+".DEPOSIT_PROFIT_ACCRUAL_DETAIL(PROFIT_ACCRUAL_DETAIL_ID,PROFIT_ACCRUAL_ID,COMPONENT_CODE,BASIS_AMOUNT,RATE_VALUE,ACCRUAL_AMOUNT,CALCULATION_NOTE,CREATED_AT,CREATED_BY,RECORD_VERSION) VALUES(:id,:aid,'BASE_PROFIT',:basis,:rate,:amount,:note,SYSTIMESTAMP,:actor,1)",new MapSqlParameterSource().addValue("id",id).addValue("aid",accrualId).addValue("basis",basis).addValue("rate",rate).addValue("amount",amount).addValue("note",note).addValue("actor",actor));
        return id;
    }
    public int applyAccrual(long contractId,LocalDate throughDate,BigDecimal amount,long version,String actor){
        return jdbc.update("UPDATE "+schema+".DEPOSIT_PROFIT_CONTRACT SET ACCRUED_AMOUNT=NVL(ACCRUED_AMOUNT,0)+:amount,LAST_ACCRUAL_DATE=:dt,UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1 WHERE PROFIT_CONTRACT_ID=:id AND RECORD_VERSION=:v AND STATUS_CODE='ACTIVE'",new MapSqlParameterSource().addValue("amount",amount).addValue("dt",java.sql.Date.valueOf(throughDate)).addValue("actor",actor).addValue("id",contractId).addValue("v",version));
    }
    public int applyPeriodAccrual(long periodId,BigDecimal amount,long version,String actor){
        return jdbc.update("UPDATE "+schema+".DEPOSIT_PROFIT_PERIOD SET TOTAL_ACCRUED_AMOUNT=NVL(TOTAL_ACCRUED_AMOUNT,0)+:amount,PAYABLE_AMOUNT=NVL(PAYABLE_AMOUNT,0)+:amount,PERIOD_STATUS_CODE=CASE WHEN PERIOD_STATUS_CODE='CLOSED' THEN 'CLOSED' WHEN NVL(PAYABLE_AMOUNT,0)+:amount>NVL(PAID_AMOUNT,0) THEN 'CALCULATED' ELSE PERIOD_STATUS_CODE END,UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1 WHERE PROFIT_PERIOD_ID=:id AND RECORD_VERSION=:v",new MapSqlParameterSource().addValue("amount",amount).addValue("actor",actor).addValue("id",periodId).addValue("v",version));
    }

    public Optional<Long> periodIdForAccrual(long accountId,long accrualId){
        String sql="SELECT PROFIT_PERIOD_ID FROM "+schema+".DEPOSIT_PROFIT_ACCRUAL WHERE ACCOUNT_ID=:aid AND PROFIT_ACCRUAL_ID=:id";
        return jdbc.query(sql,new MapSqlParameterSource().addValue("aid",accountId).addValue("id",accrualId),(r,n)->r.getObject(1,Long.class)).stream().filter(Objects::nonNull).findFirst();
    }
    public Optional<Long> periodIdForPayment(long accountId,long paymentId){
        String sql="SELECT PROFIT_PERIOD_ID FROM "+schema+".DEPOSIT_PROFIT_PAYMENT WHERE ACCOUNT_ID=:aid AND PROFIT_PAYMENT_ID=:id";
        return jdbc.query(sql,new MapSqlParameterSource().addValue("aid",accountId).addValue("id",paymentId),(r,n)->r.getLong(1)).stream().findFirst();
    }
    public void insertApproval(long id,long accountId,long adjustmentId,String requested,String approver,String org,String idem,String correlation){
        String tracking="PA-"+id;
        jdbc.update("INSERT INTO "+schema+".DEPOSIT_OPERATION_APPROVAL_REQUEST(APPROVAL_REQUEST_ID,TRACKING_NO,ACCOUNT_ID,OPERATION_TYPE_CODE,SOURCE_ENTITY_TYPE,SOURCE_ENTITY_ID,REQUESTED_BY_USER_ID,REQUESTED_AT,APPROVER_USER_ID,APPROVAL_STATUS_CODE,ORG_UNIT_CODE,IDEMPOTENCY_KEY,ORIGIN_SYSTEM_CODE,ORIGIN_MODULE_CODE,ORIGIN_REQUEST_REF,CORRELATION_ID,CREATED_AT,CREATED_BY,RECORD_VERSION) VALUES(:id,:tracking,:aid,'PROFIT_ADJUSTMENT','DEPOSIT_PROFIT_ADJUSTMENT',:sid,:req,SYSTIMESTAMP,:appr,'PENDING_APPROVAL',:org,:idem,'CORE_BANKING','PROFIT_ENGINE',:idem,:corr,SYSTIMESTAMP,:req,1)",new MapSqlParameterSource().addValue("id",id).addValue("tracking",tracking).addValue("aid",accountId).addValue("sid",adjustmentId).addValue("req",requested).addValue("appr",approver).addValue("org",org).addValue("idem",idem).addValue("corr",correlation));
    }
    public void insertAdjustment(long id,long accountId,long periodId,Long accrualId,Long paymentId,String type,BigDecimal amount,String reason,long approvalId,String actor){
        jdbc.update("INSERT INTO "+schema+".DEPOSIT_PROFIT_ADJUSTMENT(PROFIT_ADJUSTMENT_ID,ACCOUNT_ID,PROFIT_PERIOD_ID,ORIGINAL_ACCRUAL_ID,ORIGINAL_PAYMENT_ID,ADJUSTMENT_TYPE_CODE,ADJUSTMENT_AMOUNT,REASON_CODE,ADJUSTMENT_STATUS_CODE,APPROVAL_REQUEST_ID,CREATED_AT,CREATED_BY,RECORD_VERSION) VALUES(:id,:aid,:pid,:acc,:pay,:type,:amount,:reason,'DRAFT',:approval,SYSTIMESTAMP,:actor,1)",new MapSqlParameterSource().addValue("id",id).addValue("aid",accountId).addValue("pid",periodId).addValue("acc",accrualId).addValue("pay",paymentId).addValue("type",type).addValue("amount",amount).addValue("reason",reason).addValue("approval",approvalId).addValue("actor",actor));
    }
    public int approve(long approvalId,String actor){
        return jdbc.update("UPDATE "+schema+".DEPOSIT_OPERATION_APPROVAL_REQUEST SET APPROVAL_STATUS_CODE='APPROVED',DECIDED_AT=SYSTIMESTAMP,UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1 WHERE APPROVAL_REQUEST_ID=:id AND APPROVAL_STATUS_CODE='PENDING_APPROVAL' AND APPROVER_USER_ID=:actor",new MapSqlParameterSource().addValue("id",approvalId).addValue("actor",actor));
    }
    public Optional<AdjustmentLock> lockAdjustment(long accountId,long adjustmentId){
        String sql="SELECT J.PROFIT_ADJUSTMENT_ID,J.ACCOUNT_ID,J.PROFIT_PERIOD_ID,J.ORIGINAL_ACCRUAL_ID,J.ORIGINAL_PAYMENT_ID,J.ADJUSTMENT_TYPE_CODE,J.ADJUSTMENT_AMOUNT,J.REASON_CODE,J.ADJUSTMENT_STATUS_CODE,J.APPROVAL_REQUEST_ID,A.APPROVAL_STATUS_CODE,A.APPROVER_USER_ID,J.RECORD_VERSION FROM "+schema+".DEPOSIT_PROFIT_ADJUSTMENT J LEFT JOIN "+schema+".DEPOSIT_OPERATION_APPROVAL_REQUEST A ON A.APPROVAL_REQUEST_ID=J.APPROVAL_REQUEST_ID WHERE J.ACCOUNT_ID=:aid AND J.PROFIT_ADJUSTMENT_ID=:id FOR UPDATE";
        return jdbc.query(sql,new MapSqlParameterSource().addValue("aid",accountId).addValue("id",adjustmentId),(r,n)->new AdjustmentLock(r.getLong(1),r.getLong(2),r.getLong(3),r.getObject(4,Long.class),r.getObject(5,Long.class),r.getString(6),r.getBigDecimal(7),r.getString(8),r.getString(9),r.getObject(10,Long.class),r.getString(11),r.getString(12),r.getLong(13))).stream().findFirst();
    }
    public int markAdjustmentApproved(long adjustmentId,long version,String actor){
        return jdbc.update("UPDATE "+schema+".DEPOSIT_PROFIT_ADJUSTMENT SET ADJUSTMENT_STATUS_CODE='APPROVED',UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1 WHERE PROFIT_ADJUSTMENT_ID=:id AND RECORD_VERSION=:v AND ADJUSTMENT_STATUS_CODE='DRAFT'",new MapSqlParameterSource().addValue("id",adjustmentId).addValue("v",version).addValue("actor",actor));
    }
    public int markAdjustmentPosted(long adjustmentId,long version,String reference,String actor){
        return jdbc.update("UPDATE "+schema+".DEPOSIT_PROFIT_ADJUSTMENT SET ADJUSTMENT_STATUS_CODE='POSTED',POSTING_REFERENCE=:ref,UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1 WHERE PROFIT_ADJUSTMENT_ID=:id AND RECORD_VERSION=:v AND ADJUSTMENT_STATUS_CODE='APPROVED'",new MapSqlParameterSource().addValue("id",adjustmentId).addValue("v",version).addValue("ref",reference).addValue("actor",actor));
    }
    public int applyPeriodAdjustment(long periodId,BigDecimal delta,long version,String actor){
        return jdbc.update("UPDATE "+schema+".DEPOSIT_PROFIT_PERIOD SET TOTAL_ADJUSTMENT_AMOUNT=NVL(TOTAL_ADJUSTMENT_AMOUNT,0)+:delta,PAYABLE_AMOUNT=NVL(PAYABLE_AMOUNT,0)+:delta,PERIOD_STATUS_CODE=CASE WHEN PERIOD_STATUS_CODE='CLOSED' THEN 'CLOSED' WHEN NVL(PAYABLE_AMOUNT,0)+:delta>NVL(PAID_AMOUNT,0) THEN 'APPROVED' WHEN NVL(PAYABLE_AMOUNT,0)+:delta=NVL(PAID_AMOUNT,0) THEN 'PAID' ELSE PERIOD_STATUS_CODE END,UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1 WHERE PROFIT_PERIOD_ID=:id AND RECORD_VERSION=:v AND NVL(PAYABLE_AMOUNT,0)+:delta>=NVL(PAID_AMOUNT,0)",new MapSqlParameterSource().addValue("delta",delta).addValue("actor",actor).addValue("id",periodId).addValue("v",version));
    }
    public int applyContractAdjustment(long contractId,BigDecimal delta,long version,String actor){
        return jdbc.update("UPDATE "+schema+".DEPOSIT_PROFIT_CONTRACT SET ACCRUED_AMOUNT=NVL(ACCRUED_AMOUNT,0)+:delta,UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1 WHERE PROFIT_CONTRACT_ID=:id AND RECORD_VERSION=:v AND STATUS_CODE='ACTIVE' AND NVL(ACCRUED_AMOUNT,0)+:delta>=0",new MapSqlParameterSource().addValue("delta",delta).addValue("actor",actor).addValue("id",contractId).addValue("v",version));
    }

    public long insertPayment(long periodId,long accountId,LocalDate dueDate,BigDecimal amount,String destination,String destinationRef,String actor){
        long id=next("SEQ_DEPOSIT_PROFIT_PAYMENT");
        jdbc.update("INSERT INTO "+schema+".DEPOSIT_PROFIT_PAYMENT(PROFIT_PAYMENT_ID,PROFIT_PERIOD_ID,ACCOUNT_ID,PAYMENT_DUE_DATE,PAYMENT_AMOUNT,PAYMENT_DESTINATION_CODE,DESTINATION_ACCOUNT_REFERENCE,PAYMENT_STATUS_CODE,ATTEMPT_NO,CREATED_AT,CREATED_BY,RECORD_VERSION) VALUES(:id,:pid,:aid,:due,:amount,:dest,:ref,'DUE',1,SYSTIMESTAMP,:actor,1)",new MapSqlParameterSource().addValue("id",id).addValue("pid",periodId).addValue("aid",accountId).addValue("due",java.sql.Date.valueOf(dueDate)).addValue("amount",amount).addValue("dest",destination).addValue("ref",destinationRef).addValue("actor",actor));
        return id;
    }
    public Optional<PaymentLock> lockPayment(long accountId,long paymentId){
        String sql="SELECT PROFIT_PAYMENT_ID,PROFIT_PERIOD_ID,ACCOUNT_ID,PAYMENT_DUE_DATE,PAYMENT_DATE,PAYMENT_AMOUNT,PAYMENT_DESTINATION_CODE,DESTINATION_ACCOUNT_REFERENCE,PAYMENT_STATUS_CODE,POSTING_REFERENCE,ATTEMPT_NO,RECORD_VERSION FROM "+schema+".DEPOSIT_PROFIT_PAYMENT WHERE ACCOUNT_ID=:aid AND PROFIT_PAYMENT_ID=:id FOR UPDATE";
        return jdbc.query(sql,new MapSqlParameterSource().addValue("aid",accountId).addValue("id",paymentId),(r,n)->new PaymentLock(r.getLong(1),r.getLong(2),r.getLong(3),date(r,4),date(r,5),r.getBigDecimal(6),r.getString(7),r.getString(8),r.getString(9),r.getString(10),r.getInt(11),r.getLong(12))).stream().findFirst();
    }
    public int markPaymentPaid(long paymentId,LocalDate date,String postingReference,long version,String actor){
        return jdbc.update("UPDATE "+schema+".DEPOSIT_PROFIT_PAYMENT SET PAYMENT_DATE=:dt,PAYMENT_STATUS_CODE='PAID',POSTING_REFERENCE=:ref,UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1 WHERE PROFIT_PAYMENT_ID=:id AND RECORD_VERSION=:v AND PAYMENT_STATUS_CODE='DUE'",new MapSqlParameterSource().addValue("dt",java.sql.Date.valueOf(date)).addValue("ref",postingReference).addValue("actor",actor).addValue("id",paymentId).addValue("v",version));
    }
    public int applyPeriodPayment(long periodId,BigDecimal amount,long version,String actor){
        return jdbc.update("UPDATE "+schema+".DEPOSIT_PROFIT_PERIOD SET PAID_AMOUNT=NVL(PAID_AMOUNT,0)+:amount,PERIOD_STATUS_CODE=CASE WHEN NVL(PAID_AMOUNT,0)+:amount>=NVL(PAYABLE_AMOUNT,0) THEN 'PAID' ELSE 'APPROVED' END,UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1 WHERE PROFIT_PERIOD_ID=:id AND RECORD_VERSION=:v AND NVL(PAID_AMOUNT,0)+:amount<=NVL(PAYABLE_AMOUNT,0)",new MapSqlParameterSource().addValue("amount",amount).addValue("actor",actor).addValue("id",periodId).addValue("v",version));
    }

    public void insertPosting(long id,long contractId,long accountId,LocalDate date,BigDecimal amount,String destination,String destinationRef,long subledgerId,String actor){
        String sql="INSERT INTO "+schema+".DEPOSIT_PROFIT_POSTING(PROFIT_POSTING_ID,PROFIT_CONTRACT_ID,ACCOUNT_ID,POSTING_DATE,AMOUNT,DESTINATION_CODE,DESTINATION_ACCOUNT_REFERENCE,SUBLEDGER_ENTRY_ID,STATUS_CODE,POSTED_AT,POSTED_BY) VALUES(:id,:cid,:aid,:dt,:amount,:dest,:ref,:sid,'POSTED',SYSTIMESTAMP,:actor)";
        jdbc.update(sql,new MapSqlParameterSource().addValue("id",id).addValue("cid",contractId).addValue("aid",accountId).addValue("dt",java.sql.Date.valueOf(date)).addValue("amount",amount).addValue("dest",destination).addValue("ref",destinationRef).addValue("sid",subledgerId).addValue("actor",actor));
    }
    public int applyPosting(long contractId,LocalDate date,BigDecimal amount,long version,String actor){
        return jdbc.update("UPDATE "+schema+".DEPOSIT_PROFIT_CONTRACT SET ACCRUED_AMOUNT=GREATEST(NVL(ACCRUED_AMOUNT,0)-:amount,0),PAID_AMOUNT=NVL(PAID_AMOUNT,0)+:amount,LAST_PAYMENT_DATE=:dt,UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1 WHERE PROFIT_CONTRACT_ID=:id AND RECORD_VERSION=:v AND STATUS_CODE='ACTIVE'",new MapSqlParameterSource().addValue("amount",amount).addValue("dt",java.sql.Date.valueOf(date)).addValue("actor",actor).addValue("id",contractId).addValue("v",version));
    }

    public List<ProfitPeriod> periods(long accountId){
        String sql="SELECT P.PROFIT_PERIOD_ID,P.ACCOUNT_PROFIT_PROFILE_ID,P.PERIOD_START_DATE,P.PERIOD_END_DATE,P.PERIOD_STATUS_CODE,P.TOTAL_ACCRUED_AMOUNT,P.TOTAL_ADJUSTMENT_AMOUNT,P.PAYABLE_AMOUNT,P.PAID_AMOUNT,P.RECORD_VERSION FROM "+schema+".DEPOSIT_PROFIT_PERIOD P JOIN "+schema+".DEPOSIT_ACCOUNT_PROFIT_PROFILE F ON F.ACCOUNT_PROFIT_PROFILE_ID=P.ACCOUNT_PROFIT_PROFILE_ID WHERE F.ACCOUNT_ID=:id ORDER BY P.PERIOD_START_DATE DESC,P.PROFIT_PERIOD_ID DESC FETCH FIRST 100 ROWS ONLY";
        return jdbc.query(sql,new MapSqlParameterSource("id",accountId),(r,n)->mapPeriod(r));
    }
    public List<ProfitAccrual> accruals(long accountId){String sql="SELECT PROFIT_ACCRUAL_ID,PROFIT_CONTRACT_ID,ACCOUNT_ID,PROFIT_PERIOD_ID,ACCRUAL_FROM_DATE,ACCRUAL_TO_DATE,DAY_COUNT,BASIS_AMOUNT,ANNUAL_RATE,ACCRUED_AMOUNT,DAY_COUNT_BASIS_CODE,CALCULATION_METHOD_CODE,STATUS_CODE,PROFIT_POSTING_ID,CALCULATED_AT,CALCULATED_BY FROM "+schema+".DEPOSIT_PROFIT_ACCRUAL WHERE ACCOUNT_ID=:id ORDER BY PROFIT_ACCRUAL_ID DESC FETCH FIRST 100 ROWS ONLY";return jdbc.query(sql,new MapSqlParameterSource("id",accountId),(r,n)->new ProfitAccrual(r.getLong(1),r.getLong(2),r.getLong(3),r.getObject(4,Long.class),date(r,5),date(r,6),r.getInt(7),r.getBigDecimal(8),r.getBigDecimal(9),r.getBigDecimal(10),r.getString(11),r.getString(12),r.getString(13),r.getObject(14,Long.class),odt(r.getTimestamp(15)),r.getString(16)));}
    public List<ProfitAccrualDetail> accrualDetails(long accountId){
        String sql="SELECT D.PROFIT_ACCRUAL_DETAIL_ID,D.PROFIT_ACCRUAL_ID,D.COMPONENT_CODE,D.BASIS_AMOUNT,D.RATE_VALUE,D.ACCRUAL_AMOUNT,D.CALCULATION_NOTE,D.RECORD_VERSION FROM "+schema+".DEPOSIT_PROFIT_ACCRUAL_DETAIL D JOIN "+schema+".DEPOSIT_PROFIT_ACCRUAL A ON A.PROFIT_ACCRUAL_ID=D.PROFIT_ACCRUAL_ID WHERE A.ACCOUNT_ID=:id ORDER BY D.PROFIT_ACCRUAL_DETAIL_ID DESC FETCH FIRST 200 ROWS ONLY";
        return jdbc.query(sql,new MapSqlParameterSource("id",accountId),(r,n)->new ProfitAccrualDetail(r.getLong(1),r.getLong(2),r.getString(3),r.getBigDecimal(4),r.getBigDecimal(5),r.getBigDecimal(6),r.getString(7),r.getLong(8)));
    }
    public List<ProfitAdjustment> adjustments(long accountId){
        String sql="SELECT J.PROFIT_ADJUSTMENT_ID,J.ACCOUNT_ID,J.PROFIT_PERIOD_ID,J.ORIGINAL_ACCRUAL_ID,J.ORIGINAL_PAYMENT_ID,J.ADJUSTMENT_TYPE_CODE,J.ADJUSTMENT_AMOUNT,J.REASON_CODE,J.ADJUSTMENT_STATUS_CODE,J.APPROVAL_REQUEST_ID,A.APPROVAL_STATUS_CODE,A.APPROVER_USER_ID,J.POSTING_REFERENCE,J.RECORD_VERSION FROM "+schema+".DEPOSIT_PROFIT_ADJUSTMENT J LEFT JOIN "+schema+".DEPOSIT_OPERATION_APPROVAL_REQUEST A ON A.APPROVAL_REQUEST_ID=J.APPROVAL_REQUEST_ID WHERE J.ACCOUNT_ID=:id ORDER BY J.PROFIT_ADJUSTMENT_ID DESC FETCH FIRST 100 ROWS ONLY";
        return jdbc.query(sql,new MapSqlParameterSource("id",accountId),(r,n)->new ProfitAdjustment(r.getLong(1),r.getLong(2),r.getLong(3),r.getObject(4,Long.class),r.getObject(5,Long.class),r.getString(6),r.getBigDecimal(7),r.getString(8),r.getString(9),r.getObject(10,Long.class),r.getString(11),r.getString(12),r.getString(13),r.getLong(14)));
    }
    public List<ProfitPayment> payments(long accountId){
        String sql="SELECT PROFIT_PAYMENT_ID,PROFIT_PERIOD_ID,ACCOUNT_ID,PAYMENT_DUE_DATE,PAYMENT_DATE,PAYMENT_AMOUNT,PAYMENT_DESTINATION_CODE,DESTINATION_ACCOUNT_REFERENCE,PAYMENT_STATUS_CODE,POSTING_REFERENCE,ATTEMPT_NO,RECORD_VERSION FROM "+schema+".DEPOSIT_PROFIT_PAYMENT WHERE ACCOUNT_ID=:id ORDER BY PROFIT_PAYMENT_ID DESC FETCH FIRST 100 ROWS ONLY";
        return jdbc.query(sql,new MapSqlParameterSource("id",accountId),(r,n)->new ProfitPayment(r.getLong(1),r.getLong(2),r.getLong(3),date(r,4),date(r,5),r.getBigDecimal(6),r.getString(7),r.getString(8),r.getString(9),r.getString(10),r.getInt(11),r.getLong(12)));
    }
    public List<ProfitPosting> postings(long accountId){String sql="SELECT PROFIT_POSTING_ID,PROFIT_CONTRACT_ID,ACCOUNT_ID,POSTING_DATE,AMOUNT,DESTINATION_CODE,DESTINATION_ACCOUNT_REFERENCE,SUBLEDGER_ENTRY_ID,STATUS_CODE,POSTED_AT,POSTED_BY FROM "+schema+".DEPOSIT_PROFIT_POSTING WHERE ACCOUNT_ID=:id ORDER BY PROFIT_POSTING_ID DESC FETCH FIRST 100 ROWS ONLY";return jdbc.query(sql,new MapSqlParameterSource("id",accountId),(r,n)->new ProfitPosting(r.getLong(1),r.getLong(2),r.getLong(3),date(r,4),r.getBigDecimal(5),r.getString(6),r.getString(7),r.getObject(8,Long.class),r.getString(9),odt(r.getTimestamp(10)),r.getString(11)));}
    public int linkAccrualsToPosting(long contractId,long periodId,long postingId){
        String set=hasLegacyAccrualProjection()?"STATUS_CODE='POSTED',PROFIT_POSTING_ID=:pid,ACCRUAL_STATUS_CODE='POSTED',POSTING_REFERENCE=:ref,UPDATED_AT=SYSTIMESTAMP":"STATUS_CODE='POSTED',PROFIT_POSTING_ID=:pid";
        return jdbc.update("UPDATE "+schema+".DEPOSIT_PROFIT_ACCRUAL SET "+set+" WHERE PROFIT_CONTRACT_ID=:cid AND PROFIT_PERIOD_ID=:period AND STATUS_CODE='ACCRUED' AND PROFIT_POSTING_ID IS NULL",new MapSqlParameterSource().addValue("pid",postingId).addValue("ref","P11H-PROFIT-"+postingId).addValue("cid",contractId).addValue("period",periodId));
    }

    private boolean hasLegacyAccrualProjection(){
        Long v=jdbc.queryForObject("SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER=:owner AND TABLE_NAME='DEPOSIT_PROFIT_ACCRUAL' AND COLUMN_NAME IN ('ACCRUAL_DATE','BALANCE_BASIS_AMOUNT','RATE_VALUE','DAY_FRACTION','ACCRUAL_AMOUNT','ACCRUAL_STATUS_CODE','CREATED_BY')",new MapSqlParameterSource("owner",schema),Long.class);
        long count=v==null?0:v;if(count==0)return false;if(count==7)return true;throw new IllegalStateException("Incomplete legacy DEPOSIT_PROFIT_ACCRUAL projection: expected 0 or 7 compatibility columns but found "+count);
    }

    public Optional<IdempotencyRow> findIdempotency(String key){String sql="SELECT ACCOUNT_ID,OPERATION_TYPE_CODE,PAYLOAD_HASH,PROCESSING_STATUS_CODE,RESULT_REFERENCE FROM "+schema+".DEPOSIT_OPERATION_IDEMPOTENCY WHERE IDEMPOTENCY_KEY=:key";return jdbc.query(sql,new MapSqlParameterSource("key",key),(r,n)->new IdempotencyRow(r.getObject(1,Long.class),r.getString(2),r.getString(3),r.getString(4),r.getString(5))).stream().findFirst();}
    public void insertIdempotency(String key,long accountId,String op,String hash,String actor,String corr){jdbc.update("INSERT INTO "+schema+".DEPOSIT_OPERATION_IDEMPOTENCY(OPERATION_IDEMPOTENCY_ID,IDEMPOTENCY_KEY,ACCOUNT_ID,OPERATION_TYPE_CODE,PAYLOAD_HASH,PROCESSING_STATUS_CODE,REQUESTED_BY_USER_ID,REQUESTED_AT,ORIGIN_SYSTEM_CODE,ORIGIN_MODULE_CODE,ORIGIN_REQUEST_REF,CORRELATION_ID,RECORD_VERSION) VALUES(:id,:key,:aid,:op,:hash,'IN_PROGRESS',:actor,SYSTIMESTAMP,'CORE_BANKING','PROFIT_ENGINE',:key,:corr,1)",new MapSqlParameterSource().addValue("id",next("SEQ_DEPOSIT_OPERATION_IDEMPOTENCY")).addValue("key",key).addValue("aid",accountId).addValue("op",op).addValue("hash",hash).addValue("actor",actor).addValue("corr",corr));}
    public void completeIdempotency(String key,String ref){jdbc.update("UPDATE "+schema+".DEPOSIT_OPERATION_IDEMPOTENCY SET PROCESSING_STATUS_CODE='COMPLETED',RESULT_REFERENCE=:ref,COMPLETED_AT=SYSTIMESTAMP,RECORD_VERSION=RECORD_VERSION+1 WHERE IDEMPOTENCY_KEY=:key AND PROCESSING_STATUS_CODE='IN_PROGRESS'",new MapSqlParameterSource().addValue("ref",ref).addValue("key",key));}

    private static ProfitPeriod mapPeriod(java.sql.ResultSet r)throws java.sql.SQLException{return new ProfitPeriod(r.getLong(1),r.getLong(2),date(r,3),date(r,4),r.getString(5),nz(r.getBigDecimal(6)),nz(r.getBigDecimal(7)),nz(r.getBigDecimal(8)),nz(r.getBigDecimal(9)),r.getLong(10));}
    private static PeriodLock mapPeriodLock(java.sql.ResultSet r)throws java.sql.SQLException{return new PeriodLock(r.getLong(1),r.getLong(2),date(r,3),date(r,4),r.getString(5),nz(r.getBigDecimal(6)),nz(r.getBigDecimal(7)),nz(r.getBigDecimal(8)),nz(r.getBigDecimal(9)),r.getLong(10));}
    private static BigDecimal nz(BigDecimal v){return v==null?BigDecimal.ZERO:v;}
    private static LocalDate date(java.sql.ResultSet r,int i)throws java.sql.SQLException{var d=r.getDate(i);return d==null?null:d.toLocalDate();}
    private static OffsetDateTime odt(Timestamp t){return t==null?null:t.toLocalDateTime().atOffset(ZoneOffset.UTC);}
    private static Boolean bool(Object v){return v==null?null:((Number)v).intValue()!=0;}
    private static String id(String raw){String n=raw==null?"":raw.trim().toUpperCase(Locale.ROOT);if(!n.matches("[A-Z][A-Z0-9_$#]{0,127}"))throw new IllegalArgumentException("Invalid Oracle identifier: "+raw);return n;}
}
