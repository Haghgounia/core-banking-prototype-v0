package com.behsazan.corebanking.deposit.account.profit.application;

import com.behsazan.corebanking.deposit.account.balance.application.DepositBalanceService;
import com.behsazan.corebanking.deposit.account.balance.domain.DepositBalanceModels.PostEntryRequest;
import com.behsazan.corebanking.deposit.account.error.DepositAccountLifecycleException;
import com.behsazan.corebanking.deposit.account.error.DepositAccountNotFoundException;
import com.behsazan.corebanking.deposit.account.profit.domain.DepositProfitModels.*;
import com.behsazan.corebanking.deposit.account.profit.oracle.DepositProfitRepository;
import com.behsazan.corebanking.deposit.account.profit.oracle.DepositProfitRepository.ContractLock;
import com.behsazan.corebanking.deposit.account.profit.oracle.DepositProfitRepository.IdempotencyRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DepositProfitService {
    private final DepositProfitRepository repository;
    private final DepositBalanceService balanceService;
    public DepositProfitService(DepositProfitRepository repository,DepositBalanceService balanceService){this.repository=repository;this.balanceService=balanceService;}

    @Transactional(readOnly=true)
    public ProfitView get(long accountId){requireAccount(accountId,false);return state(accountId);}

    @Transactional
    public ProfitActionResponse accrue(long accountId,AccrualRequest request,String actor,String correlationId,String idempotencyKey){
        var account=requireAccount(accountId,true);
        if(!"ACTIVE".equals(upper(account.status())))throw lifecycle("Accrual فقط برای حساب ACTIVE مجاز است.","DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE",account.status());
        ContractLock c=requireContract(accountId);
        LocalDate through=request==null||request.throughDate()==null?LocalDate.now(ZoneOffset.UTC):request.throughDate();
        LocalDate from=c.lastAccrualDate()==null?c.effectiveFrom():c.lastAccrualDate();
        if(from==null)throw lifecycle("تاریخ شروع Profit Contract مشخص نیست.","DEPOSIT_PROFIT_CONTRACT.EFFECTIVE_FROM","null");
        if(c.effectiveTo()!=null&&through.isAfter(c.effectiveTo()))through=c.effectiveTo();
        if(through.isBefore(from))throw new IllegalArgumentException("THROUGH_DATE نمی‌تواند قبل از LAST_ACCRUAL_DATE باشد.");
        String payload="ACCRUE|"+accountId+"|"+through;
        String h=hash(payload);
        if(replayOrClaim(idempotencyKey,accountId,"PROFIT_ACCRUAL",h,actor,correlationId))return new ProfitActionResponse(state(accountId),parseResult(idempotencyKey,"PROFIT_ACCRUAL:"),true);
        long days=ChronoUnit.DAYS.between(from,through);
        if(days<=0){repository.completeIdempotency(idempotencyKey,"PROFIT_ACCRUAL:0");return new ProfitActionResponse(state(accountId),0,true);}
        if(!"PERCENTAGE".equals(upper(c.calculationMethodCode())))throw lifecycle("فقط روش PERCENTAGE در Phase 11G پشتیبانی می‌شود.","CALCULATION_METHOD_CODE",c.calculationMethodCode());
        int denominator=switch(upper(c.dayCountBasisCode())){case "ACT_365"->365;case "ACT_360"->360;default->throw lifecycle("Day-count basis در 11G پشتیبانی نمی‌شود.","DAY_COUNT_BASIS_CODE",c.dayCountBasisCode());};
        BigDecimal basis=repository.ledgerBalance(accountId).max(BigDecimal.ZERO);
        BigDecimal amount=basis.multiply(c.annualRate()).multiply(BigDecimal.valueOf(days)).divide(BigDecimal.valueOf(100L*denominator),8,RoundingMode.HALF_UP).setScale(4,RoundingMode.HALF_UP);
        long id=repository.next("SEQ_DEP_PROFIT_ACCRUAL");
        repository.insertAccrual(id,c,from,through,(int)days,basis,amount,actor);
        if(repository.applyAccrual(c.profitContractId(),through,amount,c.recordVersion(),actor)!=1)throw lifecycle("Profit Contract همزمان تغییر کرده است.","DEPOSIT_PROFIT_CONTRACT.RECORD_VERSION",String.valueOf(c.recordVersion()));
        repository.completeIdempotency(idempotencyKey,"PROFIT_ACCRUAL:"+id);
        return new ProfitActionResponse(state(accountId),id,false);
    }

    @Transactional
    public ProfitActionResponse postAccrued(long accountId,PostProfitRequest request,String actor,String correlationId,String idempotencyKey){
        var account=requireAccount(accountId,true);
        if(!"ACTIVE".equals(upper(account.status())))throw lifecycle("Profit Posting فقط برای حساب ACTIVE مجاز است.","DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE",account.status());
        ContractLock c=requireContract(accountId);
        LocalDate date=request==null||request.postingDate()==null?LocalDate.now(ZoneOffset.UTC):request.postingDate();
        String h=hash("POST_PROFIT|"+accountId+"|"+date);
        if(replayOrClaim(idempotencyKey,accountId,"PROFIT_POSTING",h,actor,correlationId))return new ProfitActionResponse(state(accountId),parseResult(idempotencyKey,"PROFIT_POSTING:"),true);
        BigDecimal amount=c.accruedAmount()==null?BigDecimal.ZERO:c.accruedAmount();
        if(amount.signum()<=0)throw lifecycle("سود Accrued قابل Posting وجود ندارد.","DEPOSIT_PROFIT_CONTRACT.ACCRUED_AMOUNT",amount.toPlainString());
        if(!"SAME_DEPOSIT".equals(upper(c.paymentDestinationCode())))throw lifecycle("پرداخت سود به مقصد خارجی در Phase 11H انجام می‌شود.","PAYMENT_DESTINATION_CODE",c.paymentDestinationCode());
        long postingId=repository.next("SEQ_DEP_PROFIT_POSTING");
        var posted=balanceService.post(accountId,new PostEntryRequest("CREDIT",amount,account.currencyCode(),date,date,"P11G-PROFIT-"+postingId,"DEPOSIT_PROFIT_POSTING",postingId,null,"PROFIT_ACCRUAL"),actor,correlationId,idempotencyKey+"-SL");
        repository.insertPosting(postingId,c.profitContractId(),accountId,date,amount,c.paymentDestinationCode(),c.destinationAccountReference(),posted.subledgerEntryId(),actor);
        repository.linkAccrualsToPosting(c.profitContractId(),postingId);
        if(repository.applyPosting(c.profitContractId(),date,amount,c.recordVersion(),actor)!=1)throw lifecycle("Profit Contract همزمان تغییر کرده است.","DEPOSIT_PROFIT_CONTRACT.RECORD_VERSION",String.valueOf(c.recordVersion()));
        repository.completeIdempotency(idempotencyKey,"PROFIT_POSTING:"+postingId);
        return new ProfitActionResponse(state(accountId),postingId,false);
    }

    private ProfitView state(long accountId){return new ProfitView(repository.contract(accountId).orElseThrow(()->lifecycle("Profit Contract فعال برای حساب یافت نشد.","DEPOSIT_PROFIT_CONTRACT.ACCOUNT_ID",String.valueOf(accountId))),repository.accruals(accountId),repository.postings(accountId));}
    private DepositProfitRepository.AccountRow requireAccount(long id,boolean lock){return repository.account(id,lock).orElseThrow(()->new DepositAccountNotFoundException("حساب سپرده با شناسه "+id+" یافت نشد."));}
    private ContractLock requireContract(long id){return repository.lockContract(id).orElseThrow(()->lifecycle("Profit Contract فعال برای حساب یافت نشد؛ Phase 11G migration/activation را بررسی کنید.","DEPOSIT_PROFIT_CONTRACT.ACCOUNT_ID",String.valueOf(id)));}
    private boolean replayOrClaim(String key,long accountId,String op,String h,String actor,String corr){if(key==null||key.isBlank())throw new IllegalArgumentException("X-Idempotency-Key الزامی است.");var x=repository.findIdempotency(key);if(x.isPresent()){IdempotencyRow r=x.get();if(!Objects.equals(r.accountId(),accountId)||!op.equals(r.operationType())||!h.equals(r.payloadHash()))throw new IllegalArgumentException("Idempotency Key با درخواست دیگری استفاده شده است.");if("COMPLETED".equals(r.processingStatus()))return true;throw lifecycle("درخواست با همین Idempotency Key در حال پردازش است.","DEPOSIT_OPERATION_IDEMPOTENCY.PROCESSING_STATUS_CODE",r.processingStatus());}repository.insertIdempotency(key,accountId,op,h,actor,corr);return false;}
    private long parseResult(String key,String prefix){String ref=repository.findIdempotency(key).orElseThrow().resultReference();if(ref==null||!ref.startsWith(prefix))throw new IllegalArgumentException("Idempotency result نامعتبر است.");return Long.parseLong(ref.substring(prefix.length()));}
    private static String upper(String v){return v==null?null:v.trim().toUpperCase(Locale.ROOT);}
    private static String hash(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private static DepositAccountLifecycleException lifecycle(String m,String k,String v){return new DepositAccountLifecycleException(m,Map.of(k,Objects.toString(v,"null")));}
}
