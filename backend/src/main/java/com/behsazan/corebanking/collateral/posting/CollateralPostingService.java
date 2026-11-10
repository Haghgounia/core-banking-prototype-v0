package com.behsazan.corebanking.collateral.posting;

import com.behsazan.corebanking.collateral.allocation.CollateralAllocationConsumerBinding;
import com.behsazan.corebanking.collateral.posting.CollateralPostingModels.*;
import com.behsazan.corebanking.collateral.posting.CollateralPostingRepository.*;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import com.behsazan.corebanking.collateral.unallocation.CollateralUnallocationDomainBinding;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * BUC-09/BUC-10 bank-permit posting. ONLY the trusted consumer service can call commit.
 * No endpoint issues/approves a permit. ALL financial SQL is executed using the app's
 * shared DataSource inside ONE transaction after parent-row FOR UPDATE. Feature flags
 * default off; local browser synthetic JWT is NEVER an operational posting identity.
 */
@Service
public final class CollateralPostingService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final Environment env;
    private final CollateralPostingRepository repository;
    private final CollateralAllocationConsumerBinding clients;
    private final CollateralUnallocationDomainBinding modules;
    public CollateralPostingService(DataSource dataSource,Environment env,
            CollateralPostingRepository repository,CollateralAllocationConsumerBinding clients,
            CollateralUnallocationDomainBinding modules){
        this.jdbc=new JdbcTemplate(dataSource);
        this.tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        this.tx.setTimeout(25);
        this.env=env;this.repository=repository;this.clients=clients;this.modules=modules;
    }
    private record Actor(JwtAuthenticationToken jwt,String subject,String branch) {}
    private static ResponseStatusException stop(HttpStatus s,String code){return new ResponseStatusException(s,code);}
    private static String number(BigDecimal n){return n==null?null:n.toPlainString();}
    private static boolean nonempty(String s){return s!=null && !s.isBlank();}
    private static String statusCode(String s){return CollateralPostingPolicy.referenceCode(s);}
    private boolean localPrototype(){return env.getProperty("core-banking.collateral.prototype.enabled",Boolean.class,false)
            || env.getProperty("CLT_LOCAL_UAT_READONLY_ENABLED",Boolean.class,false);}
    private boolean enabled(String op){return env.getProperty("core-banking.collateral.posting."
            +("ALLOCATE".equals(op)?"allocation-enabled":"unallocation-enabled"),Boolean.class,false);}
    private String config(String field){return env.getProperty("core-banking.collateral.posting."+field,"").strip();}
    private Actor actor(Authentication auth,boolean write,String op){
        if(!(auth instanceof JwtAuthenticationToken jwt)||!jwt.isAuthenticated())
            throw stop(HttpStatus.UNAUTHORIZED,"POSTING_SIGNED_PRINCIPAL_REQUIRED");
        String needed=write?("ALLOCATE".equals(op)?"SCOPE_clt.allocation.post":"SCOPE_clt.unallocation.post"):
                "SCOPE_clt.collateral.read";
        if(auth.getAuthorities().stream().noneMatch(a->needed.equals(a.getAuthority())))
            throw stop(HttpStatus.FORBIDDEN,"POSTING_SCOPE_REQUIRED");
        String subject=jwt.getToken().getSubject();
        if(subject==null || !subject.matches("[A-Za-z0-9_.@:-]{1,100}"))
            throw stop(HttpStatus.FORBIDDEN,"POSTING_SUBJECT_UNVERIFIED");
        if(write && (jwt.getToken().getClaims().containsKey("clt_local_prototype")
                || "CLT_UNIFIED_LOCAL_DEV".equals(subject)))
            throw stop(HttpStatus.FORBIDDEN,"POSTING_LOCAL_BRIDGE_CANNOT_POST");
        Object branchClaim=jwt.getToken().getClaims().get("branch_code");
        if(!(branchClaim instanceof String branch))throw stop(HttpStatus.FORBIDDEN,"POSTING_BRANCH_MISSING");
        try{return new Actor(jwt,subject,CollateralRegistryPolicy.branch(branch));}
        catch(IllegalArgumentException ex){throw stop(HttpStatus.FORBIDDEN,"POSTING_BRANCH_INVALID");}
    }
    private boolean mappingSyntax(){
        try{
            CollateralPostingPolicy.sequence(config("allocation-id-sequence"));
            CollateralPostingPolicy.sequence(config("transaction-id-sequence"));
            statusCode(config("allocation-transaction-type"));
            statusCode(config("unallocation-transaction-type"));
            statusCode(config("posted-transaction-status"));
            statusCode(config("source-system-code"));
            statusCode(config("allocation-direction"));
            statusCode(config("unallocation-direction"));
            String statuses=config("allocatable-collateral-statuses");
            if(statuses.isEmpty())return false;
            for(String code:statuses.split(",",-1))statusCode(code);
            return !config("allocation-transaction-type").equals(config("unallocation-transaction-type"))
                    && !config("allocation-direction").equals(config("unallocation-direction"));
        }catch(IllegalArgumentException e){return false;}
    }
    private boolean sequencePresent(String name){
        Long c=jdbc.queryForObject("""
                SELECT COUNT(*) FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER='CLT' AND SEQUENCE_NAME=?
                """,Long.class,name);
        return Long.valueOf(1).equals(c);
    }
    private boolean referencePresent(String table,String column,String code){
        // Table/column are *internal constants*, not request data or dynamic configuration.
        String sql="SELECT COUNT(*) FROM CLT."+table+" WHERE "+column+"=?";
        Long c=jdbc.queryForObject(sql,Long.class,code);
        return Long.valueOf(1).equals(c);
    }
    private boolean mappingsExist(){
        try{
            return mappingSyntax()
                    && sequencePresent(config("allocation-id-sequence"))
                    && sequencePresent(config("transaction-id-sequence"))
                    && referencePresent("REF_COLL_TXN_TYPE","TRANSACTION_TYPE_CODE",config("allocation-transaction-type"))
                    && referencePresent("REF_COLL_TXN_TYPE","TRANSACTION_TYPE_CODE",config("unallocation-transaction-type"))
                    && referencePresent("REF_COLL_TXN_STATUS","TRANSACTION_STATUS_CODE",config("posted-transaction-status"))
                    && referencePresent("REF_SOURCE_SYSTEM","SOURCE_SYSTEM_CODE",config("source-system-code"));
        }catch(DataAccessException e){return false;}
    }
    private static final String[] PERMIT_COLUMNS={"PERMIT_ID","OPERATION_CODE","COLLATERAL_ID",
            "BRANCH_CODE","CONSUMER_BRANCH_CODE","CLIENT_ID","CONSUMER_DOMAIN","MODULE_ID",
            "FILE_NUMBER","CASE_REFERENCE","AMOUNT","CURRENCY_CODE","EXPECTED_COLLATERAL_VERSION",
            "EXPECTED_ALLOCATION_VERSION","ALLOCATION_ID","APPROVED_CAPACITY_LIMIT",
            "VALUATION_HISTORY_ID","ORIGINAL_TRANSACTION_ID","SOURCE_REQUEST_ID","SOURCE_DIGEST",
            "OBLIGATION_REFERENCE","COVERAGE_POLICY_REFERENCE","DEBTOR_ROSTER_DIGEST",
            "TRANSACTION_NUMBER","RELEASE_MODE","MAKER_SUBJECT","CHECKER_SUBJECT",
            "ISSUER_SUBJECT","OBLIGATION_ATTESTED","DEBTORS_ATTESTED","POLICY_ATTESTED",
            "PERMIT_STATUS","VALID_UNTIL","IDEMPOTENCY_KEY","POSTER_SUBJECT",
            "POSTED_AT","POSTED_TRANSACTION_ID","POSTED_ALLOCATION_ID",
            "POSTED_PARENT_VERSION","POSTED_REMAINING_AMOUNT"};
    private boolean permitSchemaReady(){
        try{
            Integer cols=jdbc.queryForObject("""
                    SELECT COUNT(DISTINCT COLUMN_NAME) FROM ALL_TAB_COLUMNS
                     WHERE OWNER='CLT' AND TABLE_NAME='CLT_COVERAGE_POST_PERMIT'
                       AND COLUMN_NAME IN ('PERMIT_ID','OPERATION_CODE','COLLATERAL_ID',
                        'BRANCH_CODE','CONSUMER_BRANCH_CODE','CLIENT_ID','CONSUMER_DOMAIN','MODULE_ID',
                        'FILE_NUMBER','CASE_REFERENCE','AMOUNT','CURRENCY_CODE','EXPECTED_COLLATERAL_VERSION',
                        'EXPECTED_ALLOCATION_VERSION','ALLOCATION_ID','APPROVED_CAPACITY_LIMIT',
                        'VALUATION_HISTORY_ID','ORIGINAL_TRANSACTION_ID','SOURCE_REQUEST_ID','SOURCE_DIGEST',
                        'OBLIGATION_REFERENCE','COVERAGE_POLICY_REFERENCE','DEBTOR_ROSTER_DIGEST',
                        'TRANSACTION_NUMBER','RELEASE_MODE','MAKER_SUBJECT','CHECKER_SUBJECT',
                        'ISSUER_SUBJECT','OBLIGATION_ATTESTED','DEBTORS_ATTESTED','POLICY_ATTESTED',
                        'PERMIT_STATUS','VALID_UNTIL','IDEMPOTENCY_KEY','POSTER_SUBJECT',
                        'POSTED_AT','POSTED_TRANSACTION_ID','POSTED_ALLOCATION_ID',
                        'POSTED_PARENT_VERSION','POSTED_REMAINING_AMOUNT')
                    """,Integer.class);
            Integer constraints=jdbc.queryForObject("""
                    SELECT COUNT(DISTINCT CONSTRAINT_NAME) FROM ALL_CONSTRAINTS
                     WHERE OWNER='CLT' AND TABLE_NAME='CLT_COVERAGE_POST_PERMIT'
                       AND STATUS='ENABLED' AND VALIDATED='VALIDATED'
                       AND CONSTRAINT_NAME IN ('PK_CLT_POST_PERMIT','FK_CLT_POST_COLLATERAL',
                         'UQ_CLT_POST_TXN_NO','UQ_CLT_POST_IDEMPOTENCY','CK_CLT_POST_OPERATION',
                         'CK_CLT_POST_STATUS','CK_CLT_POST_APPROVAL','CK_CLT_POST_CONSUMED',
                         'CK_CLT_POST_RELEASE_MODE','UQ_CLT_POST_SOURCE')
                    """,Integer.class);
            String identity=jdbc.queryForObject("""
                    SELECT LISTAGG(COLUMN_NAME,',') WITHIN GROUP (ORDER BY POSITION)
                      FROM ALL_CONS_COLUMNS
                     WHERE OWNER='CLT' AND TABLE_NAME='CLT_COVERAGE_POST_PERMIT'
                       AND CONSTRAINT_NAME='UQ_CLT_POST_IDEMPOTENCY'
                    """,String.class);
            return cols!=null && cols==PERMIT_COLUMNS.length && Integer.valueOf(10).equals(constraints)
                    && "CLIENT_ID,OPERATION_CODE,IDEMPOTENCY_KEY".equals(identity);
        }catch(DataAccessException e){return false;}
    }
    private boolean requiredColumns(String table,String... names){
        String sql="SELECT COUNT(DISTINCT COLUMN_NAME) FROM ALL_TAB_COLUMNS WHERE OWNER='CLT' AND TABLE_NAME=? AND COLUMN_NAME IN ("
                +String.join(",",java.util.Collections.nCopies(names.length,"?"))+")";
        List<Object> args=new ArrayList<>();args.add(table);args.addAll(List.of(names));
        Integer c=jdbc.queryForObject(sql,Integer.class,args.toArray());
        return c!=null && c==names.length;
    }
    private boolean unknownRequired(String table,String... written){
        String sql="""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS
                 WHERE OWNER='CLT' AND TABLE_NAME=? AND NULLABLE='N'
                   AND NVL(DEFAULT_LENGTH,0)=0 AND IDENTITY_COLUMN='NO' AND VIRTUAL_COLUMN='NO'
                   AND COLUMN_NAME NOT IN (
                """+String.join(",",java.util.Collections.nCopies(written.length,"?"))+")";
        List<Object> args=new ArrayList<>();args.add(table);args.addAll(List.of(written));
        Integer count=jdbc.queryForObject(sql,Integer.class,args.toArray());
        return count==null || count!=0;
    }
    /** Gate on the approved POSTED status and the physical stage receipt columns.
     *  These are bank migrations, never installed by the source patch. */
    private boolean stagePostingReady(String table,String statusConstraint){
        try {
            if(!requiredColumns(table,"POSTED_TRANSACTION_ID","POSTED_ALLOCATION_ID",
                    table.equals("CLT_CROSS_UNIT_INTENT")?"POSTED_AT_UTC":"POSTED_AT"))return false;
            Integer c=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ALL_CONSTRAINTS
                     WHERE OWNER='CLT' AND TABLE_NAME=? AND CONSTRAINT_NAME=?
                       AND STATUS='ENABLED' AND VALIDATED='VALIDATED'
                       AND SEARCH_CONDITION_VC LIKE '%''POSTED''%'
                    """,Integer.class,table,statusConstraint);
            return Integer.valueOf(1).equals(c);
        }catch(DataAccessException ex){return false;}
    }
    private boolean stageSchemasReady(){
        return stagePostingReady("CLT_ALLOCATION_REQUEST","CK_CLT_ALLOC_REQ_STATUS")
                && stagePostingReady("CLT_UNALLOC_REQ","CK_CLT_UA_REQ_STATUS")
                && requiredColumns("CLT_ALLOCATION_REQUEST","REQUEST_ID","REQUEST_STATUS",
                    "RECORD_VERSION","CLIENT_ID","PAYLOAD_DIGEST")
                && requiredColumns("CLT_UNALLOC_REQ","REQUEST_ID","REQUEST_STATUS",
                    "RECORD_VERSION","CLIENT_ID","PAYLOAD_DIGEST");
    }
    private boolean crossUnitPostingReady(){
        return env.getProperty("core-banking.collateral.posting.cross-unit-allocation-enabled",
                Boolean.class,false)
                && stagePostingReady("CLT_CROSS_UNIT_INTENT","CK_CLT_CU_INTENT_STATUS")
                && requiredColumns("CLT_CROSS_UNIT_GRANT","GRANT_ID","COLLATERAL_ID",
                    "OWNER_BRANCH","CONSUMER_BRANCH","GRANT_STATUS",
                    "SOURCE_RECORD_VERSION","VALID_UNTIL_UTC")
                && requiredColumns("COLLATERAL_TYPE","USE_IN_OTHER_BRANCHES");
    }
    private boolean coreSchemaReady(){
        try{
            return requiredColumns("COLLATERAL","COLLATERAL_ID","BRANCH_CODE","RECORD_VERSION",
                        "UPDATED_AT","UPDATED_BY","CURRENCY_CODE","COLLATERAL_STATUS_CODE","VALID_FROM",
                        "VALID_TO","CURRENT_VALUATION_HISTORY_ID")
                    && requiredColumns("COLLATERAL_ALLOCATION","COLLATERAL_ALLOCATION_ID","COLLATERAL_ID",
                        "BRANCH_CODE","MODULE_ID","FILE_NUMBER","ALLOCATE_AMOUNT","CURRENCY_CODE",
                        "USED","RECORD_VERSION","CREATED_AT","CREATED_BY","UPDATED_AT","UPDATED_BY")
                    && requiredColumns("COLLATERAL_TRANSACTION","COLLATERAL_TRANSACTION_ID",
                        "COLLATERAL_ID","COLLATERAL_ALLOCATION_ID","COLLATERAL_TRANSACTION_NUMBER",
                        "TRANSACTION_TYPE_CODE","TRANSACTION_STATUS_CODE","TRANSACTION_DIRECTION_CODE",
                        "TRANSACTION_AMOUNT","CURRENCY_CODE","EFFECTIVE_AT","ORIGINAL_TRANSACTION_ID",
                        "SOURCE_SYSTEM_CODE","IDEMPOTENCY_KEY","CREATED_BY","OWNER_BRANCH_CODE",
                        "EXECUTING_BRANCH_CODE","REQUEST_ID","TRANSACTION_AT","CREATED_AT","RECORD_VERSION")
                    && requiredColumns("COLLATERAL_TXN_STATUS_HISTORY","COLLATERAL_TXN_STATUS_HIST_ID",
                        "COLLATERAL_TRANSACTION_ID","STATUS_SEQUENCE_NO","FROM_STATUS_CODE","TO_STATUS_CODE",
                        "CHANGED_AT","CHANGED_BY","CREATED_AT","CREATED_BY","RECORD_VERSION")
                    && !unknownRequired("COLLATERAL_ALLOCATION","COLLATERAL_ALLOCATION_ID","COLLATERAL_ID",
                        "BRANCH_CODE","MODULE_ID","FILE_NUMBER","ALLOCATE_AMOUNT","CURRENCY_CODE",
                        "USED","RECORD_VERSION","CREATED_AT","CREATED_BY","UPDATED_AT","UPDATED_BY")
                    && !unknownRequired("COLLATERAL_TRANSACTION","COLLATERAL_TRANSACTION_ID",
                        "COLLATERAL_ID","COLLATERAL_ALLOCATION_ID","COLLATERAL_TRANSACTION_NUMBER",
                        "TRANSACTION_TYPE_CODE","TRANSACTION_STATUS_CODE","TRANSACTION_DIRECTION_CODE",
                        "TRANSACTION_AMOUNT","CURRENCY_CODE","EFFECTIVE_AT","ORIGINAL_TRANSACTION_ID",
                        "SOURCE_SYSTEM_CODE","IDEMPOTENCY_KEY","CREATED_BY","OWNER_BRANCH_CODE",
                        "EXECUTING_BRANCH_CODE","REQUEST_ID","TRANSACTION_AT","CREATED_AT","RECORD_VERSION")
                    && !unknownRequired("COLLATERAL_TXN_STATUS_HISTORY","COLLATERAL_TXN_STATUS_HIST_ID",
                        "COLLATERAL_TRANSACTION_ID","STATUS_SEQUENCE_NO","FROM_STATUS_CODE","TO_STATUS_CODE",
                        "CHANGED_AT","CHANGED_BY","CREATED_AT","CREATED_BY","RECORD_VERSION")
                    && transactionHistoryKeyGeneratedByOracle();
        }catch(DataAccessException e){return false;}
    }
    private boolean transactionHistoryKeyGeneratedByOracle(){
        Integer count=jdbc.queryForObject("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS
                 WHERE OWNER='CLT' AND TABLE_NAME='COLLATERAL_TXN_STATUS_HISTORY'
                   AND COLUMN_NAME='COLLATERAL_TXN_STATUS_HIST_ID'
                   AND (IDENTITY_COLUMN='YES' OR NVL(DEFAULT_LENGTH,0)>0)
                """,Integer.class);
        return Integer.valueOf(1).equals(count);
    }
    public Readiness readiness(Authentication auth){
        actor(auth,false,"");
        boolean permit=permitSchemaReady(),core=coreSchemaReady(),maps=mappingsExist();
        boolean stages=stageSchemasReady();
        boolean local=localPrototype();
        boolean ready=(enabled("ALLOCATE")||enabled("UNALLOCATE"))&&permit&&core&&maps&&stages&&!local;
        return new Readiness(enabled("ALLOCATE"),enabled("UNALLOCATE"),permit,core,maps,local,ready,
                ready?"BANK_PERMIT_AND_SINGLE_USE_POSTED_STAGE_REQUIRED":
                    "POSTING_DISABLED_OR_BANK_SCHEMA_MAPPING_OR_STAGE_LIFECYCLE_MISSING");
    }
    private void assertReady(String operation){
        if(localPrototype())throw stop(HttpStatus.FORBIDDEN,"POSTING_LOCAL_PROTOTYPE_FINANCIAL_WRITE_DENIED");
        if(!enabled(operation))throw stop(HttpStatus.SERVICE_UNAVAILABLE,"POSTING_OPERATION_DISABLED_DEFAULT");
        if(!permitSchemaReady() || !coreSchemaReady() || !mappingsExist() || !stageSchemasReady())
            throw stop(HttpStatus.SERVICE_UNAVAILABLE,"POSTING_BANK_SCHEMA_OR_REFERENCE_MAPPING_MISSING");
    }
    private void validateActorBinding(Actor who,Permit p){
        try{
            String client=clients.authenticatedClient(who.jwt(),p.domain());
            String module=modules.approvedModule(p.domain());
            if(!client.equals(p.clientId()) || module==null || !module.equals(p.moduleId()))
                throw stop(HttpStatus.FORBIDDEN,"POSTING_BANK_CONSUMER_MODULE_NOT_BOUND");
        }catch(SecurityException ex){throw stop(HttpStatus.FORBIDDEN,"POSTING_CLIENT_GRANT_OR_MODULE_NOT_APPROVED");}
    }
    private void requirePermit(Permit p,Parent parent,Actor who,String operation,
            boolean stage,boolean original,java.time.LocalDateTime now){
        boolean matching=operation.equals(p.operation());
        if(!matching)throw stop(HttpStatus.NOT_FOUND,"POSTING_PERMIT_OPERATION_MISMATCH");
        var proof=new CollateralPostingPolicy.Attestation(p.operation(),p.status(),p.maker(),p.checker(),
                who.subject(),p.issuer(),p.expiresAt(),now,
                p.obligationAttested(),p.debtorsAttested(),p.policyAttested(),
                parent.id().equals(p.collateralId()),parent.branch().equals(p.ownerBranch())
                && who.branch().equals(p.consumerBranch()),
                p.clientId()!=null,p.moduleId()!=null,parent.version().equals(p.collateralVersion()),
                stage,original);
        List<String> problems=new ArrayList<>(CollateralPostingPolicy.blockers(proof));
        if(!nonempty(p.obligationReference()) || !nonempty(p.policyReference())
                || !nonempty(p.debtorDigest()) || p.debtorDigest().length()!=64
                || !nonempty(p.sourceDigest()) || !nonempty(p.transactionNumber())
                || !p.transactionNumber().matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,63}"))
            problems.add("POSTING_BANK_SOURCE_AND_TRANSACTION_REFERENCES_REQUIRED");
        if(!problems.isEmpty())throw stop(HttpStatus.CONFLICT,"POSTING_DENIED_"+problems.getFirst());
    }
    private static boolean activeDay(LocalDate day,LocalDate from,LocalDate to){
        return day!=null && from!=null && !day.isBefore(from) && (to==null || !day.isAfter(to));
    }
    private void requireParentPolicy(Parent parent,Permit permit,LocalDate today,String operation){
        Set<String> statusSet=Set.of(config("allocatable-collateral-statuses").split(","));
        if(!parent.statusActive() || !statusSet.contains(parent.status()) || !"ACTIVE".equals(parent.typeStatus()))
            throw stop(HttpStatus.CONFLICT,"POSTING_COLLATERAL_OR_TYPE_NOT_ACTIVE");
        if("ALLOCATE".equals(operation)){
            if(!activeDay(today,parent.validFrom(),parent.validTo()) ||
                    !activeDay(today,parent.valuationFrom(),parent.valuationTo()))
                throw stop(HttpStatus.CONFLICT,"POSTING_COLLATERAL_OR_VALUATION_EXPIRED");
            if(!"FINAL".equals(parent.valuationStatus()) || parent.valuationId()==null
                    || !parent.valuationId().equals(permit.valuationId()))
                throw stop(HttpStatus.CONFLICT,"POSTING_CURRENT_FINAL_VALUATION_NOT_MATCHED");
            if(!parent.currency().equals(permit.currency())
                    || !parent.currency().equals(parent.valuationCurrency()))
                throw stop(HttpStatus.CONFLICT,"POSTING_FX_OR_CURRENCY_MISMATCH");
        }
    }
    private Receipt receipt(Permit p,boolean replay){
        return new Receipt(p.id(),p.operation(),p.collateralId(),p.postedAllocationId(),
                p.postedTransactionId(),p.transactionNumber(),number(p.amount()),
                number(p.postedRemainingAmount()),p.currency(),p.postedParentVersion(),p.status(),
                replay,"BANK_PERMIT_ATOMIC_ORACLE_POSTING_WITH_SOURCE_STAGE_AND_IDEMPOTENCY");
    }
    public Receipt commit(String collateralId,CommitRequest request,String idempotencyKey,
            String operation,Authentication auth){
        Actor who=actor(auth,true,operation);
        String id=CollateralPostingPolicy.id(collateralId);
        if(request==null)throw stop(HttpStatus.BAD_REQUEST,"POSTING_PERMIT_ID_REQUIRED");
        String permitId=CollateralPostingPolicy.id(request.permitId());
        String key=CollateralPostingPolicy.key(idempotencyKey);
        assertReady(operation);
        Receipt response=tx.execute(txStatus->{
            // Lock parent FIRST for *both* operations; all other financial writers must cooperate.
            // Trusted caller is the consumer; the parent must be locked in its OWN branch.
            // The owner comes only from the authoritative collateral row, never the request.
            String owner=repository.ownerBranch(jdbc,id);
            Parent locked=repository.lockParent(jdbc,id,owner);
            Permit p=repository.lockPermit(jdbc,permitId,id,owner);
            if(!operation.equals(p.operation()))throw stop(HttpStatus.NOT_FOUND,"POSTING_PERMIT_OPERATION_MISMATCH");
            validateActorBinding(who,p);
            if(!who.branch().equals(p.consumerBranch()))
                throw stop(HttpStatus.FORBIDDEN,"POSTING_CONSUMER_BRANCH_NOT_ATTESTED");
            if(!owner.equals(p.ownerBranch()))
                throw stop(HttpStatus.NOT_FOUND,"POSTING_OWNER_BRANCH_MISMATCH");
            if(!owner.equals(p.consumerBranch()) && "ALLOCATE".equals(operation)
                    && !crossUnitPostingReady())
                throw stop(HttpStatus.SERVICE_UNAVAILABLE,"POSTING_CROSS_UNIT_BANK_CONTRACT_DISABLED");
            if("POSTED".equals(p.status())){
                if(CollateralPostingPolicy.sameKey(key,p.key()) && nonempty(p.postedTransactionId())
                        && nonempty(p.postedAllocationId()) && nonempty(p.postedParentVersion()))
                    return receipt(p,true);
                throw stop(HttpStatus.CONFLICT,"POSTING_PERMIT_ALREADY_CONSUMED_DIFFERENT_KEY");
            }
            if(p.key()!=null)throw stop(HttpStatus.CONFLICT,"POSTING_PERMIT_HAS_UNEXPECTED_KEY");
            boolean staging=repository.matchesReviewedStage(jdbc,p);
            boolean original="ALLOCATE".equals(operation) || repository.matchesOriginalTransaction(
                    jdbc,p,config("allocation-transaction-type"),config("posted-transaction-status"));
            requirePermit(p,locked,who,operation,staging,original,repository.oracleNow(jdbc));
            Parent parent=repository.currentFacts(jdbc,locked);
            LocalDate today=repository.bankCalendarDay(jdbc);
            requireParentPolicy(parent,p,today,operation);
            if(p.amount()==null || p.amount().signum()<=0 || p.amount().scale()>4
                    || p.amount().precision()>20)
                throw stop(HttpStatus.CONFLICT,"POSTING_AMOUNT_NOT_NUMBER20_4");
            Exposure e=repository.exposure(jdbc,id,owner,p.currency(),config("posted-transaction-status"));
            if(e.invalidUsage()!=0 || e.otherBranch()!=0 || e.mismatchedCurrencies()!=0
                    || e.pendingTransactionCount()!=0)
                throw stop(HttpStatus.CONFLICT,"POSTING_ALLOCATION_OR_TRANSACTION_INTEGRITY_BLOCKER");
            String allocationId;
            BigDecimal remaining;
            if("ALLOCATE".equals(operation)){
                if(p.allocationId()!=null || p.allocationVersion()!=null || p.originalTransactionId()!=null)
                    throw stop(HttpStatus.CONFLICT,"POSTING_ALLOCATION_PERMIT_FIELDS_INVALID");
                if(parent.exclusiveUsage() && e.activeCount()!=0)
                    throw stop(HttpStatus.CONFLICT,"POSTING_EXCLUSIVE_COLLATERAL_ALREADY_IN_USE");
                // The bank-approved limit is a signed/issued independent input, not a value invented by CLT.
                try{CollateralPostingPolicy.allocationRemaining(p.approvedLimit(),parent.eligibleValue(),
                        e.activeAmount(),p.amount(),true);}
                catch(IllegalArgumentException ex){throw stop(HttpStatus.CONFLICT,ex.getMessage());}
                allocationId=repository.nextId(jdbc,config("allocation-id-sequence"));
                repository.insertAllocation(jdbc,allocationId,p,who.subject());
                remaining=p.amount();
            }else{
                if(p.allocationId()==null || p.allocationVersion()==null || p.originalTransactionId()==null)
                    throw stop(HttpStatus.CONFLICT,"POSTING_UNALLOCATION_PERMIT_FIELDS_MISSING");
                Allocation a=repository.lockAllocation(jdbc,p.allocationId(),id,owner);
                if(!a.used() || !a.version().equals(p.allocationVersion())
                        || !a.moduleId().equals(p.moduleId()) || !a.fileNumber().equals(p.fileNumber())
                        || !a.currency().equals(p.currency()))
                    throw stop(HttpStatus.CONFLICT,"POSTING_UNALLOCATION_TARGET_CHANGED");
                try{remaining=CollateralPostingPolicy.unallocationRemaining(a.amount(),p.amount(),p.releaseMode());}
                catch(IllegalArgumentException ex){throw stop(HttpStatus.CONFLICT,ex.getMessage());}
                repository.updateAllocation(jdbc,a,remaining,who.subject());
                allocationId=a.id();
            }
            String transactionId=repository.nextId(jdbc,config("transaction-id-sequence"));
            repository.insertTransaction(jdbc,transactionId,allocationId,p,
                    config("ALLOCATE".equals(operation)?"allocation-transaction-type":"unallocation-transaction-type"),
                    config("posted-transaction-status"),
                    config("ALLOCATE".equals(operation)?"allocation-direction":"unallocation-direction"),
                    config("source-system-code"),key,who.subject());
            repository.insertInitialTransactionStatus(jdbc,transactionId,
                    config("posted-transaction-status"),who.subject());
            // Stage, core allocation, txn history, parent and permit commit or roll back TOGETHER.
            repository.consumeReviewedStage(jdbc,p,transactionId,allocationId);
            String version=repository.bumpParent(jdbc,parent,who.subject());
            repository.consumePermit(jdbc,p,key,who.subject(),transactionId,allocationId,version,remaining);
            var posted=new Permit(p.id(),p.operation(),p.collateralId(),p.ownerBranch(),
                    p.consumerBranch(),p.clientId(),p.domain(),p.moduleId(),p.fileNumber(),
                    p.caseReference(),p.amount(),p.currency(),p.collateralVersion(),p.allocationVersion(),
                    p.allocationId(),p.approvedLimit(),p.valuationId(),p.originalTransactionId(),
                    p.sourceRequestId(),p.sourceDigest(),p.obligationReference(),p.policyReference(),
                    p.debtorDigest(),p.transactionNumber(),p.releaseMode(),p.maker(),p.checker(),p.issuer(),
                    p.obligationAttested(),p.debtorsAttested(),p.policyAttested(),"POSTED",p.expiresAt(),
                    key,who.subject(),transactionId,allocationId,version,remaining);
            return receipt(posted,false);
        });
        if(response==null)throw new IllegalStateException("POSTING_TRANSACTION_EMPTY");
        return response;
    }
}
