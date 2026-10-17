package com.behsazan.corebanking.posting.cash;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class CashPostingPolicyTest {
    private CashPostingPolicy.Instruction example(String type, String amount) {
        return new CashPostingPolicy.Instruction(501,21,"12345678","CASH-DESK-A",type,
                new BigDecimal(amount),"IRR",LocalDate.of(2026,10,17),LocalDate.of(2026,10,17),
                "201","BR001","CASH-ORDER-011");
    }
    @Test void depositDrCashCrLiability() {
        var p=CashPostingPolicy.make(example("CASH_DEPOSIT","100000"),0);
        assertEquals(CashPostingPolicy.CASH_POSITION,p.sourcePositionRole());
        assertEquals(CashPostingPolicy.CUSTOMER_POSITION,p.destinationPositionRole());
        assertEquals("DEBIT",p.entries().get(0).direction());
        assertEquals(CashPostingPolicy.CASH_GL,p.entries().get(0).role());
        assertEquals("CREDIT",p.entries().get(1).direction());
        assertEquals(CashPostingPolicy.CUSTOMER_GL,p.entries().get(1).role());
        assertEquals(0,p.amountUnits().compareTo(new BigDecimal("100000")));
    }
    @Test void withdrawalDrLiabilityCrCash() {
        var p=CashPostingPolicy.make(example("CASH_WITHDRAWAL","60000"),0);
        assertEquals(CashPostingPolicy.CUSTOMER_POSITION,p.sourcePositionRole());
        assertEquals(CashPostingPolicy.CASH_POSITION,p.destinationPositionRole());
        assertEquals(CashPostingPolicy.CUSTOMER_GL,p.entries().get(0).role());
        assertEquals(CashPostingPolicy.CASH_GL,p.entries().get(1).role());
    }
    @Test void rejectsUnsupportedOrFractionalOrNegative() {
        assertThrows(IllegalArgumentException.class,()->CashPostingPolicy.make(example("TRANSFER","5"),0));
        assertThrows(IllegalArgumentException.class,()->CashPostingPolicy.make(example("CASH_DEPOSIT","1.5"),0));
        assertThrows(IllegalArgumentException.class,()->CashPostingPolicy.make(example("CASH_DEPOSIT","-5"),0));
        assertEquals(0,CashPostingPolicy.make(example("CASH_DEPOSIT","1.25"),2)
                .amountUnits().compareTo(new BigDecimal("125")));
    }
    @Test void stableIdempotencyHash() {
        var a=CashPostingPolicy.make(example("CASH_DEPOSIT","100000.00"),0);
        var b=CashPostingPolicy.make(example("CASH_DEPOSIT","100000"),0);
        assertEquals(a.payloadHash(),b.payloadHash());
        assertNotEquals(a.payloadHash(),CashPostingPolicy.make(example("CASH_DEPOSIT","100001"),0).payloadHash());
    }
}
