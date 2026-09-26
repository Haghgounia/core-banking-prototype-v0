package com.behsazan.corebanking.deposit.account.transaction.application;

import com.behsazan.corebanking.deposit.account.balance.application.DepositBalanceService;
import com.behsazan.corebanking.deposit.account.balance.domain.DepositBalanceModels.PostEntryRequest;
import com.behsazan.corebanking.deposit.account.error.DepositAccountLifecycleException;
import com.behsazan.corebanking.deposit.account.error.DepositAccountNotFoundException;
import com.behsazan.corebanking.deposit.account.transaction.domain.DepositTransactionModels.*;
import com.behsazan.corebanking.deposit.account.transaction.oracle.DepositTransactionRepository;
import com.behsazan.corebanking.deposit.account.transaction.oracle.DepositTransactionRepository.*;
import com.behsazan.corebanking.deposit.account.waveb.application.DepositAccountWaveBService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.HexFormat;

@Service
public class DepositTransactionService {
    private static final Set<String> TYPES=Set.of("CASH_DEPOSIT","CASH_WITHDRAWAL","TRANSFER");
    private static final Set<String> DERIVED_TYPES=Set.of("TERM_PARTIAL_WITHDRAWAL","MATURITY_SETTLEMENT","TERM_EARLY_TERMINATION","PROFIT_PAYMENT","ACCOUNT_CLOSURE_SETTLEMENT");
    private static final Set<String> CHANNELS=Set.of("BRANCH","INTERNET","MOBILE","API");
    private final DepositTransactionRepository repository;
    private final DepositBalanceService balanceService;
    private final DepositAccountWaveBService waveBService;

    public DepositTransactionService(DepositTransactionRepository repository,DepositBalanceService balanceService,DepositAccountWaveBService waveBService){this.repository=repository;this.balanceService=balanceService;this.waveBService=waveBService;}

    @Transactional(readOnly=true)
    public TransactionListView list(long accountId){requireAccount(accountId,false);return new TransactionListView(repository.list(accountId).stream().map(t->view(t.transactionId())).toList());}

    @Transactional(readOnly=true)
    public TransactionView get(long accountId,long transactionId){TransactionRecord t=repository.transaction(transactionId).orElseThrow(()->new IllegalArgumentException("تراکنش یافت نشد."));if(t.accountId()!=accountId)throw new IllegalArgumentException("تراکنش متعلق به حساب دیگری است.");return view(transactionId);}

