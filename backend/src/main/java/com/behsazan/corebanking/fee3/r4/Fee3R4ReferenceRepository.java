package com.behsazan.corebanking.fee3.r4;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** SELECT-only, fail-closed proof that a client-submitted draft snapshot matches Oracle R2 UAT rows. */
@Repository
public class Fee3R4ReferenceRepository {
    private final JdbcClient db;
    private final String prefix;
    private final JsonMapper mapper = new JsonMapper();
    public Fee3R4ReferenceRepository(JdbcClient db,
             com.behsazan.corebanking.fee3.oracle.Fee3Repository original){
        this.db=db;
        this.prefix=original.schema()+".";
    }
    public Fee3R4CalculationContract.VersionReference resolve(DraftRequest draft){
        if(draft.calcItemId()==null&&draft.tariffVersionId()==null)
            return new Fee3R4CalculationContract.VersionReference(
                "UNPERSISTED_CLIENT_DRAFT",null,null,null,null,"UNSAVED",false,0);
        if(draft.calcItemId()==null||draft.tariffVersionId()==null||
           draft.expectedItemRecordVersion()==null||draft.expectedVersionRecordVersion()==null)
            throw bad("ORACLE_DRAFT_REFERENCE_INCOMPLETE");
        long itemId=draft.calcItemId(), versionId=draft.tariffVersionId();
        List<VersionRow> matches=db.sql("SELECT I.CALC_ITEM_ID,I.CALC_CODE,I.TITLE_FA,I.MODULE_CODE,"+
            "I.OPERATION_CODE,I.AMOUNT_KIND_CODE,I.SERVICE_VARIANT_CODE,I.OWNER_DOMAIN_CODE,"+
            "I.DESCRIPTION_FA,I.RECORD_VERSION AS ITEM_RV,"+
            "V.TARIFF_VERSION_ID,V.VERSION_NO,V.STATUS_CODE,V.IS_ACTIVE,"+
            "V.VALID_FROM,V.VALID_TO,V.TARIFF_ORIGIN_CODE,V.SOURCE_ID,V.BASE_METHOD_CODE,"+
            "V.TAX_MODE_CODE,V.ROUNDING_CODE,V.FINANCIAL_POLICY_JSON,V.RECORD_VERSION AS VERSION_RV "+
            "FROM "+prefix+"FCE_CALC_ITEM I JOIN "+prefix+"FCE_TARIFF_VERSION V"+
            " ON V.CALC_ITEM_ID=I.CALC_ITEM_ID WHERE I.CALC_ITEM_ID=:iid"+
            " AND V.TARIFF_VERSION_ID=:vid")
            .param("iid",itemId).param("vid",versionId)
            .query(Fee3R4ReferenceRepository::versionRow).list();
        if(matches.size()!=1)throw bad("ORACLE_DRAFT_REFERENCE_NOT_FOUND");
        VersionRow r=matches.get(0);ItemDraft i=draft.item();VersionDraft v=draft.version();
        if(!"DRAFT".equals(r.status)||r.active!=0)throw bad("NON_DRAFT_REFERENCE_FORBIDDEN");
        if(r.itemRv!=draft.expectedItemRecordVersion()||r.versionRv!=draft.expectedVersionRecordVersion())
            throw bad("STALE_ORACLE_DRAFT_REFERENCE");
        if(!Objects.equals(r.title,i.title())||!Objects.equals(r.module,i.module())||
           !Objects.equals(r.operation,i.operation())||!Objects.equals(r.amountKind,i.amountKind())||
           !Objects.equals(r.variant,i.variant())||!Objects.equals(r.ownerDomain,i.ownerDomain())||
           !Objects.equals(r.description,i.description())||!Objects.equals(r.validFrom,v.validFrom())||
           !Objects.equals(r.validTo,v.validTo())||!Objects.equals(r.origin,v.origin())||
           !Objects.equals(r.sourceId,v.sourceId())||!Objects.equals(r.method,v.method())||
           !Objects.equals(r.taxMode,v.taxMode())||!Objects.equals(r.rounding,v.rounding()))
            throw bad("ORACLE_DRAFT_HEADER_DIFFERS_FROM_REQUEST");
        if(!financialMatches(r.financialJson,v))throw bad("ORACLE_FINANCIAL_POLICY_DIFFERS_FROM_REQUEST");
        checkScopes(versionId,draft.scope());
        checkRules(versionId,draft.rules());
        return new Fee3R4CalculationContract.VersionReference(
            "ORACLE_CALCULATION_FIELDS_VERIFIED",itemId,versionId,r.itemRv,r.versionRv,
            "DRAFT",false,r.versionNo);
    }
    private boolean financialMatches(String raw,VersionDraft v){
        JsonNode tree=parse(raw);
        if(tree==null||!tree.isObject())return false;
        JsonNode ui=tree.get("_fee3UiV1");
        if(ui==null||!ui.isObject()||ui.path("contractVersion").asInt()!=1 ||
           !v.serviceCurrency().equals(ui.path("serviceCurrency").asText())||
           !v.feeCurrency().equals(ui.path("feeCurrency").asText())||
           !jsonEqual(ui.get("context"),v.uiContext()))return false;
        // The server-owned _fee3UiV1 envelope cannot be supplied by the caller.
        if(v.financialPolicy()!=null&&v.financialPolicy().containsKey("_fee3UiV1"))return false;
        JsonNode copy=parse(raw);
        if(!(copy instanceof tools.jackson.databind.node.ObjectNode object))return false;
        object.remove("_fee3UiV1");
        return jsonEqual(copy,v.financialPolicy()==null?Map.of():v.financialPolicy());
    }
    private void checkScopes(long versionId,ScopeDraft requested){
        if(requested==null)throw bad("ORACLE_SCOPE_MISSING");
        List<ScopeRow> rows=db.sql("SELECT CHANNEL_CODES_JSON,CUSTOMER_TYPES_JSON,CUSTOMER_GROUPS_JSON,"+
            "PRODUCT_CODES_JSON,CONTRACT_TYPES_JSON,CURRENCY_CODE FROM "+prefix+"FCE_VERSION_SCOPE"+
            " WHERE TARIFF_VERSION_ID=:vid")
            .param("vid",versionId).query((rs,n)->new ScopeRow(rs.getString(1),rs.getString(2),
                rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6))).list();
        if(rows.size()!=1)throw bad("ORACLE_SCOPE_NOT_UNIQUE");
        ScopeRow s=rows.get(0);
        if(!Objects.equals(s.currency,requested.currency())||
           !jsonEqual(parse(s.channels),requested.channels())||
           !jsonEqual(parse(s.customers),requested.customers())||
           !jsonEqual(parse(s.groups),requested.groups()==null?List.of("ALL"):requested.groups())||
           !jsonEqual(parse(s.products),requested.products()==null?List.of("ALL"):requested.products())||
           !jsonEqual(parse(s.contracts),requested.contracts()==null?List.of():requested.contracts()))
            throw bad("ORACLE_SCOPE_DIFFERS_FROM_REQUEST");
    }
    private void checkRules(long versionId,List<RuleDraft> requested){
        if(requested==null||requested.size()!=1)throw bad("SINGLE_RULE_REQUIRED_FOR_ORACLE_PROOF");
        List<RuleRow> rows=db.sql("SELECT RULE_ORDER,METHOD_CODE,AMOUNT_BASE_CODE,RATE_SOURCE_CODE,"+
            "RATE_PERCENT,FIXED_AMOUNT_IRR,COLLATERAL_TYPE_CODE,COVERAGE_PERCENT,MIN_FEE_IRR,"+
            "MAX_FEE_IRR,FORMULA_CODE,PARAMETERS_JSON FROM "+prefix+"FCE_CALC_RULE"+
            " WHERE TARIFF_VERSION_ID=:vid")
            .param("vid",versionId).query((rs,n)->new RuleRow(rs.getInt(1),rs.getString(2),
                rs.getString(3),rs.getString(4),rs.getBigDecimal(5),rs.getBigDecimal(6),
                rs.getString(7),rs.getBigDecimal(8),rs.getBigDecimal(9),rs.getBigDecimal(10),
                rs.getString(11),rs.getString(12))).list();
        if(rows.size()!=1)throw bad("ORACLE_RULE_NOT_UNIQUE");
        RuleRow r=rows.get(0);RuleDraft q=requested.get(0);
        if(r.order!=q.order()||!Objects.equals(r.method,q.method())||
           !Objects.equals(r.amountBase,q.base())||!Objects.equals(r.rateSource,q.rateSource())||
           !numberEqual(r.rate,q.rate())||!numberEqual(r.fixed,q.fixed())||
           !Objects.equals(r.collateral,q.collateral())||!numberEqual(r.coverage,q.coverage())||
           !numberEqual(r.minimum,q.min())||!numberEqual(r.maximum,q.max())||
           !Objects.equals(r.formulaCode,q.formulaCode())||
           !jsonEqual(parse(r.params),q.parameters()==null?Map.of():q.parameters()))
            throw bad("ORACLE_RULE_DIFFERS_FROM_REQUEST");
    }
    private boolean jsonEqual(JsonNode tree,Object other){
        if(tree==null)return other==null;
        try{return tree.equals(mapper.readTree(mapper.writeValueAsString(other)));}
        catch(RuntimeException e){return false;}
    }
    private JsonNode parse(String raw){
        if(raw==null)return null;
        try{return mapper.readTree(raw);}catch(RuntimeException e){return null;}
    }
    private static boolean numberEqual(BigDecimal a,BigDecimal b){
        return a==null?b==null:b!=null&&a.compareTo(b)==0;
    }
    private static IllegalArgumentException bad(String code){return new IllegalArgumentException(code);}
    private record VersionRow(long itemId,String calcCode,String title,String module,String operation,
        String amountKind,String variant,String ownerDomain,String description,long itemRv,long versionId,
        int versionNo,String status,int active,LocalDate validFrom,LocalDate validTo,String origin,
        Long sourceId,String method,String taxMode,String rounding,String financialJson,long versionRv) {}
    private record ScopeRow(String channels,String customers,String groups,String products,
                            String contracts,String currency) {}
    private record RuleRow(int order,String method,String amountBase,String rateSource,BigDecimal rate,
        BigDecimal fixed,String collateral,BigDecimal coverage,BigDecimal minimum,BigDecimal maximum,
        String formulaCode,String params) {}
    private static VersionRow versionRow(ResultSet rs,int n)throws SQLException{
        return new VersionRow(rs.getLong("CALC_ITEM_ID"),rs.getString("CALC_CODE"),rs.getString("TITLE_FA"),
            rs.getString("MODULE_CODE"),rs.getString("OPERATION_CODE"),rs.getString("AMOUNT_KIND_CODE"),
            rs.getString("SERVICE_VARIANT_CODE"),rs.getString("OWNER_DOMAIN_CODE"),rs.getString("DESCRIPTION_FA"),
            rs.getLong("ITEM_RV"),rs.getLong("TARIFF_VERSION_ID"),rs.getInt("VERSION_NO"),
            rs.getString("STATUS_CODE"),rs.getInt("IS_ACTIVE"),date(rs,"VALID_FROM"),date(rs,"VALID_TO"),
            rs.getString("TARIFF_ORIGIN_CODE"),nullableLong(rs,"SOURCE_ID"),
            rs.getString("BASE_METHOD_CODE"),rs.getString("TAX_MODE_CODE"),rs.getString("ROUNDING_CODE"),
            rs.getString("FINANCIAL_POLICY_JSON"),rs.getLong("VERSION_RV"));
    }
    private static LocalDate date(ResultSet rs,String column)throws SQLException{
        java.sql.Date value=rs.getDate(column);return value==null?null:value.toLocalDate();
    }
    private static Long nullableLong(ResultSet rs,String column)throws SQLException{
        long value=rs.getLong(column);return rs.wasNull()?null:value;
    }
}
