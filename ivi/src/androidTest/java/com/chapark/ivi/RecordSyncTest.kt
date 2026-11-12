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
    @Test fun activePlanRevisionRetriesAndRemoteRecordDeletionMerge()=runBlocking {
        val store=JourneyStore(InstrumentationRegistry.getInstrumentation().targetContext)
        val original=store.load().first;val id="plan-test-${UUID.randomUUID()}"
        try{
            store.save(Journey(tripId=id,placeId="camp-nanji"))
            assertEquals("기기·서버에 저장됨",RecordSync(store).sync())
            assertEquals(1L,store.planSnapshot()!!.second)
            assertEquals("기기·서버에 저장됨",RecordSync(store).sync())
            assertEquals(1L,store.planSnapshot()!!.second)
            store.save(store.load().first.copy(input=store.load().first.input.copy(people=3)))
            assertEquals("기기·서버에 저장됨",RecordSync(store).sync())
            assertEquals(2L,store.planSnapshot()!!.second)
            val imported=PlanImport.list().single{it.id==id}.asNewJourney(dark=true)
            assertEquals(3,imported.input.people)
            assertEquals(1,imported.step)
            assertTrue(imported.checked.isEmpty())
            assertEquals(RoutePhase.NONE,imported.route)
            assertFalse(imported.parked)
            assertNotEquals(id,imported.tripId)
            assertTrue(imported.dark)
            val data=JSONObject().put("placeId","camp-nanji").put("placeName","한강").put("records",org.json.JSONArray())
            val download=JSONObject().put("schemaVersion",1).put("records",org.json.JSONArray().put(JSONObject().put("id",id).put("data",data)))
            assertFalse(store.mergeRecords(download.toString()))
            assertNotNull(store.archivedRecord(id))
            data.put("placeName","변경된 이름")
            assertTrue(store.mergeRecords(download.toString()))
            assertEquals("한강",JSONObject(store.archivedRecord(id)!!).getString("placeName"))
            download.put("deleted",org.json.JSONArray().put(id))
            assertFalse(store.mergeRecords(download.toString()))
            assertNull(store.archivedRecord(id))
        }finally{store.deleteHistory(id);RecordSync(store).sync();store.save(original)}
    }
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
