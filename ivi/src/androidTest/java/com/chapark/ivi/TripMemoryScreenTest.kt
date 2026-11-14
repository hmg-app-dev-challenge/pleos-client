package com.chapark.ivi

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.chapark.app.domain.*
import com.chapark.ivi.prototype.*
import com.chapark.prototype.*
import org.junit.Rule
import org.junit.Test

class TripMemoryScreenTest {
    @get:Rule val compose=createComposeRule()
    @Test fun archiveShowsCapturedValuesInBothThemesInsteadOfCurrentVehicleData() {
        var state by mutableStateOf(Journey(placeId="camp-gangdong",soc=99))
        val records=listOf(
            JourneyRecord("PLACE_CONTEXT",100,"REFERENCE",placeId="camp-nanji",label="난지캠핑장",placeMemory=PlaceMemory(listOf("강"),listOf("2026-11-13"),true)),
            JourneyRecord("PREPARATION_COMPLETED",100,"USER",placeId="camp-nanji",soc=20,socSource=Source.SDK,preparationCompleted=12,preparationRequired=12),
            JourneyRecord("GUIDANCE_OBSERVED",110,"SDK",placeId="camp-nanji"),
            JourneyRecord("TRIP_COMPLETED",200,"USER",placeId="camp-nanji",soc=99,socSource=Source.SDK,readingStatus=ReadingStatus.DISCONNECTED))
        val archive=ArchivedTrip("past","난지캠핑장","2026-11-14","Asia/Seoul",records,listOf("컵"),MemoryDetails(6,6,5,5,true,"이마트 마포점 충전시설",true))
        compose.setContent{JourneyScreen(JourneyUi(state,loaded=true,history=listOf(archive)),{})}
        compose.runOnIdle{state=state.copy(step=6,completed=true,route=RoutePhase.ARRIVED,parked=true,sleepReady=true,
            sleepChecks=Journey.SLEEP_IDS,exitChecks=Journey.EXIT_IDS,records=records)}
        compose.onNodeWithText("출발 전 필수 12/12개 확인").assertExists()
        compose.onNodeWithText("지난 기록").performClick()
        for(dark in listOf(false,true)) {
            compose.runOnIdle{state=state.copy(dark=dark)}
            listOf("풍경 태그 · 강","정보 확인일 · 2026-11-13 · 일부 날짜 미확인","출발 배터리 · 20% · 차량",
                "철수 배터리 · 확인하지 못함 · 차량","출발 전 필수 확인 12/12","선택 경유지 · 이마트 마포점 충전시설",
                "내비 결과 · 안내 확인","수면 준비 6/6 · 철수 확인 5/5","다음에 챙길 것 · 컵").forEach{
                compose.onNode(hasText(it) and hasAnyAncestor(isDialog())).assertExists()
            }
        }
    }

    @Test fun oldArchiveDoesNotPresentUncollectedValuesAsZeroOrSuccess() {
        val archive=ArchivedTrip("old","이전 여행","날짜 미수집","UTC",emptyList(),emptyList())
        compose.setContent{JourneyScreen(JourneyUi(Journey(),loaded=true,history=listOf(archive)),{})}
        compose.onNodeWithText("지난 기록").performClick()
        listOf("풍경 태그 · 미수집","출발 배터리 · 미수집","철수 배터리 · 미수집","준비 완료 수 · 미수집",
            "선택 경유지 · 미수집","내비 결과 · 미수집","수면·철수 확인 요약 · 미수집").forEach{compose.onNodeWithText(it).assertExists()}
    }
}
