package com.chapark.ivi

import androidx.test.platform.app.InstrumentationRegistry
import com.chapark.app.domain.*
import com.chapark.ivi.prototype.*
import com.chapark.prototype.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class TripMemoryStoreTest {
    @Test fun extendedEventsRoundTripAndOldEventsDoNotInventMetadata() {
        val original=JourneyRecord("PREPARATION_COMPLETED",100,"USER",placeId="camp-nanji",soc=20,socSource=Source.SDK,
            readingStatus=ReadingStatus.AVAILABLE,preparationCompleted=3,preparationRequired=3,
            placeMemory=PlaceMemory(listOf("강"),listOf("2026-11-14"),false))
        assertEquals(listOf(original),JourneyRecordCodec.decode(JourneyRecordCodec.encode(listOf(original))))
        val old="""[{"kind":"TRIP_COMPLETED","time":1,"origin":"USER","place":"camp-nanji","source":"UNKNOWN","soc":null,"label":null,"preparationRequired":null,"readingStatus":null}]"""
        val decoded=JourneyRecordCodec.decode(old).single()
        assertNull(decoded.placeMemory);assertNull(decoded.readingStatus);assertNull(decoded.preparationRequired)
        assertNull(MemoryDetails.decode(null));assertNull(MemoryDetails.decode(JSONObject().put("version",99)))
    }

    @Test fun archiveKeepsFirstSnapshotAcrossResaveAndDeletion()=runBlocking {
        val store=JourneyStore(InstrumentationRegistry.getInstrumentation().targetContext)
        val original=store.load().first
        val id="memory-${UUID.randomUUID()}"
        try {
            val draft=Journey(tripId=id,step=3,placeId="camp-nanji",soc=80,socSource=Source.SDK,
                input=TripInput(schedule=TripSchedule("2026-11-14","Asia/Seoul")))
            val departed=JourneyJournal.apply(draft.copy(checked=draft.departureItems.map{it.id}.toSet()),JourneyEvent.Next,100)
            val checkout=departed.copy(step=6,route=RoutePhase.ARRIVED,parked=true,sleepReady=true,
                sleepChecks=Journey.SLEEP_IDS,exitChecks=Journey.EXIT_IDS,soc=null,batteryStatus=ReadingStatus.DISCONNECTED,chargerId="emart-mapo",forgotten=setOf("컵"))
            val done=JourneyJournal.apply(checkout,JourneyEvent.Finish,200)
            store.save(done)
            val encoded=store.archivedRecord(id)!!
            val memory=store.history().single{it.id==id}
            assertEquals(6,memory.memory!!.sleepRequired);assertEquals(5,memory.memory.exitRequired)
            assertEquals("이마트 마포점 충전시설",memory.memory.chargerName)
            assertEquals(listOf("컵"),memory.forgotten)
            assertEquals(80,memory.records.single{it.kind=="PREPARATION_COMPLETED"}.soc)
            assertNull(memory.records.single{it.kind=="TRIP_COMPLETED"}.soc)
            store.save(done.copy(placeId="camp-gangdong",soc=99,chargerId=null,exitChecks=emptySet()))
            assertEquals(encoded,store.archivedRecord(id))
            assertEquals("난지캠핑장",store.history().single{it.id==id}.placeName)
            store.deleteHistory(id);store.save(done)
            assertNull(store.archivedRecord(id))
        } finally {store.deleteHistory(id);store.save(original)}
    }
}
