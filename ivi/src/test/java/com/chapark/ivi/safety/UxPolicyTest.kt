package com.chapark.ivi.safety

import org.junit.Assert.*
import org.junit.Test

class UxPolicyTest {
    @Test fun lateUxResponseCanRecoverUsingAnEarlierParkingSignal() {
        val parked=UxSnapshot(parking=ParkingSignal.PARK)
        val waiting=UxPolicy.waitingForUx(parked)
        assertFalse(waiting.allowsDetailedInteraction)
        assertEquals(ParkingSignal.PARK,waiting.parking)
        val observed=waiting.copy(mode=UxPolicy.evaluate(false,0),restrictions=0)
        assertTrue(observed.allowsDetailedInteraction)
        val unknownGear=UxPolicy.waitingForUx(UxSnapshot()).copy(mode=UxPolicy.evaluate(false,0))
        assertFalse(unknownGear.allowsDetailedInteraction)
    }
    @Test fun aDelayedTimerCannotReplaceAnAlreadyKnownUxDecision() {
        for(mode in listOf(UxMode.AVAILABLE,UxMode.RESTRICTED)) {
            val observed=UxSnapshot(mode,if(mode==UxMode.AVAILABLE)0 else 80,parking=ParkingSignal.PARK)
            assertEquals(observed,UxPolicy.waitingForUx(observed))
        }
    }
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
        assertFalse(UxSnapshot(UxMode.AVAILABLE).allowsDetailedInteraction)
        assertFalse(UxSnapshot(UxMode.AVAILABLE, parking=ParkingSignal.NOT_PARK).allowsDetailedInteraction)
        assertTrue(UxSnapshot(UxMode.AVAILABLE, parking=ParkingSignal.PARK).allowsDetailedInteraction)
        for (mode in listOf(UxMode.UNKNOWN, UxMode.RESTRICTED))
            assertFalse(UxSnapshot(mode, parking=ParkingSignal.PARK).allowsDetailedInteraction)
    }
}
