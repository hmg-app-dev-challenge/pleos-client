package com.chapark.ivi

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.chapark.ivi.safety.*
import org.junit.Assert.*
import org.junit.Test

class CarUxMonitorTest {
    @Test fun readsParkingAndUxIndependentlyAndLocksOnClose() {
        val i=InstrumentationRegistry.getInstrumentation()
        lateinit var monitor:CarUxMonitor
        i.runOnMainSync{monitor=CarUxMonitor(i.targetContext);monitor.start()}
        try {
            val until=System.currentTimeMillis()+10_000
            while(System.currentTimeMillis()<until && monitor.state.value.parking==ParkingSignal.UNKNOWN)Thread.sleep(100)
            val value=monitor.state.value
            i.sendStatus(0,Bundle().apply{putString("stream","VEHICLE_GATE: $value\n")})
            val expected=InstrumentationRegistry.getArguments().getString("expectedParking") ?: "PARK"
            assertEquals(ParkingSignal.valueOf(expected),value.parking)
            assertEquals(value.mode==UxMode.AVAILABLE && value.parking==ParkingSignal.PARK,value.allowsDetailedInteraction)
        } finally {i.runOnMainSync{monitor.close()}}
        assertFalse(monitor.state.value.allowsDetailedInteraction)
        assertEquals(ParkingSignal.UNKNOWN,monitor.state.value.parking)
    }
}