    @Transactional
    public TransactionActionResponse initiate(long accountId,InitiateTransactionRequest request,String actor,String correlationId,String idempotencyKey){
        if(request==null)throw new IllegalArgumentException("اطلاعات تراکنش الزامی است.");
        String type=upper(required(request.transactionTypeCode(),"TRANSACTION_TYPE_CODE"));if(!TYPES.contains(type))throw new IllegalArgumentException("TRANSACTION_TYPE_CODE نامعتبر است: "+type);
        BigDecimal amount=request.amount();if(amount==null||amount.signum()<=0)throw new IllegalArgumentException("مبلغ تراکنش باید مثبت باشد.");
        String channel=upper(required(request.channelCode(),"CHANNEL_CODE"));if(!CHANNELS.contains(channel))throw new IllegalArgumentException("CHANNEL_CODE نامعتبر است: "+channel);
        AccountRow a=requireAccount(accountId,false);String currency=upper(request.currencyCode()==null?a.currencyCode():request.currencyCode());if(!upper(a.currencyCode()).equals(currency))throw new IllegalArgumentException("ارز تراکنش باید با ارز حساب یکسان باشد.");
        LocalDate booking=request.bookingDate()==null?LocalDate.now(ZoneOffset.UTC):request.bookingDate();LocalDate value=request.valueDate()==null?booking:request.valueDate();
        String dc="CASH_DEPOSIT".equals(type)?"CREDIT":"DEBIT";
        String destination=trim(request.destinationAccountReference());String transferType=null;AccountRow destinationAccount=null;
        if("TRANSFER".equals(type)){
            destination=required(destination,"DESTINATION_ACCOUNT_REFERENCE");destinationAccount=repository.accountByReference(destination).orElse(null);
            transferType=upper(trim(request.transferTypeCode()));if(transferType==null)transferType=destinationAccount==null?"EXTERNAL":"INTERNAL";
            if(!Set.of("INTERNAL","EXTERNAL").contains(transferType))throw new IllegalArgumentException("TRANSFER_TYPE_CODE نامعتبر است.");
            if("INTERNAL".equals(transferType)&&destinationAccount==null)throw new IllegalArgumentException("حساب مقصد داخلی یافت نشد.");
            if(destinationAccount!=null&&destinationAccount.accountId()==accountId)throw new IllegalArgumentException("حساب مقصد انتقال باید با حساب مبدأ متفاوت باشد.");
            if(destinationAccount!=null&&!upper(destinationAccount.currencyCode()).equals(currency))throw new IllegalArgumentException("ارز حساب مقصد داخلی با تراکنش یکسان نیست.");
        }
        String cashRef=trim(request.cashManagementTxnRef());
        if(type.startsWith("CASH_")&&cashRef==null)throw new IllegalArgumentException("CASH_MANAGEMENT_TXN_REF برای تراکنش نقدی الزامی است.");
        String payload=String.join("|",String.valueOf(accountId),type,amount.toPlainString(),currency,booking.toString(),value.toString(),channel,Objects.toString(destination,""),Objects.toString(transferType,""),Objects.toString(cashRef,""));
        if(replayOrClaim(idempotencyKey,accountId,"TX_INITIATE",hash(payload),actor,correlationId))return new TransactionActionResponse(view(parseResult(idempotencyKey,"TX:")),true);
        long id=repository.next("SEQ_DEPOSIT_TRANSACTION");String ref="TX-"+id;
        repository.insertTransaction(id,ref,idempotencyKey,accountId,type,amount,currency,value,booking,channel,dc,null,"TRANSFER".equals(type)?destination:cashRef,actor);
        repository.history(repository.next("SEQ_DEPOSIT_TRANSACTION_STATUS_HISTORY"),id,null,"INITIATED","CREATED",actor);
        if("TRANSFER".equals(type)){
            long l1=repository.next("SEQ_DEPOSIT_TRANSACTION_LEG"),l2=repository.next("SEQ_DEPOSIT_TRANSACTION_LEG");
            repository.leg(l1,id,1,accountId,a.accountNo(),"DEBIT",amount,currency,actor);
            repository.leg(l2,id,2,destinationAccount==null?null:destinationAccount.accountId(),destination,"CREDIT",amount,currency,actor);
            repository.transferDetail(repository.next("SEQ_DEPOSIT_TRANSFER_DETAIL"),id,transferType,a.accountNo(),destination,trim(request.destinationBankCode()),trim(request.paymentPurposeCode()),actor);
        } else {
            repository.leg(repository.next("SEQ_DEPOSIT_TRANSACTION_LEG"),id,1,accountId,a.accountNo(),dc,amount,currency,actor);
            repository.cashDetail(repository.next("SEQ_DEPOSIT_CASH_TRANSACTION_DETAIL"),id,"CASH_DEPOSIT".equals(type)?"DEPOSIT":"WITHDRAWAL",trim(request.cashDeskCode()),trim(request.tellerId()),trim(request.sourceOfFundsCode()),cashRef,actor);
        }
        repository.completeIdempotency(idempotencyKey,"TX:"+id);
        return new TransactionActionResponse(view(id),false);
    }

