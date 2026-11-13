package com.chapark.ivi

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.chapark.ivi.prototype.*
import com.chapark.prototype.*
import com.chapark.app.domain.*
import org.junit.Test
import org.junit.Rule
import org.junit.Assert.*
import java.io.File

class MvpPlanningScreenTest {
    @get:Rule val compose=createComposeRule()
    @Test fun tabsKeepPlanAndCompareThreePlacesWithoutAddingAStage(){
        var state by mutableStateOf(Journey(step=1,placeId="camp-nanji",input=TripInput(people=3,cookingPlan=CookingPlan.SIMPLE,schedule=TripSchedule("2026-11-14","Asia/Seoul")),checked=setOf("water")))
        compose.setContent { Box(Modifier.requiredSize(1602.dp,1100.dp)){JourneyScreen(JourneyUi(state,loaded=true),{state=JourneyReducer.apply(state,it)})} }
        compose.onNodeWithText("1 차박 계획").assertExists()
        compose.onNodeWithText("5 철수와 기록").assertExists()
        compose.onNodeWithText("장소 비교").performClick()
        compose.runOnIdle{assertEquals(2,state.step);assertEquals(JourneyStage.PLANNING,state.stage)}
        val ids=listOf("camp-jungnang","camp-nanji","camp-gangdong")
        val boxes=ids.map{compose.onNodeWithTag("place-overview-$it").fetchSemanticsNode().boundsInRoot}
        assertTrue(boxes[0].right<=boxes[1].left);assertTrue(boxes[1].right<=boxes[2].left)
        assertEquals(boxes[0].top,boxes[2].top)
        compose.onNodeWithTag("place-overview-camp-nanji").performScrollTo()
        val dir=File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir,"verification").apply{mkdirs()}
        File(dir,"mvp-planning-wide.png").outputStream().use{compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
        compose.onNodeWithText("여행 조건").performScrollTo().performClick()
        compose.runOnIdle{assertEquals(3,state.input.people);assertEquals(CookingPlan.SIMPLE,state.input.cookingPlan);assertEquals(setOf("water"),state.checked)}
        compose.onNodeWithText("장소 비교").performClick()
        compose.onNodeWithTag("journey-next").performClick()
        compose.runOnIdle{assertEquals(3,state.step);assertEquals(JourneyStage.PREPARING,state.stage)}
    }
    @Test fun homeHasNoNumberedProgressAndInvalidPlanCannotLeavePlanning(){
        var state by mutableStateOf(Journey())
        compose.setContent{JourneyScreen(JourneyUi(state,loaded=true),{state=JourneyReducer.apply(state,it)})}
        compose.onNodeWithText("1 차박 계획").assertDoesNotExist()
        compose.runOnIdle{state=Journey(step=2,placeId="camp-nanji")}
        compose.onNodeWithTag("journey-next").assertIsNotEnabled()
        compose.onNodeWithText("여행 조건").performClick()
        compose.runOnIdle{assertEquals(1,state.step)}
    }
}
