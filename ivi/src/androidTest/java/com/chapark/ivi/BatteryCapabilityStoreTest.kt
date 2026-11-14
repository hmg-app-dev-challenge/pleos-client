package com.chapark.ivi
import androidx.datastore.preferences.core.*
import androidx.test.platform.app.InstrumentationRegistry
import com.chapark.app.domain.*
import com.chapark.ivi.prototype.JourneyStore
import com.chapark.prototype.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
class BatteryCapabilityStoreTest {
    private fun store()=JourneyStore(InstrumentationRegistry.getInstrumentation().targetContext)
    @Test fun manualReadingKeepsUnsupportedCapabilityAndArchive()=runBlocking {
        val store=store();val original=store.load().first;val archive=store.syncSnapshot()
        try {
            val state=Journey(step=3,placeId="camp-nanji",soc=30,socSource=Source.MANUAL,
                batteryStatus=ReadingStatus.AVAILABLE,batteryCapability=BatteryCapability.UNSUPPORTED,vehicleBatteryStatus=ReadingStatus.UNSUPPORTED,socReceivedAtMillis=123)
            store.save(state)
            val restored=store.load().first
            assertEquals(30,restored.soc);assertEquals(Source.MANUAL,restored.socSource)
            assertEquals(BatteryCapability.UNSUPPORTED,restored.batteryCapability)
            assertEquals(ReadingStatus.UNSUPPORTED,restored.vehicleBatteryStatus)
            assertEquals(123L,restored.socReceivedAtMillis)
            assertEquals(archive,store.syncSnapshot())
        }finally{store.save(original)}
    }
    @Test fun legacyValuesNeverImplySupportedCapability() {
        for(version in 1..10) {
            val restored=store().decode(preferencesOf(intPreferencesKey("schema") to version,
                intPreferencesKey("soc") to 37,stringPreferencesKey("batteryStatus") to "AVAILABLE")).first
            assertEquals(BatteryCapability.UNKNOWN,restored.batteryCapability)
            assertEquals(37,restored.soc)
        }
    }
}
