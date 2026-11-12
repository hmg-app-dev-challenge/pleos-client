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
    @Test fun solarEventsKeepCalculatedTimeSeparateFromObservationAndAreNotDuplicated(){
        val base=Journey(step=3,placeId="camp-nanji",input=TripInput(schedule=TripSchedule("2026-11-13","Asia/Seoul")))
        val ready=base.copy(checked=base.departureItems.map{it.id}.toSet())
        val departed=JourneyJournal.apply(ready,JourneyEvent.Next,1000)
        val calculated=departed.records.filter{it.origin=="CALCULATED"}
        assertEquals(setOf("SUNSET","SUNRISE"),calculated.map{it.kind}.toSet())
        assertTrue(calculated.all{it.observedAtMillis==1000L && it.eventAtMillis!!>1000L})
        val back=JourneyJournal.apply(departed,JourneyEvent.Back,2000)
        val repeated=JourneyJournal.apply(back,JourneyEvent.Next,3000)
        assertEquals(2,repeated.records.count{it.origin=="CALCULATED"})
        assertEquals(2,repeated.records.count{it.kind=="PREPARATION_COMPLETED"})
    }

}
