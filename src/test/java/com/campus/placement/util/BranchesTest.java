package com.campus.placement.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Branch list")
class BranchesTest {

    @Test
    @DisplayName("known branches are recognised whatever the casing")
    void recognisesKnownBranches() {
        assertTrue(Branches.isKnown("Computer Science"));
        assertTrue(Branches.isKnown("computer science"));
        assertTrue(Branches.isKnown("  INFORMATION TECHNOLOGY  "));
    }

    @Test
    @DisplayName("anything not on the list is refused")
    void refusesUnknownBranches() {
        assertFalse(Branches.isKnown("Aeronautical"));
        assertFalse(Branches.isKnown(""));
        assertFalse(Branches.isKnown(null));
        assertFalse(Branches.isKnown("' OR 1=1 -- "));
    }

    @Test
    @DisplayName("input is stored in the canonical spelling")
    void canonicalisesSpelling() {
        assertEquals("Computer Science", Branches.canonical("computer science"));
        assertEquals("Electronics", Branches.canonical("  electronics "));
        // An unknown value is trimmed but otherwise left alone, so the caller can
        // still see what was typed when it reports the error.
        assertEquals("Aeronautical", Branches.canonical(" Aeronautical "));
    }

    @Test
    @DisplayName("the list cannot be modified by a caller")
    void listIsImmutable() {
        try {
            Branches.ALL.add("Injected");
            throw new AssertionError("the branch list should be immutable");
        } catch (UnsupportedOperationException expected) {
            assertTrue(true);
        }
    }
}
