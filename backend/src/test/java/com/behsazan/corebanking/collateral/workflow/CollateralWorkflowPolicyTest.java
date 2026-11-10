package com.behsazan.corebanking.collateral.workflow;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.behsazan.corebanking.collateral.workflow.CollateralWorkflowPolicy.Action;
import com.behsazan.corebanking.collateral.workflow.CollateralWorkflowPolicy.Kind;

class CollateralWorkflowPolicyTest {
    @Test void everyDomainRequiresRevalidationAtPositiveReviewAsWellAsSubmit() {
        assertEquals(3, Kind.values().length);
        for (Kind kind : Kind.values()) {
            assertNotNull(kind.table);
            assertNotNull(kind.eventTable);
            assertTrue(CollateralWorkflowPolicy.requiresCurrentDomainValidation(Action.SUBMIT), kind.name());
            assertTrue(CollateralWorkflowPolicy.requiresCurrentDomainValidation(Action.REVIEW), kind.name());
            assertFalse(CollateralWorkflowPolicy.requiresCurrentDomainValidation(Action.REJECT), kind.name());
            assertFalse(CollateralWorkflowPolicy.requiresCurrentDomainValidation(Action.WITHDRAW), kind.name());
        }
    }

    @Test void reviewerIsIndependentAndMakerRetainsOwnActions() {
        assertEquals("SUBMITTED", CollateralWorkflowPolicy.next("DRAFT", Action.SUBMIT, true, false));
        assertEquals("WITHDRAWN", CollateralWorkflowPolicy.next("SUBMITTED", Action.WITHDRAW, true, false));
        assertEquals("REVIEWED", CollateralWorkflowPolicy.next("SUBMITTED", Action.REVIEW, false, true));
        assertEquals("REJECTED", CollateralWorkflowPolicy.next("SUBMITTED", Action.REJECT, false, true));
        assertThrows(IllegalArgumentException.class, () -> CollateralWorkflowPolicy.next("SUBMITTED", Action.REVIEW, true, true));
        assertThrows(IllegalArgumentException.class, () -> CollateralWorkflowPolicy.next("SUBMITTED", Action.REVIEW, false, false));
        assertThrows(IllegalStateException.class, () -> CollateralWorkflowPolicy.next("REVIEWED", Action.REVIEW, false, true));
    }

    @Test void reasonAndVersionAreValidatedBeforeMutation() {
        assertEquals("123", CollateralWorkflowPolicy.id("123"));
        assertEquals("1", CollateralWorkflowPolicy.version("1").toPlainString());
        assertThrows(IllegalArgumentException.class, () -> CollateralWorkflowPolicy.version("0"));
        assertThrows(IllegalArgumentException.class, () -> CollateralWorkflowPolicy.reason("  no "));
        assertEquals("review note", CollateralWorkflowPolicy.reason("  review note  "));
    }
}
