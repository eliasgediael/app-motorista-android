package br.app.motorista.domain.history

import br.app.motorista.domain.assessment.BoundaryPolicy
import br.app.motorista.domain.assessment.CriterionRule
import br.app.motorista.domain.assessment.EvaluationPolicy
import br.app.motorista.domain.calc.OfferInput
import br.app.motorista.domain.criteria.CriterionMetric
import br.app.motorista.domain.criteria.DistanceTimeBasis
import br.app.motorista.domain.finance.EntryKind
import br.app.motorista.domain.finance.Transaction
import br.app.motorista.domain.finance.TxSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryTest {
    private val trip = TripRecord(
        "t1", 0, 0, "99Pop", "manual", OfferInput(fareCents = 2480), "m", null, "", "abcd", "regras",
    )

    @Test
    fun confirmingATripCreatesOneIncomeOnly() {
        var n = 0
        val tx = TripLogic.confirmIncome(trip, 2480, emptyList(), "cat", "plat", { "id" + n++ }, 100)!!
        assertEquals(EntryKind.Income, tx.kind)
        assertEquals(TxSource.Trip, tx.source)
        assertEquals("t1", tx.sourceRef)
        assertNull(TripLogic.confirmIncome(trip, 2480, listOf(tx), "cat", "plat", { "x" }, 100))
    }

    @Test
    fun zeroOrNegativeAmountDoesNotCreateIncome() {
        assertNull(TripLogic.confirmIncome(trip, 0, emptyList(), null, null, { "x" }, 0))
        assertNull(TripLogic.confirmIncome(trip, -5, emptyList(), null, null, { "x" }, 0))
    }

    @Test
    fun ruleVersionIdIsStableAndChangesWithRules() {
        val rule = CriterionRule(CriterionMetric.RevenuePerKm, BigDecimal("1.5"), BigDecimal("2.5"), DistanceTimeBasis.TripOnly)
        val pol = EvaluationPolicy(BoundaryPolicy.MinimumIsAttention)
        val a = RuleVersion.summary(listOf(rule), pol)
        assertEquals(RuleVersion.id(a), RuleVersion.id(RuleVersion.summary(listOf(rule), pol)))
        val b = RuleVersion.summary(listOf(rule.copy(minimum = BigDecimal("1.6"))), pol)
        assertNotEquals(RuleVersion.id(a), RuleVersion.id(b))
        assertEquals("Sem critérios ativos", RuleVersion.summary(emptyList(), pol).substringBefore(" |"))
    }

    @Test
    fun bucketsGroupByWeekdayAndHourInTheGivenZone() {
        val utc = ZoneOffset.UTC
        // 2026-10-26 é segunda-feira
        val monday9 = LocalDate.of(2026, 10, 26).atTime(9, 30).toInstant(utc).toEpochMilli()
        val monday9b = LocalDate.of(2026, 10, 26).atTime(9, 5).toInstant(utc).toEpochMilli()
        val tuesday20 = LocalDate.of(2026, 10, 27).atTime(20, 0).toInstant(utc).toEpochMilli()
        val items = listOf(monday9 to 1000L, monday9b to 500L, tuesday20 to 3000L)
        val days = TimeBuckets.byWeekday(items, utc)
        assertEquals("terça", days[0].key)
        assertEquals(3000, days[0].incomeCents)
        assertEquals(2, days.first { it.key == "segunda" }.count)
        val hours = TimeBuckets.byHour(items, utc)
        assertEquals(1500, hours.first { it.key == "09h" }.incomeCents)
    }

    @Test
    fun reportSeparatesOnlineTimeFromRevenueAndRefusesWithoutJourney() {
        val zone: ZoneId = ZoneOffset.UTC
        val day = LocalDate.of(2026, 10, 26)
        val txs = listOf(Transaction("1", EntryKind.Income, 8_000, day.toEpochDay()), Transaction("2", EntryKind.Expense, 1_000, day.toEpochDay()))
        val none = ReportCalc.period(txs, emptyList(), emptyList(), day.toEpochDay(), day.toEpochDay(), zone)
        assertEquals(8_000, none.incomeCents)
        assertNull(none.incomePerOnlineHourCents)
        val start = day.atTime(8, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val j = JourneyRecord("j", start, start + 4 * 3_600_000, 4 * 3_600_000, 600_000, 1)
        val withJourney = ReportCalc.period(txs, emptyList(), listOf(j), day.toEpochDay(), day.toEpochDay(), zone)
        assertEquals(2_000L, withJourney.incomePerOnlineHourCents)
        assertNotNull(withJourney.pausedMs)
    }
}
