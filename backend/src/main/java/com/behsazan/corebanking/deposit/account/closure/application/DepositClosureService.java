package com.behsazan.corebanking.deposit.account.closure.application;

import com.behsazan.corebanking.deposit.account.balance.application.DepositBalanceService;
import com.behsazan.corebanking.deposit.account.closure.domain.DepositClosureModels.*;
import com.behsazan.corebanking.deposit.account.closure.oracle.DepositClosureRepository;
import com.behsazan.corebanking.deposit.account.closure.oracle.DepositClosureRepository.*;
import com.behsazan.corebanking.deposit.account.error.DepositAccountLifecycleException;
import com.behsazan.corebanking.deposit.account.error.DepositAccountNotFoundException;
import com.behsazan.corebanking.deposit.account.transaction.application.DepositTransactionService;
import com.behsazan.corebanking.deposit.account.transaction.domain.DepositTransactionModels.DerivedTransactionLeg;
import com.behsazan.corebanking.deposit.account.transaction.domain.DepositTransactionModels.DerivedTransactionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;

@Service
public class DepositClosureService {
    private static final Map<String, Set<String>> CLOSURE_REASONS = Map.of(
            "CUSTOMER_REQUEST", Set.of("CUSTOMER_REQUEST","NO_LONGER_NEEDED","SERVICE_CONSOLIDATION","PRODUCT_REPLACEMENT"),
            "MATURITY", Set.of("MATURITY_INSTRUCTION","NON_RENEWAL"),
            "EARLY_TERMINATION", Set.of("TERM_EARLY_TERMINATION"),
            "FORCED", Set.of("LEGAL_ENFORCEMENT","REGULATORY_ENFORCEMENT","FRAUD_CONFIRMED","DATA_CORRECTION"),
            "INACTIVE", Set.of("PROLONGED_INACTIVITY","BANK_POLICY")
    );
    private final DepositClosureRepository repository;
    private final DepositBalanceService balanceService;
    private final DepositTransactionService transactionService;
    public DepositClosureService(DepositClosureRepository repository, DepositBalanceService balanceService, DepositTransactionService transactionService){this.repository=repository;this.balanceService=balanceService;this.transactionService=transactionService;}

    public ClosureWorkflowView get(long accountId){requireAccount(accountId,false);return new ClosureWorkflowView(repository.closures(accountId),repository.reopenings(accountId));}

