package com.chapark.ivi

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import java.io.File

class JourneyFlowTest {
    @get:Rule val compose=createAndroidComposeRule<IviActivity>()
    private fun waitText(text:String) { compose.waitUntil(15_000){compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()} }
    private fun saved() { waitText("기기에 저장됨"); compose.waitForIdle() }
    private fun click(text:String) { compose.onNodeWithText(text).performScrollTo().performClick(); compose.waitForIdle() }
    private fun next() { saved(); compose.onNodeWithTag("journey-next").performClick(); compose.waitForIdle() }
    private fun screenshot(name:String) {
        saved()
        listOf("시연","데모","가상","재현","SDK","미연동").forEach { word ->
            compose.onAllNodes(hasText(word,substring=true)).assertCountEquals(0)
        }
        val folder=File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir,"verification").apply{mkdirs()}
        File(folder,name).outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
    @Test fun completeProductJourneyWithAutomaticPreparationAndManualParking() {
        waitText("설정")
        compose.onNodeWithText("설정").performClick()
        click("새 여행 계획");waitText("여행 계획을 새로 만들까요?");compose.onNodeWithText("처음부터").performClick()
        waitText("오늘 밤 차박 시작");screenshot("s1-start.png")
        next();waitText("장소 후보 비교");screenshot("s2-conditions.png")
        next();waitText("선택한 장소로 계획");screenshot("s3-places.png")
        next();waitText("오늘의 준비 계획")
        compose.onNodeWithText("충전 경유 검토").assertExists()
        compose.onNodeWithText("조리된 식사 준비").assertExists()
        screenshot("s4-plan.png")
        next();waitText("미확인 항목이 남아 있어요");compose.onNodeWithText("이동 계획 보기").performClick()
        waitText("이동 시작");next()
        waitText("도착했어요");screenshot("s5-guiding.png")
        next();waitText("주차했어요")
        compose.onNodeWithText("수면 준비로").assertDoesNotExist()
        next();waitText("수면 준비로");next();waitText("잠들기 전, 하나씩 확인해요")
        compose.onNodeWithTag("journey-next").assertIsNotEnabled()
        listOf("주차 위치와 현장 숙박 조건 확인","환기 방법과 비상 이동 동선 확인","차량 기본 설정에서 공조 직접 확인","차량 기본 설정에서 조명 직접 확인").forEach{click(it)}
        screenshot("s6-sleep.png");next();waitText("떠나기 전, 한 번 더.")
        listOf("직접 변경한 차량 설정 확인","침구와 개인 장비 모두 챙기기","쓰레기 수거와 주변 정리","출발 전 차량 주변 확인").forEach{click(it)}
        screenshot("s7-exit.png");next();waitText("오늘의 차박을 마칠까요?");compose.onNodeWithText("종료").performClick()
        waitText("오늘의 차박을 마쳤어요");screenshot("s7-completed.png")
        // Activity recreation must restore the persisted terminal state, not restart the trip.
        compose.activityRule.scenario.recreate();waitText("오늘의 차박을 마쳤어요")
        compose.onNodeWithText("설정").performClick();click("어두운 화면");click("설정 닫기")
        screenshot("s7-completed-dark.png")
    }
}
