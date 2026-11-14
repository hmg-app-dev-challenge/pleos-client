package com.chapark.ivi

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.constants.NaviMode
import ai.pleos.playground.navi.data.RouteStateInfo
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Explicit opt-in, only on an isolated verification AVD. Never called by product code. */
class NaviFixtureResetTest {
    @Test fun cancelOnlyTheVerificationRouteAndObserveIdle() {
        val i=InstrumentationRegistry.getInstrumentation()
        assertEquals("Requires an isolated AVD with a test-owned route","true",InstrumentationRegistry.getArguments().getString("allowTestRouteReset"))
        val ready=CountDownLatch(1)
        val observed=AtomicReference<NaviMode?>(null)
        lateinit var client:NaviHelper
        val listener=object:NaviHelperEventListener {
            override fun onRouteStateInfo(value:RouteStateInfo){observed.set(value.naviMode)}
        }
        i.runOnMainSync{client=NaviHelper(i.targetContext);client.initialize({ready.countDown()},{})}
        try {
            assertTrue("Navi initialization",ready.await(10,TimeUnit.SECONDS))
            i.runOnMainSync{client.addListener(listener);client.cancelRoute()}
            val until=System.currentTimeMillis()+10_000
            while(System.currentTimeMillis()<until && observed.get() !in setOf(NaviMode.SAFE_GUIDING,NaviMode.NOT_WORKING)){
                i.runOnMainSync{client.getRouteStateInfo()};Thread.sleep(250)
            }
            assertTrue("Old route must be absent before offline request: ${observed.get()}",observed.get() in setOf(NaviMode.SAFE_GUIDING,NaviMode.NOT_WORKING))
            i.sendStatus(0,Bundle().apply{putString("stream","TEST_ROUTE_RESET: ${observed.get()}\n")})
        }finally{i.runOnMainSync{runCatching{client.removeListener(listener)};client.release()}}
    }
}