    @Transactional
    public ClosureActionResponse requestClosure(long accountId, ClosureRequest request, String actor, String correlationId, String idempotencyKey){
        String type=upper(required(request==null?null:request.closureTypeCode(),"CLOSURE_TYPE_CODE"));
        String reason=upper(required(request.reasonCode(),"REASON_CODE"));
        if(!CLOSURE_REASONS.containsKey(type)||!CLOSURE_REASONS.get(type).contains(reason))throw new IllegalArgumentException("ترکیب CLOSURE_TYPE_CODE / REASON_CODE نامعتبر است.");
        String approver=required(request.approverUserId(),"APPROVER_USER_ID");
        String org=required(request.orgUnitCode(),"ORG_UNIT_CODE");
        if(actor.equals(approver))throw new IllegalArgumentException("درخواست‌کننده و تأییدکننده Closure باید متفاوت باشند.");
        String settlement=trimToNull(request.settlementAccountReference());
        String hash=hash("REQUEST|"+accountId+"|"+type+"|"+reason+"|"+Objects.toString(request.effectiveDate(),"")+"|"+Objects.toString(settlement,"")+"|"+approver+"|"+org);
        if(replayOrClaim(idempotencyKey,accountId,"ACCOUNT_CLOSURE_REQUEST",hash,actor,correlationId))return new ClosureActionResponse(closureFromRef(accountId,resultRef(idempotencyKey)),true);

        AccountLock account=requireAccount(accountId,true);
        if(!"ACTIVE".equals(upper(account.status())))throw lifecycle("درخواست بستن فقط برای حساب ACTIVE مجاز است.","DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE",account.status());
        if(repository.openClosureCount(accountId)>0)throw lifecycle("برای حساب یک Closure باز وجود دارد.","DEPOSIT_ACCOUNT_CLOSURE","REQUESTED/APPROVED");
        BalanceRow balance=repository.lockBalance(accountId).orElseThrow(()->lifecycle("Balance عملیاتی حساب موجود نیست.","DEPOSIT_ACCOUNT_BALANCE","missing"));
        long holds=repository.activeHoldCount(accountId), reservations=repository.activeReservationCount(accountId);
        BigDecimal ledger=nz(balance.ledger());
        boolean nonNegative=ledger.signum()>=0;
        boolean settlementReady=ledger.signum()==0 || settlement!=null;

        long closureId=repository.nextClosureId(); long approvalId=repository.nextApprovalId();
        repository.insertApproval(approvalId,accountId,"ACCOUNT_CLOSURE","DEPOSIT_ACCOUNT_CLOSURE",closureId,actor,approver,org,approvalKey(idempotencyKey),correlationId);
        repository.insertClosure(closureId,accountId,type,request.effectiveDate()==null?LocalDate.now():request.effectiveDate(),settlement,reason,approvalId,actor);
        repository.insertCheck(closureId,"ACCOUNT_ACTIVE",true,"حساب در وضعیت ACTIVE است.","DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE=ACTIVE",actor);
        repository.insertCheck(closureId,"NO_ACTIVE_HOLD",holds==0,holds==0?"Hold فعال وجود ندارد.":"تعداد Hold فعال: "+holds,"DEPOSIT_ACCOUNT_HOLD",actor);
        repository.insertCheck(closureId,"NO_ACTIVE_RESERVATION",reservations==0,reservations==0?"Reservation فعال وجود ندارد.":"تعداد Reservation فعال: "+reservations,"DEPOSIT_BALANCE_RESERVATION",actor);
        repository.insertCheck(closureId,"NON_NEGATIVE_LEDGER",nonNegative,"Ledger Balance="+ledger.toPlainString(),"DEPOSIT_ACCOUNT_BALANCE.LEDGER_BALANCE",actor);
        repository.insertCheck(closureId,"SETTLEMENT_READY",settlementReady,settlementReady?"مسیر تسویه آماده است.":"برای مانده غیرصفر Settlement Account Reference الزامی است.","DEPOSIT_ACCOUNT_CLOSURE.SETTLEMENT_ACCOUNT_REFERENCE",actor);
        if(ledger.signum()>0)repository.insertSettlement(closureId,ledger,actor);
        repository.completeIdempotency(idempotencyKey,"CLOSURE:"+closureId);
        return new ClosureActionResponse(closure(accountId,closureId),false);
    }

    @Transactional
    public ClosureActionResponse approveClosure(long accountId,long closureId,String actor,String correlationId,String idempotencyKey){
        String hash=hash("APPROVE|"+accountId+"|"+closureId+"|"+actor);
        if(replayOrClaim(idempotencyKey,accountId,"ACCOUNT_CLOSURE_APPROVE",hash,actor,correlationId))return new ClosureActionResponse(closure(accountId,closureId),true);
        ClosureLock c=repository.lockClosure(accountId,closureId).orElseThrow(()->new IllegalArgumentException("Closure یافت نشد."));
        if(!"REQUESTED".equals(upper(c.status())))throw lifecycle("فقط Closure در وضعیت REQUESTED قابل تأیید است.","DEPOSIT_ACCOUNT_CLOSURE.CLOSURE_STATUS_CODE",c.status());
        if(repository.failedClosureChecks(closureId)>0)throw lifecycle("Closure دارای Check ناموفق است و قابل تأیید نیست.","DEPOSIT_ACCOUNT_CLOSURE_CHECK.RESULT_STATUS_CODE","FAIL");
        if(repository.approve(c.approvalId(),actor)!=1)throw lifecycle("تأیید Closure فقط توسط Approver تعیین‌شده و در وضعیت Pending ممکن است.","DEPOSIT_OPERATION_APPROVAL_REQUEST.APPROVER_USER_ID",actor);
        if(repository.markClosureApproved(closureId,actor)!=1)throw lifecycle("Closure همزمان تغییر کرده است.","DEPOSIT_ACCOUNT_CLOSURE.RECORD_VERSION",String.valueOf(c.recordVersion()));
        repository.completeIdempotency(idempotencyKey,"CLOSURE:"+closureId);
        return new ClosureActionResponse(closure(accountId,closureId),false);
    }

