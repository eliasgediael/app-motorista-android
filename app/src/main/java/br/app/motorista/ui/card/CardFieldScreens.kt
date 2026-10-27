package br.app.motorista.ui.card

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.app.motorista.domain.card.CardField
import br.app.motorista.domain.card.CardLayout
import br.app.motorista.domain.card.CardStyle
import br.app.motorista.domain.overlay.IntBox
import br.app.motorista.domain.overlay.Placement
import br.app.motorista.domain.overlay.SafeAreaPlanner
import br.app.motorista.ui.common.ChoiceChips
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SectionTitle
import br.app.motorista.ui.common.SurfaceCard
import br.app.motorista.ui.state.PreferencesState

/** Ordem dos campos (telas/48-ordem-campos): reordenar e ativar campos do cartão completo, com prévia. */
@Composable
fun FieldOrderScreen(state: PreferencesState, onStyle: ((CardStyle) -> CardStyle) -> Unit, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val style = state.cardStyle
    ScreenScaffold("Ordem dos campos", onBack, onOpen) {
        PreviewFrame(state.previewState, style.copy(layout = CardLayout.Full))
        InfoNote("Dados de exemplo. A ordem vale para o cartão completo e o secundário. Os campos de informação (tempo, distância, nota) aparecem na linha superior e as métricas, na inferior.")
        SurfaceCard {
            Column {
                style.fieldOrder.forEachIndexed { index, field ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(field.label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { onStyle { it.moved(field, -1) } }, enabled = index > 0) { Text("▲") }
                        TextButton(onClick = { onStyle { it.moved(field, 1) } }, enabled = index < style.fieldOrder.size - 1) { Text("▼") }
                        Switch(checked = field in style.enabledFields, onCheckedChange = { on -> onStyle { it.toggled(field, on) } })
                    }
                }
            }
        }
    }
}

/** Cartão secundário (telas/49-cartao-secundario): métricas compactas, escolhidas à parte do cartão completo. */
@Composable
fun SecondaryCardScreen(state: PreferencesState, onStyle: ((CardStyle) -> CardStyle) -> Unit, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val style = state.cardStyle
    ScreenScaffold("Cartão secundário", onBack, onOpen) {
        ChoiceChips(listOf(CardLayout.Full, CardLayout.Secondary), style.layout, { it.label }) { l -> onStyle { it.copy(layout = l) } }
        PreviewFrame(state.previewState, style.copy(layout = CardLayout.Secondary))
        InfoNote("O cartão secundário é o compacto, com os campos escolhidos abaixo. É independente do cartão completo e do aviso. Prévia com dados de exemplo.")
        SectionTitle("Campos do cartão secundário")
        SurfaceCard {
            Column {
                style.fieldOrder.forEach { field ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(field.label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Switch(checked = field in style.secondaryFields, onCheckedChange = { on -> onStyle { it.secondaryToggled(field, on) } })
                    }
                }
            }
        }
        Text(
            "Layout em uso agora: " + style.layout.label + ". O botão flutuante usa o mesmo layout e os mesmos campos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private enum class Preset(val label: String, val boxes: List<IntBox>) {
    A("Exemplo A", listOf(IntBox(0, 0, 1000, 160), IntBox(700, 1500, 1000, 1700))),
    B("Exemplo B", listOf(IntBox(0, 1700, 1000, 2000), IntBox(0, 0, 300, 400))),
    C("Exemplo C", listOf(IntBox(0, 0, 1000, 900), IntBox(0, 1180, 1000, 2000))),
    D("Sem espaço", listOf(IntBox(0, 0, 1000, 2000))),
}

/** Posição segura (telas/07-sobreposicao): SIMULAÇÃO da política geométrica. Não há contexto real de outro app. */
@Composable
fun SafeAreaScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    var known by rememberSaveable { mutableStateOf(false) }
    var preset by rememberSaveable { mutableStateOf(Preset.A) }
    var margin by rememberSaveable { mutableFloatStateOf(24f) }
    val screen = IntBox(0, 0, 1000, 2000)
    val placement = SafeAreaPlanner.plan(
        screen, if (known) preset.boxes else null,
        SafeAreaPlanner.Size(900, 380), SafeAreaPlanner.Size(900, 140), margin.toInt(),
    )

    ScreenScaffold("Posição segura", onBack, onOpen) {
        InfoNote(
            "Simulação da política de posição do cartão com retângulos de EXEMPLO. Hoje não existe fonte real dos controles de 99Pop ou Uber: " +
                "no uso real o cartão fica oculto até haver contexto validado em aparelho.",
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Contexto conhecido (simulado)", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Switch(checked = known, onCheckedChange = { known = it })
        }
        ChoiceChips(Preset.entries, preset, { it.label }) { preset = it }
        Text("Margem de segurança: ${margin.toInt()} (unidades da simulação)", style = MaterialTheme.typography.bodySmall)
        Slider(value = margin, onValueChange = { margin = it }, valueRange = 0f..100f)

        val exclusionColor = Color(0xFFE5484D)
        val cardColor = Color(0xFF159B67)
        Canvas(Modifier.fillMaxWidth().aspectRatio(0.5f)) {
            val sx = size.width / 1000f
            val sy = size.height / 2000f
            drawRect(Color(0xFFEFF2F0), size = size)
            if (known) {
                preset.boxes.forEach {
                    drawRect(exclusionColor.copy(alpha = 0.25f), Offset(it.left * sx, it.top * sy), Size(it.width * sx, it.height * sy))
                    drawRect(exclusionColor, Offset(it.left * sx, it.top * sy), Size(it.width * sx, it.height * sy), style = Stroke(2f))
                }
            }
            if (placement is Placement.At) {
                val b = placement.box
                drawRect(cardColor.copy(alpha = 0.85f), Offset(b.left * sx, b.top * sy), Size(b.width * sx, b.height * sy))
            }
        }
        Text(
            when (placement) {
                is Placement.At -> "Cartão " + (if (placement.compact) "compacto" else "completo") + " posicionado (verde), fora das exclusões (vermelho) e com margem."
                is Placement.Hidden -> "Cartão OCULTO: " + placement.reason + "."
            },
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Mudança de orientação, escala, teclado ou oferta remove o cartão até nova validação. Janela sem foco e sem toque durante a operação.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
