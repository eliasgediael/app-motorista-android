package br.app.motorista.domain.assessment

import br.app.motorista.domain.calc.MetricResult
import br.app.motorista.domain.calc.OfferCalculator
import br.app.motorista.domain.calc.OfferInput
import br.app.motorista.domain.criteria.CriterionMetric
import br.app.motorista.domain.criteria.DistanceTimeBasis
import java.math.BigDecimal

enum class Band { Bad, Attention, Good }

/** Tratamento de valor exatamente igual ao mínimo. Sem escolha do usuário, nenhuma faixa é emitida. */
enum class BoundaryPolicy(val label: String) {
    MinimumIsAttention("No valor mínimo exato: amarelo"),
    MinimumIsBad("No valor mínimo exato: vermelho"),
}

enum class CombinationPolicy(val label: String, val description: String) {
    WorstWins("A pior faixa define a cor", "Entre os critérios ativos, vermelho prevalece sobre amarelo, que prevalece sobre verde."),
    SeparateOnly("Somente métricas separadas", "Cada critério mostra sua faixa; não existe cor geral."),
}

enum class MissingDataPolicy(val label: String) {
    IgnoreCriterion("Ignorar critério sem dado"),
    InsufficientData("Dados insuficientes: sem cor geral"),
}

/** Escolhas do usuário. null = ainda não decidido (pendente). */
data class EvaluationPolicy(
    val boundary: BoundaryPolicy? = null,
    val combination: CombinationPolicy? = null,
    val missing: MissingDataPolicy? = null,
) {
    companion object {
        /** Padrão de partida escolhido pelo usuário ("o resto o aplicativo já tem que ter"); pode ser trocado em Combinar critérios. */
        val standard = EvaluationPolicy(BoundaryPolicy.MinimumIsAttention, CombinationPolicy.WorstWins, MissingDataPolicy.IgnoreCriterion)
    }

    val pending: List<String>
        get() = buildList {
            if (boundary == null) add("limite exato do mínimo")
            if (combination == null) add("combinação de critérios")
            if (missing == null) add("dado ausente")
        }
}

/** Critério ativo e válido; bases e limites vêm do usuário. */
data class CriterionRule(
    val metric: CriterionMetric,
    val minimum: BigDecimal,
    val good: BigDecimal,
    val basis: DistanceTimeBasis?,
)

data class CriterionOutcome(val rule: CriterionRule, val result: MetricResult, val band: Band?)

sealed interface Assessment {
    data object NoCriteria : Assessment
    data class PolicyPending(val missing: List<String>) : Assessment
    data object SeparateOnly : Assessment
    data object InsufficientData : Assessment
    data class Colored(val band: Band) : Assessment
}

data class Evaluation(val outcomes: List<CriterionOutcome>, val assessment: Assessment)

object OfferEvaluator {
    fun metricFor(rule: CriterionRule, input: OfferInput): MetricResult = when (rule.metric) {
        CriterionMetric.RevenuePerKm -> OfferCalculator.revenuePerKm(input, rule.basis)
        CriterionMetric.RevenuePerHour -> OfferCalculator.revenuePerHour(input, rule.basis)
        CriterionMetric.PassengerRating -> OfferCalculator.passengerRating(input)
    }

    /** Faixa por critério: abaixo do mínimo = Bad; a partir de "bom" = Good; no meio = Attention. */
    fun classify(value: BigDecimal, rule: CriterionRule, boundary: BoundaryPolicy): Band = when {
        value >= rule.good -> Band.Good
        value < rule.minimum -> Band.Bad
        value.compareTo(rule.minimum) == 0 -> if (boundary == BoundaryPolicy.MinimumIsBad) Band.Bad else Band.Attention
        else -> Band.Attention
    }

    fun evaluate(input: OfferInput, rules: List<CriterionRule>, policy: EvaluationPolicy): Evaluation {
        val boundary = policy.boundary
        val outcomes = rules.map { rule ->
            val result = metricFor(rule, input)
            val band = if (boundary != null && result is MetricResult.Value) classify(result.value, rule, boundary) else null
            CriterionOutcome(rule, result, band)
        }
        if (rules.isEmpty()) return Evaluation(outcomes, Assessment.NoCriteria)
        if (policy.pending.isNotEmpty()) return Evaluation(outcomes, Assessment.PolicyPending(policy.pending))
        if (policy.combination == CombinationPolicy.SeparateOnly) return Evaluation(outcomes, Assessment.SeparateOnly)

        val anyUnavailable = outcomes.any { it.result is MetricResult.Unavailable }
        if (anyUnavailable && policy.missing == MissingDataPolicy.InsufficientData) {
            return Evaluation(outcomes, Assessment.InsufficientData)
        }
        val bands = outcomes.mapNotNull { it.band }
        if (bands.isEmpty()) return Evaluation(outcomes, Assessment.InsufficientData)
        val worst = when {
            Band.Bad in bands -> Band.Bad
            Band.Attention in bands -> Band.Attention
            else -> Band.Good
        }
        return Evaluation(outcomes, Assessment.Colored(worst))
    }
}