    @Transactional
    public TransactionActionResponse validate(long accountId,long txId,String actor,String correlationId,String idempotencyKey){
        String h=hash("VALIDATE|"+accountId+"|"+txId);if(replayOrClaim(idempotencyKey,accountId,"TX_VALIDATE",h,actor,correlationId))return new TransactionActionResponse(view(txId),true);
        TxLock t=lock(accountId,txId);if(!"INITIATED".equals(upper(t.status())))throw lifecycle("فقط تراکنش INITIATED قابل اعتبارسنجی است.","DEPOSIT_TRANSACTION.TRANSACTION_STATUS_CODE",t.status());
        List<Check> checks=checks(t);repository.clearValidations(txId);boolean failed=false;
        for(Check c:checks){repository.validation(repository.next("SEQ_DEPOSIT_TRANSACTION_VALIDATION"),txId,c.code,c.type,c.pass?"PASS":c.warn?"WARN":"FAIL",c.message,c.rule,actor);if(!c.pass&&!c.warn)failed=true;}
        if(failed){if(repository.transition(txId,t.recordVersion(),"INITIATED","REJECTED",actor)!=1)throw concurrent(t);repository.history(repository.next("SEQ_DEPOSIT_TRANSACTION_STATUS_HISTORY"),txId,"INITIATED","REJECTED","VALIDATION_FAILED",actor);repository.completeIdempotency(idempotencyKey,"TX:"+txId);return new TransactionActionResponse(view(txId),false);}
        if(repository.transition(txId,t.recordVersion(),"INITIATED","VALIDATED",actor)!=1)throw concurrent(t);repository.history(repository.next("SEQ_DEPOSIT_TRANSACTION_STATUS_HISTORY"),txId,"INITIATED","VALIDATED","VALIDATION_PASS",actor);
        TxLock validated=lock(accountId,txId);if(repository.transition(txId,validated.recordVersion(),"VALIDATED","PENDING_AUTH",actor)!=1)throw concurrent(validated);repository.history(repository.next("SEQ_DEPOSIT_TRANSACTION_STATUS_HISTORY"),txId,"VALIDATED","PENDING_AUTH","AUTHORIZATION_REQUIRED",actor);
        repository.completeIdempotency(idempotencyKey,"TX:"+txId);return new TransactionActionResponse(view(txId),false);
    }

    @Transactional
    public TransactionActionResponse authorize(long accountId,long txId,String actor,String correlationId,String idempotencyKey){
        String h=hash("AUTHORIZE|"+accountId+"|"+txId+"|"+actor);if(replayOrClaim(idempotencyKey,accountId,"TX_AUTHORIZE",h,actor,correlationId))return new TransactionActionResponse(view(txId),true);
        TxLock t=lock(accountId,txId);if(!"PENDING_AUTH".equals(upper(t.status())))throw lifecycle("تراکنش در انتظار مجوز نیست.","DEPOSIT_TRANSACTION.TRANSACTION_STATUS_CODE",t.status());if(actor.equalsIgnoreCase(t.createdBy()))throw lifecycle("Maker و Authorizer تراکنش باید متفاوت باشند.","DEPOSIT_TRANSACTION_AUTHORIZATION.DECIDED_BY",actor);
        try{repository.authorization(repository.next("SEQ_DEPOSIT_TRANSACTION_AUTHORIZATION"),txId,t.createdBy(),actor,"STEP05_AUTHORIZATION",actor);}catch(DuplicateKeyException e){throw lifecycle("Authorization این سطح قبلاً ثبت شده است.","DEPOSIT_TRANSACTION_AUTHORIZATION.TRANSACTION_ID",String.valueOf(txId));}
        if(repository.transition(txId,t.recordVersion(),"PENDING_AUTH","AUTHORIZED",actor)!=1)throw concurrent(t);repository.history(repository.next("SEQ_DEPOSIT_TRANSACTION_STATUS_HISTORY"),txId,"PENDING_AUTH","AUTHORIZED","MAKER_CHECKER_APPROVED",actor);repository.completeIdempotency(idempotencyKey,"TX:"+txId);return new TransactionActionResponse(view(txId),false);
    }

    @Transactional
    public TransactionActionResponse post(long accountId,long txId,String actor,String correlationId,String idempotencyKey){
        String h=hash("POST|"+accountId+"|"+txId);if(replayOrClaim(idempotencyKey,accountId,"TX_POST",h,actor,correlationId))return new TransactionActionResponse(view(txId),true);
        TxLock t=lock(accountId,txId);if(!"AUTHORIZED".equals(upper(t.status())))throw lifecycle("فقط تراکنش AUTHORIZED قابل ثبت مالی است.","DEPOSIT_TRANSACTION.TRANSACTION_STATUS_CODE",t.status());
        List<Check> latest=checks(t);String blockers=latest.stream().filter(c->!c.pass&&!c.warn).map(c->c.message).reduce((a,b)->a+"؛ "+b).orElse(null);if(blockers!=null)throw lifecycle("کنترل نهایی Posting ناموفق است: "+blockers,"DEPOSIT_TRANSACTION.TRANSACTION_ID",String.valueOf(txId));
        for(TransactionLeg leg:repository.legs(txId)){
            if(leg.accountId()==null)continue;
            String postingRef="DPS2-TX-"+txId+"-L"+leg.legNo();
            balanceService.post(leg.accountId(),new PostEntryRequest(leg.debitCreditCode(),leg.amount(),leg.currencyCode(),t.bookingDate(),t.valueDate(),postingRef,"DEPOSIT_TRANSACTION",txId,null,null,txId,leg.transactionLegId(),leg.legNo()),actor,correlationId,idempotencyKey+"-L"+leg.legNo());
            repository.setLegPostingReference(leg.transactionLegId(),postingRef,actor);
        }
        if(repository.transition(txId,t.recordVersion(),"AUTHORIZED","POSTED",actor)!=1)throw concurrent(t);repository.history(repository.next("SEQ_DEPOSIT_TRANSACTION_STATUS_HISTORY"),txId,"AUTHORIZED","POSTED","FINANCIAL_POSTING_COMPLETED",actor);waveBService.recordPostedTransactionUsage(t.accountId(),txId,t.type(),t.channel(),t.amount(),t.bookingDate(),actor);repository.completeIdempotency(idempotencyKey,"TX:"+txId);return new TransactionActionResponse(view(txId),false);
    }

