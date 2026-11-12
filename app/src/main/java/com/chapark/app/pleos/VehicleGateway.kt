package com.chapark.app.pleos

/** No SDK dependency until project access, permissions, units and signatures are verified. */
sealed interface ActionResult {
    data class Unavailable(val reason: String) : ActionResult
}
interface VehicleGateway {
    fun requestNavigation(): ActionResult
    fun applySleepSettings(): ActionResult
}
class UnconnectedVehicleGateway : VehicleGateway {
    override fun requestNavigation() = ActionResult.Unavailable("차량 내비에서 목적지와 경로를 확인해 주세요.")
    override fun applySleepSettings() = ActionResult.Unavailable("차량 기본 설정에서 직접 확인해 주세요.")
}
