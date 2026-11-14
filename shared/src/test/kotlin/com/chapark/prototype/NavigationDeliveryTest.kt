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
}
