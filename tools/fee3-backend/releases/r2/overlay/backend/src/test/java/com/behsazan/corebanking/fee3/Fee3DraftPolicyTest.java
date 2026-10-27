package com.behsazan.corebanking.fee3;

import com.behsazan.corebanking.fee3.application.Fee3DraftPolicy;
import com.behsazan.corebanking.fee3.application.Fee3Problem;
import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Fee3DraftPolicyTest {
    private final LocalDate today=LocalDate.of(2026,10,26);
    private DraftRequest request(ItemDraft item,VersionDraft version,ScopeDraft scope,List<RuleDraft> rules){
        return new DraftRequest(null,null,null,null,item,version,scope,rules,List.of());
    }
    private ItemDraft item(String module,String op,String kind){
        return new ItemDraft("کارمزد آزمایشی",module,op,kind,null,"TARIFF","آزمون بدون اثر مالی");
    }
    private VersionDraft version(LocalDate from,LocalDate to){
        return new VersionDraft(from,to,"BANK_INTERNAL",null,"FIXED","UNREVIEWED","HALF_UP",null,Map.of(),"IRR","IRR",Map.of());
    }
    private ScopeDraft scope(List<String> products){
        return new ScopeDraft(List.of("BRANCH"),List.of("ALL"),List.of("ALL"),products,List.of(),"IRR");
    }
    private RuleDraft rule(int order,String method,BigDecimal fixed){
        return new RuleDraft(order,method,"REQUEST_AMOUNT","RULE",null,fixed,null,null,null,null,null,Map.of());
    }
    private void check(DraftRequest r){Fee3DraftPolicy.check(r,today,List.of(r.item().amountKind()));}
    private void denied(DraftRequest r){assertThatThrownBy(()->check(r)).isInstanceOf(Fee3Problem.class);}
    @Test void fixedDraftIsValid(){check(request(item("DEPOSIT","ACCOUNT_OPENING","SERVICE_FEE"),version(today,null),scope(List.of("ALL")),List.of(rule(1,"FIXED",BigDecimal.valueOf(25000)))));}
    @Test void todayAllowed(){check(request(item("DEPOSIT","ACCOUNT_OPENING","SERVICE_FEE"),version(today,today),scope(List.of("ALL")),List.of()));}
    @Test void tomorrowAllowed(){check(request(item("DEPOSIT","ACCOUNT_OPENING","SERVICE_FEE"),version(today.plusDays(1),null),scope(List.of("ALL")),List.of()));}
    @Test void yesterdayDenied(){denied(request(item("DEPOSIT","ACCOUNT_OPENING","SERVICE_FEE"),version(today.minusDays(1),null),scope(List.of("ALL")),List.of()));}
    @Test void endBeforeStartDenied(){denied(request(item("DEPOSIT","ACCOUNT_OPENING","SERVICE_FEE"),version(today,today.minusDays(1)),scope(List.of("ALL")),List.of()));}
    @Test void chequeBookDoesNotPermitDepositProduct(){denied(request(item("DEPOSIT","CHEQUEBOOK_ISSUE","SERVICE_FEE"),version(today,null),scope(List.of("DEP_CURRENT")),List.of()));}
    @Test void collateralValuationDoesNotPermitLoanProduct(){denied(request(item("LOAN","COLLATERAL_VALUATION","SERVICE_FEE"),version(today,null),scope(List.of("LOAN_MURABAHA")),List.of()));}
    @Test void productBasedAccountOpeningPermitsSelection(){check(request(item("DEPOSIT","ACCOUNT_OPENING","SERVICE_FEE"),version(today,null),scope(List.of("DEP_CURRENT")),List.of()));}
    @Test void loanServiceProductRequiresServiceFee(){denied(request(item("LOAN","LOAN_SERVICE","FINANCING_PROFIT"),version(today,null),scope(List.of("LOAN_MURABAHA")),List.of()));}
    @Test void allMixedWithSpecificProductDenied(){denied(request(item("DEPOSIT","ACCOUNT_OPENING","SERVICE_FEE"),version(today,null),scope(List.of("ALL","DEP_CURRENT")),List.of()));}
    @Test void negativeMoneyDenied(){denied(request(item("DEPOSIT","ACCOUNT_OPENING","SERVICE_FEE"),version(today,null),scope(List.of("ALL")),List.of(rule(1,"FIXED",new BigDecimal("-0.01")))));}
    @Test void missingOperationKindDenied(){DraftRequest r=request(item("DEPOSIT","ACCOUNT_OPENING","SERVICE_FEE"),version(today,null),scope(List.of("ALL")),List.of());assertThatThrownBy(()->Fee3DraftPolicy.check(r,today,List.of("OTHER_REVENUE"))).isInstanceOf(Fee3Problem.class);}
    @Test void duplicateRuleOrdersDenied(){denied(request(item("DEPOSIT","ACCOUNT_OPENING","SERVICE_FEE"),version(today,null),scope(List.of("ALL")),List.of(rule(1,"FIXED",null),rule(1,"FIXED",null))));}
    @Test void unknownMethodDenied(){denied(request(item("DEPOSIT","ACCOUNT_OPENING","SERVICE_FEE"),version(today,null),scope(List.of("ALL")),List.of(rule(1,"LEGACY",null))));}
    @Test void noMixedRequestRate(){RuleDraft r=new RuleDraft(1,"FIXED","REQUEST_AMOUNT","REQUEST",BigDecimal.valueOf(15),null,null,null,null,null,null,Map.of());denied(request(item("DEPOSIT","ACCOUNT_OPENING","SERVICE_FEE"),version(today,null),scope(List.of("ALL")),List.of(r)));}
    @Test void coverageMustBePositive(){RuleDraft r=new RuleDraft(1,"FIXED","REQUEST_AMOUNT","RULE",null,null,"CASH_100",BigDecimal.ZERO,null,null,null,Map.of());denied(request(item("DEPOSIT","ACCOUNT_OPENING","SERVICE_FEE"),version(today,null),scope(List.of("ALL")),List.of(r)));}
    @Test void minMustNotExceedMax(){RuleDraft r=new RuleDraft(1,"FIXED","REQUEST_AMOUNT","RULE",null,null,null,null,BigDecimal.TEN,BigDecimal.ONE,null,Map.of());denied(request(item("DEPOSIT","ACCOUNT_OPENING","SERVICE_FEE"),version(today,null),scope(List.of("ALL")),List.of(r)));}
    @Test void noPriorApprovalImplicitlyAssigned(){check(request(item("DEPOSIT","ACCOUNT_OPENING","SERVICE_FEE"),version(today,null),scope(List.of("ALL")),List.of()));}
}
