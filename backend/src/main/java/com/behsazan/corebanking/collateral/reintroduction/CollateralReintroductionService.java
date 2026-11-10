package com.behsazan.corebanking.collateral.reintroduction;

import com.behsazan.corebanking.collateral.reintroduction.CollateralReintroductionModels.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionModels.AttributeInput;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionPolicy;
import tools.jackson.databind.json.JsonMapper;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * BUC-12/BR-02/04/14. Uses the application's shared Oracle DataSource, never a
 * separate starter. A verified identity and independent bank permit are required.
 * The released registration and its historical rows are NEVER changed.
 * All final DML is disabled by default and prohibited for local prototype actors.
 */
@Service
public final class CollateralReintroductionService {
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final Environment config;
    private final JsonMapper json;
    public CollateralReintroductionService(DataSource dataSource,Environment config,JsonMapper json) {
        db=new JdbcTemplate(dataSource);
        tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        tx.setTimeout(25);
        this.config=config;
        this.json=json;
    }
    private record Actor(String id,String branch) {}
    private record Old(String id,String registration,String branch,String status,
            String version,String partyId,String typeId) {}
    private record Asset(String id,String typeId,String scheme,String verification,String sourceReference,
            boolean verified,boolean previousCurrent,long otherCurrent) {}
    private record Reviewed(String reviewId,String draftId,String status,String branch,String assetReference,
            String partyId,String typeId,String currency,String estimate,
            java.sql.Date validFrom,java.sql.Date validTo,String deposit,String remarks,
            String reviewer,String maker,String reviewHash,String draftHash,String draftStatus,
            String draftVersion,boolean typeActive,boolean currencyActive,
            boolean multiplicityType,boolean countabilityConfigured,boolean countable,
            int rangeCount,boolean rangeRowsValid) {}
    private record SerialRow(int ordinal,String first,String last,BigDecimal amount) {}
    private record AttributeDefinition(String id,String dataType,boolean required,
            boolean repeatable,boolean manualEntryAllowed,String listValue) {}
    private record AttributeValue(String id,String dataType,String value,int ordinal) {}
    private record Permit(String id,String sourceId,String sourceVersion,String assetId,
            String reviewId,String approvedPartyId,String approvedTypeId,String number,
            String numberScheme,String initialStatus,int multiplicity,int usageCondition,
            int usageTerm,String approvalRef,String ownershipRef,String identityRef,
            String requirementsRef,String serialRef,String maker,String checker,
            String status,LocalDateTime expires,String key,String poster,
            String postedId,String postedNumber,LocalDateTime postedAt) {}
    private static String decimal(BigDecimal value){return value==null?null:value.toPlainString();}
    private static BigDecimal number(String raw){return new BigDecimal(raw);}
    private static LocalDateTime date(Timestamp value){return value==null?null:value.toLocalDateTime();}
    private static ResponseStatusException stop(HttpStatus status,String code){return new ResponseStatusException(status,code);}
    private Actor actor(Authentication auth,boolean posting) {
        if (!(auth instanceof JwtAuthenticationToken token) || !token.isAuthenticated())
            throw stop(HttpStatus.UNAUTHORIZED,"REINTRO_SIGNED_PRINCIPAL_REQUIRED");
        if (posting && (token.getToken().getClaims().containsKey("clt_local_prototype")
                || "CLT_UNIFIED_LOCAL_DEV".equals(token.getToken().getSubject())))
            throw stop(HttpStatus.FORBIDDEN,"REINTRO_PROTOTYPE_POSTING_DENIED");
        String required=posting?"SCOPE_clt.collateral.review":"SCOPE_clt.collateral.read";
        if(auth.getAuthorities().stream().noneMatch(a->required.equals(a.getAuthority())))
            throw stop(HttpStatus.FORBIDDEN,"REINTRO_SCOPE_REQUIRED");
        if(posting && auth.getAuthorities().stream()
                .noneMatch(a->"SCOPE_clt.collateral.reintroduce.post".equals(a.getAuthority())))
            throw stop(HttpStatus.FORBIDDEN,"REINTRO_EXPLICIT_POSTING_SCOPE_REQUIRED");
        Object raw=token.getToken().getClaims().get("branch_code");
        if(!(raw instanceof String branch) || !branch.matches("[A-Za-z0-9_-]{1,30}"))
            throw stop(HttpStatus.FORBIDDEN,"REINTRO_BRANCH_REQUIRED");
        return new Actor(CollateralReintroductionPolicy.subject(token.getToken().getSubject()),branch);
    }
    private boolean postingFlag(){return config.getProperty("core-banking.collateral.reintroduction.posting-enabled",Boolean.class,false);}
    private boolean prototype(){return config.getProperty("core-banking.collateral.prototype.enabled",Boolean.class,false)
            || config.getProperty("CLT_LOCAL_UAT_READONLY_ENABLED",Boolean.class,false);}
    private String initialStatus(){return config.getProperty("core-banking.collateral.reintroduction.initial-status-code","").strip();}
    private boolean statusesConfigured(){return initialStatus().matches("[A-Z][A-Z0-9_]{0,29}");}
    private boolean columns(String table,String... columns) {
        StringBuilder sql=new StringBuilder("SELECT COUNT(DISTINCT COLUMN_NAME) FROM ALL_TAB_COLUMNS WHERE OWNER='CLT' AND TABLE_NAME=? AND COLUMN_NAME IN (");
        Object[] params=new Object[columns.length+1];params[0]=table;
        for(int i=0;i<columns.length;i++){if(i>0)sql.append(',');sql.append('?');params[i+1]=columns[i];}
        sql.append(')');
        Integer found=db.queryForObject(sql.toString(),Integer.class,params);
        return found!=null && found==columns.length;
    }
    private boolean identityReady(){
        try {
            return columns("CLT_ASSET_IDENTITY","ASSET_ID","COLLATERAL_TYPE_ID","IDENTITY_SCHEME_CODE",
                        "VERIFICATION_EVIDENCE","IDENTITY_STATUS")
                    && columns("CLT_ASSET_COLLATERAL_LINK","ASSET_COLLATERAL_LINK_ID","ASSET_ID",
                        "COLLATERAL_ID","PREVIOUS_COLLATERAL_ID","IS_CURRENT_FLAG","LINKED_AT","LINKED_BY")
                    && columns("CLT_REGISTRATION_LEDGER","REGISTRATION_ID","REVIEW_ID","DRAFT_ID",
                        "COLLATERAL_ID","COLLATERAL_NUMBER","DRAFT_HASH","IDENTITY_SCHEME_CODE",
                        "NUMBER_SCHEME_CODE","BANK_APPROVAL_REFERENCE","REGISTRATION_STATUS","BRANCH_CODE")
                    && columns("CLT_INTRODUCTION_DRAFT","DRAFT_ID","REQUEST_HASH","PARTY_ID",
                        "COLLATERAL_TYPE_ID","ASSET_REFERENCE","ITEM_RANGES_JSON","ATTRIBUTES_JSON","VALID_FROM","VALID_TO")
                    && columns("CLT_INTRODUCTION_REVIEW","REVIEW_ID","REVIEW_STATUS","DRAFT_HASH","CHECKER_SUBJECT")
                    && columns("COLLATERAL_ATTRIBUTE_DEFINITION_ITEM","COLLATERAL_ID",
                        "COLLATERAL_ATTRIBUTE_DEFINITION_ID","NUMERIC_VALUE","TEXT_VALUE",
                        "DATE_VALUE","VALUE_SEQUENCE_NO","CREATED_BY")
                    && sequence("SEQ_CLT_ASSET_COLLATERAL_LINK") && sequence("SEQ_CLT_REGISTRATION_LEDGER")
                    && enabledUniqueIndex("UQ_CLT_ASSET_ONE_CURRENT")
                    && enabledConstraint("CLT_ASSET_COLLATERAL_LINK","UQ_CLT_LINK_COLLATERAL")
                    && enabledConstraint("CLT_REGISTRATION_LEDGER","UQ_CLT_REG_NUMBER")
                    && countabilityReady();
        } catch(DataAccessException ex){return false;}
    }
    private boolean permitReady(){
        try {
            return columns("CLT_REINTRODUCTION_PERMIT","REINTRODUCTION_PERMIT_ID",
                "PREVIOUS_COLLATERAL_ID","BASE_RECORD_VERSION","ASSET_ID","REVIEW_ID",
                "APPROVED_PARTY_ID","APPROVED_TYPE_ID","APPROVED_REGISTRATION_NUMBER",
                "NUMBER_SCHEME_CODE","INITIAL_STATUS_CODE","MULTIPLICITY","USAGE_CONDITION",
                "USAGE_TERM","BANK_APPROVAL_REFERENCE","OWNER_REVERIFICATION_REFERENCE",
                "IDENTITY_VERIFICATION_REFERENCE","REQUIREMENTS_ATTESTATION_REFERENCE",
                "SERIAL_ATTESTATION_REFERENCE","MAKER_SUBJECT","CHECKER_SUBJECT",
                "PERMIT_STATUS","VALID_UNTIL","IDEMPOTENCY_KEY","POSTER_SUBJECT",
                "POSTED_COLLATERAL_ID","POSTED_COLLATERAL_NUMBER","POSTED_AT")
              && enabledConstraint("CLT_REINTRODUCTION_PERMIT","UQ_CLT_REINTRO_NUMBER")
              && enabledConstraint("CLT_REINTRODUCTION_PERMIT","UQ_CLT_REINTRO_REVIEW")
              && enabledUniqueIndex("UQ_CLT_REINTRO_ONE_POSTED");
        }catch(DataAccessException ex){return false;}
    }
    private boolean numbersReady(){
        try{return columns("CLT_REGISTRATION_NUMBER_AUTHORITY","COLLATERAL_NUMBER",
                "SCHEME_CODE","APPROVAL_REFERENCE","ISSUE_STATUS","ISSUED_TO_REVIEW_ID","ISSUED_AT")
                && enabledConstraint("CLT_REGISTRATION_NUMBER_AUTHORITY","UQ_CLT_NUMBER_ISSUED_TO");}
        catch(DataAccessException e){return false;}
    }
    private boolean rangesReady(){
        try{return columns("CLT_COLLATERAL_SERIAL_RANGE","COLLATERAL_SERIAL_RANGE_ID",
                "COLLATERAL_ID","RANGE_SEQUENCE_NO","SERIAL_FROM","SERIAL_TO",
                "ITEM_COUNT","RANGE_AMOUNT","CURRENCY_CODE")
                && columns("CLT_INTRODUCTION_DRAFT_SERIAL_RANGE","DRAFT_ID","ROW_NO",
                "SERIAL_FROM","SERIAL_TO","TOTAL_AMOUNT_IRR")
                && sequence("SEQ_CLT_COLLATERAL_SERIAL_RANGE");}
        catch(DataAccessException e){return false;}
    }
    private boolean countabilityReady(){
        try{return columns("COLLATERAL_TYPE","COLLATERAL_TYPE_ID","HAS_MULTIPLICITY","IS_COUNTABLE_FLAG");}
        catch(DataAccessException e){return false;}
    }
    private boolean sequence(String name){
        Integer count=db.queryForObject("SELECT COUNT(*) FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER='CLT' AND SEQUENCE_NAME=?",Integer.class,name);
        return Integer.valueOf(1).equals(count);
    }
    private boolean enabledUniqueIndex(String name){
        Integer n=db.queryForObject("SELECT COUNT(*) FROM ALL_INDEXES WHERE OWNER='CLT' AND INDEX_NAME=? AND UNIQUENESS='UNIQUE' AND STATUS='VALID'",Integer.class,name);
        return Integer.valueOf(1).equals(n);
    }
    private boolean enabledConstraint(String table,String name){
        Integer n=db.queryForObject("SELECT COUNT(*) FROM ALL_CONSTRAINTS WHERE OWNER='CLT' AND TABLE_NAME=? AND CONSTRAINT_NAME=? AND STATUS='ENABLED' AND VALIDATED='VALIDATED'",Integer.class,table,name);
        return Integer.valueOf(1).equals(n);
    }
    public Readiness readiness(Authentication auth){
        actor(auth,false);
        boolean identity=identityReady(),permit=permitReady(),numbers=numbersReady();
        boolean ready=postingFlag()&&!prototype()&&identity&&permit&&numbers&&statusesConfigured();
        return new Readiness(true,identity,permit,numbers,ready,prototype(),
                statusesConfigured()?initialStatus():"UNCONFIGURED",
                ready?"BANK_APPROVED_PERMIT_AND_RUNTIME_ORACLE_EVIDENCE_REQUIRED":
                      "BUC12_BANK_SCHEMA_STATUS_OR_PERMISSION_NOT_READY");
    }
    private void assertPostingReady(){
        if(prototype())throw stop(HttpStatus.FORBIDDEN,"REINTRO_PROTOTYPE_FINAL_WRITE_BLOCKED");
        if(!postingFlag())throw stop(HttpStatus.SERVICE_UNAVAILABLE,"REINTRO_POSTING_DISABLED");
        if(!statusesConfigured())throw stop(HttpStatus.SERVICE_UNAVAILABLE,"REINTRO_APPROVED_INITIAL_STATUS_REQUIRED");
        if(!identityReady()||!permitReady()||!numbersReady())
            throw stop(HttpStatus.SERVICE_UNAVAILABLE,"REINTRO_APPROVED_ORACLE_MIGRATION_REQUIRED");
    }
    private Old previous(String rawId,String branch,boolean lock){
        List<Old> rows=db.query("""
                SELECT COLLATERAL_ID,COLLATERAL_NUMBER,BRANCH_CODE,COLLATERAL_STATUS_CODE,
                       RECORD_VERSION,PARTY_ID,COLLATERAL_TYPE_ID
                  FROM CLT.COLLATERAL
                 WHERE COLLATERAL_ID=? AND BRANCH_CODE=?
                """+(lock?" FOR UPDATE OF RECORD_VERSION":""),
                (r,i)->new Old(decimal(r.getBigDecimal(1)),decimal(r.getBigDecimal(2)),
                     r.getString(3),r.getString(4),decimal(r.getBigDecimal(5)),
                     decimal(r.getBigDecimal(6)),decimal(r.getBigDecimal(7))),number(rawId),branch);
        if(rows.size()!=1)throw stop(HttpStatus.NOT_FOUND,"REINTRO_COLLATERAL_NOT_FOUND");
        return rows.getFirst();
    }
    /** BUC-15 interlock: an older BUC-11 permit is insufficient if a pending or
     *  approved cross-branch grant still survives. No new grant may be inferred
     *  from an HTTP request, and a partial cross-unit schema fails closed. */
    private boolean crossUnitCleared(Old old){
        Integer count=db.queryForObject("""
                SELECT COUNT(*) FROM ALL_TABLES
                 WHERE OWNER='CLT' AND TABLE_NAME='CLT_CROSS_UNIT_GRANT'
                """,Integer.class);
        if(Integer.valueOf(0).equals(count))return true;
        if(!Integer.valueOf(1).equals(count))return false;
        Integer cols=db.queryForObject("""
                SELECT COUNT(DISTINCT COLUMN_NAME) FROM ALL_TAB_COLUMNS
                 WHERE OWNER='CLT' AND TABLE_NAME='CLT_CROSS_UNIT_GRANT'
                   AND COLUMN_NAME IN ('COLLATERAL_ID','OWNER_BRANCH','GRANT_STATUS')
                """,Integer.class);
        if(!Integer.valueOf(3).equals(cols))return false;
        Long unresolved=db.queryForObject("""
                SELECT COUNT(*) FROM CLT.CLT_CROSS_UNIT_GRANT
                 WHERE COLLATERAL_ID=? AND OWNER_BRANCH=?
                   AND GRANT_STATUS IN ('PENDING','APPROVED')
                """,Long.class,number(old.id()),old.branch());
        return Long.valueOf(0).equals(unresolved);
    }
    private boolean released(Old old){
        // BUC-11 evidence: real posted permit + last immutable status event; state alone is not enough.
        Integer posted=db.queryForObject("""
                SELECT COUNT(*) FROM CLT.CLT_FINAL_RELEASE_PERMIT
                 WHERE COLLATERAL_ID=? AND BRANCH_CODE=? AND PERMIT_STATUS='POSTED'
                   AND POSTED_RECORD_VERSION=? AND TO_STATUS_CODE=? AND STATUS_HISTORY_ID IS NOT NULL
                """,Integer.class,number(old.id()),old.branch(),number(old.version()),old.status());
        return Integer.valueOf(1).equals(posted) && crossUnitCleared(old);
    }
    private boolean historyReleased(Old old){
        List<String> rows=db.query("""
                SELECT SOURCE_EVENT_CODE FROM CLT.COLLATERAL_STATUS_HISTORY
                 WHERE COLLATERAL_ID=? AND TO_STATUS_CODE=?
                 ORDER BY STATUS_SEQUENCE_NO DESC FETCH FIRST 1 ROWS ONLY
                """,(r,i)->r.getString(1),number(old.id()),old.status());
        return rows.size()==1 && "BUC11_FINAL_RELEASE".equals(rows.getFirst());
    }
    private boolean terminal(String status){
        Integer c=db.queryForObject("""
                SELECT COUNT(*) FROM CLT.REF_COLLATERAL_STATUS
                 WHERE COLLATERAL_STATUS_CODE=? AND STATUS='ACTIVE'
                   AND IS_TERMINAL_STATUS_FLAG='Y' AND IS_ACTIVE_STATUS_FLAG='N'
                """,Integer.class,status);
        return Integer.valueOf(1).equals(c);
    }
    private boolean initialApproved(String status){
        Integer c=db.queryForObject("""
                SELECT COUNT(*) FROM CLT.REF_COLLATERAL_STATUS
                 WHERE COLLATERAL_STATUS_CODE=? AND STATUS='ACTIVE'
                   AND IS_TERMINAL_STATUS_FLAG='N' AND IS_ACTIVE_STATUS_FLAG='Y'
                """,Integer.class,status);
        return Integer.valueOf(1).equals(c) && status.equals(initialStatus());
    }
    private long[] allocations(Old old){
        return db.query("""
                SELECT COALESCE(SUM(CASE WHEN USED='YES' THEN 1 ELSE 0 END),0),
                       COALESCE(SUM(CASE WHEN USED IS NULL OR USED NOT IN ('YES','NO')
                             OR BRANCH_CODE<>? THEN 1 ELSE 0 END),0)
                  FROM CLT.COLLATERAL_ALLOCATION WHERE COLLATERAL_ID=?
                """,(r,i)->new long[]{r.getLong(1),r.getLong(2)},old.branch(),number(old.id())).getFirst();
    }
    private Asset asset(Old old,boolean lock){
        if(!identityReady())return new Asset(null,null,null,null,null,false,false,-1);
        // Lock stable asset first for all BUC-12 writers; database unique index is the final race guard.
        List<Asset> rows=db.query("""
                SELECT a.ASSET_ID,a.COLLATERAL_TYPE_ID,a.IDENTITY_SCHEME_CODE,
                       a.VERIFICATION_EVIDENCE,a.SOURCE_REFERENCE,a.IDENTITY_STATUS
                  FROM CLT.CLT_ASSET_IDENTITY a
                  JOIN CLT.CLT_ASSET_COLLATERAL_LINK l ON l.ASSET_ID=a.ASSET_ID
                 WHERE l.COLLATERAL_ID=?
                """+(lock?" FOR UPDATE OF a.ASSET_ID":""),(r,i)->new Asset(
                    decimal(r.getBigDecimal(1)),decimal(r.getBigDecimal(2)),r.getString(3),
                    r.getString(4),r.getString(5),"VERIFIED".equals(r.getString(6)),false,-1),number(old.id()));
        if(rows.size()!=1)return new Asset(null,null,null,null,null,false,false,-1);
        Asset a=rows.getFirst();
        List<long[]> links=db.query("""
                SELECT COALESCE(SUM(CASE WHEN IS_CURRENT_FLAG='Y' AND COLLATERAL_ID=? THEN 1 ELSE 0 END),0),
                       COALESCE(SUM(CASE WHEN IS_CURRENT_FLAG='Y' AND COLLATERAL_ID<>? THEN 1 ELSE 0 END),0)
                  FROM CLT.CLT_ASSET_COLLATERAL_LINK WHERE ASSET_ID=?
                """,(r,i)->new long[]{r.getLong(1),r.getLong(2)},number(old.id()),number(old.id()),number(a.id()));
        long[] count=links.getFirst();
        return new Asset(a.id(),a.typeId(),a.scheme(),a.verification(),a.sourceReference(),a.verified(),
                count[0]==1,count[1]);
    }
    public Evidence evidence(String rawId,Authentication auth){
        Actor who=actor(auth,false);String id=CollateralReintroductionPolicy.requiredId(rawId);
        Old old=previous(id,who.branch(),false);
        boolean terminal=terminal(old.status());
        long[] allocations=allocations(old);
        boolean release=false,history=false;
        // The source is never declared reusable when the official BUC-11 evidence table is absent.
        try {release=released(old);history=historyReleased(old);}catch(DataAccessException absent){release=false;history=false;}
        Asset a=asset(old,false);
        List<String> blockers=new java.util.ArrayList<>();
        if(!terminal||!release||!history)blockers.add("FINAL_RELEASE_NOT_PROVEN");
        if(allocations[0]!=0||allocations[1]!=0)blockers.add("OUTSTANDING_OR_UNKNOWN_ALLOCATION");
        if(!a.verified()||a.id()==null||CollateralReintroductionPolicy.empty(a.verification()))
            blockers.add("ASSET_IDENTITY_NOT_APPROVED");
        if(!a.previousCurrent()||a.otherCurrent()!=0)blockers.add("OTHER_ACTIVE_REGISTRATION_OR_MISSING_ASSET_LINK");
        return new Evidence(old.id(),old.registration(),old.status(),old.version(),old.partyId(),
                old.typeId(),old.branch(),a.id(),release&&history,terminal,
                a.verified()&&!CollateralReintroductionPolicy.empty(a.verification()),
                a.otherCurrent()>0,allocations[0],List.copyOf(blockers),
                blockers.isEmpty()?"PREVIOUS_ASSET_ELIGIBLE_FOR_BANK_APPROVED_REINTRODUCTION":
                    "REINTRODUCTION_SOURCE_EVIDENCE_INCOMPLETE");
    }
    private static final String PERMIT_SELECT="""
        SELECT REINTRODUCTION_PERMIT_ID,PREVIOUS_COLLATERAL_ID,BASE_RECORD_VERSION,
               ASSET_ID,REVIEW_ID,APPROVED_PARTY_ID,APPROVED_TYPE_ID,
               APPROVED_REGISTRATION_NUMBER,NUMBER_SCHEME_CODE,INITIAL_STATUS_CODE,
               MULTIPLICITY,USAGE_CONDITION,USAGE_TERM,BANK_APPROVAL_REFERENCE,
               OWNER_REVERIFICATION_REFERENCE,IDENTITY_VERIFICATION_REFERENCE,
               REQUIREMENTS_ATTESTATION_REFERENCE,SERIAL_ATTESTATION_REFERENCE,
               MAKER_SUBJECT,CHECKER_SUBJECT,PERMIT_STATUS,VALID_UNTIL,
               IDEMPOTENCY_KEY,POSTER_SUBJECT,POSTED_COLLATERAL_ID,
               POSTED_COLLATERAL_NUMBER,POSTED_AT
          FROM CLT.CLT_REINTRODUCTION_PERMIT
        """;
    private static Permit permitMap(java.sql.ResultSet r,int i)throws java.sql.SQLException{
        return new Permit(decimal(r.getBigDecimal(1)),decimal(r.getBigDecimal(2)),
             decimal(r.getBigDecimal(3)),decimal(r.getBigDecimal(4)),decimal(r.getBigDecimal(5)),
             decimal(r.getBigDecimal(6)),decimal(r.getBigDecimal(7)),decimal(r.getBigDecimal(8)),
             r.getString(9),r.getString(10),r.getInt(11),r.getInt(12),r.getInt(13),
             r.getString(14),r.getString(15),r.getString(16),r.getString(17),
             r.getString(18),r.getString(19),r.getString(20),r.getString(21),
             date(r.getTimestamp(22)),r.getString(23),r.getString(24),
             decimal(r.getBigDecimal(25)),decimal(r.getBigDecimal(26)),date(r.getTimestamp(27)));
    }
    private Permit permit(String permitId,Old old,boolean lock){
        List<Permit> items=db.query(PERMIT_SELECT+" WHERE REINTRODUCTION_PERMIT_ID=? AND PREVIOUS_COLLATERAL_ID=?"
                    +(lock?" FOR UPDATE":""),CollateralReintroductionService::permitMap,
                number(permitId),number(old.id()));
        if(items.size()!=1)throw stop(HttpStatus.NOT_FOUND,"REINTRO_PERMIT_NOT_FOUND");
        return items.getFirst();
    }
    public Permits permits(String rawId,Authentication auth){
        Actor who=actor(auth,false);String id=CollateralReintroductionPolicy.requiredId(rawId);
        previous(id,who.branch(),false);
        if(!permitReady())return new Permits(List.of(),"BANK_PERMIT_SCHEMA_NOT_INSTALLED");
        List<PermitView> rows=db.query(PERMIT_SELECT+"""
                 WHERE PREVIOUS_COLLATERAL_ID=? AND PERMIT_STATUS IN ('APPROVED','POSTED')
                 ORDER BY REINTRODUCTION_PERMIT_ID DESC FETCH FIRST 30 ROWS ONLY
                """,(r,i)->{
            Permit p=permitMap(r,i);
            return new PermitView(p.id(),p.status(),p.reviewId(),p.sourceId(),
                    p.assetId(),p.approvedPartyId(),p.approvedTypeId(),p.initialStatus(),
                    p.number(),p.approvalRef(),p.ownershipRef(),p.expires()==null?null:p.expires().toString(),
                    p.postedId(),p.postedNumber());
        },number(id));
        return new Permits(List.copyOf(rows),"ORACLE_BANK_APPROVED_PERMITS");
    }
    public Lineage lineage(String rawId,Authentication auth){
        Actor who=actor(auth,false);String id=CollateralReintroductionPolicy.requiredId(rawId);
        Old old=previous(id,who.branch(),false);
        if(!identityReady())return new Lineage(old.id(),null,List.of(),"ASSET_LINEAGE_SCHEMA_NOT_APPROVED");
        Asset asset=asset(old,false);
        if(asset.id()==null)return new Lineage(old.id(),null,List.of(),"VERIFIED_ASSET_LINK_MISSING");
        // The path is only disclosed in the source registration's branch.
        List<LineageItem> rows=db.query("""
                SELECT c.COLLATERAL_ID,c.COLLATERAL_NUMBER,l.PREVIOUS_COLLATERAL_ID,
                       c.COLLATERAL_STATUS_CODE,l.LINKED_AT,l.IS_CURRENT_FLAG,l.ASSET_ID
                  FROM CLT.CLT_ASSET_COLLATERAL_LINK l
                  JOIN CLT.COLLATERAL c ON c.COLLATERAL_ID=l.COLLATERAL_ID
                 WHERE l.ASSET_ID=? AND c.BRANCH_CODE=?
                 ORDER BY l.LINKED_AT,l.ASSET_COLLATERAL_LINK_ID
                """,(r,i)->new LineageItem(decimal(r.getBigDecimal(1)),decimal(r.getBigDecimal(2)),
                    decimal(r.getBigDecimal(3)),r.getString(4),date(r.getTimestamp(5)).toString(),
                    "Y".equals(r.getString(6)),decimal(r.getBigDecimal(7))),
                number(asset.id()),who.branch());
        return new Lineage(id,asset.id(),List.copyOf(rows),"BR04_HISTORICAL_LINKS_READ_ONLY");
    }
    private Reviewed reviewed(Permit p,Actor actor){
        if(!countabilityReady())throw stop(HttpStatus.SERVICE_UNAVAILABLE,"REINTRO_G12_COUNTABILITY_SCHEMA_REQUIRED");
        List<Reviewed> list=db.query("""
                SELECT r.REVIEW_ID,r.DRAFT_ID,r.REVIEW_STATUS,r.BRANCH_CODE,d.ASSET_REFERENCE,
                       d.PARTY_ID,d.COLLATERAL_TYPE_ID,d.CURRENCY_CODE,d.ESTIMATE_AMOUNT,
                       d.VALID_FROM,d.VALID_TO,d.DEPOSIT_NUMBER,d.REMARKS,
                       r.CHECKER_SUBJECT,r.MAKER_SUBJECT,r.DRAFT_HASH,d.REQUEST_HASH,
                       d.STATUS,d.RECORD_VERSION,
                       CASE WHEN t.STATUS='ACTIVE' AND g.STATUS='ACTIVE' THEN 'ACTIVE' ELSE 'INACTIVE' END,c.STATUS,
                       t.HAS_MULTIPLICITY,t.IS_COUNTABLE_FLAG
                  FROM CLT.CLT_INTRODUCTION_REVIEW r
                  JOIN CLT.CLT_INTRODUCTION_DRAFT d ON d.DRAFT_ID=r.DRAFT_ID
                  JOIN CLT.COLLATERAL_TYPE t ON t.COLLATERAL_TYPE_ID=d.COLLATERAL_TYPE_ID
                  JOIN CLT.COLLATERAL_GROUP g ON g.COLLATERAL_GROUP_ID=t.COLLATERAL_GROUP_ID
                  JOIN CLT.REF_CURRENCY c ON c.CURRENCY_CODE=d.CURRENCY_CODE
                 WHERE r.REVIEW_ID=? AND r.BRANCH_CODE=? AND d.BRANCH_CODE=?
                FOR UPDATE OF r.RECORD_VERSION
                """,(r,i)->new Reviewed(decimal(r.getBigDecimal(1)),decimal(r.getBigDecimal(2)),
                r.getString(3),r.getString(4),r.getString(5),
                decimal(r.getBigDecimal(6)),decimal(r.getBigDecimal(7)),
                r.getString(8),decimal(r.getBigDecimal(9)),r.getDate(10),r.getDate(11),
                r.getString(12),r.getString(13),r.getString(14),r.getString(15),r.getString(16),
                r.getString(17),r.getString(18),decimal(r.getBigDecimal(19)),
                "ACTIVE".equals(r.getString(20)),"ACTIVE".equals(r.getString(21)),
                "Y".equals(r.getString(22)),r.getString(23)!=null,
                "Y".equals(r.getString(23)),0,false),
                number(p.reviewId()),actor.branch(),actor.branch());
        if(list.size()!=1)throw stop(HttpStatus.CONFLICT,"REINTRO_REVIEWED_DRAFT_NOT_FOUND");
        Reviewed base=list.getFirst();
        boolean hasRangeSchema=rangesReady();
        List<SerialRow> ranges=hasRangeSchema?db.query("""
                SELECT ROW_NO,SERIAL_FROM,SERIAL_TO,TOTAL_AMOUNT_IRR
                  FROM CLT.CLT_INTRODUCTION_DRAFT_SERIAL_RANGE
                 WHERE DRAFT_ID=? ORDER BY ROW_NO
                """,(r,i)->new SerialRow(r.getInt(1),r.getString(2),r.getString(3),
                        r.getBigDecimal(4)),number(base.draftId())):List.of();
        boolean validRanges=base.multiplicityType()
                ? hasRangeSchema && base.countabilityConfigured() && base.countable()
                    && "IRR".equals(base.currency()) && validSerialRanges(ranges,new BigDecimal(base.estimate()))
                : ranges.isEmpty();
        return new Reviewed(base.reviewId(),base.draftId(),base.status(),base.branch(),
                base.assetReference(),base.partyId(),base.typeId(),base.currency(),base.estimate(),
                base.validFrom(),base.validTo(),base.deposit(),base.remarks(),base.reviewer(),base.maker(),
                base.reviewHash(),base.draftHash(),base.draftStatus(),base.draftVersion(),
                base.typeActive(),base.currencyActive(),base.multiplicityType(),
                base.countabilityConfigured(),base.countable(),ranges.size(),validRanges);
    }
    /** BigInteger validation avoids Oracle TO_NUMBER overflow on physical VARCHAR2(100) serials. */
    private static boolean validSerialRanges(List<SerialRow> ranges,BigDecimal estimate) {
        if(ranges.isEmpty()||ranges.size()>80)return false;
        BigDecimal sum=BigDecimal.ZERO;
        List<BigInteger[]> spans=new ArrayList<>();
        for(int i=0;i<ranges.size();i++) {
            SerialRow r=ranges.get(i);
            if(r.ordinal()!=i+1||r.first()==null||r.last()==null
                    ||!r.first().matches("[0-9]{1,100}") ||!r.last().matches("[0-9]{1,100}")
                    ||r.amount()==null ||r.amount().signum()<=0)return false;
            BigInteger first=new BigInteger(r.first()),last=new BigInteger(r.last());
            BigInteger count=last.subtract(first).add(BigInteger.ONE);
            if(count.signum()<=0||count.compareTo(BigInteger.valueOf(20))>0)return false;
            spans.add(new BigInteger[]{first,last});sum=sum.add(r.amount());
        }
        spans.sort(Comparator.comparing(a->a[0]));
        for(int i=1;i<spans.size();i++)if(spans.get(i)[0].compareTo(spans.get(i-1)[1])<=0)return false;
        return sum.compareTo(estimate)==0;
    }

