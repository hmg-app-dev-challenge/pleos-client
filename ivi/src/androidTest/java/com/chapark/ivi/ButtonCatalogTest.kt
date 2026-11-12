package com.chapark.ivi

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.chapark.ivi.design.ButtonCatalog
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

class ButtonCatalogTest {
    @get:Rule val compose=createComposeRule()
    @Test fun enabledClicksDisabledStatesAndThemeSwitch() {
        compose.setContent { ButtonCatalog {} }
        compose.onAllNodesWithText("Label")[0].performClick()
        compose.onNodeWithText("눌림은 터치 중 확인 · 실행 횟수 1 · 비활성 버튼은 실행되지 않음").assertExists()
        compose.onAllNodesWithText("Label")[1].assertIsNotEnabled()
        screenshot("buttons-light.png")
        compose.onAllNodesWithText("Label")[0].performTouchInput { down(center) }
        // Scroll containers defer the press indication until tap timeout.
        compose.mainClock.advanceTimeBy(250)
        compose.waitForIdle()
        screenshot("buttons-pressed.png")
        compose.onAllNodesWithText("Label")[0].performTouchInput { up() }
        compose.onNodeWithText("Dark 보기").performClick()
        compose.onNodeWithText("Light 보기").assertExists()
        screenshot("buttons-dark.png")
        compose.onNodeWithText("긴 문구가 들어가는 버튼은 한 줄로 표시됩니다").performScrollTo().assertIsDisplayed()
        screenshot("buttons-dark-bottom.png")
    }
    private fun screenshot(name:String) {
        val dir=File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir,"verification").apply { mkdirs() }
        File(dir,name).outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
}
