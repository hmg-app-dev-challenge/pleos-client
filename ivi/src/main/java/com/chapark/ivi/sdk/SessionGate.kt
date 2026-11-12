package com.chapark.ivi.sdk

/** Monotonic generation prevents callbacks from a released SDK session updating a new one. */
class SessionGate {
    private var generation = 0L
    private var active = false
    fun open(): Long { active = true; return ++generation }
    fun close() { active = false; generation++ }
    fun accepts(token: Long): Boolean = active && token == generation
}

enum class Connection { IDLE, CONNECTING, READY, PERMISSION_DENIED, UNSUPPORTED, TIMEOUT, ERROR, STOPPED }
data class SdkSnapshot(
    val vehicle: Connection = Connection.IDLE,
    val navigation: Connection = Connection.IDLE,
    val rawSoc: Int? = null,
    val receivedAtElapsedMs: Long? = null,
    val capability: String = "미확인",
    val detail: String = "조회 연결을 시작하면 SDK 응답을 확인합니다.",
)