    /**
     * Internal canonical Step 05 posting path for financial effects owned by another documented package.
     * Public transaction initiation remains restricted to CASH_DEPOSIT/CASH_WITHDRAWAL/TRANSFER.
     */
    @Transactional
    public TransactionActionResponse postDerived(DerivedTransactionRequest request,String actor,String correlationId,String idempotencyKey){
        if(request==null)throw new IllegalArgumentException("Derived transaction request الزامی است.");
        long accountId=request.contextAccountId();
        String type=upper(required(request.transactionTypeCode(),"TRANSACTION_TYPE_CODE"));
        if(!DERIVED_TYPES.contains(type))throw new IllegalArgumentException("Derived TRANSACTION_TYPE_CODE نامعتبر است: "+type);
        BigDecimal amount=request.amount();if(amount==null||amount.signum()<=0)throw new IllegalArgumentException("مبلغ Derived transaction باید مثبت باشد.");
        AccountRow context=requireAccount(accountId,false);String currency=upper(request.currencyCode()==null?context.currencyCode():request.currencyCode());
        if(!upper(context.currencyCode()).equals(currency))throw new IllegalArgumentException("ارز Derived transaction باید با حساب context یکسان باشد.");
        String dc=upper(required(request.debitCreditCode(),"DEBIT_CREDIT_CODE"));if(!Set.of("DEBIT","CREDIT").contains(dc))throw new IllegalArgumentException("DEBIT_CREDIT_CODE نامعتبر است.");
        String requestedBy=required(request.requestedBy(),"REQUESTED_BY");String decidedBy=required(request.decidedBy(),"DECIDED_BY");
        if(requestedBy.equalsIgnoreCase(decidedBy))throw lifecycle("Requester و Authorizer تراکنش مشتق‌شده باید متفاوت باشند.","DEPOSIT_TRANSACTION_AUTHORIZATION.DECIDED_BY",decidedBy);
        String authType=upper(required(request.authorizationTypeCode(),"AUTHORIZATION_TYPE_CODE"));
        List<DerivedTransactionLeg> legs=request.legs()==null?List.of():request.legs();if(legs.isEmpty())throw new IllegalArgumentException("حداقل یک Transaction Leg الزامی است.");
        BigDecimal debit=BigDecimal.ZERO,credit=BigDecimal.ZERO;
        for(DerivedTransactionLeg leg:legs){if(leg==null||leg.amount()==null||leg.amount().signum()<=0)throw new IllegalArgumentException("مبلغ Leg باید مثبت باشد.");String ldc=upper(required(leg.debitCreditCode(),"LEG.DEBIT_CREDIT_CODE"));if("DEBIT".equals(ldc))debit=debit.add(leg.amount());else if("CREDIT".equals(ldc))credit=credit.add(leg.amount());else throw new IllegalArgumentException("LEG.DEBIT_CREDIT_CODE نامعتبر است.");if(leg.accountId()!=null){AccountRow la=requireAccount(leg.accountId(),false);if(!upper(la.currencyCode()).equals(currency))throw new IllegalArgumentException("ارز حساب Transaction Leg ناسازگار است.");}else required(leg.accountReference(),"LEG.ACCOUNT_REFERENCE");}
        if(debit.compareTo(credit)!=0)throw new IllegalArgumentException("Derived transaction legs باید تراز باشند.");
        LocalDate booking=request.bookingDate()==null?LocalDate.now(ZoneOffset.UTC):request.bookingDate();LocalDate value=request.valueDate()==null?booking:request.valueDate();
        String payload=String.join("|",String.valueOf(accountId),type,amount.toPlainString(),currency,booking.toString(),value.toString(),dc,Objects.toString(request.externalReference(),""),authType,requestedBy,decidedBy,legs.toString());
        if(replayOrClaim(idempotencyKey,accountId,"TX_DERIVED_POST",hash(payload),actor,correlationId))return new TransactionActionResponse(view(parseResult(idempotencyKey,"TX:")),true);
        long id=repository.next("SEQ_DEPOSIT_TRANSACTION");String ref="TX-"+id;
        repository.insertTransaction(id,ref,idempotencyKey,accountId,type,amount,currency,value,booking,"API",dc,null,trim(request.externalReference()),requestedBy);
        repository.history(repository.next("SEQ_DEPOSIT_TRANSACTION_STATUS_HISTORY"),id,null,"INITIATED","DERIVED_OPERATION_CREATED",actor);
        int no=0;for(DerivedTransactionLeg leg:legs){no++;String accountRef=trim(leg.accountReference());if(leg.accountId()!=null&&accountRef==null)accountRef=repository.account(leg.accountId(),false).map(AccountRow::accountNo).orElseThrow();repository.leg(repository.next("SEQ_DEPOSIT_TRANSACTION_LEG"),id,no,leg.accountId(),accountRef,upper(leg.debitCreditCode()),leg.amount(),currency,actor);}
        TxLock initiated=lock(accountId,id);BigDecimal contextAmount=legs.stream().filter(x->x.accountId()!=null&&x.accountId()==accountId&&dc.equals(upper(x.debitCreditCode()))).map(DerivedTransactionLeg::amount).reduce(BigDecimal.ZERO,BigDecimal::add);if(contextAmount.signum()<=0)contextAmount=initiated.amount();TxLock validationLock=new TxLock(initiated.transactionId(),initiated.accountId(),initiated.type(),contextAmount,initiated.currencyCode(),initiated.valueDate(),initiated.bookingDate(),initiated.channel(),initiated.dc(),initiated.status(),initiated.originalTransactionId(),initiated.createdBy(),initiated.recordVersion());List<Check> validations=checks(validationLock);repository.clearValidations(id);String blockers=null;
        for(Check c:validations){repository.validation(repository.next("SEQ_DEPOSIT_TRANSACTION_VALIDATION"),id,c.code,c.type,c.pass?"PASS":c.warn?"WARN":"FAIL",c.message,c.rule,actor);if(!c.pass&&!c.warn)blockers=blockers==null?c.message:blockers+"؛ "+c.message;}
        if(blockers!=null)throw lifecycle("کنترل Derived transaction ناموفق است: "+blockers,"DEPOSIT_TRANSACTION.TRANSACTION_ID",String.valueOf(id));
        if(repository.transition(id,initiated.recordVersion(),"INITIATED","VALIDATED",actor)!=1)throw concurrent(initiated);repository.history(repository.next("SEQ_DEPOSIT_TRANSACTION_STATUS_HISTORY"),id,"INITIATED","VALIDATED","VALIDATION_PASS",actor);
        TxLock validated=lock(accountId,id);if(repository.transition(id,validated.recordVersion(),"VALIDATED","PENDING_AUTH",actor)!=1)throw concurrent(validated);repository.history(repository.next("SEQ_DEPOSIT_TRANSACTION_STATUS_HISTORY"),id,"VALIDATED","PENDING_AUTH","DERIVED_AUTHORIZATION_REQUIRED",actor);
        repository.authorization(repository.next("SEQ_DEPOSIT_TRANSACTION_AUTHORIZATION"),id,authType,requestedBy,decidedBy,trim(request.authorizationReasonCode()),actor);
        TxLock pending=lock(accountId,id);if(repository.transition(id,pending.recordVersion(),"PENDING_AUTH","AUTHORIZED",actor)!=1)throw concurrent(pending);repository.history(repository.next("SEQ_DEPOSIT_TRANSACTION_STATUS_HISTORY"),id,"PENDING_AUTH","AUTHORIZED","DERIVED_AUTHORIZATION_APPROVED",actor);
        TxLock authorized=lock(accountId,id);for(TransactionLeg leg:repository.legs(id)){if(leg.accountId()==null)continue;String postingRef="DPS2-TX-"+id+"-L"+leg.legNo();balanceService.post(leg.accountId(),new PostEntryRequest(leg.debitCreditCode(),leg.amount(),leg.currencyCode(),booking,value,postingRef,"DEPOSIT_TRANSACTION",id,null,null,id,leg.transactionLegId(),leg.legNo()),actor,correlationId,idempotencyKey+"-L"+leg.legNo());repository.setLegPostingReference(leg.transactionLegId(),postingRef,actor);}
        if(repository.transition(id,authorized.recordVersion(),"AUTHORIZED","POSTED",actor)!=1)throw concurrent(authorized);repository.history(repository.next("SEQ_DEPOSIT_TRANSACTION_STATUS_HISTORY"),id,"AUTHORIZED","POSTED","FINANCIAL_POSTING_COMPLETED",actor);waveBService.recordPostedTransactionUsage(accountId,id,type,"API",amount,booking,actor);repository.completeIdempotency(idempotencyKey,"TX:"+id);return new TransactionActionResponse(view(id),false);
    }

