package com.chapark.ivi

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.chapark.app.domain.*
import com.chapark.ivi.design.IviTheme
import com.chapark.ivi.prototype.*
import com.chapark.prototype.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class PreparationOmissionTest {
    @get:Rule val compose=createComposeRule()

    @Test fun omittedRequiredItemStaysBlockedAndCanBeCheckedAfterHomeRoundTrip() {
        val draft=Journey(step=3,placeId="camp-nanji",input=TripInput(schedule=TripSchedule("2026-11-14","Asia/Seoul")))
        var state by mutableStateOf(draft.copy(checked=draft.departureItems.map{it.id}.toSet()-"drinking-water"))
        compose.setContent{JourneyScreen(JourneyUi(state,loaded=true),{state=JourneyJournal.apply(state,it,1000)})}
        compose.onNodeWithTag("omit-drinking-water").performScrollTo().performClick()
        compose.onNodeWithTag("omitted-drinking-water").assertExists()
        compose.onNodeWithTag("journey-next").assertIsNotEnabled()
        compose.runOnIdle{assertTrue("drinking-water" in state.omitted);assertEquals("PREPARATION_OMITTED",state.records.last().kind)}
        compose.onNodeWithTag("journey-home").performClick()
        compose.onNodeWithTag("journey-resume").performClick()
        compose.onNodeWithTag("omitted-drinking-water").assertExists()
        compose.onNodeWithTag("omit-drinking-water").performScrollTo().performClick()
        compose.onNodeWithTag("journey-next").assertIsNotEnabled()
        compose.onNodeWithText("식수 챙김 확인").performScrollTo().performClick()
        compose.onNodeWithTag("journey-next").assertIsEnabled()
        compose.runOnIdle{assertTrue("drinking-water" in state.checked);assertFalse("drinking-water" in state.omitted)}
    }

    @Test fun optionalItemExplainsProgressAndUsesBothPleosThemes() {
        var dark by mutableStateOf(false)
        val item=DecisionItem("optional",Group.GEAR,"개인 준비물","선택해서 챙기는 물건",required=false)
        val state=Journey(omitted=setOf(item.id))
        compose.setContent{IviTheme(dark){androidx.compose.material3.Surface(color=com.chapark.ivi.design.LocalIviColors.current.background){Column(Modifier.fillMaxWidth()){PreparationItem(state,item,{})}}}}
        for(theme in listOf(false,true)) {
            compose.runOnIdle{dark=theme}
            compose.onNodeWithText("선택 · 필수 진행률에서 제외").assertExists()
            compose.onNodeWithTag("omitted-optional").assertExists()
            compose.onNodeWithText("누락 표시 취소").assertIsDisplayed()
            val dir=File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir,"verification").apply{mkdirs()}
            File(dir,"omission-${if(theme)"dark" else "light"}.png").outputStream().use{
                compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)
            }
        }
    }

    @Test fun pastOmissionsHaveTheirOwnGroupAndHistoryUsesOmissionLabels() {
        val record=JourneyRecord("PREPARATION_OMITTED",1000,"USER",subject="drinking-water",label="생수",confirmed=true,placeId="camp-nanji")
        val archive=ArchivedTrip("past","난지캠핑장","2026-11-14","Asia/Seoul",listOf(record,record.copy(confirmed=false)),listOf("생수"))
        val state=Journey(step=3,placeId="camp-nanji",reminders=setOf("생수"))
        compose.setContent{JourneyScreen(JourneyUi(state,loaded=true,history=listOf(archive)),{})}
        compose.onNodeWithText("지난번에 빠뜨린 준비물").assertExists()
        compose.onNodeWithText("다시 챙기기 · 생수").assertExists()
        compose.onNodeWithText("지난 기록").performClick()
        compose.onNodeWithTag("memory-more-past").performScrollTo().performClick()
        compose.onNodeWithText("누락 · 생수").assertExists()
        compose.onNodeWithText("누락 취소 · 생수").assertExists()
    }
}
