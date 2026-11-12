package com.chapark.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class TripFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private fun tap(text:String){ui.onNodeWithText(text).performClick();ui.waitForIdle()}
    private fun next(){ui.onNodeWithTag("journey-next").performClick();ui.waitForIdle()}
    private fun clean(){listOf("시연","데모","가상","재현","SDK","미연동").forEach{ui.onAllNodes(hasText(it,substring=true)).assertCountEquals(0)}}
    @Test fun fullJourneyWithParkingAndChecklists(){
        clean();next();clean();next();clean();next();clean()
        next();tap("이동 계획 보기");next()
        ui.waitUntil(10_000){ui.onAllNodesWithText("도착했어요").fetchSemanticsNodes().isNotEmpty()}
        clean();next();ui.onNodeWithText("주차했어요").assertExists()
        ui.onNodeWithText("수면 준비로").assertDoesNotExist()
        next();next();clean();ui.onNodeWithTag("journey-next").assertIsNotEnabled()
        listOf("주차 위치와 현장 숙박 조건 확인","환기 방법과 비상 이동 동선 확인","차량 기본 설정에서 공조 직접 확인","차량 기본 설정에서 조명 직접 확인").forEach { title ->
            ui.onNodeWithText(title).performScrollTo()
            ui.onNodeWithTag("check-$title").performClick()
        }
        next();clean();ui.onNodeWithTag("journey-next").assertIsNotEnabled()
        listOf("직접 변경한 차량 설정 확인","침구와 개인 장비 모두 챙기기","쓰레기 수거와 주변 정리","출발 전 차량 주변 확인").forEach { title ->
            ui.onNodeWithText(title).performScrollTo()
            ui.onNodeWithTag("check-$title").performClick()
        }
        next();tap("종료");ui.onNodeWithText("오늘의 차박을 마쳤어요").assertExists()
        ui.activityRule.scenario.recreate();ui.onNodeWithText("오늘의 차박을 마쳤어요").assertExists();clean()
    }
    @Test fun settingsOnlyContainProductPreferences(){
        tap("설정");clean();ui.onNodeWithText("어두운 화면").assertExists()
        ui.onNodeWithText("배터리 잔량").assertDoesNotExist();tap("설정 닫기")
        next();ui.activityRule.scenario.recreate();ui.onNodeWithText("장소 후보 비교").assertExists()
    }
}
