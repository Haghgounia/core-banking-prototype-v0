package com.behsazan.corebanking.collateral.posting;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

/** No Oracle dependency: verifies the three CAS Stage mutations and CU grant lock SQL.
 *  Uses the exact production repository; never claims bank DB acceptance. */
public final class CollateralLifecycleRepositorySmoke {
    private static int checks=0;
    private static void pass(boolean value,String message){
        if(!value)throw new AssertionError(message);checks++;
    }
    private static final class FakeJdbc extends JdbcTemplate {
        String sql="";
        Object[] args={};
        int writeCount=1;
        List<String> reads=List.of("17");
        FakeJdbc(){super((DataSource)null);}
        @Override public int update(String statement,Object...values){
            this.sql=statement;this.args=values;return writeCount;
        }
        @SuppressWarnings("unchecked")
        @Override public <T> List<T> query(String statement,RowMapper<T> mapper,Object...values){
            this.sql=statement;this.args=values;
            return (List<T>)reads;
        }
    }
    private static CollateralPostingRepository.Permit permit(String operation,String owner,String consumer){
        return new CollateralPostingRepository.Permit(
                "7",operation,"17",owner,consumer,"BANK_CLIENT","LOANS","8","91",
                "CASE-A",new BigDecimal("350.2500"),"IRR","5",null,"43",
                new BigDecimal("999.0000"),"4","33","17","0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "OBL-3","POL-3","abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123",
                "TXN-34","FULL","MAKER","CHECKER","ISSUER",true,true,true,
                "APPROVED",LocalDateTime.now().plusDays(1),null,null,null,null,null,null);
    }
    public static void main(String[] unused){
        var repo=new CollateralPostingRepository();var db=new FakeJdbc();
        repo.consumeReviewedStage(db,permit("ALLOCATE","101","101"),"501","41");
        pass(db.sql.contains("UPDATE CLT.CLT_ALLOCATION_REQUEST"),"owner stage table");
        pass(db.sql.contains("REQUEST_STATUS='RECORDED_NOT_ALLOCATED'"),"owner stage CAS");
        pass(db.sql.contains("REQUEST_STATUS='POSTED'"),"owner stage posted");
        pass(db.sql.contains("POSTED_TRANSACTION_ID"),"owner durable txn pointer");
        pass(db.args.length==7,"owner stage exact bind count");
        pass(new BigDecimal("501").equals(db.args[0]),"trusted txn binding");
        pass("BANK_CLIENT".equals(db.args[5]),"owner client proof");
        repo.consumeReviewedStage(db,permit("ALLOCATE","101","202"),"502","42");
        pass(db.sql.contains("UPDATE CLT.CLT_CROSS_UNIT_INTENT"),"cross-unit stage table");
        pass(db.sql.contains("POSTED_AT_UTC=SYS_EXTRACT_UTC"),"UTC cross-unit audit");
        pass(db.sql.contains("INTENT_STATUS='RECORDED_NOT_ALLOCATED'"),"cross-unit stage CAS");
        pass(db.args.length==8,"cross-unit exact bind count");
        pass("202".equals(db.args[5]),"cross-unit consuming branch");
        repo.consumeReviewedStage(db,permit("UNALLOCATE","101","101"),"503","43");
        pass(db.sql.contains("UPDATE CLT.CLT_UNALLOC_REQ"),"unallocation stage table");
        pass(db.sql.contains("REQUEST_STATUS='RECORDED_NOT_UNALLOCATED'"),"unallocation stage CAS");
        pass(db.sql.contains("RECORD_VERSION=RECORD_VERSION+1"),"unallocation version advance");
        pass(db.args.length==8,"unallocation exact binds");
        db.writeCount=0;
        try{repo.consumeReviewedStage(db,permit("ALLOCATE","101","101"),"501","41");
            throw new AssertionError("second consumption was not rejected");}
        catch(ResponseStatusException expected){pass(true,"reject second consumption");}
        db.writeCount=1;
        var cross=permit("ALLOCATE","101","202");
        db.reads=List.of("17");
        pass(repo.matchesReviewedStage(db,cross),"cross-unit approved request matched");
        pass(db.sql.contains("FOR UPDATE OF g.GRANT_STATUS,i.INTENT_STATUS"),"grant and stage locked");
        pass(db.sql.contains("g.VALID_UNTIL_UTC>SYS_EXTRACT_UTC"),"grant unexpired");
        pass(db.sql.contains("t.USE_IN_OTHER_BRANCHES='Y'"),"type grant mandatory");
        pass(db.sql.contains("g.SOURCE_RECORD_VERSION=c.RECORD_VERSION"),"grant source version");
        pass(db.sql.contains("g.CONSUMER_BRANCH=i.CONSUMER_BRANCH"),"individual grant mandatory");
        pass(db.args.length==13,"cross-unit binding count");
        db.reads=List.of();
        pass(!repo.matchesReviewedStage(db,cross),"missing grant fails closed");
        db.reads=List.of("19");
        pass(!repo.matchesReviewedStage(db,cross),"wrong stage fails closed");
        System.out.println("CLT_LIFECYCLE_POSTING_JDBC_SMOKE_PASS="+checks+"/"+checks+"; DB_CONNECTION=NONE");
    }
}
