package com.chapark.ivi.safety

/** UX permission is not a gear, parking brake, arrival, or vehicle-control signal. */
enum class ParkingSignal { UNKNOWN, PARK, NOT_PARK }
enum class UxMode { UNKNOWN, RESTRICTED, AVAILABLE }
data class UxSnapshot(
    val mode: UxMode = UxMode.UNKNOWN,
    val restrictions: Int? = null,
    val reason: String = "차량 UX 제한 확인 중",
    val parking: ParkingSignal = ParkingSignal.UNKNOWN,
) {
    val allowsDetailedInteraction: Boolean get() = mode == UxMode.AVAILABLE && parking == ParkingSignal.PARK
}
object UxPolicy {
    fun evaluate(requiresOptimization: Boolean?, restrictions: Int?): UxMode = when {
        requiresOptimization == null || restrictions == null -> UxMode.UNKNOWN
        requiresOptimization || restrictions != 0 -> UxMode.RESTRICTED
        else -> UxMode.AVAILABLE
    }
}
