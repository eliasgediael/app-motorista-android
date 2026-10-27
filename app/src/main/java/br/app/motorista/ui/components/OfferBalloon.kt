package br.app.motorista.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.app.motorista.domain.assessment.Band
import br.app.motorista.domain.card.CardField
import br.app.motorista.domain.card.CardLayout
import br.app.motorista.domain.card.CardStyle
import br.app.motorista.domain.card.PreviewState
import br.app.motorista.domain.card.SampleOffer
import br.app.motorista.ui.theme.FunctionalColors

/**
 * Escala máxima de fonte do sistema aplicada ao balão. PROVISÓRIO: limita a altura do balão em
 * sobreposição para ele não ficar maior que a tela com fonte muito ampliada. O restante do app segue
 * a escala do sistema. Mesmo limite na prévia e no balão flutuante.
 */
const val BALLOON_MAX_FONT_SCALE = 1.3f

fun PreviewState.color(): Color = when (this) {
    PreviewState.Good -> FunctionalColors.Good
    PreviewState.Attention -> FunctionalColors.Attention
    PreviewState.Bad -> FunctionalColors.Bad
}

fun Band.toPreviewState(): PreviewState = when (this) {
    Band.Good -> PreviewState.Good
    Band.Attention -> PreviewState.Attention
    Band.Bad -> PreviewState.Bad
}

/** Cinza do balão sem avaliação (indisponível ou sem cor geral). */
val BalloonNeutral = Color(0xFF5F6E66)

private val Ink = Color(0xFF15211B)
private val Grey = Color(0xFF5F6E66)

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

private data class Cell(val label: String, val value: String)

private val INFO_FIELDS = setOf(CardField.TotalTime, CardField.TotalDistance, CardField.PassengerRating)

/** Moldura comum a todos os balões: selo, contorno colorido, opacidade e escala de fonte. */
@Composable
fun BalloonFrame(
    seal: String,
    accent: Color,
    style: CardStyle,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(scale: Float) -> Unit,
) {
    val outer = LocalDensity.current
    val capped = Density(outer.density, minOf(outer.fontScale, BALLOON_MAX_FONT_SCALE))
    CompositionLocalProvider(LocalDensity provides capped) {
        val scale = style.fontSize.scale
        Column(modifier.fillMaxWidth().alpha(style.opacity)) {
            if (style.highlightSeal) {
                Text(
                    seal,
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
                content(scale)
            }
        }
    }
}

/**
 * Balão de análise. Usado igualmente na prévia do app e no balão flutuante, para os dois seguirem
 * o mesmo layout e as mesmas configurações. O selo é informativo: nunca é botão e não aceita nem
 * recusa nada em plataforma alguma.
 */
@Composable
fun OfferBalloon(
    seal: String,
    accent: Color,
    style: CardStyle,
    values: BalloonValues,
    reason: String?,
    modifier: Modifier = Modifier,
) {
    BalloonFrame(seal, accent, style, modifier) { scale ->
        // Ordem fixa por enquanto; reordenação é a tela 48 (ainda não implementada).
        val fields = CardField.entries.filter { it in style.enabledFields }
        val info = fields.filter { it in INFO_FIELDS }
        val metrics = fields.filter { it !in INFO_FIELDS && it != CardField.EstimatedResult }
        val showResult = CardField.EstimatedResult in fields

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
                    color = Grey,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (metrics.isNotEmpty()) {
            MetricGrid(metrics.map { cellFor(it, values) }, accent, scale, compact = style.layout == CardLayout.Secondary)
        }
        if (showResult) {
            Text(
                "Resultado estimado: " + values.result,
                fontSize = (13 * scale).sp,
                color = Grey,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (fields.isEmpty()) {
            Text("Nenhum dado selecionado.", fontSize = (13 * scale).sp, color = Grey)
        }
        if (reason != null && style.showReason && style.layout == CardLayout.Full) {
            Text(reason, fontSize = (12 * scale).sp, color = accent, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Balão para um estado de prévia (verde, amarelo ou vermelho). */
@Composable
fun OfferBalloon(
    state: PreviewState,
    style: CardStyle,
    modifier: Modifier = Modifier,
    values: BalloonValues = BalloonValues.Sample,
) {
    OfferBalloon(
        seal = state.seal,
        accent = state.color(),
        style = style,
        values = values,
        reason = "Estado de prévia: " + state.label.lowercase() + " · decisão manual",
        modifier = modifier,
    )
}

/** Balão sem avaliação: oferta ausente, expirada ou substituída. Nunca mostra valores antigos. */
@Composable
fun UnavailableBalloon(
    origin: String?,
    message: String,
    style: CardStyle,
    modifier: Modifier = Modifier,
) {
    BalloonFrame("INDISPONÍVEL", BalloonNeutral, style, modifier) { scale ->
        if (origin != null) {
            Text(origin, fontSize = (11 * scale).sp, color = Grey, modifier = Modifier.fillMaxWidth())
        }
        Text(message, fontSize = (14 * scale).sp, color = Ink, modifier = Modifier.fillMaxWidth())
    }
}

/** Métricas em colunas de mesma largura. Menos colunas quando a largura ou a fonte efetiva exigem. */
@Composable
private fun MetricGrid(cells: List<Cell>, accent: Color, scale: Float, compact: Boolean) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val effective = LocalDensity.current.fontScale * scale
        val columns = if (maxWidth < 260.dp || effective >= 1.35f) 2 else 3
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cells.chunked(columns).forEach { rowCells ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowCells.forEach { cell ->
                        Row(Modifier.weight(1f)) {
                            Spacer(Modifier.width(3.dp).fillMaxHeight().background(accent))
                            Column(Modifier.padding(start = 8.dp)) {
                                Text(cell.label, fontSize = (11 * scale).sp, color = Grey)
                                Text(
                                    cell.value,
                                    fontSize = ((if (compact) 16 else 24) * scale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink,
                                )
                            }
                        }
                    }
                    repeat(columns - rowCells.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

private fun infoText(field: CardField, v: BalloonValues): String = when (field) {
    CardField.TotalTime -> v.time
    CardField.TotalDistance -> v.distance
    CardField.PassengerRating -> "★ " + v.rating
    else -> ""
}

private fun cellFor(field: CardField, v: BalloonValues): Cell = when (field) {
    CardField.RevenuePerKm -> Cell("R$/km", v.perKm)
    CardField.RevenuePerHour -> Cell("R$/h", v.perHour)
    CardField.RevenuePerMinute -> Cell("R$/min", v.perMinute)
    CardField.OfferValue -> Cell("Oferta", "R$ " + v.fare)
    else -> Cell(field.label, "—")
}
