package com.chapark.ivi

import android.os.ParcelFileDescriptor
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.chapark.app.domain.*
import com.chapark.ivi.design.*
import com.chapark.ivi.prototype.*
import com.chapark.prototype.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.*

class TripMemoryScreenTest {
    @get:Rule val compose=createEmptyComposeRule()
    private lateinit var scenario:ActivityScenario<IviActivity>
    private lateinit var original:Journey
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private val store get()=JourneyStore(instrumentation.targetContext)
    @Before fun launch(){
        original=runBlocking{store.load().first}
        scenario=ActivityScenario.launch(IviActivity::class.java)
        var stableSince=0L
        var geometry=""
        compose.waitUntil(10_000){
            var ready=false
            scenario.onActivity { activity->
                val view=activity.window.decorView
                val location=IntArray(2);view.getLocationOnScreen(location)
                val current="${view.width}/${view.height}/${location.toList()}"
                val now=android.os.SystemClock.elapsedRealtime()
                if(!view.hasWindowFocus() || view.width==0 || current!=geometry){stableSince=now;geometry=current}
                ready=view.hasWindowFocus() && now-stableSince>=800
            }
            ready
        }
    }
    @After fun restore(){try{scenario.close()}finally{runBlocking{store.save(original)}}}
    private fun content(block:@Composable ()->Unit){
        scenario.onActivity{it.setContent(content=block)}
        compose.waitUntil(10_000){runCatching{compose.onAllNodes(isRoot()).fetchSemanticsNodes().isNotEmpty()}.getOrDefault(false)}
    }
    private fun capture(name:String){
        val root=if(compose.onAllNodes(isDialog()).fetchSemanticsNodes().isNotEmpty())compose.onNode(isDialog()) else compose.onRoot()
        val bitmap=root.captureToImage().asAndroidBitmap()
        val pipes=instrumentation.uiAutomation.executeShellCommandRw("dd of=/data/local/tmp/chapark055-$name.png")
        ParcelFileDescriptor.AutoCloseOutputStream(pipes[1]).use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        ParcelFileDescriptor.AutoCloseInputStream(pipes[0]).use{it.readBytes()}
    }
    private val records=listOf(
        JourneyRecord("PLACE_CONTEXT",100,"REFERENCE",placeId="camp-nanji",label="난지캠핑장",placeMemory=PlaceMemory(listOf("강"),listOf("2026-11-13"),true)),
        JourneyRecord("PREPARATION_COMPLETED",100,"USER",placeId="camp-nanji",soc=20,socSource=Source.SDK,preparationCompleted=12,preparationRequired=12),
        JourneyRecord("GUIDANCE_OBSERVED",110,"SDK",placeId="camp-nanji"),
        JourneyRecord("PREPARATION_OMITTED",120,"USER",subject="cup",label="컵",confirmed=true,placeId="camp-nanji"),
        JourneyRecord("TRIP_COMPLETED",200,"USER",placeId="camp-nanji",soc=99,socSource=Source.SDK,readingStatus=ReadingStatus.DISCONNECTED))
    private val archive=ArchivedTrip("a","난지캠핑장","2026-11-14","Asia/Seoul",records,listOf("컵"),MemoryDetails(6,6,5,5,true,"이마트 마포점 충전시설",true))

