package com.chapark.prototype

import com.chapark.app.domain.TripInput
import org.junit.Assert.*
import org.junit.Test

class JourneyTest {
    private fun apply(s:Journey,e:JourneyEvent)=JourneyReducer.apply(s,e)
    @Test fun resetKeepsThemeWhileNewTripAlsoKeepsProfile() {
        val changed=Journey(input=TripInput(2,com.chapark.app.domain.CookingPlan.NONE,true,false),dark=true)
        assertEquals(Journey(dark=true),apply(changed,JourneyEvent.ResetScenario))
        assertEquals(changed.input,apply(changed,JourneyEvent.NewTrip).input)
    }
    @Test fun endToEndRequiresArrivalParkingAndManualChecks() {
        var s=Journey(input=TripInput(cookingPlan=com.chapark.app.domain.CookingPlan.COOKING,schedule=com.chapark.app.domain.TripSchedule("2026-11-12","Asia/Seoul")))
        repeat(3){s=apply(s,JourneyEvent.Next)}
        assertEquals(s,apply(s,JourneyEvent.Next))
        s.departureItems.forEach{s=apply(s,JourneyEvent.Check(it.id))}
        s=apply(s,JourneyEvent.Next)
        assertEquals(4,s.step)
        assertEquals(s,apply(s,JourneyEvent.Next))
        s=apply(s,JourneyEvent.RequestRoute)
        assertEquals(s,apply(s,JourneyEvent.Arrive))
        s=apply(s,JourneyEvent.RouteResponse(true));s=apply(s,JourneyEvent.Arrive)
        assertFalse(s.canSleep)
        assertEquals(s,apply(s,JourneyEvent.Next))
        s=apply(s,JourneyEvent.Park);s=apply(s,JourneyEvent.Next)
        assertEquals(5,s.step)
        assertEquals(s,apply(s,JourneyEvent.Next))
        s.siteItems.forEach{s=apply(s,JourneyEvent.Check(it.id))}
        Journey.SLEEP_IDS.forEach{s=apply(s,JourneyEvent.SleepCheck(it))}
        s=apply(s,JourneyEvent.Next)
        assertFalse(apply(s,JourneyEvent.Finish).completed)
        Journey.EXIT_IDS.forEach{s=apply(s,JourneyEvent.ExitCheck(it))}
        assertTrue(apply(s,JourneyEvent.Finish).completed)
    }
    @Test fun failedRequestRetriesAndLateSuccessIsIgnoredAfterBack() {
        var s=Journey(step=4)
        s=apply(s,JourneyEvent.RequestRoute);s=apply(s,JourneyEvent.RouteResponse(false))
        assertEquals(RoutePhase.FAILED,s.route)
        s=apply(s,JourneyEvent.RequestRoute);s=apply(s,JourneyEvent.Back)
        assertEquals(s,apply(s,JourneyEvent.RouteResponse(true)))
    }
    @Test fun drivingLocksEveryJourneyMutationAndKeepsDraft() {
        val s=Journey(step=3,restricted=true)
        for(e in listOf(JourneyEvent.Next,JourneyEvent.Back,JourneyEvent.Check("water"),JourneyEvent.Place("demo-a"),JourneyEvent.NewTrip,JourneyEvent.Soc(60)))
            assertEquals(s,apply(s,e))
        assertEquals(3,apply(s,JourneyEvent.Driving(false)).step)
    }
    @Test fun batteryAndPlaceChangeActionsAndInvalidateOnlyChangedChecks() {
        val initial=Journey(step=3,checked=setOf("water","charge-review","meal-ready"))
        assertTrue(initial.decisions.any{it.id=="charge-review"})
        assertTrue(initial.decisions.any{it.id=="meal-ready"})
        val high=apply(initial,JourneyEvent.Soc(60))
        assertFalse(high.decisions.any{it.id=="charge-review"})
        assertFalse("charge-review" in high.checked)
        assertTrue("water" in high.checked)
        val other=apply(high,JourneyEvent.Place("demo-a"))
        assertFalse(other.decisions.any{it.id=="meal-ready"})
        assertFalse("meal-ready" in other.checked)
    }
    @Test fun unknownSocDoesNotBecomeZeroAndColdWithoutBagAddsAction() {
        val s=Journey(soc=null,placeId="demo-c",input=TripInput(sleepingBag=false))
        assertTrue(s.decisions.any{it.id=="battery-check"})
        assertFalse(s.decisions.any{it.id=="charge-review"})
        assertTrue(s.decisions.any{it.id=="warmth"})
    }
    @Test fun navigationLossRequiresRetryAndCannotUndoArrival() {
        val moving=Journey(step=4,route=RoutePhase.GUIDING)
        val failed=apply(moving,JourneyEvent.NavigationLost)
        assertEquals(RoutePhase.FAILED,failed.route)
        assertEquals(failed,apply(failed,JourneyEvent.Arrive))
        assertEquals(RoutePhase.REQUESTED,apply(failed,JourneyEvent.RequestRoute).route)
        val arrived=apply(moving,JourneyEvent.Arrive)
        assertEquals(arrived,apply(arrived,JourneyEvent.NavigationLost))
    }
    @Test fun familyQuantityChangesInvalidateOnlyAffectedChecks() {
        var s=Journey(step=3,checked=setOf("water","bedding-count"))
        s=apply(s,JourneyEvent.Conditions(s.input.copy(people=3,endHour=8,batteryThreshold=20)))
        assertEquals(3,s.input.people)
        assertEquals(8,s.input.endHour)
        assertFalse("bedding-count" in s.checked)
        assertTrue("water" in s.checked)
        assertTrue(s.decisions.any{it.id=="extra-sleeping-space"})
        assertFalse(s.decisions.any{it.id=="charge-review"})
        assertTrue(apply(s,JourneyEvent.Soc(19)).decisions.any{it.id=="charge-review"})
    }
    @Test fun departureExcludesSiteChecksAndInvalidConditionsAreRejected() {
        var s=Journey(step=3,input=TripInput(cookingPlan=com.chapark.app.domain.CookingPlan.COOKING,schedule=com.chapark.app.domain.TripSchedule("2026-11-12","Asia/Seoul")))
        assertEquals(s,apply(s,JourneyEvent.Check("site-rules")))
        assertEquals(s,apply(s,JourneyEvent.Conditions(s.input.copy(people=0))))
        assertEquals(s,apply(s,JourneyEvent.Conditions(s.input.copy(batteryThreshold=101))))
        assertEquals(s,apply(s,JourneyEvent.Conditions(s.input.copy(endHour=24))))
        s.departureItems.forEach{s=apply(s,JourneyEvent.Check(it.id))}
        assertTrue(s.canDepart)
        assertFalse(s.siteReady)
        assertEquals(4,apply(s,JourneyEvent.Next).step)
    }
    @Test fun editingConditionsWhileMovingIsRejected() {
        val s=Journey(step=4,route=RoutePhase.GUIDING)
        assertEquals(s,apply(s,JourneyEvent.Soc(60)))
        assertEquals(s,apply(s,JourneyEvent.Place("demo-c")))
    }
}
