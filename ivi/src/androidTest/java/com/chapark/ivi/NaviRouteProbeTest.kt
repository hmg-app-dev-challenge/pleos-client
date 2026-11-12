package com.chapark.ivi

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.chapark.app.data.NavigationPlaces
import com.chapark.ivi.sdk.*
import org.junit.Test
import org.junit.Assert.*

/** Real SDK integration; it may change the emulator's active navigation route. */
class NaviRouteProbeTest {
    @Test fun selectedCampgroundStartsRealGuidanceAndOpensNavigation() {
        val i=InstrumentationRegistry.getInstrumentation()
        lateinit var session:NavigationSession
        i.runOnMainSync {session=NavigationSession(i.targetContext);session.connect(NavigationPlaces.all[1],true)}
        try {
            val until=System.currentTimeMillis()+35_000
            while(System.currentTimeMillis()<until && session.state.value.phase !in setOf(NavigationPhase.GUIDING,NavigationPhase.FAILED))Thread.sleep(100)
            i.sendStatus(0,Bundle().apply{putString("stream","NAVI_ROUTE: ${session.state.value}\n")})
            assertEquals(NavigationPhase.GUIDING,session.state.value.phase)
            assertTrue(session.state.value.routeConfirmed)
            // Reconnect observes the ongoing route without starting a replacement route.
            i.runOnMainSync {session.close();session.connect(NavigationPlaces.all[1],false)}
            val reconnectUntil=System.currentTimeMillis()+35_000
            while(System.currentTimeMillis()<reconnectUntil && session.state.value.phase !in setOf(NavigationPhase.GUIDING,NavigationPhase.FAILED))Thread.sleep(100)
            assertEquals(NavigationPhase.GUIDING,session.state.value.phase)
            assertTrue(session.state.value.routeConfirmed)
            i.sendStatus(0,Bundle().apply{putString("stream","NAVI_RECONNECT: ${session.state.value}\n")})
            i.runOnMainSync {session.openScreen()}
            Thread.sleep(1500)
        } finally {i.runOnMainSync{session.close()}}
    }
}
