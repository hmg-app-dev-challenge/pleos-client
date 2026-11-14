package com.chapark.ivi

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.chapark.app.domain.*
import com.chapark.ivi.prototype.*
import com.chapark.prototype.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ChargingReviewScreenTest {
    @get:Rule val compose=createComposeRule()
    private val low=Journey(step=3,placeId="camp-nanji",soc=34,input=TripInput(batteryThreshold=35))

    @Test fun reviewIsOnlyVisibleBelowThresholdAndEmptyPlacesDoNotBorrowCandidates() {
        var state by mutableStateOf(low)
        compose.setContent{JourneyScreen(JourneyUi(state,loaded=true),{state=JourneyReducer.apply(state,it)})}
        compose.onNodeWithText("출발 전 필수 0/${low.departureProgress.total}개 확인").assertExists()
        compose.onNodeWithTag("charging-review").assertExists()
        compose.onNodeWithText("충전 경유지로 선택").assertExists()
        for(next in listOf(low.copy(soc=35),low.copy(soc=36),low.copy(soc=null),low.copy(batteryStatus=ReadingStatus.STALE),low.copy(batteryStatus=ReadingStatus.UNSUPPORTED))) {
            compose.runOnIdle{state=next}
            compose.onNodeWithTag("charging-review").assertDoesNotExist()
        }
        compose.runOnIdle{state=low.copy(placeId="camp-mongsanpo")}
        compose.onNodeWithTag("charging-review").assertExists()
        compose.onNodeWithText("충전 경유지로 선택").assertDoesNotExist()
        compose.onNodeWithText("이 장소에 연결된 충전 후보가 아직 없어요. 내비게이션에서 충전시설을 확인해 주세요.").assertExists()
    }

    @Test fun oldSelectionIsVisibleWithoutReviewAndCanBeExplicitlyRemovedBeforeNavigation() {
        var state by mutableStateOf(low.copy(soc=80,chargerId="emart-mapo"))
        compose.setContent{JourneyScreen(JourneyUi(state,loaded=true),{state=JourneyReducer.apply(state,it)})}
        compose.onNodeWithTag("charging-review").assertDoesNotExist()
        compose.onNodeWithTag("selected-charger").assertExists()
        compose.runOnIdle{state=state.copy(step=4,placeId="camp-mongsanpo")}
        compose.onNodeWithText("현재 목적지의 연결 후보가 아니에요. 내비에서 경로를 다시 확인해 주세요.").assertExists()
        compose.onNodeWithTag("clear-charger").performScrollTo().performClick()
        compose.onNodeWithTag("selected-charger").assertDoesNotExist()
        compose.runOnIdle{assertNull(state.chargerId);assertEquals(4,state.step)}
    }

    @Test fun toiletComparisonKeepsInputAndChecksInPlanningStage() {
        var state by mutableStateOf(low.copy(checked=setOf("drinking-water")))
        compose.setContent{JourneyScreen(JourneyUi(state,loaded=true),{state=JourneyReducer.apply(state,it)})}
        compose.onNodeWithText("다른 장소 비교").performScrollTo().performClick()
        compose.runOnIdle{assertEquals(2,state.step);assertEquals(low.input,state.input);assertEquals(setOf("drinking-water"),state.checked)}
    }
}
