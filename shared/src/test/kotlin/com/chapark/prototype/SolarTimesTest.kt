package com.chapark.prototype
import com.chapark.app.domain.SolarTimes
import java.time.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
class SolarTimesTest {
    @Test fun seoulMatchesIndependentUsnoReferenceWithinFiveMinutes(){
        val zone=ZoneId.of("Asia/Seoul")
        val date=LocalDate.parse("2026-11-13")
        val day=SolarTimes.calculate(date,zone,37.57,126.87)
        // USNO 4.0.1 one-day data, fetched 2026-11-13; evidence in docs/verification/v211-solar.
        val rise=date.atTime(7,10).atZone(zone).toInstant()
        val set=date.atTime(17,23).atZone(zone).toInstant()
        assertTrue(abs(Duration.between(rise,day.sunrise!!.instant).seconds)<=300)
        assertTrue(abs(Duration.between(set,day.sunset!!.instant).seconds)<=300)
        assertTrue(day.sunrise!!.azimuthDegrees in 90.0..180.0)
        assertTrue(day.sunset!!.azimuthDegrees in 180.0..270.0)
    }
    @Test fun polarNightAndDateLineAreHandled(){
        val polar=SolarTimes.calculate(LocalDate.parse("2026-12-21"),ZoneId.of("Arctic/Longyearbyen"),78.22,15.65)
        assertNull(polar.sunrise);assertNull(polar.sunset)
        val date=LocalDate.parse("2026-06-21");val zone=ZoneId.of("Pacific/Kiritimati")
        val day=SolarTimes.calculate(date,zone,1.87,-157.43)
        assertEquals(date,day.sunrise!!.instant.atZone(zone).toLocalDate())
        assertEquals(date,day.sunset!!.instant.atZone(zone).toLocalDate())
    }
}
