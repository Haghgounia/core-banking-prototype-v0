package com.behsazan.corebanking.fee3.oracle;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class Fee3Repository {
    private static final List<String> TABLES=List.of("FCE_CALC_ITEM","FCE_TARIFF_VERSION","FCE_VERSION_SCOPE",
        "FCE_CALC_RULE","FCE_TEST_CASE","FCE_AUDIT_EVENT","FCE_CHANGE_REQUEST",
        "FCE_REF_CODE","FCE_REF_FORMULA","FCE_REF_OPERATION","FCE_REF_SOURCE");
    private final JdbcClient db;
    private final String schema;

    public Fee3Repository(JdbcClient db, @Value("${FEE3_SCHEMA:FEE3}") String schema) {
        if(schema==null||!schema.matches("[A-Za-z][A-Za-z0-9_]{0,29}"))
            throw new IllegalArgumentException("Invalid FEE3 schema identifier");
        this.db=db;
        this.schema=schema.toUpperCase(java.util.Locale.ROOT);
    }
    private String table(String name){return schema+"."+name;}
    public String schema(){return schema;}
    public List<String> missingTables(){
        List<String> absent=new ArrayList<>();
        for(String t:TABLES) {
            Long count=db.sql("SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER=:s AND TABLE_NAME=:t")
                .param("s",schema).param("t",t).query(Long.class).single();
            if(count==0)absent.add(t);
        }
        return absent;
    }
    public LocalDate bankDate(){
        return db.sql("SELECT TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Tehran' AS DATE)) FROM DUAL")
            .query((rs,n)->rs.getDate(1).toLocalDate()).single();
    }
    public long count(String constantTable){
        if(!TABLES.contains(constantTable))throw new IllegalArgumentException("Unknown FEE3 table");
        return db.sql("SELECT COUNT(*) FROM "+table(constantTable)).query(Long.class).single();
    }
    public Page<CatalogRow> catalog(String search,String module,int page,int size){
        List<String> criteria=new ArrayList<>();Map<String,Object> params=new LinkedHashMap<>();
        if(search!=null&&!search.isBlank()) {
            criteria.add("(UPPER(I.CALC_CODE) LIKE :query OR UPPER(I.TITLE_FA) LIKE :query)");
            params.put("query","%"+search.strip().toUpperCase(java.util.Locale.ROOT)+"%");
        }
        if(module!=null&&!module.isBlank()){
            criteria.add("I.MODULE_CODE=:module");params.put("module",module.strip());
        }
        String where=criteria.isEmpty()?"":" WHERE "+String.join(" AND ",criteria);
        long total=db.sql("SELECT COUNT(*) FROM "+table("FCE_CALC_ITEM")+" I"+where)
            .params(params).query(Long.class).single();
        String query="SELECT I.CALC_ITEM_ID,I.CALC_CODE,I.TITLE_FA,I.MODULE_CODE,I.OPERATION_CODE,"+
            "I.AMOUNT_KIND_CODE,I.OWNER_DOMAIN_CODE,I.IS_ENABLED,I.RECORD_VERSION,"+
            "V.TARIFF_VERSION_ID,V.VERSION_NO,V.STATUS_CODE,V.VALID_FROM "+
            "FROM "+table("FCE_CALC_ITEM")+" I LEFT JOIN "+table("FCE_TARIFF_VERSION")+
            " V ON V.CALC_ITEM_ID=I.CALC_ITEM_ID AND V.VERSION_NO="+
            " (SELECT MAX(V2.VERSION_NO) FROM "+table("FCE_TARIFF_VERSION")+" V2 WHERE V2.CALC_ITEM_ID=I.CALC_ITEM_ID)"+
            where+" ORDER BY I.CALC_ITEM_ID DESC OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY";
        Map<String,Object> pagination=new LinkedHashMap<>(params);
        pagination.put("offset",page*size);pagination.put("size",size);
        List<CatalogRow> rows=db.sql(query).params(pagination).query((rs,n)->new CatalogRow(
            rs.getLong("CALC_ITEM_ID"),rs.getString("CALC_CODE"),rs.getString("TITLE_FA"),
            rs.getString("MODULE_CODE"),rs.getString("OPERATION_CODE"),rs.getString("AMOUNT_KIND_CODE"),
            rs.getString("OWNER_DOMAIN_CODE"),rs.getInt("IS_ENABLED")==1,rs.getLong("RECORD_VERSION"),
            nullableLong(rs,"TARIFF_VERSION_ID"),nullableInteger(rs,"VERSION_NO"),rs.getString("STATUS_CODE"),
            rs.getDate("VALID_FROM")==null?null:rs.getDate("VALID_FROM").toLocalDate())).list();
        return new Page<>(rows,total,page,size);
    }
    public List<Operation> operations(){
        return db.sql("SELECT OPERATION_CODE,MODULE_CODE,OPERATION_NAME_FA,ALLOWED_AMOUNT_KINDS_JSON,IS_ACTIVE "+
            "FROM "+table("FCE_REF_OPERATION")+" ORDER BY MODULE_CODE,OPERATION_CODE")
            .query((rs,n)->new Operation(rs.getString(1),rs.getString(2),rs.getString(3),
               parseCodeJson(rs.getString(4)),rs.getInt(5)==1)).list();
    }
    private static List<String> parseCodeJson(String raw) {
        try {
            tools.jackson.databind.JsonNode root=new tools.jackson.databind.json.JsonMapper().readTree(raw);
            if(!root.isArray())return List.of();
            List<String> result=new ArrayList<>();
            root.forEach(node->{if(node.isTextual())result.add(node.asText());});
            return List.copyOf(result);
        }catch(RuntimeException ex){return List.of();}
    }
    public Optional<Operation> activeOperation(String module,String operation){
        return operations().stream().filter(o->o.active() && o.module().equals(module) && o.code().equals(operation)).findFirst();
    }
    public List<Formula> formulas(){
        return db.sql("SELECT FORMULA_CODE,FORMULA_NAME_FA,METHOD_CODE,EXPRESSION_TEXT,INPUT_SCHEMA_JSON,"+
           "ELIGIBLE_CONTEXT_JSON,IS_ACTIVE FROM "+table("FCE_REF_FORMULA")+" ORDER BY FORMULA_CODE")
           .query((rs,n)->new Formula(rs.getString(1),rs.getString(2),rs.getString(3),
               rs.getString(4),rs.getString(5),rs.getString(6),rs.getInt(7)==1)).list();
    }
    public boolean activeFormula(String code){
        return db.sql("SELECT COUNT(*) FROM "+table("FCE_REF_FORMULA")+" WHERE FORMULA_CODE=:code AND IS_ACTIVE=1")
          .param("code",code).query(Long.class).single()>0;
    }
    public List<RefCode> codes(String group){
        return db.sql("SELECT CODE_GROUP,CODE_VALUE,LABEL_FA,IS_ACTIVE,SORT_NO FROM "+table("FCE_REF_CODE")+
            " WHERE CODE_GROUP=:groupCode ORDER BY SORT_NO NULLS LAST,CODE_VALUE")
            .param("groupCode",group).query((rs,n)->new RefCode(rs.getString(1),rs.getString(2),
            rs.getString(3),rs.getInt(4)==1,nullableInteger(rs,"SORT_NO"))).list();
    }
    public List<Source> sources(){
        return db.sql("SELECT SOURCE_ID,DOCUMENT_NO,DOCUMENT_TITLE_FA,SOURCE_TYPE_CODE,VALIDATION_STATUS FROM "+table("FCE_REF_SOURCE")+
            " ORDER BY SOURCE_ID FETCH NEXT 500 ROWS ONLY")
            .query((rs,n)->new Source(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5))).list();
    }
    public String sourceStatus(long sourceId){
        return db.sql("SELECT VALIDATION_STATUS FROM "+table("FCE_REF_SOURCE")+" WHERE SOURCE_ID=:id")
            .param("id",sourceId).query(String.class).optional().orElse(null);
    }
    public Optional<Map<String,Object>> itemByCode(String code) {
        return db.sql("SELECT * FROM "+table("FCE_CALC_ITEM")+" WHERE CALC_CODE=:code")
            .param("code",code).query(Fee3Repository::rowMap).optional();
    }
    public Optional<Map<String,Object>> itemByIdForUpdate(long id) {
        return db.sql("SELECT * FROM "+table("FCE_CALC_ITEM")+" WHERE CALC_ITEM_ID=:id FOR UPDATE")
            .param("id",id).query(Fee3Repository::rowMap).optional();
    }
    public long versionCount(long itemId){
        return db.sql("SELECT COUNT(*) FROM "+table("FCE_TARIFF_VERSION")+" WHERE CALC_ITEM_ID=:id")
            .param("id",itemId).query(Long.class).single();
    }
    public long executedTestCount(long versionId){
        return db.sql("SELECT COUNT(*) FROM "+table("FCE_TEST_CASE")+
            " WHERE TARIFF_VERSION_ID=:vid AND (TESTED_AT IS NOT NULL OR LAST_STATUS_CODE IS NOT NULL)")
            .param("vid",versionId).query(Long.class).single();
    }
    public Optional<Map<String,Object>> versionByIdForUpdate(long versionId,long itemId) {
        return db.sql("SELECT * FROM "+table("FCE_TARIFF_VERSION")+
            " WHERE TARIFF_VERSION_ID=:vid AND CALC_ITEM_ID=:iid FOR UPDATE")
            .param("vid",versionId).param("iid",itemId).query(Fee3Repository::rowMap).optional();
    }
    public Optional<ItemAggregate> aggregate(String code){
        Optional<Map<String,Object>> item=itemByCode(code);
        if(item.isEmpty())return Optional.empty();
        long itemId=((Number)item.get().get("CALC_ITEM_ID")).longValue();
        List<Map<String,Object>> versions=db.sql("SELECT * FROM "+table("FCE_TARIFF_VERSION")+
            " WHERE CALC_ITEM_ID=:id ORDER BY VERSION_NO DESC")
            .param("id",itemId).query(Fee3Repository::rowMap).list();
        List<VersionAggregate> result=new ArrayList<>();
        for(Map<String,Object> version:versions) {
            long v=((Number)version.get("TARIFF_VERSION_ID")).longValue();
            result.add(new VersionAggregate(version,children("FCE_CALC_RULE",v,"RULE_ORDER"),
                children("FCE_VERSION_SCOPE",v,"SCOPE_ID"),children("FCE_TEST_CASE",v,"TEST_CASE_ID")));
        }
        return Optional.of(new ItemAggregate(item.get(),result));
    }
    private List<Map<String,Object>> children(String tableName,long versionId,String orderBy){
        return db.sql("SELECT * FROM "+table(tableName)+" WHERE TARIFF_VERSION_ID=:id ORDER BY "+orderBy)
            .param("id",versionId).query(Fee3Repository::rowMap).list();
    }
    private static Map<String,Object> rowMap(ResultSet rs,int row) throws SQLException {
        ResultSetMetaData md=rs.getMetaData();Map<String,Object> data=new LinkedHashMap<>();
        for(int i=1;i<=md.getColumnCount();i++) {
            Object value=(md.getColumnType(i)==Types.CLOB||md.getColumnType(i)==Types.NCLOB)?rs.getString(i):rs.getObject(i);
            data.put(md.getColumnLabel(i).toUpperCase(java.util.Locale.ROOT),value);
        }
        return data;
    }
    private static Long nullableLong(ResultSet rs,String column) throws SQLException {
        long value=rs.getLong(column);return rs.wasNull()?null:value;
    }
    private static Integer nullableInteger(ResultSet rs,String column) throws SQLException {
        int value=rs.getInt(column);return rs.wasNull()?null:value;
    }
    private long sequence(String sequence){
        // All names here are internal constants, never request-controlled.
        return db.sql("SELECT "+schema+".SEQ_"+sequence+".NEXTVAL FROM DUAL").query(Long.class).single();
    }
    private static SqlParameterValue jsonClob(String value){return new SqlParameterValue(Types.CLOB,value);}
    private static Map<String,Object> jsonParams(ScopeDraft scope, String ch,String ct,String gr,String prod,String contract){
        Map<String,Object> p=new LinkedHashMap<>();
        p.put("channels",jsonClob(ch));p.put("customers",jsonClob(ct));p.put("groups",jsonClob(gr));
        p.put("products",jsonClob(prod));p.put("contracts",jsonClob(contract));
        return p;
    }
    public DraftReceipt insert(DraftRequest request,String actor,String financeJson,
                               String channels,String customers,String groups,String products,String contracts,
                               List<String> ruleJsons,List<String> testJsons,String auditAfter) {
        long itemId=sequence("FCE_CALC_ITEM"),versionId=sequence("FCE_TARIFF_VERSION");
        String code="F3-UAT-"+itemId;
        ItemDraft i=request.item();VersionDraft v=request.version();
        db.sql("INSERT INTO "+table("FCE_CALC_ITEM")+" (CALC_ITEM_ID,CALC_CODE,TITLE_FA,MODULE_CODE,OPERATION_CODE,"+
                "AMOUNT_KIND_CODE,SERVICE_VARIANT_CODE,OWNER_DOMAIN_CODE,IS_ENABLED,DESCRIPTION_FA,CREATED_BY) "+
                "VALUES (:id,:code,:title,:module,:operation,:kind,:variant,:owner,0,:description,:actor)")
            .param("id",itemId).param("code",code).param("title",i.title()).param("module",i.module())
            .param("operation",i.operation()).param("kind",i.amountKind()).param("variant",i.variant())
            .param("owner",i.ownerDomain()).param("description",i.description()).param("actor",actor).update();
        db.sql("INSERT INTO "+table("FCE_TARIFF_VERSION")+
            " (TARIFF_VERSION_ID,CALC_ITEM_ID,VERSION_NO,VALID_FROM,VALID_TO,STATUS_CODE,IS_ACTIVE,"+
            "TARIFF_ORIGIN_CODE,SOURCE_ID,BASE_METHOD_CODE,TAX_MODE_CODE,ROUNDING_CODE,FINANCIAL_POLICY_JSON,CHANGE_REASON,CREATED_BY)"+
            " VALUES (:vid,:iid,1,:fromDate,:toDate,'DRAFT',0,:origin,:source,:method,:tax,:round,:financial,:reason,:actor)")
            .param("vid",versionId).param("iid",itemId).param("fromDate",Date.valueOf(v.validFrom()))
            .param("toDate",v.validTo()==null?null:Date.valueOf(v.validTo()))
            .param("origin",v.origin()).param("source",v.sourceId()).param("method",v.method())
            .param("tax",v.taxMode()).param("round",v.rounding()).param("financial",jsonClob(financeJson))
            .param("reason",v.changeReason()).param("actor",actor).update();
        insertChildren(versionId,request,actor,channels,customers,groups,products,contracts,ruleJsons,testJsons);
        insertAudit(itemId,versionId,"UAT_DRAFT_CREATED",actor,null,auditAfter);
        return new DraftReceipt(itemId,versionId,code,1,1,1,"DRAFT",false);
    }
    public DraftReceipt update(DraftRequest request,String actor,String financeJson,
                               String channels,String customers,String groups,String products,String contracts,
                               List<String> ruleJsons,List<String> testJsons,String auditBefore,String auditAfter,
                               String code,int versionNo) {
        long itemId=request.calcItemId(),versionId=request.tariffVersionId();
        ItemDraft i=request.item();VersionDraft v=request.version();
        int itemCount=db.sql("UPDATE "+table("FCE_CALC_ITEM")+
            " SET TITLE_FA=:title,MODULE_CODE=:module,OPERATION_CODE=:operation,AMOUNT_KIND_CODE=:kind,"+
            "SERVICE_VARIANT_CODE=:variant,OWNER_DOMAIN_CODE=:owner,DESCRIPTION_FA=:description,"+
            "UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,RECORD_VERSION=RECORD_VERSION+1"+
            " WHERE CALC_ITEM_ID=:id AND RECORD_VERSION=:expected")
            .param("title",i.title()).param("module",i.module()).param("operation",i.operation())
            .param("kind",i.amountKind()).param("variant",i.variant()).param("owner",i.ownerDomain())
            .param("description",i.description()).param("actor",actor).param("id",itemId)
            .param("expected",request.expectedItemRecordVersion()).update();
        int versionCount=db.sql("UPDATE "+table("FCE_TARIFF_VERSION")+
            " SET VALID_FROM=:fromDate,VALID_TO=:toDate,TARIFF_ORIGIN_CODE=:origin,SOURCE_ID=:source,"+
            "BASE_METHOD_CODE=:method,TAX_MODE_CODE=:tax,ROUNDING_CODE=:round,"+
            "FINANCIAL_POLICY_JSON=:financial,CHANGE_REASON=:reason,UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=:actor,"+
            "RECORD_VERSION=RECORD_VERSION+1 WHERE TARIFF_VERSION_ID=:vid AND CALC_ITEM_ID=:iid"+
            " AND STATUS_CODE='DRAFT' AND RECORD_VERSION=:expected")
            .param("fromDate",Date.valueOf(v.validFrom())).param("toDate",v.validTo()==null?null:Date.valueOf(v.validTo()))
            .param("origin",v.origin()).param("source",v.sourceId()).param("method",v.method())
            .param("tax",v.taxMode()).param("round",v.rounding()).param("financial",jsonClob(financeJson))
            .param("reason",v.changeReason()).param("actor",actor).param("vid",versionId)
            .param("iid",itemId).param("expected",request.expectedVersionRecordVersion()).update();
        if(itemCount!=1||versionCount!=1)throw new IllegalStateException("Concurrent FEE3 draft modification");
        // Children belong exclusively to this UAT DRAFT version. Replace transactionally, never on Seed/ACTIVE versions.
        for(String child:List.of("FCE_TEST_CASE","FCE_VERSION_SCOPE","FCE_CALC_RULE"))
            db.sql("DELETE FROM "+table(child)+" WHERE TARIFF_VERSION_ID=:vid")
                .param("vid",versionId).update();
        insertChildren(versionId,request,actor,channels,customers,groups,products,contracts,ruleJsons,testJsons);
        insertAudit(itemId,versionId,"UAT_DRAFT_UPDATED",actor,auditBefore,auditAfter);
        return new DraftReceipt(itemId,versionId,code,versionNo,
            request.expectedItemRecordVersion()+1,request.expectedVersionRecordVersion()+1,"DRAFT",false);
    }
    private void insertChildren(long versionId,DraftRequest request,String actor,String channels,
                                String customers,String groups,String products,String contracts,
                                List<String> ruleJsons,List<String> testJsons) {
        ScopeDraft scope=request.scope();
        long scopeId=sequence("FCE_VERSION_SCOPE");
        db.sql("INSERT INTO "+table("FCE_VERSION_SCOPE")+
            " (SCOPE_ID,TARIFF_VERSION_ID,CHANNEL_CODES_JSON,CUSTOMER_TYPES_JSON,CUSTOMER_GROUPS_JSON,"+
            "PRODUCT_CODES_JSON,CONTRACT_TYPES_JSON,CURRENCY_CODE,PRIORITY_NO,CREATED_BY)"+
            " VALUES (:id,:vid,:channels,:customers,:groups,:products,:contracts,:currency,100,:actor)")
            .param("id",scopeId).param("vid",versionId).params(jsonParams(scope,channels,customers,groups,products,contracts))
            .param("currency",scope.currency()).param("actor",actor).update();
        List<RuleDraft> rules=request.rules()==null?List.of():request.rules();
        for(int n=0;n<rules.size();n++){
            RuleDraft r=rules.get(n);
            db.sql("INSERT INTO "+table("FCE_CALC_RULE")+
                " (RULE_ID,TARIFF_VERSION_ID,RULE_ORDER,METHOD_CODE,AMOUNT_BASE_CODE,RATE_SOURCE_CODE,"+
                "RATE_PERCENT,FIXED_AMOUNT_IRR,COLLATERAL_TYPE_CODE,COVERAGE_PERCENT,MIN_FEE_IRR,MAX_FEE_IRR,"+
                "FORMULA_CODE,PARAMETERS_JSON,CREATED_BY) VALUES"+
                " (:id,:vid,:ord,:method,:base,:rateSource,:rate,:fixed,:collateral,:coverage,:min,:max,:formula,:json,:actor)")
                .param("id",sequence("FCE_CALC_RULE")).param("vid",versionId).param("ord",r.order())
                .param("method",r.method()).param("base",r.base()).param("rateSource",r.rateSource())
                .param("rate",r.rate()).param("fixed",r.fixed()).param("collateral",r.collateral())
                .param("coverage",r.coverage()).param("min",r.min()).param("max",r.max())
                .param("formula",r.formulaCode()).param("json",jsonClob(ruleJsons.get(n))).param("actor",actor).update();
        }
        List<TestDraft> tests=request.tests()==null?List.of():request.tests();
        for(int n=0;n<tests.size();n++){
            TestDraft t=tests.get(n);
            db.sql("INSERT INTO "+table("FCE_TEST_CASE")+
                " (TEST_CASE_ID,TARIFF_VERSION_ID,TEST_TITLE_FA,REQUEST_JSON,EXPECTED_IRR,CREATED_BY)"+
                " VALUES (:id,:vid,:title,:json,:expected,:actor)")
                .param("id",sequence("FCE_TEST_CASE")).param("vid",versionId).param("title",t.title())
                .param("json",jsonClob(testJsons.get(n))).param("expected",t.expected()).param("actor",actor).update();
        }
    }
    private void insertAudit(long itemId,long versionId,String action,String actor,String before,String after){
        db.sql("INSERT INTO "+table("FCE_AUDIT_EVENT")+
            " (AUDIT_EVENT_ID,CALC_ITEM_ID,TARIFF_VERSION_ID,ACTION_CODE,ACTOR_REF,EVENT_AT,DETAILS_JSON,"+
            "BEFORE_JSON,AFTER_JSON,CREATED_BY) VALUES (:id,:iid,:vid,:action,:actor,SYSTIMESTAMP,:details,:before,:after,:actor)")
            .param("id",sequence("FCE_AUDIT_EVENT")).param("iid",itemId).param("vid",versionId)
            .param("action",action).param("actor",actor)
            .param("details",jsonClob("{\"scope\":\"UAT_DRAFT_ONLY\",\"financialEffect\":false}"))
            .param("before",before==null?null:jsonClob(before)).param("after",jsonClob(after)).update();
    }
}
