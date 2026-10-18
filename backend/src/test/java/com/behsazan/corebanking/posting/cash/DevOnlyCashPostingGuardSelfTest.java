package com.behsazan.corebanking.posting.cash;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Dependency-free execution gate regression tests: javac + java. */
public final class DevOnlyCashPostingGuardSelfTest {
    private static int passed=0;
    private static DevOnlyCashPostingGuard.Settings settings() {
        return new DevOnlyCashPostingGuard.Settings(true,false,true,"FREEPDB1","FREEPDB1",
                "FREEPDB1","FREEPDB1",1,"UAT1405-DESK-01","IRR","9","0101",
                "UAT1405-",new BigDecimal("100000"),"UAT1405-IRR","UAT_ENTITY_1405",
                "UAT1405-LEDGER","UAT1405-CHART","UAT1405-CASH-DEP","UAT1405-CASH-WDR",
                "UAT1405-MAP-DEP","UAT1405-MAP-WDR");
    }
    private static CashPostingPolicy.Instruction instruction(long accountId, String type,
            BigDecimal amount,String ref, String desk, String product, String unit, String currency, LocalDate date) {
        return new CashPostingPolicy.Instruction(1001,accountId,"SYNTHETIC-ACCOUNT-NO",desk,type,
                amount,currency,date,date,product,unit,ref);
    }
    private static CashPostingPolicy.Instruction ok() {
        return instruction(1,"CASH_DEPOSIT",new BigDecimal("100000"),"UAT1405-CASH-DEP-01",
                "UAT1405-DESK-01","9","0101","IRR",LocalDate.of(2026,10,18));
    }
    private static void accept(DevOnlyCashPostingGuard.Settings s,CashPostingPolicy.Instruction r) {
        DevOnlyCashPostingGuard.validate(s,r);passed++;
    }
    private static void reject(DevOnlyCashPostingGuard.Settings s,CashPostingPolicy.Instruction r) {
        try { DevOnlyCashPostingGuard.validate(s,r); }
        catch (IllegalStateException ex) { passed++; return; }
        throw new AssertionError("DEV_ONLY gate unexpectedly allowed unsafe context");
    }
    private static DevOnlyCashPostingGuard.Settings withProfile(boolean dev,boolean prod) {
        var s=settings(); return new DevOnlyCashPostingGuard.Settings(dev,prod,s.syntheticAccountConfirmed(),
                s.actualDb(),s.actualPdb(),s.expectedDb(),s.expectedPdb(),s.pilotAccountId(),
                s.cashDeskCode(),s.currency(),s.productVersion(),s.bankUnit(),s.externalReferencePrefix(),
                s.maxAmount(),s.bookCode(),s.legalEntity(),s.ledgerCode(),s.chartCode(),
                s.depositRule(),s.withdrawalRule(),s.depositMapping(),s.withdrawalMapping());
    }
    public static void main(String[] args) {
        accept(settings(),ok());
        accept(settings(),instruction(1,"CASH_WITHDRAWAL",new BigDecimal("60000"),
                "UAT1405-CASH-WDR-01","UAT1405-DESK-01","9","0101","IRR",LocalDate.of(2026,10,18)));
        reject(settings(),instruction(2,"CASH_DEPOSIT",new BigDecimal("100000"),"UAT1405-CASH-DEP-01","UAT1405-DESK-01","9","0101","IRR",LocalDate.of(2026,10,18)));
        reject(settings(),instruction(1,"CASH_DEPOSIT",new BigDecimal("100001"),"UAT1405-CASH-DEP-01","UAT1405-DESK-01","9","0101","IRR",LocalDate.of(2026,10,18)));
        reject(settings(),instruction(1,"CASH_DEPOSIT",new BigDecimal("100000"),"LIVE-REF-01","UAT1405-DESK-01","9","0101","IRR",LocalDate.of(2026,10,18)));
        reject(settings(),instruction(1,"CASH_DEPOSIT",new BigDecimal("100000"),"UAT1405-DEP-1","LIVE-DESK","9","0101","IRR",LocalDate.of(2026,10,18)));
        reject(settings(),instruction(1,"CASH_DEPOSIT",new BigDecimal("100000"),"UAT1405-DEP-1","UAT1405-DESK-01","2","0101","IRR",LocalDate.of(2026,10,18)));
        reject(settings(),instruction(1,"CASH_DEPOSIT",new BigDecimal("100000"),"UAT1405-DEP-1","UAT1405-DESK-01","9","0205","IRR",LocalDate.of(2026,10,18)));
        reject(settings(),instruction(1,"CASH_DEPOSIT",new BigDecimal("100000"),"UAT1405-DEP-1","UAT1405-DESK-01","9","0101","USD",LocalDate.of(2026,10,18)));
        reject(settings(),instruction(1,"TRANSFER",new BigDecimal("100000"),"UAT1405-DEP-1","UAT1405-DESK-01","9","0101","IRR",LocalDate.of(2026,10,18)));
        reject(settings(),instruction(1,"CASH_DEPOSIT",new BigDecimal("100000"),"UAT1405-DEP-1","UAT1405-DESK-01","9","0101","IRR",LocalDate.of(2027,4,1)));
        reject(withProfile(false,false),ok());
        reject(withProfile(true,true),ok());
        var s=settings();
        reject(new DevOnlyCashPostingGuard.Settings(s.postingDevProfile(),s.productionProfile(),false,
                s.actualDb(),s.actualPdb(),s.expectedDb(),s.expectedPdb(),s.pilotAccountId(),s.cashDeskCode(),
                s.currency(),s.productVersion(),s.bankUnit(),s.externalReferencePrefix(),s.maxAmount(),
                s.bookCode(),s.legalEntity(),s.ledgerCode(),s.chartCode(),s.depositRule(),s.withdrawalRule(),
                s.depositMapping(),s.withdrawalMapping()),ok());
        reject(new DevOnlyCashPostingGuard.Settings(s.postingDevProfile(),s.productionProfile(),true,
                "WRONGDB",s.actualPdb(),s.expectedDb(),s.expectedPdb(),s.pilotAccountId(),s.cashDeskCode(),
                s.currency(),s.productVersion(),s.bankUnit(),s.externalReferencePrefix(),s.maxAmount(),
                s.bookCode(),s.legalEntity(),s.ledgerCode(),s.chartCode(),s.depositRule(),s.withdrawalRule(),
                s.depositMapping(),s.withdrawalMapping()),ok());
        System.out.println("POSTING_R1D8I_DEV_ONLY_GUARD_PASS="+passed+"/"+passed);
    }
}
