package br.app.motorista.domain.finance

import java.time.LocalDate
import java.time.YearMonth

// ---------- Contas fixas ----------

enum class Recurrence(val label: String) { Monthly("Mensal"), Once("Única") }

data class Bill(
    val id: String,
    val name: String,
    val amountCents: Long,
    /** Mensal: dia do mês (1 a 31; meses curtos usam o último dia). Única: ignorado. */
    val dueDay: Int,
    val recurrence: Recurrence,
    /** Data de vencimento da conta única. */
    val onceEpochDay: Long? = null,
    val categoryId: String? = null,
    val active: Boolean = true,
)

/** Baixa de uma conta em um mês. Uma conta só pode ter UMA baixa por mês (evita duplicar a despesa). */
data class BillPayment(val id: String, val billId: String, val yearMonth: String, val txId: String, val epochDay: Long)

sealed interface BillStatus {
    data object Paid : BillStatus
    data class Overdue(val daysLate: Long) : BillStatus
    data class DueIn(val days: Long) : BillStatus
}

object BillCalc {
    fun dueDate(bill: Bill, ym: YearMonth): LocalDate? = when (bill.recurrence) {
        Recurrence.Monthly -> ym.atDay(minOf(bill.dueDay.coerceIn(1, 31), ym.lengthOfMonth()))
        Recurrence.Once -> bill.onceEpochDay?.let { LocalDate.ofEpochDay(it) }?.takeIf { YearMonth.from(it) == ym }
    }

    fun paymentFor(bill: Bill, ym: YearMonth, payments: List<BillPayment>): BillPayment? =
        payments.firstOrNull { it.billId == bill.id && it.yearMonth == ym.toString() }

    fun status(bill: Bill, ym: YearMonth, payments: List<BillPayment>, today: LocalDate): BillStatus? {
        val due = dueDate(bill, ym) ?: return null
        if (paymentFor(bill, ym, payments) != null) return BillStatus.Paid
        val diff = due.toEpochDay() - today.toEpochDay()
        return if (diff < 0) BillStatus.Overdue(-diff) else BillStatus.DueIn(diff)
    }

    /** Despesa e baixa para registrar, ou null se a conta já foi baixada neste mês. */
    fun pay(bill: Bill, ym: YearMonth, payments: List<BillPayment>, today: LocalDate, newId: () -> String): Pair<Transaction, BillPayment>? {
        if (!bill.active || paymentFor(bill, ym, payments) != null || dueDate(bill, ym) == null) return null
        val txId = newId()
        val tx = Transaction(
            id = txId,
            kind = EntryKind.Expense,
            amountCents = bill.amountCents,
            epochDay = today.toEpochDay(),
            categoryId = bill.categoryId,
            note = bill.name,
            source = TxSource.Bill,
            sourceRef = bill.id + "@" + ym,
        )
        return tx to BillPayment(newId(), bill.id, ym.toString(), txId, today.toEpochDay())
    }
}

// ---------- Reserva de manutenção ----------

/** Provisão: valor que você pretende separar (não é dinheiro movido). Transferência: dinheiro realmente separado. Gasto: saída da reserva. */
enum class ReserveKind(val label: String) { Provision("Provisão"), Transfer("Transferência"), Spend("Gasto da reserva") }

data class ReserveEntry(
    val id: String,
    val kind: ReserveKind,
    val amountCents: Long,
    val epochDay: Long,
    val note: String = "",
    /** Gasto: id do lançamento de despesa que o representa (a despesa é contada uma vez, por ele). */
    val txId: String? = null,
)

data class ReserveSummary(val provisionedCents: Long, val transferredCents: Long, val spentCents: Long) {
    /** Saldo real da reserva: transferido menos gasto. Provisão não entra. */
    val balanceCents: Long get() = transferredCents - spentCents

    /** Provisão ainda não transferida (nunca negativa). */
    val pendingTransferCents: Long get() = maxOf(0, provisionedCents - transferredCents)
}

object ReserveCalc {
    fun summary(entries: List<ReserveEntry>): ReserveSummary = ReserveSummary(
        provisionedCents = entries.filter { it.kind == ReserveKind.Provision }.sumOf { it.amountCents },
        transferredCents = entries.filter { it.kind == ReserveKind.Transfer }.sumOf { it.amountCents },
        spentCents = entries.filter { it.kind == ReserveKind.Spend }.sumOf { it.amountCents },
    )

    /** Gasto acima do saldo é recusado (a reserva não fica negativa). */
    fun canSpend(entries: List<ReserveEntry>, cents: Long): Boolean = cents > 0 && cents <= summary(entries).balanceCents
}

// ---------- Metas e fechamento do dia ----------

enum class GoalKind(val label: String) { Income("Receita confirmada"), CashResult("Resultado de caixa") }

/** Meta de um mês (id = "aaaa-mm"). [workDays] é o número de dias de trabalho que o usuário planeja no mês. */
data class Goal(val id: String, val kind: GoalKind, val targetCents: Long, val workDays: Int)

data class GoalProgress(
    val earnedCents: Long,
    val remainingCents: Long,
    val workedDays: Int,
    val remainingWorkDays: Int,
    /** null quando não dá para calcular (sem dias restantes) ou a meta já foi atingida. */
    val neededPerDayCents: Long?,
)

object GoalCalc {
    fun progress(goal: Goal, monthTxs: List<Transaction>, closedDays: Int): GoalProgress {
        val s = FinanceCalc.summary(monthTxs)
        val earned = if (goal.kind == GoalKind.Income) s.incomeCents else s.cashCents
        val remaining = maxOf(0, goal.targetCents - earned)
        val remainingDays = maxOf(0, goal.workDays - closedDays)
        val perDay = if (remaining > 0 && remainingDays > 0) Math.ceil(remaining.toDouble() / remainingDays).toLong() else null
        return GoalProgress(earned, remaining, closedDays, remainingDays, perDay)
    }
}

/** Dia fechado: guarda os totais no momento do fechamento (histórico não muda se um lançamento mudar depois). */
data class DayClose(
    val id: String,
    val epochDay: Long,
    val odometerKm: Long?,
    val vehicleId: String?,
    val incomeCents: Long,
    val expenseCents: Long,
    val note: String = "",
    val closedAtMs: Long = 0,
)

/** Registro simples chave/valor (ex.: veículo ativo, modelo de custo validado). */
data class Setting(val id: String, val value: String)
