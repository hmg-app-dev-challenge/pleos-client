package com.chapark.ivi

import android.os.ParcelFileDescriptor
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
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

class PlanningPresentationTest {
    @get:Rule val compose=createEmptyComposeRule()
    @Test fun groupedConditionsStayReadableAndControlsWorkInBothThemes(){
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val store=JourneyStore(instrumentation.targetContext)
        val original=runBlocking{store.load().first}
        val scenario=ActivityScenario.launch(IviActivity::class.java)
        var state by mutableStateOf(Journey(step=1,placeId="camp-nanji",batteryStatus=ReadingStatus.AVAILABLE,
            input=TripInput(schedule=TripSchedule("2026-11-14","Asia/Seoul"))))
        var narrow by mutableStateOf(false)
        var scale by mutableFloatStateOf(1f)
        try {
            scenario.onActivity{it.setContent{
                val density=LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(density,scale)) {
                    Box(Modifier.then(if(narrow)Modifier.requiredSize(800.dp,1000.dp)else Modifier.fillMaxSize()).clipToBounds().testTag("planning-window")) {
                        JourneyScreen(JourneyUi(state,loaded=true),{event->state=JourneyReducer.apply(state,event)})
                    }
                }
            }}
            for(compact in listOf(false,true)) for(dark in listOf(false,true)) {
                compose.runOnIdle{narrow=compact;scale=if(compact)1.3f else 1f;state=state.copy(dark=dark,step=1)}
                compose.onNodeWithText("여행 일정").performScrollTo()
                compose.onNodeWithTag("journey-next").assertIsDisplayed()
                val window=compose.onNodeWithTag("planning-window").getUnclippedBoundsInRoot()
                listOf("여행 날짜","철수 예정","인원","배터리 확인 기준").forEach { label->
                    val action=compose.onNodeWithContentDescription("$label 늘리기")
                    action.performScrollTo().assertIsDisplayed().performClick()
                    val bounds=action.getUnclippedBoundsInRoot()
                    assertTrue("$label must fit the window",bounds.left>=window.left && bounds.right<=window.right)
                    assertTrue("Controls retain the IVI touch target",bounds.bottom-bounds.top>=80.dp)
                }
                compose.onNodeWithText("함께하는 여행").performScrollTo()
                val before=state.input.people
                compose.onNodeWithContentDescription("인원 줄이기").performClick()
                compose.runOnIdle{assertEquals(before-1,state.input.people)}
                compose.onNodeWithText("취사 계획").performScrollTo()
                compose.onNodeWithText(CookingPlan.SIMPLE.label).performScrollTo().performClick()
                compose.runOnIdle{assertEquals(CookingPlan.SIMPLE,state.input.cookingPlan)}
                compose.onNodeWithText("함께하는 여행").performScrollTo()
                val name="chapark052-planning-${if(compact)"narrow" else "wide"}-${if(dark)"dark" else "light"}"
                ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("screencap -p /data/local/tmp/$name.png")).use{it.readBytes()}
            }
        } finally {
            scenario.close()
            runBlocking{store.save(original)}
        }
    }
}
