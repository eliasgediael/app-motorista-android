package br.app.motorista.domain.calc

import br.app.motorista.domain.criteria.DistanceTimeBasis
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferCalculatorTest {
    // Exemplo fictício de ESPECIFICACAO-COMPLETA.md: 24,80 / 12,4 km / 22 min.
    private val sample = OfferInput(fareCents = 2480, tripMeters = 12_400, tripSeconds = 22 * 60)

    private fun shown(r: MetricResult) = Display.decimal((r as MetricResult.Value).value)

    @Test
    fun specExampleTripOnly() {
        val b = DistanceTimeBasis.TripOnly
        assertEquals("2,00", shown(OfferCalculator.revenuePerKm(sample, b)))
        assertEquals("67,64", shown(OfferCalculator.revenuePerHour(sample, b)))
        assertEquals("1,13", shown(OfferCalculator.revenuePerMinute(sample, b)))
    }

    @Test
    fun missingBaseIsUnavailable() {
        val r = OfferCalculator.revenuePerKm(sample, null)
        assertEquals(MetricResult.Unavailable(UnavailableReason.BasisMissing), r)
    }

    @Test
    fun basisWithMissingPickupIsUnavailableNotZero() {
        val r = OfferCalculator.revenuePerKm(sample, DistanceTimeBasis.PickupAndTrip)
        assertEquals(MetricResult.Unavailable(UnavailableReason.DistanceMissing), r)
        val h = OfferCalculator.revenuePerHour(sample, DistanceTimeBasis.PickupAndTrip)
        assertEquals(MetricResult.Unavailable(UnavailableReason.TimeMissing), h)
    }

    @Test
    fun pickupIsAddedToDenominatorOnly() {
        val input = sample.copy(pickupMeters = 3_600, pickupSeconds = 8 * 60)
        // 24,80 / 16 km = 1,55
        assertEquals("1,55", shown(OfferCalculator.revenuePerKm(input, DistanceTimeBasis.PickupAndTrip)))
        // 24,80 / 30 min = 0,83 (0,8266...)
        assertEquals("0,83", shown(OfferCalculator.revenuePerMinute(input, DistanceTimeBasis.PickupAndTrip)))
    }

    @Test
    fun nonPositiveDenominatorIsUnavailable() {
        val input = sample.copy(tripMeters = 0, tripSeconds = 0)
        assertEquals(
            MetricResult.Unavailable(UnavailableReason.DenominatorNotPositive),
            OfferCalculator.revenuePerKm(input, DistanceTimeBasis.TripOnly),
        )
        assertEquals(
            MetricResult.Unavailable(UnavailableReason.DenominatorNotPositive),
            OfferCalculator.revenuePerHour(input, DistanceTimeBasis.TripOnly),
        )
    }

    @Test
    fun missingFareIsUnavailable() {
        val r = OfferCalculator.revenuePerKm(sample.copy(fareCents = null), DistanceTimeBasis.TripOnly)
        assertEquals(MetricResult.Unavailable(UnavailableReason.FareMissing), r)
    }

    @Test
    fun valueKeepsFullPrecisionAndMetadata() {
        val v = OfferCalculator.revenuePerHour(sample, DistanceTimeBasis.TripOnly) as MetricResult.Value
        assertTrue(v.value.scale() > 2)
        assertEquals(DistanceTimeBasis.TripOnly, v.basis)
        assertTrue(v.formula.contains(OfferCalculator.FORMULA_VERSION))
    }

    @Test
    fun ratingPassesThrough() {
        val r = OfferCalculator.passengerRating(OfferInput(passengerRating = BigDecimal("4.92"))) as MetricResult.Value
        assertEquals(0, BigDecimal("4.92").compareTo(r.value))
        assertEquals(MetricResult.Unavailable(UnavailableReason.RatingMissing), OfferCalculator.passengerRating(OfferInput()))
    }

    @Test
    fun parsingUnits() {
        assertEquals(Parsed.Ok(2480L), OfferParsing.cents("24,80"))
        assertEquals(Parsed.Ok(12_400L), OfferParsing.meters("12,4"))
        assertEquals(Parsed.Ok(1320L), OfferParsing.seconds("22"))
        assertEquals(Parsed.Blank, OfferParsing.cents(" "))
        assertEquals(Parsed.Invalid, OfferParsing.cents("24,805"))
        assertEquals(Parsed.Invalid, OfferParsing.meters("-1"))
        assertEquals(Parsed.Invalid, OfferParsing.seconds("1,5"))
        assertEquals(Parsed.Invalid, OfferParsing.cents("1e30"))
    }
}
