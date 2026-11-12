package com.chapark.prototype

import com.chapark.app.domain.Source

/** Event time is app observation time, never a fabricated vehicle sensor timestamp. */
data class JourneyRecord(
    val kind:String, val observedAtMillis:Long, val origin:String,
    val subject:String?=null, val label:String?=null, val confirmed:Boolean?=null,
    val placeId:String, val soc:Int?=null, val socSource:Source=Source.UNKNOWN,
    val socReceivedAtMillis:Long?=null,
)
object JourneyJournal {
    fun apply(old:Journey,event:JourneyEvent,now:Long,origin:String="USER"):Journey {
        require(now>=0)
        val next=JourneyReducer.apply(old,event)
        if(next==old || event==JourneyEvent.ResetScenario || event==JourneyEvent.NewTrip)return next
        val kind=when {
            event is JourneyEvent.Check->"PREPARATION_CHECK"
            event is JourneyEvent.SleepCheck->"SLEEP_CHECK"
            event is JourneyEvent.ExitCheck->"EXIT_CHECK"
            old.step==3 && next.step==4->"PREPARATION_COMPLETED"
            old.route!=RoutePhase.REQUESTED && next.route==RoutePhase.REQUESTED->"ROUTE_REQUESTED"
            old.route!=RoutePhase.GUIDING && next.route==RoutePhase.GUIDING->"GUIDANCE_OBSERVED"
            old.route!=RoutePhase.ARRIVED && next.route==RoutePhase.ARRIVED->"ARRIVAL"
            !old.parked && next.parked->"PARKING_CONFIRMED"
            !old.completed && next.completed->"TRIP_COMPLETED"
            else->return next
        }
        val id=when(event){is JourneyEvent.Check->event.id;is JourneyEvent.SleepCheck->event.id;is JourneyEvent.ExitCheck->event.id;else->null}
        val checked=when(event){is JourneyEvent.Check->event.id in next.checked;is JourneyEvent.SleepCheck->event.id in next.sleepChecks;is JourneyEvent.ExitCheck->event.id in next.exitChecks;else->null}
        val label=if(event is JourneyEvent.Check)next.decisions.firstOrNull{it.id==id}?.let{"${it.title} · ${it.reason}"} else null
        // A reconnect confirms current guidance, not a second physical departure.
        if(kind=="GUIDANCE_OBSERVED" && old.records.lastOrNull{it.kind in setOf("ROUTE_REQUESTED","GUIDANCE_OBSERVED")}?.kind==kind)return next
        val reading=kind in setOf("PREPARATION_COMPLETED","TRIP_COMPLETED")
        return next.copy(records=old.records+JourneyRecord(kind,now,origin,id,label,checked,next.placeId,
            if(reading)next.soc else null,if(reading)next.socSource else Source.UNKNOWN,
            if(reading)next.socReceivedAtMillis else null))
    }
}
