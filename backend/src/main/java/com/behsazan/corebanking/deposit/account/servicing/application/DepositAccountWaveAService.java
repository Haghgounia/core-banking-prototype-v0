package com.behsazan.corebanking.deposit.account.servicing.application;

import com.behsazan.corebanking.deposit.account.application.DepositAccountLifecycleService;
import com.behsazan.corebanking.deposit.account.error.DepositAccountLifecycleException;
import com.behsazan.corebanking.deposit.account.error.DepositAccountNotFoundException;
import com.behsazan.corebanking.deposit.account.operations.application.DepositAccountOperationsService;
import com.behsazan.corebanking.deposit.account.servicing.domain.DepositAccountServicingModels.CreateHoldRequest;
import com.behsazan.corebanking.deposit.account.servicing.domain.DepositAccountServicingModels.LifecycleActionRequest;
import com.behsazan.corebanking.deposit.account.servicing.domain.DepositAccountServicingModels.ReleaseHoldRequest;
import com.behsazan.corebanking.deposit.account.servicing.domain.DepositAccountWaveAModels.*;
import com.behsazan.corebanking.deposit.account.servicing.oracle.DepositAccountServicingRepository;
import com.behsazan.corebanking.deposit.account.servicing.oracle.DepositAccountWaveARepository;
import com.behsazan.corebanking.deposit.account.servicing.oracle.DepositAccountWaveARepository.*;
import com.behsazan.corebanking.deposit.opening.operational.application.DepositOpeningOperationalService;
import com.behsazan.corebanking.deposit.opening.operational.domain.DepositOpeningOperationalModels.ReadinessCheckView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Service
public class DepositAccountWaveAService {
    private static final Set<String> VALUE_TYPES=Set.of("STRING","NUMBER","DATE","BOOLEAN");
    private static final Set<String> SIGNATORY_ROLES=Set.of("OWNER_SIGNATORY","AUTHORIZED_SIGNATORY","REPRESENTATIVE","GUARDIAN");
    private static final Set<String> BULK_ACTIONS=Set.of("HOLD","RELEASE_HOLD","SUSPEND","REACTIVATE","MARK_DORMANT");
    private static final Set<String> ACTIVATION_TRIGGERS=Set.of("POST_CREATE","MANUAL_RECHECK","SYSTEM_RETRY","PRE_ACTIVATE");
    private static final Set<String> ACTIVATION_TYPES=Set.of("REGULATORY","COMPLIANCE","FINANCIAL","OPERATIONAL","CONTRACTUAL","PRODUCT","NOTIFICATION","SERVICE");
    private static final Set<String> ACTIVATION_SCOPES=Set.of("ACCOUNT_ACTIVATION","DEBIT_CAPABILITY","SERVICE_ONLY","NON_BLOCKING");
    private static final Set<String> ACTIVATION_RESULTS=Set.of("PASS","FAIL","PENDING","WAIVED","NOT_APPLICABLE");

    private final DepositAccountWaveARepository repository;
    private final DepositAccountServicingRepository servicingRepository;
    private final DepositAccountOperationsService operations;
    private final DepositAccountServicingService servicing;
    private final DepositOpeningOperationalService openingOperational;
    private final DepositAccountLifecycleService lifecycleService;
    private final TransactionTemplate required;
    private final TransactionTemplate requiresNew;

    public DepositAccountWaveAService(
            DepositAccountWaveARepository repository,
            DepositAccountServicingRepository servicingRepository,
            DepositAccountOperationsService operations,
            DepositAccountServicingService servicing,
            DepositOpeningOperationalService openingOperational,
            DepositAccountLifecycleService lifecycleService,
            PlatformTransactionManager transactionManager
    ) {
        this.repository=repository;this.servicingRepository=servicingRepository;this.operations=operations;this.servicing=servicing;
        this.openingOperational=openingOperational;this.lifecycleService=lifecycleService;
        this.required=new TransactionTemplate(transactionManager);
        this.requiresNew=new TransactionTemplate(transactionManager);this.requiresNew.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
    }

