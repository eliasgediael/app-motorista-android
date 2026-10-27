package br.app.motorista.domain.calc

import br.app.motorista.domain.criteria.DistanceTimeBasis
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Oferta digitada. Unidades fixas: dinheiro em centavos, distância em metros, tempo em segundos.
 * Campo ausente é null (desconhecido), nunca zero.
 *
 * [fareCents] é o valor ofertado AO MOTORISTA. O cálculo não desconta comissão de novo nem
 * soma embarque a este valor.
 */
data class OfferInput(
    val fareCents: Long? = null,
    val pickupMeters: Long? = null,
    val tripMeters: Long? = null,
    val returnMeters: Long? = null,
    val pickupSeconds: Long? = null,
    val tripSeconds: Long? = null,
    val returnSeconds: Long? = null,
    val passengerRating: BigDecimal? = null,
)

enum class UnavailableReason(val label: String) {
    FareMissing("valor da oferta ausente"),
    DistanceMissing("distância da base escolhida ausente"),
    TimeMissing("tempo da base escolhida ausente"),
    DenominatorNotPositive("distância ou tempo da base não é positivo"),
    BasisMissing("base de distância/tempo não escolhida"),
    RatingMissing("nota do passageiro ausente"),
    CostMissing("modelo de custo ausente ou não confirmado"),
}

sealed interface MetricResult {
    /** [value] é preservado sem arredondar; arredondar só ao exibir (ver [Display]). */
    data class Value(
        val value: BigDecimal,
        val unit: String,
        val basis: DistanceTimeBasis?,
        val formula: String,
    ) : MetricResult

    data class Unavailable(val reason: UnavailableReason) : MetricResult
}

/**
 * Fórmulas CANDIDATAS (referencias/validacoes-apex.md), ainda não aprovadas como regra final.
 * Cada resultado carrega base e versão da fórmula.
 */
object OfferCalculator {
    const val FORMULA_VERSION = "candidata-0.1"
    private val mc = MathContext.DECIMAL128
    private val HUNDRED = BigDecimal(100)

    fun totalMeters(input: OfferInput, basis: DistanceTimeBasis): Long? =
        sum(basis, input.pickupMeters, input.tripMeters, input.returnMeters)

    fun totalSeconds(input: OfferInput, basis: DistanceTimeBasis): Long? =
        sum(basis, input.pickupSeconds, input.tripSeconds, input.returnSeconds)

    private fun sum(basis: DistanceTimeBasis, pickup: Long?, trip: Long?, ret: Long?): Long? {
        val parts = buildList {
            if (basis != DistanceTimeBasis.TripOnly) add(pickup)
            add(trip)
            if (basis == DistanceTimeBasis.PickupTripAndReturn) add(ret)
        }
        return if (parts.any { it == null }) null else parts.sumOf { it!! }
    }

    private fun fareReais(input: OfferInput): BigDecimal? = input.fareCents?.let { BigDecimal(it).divide(HUNDRED, mc) }

    /** R$/km = valor ao motorista / km da base escolhida. */
    fun revenuePerKm(input: OfferInput, basis: DistanceTimeBasis?): MetricResult {
        if (basis == null) return MetricResult.Unavailable(UnavailableReason.BasisMissing)
        val fare = fareReais(input) ?: return MetricResult.Unavailable(UnavailableReason.FareMissing)
        val meters = totalMeters(input, basis) ?: return MetricResult.Unavailable(UnavailableReason.DistanceMissing)
        if (meters <= 0) return MetricResult.Unavailable(UnavailableReason.DenominatorNotPositive)
        val km = BigDecimal(meters).divide(BigDecimal(1000), mc)
        return MetricResult.Value(fare.divide(km, mc), "R$/km", basis, "valor ao motorista / km ($FORMULA_VERSION)")
    }

    /** R$/h estimada da oferta = valor / horas da base. Não é o ganho/hora online da jornada. */
    fun revenuePerHour(input: OfferInput, basis: DistanceTimeBasis?): MetricResult =
        when (val t = timeBase(input, basis)) {
            is TimeBase.Fail -> MetricResult.Unavailable(t.reason)
            is TimeBase.Ok -> {
                val hours = BigDecimal(t.seconds).divide(BigDecimal(3600), mc)
                MetricResult.Value(t.fare.divide(hours, mc), "R$/h", basis, "valor ao motorista / horas ($FORMULA_VERSION)")
            }
        }

    /** R$/min = valor / minutos da base. */
    fun revenuePerMinute(input: OfferInput, basis: DistanceTimeBasis?): MetricResult =
        when (val t = timeBase(input, basis)) {
            is TimeBase.Fail -> MetricResult.Unavailable(t.reason)
            is TimeBase.Ok -> {
                val minutes = BigDecimal(t.seconds).divide(BigDecimal(60), mc)
                MetricResult.Value(t.fare.divide(minutes, mc), "R$/min", basis, "valor ao motorista / minutos ($FORMULA_VERSION)")
            }
        }

    /**
     * Resultado estimado (R$) = valor ao motorista − custo por km × km da base escolhida.
     * Só existe com modelo de custo validado ([costPerKm] != null). O valor da oferta já é o do motorista:
     * nenhuma comissão é descontada de novo. Pode ser negativo. Não é lucro líquido real.
     */
    fun estimatedResult(input: OfferInput, basis: DistanceTimeBasis?, costPerKm: BigDecimal?): MetricResult {
        if (costPerKm == null) return MetricResult.Unavailable(UnavailableReason.CostMissing)
        if (basis == null) return MetricResult.Unavailable(UnavailableReason.BasisMissing)
        val fare = fareReais(input) ?: return MetricResult.Unavailable(UnavailableReason.FareMissing)
        val meters = totalMeters(input, basis) ?: return MetricResult.Unavailable(UnavailableReason.DistanceMissing)
        if (meters <= 0) return MetricResult.Unavailable(UnavailableReason.DenominatorNotPositive)
        val km = BigDecimal(meters).divide(BigDecimal(1000), mc)
        return MetricResult.Value(fare - costPerKm.multiply(km, mc), "R$", basis, "valor ao motorista − custo/km × km da base ($FORMULA_VERSION)")
    }

    fun passengerRating(input: OfferInput): MetricResult {
        val rating = input.passengerRating ?: return MetricResult.Unavailable(UnavailableReason.RatingMissing)
        return MetricResult.Value(rating, "nota", null, "nota informada, sem conversão")
    }

    private sealed interface TimeBase {
        data class Ok(val fare: BigDecimal, val seconds: Long) : TimeBase
        data class Fail(val reason: UnavailableReason) : TimeBase
    }

    private fun timeBase(input: OfferInput, basis: DistanceTimeBasis?): TimeBase {
        if (basis == null) return TimeBase.Fail(UnavailableReason.BasisMissing)
        val fare = fareReais(input) ?: return TimeBase.Fail(UnavailableReason.FareMissing)
        val secs = totalSeconds(input, basis) ?: return TimeBase.Fail(UnavailableReason.TimeMissing)
        if (secs <= 0) return TimeBase.Fail(UnavailableReason.DenominatorNotPositive)
        return TimeBase.Ok(fare, secs)
    }
}

object Display {
    /** Arredondamento provisório de EXIBIÇÃO (2 casas, meio para cima). Política final pendente. */
    fun decimal(value: BigDecimal, scale: Int = 2): String =
        value.setScale(scale, RoundingMode.HALF_UP).toPlainString().replace('.', ',')
}
