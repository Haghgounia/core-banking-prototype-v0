package com.behsazan.corebanking.fee3.r3;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

/** F6 formula-safe, read-only engineering preview. No Oracle or posting. */
class Fee3F6PreviewEngineTest {
  private static BigDecimal d(String v){return new BigDecimal(v);}
  private static Map<String,Object> row(Object... fields){
    var result=new java.util.LinkedHashMap<String,Object>();
    for(int i=0;i<fields.length;i+=2)result.put((String)fields[i],fields[i+1]);
    return result;
  }
  private static Fee3PreviewEngine.PreviewResult preview(String method,Map<String,Object> cfg,Map<String,Object> inputs,String tax){
    var r=new RuleDraft(1,method,"REQUEST_AMOUNT","RULE",d("1"),null,null,null,null,null,null,
      Map.of("originalConfig",cfg));
    var v=new VersionDraft(LocalDate.of(2026,10,29),null,"BANK_INTERNAL",null,method,tax,"HALF_UP",null,
      Map.of(),"IRR","IRR",Map.of());
    var request=new DraftRequest(null,null,null,null,
      new ItemDraft("Sandbox","DEPOSIT","ACCOUNT_OPENING","SERVICE_FEE",null,"TARIFF",null),
      v,new ScopeDraft(List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of(),"IRR"),
      List.of(r),List.of());
    return Fee3PreviewEngine.calculate(new Fee3PreviewEngine.PreviewRequest(request,inputs));
  }
  private static Fee3PreviewEngine.PreviewResult preview(String method,Map<String,Object> cfg,Map<String,Object> inputs){
    return preview(method,cfg,inputs,"EXEMPT");
  }
  @Test void usageCountsBillableUnitsOnly(){
    var x=preview("USAGE",Map.of("freeUnits",2,"unitPrice",10000),Map.of("usage",6));
    assertThat(x.totalIrr()).isEqualByComparingTo("40000");
    assertThat(x.persisted()).isFalse();assertThat(x.financialPosting()).isFalse();
  }
  @Test void usageWithoutPriceIsBlocked(){
    assertThat(preview("USAGE",Map.of("freeUnits",0),Map.of("usage",6)).status()).isEqualTo("BLOCKED");
  }
  @Test void dailyAct365IsBoundedAndActActIsBlocked(){
    assertThat(preview("DAILY",Map.of("dayBasis","ACT_365"),Map.of("amount",3650000,"days",1)).totalIrr())
      .isEqualByComparingTo("100");
    assertThat(preview("DAILY",Map.of("dayBasis","ACT_ACT"),Map.of("amount",3650000,"days",1)).status())
      .isEqualTo("BLOCKED");
  }
  @Test void bracketLegacyBoundaryRemainsExclusive(){
    var cfg=Map.<String,Object>of("brackets",List.of(
      row("from",0,"to",10000000,"ratePercent",0,"fixedAmount",25000),
      row("from",10000000,"to",null,"ratePercent",0.08,"fixedAmount",0)));
    assertThat(preview("BRACKET",cfg,Map.of("amount",10000000)).totalIrr()).isEqualByComparingTo("8000");
  }
  @Test void bracketOverlappingIsNotZeroFee(){
    var cfg=Map.<String,Object>of("brackets",List.of(
      row("from",0,"to",100,"inclusiveUpper",true,"calcType","FIXED","fixedAmount",10),
      row("from",100,"to",null,"calcType","FIXED","fixedAmount",20)));
    assertThat(preview("BRACKET",cfg,Map.of("amount",100)).status()).isEqualTo("BLOCKED");
  }
  @Test void progressiveChargesShareOfEachTier(){
    var cfg=Map.<String,Object>of("tiers",List.of(
      row("upTo",10000000,"ratePercent",0.1,"fixedAmount",0),
      row("upTo",null,"ratePercent",0.05,"fixedAmount",0)));
    assertThat(preview("PROGRESSIVE",cfg,Map.of("amount",15000000)).totalIrr()).isEqualByComparingTo("12500");
  }
  @Test void matrixPicksMostSpecificMatchingCell(){
    var cfg=Map.<String,Object>of("matrix",List.of(
      row("customer","ALL","channel","ALL","basis","FIXED","value",30000),
      row("customer","PERSON","channel","MOBILE","basis","FIXED","value",10000)));
    assertThat(preview("MATRIX",cfg,Map.of("customer","PERSON","channel","MOBILE")).totalIrr())
      .isEqualByComparingTo("10000");
  }
  @Test void matrixAmbiguousAndUnknownFormulaRemainBlocked(){
    var cfg=Map.<String,Object>of("matrix",List.of(
      row("customer","PERSON","channel","ALL","basis","FIXED","value",1000),
      row("customer","ALL","channel","MOBILE","basis","FIXED","value",1000)));
    assertThat(preview("MATRIX",cfg,Map.of("customer","PERSON","channel","MOBILE")).status())
      .isEqualTo("BLOCKED");
    assertThat(preview("FORMULA",Map.of(),Map.of()).status()).isEqualTo("BLOCKED");
  }
  @Test void unknownTaxPolicyNeverSimulatesPayableFee(){
    var r=preview("USAGE",Map.of("freeUnits",0,"unitPrice",1000),Map.of("usage",1),"UNREVIEWED");
    assertThat(r.status()).isEqualTo("BLOCKED");
    assertThat(r.totalIrr()).isNull();
  }
}