    @Transactional(readOnly=true)
    public ServicingControlsView get(long accountId){requireAccount(accountId,false);return view(accountId);}

    @Transactional
    public WaveAActionResponse upsertAttribute(long accountId,AttributeRequest request,String actor,String correlation,String idempotencyKey){
        if(request==null)throw new IllegalArgumentException("اطلاعات Attribute الزامی است.");
        String code=upper(required(request.attributeCode(),"ATTRIBUTE_CODE"));String type=upper(required(request.valueTypeCode(),"VALUE_TYPE_CODE"));if(!VALUE_TYPES.contains(type))throw new IllegalArgumentException("VALUE_TYPE_CODE نامعتبر است: "+type);
        String value=trim(request.attributeValue());LocalDate from=request.validFrom()==null?LocalDate.now(ZoneOffset.UTC):request.validFrom();
        String payload=String.join("|",String.valueOf(accountId),code,Objects.toString(value,""),type,from.toString());
        if(replayOrClaim(idempotencyKey,accountId,"ACCOUNT_ATTRIBUTE_UPSERT",hash(payload),actor,correlation)){long id=parseResult(idempotencyKey,"ATTR:");return new WaveAActionResponse(view(accountId),id,true);}
        requireAccount(accountId,true);long id;
        var current=repository.activeAttribute(accountId,code).orElse(null);
        if(current==null){id=repository.next("SEQ_DEPOSIT_ACCOUNT_ATTRIBUTE");repository.insertAttribute(id,accountId,code,value,type,from,actor);}else{id=current.id();if(repository.updateAttribute(id,current.recordVersion(),value,type,actor)!=1)throw conflict("DEPOSIT_ACCOUNT_ATTRIBUTE.RECORD_VERSION");}
        repository.completeIdempotency(idempotencyKey,"ATTR:"+id);return new WaveAActionResponse(view(accountId),id,false);
    }

    @Transactional
    public WaveAActionResponse requestConditionOverride(long accountId,ConditionOverrideRequest request,String actor,String correlation,String idempotencyKey){
        if(request==null)throw new IllegalArgumentException("اطلاعات Condition Override الزامی است.");
        String code=upper(required(request.conditionCode(),"CONDITION_CODE"));String override=required(request.overrideValue(),"OVERRIDE_VALUE");String type=upper(required(request.valueTypeCode(),"VALUE_TYPE_CODE"));if(!VALUE_TYPES.contains(type))throw new IllegalArgumentException("VALUE_TYPE_CODE نامعتبر است: "+type);
        String approver=required(request.approverUserId(),"APPROVER_USER_ID");if(actor.equalsIgnoreCase(approver))throw new IllegalArgumentException("Maker و Approver باید متفاوت باشند.");String org=required(request.orgUnitCode(),"ORG_UNIT_CODE");String reason=upper(required(request.reasonCode(),"REASON_CODE"));LocalDate from=request.validFrom()==null?LocalDate.now(ZoneOffset.UTC):request.validFrom();if(request.validTo()!=null&&request.validTo().isBefore(from))throw new IllegalArgumentException("VALID_TO نمی‌تواند قبل از VALID_FROM باشد.");
        String payload=String.join("|",String.valueOf(accountId),code,Objects.toString(request.baseValue(),""),override,type,from.toString(),Objects.toString(request.validTo(),""),reason,approver,org);
        if(replayOrClaim(idempotencyKey,accountId,"ACCOUNT_CONDITION_OVERRIDE",hash(payload),actor,correlation)){long id=parseResult(idempotencyKey,"COND:");return new WaveAActionResponse(view(accountId),id,true);}
        requireAccount(accountId,true);long conditionId=repository.next("SEQ_DEPOSIT_ACCOUNT_CONDITION_OVERRIDE"),approvalId=repository.next("SEQ_DEPOSIT_OPERATION_APPROVAL_REQUEST");
        repository.insertApproval(approvalId,accountId,"ACCOUNT_CONDITION_OVERRIDE","DEPOSIT_ACCOUNT_CONDITION_OVERRIDE",conditionId,actor,approver,org,idempotencyKey+"-APR",correlation,actor);
        repository.insertCondition(conditionId,accountId,code,trim(request.baseValue()),override,type,from,request.validTo(),reason,approvalId,actor);
        servicingRepository.history(accountId,"CONDITION",request.baseValue(),override,reason,"DEPOSIT_ACCOUNT_CONDITION_OVERRIDE",conditionId,actor);
        repository.completeIdempotency(idempotencyKey,"COND:"+conditionId);return new WaveAActionResponse(view(accountId),conditionId,false);
    }

