package com.chapark.ivi

import android.content.Intent
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.chapark.app.domain.*
import com.chapark.ivi.prototype.JourneyStore
import com.chapark.prototype.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Uses the real stage-3 CTA and checks the foreground Activity, not only SDK guidance. */
class NavigationPresentationTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private fun shell(command:String)=ParcelFileDescriptor.AutoCloseInputStream(
        instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use{it.readText()}
    private fun navigationIsForeground():Boolean {
        // Pleos embeds two Activities: the cluster can retain RESUMED while the
        // navigation pane is visible. Assert pane visibility and ChaPark being hidden.
        val tasks=shell("dumpsys activity activities").lineSequence().filter{"* Task{" in it}.toList()
        return tasks.any{"ai.umos.maps.android.navigation.app" in it && "visible=true" in it} &&
            tasks.none{"com.chapark.ivi" in it && "visible=true" in it}
    }
    private fun waitForNavigation(){
        val until=System.currentTimeMillis()+35_000
        while(System.currentTimeMillis()<until && !navigationIsForeground())Thread.sleep(250)
        assertTrue("Stage 3 must bring the navigation Activity forward",navigationIsForeground())
        shell("screencap -p /data/local/tmp/chapark052-navigation.png")
    }
    @Test fun routeStartAndReopenBringNavigationForwardButRestoreDoesNot(){
        val context=instrumentation.targetContext
        val store=JourneyStore(context)
        val original=runBlocking{store.load().first}
        val planned=Journey(step=4,tripId=java.util.UUID.randomUUID().toString(),placeId="camp-nanji",
            input=TripInput(schedule=TripSchedule(java.time.LocalDate.now().toString(),"Asia/Seoul")))
        runBlocking{store.save(planned.copy(checked=planned.departureItems.map{it.id}.toSet()))}
        var scenario:ActivityScenario<IviActivity>?=null
        try {
            scenario=ActivityScenario.launch(IviActivity::class.java)
            compose.waitUntil(20_000){compose.onAllNodesWithText("길 안내 시작").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithTag("journey-next").performClick()
            waitForNavigation()
            context.startActivity(Intent(context,IviActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
            compose.waitUntil(40_000){compose.onAllNodesWithText("내비게이션 열기").fetchSemanticsNodes().isNotEmpty()}
            scenario.recreate()
            compose.waitUntil(40_000){compose.onAllNodesWithText("내비게이션 열기").fetchSemanticsNodes().isNotEmpty()}
            assertFalse("Observer reconnect must not steal focus",navigationIsForeground())
            compose.onNodeWithTag("journey-next").performClick()
            waitForNavigation()
        } finally {
            scenario?.close()
            runBlocking{store.save(original)}
        }
    }
    @Test fun failedConnectionCanOpenNavigationWithoutAReadySdk(){
        val context=instrumentation.targetContext
        val store=JourneyStore(context)
        val original=runBlocking{store.load().first}
        runBlocking{store.save(original.copy(step=4,completed=false,route=RoutePhase.FAILED))}
        var scenario:ActivityScenario<IviActivity>?=null
        try {
            scenario=ActivityScenario.launch(IviActivity::class.java)
            compose.waitUntil(20_000){compose.onAllNodesWithText("내비게이션 앱 열기").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithText("내비게이션 앱 열기").performScrollTo().performClick()
            waitForNavigation()
            assertEquals(RoutePhase.FAILED,runBlocking{store.load().first.route})
        } finally {
            scenario?.close()
            runBlocking{store.save(original)}
        }
    }
}
