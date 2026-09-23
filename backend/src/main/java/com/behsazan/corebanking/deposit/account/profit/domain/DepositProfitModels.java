package com.behsazan.corebanking.deposit.account.profit.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class DepositProfitModels {
    private DepositProfitModels() {}

    public record ProfitContract(
            long profitContractId,long accountId,long openingRequestId,long productVersionId,
            Long pricingRuleId,Long pricingComponentId,Long rateTierId,Long profitPaymentRuleId,
            BigDecimal annualRate,String calculationMethodCode,String dayCountBasisCode,String accrualFrequencyCode,
            String paymentFrequencyCode,String paymentDayRuleCode,String firstPaymentRuleCode,String holidayAdjustmentCode,
            String paymentDestinationCode,String destinationAccountReference,LocalDate effectiveFrom,LocalDate effectiveTo,
            BigDecimal accruedAmount,BigDecimal paidAmount,LocalDate lastAccrualDate,LocalDate lastPaymentDate,
            String statusCode,long recordVersion
    ) {}

    public record ProfitAccrual(
            long profitAccrualId,long profitContractId,long accountId,LocalDate accrualFromDate,LocalDate accrualToDate,
            int dayCount,BigDecimal basisAmount,BigDecimal annualRate,BigDecimal accruedAmount,String dayCountBasisCode,
            String calculationMethodCode,String statusCode,Long postingId,OffsetDateTime calculatedAt,String calculatedBy
    ) {}

    public record ProfitPosting(
            long profitPostingId,long profitContractId,long accountId,LocalDate postingDate,BigDecimal amount,
            String destinationCode,String destinationAccountReference,Long subledgerEntryId,String statusCode,
            OffsetDateTime postedAt,String postedBy
    ) {}

    public record ProfitView(ProfitContract contract,List<ProfitAccrual> accruals,List<ProfitPosting> postings) {}
    public record AccrualRequest(LocalDate throughDate) {}
    public record ProfitActionResponse(ProfitView state,long entityId,boolean idempotentReplay) {}
    public record PostProfitRequest(LocalDate postingDate) {}
}
