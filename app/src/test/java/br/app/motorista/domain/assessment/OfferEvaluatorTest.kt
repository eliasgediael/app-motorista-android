package br.app.motorista.domain.assessment

import br.app.motorista.domain.calc.OfferInput
import br.app.motorista.domain.criteria.CriterionMetric
import br.app.motorista.domain.criteria.DistanceTimeBasis
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferEvaluatorTest {
    private val trip = DistanceTimeBasis.TripOnly
    // Valores só para testar fronteiras; não são preferências reais.
    private val kmRule = CriterionRule(CriterionMetric.RevenuePerKm, BigDecimal("1.5"), BigDecimal("2.5"), trip)
    private val ratingRule = CriterionRule(CriterionMetric.PassengerRating, BigDecimal("4.5"), BigDecimal("4.8"), null)
    private val full = EvaluationPolicy(BoundaryPolicy.MinimumIsAttention, CombinationPolicy.WorstWins, MissingDataPolicy.IgnoreCriterion)
    private val offer = OfferInput(fareCents = 2480, tripMeters = 12_400, tripSeconds = 1320, passengerRating = BigDecimal("4.92"))

    @Test
    fun classifyBoundaries() {
        val b = BoundaryPolicy.MinimumIsAttention
        assertEquals(Band.Bad, OfferEvaluator.classify(BigDecimal("1.49"), kmRule, b))
        assertEquals(Band.Attention, OfferEvaluator.classify(BigDecimal("1.5"), kmRule, b))
        assertEquals(Band.Bad, OfferEvaluator.classify(BigDecimal("1.5"), kmRule, BoundaryPolicy.MinimumIsBad))
        assertEquals(Band.Attention, OfferEvaluator.classify(BigDecimal("2.49"), kmRule, b))
        assertEquals(Band.Good, OfferEvaluator.classify(BigDecimal("2.5"), kmRule, b))
    }

    @Test
    fun noCriteriaYieldsNoAssessment() {
        assertEquals(Assessment.NoCriteria, OfferEvaluator.evaluate(offer, emptyList(), full).assessment)
    }

    @Test
    fun pendingPolicyEmitsNoColorAndNoBand() {
        val ev = OfferEvaluator.evaluate(offer, listOf(kmRule), EvaluationPolicy())
        assertTrue(ev.assessment is Assessment.PolicyPending)
        assertEquals(3, (ev.assessment as Assessment.PolicyPending).missing.size)
        assertNull(ev.outcomes.single().band)
    }

    @Test
    fun worstWins() {
        // km = 2,00 -> amarelo; nota 4,92 -> verde; pior = amarelo
        val ev = OfferEvaluator.evaluate(offer, listOf(kmRule, ratingRule), full)
        assertEquals(Assessment.Colored(Band.Attention), ev.assessment)
    }

    @Test
    fun missingDataPolicy() {
        val noRating = offer.copy(passengerRating = null)
        val ignore = OfferEvaluator.evaluate(noRating, listOf(kmRule, ratingRule), full)
        assertEquals(Assessment.Colored(Band.Attention), ignore.assessment)
        val strict = OfferEvaluator.evaluate(noRating, listOf(kmRule, ratingRule), full.copy(missing = MissingDataPolicy.InsufficientData))
        assertEquals(Assessment.InsufficientData, strict.assessment)
    }

    @Test
    fun allMissingIsInsufficientNotRed() {
        val ev = OfferEvaluator.evaluate(OfferInput(), listOf(kmRule), full)
        assertEquals(Assessment.InsufficientData, ev.assessment)
    }

    @Test
    fun separateOnlyHasNoOverallColor() {
        val ev = OfferEvaluator.evaluate(offer, listOf(kmRule), full.copy(combination = CombinationPolicy.SeparateOnly))
        assertEquals(Assessment.SeparateOnly, ev.assessment)
        assertEquals(Band.Attention, ev.outcomes.single().band)
    }
}
