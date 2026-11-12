package com.chapark.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Explicit local trip date and zone; missing legacy dates never become today's date. */
data class TripSchedule(val date:String,val zoneId:String) {
    val valid get()=runCatching { LocalDate.parse(date);ZoneId.of(zoneId);true }.getOrDefault(false)
    fun shiftDays(days:Long):TripSchedule?=runCatching{copy(date=LocalDate.parse(date).plusDays(days).toString())}.getOrNull()
    fun departureEnd(endHour:Int):Instant? {
        if(!valid || endHour !in 0..23)return null
        val local=LocalDate.parse(date).plusDays(1).atTime(endHour,0)
        val offsets=ZoneId.of(zoneId).rules.getValidOffsets(local)
        // A nonexistent or repeated DST hour needs an explicit user decision, not silent adjustment.
        return if(offsets.size==1)local.toInstant(offsets.single()) else null
    }
}
