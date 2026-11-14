package com.chapark.ivi.safety

import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.chapark.ivi.sdk.SessionGate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Listen only while visible. Disconnection/error returns to UNKNOWN immediately. */
class CarUxMonitor(context: Context) : UxMonitor {
    private val context = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val gate = SessionGate()
    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null
    private var properties: CarPropertyManager? = null
    private var gearListener: CarPropertyManager.CarPropertyEventCallback? = null
    private val mutable = MutableStateFlow(UxSnapshot())
    override val state = mutable.asStateFlow()

    override fun start() {
        check(Looper.myLooper() == Looper.getMainLooper())
        close()
        val token = gate.open()
        var connectionEpoch = 0L
        var latestUx: CarUxRestrictions? = null
        mutable.value = UxSnapshot()
        fun publish(value: CarUxRestrictions?) {
            if (!gate.accepts(token)) return
            latestUx = value
            val mode = UxPolicy.evaluate(value?.isRequiresDistractionOptimization, value?.activeRestrictions)
            mutable.value = UxSnapshot(mode, value?.activeRestrictions, when (mode) {
                UxMode.UNKNOWN -> "차량 UX 제한을 확인할 수 없습니다"
                UxMode.RESTRICTED -> "차량 UX 제한으로 상세 조작이 잠겼습니다"
                UxMode.AVAILABLE -> when (mutable.value.parking) {
                    ParkingSignal.PARK -> "P단 및 플랫폼 상세 조작 허용 확인"
                    ParkingSignal.NOT_PARK -> "P단에서 상세 조작할 수 있습니다"
                    ParkingSignal.UNKNOWN -> "차량 P단 신호 확인 중 · 상세 조작 잠금"
                }
            }, mutable.value.parking)
        }
        handler.postDelayed({
            if (gate.accepts(token) && mutable.value.mode == UxMode.UNKNOWN)
                mutable.value = UxSnapshot(reason = "차량 UX 제한 응답 대기 · 상세 조작 잠금")
        }, 8_000)
        try {
            car = Car.createCar(context, handler, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { client, ready ->
                // Queue even synchronous lifecycle callbacks until createCar has returned.
                handler.post {
                    if (!gate.accepts(token)) return@post
                    val epoch = ++connectionEpoch
                    if (!ready) {
                        manager = null
                        properties = null; gearListener = null
                        mutable.value = UxSnapshot(reason = "차량 서비스 연결 해제 · 상세 조작 잠금")
                    } else try {
                        manager?.unregisterListener()
                        gearListener?.let { properties?.unregisterCallback(it) }
                        mutable.value = UxSnapshot()
                        latestUx = null
                        val prop = client.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
                        properties = prop
                        fun publishGear(value: CarPropertyValue<*>?) {
                            if (!gate.accepts(token) || connectionEpoch != epoch) return
                            val gear = value?.value as? Int
                            val parking = when {
                                value?.status != CarPropertyValue.STATUS_AVAILABLE || gear == null || gear == VehicleGear.GEAR_UNKNOWN -> ParkingSignal.UNKNOWN
                                gear == VehicleGear.GEAR_PARK -> ParkingSignal.PARK
                                else -> ParkingSignal.NOT_PARK
                            }
                            mutable.value = mutable.value.copy(parking = parking)
                            publish(latestUx)
                        }
                        val callback = object : CarPropertyManager.CarPropertyEventCallback {
                            override fun onChangeEvent(value: CarPropertyValue<*>) {
                                if (value.propertyId == VehiclePropertyIds.GEAR_SELECTION) handler.post { publishGear(value) }
                            }
                            override fun onErrorEvent(propertyId: Int, areaId: Int) {
                                if (propertyId == VehiclePropertyIds.GEAR_SELECTION) handler.post { publishGear(null) }
                            }
                        }
                        gearListener = callback
                        try {
                            if (prop?.registerCallback(callback, VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE) == true)
                                publishGear(prop.getProperty<Int>(VehiclePropertyIds.GEAR_SELECTION, 0))
                        } catch (_: Exception) { publishGear(null) }
                        val ux = client.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
                        manager = ux
                        ux?.registerListener { value ->
                            if (connectionEpoch == epoch) publish(value)
                        }
                        publish(ux?.currentCarUxRestrictions)
                    } catch (e: Exception) {
                        mutable.value = UxSnapshot(reason = "UX 제한 조회 실패 · ${e.javaClass.simpleName}")
                    }
                }
            }
        } catch (e: Exception) {
            mutable.value = UxSnapshot(reason = "차량 서비스 사용 불가 · ${e.javaClass.simpleName}")
        }
    }

    override fun close() {
        check(Looper.myLooper() == Looper.getMainLooper())
        gate.close()
        handler.removeCallbacksAndMessages(null)
        runCatching { manager?.unregisterListener() }; manager = null
        runCatching { gearListener?.let { properties?.unregisterCallback(it) } }; gearListener = null; properties = null
        runCatching { car?.disconnect() }; car = null
        mutable.value = UxSnapshot(reason = "화면 종료 · 상세 조작 잠금")
    }
}
