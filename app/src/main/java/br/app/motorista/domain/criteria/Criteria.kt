package br.app.motorista.domain.criteria

import java.math.BigDecimal

/** Métricas com faixa mínimo/bom (telas 03 e 46). */
enum class CriterionMetric(val label: String, val unit: String, val needsBasis: Boolean) {
    RevenuePerKm("R$/km", "R$/km", needsBasis = true),
    RevenuePerHour("R$/hora", "R$/h", needsBasis = true),
    PassengerRating("Nota", "nota", needsBasis = false),
}

/**
 * Bases de distância/tempo oferecidas como escolha. Qual base vale é decisão do usuário
 * (pendente em validacoes-apex.md); nenhuma é pré-selecionada.
 */
enum class DistanceTimeBasis(val label: String) {
    TripOnly("Somente viagem"),
    PickupAndTrip("Embarque + viagem"),
    PickupTripAndReturn("Embarque + viagem + retorno"),
}

/** Rascunho digitado pelo usuário. Texto cru: ausente é "", nunca zero. */
data class CriterionDraft(
    val enabled: Boolean = false,
    val minimum: String = "",
    val good: String = "",
    val basis: DistanceTimeBasis? = null,
) {
    companion object {
        /** Base de partida (embarque + viagem) para quem não quer escolher; pode ser trocada no cadastro. */
        val DEFAULT_BASIS = DistanceTimeBasis.PickupAndTrip

        /** Critério novo: já ligado e com a base de partida. Só vale quando mínimo e bom forem preenchidos. */
        fun fresh(metric: CriterionMetric) = CriterionDraft(enabled = true, basis = if (metric.needsBasis) DEFAULT_BASIS else null)
    }
}

enum class CriterionIssue {
    MinimumMissing,
    MinimumInvalid,
    GoodMissing,
    GoodInvalid,
    GoodBelowMinimum,
    BasisMissing,
}

/** Resultado da validação de forma. Não decide faixa nem avalia ofertas. */
data class CriterionValidation(
    val minimum: BigDecimal?,
    val good: BigDecimal?,
    val issues: List<CriterionIssue>,
    /** Mínimo igual a "bom": faixa amarela vazia. Tratamento do limite exato ainda pendente. */
    val bandsCollapsed: Boolean,
) {
    val isValid: Boolean get() = issues.isEmpty()
}

object CriterionValidator {
    /** Aceita vírgula ou ponto decimal; rejeita vazio, negativo e não numérico. */
    fun parse(text: String): BigDecimal? {
        val normalized = text.trim().replace(',', '.')
        if (normalized.isEmpty()) return null
        val value = normalized.toBigDecimalOrNull() ?: return null
        return if (value.signum() < 0) null else value
    }

    fun validate(metric: CriterionMetric, draft: CriterionDraft): CriterionValidation {
        val issues = mutableListOf<CriterionIssue>()
        val min = parse(draft.minimum)
        val good = parse(draft.good)

        if (draft.minimum.isBlank()) issues += CriterionIssue.MinimumMissing
        else if (min == null) issues += CriterionIssue.MinimumInvalid

        if (draft.good.isBlank()) issues += CriterionIssue.GoodMissing
        else if (good == null) issues += CriterionIssue.GoodInvalid

        if (min != null && good != null && good < min) issues += CriterionIssue.GoodBelowMinimum
        if (metric.needsBasis && draft.basis == null) issues += CriterionIssue.BasisMissing

        return CriterionValidation(
            minimum = min,
            good = good,
            issues = issues,
            bandsCollapsed = min != null && good != null && good.compareTo(min) == 0,
        )
    }
}