    /** Preserve every reviewed type-specific attribute. Empty JSON never satisfies required rules. */
    private List<AttributeValue> verifiedAttributes(Reviewed review){
        String content=db.queryForObject("SELECT ATTRIBUTES_JSON FROM CLT.CLT_INTRODUCTION_DRAFT WHERE DRAFT_ID=?",
                String.class,number(review.draftId()));
        AttributeInput[] parsed;
        try{parsed=json.readValue(content,AttributeInput[].class);}
        catch(Exception ex){throw stop(HttpStatus.CONFLICT,"REINTRO_REVIEWED_ATTRIBUTES_INVALID");}
        if(parsed==null||parsed.length>80)throw stop(HttpStatus.CONFLICT,"REINTRO_ATTRIBUTE_ROW_LIMIT");
        List<AttributeDefinition> rules=db.query("""
                SELECT a.COLLATERAL_ATTRIBUTE_DEFINITION_ID,d.DATA_TYPE_CODE,a.IS_REQUIRED,
                       d.IS_REPEATABLE_FLAG,a.MANUAL_ENTRY_ALLOWED_FLAG,d.LIST_VALUE
                  FROM CLT.COLLATERAL_TYPE_ATTRIBUTE a
                  JOIN CLT.COLLATERAL_ATTRIBUTE_DEFINITION d
                    ON d.COLLATERAL_ATTRIBUTE_DEFINITION_ID=a.COLLATERAL_ATTRIBUTE_DEFINITION_ID
                 WHERE a.COLLATERAL_TYPE_ID=? AND a.STATUS='ACTIVE' AND d.STATUS='ACTIVE'
                """,(r,i)->new AttributeDefinition(decimal(r.getBigDecimal(1)),r.getString(2),
                    "Y".equals(r.getString(3)),"Y".equals(r.getString(4)),
                    "Y".equals(r.getString(5)),r.getString(6)),
                number(review.typeId()));
        Map<String,AttributeDefinition> known=new HashMap<>();
        for(AttributeDefinition rule:rules){
            if(known.put(rule.id(),rule)!=null)
                throw stop(HttpStatus.CONFLICT,"REINTRO_ATTRIBUTE_DEFINITION_DUPLICATE");
        }
        Map<String,Integer> counts=new HashMap<>();
        Set<String> distinctValues=new HashSet<>();
        List<AttributeValue> values=new ArrayList<>();
        for(AttributeInput entry:parsed){
            if(entry==null || entry.attributeId()==null || entry.value()==null)
                throw stop(HttpStatus.CONFLICT,"REINTRO_ATTRIBUTE_INPUT_INVALID");
            AttributeDefinition rule=known.get(entry.attributeId());
            if(rule==null)throw stop(HttpStatus.CONFLICT,"REINTRO_ATTRIBUTE_NOT_APPROVED");
            if(!rule.manualEntryAllowed())throw stop(HttpStatus.CONFLICT,"REINTRO_ATTRIBUTE_MANUAL_ENTRY_DISABLED");
            if(!distinctValues.add(rule.id()+"\u0000"+entry.value()))
                throw stop(HttpStatus.CONFLICT,"REINTRO_ATTRIBUTE_VALUE_DUPLICATE");
            int count=counts.merge(rule.id(),1,Integer::sum);
            if(count>1&&!rule.repeatable())throw stop(HttpStatus.CONFLICT,"REINTRO_ATTRIBUTE_NONREPEATABLE");
            String cleaned;
            try{cleaned=CollateralIntroductionPolicy.validateValue(rule.dataType(),entry.value(),rule.listValue());}
            catch(IllegalArgumentException ex){throw stop(HttpStatus.CONFLICT,"REINTRO_ATTRIBUTE_VALUE_INVALID");}
            values.add(new AttributeValue(rule.id(),rule.dataType(),cleaned,count));
        }
        for(AttributeDefinition rule:rules){
            if(rule.required()&&!counts.containsKey(rule.id()))
                throw stop(HttpStatus.CONFLICT,"REINTRO_REQUIRED_ATTRIBUTE_MISSING");
        }
        return List.copyOf(values);
    }
    private void persistAttributes(List<AttributeValue> attrs,String newId,String poster){
        for(AttributeValue a:attrs){
            BigDecimal numberValue="NUMBER".equals(a.dataType())?new BigDecimal(a.value()):null;
            java.sql.Date dateValue="DATE".equals(a.dataType())?java.sql.Date.valueOf(a.value()):null;
            String textValue=numberValue==null&&dateValue==null?a.value():null;
            int inserted=db.update("""
                    INSERT INTO CLT.COLLATERAL_ATTRIBUTE_DEFINITION_ITEM
                     (COLLATERAL_ID,COLLATERAL_ATTRIBUTE_DEFINITION_ID,NUMERIC_VALUE,
                      TEXT_VALUE,DATE_VALUE,VALUE_SEQUENCE_NO,CREATED_BY,RECORD_VERSION)
                    VALUES (?,?,?,?,?,?,?,1)
                    """,number(newId),number(a.id()),numberValue,textValue,dateValue,a.ordinal(),poster);
            if(inserted!=1)throw stop(HttpStatus.CONFLICT,"REINTRO_ATTRIBUTE_WRITE_INCOMPLETE");
        }
    }
    private List<SerialRow> approvedSerialRows(Reviewed review){
        if(!review.multiplicityType())return List.of();
        if(!rangesReady())throw stop(HttpStatus.CONFLICT,"REINTRO_SERIAL_RANGE_SCHEMA_MISSING");
        List<SerialRow> rows=db.query("""
                SELECT ROW_NO,SERIAL_FROM,SERIAL_TO,TOTAL_AMOUNT_IRR
                  FROM CLT.CLT_INTRODUCTION_DRAFT_SERIAL_RANGE
                 WHERE DRAFT_ID=? ORDER BY ROW_NO
                """,(r,i)->new SerialRow(r.getInt(1),r.getString(2),r.getString(3),r.getBigDecimal(4)),
                number(review.draftId()));
        if(rows.size()!=review.rangeCount() || !validSerialRanges(rows,new BigDecimal(review.estimate())))
            throw stop(HttpStatus.CONFLICT,"REINTRO_SERIAL_RANGE_CHANGED");
        return List.copyOf(rows);
    }
    private void persistSerialRows(List<SerialRow> rows,String newId,String currency,String poster){
        for(SerialRow r:rows){
            BigInteger items=new BigInteger(r.last()).subtract(new BigInteger(r.first())).add(BigInteger.ONE);
            int inserted=db.update("""
                    INSERT INTO CLT.CLT_COLLATERAL_SERIAL_RANGE
                     (COLLATERAL_SERIAL_RANGE_ID,COLLATERAL_ID,RANGE_SEQUENCE_NO,SERIAL_FROM,
                      SERIAL_TO,ITEM_COUNT,RANGE_AMOUNT,CURRENCY_CODE,CREATED_BY)
                    VALUES (CLT.SEQ_CLT_COLLATERAL_SERIAL_RANGE.NEXTVAL,?,?,?,?,?,?,?,?)
                    """,number(newId),r.ordinal(),r.first(),r.last(),items.intValueExact(),
                    r.amount(),currency,poster);
            if(inserted!=1)throw stop(HttpStatus.CONFLICT,"REINTRO_SERIAL_RANGE_WRITE_INCOMPLETE");
        }
    }