    @Transactional
    public WaveAActionResponse approveConditionOverride(long accountId,long conditionId,String actor,String correlation,String idempotencyKey){
        String payload="APPROVE_CONDITION|"+accountId+"|"+conditionId+"|"+actor;if(replayOrClaim(idempotencyKey,accountId,"ACCOUNT_CONDITION_APPROVE",hash(payload),actor,correlation))return new WaveAActionResponse(view(accountId),conditionId,true);
        requireAccount(accountId,true);ConditionLock c=repository.condition(accountId,conditionId,true).orElseThrow(()->new IllegalArgumentException("Condition Override یافت نشد."));if(c.approvalRequestId()==null)throw new IllegalArgumentException("Condition Override فاقد Approval trace است.");ApprovalLock a=repository.approval(c.approvalRequestId(),true).orElseThrow();if(!actor.equalsIgnoreCase(a.approver()))throw new IllegalArgumentException("فقط Approver تعیین‌شده مجاز به تأیید است.");if("APPROVED".equals(upper(a.status()))){repository.completeIdempotency(idempotencyKey,"COND:"+conditionId);return new WaveAActionResponse(view(accountId),conditionId,false);}if(repository.approve(a.approvalId(),a.recordVersion(),actor)!=1)throw conflict("DEPOSIT_OPERATION_APPROVAL_REQUEST.RECORD_VERSION");repository.completeIdempotency(idempotencyKey,"COND:"+conditionId);return new WaveAActionResponse(view(accountId),conditionId,false);
    }

    @Transactional
    public WaveAActionResponse requestProductChange(long accountId,ProductChangeRequest request,String actor,String correlation,String idempotencyKey){
        if(request==null||request.toProductVersionId()<=0)throw new IllegalArgumentException("TO_PRODUCT_VERSION_ID باید مثبت باشد.");String approver=required(request.approverUserId(),"APPROVER_USER_ID");if(actor.equalsIgnoreCase(approver))throw new IllegalArgumentException("Maker و Approver باید متفاوت باشند.");String org=required(request.orgUnitCode(),"ORG_UNIT_CODE");String reason=upper(required(request.changeReasonCode(),"CHANGE_REASON_CODE"));LocalDate effective=request.effectiveDate()==null?LocalDate.now(ZoneOffset.UTC):request.effectiveDate();AccountContext account=requireAccount(accountId,true);if(account.currentProductVersionId()==request.toProductVersionId())throw new IllegalArgumentException("نسخه مقصد با نسخه جاری یکسان است.");if(!repository.sameProductVersion(account.currentProductVersionId(),request.toProductVersionId()))throw new IllegalArgumentException("نسخه مقصد باید متعلق به همان Product حساب باشد.");
        String payload=String.join("|",String.valueOf(accountId),String.valueOf(account.currentProductVersionId()),String.valueOf(request.toProductVersionId()),effective.toString(),reason,approver,org);if(replayOrClaim(idempotencyKey,accountId,"ACCOUNT_PRODUCT_CHANGE",hash(payload),actor,correlation)){long id=parseResult(idempotencyKey,"PROD:");return new WaveAActionResponse(view(accountId),id,true);}
        long historyId=repository.next("SEQ_DEPOSIT_ACCOUNT_PRODUCT_HISTORY"),approvalId=repository.next("SEQ_DEPOSIT_OPERATION_APPROVAL_REQUEST");repository.insertApproval(approvalId,accountId,"ACCOUNT_PRODUCT_CHANGE","DEPOSIT_ACCOUNT_PRODUCT_HISTORY",historyId,actor,approver,org,idempotencyKey+"-APR",correlation,actor);repository.insertProductHistory(historyId,accountId,account.currentProductVersionId(),request.toProductVersionId(),effective,reason,approvalId,actor);repository.completeIdempotency(idempotencyKey,"PROD:"+historyId);return new WaveAActionResponse(view(accountId),historyId,false);
    }

