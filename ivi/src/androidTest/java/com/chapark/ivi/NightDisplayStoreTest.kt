package com.chapark.ivi

import androidx.datastore.preferences.core.*
import androidx.test.platform.app.InstrumentationRegistry
import com.chapark.ivi.prototype.JourneyStore
import com.chapark.prototype.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NightDisplayStoreTest {
    private fun store()=JourneyStore(InstrumentationRegistry.getInstrumentation().targetContext)
    @Test fun legacyModesMigrateWithoutChangingChecksOrNavigation() {
        for(version in 1..9)for(mode in listOf("REST","SLEEP","STARS")) {
            val restored=store().decode(preferencesOf(intPreferencesKey("schema") to version,
                intPreferencesKey("step") to 5,stringPreferencesKey("route") to "ARRIVED",
                booleanPreferencesKey("parked") to true,stringPreferencesKey("restMode") to mode,
                stringSetPreferencesKey("sleep") to setOf("parking"))).first
            assertEquals(5,restored.step);assertEquals(RoutePhase.ARRIVED,restored.route)
            assertEquals(setOf("parking"),restored.sleepChecks)
            assertEquals(if(mode=="REST")NightDisplay.AUTO else NightDisplay.NIGHT,restored.nightDisplay)
        }
    }
    @Test fun explicitPreferenceSurvivesReloadWithoutTouchingArchive()=runBlocking {
        val store=store();val original=store.load().first;val archives=store.syncSnapshot()
        try {
            for(preference in NightDisplay.entries) {
                store.save(Journey(step=5,route=RoutePhase.ARRIVED,parked=true,restMode="SLEEP",nightDisplay=preference))
                assertEquals(preference,store.load().first.nightDisplay)
                assertEquals(archives,store.syncSnapshot())
            }
        }finally {store.save(original)}
    }
}
