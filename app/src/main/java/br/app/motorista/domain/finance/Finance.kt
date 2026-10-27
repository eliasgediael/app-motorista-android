package br.app.motorista.domain.finance

import java.time.LocalDate
import java.time.YearMonth

enum class EntryKind(val label: String) { Income("Receita"), Expense("Despesa") }

enum class CategoryKind(val label: String) { Income("Receita"), Expense("Despesa"), Platform("Plataforma") }

/** Categoria ou plataforma definida pelo usuário. Arquivar não apaga lançamentos antigos. */
data class Category(
    val id: String,
    val name: String,
    val kind: CategoryKind,
    val archived: Boolean = false,
)

/** Origem do lançamento, para evitar dupla contagem (viagem, conta fixa e manutenção têm vínculo único). */
enum class TxSource { Manual, Trip, Bill, Maintenance }

/** Dinheiro sempre em centavos. [epochDay] é o dia local do lançamento. */
data class Transaction(
    val id: String,
    val kind: EntryKind,
    val amountCents: Long,
    val epochDay: Long,
    val categoryId: String? = null,
    val platformId: String? = null,
    val vehicleId: String? = null,
    val note: String = "",
    val source: TxSource = TxSource.Manual,
    /** Id do registro de origem (viagem, conta, manutenção). Dois lançamentos nunca compartilham a mesma origem. */
    val sourceRef: String? = null,
)

data class PeriodSummary(val incomeCents: Long, val expenseCents: Long) {
    /** Resultado de CAIXA: receitas confirmadas menos despesas lançadas. Não é lucro nem resultado econômico. */
    val cashCents: Long get() = incomeCents - expenseCents
}

object FinanceCalc {
    fun inRange(txs: List<Transaction>, fromDay: Long, toDay: Long): List<Transaction> =
        txs.filter { it.epochDay in fromDay..toDay }

    fun summary(txs: List<Transaction>): PeriodSummary = PeriodSummary(
        incomeCents = txs.filter { it.kind == EntryKind.Income }.sumOf { it.amountCents },
        expenseCents = txs.filter { it.kind == EntryKind.Expense }.sumOf { it.amountCents },
    )

    fun month(txs: List<Transaction>, ym: YearMonth): List<Transaction> =
        inRange(txs, ym.atDay(1).toEpochDay(), ym.atEndOfMonth().toEpochDay())

    fun day(txs: List<Transaction>, day: LocalDate): List<Transaction> = inRange(txs, day.toEpochDay(), day.toEpochDay())

    /** true se já existe lançamento para esta origem (impede contar a mesma viagem/conta duas vezes). */
    fun hasSource(txs: List<Transaction>, source: TxSource, ref: String): Boolean =
        txs.any { it.source == source && it.sourceRef == ref }

    fun formatCents(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val abs = Math.abs(cents)
        return sign + "R$ " + (abs / 100) + "," + "%02d".format(abs % 100)
    }
}
