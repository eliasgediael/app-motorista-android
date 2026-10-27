package br.app.motorista.reader

import android.content.Context
import br.app.motorista.data.PrefsStore
import br.app.motorista.domain.assessment.CriterionRule
import br.app.motorista.domain.assessment.OfferEvaluator
import br.app.motorista.domain.calc.Display
import br.app.motorista.domain.calc.MetricResult
import br.app.motorista.domain.calc.OfferCalculator
import br.app.motorista.domain.criteria.CriterionMetric
import br.app.motorista.domain.criteria.CriterionValidator
import br.app.motorista.domain.offer.OfferSnapshot
import br.app.motorista.offer.BalloonHub
import br.app.motorista.ui.components.BalloonValues

/**
 * Leva uma oferta INTERPRETADA da leitura de tela ao mesmo fluxo da entrada manual (cálculo, regras,
 * balão, voz). Usa as regras e a política salvas do usuário. Só roda quando um interpretador validado
 * devolve [br.app.motorista.domain.offer.ParseOutcome.Parsed]; hoje nenhum devolve.
 */
internal object ReaderPublisher {
    fun publish(context: Context, snapshot: OfferSnapshot) {
        val prefs = PrefsStore(context).load()
        val rules = CriterionMetric.entries.mapNotNull { m ->
            val d = prefs.drafts[m] ?: return@mapNotNull null
            val v = CriterionValidator.validate(m, d)
            if (d.enabled && v.isValid) CriterionRule(m, v.minimum!!, v.good!!, d.basis) else null
        }
        val evaluation = OfferEvaluator.evaluate(snapshot.input, rules, prefs.policy)
        val basis = rules.firstOrNull { it.basis != null }?.basis
        fun fmt(r: MetricResult) = if (r is MetricResult.Value) Display.decimal(r.value) else "—"
        val values = BalloonValues(
            time = basis?.let { OfferCalculator.totalSeconds(snapshot.input, it) }?.let { (it / 60).toString() + " min" } ?: "— min",
            distance = basis?.let { OfferCalculator.totalMeters(snapshot.input, it) }?.let { Display.decimal(java.math.BigDecimal(it).movePointLeft(3), 1) + " km" } ?: "— km",
            rating = snapshot.input.passengerRating?.let { Display.decimal(it) } ?: "—",
            perKm = fmt(OfferCalculator.revenuePerKm(snapshot.input, basis)),
            perHour = fmt(OfferCalculator.revenuePerHour(snapshot.input, basis)),
            perMinute = fmt(OfferCalculator.revenuePerMinute(snapshot.input, basis)),
            fare = snapshot.input.fareCents?.let { Display.decimal(java.math.BigDecimal(it).movePointLeft(2)) } ?: "—",
            result = "indisponível",
            tag = snapshot.platform.label,
        )
        BalloonHub.publish(snapshot, evaluation, values)
    }
}