    @Test fun moreShowsOnlyArchivedFactsAndCollapseHidesDetailsInBothThemes() {
        var state by mutableStateOf(Journey(placeId="camp-gangdong",soc=99))
        var deleted:String?=null
        content{JourneyScreen(JourneyUi(state,loaded=true,history=listOf(archive)),{},onDeleteHistory={deleted=it})}
        compose.onNodeWithText("지난 기록").performClick()
        for(dark in listOf(false,true)) {
            compose.runOnIdle{state=state.copy(dark=dark)}
            compose.onNodeWithTag("memory-details-a").assertDoesNotExist()
            compose.onNodeWithText("이 기록 삭제").assertDoesNotExist()
            capture(if(dark)"history-dark" else "history-light")
            compose.onNodeWithTag("memory-more-a").performClick()
            listOf("풍경 태그 · 강","정보 확인일 · 2026-11-13 · 일부 날짜 미확인","출발 배터리 · 20% · 차량",
                "철수 배터리 · 확인하지 못함 · 차량","출발 전 필수 확인 12/12","선택 경유지 · 이마트 마포점 충전시설",
                "내비 결과 · 안내 확인","수면 준비 6/6 · 철수 확인 5/5","다음에 챙길 것 · 컵","누락 · 컵").forEach{
                compose.onNodeWithText(it).performScrollTo().assertIsDisplayed()
            }
            val layouts=mutableListOf<TextLayoutResult>()
            compose.onNodeWithText("출발 배터리 · 20% · 차량").performScrollTo().performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(layouts)}
            assertEquals(if(dark)Color.White else Color(0xFF131417),layouts.single().layoutInput.style.color)
            capture(if(dark)"details-dark" else "details-light")
            compose.onNodeWithTag("memory-delete-a").performScrollTo().performClick()
            compose.onNodeWithText("여행 기록을 삭제할까요?").assertIsDisplayed()
            compose.onNodeWithText("취소").performClick()
            assertNull(deleted)
            compose.onNodeWithTag("memory-more-a").performScrollTo().performClick()
            compose.onNodeWithTag("memory-details-a").assertDoesNotExist()
        }
        compose.onNodeWithText("기록 닫기").assertIsDisplayed().performClick()
        compose.onNodeWithTag("memory-card-a").assertDoesNotExist()
    }

    @Test fun oldArchiveKeepsMissingDataAndCardsExpandIndependently() {
        val old=ArchivedTrip("b","이전 여행","날짜 미수집","UTC",emptyList(),emptyList())
        content{JourneyScreen(JourneyUi(Journey(),loaded=true,history=listOf(archive,old)),{})}
        compose.onNodeWithText("지난 기록").performClick()
        compose.onNodeWithTag("memory-more-b").performScrollTo().performClick()
        listOf("풍경 태그 · 미수집","출발 배터리 · 미수집","철수 배터리 · 미수집","준비 완료 수 · 미수집",
            "선택 경유지 · 미수집","내비 결과 · 미수집","수면·철수 확인 요약 · 미수집").forEach{compose.onNodeWithText(it).performScrollTo().assertIsDisplayed()}
        compose.onNodeWithTag("memory-details-a").assertDoesNotExist()
        compose.onNodeWithTag("memory-more-b").performScrollTo().performClick()
        compose.onNodeWithTag("memory-more-a").performScrollTo().performClick()
        compose.onNodeWithTag("memory-details-b").assertDoesNotExist()
    }

    @Test fun graphicCoversRemainWideAndReadableInBothThemesAndLargeText() {
        var dark by mutableStateOf(false)
        var narrow by mutableStateOf(false)
        content {
            val density=LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density,if(narrow)1.3f else 1f)) {
                IviTheme(dark){
                    Column(Modifier.width(if(narrow)800.dp else 1500.dp).fillMaxHeight().background(LocalIviColors.current.background).clipToBounds().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
                        TripMemoryCard(archive)
                        TripMemoryCard(archive.copy(id="b",placeName="강동그린웨이 가족캠핑장"))
                    }
                }
            }
        }
        for(compact in listOf(false,true))for(theme in listOf(false,true)) {
            compose.runOnIdle{dark=theme;narrow=compact}
            for(id in listOf("a","b")) {
                compose.onNodeWithTag("memory-cover-$id").performScrollTo()
                val bounds=compose.onNodeWithTag("memory-cover-$id").getUnclippedBoundsInRoot()
                assertTrue("The cover must remain horizontal",bounds.right-bounds.left>bounds.bottom-bounds.top)
                val pixels=compose.onNodeWithTag("memory-cover-$id").captureToImage().toPixelMap()
                val colors=mutableSetOf<Int>()
                for(y in 0 until pixels.height step 8)for(x in pixels.width/2 until pixels.width step 8){
                    val c=pixels[x,y];colors.add((c.red*31).toInt()*1024+(c.green*31).toInt()*32+(c.blue*31).toInt())
                }
                android.util.Log.i("MemoryCardTest","cover=$id compact=$compact colors=${colors.size}")
                assertTrue("Artwork must render distinct shades, not a flat panel: $id / $compact / ${colors.size}",colors.size>32)
                var motifPixels=0
                for(y in 0 until pixels.height step 2)for(x in pixels.width*3/5 until pixels.width step 2){
                    val c=pixels[x,y]
                    if(if(id=="a")c.red>.5f && c.red>c.blue*1.2f else c.red>.5f && c.green>.5f && c.blue>.5f)motifPixels++
                }
                assertTrue("The sun or stars must remain visible: $id / $motifPixels",motifPixels>20)
                compose.onNodeWithTag("memory-more-$id").assertIsDisplayed()
            }
            compose.onNodeWithTag("memory-cover-a").performScrollTo()
            capture("cards-${if(compact)"narrow" else "wide"}-${if(theme)"dark" else "light"}")
        }
    }

    @Test fun completedJourneyAlsoUsesCollapsedCardWithoutChangingTheTrip() {
        val state=Journey(step=6,tripId="a",completed=true,route=RoutePhase.ARRIVED,parked=true,
            sleepChecks=Journey.SLEEP_IDS,exitChecks=Journey.EXIT_IDS,records=records)
        content{JourneyScreen(JourneyUi(state,loaded=true,history=listOf(archive)),{fail("Card must not mutate journey")})}
        compose.onNodeWithTag("memory-card-a").assertExists()
        compose.onNodeWithText("출발 배터리 · 20% · 차량").assertDoesNotExist()
        compose.onNodeWithTag("memory-more-a").performScrollTo().performClick()
        compose.onNodeWithText("출발 배터리 · 20% · 차량").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("이 기록 삭제").assertDoesNotExist()
    }
}
