package br.app.motorista.floating

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import br.app.motorista.domain.card.CardStyle

/**
 * Configuração compartilhada entre o app e o serviço do botão flutuante (mesmo processo).
 *
 * [style] é o estilo do cartão (tela 06): o balão flutuante usa exatamente o mesmo, inclusive opacidade.
 * [buttonOpacity] é do BOTÃO e independente de [CardStyle.opacity] (opacidade do balão).
 * A cor do botão é fixa (verde) e não aparece aqui de propósito: não existe configuração que a ligue à avaliação.
 */
object FloatingConfig {
    /** Limite inferior provisório da opacidade do botão; legibilidade real a validar em aparelho. */
    const val BUTTON_OPACITY_MIN = 0.3f

    var style: CardStyle by mutableStateOf(CardStyle())
        private set

    var buttonOpacity: Float by mutableFloatStateOf(1f)
        private set

    /** Chamado quando a opacidade do botão muda, para o serviço atualizar a View. */
    @Volatile
    var onButtonOpacityChanged: (() -> Unit)? = null

    fun update(style: CardStyle, buttonOpacity: Float) {
        this.style = style
        val clamped = buttonOpacity.coerceIn(BUTTON_OPACITY_MIN, 1f)
        if (clamped != this.buttonOpacity) {
            this.buttonOpacity = clamped
            onButtonOpacityChanged?.invoke()
        }
    }
}