    @Transactional
    public WaveAActionResponse approveAndExecuteProductChange(long accountId,long historyId,String actor,String correlation,String idempotencyKey){
        String payload="APPROVE_PRODUCT|"+accountId+"|"+historyId+"|"+actor;if(replayOrClaim(idempotencyKey,accountId,"ACCOUNT_PRODUCT_CHANGE_APPROVE",hash(payload),actor,correlation))return new WaveAActionResponse(view(accountId),historyId,true);
        AccountContext account=requireAccount(accountId,true);ProductChangeLock p=repository.productChange(accountId,historyId,true).orElseThrow(()->new IllegalArgumentException("Product Change یافت نشد."));if(p.approvalRequestId()==null)throw new IllegalArgumentException("Product Change فاقد Approval trace است.");ApprovalLock approval=repository.approval(p.approvalRequestId(),true).orElseThrow();if(!actor.equalsIgnoreCase(approval.approver()))throw new IllegalArgumentException("فقط Approver تعیین‌شده مجاز به تأیید است.");if(!"APPROVED".equals(upper(approval.status()))&&repository.approve(approval.approvalId(),approval.recordVersion(),actor)!=1)throw conflict("DEPOSIT_OPERATION_APPROVAL_REQUEST.RECORD_VERSION");
        long historyVersion=p.recordVersion();if("PLANNED".equals(upper(p.migrationStatus()))){if(repository.markProductHistoryApproved(historyId,historyVersion,actor)!=1)throw conflict("DEPOSIT_ACCOUNT_PRODUCT_HISTORY.RECORD_VERSION");historyVersion++;}
        if(!"EXECUTED".equals(upper(p.migrationStatus()))){if(account.currentProductVersionId()!=p.fromPv())throw new DepositAccountLifecycleException("نسخه جاری حساب با مبنای Product Change یکسان نیست.",Map.of("DEPOSIT_ACCOUNT.CURRENT_PRODUCT_VERSION_ID",String.valueOf(account.currentProductVersionId())));if(repository.executeProductChange(accountId,account.recordVersion(),historyId,historyVersion,p.fromPv(),p.toPv(),actor)!=1)throw conflict("DEPOSIT_ACCOUNT.RECORD_VERSION");servicingRepository.history(accountId,"PRODUCT_VERSION",String.valueOf(p.fromPv()),String.valueOf(p.toPv()),"PRODUCT_REPLACEMENT","DEPOSIT_ACCOUNT_PRODUCT_HISTORY",historyId,actor);}
        repository.completeIdempotency(idempotencyKey,"PROD:"+historyId);return new WaveAActionResponse(view(accountId),historyId,false);
    }