    @Transactional
    public ClosureActionResponse executeClosure(long accountId,long closureId,String actor,String correlationId,String idempotencyKey){
        String hash=hash("EXECUTE|"+accountId+"|"+closureId);
        if(replayOrClaim(idempotencyKey,accountId,"ACCOUNT_CLOSURE_EXECUTE",hash,actor,correlationId))return new ClosureActionResponse(closure(accountId,closureId),true);
        AccountLock account=requireAccount(accountId,true);
        ClosureLock c=repository.lockClosure(accountId,closureId).orElseThrow(()->new IllegalArgumentException("Closure یافت نشد."));
        if(!"APPROVED".equals(upper(c.status())))throw lifecycle("Closure قبل از اجرا باید APPROVED باشد.","DEPOSIT_ACCOUNT_CLOSURE.CLOSURE_STATUS_CODE",c.status());
        if(!"ACTIVE".equals(upper(account.status())))throw lifecycle("اجرای Closure فقط روی حساب ACTIVE مجاز است.","DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE",account.status());
        if(repository.activeHoldCount(accountId)>0)throw lifecycle("Hold فعال مانع بستن حساب است.","DEPOSIT_ACCOUNT_HOLD.HOLD_STATUS_CODE","ACTIVE");
        if(repository.activeReservationCount(accountId)>0)throw lifecycle("Reservation فعال مانع بستن حساب است.","DEPOSIT_BALANCE_RESERVATION.RESERVATION_STATUS_CODE","ACTIVE");
        balanceService.refreshBalance(accountId,actor);
        BalanceRow before=repository.lockBalance(accountId).orElseThrow(()->lifecycle("Balance عملیاتی حساب موجود نیست.","DEPOSIT_ACCOUNT_BALANCE","missing"));
        BigDecimal ledger=nz(before.ledger()); String postingRef=null;
        if(ledger.signum()<0)throw lifecycle("حساب با Ledger منفی قابل بستن نیست.","DEPOSIT_ACCOUNT_BALANCE.LEDGER_BALANCE",ledger.toPlainString());
        if(ledger.signum()>0){
            String settlementRef=trimToNull(c.settlementRef());
            if(settlementRef==null)throw lifecycle("Settlement Account Reference برای تسویه مانده الزامی است.","DEPOSIT_ACCOUNT_CLOSURE.SETTLEMENT_ACCOUNT_REFERENCE","missing");
            if(nz(before.available()).compareTo(ledger)<0)throw lifecycle("کل مانده برای تسویه Closure در دسترس نیست.","DEPOSIT_ACCOUNT_BALANCE.AVAILABLE_BALANCE",before.available().toPlainString());
            var tx=transactionService.postDerived(
                    new DerivedTransactionRequest(accountId,"ACCOUNT_CLOSURE_SETTLEMENT",ledger,account.currency(),LocalDate.now(),LocalDate.now(),"DEBIT","CLOSURE:"+closureId,"CLOSURE_APPROVAL",c.requestedBy(),c.approver(),c.reason(),List.of(
                            new DerivedTransactionLeg(accountId,null,"DEBIT",ledger),
                            new DerivedTransactionLeg(null,settlementRef,"CREDIT",ledger))),
                    actor,correlationId,childKey(idempotencyKey,"CLOSURE-TX"));
            long transactionId=tx.transaction().transaction().transactionId();
            postingRef="TX-"+transactionId;
            repository.settleItems(closureId,transactionId,actor);
        }
        BalanceRow after=repository.lockBalance(accountId).orElseThrow();
        if(nz(after.ledger()).signum()!=0||nz(after.available()).signum()!=0||nz(after.blocked()).signum()!=0||nz(after.pendingDebit()).signum()!=0)throw lifecycle("مانده‌های حساب پس از تسویه Closure صفر نیستند.","DEPOSIT_ACCOUNT_BALANCE","ledger="+after.ledger()+", available="+after.available()+", blocked="+after.blocked()+", pendingDebit="+after.pendingDebit());
        if(repository.changeStatus(accountId,account.recordVersion(),"ACTIVE","CLOSED",actor)!=1)throw lifecycle("وضعیت حساب همزمان تغییر کرده است.","DEPOSIT_ACCOUNT.RECORD_VERSION",String.valueOf(account.recordVersion()));
        repository.insertLifecycle(accountId,account.openingRequestId(),"CLOSE","ACTIVE","CLOSED",c.reason(),c.approvalId(),actor,correlationId);
        repository.insertStatusHistory(accountId,"ACTIVE","CLOSED",c.reason(),"CLOSURE:"+closureId,c.approvalId(),actor);
        if(repository.executeClosure(closureId,postingRef,actor)!=1)throw lifecycle("Closure همزمان تغییر کرده است.","DEPOSIT_ACCOUNT_CLOSURE.CLOSURE_STATUS_CODE",c.status());
        repository.completeIdempotency(idempotencyKey,"CLOSURE:"+closureId);
        return new ClosureActionResponse(closure(accountId,closureId),false);
    }

