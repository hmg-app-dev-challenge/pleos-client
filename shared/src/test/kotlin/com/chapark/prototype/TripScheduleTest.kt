package com.chapark.prototype

import com.chapark.app.domain.TripSchedule
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class TripScheduleTest {
    @Test fun overnightUsesNextCalendarDateAndRecordedZone(){
        assertEquals(Instant.parse("2027-01-01T01:00:00Z"),TripSchedule("2026-12-31","Asia/Seoul").departureEnd(10))
        assertEquals("2027-01-01",TripSchedule("2026-12-31","Asia/Seoul").shiftDays(1)?.date)
    }
    @Test fun gapAndOverlapRequireExplicitResolution(){
        assertNull(TripSchedule("2026-03-07","America/New_York").departureEnd(2))
        assertNull(TripSchedule("2026-10-31","America/New_York").departureEnd(1))
        assertNotNull(TripSchedule("2026-10-31","America/New_York").departureEnd(3))
    }
    @Test fun invalidInputsDoNotCreateAnInstant(){
        assertFalse(TripSchedule("2026-02-30","Asia/Seoul").valid)
        assertNull(TripSchedule("2026-11-12","bad-zone").departureEnd(10))
        assertNull(TripSchedule("2026-11-12","Asia/Seoul").departureEnd(24))
    }
    @Test fun unresolvedTimeBlocksDepartureButDoesNotEraseChecks(){
        val base=Journey(step=3)
        val checked=base.copy(checked=base.departureItems.map{it.id}.toSet())
        val updated=JourneyReducer.apply(checked,JourneyEvent.Conditions(checked.input.copy(
            schedule=TripSchedule("2026-03-07","America/New_York"),endHour=2)))
        assertEquals(checked.checked,updated.checked)
        assertFalse(updated.canDepart)
        assertEquals(3,JourneyReducer.apply(updated,JourneyEvent.Next).step)
    }
}
