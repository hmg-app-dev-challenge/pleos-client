package com.chapark.ivi

import androidx.test.platform.app.InstrumentationRegistry
import com.chapark.ivi.prototype.JourneyStore
import com.chapark.prototype.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class JourneyStoreTest {
    @Test fun freshRepositoryRestoresFieldsAndPendingRequestBecomesRetryable() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val store=JourneyStore(context)
        val original=store.load().first
        InstrumentationRegistry.getInstrumentation().sendStatus(0,android.os.Bundle().apply {
            putString("stream","STORE_BEFORE: step=${original.step}, completed=${original.completed}, uid=${android.os.Process.myUid()}\n")
        })
        try {
            val sample=Journey(step=4,soc=null,route=RoutePhase.REQUESTED,restricted=true,dark=true,checked=setOf("water"))
            store.save(sample)
            val restored=JourneyStore(context).load()
            assertEquals(RoutePhase.FAILED,restored.first.route)
            assertNull(restored.first.soc)
            assertTrue(restored.first.restricted)
            assertTrue(restored.first.dark)
            assertEquals(setOf("water"),restored.first.checked)
            assertNotNull(restored.second)
        } finally { store.save(original) }
    }
}