    private boolean availableNumber(Permit p,String oldNumber){
        Integer count=db.queryForObject("""
                SELECT COUNT(*) FROM CLT.CLT_REGISTRATION_NUMBER_AUTHORITY
                 WHERE COLLATERAL_NUMBER=? AND SCHEME_CODE=? AND APPROVAL_REFERENCE=?
                   AND ISSUE_STATUS='AVAILABLE' AND ISSUED_TO_REVIEW_ID IS NULL AND ISSUED_AT IS NULL
                """,Integer.class,number(p.number()),p.numberScheme(),p.approvalRef());
        return Integer.valueOf(1).equals(count)&&!oldNumber.equals(p.number());
    }
    private static Receipt receipt(Permit p,String previousNumber,boolean replay){
        return new Receipt(p.id(),p.sourceId(),previousNumber,p.postedId(),p.postedNumber(),
                p.assetId(),p.reviewId(),p.status(),p.postedAt()==null?null:p.postedAt().toString(),
                replay,"BUC12_NEW_COLLATERAL_REGISTERED_WITH_HISTORICAL_ASSET_LINK");
    }
    /** No POST of arbitrary identity, owner, number or release confirmation is accepted. */
    public Receipt commit(String rawPrevious,FinalizeCommand command,String ifMatch,String requestKey,Authentication auth){
        Actor actor=actor(auth,true);
        String oldId=CollateralReintroductionPolicy.requiredId(rawPrevious);
        if(command==null)throw stop(HttpStatus.BAD_REQUEST,"REINTRO_PERMIT_REQUIRED");
        String permitId=CollateralReintroductionPolicy.requiredId(command.permitId());
        String expected=CollateralReintroductionPolicy.etag(ifMatch);
        String key=CollateralReintroductionPolicy.idempotency(requestKey);
        assertPostingReady();
        try{return Objects.requireNonNull(tx.execute(status->{
            Old old=previous(oldId,actor.branch(),true);
            Asset asset=asset(old,true);
            Permit p=permit(permitId,old,true);
            if("POSTED".equals(p.status())){
                if(key.equals(p.key()) && actor.id().equals(p.poster())
                        && expected.equals(p.sourceVersion())
                        && p.postedId()!=null && p.postedNumber()!=null && p.postedAt()!=null)
                    return receipt(p,old.registration(),true);
                throw stop(HttpStatus.CONFLICT,"REINTRO_ALREADY_POSTED_DIFFERENT_REQUEST");
            }
            if(asset.id()==null)throw stop(HttpStatus.CONFLICT,"REINTRO_VERIFIED_ASSET_LINK_REQUIRED");
            Reviewed review=reviewed(p,actor);
            boolean release=released(old),releaseHistory=historyReleased(old);
            long[] allocation=allocations(old);
            LocalDateTime now=db.queryForObject("SELECT CAST(SYSTIMESTAMP AS TIMESTAMP) FROM DUAL",
                    (r,i)->date(r.getTimestamp(1)));
            boolean ownerAndRules=!CollateralReintroductionPolicy.empty(p.ownershipRef())
                    && !CollateralReintroductionPolicy.empty(p.requirementsRef())
                    && review.typeActive() && review.currencyActive() && review.validFrom()!=null
                    && !review.validFrom().toLocalDate().isAfter(now.toLocalDate())
                    && (review.validTo()==null||!review.validTo().toLocalDate().isBefore(now.toLocalDate()))
                    && number(review.estimate()).signum()>0;
            boolean rangesValid=review.rangeRowsValid() && (review.multiplicityType()==(p.multiplicity()==1))
                    && (!review.multiplicityType() || !CollateralReintroductionPolicy.empty(p.serialRef()));
            // Freshly revalidate all draft attribute rules under the same Oracle transaction.
            List<AttributeValue> attrs=verifiedAttributes(review);
            List<SerialRow> serials=approvedSerialRows(review);
            boolean available=availableNumber(p,old.registration());
            boolean consistentReview=review.typeId().equals(p.approvedTypeId())
                    && review.typeId().equals(old.typeId())
                    && asset.typeId().equals(old.typeId())
                    && review.assetReference()!=null && asset.sourceReference()!=null
                    && review.assetReference().strip().equalsIgnoreCase(asset.sourceReference().strip())
                    && review.partyId().equals(p.approvedPartyId())
                    && actor.branch().equals(review.branch())
                    && "DRAFT".equals(review.draftStatus()) && "1".equals(review.draftVersion());
            var facts=new CollateralReintroductionPolicy.Facts(actor.branch(),old.branch(),old.status(),
                    terminal(old.status()),release,releaseHistory,allocation[0],allocation[1],
                    asset.id(),p.assetId(),asset.verified()&&!CollateralReintroductionPolicy.empty(asset.verification()),
                    asset.otherCurrent(),asset.previousCurrent(),asset.typeId(),review.typeId(),
                    review.branch(),review.status(),review.reviewHash(),review.draftHash(),
                    consistentReview,review.partyId(),p.approvedPartyId(),
                    !CollateralReintroductionPolicy.empty(p.ownershipRef()),ownerAndRules,rangesValid,
                    available,old.registration(),p.number(),p.initialStatus(),initialApproved(p.initialStatus()),
                    old.version(),p.sourceVersion(),p.maker(),p.checker(),actor.id(),
                    p.approvalRef(),p.ownershipRef(),p.identityRef(),p.status(),p.expires(),now);
            List<String> blockers=CollateralReintroductionPolicy.blockers(facts);
            if(!blockers.isEmpty())throw stop(HttpStatus.CONFLICT,"REINTRO_DENIED_"+blockers.getFirst());
            if(!review.maker().equals(p.maker())||!review.reviewer().equals(p.checker()))
                throw stop(HttpStatus.CONFLICT,"REINTRO_REVIEW_ACTORS_DO_NOT_MATCH_APPROVED_PERMIT");
            if(!"VERIFIED".equals(db.queryForObject("SELECT IDENTITY_STATUS FROM CLT.CLT_ASSET_IDENTITY WHERE ASSET_ID=?",String.class,number(asset.id()))))
                throw stop(HttpStatus.CONFLICT,"REINTRO_ASSET_IDENTITY_CHANGED");
            // Serialize this specific bank-issued number; an old number is never reused.
            List<String> authority=db.query("""
                    SELECT ISSUE_STATUS FROM CLT.CLT_REGISTRATION_NUMBER_AUTHORITY
                     WHERE COLLATERAL_NUMBER=? AND SCHEME_CODE=? AND APPROVAL_REFERENCE=? FOR UPDATE
                    """,(r,i)->r.getString(1),number(p.number()),p.numberScheme(),p.approvalRef());
            if(authority.size()!=1||!"AVAILABLE".equals(authority.getFirst()))
                throw stop(HttpStatus.CONFLICT,"REINTRO_BANK_NUMBER_ALREADY_USED");
            int numberUpdated=db.update("""
                    UPDATE CLT.CLT_REGISTRATION_NUMBER_AUTHORITY
                       SET ISSUE_STATUS='ISSUED',ISSUED_TO_REVIEW_ID=?,ISSUED_AT=SYSTIMESTAMP
                     WHERE COLLATERAL_NUMBER=? AND SCHEME_CODE=? AND APPROVAL_REFERENCE=?
                       AND ISSUE_STATUS='AVAILABLE' AND ISSUED_TO_REVIEW_ID IS NULL
                    """,number(review.reviewId()),number(p.number()),p.numberScheme(),p.approvalRef());
            if(numberUpdated!=1)throw stop(HttpStatus.CONFLICT,"REINTRO_BANK_NUMBER_RACE");
            KeyHolder keyHolder=new GeneratedKeyHolder();
            db.update(conn->{
                PreparedStatement ps=conn.prepareStatement("""
                    INSERT INTO CLT.COLLATERAL
                       (COLLATERAL_NUMBER,PARTY_ID,COLLATERAL_STATUS_CODE,COLLATERAL_TYPE_ID,
                        ESTIMATE_AMOUNT,VALID_FROM,VALID_TO,MULTIPLICITY,USAGE_CONDITION,USAGE_TERM,
                        DEPOSIT_NUMBER,BRANCH_CODE,REMARKS,CURRENCY_CODE,CREATED_BY,RECORD_VERSION)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,1)
                    """,new String[]{"COLLATERAL_ID"});
                ps.setBigDecimal(1,number(p.number()));ps.setBigDecimal(2,number(review.partyId()));
                ps.setString(3,p.initialStatus());ps.setBigDecimal(4,number(review.typeId()));
                ps.setBigDecimal(5,number(review.estimate()));
                ps.setDate(6,review.validFrom());ps.setDate(7,review.validTo());
                ps.setInt(8,p.multiplicity());ps.setInt(9,p.usageCondition());ps.setInt(10,p.usageTerm());
                ps.setString(11,review.deposit());ps.setString(12,actor.branch());
                ps.setString(13,review.remarks());ps.setString(14,review.currency());
                ps.setString(15,actor.id());return ps;
            },keyHolder);
            Number id=keyHolder.getKey();
            if(id==null)throw new IllegalStateException("REINTRO_ORACLE_COLLATERAL_ID_NOT_RETURNED");
            String newId=new BigDecimal(id.toString()).toPlainString();
            // BUC12/BR04: previous remains immutable. Historical current pointer changes only in the link table.
            int releasedLink=db.update("""
                    UPDATE CLT.CLT_ASSET_COLLATERAL_LINK SET IS_CURRENT_FLAG='N'
                     WHERE ASSET_ID=? AND COLLATERAL_ID=? AND IS_CURRENT_FLAG='Y'
                    """,number(asset.id()),number(old.id()));
            if(releasedLink!=1)throw stop(HttpStatus.CONFLICT,"REINTRO_PREVIOUS_ASSET_LINK_CONFLICT");
            db.update("""
                    INSERT INTO CLT.CLT_ASSET_COLLATERAL_LINK
                     (ASSET_COLLATERAL_LINK_ID,ASSET_ID,COLLATERAL_ID,PREVIOUS_COLLATERAL_ID,
                      IS_CURRENT_FLAG,LINKED_BY)
                    VALUES (CLT.SEQ_CLT_ASSET_COLLATERAL_LINK.NEXTVAL,?,?,?,'Y',?)
                    """,number(asset.id()),number(newId),number(old.id()),actor.id());
            KeyHolder statusKey=new GeneratedKeyHolder();
            db.update(conn->{
                PreparedStatement ps=conn.prepareStatement("""
                        INSERT INTO CLT.COLLATERAL_STATUS_HISTORY
                        (COLLATERAL_ID,STATUS_SEQUENCE_NO,FROM_STATUS_CODE,TO_STATUS_CODE,
                         STATUS_REASON_CODE,STATUS_REASON_TEXT,SOURCE_EVENT_CODE,SOURCE_REFERENCE,
                         CHANGED_BY,PREVIOUS_STATUS_HISTORY_ID,CREATED_BY,RECORD_VERSION)
                        VALUES (?,1,NULL,?,'REINTRODUCTION_APPROVED',?,'BUC12_REINTRODUCTION',?, ?,NULL,?,1)
                        """,new String[]{"COLLATERAL_STATUS_HISTORY_ID"});
                ps.setBigDecimal(1,number(newId));ps.setString(2,p.initialStatus());
                ps.setString(3,"BUC12: previous registration "+old.registration());
                ps.setString(4,p.approvalRef());ps.setString(5,actor.id());ps.setString(6,actor.id());
                return ps;
            },statusKey);
            if(statusKey.getKey()==null)throw new IllegalStateException("REINTRO_STATUS_HISTORY_NOT_RETURNED");
            persistAttributes(attrs,newId,actor.id());
            persistSerialRows(serials,newId,review.currency(),actor.id());
            db.update("""
                INSERT INTO CLT.CLT_REGISTRATION_LEDGER
                 (REGISTRATION_ID,REVIEW_ID,DRAFT_ID,COLLATERAL_ID,COLLATERAL_NUMBER,DRAFT_HASH,
                  IDENTITY_SCHEME_CODE,NUMBER_SCHEME_CODE,BANK_APPROVAL_REFERENCE,
                  REGISTRATION_STATUS,REGISTERED_BY,BRANCH_CODE)
                VALUES (CLT.SEQ_CLT_REGISTRATION_LEDGER.NEXTVAL,?,?,?,?,?,?,?,?,'REGISTERED',?,?)
                """,number(review.reviewId()),number(review.draftId()),number(newId),
                number(p.number()),review.draftHash(),asset.scheme(),p.numberScheme(),
                p.approvalRef(),actor.id(),actor.branch());
            int updated=db.update("""
                UPDATE CLT.CLT_REINTRODUCTION_PERMIT
                   SET PERMIT_STATUS='POSTED',IDEMPOTENCY_KEY=?,POSTER_SUBJECT=?,
                       POSTED_COLLATERAL_ID=?,POSTED_COLLATERAL_NUMBER=?,POSTED_AT=SYSTIMESTAMP
                 WHERE REINTRODUCTION_PERMIT_ID=? AND PREVIOUS_COLLATERAL_ID=?
                   AND PERMIT_STATUS='APPROVED' AND BASE_RECORD_VERSION=?
                """,key,actor.id(),number(newId),number(p.number()),number(p.id()),
                    number(old.id()),number(expected));
            if(updated!=1)throw stop(HttpStatus.CONFLICT,"REINTRO_PERMIT_CONCURRENTLY_CONSUMED");
            return receipt(permit(p.id(),old,false),old.registration(),false);
        }));}
        catch(DuplicateKeyException ex){throw stop(HttpStatus.CONFLICT,"REINTRO_ACTIVE_ASSET_OR_NUMBER_DUPLICATE");}
    }
}
