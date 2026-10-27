package br.app.motorista.domain.finance

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceTest {
    private fun tx(kind: EntryKind, cents: Long, day: LocalDate, source: TxSource = TxSource.Manual, ref: String? = null) =
        Transaction("id-" + cents + day, kind, cents, day.toEpochDay(), source = source, sourceRef = ref)

    @Test
    fun summarySeparatesIncomeAndExpenseAndComputesCash() {
        val d = LocalDate.of(2026, 10, 5)
        val s = FinanceCalc.summary(listOf(tx(EntryKind.Income, 10_000, d), tx(EntryKind.Income, 2_550, d), tx(EntryKind.Expense, 4_000, d)))
        assertEquals(12_550, s.incomeCents)
        assertEquals(4_000, s.expenseCents)
        assertEquals(8_550, s.cashCents)
    }

    @Test
    fun monthFilterIncludesFirstAndLastDayOnly() {
        val txs = listOf(
            tx(EntryKind.Income, 1, LocalDate.of(2026, 9, 30)),
            tx(EntryKind.Income, 2, LocalDate.of(2026, 10, 1)),
            tx(EntryKind.Income, 3, LocalDate.of(2026, 10, 31)),
            tx(EntryKind.Income, 4, LocalDate.of(2026, 11, 1)),
        )
        assertEquals(listOf(2L, 3L), FinanceCalc.month(txs, YearMonth.of(2026, 10)).map { it.amountCents })
    }

    @Test
    fun sourceGuardPreventsDoubleCounting() {
        val d = LocalDate.of(2026, 10, 5)
        val txs = listOf(tx(EntryKind.Income, 2480, d, TxSource.Trip, "trip-1"))
        assertTrue(FinanceCalc.hasSource(txs, TxSource.Trip, "trip-1"))
        assertFalse(FinanceCalc.hasSource(txs, TxSource.Trip, "trip-2"))
        assertFalse(FinanceCalc.hasSource(txs, TxSource.Bill, "trip-1"))
    }

    @Test
    fun formatsCentsInReais() {
        assertEquals("R$ 24,80", FinanceCalc.formatCents(2480))
        assertEquals("R$ 0,05", FinanceCalc.formatCents(5))
        assertEquals("-R$ 1,00", FinanceCalc.formatCents(-100))
    }

    @Test
    fun dateTextIsStrict() {
        assertEquals(LocalDate.of(2026, 10, 27), DateText.parse("27/10/2026"))
        assertNull(DateText.parse("31/02/2026"))
        assertNull(DateText.parse("2026-10-27"))
        assertNull(DateText.parse(""))
        assertEquals("27/10/2026", DateText.format(LocalDate.of(2026, 10, 27)))
    }
}