    @Transactional
    public WaveAActionResponse addSignatoryAuthority(long accountId,SignatoryAuthorityRequest request,String actor,String correlation,String idempotencyKey){
        if(request==null||request.partyId()<=0)throw new IllegalArgumentException("PARTY_ID باید مثبت باشد.");if(!repository.activeParty(accountId,request.partyId()))throw new IllegalArgumentException("Party انتخاب‌شده عضو فعال حساب نیست.");String role=upper(request.signatoryRoleCode());if(role==null)role="AUTHORIZED_SIGNATORY";if(!SIGNATORY_ROLES.contains(role))throw new IllegalArgumentException("SIGNATORY_ROLE_CODE نامعتبر است: "+role);String operation=upper(required(request.operationCode(),"OPERATION_CODE"));String channel=upper(trim(request.channelCode()));BigDecimal max=request.maxAmount();if(max!=null&&max.signum()<0)throw new IllegalArgumentException("MAX_AMOUNT نمی‌تواند منفی باشد.");String currency=upper(trim(request.currencyCode()));if(max!=null&&currency==null)currency=requireAccount(accountId,false).currencyCode();LocalDate from=request.validFrom()==null?LocalDate.now(ZoneOffset.UTC):request.validFrom();boolean cosign=Boolean.TRUE.equals(request.requiresCosign());
        String payload=String.join("|",String.valueOf(accountId),String.valueOf(request.partyId()),role,operation,Objects.toString(channel,""),Objects.toString(max,""),Objects.toString(currency,""),String.valueOf(cosign),from.toString());if(replayOrClaim(idempotencyKey,accountId,"ACCOUNT_SIGNATORY_AUTHORITY",hash(payload),actor,correlation)){long id=parseResult(idempotencyKey,"AUTH:");return new WaveAActionResponse(view(accountId),id,true);}
        requireAccount(accountId,true);SignatoryLock sign=repository.activeSignatory(accountId,request.partyId()).orElse(null);long signatoryId;if(sign==null){signatoryId=repository.next("SEQ_DEPOSIT_ACCOUNT_SIGNATORY");repository.insertSignatory(signatoryId,accountId,request.partyId(),role,repository.nextSignatorySequence(accountId),false,from,actor);}else signatoryId=sign.signatoryId();long authorityId=repository.next("SEQ_DEPOSIT_ACCOUNT_SIGNATORY_AUTHORITY");repository.insertAuthority(authorityId,signatoryId,operation,channel,max,currency,cosign,from,actor);servicingRepository.history(accountId,"SIGNATORY",null,"partyId="+request.partyId()+",operation="+operation,"CUSTOMER_REQUEST","DEPOSIT_ACCOUNT_SIGNATORY_AUTHORITY",authorityId,actor);repository.completeIdempotency(idempotencyKey,"AUTH:"+authorityId);return new WaveAActionResponse(view(accountId),authorityId,false);
    }

    @Transactional
    public ActivationActionResponse runActivationChecks(long accountId,ActivationRunRequest request,String actor,String correlation,String idempotencyKey){
        AccountContext account=requireAccount(accountId,true);if(!"PENDING_ACTIVATION".equals(upper(account.status())))throw new DepositAccountLifecycleException("Activation Run فقط برای حساب PENDING_ACTIVATION مجاز است.",Map.of("DEPOSIT_ACCOUNT.ACCOUNT_STATUS_CODE",account.status()));if(account.openingRequestId()==null)throw new IllegalArgumentException("حساب فاقد OPENING_REQUEST_ID است.");String trigger=upper(request==null?null:request.triggerCode());if(trigger==null)trigger="MANUAL_RECHECK";if(!ACTIVATION_TRIGGERS.contains(trigger))throw new IllegalArgumentException("TRIGGER_CODE نامعتبر است: "+trigger);
        String payload="ACTIVATION_RUN|"+accountId+"|"+trigger;if(replayOrClaim(idempotencyKey,accountId,"ACCOUNT_ACTIVATION_RUN",hash(payload),actor,correlation)){long id=parseResult(idempotencyKey,"ACTRUN:");return new ActivationActionResponse(view(accountId),id,true);}
        var readiness=openingOperational.getReadiness(account.openingRequestId());long runId=repository.next("SEQ_DEPOSIT_ACTIVATION_RUN"),runNo=repository.nextActivationRunNo(accountId);String corr=trim(correlation);if(corr==null)corr=idempotencyKey;repository.insertActivationRun(runId,accountId,account.openingRequestId(),runNo,corr,trigger,actor);
        long mandatory=0,passed=0,blocking=0;for(ReadinessCheckView c:readiness.checks()){String type=activationType(c.checkTypeCode());String scope=activationScope(c.blockingScopeCode());String result=activationResult(c.resultStatusCode());if(c.required())mandatory++;if(Set.of("PASS","WAIVED","NOT_APPLICABLE").contains(result))passed++;if(c.required()&&"ACCOUNT_ACTIVATION".equals(scope)&&!Set.of("PASS","WAIVED","NOT_APPLICABLE").contains(result))blocking++;String sourceRef=trim(c.sourceEvaluationReference());if(sourceRef==null)sourceRef=trim(c.resultReference());repository.insertActivationCheck(repository.next("SEQ_DEPOSIT_ACTIVATION_CHECK"),runId,accountId,c.checkCode(),type,scope,c.required(),result,"DEPOSIT_OPENING_READINESS",sourceRef,null,activationDetails(c),c.checkedAt(),c.validUntil(),actor);}
        boolean ready="READY".equals(upper(readiness.activationStatusCode()))&&blocking==0;repository.completeActivationRun(runId,mandatory,passed,blocking,ready,actor);repository.completeIdempotency(idempotencyKey,"ACTRUN:"+runId);return new ActivationActionResponse(view(accountId),runId,false);
    }

