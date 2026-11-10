package com.behsazan.corebanking.collateral.registration;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionService;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceReaderPool;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryRepository;
import com.behsazan.corebanking.collateral.registration.CollateralRegistrationReadbackModels.Check;
import com.behsazan.corebanking.collateral.registration.CollateralRegistrationReadbackModels.Report;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * BUC-03/13 persisted registration readback. Uses the existing CLT read datasource.
 * No DML, no number allocation and no implicit approval of R2E design candidates.
 * A review can only be queried by its maker in the same authenticated branch.
 * A number can only be queried after the canonical branch-scoped core lookup.
 */
@Service
public final class CollateralRegistrationReadbackService {
    private final ObjectProvider<CollateralReferenceReaderPool> reader;
    private final CollateralRegistryRepository registry;
    private final CollateralIntroductionService introduction;
    public CollateralRegistrationReadbackService(ObjectProvider<CollateralReferenceReaderPool> reader,
            CollateralRegistryRepository registry, CollateralIntroductionService introduction) {
        this.reader = reader;
        this.registry = registry;
        this.introduction = introduction;
    }
    private JdbcTemplate db() { return reader.getObject().jdbc(); }
    private static String value(Object x) { return x == null ? null : x.toString(); }
    private static BigDecimal num(String x) { return CollateralRegistryPolicy.id(x); }
    private static Check check(String code, boolean ready, boolean matches) {
        return new Check(code, !ready ? "NOT_VERIFIED" : matches ? "VERIFIED" : "MISMATCH",
                !ready ? "ORACLE_SCHEMA_OR_EVIDENCE_NOT_AVAILABLE" : matches
                        ? "ORACLE_READBACK_MATCHED" : "ORACLE_READBACK_DIFFERENCE");
    }
    private static Map<String,Object> normalized(Map<String,Object> row) {
        if (row == null) return Map.of();
        Map<String,Object> safe=new LinkedHashMap<>();
        row.forEach((key,val)->{
            Object v=val;
            if(v instanceof BigDecimal b)v=b.toPlainString();
            else if(v instanceof java.sql.Timestamp t)v=t.toLocalDateTime().toString();
            else if(v instanceof java.sql.Date d)v=d.toLocalDate().toString();
            safe.put(key,v);
        });
        return safe;
    }
    private boolean columns(JdbcTemplate jdbc, String table, String... required) {
        String sql = "SELECT COUNT(DISTINCT COLUMN_NAME) FROM ALL_TAB_COLUMNS"
                + " WHERE OWNER='CLT' AND TABLE_NAME=? AND COLUMN_NAME IN ("
                + String.join(",", java.util.Collections.nCopies(required.length, "?")) + ")";
        Object[] args = new Object[required.length+1]; args[0] = table;
        System.arraycopy(required,0,args,1,required.length);
        Integer count = jdbc.queryForObject(sql,Integer.class,args);
        return count != null && count == required.length;
    }
    private static Map<String,Object> first(List<Map<String,Object>> list) {
        return list.size() == 1 ? normalized(list.getFirst()) : null;
    }
    private static final String[] LEDGER_COLUMNS = {"REVIEW_ID","DRAFT_ID","COLLATERAL_ID",
            "COLLATERAL_NUMBER","DRAFT_HASH","NUMBER_SCHEME_CODE","BANK_APPROVAL_REFERENCE",
            "REGISTRATION_STATUS","BRANCH_CODE"};
    private boolean ledgerReady(JdbcTemplate jdbc) { return columns(jdbc,"CLT_REGISTRATION_LEDGER",LEDGER_COLUMNS); }
    private List<Map<String,Object>> ledgerByNumber(JdbcTemplate jdbc, String branch,String number) {
        return jdbc.queryForList("""
                SELECT REVIEW_ID,DRAFT_ID,COLLATERAL_ID,COLLATERAL_NUMBER,DRAFT_HASH,
                       NUMBER_SCHEME_CODE,BANK_APPROVAL_REFERENCE,REGISTRATION_STATUS,BRANCH_CODE
                  FROM CLT.CLT_REGISTRATION_LEDGER
                 WHERE BRANCH_CODE=? AND COLLATERAL_NUMBER=?
                """,branch,num(number));
    }
    private List<Map<String,Object>> ledgerByReview(JdbcTemplate jdbc, String branch,String reviewId) {
        return jdbc.queryForList("""
                SELECT REVIEW_ID,DRAFT_ID,COLLATERAL_ID,COLLATERAL_NUMBER,DRAFT_HASH,
                       NUMBER_SCHEME_CODE,BANK_APPROVAL_REFERENCE,REGISTRATION_STATUS,BRANCH_CODE
                  FROM CLT.CLT_REGISTRATION_LEDGER
                 WHERE BRANCH_CODE=? AND REVIEW_ID=?
                """,branch,num(reviewId));
    }
    public Report byNumber(String branch, String number) {
        String scopedBranch = CollateralRegistryPolicy.branch(branch);
        String safeNumber = num(number).toPlainString();
        // Crucial authorization boundary: no child or ledger read before branch ownership.
        Map<String,Object> core = registry.byRegistration(scopedBranch,safeNumber);
        JdbcTemplate jdbc = db();
        if (!ledgerReady(jdbc)) return report("NUMBER",null,core,null,false,jdbc);
        List<Map<String,Object>> ledgers = ledgerByNumber(jdbc,scopedBranch,safeNumber);
        if (ledgers.size()>1) return duplicateLedger("NUMBER",core);
        return report("NUMBER",null,core,first(ledgers),true,jdbc);
    }
    public Report byReview(Authentication authentication, String reviewId) {
        var actor = introduction.actor(authentication);
        if (authentication.getAuthorities().stream().noneMatch(a ->
                "SCOPE_clt.collateral.introduce".equals(a.getAuthority())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"CLT_REGISTRATION_MAKER_SCOPE_REQUIRED");
        String safeId = num(reviewId).toPlainString();
        JdbcTemplate jdbc = db();
        if (!columns(jdbc,"CLT_INTRODUCTION_REVIEW","REVIEW_ID","MAKER_SUBJECT","BRANCH_CODE"))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"CLT_REVIEW_SCHEMA_NOT_READY");
        List<Map<String,Object>> owned = jdbc.queryForList("""
                SELECT REVIEW_ID FROM CLT.CLT_INTRODUCTION_REVIEW
                 WHERE REVIEW_ID=? AND MAKER_SUBJECT=? AND BRANCH_CODE=?
                """,num(safeId),actor.subject(),actor.branch());
        if (owned.size()!=1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "CLT_REGISTRATION_OWN_REVIEW_NOT_FOUND");
        if (!ledgerReady(jdbc)) return report("REVIEW",safeId,null,null,false,jdbc);
        List<Map<String,Object>> ledgers = ledgerByReview(jdbc,actor.branch(),safeId);
        if (ledgers.size()>1) return duplicateLedger("REVIEW",null);
        Map<String,Object> ledger = first(ledgers);
        if (ledger==null) return report("REVIEW",safeId,null,null,true,jdbc);
        Map<String,Object> core=null;
        try { core=registry.byRegistration(actor.branch(),value(ledger.get("COLLATERAL_NUMBER"))); }
        catch (ResponseStatusException notFound) {
            if (notFound.getStatusCode()!=HttpStatus.NOT_FOUND) throw notFound;
        }
        return report("REVIEW",safeId,core,ledger,true,jdbc);
    }
    private static Report duplicateLedger(String mode,Map<String,Object> core) {
        return new Report(mode,null,value(core==null?null:core.get("COLLATERAL_ID")),
                value(core==null?null:core.get("COLLATERAL_NUMBER")),"READBACK_MISMATCH",
                false,false,"ORACLE_CLT_READ_ONLY",List.of(new Check("LEDGER","MISMATCH",
                "MULTIPLE_REGISTRATION_LEDGER_ROWS")),core==null?Map.of():core,List.of(),List.of());
    }
    private Report report(String mode,String reviewedId,Map<String,Object> core,
            Map<String,Object> ledger,boolean ledgerSchema,JdbcTemplate jdbc) {
        List<Check> checks=new ArrayList<>();
        boolean corePresent=core!=null;
        boolean ledgerPresent=ledger!=null;
        checks.add(check("CORE_BRANCH_ROW",true,corePresent || !ledgerPresent));
        checks.add(new Check("REGISTRATION_LEDGER",
                !ledgerSchema?"NOT_VERIFIED":ledgerPresent?"VERIFIED":"NOT_APPLICABLE",
                !ledgerSchema?"APPROVED_REGISTRATION_LEDGER_NOT_OBSERVED":ledgerPresent
                        ?"ONE_LEDGER_ROW_OBSERVED":"NO_REGISTRATION_LEDGER_ROW"));
        if (!ledgerPresent) {
            String state=CollateralRegistrationReadbackPolicy.state(mode,corePresent,ledgerSchema,false,checks);
            return new Report(mode,reviewedId,value(core==null?null:core.get("COLLATERAL_ID")),
                    value(core==null?null:core.get("COLLATERAL_NUMBER")),state,false,false,
                    "ORACLE_CLT_READ_ONLY",List.copyOf(checks),core==null?Map.of():core,List.of(),List.of());
        }
        if(reviewedId!=null && !Objects.equals(reviewedId,value(ledger.get("REVIEW_ID"))))
            checks.add(check("REVIEW_LINK",true,false));
        boolean bound=CollateralRegistrationReadbackPolicy.matchesCore(core,ledger);
        checks.add(check("LEDGER_TO_CORE_BINDING",true,bound));
        String collId=value(ledger.get("COLLATERAL_ID"));
        String reviewId=value(ledger.get("REVIEW_ID"));
        boolean reviewReady=columns(jdbc,"CLT_INTRODUCTION_REVIEW",
                "REVIEW_ID","DRAFT_ID","DRAFT_HASH","REVIEW_STATUS","MAKER_SUBJECT",
                "CHECKER_SUBJECT","BRANCH_CODE");
        boolean reviewed=false;
        if(reviewReady){
            Map<String,Object> review=first(jdbc.queryForList("""
                    SELECT REVIEW_ID,DRAFT_ID,DRAFT_HASH,REVIEW_STATUS,MAKER_SUBJECT,CHECKER_SUBJECT
                      FROM CLT.CLT_INTRODUCTION_REVIEW WHERE REVIEW_ID=? AND BRANCH_CODE=?
                    """,num(reviewId),ledger.get("BRANCH_CODE")));
            reviewed=review!=null && "REVIEWED".equals(review.get("REVIEW_STATUS"))
                    && CollateralRegistrationReadbackPolicy.equalId(review.get("DRAFT_ID"),ledger.get("DRAFT_ID"))
                    && Objects.equals(review.get("DRAFT_HASH"),ledger.get("DRAFT_HASH"))
                    && review.get("MAKER_SUBJECT")!=null && review.get("CHECKER_SUBJECT")!=null
                    && !review.get("MAKER_SUBJECT").equals(review.get("CHECKER_SUBJECT"));
        }
        checks.add(check("INDEPENDENT_REVIEW_DECISION",reviewReady,reviewed));
        boolean authorityReady=columns(jdbc,"CLT_REGISTRATION_NUMBER_AUTHORITY",
                "COLLATERAL_NUMBER","SCHEME_CODE","APPROVAL_REFERENCE","ISSUE_STATUS","ISSUED_TO_REVIEW_ID");
        boolean issued=false;
        if(authorityReady){
            Integer count=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM CLT.CLT_REGISTRATION_NUMBER_AUTHORITY
                     WHERE COLLATERAL_NUMBER=? AND SCHEME_CODE=? AND APPROVAL_REFERENCE=?
                       AND ISSUE_STATUS='ISSUED' AND ISSUED_TO_REVIEW_ID=?
                    """,Integer.class,num(value(ledger.get("COLLATERAL_NUMBER"))),
                    ledger.get("NUMBER_SCHEME_CODE"),ledger.get("BANK_APPROVAL_REFERENCE"),num(reviewId));
            issued=Integer.valueOf(1).equals(count);
        }
        checks.add(check("BANK_NUMBER_ISSUANCE_ROW",authorityReady,issued));
        boolean assetReady=columns(jdbc,"CLT_ASSET_COLLATERAL_LINK","COLLATERAL_ID","ASSET_ID","IS_CURRENT_FLAG")
                && columns(jdbc,"CLT_ASSET_IDENTITY","ASSET_ID","IDENTITY_STATUS","COLLATERAL_TYPE_ID");
        boolean assetVerified=false;
        if(assetReady && corePresent){
            Integer count=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM CLT.CLT_ASSET_COLLATERAL_LINK l
                      JOIN CLT.CLT_ASSET_IDENTITY a ON a.ASSET_ID=l.ASSET_ID
                     WHERE l.COLLATERAL_ID=? AND l.IS_CURRENT_FLAG='Y'
                       AND a.IDENTITY_STATUS='VERIFIED' AND a.COLLATERAL_TYPE_ID=?
                    """,Integer.class,num(collId),num(value(core.get("COLLATERAL_TYPE_ID"))));
            assetVerified=Integer.valueOf(1).equals(count);
        }
        checks.add(check("VERIFIED_ASSET_CURRENT_LINK",assetReady && corePresent,assetVerified));
        boolean draftReady=columns(jdbc,"CLT_INTRODUCTION_DRAFT","DRAFT_ID","PARTY_ID",
                "COLLATERAL_TYPE_ID","REQUEST_HASH","ESTIMATE_AMOUNT","CURRENCY_CODE",
                "VALID_FROM","VALID_TO","DEPOSIT_NUMBER","BRANCH_CODE");
        Map<String,Object> draft=null;
        if(draftReady){
            draft=first(jdbc.queryForList("""
                    SELECT DRAFT_ID,PARTY_ID,COLLATERAL_TYPE_ID,REQUEST_HASH,
                           ESTIMATE_AMOUNT,CURRENCY_CODE,VALID_FROM,VALID_TO,DEPOSIT_NUMBER
                      FROM CLT.CLT_INTRODUCTION_DRAFT WHERE DRAFT_ID=? AND BRANCH_CODE=?
                    """,num(value(ledger.get("DRAFT_ID"))),ledger.get("BRANCH_CODE")));
        }
        checks.add(check("IMMUTABLE_DRAFT_TO_CORE",draftReady && corePresent,
                CollateralRegistrationReadbackPolicy.matchesDraft(core,ledger,draft)));
        List<Map<String,Object>> attrs=List.of();
        boolean attributesTruncated=false;
        boolean attrsReady=columns(jdbc,"COLLATERAL_ATTRIBUTE_DEFINITION_ITEM",
                "COLLATERAL_ID","COLLATERAL_ATTRIBUTE_DEFINITION_ID","VALUE_SEQUENCE_NO",
                "NUMERIC_VALUE","TEXT_VALUE","DATE_VALUE");
        if(attrsReady && corePresent){
            List<Map<String,Object>> read=jdbc.queryForList("""
                    SELECT COLLATERAL_ATTRIBUTE_DEFINITION_ID,VALUE_SEQUENCE_NO,
                           NUMERIC_VALUE,TEXT_VALUE,DATE_VALUE
                      FROM CLT.COLLATERAL_ATTRIBUTE_DEFINITION_ITEM
                     WHERE COLLATERAL_ID=? ORDER BY COLLATERAL_ATTRIBUTE_DEFINITION_ID,VALUE_SEQUENCE_NO
                     FETCH FIRST 201 ROWS ONLY
                    """,num(collId)).stream().map(CollateralRegistrationReadbackService::normalized).toList();
            attributesTruncated=read.size()>200;
            attrs=attributesTruncated?read.subList(0,200):read;
        }
        checks.add(new Check("PERSISTED_ATTRIBUTE_VALUES",
                attributesTruncated?"NOT_VERIFIED":attrsReady && corePresent?"OBSERVED":"NOT_VERIFIED",
                attributesTruncated?"ATTRIBUTE_RESULT_TRUNCATED":attrsReady && corePresent
                    ?"ROWS="+attrs.size():"ATTRIBUTE_TABLE_NOT_OBSERVED"));
        List<Map<String,Object>> ranges=List.of();
        boolean rangesTruncated=false;
        boolean rangesReady=columns(jdbc,"CLT_COLLATERAL_SERIAL_RANGE","COLLATERAL_ID",
                "RANGE_SEQUENCE_NO","SERIAL_FROM","SERIAL_TO","ITEM_COUNT","RANGE_AMOUNT","CURRENCY_CODE");
        if(rangesReady && corePresent){
            List<Map<String,Object>> read=jdbc.queryForList("""
                    SELECT RANGE_SEQUENCE_NO,SERIAL_FROM,SERIAL_TO,ITEM_COUNT,RANGE_AMOUNT,CURRENCY_CODE
                      FROM CLT.CLT_COLLATERAL_SERIAL_RANGE
                     WHERE COLLATERAL_ID=? ORDER BY RANGE_SEQUENCE_NO FETCH FIRST 201 ROWS ONLY
                    """,num(collId)).stream().map(CollateralRegistrationReadbackService::normalized).toList();
            rangesTruncated=read.size()>200;
            ranges=rangesTruncated?read.subList(0,200):read;
        }
        boolean countabilityReady=columns(jdbc,"COLLATERAL_TYPE","COLLATERAL_TYPE_ID","IS_COUNTABLE_FLAG");
        String countability=null;
        if(countabilityReady && corePresent){
            List<String> flags=jdbc.query("""
                    SELECT IS_COUNTABLE_FLAG FROM CLT.COLLATERAL_TYPE
                     WHERE COLLATERAL_TYPE_ID=?
                    """,(rs,i)->rs.getString(1),num(value(core.get("COLLATERAL_TYPE_ID"))));
            if(flags.size()==1)countability=flags.getFirst();
        }
        String rangeState="NOT_VERIFIED";
        if(countability!=null && rangesReady && corePresent && !rangesTruncated){
            if("Y".equals(countability))rangeState=ranges.isEmpty()?"MISMATCH":"OBSERVED";
            else if("N".equals(countability))rangeState=ranges.isEmpty()?"NOT_APPLICABLE":"MISMATCH";
        }
        checks.add(new Check("PERSISTED_SERIAL_RANGES",rangeState,
                rangesTruncated?"SERIAL_RESULT_TRUNCATED":rangesReady && corePresent
                    ?"ROWS="+ranges.size()+"; COUNTABLE="+value(countability):"SERIAL_TABLE_NOT_OBSERVED"));
        String state=CollateralRegistrationReadbackPolicy.state(mode,corePresent,ledgerSchema,true,checks);
        // An OBSERVED child table is NOT proof that it matches the original approved draft.
        return new Report(mode,reviewId,value(ledger.get("COLLATERAL_ID")),
                value(ledger.get("COLLATERAL_NUMBER")),state,
                CollateralRegistrationReadbackPolicy.technical(state),false,
                "ORACLE_CLT_READ_ONLY",List.copyOf(checks),core==null?Map.of():core,
                List.copyOf(attrs),List.copyOf(ranges));
    }
}
