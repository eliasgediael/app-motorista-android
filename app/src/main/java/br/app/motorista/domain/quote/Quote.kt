package br.app.motorista.domain.quote

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Orçamento de viagem particular. Todos os preços (por km, por minuto, pedágios) são digitados pelo usuário;
 * campo vazio = não usado nessa linha. Nada é sugerido.
 */
data class QuoteInput(
    val pickupKm: BigDecimal?,
    val tripKm: BigDecimal?,
    val returnKm: BigDecimal?,
    val includePickup: Boolean,
    val includeReturn: Boolean,
    val minutes: BigDecimal?,
    val ratePerKm: BigDecimal?,
    val ratePerMinute: BigDecimal?,
    val tollsCents: Long?,
)

data class QuoteLine(val label: String, val cents: Long)

data class Quote(
    val billableKm: BigDecimal?,
    val lines: List<QuoteLine>,
    val totalCents: Long,
    /** Linhas que não puderam ser calculadas, com o motivo. */
    val notes: List<String>,
)

object QuoteCalc {
    /** Arredonda para centavos (meio para cima) só no fim de cada linha. */
    private fun cents(reais: BigDecimal): Long = reais.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()

    fun billableKm(i: QuoteInput): BigDecimal? {
        val parts = mutableListOf<BigDecimal?>()
        if (i.includePickup) parts += i.pickupKm
        parts += i.tripKm
        if (i.includeReturn) parts += i.returnKm
        if (parts.any { it == null }) return null
        return parts.fold(BigDecimal.ZERO) { a, b -> a + b!! }
    }

    fun compute(i: QuoteInput): Quote {
        val lines = mutableListOf<QuoteLine>()
        val notes = mutableListOf<String>()
        val km = billableKm(i)
        if (i.ratePerKm != null) {
            if (km == null) notes += "Distância incompleta: faltam km das etapas incluídas" else lines += QuoteLine("Distância", cents(km.multiply(i.ratePerKm)))
        }
        if (i.ratePerMinute != null) {
            if (i.minutes == null) notes += "Tempo não informado" else lines += QuoteLine("Tempo", cents(i.minutes.multiply(i.ratePerMinute)))
        }
        if (i.tollsCents != null && i.tollsCents > 0) lines += QuoteLine("Pedágios", i.tollsCents)
        if (lines.isEmpty() && notes.isEmpty()) notes += "Informe ao menos um preço (por km, por minuto) ou pedágios"
        return Quote(km, lines, lines.sumOf { it.cents }, notes)
    }

    /** Custo interno estimado (R$) com o custo por km do modelo validado; null se faltar km ou custo. NUNCA vai ao recibo. */
    fun internalCostCents(i: QuoteInput, costPerKm: BigDecimal?): Long? {
        val km = billableKm(i) ?: return null
        if (costPerKm == null) return null
        return cents(km.multiply(costPerKm))
    }
}
