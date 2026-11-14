package com.chapark.ivi

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.unit.dp
import com.chapark.ivi.prototype.*
import com.chapark.prototype.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class JourneyHomeTest {
    @get:Rule val compose=createComposeRule()

    @Test fun homeAndResumePreserveEveryStageInBothThemes() {
        var current by mutableStateOf(Journey())
        val events=mutableListOf<JourneyEvent>()
        compose.setContent {
            Box(Modifier.requiredSize(800.dp,1000.dp)) {
                JourneyScreen(JourneyUi(current,loaded=true),{events.add(it)})
            }
        }
        for(dark in listOf(false,true)) for(step in 1..6) {
            val original=Journey(tripId="home-$dark-$step",step=step,dark=dark,placeId="camp-nanji",
                checked=setOf("water"),omitted=setOf("sleeping-bag"),
                route=if(step==4)RoutePhase.GUIDING else if(step>=5)RoutePhase.ARRIVED else RoutePhase.NONE,
                parked=step>=5,sleepReady=step==6,sleepChecks=Journey.SLEEP_IDS,exitChecks=setOf("gear"))
            compose.runOnIdle{current=original}
            compose.onNodeWithTag("journey-home").performClick()
            compose.onNodeWithText("진행 중인 여행").assertExists()
            compose.onNodeWithTag("journey-next").assertDoesNotExist()
            compose.onNodeWithTag("journey-resume").assertIsDisplayed().performClick()
            compose.onNodeWithTag("journey-next").assertIsDisplayed()
            compose.runOnIdle{assertEquals(original,current);assertTrue(events.isEmpty())}
        }
    }

    @Test fun homeRestoresAndDrivingLockDoesNotDiscardTheTrip() {
        var current by mutableStateOf(Journey(tripId="resume",step=4,route=RoutePhase.GUIDING,placeId="camp-nanji"))
        val restoration=StateRestorationTester(compose)
        restoration.setContent{JourneyScreen(JourneyUi(current,loaded=true),{})}
        compose.onNodeWithTag("journey-home").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("journey-resume").assertIsDisplayed()
        compose.runOnIdle{current=current.copy(restricted=true)}
        compose.onNodeWithTag("journey-resume").assertDoesNotExist()
        compose.onNodeWithText("정차 후 이어서\n확인할 수 있어요").assertExists()
        compose.runOnIdle{current=current.copy(restricted=false)}
        compose.onNodeWithTag("journey-resume").performClick()
        compose.runOnIdle{assertEquals(RoutePhase.GUIDING,current.route);assertEquals(4,current.step)}
    }

    @Test fun startingAnotherTripRequiresConfirmationAndCompletedTripRemainsViewable() {
        var current by mutableStateOf(Journey(tripId="finished",step=6,completed=true,placeId="camp-nanji"))
        val events=mutableListOf<JourneyEvent>()
        compose.setContent{JourneyScreen(JourneyUi(current,loaded=true),{events.add(it);current=JourneyReducer.apply(current,it)})}
        compose.onNodeWithTag("journey-home").performClick()
        compose.onNodeWithText("완료한 여행 보기").performClick()
        compose.runOnIdle{assertTrue(current.completed);assertTrue(events.isEmpty())}
        compose.onNodeWithTag("journey-home").performClick()
        compose.onNodeWithText("새 차박 시작").performScrollTo().performClick()
        compose.onNodeWithText("새 차박을 시작할까요?").assertExists()
        compose.runOnIdle{assertTrue(events.isEmpty())}
        compose.onNodeWithText("새로 시작").performClick()
        compose.runOnIdle{assertEquals(listOf(JourneyEvent.NewTrip),events);assertEquals(0,current.step)}
    }
}
