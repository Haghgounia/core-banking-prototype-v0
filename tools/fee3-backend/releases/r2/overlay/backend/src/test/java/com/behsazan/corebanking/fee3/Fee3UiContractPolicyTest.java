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

/** UAT-only write adapter policy: rejects mismatched contract and missing reference constraints. */
class Fee3UiContractPolicyTest {
    private final LocalDate today=LocalDate.of(2026,10,26);
    private ItemDraft item(){return new ItemDraft("حق‌الزحمه آزمایشی","GUARANTEE","GUARANTEE_ISSUE","GUARANTEE_FEE",null,"GUARANTEE",null);}
    private ScopeDraft scope(){return new ScopeDraft(List.of("BRANCH"),List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of(),"IRR");}
    private VersionDraft version(String service,String fee,Map<String,Object> ui,Map<String,Object> financial){
        return new VersionDraft(today,null,"BANK_INTERNAL",null,"PERCENT","UNREVIEWED","HALF_UP",null,financial,service,fee,ui);
    }
    private DraftRequest request(VersionDraft version){
        return new DraftRequest(null,null,null,null,item(),version,scope(),List.of(
            new RuleDraft(1,"PERCENT","GUARANTEE_AMOUNT","RULE",BigDecimal.ONE,null,"CASH_100",BigDecimal.valueOf(100),null,null,null,Map.of("originalConfig",Map.of("tiers",List.of())))),List.of());
    }
    private void accept(VersionDraft version){Fee3DraftPolicy.check(request(version),today,List.of("GUARANTEE_FEE"));}
    private void reject(VersionDraft version){assertThatThrownBy(()->accept(version)).isInstanceOf(Fee3Problem.class);}
    @Test void validIrRContractWithGuaranteePortfolioContext(){
        accept(version("IRR","IRR",Map.of("guaranteePolicy",Map.of("collateralType","MIXED_PORTFOLIO","portfolioRules",List.of())),Map.of()));
    }
    @Test void feeCurrencyOtherThanIRRRequiresExplicitSchemaMigration(){reject(version("IRR","USD",Map.of(),Map.of()));}
    @Test void currencyScopeMismatchIsDenied(){reject(version("EUR","IRR",Map.of(),Map.of()));}
    @Test void arbitraryTopLevelUiKeyIsDenied(){reject(version("IRR","IRR",Map.of("isPublished",true),Map.of()));}
    @Test void internalUiNamespaceCannotBeForged(){reject(version("IRR","IRR",Map.of(),Map.of("_fee3UiV1",Map.of("contractVersion",99))));}
    @Test void missingUiContextRejected(){reject(version("IRR","IRR",null,Map.of()));}
    @Test void financialAgreementBeforeBankTodayDenied(){reject(version("IRR","IRR",Map.of(),Map.of("deals",List.of(Map.of("from","2026-10-25")))));}
    @Test void futureDiscountIsAllowed(){accept(version("IRR","IRR",Map.of(),Map.of("discountGroups",List.of(Map.of("from","2026-10-27","to","2026-10-29")))));}
    @Test void discountEndBeforeStartDenied(){reject(version("IRR","IRR",Map.of(),Map.of("discountGroups",List.of(Map.of("from","2026-10-27","to","2026-10-26")))));}
    @Test void malformedDateDenied(){reject(version("IRR","IRR",Map.of(),Map.of("deals",List.of(Map.of("from","1405/08/04")))));}
    @Test void wrongDateArrayDenied(){reject(version("IRR","IRR",Map.of(),Map.of("discountGroups","bad")));}
}