    /**
     * Internal closure execution for an owning operation that has already settled the deposit balance
     * through Step 05. This preserves Package 16 as the only owner of ACTIVE -> CLOSED while keeping
     * the financial transaction reference from the owning flow (maturity / early termination).
     */
    @Transactional
    public ClosureActionResponse executePreSettledClosure(long accountId,long closureId,String settlementTransactionReference,String actor,String correlationId,String idempotencyKey){
        String settlementRef=required(settlementTransactionReference,"SETTLEMENT_TRANSACTION_REFERENCE");
        String hash=hash("EXECUTE_PRE_SETTLED|"+accountId+"|"+closureId+"|"+settlementRef);
        if(replayOrClaim(idempotencyKey,accountId,"ACCOUNT_CLOSURE_EXECUTE_PRE_SETTLED",hash,actor,correlationId))return new ClosureActionResponse(closure(accountId,closureId),true);
        AccountLock account=requireAccount(accountId,true);
        ClosureLock c=repository.lockClosure(accountId,closureId).orElseThrow(()->new IllegalArgumentException("Closure یافت نشد."));
        if(!"APPROVED".equals(upper(c.status())))throw lifecycle("Closure قبل از اجرای Pre-settled باید APPROVED باشد.","DEPOSIT_ACCOUNT_CLOSURE.CLOSURE_STATUS_CODE",c.status());
        if(!"ACTIVE".equals(upper(account.status())))throw lifecycle("اجرای Closure فقط روی حساب ACTIVE مجاز است.","DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE",account.status());
        if(repository.activeHoldCount(accountId)>0)throw lifecycle("Hold فعال مانع بستن حساب است.","DEPOSIT_ACCOUNT_HOLD.HOLD_STATUS_CODE","ACTIVE");
        if(repository.activeReservationCount(accountId)>0)throw lifecycle("Reservation فعال مانع بستن حساب است.","DEPOSIT_BALANCE_RESERVATION.RESERVATION_STATUS_CODE","ACTIVE");
        balanceService.refreshBalance(accountId,actor);
        BalanceRow afterSettlement=repository.lockBalance(accountId).orElseThrow(()->lifecycle("Balance عملیاتی حساب موجود نیست.","DEPOSIT_ACCOUNT_BALANCE","missing"));
        if(nz(afterSettlement.ledger()).signum()!=0||nz(afterSettlement.available()).signum()!=0||nz(afterSettlement.blocked()).signum()!=0||nz(afterSettlement.pendingDebit()).signum()!=0)throw lifecycle("Pre-settled Closure فقط بعد از صفر شدن کامل مانده قابل اجرا است.","DEPOSIT_ACCOUNT_BALANCE","ledger="+afterSettlement.ledger()+", available="+afterSettlement.available()+", blocked="+afterSettlement.blocked()+", pendingDebit="+afterSettlement.pendingDebit());
        repository.settleItems(closureId,transactionIdFromReference(settlementRef),actor);
        if(repository.changeStatus(accountId,account.recordVersion(),"ACTIVE","CLOSED",actor)!=1)throw lifecycle("وضعیت حساب همزمان تغییر کرده است.","DEPOSIT_ACCOUNT.RECORD_VERSION",String.valueOf(account.recordVersion()));
        repository.insertLifecycle(accountId,account.openingRequestId(),"CLOSE","ACTIVE","CLOSED",c.reason(),c.approvalId(),actor,correlationId);
        repository.insertStatusHistory(accountId,"ACTIVE","CLOSED",c.reason(),"CLOSURE:"+closureId,c.approvalId(),actor);
        if(repository.executeClosure(closureId,settlementRef,actor)!=1)throw lifecycle("Closure همزمان تغییر کرده است.","DEPOSIT_ACCOUNT_CLOSURE.CLOSURE_STATUS_CODE",c.status());
        repository.completeIdempotency(idempotencyKey,"CLOSURE:"+closureId);
        return new ClosureActionResponse(closure(accountId,closureId),false);
    }

