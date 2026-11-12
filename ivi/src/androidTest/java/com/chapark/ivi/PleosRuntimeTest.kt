package com.chapark.ivi

import android.util.Log
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chapark.ivi.sdk.*
import com.chapark.ivi.safety.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Device smoke probe: terminal failure is recorded, never counted as successful SDK access. */
@RunWith(AndroidJUnit4::class)
class PleosRuntimeTest {
    @Test fun readOnlySdkProbeTerminatesAndReleases() {
        val instrument = InstrumentationRegistry.getInstrumentation()
        lateinit var session: PleosSession
        instrument.runOnMainSync { session=PleosSession(instrument.targetContext); session.start() }
        try {
            val deadline=System.currentTimeMillis()+12_000
            while(System.currentTimeMillis()<deadline &&
                (session.state.value.vehicle==Connection.CONNECTING || session.state.value.navigation==Connection.CONNECTING)) Thread.sleep(100)
            val result=session.state.value
            Log.i("ChaParkProbe", "SDK result: $result")
            instrument.sendStatus(0, Bundle().apply { putString("stream", "SDK_PROBE: $result\n") })
            assertNotEquals(Connection.CONNECTING,result.vehicle)
            assertNotEquals(Connection.CONNECTING,result.navigation)
        } finally { instrument.runOnMainSync { session.close() } }
        assertNull(session.state.value.rawSoc)
        assertEquals(Connection.STOPPED,session.state.value.vehicle)
    }

    @Test fun missingCarLibraryKeepsDetailsLocked() {
        val instrument=InstrumentationRegistry.getInstrumentation()
        if(instrument.targetContext.packageManager.systemSharedLibraryNames?.contains("android.car")==true) return
        instrument.runOnMainSync {
            val monitor=createUxMonitor(instrument.targetContext)
            monitor.start()
            assertEquals(UxMode.UNKNOWN,monitor.state.value.mode)
            assertFalse(monitor.state.value.allowsDetailedInteraction)
            monitor.close()
        }
    }
}
