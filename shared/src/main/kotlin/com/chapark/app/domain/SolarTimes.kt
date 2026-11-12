package com.chapark.app.domain

import java.time.*
import kotlin.math.*

data class SolarEvent(val instant:Instant,val azimuthDegrees:Double)
data class SolarDay(val sunrise:SolarEvent?,val sunset:SolarEvent?)
/** NOAA fractional-year approximation, version 1. Apparent horizon: 90.833 degrees.
 * https://gml.noaa.gov/grad/solcalc/solareqns.PDF
 * Times are approximate; atmosphere and terrain are not modelled.
 */
object SolarTimes {
    private fun rad(v:Double)=Math.toRadians(v)
    fun calculate(date:LocalDate,zone:ZoneId,latitude:Double,longitude:Double):SolarDay {
        require(latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0)
        require(date.year in 1900..2100)
        val lat=rad(latitude)
        fun event(day:LocalDate,rising:Boolean):SolarEvent? {
            val gamma=2*PI/day.lengthOfYear()*(day.dayOfYear-1)
            val eq=229.18*(.000075+.001868*cos(gamma)-.032077*sin(gamma)-.014615*cos(2*gamma)-.040849*sin(2*gamma))
            val dec=.006918-.399912*cos(gamma)+.070257*sin(gamma)-.006758*cos(2*gamma)+.000907*sin(2*gamma)-.002697*cos(3*gamma)+.00148*sin(3*gamma)
            val c=cos(rad(90.833))/(cos(lat)*cos(dec))-tan(lat)*tan(dec)
            if(!c.isFinite() || c !in -1.0..1.0)return null
            val ha=acos(c)
            val signed=if(rising)-ha else ha
            val minutes=720-4*longitude-eq+Math.toDegrees(signed)*4
            val instant=day.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds((minutes*60).roundToLong())
            val az=(Math.toDegrees(atan2(sin(signed),cos(signed)*sin(lat)-tan(dec)*cos(lat)))+180+360)%360
            return SolarEvent(instant,az)
        }
        fun select(rising:Boolean)=(-1L..1L).mapNotNull{event(date.plusDays(it),rising)}.firstOrNull{it.instant.atZone(zone).toLocalDate()==date}
        return SolarDay(select(true),select(false))
    }
}