    @Transactional
    public ReopeningActionResponse requestReopening(long accountId,ReopeningRequest request,String actor,String correlationId,String idempotencyKey){
        String reason=upper(required(request==null?null:request.reopenReasonCode(),"REOPEN_REASON_CODE"));
        String approver=required(request.approverUserId(),"APPROVER_USER_ID"),org=required(request.orgUnitCode(),"ORG_UNIT_CODE");
        if(actor.equals(approver))throw new IllegalArgumentException("درخواست‌کننده و تأییدکننده Reopening باید متفاوت باشند.");
        String hash=hash("REOPEN_REQUEST|"+accountId+"|"+reason+"|"+approver+"|"+org);
        if(replayOrClaim(idempotencyKey,accountId,"ACCOUNT_REOPEN_REQUEST",hash,actor,correlationId))return new ReopeningActionResponse(reopeningFromRef(accountId,resultRef(idempotencyKey)),true);
        AccountLock account=requireAccount(accountId,true);
        if(!"CLOSED".equals(upper(account.status())))throw lifecycle("Reopening فقط برای حساب CLOSED مجاز است.","DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE",account.status());
        if(repository.openReopeningCount(accountId)>0)throw lifecycle("برای حساب یک Reopening باز وجود دارد.","DEPOSIT_ACCOUNT_REOPENING","REQUESTED/APPROVED");
        long reopeningId=repository.nextReopeningId(),approvalId=repository.nextApprovalId();
        repository.insertApproval(approvalId,accountId,"ACCOUNT_REOPEN","DEPOSIT_ACCOUNT_REOPENING",reopeningId,actor,approver,org,approvalKey(idempotencyKey),correlationId);
        repository.insertReopening(reopeningId,accountId,reason,approvalId,actor);
        repository.completeIdempotency(idempotencyKey,"REOPENING:"+reopeningId);
        return new ReopeningActionResponse(reopening(accountId,reopeningId),false);
    }

    @Transactional
    public ReopeningActionResponse approveReopening(long accountId,long reopeningId,String actor,String correlationId,String idempotencyKey){
        String hash=hash("REOPEN_APPROVE|"+accountId+"|"+reopeningId+"|"+actor);
        if(replayOrClaim(idempotencyKey,accountId,"ACCOUNT_REOPEN_APPROVE",hash,actor,correlationId))return new ReopeningActionResponse(reopening(accountId,reopeningId),true);
        ReopeningLock r=repository.lockReopening(accountId,reopeningId).orElseThrow(()->new IllegalArgumentException("Reopening یافت نشد."));
        if(!"REQUESTED".equals(upper(r.status())))throw lifecycle("فقط Reopening در وضعیت REQUESTED قابل تأیید است.","DEPOSIT_ACCOUNT_REOPENING.REOPEN_STATUS_CODE",r.status());
        if(repository.approve(r.approvalId(),actor)!=1)throw lifecycle("تأیید Reopening فقط توسط Approver تعیین‌شده ممکن است.","DEPOSIT_OPERATION_APPROVAL_REQUEST.APPROVER_USER_ID",actor);
        if(repository.markReopeningApproved(reopeningId,actor)!=1)throw lifecycle("Reopening همزمان تغییر کرده است.","DEPOSIT_ACCOUNT_REOPENING.RECORD_VERSION",String.valueOf(r.recordVersion()));
        repository.completeIdempotency(idempotencyKey,"REOPENING:"+reopeningId);
        return new ReopeningActionResponse(reopening(accountId,reopeningId),false);
    }

