package br.app.motorista.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.app.motorista.domain.card.CardField
import br.app.motorista.domain.card.CardFontSize
import br.app.motorista.domain.card.CardLayout
import br.app.motorista.domain.card.CardStyle
import br.app.motorista.domain.card.PreviewState
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SectionTitle
import br.app.motorista.ui.common.SettingRow
import br.app.motorista.ui.common.SurfaceCard
import br.app.motorista.ui.components.BalloonValues
import br.app.motorista.ui.components.OfferBalloon
import br.app.motorista.ui.components.color
import br.app.motorista.ui.state.PreferencesState

/** Estilo do cartão (telas/06-cartao): prévia com estados verde/amarelo/vermelho e opções de exibição. */
@Composable
fun CardStyleScreen(
    state: PreferencesState,
    onPreviewState: (PreviewState) -> Unit,
    onStyle: ((CardStyle) -> CardStyle) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val style = state.cardStyle

    ScreenScaffold("Estilo do cartão", onBack, onOpen) {
        PreviewStateChips(state.previewState, onPreviewState)
        PreviewFrame(state.previewState, style)

        SectionTitle("Layout do cartão")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CardLayout.entries.forEach { layout ->
                FilterChip(
                    selected = style.layout == layout,
                    onClick = { onStyle { it.copy(layout = layout) } },
                    label = { Text(layout.label) },
                )
            }
        }

        SectionTitle("Aparência")
        SurfaceCard {
            Column {
                SettingRow(Icons.Outlined.Notifications, "Aviso em destaque", trailing = {
                    Switch(style.highlightSeal, { on -> onStyle { it.copy(highlightSeal = on) } })
                })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(Icons.AutoMirrored.Outlined.List, "Exibir motivo", trailing = {
                    Switch(style.showReason, { on -> onStyle { it.copy(showReason = on) } })
                })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    Icons.Outlined.LocationOn,
                    "Posição do cartão",
                    subtitle = "Ajustar dentro da área segura",
                    onClick = { onOpen("sobreposicao") },
                    trailing = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    Icons.Outlined.Description,
                    "Tamanho da fonte",
                    subtitle = CardFontSize.entries.joinToString(" · ") { it.label },
                    trailing = { FontSizePicker(style.fontSize) { size -> onStyle { it.copy(fontSize = size) } } },
                )
            }
        }

        SurfaceCard {
            Column(Modifier.padding(16.dp)) {
                Text("Opacidade ${(style.opacity * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = style.opacity,
                    onValueChange = { v -> onStyle { it.copy(opacity = v) } },
                    valueRange = OPACITY_MIN..1f,
                )
            }
        }

        SectionTitle("Dados do cartão")
        SurfaceCard {
            Column {
                CardField.entries.forEachIndexed { index, field ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(
                        icon = Icons.AutoMirrored.Outlined.List,
                        title = field.label,
                        subtitle = if (field == CardField.EstimatedResult) "Indisponível sem modelo de custo validado" else null,
                        trailing = {
                            Switch(field in style.enabledFields, { on -> onStyle { it.toggled(field, on) } })
                        },
                    )
                }
            }
        }

        OutlinedButton(
            onClick = {
                onOpen(
                    when (state.previewState) {
                        PreviewState.Good -> "balao-boa"
                        PreviewState.Attention -> "balao-atencao"
                        PreviewState.Bad -> "balao-ruim"
                    },
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Abrir tela do balão ${state.previewState.label.lowercase()}") }
        OutlinedButton(onClick = { onOpen("ordem-campos") }, modifier = Modifier.fillMaxWidth()) { Text("Reordenar campos") }
        InfoNote("Cartão informativo: nenhum estado realiza aceite ou recusa. Prévia com dados de exemplo, sem integração com plataforma.")
    }
}

/** Limite inferior provisório da opacidade; valor real de legibilidade ainda a validar em aparelho. */
private const val OPACITY_MIN = 0.3f

@Composable
fun PreviewStateChips(selected: PreviewState, onSelect: (PreviewState) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PreviewState.entries.forEach { s ->
            FilterChip(
                selected = s == selected,
                onClick = { onSelect(s) },
                label = { Text(s.label) },
                leadingIcon = { Box(Modifier.size(10.dp).background(s.color(), CircleShape)) },
            )
        }
    }
}

/** Moldura tracejada de prévia com aviso de dados de exemplo. */
@Composable
fun PreviewFrame(state: PreviewState, style: CardStyle, values: BalloonValues = BalloonValues.Sample) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(state.color().copy(alpha = 0.08f))
            .border(1.dp, state.color().copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OfferBalloon(state = state, style = style, values = values)
        Text(
            "Dados de exemplo · avaliação ilustrativa",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FontSizePicker(selected: CardFontSize, onSelect: (CardFontSize) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        CardFontSize.entries.forEach { size ->
            Text(
                size.label,
                fontWeight = if (size == selected) FontWeight.Bold else FontWeight.Normal,
                color = if (size == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable { onSelect(size) }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
    }
}
