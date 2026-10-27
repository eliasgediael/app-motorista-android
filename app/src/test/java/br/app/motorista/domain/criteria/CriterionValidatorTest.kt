package br.app.motorista.domain.criteria

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CriterionValidatorTest {
    private val km = CriterionMetric.RevenuePerKm
    private val rating = CriterionMetric.PassengerRating

    @Test
    fun parseAcceptsCommaAndDot() {
        assertEquals(0, BigDecimal("1.5").compareTo(CriterionValidator.parse("1,5")))
        assertEquals(0, BigDecimal("1.5").compareTo(CriterionValidator.parse(" 1.5 ")))
    }

    @Test
    fun parseRejectsBlankNegativeAndText() {
        assertNull(CriterionValidator.parse(""))
        assertNull(CriterionValidator.parse("   "))
        assertNull(CriterionValidator.parse("-1"))
        assertNull(CriterionValidator.parse("abc"))
    }

    @Test
    fun emptyDraftIsMissingNotZero() {
        val v = CriterionValidator.validate(km, CriterionDraft())
        assertFalse(v.isValid)
        assertNull(v.minimum)
        assertNull(v.good)
        assertTrue(CriterionIssue.MinimumMissing in v.issues)
        assertTrue(CriterionIssue.GoodMissing in v.issues)
        assertTrue(CriterionIssue.BasisMissing in v.issues)
    }

    @Test
    fun goodBelowMinimumIsInvalid() {
        val v = CriterionValidator.validate(rating, CriterionDraft(minimum = "5", good = "4"))
        assertEquals(listOf(CriterionIssue.GoodBelowMinimum), v.issues)
    }

    @Test
    fun ratingDoesNotNeedBasis() {
        val v = CriterionValidator.validate(rating, CriterionDraft(minimum = "1", good = "2"))
        assertTrue(v.isValid)
    }

    @Test
    fun kmNeedsBasis() {
        val ok = CriterionDraft(minimum = "1", good = "2", basis = DistanceTimeBasis.TripOnly)
        assertTrue(CriterionValidator.validate(km, ok).isValid)
        assertEquals(listOf(CriterionIssue.BasisMissing), CriterionValidator.validate(km, ok.copy(basis = null)).issues)
    }

    @Test
    fun equalMinimumAndGoodIsValidButCollapsed() {
        val v = CriterionValidator.validate(rating, CriterionDraft(minimum = "2", good = "2,0"))
        assertTrue(v.isValid)
        assertTrue(v.bandsCollapsed)
    }
}
