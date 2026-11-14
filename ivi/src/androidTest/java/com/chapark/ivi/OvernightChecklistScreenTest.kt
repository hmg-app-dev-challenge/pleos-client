package com.chapark.ivi

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.chapark.ivi.prototype.*
import com.chapark.prototype.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class OvernightChecklistScreenTest {
    @get:Rule val compose=createComposeRule()
    @Test fun currentSleepAndCheckoutCannotSkipNewItems() {
        var state by mutableStateOf(Journey(step=5,placeId="camp-nanji",route=RoutePhase.ARRIVED,parked=true,
            checked=setOf("site-rules"),sleepChecks=JourneyChecklists.legacySleep.keys))
        compose.setContent{JourneyScreen(JourneyUi(state,loaded=true),{state=JourneyJournal.apply(state,it,1000)})}
        compose.onNodeWithTag("journey-next").assertIsNotEnabled()
        compose.onNodeWithText("문 잠금 직접 확인").performScrollTo().performClick()
        compose.onNodeWithTag("journey-next").assertIsNotEnabled()
        compose.onNodeWithText("개인 준비물 정리 확인").performScrollTo().performClick()
        compose.onNodeWithTag("journey-next").assertIsEnabled().performClick()
        for((index,title) in JourneyChecklists.exit.values.withIndex()) {
            compose.onNodeWithTag("journey-next").assertIsNotEnabled()
            compose.onNodeWithText(title).performScrollTo().performClick()
            if(index<4)compose.runOnIdle{assertFalse(state.canFinish)}
        }
        compose.onNodeWithTag("journey-next").assertIsEnabled().performClick()
        compose.onNodeWithText("종료").performClick()
        compose.onNodeWithText("수면 준비 6/6 · 철수 확인 5/5").assertExists()
        compose.runOnIdle{assertTrue(state.completed);assertEquals("USER",state.records.last().origin)}
    }

    @Test fun completedLegacyCountsRemainHistoricalInBothThemes() {
        var state by mutableStateOf(Journey(step=6,completed=true,checklistVersion=1,
            sleepChecks=JourneyChecklists.legacySleep.keys,exitChecks=JourneyChecklists.legacyExit.keys))
        compose.setContent{JourneyScreen(JourneyUi(state,loaded=true),{})}
        for(dark in listOf(false,true)) {
            compose.runOnIdle{state=state.copy(dark=dark)}
            compose.onNodeWithText("수면 준비 4/4 · 철수 확인 4/4").assertExists()
            compose.onNodeWithText("오늘의 차박을 마쳤어요").assertExists()
        }
    }
}
