package com.chapark.prototype

import com.chapark.app.data.DemoPlaces
import com.chapark.app.data.PlaceCatalog
import com.chapark.app.domain.*

enum class JourneyStage { PLANNING, PREPARING, MOVING, STAYING, COMPLETED }

enum class RoutePhase { NONE, REQUESTED, GUIDING, FAILED, ARRIVED }
data class Journey(
    val step:Int=0, val input:TripInput=TripInput(cookingPlan=com.chapark.app.domain.CookingPlan.COOKING), val placeId:String="demo-b",
    val soc:Int?=20, val checked:Set<String> = emptySet(), val omitted:Set<String> = emptySet(),
    val route:RoutePhase=RoutePhase.NONE, val parked:Boolean=false,
    val sleepChecks:Set<String> = emptySet(), val exitChecks:Set<String> = emptySet(),
    val sleepReady:Boolean=false, val completed:Boolean=false,
    val restricted:Boolean=false, val dark:Boolean=false,
    val tripId:String?=null, val chargerId:String?=null, val restMode:String="REST",
    val records:List<JourneyRecord> = emptyList(),
    val forgotten:Set<String> = emptySet(), val reminders:Set<String> = emptySet(), val followUps:Set<String> = emptySet(),
    val batteryStatus:ReadingStatus=ReadingStatus.AVAILABLE,
    val socSource:Source=Source.DEMO, val socReceivedAtMillis:Long?=null,
) {
    /** Display stage only: COMPLETED includes checkout UI; completed is the actual persisted fact. */
    val stage:JourneyStage? get()=when(step){
        0->null;1,2->JourneyStage.PLANNING;3->JourneyStage.PREPARING
        4->if(route==RoutePhase.ARRIVED)JourneyStage.STAYING else JourneyStage.MOVING
        5->JourneyStage.STAYING;else->JourneyStage.COMPLETED
    }
    fun preparationStatus(id:String)=when {id in omitted->PreparationStatus.OMITTED;id in checked->PreparationStatus.COMPLETED;else->PreparationStatus.PENDING}
    val batteryVisible get()=batteryStatus!=ReadingStatus.UNSUPPORTED
    val place get()=PlaceCatalog.find(placeId) ?: Place(placeId,"장소 정보 확인 필요","저장된 장소 자료를 확인해 주세요.",Condition.UNKNOWN,Condition.UNKNOWN,Condition.UNKNOWN,Condition.UNKNOWN,Condition.UNKNOWN,12)
    val decisions get()=followUps.sorted().map{DecisionItem("followup-$it",Group.BEFORE,"다시 확인 · $it","지난 여행에서 확인하지 못한 정보예요. 이번 장소의 조건도 확인해 주세요.")} + reminders.sorted().map{DecisionItem("reminder-$it",Group.GEAR,"다시 챙기기 · $it","지난 여행에서 빠뜨렸다고 기록한 준비물이에요.")} + PlanningRules.evaluate(place,input,SocReading(soc?.toDouble(),
        if(soc==null && batteryStatus==ReadingStatus.AVAILABLE)ReadingStatus.UNKNOWN else batteryStatus,socSource))
    val checkedCount get()=decisions.count { it.id in checked }
    val departureItems get()=decisions.filter{it.group!=Group.SITE}
    val siteItems get()=decisions.filter{it.group==Group.SITE}
    val departureProgress get()=preparationProgress(departureItems,checked,omitted)
    val departureCheckedCount get()=departureProgress.completed
    val departureBlockReason:String? get()=when {
        input.schedule==null -> "여행 날짜를 설정해 주세요."
        input.schedule.departureEnd(input.endHour)==null -> "여행 날짜·시간대와 철수 시각을 확인해 주세요."
        input.cookingNeedsReview -> "취사 계획을 다시 선택해 주세요."
        !input.valid -> "여행 조건을 확인해 주세요."
        departureCheckedCount<departureProgress.total -> "출발 전 항목 ${departureProgress.total-departureCheckedCount}개를 더 확인해 주세요."
        else -> null
    }
    val canDepart get()=input.valid && (input.schedule?.departureEnd(input.endHour)!=null) && !input.cookingNeedsReview && departureProgress.ready
    val siteReady get()=preparationProgress(siteItems,checked,omitted).ready
    val canSleep get()=parked && route==RoutePhase.ARRIVED
    val canFinish get()=canSleep && sleepReady && exitChecks.containsAll(EXIT_IDS)
    companion object {
        val SLEEP_IDS=setOf("parking","ventilation","climate","lights")
        val EXIT_IDS=setOf("settings","gear","waste","surroundings")
    }
}
sealed interface JourneyEvent {
    data object Next:JourneyEvent
    data object Back:JourneyEvent
    data class PlanningTab(val index:Int):JourneyEvent
    data class Conditions(val value:TripInput):JourneyEvent
    data class Place(val id:String):JourneyEvent
    data class Charger(val id:String?):JourneyEvent
    data class Battery(val value:Int?,val status:ReadingStatus,val source:Source,val receivedAt:Long?):JourneyEvent
    data class Soc(val value:Int?,val receivedAt:Long?=null):JourneyEvent
    data class Check(val id:String):JourneyEvent
    data class Omit(val id:String):JourneyEvent
    data object RequestRoute:JourneyEvent
    data class RouteResponse(val success:Boolean):JourneyEvent
    data object NavigationLost:JourneyEvent
    data object Arrive:JourneyEvent
    data object Park:JourneyEvent
    data class SleepCheck(val id:String):JourneyEvent
    data class ExitCheck(val id:String):JourneyEvent
    data class RestMode(val mode:String):JourneyEvent
    data class Forgotten(val name:String):JourneyEvent
    data object Finish:JourneyEvent
    data object NewTrip:JourneyEvent
    data object ResetScenario:JourneyEvent
    data class Driving(val value:Boolean):JourneyEvent
    data class Dark(val value:Boolean):JourneyEvent
}

