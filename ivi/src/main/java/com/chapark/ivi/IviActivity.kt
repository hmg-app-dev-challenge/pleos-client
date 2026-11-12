package com.chapark.ivi

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chapark.ivi.safety.*
import com.chapark.ivi.prototype.*

class IviActivity:ComponentActivity() {
    private lateinit var uxMonitor:UxMonitor
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        uxMonitor=createUxMonitor(this)
        val localPrototype=BuildConfig.DEBUG && (Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"))
        setContent {
            val ux by uxMonitor.state.collectAsStateWithLifecycle()
            val vm:JourneyViewModel=viewModel()
            val ui by vm.state.collectAsStateWithLifecycle()
            val presented=if(localPrototype)ui else ui.copy(journey=ui.journey.copy(restricted=!ux.allowsDetailedInteraction))
            JourneyScreen(presented,vm::send)
        }
    }
    override fun onStart(){super.onStart();uxMonitor.start()}
    override fun onStop(){uxMonitor.close();super.onStop()}
    override fun onDestroy(){uxMonitor.close();super.onDestroy()}
}
