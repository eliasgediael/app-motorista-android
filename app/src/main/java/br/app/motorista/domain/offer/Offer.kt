package br.app.motorista.domain.offer

import br.app.motorista.domain.calc.OfferInput

/** Aplicativos de corrida. Cada um tem leitura/interpretação própria; cálculo e balão são compartilhados. */
enum class Platform(val label: String) {
    P99("99Pop"),
    Uber("Uber"),
}

/** De onde veio a oferta. Só [Manual] existe hoje; [ScreenReader] fica reservado e desativado. */
enum class OfferOrigin(val label: String) {
    Manual("manual"),
    ScreenReader("leitura de tela"),
}

/**
 * Oferta já normalizada e identificada. [capturedAtMs] vem de relógio monotônico (elapsedRealtime).
 * [layoutId] identifica a versão de layout que o interpretador validou; null para entrada manual.
 */
data class OfferSnapshot(
    val id: String,
    val platform: Platform,
    val origin: OfferOrigin,
    val capturedAtMs: Long,
    val input: OfferInput,
    val layoutId: String? = null,
)

/** Texto visível de uma tela de terceiros, já reduzido a nós de texto. Sem imagem, sem ação. */
data class TextNode(val text: String, val viewId: String? = null)

data class ScreenSnapshot(val packageName: String, val capturedAtMs: Long, val nodes: List<TextNode>)

sealed interface ParseOutcome {
    data class Parsed(val snapshot: OfferSnapshot) : ParseOutcome

    /** Layout não reconhecido ou não validado: nada é inventado. */
    data class Unsupported(val reason: String) : ParseOutcome

    /** Layout reconhecido, mas faltam campos para uma oferta utilizável. */
    data class Incomplete(val missing: List<String>) : ParseOutcome
}

/** Interpretador de UMA plataforma. Nunca recebe nem devolve dados de outra. */
interface PlatformOfferParser {
    val platform: Platform
    fun parse(screen: ScreenSnapshot): ParseOutcome
}

/**
 * Interpretador padrão enquanto não houver amostra real e layout validado em aparelho.
 * Devolve sempre [ParseOutcome.Unsupported]; não adivinha campos.
 */
class UnvalidatedParser(override val platform: Platform) : PlatformOfferParser {
    override fun parse(screen: ScreenSnapshot): ParseOutcome =
        ParseOutcome.Unsupported("layout de ${platform.label} ainda não validado: falta amostra real e teste em aparelho")
}
