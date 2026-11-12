package com.chapark.ivi.safety

import org.junit.Assert.*
import org.junit.Test

class UxPolicyTest {
    @Test fun missingSignalLocksDetailedInteraction() {
        assertEquals(UxMode.UNKNOWN, UxPolicy.evaluate(null, 0))
        assertEquals(UxMode.UNKNOWN, UxPolicy.evaluate(false, null))
        assertFalse(UxSnapshot().allowsDetailedInteraction)
    }
    @Test fun eitherOptimizationOrAnyRestrictionLocks() {
        assertEquals(UxMode.RESTRICTED, UxPolicy.evaluate(true, 0))
        for (bit in 0..15) assertEquals(UxMode.RESTRICTED, UxPolicy.evaluate(false, 1 shl bit))
    }
    @Test fun onlyKnownUnrestrictedSignalAllowsDetails() {
        assertEquals(UxMode.AVAILABLE, UxPolicy.evaluate(false, 0))
        assertTrue(UxSnapshot(UxMode.AVAILABLE).allowsDetailedInteraction)
    }
}
