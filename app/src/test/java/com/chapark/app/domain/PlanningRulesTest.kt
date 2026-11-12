package com.chapark.app.domain

import com.chapark.app.data.DemoPlaces
import org.junit.Assert.*
import org.junit.Test

class PlanningRulesTest {
    private val place = DemoPlaces.all[0]
    private fun soc(n: Double?, status: ReadingStatus = ReadingStatus.AVAILABLE) = SocReading(n, status, Source.DEMO)
    private fun rules(p: Place = place, input: TripInput = TripInput(), reading: SocReading = soc(60.0)) = PlanningRules.evaluate(p, input, reading)
    @Test fun batteryBoundaryIsStrict() {
        assertTrue(rules(reading = soc(34.0)).any { it.id == "charge-review" })
        for (n in listOf(35.0, 36.0)) assertFalse(rules(reading = soc(n)).any { it.id == "charge-review" })
    }
    @Test fun invalidOrStaleSocIsNotZeroOrSafe() {
        for (r in listOf(soc(null), soc(-1.0), soc(101.0), soc(Double.NaN), soc(60.0, ReadingStatus.STALE))) {
            assertTrue(rules(reading = r).any { it.id == "battery-check" })
            assertFalse(rules(reading = r).any { it.id == "charge-review" })
        }
    }
    @Test fun restrictedCookingRemovesGearAndAddsMeal() {
        val result = rules(DemoPlaces.all[1], TripInput(cooking = true))
        assertTrue(result.any { it.id == "meal-ready" })
        assertFalse(result.any { it.id == "cooking-gear" })
        assertTrue(result.any { it.id == "toilet-UNKNOWN" })
    }
    @Test fun noCookingDoesNotAddMealChange() {
        assertFalse(rules(DemoPlaces.all[1]).any { it.id == "meal-ready" })
    }
    @Test fun fireRestrictionDoesNotAssumeElectricCookingAllowed() {
        val result = rules(DemoPlaces.all[2], TripInput(cooking = true))
        assertTrue(result.any { it.id == "fire-check-RESTRICTED" })
        assertFalse(result.any { it.id == "cooking-gear" })
    }
    @Test fun coldWithoutBagAddsWarmthAndPetsRestrictionChangesAction() {
        val result = rules(DemoPlaces.all[2], TripInput(pets = true, sleepingBag = false))
        assertTrue(result.any { it.id == "warmth" })
        assertTrue(result.any { it.id == "pets-RESTRICTED" })
        assertFalse(result.any { it.id == "sleeping-bag" })
        assertEquals(result.size, result.map { it.id }.distinct().size)
    }
    @Test fun checksOnlySurviveUnchangedMeaning() {
        val old = rules(input = TripInput(cooking = true))
        val next = rules(DemoPlaces.all[1], TripInput(cooking = true))
        assertEquals(setOf("water"), retainedChecks(old, next, setOf("water", "cooking-gear")))
        val changed = old.map { if (it.id == "water") it.copy(reason = "changed") else it }
        assertTrue(retainedChecks(old, changed, setOf("water")).isEmpty())
    }
}
