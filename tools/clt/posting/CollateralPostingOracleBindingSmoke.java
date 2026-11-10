import com.behsazan.corebanking.collateral.posting.CollateralPostingRepository;
import com.behsazan.corebanking.collateral.posting.CollateralPostingRepository.Permit;
import com.behsazan.corebanking.collateral.posting.CollateralPostingRepository.Allocation;
import org.springframework.jdbc.core.JdbcTemplate;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.*;

/** Source repository + recording JDBC stub. Never establishes a database connection. */
public class CollateralPostingOracleBindingSmoke {
    static int tests=0;
    static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);tests++;}
    static class Recorder extends JdbcTemplate {
        String sql;Object[] values;
        String countSql;int affected=1;
        Recorder(){super(null);}
        @Override public int update(String sql,Object... values){this.sql=sql;this.values=values;return affected;}
        @Override public <T> T queryForObject(String sql,Class<T> type,Object... args){
            this.countSql=sql;return type.cast(Integer.valueOf(0));
        }
    }
    static Permit permit()throws Exception {
        RecordComponent[] comps=Permit.class.getRecordComponents();Object[] values=new Object[comps.length];
        for(int i=0;i<comps.length;i++){
            String name=comps[i].getName();Class<?> type=comps[i].getType();
            if(type==String.class) values[i]="v";
            else if(type==BigDecimal.class) values[i]=new BigDecimal("12.3400");
            else if(type==boolean.class)values[i]=true;
            else values[i]=null;
            if(name.equals("collateralId"))values[i]="12";
            if(name.equals("ownerBranch"))values[i]="100";
            if(name.equals("consumerBranch"))values[i]="100";
            if(name.equals("moduleId"))values[i]="2";
            if(name.equals("fileNumber"))values[i]="30";
            if(name.equals("transactionNumber"))values[i]="T-100";
            if(name.equals("sourceRequestId"))values[i]="7";
            if(name.equals("originalTransactionId"))values[i]="22";
            if(name.equals("currency"))values[i]="IRR";
            if(name.equals("amount"))values[i]=new BigDecimal("12.3400");
        }
        var types=Arrays.stream(comps).map(RecordComponent::getType).toArray(Class<?>[]::new);
        return Permit.class.getDeclaredConstructor(types).newInstance(values);
    }
    public static void main(String[] unused)throws Exception {
        var repo=new CollateralPostingRepository();var jdbc=new Recorder();var p=permit();
        repo.insertAllocation(jdbc,"123",p,"BANK_SVC");
        check(jdbc.sql.contains("INSERT INTO CLT.COLLATERAL_ALLOCATION"),"allocation insert");
        check(jdbc.sql.contains("CREATED_BY"),"allocation creator physical column");
        check(jdbc.values.length==8,"allocation 8 JDBC binds");
        check("BANK_SVC".equals(jdbc.values[7]),"allocation creator trusted identity");
        var a=new Allocation("123","12","2","30","IRR",new BigDecimal("12.3400"),true,"1");
        repo.updateAllocation(jdbc,a,new BigDecimal("2.3400"),"BANK_SVC");
        check(jdbc.sql.contains("UPDATED_BY=?"),"unallocation change has author");
        check(jdbc.values.length==7,"unallocation 7 JDBC binds");
        check("YES".equals(jdbc.values[1]),"partial remains in use");
        check("BANK_SVC".equals(jdbc.values[2]),"partial recorded author");
        repo.updateAllocation(jdbc,a,BigDecimal.ZERO,"BANK_SVC");
        check("NO".equals(jdbc.values[1]),"full release marks unused");
        check(new BigDecimal("12.3400").compareTo((BigDecimal)jdbc.values[0])==0,"full release keeps original amount for history");
        repo.insertTransaction(jdbc,"800","123",p,"ALLOCATION","POSTED","CREDIT","CLT_POST", "123e4567-e89b-12d3-a456-426614174000","BANK_SVC");
        check(jdbc.countSql.contains("COLLATERAL_TRANSACTION_NUMBER"),"bank transaction collision precheck");
        check(jdbc.sql.contains("INSERT INTO CLT.COLLATERAL_TRANSACTION"),"financial transaction insert");
        check(jdbc.values.length==16,"financial transaction 16 JDBC binds");
        check("CLT_POST".equals(jdbc.values[10]),"source system bound");
        check("123e4567-e89b-12d3-a456-426614174000".equals(jdbc.values[11]),"idempotency bound");
        check("BANK_SVC".equals(jdbc.values[12]),"posting actor bound");
        check("100".equals(jdbc.values[13]) && "100".equals(jdbc.values[14]),"owner and actual executing branch bound");
        check("7".equals(jdbc.values[15]),"source-stage business reference bound");
        repo.insertInitialTransactionStatus(jdbc,"800","POSTED","BANK_SVC");
        check(jdbc.sql.contains("INSERT INTO CLT.COLLATERAL_TXN_STATUS_HISTORY"),"journal insert");
        check(jdbc.values.length==4,"status journal 4 JDBC binds");
        check("POSTED".equals(jdbc.values[1]),"journal status matches transaction");
        check("BANK_SVC".equals(jdbc.values[2]) && "BANK_SVC".equals(jdbc.values[3]),"journal attributed to actor");
        jdbc.affected=0;
        boolean denied=false;
        try{repo.insertInitialTransactionStatus(jdbc,"801","POSTED","BANK_SVC");}catch(org.springframework.web.server.ResponseStatusException ignored){denied=true;}
        check(denied,"journal cannot silently vanish");
        System.out.println("CLT_BUC09_10_JDBC_RECORDING_SMOKE_PASS="+tests+"/"+tests);
    }
}
