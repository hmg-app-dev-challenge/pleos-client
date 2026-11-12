package com.chapark.ivi.prototype

import com.chapark.ivi.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Development endpoint only. Immutable completed records make retries idempotent. */
private class SyncConflict:Exception()
class RecordSync(private val store:JourneyStore){
    suspend fun sync():String=withContext(Dispatchers.IO){
        if(!BuildConfig.DEBUG || BuildConfig.LOCAL_API_TOKEN.isBlank())return@withContext "기기에 저장됨"
        try {
            val (records,deleted)=store.syncSnapshot()
            fun request(id:String,method:String,body:String?){
                val conn=URL("http://10.0.2.2:8091/v1/records/$id").openConnection() as HttpURLConnection
                try{
                    conn.requestMethod=method;conn.connectTimeout=2500;conn.readTimeout=2500
                    conn.setRequestProperty("Authorization","Bearer ${BuildConfig.LOCAL_API_TOKEN}")
                    if(body!=null){conn.doOutput=true;conn.setRequestProperty("Content-Type","application/json");conn.outputStream.use{it.write(body.toByteArray())}}
                    val code=conn.responseCode
                    if(code==409 || code==410)throw SyncConflict()
                    check(code in 200..299){"sync:$code"}
                }finally{conn.disconnect()}
            }
            deleted.forEach{request(it,"DELETE",null)}
            records.filterKeys{it !in deleted}.forEach{(id,body)->request(id,"PUT",body)}
            if(records.isEmpty() && deleted.isEmpty())"기기에 저장됨" else "기기·서버에 저장됨"
        }catch(e:CancellationException){throw e}
        catch(e:SyncConflict){"기기에 저장됨 · 서버 기록 충돌 확인 필요"}
        catch(e:Exception){"기기에 저장됨 · 서버 연결 대기"}
    }
}
