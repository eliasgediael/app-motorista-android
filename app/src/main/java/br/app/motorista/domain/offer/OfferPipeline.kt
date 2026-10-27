package br.app.motorista.domain.offer

import br.app.motorista.domain.assessment.CriterionRule
import br.app.motorista.domain.assessment.Evaluation
import br.app.motorista.domain.assessment.EvaluationPolicy
import br.app.motorista.domain.assessment.OfferEvaluator

sealed interface Availability {
    data class NoOffer(val platform: Platform) : Availability

    /** Oferta existe, mas é antiga demais: nunca mostrar como atual. */
    data class Stale(val snapshot: OfferSnapshot, val ageMs: Long) : Availability

    data class Ready(val snapshot: OfferSnapshot, val ageMs: Long, val evaluation: Evaluation) : Availability
}

/** Fluxo único compartilhado por todas as plataformas: oferta atual da plataforma -> avaliação. */
object OfferPipeline {
    fun analyze(
        store: OfferStore,
        platform: Platform,
        nowMs: Long,
        rules: List<CriterionRule>,
        policy: EvaluationPolicy,
    ): Availability = when (val s = store.slot(platform, nowMs)) {
        Slot.Empty -> Availability.NoOffer(platform)
        is Slot.Stale -> Availability.Stale(s.snapshot, s.ageMs)
        is Slot.Fresh -> Availability.Ready(s.snapshot, s.ageMs, OfferEvaluator.evaluate(s.snapshot.input, rules, policy))
    }
}
