package br.app.motorista.domain.calc

import java.math.BigDecimal

/** Conversão de texto digitado para unidades-base. Vazio = ausente (null), inválido = null com erro. */
sealed interface Parsed<out T> {
    data object Blank : Parsed<Nothing>
    data class Ok<T>(val value: T) : Parsed<T>
    data object Invalid : Parsed<Nothing>
}

object OfferParsing {
    private fun decimal(text: String): Parsed<BigDecimal> {
        val t = text.trim().replace(',', '.')
        if (t.isEmpty()) return Parsed.Blank
        val v = t.toBigDecimalOrNull() ?: return Parsed.Invalid
        return if (v.signum() < 0) Parsed.Invalid else Parsed.Ok(v)
    }

    /** R$ -> centavos. Aceita no máximo 2 casas decimais (sem arredondar entrada). */
    fun cents(text: String): Parsed<Long> = scaled(text, 2)

    /** km -> metros. Máximo 3 casas decimais. */
    fun meters(text: String): Parsed<Long> = scaled(text, 3)

    private fun scaled(text: String, places: Int): Parsed<Long> =
        when (val d = decimal(text)) {
            Parsed.Blank -> Parsed.Blank
            Parsed.Invalid -> Parsed.Invalid
            is Parsed.Ok -> {
                if (d.value.stripTrailingZeros().scale() > places) {
                    Parsed.Invalid
                } else {
                    try {
                        Parsed.Ok(d.value.movePointRight(places).longValueExact())
                    } catch (e: ArithmeticException) {
                        Parsed.Invalid
                    }
                }
            }
        }

    /** Minutos inteiros -> segundos. */
    fun seconds(text: String): Parsed<Long> {
        val t = text.trim()
        if (t.isEmpty()) return Parsed.Blank
        val m = t.toLongOrNull() ?: return Parsed.Invalid
        return if (m < 0) Parsed.Invalid else Parsed.Ok(m * 60)
    }

    fun rating(text: String): Parsed<BigDecimal> = decimal(text)
}