    @Transactional
    public ActivationActionResponse executeActivation(long accountId,long activationRunId,String actor,String correlation,String idempotencyKey){
        String payload="ACTIVATE|"+accountId+"|"+activationRunId;if(replayOrClaim(idempotencyKey,accountId,"ACCOUNT_ACTIVATION_EXECUTE",hash(payload),actor,correlation))return new ActivationActionResponse(view(accountId),activationRunId,true);AccountContext account=requireAccount(accountId,true);ActivationRun latest=repository.latestActivationRun(accountId);if(latest==null||latest.activationRunId()!=activationRunId)throw new IllegalArgumentException("Activation Run باید آخرین Run حساب باشد.");if(!latest.ready()||!"READY".equals(upper(latest.runStatusCode())))throw new DepositAccountLifecycleException("Activation Run آماده فعال‌سازی نیست.",Map.of("DEPOSIT_ACTIVATION_RUN.RUN_STATUS_CODE",latest.runStatusCode()));if(account.openingRequestId()==null)throw new IllegalArgumentException("OPENING_REQUEST_ID موجود نیست.");lifecycleService.activateAccount(account.openingRequestId(),activationRunId,actor,correlation);repository.completeIdempotency(idempotencyKey,"ACTRUN:"+activationRunId);return new ActivationActionResponse(view(accountId),activationRunId,false);
    }

