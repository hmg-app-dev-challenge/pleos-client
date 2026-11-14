package com.chapark.app.data
import com.chapark.app.domain.Condition
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
class PlaceEvidenceTest {
    @Test fun filtersDoNotTreatUnknownConditionsAsConfirmed(){
        val today=LocalDate.parse("2026-11-13")
        assertEquals(listOf("camp-mongsanpo"),PlaceEvidenceCatalog.filter(Scenery.SEA,false,today).map{it.place.id})
        assertTrue(PlaceEvidenceCatalog.filter(null,true,today).isEmpty())
        assertTrue(PlaceEvidenceCatalog.find("camp-mongsanpo").exclusions(NavigationPlaces.find("camp-mongsanpo")!!,today).isNotEmpty())
    }
    @Test fun recommendedMomentNeedsFreshAttributedEvidence(){
        val today=LocalDate.parse("2026-11-14")
        val evidence=Evidence("추천 시간","일몰","https://example.org","2026-11-14",Condition.ALLOWED,"운영 안내")
        val place=PlaceEvidence(Scenery.SEA,listOf(evidence),sceneryTags=setOf(Scenery.SEA,Scenery.FOREST),bestMoments=setOf(BestMoment.SUNSET,BestMoment.STARS))
        assertEquals(listOf(BestMoment.SUNSET),place.confirmedBestMoments(today))
        assertTrue(place.confirmedBestMoments(today.plusDays(31)).isEmpty())
        assertTrue(place.copy(facts=emptyList()).confirmedBestMoments(today).isEmpty())
    }
    @Test fun staleAndFutureEvidenceCannotPassFreshness(){
        val fact=Evidence("test","test","https://example.org","2026-11-13",Condition.ALLOWED)
        assertTrue(fact.fresh(LocalDate.parse("2026-11-13")))
        assertFalse(fact.fresh(LocalDate.parse("2027-01-01")))
        assertFalse(fact.fresh(LocalDate.parse("2026-11-12")))
    }
}
