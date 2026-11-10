import com.behsazan.corebanking.collateral.uat.CollateralR6ReadinessPolicy;
import java.util.*;
/** No Spring/Oracle: regression of R6 metadata semantics, not business acceptance. */
public class R6BusinessReadinessPolicySmoke {
  static int checks=0;
  static void yes(boolean ok,String id){ if(!ok) throw new AssertionError(id);checks++; }
  public static void main(String[]args){
    var all=CollateralR6ReadinessPolicy.allRequiredTables();
    yes(all.size()>=20,"R6_ALL_REQUIRED_PHYSICAL_TABLES");
    var complete=CollateralR6ReadinessPolicy.evaluate(all,true);
    yes(complete.size()==16,"R6_CATALOG_16");
    Set<String> unique=new HashSet<>();
    for(int i=0;i<16;i++){
      String id=String.format("BUC-%02d",i+1);
      var item=complete.get(i);
      yes(id.equals(item.buc()),"R6_STABLE_BUC_ORDER_"+id);
      yes(unique.add(item.buc()),"R6_DUPLICATE_BUC_"+id);
      yes("PRESENT".equals(item.schemaState())&&item.missingTables().isEmpty(),"R6_ALL_TABLES_PRESENT_"+id);
      yes(!item.requiredTables().isEmpty(),"R6_NONEMPTY_REQUIRED_"+id);
    }
    var noTables=CollateralR6ReadinessPolicy.evaluate(Set.of(),true);
    for(var item:noTables){
      yes("MISSING".equals(item.schemaState()),"R6_MISSING_STATE_"+item.buc());
      yes(item.missingTables().equals(item.requiredTables()),"R6_MISSING_LIST_"+item.buc());
    }
    var offline=CollateralR6ReadinessPolicy.evaluate(Set.of(),false);
    for(var item:offline){
      yes("UNKNOWN".equals(item.schemaState()),"R6_UNKNOWN_STATE_"+item.buc());
      yes(item.missingTables().isEmpty(),"R6_UNKNOWN_IS_NOT_MISSING_"+item.buc());
    }
    var partial=CollateralR6ReadinessPolicy.evaluate(Set.of("COLLATERAL"),true);
    yes("PRESENT".equals(partial.get(1).schemaState()),"R6_CORE_OWNER_TABLE_PRESENT");
    yes("MISSING".equals(partial.get(8).schemaState()),"R6_POSTING_REQUIRES_APPROVED_TABLES");
    yes("MISSING".equals(partial.get(14).schemaState()),"R6_CROSS_UNIT_REQUIRES_GRANTS");
    yes("UNKNOWN".equals(CollateralR6ReadinessPolicy.evaluate(null,false).get(0).schemaState()),"R6_NULL_OFFLINE");
    try {all.add("UNAUTHORIZED");throw new AssertionError("R6_REQUIRED_TABLE_SET_MUTABLE");}
    catch(UnsupportedOperationException expected){checks++;}
    System.out.println("CLT_R6_POLICY_SMOKE_PASS="+checks+"/"+checks);
  }
}
