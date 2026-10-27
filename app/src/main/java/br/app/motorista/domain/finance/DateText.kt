package br.app.motorista.domain.finance

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Datas digitadas como dd/MM/aaaa. Inválida devolve null (nunca assume o dia de hoje). */
object DateText {
    private val fmt = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT)

    fun parse(text: String): LocalDate? = try {
        LocalDate.parse(text.trim(), fmt)
    } catch (e: DateTimeParseException) {
        null
    }

    fun format(date: LocalDate): String = date.format(fmt)

    fun formatEpochDay(day: Long): String = format(LocalDate.ofEpochDay(day))
}
