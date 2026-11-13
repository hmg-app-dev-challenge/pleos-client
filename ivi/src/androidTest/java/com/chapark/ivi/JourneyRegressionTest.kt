package com.chapark.ivi

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import com.chapark.ivi.prototype.*
import com.chapark.prototype.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import android.graphics.Bitmap

class JourneyRegressionTest {
    @get:Rule val compose=createComposeRule()
    private fun assertInk(text:String,expected:Color) {
        val results=mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text,useUnmergedTree=true).performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(results)}
        assertEquals(text,expected,results.single().layoutInput.style.color)
    }
    private fun capture(name:String) {
        val dir=File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir,"verification").apply{mkdirs()}
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().let { bitmap->
            File(dir,"regression-$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        }
    }
    @Test fun migratedCookingCanBeConfirmedInPreparationWithoutLosingChecks() {
        val base=Journey(step=3,placeId="camp-nanji",input=com.chapark.app.domain.TripInput(
            cookingPlan=com.chapark.app.domain.CookingPlan.COOKING,cookingNeedsReview=true,
            schedule=com.chapark.app.domain.TripSchedule("2026-11-14","Asia/Seoul")))
        var state by mutableStateOf(base.copy(checked=base.departureItems.map{it.id}.toSet()))
        compose.setContent{JourneyScreen(JourneyUi(state,loaded=true),{state=JourneyReducer.apply(state,it)})}
        compose.onNodeWithTag("journey-next").assertIsNotEnabled()
        compose.onNodeWithText("취사 예정").performScrollTo().performClick()
        compose.onNodeWithTag("journey-next").assertIsEnabled()
        compose.runOnIdle{assertFalse(state.input.cookingNeedsReview);assertEquals(base.departureItems.map{it.id}.toSet(),state.checked)}
    }
    @Test fun checkedPreparationWithoutDateCanSetDateAndProceedWithoutLosingChecks() {
        val base=Journey(step=3,placeId="camp-nanji")
        var state by mutableStateOf(base.copy(checked=base.departureItems.map{it.id}.toSet()))
        compose.setContent{JourneyScreen(JourneyUi(state,loaded=true),{state=JourneyReducer.apply(state,it)})}
        compose.onNodeWithTag("journey-next").assertIsNotEnabled()
        compose.onNodeWithTag("departure-block-reason").assertTextEquals("여행 날짜를 설정해 주세요.")
        compose.onNodeWithText("오늘 날짜로 계획").performScrollTo().performClick()
        compose.onNodeWithTag("journey-next").assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(4,state.step)
            assertEquals(base.departureItems.map{it.id}.toSet(),state.checked)
            assertNotNull(state.input.schedule)
        }
    }
    @Test fun settingsHistoryAndConfirmUseThemeInkInLightDarkAndNight() {
        var state by mutableStateOf(Journey(placeId="camp-nanji"))
        val trip=ArchivedTrip("color-test","난지캠핑장","2026-11-12","Asia/Seoul",emptyList(),listOf("담요"))
        compose.setContent{JourneyScreen(JourneyUi(state,loaded=true,history=listOf(trip)),{state=JourneyReducer.apply(state,it)})}
        for(mode in listOf("light","dark","night")) {
            compose.runOnIdle{state=Journey(placeId="camp-nanji",dark=mode=="dark",step=if(mode=="night")5 else 0,restMode=if(mode=="night")"SLEEP" else "REST")}
            val ink=when(mode){"dark"->Color.White;"night"->Color(0xFFFFE4BD);else->Color(0xFF131417)}
            compose.onNodeWithText("설정").performClick()
            assertInk("화면과 여행 계획을 관리하세요.",ink)
            assertInk("어두운 화면",ink)
            capture("$mode-settings")
            compose.onNodeWithText("설정 닫기").performScrollTo().performClick()
            compose.onNodeWithText("지난 기록").performClick()
            assertInk("지난 여행 기록",ink)
            assertInk("다음에 챙길 것 · 담요",ink)
            capture("$mode-history")
            compose.onNodeWithText("이 기록 삭제").performScrollTo().performClick()
            assertInk("여행 기록을 삭제할까요?",ink)
            capture("$mode-confirm")
            compose.onNodeWithText("취소").performClick()
            compose.onNodeWithText("기록 닫기").performScrollTo().performClick()
        }
    }
    @Test fun inputAndRestModeSelectionRemainReadableAcrossThemes() {
        var state by mutableStateOf(Journey(step=6,placeId="camp-nanji"))
        compose.setContent{JourneyScreen(JourneyUi(state,loaded=true),{state=JourneyReducer.apply(state,it)})}
        for(dark in listOf(false,true)) {
            compose.runOnIdle{state=Journey(step=6,placeId="camp-nanji",dark=dark)}
            compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("담요")
            assertInk("담요",if(dark)Color.White else Color(0xFF131417))
            capture(if(dark)"dark-input" else "light-input")
        }
        compose.runOnIdle{state=Journey(step=5,placeId="camp-nanji",route=RoutePhase.ARRIVED,parked=true)}
        compose.onNodeWithText("수면",useUnmergedTree=true).performScrollTo().performClick()
        assertInk("수면",Color(0xFFFFE4BD))
        capture("night-segments")
    }

    @Test fun unsupportedBatteryIsHiddenAndPermissionFailureAllowsManualReading(){
        var state by mutableStateOf(Journey(step=3,placeId="camp-nanji",soc=null,batteryStatus=com.chapark.app.domain.ReadingStatus.UNSUPPORTED))
        compose.setContent{JourneyScreen(JourneyUi(state,loaded=true),{state=JourneyReducer.apply(state,it)})}
        compose.onAllNodes(hasText("현재 배터리",substring=true)).assertCountEquals(0)
        compose.onNodeWithText("충전 경유 검토").assertDoesNotExist()
        compose.runOnIdle{state=state.copy(batteryStatus=com.chapark.app.domain.ReadingStatus.PERMISSION_DENIED)}
        compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("30")
        compose.onNodeWithText("직접 입력한 잔량 사용").performScrollTo().performClick()
        compose.runOnIdle{
            assertEquals(com.chapark.app.domain.Source.MANUAL,state.socSource)
            assertEquals(30,state.soc)
        }
        compose.onNodeWithText("충전 경유 검토").assertExists()
    }

}
