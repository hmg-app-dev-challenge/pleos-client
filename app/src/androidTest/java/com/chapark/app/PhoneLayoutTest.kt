package com.chapark.app

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import com.chapark.app.ui.TripViewModel
import com.chapark.prototype.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import java.io.File

class PhoneLayoutTest {
    @get:Rule val ui=createComposeRule()
    @Test fun productFlowFitsPhoneViewportInBothThemes(){
        val vm=TripViewModel(SavedStateHandle())
        ui.setContent { Box(Modifier.requiredSize(390.dp,844.dp).clipToBounds().testTag("phone")){ChaParkApp(vm)} }
        fun send(event:JourneyEvent)=ui.runOnIdle{vm.send(event)}
        fun check(stage:Int,dark:Boolean){
            ui.waitForIdle()
            val button=ui.onNodeWithTag("journey-next")
            button.assertIsDisplayed()
            val b=button.getUnclippedBoundsInRoot();val w=ui.onNodeWithTag("phone").getUnclippedBoundsInRoot()
            assertTrue(b.left>=w.left && b.right<=w.right && b.bottom<=w.bottom)
            listOf("시연","데모","가상","재현","SDK","미연동").forEach{ui.onAllNodes(hasText(it,substring=true)).assertCountEquals(0)}
            val dir=File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir,"verification").apply{mkdirs()}
            File(dir,"phone-${if(dark)"dark" else "light"}-$stage.png").outputStream().use{ui.onNodeWithTag("phone").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
        }
        for(dark in listOf(false,true)) {
            send(JourneyEvent.ResetScenario);send(JourneyEvent.Dark(dark))
            for(stage in 0..3){check(stage,dark);send(JourneyEvent.Next)}
            send(JourneyEvent.RequestRoute)
            ui.waitUntil(5_000){vm.state.value.route==RoutePhase.GUIDING}
            check(4,dark);send(JourneyEvent.Arrive);send(JourneyEvent.Park);send(JourneyEvent.Next)
            check(5,dark);Journey.SLEEP_IDS.forEach{send(JourneyEvent.SleepCheck(it))};send(JourneyEvent.Next)
            check(6,dark);Journey.EXIT_IDS.forEach{send(JourneyEvent.ExitCheck(it))};send(JourneyEvent.Finish)
            check(7,dark)
        }
    }
}
