package com.behsazan.corebanking.fee3.r3;

import com.behsazan.corebanking.fee3.application.Fee3DraftPolicy;
import com.behsazan.corebanking.fee3.application.Fee3Problem;
import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.oracle.Fee3Repository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

/** R3: validation, safe preview, identity-gated UAT Maker/Checker. Never activates a tariff. */
@Service
public class Fee3R3Service {
    public record Actor(String name,boolean authenticated,boolean maker,boolean checker) {}
    public record WorkflowReadiness(boolean workflowEnabled,boolean checkerDecisionEnabled,
                                    boolean approvalEnabled,boolean authenticated,
                                    boolean makerRole,boolean checkerRole,boolean postingEnabled) {}
    public record SubmitRequest(long itemId,long tariffVersionId,long expectedVersionRecordVersion,String reason) {}
    public record DecisionRequest(String decision,long expectedRequestRecordVersion,String reason) {}
    private final Fee3Repository repo;
    private final Fee3R3WorkflowRepository workflow;
    private final boolean readsEnabled, workflowEnabled,decisionEnabled,approvalEnabled;
    public Fee3R3Service(Fee3Repository repo,Fee3R3WorkflowRepository workflow,
            @Value("${FEE3_READS_ENABLED:true}") boolean readsEnabled,
            @Value("${FEE3_R3_WORKFLOW_ENABLED:false}") boolean workflowEnabled,
            @Value("${FEE3_R3_CHECKER_DECISIONS_ENABLED:false}") boolean decisionEnabled,
            @Value("${FEE3_R3_UAT_APPROVAL_ENABLED:false}") boolean approvalEnabled){
        this.repo=repo;this.workflow=workflow;this.readsEnabled=readsEnabled;
        this.workflowEnabled=workflowEnabled;this.decisionEnabled=decisionEnabled;
        this.approvalEnabled=approvalEnabled;
    }
    private static Fee3Problem error(HttpStatus status,String code,String message){
        return new Fee3Problem(status,code,message);
    }
    private void canRead(){
        if(!readsEnabled||!repo.missingTables().isEmpty())
            throw error(HttpStatus.SERVICE_UNAVAILABLE,"FEE3_R3_NOT_READY","FEE3 Oracle read contract is not available");
    }
    private void actor(Actor identity,boolean maker){
        if(!workflowEnabled)throw error(HttpStatus.FORBIDDEN,"FEE3_WORKFLOW_DISABLED","Workflow is disabled");
        if(identity==null||!identity.authenticated()||identity.name()==null||
           !identity.name().matches("[A-Za-z0-9_.@-]{3,100}"))
            throw error(HttpStatus.UNAUTHORIZED,"FEE3_AUTH_REQUIRED","Authenticated organizational identity required");
        if(maker&&!identity.maker()||!maker&&!identity.checker())
            throw error(HttpStatus.FORBIDDEN,"FEE3_ROLE_REQUIRED","Authenticated FEE3 role required");
    }
    @Transactional(readOnly=true)
    public WorkflowReadiness workflowReadiness(Actor actor){
        return new WorkflowReadiness(workflowEnabled,decisionEnabled,approvalEnabled,
           actor!=null&&actor.authenticated(),actor!=null&&actor.maker(),
           actor!=null&&actor.checker(),false);
    }
    @Transactional(readOnly=true)
    public Fee3SubmissionPolicy.Report preflight(DraftRequest draft){
        canRead();
        Fee3SubmissionPolicy.Report report=Fee3SubmissionPolicy.check(draft);
        if(!report.submissionReady())return report;
        try{verify(draft);return report;}
        catch(Fee3Problem ex){
            return new Fee3SubmissionPolicy.Report(false,false,
                List.of(new Fee3SubmissionPolicy.Issue(ex.errorCode(),"draft",ex.getMessage())),
                "SERVER_VALIDATION_REQUIRED",false);
        }
    }
    @Transactional(readOnly=true)
    public Fee3PreviewEngine.PreviewResult preview(Fee3PreviewEngine.PreviewRequest request){
        canRead();
        if(request==null||request.draft()==null)
            throw error(HttpStatus.UNPROCESSABLE_ENTITY,"FEE3_PREVIEW_DRAFT_MISSING","Draft required");
        try {verify(request.draft());}
        catch(Fee3Problem ex){
            return new Fee3PreviewEngine.PreviewResult("BLOCKED",false,false,
                request.draft().version()==null?"UNKNOWN":request.draft().version().method(),
                null,null,null,null,List.of(),List.of(ex.errorCode()+": "+ex.getMessage()));
        }
        return Fee3PreviewEngine.calculate(request);
    }
    private void verify(DraftRequest draft){
        if(draft==null||draft.item()==null||draft.version()==null)
            throw error(HttpStatus.UNPROCESSABLE_ENTITY,"FEE3_INVALID_DRAFT","Missing required draft sections");
        Operation op=repo.activeOperation(draft.item().module(),draft.item().operation())
           .orElseThrow(()->error(HttpStatus.UNPROCESSABLE_ENTITY,"FEE3_UNKNOWN_OPERATION","Unknown operation in live Oracle catalog"));
        Fee3DraftPolicy.check(draft,repo.bankDate(),op.allowedAmountKinds());
        if(draft.version().sourceId()!=null){
            String source=repo.sourceStatus(draft.version().sourceId());
            if(source==null)throw error(HttpStatus.UNPROCESSABLE_ENTITY,"FEE3_SOURCE_NOT_FOUND","Unknown source");
            if("CBI_MANDATORY".equals(draft.version().origin())&&!"VERIFIED".equals(source))
                throw error(HttpStatus.UNPROCESSABLE_ENTITY,"FEE3_CBI_SOURCE_UNVERIFIED","Regulatory source must be verified");
        }
        for(RuleDraft rule:draft.rules()==null?List.<RuleDraft>of():draft.rules()){
            if(rule.formulaCode()!=null&&!rule.formulaCode().isBlank()&&!repo.activeFormula(rule.formulaCode()))
                throw error(HttpStatus.UNPROCESSABLE_ENTITY,"FEE3_UNKNOWN_FORMULA","Unknown formula in live Oracle catalog");
            if(!draft.version().method().equals(rule.method()))
                throw error(HttpStatus.UNPROCESSABLE_ENTITY,"FEE3_RULE_METHOD_MISMATCH","Rule method differs from tariff method");
        }
    }
    @Transactional(readOnly=true)
    public List<Fee3R3WorkflowRepository.RequestInfo> requests(long itemId,Actor caller){
        canRead();actor(caller,caller!=null&&caller.maker());
        if(itemId<=0)throw error(HttpStatus.BAD_REQUEST,"FEE3_BAD_ID","Invalid item id");
        List<Fee3R3WorkflowRepository.RequestInfo> recent=workflow.recent(itemId);
        if(caller.checker())return recent.stream().filter(r->"SUBMITTED".equals(r.status())).toList();
        return recent.stream().filter(r->caller.name().equals(r.requestedBy())).toList();
    }
    @Transactional
    public Fee3R3WorkflowRepository.RequestInfo submit(SubmitRequest input,Actor maker){
        canRead();actor(maker,true);
        if(input==null||input.itemId()<=0||input.tariffVersionId()<=0||
            input.expectedVersionRecordVersion()<1||input.reason()==null||
            input.reason().strip().length()<5||input.reason().length()>500)
            throw error(HttpStatus.UNPROCESSABLE_ENTITY,"FEE3_BAD_SUBMISSION","Provide item, version, record version and a meaningful reason");
        Map<String,Object> item=repo.itemByIdForUpdate(input.itemId()).orElseThrow(()->error(
            HttpStatus.NOT_FOUND,"FEE3_ITEM_NOT_FOUND","Unknown item"));
        if(!String.valueOf(item.get("CALC_CODE")).matches("F3-UAT-[0-9]+"))
            throw error(HttpStatus.FORBIDDEN,"FEE3_SEED_PROTECTED","Only new UAT items can enter this workflow");
        Map<String,Object> version=repo.versionByIdForUpdate(input.tariffVersionId(),input.itemId())
            .orElseThrow(()->error(HttpStatus.NOT_FOUND,"FEE3_VERSION_NOT_FOUND","Unknown version"));
        if(!"DRAFT".equals(version.get("STATUS_CODE"))||
            ((Number)version.get("IS_ACTIVE")).intValue()!=0||
            ((Number)version.get("VERSION_NO")).intValue()!=1)
            throw error(HttpStatus.CONFLICT,"FEE3_INVALID_VERSION_STATE","Only initial inactive UAT DRAFT can be submitted");
        if(((Number)version.get("RECORD_VERSION")).longValue()!=input.expectedVersionRecordVersion())
            throw error(HttpStatus.CONFLICT,"FEE3_STALE_VERSION","Tariff changed after opening");
        if(workflow.openCount(input.itemId())>0)
            throw error(HttpStatus.CONFLICT,"FEE3_OPEN_REVIEW_EXISTS","There is already an open change request");
        if(workflow.scopeCount(input.tariffVersionId())!=1||workflow.ruleCount(input.tariffVersionId())<1||
           workflow.testCount(input.tariffVersionId())<1)
            throw error(HttpStatus.UNPROCESSABLE_ENTITY,"FEE3_REVIEW_EVIDENCE_MISSING",
                "One scope, at least one rule and one test case required");
        Object fromValue=version.get("VALID_FROM");
        java.time.LocalDate validFrom=fromValue instanceof java.sql.Date d?d.toLocalDate():
           fromValue instanceof java.sql.Timestamp ts?ts.toLocalDateTime().toLocalDate():
           fromValue instanceof java.time.LocalDate date?date:null;
        if(validFrom==null||validFrom.isBefore(repo.bankDate()))
            throw error(HttpStatus.UNPROCESSABLE_ENTITY,"FEE3_VALIDITY_EXPIRED","Start date is invalid or expired");
        String tax=(String)version.get("TAX_MODE_CODE"),round=(String)version.get("ROUNDING_CODE");
        if(tax==null||tax.isBlank()||"UNREVIEWED".equals(tax)||round==null||
           !List.of("HALF_UP","HALF_EVEN","UP","DOWN","CEIL","FLOOR").contains(round))
            throw error(HttpStatus.UNPROCESSABLE_ENTITY,"FEE3_POLICY_UNREVIEWED","Tax and rounding policies must be reviewed");
        String origin=(String)version.get("TARIFF_ORIGIN_CODE");
        if("CBI_MANDATORY".equals(origin)){
            Object source=version.get("SOURCE_ID");
            if(!(source instanceof Number sourceId)||!"VERIFIED".equals(repo.sourceStatus(sourceId.longValue())))
                throw error(HttpStatus.UNPROCESSABLE_ENTITY,"FEE3_CBI_SOURCE_UNVERIFIED","Verified regulatory evidence required");
        }
        return workflow.submit(input.itemId(),input.tariffVersionId(),input.expectedVersionRecordVersion(),
            maker.name(),input.reason().strip());
    }
    @Transactional
    public Fee3R3WorkflowRepository.RequestInfo decide(long requestId,DecisionRequest decision,Actor checker){
        canRead();actor(checker,false);
        if(!decisionEnabled)throw error(HttpStatus.FORBIDDEN,"FEE3_CHECKER_DISABLED","Checker decisions are disabled");
        if(requestId<=0||decision==null||decision.expectedRequestRecordVersion()<1||
            decision.reason()==null||decision.reason().strip().length()<5||decision.reason().length()>500||
            !("APPROVE".equals(decision.decision())||"REJECT".equals(decision.decision())))
            throw error(HttpStatus.UNPROCESSABLE_ENTITY,"FEE3_INVALID_DECISION","Invalid decision or reason");
        if("APPROVE".equals(decision.decision())&&!approvalEnabled)
            throw error(HttpStatus.FORBIDDEN,"FEE3_APPROVAL_DISABLED","UAT approval is separately disabled");
        // Snapshot lookup first; then take locks in invariant ITEM -> VERSION -> REQUEST order.
        Fee3R3WorkflowRepository.RequestInfo snapshot=workflow.lookup(requestId).orElseThrow(()->error(
            HttpStatus.NOT_FOUND,"FEE3_REQUEST_NOT_FOUND","Review request not found"));
        Map<String,Object> item=repo.itemByIdForUpdate(snapshot.calcItemId()).orElseThrow(()->error(
            HttpStatus.NOT_FOUND,"FEE3_ITEM_NOT_FOUND","Item not found"));
        if(!String.valueOf(item.get("CALC_CODE")).matches("F3-UAT-[0-9]+"))
            throw error(HttpStatus.FORBIDDEN,"FEE3_SEED_PROTECTED","Only UAT requests can be reviewed");
        Map<String,Object> version=repo.versionByIdForUpdate(snapshot.proposedVersionId(),snapshot.calcItemId())
            .orElseThrow(()->error(HttpStatus.NOT_FOUND,"FEE3_VERSION_NOT_FOUND","Version not found"));
        Fee3R3WorkflowRepository.RequestInfo locked=workflow.lock(requestId).orElseThrow(()->error(
            HttpStatus.CONFLICT,"FEE3_REQUEST_CHANGED","Review request changed"));
        if(locked.calcItemId()!=snapshot.calcItemId()||locked.proposedVersionId()!=snapshot.proposedVersionId()||
           locked.recordVersion()!=decision.expectedRequestRecordVersion())
            throw error(HttpStatus.CONFLICT,"FEE3_STALE_REVIEW","Review changed since it was loaded");
        if(!Fee3SubmissionPolicy.validTransition(locked.status(),String.valueOf(version.get("STATUS_CODE")),decision.decision()))
            throw error(HttpStatus.CONFLICT,"FEE3_REVIEW_STATE_MISMATCH","Request or tariff status is not reviewable");
        if(((Number)version.get("IS_ACTIVE")).intValue()!=0||
           !Fee3SubmissionPolicy.differentActors(locked.requestedBy(),checker.name()))
            throw error(HttpStatus.FORBIDDEN,"FEE3_MAKER_CHECKER_SEPARATION","Checker must be distinct from Maker");
        if("APPROVE".equals(decision.decision())&&!workflow.allTestsCertified(locked.proposedVersionId()))
            throw error(HttpStatus.CONFLICT,"FEE3_CERTIFIED_TESTS_REQUIRED","All tests must have verified PASS evidence");
        return workflow.decide(locked,checker.name(),decision.decision(),decision.reason().strip());
    }
}
