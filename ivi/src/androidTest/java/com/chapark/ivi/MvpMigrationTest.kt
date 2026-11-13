package com.chapark.ivi

import androidx.datastore.preferences.core.*
import androidx.test.platform.app.InstrumentationRegistry
import com.chapark.app.domain.*
import com.chapark.ivi.prototype.*
import com.chapark.prototype.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class MvpMigrationTest {
    private fun store()=JourneyStore(InstrumentationRegistry.getInstrumentation().targetContext)
    @Test fun allLegacyVersionsKeepNavigationChecksAndMarkAmbiguousCookingForReview() {
        for(version in 1..6){
            val state=store().decode(preferencesOf(intPreferencesKey("schema") to version,
                stringPreferencesKey("tripId") to "legacy",intPreferencesKey("step") to 4,
                stringPreferencesKey("route") to "GUIDING",booleanPreferencesKey("cooking") to true,
                stringPreferencesKey("place") to "camp-nanji",stringSetPreferencesKey("checked") to setOf("water"))).first
            assertEquals("legacy",state.tripId);assertEquals(4,state.step)
            assertEquals(RoutePhase.GUIDING,state.route);assertEquals(setOf("water"),state.checked)
            assertEquals(CookingPlan.COOKING,state.input.cookingPlan)
            assertTrue(state.input.cookingNeedsReview);assertTrue(state.omitted.isEmpty())
        }
    }
    @Test fun newSchemaRoundTripsEveryCookingModeAndOmissionsWithoutRewritingArchive()=runBlocking {
        val store=store();val original=store.load().first
        val archiveBefore=store.syncSnapshot()
        try{
            for(mode in CookingPlan.entries){
                val sample=Journey(tripId="mvp-storage-test",step=3,placeId="camp-nanji",input=TripInput(cookingPlan=mode),checked=setOf("water"),omitted=setOf("bedding-count"))
                store.save(sample);val loaded=store.load().first
                assertEquals(mode,loaded.input.cookingPlan);assertFalse(loaded.input.cookingNeedsReview)
                assertEquals(sample.omitted,loaded.omitted);assertEquals(sample.checked,loaded.checked)
                val plan=JSONObject(store.planSnapshot()!!.third)
                assertEquals(2,plan.getInt("schemaVersion"));assertEquals(mode.name,plan.getString("cookingPlan"))
                assertFalse(plan.has("cooking"))
            }
            assertEquals(archiveBefore,store.syncSnapshot())
        }finally{store.save(original)}
    }
    @Test fun unknownNewCookingCannotSilentlyPassDepartureReview() {
        val state=store().decode(preferencesOf(intPreferencesKey("schema") to 7,
            stringPreferencesKey("cookingPlan") to "FUTURE_MODE")).first
        assertTrue(state.input.cookingNeedsReview)
    }
}
