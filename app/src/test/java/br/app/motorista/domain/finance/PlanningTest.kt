package br.app.motorista.domain.finance

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanningTest {
    private val ids = generateSequence(1) { it + 1 }.iterator()
    private fun newId() = "id" + ids.next()

    private val monthly = Bill("b1", "Seguro", 15_000, 31, Recurrence.Monthly)

    @Test
    fun monthlyDueDayClampsToShortMonths() {
        assertEquals(LocalDate.of(2026, 2, 28), BillCalc.dueDate(monthly, YearMonth.of(2026, 2)))
        assertEquals(LocalDate.of(2026, 10, 31), BillCalc.dueDate(monthly, YearMonth.of(2026, 10)))
    }

    @Test
    fun onceBillOnlyExistsInItsMonth() {
        val once = Bill("b2", "IPVA", 100_00, 1, Recurrence.Once, onceEpochDay = LocalDate.of(2026, 11, 10).toEpochDay())
        assertNull(BillCalc.dueDate(once, YearMonth.of(2026, 10)))
        assertEquals(LocalDate.of(2026, 11, 10), BillCalc.dueDate(once, YearMonth.of(2026, 11)))
    }

    @Test
    fun payingTwiceInTheSameMonthIsRefused() {
        val ym = YearMonth.of(2026, 10)
        val today = LocalDate.of(2026, 10, 20)
        val (tx, pay) = BillCalc.pay(monthly, ym, emptyList(), today, ::newId)!!
        assertEquals(EntryKind.Expense, tx.kind)
        assertEquals(15_000, tx.amountCents)
        assertEquals(TxSource.Bill, tx.source)
        assertNull(BillCalc.pay(monthly, ym, listOf(pay), today, ::newId))
        assertNotNull(BillCalc.pay(monthly, YearMonth.of(2026, 11), listOf(pay), today, ::newId))
    }

    @Test
    fun statusIsOverdueDueOrPaid() {
        val ym = YearMonth.of(2026, 10)
        val late = BillCalc.status(monthly, ym, emptyList(), LocalDate.of(2026, 11, 2))
        assertEquals(BillStatus.Overdue(2), late)
        assertEquals(BillStatus.DueIn(11), BillCalc.status(monthly, ym, emptyList(), LocalDate.of(2026, 10, 20)))
        val pay = BillPayment("p", "b1", ym.toString(), "t", 0)
        assertEquals(BillStatus.Paid, BillCalc.status(monthly, ym, listOf(pay), LocalDate.of(2026, 10, 20)))
    }

    @Test
    fun reserveBalanceIgnoresProvisionAndNeverGoesNegativeBySpend() {
        val e = listOf(
            ReserveEntry("1", ReserveKind.Provision, 50_000, 0),
            ReserveEntry("2", ReserveKind.Transfer, 20_000, 0),
            ReserveEntry("3", ReserveKind.Spend, 5_000, 0),
        )
        val s = ReserveCalc.summary(e)
        assertEquals(15_000, s.balanceCents)
        assertEquals(30_000, s.pendingTransferCents)
        assertTrue(ReserveCalc.canSpend(e, 15_000))
        assertFalse(ReserveCalc.canSpend(e, 15_001))
        assertFalse(ReserveCalc.canSpend(e, 0))
    }

    @Test
    fun goalProgressUsesConfirmedIncomeAndRemainingWorkDays() {
        val d = LocalDate.of(2026, 10, 5).toEpochDay()
        val txs = listOf(
            Transaction("1", EntryKind.Income, 30_000, d),
            Transaction("2", EntryKind.Expense, 10_000, d),
        )
        val income = GoalCalc.progress(Goal("2026-10", GoalKind.Income, 100_000, 20), txs, 4)
        assertEquals(30_000, income.earnedCents)
        assertEquals(70_000, income.remainingCents)
        assertEquals(16, income.remainingWorkDays)
        assertEquals(4_375L, income.neededPerDayCents)
        val cash = GoalCalc.progress(Goal("2026-10", GoalKind.CashResult, 100_000, 20), txs, 4)
        assertEquals(20_000, cash.earnedCents)
    }

    @Test
    fun goalWithNoDaysLeftOrAlreadyMetHasNoDailyNeed() {
        val txs = listOf(Transaction("1", EntryKind.Income, 200_000, 0))
        assertNull(GoalCalc.progress(Goal("x", GoalKind.Income, 100_000, 20), txs, 3).neededPerDayCents)
        assertNull(GoalCalc.progress(Goal("x", GoalKind.Income, 500_000, 5), txs, 5).neededPerDayCents)
    }
}
