package com.chapark.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapark.prototype.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Local prototype data; never sends fixture destinations or vehicle commands to the SDK. */
class TripViewModel(private val saved:SavedStateHandle):ViewModel() {
    private val mutable=MutableStateFlow(restore())
    val state=mutable.asStateFlow()
    private var routeJob:Job?=null
    private fun restore():Journey {
        val base=Journey()
        val route=runCatching { RoutePhase.valueOf(saved["journey.route"] ?: "NONE") }.getOrDefault(RoutePhase.NONE)
        return base.copy(
            step=(saved.get<Int>("journey.step") ?: 0).coerceIn(0,6),
            input=base.input.copy(people=saved["journey.people"] ?: 1,cooking=saved["journey.cooking"] ?: true,
                pets=saved["journey.pets"] ?: false,sleepingBag=saved["journey.bag"] ?: true,
                batteryThreshold=saved["journey.threshold"] ?: 35,endHour=saved["journey.endHour"] ?: 10,
                schedule=saved.get<String>("journey.date")?.let{com.chapark.app.domain.TripSchedule(it,saved["journey.zone"] ?: "")}),
            placeId=saved["journey.place"] ?: base.placeId,
            route=if(route==RoutePhase.REQUESTED)RoutePhase.FAILED else route,
            parked=saved["journey.parked"] ?: false,
            checked=saved.get<ArrayList<String>>("journey.checked")?.toSet() ?: emptySet(),
            sleepChecks=saved.get<ArrayList<String>>("journey.sleep")?.toSet() ?: emptySet(),
            exitChecks=saved.get<ArrayList<String>>("journey.exit")?.toSet() ?: emptySet(),
            sleepReady=saved["journey.sleepReady"] ?: false,completed=saved["journey.completed"] ?: false,
            dark=saved["journey.dark"] ?: false)
    }
    fun send(event:JourneyEvent) {
        val next=JourneyReducer.apply(mutable.value,event)
        if(next.route!=RoutePhase.REQUESTED) { routeJob?.cancel();routeJob=null }
        mutable.value=next
        saved["journey.date"]=next.input.schedule?.date;saved["journey.zone"]=next.input.schedule?.zoneId
        saved["journey.threshold"]=next.input.batteryThreshold;saved["journey.endHour"]=next.input.endHour
        saved["journey.step"]=next.step;saved["journey.people"]=next.input.people
        saved["journey.cooking"]=next.input.cooking;saved["journey.pets"]=next.input.pets;saved["journey.bag"]=next.input.sleepingBag
        saved["journey.place"]=next.placeId;saved["journey.route"]=next.route.name;saved["journey.parked"]=next.parked
        saved["journey.checked"]=ArrayList(next.checked);saved["journey.sleep"]=ArrayList(next.sleepChecks);saved["journey.exit"]=ArrayList(next.exitChecks)
        saved["journey.sleepReady"]=next.sleepReady;saved["journey.completed"]=next.completed;saved["journey.dark"]=next.dark
        if(event==JourneyEvent.RequestRoute && next.route==RoutePhase.REQUESTED) {
            routeJob?.cancel()
            routeJob=viewModelScope.launch { delay(1200);send(JourneyEvent.RouteResponse(true)) }
        }
    }
}
