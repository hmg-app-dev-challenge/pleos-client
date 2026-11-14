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
    val error:String?=null,val notice:String?=null,val navigationMessage:String?=null,val navigationReady:Boolean=false,val waypointNeedsConfirmation:Boolean=false,val history:List<ArchivedTrip> = emptyList(),val syncStatus:String="기기에 저장됨",val catalogStatus:String="",val savedPlans:List<PlanChoice> = emptyList())
data class PlanChoice(val id:String,val title:String)
class JourneyViewModel(app:Application):AndroidViewModel(app) {
    private val store=JourneyStore(app)
    private val sync=RecordSync(store)
    private val catalog=CatalogCache(app)
    private val battery=BatterySession(app)
    private val navigation=NavigationSession(app)
    private val mutable=MutableStateFlow(JourneyUi())
    val state=mutable.asStateFlow()
    private sealed interface Command {
        data object LoadPlans:Command
        data class ImportPlan(val id:String):Command
        data object ForkPlan:Command
        data class DeleteHistory(val id:String):Command
        data class Event(val value:JourneyEvent):Command
        data class Delivery(val value:NavigationDelivery):Command
        data class Navigation(val value:NavigationSnapshot):Command
    }
    private var importPlans:List<SavedPlan> = emptyList()
    private val commands=Channel<Command>(Channel.UNLIMITED)
    private fun realPlace(s:Journey)=if(NavigationPlaces.find(s.placeId)!=null)s
        else if(s.step==0) s.copy(placeId=NavigationPlaces.find(NavigationPlaces.DEFAULT_ID)?.place?.id ?: NavigationPlaces.all.first().place.id) else s
    init {
        catalog.loadActive()
        viewModelScope.launch {
            while(true){kotlinx.coroutines.delay(15_000);val status=sync.sync();mutable.value=mutable.value.copy(syncStatus=status,history=store.history());val catalogStatus=catalog.refresh();mutable.value=mutable.value.copy(catalogStatus=catalogStatus)}
        }
        viewModelScope.launch {battery.state.collect{b->commands.send(Command.Event(JourneyEvent.Battery(b.value,b.status,b.source,b.receivedAt,b.capability)))}}
        viewModelScope.launch {navigation.requestDeliveries.collect{commands.send(Command.Delivery(it))}}
        viewModelScope.launch {navigation.state.collect {commands.send(Command.Navigation(it))}}
        viewModelScope.launch {
            try {
                val (saved,notice)=store.load()
                val restored=realPlace(saved)
                val reconnect=restored.step==4 && restored.route==RoutePhase.GUIDING
                mutable.value=JourneyUi(if(reconnect)restored.copy(route=RoutePhase.REQUESTED)else restored,loaded=true,notice=notice,history=store.history())
                if(reconnect){
                    val place=NavigationPlaces.find(restored.placeId)
                    if(place!=null)navigation.connect(place,requestRoute=false,charger=com.chapark.app.data.ChargingPlaces.find(restored.chargerId))
                    else {
                        mutable.value=mutable.value.copy(navigationMessage="이 장소의 위치를 확인하지 못했어요. 이전 단계에서 장소를 다시 선택해 주세요.")
                        persist(restored.copy(route=RoutePhase.FAILED))
                    }
                }
            } catch(e:Exception){mutable.value=JourneyUi(loaded=true,error="저장된 여행을 읽지 못했습니다. 초기화 후 시작할 수 있습니다.")}
            for(command in commands) {
                if(command is Command.LoadPlans){
                    if(mutable.value.journey.restricted || mutable.value.journey.step!=0)continue
                    try{
                        importPlans=PlanImport.list().filter{it.id!=mutable.value.journey.tripId}
                        mutable.value=mutable.value.copy(savedPlans=importPlans.map{PlanChoice(it.id,"${it.input.schedule?.date ?: "날짜 확인 필요"} · ${it.name} · ${it.input.people}명")},notice=if(importPlans.isEmpty())"가져올 수 있는 계획이 없어요." else null)
                    }catch(e:Exception){mutable.value=mutable.value.copy(notice="저장된 계획에 연결하지 못했어요. 기기에서 계속 계획할 수 있습니다.")}
                    continue
                }
                if(command is Command.ImportPlan){
                    if(mutable.value.journey.restricted || mutable.value.journey.step!=0)continue
                    val plan=importPlans.find{it.id==command.id} ?: continue
                    val b=battery.state.value
                    persist(JourneyReducer.apply(plan.asNewJourney(mutable.value.journey.dark),JourneyEvent.Battery(b.value,b.status,b.source,b.receivedAt,b.capability)))
                    mutable.value=mutable.value.copy(savedPlans=emptyList(),notice="계획 조건을 가져왔어요. 준비와 현장 확인은 이번 여행에서 다시 진행합니다.")
                    continue
                }
                if(command is Command.ForkPlan){
                    if(mutable.value.journey.restricted || mutable.value.journey.completed)continue
                    persist(mutable.value.journey.copy(tripId=java.util.UUID.randomUUID().toString()))
                    mutable.value=mutable.value.copy(syncStatus="기기에 저장됨",notice="이 기기 계획을 새 여행 ID로 보관했어요. 서버의 기존 계획도 유지됩니다.")
                    continue
                }
                if(command is Command.DeleteHistory){
                    if(mutable.value.journey.restricted)continue
                    try{store.deleteHistory(command.id);mutable.value=mutable.value.copy(history=store.history())}
                    catch(e:Exception){mutable.value=mutable.value.copy(notice="기록을 삭제하지 못했어요. 다시 시도해 주세요.")}
                    continue
                }
                if(command is Command.Delivery){
                    val d=command.value
                    val next=JourneyJournal.recordNavigationDelivery(mutable.value.journey,d.id,d.observedAt,d.returned,d.errorType)
                    if(next!=mutable.value.journey)persist(next)
                    continue
                }
                if(command is Command.Navigation){
                    val n=command.value
                    mutable.value=mutable.value.copy(navigationMessage=n.message,navigationReady=n.routeConfirmed,waypointNeedsConfirmation=n.phase==NavigationPhase.VERIFY_WAYPOINT)
                    val event=when(n.phase){
                        NavigationPhase.GUIDING->JourneyEvent.RouteResponse(true)
                        NavigationPhase.ARRIVED->JourneyEvent.Arrive
                        NavigationPhase.FAILED->JourneyEvent.NavigationLost
                        else->null
                    }
                    if(event!=null && mutable.value.journey.step==4){
                        val next=JourneyJournal.apply(mutable.value.journey.copy(restricted=false),event,System.currentTimeMillis(),n.confirmationSource).copy(restricted=mutable.value.journey.restricted)
                        if(next!=mutable.value.journey)persist(next)
                    }
                    continue
                }
                val rawEvent=(command as Command.Event).value
                val event=if(rawEvent is JourneyEvent.Soc) rawEvent.copy(receivedAt=System.currentTimeMillis()) else rawEvent
                if(mutable.value.journey.restricted && event !is JourneyEvent.Battery && event !is JourneyEvent.Driving)continue
                if(mutable.value.error!=null && event!=JourneyEvent.ResetScenario && event !is JourneyEvent.Driving)continue
                if(event==JourneyEvent.NewTrip || event==JourneyEvent.ResetScenario){
                    try{catalog.activatePending();mutable.value=mutable.value.copy(catalogStatus="")}
                    catch(e:Exception){mutable.value=mutable.value.copy(notice="새 장소 자료를 적용하지 못했어요. 저장된 자료를 사용합니다.")}
                }
                val old=mutable.value.journey
                // A new trip gets today's date once; reopening an existing plan never rewrites it.
                val current=if(event==JourneyEvent.Next && old.step==0 && old.input.schedule==null){
                    val zone=java.time.ZoneId.systemDefault()
                    old.copy(input=old.input.copy(schedule=com.chapark.app.domain.TripSchedule(java.time.LocalDate.now(zone).toString(),zone.id)))
                }else old
                var reduced=realPlace(JourneyJournal.apply(current,event,System.currentTimeMillis(),if(event is JourneyEvent.Battery)"SDK" else "USER"))
                if(event==JourneyEvent.NewTrip || event==JourneyEvent.ResetScenario){
                    val b=battery.state.value
                    reduced=JourneyReducer.apply(reduced,JourneyEvent.Battery(b.value,b.status,b.source,b.receivedAt,b.capability))
                }
                val next=if(reduced.tripId==null)reduced.copy(tripId=java.util.UUID.randomUUID().toString()) else reduced
                if(next==old && event!=JourneyEvent.ResetScenario)continue
                if(next.step!=4 || next.route in setOf(RoutePhase.NONE,RoutePhase.ARRIVED))navigation.close()
                persist(next)
                if(event==JourneyEvent.RequestRoute && next.route==RoutePhase.REQUESTED && mutable.value.error==null){
                    val place=NavigationPlaces.find(next.placeId)
                    if(place!=null)navigation.connect(place,requestRoute=true,charger=com.chapark.app.data.ChargingPlaces.find(next.chargerId))
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
    fun startVehicleObservation(){battery.start()}
    fun stopVehicleObservation(){battery.close()}
    fun send(event:JourneyEvent){if(mutable.value.loaded)commands.trySend(Command.Event(event))}
    fun loadPlans(){commands.trySend(Command.LoadPlans)}
    fun importPlan(id:String){commands.trySend(Command.ImportPlan(id))}
    fun forkPlan(){commands.trySend(Command.ForkPlan)}
    fun deleteHistory(id:String){commands.trySend(Command.DeleteHistory(id))}
    fun confirmWaypoint(){if(!mutable.value.journey.restricted)navigation.confirmWaypointByUser()}
    fun openNavigation(){navigation.openScreen()}
    override fun onCleared(){battery.close();navigation.close();super.onCleared()}
}
