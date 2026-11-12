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
    // The Pleos shared library can expose android.car even without that library name.
    return try { Class.forName("android.car.Car");CarUxMonitor(context) }
    catch (_: LinkageError) { UnavailableUxMonitor() }
    catch (_: ClassNotFoundException) { UnavailableUxMonitor() }
}

private class UnavailableUxMonitor : UxMonitor {
    private val mutable = MutableStateFlow(UxSnapshot(reason="이 플랫폼의 주행 제한 신호는 아직 연동되지 않았습니다 · 상세 조작 잠금"))
    override val state = mutable.asStateFlow()
    override fun start() = Unit
    override fun close() = Unit
}
