package com.chapark.ivi.prototype

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chapark.prototype.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

 data class JourneyUi(val journey:Journey=Journey(),val loaded:Boolean=false,val saving:Boolean=false,
    val error:String?=null,val notice:String?=null)
class JourneyViewModel(app:Application):AndroidViewModel(app) {
    private val store=JourneyStore(app)
    private val mutable=MutableStateFlow(JourneyUi())
    val state=mutable.asStateFlow()
    private val events=Channel<JourneyEvent>(Channel.UNLIMITED)
    private var routeJob:Job?=null
    init { viewModelScope.launch {
        try { val (saved,notice)=store.load(); mutable.value=JourneyUi(saved,loaded=true,notice=notice) }
        catch(e:Exception) { mutable.value=JourneyUi(loaded=true,error="저장된 여행을 읽지 못했습니다. 초기화 후 시작할 수 있습니다.") }
        for(event in events) {
            if(mutable.value.error!=null && event!=JourneyEvent.ResetScenario) continue
            val next=JourneyReducer.apply(mutable.value.journey,event)
            if(next.route!=RoutePhase.REQUESTED) { routeJob?.cancel(); routeJob=null }
            mutable.value=mutable.value.copy(journey=next,saving=true)
            try { store.save(next); mutable.value=JourneyUi(next,loaded=true) }
            catch(e:Exception) { mutable.value=mutable.value.copy(saving=false,error="저장하지 못했습니다. 저장 공간을 확인한 뒤 초기화하거나 다시 실행해 주세요.") }
            if(event==JourneyEvent.RequestRoute && next.route==RoutePhase.REQUESTED && mutable.value.error==null) {
                routeJob?.cancel()
                routeJob=viewModelScope.launch { delay(1200); events.send(JourneyEvent.RouteResponse(true)) }
            }
        }
    } }
    fun send(event:JourneyEvent) { if(mutable.value.loaded) events.trySend(event) }
}
