package com.chapark.ivi.prototype

import com.chapark.ivi.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/** Development endpoint only. Immutable completed records make retries idempotent. */
private class SyncConflict(val plan:Boolean=false):Exception()
class RecordSync(private val store:JourneyStore){
    suspend fun sync():String=withContext(Dispatchers.IO){
        if(!BuildConfig.DEBUG || BuildConfig.LOCAL_API_TOKEN.isBlank())return@withContext "기기에 저장됨"
        try {
            val (records,deleted)=store.syncSnapshot()
            fun request(path:String,method:String,body:String?):String {
                val conn=URL("http://10.0.2.2:8091/v1/$path").openConnection() as HttpURLConnection
                try{
                    conn.requestMethod=method;conn.connectTimeout=2500;conn.readTimeout=2500
                    conn.setRequestProperty("Authorization","Bearer ${BuildConfig.LOCAL_API_TOKEN}")
                    if(body!=null){conn.doOutput=true;conn.setRequestProperty("Content-Type","application/json");conn.outputStream.use{it.write(body.toByteArray())}}
                    val code=conn.responseCode
                    if(code==409 || code==410)throw SyncConflict(path.startsWith("plans/") && code==409)
                    check(code in 200..299){"sync:$code"}
                    return conn.inputStream.bufferedReader().use{it.readText()}
                }finally{conn.disconnect()}
            }
            deleted.forEach{request("records/$it","DELETE",null);request("plans/$it","DELETE",null)}
            val downloadConflict=store.mergeRecords(request("records","GET",null))
            store.syncSnapshot().first.forEach{(id,body)->request("records/$id","PUT",body)}
            val plan=store.planSnapshot()
            plan?.let{(id,revision,data)->
                val envelope=JSONObject().put("baseRevision",revision).put("data",JSONObject(data)).toString()
                val response=JSONObject(request("plans/$id","PUT",envelope))
                store.acknowledgePlan(id,response.getLong("revision"))
            }
            if(downloadConflict)throw SyncConflict()
            if(records.isEmpty() && deleted.isEmpty() && plan==null)"기기에 저장됨" else "기기·서버에 저장됨"
        }catch(e:CancellationException){throw e}
        catch(e:SyncConflict){if(e.plan)"기기에 저장됨 · 계획 충돌 확인 필요" else "기기에 저장됨 · 기록 충돌 확인 필요"}
        catch(e:Exception){"기기에 저장됨 · 서버 연결 대기"}
    }
}
