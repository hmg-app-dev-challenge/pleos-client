package com.chapark.ivi.safety

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface UxMonitor : AutoCloseable {
    val state: StateFlow<UxSnapshot>
    fun start()
}

fun createUxMonitor(context: Context): UxMonitor {
    // Pleos Connect 2.1.0 image does not advertise the AAOS android.car library.
    // Never instantiate AAOS classes on a platform that cannot load them.
    if (context.packageManager.systemSharedLibraryNames?.contains("android.car") != true)
        return UnavailableUxMonitor()
    return try { CarUxMonitor(context) } catch (_: LinkageError) { UnavailableUxMonitor() }
}

private class UnavailableUxMonitor : UxMonitor {
    private val mutable = MutableStateFlow(UxSnapshot(reason="이 플랫폼의 주행 제한 신호는 아직 연동되지 않았습니다 · 상세 조작 잠금"))
    override val state = mutable.asStateFlow()
    override fun start() = Unit
    override fun close() = Unit
}
