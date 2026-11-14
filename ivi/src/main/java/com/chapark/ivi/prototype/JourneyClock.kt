package com.chapark.ivi.prototype

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.time.Instant

/** One minute cadence while visible, immediate refresh on return. No DataStore writes. */
@Composable internal fun journeyTime():Instant {
    val owner=LocalLifecycleOwner.current
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while(true) {
                now=Instant.now()
                delay(60_000)
            }
        }
    }
    return now
}
