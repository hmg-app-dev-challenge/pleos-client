package com.chapark.ivi.sdk

import com.chapark.ivi.sdk.NavigationFreshness.Reading.*
import org.junit.Assert.*
import org.junit.Test

class NavigationFreshnessTest {
    @Test fun destinationAndRouteAreBothRequiredBeforeConfirmation(){
        val observations=NavigationFreshness(false)
        assertFalse(observations.isFresh(0))
        observations.observe(ROUTE,0)
        assertFalse(observations.isFresh(0))
        observations.observe(DESTINATION,0)
        assertTrue(observations.isFresh(29_999))
        assertFalse(observations.isFresh(30_000))
    }
    @Test fun frequentDestinationResponsesCannotHideLostRouteResponses(){
        val observations=NavigationFreshness(false)
        observations.observe(ROUTE,1_000)
        observations.observe(DESTINATION,30_999)
        assertTrue(observations.isFresh(30_999))
        assertFalse(observations.isFresh(31_000))
    }
    @Test fun frequentRouteResponsesCannotHideLostDestinationResponses(){
        val observations=NavigationFreshness(false)
        observations.observe(DESTINATION,0)
        observations.observe(ROUTE,30_000)
        assertFalse(observations.isFresh(30_000))
    }
    @Test fun waypointIsRequiredOnlyForChargingRoutesAndMustStayFresh(){
        val observations=NavigationFreshness(true)
        observations.observe(ROUTE,0);observations.observe(DESTINATION,0)
        assertFalse(observations.isFresh(0))
        observations.observe(WAYPOINT,0)
        assertTrue(observations.isFresh(0))
        observations.observe(ROUTE,30_000);observations.observe(DESTINATION,30_000)
        assertFalse(observations.isFresh(30_000))
        observations.observe(WAYPOINT,30_000)
        assertTrue(observations.isFresh(30_000))
    }
    @Test fun delayedOlderEventsDoNotReplaceNewerReceiptsAndClockRegressionIsRejected(){
        val observations=NavigationFreshness(false)
        observations.observe(ROUTE,20_000);observations.observe(DESTINATION,20_000)
        observations.observe(ROUTE,0)
        assertTrue(observations.isFresh(40_000))
        assertFalse(observations.isFresh(19_999))
        assertFalse(NavigationFreshness(false).isFresh(40_000))
    }
}
