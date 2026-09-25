package com.behsazan.corebanking.deposit.account.servicing.oracle;

import com.behsazan.corebanking.deposit.account.servicing.domain.DepositAccountWaveAModels.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Repository
public class DepositAccountWaveARepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final String schema;
    private final String productSchema;

    public DepositAccountWaveARepository(
            NamedParameterJdbcTemplate jdbc,
            @Value("${core-banking.schemas.deposit-account:DPS2}") String schema,
            @Value("${core-banking.schemas.product-definition:PDL}") String productSchema
    ) {
        this.jdbc=jdbc; this.schema=id(schema); this.productSchema=id(productSchema);
    }

    public long next(String sequence){Long v=jdbc.getJdbcOperations().queryForObject("SELECT "+schema+"."+id(sequence)+".NEXTVAL FROM DUAL",Long.class);if(v==null)throw new IllegalStateException(sequence+" returned null");return v;}

    public Optional<AccountContext> account(long accountId,boolean lock){
        String sql="SELECT ACCOUNT_ID,OPENING_REQUEST_ID,ACCOUNT_STATUS_CODE,CURRENCY_CODE,CURRENT_PRODUCT_VERSION_ID,RECORD_VERSION FROM "+schema+".DEPOSIT_ACCOUNT WHERE ACCOUNT_ID=:id"+(lock?" FOR UPDATE":"");
        return jdbc.query(sql,Map.of("id",accountId),(r,n)->new AccountContext(r.getLong(1),r.getObject(2,Long.class),r.getString(3),r.getString(4),r.getLong(5),r.getLong(6))).stream().findFirst();
    }

    public List<AccountAttribute> attributes(long accountId){
        String sql="SELECT ACCOUNT_ATTRIBUTE_ID,ACCOUNT_ID,ATTRIBUTE_CODE,ATTRIBUTE_VALUE,VALUE_TYPE_CODE,VALID_FROM,VALID_TO,RECORD_VERSION FROM "+schema+".DEPOSIT_ACCOUNT_ATTRIBUTE WHERE ACCOUNT_ID=:id ORDER BY ACCOUNT_ATTRIBUTE_ID DESC";
        return jdbc.query(sql,Map.of("id",accountId),(r,n)->new AccountAttribute(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),r.getString(5),date(r.getDate(6)),date(r.getDate(7)),r.getLong(8)));
    }
    public Optional<AttributeLock> activeAttribute(long accountId,String code){
        String sql="SELECT ACCOUNT_ATTRIBUTE_ID,ATTRIBUTE_VALUE,VALUE_TYPE_CODE,VALID_FROM,RECORD_VERSION FROM "+schema+".DEPOSIT_ACCOUNT_ATTRIBUTE WHERE ACCOUNT_ID=:aid AND ATTRIBUTE_CODE=:code AND VALID_TO IS NULL AND ACCOUNT_ATTRIBUTE_ID=(SELECT MAX(X.ACCOUNT_ATTRIBUTE_ID) FROM "+schema+".DEPOSIT_ACCOUNT_ATTRIBUTE X WHERE X.ACCOUNT_ID=:aid AND X.ATTRIBUTE_CODE=:code AND X.VALID_TO IS NULL) FOR UPDATE";
        return jdbc.query(sql,new MapSqlParameterSource().addValue("aid",accountId).addValue("code",code),(r,n)->new AttributeLock(r.getLong(1),r.getString(2),r.getString(3),date(r.getDate(4)),r.getLong(5))).stream().findFirst();
    }
    public void insertAttribute(long id,long accountId,String code,String value,String type,LocalDate validFrom,String actor){
        jdbc.update("INSERT INTO "+schema+".DEPOSIT_ACCOUNT_ATTRIBUTE(ACCOUNT_ATTRIBUTE_ID,ACCOUNT_ID,ATTRIBUTE_CODE,ATTRIBUTE_VALUE,VALUE_TYPE_CODE,VALID_FROM,CREATED_AT,CREATED_BY,RECORD_VERSION) VALUES(:id,:aid,:code,:val,:type,:vf,SYSTIMESTAMP,:actor,1)",new MapSqlParameterSource().addValue("id",id).addValue("aid",accountId).addValue("code",code).addValue("val",value,Types.VARCHAR).addValue("type",type).addValue("vf",Date.valueOf(validFrom),Types.DATE).addValue("actor",actor));
    }
    public int updateAttribute(long id,long version,String value,String type,String actor){return jdbc.update("UPDATE "+schema+".DEPOSIT_ACCOUNT_ATTRIBUTE SET ATTRIBUTE_VALUE=:val,VALUE_TYPE_CODE=:type,UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1 WHERE ACCOUNT_ATTRIBUTE_ID=:id AND RECORD_VERSION=:ver",new MapSqlParameterSource().addValue("val",value,Types.VARCHAR).addValue("type",type).addValue("actor",actor).addValue("id",id).addValue("ver",version));}

    public List<ConditionOverride> conditionOverrides(long accountId){
        String sql="SELECT C.ACCOUNT_CONDITION_OVERRIDE_ID,C.ACCOUNT_ID,C.CONDITION_CODE,C.BASE_VALUE,C.OVERRIDE_VALUE,C.VALUE_TYPE_CODE,C.VALID_FROM,C.VALID_TO,C.REASON_CODE,C.APPROVAL_REQUEST_ID,A.TRACKING_NO,A.APPROVAL_STATUS_CODE,A.APPROVER_USER_ID,C.RECORD_VERSION FROM "+schema+".DEPOSIT_ACCOUNT_CONDITION_OVERRIDE C LEFT JOIN "+schema+".DEPOSIT_OPERATION_APPROVAL_REQUEST A ON A.APPROVAL_REQUEST_ID=C.APPROVAL_REQUEST_ID WHERE C.ACCOUNT_ID=:id ORDER BY C.ACCOUNT_CONDITION_OVERRIDE_ID DESC";
        return jdbc.query(sql,Map.of("id",accountId),(r,n)->new ConditionOverride(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),date(r.getDate(7)),date(r.getDate(8)),r.getString(9),r.getObject(10,Long.class),r.getString(11),r.getString(12),r.getString(13),r.getLong(14)));
    }
    public void insertCondition(long id,long accountId,String code,String base,String override,String type,LocalDate from,LocalDate to,String reason,long approval,String actor){
        jdbc.update("INSERT INTO "+schema+".DEPOSIT_ACCOUNT_CONDITION_OVERRIDE(ACCOUNT_CONDITION_OVERRIDE_ID,ACCOUNT_ID,CONDITION_CODE,BASE_VALUE,OVERRIDE_VALUE,VALUE_TYPE_CODE,VALID_FROM,VALID_TO,REASON_CODE,APPROVAL_REQUEST_ID,CREATED_AT,CREATED_BY,RECORD_VERSION) VALUES(:id,:aid,:code,:base,:ov,:type,:vf,:vt,:reason,:approval,SYSTIMESTAMP,:actor,1)",new MapSqlParameterSource().addValue("id",id).addValue("aid",accountId).addValue("code",code).addValue("base",base,Types.VARCHAR).addValue("ov",override).addValue("type",type).addValue("vf",Date.valueOf(from),Types.DATE).addValue("vt",to==null?null:Date.valueOf(to),Types.DATE).addValue("reason",reason).addValue("approval",approval).addValue("actor",actor));
    }
    public Optional<ConditionLock> condition(long accountId,long conditionId,boolean lock){
        String sql="SELECT ACCOUNT_CONDITION_OVERRIDE_ID,APPROVAL_REQUEST_ID FROM "+schema+".DEPOSIT_ACCOUNT_CONDITION_OVERRIDE WHERE ACCOUNT_ID=:aid AND ACCOUNT_CONDITION_OVERRIDE_ID=:id"+(lock?" FOR UPDATE":"");
        return jdbc.query(sql,new MapSqlParameterSource().addValue("aid",accountId).addValue("id",conditionId),(r,n)->new ConditionLock(r.getLong(1),r.getObject(2,Long.class))).stream().findFirst();
    }

    public List<ProductVersionCandidate> productCandidates(long accountId){
        String sql="SELECT TPV.PRODUCT_VERSION_ID FROM "+schema+".DEPOSIT_ACCOUNT A JOIN "+productSchema+".PRODUCT_VERSION CPV ON CPV.PRODUCT_VERSION_ID=A.CURRENT_PRODUCT_VERSION_ID JOIN "+productSchema+".PRODUCT_VERSION TPV ON TPV.PRODUCT_ID=CPV.PRODUCT_ID WHERE A.ACCOUNT_ID=:id AND TPV.PRODUCT_VERSION_ID<>A.CURRENT_PRODUCT_VERSION_ID ORDER BY TPV.PRODUCT_VERSION_ID DESC";
        return jdbc.query(sql,Map.of("id",accountId),(r,n)->new ProductVersionCandidate(r.getLong(1)));
    }
    public boolean sameProductVersion(long currentVersion,long targetVersion){
        String sql="SELECT COUNT(*) FROM "+productSchema+".PRODUCT_VERSION C JOIN "+productSchema+".PRODUCT_VERSION T ON T.PRODUCT_ID=C.PRODUCT_ID WHERE C.PRODUCT_VERSION_ID=:c AND T.PRODUCT_VERSION_ID=:t";
        Integer v=jdbc.queryForObject(sql,new MapSqlParameterSource().addValue("c",currentVersion).addValue("t",targetVersion),Integer.class);return v!=null&&v>0;
    }
    public void insertProductHistory(long id,long accountId,long fromPv,long toPv,LocalDate effective,String reason,long approval,String actor){
        jdbc.update("INSERT INTO "+schema+".DEPOSIT_ACCOUNT_PRODUCT_HISTORY(ACCOUNT_PRODUCT_HISTORY_ID,ACCOUNT_ID,FROM_PRODUCT_VERSION_ID,TO_PRODUCT_VERSION_ID,EFFECTIVE_DATE,CHANGE_REASON_CODE,APPROVAL_REQUEST_ID,MIGRATION_STATUS_CODE,CREATED_AT,CREATED_BY,RECORD_VERSION) VALUES(:id,:aid,:fromPv,:toPv,:eff,:reason,:approval,'PLANNED',SYSTIMESTAMP,:actor,1)",new MapSqlParameterSource().addValue("id",id).addValue("aid",accountId).addValue("fromPv",fromPv).addValue("toPv",toPv).addValue("eff",Date.valueOf(effective),Types.DATE).addValue("reason",reason).addValue("approval",approval).addValue("actor",actor));
    }
    public Optional<ProductChangeLock> productChange(long accountId,long historyId,boolean lock){
        String sql="SELECT H.ACCOUNT_PRODUCT_HISTORY_ID,H.ACCOUNT_ID,H.FROM_PRODUCT_VERSION_ID,H.TO_PRODUCT_VERSION_ID,H.EFFECTIVE_DATE,H.APPROVAL_REQUEST_ID,H.MIGRATION_STATUS_CODE,H.RECORD_VERSION,A.APPROVAL_STATUS_CODE,A.APPROVER_USER_ID FROM "+schema+".DEPOSIT_ACCOUNT_PRODUCT_HISTORY H LEFT JOIN "+schema+".DEPOSIT_OPERATION_APPROVAL_REQUEST A ON A.APPROVAL_REQUEST_ID=H.APPROVAL_REQUEST_ID WHERE H.ACCOUNT_ID=:aid AND H.ACCOUNT_PRODUCT_HISTORY_ID=:id"+(lock?" FOR UPDATE OF H.RECORD_VERSION":"");
        return jdbc.query(sql,new MapSqlParameterSource().addValue("aid",accountId).addValue("id",historyId),(r,n)->new ProductChangeLock(r.getLong(1),r.getLong(2),r.getLong(3),r.getLong(4),date(r.getDate(5)),r.getObject(6,Long.class),r.getString(7),r.getLong(8),r.getString(9),r.getString(10))).stream().findFirst();
    }
    public int markProductHistoryApproved(long historyId,long version,String actor){return jdbc.update("UPDATE "+schema+".DEPOSIT_ACCOUNT_PRODUCT_HISTORY SET MIGRATION_STATUS_CODE='APPROVED',UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1 WHERE ACCOUNT_PRODUCT_HISTORY_ID=:id AND RECORD_VERSION=:ver AND MIGRATION_STATUS_CODE='PLANNED'",new MapSqlParameterSource().addValue("actor",actor).addValue("id",historyId).addValue("ver",version));}
    public int executeProductChange(long accountId,long expectedAccountVersion,long historyId,long expectedHistoryVersion,long fromPv,long toPv,String actor){
        int a=jdbc.update("UPDATE "+schema+".DEPOSIT_ACCOUNT SET PRODUCT_VERSION_ID=:toPv,CURRENT_PRODUCT_VERSION_ID=:toPv,UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1 WHERE ACCOUNT_ID=:aid AND CURRENT_PRODUCT_VERSION_ID=:fromPv AND RECORD_VERSION=:ver",new MapSqlParameterSource().addValue("toPv",toPv).addValue("actor",actor).addValue("aid",accountId).addValue("fromPv",fromPv).addValue("ver",expectedAccountVersion));
        if(a==1)jdbc.update("UPDATE "+schema+".DEPOSIT_ACCOUNT_PRODUCT_HISTORY SET MIGRATION_STATUS_CODE='EXECUTED',UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1 WHERE ACCOUNT_PRODUCT_HISTORY_ID=:id AND RECORD_VERSION=:ver AND MIGRATION_STATUS_CODE IN ('PLANNED','APPROVED')",new MapSqlParameterSource().addValue("actor",actor).addValue("id",historyId).addValue("ver",expectedHistoryVersion));
        return a;
    }
    public List<ProductChange> productChanges(long accountId){
        String sql="SELECT H.ACCOUNT_PRODUCT_HISTORY_ID,H.ACCOUNT_ID,H.FROM_PRODUCT_VERSION_ID,H.TO_PRODUCT_VERSION_ID,H.EFFECTIVE_DATE,H.CHANGE_REASON_CODE,H.APPROVAL_REQUEST_ID,A.TRACKING_NO,A.APPROVAL_STATUS_CODE,A.APPROVER_USER_ID,H.MIGRATION_STATUS_CODE FROM "+schema+".DEPOSIT_ACCOUNT_PRODUCT_HISTORY H LEFT JOIN "+schema+".DEPOSIT_OPERATION_APPROVAL_REQUEST A ON A.APPROVAL_REQUEST_ID=H.APPROVAL_REQUEST_ID WHERE H.ACCOUNT_ID=:id ORDER BY H.ACCOUNT_PRODUCT_HISTORY_ID DESC FETCH FIRST 50 ROWS ONLY";
        return jdbc.query(sql,Map.of("id",accountId),(r,n)->new ProductChange(r.getLong(1),r.getLong(2),r.getLong(3),r.getLong(4),date(r.getDate(5)),r.getString(6),r.getObject(7,Long.class),r.getString(8),r.getString(9),r.getString(10),r.getString(11)));
    }

    public boolean activeParty(long accountId,long partyId){Integer v=jdbc.queryForObject("SELECT COUNT(*) FROM "+schema+".DEPOSIT_ACCOUNT_PARTY WHERE ACCOUNT_ID=:aid AND PARTY_ID=:pid AND STATUS_CODE='ACTIVE' AND (VALID_TO IS NULL OR VALID_TO>=TRUNC(SYSDATE))",new MapSqlParameterSource().addValue("aid",accountId).addValue("pid",partyId),Integer.class);return v!=null&&v>0;}
    public Optional<SignatoryLock> activeSignatory(long accountId,long partyId){
        String sql="SELECT SIGNATORY_ID,SIGNATORY_ROLE_CODE FROM "+schema+".DEPOSIT_ACCOUNT_SIGNATORY WHERE ACCOUNT_ID=:aid AND PARTY_ID=:pid AND STATUS_CODE='ACTIVE' AND (VALID_TO IS NULL OR VALID_TO>=TRUNC(SYSDATE)) ORDER BY SIGNATORY_ID DESC FETCH FIRST 1 ROWS ONLY";
        return jdbc.query(sql,new MapSqlParameterSource().addValue("aid",accountId).addValue("pid",partyId),(r,n)->new SignatoryLock(r.getLong(1),r.getString(2))).stream().findFirst();
    }
    public int nextSignatorySequence(long accountId){Integer v=jdbc.queryForObject("SELECT NVL(MAX(SIGNATURE_SEQUENCE_NO),0)+1 FROM "+schema+".DEPOSIT_ACCOUNT_SIGNATORY WHERE ACCOUNT_ID=:id",Map.of("id",accountId),Integer.class);return v==null?1:v;}
    public void insertSignatory(long id,long accountId,long partyId,String role,int sequence,boolean primary,LocalDate validFrom,String actor){
        jdbc.update("INSERT INTO "+schema+".DEPOSIT_ACCOUNT_SIGNATORY(SIGNATORY_ID,ACCOUNT_ID,PARTY_ID,SIGNATORY_ROLE_CODE,SIGNATURE_SEQUENCE_NO,IS_PRIMARY_SIGNATORY,VALID_FROM,STATUS_CODE,CREATED_AT,CREATED_BY,RECORD_VERSION) VALUES(:id,:aid,:pid,:role,:seq,:primary,:vf,'ACTIVE',SYSTIMESTAMP,:actor,1)",new MapSqlParameterSource().addValue("id",id).addValue("aid",accountId).addValue("pid",partyId).addValue("role",role).addValue("seq",sequence).addValue("primary",primary?1:0).addValue("vf",Date.valueOf(validFrom),Types.DATE).addValue("actor",actor));
    }
    public void insertAuthority(long id,long signatoryId,String operation,String channel,BigDecimal maxAmount,String currency,boolean cosign,LocalDate validFrom,String actor){
        jdbc.update("INSERT INTO "+schema+".DEPOSIT_ACCOUNT_SIGNATORY_AUTHORITY(SIGNATORY_AUTHORITY_ID,SIGNATORY_ID,OPERATION_CODE,CHANNEL_CODE,MAX_AMOUNT,CURRENCY_CODE,REQUIRES_COSIGN,VALID_FROM,CREATED_AT,CREATED_BY,RECORD_VERSION) VALUES(:id,:sid,:op,:channel,:max,:currency,:cosign,:vf,SYSTIMESTAMP,:actor,1)",new MapSqlParameterSource().addValue("id",id).addValue("sid",signatoryId).addValue("op",operation).addValue("channel",channel,Types.VARCHAR).addValue("max",maxAmount,Types.NUMERIC).addValue("currency",currency,Types.VARCHAR).addValue("cosign",cosign?1:0).addValue("vf",Date.valueOf(validFrom),Types.DATE).addValue("actor",actor));
    }
    public List<SignatoryAuthority> signatoryAuthorities(long accountId){
        String sql="SELECT AU.SIGNATORY_AUTHORITY_ID,S.SIGNATORY_ID,S.PARTY_ID,S.SIGNATORY_ROLE_CODE,AU.OPERATION_CODE,AU.CHANNEL_CODE,AU.MAX_AMOUNT,AU.CURRENCY_CODE,AU.REQUIRES_COSIGN,AU.VALID_FROM,AU.VALID_TO,S.STATUS_CODE FROM "+schema+".DEPOSIT_ACCOUNT_SIGNATORY S JOIN "+schema+".DEPOSIT_ACCOUNT_SIGNATORY_AUTHORITY AU ON AU.SIGNATORY_ID=S.SIGNATORY_ID WHERE S.ACCOUNT_ID=:id ORDER BY AU.SIGNATORY_AUTHORITY_ID DESC";
        return jdbc.query(sql,Map.of("id",accountId),(r,n)->new SignatoryAuthority(r.getLong(1),r.getLong(2),r.getLong(3),r.getString(4),r.getString(5),r.getString(6),r.getBigDecimal(7),r.getString(8),r.getInt(9)==1,date(r.getDate(10)),date(r.getDate(11)),r.getString(12)));
    }

    public long insertApproval(long approvalId,long accountId,String operation,String sourceType,long sourceId,String requestedBy,String approver,String orgUnit,String idem,String correlation,String actor){
        String tracking="APR-11J-"+approvalId;
        jdbc.update("INSERT INTO "+schema+".DEPOSIT_OPERATION_APPROVAL_REQUEST(APPROVAL_REQUEST_ID,TRACKING_NO,ACCOUNT_ID,OPERATION_TYPE_CODE,SOURCE_ENTITY_TYPE,SOURCE_ENTITY_ID,REQUESTED_BY_USER_ID,REQUESTED_AT,APPROVER_USER_ID,APPROVAL_STATUS_CODE,ORG_UNIT_CODE,IDEMPOTENCY_KEY,ORIGIN_SYSTEM_CODE,ORIGIN_MODULE_CODE,ORIGIN_REQUEST_REF,CORRELATION_ID,CREATED_AT,CREATED_BY,RECORD_VERSION) VALUES(:id,:tracking,:aid,:op,:stype,:sid,:requested,SYSTIMESTAMP,:approver,'PENDING_APPROVAL',:org,:idem,'CORE_BANKING','ACCOUNT_SERVICING',:idem,:corr,SYSTIMESTAMP,:actor,1)",new MapSqlParameterSource().addValue("id",approvalId).addValue("tracking",tracking).addValue("aid",accountId).addValue("op",operation).addValue("stype",sourceType).addValue("sid",sourceId).addValue("requested",requestedBy).addValue("approver",approver).addValue("org",orgUnit).addValue("idem",idem).addValue("corr",correlation,Types.VARCHAR).addValue("actor",actor));
        return approvalId;
    }
    public Optional<ApprovalLock> approval(long id,boolean lock){String sql="SELECT APPROVAL_REQUEST_ID,ACCOUNT_ID,APPROVER_USER_ID,APPROVAL_STATUS_CODE,RECORD_VERSION FROM "+schema+".DEPOSIT_OPERATION_APPROVAL_REQUEST WHERE APPROVAL_REQUEST_ID=:id"+(lock?" FOR UPDATE":"");return jdbc.query(sql,Map.of("id",id),(r,n)->new ApprovalLock(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),r.getLong(5))).stream().findFirst();}
    public int approve(long id,long version,String actor){return jdbc.update("UPDATE "+schema+".DEPOSIT_OPERATION_APPROVAL_REQUEST SET APPROVAL_STATUS_CODE='APPROVED',DECIDED_AT=SYSTIMESTAMP,UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1 WHERE APPROVAL_REQUEST_ID=:id AND RECORD_VERSION=:ver AND APPROVAL_STATUS_CODE='PENDING_APPROVAL' AND APPROVER_USER_ID=:actor",new MapSqlParameterSource().addValue("actor",actor).addValue("id",id).addValue("ver",version));}

    public long nextActivationRunNo(long accountId){Long v=jdbc.queryForObject("SELECT NVL(MAX(RUN_NO),0)+1 FROM "+schema+".DEPOSIT_ACTIVATION_RUN WHERE ACCOUNT_ID=:id",Map.of("id",accountId),Long.class);return v==null?1:v;}
    public void insertActivationRun(long id,long accountId,Long openingId,long runNo,String correlation,String trigger,String actor){
        jdbc.update("INSERT INTO "+schema+".DEPOSIT_ACTIVATION_RUN(ACTIVATION_RUN_ID,ACCOUNT_ID,OPENING_REQUEST_ID,RUN_NO,CORRELATION_ID,TRIGGER_CODE,RUN_STATUS_CODE,MANDATORY_CHECK_COUNT,PASSED_CHECK_COUNT,BLOCKING_CHECK_COUNT,READY_FLAG,REQUESTED_AT,CREATED_AT,CREATED_BY,UPDATED_AT,UPDATED_BY,RECORD_VERSION) VALUES(:id,:aid,:oid,:runNo,:corr,:trigger,'STARTED',0,0,0,0,SYSTIMESTAMP,SYSTIMESTAMP,:actor,SYSTIMESTAMP,:actor,1)",new MapSqlParameterSource().addValue("id",id).addValue("aid",accountId).addValue("oid",openingId,Types.NUMERIC).addValue("runNo",runNo).addValue("corr",correlation).addValue("trigger",trigger).addValue("actor",actor));
    }
    public void insertActivationCheck(long id,long runId,long accountId,String code,String type,String scope,boolean required,String result,String sourceSystem,String sourceRef,String reason,String details,OffsetDateTime evaluatedAt,OffsetDateTime validUntil,String actor){
        jdbc.update("INSERT INTO "+schema+".DEPOSIT_ACTIVATION_CHECK(ACTIVATION_CHECK_ID,ACTIVATION_RUN_ID,ACCOUNT_ID,REQUIREMENT_CODE,REQUIREMENT_TYPE_CODE,BLOCKING_SCOPE_CODE,REQUIRED_FLAG,RESULT_STATUS_CODE,SOURCE_SYSTEM_CODE,SOURCE_REFERENCE,RESULT_REASON_CODE,DETAILS,EVALUATED_AT,VALID_UNTIL,CREATED_AT,CREATED_BY,UPDATED_AT,UPDATED_BY,RECORD_VERSION) VALUES(:id,:run,:aid,:code,:type,:scope,:required,:result,:sourceSystem,:sourceRef,:reason,:details,:eval,:valid,SYSTIMESTAMP,:actor,SYSTIMESTAMP,:actor,1)",new MapSqlParameterSource().addValue("id",id).addValue("run",runId).addValue("aid",accountId).addValue("code",code).addValue("type",type).addValue("scope",scope).addValue("required",required?1:0).addValue("result",result).addValue("sourceSystem",sourceSystem).addValue("sourceRef",sourceRef,Types.VARCHAR).addValue("reason",reason,Types.VARCHAR).addValue("details",details,Types.VARCHAR).addValue("eval",ts(evaluatedAt),Types.TIMESTAMP).addValue("valid",ts(validUntil),Types.TIMESTAMP).addValue("actor",actor));
    }
    public void completeActivationRun(long id,long mandatory,long passed,long blocking,boolean ready,String actor){String status=ready?"READY":"BLOCKED";jdbc.update("UPDATE "+schema+".DEPOSIT_ACTIVATION_RUN SET RUN_STATUS_CODE=:status,MANDATORY_CHECK_COUNT=:mandatory,PASSED_CHECK_COUNT=:passed,BLOCKING_CHECK_COUNT=:blocking,READY_FLAG=:ready,EVALUATED_AT=SYSTIMESTAMP,UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1 WHERE ACTIVATION_RUN_ID=:id",new MapSqlParameterSource().addValue("status",status).addValue("mandatory",mandatory).addValue("passed",passed).addValue("blocking",blocking).addValue("ready",ready?1:0).addValue("actor",actor).addValue("id",id));}
    public ActivationRun latestActivationRun(long accountId){
        String sql="SELECT ACTIVATION_RUN_ID,ACCOUNT_ID,OPENING_REQUEST_ID,RUN_NO,CORRELATION_ID,TRIGGER_CODE,RUN_STATUS_CODE,MANDATORY_CHECK_COUNT,PASSED_CHECK_COUNT,BLOCKING_CHECK_COUNT,READY_FLAG,REQUESTED_AT,EVALUATED_AT FROM "+schema+".DEPOSIT_ACTIVATION_RUN WHERE ACCOUNT_ID=:id ORDER BY RUN_NO DESC FETCH FIRST 1 ROWS ONLY";
        var row=jdbc.query(sql,Map.of("id",accountId),(r,n)->new ActivationRunRow(r.getLong(1),r.getLong(2),r.getObject(3,Long.class),r.getLong(4),r.getString(5),r.getString(6),r.getString(7),r.getLong(8),r.getLong(9),r.getLong(10),r.getInt(11)==1,offset(r.getTimestamp(12)),offset(r.getTimestamp(13)))).stream().findFirst().orElse(null);
        if(row==null)return null;return new ActivationRun(row.id,row.accountId,row.openingId,row.runNo,row.correlation,row.trigger,row.status,row.mandatory,row.passed,row.blocking,row.ready,row.requestedAt,row.evaluatedAt,activationChecks(row.id));
    }
    public List<ActivationCheck> activationChecks(long runId){String sql="SELECT ACTIVATION_CHECK_ID,ACTIVATION_RUN_ID,ACCOUNT_ID,REQUIREMENT_CODE,REQUIREMENT_TYPE_CODE,BLOCKING_SCOPE_CODE,REQUIRED_FLAG,RESULT_STATUS_CODE,SOURCE_SYSTEM_CODE,SOURCE_REFERENCE,DETAILS,EVALUATED_AT,VALID_UNTIL FROM "+schema+".DEPOSIT_ACTIVATION_CHECK WHERE ACTIVATION_RUN_ID=:id ORDER BY ACTIVATION_CHECK_ID";return jdbc.query(sql,Map.of("id",runId),(r,n)->new ActivationCheck(r.getLong(1),r.getLong(2),r.getLong(3),r.getString(4),r.getString(5),r.getString(6),r.getInt(7)==1,r.getString(8),r.getString(9),r.getString(10),r.getString(11),offset(r.getTimestamp(12)),offset(r.getTimestamp(13))));}

    public void insertBulk(long id,String no,String action,String reason,int total,String actor,String correlation){jdbc.update("INSERT INTO "+schema+".DEPOSIT_ACCOUNT_BULK_ACTION(BULK_ACTION_ID,BULK_ACTION_NO,ACTION_TYPE_CODE,REASON_CODE,TOTAL_COUNT,SUCCESS_COUNT,FAILED_COUNT,ACTION_STATUS_CODE,REQUESTED_AT,ORIGIN_SYSTEM_CODE,ORIGIN_MODULE_CODE,ORIGIN_REQUEST_REF,CORRELATION_ID,CREATED_AT,CREATED_BY,RECORD_VERSION) VALUES(:id,:no,:action,:reason,:total,0,0,'READY',SYSTIMESTAMP,'CORE_BANKING','ACCOUNT_SERVICING',:no,:corr,SYSTIMESTAMP,:actor,1)",new MapSqlParameterSource().addValue("id",id).addValue("no",no).addValue("action",action).addValue("reason",reason,Types.VARCHAR).addValue("total",total).addValue("corr",correlation,Types.VARCHAR).addValue("actor",actor));}
    public long insertBulkItem(long id,long bulkId,long accountId,String actor){jdbc.update("INSERT INTO "+schema+".DEPOSIT_ACCOUNT_BULK_ACTION_ITEM(BULK_ACTION_ITEM_ID,BULK_ACTION_ID,ACCOUNT_ID,ITEM_STATUS_CODE,CREATED_AT,CREATED_BY,RECORD_VERSION) VALUES(:id,:bulk,:aid,'PENDING',SYSTIMESTAMP,:actor,1)",new MapSqlParameterSource().addValue("id",id).addValue("bulk",bulkId).addValue("aid",accountId).addValue("actor",actor));return id;}
    public void updateBulkItem(long itemId,String status,String errorCode,String errorMessage,String actor){jdbc.update("UPDATE "+schema+".DEPOSIT_ACCOUNT_BULK_ACTION_ITEM SET ITEM_STATUS_CODE=:status,ERROR_CODE=:code,ERROR_MESSAGE=:msg,EXECUTED_AT=SYSTIMESTAMP,UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1 WHERE BULK_ACTION_ITEM_ID=:id",new MapSqlParameterSource().addValue("status",status).addValue("code",errorCode,Types.VARCHAR).addValue("msg",errorMessage,Types.VARCHAR).addValue("actor",actor).addValue("id",itemId));}
    public void completeBulk(long bulkId,long success,long failed,String actor){String status=failed==0?"COMPLETED":success==0?"FAILED":"PARTIAL";jdbc.update("UPDATE "+schema+".DEPOSIT_ACCOUNT_BULK_ACTION SET SUCCESS_COUNT=:success,FAILED_COUNT=:failed,ACTION_STATUS_CODE=:status,UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1 WHERE BULK_ACTION_ID=:id",new MapSqlParameterSource().addValue("success",success).addValue("failed",failed).addValue("status",status).addValue("actor",actor).addValue("id",bulkId));}
    public BulkAction bulk(long bulkId){String sql="SELECT BULK_ACTION_ID,BULK_ACTION_NO,ACTION_TYPE_CODE,REASON_CODE,TOTAL_COUNT,SUCCESS_COUNT,FAILED_COUNT,ACTION_STATUS_CODE,REQUESTED_AT FROM "+schema+".DEPOSIT_ACCOUNT_BULK_ACTION WHERE BULK_ACTION_ID=:id";return jdbc.query(sql,Map.of("id",bulkId),(r,n)->new BulkAction(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getLong(5),r.getLong(6),r.getLong(7),r.getString(8),offset(r.getTimestamp(9)),bulkItems(r.getLong(1)))).stream().findFirst().orElse(null);}
    public List<BulkAction> recentBulks(long accountId){String sql="SELECT DISTINCT B.BULK_ACTION_ID,B.BULK_ACTION_NO,B.ACTION_TYPE_CODE,B.REASON_CODE,B.TOTAL_COUNT,B.SUCCESS_COUNT,B.FAILED_COUNT,B.ACTION_STATUS_CODE,B.REQUESTED_AT FROM "+schema+".DEPOSIT_ACCOUNT_BULK_ACTION B JOIN "+schema+".DEPOSIT_ACCOUNT_BULK_ACTION_ITEM I ON I.BULK_ACTION_ID=B.BULK_ACTION_ID WHERE I.ACCOUNT_ID=:id ORDER BY B.BULK_ACTION_ID DESC FETCH FIRST 10 ROWS ONLY";return jdbc.query(sql,Map.of("id",accountId),(r,n)->new BulkAction(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getLong(5),r.getLong(6),r.getLong(7),r.getString(8),offset(r.getTimestamp(9)),bulkItems(r.getLong(1))));}
    public List<BulkActionItem> bulkItems(long bulkId){String sql="SELECT BULK_ACTION_ITEM_ID,BULK_ACTION_ID,ACCOUNT_ID,ITEM_STATUS_CODE,ERROR_CODE,ERROR_MESSAGE,EXECUTED_AT FROM "+schema+".DEPOSIT_ACCOUNT_BULK_ACTION_ITEM WHERE BULK_ACTION_ID=:id ORDER BY BULK_ACTION_ITEM_ID";return jdbc.query(sql,Map.of("id",bulkId),(r,n)->new BulkActionItem(r.getLong(1),r.getLong(2),r.getLong(3),r.getString(4),r.getString(5),r.getString(6),offset(r.getTimestamp(7))));}

    public Optional<IdemRow> idempotency(String key){String sql="SELECT IDEMPOTENCY_KEY,ACCOUNT_ID,OPERATION_TYPE_CODE,PAYLOAD_HASH,PROCESSING_STATUS_CODE,RESULT_REFERENCE FROM "+schema+".DEPOSIT_OPERATION_IDEMPOTENCY WHERE IDEMPOTENCY_KEY=:key";return jdbc.query(sql,Map.of("key",key),(r,n)->new IdemRow(r.getString(1),r.getObject(2,Long.class),r.getString(3),r.getString(4),r.getString(5),r.getString(6))).stream().findFirst();}
    public void claimIdempotency(long id,String key,Long accountId,String operation,String hash,String actor,String correlation){jdbc.update("INSERT INTO "+schema+".DEPOSIT_OPERATION_IDEMPOTENCY(OPERATION_IDEMPOTENCY_ID,IDEMPOTENCY_KEY,ACCOUNT_ID,OPERATION_TYPE_CODE,PAYLOAD_HASH,PROCESSING_STATUS_CODE,REQUESTED_BY_USER_ID,REQUESTED_AT,ORIGIN_SYSTEM_CODE,ORIGIN_MODULE_CODE,ORIGIN_REQUEST_REF,CORRELATION_ID,RECORD_VERSION) VALUES(:id,:key,:aid,:op,:hash,'IN_PROGRESS',:actor,SYSTIMESTAMP,'CORE_BANKING','ACCOUNT_SERVICING',:key,:corr,1)",new MapSqlParameterSource().addValue("id",id).addValue("key",key).addValue("aid",accountId,Types.NUMERIC).addValue("op",operation).addValue("hash",hash).addValue("actor",actor).addValue("corr",correlation,Types.VARCHAR));}
    public void completeIdempotency(String key,String result){jdbc.update("UPDATE "+schema+".DEPOSIT_OPERATION_IDEMPOTENCY SET PROCESSING_STATUS_CODE='COMPLETED',RESULT_REFERENCE=:result,COMPLETED_AT=SYSTIMESTAMP,RECORD_VERSION=RECORD_VERSION+1 WHERE IDEMPOTENCY_KEY=:key",new MapSqlParameterSource().addValue("result",result).addValue("key",key));}

    public record AccountContext(long accountId,Long openingRequestId,String status,String currencyCode,long currentProductVersionId,long recordVersion) {}
    public record AttributeLock(long id,String value,String valueType,LocalDate validFrom,long recordVersion) {}
    public record ConditionLock(long conditionId,Long approvalRequestId) {}
    public record ProductChangeLock(long historyId,long accountId,long fromPv,long toPv,LocalDate effectiveDate,Long approvalRequestId,String migrationStatus,long recordVersion,String approvalStatus,String approver) {}
    public record SignatoryLock(long signatoryId,String role) {}
    public record ApprovalLock(long approvalId,long accountId,String approver,String status,long recordVersion) {}
    public record IdemRow(String key,Long accountId,String operationType,String payloadHash,String status,String resultReference) {}
    private record ActivationRunRow(long id,long accountId,Long openingId,long runNo,String correlation,String trigger,String status,long mandatory,long passed,long blocking,boolean ready,OffsetDateTime requestedAt,OffsetDateTime evaluatedAt) {}

    private static String id(String value){if(value==null||!value.matches("[A-Za-z0-9_]+"))throw new IllegalArgumentException("Oracle identifier نامعتبر است: "+value);return value.toUpperCase(Locale.ROOT);}
    private static LocalDate date(Date d){return d==null?null:d.toLocalDate();}
    private static OffsetDateTime offset(Timestamp ts){return ts==null?null:ts.toInstant().atOffset(ZoneOffset.UTC);}
    private static Timestamp ts(OffsetDateTime value){return value==null?null:Timestamp.from(value.toInstant());}
}
