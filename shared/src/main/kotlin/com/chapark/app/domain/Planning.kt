package com.chapark.app.domain

enum class Condition { ALLOWED, RESTRICTED, UNKNOWN }
enum class Source { SDK, MANUAL, EMULATOR, DEMO, UNKNOWN }
enum class ReadingStatus { AVAILABLE, STALE, UNSUPPORTED, PERMISSION_DENIED, DISCONNECTED, DRIVING_RESTRICTED, INVALID, UNKNOWN }

/** Only a verified, normalized percentage may enter the decision engine. */
data class SocReading(val percent: Double?, val status: ReadingStatus, val source: Source) {
    val usable: Boolean get() = status == ReadingStatus.AVAILABLE &&
        percent != null && percent.isFinite() && percent in 0.0..100.0
}
data class Place(
    val id: String, val name: String, val description: String,
    val overnight: Condition, val nightToilet: Condition,
    val cooking: Condition, val fire: Condition, val pets: Condition,
    val demoTemperatureC: Int,
)
data class TripInput(val people: Int = 1, val cooking: Boolean = false,
    val pets: Boolean = false, val sleepingBag: Boolean = true,
    val batteryThreshold: Int = 35, val endHour: Int = 10, val schedule:TripSchedule?=null) {
    val valid get() = people > 0 && batteryThreshold in 0..100 && endHour in 0..23 && (schedule==null || schedule.valid)
}
enum class Group(val label: String) { BEFORE("출발 전 행동"), SITE("현장 확인"), GEAR("준비물") }
data class DecisionItem(val id: String, val group: Group, val title: String, val reason: String)

object PlanningRules {
    const val DEMO_SOC_THRESHOLD = 35.0
    fun evaluate(place: Place, input: TripInput, soc: SocReading): List<DecisionItem> = buildList {
        if (soc.status==ReadingStatus.UNSUPPORTED) Unit
        else if (!soc.usable) add(DecisionItem("battery-check", Group.BEFORE, "배터리 확인 필요", "유효한 잔량을 확인하지 못했어요."))
        else if (soc.percent!! < input.batteryThreshold)
            add(DecisionItem("charge-review", Group.BEFORE, "충전 경유 검토", "현재 잔량이 확인 기준 ${input.batteryThreshold}% 미만이에요. 출발 전 충전 계획을 확인하세요."))
        when (place.overnight) {
            Condition.RESTRICTED -> add(DecisionItem("overnight-denied", Group.BEFORE, "다른 장소를 선택해 주세요", "차내 숙박 제한 장소예요."))
            Condition.UNKNOWN -> add(DecisionItem("overnight-check", Group.BEFORE, "차내 숙박 조건 확인", "주차 허용과 숙박 허용은 달라요."))
            else -> Unit
        }
        if (place.nightToilet != Condition.ALLOWED)
            add(DecisionItem("toilet-${place.nightToilet}", Group.BEFORE, "야간 화장실 확인 또는 다른 장소 비교",
                if (place.nightToilet == Condition.UNKNOWN) "야간 운영이 확인되지 않았어요." else "야간에는 이용할 수 없어요."))
        if (input.cooking) when (place.cooking) {
            Condition.RESTRICTED -> add(DecisionItem("meal-ready", Group.BEFORE, "조리된 식사 준비", "이 장소는 현장 취사가 제한돼요."))
            Condition.UNKNOWN -> add(DecisionItem("cooking-check", Group.BEFORE, "취사 허용 여부 확인", "조리 도구를 정하기 전에 운영 안내를 확인하세요."))
            Condition.ALLOWED -> {
                if (place.fire == Condition.ALLOWED)
                    add(DecisionItem("cooking-gear", Group.GEAR, "조리 도구 챙김 확인", "현장 취사와 화기 사용 조건에 맞춰 준비하세요."))
                else add(DecisionItem("fire-check-${place.fire}", Group.BEFORE, "허용된 조리 방식 확인", "화기 제한이 전기 조리 허용을 뜻하지는 않아요."))
            }
        }
        if (input.pets) {
            add(DecisionItem("pets-${place.pets}", Group.BEFORE,
                if (place.pets == Condition.RESTRICTED) "반려동물 동반 가능한 다른 장소 비교" else "반려동물 출입 조건 확인", "장소의 동반 조건을 확인하세요."))
            add(DecisionItem("pet-gear", Group.GEAR, "반려동물 물·배변용품", "동반 조건에 따라 추가됐어요."))
        }
        if (place.demoTemperatureC < 10 && !input.sleepingBag)
            add(DecisionItem("warmth", Group.GEAR, "보온 침구 추가 또는 장소 재검토", "예상 기온 ${place.demoTemperatureC}°C · 침낭 미보유"))
        add(DecisionItem("bedding-count", Group.GEAR, "${input.people}명분 침구·매트 확인", "인원에 맞는 수량과 실제 잠자리를 확인하세요."))
        if (input.people >= 3) add(DecisionItem("extra-sleeping-space", Group.BEFORE, "추가 잠자리 검토", "${input.people}명이 차에서 잘 수 있다는 뜻은 아니에요. 공간과 텐트 등 추가 잠자리를 확인하세요."))
        if (input.sleepingBag) add(DecisionItem("sleeping-bag", Group.GEAR, "침낭 챙김 확인", "보유와 실제 챙김은 별개예요."))
        add(DecisionItem("water", Group.GEAR, "생수·쓰레기 봉투", "기본 준비물"))
        add(DecisionItem("site-rules", Group.SITE, "현장 운영 안내 확인", "숙박·주차·취사 조건을 현장에서 다시 확인하세요."))
    }.distinctBy { it.id }
}

/** Retain checks only when the meaning of the item has not changed. */
fun retainedChecks(old: List<DecisionItem>, new: List<DecisionItem>, checked: Set<String>): Set<String> =
    checked.filterTo(mutableSetOf()) { id -> old.find { it.id == id }?.let { it == new.find { n -> n.id == id } } == true }
