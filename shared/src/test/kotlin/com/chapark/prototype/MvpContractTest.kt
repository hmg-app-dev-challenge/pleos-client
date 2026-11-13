package com.chapark.prototype

import com.chapark.app.domain.*
import com.chapark.app.data.DemoPlaces
import org.junit.Assert.*
import org.junit.Test

class MvpContractTest {
    @Test fun planningTabsPreserveDraftAndCannotChangeOtherStagesOrDrivingState(){
        val draft=Journey(step=1,checked=setOf("water"),input=TripInput(people=3))
        val places=JourneyReducer.apply(draft,JourneyEvent.PlanningTab(1))
        assertEquals(draft.copy(step=2),places)
        assertEquals(draft,JourneyReducer.apply(places,JourneyEvent.PlanningTab(0)))
        assertEquals(draft,JourneyReducer.apply(draft,JourneyEvent.PlanningTab(2)))
        for(s in listOf(draft.copy(restricted=true),draft.copy(step=3),draft.copy(step=4)))assertEquals(s,JourneyReducer.apply(s,JourneyEvent.PlanningTab(1)))
    }
    @Test fun cookingModesDoNotConflateSimpleMealsWithCookingPermission() {
        val restricted=DemoPlaces.all[1]
        fun rules(mode:CookingPlan)=PlanningRules.evaluate(restricted,TripInput(cookingPlan=mode),SocReading(80.0,ReadingStatus.AVAILABLE,Source.EMULATOR))
        assertFalse(rules(CookingPlan.NONE).any{it.id in setOf("simple-meal","meal-ready","cooking-gear")})
        assertTrue(rules(CookingPlan.SIMPLE).any{it.id=="simple-meal"})
        assertFalse(rules(CookingPlan.SIMPLE).any{it.id in setOf("cooking-gear","meal-ready")})
        assertTrue(rules(CookingPlan.COOKING).any{it.id=="meal-ready"})
        assertFalse(rules(CookingPlan.COOKING).any{it.id=="cooking-gear"})
    }
    @Test fun optionalItemsDoNotBlockButOmittedRequiredItemsNeverCountAsDone() {
        val required=DecisionItem("r",Group.GEAR,"물","기본",quantity=3)
        val optional=DecisionItem("o",Group.GEAR,"개인 물품","선택",required=false)
        assertEquals(PreparationProgress(1,1),preparationProgress(listOf(required,optional),setOf("r"),setOf("o")))
        assertFalse(preparationProgress(listOf(required,optional),setOf("r"),setOf("r")).ready)
        assertEquals(PreparationCategory.PACK,required.category)
    }
    @Test fun omissionIsMutuallyExclusiveRecordedAndCarriedToNextTrip() {
        var state=Journey(step=3)
        state=JourneyJournal.apply(state,JourneyEvent.Check("water"),10)
        state=JourneyJournal.apply(state,JourneyEvent.Omit("water"),20)
        assertFalse("water" in state.checked)
        assertEquals(PreparationStatus.OMITTED,state.preparationStatus("water"))
        assertEquals("PREPARATION_OMITTED",state.records.last().kind)
        assertTrue(JourneyReducer.apply(state,JourneyEvent.NewTrip).reminders.contains(state.decisions.single{it.id=="water"}.title))
        assertEquals(state,JourneyReducer.apply(state,JourneyEvent.Omit("missing")))
        assertEquals(state.copy(restricted=true),JourneyReducer.apply(state.copy(restricted=true),JourneyEvent.Omit("water")))
        val checked=JourneyReducer.apply(state,JourneyEvent.Check("water"))
        assertEquals(PreparationStatus.COMPLETED,checked.preparationStatus("water"))
        assertFalse("water" in checked.omitted)
    }
    @Test fun quantityChangeInvalidatesOmissionButPreservesUnchangedChecksAndHistory() {
        var state=Journey(step=3,checked=setOf("water"))
        state=JourneyJournal.apply(state,JourneyEvent.Omit("bedding-count"),10)
        state=JourneyJournal.apply(state,JourneyEvent.Conditions(state.input.copy(people=3)),20)
        assertTrue("water" in state.checked)
        assertFalse("bedding-count" in state.omitted)
        assertEquals(3,state.decisions.single{it.id=="bedding-count"}.quantity)
        assertEquals("PREPARATION_OMITTED",state.records.single().kind)
    }
    @Test fun fiveDisplayStagesDoNotInventArrivalOrCompletion() {
        assertNull(Journey().stage)
        assertEquals(JourneyStage.PLANNING,Journey(step=1).stage)
        assertEquals(JourneyStage.PLANNING,Journey(step=2).stage)
        assertEquals(JourneyStage.PREPARING,Journey(step=3).stage)
        assertEquals(JourneyStage.MOVING,Journey(step=4,route=RoutePhase.GUIDING).stage)
        assertEquals(JourneyStage.STAYING,Journey(step=4,route=RoutePhase.ARRIVED).stage)
        assertFalse(Journey(step=4,route=RoutePhase.ARRIVED).canSleep)
        assertEquals(JourneyStage.COMPLETED,Journey(step=6).stage)
        assertFalse(Journey(step=6).completed)
    }
    @Test fun legacyCookingMustBeReviewedBeforeDeparture() {
        val base=Journey(step=3,input=TripInput(schedule=TripSchedule("2026-11-14","Asia/Seoul"),cookingNeedsReview=true))
        val ready=base.copy(checked=base.departureItems.map{it.id}.toSet())
        assertFalse(ready.canDepart)
        assertEquals("취사 계획을 다시 선택해 주세요.",ready.departureBlockReason)
        assertTrue(JourneyReducer.apply(ready,JourneyEvent.Conditions(ready.input.copy(cookingNeedsReview=false))).canDepart)
    }
}
