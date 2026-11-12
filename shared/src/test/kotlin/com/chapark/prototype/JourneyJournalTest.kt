package com.chapark.prototype

import com.chapark.app.domain.*
import org.junit.Assert.*
import org.junit.Test

class JourneyJournalTest {
    @Test fun checksRecordAcceptedChangesAndKeepOldMeaning(){
        var s=Journey(step=3)
        s=JourneyJournal.apply(s,JourneyEvent.Check("bedding-count"),100)
        assertEquals(true,s.records.single().confirmed)
        val changed=JourneyJournal.apply(s,JourneyEvent.Conditions(s.input.copy(people=3)),200)
        assertFalse("bedding-count" in changed.checked)
        assertTrue(changed.records.single().label!!.contains("1명"))
        assertEquals(changed,JourneyJournal.apply(changed,JourneyEvent.Check("missing"),300))
    }
    @Test fun reconnectDoesNotInventAnotherDepartureAndArrivalOriginIsPreserved(){
        var s=Journey(step=4,route=RoutePhase.REQUESTED)
        s=JourneyJournal.apply(s,JourneyEvent.RouteResponse(true),100,"SDK")
        s=JourneyJournal.apply(s.copy(route=RoutePhase.REQUESTED),JourneyEvent.RouteResponse(true),200,"SDK")
        assertEquals(1,s.records.size)
        s=JourneyJournal.apply(s,JourneyEvent.Arrive,300,"SDK")
        assertEquals("SDK",s.records.last().origin)
        assertEquals(s,JourneyJournal.apply(s,JourneyEvent.Arrive,400))
    }
    @Test fun completionSnapshotsValueSourceAndTimeWithoutInventingSensorTime(){
        val s=Journey(step=6,route=RoutePhase.ARRIVED,parked=true,sleepReady=true,
            exitChecks=Journey.EXIT_IDS,soc=20,socSource=Source.DEMO)
        val done=JourneyJournal.apply(s,JourneyEvent.Finish,1000)
        assertEquals(20,done.records.single().soc)
        assertEquals(Source.DEMO,done.records.single().socSource)
        assertNull(done.records.single().socReceivedAtMillis)
        assertEquals(1000L,done.records.single().observedAtMillis)
        assertEquals(done,JourneyJournal.apply(done,JourneyEvent.Finish,2000))
    }
}
