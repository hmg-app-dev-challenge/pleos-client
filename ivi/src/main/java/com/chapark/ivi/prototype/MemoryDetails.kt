package com.chapark.ivi.prototype

import com.chapark.app.data.ChargingPlaces
import com.chapark.prototype.Journey
import org.json.JSONObject

/** Frozen on first archive creation. Missing in old archives means uncollected, not zero. */
data class MemoryDetails(val sleepChecked:Int,val sleepRequired:Int,val exitChecked:Int,val exitRequired:Int,
    val chargerSelected:Boolean,val chargerName:String?,val completed:Boolean) {
    init {
        require(sleepRequired>=0 && sleepChecked in 0..sleepRequired)
        require(exitRequired>=0 && exitChecked in 0..exitRequired)
    }
    fun encode()=JSONObject().put("version",1).put("sleepChecked",sleepChecked).put("sleepRequired",sleepRequired)
        .put("exitChecked",exitChecked).put("exitRequired",exitRequired).put("chargerSelected",chargerSelected)
        .put("chargerName",chargerName).put("completed",completed)
    companion object {
        fun capture(s:Journey)=MemoryDetails(s.sleepChecks.intersect(s.requiredSleepIds).size,s.requiredSleepIds.size,
            s.exitChecks.intersect(s.requiredExitIds).size,s.requiredExitIds.size,s.chargerId!=null,ChargingPlaces.find(s.chargerId)?.name,s.completed)
        fun decode(o:JSONObject?):MemoryDetails? {
            if(o==null || o.optInt("version")!=1)return null
            return MemoryDetails(o.getInt("sleepChecked"),o.getInt("sleepRequired"),o.getInt("exitChecked"),o.getInt("exitRequired"),
                o.getBoolean("chargerSelected"),if(!o.isNull("chargerName"))o.getString("chargerName") else null,o.getBoolean("completed"))
        }
    }
}
