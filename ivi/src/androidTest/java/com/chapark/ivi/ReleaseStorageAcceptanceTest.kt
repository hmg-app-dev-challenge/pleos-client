package com.chapark.ivi

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.chapark.ivi.prototype.JourneyStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Runs against the signed APK before the first Activity launch. No fixture overwrite. */
class ReleaseStorageAcceptanceTest {
    @Test fun existingStorageLoadsAndRoundTripsWithoutChangingArchives()=runBlocking {
        val i=InstrumentationRegistry.getInstrumentation()
        val context=i.targetContext
        val file=java.io.File(context.filesDir,"datastore/ivi_demo_journey.preferences_pb")
        val existed=file.exists()
        val size=if(existed)file.length() else 0
        val store=JourneyStore(context)
        val archivesBefore=store.syncSnapshot()
        val (state,notice)=store.load()
        store.save(state)
        val restored=store.load().first
        assertEquals(state,restored)
        assertEquals(archivesBefore,store.syncSnapshot())
        i.sendStatus(0,Bundle().apply{putString("stream","RELEASE_STORAGE: existed=$existed previousBytes=$size step=${state.step} completed=${state.completed} archived=${archivesBefore.first.size} preserved=true notice=$notice\n")})
    }
}
