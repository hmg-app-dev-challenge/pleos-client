package com.chapark.ivi.prototype

import com.chapark.prototype.JourneyRecord
import com.chapark.app.domain.Source
import org.json.JSONArray
import org.json.JSONObject

internal object JourneyRecordCodec {
    fun encode(records:List<JourneyRecord>):String=JSONArray().apply {
        records.forEach { r->put(JSONObject().apply {
            put("kind",r.kind);put("time",r.observedAtMillis);put("origin",r.origin)
            put("subject",r.subject);put("label",r.label);put("confirmed",r.confirmed)
            put("place",r.placeId);put("soc",r.soc);put("source",r.socSource.name)
            put("received",r.socReceivedAtMillis);put("eventAt",r.eventAtMillis)
        }) }
    }.toString()
    fun decode(raw:String):List<JourneyRecord> {
        val array=JSONArray(raw)
        return (0 until array.length()).map { index->
            val o=array.getJSONObject(index)
            val time=o.getLong("time");require(time>=0)
            JourneyRecord(o.getString("kind"),time,o.getString("origin"),
                if(o.has("subject"))o.getString("subject") else null,
                if(o.has("label"))o.getString("label") else null,
                if(o.has("confirmed"))o.getBoolean("confirmed") else null,
                o.getString("place"),if(o.has("soc"))o.getInt("soc") else null,
                Source.valueOf(o.getString("source")),if(o.has("received"))o.getLong("received") else null,if(o.has("eventAt"))o.getLong("eventAt") else null)
        }
    }
}
