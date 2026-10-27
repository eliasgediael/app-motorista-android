package br.app.motorista.domain.vehicle

import br.app.motorista.domain.calc.MetricResult
import br.app.motorista.domain.calc.OfferCalculator
import br.app.motorista.domain.calc.OfferInput
import br.app.motorista.domain.calc.UnavailableReason
import br.app.motorista.domain.criteria.DistanceTimeBasis
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleCostTest {
    // Valores só para testar aritmética; não são sugestões de custo.
    private fun fuel(price: String?, kmPerL: String?) = CostComponent("f", "Combustível", CostKind.Fuel, price?.let { BigDecimal(it) }, kmPerL?.let { BigDecimal(it) })
    private fun perKm(v: String?) = CostComponent("m", "Direto", CostKind.PerKm, v?.let { BigDecimal(it) }, null)
    private fun monthly(v: String?, km: String?) = CostComponent("x", "Fixo", CostKind.Monthly, v?.let { BigDecimal(it) }, km?.let { BigDecimal(it) })

    @Test
    fun sumsComponentsPerKm() {
        val r = CostModel.costPerKm(listOf(fuel("6", "12"), perKm("0.25"), monthly("1000", "4000"))) as CostResult.Valid
        // 6/12 = 0,5 ; 0,25 ; 1000/4000 = 0,25  -> 1,00
        assertEquals(0, BigDecimal("1.00").compareTo(r.perKm))
        assertEquals(3, r.parts.size)
    }

    @Test
    fun noComponentOrMissingValueIsIncompleteNeverZero() {
        assertTrue(CostModel.costPerKm(emptyList()) is CostResult.Incomplete)
        assertTrue(CostModel.costPerKm(listOf(fuel("6", null))) is CostResult.Incomplete)
        assertTrue(CostModel.costPerKm(listOf(fuel("6", "12"), perKm(null))) is CostResult.Incomplete)
    }

    @Test
    fun zeroDenominatorIsInvalid() {
        assertTrue(CostModel.costPerKm(listOf(fuel("6", "0"))) is CostResult.Incomplete)
        assertTrue(CostModel.costPerKm(listOf(monthly("500", "0"))) is CostResult.Incomplete)
    }

    @Test
    fun disabledComponentsAreIgnored() {
        val off = perKm(null).copy(enabled = false)
        val r = CostModel.costPerKm(listOf(fuel("6", "12"), off)) as CostResult.Valid
        assertEquals(0, BigDecimal("0.5").compareTo(r.perKm))
    }

    @Test
    fun maintenanceStatusNeedsAllInputs() {
        val item = MaintenanceItem("i", "v", "Óleo", intervalKm = 10_000, lastDoneKm = 50_000)
        assertTrue(MaintenanceCalc.status(item, null) is MaintenanceStatus.Unknown)
        val s = MaintenanceCalc.status(item, 58_000) as MaintenanceStatus.Scheduled
        assertEquals(60_000, s.nextDueKm)
        assertEquals(2_000, s.remainingKm)
        assertFalse(s.overdue)
        assertTrue((MaintenanceCalc.status(item, 61_000) as MaintenanceStatus.Scheduled).overdue)
        assertTrue(MaintenanceCalc.status(item.copy(intervalKm = null), 58_000) is MaintenanceStatus.Unknown)
    }

    @Test
    fun odometerOnlyAdvances() {
        assertTrue(MaintenanceCalc.acceptOdometer(null, 10))
        assertTrue(MaintenanceCalc.acceptOdometer(100, 100))
        assertFalse(MaintenanceCalc.acceptOdometer(100, 99))
    }

    @Test
    fun estimatedResultNeedsCostAndBasisAndSubtractsOnce() {
        val input = OfferInput(fareCents = 2480, tripMeters = 12_400, tripSeconds = 1320)
        val basis = DistanceTimeBasis.TripOnly
        assertEquals(MetricResult.Unavailable(UnavailableReason.CostMissing), OfferCalculator.estimatedResult(input, basis, null))
        assertEquals(MetricResult.Unavailable(UnavailableReason.BasisMissing), OfferCalculator.estimatedResult(input, null, BigDecimal("1")))
        // 24,80 - 1,00 * 12,4 = 12,40
        val v = OfferCalculator.estimatedResult(input, basis, BigDecimal("1.00")) as MetricResult.Value
        assertEquals(0, BigDecimal("12.40").compareTo(v.value))
        // custo alto torna o resultado negativo (não é truncado em zero)
        val neg = OfferCalculator.estimatedResult(input, basis, BigDecimal("3")) as MetricResult.Value
        assertTrue(neg.value.signum() < 0)
    }
}
