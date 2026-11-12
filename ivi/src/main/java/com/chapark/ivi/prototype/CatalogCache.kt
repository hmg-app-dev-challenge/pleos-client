package com.chapark.ivi.prototype

import android.content.Context
import android.util.AtomicFile
import com.chapark.app.data.*
import com.chapark.app.domain.*
import com.chapark.ivi.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.time.LocalDate

/** Downloaded data never changes an in-progress trip; activate only at a new-trip boundary. */
internal class CatalogCache(context:Context){
    private val active=AtomicFile(File(context.filesDir,"catalog-active.json"))
    private val pending=AtomicFile(File(context.filesDir,"catalog-pending.json"))
    fun loadActive(){runCatching{parse(active.openRead().bufferedReader().use{it.readText()})}.getOrNull()?.let{AcceptedCatalog.install(it)}}
    fun activatePending(){
        val raw=runCatching{pending.openRead().bufferedReader().use{it.readText()}}.getOrNull() ?: return
        val value=runCatching{parse(raw)}.getOrNull() ?: return
        write(active,raw);AcceptedCatalog.install(value);pending.delete()
    }
    suspend fun refresh():String=withContext(Dispatchers.IO){
        if(!BuildConfig.DEBUG || BuildConfig.LOCAL_API_TOKEN.isBlank())return@withContext ""
        try{
            val rules=JSONObject(get("rules"))
            require(rules.getString("version")=="v211-1")
            val raw=get("catalog");val value=parse(raw)
            if(value.version==(AcceptedCatalog.snapshot?.version ?: PlaceEvidenceCatalog.VERSION))"장소 자료 확인됨"
            else {write(pending,raw);"새 장소 자료 · 다음 여행부터 적용"}
        }catch(e:CancellationException){throw e}
        catch(e:Exception){"저장된 장소 자료 사용 중"}
    }
    private fun get(path:String):String {
        val conn=URL("http://10.0.2.2:8091/v1/$path").openConnection() as HttpURLConnection
        return try{
            conn.connectTimeout=2500;conn.readTimeout=2500
            conn.setRequestProperty("Authorization","Bearer ${BuildConfig.LOCAL_API_TOKEN}")
            check(conn.responseCode==200)
            val bytes=conn.inputStream.use{input->
                val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                while(output.size()<=1_000_000){val count=input.read(buffer);if(count<0)break;output.write(buffer,0,count)}
                output.toByteArray()
            }
            require(bytes.size<=1_000_000);bytes.toString(Charsets.UTF_8)
        }finally{conn.disconnect()}
    }
    private fun write(file:AtomicFile,raw:String){
        val stream=file.startWrite()
        try{stream.write(raw.toByteArray());file.finishWrite(stream)}catch(e:Exception){file.failWrite(stream);throw e}
    }
    companion object {
        internal fun parse(raw:String):CatalogSnapshot {
            val root=JSONObject(raw);require(root.getInt("schemaVersion")==1 && root.getString("ruleVersion")=="v211-1")
            val version=root.getString("version");require(version.length in 1..64)
            val list=root.getJSONArray("places");require(list.length() in 1..100)
            val evidence=mutableMapOf<String,PlaceEvidence>();val ids=mutableSetOf<String>()
            fun text(o:JSONObject,key:String,max:Int=300)=o.getString(key).also{require(it.isNotBlank() && it.length<=max)}
            fun url(o:JSONObject,key:String)=text(o,key,2000).also{require(URI(it).scheme=="https" && !URI(it).host.isNullOrBlank())}
            val places=(0 until list.length()).map{index->
                val o=list.getJSONObject(index);val id=text(o,"id",100);require(ids.add(id) && id.matches(Regex("[A-Za-z0-9_-]+")))
                val lat=o.getDouble("latitude");val lon=o.getDouble("longitude")
                require(lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0)
                require(o.getString("coordinateKind")=="MAP_POINT")
                val facts=o.getJSONArray("facts");require(facts.length() in 1..30)
                val entries=(0 until facts.length()).map{i->
                    val f=facts.getJSONObject(i)
                    val date=if(f.isNull("checkedOn"))null else f.getString("checkedOn").also{LocalDate.parse(it)}
                    Evidence(text(f,"label"),text(f,"detail",2000),url(f,"url"),date,Condition.valueOf(f.getString("condition")))
                }
                fun condition(key:String,label:String):Condition {
                    val proposed=Condition.valueOf(o.getString(key))
                    return if(proposed!=Condition.UNKNOWN && entries.none{it.label==label && it.condition==proposed && it.fresh(LocalDate.now())})Condition.UNKNOWN else proposed
                }
                evidence[id]=PlaceEvidence(Scenery.valueOf(o.getString("scenery")),entries,condition("parking","주차"))
                NavigationPlace(Place(id,text(o,"name"),"출처와 운영 조건을 확인해 주세요.",condition("overnight","차내 숙박"),
                    condition("nightToilet","야간 화장실"),condition("cooking","취사"),condition("fire","화기"),condition("pets","반려동물"),12),
                    lat,lon,text(o,"address"),url(o,"source"))
            }
            return CatalogSnapshot(version,places,evidence)
        }
    }
}
