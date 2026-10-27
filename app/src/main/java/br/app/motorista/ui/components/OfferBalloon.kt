package br.app.motorista.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.app.motorista.domain.card.CardField
import br.app.motorista.domain.card.CardLayout
import br.app.motorista.domain.card.CardStyle
import br.app.motorista.domain.card.PreviewState
import br.app.motorista.domain.card.SampleOffer
import br.app.motorista.ui.theme.FunctionalColors

fun PreviewState.color(): Color = when (this) {
    PreviewState.Good -> FunctionalColors.Good
    PreviewState.Attention -> FunctionalColors.Attention
    PreviewState.Bad -> FunctionalColors.Bad
}

/** Textos já formatados exibidos no balão. [Sample] são valores fictícios das prévias. */
data class BalloonValues(
    val time: String,
    val distance: String,
    val rating: String,
    val perKm: String,
    val perHour: String,
    val perMinute: String,
    val fare: String,
    val result: String,
    val tag: String,
) {
    companion object {
        val Sample = BalloonValues(
            SampleOffer.MINUTES, SampleOffer.DISTANCE, SampleOffer.RATING,
            SampleOffer.PER_KM, SampleOffer.PER_HOUR, SampleOffer.PER_MINUTE,
            SampleOffer.FARE, "Indisponível", "Exemplo",
        )
    }
}

private data class Cell(val label: String, val value: String, val big: Boolean)

/**
 * Prévia do balão de análise com oferta de EXEMPLO. O selo é informativo:
 * nunca é botão, não aceita nem recusa nada em plataforma alguma.
 * O estado vem de um seletor de prévia, não de avaliação de critérios.
 */
@Composable
fun OfferBalloon(
    state: PreviewState,
    style: CardStyle,
    modifier: Modifier = Modifier,
    values: BalloonValues = BalloonValues.Sample,
) {
    val accent = state.color()
    val scale = style.fontSize.scale
    // Ordem fixa por enquanto; reordenação é a tela 48 (ainda não implementada).
    val fields = CardField.entries.filter { it in style.enabledFields }

    Column(modifier.fillMaxWidth().alpha(style.opacity)) {
        if (style.highlightSeal) {
            Text(
                state.seal,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (12 * scale).sp,
                modifier = Modifier
                    .padding(start = 14.dp)
                    .background(accent, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(BorderStroke(2.dp, accent), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val info = fields.filter { it in INFO_FIELDS }
            val metrics = fields.filter { it !in INFO_FIELDS }

            if (info.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        values.tag,
                        fontSize = (11 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3A463F),
                        modifier = Modifier.background(Color(0xFFE9EEEB), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                    Text(
                        info.joinToString(" · ") { infoText(it, values) },
                        fontSize = (13 * scale).sp,
                        color = Color(0xFF5F6E66),
                    )
                }
            }
            if (metrics.isNotEmpty()) {
                val cells = metrics.map { cellFor(it, values, style.layout == CardLayout.Full) }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    cells.forEach { cell ->
                        Row {
                            Box(Modifier.padding(end = 8.dp).background(accent).padding(horizontal = 1.5.dp, vertical = 16.dp))
                            Column {
                                Text(cell.label, fontSize = (11 * scale).sp, color = Color(0xFF5F6E66))
                                Text(
                                    cell.value,
                                    fontSize = ((if (cell.big) 26 else 16) * scale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15211B),
                                )
                            }
                        }
                    }
                }
            }
            if (fields.isEmpty()) {
                Text("Nenhum dado selecionado.", fontSize = (13 * scale).sp, color = Color(0xFF5F6E66))
            }
            if (style.showReason && style.layout == CardLayout.Full) {
                Text(
                    "Estado de prévia: ${state.label.lowercase()} · decisão manual",
                    fontSize = (12 * scale).sp,
                    color = accent,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private val INFO_FIELDS = setOf(CardField.TotalTime, CardField.TotalDistance, CardField.PassengerRating)

private fun infoText(field: CardField, v: BalloonValues): String = when (field) {
    CardField.TotalTime -> v.time
    CardField.TotalDistance -> v.distance
    CardField.PassengerRating -> "★ ${v.rating}"
    else -> ""
}

private fun cellFor(field: CardField, v: BalloonValues, big: Boolean): Cell = when (field) {
    CardField.RevenuePerKm -> Cell("R$/km", v.perKm, big)
    CardField.RevenuePerHour -> Cell("R$/h", v.perHour, big)
    CardField.RevenuePerMinute -> Cell("R$/min", v.perMinute, big)
    CardField.OfferValue -> Cell("Oferta", "R$ ${v.fare}", big)
    // Sem modelo de custo validado: não calcular nem mostrar zero.
    CardField.EstimatedResult -> Cell("Resultado", v.result, false)
    else -> Cell(field.label, "—", false)
}
