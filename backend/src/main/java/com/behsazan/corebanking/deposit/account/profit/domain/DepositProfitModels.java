package com.behsazan.corebanking.deposit.account.profit.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class DepositProfitModels {
    private DepositProfitModels() {}

    public record ProfitProfile(
            long accountProfitProfileId,long accountId,
            Long productPricingRuleId,Long productPricingComponentId,Long productRateTierId,Long profitPaymentRuleId,
            BigDecimal rateValue,String calculationMethodCode,String dayCountBasisCode,String accrualFrequencyCode,
            String paymentFrequencyCode,String paymentDayRuleCode,Integer paymentDayNo,String firstPaymentRuleCode,
            String holidayAdjustmentCode,String paymentDestinationCode,Long destinationAccountId,Boolean destinationSelectedByCustomer,
            String destinationAccountReference,LocalDate validFrom,LocalDate validTo,String statusCode,
            Long sourceOpeningProfitInstructionId,long recordVersion
    ) {}

    public record ProfitContract(
            long profitContractId,long accountId,long openingRequestId,long productVersionId,
            Long pricingRuleId,Long pricingComponentId,Long rateTierId,Long profitPaymentRuleId,
            BigDecimal annualRate,String calculationMethodCode,String dayCountBasisCode,String accrualFrequencyCode,
            String paymentFrequencyCode,String paymentDayRuleCode,String firstPaymentRuleCode,String holidayAdjustmentCode,
            String paymentDestinationCode,String destinationAccountReference,LocalDate effectiveFrom,LocalDate effectiveTo,
            BigDecimal accruedAmount,BigDecimal paidAmount,LocalDate lastAccrualDate,LocalDate lastPaymentDate,
            String statusCode,long recordVersion
    ) {}

    public record ProfitPeriod(
            long profitPeriodId,long accountProfitProfileId,LocalDate periodStartDate,LocalDate periodEndDate,
            String periodStatusCode,BigDecimal totalAccruedAmount,BigDecimal totalAdjustmentAmount,
            BigDecimal payableAmount,BigDecimal paidAmount,long recordVersion
    ) {}

    public record ProfitAccrual(
            long profitAccrualId,long profitContractId,long accountId,Long profitPeriodId,LocalDate accrualFromDate,LocalDate accrualToDate,
            int dayCount,BigDecimal basisAmount,BigDecimal annualRate,BigDecimal accruedAmount,String dayCountBasisCode,
            String calculationMethodCode,String statusCode,Long postingId,OffsetDateTime calculatedAt,String calculatedBy
    ) {}

    public record ProfitAccrualDetail(
            long profitAccrualDetailId,long profitAccrualId,String componentCode,BigDecimal basisAmount,
            BigDecimal rateValue,BigDecimal accrualAmount,String calculationNote,long recordVersion
    ) {}

    public record ProfitAdjustment(
            long profitAdjustmentId,long accountId,long profitPeriodId,Long originalAccrualId,Long originalPaymentId,
            String adjustmentTypeCode,BigDecimal adjustmentAmount,String reasonCode,String adjustmentStatusCode,
            Long approvalRequestId,String approvalStatusCode,String approverUserId,String postingReference,long recordVersion
    ) {}

    public record ProfitPayment(
            long profitPaymentId,long profitPeriodId,long accountId,LocalDate paymentDueDate,LocalDate paymentDate,
            BigDecimal paymentAmount,String paymentDestinationCode,String destinationAccountReference,
            String paymentStatusCode,String postingReference,int attemptNo,long recordVersion
    ) {}

    public record ProfitPosting(
            long profitPostingId,long profitContractId,long accountId,LocalDate postingDate,BigDecimal amount,
            String destinationCode,String destinationAccountReference,Long subledgerEntryId,String statusCode,
            OffsetDateTime postedAt,String postedBy
    ) {}

    public record ProfitView(
            ProfitProfile profile,ProfitContract contract,List<ProfitPeriod> periods,List<ProfitAccrual> accruals,
            List<ProfitAccrualDetail> accrualDetails,List<ProfitAdjustment> adjustments,List<ProfitPayment> payments,
            List<ProfitPosting> postings
    ) {}

    public record AccrualRequest(LocalDate throughDate) {}
    public record AdjustmentRequest(Long originalAccrualId,Long originalPaymentId,String adjustmentTypeCode,
                                    BigDecimal adjustmentAmount,String reasonCode,String approverUserId,String orgUnitCode) {}
    public record ProfitActionResponse(ProfitView state,long entityId,boolean idempotentReplay) {}
    public record PostProfitRequest(LocalDate postingDate) {}
}
