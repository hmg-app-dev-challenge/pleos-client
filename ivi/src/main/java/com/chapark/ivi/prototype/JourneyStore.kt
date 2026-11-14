package com.chapark.ivi.prototype

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.chapark.app.domain.CookingPlan
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
        if((p[intPreferencesKey("schema")] ?: 1)<9 || decoded.first.tripId==null)save(result.first)
        return result
    }
    internal fun decode(p:Preferences):Pair<Journey,String?> {
        val version=p[intPreferencesKey("schema")] ?: 1
        require(version in 1..9) { "저장 형식을 읽을 수 없습니다. 초기화 후 시작해 주세요." }
        fun bool(k:String,default:Boolean=false)=p[booleanPreferencesKey(k)] ?: default
        fun int(k:String,default:Int)=p[intPreferencesKey(k)] ?: default
        fun checks(k:String)=p[stringSetPreferencesKey(k)] ?: emptySet()
        val route=runCatching { RoutePhase.valueOf(p[stringPreferencesKey("route")] ?: "NONE") }.getOrDefault(RoutePhase.NONE)
        val storedCooking=p[stringPreferencesKey("cookingPlan")]
        val parsedCooking=storedCooking?.let{runCatching{CookingPlan.valueOf(it)}.getOrNull()}
        val cooking=parsedCooking ?: if(version<7)CookingPlan.fromLegacy(bool("cooking",true)) else CookingPlan.NONE
        val review=if(version<7)p[booleanPreferencesKey("cooking")]==true else parsedCooking==null || bool("cookingNeedsReview")
        val checklistVersion=if(bool("completed") && (version<9 || int("checklistVersion",2)==1))1 else JourneyChecklists.CURRENT_VERSION
        var state=Journey(step=int("step",0).coerceIn(0,6),
            input=TripInput(int("people",1).coerceAtLeast(1),cooking,bool("pets"),bool("bag",true),
                int("threshold",35).coerceIn(0,100),int("endHour",10).coerceIn(0,23),
                p[stringPreferencesKey("tripDate")]?.let{date->TripSchedule(date,p[stringPreferencesKey("tripZone")] ?: "")},cookingNeedsReview=review ),
            tripId=p[stringPreferencesKey("tripId")],chargerId=p[stringPreferencesKey("chargerId")]?.takeIf{com.chapark.app.data.ChargingPlaces.find(it)!=null},restMode=p[stringPreferencesKey("restMode")] ?: "REST",
            forgotten=checks("forgotten"),reminders=checks("reminders"),followUps=checks("followUps"),
            records=JourneyRecordCodec.decode(p[stringPreferencesKey("records")] ?: "[]"),
            placeId=p[stringPreferencesKey("place")]?.takeIf{it.isNotBlank() && it.length<=100} ?: "demo-b",
            soc=if(bool("socMissing"))null else int("soc",20).takeIf{it in 0..100},
            checked=checks("checked"),omitted=checks("omitted"),route=if(route==RoutePhase.REQUESTED)RoutePhase.FAILED else route,
            parked=bool("parked") && route==RoutePhase.ARRIVED,
            sleepChecks=checks("sleep").intersect(JourneyChecklists.sleep(checklistVersion).keys),exitChecks=checks("exit").intersect(JourneyChecklists.exit(checklistVersion).keys),
            checklistVersion=checklistVersion,
            sleepReady=bool("sleepReady"),completed=bool("completed"),restricted=bool("restricted"),dark=bool("dark"),
            batteryStatus=runCatching{ReadingStatus.valueOf(p[stringPreferencesKey("batteryStatus")] ?: "UNKNOWN")}.getOrDefault(ReadingStatus.UNKNOWN),
            socSource=if(version==1)Source.DEMO else runCatching { Source.valueOf(p[stringPreferencesKey("socSource")] ?: "UNKNOWN") }.getOrDefault(Source.UNKNOWN),
            socReceivedAtMillis=p[longPreferencesKey("socReceivedAt")])
        if(version<8)state=migrateCombinedPreparation(state)
        val itemIds=state.decisions.map{it.id}.toSet()
        state=state.copy(checked=state.checked.intersect(itemIds)-state.omitted,omitted=state.omitted.intersect(itemIds))
        if(state.step>=5 && !state.canSleep) state=state.copy(step=4,sleepReady=false,completed=false)
        if(!state.sleepChecks.containsAll(state.requiredSleepIds)) state=state.copy(sleepReady=false)
        if(state.step==6 && !state.sleepReady) state=state.copy(step=5,completed=false)
        if(!state.canFinish) state=state.copy(completed=false)
        return state to if(bool("recovered"))"손상된 여행 저장 파일을 복구했습니다. 처음부터 시작합니다." else if(route==RoutePhase.REQUESTED)"이동 정보 준비가 중단됐어요. 다시 시도해 주세요." else if(version<8 && state.step<=3 && (checks("checked")+checks("omitted")).any{it in setOf("water","bedding-count")})"준비 항목을 나눴어요. 식수·회수 용품과 매트·침구를 각각 확인해 주세요." else null
    }
    internal suspend fun archivedRecord(id:String):String?=JSONObject(store.data.first()[stringPreferencesKey("archive")] ?: "{}").optJSONObject(id)?.toString()
    internal suspend fun syncSnapshot():Pair<Map<String,String>,Set<String>> {
        val p=store.data.first();val archive=JSONObject(p[stringPreferencesKey("archive")] ?: "{}")
        return archive.keys().asSequence().associateWith{archive.getJSONObject(it).toString()} to (p[stringSetPreferencesKey("deletedArchives")] ?: emptySet())
    }
    internal suspend fun planSnapshot():Triple<String,Long,String>? {
        val p=store.data.first();val s=decode(p).first;val id=s.tripId ?: return null
        if(id in (p[stringSetPreferencesKey("deletedArchives")] ?: emptySet()))return null
        val revision=JSONObject(p[stringPreferencesKey("syncRevisions")] ?: "{}").optLong(id,0)
        val data=JSONObject().apply{
            put("schemaVersion",2);put("ruleVersion",com.chapark.app.domain.TripPolicy.RULE_VERSION);put("placeId",s.placeId);put("chargerId",s.chargerId)
            put("people",s.input.people);put("cookingPlan",s.input.cookingPlan.name);put("cookingNeedsReview",s.input.cookingNeedsReview);put("pets",s.input.pets);put("sleepingBag",s.input.sleepingBag)
            put("threshold",s.input.batteryThreshold);put("endHour",s.input.endHour)
            put("date",s.input.schedule?.date);put("zone",s.input.schedule?.zoneId)
            put("step",s.step);put("completed",s.completed)
            put("checked",JSONArray(s.checked.sorted()));put("omitted",JSONArray(s.omitted.sorted()));put("sleepChecks",JSONArray(s.sleepChecks.sorted()));put("exitChecks",JSONArray(s.exitChecks.sorted()))
            put("forgotten",JSONArray(s.forgotten.sorted()));put("records",JSONArray(JourneyRecordCodec.encode(s.records)))
        }
        return Triple(id,revision,data.toString())
    }
    internal suspend fun acknowledgePlan(id:String,revision:Long){store.edit{p->
        val revisions=JSONObject(p[stringPreferencesKey("syncRevisions")] ?: "{}")
        revisions.put(id,revision);p[stringPreferencesKey("syncRevisions")]=revisions.toString()
    }}
    /** Download is additive. Conflicting immutable records are kept locally and reported. */
    internal suspend fun mergeRecords(raw:String):Boolean {
        val root=JSONObject(raw);require(root.getInt("schemaVersion")==1)
        val rows=root.getJSONArray("records");require(rows.length()<=10000)
        var conflict=false
        store.edit{p->
            val archive=JSONObject(p[stringPreferencesKey("archive")] ?: "{}")
            val deleted=(p[stringSetPreferencesKey("deletedArchives")] ?: emptySet()).toMutableSet()
            val tombstones=root.optJSONArray("deleted") ?: JSONArray()
            for(i in 0 until tombstones.length())deleted.add(tombstones.getString(i))
            deleted.forEach{archive.remove(it)}
            for(i in 0 until rows.length()){
                val row=rows.getJSONObject(i);val id=row.getString("id");val data=row.getJSONObject("data")
                require(id.matches(Regex("[A-Za-z0-9_-]{1,100}")))
                require(data.getString("placeName").length in 1..300)
                val encoded=data.getJSONArray("records").toString()
                require(encoded.length<=1_000_000)
                JourneyRecordCodec.decode(encoded)
                if(id !in deleted){
                    val old=archive.optJSONObject(id)
                    if(old==null)archive.put(id,data)
                    else if(canonical(old)!=canonical(data))conflict=true
                }
            }
            p[stringPreferencesKey("archive")]=archive.toString()
            p[stringSetPreferencesKey("deletedArchives")]=deleted
        }
        return conflict
    }
    private fun canonical(value:Any?):String=when(value){
        is JSONObject->value.keys().asSequence().sorted().joinToString(prefix="{",postfix="}"){JSONObject.quote(it)+":"+canonical(value.get(it))}
        is JSONArray->(0 until value.length()).joinToString(prefix="[",postfix="]"){canonical(value.get(it))}
        is String->JSONObject.quote(value)
        null,JSONObject.NULL->"null"
        else->value.toString()
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
        val syncRevisions=p[stringPreferencesKey("syncRevisions")] ?: "{}"
        if(s.completed && s.tripId!=null && s.tripId !in deleted && !archive.has(s.tripId))archive.put(s.tripId,JSONObject().apply {
            put("chargerId",s.chargerId);put("placeId",s.placeId);put("placeName",s.place.name)
            put("date",s.input.schedule?.date);put("zone",s.input.schedule?.zoneId)
            put("forgotten",JSONArray((s.forgotten+s.decisions.filter{it.id in s.omitted}.map{it.title}).sorted()));
            put("cookingPlan",s.input.cookingPlan.name);put("omitted",JSONArray(s.omitted.sorted()));
            put("people",s.input.people);put("threshold",s.input.batteryThreshold)
            put("records",JSONArray(JourneyRecordCodec.encode(s.records)))
        })
        p.clear()
        p[stringPreferencesKey("syncRevisions")]=syncRevisions
        p[stringPreferencesKey("archive")]=archive.toString()
        p[stringSetPreferencesKey("deletedArchives")]=deleted
        p[intPreferencesKey("schema")]=9
        p[intPreferencesKey("checklistVersion")]=s.checklistVersion
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
        p[stringPreferencesKey("cookingPlan")]=s.input.cookingPlan.name; p[booleanPreferencesKey("cookingNeedsReview")]=s.input.cookingNeedsReview; p[booleanPreferencesKey("pets")]=s.input.pets
        p[booleanPreferencesKey("bag")]=s.input.sleepingBag; p[stringPreferencesKey("place")]=s.placeId
        p[booleanPreferencesKey("socMissing")]=s.soc==null; p[intPreferencesKey("soc")]=s.soc ?: 0
        p[stringSetPreferencesKey("checked")]=s.checked;p[stringSetPreferencesKey("omitted")]=s.omitted; p[stringPreferencesKey("route")]=s.route.name
        p[booleanPreferencesKey("parked")]=s.parked; p[stringSetPreferencesKey("sleep")]=s.sleepChecks
        p[stringSetPreferencesKey("exit")]=s.exitChecks; p[booleanPreferencesKey("sleepReady")]=s.sleepReady
        p[booleanPreferencesKey("completed")]=s.completed; p[booleanPreferencesKey("restricted")]=s.restricted
        p[booleanPreferencesKey("dark")]=s.dark
    } }
}

data class ArchivedTrip(val id:String,val placeName:String,val date:String,val zone:String,val records:List<JourneyRecord>,val forgotten:List<String>)