    @Transactional
    public ReopeningActionResponse executeReopening(long accountId,long reopeningId,String actor,String correlationId,String idempotencyKey){
        String hash=hash("REOPEN_EXECUTE|"+accountId+"|"+reopeningId);
        if(replayOrClaim(idempotencyKey,accountId,"ACCOUNT_REOPEN_EXECUTE",hash,actor,correlationId))return new ReopeningActionResponse(reopening(accountId,reopeningId),true);
        AccountLock account=requireAccount(accountId,true);
        ReopeningLock r=repository.lockReopening(accountId,reopeningId).orElseThrow(()->new IllegalArgumentException("Reopening یافت نشد."));
        if(!"APPROVED".equals(upper(r.status())))throw lifecycle("Reopening قبل از اجرا باید APPROVED باشد.","DEPOSIT_ACCOUNT_REOPENING.REOPEN_STATUS_CODE",r.status());
        if(!"CLOSED".equals(upper(account.status())))throw lifecycle("اجرای Reopening فقط روی حساب CLOSED مجاز است.","DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE",account.status());
        if(repository.changeStatus(accountId,account.recordVersion(),"CLOSED","ACTIVE",actor)!=1)throw lifecycle("وضعیت حساب همزمان تغییر کرده است.","DEPOSIT_ACCOUNT.RECORD_VERSION",String.valueOf(account.recordVersion()));
        repository.insertLifecycle(accountId,account.openingRequestId(),"REOPEN","CLOSED","ACTIVE",r.reason(),r.approvalId(),actor,correlationId);
        repository.insertStatusHistory(accountId,"CLOSED","ACTIVE",r.reason(),"REOPENING:"+reopeningId,r.approvalId(),actor);
        if(repository.executeReopening(reopeningId,actor)!=1)throw lifecycle("Reopening همزمان تغییر کرده است.","DEPOSIT_ACCOUNT_REOPENING.REOPEN_STATUS_CODE",r.status());
        repository.completeIdempotency(idempotencyKey,"REOPENING:"+reopeningId);
        return new ReopeningActionResponse(reopening(accountId,reopeningId),false);
    }

    private AccountLock requireAccount(long id,boolean lock){if(id<=0)throw new IllegalArgumentException("accountId باید مثبت باشد.");return repository.findAccount(id,lock).orElseThrow(()->new DepositAccountNotFoundException("حساب سپرده با شناسه "+id+" یافت نشد."));}
    private ClosureCase closure(long accountId,long closureId){return repository.closures(accountId).stream().filter(x->x.accountClosureId()==closureId).findFirst().orElseThrow();}
    private ReopeningCase reopening(long accountId,long reopeningId){return repository.reopenings(accountId).stream().filter(x->x.accountReopeningId()==reopeningId).findFirst().orElseThrow();}
    private ClosureCase closureFromRef(long accountId,String ref){return closure(accountId,parseRef(ref,"CLOSURE:"));}
    private ReopeningCase reopeningFromRef(long accountId,String ref){return reopening(accountId,parseRef(ref,"REOPENING:"));}
    private String resultRef(String key){return repository.findIdempotency(key).orElseThrow().resultReference();}
    private boolean replayOrClaim(String key,long accountId,String operation,String payloadHash,String actor,String correlation){String k=required(key,"X-Idempotency-Key");if(k.length()>128)throw new IllegalArgumentException("X-Idempotency-Key حداکثر 128 کاراکتر است.");var x=repository.findIdempotency(k);if(x.isPresent()){var v=x.get();if(!Objects.equals(v.accountId(),accountId)||!operation.equals(v.operationType())||!payloadHash.equals(v.payloadHash()))throw new IllegalArgumentException("Idempotency-Key قبلاً برای درخواست دیگری استفاده شده است.");if("COMPLETED".equals(v.processingStatus()))return true;throw new IllegalArgumentException("درخواست با این Idempotency-Key در حال پردازش است.");}repository.insertIdempotency(k,accountId,operation,payloadHash,actor,correlation);return false;}
    private static String approvalKey(String key){return key.length()<=120?key+"-APR":hash(key+"|APR");}
    private static String childKey(String key,String suffix){String candidate=key+"-"+suffix;return candidate.length()<=80?candidate:hash(key+"|"+suffix);}
    private static Long transactionIdFromReference(String ref){String x=trimToNull(ref);if(x==null||!x.startsWith("TX-"))return null;try{return Long.parseLong(x.substring(3));}catch(NumberFormatException e){return null;}}
    private static long parseRef(String ref,String prefix){if(ref==null||!ref.startsWith(prefix))throw new IllegalStateException("Idempotency result reference نامعتبر است.");return Long.parseLong(ref.substring(prefix.length()));}
    private static BigDecimal nz(BigDecimal v){return v==null?BigDecimal.ZERO:v;}
    private static String upper(String v){String x=trimToNull(v);return x==null?null:x.toUpperCase(Locale.ROOT);}
    private static String required(String v,String field){String x=trimToNull(v);if(x==null)throw new IllegalArgumentException(field+" الزامی است.");return x;}
    private static String trimToNull(String v){return v==null||v.isBlank()?null:v.trim();}
    private static DepositAccountLifecycleException lifecycle(String message,String field,String detail){return new DepositAccountLifecycleException(message,Map.of(field,detail==null?"—":detail));}
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
