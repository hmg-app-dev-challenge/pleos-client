package com.chapark.ivi

import android.os.Bundle
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import com.chapark.app.data.NavigationPlaces
import com.chapark.ivi.sdk.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

/** Real SDK responses; only elapsed-time expiry is injected, not a network outage. */
class NavigationRecoveryTest {
    @Test fun healthyGuidanceExpiresWhenReceiptsAgeAndReconnectDoesNotResendRoute(){
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        lateinit var session:NavigationSession
        var clockOffset=0L
        val deliveries=mutableListOf<NavigationDelivery>()
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
        fun report(message:String)=instrumentation.sendStatus(0,Bundle().apply{putString("stream","NAVIGATION_RECOVERY: $message\n")})
        fun awaitGuidance(){
            val deadline=SystemClock.elapsedRealtime()+35_000
            while(SystemClock.elapsedRealtime()<deadline && session.state.value.phase !in setOf(NavigationPhase.GUIDING,NavigationPhase.FAILED))Thread.sleep(100)
            assertEquals(session.state.value.toString(),NavigationPhase.GUIDING,session.state.value.phase)
            assertTrue(session.state.value.routeConfirmed)
        }
        instrumentation.runOnMainSync {
            session=NavigationSession(instrumentation.targetContext){SystemClock.elapsedRealtime()+clockOffset}
            scope.launch {session.requestDeliveries.collect{deliveries.add(it)}}
            session.connect(NavigationPlaces.find("camp-nanji")!!,true)
        }
        try {
            awaitGuidance()
            report("real GUIDING observed; keeping public SDK polling alive for 31 seconds")
            val until=SystemClock.elapsedRealtime()+31_000
            while(SystemClock.elapsedRealtime()<until){
                assertEquals(NavigationPhase.GUIDING,session.state.value.phase)
                Thread.sleep(250)
            }
            instrumentation.runOnMainSync {
                session.checkHealth()
                assertEquals(NavigationPhase.GUIDING,session.state.value.phase)
                assertEquals(1,deliveries.size)
                // Advance the receipt clock only. This must revoke stale confirmation immediately.
                clockOffset+=NavigationFreshness.MAX_AGE_MS
                session.checkHealth()
                assertEquals(NavigationPhase.FAILED,session.state.value.phase)
                assertFalse(session.state.value.routeConfirmed)
                assertTrue(session.state.value.message!!.contains("목적지:"))
                session.confirmWaypointByUser()
                assertEquals(NavigationPhase.FAILED,session.state.value.phase)
            }
            report("injected receipt age revoked guidance; observer reconnect follows, no route replacement")
            instrumentation.runOnMainSync {session.connect(NavigationPlaces.find("camp-nanji")!!,false)}
            awaitGuidance()
            instrumentation.runOnMainSync{assertEquals("Observer reconnect must not deliver another route",1,deliveries.size)}
            report("real SDK guidance recovered with exactly one route delivery across the session")
        } finally {
            instrumentation.runOnMainSync{session.close();scope.cancel()}
        }
    }
}
