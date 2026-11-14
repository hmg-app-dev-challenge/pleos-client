package com.chapark.ivi.prototype

import com.chapark.prototype.JourneyRecord
import com.chapark.prototype.PlaceMemory
import com.chapark.app.domain.ReadingStatus
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
            put("readingStatus",r.readingStatus?.name);put("preparationCompleted",r.preparationCompleted);put("preparationRequired",r.preparationRequired)
            r.placeMemory?.let{m->put("placeMemory",JSONObject().put("sceneryTags",JSONArray(m.sceneryTags)).put("informationDates",JSONArray(m.informationDates)).put("hasUndatedInfo",m.hasUndatedInfo))}
        }) }
    }.toString()
    private fun strings(a:JSONArray)=(0 until a.length()).map{a.getString(it)}
    fun decode(raw:String):List<JourneyRecord> {
        val array=JSONArray(raw)
        return (0 until array.length()).map { index->
            val o=array.getJSONObject(index)
            val time=o.getLong("time");require(time>=0)
            JourneyRecord(o.getString("kind"),time,o.getString("origin"),
                if(!o.isNull("subject"))o.getString("subject") else null,
                if(!o.isNull("label"))o.getString("label") else null,
                if(!o.isNull("confirmed"))o.getBoolean("confirmed") else null,
                o.getString("place"),if(!o.isNull("soc"))o.getInt("soc") else null,
                Source.valueOf(o.getString("source")),if(!o.isNull("received"))o.getLong("received") else null,if(!o.isNull("eventAt"))o.getLong("eventAt") else null,
                readingStatus=if(!o.isNull("readingStatus"))runCatching{ReadingStatus.valueOf(o.getString("readingStatus"))}.getOrDefault(ReadingStatus.UNKNOWN) else null,
                preparationCompleted=if(!o.isNull("preparationCompleted"))o.getInt("preparationCompleted") else null,
                preparationRequired=if(!o.isNull("preparationRequired"))o.getInt("preparationRequired") else null,
                placeMemory=o.optJSONObject("placeMemory")?.let{m->PlaceMemory(strings(m.getJSONArray("sceneryTags")),strings(m.getJSONArray("informationDates")),m.getBoolean("hasUndatedInfo"))})
        }
    }
}
