package com.behsazan.corebanking.collateral.workflow;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.behsazan.corebanking.collateral.workflow.CollateralR3WorkflowPolicy.Action;
import com.behsazan.corebanking.collateral.workflow.CollateralR3WorkflowPolicy.Kind;

class CollateralR3WorkflowPolicyTest {
    @Test void everyDomainRequiresRevalidationAtPositiveReviewAsWellAsSubmit() {
        assertEquals(3, Kind.values().length);
        for (Kind kind : Kind.values()) {
            assertNotNull(kind.table);
            assertNotNull(kind.eventTable);
            assertTrue(CollateralR3WorkflowPolicy.requiresCurrentDomainValidation(Action.SUBMIT), kind.name());
            assertTrue(CollateralR3WorkflowPolicy.requiresCurrentDomainValidation(Action.REVIEW), kind.name());
            assertFalse(CollateralR3WorkflowPolicy.requiresCurrentDomainValidation(Action.REJECT), kind.name());
            assertFalse(CollateralR3WorkflowPolicy.requiresCurrentDomainValidation(Action.WITHDRAW), kind.name());
        }
    }

    @Test void reviewerIsIndependentAndMakerRetainsOwnActions() {
        assertEquals("SUBMITTED", CollateralR3WorkflowPolicy.next("DRAFT", Action.SUBMIT, true, false));
        assertEquals("WITHDRAWN", CollateralR3WorkflowPolicy.next("SUBMITTED", Action.WITHDRAW, true, false));
        assertEquals("REVIEWED", CollateralR3WorkflowPolicy.next("SUBMITTED", Action.REVIEW, false, true));
        assertEquals("REJECTED", CollateralR3WorkflowPolicy.next("SUBMITTED", Action.REJECT, false, true));
        assertThrows(IllegalArgumentException.class, () -> CollateralR3WorkflowPolicy.next("SUBMITTED", Action.REVIEW, true, true));
        assertThrows(IllegalArgumentException.class, () -> CollateralR3WorkflowPolicy.next("SUBMITTED", Action.REVIEW, false, false));
        assertThrows(IllegalStateException.class, () -> CollateralR3WorkflowPolicy.next("REVIEWED", Action.REVIEW, false, true));
    }

    @Test void reasonAndVersionAreValidatedBeforeMutation() {
        assertEquals("123", CollateralR3WorkflowPolicy.id("123"));
        assertEquals("1", CollateralR3WorkflowPolicy.version("1").toPlainString());
        assertThrows(IllegalArgumentException.class, () -> CollateralR3WorkflowPolicy.version("0"));
        assertThrows(IllegalArgumentException.class, () -> CollateralR3WorkflowPolicy.reason("  no "));
        assertEquals("review note", CollateralR3WorkflowPolicy.reason("  review note  "));
    }
}
