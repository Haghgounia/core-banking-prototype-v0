package com.behsazan.corebanking.deposit.account.servicing.domain;

import com.behsazan.corebanking.deposit.account.operations.domain.DepositAccountOperationsModels.AccountDetails;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public final class DepositAccountServicingModels {
    private DepositAccountServicingModels() {}
    public record CloseAccountRequest(long expectedRecordVersion) {}
    public record CloseAccountResponse(AccountDetails account,boolean idempotentReplay) {}
    public record UpdateBasicInfoRequest(String accountName,String openingOrgUnitCode,long expectedRecordVersion,String reasonCode) {}
    public record AccountPartyRequest(long partyId,String roleCode,Boolean primary,BigDecimal ownershipPercent,LocalDate validFrom,String reasonCode) {}
    public record AccountContactRequest(String contactTypeCode,String contactValue,String purposeCode,Boolean primary,LocalDate validFrom,String reasonCode) {}

    public record LifecycleActionRequest(long expectedRecordVersion,String reasonCode) {}
    public record LifecycleActionResponse(AccountDetails account,boolean idempotentReplay,String eventTypeCode) {}

    public record CreateHoldRequest(String holdTypeCode,BigDecimal holdAmount,String currencyCode,String holdReasonCode,String sourceReference,OffsetDateTime validTo,String originSystemCode,String originModuleCode,String originRequestRef,String originExecutionModeCode,String releasePolicyCode) {}
    public record ReleaseHoldRequest(BigDecimal releaseAmount,String reasonCode,String actionSourceSystemCode,String actionSourceModuleCode,String actionRequestRef,String actionExecutionModeCode) {}
    public record HoldActionResponse(AccountDetails account,boolean idempotentReplay,long accountHoldId) {}
}