/** Pure journey transitions; platform adapters supply events without direct SDK calls here. */
object JourneyReducer {
    fun apply(old:Journey,event:JourneyEvent):Journey {
        if(event==JourneyEvent.ResetScenario) return if(old.restricted)old else Journey(dark=old.dark)
        if(event is JourneyEvent.Driving) return old.copy(restricted=event.value)
        if(event is JourneyEvent.Dark) return old.copy(dark=event.value)
        if(event is JourneyEvent.Battery){
            if(old.completed)return old
            // A failed refresh cannot relabel or erase the user's explicit reading.
            // A valid vehicle value, unsupported capability, or new trip replaces it.
            if(old.socSource==Source.MANUAL && event.status !in setOf(ReadingStatus.AVAILABLE,ReadingStatus.UNSUPPORTED))return old
            val next=old.copy(soc=event.value?.takeIf{it in 0..100 && event.status==ReadingStatus.AVAILABLE},batteryStatus=event.status,socSource=event.source,socReceivedAtMillis=event.receivedAt)
            return if(old.step<=3)next.copy(omitted=retainedChecks(old.decisions,next.decisions,old.omitted),checked=retainedChecks(old.decisions,next.decisions,old.checked)) else next
        }
        if(old.restricted) return old
        if(event==JourneyEvent.NewTrip) return Journey(input=old.input.copy(schedule=null),dark=old.dark,reminders=old.forgotten+old.decisions.filter{it.id in old.omitted}.map{it.title}+old.reminders.filterNot{"reminder-$it" in old.checked},
            followUps=old.records.filter{it.kind=="PLACE_INFO" && it.confirmed==false && it.label?.substringBefore(" · ") in setOf("차내 숙박","야간 화장실","운영 조건")}
                .map{"${old.place.name} · ${it.label!!.substringBefore(" · ")}"}.toSet()+old.followUps.filterNot{"followup-$it" in old.checked})
        if(old.completed) return old
        fun update(next:Journey):Journey {
            if(old.step>3) return old
            return next.copy(omitted=retainedChecks(old.decisions,next.decisions,old.omitted),checked=retainedChecks(old.decisions,next.decisions,old.checked),
                route=RoutePhase.NONE,parked=false,sleepChecks=emptySet(),exitChecks=emptySet(),sleepReady=false)
        }
        fun toggle(set:Set<String>,id:String)=if(id in set)set-id else set+id
        return when(event) {
            JourneyEvent.Next -> when(old.step) {
                in 0..2 -> old.copy(step=old.step+1)
                3 -> if(old.canDepart) old.copy(step=4) else old
                4 -> if(old.canSleep) old.copy(step=5) else old
                5 -> if(old.canSleep && old.siteReady && old.sleepChecks.containsAll(Journey.SLEEP_IDS)) old.copy(step=6,sleepReady=true) else old
                else -> old
            }
            is JourneyEvent.PlanningTab -> if(old.step in 1..2 && event.index in 0..1)old.copy(step=event.index+1) else old
            JourneyEvent.Back -> when {
                old.step==4 -> old.copy(step=3,route=RoutePhase.NONE,parked=false,sleepChecks=emptySet(),exitChecks=emptySet(),sleepReady=false)
                old.step>0 -> old.copy(step=old.step-1)
                else -> old
            }
            is JourneyEvent.Conditions -> if(event.value.valid) update(old.copy(input=event.value)) else old
            is JourneyEvent.Charger -> if(event.id==null || com.chapark.app.data.ChargingPlaces.find(event.id)!=null)update(old.copy(chargerId=event.id,checked=old.checked-"charge-review")) else old
            is JourneyEvent.Place -> if(PlaceCatalog.all.any{it.id==event.id}) update(old.copy(placeId=event.id)) else old
            is JourneyEvent.Soc -> if(old.batteryVisible && (event.value==null || event.value in 0..100))
                update(old.copy(soc=event.value,socSource=Source.MANUAL,socReceivedAtMillis=event.receivedAt,
                    batteryStatus=if(event.value==null)ReadingStatus.UNKNOWN else ReadingStatus.AVAILABLE)) else old
            is JourneyEvent.Check -> if((old.step==3 && old.departureItems.any{it.id==event.id} || old.step==5 && old.canSleep && old.siteItems.any{it.id==event.id})) old.copy(checked=toggle(old.checked,event.id),omitted=old.omitted-event.id) else old
            is JourneyEvent.Omit -> if((old.step==3 && old.departureItems.any{it.id==event.id}) || (old.step==5 && old.canSleep && old.siteItems.any{it.id==event.id}))
                old.copy(omitted=toggle(old.omitted,event.id),checked=old.checked-event.id) else old
            JourneyEvent.RequestRoute -> if(old.step==4 && old.route in setOf(RoutePhase.NONE,RoutePhase.FAILED)) old.copy(route=RoutePhase.REQUESTED) else old
            is JourneyEvent.RouteResponse -> if(old.step==4 && old.route==RoutePhase.REQUESTED) old.copy(route=if(event.success)RoutePhase.GUIDING else RoutePhase.FAILED) else old
            JourneyEvent.NavigationLost -> if(old.step==4 && old.route in setOf(RoutePhase.REQUESTED,RoutePhase.GUIDING)) old.copy(route=RoutePhase.FAILED,parked=false) else old
            JourneyEvent.Arrive -> if(old.step==4 && old.route==RoutePhase.GUIDING) old.copy(route=RoutePhase.ARRIVED,parked=false) else old
            JourneyEvent.Park -> if(old.step==4 && old.route==RoutePhase.ARRIVED) old.copy(parked=true) else old
            is JourneyEvent.SleepCheck -> if(old.step==5 && old.canSleep && event.id in Journey.SLEEP_IDS) old.copy(sleepChecks=toggle(old.sleepChecks,event.id),sleepReady=false) else old
            is JourneyEvent.ExitCheck -> if(old.step==6 && event.id in Journey.EXIT_IDS) old.copy(exitChecks=toggle(old.exitChecks,event.id)) else old
            is JourneyEvent.RestMode -> if(old.step==5 && event.mode in setOf("REST","SLEEP","STARS"))old.copy(restMode=event.mode) else old
            is JourneyEvent.Forgotten -> {
                val name=event.name.trim()
                if(old.step==6 && name.isNotBlank() && name.length<=100)old.copy(forgotten=toggle(old.forgotten,name)) else old
            }
            JourneyEvent.Finish -> if(old.step==6 && old.canFinish) old.copy(completed=true) else old
            else -> old
        }
    }
}
