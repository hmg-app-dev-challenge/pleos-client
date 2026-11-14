package com.chapark.ivi

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.chapark.ivi.prototype.*
import com.chapark.prototype.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.File

class JourneyLayoutTest {
    @get:Rule val compose=createComposeRule()
    @Test fun allSevenStagesFitNarrowWindowInBothThemes() = verifyLayout(1f)
    @Test fun largeTextKeepsPrimaryActionInsideWindow() = verifyLayout(1.3f)
    private fun verifyLayout(fontScale:Float) {
        var current by mutableStateOf(Journey())
        compose.setContent {
            val density=androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density,fontScale)) {
            Box(Modifier.requiredSize(800.dp,1000.dp).clipToBounds().testTag("narrow-window")) {
                JourneyScreen(JourneyUi(current,loaded=true),{})
            }
            }
        }
        for(dark in listOf(false,true)) for(step in 0..6) {
            compose.runOnIdle { current=Journey(placeId="camp-nanji",step=step,dark=dark,route=if(step>=5)RoutePhase.ARRIVED else RoutePhase.NONE,
                parked=step>=5,sleepChecks=if(step==6)Journey.SLEEP_IDS else emptySet(),sleepReady=step==6) }
            compose.waitForIdle()
            compose.onNodeWithTag("journey-next").assertIsDisplayed()
            val window=compose.onNodeWithTag("narrow-window").getUnclippedBoundsInRoot()
            val button=compose.onNodeWithTag("journey-next").getUnclippedBoundsInRoot()
            assertTrue(button.left>=window.left && button.right<=window.right && button.bottom<=window.bottom)
            if(step in setOf(0,1,2,5,6)) {
                val dir=File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir,"verification").apply{mkdirs()}
                File(dir,"narrow-$fontScale-${if(dark)"dark" else "light"}-$step.png").outputStream().use {
                    compose.onNodeWithTag("narrow-window").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)
                }
            }
        }
    }
}
