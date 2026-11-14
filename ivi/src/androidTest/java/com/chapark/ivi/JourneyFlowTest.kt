package com.chapark.ivi

import android.graphics.Bitmap
import android.content.Intent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import java.io.File

class JourneyFlowTest {
    private var original:com.chapark.prototype.Journey?=null
    @org.junit.Before fun preserveActiveJourney(){kotlinx.coroutines.runBlocking {
        original=com.chapark.ivi.prototype.JourneyStore(InstrumentationRegistry.getInstrumentation().targetContext).load().first
    }}
    @org.junit.After fun restoreActiveJourney(){kotlinx.coroutines.runBlocking {
        val store=com.chapark.ivi.prototype.JourneyStore(InstrumentationRegistry.getInstrumentation().targetContext)
        original?.let{saved->
            store.load().first.tripId?.takeIf{it!=saved.tripId}?.let{store.deleteHistory(it)}
            store.save(saved)
        }
    }}
    @get:Rule val compose=createAndroidComposeRule<IviActivity>()
    private val offline get()=InstrumentationRegistry.getArguments().getString("networkOffline")=="true"
    private fun hasDefaultNetwork():Boolean {
        val fd=InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("dumpsys connectivity")
        val text=android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).bufferedReader().use{it.readText()}
        val line=text.lineSequence().firstOrNull{it.startsWith("Active default network:")}
        org.junit.Assert.assertNotNull("Connectivity state must be observable",line)
        return line!!.trim()!="Active default network: none"
    }
    private fun verifyOffline(){
        if(offline)org.junit.Assert.assertFalse("Offline scenario requires no active default network",hasDefaultNetwork())
    }
    private fun reportNetworkOutcome(outcome:String){
        verifyOffline()
        if(offline)InstrumentationRegistry.getInstrumentation().sendStatus(0,android.os.Bundle().apply{putString("stream","OFFLINE_NAVIGATION: $outcome; active default network: none\n")})
    }
    private fun waitText(text:String) { compose.waitUntil(40_000){runCatching{compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()}.getOrDefault(false)} }
    private fun saved() { waitText("기기에 저장됨"); compose.waitForIdle() }
    private fun click(text:String) { compose.onNode(hasText(text) or hasContentDescription(text)).performScrollTo().performClick(); compose.waitForIdle() }
    private fun toggleTheme() { compose.onNodeWithText("어두운 화면").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); compose.waitForIdle() }
    private fun next() { saved(); compose.onNodeWithTag("journey-next").performClick(); compose.waitForIdle() }
    private fun screenshot(name:String) {
        saved()
        listOf("시연","데모","가상","재현","SDK","미연동","예시","에뮬레이터").forEach { word ->
            compose.onAllNodes(hasText(word,substring=true)).assertCountEquals(0)
        }
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val folder=File(if(BuildConfig.DEBUG)context.filesDir else context.getExternalFilesDir(null),"verification").apply{mkdirs()}
        File(folder,name).outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
        InstrumentationRegistry.getArguments().getString("captureDelayMs")?.toLongOrNull()?.coerceIn(0,3000)?.let{Thread.sleep(it)}
    }
    @Test fun completeProductJourneyWithAutomaticPreparationAndManualParking() {
        verifyOffline()
        waitText("설정")
        compose.onNodeWithText("설정").performClick()
        val initiallyDark=kotlinx.coroutines.runBlocking { com.chapark.ivi.prototype.JourneyStore(InstrumentationRegistry.getInstrumentation().targetContext).load().first.dark }
        if(initiallyDark)toggleTheme()
        click("새 여행 계획");waitText("여행 계획을 새로 만들까요?");compose.onNodeWithText("처음부터").performClick()
        waitText("오늘 밤 차박 시작");screenshot("s1-start.png")
        next();waitText("차박 계획")
        click("오늘 날짜로 계획");saved()
        click("인원 늘리기");click("인원 늘리기")
        click("배터리 확인 기준 늘리기");click("철수 예정 줄이기")
        compose.activityRule.scenario.recreate();waitText("인원 · 3명")
        screenshot("s2-conditions.png")
        next();waitText("이 장소 선택");screenshot("s3-places.png")
        next();waitText("오늘의 준비 계획")
        compose.onNodeWithText("충전 경유 검토").assertExists()
        compose.onNodeWithText("취사 허용 여부 확인").assertExists()
        screenshot("s4-plan.png")
        compose.onNodeWithTag("journey-next").assertIsNotEnabled()
        val planned=com.chapark.prototype.Journey(placeId="camp-nanji",input=com.chapark.app.domain.TripInput(3,com.chapark.app.domain.CookingPlan.COOKING,false,true,40,9))
        planned.departureItems.forEach{click(it.title);saved()}
        compose.onNodeWithTag("journey-next").assertIsEnabled()
        next()
        waitText("길 안내 시작");next()
        // ChaPark explicitly requests navigation foreground after delivering the route.
        Thread.sleep(5_000)
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        context.startActivity(Intent(context,IviActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        if(offline){
            compose.waitUntil(40_000){runCatching{compose.onAllNodes(hasText("내비게이션 열기") or hasText("다시 시도")).fetchSemanticsNodes().isNotEmpty()}.getOrDefault(false)}
            if(compose.onAllNodesWithText("다시 시도").fetchSemanticsNodes().isNotEmpty()){
                saved();screenshot("offline-navigation-failed.png")
                val failed=kotlinx.coroutines.runBlocking { com.chapark.ivi.prototype.JourneyStore(context).load().first }
                org.junit.Assert.assertEquals(com.chapark.prototype.RoutePhase.FAILED,failed.route)
                org.junit.Assert.assertEquals("camp-nanji",failed.placeId)
                org.junit.Assert.assertEquals(3,failed.input.people)
                org.junit.Assert.assertEquals(planned.departureItems.map{it.id}.toSet(),failed.checked)
                org.junit.Assert.assertFalse(failed.completed)
                org.junit.Assert.assertTrue(failed.records.any{it.kind=="ROUTE_FAILED"})
                compose.activityRule.scenario.recreate();waitText("다시 시도")
                compose.onNodeWithTag("journey-next").assertIsEnabled()
                reportNetworkOutcome("FAILED; plan and checks preserved after recreation; retry enabled")
                return
            }
            reportNetworkOutcome("GUIDING; SDK observed guidance without default network")
        }
        waitText("내비게이션 열기");screenshot("s5-guiding.png")
        compose.activityRule.scenario.recreate();waitText("내비게이션 열기")
        click("도착했어요");waitText("주차했어요")
        compose.onNodeWithText("수면 준비로").assertDoesNotExist()
        next();waitText("수면 준비로");next();waitText("잠들기 전, 하나씩 확인해요")
        compose.onNodeWithTag("journey-next").assertIsNotEnabled()
        listOf("현장 운영 안내 확인","주차 위치와 현장 숙박 조건 확인","환기 방법과 비상 이동 동선 확인","차량 기본 설정에서 공조 직접 확인","차량 기본 설정에서 조명 직접 확인","문 잠금 직접 확인","개인 준비물 정리 확인").forEach{click(it)}
        screenshot("s6-sleep.png");next();waitText("떠나기 전, 한 번 더.")
        listOf("직접 변경한 차량 설정 확인","실내·외 장비 모두 회수","쓰레기 수거와 주변 정리","놓고 가는 물품 확인","출발 전 장소 상태 확인").forEach{click(it)}
        screenshot("s7-exit.png");next();waitText("오늘의 차박을 마칠까요?");compose.onNodeWithText("종료").performClick()
        waitText("오늘의 차박을 마쳤어요");screenshot("s7-completed.png")
        kotlinx.coroutines.runBlocking {
            val store=com.chapark.ivi.prototype.JourneyStore(InstrumentationRegistry.getInstrumentation().targetContext)
            val finished=store.load().first
            org.junit.Assert.assertTrue(finished.records.any{it.kind=="GUIDANCE_OBSERVED" && it.origin=="SDK"})
            org.junit.Assert.assertEquals(1,finished.records.count{it.kind=="TRIP_COMPLETED"})
            org.junit.Assert.assertEquals(1,finished.records.count{it.kind=="ROUTE_CALL_RETURNED"})
            org.junit.Assert.assertNotNull(store.archivedRecord(finished.tripId!!))
            val memory=store.history().single{it.id==finished.tripId}.memory!!
            org.junit.Assert.assertEquals(6,memory.sleepChecked)
            org.junit.Assert.assertEquals(5,memory.exitChecked)
            org.junit.Assert.assertTrue(finished.records.any{it.kind=="PLACE_CONTEXT" && it.placeMemory!=null})
            org.junit.Assert.assertNotNull(finished.records.single{it.kind=="PREPARATION_COMPLETED"}.preparationRequired)
        }
        // Activity recreation must restore the persisted terminal state, not restart the trip.
        compose.activityRule.scenario.recreate();waitText("오늘의 차박을 마쳤어요")
        compose.onNodeWithText("설정").performClick();toggleTheme();click("설정 닫기")
        screenshot("s7-completed-dark.png")
        reportNetworkOutcome("COMPLETED; local record persisted")
    }
}
