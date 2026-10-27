package br.app.motorista.offer

import br.app.motorista.domain.assessment.Assessment
import br.app.motorista.domain.assessment.Band
import br.app.motorista.domain.assessment.Evaluation
import br.app.motorista.domain.offer.OfferOrigin
import br.app.motorista.domain.offer.OfferSnapshot
import br.app.motorista.domain.offer.OfferStore
import br.app.motorista.domain.offer.Platform
import br.app.motorista.ui.components.BalloonValues

/** Resultado já calculado de UMA oferta de UMA plataforma, pronto para o balão. */
data class PublishedBalloon(
    val offerId: String,
    val platform: Platform,
    val origin: OfferOrigin,
    val capturedAtMs: Long,
    /** null = sem cor geral (ver [note]). */
    val band: Band?,
    val note: String,
    val values: BalloonValues,
)

/**
 * Ponto único entre as fontes de oferta (hoje só entrada manual) e os balões (app e botão flutuante).
 * Process-wide e em memória; nada é gravado em disco.
 */
object BalloonHub {
    val store = OfferStore()

    private var latestByPlatform = mutableMapOf<Platform, PublishedBalloon>()

    /** Avisa quem exibe balão que a oferta atual mudou (publicada, substituída ou invalidada). */
    @Volatile
    var listener: (() -> Unit)? = null

    @Synchronized
    fun publish(snapshot: OfferSnapshot, evaluation: Evaluation, values: BalloonValues): Boolean {
        if (!store.accept(snapshot)) return false
        val a = evaluation.assessment
        latestByPlatform[snapshot.platform] = PublishedBalloon(
            offerId = snapshot.id,
            platform = snapshot.platform,
            origin = snapshot.origin,
            capturedAtMs = snapshot.capturedAtMs,
            band = (a as? Assessment.Colored)?.band,
            note = noteFor(a),
            values = values,
        )
        listener?.invoke()
        return true
    }

    /** Último publicado da plataforma, mesmo que antigo; quem exibe confere a validade na [store]. */
    @Synchronized
    fun latest(platform: Platform): PublishedBalloon? = latestByPlatform[platform]

    /** Plataforma com a oferta publicada mais recente, para o menu do botão flutuante. */
    @Synchronized
    fun latestAny(): PublishedBalloon? = latestByPlatform.values.maxByOrNull { it.capturedAtMs }

    @Synchronized
    fun invalidate(platform: Platform) {
        store.invalidate(platform)
        latestByPlatform.remove(platform)
        listener?.invoke()
    }

    private fun noteFor(a: Assessment): String = when (a) {
        is Assessment.Colored -> "Selo informativo; decisão manual."
        Assessment.NoCriteria -> "Sem critérios ativos: sem avaliação."
        is Assessment.PolicyPending -> "Sem cor geral: falta definir " + a.missing.joinToString(", ") + "."
        Assessment.SeparateOnly -> "Métricas separadas: sem cor geral."
        Assessment.InsufficientData -> "Dados insuficientes: sem cor geral."
    }
}
