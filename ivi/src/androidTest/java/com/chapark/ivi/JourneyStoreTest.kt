package com.chapark.ivi

import androidx.test.platform.app.InstrumentationRegistry
import com.chapark.ivi.prototype.JourneyStore
import com.chapark.prototype.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import androidx.datastore.preferences.core.*
import com.chapark.app.domain.*

class JourneyStoreTest {
    @Test fun legacySchemaKeepsJourneyAndDoesNotInventObservationTime() {
        val store=JourneyStore(InstrumentationRegistry.getInstrumentation().targetContext)
        val restored=store.decode(preferencesOf(intPreferencesKey("schema") to 1,
            intPreferencesKey("step") to 4,intPreferencesKey("people") to 2,
            stringPreferencesKey("place") to "demo-b",stringPreferencesKey("route") to "GUIDING",
            stringSetPreferencesKey("checked") to setOf("drinking-water"))).first
        assertEquals(4,restored.step)
        assertEquals(RoutePhase.GUIDING,restored.route)
        assertEquals("demo-b",restored.placeId)
        assertEquals(setOf("drinking-water"),restored.checked)
        assertEquals(35,restored.input.batteryThreshold)
        assertEquals(Source.DEMO,restored.socSource)
        assertNull(restored.socReceivedAtMillis)
        assertNull(restored.input.schedule)
        assertTrue(restored.records.isEmpty())
    }
    @Test fun freshRepositoryRestoresFieldsAndPendingRequestBecomesRetryable() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val store=JourneyStore(context)
        val original=store.load().first
        InstrumentationRegistry.getInstrumentation().sendStatus(0,android.os.Bundle().apply {
            putString("stream","STORE_BEFORE: step=${original.step}, completed=${original.completed}, uid=${android.os.Process.myUid()}\n")
        })
        try {
            val sample=Journey(records=listOf(JourneyRecord("PREPARATION_CHECK",100L,"USER","drinking-water","생수 · 확인",false,"camp-nanji")),tripId="test-trip",input=TripInput(3,com.chapark.app.domain.CookingPlan.COOKING,false,true,45,8,TripSchedule("2026-12-31","Asia/Seoul")),socSource=Source.MANUAL,socReceivedAtMillis=1234L,step=4,soc=null,route=RoutePhase.REQUESTED,restricted=true,dark=true,checked=setOf("drinking-water"))
            if(original.completed)store.save(original)
            store.save(sample)
            if(original.completed && original.tripId!=null)assertNotNull(store.archivedRecord(original.tripId!!))
            val restored=JourneyStore(context).load()
            assertEquals(RoutePhase.FAILED,restored.first.route)
            assertNull(restored.first.soc)
            assertEquals(sample.input,restored.first.input)
            assertEquals(sample.records,restored.first.records)
            assertEquals("test-trip",restored.first.tripId)
            assertEquals("test-trip",JourneyStore(context).load().first.tripId)
            assertEquals(Source.MANUAL,restored.first.socSource)
            assertEquals(1234L,restored.first.socReceivedAtMillis)
            assertTrue(restored.first.restricted)
            assertTrue(restored.first.dark)
            assertEquals(setOf("drinking-water"),restored.first.checked)
            assertNotNull(restored.second)
        } finally { store.save(original) }
    }
}
