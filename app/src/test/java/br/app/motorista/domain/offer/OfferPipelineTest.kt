package br.app.motorista.domain.offer

import br.app.motorista.domain.assessment.Assessment
import br.app.motorista.domain.assessment.BoundaryPolicy
import br.app.motorista.domain.assessment.CombinationPolicy
import br.app.motorista.domain.assessment.CriterionRule
import br.app.motorista.domain.assessment.EvaluationPolicy
import br.app.motorista.domain.assessment.MissingDataPolicy
import br.app.motorista.domain.calc.OfferInput
import br.app.motorista.domain.criteria.CriterionMetric
import br.app.motorista.domain.criteria.DistanceTimeBasis
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferPipelineTest {
    private val input = OfferInput(fareCents = 2480, tripMeters = 12_400, tripSeconds = 1320)
    private val rule = CriterionRule(CriterionMetric.RevenuePerKm, BigDecimal("1.5"), BigDecimal("2.5"), DistanceTimeBasis.TripOnly)
    private val policy = EvaluationPolicy(BoundaryPolicy.MinimumIsAttention, CombinationPolicy.WorstWins, MissingDataPolicy.IgnoreCriterion)

    private fun snap(id: String, p: Platform, at: Long) = OfferSnapshot(id, p, OfferOrigin.Manual, at, input)

    @Test
    fun emptyStoreIsNoOffer() {
        val r = OfferPipeline.analyze(OfferStore(), Platform.P99, 0, listOf(rule), policy)
        assertEquals(Availability.NoOffer(Platform.P99), r)
    }

    @Test
    fun platformsDoNotMix() {
        val store = OfferStore()
        store.accept(snap("a", Platform.P99, 1000))
        assertEquals(Availability.NoOffer(Platform.Uber), OfferPipeline.analyze(store, Platform.Uber, 1000, listOf(rule), policy))
        assertTrue(OfferPipeline.analyze(store, Platform.P99, 1000, listOf(rule), policy) is Availability.Ready)
    }

    @Test
    fun staleOfferIsNeverReady() {
        val store = OfferStore(maxAgeMs = 1000)
        store.accept(snap("a", Platform.P99, 0))
        assertTrue(OfferPipeline.analyze(store, Platform.P99, 1000, listOf(rule), policy) is Availability.Ready)
        assertTrue(OfferPipeline.analyze(store, Platform.P99, 1001, listOf(rule), policy) is Availability.Stale)
    }

    @Test
    fun newerOfferReplacesOlderAndOldOrRepeatedIsIgnored() {
        val store = OfferStore()
        assertTrue(store.accept(snap("a", Platform.P99, 100)))
        assertTrue(store.accept(snap("b", Platform.P99, 200)))
        assertFalse(store.accept(snap("c", Platform.P99, 150)))
        assertFalse(store.accept(snap("b", Platform.P99, 300)))
        val r = OfferPipeline.analyze(store, Platform.P99, 250, listOf(rule), policy) as Availability.Ready
        assertEquals("b", r.snapshot.id)
    }

    @Test
    fun clockGoingBackwardsIsStale() {
        val store = OfferStore()
        store.accept(snap("a", Platform.Uber, 5000))
        assertTrue(OfferPipeline.analyze(store, Platform.Uber, 4000, listOf(rule), policy) is Availability.Stale)
    }

    @Test
    fun invalidateRemovesOnlyThatPlatform() {
        val store = OfferStore()
        store.accept(snap("a", Platform.P99, 0))
        store.accept(snap("b", Platform.Uber, 0))
        store.invalidate(Platform.P99)
        assertEquals(Slot.Empty, store.slot(Platform.P99, 0))
        assertTrue(store.slot(Platform.Uber, 0) is Slot.Fresh)
    }

    @Test
    fun readyCarriesEvaluationFromSameOffer() {
        val store = OfferStore()
        store.accept(snap("a", Platform.P99, 0))
        val r = OfferPipeline.analyze(store, Platform.P99, 10, listOf(rule), policy) as Availability.Ready
        // 2,00 R$/km entre 1,5 e 2,5 -> amarelo
        assertTrue(r.evaluation.assessment is Assessment.Colored)
    }

    @Test
    fun unvalidatedParsersRefuseToGuess() {
        val screen = ScreenSnapshot("pkg.qualquer", 0, listOf(TextNode("24,80")))
        Platform.entries.forEach {
            assertTrue(UnvalidatedParser(it).parse(screen) is ParseOutcome.Unsupported)
        }
    }
}