    @Transactional
    public TransactionActionResponse reverse(long accountId,long txId,ReversalRequest request,String actor,String correlationId,String idempotencyKey){
        String reason=upper(required(request==null?null:request.reasonCode(),"REVERSAL_REASON_CODE"));String h=hash("REVERSE|"+accountId+"|"+txId+"|"+reason);if(replayOrClaim(idempotencyKey,accountId,"TX_REVERSE",h,actor,correlationId)){long rev=parseResult(idempotencyKey,"REVTX:");return new TransactionActionResponse(view(rev),true);}
        TxLock original=lock(accountId,txId);if(!"POSTED".equals(upper(original.status())))throw lifecycle("فقط تراکنش POSTED قابل برگشت است.","DEPOSIT_TRANSACTION.TRANSACTION_STATUS_CODE",original.status());if(repository.reversalByOriginal(txId)!=null)throw lifecycle("برای تراکنش قبلاً Reversal ثبت شده است.","DEPOSIT_TRANSACTION_REVERSAL.ORIGINAL_TRANSACTION_ID",String.valueOf(txId));
        List<OriginalPosting> postings=repository.originalPostings(txId);if(postings.stream().filter(x->x.accountId()!=null).anyMatch(x->x.subledgerEntryId()==null))throw lifecycle("Trace مالی تراکنش اصلی ناقص است.","DEPOSIT_SUBLEDGER_ENTRY.TRANSACTION_ID",String.valueOf(txId));
        long revId=repository.next("SEQ_DEPOSIT_TRANSACTION");String ref="REV-"+revId;repository.insertTransaction(revId,ref,idempotencyKey+"-REVTX",accountId,"REVERSAL",original.amount(),original.currencyCode(),original.valueDate(),LocalDate.now(ZoneOffset.UTC),original.channel(),opposite(original.dc()),txId,original.transactionId()+"",actor);repository.history(repository.next("SEQ_DEPOSIT_TRANSACTION_STATUS_HISTORY"),revId,null,"INITIATED","REVERSAL_CREATED",actor);
        int no=0;for(OriginalPosting op:postings){no++;long legId=repository.next("SEQ_DEPOSIT_TRANSACTION_LEG");String dc=opposite(op.dc());repository.leg(legId,revId,no,op.accountId(),op.accountReference(),dc,op.amount(),op.currencyCode(),actor);if(op.accountId()!=null){String postingRef="DPS2-REV-"+revId+"-L"+no;balanceService.post(op.accountId(),new PostEntryRequest(dc,op.amount(),op.currencyCode(),LocalDate.now(ZoneOffset.UTC),original.valueDate(),postingRef,"DEPOSIT_TRANSACTION_REVERSAL",revId,op.subledgerEntryId(),null,revId,legId,no),actor,correlationId,idempotencyKey+"-REV-L"+no);repository.setLegPostingReference(legId,postingRef,actor);}}
        repository.forceStatus(revId,"POSTED",actor);repository.history(repository.next("SEQ_DEPOSIT_TRANSACTION_STATUS_HISTORY"),revId,"INITIATED","POSTED","REVERSAL_POSTED",actor);repository.forceStatus(txId,"REVERSED",actor);repository.history(repository.next("SEQ_DEPOSIT_TRANSACTION_STATUS_HISTORY"),txId,"POSTED","REVERSED",reason,actor);waveBService.recordPostedTransactionUsage(original.accountId(),txId,original.type(),original.channel(),original.amount(),original.bookingDate(),actor);repository.reversal(repository.next("SEQ_DEPOSIT_TRANSACTION_REVERSAL"),txId,revId,reason,actor);repository.completeIdempotency(idempotencyKey,"REVTX:"+revId);return new TransactionActionResponse(view(revId),false);
    }

