package com.chapark.ivi

import androidx.test.platform.app.InstrumentationRegistry
import com.chapark.ivi.prototype.*
import com.chapark.prototype.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import org.json.JSONObject

class RecordSyncTest {
    @Test fun completedRecordUploadsAndDeletionSurvivesRetry()= runBlocking {
        val store=JourneyStore(InstrumentationRegistry.getInstrumentation().targetContext)
        val original=store.load().first
        val id="sync-test-${UUID.randomUUID()}"
        fun serverHasRecord():Boolean {
            val conn=URL("http://10.0.2.2:8091/v1/records").openConnection() as HttpURLConnection
            conn.connectTimeout=3000;conn.readTimeout=3000
            conn.setRequestProperty("Authorization","Bearer ${BuildConfig.LOCAL_API_TOKEN}")
            return try {
                val records=JSONObject(conn.inputStream.bufferedReader().use{it.readText()}).getJSONArray("records")
                (0 until records.length()).any{records.getJSONObject(it).getString("id")==id}
            }finally{conn.disconnect()}
        }
        try {
            store.save(Journey(tripId=id,placeId="camp-nanji",completed=true,
                records=listOf(JourneyRecord("TRIP_COMPLETED",System.currentTimeMillis(),"USER",placeId="camp-nanji"))))
            assertEquals("기기·서버에 저장됨",RecordSync(store).sync())
            assertTrue(serverHasRecord())
            assertEquals("기기·서버에 저장됨",RecordSync(store).sync())
            store.deleteHistory(id)
            assertEquals("기기·서버에 저장됨",RecordSync(store).sync())
            assertFalse(serverHasRecord())
            store.save(Journey(tripId=id,placeId="camp-nanji",completed=true))
            assertNull(store.archivedRecord(id))
        }finally{store.deleteHistory(id);RecordSync(store).sync();store.save(original)}
    }
}
