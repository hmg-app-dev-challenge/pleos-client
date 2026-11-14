package com.chapark.prototype

import com.chapark.app.data.NavigationPlaces
import com.chapark.app.domain.SolarTimes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class NightDisplay { AUTO, STANDARD, NIGHT }
data class NightWindow(val sunset:Instant,val sunrise:Instant) {
    init { require(sunrise>sunset) }
    fun contains(now:Instant)=now>=sunset && now<sunrise
}

/** Presentation only. Never changes the user's theme, journey facts, or vehicle controls. */
object NightDisplayPolicy {
    fun window(s:Journey):NightWindow?=runCatching {
        val schedule=s.input.schedule ?: return null
        val place=NavigationPlaces.find(s.placeId) ?: return null
        val date=LocalDate.parse(schedule.date)
        val zone=ZoneId.of(schedule.zoneId)
        val sunset=SolarTimes.calculate(date,zone,place.latitude,place.longitude).sunset ?: return null
        val sunrise=SolarTimes.calculate(date.plusDays(1),zone,place.latitude,place.longitude).sunrise ?: return null
        NightWindow(sunset.instant,sunrise.instant)
    }.getOrNull()

    fun active(s:Journey,now:Instant,window:NightWindow?=window(s)):Boolean {
        if(s.step!=5 || !s.canSleep || s.restricted || s.completed)return false
        return when(s.nightDisplay) {
            NightDisplay.AUTO->window?.contains(now)==true
            NightDisplay.STANDARD->false
            NightDisplay.NIGHT->true
        }
    }

    fun restore(value:String?,restMode:String):NightDisplay =
        if(value==null && restMode in setOf("SLEEP","STARS"))NightDisplay.NIGHT
        else runCatching{NightDisplay.valueOf(value ?: "AUTO")}.getOrDefault(NightDisplay.AUTO)
}
