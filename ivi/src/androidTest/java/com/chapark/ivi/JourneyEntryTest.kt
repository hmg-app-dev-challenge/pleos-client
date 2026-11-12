package com.chapark.ivi

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import java.io.File

class JourneyEntryTest {
    @get:Rule val compose=createAndroidComposeRule<IviActivity>()
    private fun waitText(text:String) { compose.waitUntil(15_000){compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()} }
    @Test fun cleanProductEntryAndSettings() {
        waitText("설정");compose.onNodeWithText("설정").performClick()
        compose.onNodeWithText("새 여행 계획").performScrollTo().performClick()
        waitText("여행 계획을 새로 만들까요?");compose.onNodeWithText("처음부터").performClick()
        waitText("오늘 밤 차박 시작");waitText("기기에 저장됨")
        waitText("배터리 20% · 에뮬레이터")
        compose.onNodeWithText("1인 · 간단 조리 예정 · 침낭 보유").assertExists()
        listOf("시연","데모","가상","재현","SDK","미연동").forEach { word ->
            compose.onAllNodes(hasText(word,substring=true)).assertCountEquals(0)
        }
        val dir=File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir,"verification").apply{mkdirs()}
        File(dir,"s1-ready.png").outputStream().use{compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
}
