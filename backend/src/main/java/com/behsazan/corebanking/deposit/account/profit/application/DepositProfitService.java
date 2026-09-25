package com.behsazan.corebanking.deposit.account.profit.application;

import com.behsazan.corebanking.deposit.account.balance.application.DepositBalanceService;
import com.behsazan.corebanking.deposit.account.balance.domain.DepositBalanceModels.PostEntryRequest;
import com.behsazan.corebanking.deposit.account.error.DepositAccountLifecycleException;
import com.behsazan.corebanking.deposit.account.error.DepositAccountNotFoundException;
import com.behsazan.corebanking.deposit.account.profit.domain.DepositProfitModels.*;
import com.behsazan.corebanking.deposit.account.profit.oracle.DepositProfitRepository;
import com.behsazan.corebanking.deposit.account.profit.oracle.DepositProfitRepository.*;
import com.behsazan.corebanking.deposit.account.transaction.application.DepositTransactionService;
import com.behsazan.corebanking.deposit.account.transaction.domain.DepositTransactionModels.DerivedTransactionLeg;
import com.behsazan.corebanking.deposit.account.transaction.domain.DepositTransactionModels.DerivedTransactionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class DepositProfitService {
    private final DepositProfitRepository repository;
    private final DepositBalanceService balanceService;
    private final DepositTransactionService transactionService;
    public DepositProfitService(DepositProfitRepository repository,DepositBalanceService balanceService,DepositTransactionService transactionService){this.repository=repository;this.balanceService=balanceService;this.transactionService=transactionService;}

    public record ProfitTransactionResult(Long transactionId,BigDecimal amount) {}

    @Transactional(readOnly=true)
    public ProfitView get(long accountId){requireAccount(accountId,false);return state(accountId);}

    @Transactional(readOnly=true)
    public BigDecimal outstandingPayable(long accountId){requireAccount(accountId,false);return repository.outstandingPayableAmount(accountId).setScale(4,RoundingMode.HALF_UP);}

    /** Always uses Step 05, including SAME_DEPOSIT, for flows whose owning step requires TRANSACTION_ID evidence. */
    @Transactional
    public ProfitTransactionResult postOutstandingViaStep05(long accountId,LocalDate date,String actor,String correlationId,String idempotencyKey){
        var source=requireAccount(accountId,true);ProfitProfile profile=requireProfile(accountId);BigDecimal amount=repository.outstandingPayableAmount(accountId).setScale(4,RoundingMode.HALF_UP);if(amount.signum()<=0)return new ProfitTransactionResult(null,BigDecimal.ZERO.setScale(4));
        String destination=upper(profile.paymentDestinationCode());DepositProfitRepository.AccountRow target;String destRef;
        if("SAME_DEPOSIT".equals(destination)){target=source;destRef="ACCOUNT_ID:"+accountId;}
        else if(Set.of("LINKED_ACCOUNT","CUSTOMER_SELECTED_ACCOUNT").contains(destination)){target=profile.destinationAccountId()!=null?repository.account(profile.destinationAccountId(),false).orElse(null):repository.accountByReference(profile.destinationAccountReference()).orElse(null);destRef=profile.destinationAccountReference();if(target==null)throw lifecycle("مقصد سود به حساب سپرده داخلی قابل Resolve نیست؛ Settlement خارجی خارج از مرز این فاز است.","DEPOSIT_ACCOUNT_PROFIT_PROFILE.DESTINATION_ACCOUNT_REFERENCE",String.valueOf(profile.destinationAccountReference()));}
        else throw lifecycle("PAYMENT_DESTINATION_CODE پشتیبانی‌شده نیست.","DEPOSIT_ACCOUNT_PROFIT_PROFILE.PAYMENT_DESTINATION_CODE",destination);
        if(!"ACTIVE".equals(upper(target.status())))throw lifecycle("حساب مقصد سود باید ACTIVE باشد.","DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE",target.status());if(!upper(target.currencyCode()).equals(upper(source.currencyCode())))throw lifecycle("ارز حساب مقصد سود با حساب مبدأ یکسان نیست.","DEPOSIT_ACCOUNT.CURRENCY_CODE",target.currencyCode());
        String policyActor="deposit.profit.policy";if(policyActor.equalsIgnoreCase(actor))policyActor="deposit.profit.policy.authorizer";LocalDate postingDate=date==null?LocalDate.now(ZoneOffset.UTC):date;
        var tx=transactionService.postDerived(new DerivedTransactionRequest(target.accountId(),"PROFIT_PAYMENT",amount,source.currencyCode(),postingDate,postingDate,"CREDIT","PROFIT_PAYMENT:"+accountId,"PROFIT_POLICY",actor,policyActor,"CANONICAL_PROFIT_PAYMENT",List.of(new DerivedTransactionLeg(null,"PROFIT_EXPENSE:"+accountId,"DEBIT",amount),new DerivedTransactionLeg(target.accountId(),destRef,"CREDIT",amount))),actor,correlationId,childKey(idempotencyKey,"TX"));long txId=tx.transaction().transaction().transactionId();
        recordPaidByTransaction(accountId,txId,postingDate,destination,destRef,amount,actor);return new ProfitTransactionResult(txId,amount);
    }

    /** Records canonical Profit Payment evidence for profit already carried by an owning Step 05 transaction. */
    @Transactional
    public BigDecimal recordPaidByTransaction(long accountId,long transactionId,LocalDate date,String destinationCode,String destinationRef,BigDecimal expectedAmount,String actor){
        ContractLock c=requireContract(accountId);BigDecimal outstanding=repository.outstandingPayableAmount(accountId).setScale(4,RoundingMode.HALF_UP);BigDecimal expected=expectedAmount==null?outstanding:expectedAmount.setScale(4,RoundingMode.HALF_UP);if(outstanding.compareTo(expected)!=0)throw lifecycle("مبلغ سود Settlement با Profit Periodهای قابل پرداخت سازگار نیست.","DEPOSIT_PROFIT_PERIOD.PAYABLE_AMOUNT",outstanding.toPlainString());if(outstanding.signum()<=0)return outstanding;
        BigDecimal paid=BigDecimal.ZERO;LocalDate paymentDate=date==null?LocalDate.now(ZoneOffset.UTC):date;while(true){PeriodLock period=repository.lockPayablePeriod(accountId).orElse(null);if(period==null)break;BigDecimal amount=period.payableAmount().subtract(period.paidAmount()).setScale(4,RoundingMode.HALF_UP);if(amount.signum()<=0)break;long paymentId=repository.insertPayment(period.profitPeriodId(),accountId,period.periodEndDate(),amount,destinationCode,destinationRef,actor);PaymentLock payment=repository.lockPayment(accountId,paymentId).orElseThrow();if(repository.markPaymentPaid(paymentId,paymentDate,"TX-"+transactionId,payment.recordVersion(),actor)!=1)throw lifecycle("Profit Payment همزمان تغییر کرده است.","DEPOSIT_PROFIT_PAYMENT.RECORD_VERSION",String.valueOf(payment.recordVersion()));if(repository.applyPeriodPayment(period.profitPeriodId(),amount,period.recordVersion(),actor)!=1)throw lifecycle("Profit Period همزمان تغییر کرده است.","DEPOSIT_PROFIT_PERIOD.RECORD_VERSION",String.valueOf(period.recordVersion()));paid=paid.add(amount);}
        if(paid.compareTo(outstanding)!=0)throw lifecycle("تمام Profit Periodهای قابل پرداخت تسویه نشدند.","DEPOSIT_PROFIT_PERIOD.PAID_AMOUNT",paid.toPlainString());if(repository.applyPosting(c.profitContractId(),paymentDate,paid,c.recordVersion(),actor)!=1)throw lifecycle("Profit Contract همزمان تغییر کرده است.","DEPOSIT_PROFIT_CONTRACT.RECORD_VERSION",String.valueOf(c.recordVersion()));return paid;
    }

    @Transactional
    public ProfitActionResponse accrue(long accountId,AccrualRequest request,String actor,String correlationId,String idempotencyKey){
        var account=requireAccount(accountId,true);
        if(!"ACTIVE".equals(upper(account.status())))throw lifecycle("Accrual فقط برای حساب ACTIVE مجاز است.","DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE",account.status());
        ContractLock c=requireContract(accountId);ProfitProfile profile=requireProfile(accountId);
        LocalDate through=request==null||request.throughDate()==null?LocalDate.now(ZoneOffset.UTC):request.throughDate();
        LocalDate from=c.lastAccrualDate()==null?c.effectiveFrom():c.lastAccrualDate();
        if(from==null)throw lifecycle("تاریخ شروع Profit Contract مشخص نیست.","DEPOSIT_PROFIT_CONTRACT.EFFECTIVE_FROM","null");
        if(c.effectiveTo()!=null&&through.isAfter(c.effectiveTo()))through=c.effectiveTo();
        if(through.isBefore(from))throw new IllegalArgumentException("THROUGH_DATE نمی‌تواند قبل از LAST_ACCRUAL_DATE باشد.");
        String payload="ACCRUE|"+accountId+"|"+through;String h=hash(payload);
        if(replayOrClaim(idempotencyKey,accountId,"PROFIT_ACCRUAL",h,actor,correlationId))return new ProfitActionResponse(state(accountId),parseResult(idempotencyKey,"PROFIT_ACCRUAL:"),true);
        long days=ChronoUnit.DAYS.between(from,through);
        if(days<=0){repository.completeIdempotency(idempotencyKey,"PROFIT_ACCRUAL:0");return new ProfitActionResponse(state(accountId),0,true);}
        if(!"PERCENTAGE".equals(upper(c.calculationMethodCode())))throw lifecycle("فقط روش PERCENTAGE در Phase 11H پشتیبانی می‌شود.","CALCULATION_METHOD_CODE",c.calculationMethodCode());
        int denominator=switch(upper(c.dayCountBasisCode())){case "ACT_365"->365;case "ACT_360"->360;default->throw lifecycle("Day-count basis پشتیبانی نمی‌شود.","DAY_COUNT_BASIS_CODE",c.dayCountBasisCode());};
        ProfitPeriod period=ensurePeriod(accountId,profile,c,through,actor);
        if(from.isBefore(period.periodStartDate())||through.isAfter(period.periodEndDate()))throw lifecycle(
                "Accrual چند دوره‌ای باید در اجرای جداگانه هر Profit Period انجام شود.","DEPOSIT_PROFIT_PERIOD.PROFIT_PERIOD_ID",String.valueOf(period.profitPeriodId()));
        BigDecimal basis=repository.ledgerBalance(accountId).max(BigDecimal.ZERO);
        BigDecimal amount=basis.multiply(c.annualRate()).multiply(BigDecimal.valueOf(days)).divide(BigDecimal.valueOf(100L*denominator),8,RoundingMode.HALF_UP).setScale(4,RoundingMode.HALF_UP);
        long id=repository.next("SEQ_DEP_PROFIT_ACCRUAL");
        repository.insertAccrual(id,period.profitPeriodId(),c,from,through,(int)days,basis,amount,actor);
        repository.insertAccrualDetail(id,basis,c.annualRate(),amount,"BASE_PROFIT "+upper(c.calculationMethodCode())+" / "+upper(c.dayCountBasisCode()),actor);
        if(repository.applyPeriodAccrual(period.profitPeriodId(),amount,period.recordVersion(),actor)!=1)throw lifecycle("Profit Period همزمان تغییر کرده است.","DEPOSIT_PROFIT_PERIOD.RECORD_VERSION",String.valueOf(period.recordVersion()));
        if(repository.applyAccrual(c.profitContractId(),through,amount,c.recordVersion(),actor)!=1)throw lifecycle("Profit Contract همزمان تغییر کرده است.","DEPOSIT_PROFIT_CONTRACT.RECORD_VERSION",String.valueOf(c.recordVersion()));
        repository.completeIdempotency(idempotencyKey,"PROFIT_ACCRUAL:"+id);
        return new ProfitActionResponse(state(accountId),id,false);
    }

    @Transactional
    public ProfitActionResponse requestAdjustment(long accountId,AdjustmentRequest request,String actor,String correlationId,String idempotencyKey){
        requireAccount(accountId,true);requireContract(accountId);requireProfile(accountId);
        if(request==null)throw new IllegalArgumentException("Adjustment request الزامی است.");
        boolean accrual=request.originalAccrualId()!=null,payment=request.originalPaymentId()!=null;
        if(accrual==payment)throw new IllegalArgumentException("دقیقاً یکی از ORIGINAL_ACCRUAL_ID یا ORIGINAL_PAYMENT_ID باید تعیین شود.");
        String type=upper(request.adjustmentTypeCode());if(!Set.of("INCREASE","DECREASE","REVERSAL").contains(type))throw new IllegalArgumentException("ADJUSTMENT_TYPE_CODE نامعتبر است.");
        BigDecimal amount=request.adjustmentAmount();if(amount==null||amount.signum()<=0)throw new IllegalArgumentException("ADJUSTMENT_AMOUNT باید بزرگتر از صفر باشد.");
        String reason=upper(request.reasonCode());if(reason==null||reason.isBlank())throw new IllegalArgumentException("REASON_CODE الزامی است.");
        String approver=trim(request.approverUserId());if(approver==null||approver.isBlank())throw new IllegalArgumentException("APPROVER_USER_ID الزامی است.");
        if(approver.equalsIgnoreCase(trim(actor)))throw new IllegalArgumentException("Maker و Approver تعدیل سود باید متفاوت باشند.");
        String org=trim(request.orgUnitCode());if(org==null||org.isBlank())org="HQ";
        long periodId=accrual?repository.periodIdForAccrual(accountId,request.originalAccrualId()).orElseThrow(()->lifecycle("Accrual معتبر و متصل به Profit Period یافت نشد.","DEPOSIT_PROFIT_ACCRUAL.PROFIT_ACCRUAL_ID",String.valueOf(request.originalAccrualId()))):repository.periodIdForPayment(accountId,request.originalPaymentId()).orElseThrow(()->lifecycle("Payment معتبر یافت نشد.","DEPOSIT_PROFIT_PAYMENT.PROFIT_PAYMENT_ID",String.valueOf(request.originalPaymentId())));
        String payload="ADJUST|"+accountId+"|"+periodId+"|"+request.originalAccrualId()+"|"+request.originalPaymentId()+"|"+type+"|"+amount.toPlainString()+"|"+reason+"|"+approver;
        if(replayOrClaim(idempotencyKey,accountId,"PROFIT_ADJUSTMENT_REQUEST",hash(payload),actor,correlationId))return new ProfitActionResponse(state(accountId),parseResult(idempotencyKey,"PROFIT_ADJUSTMENT:"),true);
        long id=repository.next("SEQ_DEPOSIT_PROFIT_ADJUSTMENT"),approvalId=repository.next("SEQ_DEPOSIT_OPERATION_APPROVAL_REQUEST");
        repository.insertApproval(approvalId,accountId,id,actor,approver,org,idempotencyKey,correlationId);
        repository.insertAdjustment(id,accountId,periodId,request.originalAccrualId(),request.originalPaymentId(),type,amount,reason,approvalId,actor);
        repository.completeIdempotency(idempotencyKey,"PROFIT_ADJUSTMENT:"+id);
        return new ProfitActionResponse(state(accountId),id,false);
    }

    @Transactional
    public ProfitActionResponse approveAdjustment(long accountId,long adjustmentId,String actor,String correlationId,String idempotencyKey){
        requireAccount(accountId,true);AdjustmentLock a=requireAdjustment(accountId,adjustmentId);
        String payload="ADJUST_APPROVE|"+accountId+"|"+adjustmentId+"|"+actor;
        if(replayOrClaim(idempotencyKey,accountId,"PROFIT_ADJUSTMENT_APPROVE",hash(payload),actor,correlationId))return new ProfitActionResponse(state(accountId),adjustmentId,true);
        if(!"DRAFT".equals(upper(a.adjustmentStatusCode())))throw lifecycle("Adjustment در وضعیت قابل تأیید نیست.","DEPOSIT_PROFIT_ADJUSTMENT.ADJUSTMENT_STATUS_CODE",a.adjustmentStatusCode());
        if(a.approvalRequestId()==null||repository.approve(a.approvalRequestId(),actor)!=1)throw lifecycle("Adjustment فقط توسط Approver تعیین‌شده قابل تأیید است.","DEPOSIT_OPERATION_APPROVAL_REQUEST.APPROVER_USER_ID",actor);
        if(repository.markAdjustmentApproved(adjustmentId,a.recordVersion(),actor)!=1)throw lifecycle("Adjustment همزمان تغییر کرده است.","DEPOSIT_PROFIT_ADJUSTMENT.RECORD_VERSION",String.valueOf(a.recordVersion()));
        repository.completeIdempotency(idempotencyKey,"PROFIT_ADJUSTMENT:"+adjustmentId);
        return new ProfitActionResponse(state(accountId),adjustmentId,false);
    }

    @Transactional
    public ProfitActionResponse postAdjustment(long accountId,long adjustmentId,String actor,String correlationId,String idempotencyKey){
        requireAccount(accountId,true);ContractLock c=requireContract(accountId);AdjustmentLock a=requireAdjustment(accountId,adjustmentId);
        String payload="ADJUST_POST|"+accountId+"|"+adjustmentId;
        if(replayOrClaim(idempotencyKey,accountId,"PROFIT_ADJUSTMENT_POST",hash(payload),actor,correlationId))return new ProfitActionResponse(state(accountId),adjustmentId,true);
        if(!"APPROVED".equals(upper(a.adjustmentStatusCode()))||!"APPROVED".equals(upper(a.approvalStatusCode())))throw lifecycle("Adjustment باید قبل از Posting تأیید شده باشد.","DEPOSIT_PROFIT_ADJUSTMENT.ADJUSTMENT_STATUS_CODE",a.adjustmentStatusCode());
        PeriodLock p=repository.lockPeriod(a.profitPeriodId()).orElseThrow(()->lifecycle("Profit Period Adjustment یافت نشد.","DEPOSIT_PROFIT_PERIOD.PROFIT_PERIOD_ID",String.valueOf(a.profitPeriodId())));
        BigDecimal delta="INCREASE".equals(upper(a.adjustmentTypeCode()))?a.adjustmentAmount():a.adjustmentAmount().negate();
        if(c.accruedAmount().add(delta).signum()<0)throw lifecycle("Adjustment کاهشی از سود Accrued قابل پرداخت بیشتر است.","DEPOSIT_PROFIT_CONTRACT.ACCRUED_AMOUNT",c.accruedAmount().toPlainString());
        if(p.payableAmount().add(delta).compareTo(p.paidAmount())<0)throw lifecycle("Adjustment کاهشی از مانده قابل تعدیل دوره بیشتر است.","DEPOSIT_PROFIT_PERIOD.PAYABLE_AMOUNT",p.payableAmount().toPlainString());
        if(repository.applyPeriodAdjustment(p.profitPeriodId(),delta,p.recordVersion(),actor)!=1)throw lifecycle("Profit Period همزمان تغییر کرده است.","DEPOSIT_PROFIT_PERIOD.RECORD_VERSION",String.valueOf(p.recordVersion()));
        if(repository.applyContractAdjustment(c.profitContractId(),delta,c.recordVersion(),actor)!=1)throw lifecycle("Profit Contract همزمان تغییر کرده است.","DEPOSIT_PROFIT_CONTRACT.RECORD_VERSION",String.valueOf(c.recordVersion()));
        String ref="P11H-ADJ-"+adjustmentId;if(repository.markAdjustmentPosted(adjustmentId,a.recordVersion(),ref,actor)!=1)throw lifecycle("Adjustment در وضعیت APPROVED نیست.","DEPOSIT_PROFIT_ADJUSTMENT.ADJUSTMENT_STATUS_CODE",a.adjustmentStatusCode());
        repository.completeIdempotency(idempotencyKey,"PROFIT_ADJUSTMENT:"+adjustmentId);
        return new ProfitActionResponse(state(accountId),adjustmentId,false);
    }

    @Transactional
    public ProfitActionResponse postAccrued(long accountId,PostProfitRequest request,String actor,String correlationId,String idempotencyKey){
        var account=requireAccount(accountId,true);
        if(!"ACTIVE".equals(upper(account.status())))throw lifecycle("Profit Payment فقط برای حساب ACTIVE مجاز است.","DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE",account.status());
        ContractLock c=requireContract(accountId);ProfitProfile profile=requireProfile(accountId);
        LocalDate date=request==null||request.postingDate()==null?LocalDate.now(ZoneOffset.UTC):request.postingDate();
        String h=hash("POST_PROFIT|"+accountId+"|"+date);
        if(replayOrClaim(idempotencyKey,accountId,"PROFIT_POSTING",h,actor,correlationId))return new ProfitActionResponse(state(accountId),parseResult(idempotencyKey,"PROFIT_POSTING:"),true);
        PeriodLock period=repository.lockPayablePeriod(accountId).orElseThrow(()->lifecycle("Profit Period قابل پرداخت وجود ندارد.","DEPOSIT_PROFIT_PERIOD.ACCOUNT_ID",String.valueOf(accountId)));
        BigDecimal amount=period.payableAmount().subtract(period.paidAmount()).setScale(4,RoundingMode.HALF_UP);
        if(amount.signum()<=0)throw lifecycle("سود قابل پرداخت وجود ندارد.","DEPOSIT_PROFIT_PERIOD.PAYABLE_AMOUNT",period.payableAmount().toPlainString());
        if(c.accruedAmount().compareTo(amount)<0)throw lifecycle("Profit Contract و Profit Period از نظر مبلغ قابل پرداخت ناسازگارند.","DEPOSIT_PROFIT_CONTRACT.ACCRUED_AMOUNT",c.accruedAmount().toPlainString());
        String destination=upper(profile.paymentDestinationCode());
        long paymentId=repository.insertPayment(period.profitPeriodId(),accountId,period.periodEndDate(),amount,destination,profile.destinationAccountReference(),actor);
        String postingReference;long resultId;
        if("SAME_DEPOSIT".equals(destination)){
            long postingId=repository.next("SEQ_DEP_PROFIT_POSTING");
            var posted=balanceService.post(accountId,new PostEntryRequest("CREDIT",amount,account.currencyCode(),date,date,"P11H-PROFIT-"+postingId,"DEPOSIT_PROFIT_POSTING",postingId,null,"PROFIT_PAYMENT"),actor,correlationId,idempotencyKey+"-SL");
            repository.insertPosting(postingId,c.profitContractId(),accountId,date,amount,destination,profile.destinationAccountReference(),posted.subledgerEntryId(),actor);
            repository.linkAccrualsToPosting(c.profitContractId(),period.profitPeriodId(),postingId);
            postingReference="P11H-PROFIT-"+postingId;resultId=postingId;
        }else if(Set.of("LINKED_ACCOUNT","CUSTOMER_SELECTED_ACCOUNT").contains(destination)){
            DepositProfitRepository.AccountRow target=profile.destinationAccountId()!=null?repository.account(profile.destinationAccountId(),false).orElse(null):repository.accountByReference(profile.destinationAccountReference()).orElse(null);
            if(target==null)throw lifecycle("مقصد سود به حساب سپرده داخلی قابل Resolve نیست؛ Settlement خارجی خارج از مرز این فاز است.","DEPOSIT_ACCOUNT_PROFIT_PROFILE.DESTINATION_ACCOUNT_REFERENCE",String.valueOf(profile.destinationAccountReference()));
            if(target.accountId()==accountId)throw lifecycle("برای مقصد همان حساب از SAME_DEPOSIT استفاده کنید.","DEPOSIT_ACCOUNT_PROFIT_PROFILE.PAYMENT_DESTINATION_CODE",destination);
            if(!"ACTIVE".equals(upper(target.status())))throw lifecycle("حساب مقصد سود باید ACTIVE باشد.","DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE",target.status());
            if(!upper(target.currencyCode()).equals(upper(account.currencyCode())))throw lifecycle("ارز حساب مقصد سود با حساب مبدأ یکسان نیست.","DEPOSIT_ACCOUNT.CURRENCY_CODE",target.currencyCode());
            String destRef=profile.destinationAccountReference()==null?"ACCOUNT_ID:"+target.accountId():profile.destinationAccountReference();String policyActor="deposit.profit.policy";if(policyActor.equalsIgnoreCase(actor))policyActor="deposit.profit.policy.authorizer";
            var tx=transactionService.postDerived(new DerivedTransactionRequest(target.accountId(),"PROFIT_PAYMENT",amount,account.currencyCode(),date,date,"CREDIT","PROFIT_PAYMENT:"+paymentId,"PROFIT_POLICY",actor,policyActor,"CANONICAL_PROFIT_PAYMENT",List.of(new DerivedTransactionLeg(null,"PROFIT_EXPENSE:"+accountId,"DEBIT",amount),new DerivedTransactionLeg(target.accountId(),destRef,"CREDIT",amount))),actor,correlationId,childKey(idempotencyKey,"TX"));
            long txId=tx.transaction().transaction().transactionId();postingReference="TX-"+txId;resultId=txId;
        }else throw lifecycle("PAYMENT_DESTINATION_CODE پشتیبانی‌شده نیست.","DEPOSIT_ACCOUNT_PROFIT_PROFILE.PAYMENT_DESTINATION_CODE",destination);
        PaymentLock payment=repository.lockPayment(accountId,paymentId).orElseThrow();
        if(repository.markPaymentPaid(paymentId,date,postingReference,payment.recordVersion(),actor)!=1)throw lifecycle("Profit Payment همزمان تغییر کرده است.","DEPOSIT_PROFIT_PAYMENT.RECORD_VERSION",String.valueOf(payment.recordVersion()));
        if(repository.applyPeriodPayment(period.profitPeriodId(),amount,period.recordVersion(),actor)!=1)throw lifecycle("Profit Period همزمان تغییر کرده است.","DEPOSIT_PROFIT_PERIOD.RECORD_VERSION",String.valueOf(period.recordVersion()));
        if(repository.applyPosting(c.profitContractId(),date,amount,c.recordVersion(),actor)!=1)throw lifecycle("Profit Contract همزمان تغییر کرده است.","DEPOSIT_PROFIT_CONTRACT.RECORD_VERSION",String.valueOf(c.recordVersion()));
        repository.completeIdempotency(idempotencyKey,"PROFIT_POSTING:"+resultId);
        return new ProfitActionResponse(state(accountId),resultId,false);
    }

    private ProfitView state(long accountId){
        return new ProfitView(requireProfile(accountId),repository.contract(accountId).orElseThrow(()->lifecycle("Profit Contract فعال برای حساب یافت نشد.","DEPOSIT_PROFIT_CONTRACT.ACCOUNT_ID",String.valueOf(accountId))),repository.periods(accountId),repository.accruals(accountId),repository.accrualDetails(accountId),repository.adjustments(accountId),repository.payments(accountId),repository.postings(accountId));
    }
    private ProfitPeriod ensurePeriod(long accountId,ProfitProfile profile,ContractLock c,LocalDate date,String actor){
        var existing=repository.periodForDate(accountId,date);if(existing.isPresent())return existing.get();
        LocalDate[] b=periodBounds(c,date);repository.createPeriod(profile.accountProfitProfileId(),b[0],b[1],actor);
        return repository.periodForDate(accountId,date).orElseThrow(()->lifecycle("Profit Period ایجاد نشد.","DEPOSIT_ACCOUNT_PROFIT_PROFILE.ACCOUNT_PROFIT_PROFILE_ID",String.valueOf(profile.accountProfitProfileId())));
    }
    private static LocalDate[] periodBounds(ContractLock c,LocalDate date){
        LocalDate start,end;String f=upper(c.paymentFrequencyCode());
        switch(f){
            case "MATURITY"->{start=c.effectiveFrom();end=c.effectiveTo();if(end==null)throw lifecycle("برای دوره MATURITY تاریخ پایان قرارداد الزامی است.","DEPOSIT_PROFIT_CONTRACT.EFFECTIVE_TO","null");}
            case "MONTHLY"->{start=date.withDayOfMonth(1);end=date.with(TemporalAdjusters.lastDayOfMonth());}
            case "QUARTERLY"->{int m=((date.getMonthValue()-1)/3)*3+1;start=LocalDate.of(date.getYear(),m,1);end=start.plusMonths(3).minusDays(1);}
            case "SEMI_ANNUAL"->{int m=date.getMonthValue()<=6?1:7;start=LocalDate.of(date.getYear(),m,1);end=start.plusMonths(6).minusDays(1);}
            case "ANNUAL"->{start=LocalDate.of(date.getYear(),1,1);end=LocalDate.of(date.getYear(),12,31);}
            default->throw lifecycle("PAYMENT_FREQUENCY_CODE برای Profit Period پشتیبانی نمی‌شود.","DEPOSIT_PROFIT_CONTRACT.PAYMENT_FREQUENCY_CODE",String.valueOf(f));
        }
        if(c.effectiveFrom()!=null&&start.isBefore(c.effectiveFrom()))start=c.effectiveFrom();if(c.effectiveTo()!=null&&end.isAfter(c.effectiveTo()))end=c.effectiveTo();
        if(end==null||end.isBefore(start))throw lifecycle("بازه Profit Period نامعتبر است.","DEPOSIT_PROFIT_PERIOD.PERIOD_START_DATE",String.valueOf(start));return new LocalDate[]{start,end};
    }
    private DepositProfitRepository.AccountRow requireAccount(long id,boolean lock){return repository.account(id,lock).orElseThrow(()->new DepositAccountNotFoundException("حساب سپرده با شناسه "+id+" یافت نشد."));}
    private ContractLock requireContract(long id){return repository.lockContract(id).orElseThrow(()->lifecycle("Profit Contract فعال برای حساب یافت نشد؛ migration/activation را بررسی کنید.","DEPOSIT_PROFIT_CONTRACT.ACCOUNT_ID",String.valueOf(id)));}
    private ProfitProfile requireProfile(long id){return repository.profile(id).orElseThrow(()->lifecycle("Profit Profile فعال برای حساب یافت نشد؛ Phase 11H migration/activation را بررسی کنید.","DEPOSIT_ACCOUNT_PROFIT_PROFILE.ACCOUNT_ID",String.valueOf(id)));}
    private AdjustmentLock requireAdjustment(long accountId,long id){return repository.lockAdjustment(accountId,id).orElseThrow(()->lifecycle("Profit Adjustment یافت نشد.","DEPOSIT_PROFIT_ADJUSTMENT.PROFIT_ADJUSTMENT_ID",String.valueOf(id)));}
    private boolean replayOrClaim(String key,long accountId,String op,String h,String actor,String corr){if(key==null||key.isBlank())throw new IllegalArgumentException("X-Idempotency-Key الزامی است.");var x=repository.findIdempotency(key);if(x.isPresent()){IdempotencyRow r=x.get();if(!Objects.equals(r.accountId(),accountId)||!op.equals(r.operationType())||!h.equals(r.payloadHash()))throw new IllegalArgumentException("Idempotency Key با درخواست دیگری استفاده شده است.");if("COMPLETED".equals(r.processingStatus()))return true;throw lifecycle("درخواست با همین Idempotency Key در حال پردازش است.","DEPOSIT_OPERATION_IDEMPOTENCY.PROCESSING_STATUS_CODE",r.processingStatus());}repository.insertIdempotency(key,accountId,op,h,actor,corr);return false;}
    private long parseResult(String key,String prefix){String ref=repository.findIdempotency(key).orElseThrow().resultReference();if(ref==null||!ref.startsWith(prefix))throw new IllegalArgumentException("Idempotency result نامعتبر است.");return Long.parseLong(ref.substring(prefix.length()));}
    private static String upper(String v){return v==null?null:v.trim().toUpperCase(Locale.ROOT);}private static String trim(String v){return v==null?null:v.trim();}
    private static String childKey(String base,String suffix){String x=base+"-"+suffix;return x.length()<=80?x:hash(base+"|"+suffix);}
    private static String hash(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private static DepositAccountLifecycleException lifecycle(String m,String k,String v){return new DepositAccountLifecycleException(m,Map.of(k,Objects.toString(v,"null")));}
}