    public BulkActionResponse bulk(BulkActionRequest request,String actor,String correlation,String idempotencyKey){
        if(request==null)throw new IllegalArgumentException("اطلاعات Bulk Action الزامی است.");String action=upper(required(request.actionTypeCode(),"ACTION_TYPE_CODE"));if(!BULK_ACTIONS.contains(action))throw new IllegalArgumentException("ACTION_TYPE_CODE نامعتبر است: "+action);List<Long> ids=request.accountIds()==null?List.of():request.accountIds().stream().filter(Objects::nonNull).filter(x->x>0).distinct().toList();if(ids.isEmpty())throw new IllegalArgumentException("حداقل یک ACCOUNT_ID الزامی است.");String reason=upper(trim(request.reasonCode()));String payload=action+"|"+ids+"|"+Objects.toString(reason,"")+"|"+Objects.toString(request.holdTypeCode(),"")+"|"+Objects.toString(request.holdAmount(),"")+"|"+Objects.toString(request.releaseHoldIds(),"");
        final long[] bulkId={-1};final boolean[] replay={false};required.executeWithoutResult(status->{var idem=repository.idempotency(required(idempotencyKey,"X-Idempotency-Key")).orElse(null);String h=hash(payload);if(idem!=null){if(!"ACCOUNT_BULK_ACTION".equals(idem.operationType())||!h.equals(idem.payloadHash()))throw new IllegalArgumentException("Idempotency-Key قبلاً برای Bulk دیگری استفاده شده است.");if("COMPLETED".equals(idem.status())){bulkId[0]=parseRef(idem.resultReference(),"BULK:");replay[0]=true;return;}throw new IllegalArgumentException("Bulk Action با این Idempotency-Key در حال پردازش است.");}repository.claimIdempotency(repository.next("SEQ_DEPOSIT_OPERATION_IDEMPOTENCY"),idempotencyKey,null,"ACCOUNT_BULK_ACTION",h,actor,correlation);bulkId[0]=repository.next("SEQ_DEPOSIT_ACCOUNT_BULK_ACTION");repository.insertBulk(bulkId[0],"BULK-11J-"+bulkId[0],action,reason,ids.size(),actor,correlation);for(long aid:ids)repository.insertBulkItem(repository.next("SEQ_DEPOSIT_ACCOUNT_BULK_ACTION_ITEM"),bulkId[0],aid,actor);});
        if(replay[0])return new BulkActionResponse(repository.bulk(bulkId[0]),true);
        List<BulkActionItem> items=repository.bulkItems(bulkId[0]);long success=0,failed=0;for(BulkActionItem item:items){try{processBulkItem(bulkId[0],item,request,action,reason,actor,correlation);success++;}catch(Exception e){failed++;String msg=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();long itemId=item.bulkActionItemId();required.executeWithoutResult(s->repository.updateBulkItem(itemId,"FAILED","ITEM_FAILED",msg.length()>900?msg.substring(0,900):msg,actor));}}
        final long ok=success,bad=failed;required.executeWithoutResult(status->{repository.completeBulk(bulkId[0],ok,bad,actor);repository.completeIdempotency(idempotencyKey,"BULK:"+bulkId[0]);});return new BulkActionResponse(repository.bulk(bulkId[0]),false);
    }

    private void processBulkItem(long bulkId,BulkActionItem item,BulkActionRequest request,String action,String reason,String actor,String correlation){requiresNew.executeWithoutResult(status->{long aid=item.accountId();var d=operations.get(aid);String key="11J-BULK-"+bulkId+"-"+item.bulkActionItemId();switch(action){case "SUSPEND"->servicing.suspend(aid,new LifecycleActionRequest(d.account().recordVersion(),reason==null?"BULK_CONTROLLED":reason),actor,correlation,key);case "REACTIVATE"->servicing.reactivate(aid,new LifecycleActionRequest(d.account().recordVersion(),reason==null?"BULK_CONTROLLED":reason),actor,correlation,key);case "MARK_DORMANT"->servicing.markDormant(aid,new LifecycleActionRequest(d.account().recordVersion(),reason==null?"BULK_CONTROLLED":reason),actor,correlation,key);case "HOLD"->{String holdType=upper(trim(request.holdTypeCode()));if(holdType==null)holdType="DEBIT_ONLY";String holdReason=upper(trim(request.holdReasonCode()));if(holdReason==null)holdReason="COMPLIANCE_RESTRICTION";servicing.createHold(aid,new CreateHoldRequest(holdType,"PARTIAL".equals(holdType)?request.holdAmount():null,"PARTIAL".equals(holdType)?(request.currencyCode()==null?d.account().currencyCode():request.currencyCode()):null,holdReason,"BULK-11J-"+bulkId,null,"CORE_BANKING","BULK_ACTION",key,"SYSTEM_BATCH","ORIGIN_OR_AUTHORITY"),actor,correlation,key);}case "RELEASE_HOLD"->{Long holdId=request.releaseHoldIds()==null?null:request.releaseHoldIds().get(aid);if(holdId==null)throw new IllegalArgumentException("برای RELEASE_HOLD باید holdIdsByAccount برای هر حساب تعیین شود.");servicing.releaseHold(aid,holdId,new ReleaseHoldRequest(null,"BULK_RELEASE","CORE_BANKING","BULK_ACTION",key,"SYSTEM_BATCH"),actor,correlation,key);}default->throw new IllegalArgumentException("Bulk Action پشتیبانی نمی‌شود.");}repository.updateBulkItem(item.bulkActionItemId(),"SUCCESS",null,null,actor);});}