    private List<Check> checks(TxLock t){
        List<Check> out=new ArrayList<>();AccountRow a=requireAccount(t.accountId(),false);String dc=upper(t.dc());String status=upper(a.status());boolean statusOk="DEBIT".equals(dc)?"ACTIVE".equals(status):!Set.of("PENDING_ACTIVATION","CLOSED").contains(status);out.add(new Check("ACCOUNT_STATUS","ACCOUNT_STATUS",statusOk,false,statusOk?"وضعیت حساب مجاز است.":"وضعیت حساب برای جهت تراکنش مجاز نیست.","DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE"));
        boolean holdOk="DEBIT".equals(dc)?repository.activeDebitHoldCount(t.accountId())==0:repository.activeCreditHoldCount(t.accountId())==0;out.add(new Check("ACTIVE_HOLD","HOLD",holdOk,false,holdOk?"Hold مسدودکننده وجود ندارد.":"Hold فعال مانع جهت تراکنش است.","DEPOSIT_ACCOUNT_HOLD"));
        if("DEBIT".equals(dc)){BigDecimal available=balanceService.currentBalance(t.accountId()).availableBalance();boolean ok=available.compareTo(t.amount())>=0;out.add(new Check("AVAILABLE_BALANCE","LIMIT",ok,false,ok?"مانده قابل برداشت کافی است.":"مانده قابل برداشت کافی نیست.","DEPOSIT_ACCOUNT_BALANCE.AVAILABLE_BALANCE"));}
        List<TxRestriction> txr=repository.txRestrictions(t.accountId(),t.type(),t.channel(),dc);boolean txrOk=true;for(TxRestriction r:txr){if("BLOCK".equals(upper(r.mode()))||("LIMIT".equals(upper(r.mode()))&&r.maxAmount()!=null&&t.amount().compareTo(r.maxAmount())>0))txrOk=false;}out.add(new Check("TRANSACTION_RESTRICTION","LIMIT",txrOk,false,txrOk?"محدودیت نوع تراکنش/کانال مانع نیست.":"محدودیت نوع تراکنش یا کانال مانع عملیات است.","DEPOSIT_ACCOUNT_TRANSACTION_RESTRICTION"));
        boolean limitsOk=true;List<String> limitMessages=new ArrayList<>();for(AccountLimit l:repository.limits(t.accountId(),t.type(),t.channel())){String lt=upper(l.type());if("TRANSACTION_AMOUNT".equals(lt)&&l.amountLimit()!=null&&t.amount().compareTo(l.amountLimit())>0){limitsOk=false;limitMessages.add("سقف مبلغ هر تراکنش");continue;}LocalDate from=null,to=null;if(lt.startsWith("DAILY_")){from=t.bookingDate();to=t.bookingDate();}else if(lt.startsWith("MONTHLY_")){from=t.bookingDate().withDayOfMonth(1);to=t.bookingDate().with(TemporalAdjusters.lastDayOfMonth());}if(from!=null){if(lt.endsWith("AMOUNT")&&l.amountLimit()!=null&&repository.postedAmount(t.accountId(),l.transactionType(),l.channel(),from,to).add(t.amount()).compareTo(l.amountLimit())>0){limitsOk=false;limitMessages.add(lt);}if(lt.endsWith("COUNT")&&l.countLimit()!=null&&repository.postedCount(t.accountId(),l.transactionType(),l.channel(),from,to)+1>l.countLimit()){limitsOk=false;limitMessages.add(lt);}}}out.add(new Check("ACCOUNT_LIMIT","LIMIT",limitsOk,false,limitsOk?"سقف‌های حساب اجازه تراکنش می‌دهند.":"سقف حساب تکمیل شده است: "+String.join(",",limitMessages),"DEPOSIT_ACCOUNT_LIMIT"));
        boolean regOk=true;for(RegulatoryRestriction r:repository.regulatoryRestrictions(t.accountId(),t.type(),t.channel())){String rt=upper(r.type());if("BLOCK_ALL".equals(rt)||("DEBIT_BLOCK".equals(rt)&&"DEBIT".equals(dc))||("CREDIT_BLOCK".equals(rt)&&"CREDIT".equals(dc))||"TRANSACTION_BLOCK".equals(rt)||"CHANNEL_BLOCK".equals(rt)||("AMOUNT_LIMIT".equals(rt)&&r.limitAmount()!=null&&t.amount().compareTo(r.limitAmount())>0))regOk=false;}out.add(new Check("REGULATORY_RESTRICTION","COMPLIANCE",regOk,false,regOk?"محدودیت نظارتی مانع نیست.":"محدودیت نظارتی مانع تراکنش است.","DEPOSIT_ACCOUNT_REGULATORY_RESTRICTION"));return out;
    }

