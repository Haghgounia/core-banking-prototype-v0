package com.behsazan.corebanking.posting.cash;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Dependency-free test entrypoint usable before Maven/Oracle is available. */
public final class CashPostingPolicySelfTest {
    private static CashPostingPolicy.Instruction i(String type, String amount) {
        return new CashPostingPolicy.Instruction(505,9,"DEPOSIT-009","TELLER-01",type,
                new BigDecimal(amount),"IRR", LocalDate.of(2026,10,17),LocalDate.of(2026,10,17),
                "PV-1","UNIT-02","CASH-REF-1");
    }
    private static int checks=0;
    private static void eq(Object expected,Object actual) {
        checks++;
        if(!expected.equals(actual)) throw new AssertionError("Expected "+expected+" but got "+actual);
    }
    private static void reject(Runnable task) {
        checks++;
        try {task.run();throw new AssertionError("Expected rejection");}
        catch(IllegalArgumentException expected) { /* fail closed */ }
    }
    public static void main(String[] args) {
        var deposit=CashPostingPolicy.make(i("CASH_DEPOSIT","125000"),0);
        eq("CASH_CLEARING", deposit.sourcePositionRole());
        eq("CUSTOMER_DEPOSIT",deposit.destinationPositionRole());
        eq("TELLER_CASH",deposit.entries().get(0).role());
        eq("DEBIT",deposit.entries().get(0).direction());
        eq("CUSTOMER_DEPOSIT_LIABILITY",deposit.entries().get(1).role());
        eq("CREDIT",deposit.entries().get(1).direction());
        var withdrawal=CashPostingPolicy.make(i("CASH_WITHDRAWAL","20000"),0);
        eq("CUSTOMER_DEPOSIT",withdrawal.sourcePositionRole());
        eq("CASH_CLEARING",withdrawal.destinationPositionRole());
        eq("CUSTOMER_DEPOSIT_LIABILITY",withdrawal.entries().get(0).role());
        eq("DEBIT",withdrawal.entries().get(0).direction());
        eq("TELLER_CASH",withdrawal.entries().get(1).role());
        eq("CREDIT",withdrawal.entries().get(1).direction());
        eq("125",CashPostingPolicy.make(i("CASH_DEPOSIT","1.25"),2).amountUnits().toPlainString());
        eq(deposit.payloadHash(),CashPostingPolicy.make(i("CASH_DEPOSIT","125000.000"),0).payloadHash());
        checks++;if (deposit.payloadHash().equals(withdrawal.payloadHash())) throw new AssertionError("Hash collision");
        reject(()->CashPostingPolicy.make(i("CASH_DEPOSIT","1.2"),0));
        reject(()->CashPostingPolicy.make(i("CASH_WITHDRAWAL","0"),0));
        reject(()->CashPostingPolicy.make(i("TRANSFER","100"),0));
        reject(()->CashPostingPolicy.make(i("CASH_DEPOSIT","-1"),0));
        reject(()->CashPostingPolicy.make(i("CASH_DEPOSIT","1000000000000000000000000"),0));
        reject(()->CashPostingPolicy.make(i("CASH_DEPOSIT","1"),9));
        System.out.println("POSTING_R1D5_POLICY_PASS="+checks+"/"+checks);
    }
}
