package com.chapark.prototype

import com.chapark.app.domain.TripInput
import com.chapark.app.domain.TripSchedule
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class NightDisplayTest {
    private val stay=Journey(step=5,placeId="camp-nanji",route=RoutePhase.ARRIVED,parked=true,
        input=TripInput(schedule=TripSchedule("2026-11-14","Asia/Seoul")))
    @Test fun sunsetInclusiveSunriseExclusiveAndOldOrFutureTripsStayStandard() {
        val window=NightDisplayPolicy.window(stay)!!
        assertFalse(NightDisplayPolicy.active(stay,window.sunset.minusSeconds(1)))
        assertTrue(NightDisplayPolicy.active(stay,window.sunset))
        assertTrue(NightDisplayPolicy.active(stay,Instant.parse("2026-11-14T16:00:00Z"))) // 01:00 next day in Seoul
        assertTrue(NightDisplayPolicy.active(stay,window.sunrise.minusSeconds(1)))
        assertFalse(NightDisplayPolicy.active(stay,window.sunrise))
        assertFalse(NightDisplayPolicy.active(stay,window.sunset.minusSeconds(86400)))
        assertFalse(NightDisplayPolicy.active(stay,window.sunrise.plusSeconds(86400)))
    }
    @Test fun drivingAndUnconfirmedParkingOverrideAutomaticAndManualNight() {
        val now=NightDisplayPolicy.window(stay)!!.sunset
        for(preference in NightDisplay.entries) {
            val s=stay.copy(nightDisplay=preference)
            listOf(s.copy(restricted=true),s.copy(parked=false),s.copy(route=RoutePhase.GUIDING),
                s.copy(completed=true),s.copy(step=4),s.copy(step=6),s.copy(step=0)).forEach {
                assertFalse(NightDisplayPolicy.active(it,now))
            }
        }
    }
    @Test fun missingInvalidOrUnknownSolarContextAllowsExplicitNightAndExit() {
        for(s in listOf(stay.copy(input=TripInput()),stay.copy(placeId="unknown"),
            stay.copy(input=TripInput(schedule=TripSchedule("not-a-date","Asia/Seoul"))),
            stay.copy(input=TripInput(schedule=TripSchedule("2026-11-14","bad-zone"))))) {
            assertNull(NightDisplayPolicy.window(s))
            assertFalse(NightDisplayPolicy.active(s,Instant.EPOCH))
            val manual=JourneyReducer.apply(s,JourneyEvent.NightScreen(NightDisplay.NIGHT))
            assertTrue(NightDisplayPolicy.active(manual,Instant.EPOCH))
            assertFalse(NightDisplayPolicy.active(JourneyReducer.apply(manual,JourneyEvent.NightScreen(NightDisplay.STANDARD)),Instant.EPOCH))
        }
    }
    @Test fun manualChoicePersistsWithoutMutatingThemeOrChecksAndNewTripResetsIt() {
        val s=stay.copy(dark=true,sleepChecks=setOf("parking"))
        val sleeping=JourneyReducer.apply(s,JourneyEvent.RestMode("SLEEP"))
        assertEquals(NightDisplay.NIGHT,sleeping.nightDisplay)
        assertEquals(s.sleepChecks,sleeping.sleepChecks);assertTrue(sleeping.dark)
        val standard=JourneyReducer.apply(sleeping,JourneyEvent.NightScreen(NightDisplay.STANDARD))
        assertEquals("SLEEP",standard.restMode)
        assertFalse(NightDisplayPolicy.active(standard,NightDisplayPolicy.window(s)!!.sunset))
        assertEquals(NightDisplay.AUTO,JourneyReducer.apply(sleeping,JourneyEvent.NewTrip).nightDisplay)
        val locked=s.copy(restricted=true)
        assertEquals(locked,JourneyReducer.apply(locked,JourneyEvent.RestMode("SLEEP")))
        assertEquals(locked,JourneyReducer.apply(locked,JourneyEvent.NightScreen(NightDisplay.NIGHT)))
    }
    @Test fun preferenceMigrationPreservesOldManualModesAndRejectsUnknownValues() {
        assertEquals(NightDisplay.NIGHT,NightDisplayPolicy.restore(null,"SLEEP"))
        assertEquals(NightDisplay.NIGHT,NightDisplayPolicy.restore(null,"STARS"))
        assertEquals(NightDisplay.AUTO,NightDisplayPolicy.restore(null,"REST"))
        assertEquals(NightDisplay.AUTO,NightDisplayPolicy.restore("future","SLEEP"))
        assertEquals(NightDisplay.STANDARD,NightDisplayPolicy.restore("STANDARD","SLEEP"))
    }
    @Test fun starSceneRequiresConfirmedParkingAndLeavesJourneyChecksUntouched(){
        val base=stay.copy(checked=setOf("drinking-water"),sleepChecks=setOf("parking"))
        val stars=JourneyReducer.apply(base,JourneyEvent.RestMode("STARS"))
        assertTrue(NightDisplayPolicy.starsActive(stars))
        for(blocked in listOf(stars.copy(restricted=true),stars.copy(parked=false),stars.copy(route=RoutePhase.GUIDING),
            stars.copy(step=4),stars.copy(step=6),stars.copy(completed=true))) {
            assertFalse(NightDisplayPolicy.starsActive(blocked))
        }
        val returned=JourneyReducer.apply(stars,JourneyEvent.RestMode("REST"))
        assertFalse(NightDisplayPolicy.starsActive(returned))
        assertEquals(base.checked,returned.checked)
        assertEquals(base.sleepChecks,returned.sleepChecks)
        assertEquals(base.step,returned.step)
        assertEquals(base.input,returned.input)
        assertEquals(base.dark,returned.dark)
    }
}