    private ServicingControlsView view(long accountId){return new ServicingControlsView(operations.get(accountId),repository.attributes(accountId),repository.conditionOverrides(accountId),repository.signatoryAuthorities(accountId),repository.productCandidates(accountId),repository.productChanges(accountId),repository.latestActivationRun(accountId),repository.recentBulks(accountId));}
    private AccountContext requireAccount(long id,boolean lock){if(id<=0)throw new IllegalArgumentException("accountId باید مثبت باشد.");return repository.account(id,lock).orElseThrow(()->new DepositAccountNotFoundException("حساب سپرده با شناسه "+id+" یافت نشد."));}
    private boolean replayOrClaim(String key,long accountId,String operation,String payloadHash,String actor,String correlation){String k=required(key,"X-Idempotency-Key");var row=repository.idempotency(k).orElse(null);if(row!=null){if(!Objects.equals(row.accountId(),accountId)||!operation.equals(row.operationType())||!payloadHash.equals(row.payloadHash()))throw new IllegalArgumentException("Idempotency-Key قبلاً برای درخواست دیگری استفاده شده است.");if("COMPLETED".equals(row.status()))return true;throw new IllegalArgumentException("درخواست با این Idempotency-Key در حال پردازش است.");}repository.claimIdempotency(repository.next("SEQ_DEPOSIT_OPERATION_IDEMPOTENCY"),k,accountId,operation,payloadHash,actor,correlation);return false;}
    private long parseResult(String key,String prefix){return repository.idempotency(key).map(IdemRow::resultReference).map(x->parseRef(x,prefix)).orElseThrow();}
    private static long parseRef(String value,String prefix){if(value==null||!value.startsWith(prefix))throw new IllegalStateException("Idempotency result نامعتبر است: "+value);return Long.parseLong(value.substring(prefix.length()));}
    private static String activationType(String value){String x=upper(value);if(x==null)return "OPERATIONAL";if(ACTIVATION_TYPES.contains(x))return x;if(x.contains("COMPLIANCE")||x.contains("KYC"))return "COMPLIANCE";if(x.contains("PRODUCT"))return "PRODUCT";if(x.contains("FINANC")||x.contains("FUND"))return "FINANCIAL";if(x.contains("CONSENT")||x.contains("CONTRACT"))return "CONTRACTUAL";return "OPERATIONAL";}
    private static String activationScope(String value){String x=upper(value);return ACTIVATION_SCOPES.contains(x)?x:"NON_BLOCKING";}
    private static String activationResult(String value){String x=upper(value);return ACTIVATION_RESULTS.contains(x)?x:"PENDING";}
    private static String activationDetails(ReadinessCheckView c){String title=trim(c.titleFa());String waiver=trim(c.waiverReason());String value=waiver==null?title:(title==null?waiver:title+" | "+waiver);if(value==null)return null;return value.length()<=500?value:value.substring(0,500);}
    private static DepositAccountLifecycleException conflict(String field){return new DepositAccountLifecycleException("رکورد همزمان تغییر کرده است؛ اطلاعات را تازه‌سازی کنید.",Map.of(field,"Optimistic lock conflict"));}
    private static String required(String v,String field){String x=trim(v);if(x==null)throw new IllegalArgumentException(field+" الزامی است.");return x;}
    private static String trim(String v){return v==null||v.isBlank()?null:v.trim();}
    private static String upper(String v){String x=trim(v);return x==null?null:x.toUpperCase(Locale.ROOT);}
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
