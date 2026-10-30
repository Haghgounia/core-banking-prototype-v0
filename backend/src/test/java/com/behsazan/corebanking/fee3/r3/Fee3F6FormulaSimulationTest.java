package com.behsazan.corebanking.fee3.r3;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/** Analyst FORMULA arithmetic: server-owned, exact decimals, no tax invention or financial effects. */
class Fee3F6FormulaSimulationTest {
  private static Fee3F6SimulationService.SimulationResult run(String expression,String tax,
      Map<String,Object> inputs,Map<String,Object> config,String code) {
    Map<String,Object> parameters=new java.util.HashMap<>();
    parameters.put("originalConfig",config);
    if(expression!=null)parameters.put("_fceEditableExpressionV1",expression);
    RuleDraft rule=new RuleDraft(1,"FORMULA","REQUEST_AMOUNT","REQUEST",null,null,
        null,null,null,null,code,parameters);
    VersionDraft version=new VersionDraft(LocalDate.of(2026,10,29),null,"BANK_INTERNAL",null,
        "FORMULA",tax,"HALF_UP",null,Map.of(),"IRR","IRR",Map.of());
    DraftRequest draft=new DraftRequest(null,null,null,null,
        new ItemDraft("Formula sandbox","PAYMENT","TRANSFER","SERVICE_FEE",null,"TARIFF",null),
        version,new ScopeDraft(List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of(),"IRR"),
        List.of(rule),List.of());
    return Fee3F6SimulationService.simulate(new Fee3PreviewEngine.PreviewRequest(draft,inputs));
  }
  private static Fee3F6SimulationService.SimulationResult run(String expr,Map<String,Object> inputs){
    return run(expr,"UNREVIEWED",inputs,Map.of(),null);
  }
  @Test void percentageUsesExactDecimalAndPretaxOnly(){
    var r=run("amount * rate / 100",Map.of("amount","9007199254740993","rate","0.1"));
    assertThat(r.status()).isEqualTo("PRETAX_ONLY");
    assertThat(r.feeIrr()).isEqualTo("9007199254741");
    assertThat(r.rawIrr()).isEqualTo("9007199254740.993");
    assertThat(r.taxIrr()).isNull();assertThat(r.totalIrr()).isNull();
    assertThat(r.persisted()).isFalse();assertThat(r.financialPosting()).isFalse();
    assertThat(r.breakdown()).extracting(x->x.get("kind")).contains("FORMULA_DECIMAL","LIMITS_AND_ROUNDING");
  }
  @Test void paymentAndCompoundPresets(){
    assertThat(run("fixed + amount * rate / 100",Map.of("fixed","25000","amount","1000000","rate","1.5")).feeIrr())
      .isEqualTo("40000");
    assertThat(run("amount * (pow(1 + rate / 1200, months) - 1)",
      Map.of("amount","1000000","rate","12","months","12")).status()).isEqualTo("PRETAX_ONLY");
    assertThat(run("pmt(amount,rate,months) * months - amount",
      Map.of("amount","1200000","rate","0","months","12")).feeIrr()).isEqualTo("0");
    assertThat(run("ipmt(amount,rate,months,period)",
      Map.of("amount","1000000","rate","12","months","12","period","1")).feeIrr()).isEqualTo("10000");
  }
  @Test void customDeclaredVariableAndRounding(){
    var config=Map.<String,Object>of("formulaParams",List.of(Map.of("name","printingCost","label","Printing cost","type","MONEY")));
    var r=run("75000 + printingCost","NONE",Map.of("printingCost","12500.5"),config,null);
    assertThat(r.feeIrr()).isEqualTo("87501");
    assertThat(r.totalIrr()).isEqualTo("87501");
  }
  @Test void rejectsUnknownInputsAndSyntaxAndUnsafeMath(){
    assertThat(run("amount + unknown",Map.of("amount","10")).status()).isEqualTo("BLOCKED");
    assertThat(run("amount / 0",Map.of("amount","100")).diagnostics()).contains("FORMULA_DIVIDE_BY_ZERO");
    assertThat(run("pow(amount,0.5)",Map.of("amount","100")).diagnostics())
      .contains("FORMULA_FRACTIONAL_POWER_UNSUPPORTED");
    assertThat(run("amount; System.exit(0)",Map.of("amount","100")).status()).isEqualTo("BLOCKED");
    assertThat(run("pow(amount,2000)",Map.of("amount","10")).status()).isEqualTo("BLOCKED");
    assertThat(run("ipmt(amount,rate,months,period)",Map.of("amount","1000","rate","12",
      "months","12","period","13")).status()).isEqualTo("BLOCKED");
  }
  @Test void existingF2ReferenceMustUseF2ContractNotEditableInterpreter(){
    var r=run("amount * rate / 100","EXEMPT",Map.of("amount","100","rate","2"),Map.of(),"FCE_EXPR_CODE");
    assertThat(r.status()).isEqualTo("BLOCKED");
    assertThat(r.diagnostics()).contains("FORMULA_REFERENCE_REQUIRES_F2_ALLOWLIST_CONTRACT");
  }
}
