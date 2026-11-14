package com.chapark.prototype

import org.junit.Assert.*
import org.junit.Test

class OvernightChecklistTest {
    @Test fun everyCurrentSleepAndExitItemIsRequired() {
        val sleeping=Journey(step=5,route=RoutePhase.ARRIVED,parked=true,checked=setOf("site-rules"),sleepChecks=Journey.SLEEP_IDS)
        assertEquals(6,Journey.SLEEP_IDS.size);assertEquals(5,Journey.EXIT_IDS.size)
        for(id in Journey.SLEEP_IDS) {
            val missing=sleeping.copy(sleepChecks=Journey.SLEEP_IDS-id)
            assertEquals(missing,JourneyReducer.apply(missing,JourneyEvent.Next))
        }
        val checkout=JourneyReducer.apply(sleeping,JourneyEvent.Next).copy(exitChecks=Journey.EXIT_IDS)
        for(id in Journey.EXIT_IDS)assertFalse(JourneyReducer.apply(checkout.copy(exitChecks=Journey.EXIT_IDS-id),JourneyEvent.Finish).completed)
        assertTrue(JourneyReducer.apply(checkout,JourneyEvent.Finish).completed)
        assertFalse(JourneyReducer.apply(checkout.copy(sleepChecks=emptySet()),JourneyEvent.Finish).completed)
    }

    @Test fun historicalVersionIsNotRetrofittedButNextTripUsesCurrentRequirements() {
        val old=Journey(step=6,completed=true,checklistVersion=1,route=RoutePhase.ARRIVED,parked=true,
            sleepReady=true,sleepChecks=JourneyChecklists.legacySleep.keys,exitChecks=JourneyChecklists.legacyExit.keys)
        assertTrue(old.canFinish)
        assertEquals(old,JourneyReducer.apply(old,JourneyEvent.ExitCheck("left-behind")))
        val next=JourneyReducer.apply(old,JourneyEvent.NewTrip)
        assertEquals(2,next.checklistVersion);assertFalse(next.completed);assertTrue(next.exitChecks.isEmpty())
    }

    @Test fun newChecksAreUserObservationsAndDrivingCannotChangeThem() {
        val state=Journey(step=5,route=RoutePhase.ARRIVED,parked=true)
        val checked=JourneyJournal.apply(state,JourneyEvent.SleepCheck("door-lock"),100)
        assertEquals("문 잠금 직접 확인",checked.records.single().label)
        assertEquals("USER",checked.records.single().origin)
        assertEquals(state.copy(restricted=true),JourneyJournal.apply(state.copy(restricted=true),JourneyEvent.SleepCheck("door-lock"),100))
    }
}