    private TransactionView view(long id){TransactionRecord t=repository.transaction(id).orElseThrow(()->new IllegalArgumentException("تراکنش یافت نشد."));return new TransactionView(t,repository.validations(id),repository.authorizations(id),repository.legs(id),repository.transfer(id),repository.cash(id),repository.reversalByOriginal(id));}
    private AccountRow requireAccount(long id,boolean lock){return repository.account(id,lock).orElseThrow(()->new DepositAccountNotFoundException("حساب سپرده با شناسه "+id+" یافت نشد."));}
    private TxLock lock(long accountId,long txId){return repository.lock(accountId,txId).orElseThrow(()->new IllegalArgumentException("تراکنش برای حساب یافت نشد."));}
    private DepositAccountLifecycleException concurrent(TxLock t){return lifecycle("تراکنش همزمان تغییر کرده است.","DEPOSIT_TRANSACTION.RECORD_VERSION",String.valueOf(t.recordVersion()));}
    private boolean replayOrClaim(String key,long accountId,String op,String hash,String actor,String correlation){String k=required(key,"X-Idempotency-Key");if(k.length()>80)throw new IllegalArgumentException("X-Idempotency-Key حداکثر 80 کاراکتر است.");var row=repository.idempotency(k).orElse(null);if(row!=null){if(!Objects.equals(row.accountId(),accountId)||!op.equals(row.operationType())||!hash.equals(row.payloadHash()))throw lifecycle("Idempotency key با payload دیگری استفاده شده است.","DEPOSIT_OPERATION_IDEMPOTENCY.IDEMPOTENCY_KEY",k);if("COMPLETED".equals(row.status()))return true;throw lifecycle("درخواست با همین Idempotency key در حال پردازش است.","DEPOSIT_OPERATION_IDEMPOTENCY.IDEMPOTENCY_KEY",k);}repository.claimIdempotency(repository.next("SEQ_DEPOSIT_OPERATION_IDEMPOTENCY"),k,accountId,op,hash,actor,correlation);return false;}
    private long parseResult(String key,String prefix){String ref=repository.idempotency(key).map(IdemRow::resultReference).orElseThrow();if(ref==null||!ref.startsWith(prefix))throw new IllegalStateException("Idempotency result نامعتبر است: "+ref);return Long.parseLong(ref.substring(prefix.length()));}
    private static DepositAccountLifecycleException lifecycle(String m,String f,String v){return new DepositAccountLifecycleException(m,Map.of(f,v==null?"":v));}
    private static String opposite(String dc){return "DEBIT".equals(upper(dc))?"CREDIT":"DEBIT";}
    private static String required(String v,String field){String x=trim(v);if(x==null)throw new IllegalArgumentException(field+" الزامی است.");return x;}
    private static String trim(String v){return v==null||v.isBlank()?null:v.trim();}
    private static String upper(String v){String x=trim(v);return x==null?null:x.toUpperCase(Locale.ROOT);}
    private static String hash(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private record Check(String code,String type,boolean pass,boolean warn,String message,String rule){}
}
