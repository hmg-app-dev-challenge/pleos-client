package com.chapark.prototype
import org.junit.Assert.*
import org.junit.Test
class NavigationDeliveryTest {
    @Test fun callReturnDoesNotImplyGuidanceAndDeduplicates() {
        val requested=Journey(step=4,route=RoutePhase.REQUESTED)
        val next=JourneyJournal.recordNavigationDelivery(requested,"attempt-1",100,true)
        assertEquals(RoutePhase.REQUESTED,next.route)
        assertEquals("ROUTE_CALL_RETURNED",next.records.single().kind)
        assertEquals(next,JourneyJournal.recordNavigationDelivery(next,"attempt-1",200,true))
        assertEquals(next,JourneyJournal.recordNavigationDelivery(next,"attempt-1",200,false))
        val failed=JourneyJournal.recordNavigationDelivery(next,"attempt-2",300,false,"IllegalStateException")
        assertEquals("ROUTE_CALL_FAILED",failed.records.last().kind)
        assertEquals(RoutePhase.REQUESTED,failed.route)
    }
    @Test fun completedAndOtherStagesIgnoreLateDelivery() {
        for(state in listOf(Journey(step=3),Journey(step=6,completed=true)))
            assertEquals(state,JourneyJournal.recordNavigationDelivery(state,"late",100,true))
    }
    @Test fun lostGuidanceBlocksArrivalAndRetryPreservesThePreparedTrip() {
        val input=com.chapark.app.domain.TripInput(people=3,
            cookingPlan=com.chapark.app.domain.CookingPlan.SIMPLE,
            schedule=com.chapark.app.domain.TripSchedule("2026-11-14","Asia/Seoul"))
        val original=Journey(step=4,tripId="retained-trip",placeId="camp-nanji",input=input,
            chargerId="selected-charger",checked=setOf("drinking-water","bedding-set"),
            route=RoutePhase.GUIDING)
        val lost=JourneyJournal.apply(original,JourneyEvent.NavigationLost,100,"SDK")
        assertEquals(RoutePhase.FAILED,lost.route)
        assertEquals(lost,JourneyJournal.apply(lost,JourneyEvent.Arrive,200,"USER"))
        val retry=JourneyJournal.apply(lost,JourneyEvent.RequestRoute,300,"USER")
        assertEquals(RoutePhase.REQUESTED,retry.route)
        val recovered=JourneyJournal.apply(retry,JourneyEvent.RouteResponse(true),400,"SDK")
        assertEquals(RoutePhase.GUIDING,recovered.route)
        assertEquals(original.input,recovered.input)
        assertEquals(original.placeId,recovered.placeId)
        assertEquals(original.chargerId,recovered.chargerId)
        assertEquals(original.checked,recovered.checked)
        assertEquals(original.tripId,recovered.tripId)
        assertFalse(recovered.parked)
        assertFalse(recovered.completed)
        assertEquals(listOf("ROUTE_FAILED","ROUTE_REQUESTED","GUIDANCE_OBSERVED"),recovered.records.map{it.kind})
    }
}
