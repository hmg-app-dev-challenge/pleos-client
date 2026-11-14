package com.chapark.app.domain

enum class Condition { ALLOWED, RESTRICTED, UNKNOWN }
enum class Source { SDK, MANUAL, EMULATOR, DEMO, UNKNOWN }
enum class BatteryCapability { UNKNOWN, READ, READ_WRITE, UNSUPPORTED }
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
enum class CookingPlan(val label:String) { NONE("취사 안 함"), SIMPLE("간편식"), COOKING("취사 예정");
    companion object { fun fromLegacy(value:Boolean)=if(value)COOKING else NONE }
}
/** Compatibility defaults, not vehicle capacity or a safety threshold. Final MVP policies remain configurable. */
object TripPolicy {
    const val RULE_VERSION="mvp-v02-rules-3"
    const val MAX_PEOPLE=Int.MAX_VALUE
    const val DEFAULT_SOC_THRESHOLD=35
    const val DEFAULT_END_HOUR=10
}
data class TripInput(val people: Int = 1, val cookingPlan: CookingPlan = CookingPlan.NONE,
    val pets: Boolean = false, val sleepingBag: Boolean = true,
    val batteryThreshold: Int = TripPolicy.DEFAULT_SOC_THRESHOLD, val endHour: Int = TripPolicy.DEFAULT_END_HOUR, val schedule:TripSchedule?=null,
    val cookingNeedsReview:Boolean=false) {
    val valid get() = people in 1..TripPolicy.MAX_PEOPLE && batteryThreshold in 0..100 && endHour in 0..23 && (schedule==null || schedule.valid)
}
enum class Group(val label: String) { BEFORE("출발 전 할 일"), SITE("현장에서 할 일"), GEAR("챙길 것") }
enum class PreparationCategory { PACK, BEFORE_DEPARTURE, ON_SITE, CHECKOUT }
enum class PreparationStatus { PENDING, COMPLETED, OMITTED }
data class DecisionItem(val id: String, val group: Group, val title: String, val reason: String,
    val quantity:Int?=null,val required:Boolean=true,
    val category:PreparationCategory=when(group){Group.GEAR->PreparationCategory.PACK;Group.BEFORE->PreparationCategory.BEFORE_DEPARTURE;Group.SITE->PreparationCategory.ON_SITE}) {
    init { require(quantity==null || quantity>0) }
}
data class PreparationProgress(val completed:Int,val total:Int) { val ready get()=completed==total }
fun preparationProgress(items:List<DecisionItem>,checked:Set<String>,omitted:Set<String>)=items.filter{it.required}.let { required->
    PreparationProgress(required.count{it.id in checked && it.id !in omitted},required.size)
}

object PlanningRules {
    const val DEMO_SOC_THRESHOLD = 35.0
    fun evaluate(place: Place, input: TripInput, soc: SocReading): List<DecisionItem> = buildList {
        add(DecisionItem("battery-threshold", Group.BEFORE, "배터리 확인 기준 ${input.batteryThreshold}% 확인",
            "이번 여행에서 잔량이 ${input.batteryThreshold}% 미만이면 충전 경유를 검토해요. 사용 가능 시간이나 안전을 보장하는 기준은 아니에요."))
        if (!soc.usable) add(DecisionItem("battery-check", Group.BEFORE, "배터리 확인 필요", "배터리 잔량을 확인하지 못해 충전 경유를 판단할 수 없어요."))
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
        if(input.cookingPlan==CookingPlan.SIMPLE) add(DecisionItem("simple-meal",Group.GEAR,"간편식 챙김 확인","현장 취사 없이 먹을 수 있는 식사를 준비하세요."))
        if (input.cookingPlan==CookingPlan.COOKING) when (place.cooking) {
            Condition.RESTRICTED -> add(DecisionItem("meal-ready", Group.BEFORE, "조리된 식사 준비", "취사 불가 장소예요. 식사 계획을 바꿔 주세요."))
            Condition.UNKNOWN -> add(DecisionItem("cooking-check", Group.BEFORE, "취사 허용 여부 확인", "취사 가능 여부를 출처에서 다시 확인해 주세요."))
            Condition.ALLOWED -> {
                add(DecisionItem("cooking-food",Group.GEAR,"식재료 챙김 확인","취사 예정에 맞는 식재료를 준비하세요."))
                add(DecisionItem("cooking-cleanup",Group.GEAR,"취사 정리 용품","취사 후 사용한 도구와 주변을 정리해요."))
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
        add(DecisionItem("sleeping-mat",Group.GEAR,"매트 ${input.people}개","함께하는 인원에 맞춰 준비하세요.",quantity=input.people))
        add(DecisionItem("bedding-set",Group.GEAR,"침구 ${input.people}세트","함께하는 인원에 맞춰 준비하세요.",quantity=input.people))
        if (input.people >= 3) add(DecisionItem("extra-sleeping-space", Group.BEFORE, "추가 잠자리 검토", "${input.people}명이 차에서 잘 수 있다는 뜻은 아니에요. 공간과 텐트 등 추가 잠자리를 확인하세요."))
        if (input.sleepingBag) add(DecisionItem("sleeping-bag", Group.GEAR, "침낭 챙김 확인", "보유와 실제 챙김은 별개예요."))
        add(DecisionItem("place-recheck",Group.BEFORE,"장소 이용 조건 재확인","출발 전 운영 시간과 주차·숙박 조건을 출처에서 다시 확인해 주세요."))
        add(DecisionItem("phone-charge",Group.BEFORE,"휴대폰 충전 확인","길 안내와 연락에 사용할 휴대폰을 충전해 주세요."))
        add(DecisionItem("drinking-water",Group.GEAR,"식수 챙김 확인","함께하는 인원과 일정에 맞춰 마실 물을 준비하세요."))
        add(DecisionItem("waste-kit",Group.GEAR,"쓰레기 회수 용품","가져간 쓰레기를 회수할 봉투와 용품을 준비하세요."))
        add(DecisionItem("site-rules", Group.SITE, "현장 운영 안내 확인", "숙박·주차·취사 조건을 현장에서 다시 확인하세요."))
    }.distinctBy { it.id }
}

/** Retain checks only when the meaning of the item has not changed. */
fun retainedChecks(old: List<DecisionItem>, new: List<DecisionItem>, checked: Set<String>): Set<String> =
    checked.filterTo(mutableSetOf()) { id -> old.find { it.id == id }?.let { it == new.find { n -> n.id == id } } == true }
