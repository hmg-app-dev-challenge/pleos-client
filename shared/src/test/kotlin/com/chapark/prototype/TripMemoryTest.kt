package com.chapark.prototype

import com.chapark.app.domain.*
import org.junit.Assert.*
import org.junit.Test

class TripMemoryTest {
    private fun departure():Journey {
        val draft=Journey(step=3,placeId="camp-nanji",soc=80,socSource=Source.SDK,
            input=TripInput(schedule=TripSchedule("2026-11-14","Asia/Seoul")))
        return JourneyJournal.apply(draft.copy(checked=draft.departureItems.map{it.id}.toSet()),JourneyEvent.Next,100)
    }

    @Test fun departureContextAndCountsRemainFrozenAfterBatteryOrCatalogContextChanges() {
        val left=departure()
        val context=left.records.single{it.kind=="PLACE_CONTEXT"}
        assertEquals(listOf("강"),context.placeMemory!!.sceneryTags)
        assertTrue(context.placeMemory.hasUndatedInfo)
        val preparation=left.records.single{it.kind=="PREPARATION_COMPLETED"}
        assertEquals(left.departureProgress.total,preparation.preparationRequired)
        assertEquals(preparation.preparationRequired,preparation.preparationCompleted)
        val changed=JourneyJournal.apply(left,JourneyEvent.Battery(20,ReadingStatus.AVAILABLE,Source.SDK,200),200)
        assertEquals(left.records,changed.records)
        assertEquals(80,changed.records.single{it.kind=="PREPARATION_COMPLETED"}.soc)
    }

    @Test fun checkoutNeverPromotesStaleReadingAndCompletionIsIdempotent() {
        val state=departure().copy(step=6,route=RoutePhase.ARRIVED,parked=true,sleepReady=true,
            sleepChecks=Journey.SLEEP_IDS,exitChecks=Journey.EXIT_IDS,batteryStatus=ReadingStatus.STALE)
        val done=JourneyJournal.apply(state,JourneyEvent.Finish,300)
        assertTrue(done.completed)
        assertNull(done.records.last().soc)
        assertEquals(ReadingStatus.STALE,done.records.last().readingStatus)
        assertEquals(done,JourneyJournal.apply(done,JourneyEvent.Finish,400))
        assertTrue(JourneyJournal.apply(done,JourneyEvent.NewTrip,400).records.isEmpty())
    }

    @Test fun requestFailureRetryAndGuidanceRemainDistinct() {
        var state=departure()
        state=JourneyJournal.apply(state,JourneyEvent.RequestRoute,200)
        assertEquals("ROUTE_REQUESTED",state.records.last().kind)
        assertEquals("경유 없음",state.records.last().label)
        state=JourneyJournal.apply(state,JourneyEvent.NavigationLost,300,"SDK")
        assertEquals("ROUTE_FAILED",state.records.last().kind)
        state=JourneyJournal.apply(state,JourneyEvent.RequestRoute,400)
        state=JourneyJournal.apply(state,JourneyEvent.RouteResponse(true),500,"SDK")
        assertEquals(2,state.records.count{it.kind=="ROUTE_REQUESTED"})
        assertEquals(1,state.records.count{it.kind=="GUIDANCE_OBSERVED"})
    }
}
