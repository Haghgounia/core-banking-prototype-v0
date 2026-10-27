package com.behsazan.corebanking.fee3.r3;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure Maker/Checker readiness checks: never infer approval from an offline result. */
public final class Fee3SubmissionPolicy {
    private Fee3SubmissionPolicy() {}
    public record Issue(String code,String field,String message) {}
    public record Report(boolean submissionReady,boolean approvalReady,List<Issue> issues,
                         String guard,boolean bankEffect) {}
    public static Report check(DraftRequest draft) {
        List<Issue> issues=new ArrayList<>();
        if(draft==null||draft.item()==null||draft.version()==null||draft.scope()==null){
            issues.add(new Issue("INCOMPLETE_DRAFT","draft","Identity, version, and scope are required"));
            return new Report(false,false,List.copyOf(issues),"DRAFT_UAT_ONLY",false);
        }
        VersionDraft v=draft.version();
        if(v.validFrom()==null)issues.add(new Issue("VALID_FROM_MISSING","version.validFrom","Start date is required"));
        if(v.validFrom()!=null&&v.validTo()!=null&&v.validTo().isBefore(v.validFrom()))
            issues.add(new Issue("INVALID_VALIDITY_RANGE","version.validTo","End precedes start"));
        if(!"IRR".equals(v.feeCurrency()))issues.add(new Issue("NON_IRR_NOT_READY","version.feeCurrency","IRR only in physical model"));
        if(v.method()==null||v.method().isBlank())issues.add(new Issue("METHOD_REQUIRED","version.method","Method is required"));
        if(v.rounding()==null||!Set.of("HALF_UP","HALF_EVEN","FLOOR","DOWN","UP","CEIL").contains(v.rounding()))
            issues.add(new Issue("ROUNDING_UNRESOLVED","version.rounding","Rounding policy needs review"));
        if(v.taxMode()==null||"UNREVIEWED".equals(v.taxMode())||v.taxMode().isBlank())
            issues.add(new Issue("TAX_UNREVIEWED","version.taxMode","Tax decision must be recorded"));
        if(v.uiContext()!=null&&Boolean.TRUE.equals(v.uiContext().get("loanFormulaContextReviewRequired")))
            issues.add(new Issue("LOAN_CONTRACT_REVIEW_REQUIRED","version.uiContext","Loan model contract match must be confirmed"));
        if(draft.scope().channels()==null||draft.scope().channels().isEmpty()||
           draft.scope().customers()==null||draft.scope().customers().isEmpty())
            issues.add(new Issue("SCOPE_INCOMPLETE","scope","Channels and customer types are required"));
        List<RuleDraft> rules=draft.rules()==null?List.of():draft.rules();
        if(rules.isEmpty())issues.add(new Issue("NO_CALCULATION_RULE","rules","At least one calculation rule is required"));
        if(rules.size()>100)issues.add(new Issue("RULES_EXCEED_LIMIT","rules","More than 100 rules"));
        Set<Integer> orders=new HashSet<>();
        for(RuleDraft r:rules) {
            if(r==null){issues.add(new Issue("NULL_RULE","rules","Null rule"));continue;}
            if(r.order()<1||!orders.add(r.order()))issues.add(new Issue("DUPLICATE_RULE_ORDER","rules","Rule order must be unique and positive"));
            if(v.method()!=null&&!v.method().equals(r.method()))issues.add(new Issue("METHOD_MISMATCH","rules","Rule differs from tariff method"));
            if(r.min()!=null&&r.max()!=null&&r.min().compareTo(r.max())>0)
                issues.add(new Issue("INVALID_BOUNDS","rules","Minimum exceeds maximum"));
            if("FIXED".equals(r.method())&&r.fixed()==null)
                issues.add(new Issue("FIXED_AMOUNT_REQUIRED","rules","Fixed amount is required"));
            if("PERCENT".equals(r.method())&&"RULE".equals(r.rateSource())&&r.rate()==null)
                issues.add(new Issue("RULE_RATE_REQUIRED","rules","Configured rate is missing"));
            if("FORMULA".equals(r.method())&&r.formulaCode()==null&&
               (r.parameters()==null||!r.parameters().containsKey("_fceEditableExpressionV1")))
                issues.add(new Issue("FORMULA_MISSING","rules","Formula expression or code required"));
        }
        if(draft.tests()==null||draft.tests().isEmpty())
            issues.add(new Issue("NO_TEST_CASE","tests","At least one evidence-backed test case is required before review"));
        else for(TestDraft test:draft.tests())if(test==null||test.request()==null||test.request().isEmpty())
            issues.add(new Issue("TEST_INPUT_MISSING","tests","Test request is required"));
        if(v.uiContext()!=null&&v.uiContext().get("guaranteePolicy") instanceof Map<?,?> policy &&
           "MIXED_PORTFOLIO".equals(policy.get("collateralType"))){
            if(!(policy.get("portfolioRules") instanceof List<?> list)||list.isEmpty())
                issues.add(new Issue("GUARANTEE_PORTFOLIO_EMPTY","version.uiContext","Portfolio tariff rules required"));
            else for(Object row:list)if(!(row instanceof Map<?,?> r)||r.get("coveragePercent")==null||
                    ("CONFIG".equals(r.get("rateSource"))&&r.get("ratePercent")==null))
                issues.add(new Issue("PORTFOLIO_TARIFF_INCOMPLETE","version.uiContext","Each collateral requires rate policy and coverage"));
        }
        // Separate evidence gate: a draft test definition and preview are not certified bank execution.
        return new Report(issues.isEmpty(),false,List.copyOf(issues),"MAKER_CHECKER_AND_BANK_CERTIFICATION_REQUIRED",false);
    }
    public static boolean differentActors(String maker,String checker){
        return maker!=null&&checker!=null&&!maker.isBlank()&&!checker.isBlank()&&
            !maker.trim().equalsIgnoreCase(checker.trim());
    }
    public static boolean validTransition(String requestStatus,String versionStatus,String decision){
        if(!"SUBMITTED".equals(requestStatus)||!"REVIEW".equals(versionStatus))return false;
        return "APPROVE".equals(decision)||"REJECT".equals(decision);
    }
}
