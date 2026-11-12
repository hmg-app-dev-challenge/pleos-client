package com.chapark.ivi.sdk

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.navi.helper.NaviHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Read-only integration spike. No route request, write API, fake SOC or invented unit conversion. */
class PleosSession(context: Context) : AutoCloseable {
    private val context = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val gate = SessionGate()
    private var vehicle: Vehicle? = null
    private var navigation: NaviHelper? = null
    private val mutable = MutableStateFlow(SdkSnapshot())
    val state = mutable.asStateFlow()

    fun start() {
        check(Looper.myLooper() == Looper.getMainLooper())
        close()
        val token = gate.open()
        mutable.value = SdkSnapshot(vehicle = Connection.CONNECTING, navigation = Connection.CONNECTING)
        fun post(action: () -> Unit) { handler.post { if (gate.accepts(token)) action() } }
        fun failVehicle(error: Throwable) = post {
            if (mutable.value.vehicle == Connection.CONNECTING) {
                mutable.value = mutable.value.copy(vehicle = if (error is SecurityException) Connection.PERMISSION_DENIED else Connection.ERROR,
                    rawSoc = null, receivedAtElapsedMs = null, detail = "차량 조회 실패 · ${error.javaClass.simpleName}")
            }
        }
        handler.postDelayed({
            if (gate.accepts(token)) {
                val old = mutable.value
                mutable.value = old.copy(
                    vehicle = if (old.vehicle == Connection.CONNECTING) Connection.TIMEOUT else old.vehicle,
                    navigation = if (old.navigation == Connection.CONNECTING) Connection.TIMEOUT else old.navigation)
            }
        }, 8_000)
        if (context.checkSelfPermission("pleos.car.permission.CAR_ENERGY") != PackageManager.PERMISSION_GRANTED) {
            mutable.value = mutable.value.copy(vehicle = Connection.PERMISSION_DENIED, detail = "차량 에너지 조회 권한이 없습니다. 프로젝트 승인과 기기 권한을 확인하세요.")
        } else try {
            val client = Vehicle(context).also { vehicle = it }
            client.initialize()
            val battery = client.getEvBattery()
            battery.checkEvBatteryStateOfChargeCapabilities({ capability -> post {
                if (mutable.value.vehicle != Connection.CONNECTING) return@post
                mutable.value = mutable.value.copy(capability = capability.accessType.name)
                if (capability.accessType !in setOf(AccessType.READ, AccessType.READ_WRITE)) {
                    mutable.value = mutable.value.copy(vehicle = Connection.UNSUPPORTED, detail = "SOC 읽기를 지원하지 않습니다.")
                } else try {
                    battery.getEvBatteryStateOfCharge({ raw -> post {
                        if (mutable.value.vehicle == Connection.CONNECTING) {
                            mutable.value = mutable.value.copy(vehicle = Connection.READY, rawSoc = raw,
                                receivedAtElapsedMs = SystemClock.elapsedRealtime(), detail = "SDK 응답 수신 · 단위 검증 전이므로 준비 판단에는 사용하지 않습니다.")
                            handler.postDelayed({ if (gate.accepts(token)) mutable.value = mutable.value.copy(
                                vehicle = Connection.STOPPED, rawSoc = null, receivedAtElapsedMs = null, detail = "조회값 유효 시간 60초가 지났습니다. 다시 조회하세요.") }, 60_000)
                        }
                    } }, { failVehicle(it) })
                } catch (e: Exception) { failVehicle(e) }
            } }, { failVehicle(it) })
        } catch (e: Exception) { failVehicle(e) }
        try {
            val client = NaviHelper(context).also { navigation = it }
            client.initialize({ post {
                if (mutable.value.navigation == Connection.CONNECTING) mutable.value = mutable.value.copy(navigation = Connection.READY)
            } }, { error -> post {
                if (mutable.value.navigation == Connection.CONNECTING) mutable.value = mutable.value.copy(
                    navigation = if (error is SecurityException) Connection.PERMISSION_DENIED else Connection.ERROR)
            } })
        } catch (e: Exception) {
            mutable.value = mutable.value.copy(navigation = if (e is SecurityException) Connection.PERMISSION_DENIED else Connection.ERROR)
        }
    }
    override fun close() {
        check(Looper.myLooper() == Looper.getMainLooper())
        gate.close()
        handler.removeCallbacksAndMessages(null)
        runCatching { vehicle?.release() }; vehicle = null
        runCatching { navigation?.release() }; navigation = null
        mutable.value = mutable.value.copy(vehicle = Connection.STOPPED, navigation = Connection.STOPPED,
            rawSoc = null, receivedAtElapsedMs = null, detail = "화면 종료로 SDK 연결을 해제했습니다.")
    }
}
