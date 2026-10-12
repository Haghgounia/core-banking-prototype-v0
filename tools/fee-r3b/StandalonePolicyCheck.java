package com.behsazan.corebanking.fee.admin.application;

import java.math.BigDecimal;
import java.util.Map;

/** Dependency-free smoke test, executed by tools/verify-fee-r3b.mjs. */
public final class StandalonePolicyCheck {
  public static void main(String[] args) {
    var links = FeeChargeStageIntegrityPolicy.links(Map.of(
      "FEE_DEFINITION_VERSION_ID", new BigDecimal("16"),
      "CALCULATION_RULE_ID", "22",
      "CHARGE_COMPONENT_ID", 33), null);
    FeeChargeStageIntegrityPolicy.verifyOwners(links, 16, 16L);
    expectReject(() -> FeeChargeStageIntegrityPolicy.verifyOwners(links, 17, 16L));
    expectReject(() -> FeeChargeStageIntegrityPolicy.verifyOwners(links, 16, 17L));
    expectReject(() -> FeeChargeStageIntegrityPolicy.verifyParentMove(16, 17, true, "قاعده"));
    FeeChargeStageIntegrityPolicy.verifyParentMove(16, 17, false, "قاعده");
    expectReject(() -> FeeChargeStageIntegrityPolicy.links(Map.of(
      "FEE_DEFINITION_VERSION_ID", 16, "CALCULATION_RULE_ID", 0), null));
    expectReject(() -> FeeChargeStageIntegrityPolicy.links(Map.of(
      "FEE_DEFINITION_VERSION_ID", 16, "CALCULATION_RULE_ID", "2.2"), null));
    var retained = FeeChargeStageIntegrityPolicy.links(Map.of("STAGE_CODE", "OTHER"), Map.of(
      "FEE_DEFINITION_VERSION_ID", 16, "CALCULATION_RULE_ID", 22));
    if (retained.versionId() != 16 || retained.ruleId() != 22) throw new AssertionError("partial update");
    System.out.println("FEE_R3B_POLICY_RUNTIME_PASS");
  }
  private static void expectReject(Runnable action) {
    try { action.run(); } catch (FeeAdminValidationException expected) { return; }
    throw new AssertionError("expected rejection");
  }
}
