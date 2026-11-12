package com.chapark.ivi.prototype

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.chapark.app.domain.TripInput
import com.chapark.app.data.DemoPlaces
import com.chapark.prototype.*
import kotlinx.coroutines.flow.first

private val recoveredKey=booleanPreferencesKey("recovered")
private val Context.journeyStore by preferencesDataStore(name="ivi_demo_journey",corruptionHandler=ReplaceFileCorruptionHandler {
    preferencesOf(recoveredKey to true)
})

class JourneyStore(context:Context) {
    private val store=context.applicationContext.journeyStore
    suspend fun load():Pair<Journey,String?> {
        val p=store.data.first()
        val version=p[intPreferencesKey("schema")] ?: 1
        require(version==1) { "저장 형식을 읽을 수 없습니다. 초기화 후 시작해 주세요." }
        fun bool(k:String,default:Boolean=false)=p[booleanPreferencesKey(k)] ?: default
        fun int(k:String,default:Int)=p[intPreferencesKey(k)] ?: default
        fun checks(k:String)=p[stringSetPreferencesKey(k)] ?: emptySet()
        val route=runCatching { RoutePhase.valueOf(p[stringPreferencesKey("route")] ?: "NONE") }.getOrDefault(RoutePhase.NONE)
        var state=Journey(step=int("step",0).coerceIn(0,6),
            input=TripInput(int("people",1).coerceIn(1,2),bool("cooking",true),bool("pets"),bool("bag",true)),
            placeId=p[stringPreferencesKey("place")]?.takeIf{ id->DemoPlaces.all.any{it.id==id} } ?: "demo-b",
            soc=if(bool("socMissing"))null else int("soc",20).takeIf{it in 0..100},
            checked=checks("checked"),route=if(route==RoutePhase.REQUESTED)RoutePhase.FAILED else route,
            parked=bool("parked") && route==RoutePhase.ARRIVED,
            sleepChecks=checks("sleep").intersect(Journey.SLEEP_IDS),exitChecks=checks("exit").intersect(Journey.EXIT_IDS),
            sleepReady=bool("sleepReady"),completed=bool("completed"),restricted=bool("restricted"),dark=bool("dark"))
        state=state.copy(checked=state.checked.intersect(state.decisions.map{it.id}.toSet()))
        if(state.step>=5 && !state.canSleep) state=state.copy(step=4,sleepReady=false,completed=false)
        if(!state.sleepChecks.containsAll(Journey.SLEEP_IDS)) state=state.copy(sleepReady=false)
        if(state.step==6 && !state.sleepReady) state=state.copy(step=5,completed=false)
        if(!state.canFinish) state=state.copy(completed=false)
        return state to if(bool("recovered"))"손상된 여행 저장 파일을 복구했습니다. 처음부터 시작합니다." else if(route==RoutePhase.REQUESTED)"이동 정보 준비가 중단됐어요. 다시 시도해 주세요." else null
    }
    suspend fun save(s:Journey) { store.edit { p->
        p.clear()
        p[intPreferencesKey("schema")]=1
        p[intPreferencesKey("step")]=s.step; p[intPreferencesKey("people")]=s.input.people
        p[booleanPreferencesKey("cooking")]=s.input.cooking; p[booleanPreferencesKey("pets")]=s.input.pets
        p[booleanPreferencesKey("bag")]=s.input.sleepingBag; p[stringPreferencesKey("place")]=s.placeId
        p[booleanPreferencesKey("socMissing")]=s.soc==null; p[intPreferencesKey("soc")]=s.soc ?: 0
        p[stringSetPreferencesKey("checked")]=s.checked; p[stringPreferencesKey("route")]=s.route.name
        p[booleanPreferencesKey("parked")]=s.parked; p[stringSetPreferencesKey("sleep")]=s.sleepChecks
        p[stringSetPreferencesKey("exit")]=s.exitChecks; p[booleanPreferencesKey("sleepReady")]=s.sleepReady
        p[booleanPreferencesKey("completed")]=s.completed; p[booleanPreferencesKey("restricted")]=s.restricted
        p[booleanPreferencesKey("dark")]=s.dark
    } }
}
