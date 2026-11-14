package com.chapark.ivi

import android.os.ParcelFileDescriptor
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.chapark.app.domain.*
import com.chapark.ivi.prototype.*
import com.chapark.prototype.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class StarViewingTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private val store get()=JourneyStore(instrumentation.targetContext)
    private val stay get()=Journey(step=5,tripId=java.util.UUID.randomUUID().toString(),placeId="camp-nanji",
        route=RoutePhase.ARRIVED,parked=true,sleepChecks=setOf(Journey.SLEEP_IDS.first()),
        input=TripInput(schedule=TripSchedule(java.time.LocalDate.now().toString(),"Asia/Seoul")))
    private fun waitFor(tag:String)=compose.waitUntil(20_000){compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()}
    private fun capture(name:String){
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
            "screencap -p /data/local/tmp/chapark054-$name.png")).use{it.readBytes()}
    }
    private fun assertVisibleStars(){
        val field=compose.onNodeWithTag("star-field").assertIsDisplayed()
        val bounds=field.getUnclippedBoundsInRoot()
        assertTrue("The sky must have a useful visible height",bounds.bottom-bounds.top>=250.dp)
        val pixels=field.captureToImage().toPixelMap()
        var bright=0;var dark=0;var samples=0
        val occupied=mutableSetOf<Pair<Int,Int>>()
        for(y in 0 until pixels.height step 2)for(x in 0 until pixels.width step 2){
            val color=pixels[x,y];val level=(color.red+color.green+color.blue)/3
            samples++
            if(level<.2f)dark++
            if(level>.6f){bright++;occupied.add((x*12/pixels.width) to (y*8/pixels.height))}
        }
        assertTrue("Visible stars must be rendered, not only a dark theme: $bright bright samples",bright>150)
        assertTrue("Stars must cover the sky, not four tiny decoration dots: ${occupied.size} tiles",occupied.size>=48)
        assertTrue("Night scene must remain predominantly dark",dark.toFloat()/samples>.85f)
        compose.onNodeWithTag("star-view-exit").assertIsDisplayed().assertIsEnabled()
    }
    @Test fun actualSelectionRendersStarsRestoresAfterRecreationAndReturnsWithoutLosingChecks(){
        val original=runBlocking{store.load().first}
        val fixture=stay
        runBlocking{store.save(fixture)}
        var scenario:ActivityScenario<IviActivity>?=null
        try {
            scenario=ActivityScenario.launch(IviActivity::class.java)
            waitFor("journey-next")
            compose.onNodeWithText("별 보기").performScrollTo().performClick()
            waitFor("star-view-screen")
            assertVisibleStars();capture("stars-wide")
            compose.onNodeWithTag("journey-next").assertDoesNotExist()
            compose.waitUntil(10_000){runBlocking{store.load().first.restMode=="STARS"}}
            scenario.recreate();waitFor("star-view-screen");assertVisibleStars()
            assertEquals(fixture.sleepChecks,runBlocking{store.load().first.sleepChecks})
            scenario.onActivity{it.onBackPressedDispatcher.onBackPressed()}
            waitFor("journey-next")
            compose.onNodeWithTag("star-view-screen").assertDoesNotExist()
            compose.onNodeWithText("별 보기").performScrollTo().performClick()
            waitFor("star-view-screen")
            compose.onNodeWithTag("star-view-exit").performClick()
            waitFor("journey-next")
            compose.waitUntil(10_000){runBlocking{store.load().first.restMode=="REST"}}
            val returned=runBlocking{store.load().first}
            assertEquals(fixture.tripId,returned.tripId)
            assertEquals(fixture.sleepChecks,returned.sleepChecks)
            assertEquals(fixture.input,returned.input)
            assertEquals(5,returned.step);assertFalse(returned.dark)
            capture("returned-to-preparation")
        } finally {scenario?.close();runBlocking{store.save(original)}}
    }
    @Test fun bothThemesAndLargeTextShowStarsAndDrivingImmediatelyHidesTheScene(){
        val original=runBlocking{store.load().first}
        val scenario=ActivityScenario.launch(IviActivity::class.java)
        var state by mutableStateOf(stay)
        try {
            scenario.onActivity{it.setContent{
                val density=LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(density,1.3f)) {
                    Box(Modifier.requiredSize(800.dp,1000.dp).clipToBounds()) {
                        JourneyScreen(JourneyUi(state,loaded=true),{state=JourneyReducer.apply(state,it)})
                    }
                }
            }}
            for(dark in listOf(false,true)) {
                compose.runOnIdle{state=stay.copy(dark=dark)}
                compose.onNodeWithText("별 보기").performScrollTo().performClick()
                assertVisibleStars();capture("stars-narrow-${if(dark)"dark" else "light"}")
                compose.runOnIdle{state=state.copy(restricted=true)}
                compose.onNodeWithTag("star-view-screen").assertDoesNotExist()
                compose.onNodeWithTag("star-field").assertDoesNotExist()
                compose.onNodeWithText("정차 후 이어서\n확인할 수 있어요").assertExists()
                compose.runOnIdle{state=state.copy(restricted=false)}
                assertVisibleStars()
                compose.onNodeWithTag("star-view-exit").performClick()
                compose.onNodeWithTag("star-view-screen").assertDoesNotExist()
                compose.runOnIdle{assertEquals("REST",state.restMode);assertEquals(dark,state.dark)}
            }
        } finally {scenario.close();runBlocking{store.save(original)}}
    }
}
