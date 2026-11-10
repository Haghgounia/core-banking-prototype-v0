import com.behsazan.corebanking.collateral.reconciliation.CollateralOracleReconciliationPolicy;
import java.util.HashSet;
import java.util.Set;

/** Deterministic policy regression; intentionally not a claim of an Oracle live test. */
public final class CollateralOracleReconciliationSmoke {
    static int assertions;
    static void check(boolean value,String name) {if(!value)throw new AssertionError(name);assertions++;}
    public static void main(String[] args) {
        CollateralOracleReconciliationPolicy.validateCatalog();
        var tables=CollateralOracleReconciliationPolicy.REFERENCE_TABLES;
        check(tables.size()==17,"ALL_17_REFERENCES");
        check(new HashSet<>(tables).size()==17,"UNIQUE_REFERENCE_NAMES");
        check(tables.contains("REF_COLL_TXN_STATUS"),"STATUS_REFERENCE");
        check("UNKNOWN".equals(CollateralOracleReconciliationPolicy.visibility(false,Set.of(),"COLLATERAL")),"UNAVAILABLE_NOT_EMPTY");
        check("NOT_VISIBLE".equals(CollateralOracleReconciliationPolicy.visibility(true,Set.of(),"COLLATERAL")),"INVISIBLE_NOT_MISSING");
        check("VISIBLE".equals(CollateralOracleReconciliationPolicy.visibility(true,Set.of("COLLATERAL"),"COLLATERAL")),"VISIBLE");
        check(CollateralOracleReconciliationPolicy.observedReference("REF_CURRENCY",0,0).dataState().equals("OBSERVED_EMPTY"),"EMPTY_IS_OBSERVED");
        check(CollateralOracleReconciliationPolicy.unknownReference("REF_CURRENCY","VISIBLE").totalRows()==null,"UNKNOWN_NOT_ZERO");
        check(CollateralOracleReconciliationPolicy.observedReference("REF_CURRENCY",4,2).activeRows()==2,"ACTIVE_COUNT");
        for(String text:new String[]{"'PENDING'","  'PENDING'  ","'pending_approval'"})
            check(CollateralOracleReconciliationPolicy.canonicalDefault(text)!=null,"DEFAULT_PARSE");
        check(CollateralOracleReconciliationPolicy.transactionDefault(0L,1L,"PENDING").state().equals("BLOCKED"),"G16_MISMATCH");
        check(CollateralOracleReconciliationPolicy.transactionDefault(1L,0L,"PENDING").state().equals("OBSERVED"),"G16_MATCH");
        check(CollateralOracleReconciliationPolicy.transactionDefault(null,1L,"PENDING").state().equals("UNKNOWN"),"G16_UNKNOWN");
        check(CollateralOracleReconciliationPolicy.binary("G17",null).state().equals("UNKNOWN"),"UNKNOWN_NOT_BLOCKED");
        check(CollateralOracleReconciliationPolicy.binary("G17",false).state().equals("BLOCKED"),"NEGATIVE");
        check(CollateralOracleReconciliationPolicy.binary("G17",true).state().equals("OBSERVED"),"TECHNICAL_NOT_BANK");
        try {CollateralOracleReconciliationPolicy.observedReference("REF_CURRENCY",1,2);throw new AssertionError("IMPOSSIBLE_COUNT");}
        catch(IllegalArgumentException expected){assertions++;}
        System.out.println("CLT_RECONCILIATION_POLICY_PASS="+assertions+"/"+assertions);
    }
}
