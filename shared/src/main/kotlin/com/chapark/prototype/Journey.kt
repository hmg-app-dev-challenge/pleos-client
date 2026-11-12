package com.chapark.prototype

import com.chapark.app.data.DemoPlaces
import com.chapark.app.domain.*

enum class RoutePhase { NONE, REQUESTED, GUIDING, FAILED, ARRIVED }
data class Journey(
    val step:Int=0, val input:TripInput=TripInput(cooking=true), val placeId:String="demo-b",
    val soc:Int?=20, val checked:Set<String> = emptySet(),
    val route:RoutePhase=RoutePhase.NONE, val parked:Boolean=false,
    val sleepChecks:Set<String> = emptySet(), val exitChecks:Set<String> = emptySet(),
    val sleepReady:Boolean=false, val completed:Boolean=false,
    val restricted:Boolean=false, val dark:Boolean=false,
) {
    val place get()=DemoPlaces.all.firstOrNull { it.id==placeId } ?: DemoPlaces.all.first()
    val decisions get()=PlanningRules.evaluate(place,input,SocReading(soc?.toDouble(),
        if(soc==null)ReadingStatus.UNKNOWN else ReadingStatus.AVAILABLE,Source.DEMO))
    val checkedCount get()=decisions.count { it.id in checked }
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
    data class Conditions(val value:TripInput):JourneyEvent
    data class Place(val id:String):JourneyEvent
    data class Soc(val value:Int?):JourneyEvent
    data class Check(val id:String):JourneyEvent
    data object RequestRoute:JourneyEvent
    data class RouteResponse(val success:Boolean):JourneyEvent
    data object Arrive:JourneyEvent
    data object Park:JourneyEvent
    data class SleepCheck(val id:String):JourneyEvent
    data class ExitCheck(val id:String):JourneyEvent
    data object Finish:JourneyEvent
    data object NewTrip:JourneyEvent
    data object ResetScenario:JourneyEvent
    data class Driving(val value:Boolean):JourneyEvent
    data class Dark(val value:Boolean):JourneyEvent
}

/** Simulator only. No SDK input or command can enter this reducer. */
object JourneyReducer {
    fun apply(old:Journey,event:JourneyEvent):Journey {
        if(event==JourneyEvent.ResetScenario) return Journey()
        if(event is JourneyEvent.Driving) return old.copy(restricted=event.value)
        if(event is JourneyEvent.Dark) return old.copy(dark=event.value)
        if(old.restricted) return old
        if(event==JourneyEvent.NewTrip) return Journey(input=old.input,dark=old.dark)
        if(old.completed) return old
        fun update(next:Journey):Journey {
            if(old.step>3) return old
            return next.copy(checked=retainedChecks(old.decisions,next.decisions,old.checked),
                route=RoutePhase.NONE,parked=false,sleepChecks=emptySet(),exitChecks=emptySet(),sleepReady=false)
        }
        fun toggle(set:Set<String>,id:String)=if(id in set)set-id else set+id
        return when(event) {
            JourneyEvent.Next -> when(old.step) {
                in 0..3 -> old.copy(step=old.step+1)
                4 -> if(old.canSleep) old.copy(step=5) else old
                5 -> if(old.canSleep && old.sleepChecks.containsAll(Journey.SLEEP_IDS)) old.copy(step=6,sleepReady=true) else old
                else -> old
            }
            JourneyEvent.Back -> when {
                old.step==4 -> old.copy(step=3,route=RoutePhase.NONE,parked=false,sleepChecks=emptySet(),exitChecks=emptySet(),sleepReady=false)
                old.step>0 -> old.copy(step=old.step-1)
                else -> old
            }
            is JourneyEvent.Conditions -> update(old.copy(input=event.value.copy(people=event.value.people.coerceIn(1,2))))
            is JourneyEvent.Place -> if(DemoPlaces.all.any{it.id==event.id}) update(old.copy(placeId=event.id)) else old
            is JourneyEvent.Soc -> if(event.value==null || event.value in 0..100) update(old.copy(soc=event.value)) else old
            is JourneyEvent.Check -> if(old.step==3 && old.decisions.any{it.id==event.id}) old.copy(checked=toggle(old.checked,event.id)) else old
            JourneyEvent.RequestRoute -> if(old.step==4 && old.route in setOf(RoutePhase.NONE,RoutePhase.FAILED)) old.copy(route=RoutePhase.REQUESTED) else old
            is JourneyEvent.RouteResponse -> if(old.step==4 && old.route==RoutePhase.REQUESTED) old.copy(route=if(event.success)RoutePhase.GUIDING else RoutePhase.FAILED) else old
            JourneyEvent.Arrive -> if(old.step==4 && old.route==RoutePhase.GUIDING) old.copy(route=RoutePhase.ARRIVED,parked=false) else old
            JourneyEvent.Park -> if(old.step==4 && old.route==RoutePhase.ARRIVED) old.copy(parked=true) else old
            is JourneyEvent.SleepCheck -> if(old.step==5 && old.canSleep && event.id in Journey.SLEEP_IDS) old.copy(sleepChecks=toggle(old.sleepChecks,event.id),sleepReady=false) else old
            is JourneyEvent.ExitCheck -> if(old.step==6 && event.id in Journey.EXIT_IDS) old.copy(exitChecks=toggle(old.exitChecks,event.id)) else old
            JourneyEvent.Finish -> if(old.step==6 && old.canFinish) old.copy(completed=true) else old
            else -> old
        }
    }
}
