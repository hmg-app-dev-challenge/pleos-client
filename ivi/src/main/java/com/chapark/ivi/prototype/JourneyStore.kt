package com.chapark.ivi.prototype

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.chapark.app.domain.TripInput
import com.chapark.app.domain.Source
import com.chapark.app.domain.ReadingStatus
import com.chapark.app.domain.TripSchedule
import java.util.UUID
import org.json.JSONObject
import org.json.JSONArray
import com.chapark.app.data.PlaceCatalog
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
        val decoded=decode(p)
        val result=decoded.first.let{if(it.tripId==null)it.copy(tripId=UUID.randomUUID().toString()) else it} to decoded.second
        if((p[intPreferencesKey("schema")] ?: 1)<6 || decoded.first.tripId==null)save(result.first)
        return result
    }
    internal fun decode(p:Preferences):Pair<Journey,String?> {
        val version=p[intPreferencesKey("schema")] ?: 1
        require(version in 1..6) { "저장 형식을 읽을 수 없습니다. 초기화 후 시작해 주세요." }
        fun bool(k:String,default:Boolean=false)=p[booleanPreferencesKey(k)] ?: default
        fun int(k:String,default:Int)=p[intPreferencesKey(k)] ?: default
        fun checks(k:String)=p[stringSetPreferencesKey(k)] ?: emptySet()
        val route=runCatching { RoutePhase.valueOf(p[stringPreferencesKey("route")] ?: "NONE") }.getOrDefault(RoutePhase.NONE)
        var state=Journey(step=int("step",0).coerceIn(0,6),
            input=TripInput(int("people",1).coerceAtLeast(1),bool("cooking",true),bool("pets"),bool("bag",true),
                int("threshold",35).coerceIn(0,100),int("endHour",10).coerceIn(0,23),
                p[stringPreferencesKey("tripDate")]?.let{date->TripSchedule(date,p[stringPreferencesKey("tripZone")] ?: "")} ),
            tripId=p[stringPreferencesKey("tripId")],chargerId=p[stringPreferencesKey("chargerId")]?.takeIf{com.chapark.app.data.ChargingPlaces.find(it)!=null},restMode=p[stringPreferencesKey("restMode")] ?: "REST",
            forgotten=checks("forgotten"),reminders=checks("reminders"),followUps=checks("followUps"),
            records=JourneyRecordCodec.decode(p[stringPreferencesKey("records")] ?: "[]"),
            placeId=p[stringPreferencesKey("place")]?.takeIf{ id->PlaceCatalog.all.any{it.id==id} } ?: "demo-b",
            soc=if(bool("socMissing"))null else int("soc",20).takeIf{it in 0..100},
            checked=checks("checked"),route=if(route==RoutePhase.REQUESTED)RoutePhase.FAILED else route,
            parked=bool("parked") && route==RoutePhase.ARRIVED,
            sleepChecks=checks("sleep").intersect(Journey.SLEEP_IDS),exitChecks=checks("exit").intersect(Journey.EXIT_IDS),
            sleepReady=bool("sleepReady"),completed=bool("completed"),restricted=bool("restricted"),dark=bool("dark"),
            batteryStatus=runCatching{ReadingStatus.valueOf(p[stringPreferencesKey("batteryStatus")] ?: "UNKNOWN")}.getOrDefault(ReadingStatus.UNKNOWN),
            socSource=if(version==1)Source.DEMO else runCatching { Source.valueOf(p[stringPreferencesKey("socSource")] ?: "UNKNOWN") }.getOrDefault(Source.UNKNOWN),
            socReceivedAtMillis=p[longPreferencesKey("socReceivedAt")])
        state=state.copy(checked=state.checked.intersect(state.decisions.map{it.id}.toSet()))
        if(state.step>=5 && !state.canSleep) state=state.copy(step=4,sleepReady=false,completed=false)
        if(!state.sleepChecks.containsAll(Journey.SLEEP_IDS)) state=state.copy(sleepReady=false)
        if(state.step==6 && !state.sleepReady) state=state.copy(step=5,completed=false)
        if(!state.canFinish) state=state.copy(completed=false)
        return state to if(bool("recovered"))"손상된 여행 저장 파일을 복구했습니다. 처음부터 시작합니다." else if(route==RoutePhase.REQUESTED)"이동 정보 준비가 중단됐어요. 다시 시도해 주세요." else null
    }
    internal suspend fun archivedRecord(id:String):String?=JSONObject(store.data.first()[stringPreferencesKey("archive")] ?: "{}").optJSONObject(id)?.toString()
    internal suspend fun syncSnapshot():Pair<Map<String,String>,Set<String>> {
        val p=store.data.first();val archive=JSONObject(p[stringPreferencesKey("archive")] ?: "{}")
        return archive.keys().asSequence().associateWith{archive.getJSONObject(it).toString()} to (p[stringSetPreferencesKey("deletedArchives")] ?: emptySet())
    }
    suspend fun history():List<ArchivedTrip> {
        val archive=JSONObject(store.data.first()[stringPreferencesKey("archive")] ?: "{}")
        return archive.keys().asSequence().map{ id->
            val o=archive.getJSONObject(id)
            val records=JourneyRecordCodec.decode(o.getJSONArray("records").toString())
            ArchivedTrip(id,o.getString("placeName"),o.optString("date", "날짜 미수집"),o.optString("zone","UTC"),records,
                o.optJSONArray("forgotten")?.let{a->(0 until a.length()).map{a.getString(it)}} ?: emptyList())
        }.sortedByDescending{it.records.lastOrNull()?.observedAtMillis ?: 0}.toList()
    }
    suspend fun deleteHistory(id:String){store.edit{p->
        val archive=JSONObject(p[stringPreferencesKey("archive")] ?: "{}")
        archive.remove(id);p[stringPreferencesKey("archive")]=archive.toString()
        p[stringSetPreferencesKey("deletedArchives")]=(p[stringSetPreferencesKey("deletedArchives")] ?: emptySet())+id
    }}
    suspend fun save(s:Journey) { store.edit { p->
        val archive=JSONObject(p[stringPreferencesKey("archive")] ?: "{}")
        val deleted=p[stringSetPreferencesKey("deletedArchives")] ?: emptySet()
        if(s.completed && s.tripId!=null && s.tripId !in deleted)archive.put(s.tripId,JSONObject().apply {
            put("chargerId",s.chargerId);put("placeId",s.placeId);put("placeName",s.place.name)
            put("date",s.input.schedule?.date);put("zone",s.input.schedule?.zoneId)
            put("forgotten",JSONArray(s.forgotten.toList()));
            put("people",s.input.people);put("threshold",s.input.batteryThreshold)
            put("records",JSONArray(JourneyRecordCodec.encode(s.records)))
        })
        p.clear()
        p[stringPreferencesKey("archive")]=archive.toString()
        p[stringSetPreferencesKey("deletedArchives")]=deleted
        p[intPreferencesKey("schema")]=6
        p[stringPreferencesKey("restMode")]=s.restMode
        p[stringSetPreferencesKey("followUps")]=s.followUps
        p[stringSetPreferencesKey("forgotten")]=s.forgotten;p[stringSetPreferencesKey("reminders")]=s.reminders
        p[stringPreferencesKey("records")]=JourneyRecordCodec.encode(s.records)
        s.tripId?.let{p[stringPreferencesKey("tripId")]=it}
        s.chargerId?.let{p[stringPreferencesKey("chargerId")]=it}
        s.input.schedule?.let{p[stringPreferencesKey("tripDate")]=it.date;p[stringPreferencesKey("tripZone")]=it.zoneId}
        p[intPreferencesKey("threshold")]=s.input.batteryThreshold
        p[intPreferencesKey("endHour")]=s.input.endHour
        p[stringPreferencesKey("batteryStatus")]=s.batteryStatus.name
        p[stringPreferencesKey("socSource")]=s.socSource.name
        s.socReceivedAtMillis?.let{p[longPreferencesKey("socReceivedAt")]=it}
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

data class ArchivedTrip(val id:String,val placeName:String,val date:String,val zone:String,val records:List<JourneyRecord>,val forgotten:List<String>)
