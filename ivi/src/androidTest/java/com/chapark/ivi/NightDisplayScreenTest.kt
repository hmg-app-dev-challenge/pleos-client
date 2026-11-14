package com.chapark.ivi

import androidx.compose.runtime.*
import androidx.activity.compose.setContent
import androidx.test.core.app.ActivityScenario
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.TextLayoutResult
import com.chapark.app.domain.*
import com.chapark.ivi.prototype.*
import com.chapark.prototype.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class NightDisplayScreenTest {
    @get:Rule val compose=createEmptyComposeRule()
    private lateinit var scenario:ActivityScenario<IviActivity>
    private var original:Journey?=null
    private val store get()=JourneyStore(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext)
    @org.junit.Before fun launch(){
        original=kotlinx.coroutines.runBlocking{store.load().first}
        scenario=ActivityScenario.launch(IviActivity::class.java)
    }
    @org.junit.After fun restore(){
        scenario.close()
        original?.let{kotlinx.coroutines.runBlocking{store.save(it)}}
    }
    private fun content(block:@Composable ()->Unit){scenario.onActivity{it.setContent(content=block)}}
    private val stay=Journey(step=5,placeId="camp-nanji",route=RoutePhase.ARRIVED,parked=true,
        input=TripInput(schedule=TripSchedule("2026-11-14","Asia/Seoul")))
    private fun capture(name:String) {
        val context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val folder=java.io.File(context.filesDir,"verification").apply{mkdirs()}
        java.io.File(folder,"night-$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }
    }
    @Test fun sunsetSunriseAndDrivingChangePresentationWithoutChangingJourney() {
        var state by mutableStateOf(stay)
        val window=NightDisplayPolicy.window(stay)!!
        var now by mutableStateOf(window.sunset.minusSeconds(1))
        content{JourneyScreen(JourneyUi(state,loaded=true),{},now=now)}
        compose.onNodeWithTag("standard-screen").assertExists()
        compose.runOnIdle{now=window.sunset}
        compose.onNodeWithTag("night-screen").assertExists()
        compose.runOnIdle{state=state.copy(restricted=true)}
        compose.onNodeWithTag("standard-screen").assertExists()
        compose.onNodeWithText("밤 화면 켜기").assertDoesNotExist()
        compose.runOnIdle{state=state.copy(restricted=false)}
        compose.onNodeWithTag("night-screen").assertExists()
        compose.runOnIdle{now=window.sunrise}
        compose.onNodeWithTag("standard-screen").assertExists()
        compose.runOnIdle{assertEquals(stay,state)}
    }
    @Test fun missingSolarInfoAllowsManualNightAndStandardInBothThemes() {
        var state by mutableStateOf(stay.copy(input=TripInput()))
        content{JourneyScreen(JourneyUi(state,loaded=true),{state=JourneyReducer.apply(state,it)},now=Instant.EPOCH)}
        compose.onNodeWithText("일몰 정보를 계산하지 못했어요. 밤 화면을 직접 선택할 수 있어요.").assertExists()
        for(dark in listOf(false,true)) {
            compose.runOnIdle{state=state.copy(dark=dark)}
            compose.onNodeWithText("밤 화면 켜기").performScrollTo()
            capture("before-tap")
            compose.onNodeWithText("밤 화면 켜기").performClick()
            capture("after-tap")
            compose.waitForIdle()
            compose.runOnIdle{assertEquals(NightDisplay.NIGHT,state.nightDisplay)}
            compose.onNodeWithTag("night-screen").assertExists()
            capture("manual")
            val layouts=mutableListOf<TextLayoutResult>()
            compose.onNodeWithText("밤 화면 켜기",useUnmergedTree=true).performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(layouts)}
            assertEquals(Color.White,layouts.single().layoutInput.style.color)
            compose.onNodeWithText("기본 화면").performScrollTo().performClick()
            compose.onNodeWithTag("standard-screen").assertExists()
            capture(if(dark)"dark" else "light")
            compose.runOnIdle{assertEquals(dark,state.dark)}
        }
    }
    @Test fun homeTemporarilyUsesStandardThemeAndSleepSelectionEntersNight() {
        var state by mutableStateOf(stay)
        content{JourneyScreen(JourneyUi(state,loaded=true),{state=JourneyReducer.apply(state,it)},now=Instant.EPOCH)}
        compose.onNodeWithText("수면",useUnmergedTree=true).performScrollTo().performClick()
        compose.onNodeWithTag("night-screen").assertExists()
        compose.onNodeWithTag("journey-home").performClick()
        compose.onNodeWithTag("standard-screen").assertExists()
        compose.onNodeWithText("이어하기").performClick()
        compose.onNodeWithTag("night-screen").assertExists()
        compose.runOnIdle{assertEquals(NightDisplay.NIGHT,state.nightDisplay)}
    }
}
