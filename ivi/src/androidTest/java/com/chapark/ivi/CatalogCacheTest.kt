package com.chapark.ivi

import com.chapark.ivi.prototype.CatalogCache
import com.chapark.app.domain.Condition
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class CatalogCacheTest {
    private fun payload()="""{"schemaVersion":1,"ruleVersion":"mvp-v02-rules-3","version":"test-1","places":[{"id":"test","name":"장소","latitude":37.0,"longitude":127.0,"coordinateKind":"MAP_POINT","address":"주소","source":"https://example.org/place","scenery":"SEA","parking":"ALLOWED","overnight":"ALLOWED","nightToilet":"UNKNOWN","cooking":"UNKNOWN","fire":"UNKNOWN","pets":"UNKNOWN","facts":[{"label":"주차","detail":"오래된 자료","url":"https://example.org/place","checkedOn":"2000-01-01","condition":"ALLOWED"}]}]}"""
    @Test fun staleOrMissingEvidenceNeverPromotesConditions(){
        val value=CatalogCache.parse(payload())
        assertEquals(Condition.UNKNOWN,value.evidence.getValue("test").parking)
        assertEquals(Condition.UNKNOWN,value.places.single().place.overnight)
    }
    @Test fun incompatibleRulesInvalidCoordinatesAndDuplicateIdsAreRejected(){
        fun rejected(raw:String){assertTrue(runCatching{CatalogCache.parse(raw)}.isFailure)}
        rejected(payload().replace("mvp-v02-rules-3","future"))
        rejected(payload().replace("37.0","190.0"))
        val root=JSONObject(payload());val places=root.getJSONArray("places");places.put(places.getJSONObject(0))
        rejected(root.toString())
    }
}
