package br.app.motorista.domain.history

import br.app.motorista.domain.assessment.CriterionRule
import br.app.motorista.domain.assessment.EvaluationPolicy
import br.app.motorista.domain.calc.OfferInput
import br.app.motorista.domain.finance.EntryKind
import br.app.motorista.domain.finance.Transaction
import br.app.motorista.domain.finance.TxSource
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId

enum class TripStatus(val label: String) {
    NotConfirmed("Não confirmada"),
    Done("Realizada"),
    NotDone("Não realizada"),
}

/**
 * Oferta analisada. Só vira receita quando o usuário confirma que a viagem foi realizada.
 * Guarda a versão das regras usada, para o histórico não mudar quando as regras mudarem depois.
 * Não guarda nome, endereço nem nota de passageiro além do número digitado.
 */
data class TripRecord(
    val id: String,
    val atMs: Long,
    val epochDay: Long,
    val platform: String,
    val origin: String,
    val input: OfferInput,
    val metricsText: String,
    val bandName: String?,
    val note: String,
    val ruleVersionId: String,
    val ruleSummary: String,
    val status: TripStatus = TripStatus.NotConfirmed,
    val txId: String? = null,
)

data class JourneyRecord(
    val id: String,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val workedMs: Long,
    val pausedMs: Long,
    val pauseCount: Int,
)

/** Perfil de trabalho: critérios, política e estilo do cartão guardados juntos (JSON). */
data class Profile(val id: String, val name: String, val json: String)

object RuleVersion {
    /** Resumo legível das regras ativas e da política usadas em uma avaliação. */
    fun summary(rules: List<CriterionRule>, policy: EvaluationPolicy): String {
        val parts = rules.map { r ->
            r.metric.label + " mín " + r.minimum.toPlainString() + " bom " + r.good.toPlainString() + (r.basis?.let { " base " + it.label } ?: "")
        }
        val pol = listOf(
            policy.boundary?.label ?: "limite exato: pendente",
            policy.combination?.label ?: "combinação: pendente",
            policy.missing?.label ?: "dado ausente: pendente",
        )
        return (if (parts.isEmpty()) "Sem critérios ativos" else parts.joinToString("; ")) + " | " + pol.joinToString("; ")
    }

    /** Identificador curto e estável do resumo: mesmas regras geram o mesmo id. */
    fun id(summary: String): String =
        MessageDigest.getInstance("SHA-256").digest(summary.toByteArray(Charsets.UTF_8)).take(4).joinToString("") { "%02x".format(it) }
}

object TripLogic {
    /** Receita a registrar ao confirmar a viagem, ou null se já existe uma para ela (sem dupla contagem). */
    fun confirmIncome(
        trip: TripRecord,
        amountCents: Long,
        existing: List<Transaction>,
        categoryId: String?,
        platformId: String?,
        newId: () -> String,
        nowDay: Long,
    ): Transaction? {
        if (amountCents <= 0) return null
        if (existing.any { it.source == TxSource.Trip && it.sourceRef == trip.id }) return null
        return Transaction(
            id = newId(), kind = EntryKind.Income, amountCents = amountCents, epochDay = nowDay,
            categoryId = categoryId, platformId = platformId, source = TxSource.Trip, sourceRef = trip.id,
            note = "Viagem " + trip.platform,
        )
    }
}

data class Bucket(val key: String, val count: Int, val incomeCents: Long)

/** Agrupamentos de viagens REALIZADAS por dia da semana e hora. Mostra amostra; não afirma ranking estatístico. */
object TimeBuckets {
    private val weekdays = listOf("segunda", "terça", "quarta", "quinta", "sexta", "sábado", "domingo")

    fun byWeekday(items: List<Pair<Long, Long>>, zone: ZoneId): List<Bucket> =
        group(items) { weekdays[Instant.ofEpochMilli(it).atZone(zone).dayOfWeek.value - 1] }
            .sortedWith(compareByDescending<Bucket> { it.incomeCents })

    fun byHour(items: List<Pair<Long, Long>>, zone: ZoneId): List<Bucket> =
        group(items) { "%02dh".format(Instant.ofEpochMilli(it).atZone(zone).hour) }
            .sortedWith(compareByDescending<Bucket> { it.incomeCents })

    private fun group(items: List<Pair<Long, Long>>, key: (Long) -> String): List<Bucket> =
        items.groupBy { key(it.first) }.map { (k, v) -> Bucket(k, v.size, v.sumOf { it.second }) }
}

data class PeriodReport(
    val doneTrips: Int,
    val analysedOffers: Int,
    val incomeCents: Long,
    val workedMs: Long,
    val pausedMs: Long,
    /** Receita confirmada ÷ horas de jornada em andamento. null se não há tempo de jornada no período. */
    val incomePerOnlineHourCents: Long?,
)

object ReportCalc {
    fun period(txs: List<Transaction>, trips: List<TripRecord>, journeys: List<JourneyRecord>, fromDay: Long, toDay: Long, zone: ZoneId): PeriodReport {
        val inc = txs.filter { it.kind == EntryKind.Income && it.epochDay in fromDay..toDay }.sumOf { it.amountCents }
        val periodTrips = trips.filter { it.epochDay in fromDay..toDay }
        val js = journeys.filter {
            val d = Instant.ofEpochMilli(it.startedAtMs).atZone(zone).toLocalDate().toEpochDay()
            d in fromDay..toDay
        }
        val worked = js.sumOf { it.workedMs }
        val perHour = if (worked > 0) Math.round(inc.toDouble() * 3_600_000.0 / worked) else null
        return PeriodReport(
            doneTrips = periodTrips.count { it.status == TripStatus.Done },
            analysedOffers = periodTrips.size,
            incomeCents = inc,
            workedMs = worked,
            pausedMs = js.sumOf { it.pausedMs },
            incomePerOnlineHourCents = perHour,
        )
    }
}
