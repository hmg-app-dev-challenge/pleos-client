package com.chapark.prototype

import com.chapark.app.data.ChargingPlaces
import com.chapark.app.domain.*
import org.junit.Assert.*
import org.junit.Test

class ChargingCandidateTest {
    private val low=Journey(step=3,placeId="camp-nanji",soc=34,input=TripInput(batteryThreshold=35))
    @Test fun onlyLinkedCandidatesCanBeNewlySelectedBelowThreshold() {
        assertEquals(listOf("emart-mapo"),ChargingPlaces.forPlace("camp-nanji").map{it.id})
        for(id in listOf("camp-mongsanpo","camp-gangdong","camp-jungnang","unknown")) {
            assertTrue(ChargingPlaces.forPlace(id).isEmpty())
            val state=low.copy(placeId=id)
            assertEquals(state,JourneyReducer.apply(state,JourneyEvent.Charger("emart-mapo")))
        }
        assertEquals("emart-mapo",JourneyReducer.apply(low,JourneyEvent.Charger("emart-mapo")).chargerId)
        for(state in listOf(low.copy(soc=35),low.copy(soc=36),low.copy(soc=null),low.copy(batteryStatus=ReadingStatus.STALE),low.copy(batteryStatus=ReadingStatus.UNSUPPORTED))) {
            assertFalse(state.needsChargingReview)
            assertEquals(state,JourneyReducer.apply(state,JourneyEvent.Charger("emart-mapo")))
        }
    }

    @Test fun conditionChangesKeepExistingSelectionButChangedDestinationRequiresReview() {
        val selected=JourneyReducer.apply(low.copy(checked=setOf("charge-review","drinking-water")),JourneyEvent.Charger("emart-mapo"))
        assertFalse("charge-review" in selected.checked)
        assertTrue("drinking-water" in selected.checked)
        val reviewed=selected.copy(checked=selected.checked+"charge-review")
        val changed=JourneyReducer.apply(reviewed,JourneyEvent.Place("camp-mongsanpo"))
        assertEquals("emart-mapo",changed.chargerId);assertFalse("charge-review" in changed.checked)
        val high=JourneyReducer.apply(selected,JourneyEvent.Soc(80))
        assertEquals("emart-mapo",high.chargerId);assertFalse(high.needsChargingReview)
    }

    @Test fun explicitRemovalBeforeRouteIsAllowedButGuidanceAndDrivingAreProtected() {
        for(route in listOf(RoutePhase.NONE,RoutePhase.FAILED)) {
            val state=low.copy(step=4,route=route,chargerId="emart-mapo")
            val cleared=JourneyJournal.apply(state,JourneyEvent.Charger(null),100)
            assertNull(cleared.chargerId);assertEquals(RoutePhase.NONE,cleared.route)
            assertEquals("CHARGING_PLAN",cleared.records.single().kind)
        }
        for(state in listOf(low.copy(step=4,route=RoutePhase.GUIDING,chargerId="emart-mapo"),low.copy(restricted=true,chargerId="emart-mapo")))
            assertEquals(state,JourneyReducer.apply(state,JourneyEvent.Charger(null)))
    }
}
