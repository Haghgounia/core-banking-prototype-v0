package com.behsazan.corebanking.fee3.r3;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

/** F6 end-to-end service contract: exact decimals, pre-tax gates, no posting/Oracle writes. */
class Fee3F6SimulationServiceTest {
  private static Fee3F6SimulationService.SimulationResult run(String method,String tax,
      BigDecimal rate,BigDecimal fixed, Map<String,Object> config,Map<String,Object> inputs){
    RuleDraft rule=new RuleDraft(1,method,"REQUEST_AMOUNT","RULE",rate,fixed,null,null,
        null,null,null,Map.of("originalConfig",config));
    VersionDraft version=new VersionDraft(LocalDate.of(2026,10,29),null,"BANK_INTERNAL",null,
        method,tax,"HALF_UP",null,Map.of(),"IRR","IRR",Map.of());
    DraftRequest draft=new DraftRequest(null,null,null,null,
        new ItemDraft("Analyst test","DEPOSIT","ACCOUNT_OPENING","SERVICE_FEE",null,"TARIFF",null),
        version,new ScopeDraft(List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of(),"IRR"),
        List.of(rule),List.of());
    return Fee3F6SimulationService.simulate(new Fee3PreviewEngine.PreviewRequest(draft,inputs));
  }
  @Test void fixedReturnsDecimalStringsAndReadableBreakdown(){
    var result=run("FIXED","EXEMPT",null,new BigDecimal("25000"),Map.of(),Map.of());
    assertThat(result.status()).isEqualTo("SIMULATED");
    assertThat(result.feeIrr()).isEqualTo("25000");
    assertThat(result.totalIrr()).isEqualTo("25000");
    assertThat(result.breakdown()).extracting(r->r.get("kind"))
      .contains("FIXED","LIMITS_AND_ROUNDING");
    assertThat(result.persisted()).isFalse();
    assertThat(result.financialPosting()).isFalse();
  }
  @Test void unreviewedTaxDoesNotInventPayableTotal(){
    var result=run("PERCENT","UNREVIEWED",new BigDecimal("1.5"),null,
        Map.of(),Map.of("amount","1000000"));
    assertThat(result.status()).isEqualTo("PRETAX_ONLY");
    assertThat(result.feeIrr()).isEqualTo("15000");
    assertThat(result.taxIrr()).isNull();
    assertThat(result.totalIrr()).isNull();
    assertThat(result.diagnostics()).contains("TAX_UNREVIEWED_PRETAX_ONLY_NO_PAYABLE_TOTAL");
  }
  @Test void usageFromF6EngineReturnsDetailedChargeableUnits(){
    var result=run("USAGE","NONE",null,null,
        Map.of("freeUnits",2,"unitPrice",3000),Map.of("usage","5"));
    assertThat(result.feeIrr()).isEqualTo("9000");
    assertThat(result.breakdown().stream().anyMatch(r ->
        "USAGE".equals(r.get("kind")) && "3".equals(r.get("chargeableUnits")))).isTrue();
  }
  @Test void unknownFormulaAndUnsafeSpecializedContractRemainBlocked(){
    var formula=run("FORMULA","EXEMPT",null,null,Map.of(),Map.of());
    assertThat(formula.status()).isEqualTo("BLOCKED");
    assertThat(formula.totalIrr()).isNull();
  }
}
