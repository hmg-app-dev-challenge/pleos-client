package com.chapark.ivi.sdk

/** Receipt times from a monotonic clock. A noisy channel cannot keep other data alive. */
internal class NavigationFreshness(private val needsWaypoint:Boolean) {
    enum class Reading { ROUTE, DESTINATION, WAYPOINT }
    private val received=mutableMapOf<Reading,Long>()
    private val required=if(needsWaypoint)Reading.entries else listOf(Reading.ROUTE,Reading.DESTINATION)

    fun observe(reading:Reading,at:Long) {
        val previous=received[reading]
        if(previous==null || at>previous)received[reading]=at
    }

    fun isFresh(now:Long)=required.all { reading->
        received[reading]?.let { at->now>=at && now-at<MAX_AGE_MS } ?: false
    }

    companion object { const val MAX_AGE_MS=30_000L }
}
