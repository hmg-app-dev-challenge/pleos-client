package com.chapark.ivi.prototype

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chapark.prototype.*
import com.chapark.app.data.NavigationPlaces
import com.chapark.ivi.sdk.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class JourneyUi(val journey:Journey=Journey(),val loaded:Boolean=false,val saving:Boolean=false,
    val error:String?=null,val notice:String?=null,val navigationMessage:String?=null,val navigationReady:Boolean=false,val history:List<ArchivedTrip> = emptyList(),val syncStatus:String="기기에 저장됨")
class JourneyViewModel(app:Application):AndroidViewModel(app) {
    private val store=JourneyStore(app)
    private val sync=RecordSync(store)
    private val battery=BatterySession(app)
    private val navigation=NavigationSession(app)
    private val mutable=MutableStateFlow(JourneyUi())
    val state=mutable.asStateFlow()
    private sealed interface Command {
        data class DeleteHistory(val id:String):Command
        data class Event(val value:JourneyEvent):Command
        data class Navigation(val value:NavigationSnapshot):Command
    }
    private val commands=Channel<Command>(Channel.UNLIMITED)
    private fun realPlace(s:Journey)=if(NavigationPlaces.find(s.placeId)!=null)s
        else if(s.step==0) s.copy(placeId=NavigationPlaces.DEFAULT_ID) else s
    init {
        viewModelScope.launch {
            while(true){kotlinx.coroutines.delay(15_000);val status=sync.sync();mutable.value=mutable.value.copy(syncStatus=status)}
        }
        battery.start()
        viewModelScope.launch {battery.state.collect{b->commands.send(Command.Event(JourneyEvent.Battery(b.value,b.status,b.source,b.receivedAt)))}}
        viewModelScope.launch {navigation.state.collect {commands.send(Command.Navigation(it))}}
        viewModelScope.launch {
            try {
                val (saved,notice)=store.load()
                val restored=realPlace(saved)
                val reconnect=restored.step==4 && restored.route==RoutePhase.GUIDING
                mutable.value=JourneyUi(if(reconnect)restored.copy(route=RoutePhase.REQUESTED)else restored,loaded=true,notice=notice,history=store.history())
                if(reconnect){
                    val place=NavigationPlaces.find(restored.placeId)
                    if(place!=null)navigation.connect(place,requestRoute=false)
                    else {
                        mutable.value=mutable.value.copy(navigationMessage="이 장소의 위치를 확인하지 못했어요. 이전 단계에서 장소를 다시 선택해 주세요.")
                        persist(restored.copy(route=RoutePhase.FAILED))
                    }
                }
            } catch(e:Exception){mutable.value=JourneyUi(loaded=true,error="저장된 여행을 읽지 못했습니다. 초기화 후 시작할 수 있습니다.")}
            for(command in commands) {
                if(command is Command.DeleteHistory){
                    try{store.deleteHistory(command.id);mutable.value=mutable.value.copy(history=store.history())}
                    catch(e:Exception){mutable.value=mutable.value.copy(notice="기록을 삭제하지 못했어요. 다시 시도해 주세요.")}
                    continue
                }
                if(command is Command.Navigation){
                    val n=command.value
                    mutable.value=mutable.value.copy(navigationMessage=n.message,navigationReady=n.routeConfirmed)
                    val event=when(n.phase){
                        NavigationPhase.GUIDING->JourneyEvent.RouteResponse(true)
                        NavigationPhase.ARRIVED->JourneyEvent.Arrive
                        NavigationPhase.FAILED->JourneyEvent.NavigationLost
                        else->null
                    }
                    if(event!=null && mutable.value.journey.step==4){
                        val next=JourneyJournal.apply(mutable.value.journey,event,System.currentTimeMillis(),"SDK")
                        if(next!=mutable.value.journey)persist(next)
                    }
                    continue
                }
                val event=(command as Command.Event).value
                if(mutable.value.error!=null && event!=JourneyEvent.ResetScenario)continue
                val old=mutable.value.journey
                var reduced=realPlace(JourneyJournal.apply(old,event,System.currentTimeMillis(),if(event is JourneyEvent.Battery)"SDK" else "USER"))
                if(event==JourneyEvent.NewTrip || event==JourneyEvent.ResetScenario){
                    val b=battery.state.value
                    reduced=JourneyReducer.apply(reduced,JourneyEvent.Battery(b.value,b.status,b.source,b.receivedAt))
                }
                val next=if(reduced.tripId==null)reduced.copy(tripId=java.util.UUID.randomUUID().toString()) else reduced
                if(next==old && event!=JourneyEvent.ResetScenario)continue
                if(next.step!=4 || next.route in setOf(RoutePhase.NONE,RoutePhase.ARRIVED))navigation.close()
                persist(next)
                if(event==JourneyEvent.RequestRoute && next.route==RoutePhase.REQUESTED && mutable.value.error==null){
                    val place=NavigationPlaces.find(next.placeId)
                    if(place!=null)navigation.connect(place,requestRoute=true)
                    else {
                        mutable.value=mutable.value.copy(navigationMessage="이 장소의 위치를 확인하지 못했어요. 이전 단계에서 장소를 다시 선택해 주세요.")
                        persist(next.copy(route=RoutePhase.FAILED))
                    }
                }
            }
        }
    }
    private suspend fun persist(next:Journey){
        mutable.value=mutable.value.copy(journey=next,saving=true)
        try {store.save(next);mutable.value=mutable.value.copy(saving=false,error=null,history=store.history())}
        catch(e:Exception){mutable.value=mutable.value.copy(saving=false,error="저장하지 못했습니다. 저장 공간을 확인한 뒤 다시 실행해 주세요.")}
    }
    fun send(event:JourneyEvent){if(mutable.value.loaded)commands.trySend(Command.Event(event))}
    fun deleteHistory(id:String){commands.trySend(Command.DeleteHistory(id))}
    fun openNavigation(){navigation.openScreen()}
    override fun onCleared(){battery.close();navigation.close();super.onCleared()}
}
