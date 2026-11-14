package com.chapark.prototype

import com.chapark.app.domain.Source
import com.chapark.app.domain.ReadingStatus

data class PlaceMemory(val sceneryTags:List<String>,val informationDates:List<String>,val hasUndatedInfo:Boolean)

/** Event time is app observation time, never a fabricated vehicle sensor timestamp. */
data class JourneyRecord(
    val kind:String, val observedAtMillis:Long, val origin:String,
    val subject:String?=null, val label:String?=null, val confirmed:Boolean?=null,
    val placeId:String, val soc:Int?=null, val socSource:Source=Source.UNKNOWN,
    val socReceivedAtMillis:Long?=null, val eventAtMillis:Long?=null,
    val readingStatus:ReadingStatus?=null, val preparationCompleted:Int?=null, val preparationRequired:Int?=null,
    val placeMemory:PlaceMemory?=null,
)
object JourneyJournal {
    fun apply(old:Journey,event:JourneyEvent,now:Long,origin:String="USER"):Journey {
        require(now>=0)
        val next=JourneyReducer.apply(old,event)
        if(next==old || event==JourneyEvent.ResetScenario || event==JourneyEvent.NewTrip)return next
        val kind=when {
            event is JourneyEvent.Place->"PLACE_SELECTED"
            event is JourneyEvent.Charger->"CHARGING_PLAN"
            event is JourneyEvent.Check->"PREPARATION_CHECK"
            event is JourneyEvent.Omit->"PREPARATION_OMITTED"
            event is JourneyEvent.SleepCheck->"SLEEP_CHECK"
            event is JourneyEvent.ExitCheck->"EXIT_CHECK"
            old.step==3 && next.step==4->"PREPARATION_COMPLETED"
            old.route!=RoutePhase.REQUESTED && next.route==RoutePhase.REQUESTED->"ROUTE_REQUESTED"
            old.route!=RoutePhase.GUIDING && next.route==RoutePhase.GUIDING->"GUIDANCE_OBSERVED"
            old.route!=RoutePhase.FAILED && next.route==RoutePhase.FAILED->"ROUTE_FAILED"
            old.route!=RoutePhase.ARRIVED && next.route==RoutePhase.ARRIVED->"ARRIVAL"
            !old.parked && next.parked->"PARKING_CONFIRMED"
            !old.completed && next.completed->"TRIP_COMPLETED"
            else->return next
        }
        val id=when(event){is JourneyEvent.Check->event.id;is JourneyEvent.Omit->event.id;is JourneyEvent.SleepCheck->event.id;is JourneyEvent.ExitCheck->event.id;else->null}
        val checked=when(event){is JourneyEvent.Check->event.id in next.checked;is JourneyEvent.Omit->event.id in next.omitted;is JourneyEvent.SleepCheck->event.id in next.sleepChecks;is JourneyEvent.ExitCheck->event.id in next.exitChecks;else->null}
        val label=when(event){
            is JourneyEvent.SleepCheck->JourneyChecklists.sleep(next.checklistVersion)[event.id]
            is JourneyEvent.ExitCheck->JourneyChecklists.exit(next.checklistVersion)[event.id]
            is JourneyEvent.Omit->next.decisions.firstOrNull{it.id==id}?.title
            is JourneyEvent.Check->next.decisions.firstOrNull{it.id==id}?.let{"${it.title} · ${it.reason}"}
            JourneyEvent.RequestRoute->com.chapark.app.data.ChargingPlaces.find(next.chargerId)?.let{"경유 · ${it.name}"} ?: "경유 없음"
            is JourneyEvent.Place->next.place.name
            is JourneyEvent.Charger->com.chapark.app.data.ChargingPlaces.find(event.id)?.name ?: "충전 경유 없음"
            else->null
        }
        // A reconnect confirms current guidance, not a second physical departure.
        if(kind=="GUIDANCE_OBSERVED" && old.records.lastOrNull{it.kind in setOf("ROUTE_REQUESTED","GUIDANCE_OBSERVED")}?.kind==kind)return next
        val reading=kind in setOf("PREPARATION_COMPLETED","TRIP_COMPLETED")
        val evidence=com.chapark.app.data.PlaceEvidenceCatalog.find(next.placeId)
        val context=if(com.chapark.app.data.NavigationPlaces.find(next.placeId)!=null)PlaceMemory(
            listOf(evidence.scenery.label),evidence.facts.mapNotNull{it.checkedOn}.distinct().sorted(),
            evidence.facts.isEmpty() || evidence.facts.any{it.checkedOn==null}) else null
        val computed=if(kind=="PREPARATION_COMPLETED")listOf(JourneyRecord("PLACE_CONTEXT",now,"REFERENCE",label=next.place.name,placeId=next.placeId,placeMemory=context))+solarRecords(next,now)+
            com.chapark.app.data.PlaceEvidenceCatalog.find(next.placeId).facts.map{fact->
                JourneyRecord("PLACE_INFO",now,"REFERENCE",label="${fact.label} · ${fact.detail} · 자료 확인 ${fact.checkedOn ?: "미확인"} · ${fact.url ?: "출처 미확인"}",
                    confirmed=fact.condition==com.chapark.app.domain.Condition.ALLOWED,placeId=next.placeId)
            } else emptyList()
        return next.copy(records=old.records.filterNot{kind=="PREPARATION_COMPLETED" && it.origin in setOf("CALCULATED","REFERENCE")}+computed+JourneyRecord(kind,now,origin,id,label,checked,next.placeId,
            if(reading && next.batteryStatus==ReadingStatus.AVAILABLE)next.soc?.takeIf{it in 0..100} else null,if(reading)next.socSource else Source.UNKNOWN,
            if(reading)next.socReceivedAtMillis else null,
            readingStatus=if(reading)next.batteryStatus else null,
            preparationCompleted=if(kind=="PREPARATION_COMPLETED")next.departureCheckedCount else null,
            preparationRequired=if(kind=="PREPARATION_COMPLETED")next.departureProgress.total else null))
    }
    private fun solarRecords(s:Journey,now:Long):List<JourneyRecord> {
        val place=com.chapark.app.data.NavigationPlaces.find(s.placeId) ?: return emptyList()
        val schedule=s.input.schedule ?: return emptyList()
        return runCatching {
            val date=java.time.LocalDate.parse(schedule.date);val zone=java.time.ZoneId.of(schedule.zoneId)
            val sunset=com.chapark.app.domain.SolarTimes.calculate(date,zone,place.latitude,place.longitude).sunset
            val sunrise=com.chapark.app.domain.SolarTimes.calculate(date.plusDays(1),zone,place.latitude,place.longitude).sunrise
            listOf("SUNSET" to sunset,"SUNRISE" to sunrise).mapNotNull{(kind,event)->event?.let{
                JourneyRecord(kind,now,"CALCULATED",label="약 ${it.azimuthDegrees.toInt()}° · 조망 방향 미확인",placeId=s.placeId,eventAtMillis=it.instant.toEpochMilli())
            }}
        }.getOrDefault(emptyList())
    }

}
