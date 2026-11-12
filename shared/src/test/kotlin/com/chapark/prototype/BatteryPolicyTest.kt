package com.chapark.prototype

import com.chapark.app.domain.*
import org.junit.Assert.*
import org.junit.Test

class BatteryPolicyTest {
    @Test fun unsupportedRemovesBatteryRulesAndRejectsManualEntry(){
        val state=JourneyReducer.apply(Journey(step=3),JourneyEvent.Battery(null,ReadingStatus.UNSUPPORTED,Source.SDK,null))
        assertFalse(state.batteryVisible)
        assertFalse(state.decisions.any{it.id in setOf("battery-check","charge-review")})
        assertEquals(state,JourneyReducer.apply(state,JourneyEvent.Soc(30,100)))
    }
    @Test fun manualSourceSurvivesFailedRefreshAndYieldsToValidVehicleReading(){
        val state=JourneyReducer.apply(Journey(step=3,soc=null,batteryStatus=ReadingStatus.PERMISSION_DENIED),JourneyEvent.Soc(30,100))
        assertEquals(Source.MANUAL,state.socSource)
        assertEquals(100L,state.socReceivedAtMillis)
        assertEquals(state,JourneyReducer.apply(state,JourneyEvent.Battery(null,ReadingStatus.DISCONNECTED,Source.SDK,null)))
        val refreshed=JourneyReducer.apply(state,JourneyEvent.Battery(80,ReadingStatus.AVAILABLE,Source.SDK,200))
        assertEquals(Source.SDK,refreshed.socSource)
        assertEquals(80,refreshed.soc)
        assertFalse(refreshed.decisions.any{it.id=="charge-review"})
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
