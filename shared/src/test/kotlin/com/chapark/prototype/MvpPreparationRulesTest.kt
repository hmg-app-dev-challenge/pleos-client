package com.chapark.prototype

import com.chapark.app.data.DemoPlaces
import com.chapark.app.domain.*
import org.junit.Assert.*
import org.junit.Test

class MvpPreparationRulesTest {
    private val place=DemoPlaces.all.first()
    private val high=SocReading(80.0,ReadingStatus.AVAILABLE,Source.SDK)

    @Test fun everyCookingPermissionCombinationHasSeparateBasicsAndCorrectCookingItems() {
        for(mode in CookingPlan.entries) for(cooking in Condition.entries) for(fire in Condition.entries) {
            val input=TripInput(people=3,cookingPlan=mode)
            val p=place.copy(cooking=cooking,fire=fire)
            val items=PlanningRules.evaluate(p,input,high)
            val ids=items.map{it.id}.toSet()
            assertTrue(ids.containsAll(setOf("place-recheck","phone-charge","drinking-water","waste-kit")))
            assertEquals(3,items.single{it.id=="sleeping-mat"}.quantity)
            assertEquals(3,items.single{it.id=="bedding-set"}.quantity)
            assertEquals(mode==CookingPlan.SIMPLE,"simple-meal" in ids)
            assertEquals(mode==CookingPlan.COOKING && cooking==Condition.RESTRICTED,"meal-ready" in ids)
            assertEquals(mode==CookingPlan.COOKING && cooking==Condition.UNKNOWN,"cooking-check" in ids)
            for(id in listOf("cooking-food","cooking-cleanup"))assertEquals(mode==CookingPlan.COOKING && cooking==Condition.ALLOWED,id in ids)
            assertEquals(mode==CookingPlan.COOKING && cooking==Condition.ALLOWED && fire==Condition.ALLOWED,"cooking-gear" in ids)
            assertEquals(items.size,ids.size)
            assertEquals(items,PlanningRules.evaluate(p,input,high))
        }
    }

    @Test fun oldCombinedChecksNeedReconfirmationButOmissionsAndHistorySurvive() {
        val record=JourneyRecord("PREPARATION_OMITTED",10,"USER",subject="water",label="생수·쓰레기 봉투",confirmed=true,placeId=place.id)
        val old=Journey(step=4,route=RoutePhase.GUIDING,input=TripInput(people=3),checked=setOf("water","bedding-count","sleeping-bag"),omitted=setOf("water"),records=listOf(record))
        val migrated=migrateCombinedPreparation(old)
        assertEquals(setOf("sleeping-bag"),migrated.checked)
        assertEquals(setOf("생수·쓰레기 봉투"),migrated.forgotten)
        assertTrue(migrated.omitted.isEmpty())
        assertEquals(old.records,migrated.records)
        assertEquals(old.route,migrated.route);assertEquals(old.step,migrated.step)
        assertEquals(migrated,migrateCombinedPreparation(migrated))
    }

    @Test fun peopleChangeOnlyInvalidatesQuantityDependentPreparation() {
        val old=Journey(step=3,input=TripInput(people=2),checked=setOf("sleeping-mat","bedding-set","drinking-water","waste-kit","phone-charge"))
        val next=JourneyReducer.apply(old,JourneyEvent.Conditions(old.input.copy(people=3)))
        assertEquals(setOf("drinking-water","waste-kit","phone-charge"),next.checked)
    }
}
