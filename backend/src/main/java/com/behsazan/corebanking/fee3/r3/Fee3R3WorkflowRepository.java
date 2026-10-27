package com.behsazan.corebanking.fee3.r3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Repository;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;
import java.util.Optional;

/** Uses the existing FCE_CHANGE_REQUEST and FCE_AUDIT_EVENT tables; no DDL. */
@Repository
public class Fee3R3WorkflowRepository {
    private final JdbcClient db;
    private final String schema;
    public Fee3R3WorkflowRepository(JdbcClient db,@Value("${FEE3_SCHEMA:FEE3}") String schema) {
        if(schema==null||!schema.matches("[A-Za-z][A-Za-z0-9_]{0,29}"))
            throw new IllegalArgumentException("Invalid fee3 schema");
        this.db=db;this.schema=schema.toUpperCase(java.util.Locale.ROOT);
    }
    private String t(String name){return schema+"."+name;}
    public record RequestInfo(long requestId,String requestNo,long calcItemId,long proposedVersionId,
                              String status,String requestedBy,String decidedBy,long recordVersion) {}
    public List<RequestInfo> recent(long itemId){
        return db.sql("SELECT CHANGE_REQUEST_ID,REQUEST_NO,CALC_ITEM_ID,PROPOSED_VERSION_ID,"+
                "REQUEST_STATUS_CODE,REQUESTED_BY,DECIDED_BY,RECORD_VERSION FROM "+t("FCE_CHANGE_REQUEST")+
                " WHERE CALC_ITEM_ID=:iid AND REQUEST_NO LIKE 'F3-UAT-CR-%'"+
                " ORDER BY CHANGE_REQUEST_ID DESC FETCH FIRST 100 ROWS ONLY")
            .param("iid",itemId).query((rs,n)->new RequestInfo(rs.getLong(1),rs.getString(2),
                rs.getLong(3),rs.getLong(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getLong(8))).list();
    }
    public Optional<RequestInfo> lookup(long id){
        return db.sql("SELECT CHANGE_REQUEST_ID,REQUEST_NO,CALC_ITEM_ID,PROPOSED_VERSION_ID,"+
                "REQUEST_STATUS_CODE,REQUESTED_BY,DECIDED_BY,RECORD_VERSION FROM "+t("FCE_CHANGE_REQUEST")+
                " WHERE CHANGE_REQUEST_ID=:id AND REQUEST_NO LIKE 'F3-UAT-CR-%'")
            .param("id",id).query((rs,n)->new RequestInfo(rs.getLong(1),rs.getString(2),
                rs.getLong(3),rs.getLong(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getLong(8))).optional();
    }
    public Optional<RequestInfo> lock(long id){
        return db.sql("SELECT CHANGE_REQUEST_ID,REQUEST_NO,CALC_ITEM_ID,PROPOSED_VERSION_ID,"+
                "REQUEST_STATUS_CODE,REQUESTED_BY,DECIDED_BY,RECORD_VERSION FROM "+t("FCE_CHANGE_REQUEST")+
                " WHERE CHANGE_REQUEST_ID=:id AND REQUEST_NO LIKE 'F3-UAT-CR-%' FOR UPDATE")
            .param("id",id).query((rs,n)->new RequestInfo(rs.getLong(1),rs.getString(2),
                rs.getLong(3),rs.getLong(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getLong(8))).optional();
    }
    public long openCount(long itemId){
        return db.sql("SELECT COUNT(*) FROM "+t("FCE_CHANGE_REQUEST")+
           " WHERE CALC_ITEM_ID=:iid AND REQUEST_STATUS_CODE IN ('DRAFT','SUBMITTED')")
           .param("iid",itemId).query(Long.class).single();
    }
    public long scopeCount(long versionId){
        return db.sql("SELECT COUNT(*) FROM "+t("FCE_VERSION_SCOPE")+" WHERE TARIFF_VERSION_ID=:vid")
            .param("vid",versionId).query(Long.class).single();
    }
    public long ruleCount(long versionId){
        return db.sql("SELECT COUNT(*) FROM "+t("FCE_CALC_RULE")+" WHERE TARIFF_VERSION_ID=:vid")
            .param("vid",versionId).query(Long.class).single();
    }
    public long testCount(long versionId){
        return db.sql("SELECT COUNT(*) FROM "+t("FCE_TEST_CASE")+" WHERE TARIFF_VERSION_ID=:vid")
            .param("vid",versionId).query(Long.class).single();
    }
    public boolean allTestsCertified(long versionId){
        Long bad=db.sql("SELECT COUNT(*) FROM "+t("FCE_TEST_CASE")+
            " WHERE TARIFF_VERSION_ID=:vid AND (LAST_STATUS_CODE IS NULL OR LAST_STATUS_CODE <> 'PASS' OR TESTED_AT IS NULL)")
            .param("vid",versionId).query(Long.class).single();
        return testCount(versionId)>0&&bad==0L;
    }
    public RequestInfo submit(long itemId,long versionId,long expectedVersionRv,String actor,String reason){
        long id=db.sql("SELECT "+schema+".SEQ_FCE_CHANGE_REQUEST.NEXTVAL FROM DUAL")
            .query(Long.class).single();
        String requestNo="F3-UAT-CR-"+id;
        String json="{\"contractVersion\":1,\"purpose\":\"UAT_REVIEW_ONLY\",\"published\":false}";
        int changed=db.sql("UPDATE "+t("FCE_TARIFF_VERSION")+
          " SET STATUS_CODE='REVIEW',UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1"+
          " WHERE TARIFF_VERSION_ID=:vid AND CALC_ITEM_ID=:iid AND STATUS_CODE='DRAFT' AND IS_ACTIVE=0"+
          " AND RECORD_VERSION=:rv")
          .param("actor",actor).param("vid",versionId).param("iid",itemId).param("rv",expectedVersionRv).update();
        if(changed!=1)throw new IllegalStateException("FEE3_SUBMIT_CONCURRENT_CHANGE");
        db.sql("INSERT INTO "+t("FCE_CHANGE_REQUEST")+
            " (CHANGE_REQUEST_ID,REQUEST_NO,CALC_ITEM_ID,PROPOSED_VERSION_ID,CHANGE_TYPE_CODE,"+
            "REQUEST_STATUS_CODE,CHANGE_REASON,CHANGE_PAYLOAD_JSON,REQUESTED_BY,REQUESTED_AT,CREATED_BY)"+
            " VALUES (:id,:no,:iid,:vid,'NEW_VERSION','SUBMITTED',:reason,:payload,:actor,SYSTIMESTAMP,:actor)")
          .param("id",id).param("no",requestNo).param("iid",itemId).param("vid",versionId)
          .param("reason",reason).param("payload",new SqlParameterValue(Types.CLOB,json))
          .param("actor",actor).update();
        audit(itemId,versionId,id,"UAT_REVIEW_SUBMITTED",actor,"DRAFT","REVIEW",reason);
        return new RequestInfo(id,requestNo,itemId,versionId,"SUBMITTED",actor,null,1);
    }
    public RequestInfo decide(RequestInfo info,String actor,String decision,String reason){
        String requestStatus="APPROVE".equals(decision)?"APPROVED":"REJECTED";
        String tariffStatus="APPROVE".equals(decision)?"APPROVED":"DRAFT";
        int versionUpdated=db.sql("UPDATE "+t("FCE_TARIFF_VERSION")+
           " SET STATUS_CODE=:status,APPROVED_BY=CASE WHEN :status='APPROVED' THEN :actor ELSE NULL END,"+
           "APPROVED_AT=CASE WHEN :status='APPROVED' THEN SYSTIMESTAMP ELSE NULL END,"+
           "UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1"+
           " WHERE TARIFF_VERSION_ID=:vid AND CALC_ITEM_ID=:iid AND STATUS_CODE='REVIEW' AND IS_ACTIVE=0")
           .param("status",tariffStatus).param("actor",actor).param("vid",info.proposedVersionId())
           .param("iid",info.calcItemId()).update();
        int requestUpdated=db.sql("UPDATE "+t("FCE_CHANGE_REQUEST")+
           " SET REQUEST_STATUS_CODE=:status,DECIDED_BY=:actor,DECIDED_AT=SYSTIMESTAMP,"+
           " UPDATED_BY=:actor,UPDATED_AT=SYSTIMESTAMP,RECORD_VERSION=RECORD_VERSION+1"+
           " WHERE CHANGE_REQUEST_ID=:id AND REQUEST_STATUS_CODE='SUBMITTED' AND RECORD_VERSION=:rv")
           .param("status",requestStatus).param("actor",actor).param("id",info.requestId())
           .param("rv",info.recordVersion()).update();
        if(versionUpdated!=1||requestUpdated!=1)throw new IllegalStateException("FEE3_DECISION_CONCURRENT_CHANGE");
        audit(info.calcItemId(),info.proposedVersionId(),info.requestId(),
           "APPROVE".equals(decision)?"UAT_REVIEW_APPROVED":"UAT_REVIEW_REJECTED",actor,"REVIEW",tariffStatus,reason);
        return new RequestInfo(info.requestId(),info.requestNo(),info.calcItemId(),
            info.proposedVersionId(),requestStatus,info.requestedBy(),actor,info.recordVersion()+1);
    }
    private void audit(long itemId,long versionId,long changeId,String action,String actor,String before,String after,String reason){
        long id=db.sql("SELECT "+schema+".SEQ_FCE_AUDIT_EVENT.NEXTVAL FROM DUAL").query(Long.class).single();
        String detail=new JsonMapper().writeValueAsString(Map.of(
           "purpose","FEE3_UAT_GOVERNANCE","bankPosting",false,"published",false,"decisionReason",reason));
        String stateBefore="{\"status\":\""+before+"\"}";
        String stateAfter="{\"status\":\""+after+"\"}";
        db.sql("INSERT INTO "+t("FCE_AUDIT_EVENT")+
           " (AUDIT_EVENT_ID,CALC_ITEM_ID,TARIFF_VERSION_ID,CHANGE_REQUEST_ID,ACTION_CODE,ACTOR_REF,"+
           "EVENT_AT,DETAILS_JSON,BEFORE_JSON,AFTER_JSON,CREATED_BY) VALUES"+
           " (:id,:iid,:vid,:crid,:action,:actor,SYSTIMESTAMP,:detail,:before,:after,:actor)")
         .param("id",id).param("iid",itemId).param("vid",versionId).param("crid",changeId)
         .param("action",action).param("actor",actor)
         .param("detail",new SqlParameterValue(Types.CLOB,detail))
         .param("before",new SqlParameterValue(Types.CLOB,stateBefore))
         .param("after",new SqlParameterValue(Types.CLOB,stateAfter)).update();
    }
}
