package com.chapark.prototype

import com.chapark.app.domain.*
import org.junit.Assert.*
import org.junit.Test

class BatteryPolicyTest {
    @Test fun unsupportedAllowsExplicitManualEntryWithoutChangingCapability(){
        val state=JourneyReducer.apply(Journey(step=3),JourneyEvent.Battery(null,ReadingStatus.UNSUPPORTED,Source.SDK,null))
        assertTrue(state.batteryVisible)
        assertTrue(state.decisions.any{it.id=="battery-check"})
        val manual=JourneyReducer.apply(state,JourneyEvent.Soc(30,100))
        assertEquals(BatteryCapability.UNSUPPORTED,manual.batteryCapability)
        assertEquals(Source.MANUAL,manual.socSource)
        assertTrue(manual.needsChargingReview)
    }
    @Test fun manualSourceSurvivesFailedRefreshAndYieldsToValidVehicleReading(){
        val state=JourneyReducer.apply(Journey(step=3,soc=null,batteryStatus=ReadingStatus.PERMISSION_DENIED),JourneyEvent.Soc(30,100))
        assertEquals(Source.MANUAL,state.socSource)
        assertEquals(100L,state.socReceivedAtMillis)
        assertEquals(state.copy(vehicleBatteryStatus=ReadingStatus.DISCONNECTED),JourneyReducer.apply(state,JourneyEvent.Battery(null,ReadingStatus.DISCONNECTED,Source.SDK,null)))
        val refreshed=JourneyReducer.apply(state,JourneyEvent.Battery(80,ReadingStatus.AVAILABLE,Source.SDK,200))
        assertEquals(Source.SDK,refreshed.socSource)
        assertEquals(80,refreshed.soc)
        assertFalse(refreshed.decisions.any{it.id=="charge-review"})
    }
    @Test fun changingThresholdRequiresFreshExplicitConfirmation() {
        val state=Journey(step=3,checked=setOf("battery-threshold","drinking-water"))
        val next=JourneyReducer.apply(state,JourneyEvent.Conditions(state.input.copy(batteryThreshold=40)))
        assertFalse("battery-threshold" in next.checked)
        assertTrue("drinking-water" in next.checked)
        assertTrue(next.departureItems.any{it.id=="battery-threshold" && it.required})
    }
    @Test fun invalidAndRestrictedInputsCannotChangeTrip(){
        val state=Journey(step=3)
        assertEquals(state,JourneyReducer.apply(state,JourneyEvent.Soc(101,100)))
        val locked=state.copy(restricted=true)
        assertEquals(locked,JourneyReducer.apply(locked,JourneyEvent.Soc(30,100)))
        assertEquals(locked,JourneyReducer.apply(locked,JourneyEvent.ResetScenario))
        val update=JourneyReducer.apply(locked,JourneyEvent.Battery(50,ReadingStatus.AVAILABLE,Source.SDK,100))
        assertEquals(50,update.soc)
        assertTrue(update.restricted)
    }
}
