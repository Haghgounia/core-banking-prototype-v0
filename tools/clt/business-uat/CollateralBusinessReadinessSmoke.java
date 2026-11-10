import com.behsazan.corebanking.collateral.uat.CollateralBusinessReadinessPolicy;
import java.util.*;
/** No Spring/Oracle: regression of Collateral BUC metadata semantics, not business acceptance. */
public class CollateralBusinessReadinessSmoke {
  static int checks=0;
  static void yes(boolean ok,String id){ if(!ok) throw new AssertionError(id);checks++; }
  public static void main(String[]args){
    var all=CollateralBusinessReadinessPolicy.allRequiredTables();
    yes(all.size()>=20,"BUC_UAT_ALL_REQUIRED_PHYSICAL_TABLES");
    var complete=CollateralBusinessReadinessPolicy.evaluate(all,true);
    yes(complete.size()==16,"BUC_UAT_CATALOG_16");
    Set<String> unique=new HashSet<>();
    for(int i=0;i<16;i++){
      String id=String.format("BUC-%02d",i+1);
      var item=complete.get(i);
      yes(id.equals(item.buc()),"BUC_UAT_STABLE_BUC_ORDER_"+id);
      yes(unique.add(item.buc()),"BUC_UAT_DUPLICATE_BUC_"+id);
      yes("PRESENT".equals(item.schemaState())&&item.missingTables().isEmpty(),"BUC_UAT_ALL_TABLES_PRESENT_"+id);
      yes(!item.requiredTables().isEmpty(),"BUC_UAT_NONEMPTY_REQUIRED_"+id);
    }
    var noTables=CollateralBusinessReadinessPolicy.evaluate(Set.of(),true);
    for(var item:noTables){
      yes("MISSING".equals(item.schemaState()),"BUC_UAT_MISSING_STATE_"+item.buc());
      yes(item.missingTables().equals(item.requiredTables()),"BUC_UAT_MISSING_LIST_"+item.buc());
    }
    var offline=CollateralBusinessReadinessPolicy.evaluate(Set.of(),false);
    for(var item:offline){
      yes("UNKNOWN".equals(item.schemaState()),"BUC_UAT_UNKNOWN_STATE_"+item.buc());
      yes(item.missingTables().isEmpty(),"BUC_UAT_UNKNOWN_IS_NOT_MISSING_"+item.buc());
    }
    var partial=CollateralBusinessReadinessPolicy.evaluate(Set.of("COLLATERAL"),true);
    yes("PRESENT".equals(partial.get(1).schemaState()),"BUC_UAT_CORE_OWNER_TABLE_PRESENT");
    yes("MISSING".equals(partial.get(8).schemaState()),"BUC_UAT_POSTING_REQUIRES_APPROVED_TABLES");
    yes("MISSING".equals(partial.get(14).schemaState()),"BUC_UAT_CROSS_UNIT_REQUIRES_GRANTS");
    yes("UNKNOWN".equals(CollateralBusinessReadinessPolicy.evaluate(null,false).get(0).schemaState()),"BUC_UAT_NULL_OFFLINE");
    try {all.add("UNAUTHORIZED");throw new AssertionError("BUC_UAT_REQUIRED_TABLE_SET_MUTABLE");}
    catch(UnsupportedOperationException expected){checks++;}
    System.out.println("CLT_BUC_UAT_POLICY_SMOKE_PASS="+checks+"/"+checks);
  }
}
