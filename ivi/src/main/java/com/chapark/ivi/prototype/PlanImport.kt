package com.chapark.ivi.prototype

import com.chapark.app.domain.*
import com.chapark.app.data.*
import com.chapark.prototype.*
import com.chapark.ivi.BuildConfig
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

internal data class SavedPlan(val id:String,val revision:Long,val name:String,val input:TripInput,val placeId:String,val chargerId:String?) {
    fun asNewJourney(dark:Boolean)=Journey(tripId=UUID.randomUUID().toString(),step=1,input=input,placeId=placeId,
        chargerId=chargerId,dark=dark,records=listOf(JourneyRecord("PLAN_IMPORTED",System.currentTimeMillis(),"USER",label="서버 계획 $id · 버전 ${revision}의 조건 가져옴",placeId=placeId)))
}
internal object PlanImport {
    suspend fun list():List<SavedPlan> = withContext(Dispatchers.IO){
        check(BuildConfig.DEBUG && BuildConfig.LOCAL_API_TOKEN.isNotBlank())
        val connection=URL("http://10.0.2.2:8091/v1/plans").openConnection() as HttpURLConnection
        try{
            connection.connectTimeout=2500;connection.readTimeout=2500
            connection.setRequestProperty("Authorization","Bearer ${BuildConfig.LOCAL_API_TOKEN}")
            check(connection.responseCode==200)
            val raw=connection.inputStream.bufferedReader().use{reader->
                val result=StringBuilder();val buffer=CharArray(8192)
                while(result.length<=2_000_000){val n=reader.read(buffer);if(n<0)break;result.append(buffer,0,n)}
                require(result.length<=2_000_000);result.toString()
            }
            parse(raw)
        }finally{connection.disconnect()}
    }
    internal fun parse(raw:String):List<SavedPlan>{
        val root=JSONObject(raw);require(root.getInt("schemaVersion")==1)
        val rows=root.getJSONArray("plans");require(rows.length()<=10000)
        return (0 until rows.length()).mapNotNull{i->runCatching{
            val row=rows.getJSONObject(i);val data=row.getJSONObject("data")
            require(data.getInt("schemaVersion") in 1..2 && !data.optBoolean("completed"))
            val id=row.getString("id");require(id.matches(Regex("[A-Za-z0-9_-]{1,100}")))
            val place=NavigationPlaces.find(data.getString("placeId")) ?: error("unknown place")
            val schedule=if(data.isNull("date"))null else TripSchedule(data.getString("date"),data.getString("zone"))
            schedule?.let{java.time.LocalDate.parse(it.date);java.time.ZoneId.of(it.zoneId)}
            val people=data.getInt("people");val threshold=data.getInt("threshold");val hour=data.getInt("endHour")
            require(people in 1..TripPolicy.MAX_PEOPLE && threshold in 0..100 && hour in 0..23)
            val legacy=data.getInt("schemaVersion")==1
            val cooking=if(legacy)CookingPlan.fromLegacy(data.getBoolean("cooking")) else CookingPlan.valueOf(data.getString("cookingPlan"))
            val input=TripInput(people,cooking,data.getBoolean("pets"),data.getBoolean("sleepingBag"),threshold,hour,schedule,cookingNeedsReview=if(legacy)cooking==CookingPlan.COOKING else data.optBoolean("cookingNeedsReview"))
            val revision=row.getLong("revision");require(revision>0)
            SavedPlan(id,revision,place.place.name,input,place.place.id,data.optString("chargerId").takeIf{ChargingPlaces.find(it)!=null})
        }.getOrNull()}.sortedByDescending{it.input.schedule?.date ?: ""}
    }
}
