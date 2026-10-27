package br.app.motorista.ui.criteria

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.app.motorista.domain.criteria.CriterionMetric
import br.app.motorista.domain.criteria.CriterionValidator
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SectionTitle
import br.app.motorista.ui.common.SettingRow
import br.app.motorista.ui.common.SurfaceCard
import br.app.motorista.ui.state.PreferencesState
import br.app.motorista.ui.state.RidePlatform
import br.app.motorista.ui.theme.FunctionalColors

/** Critérios de análise (telas/03-criterios). */
@Composable
fun CriteriaScreen(
    state: PreferencesState,
    onTogglePlatform: (RidePlatform) -> Unit,
    onEditMetric: (CriterionMetric) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    ScreenScaffold("Critérios de análise", onBack, onOpen) {
        SurfaceCard(onClick = { onOpen("cartao") }) {
            SettingRow(
                icon = Icons.Outlined.CreditCard,
                title = "Estilo do cartão",
                subtitle = "Personalize as informações",
                trailing = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) },
            )
        }

        SectionTitle("Aplicativos")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RidePlatform.entries.forEach { platform ->
                FilterChip(
                    selected = platform in state.selectedPlatforms,
                    onClick = { onTogglePlatform(platform) },
                    label = { Text(platform.label) },
                )
            }
        }
        Text(
            "Marcar um aplicativo apenas registra sua preferência. Não existe leitura nem integração com nenhum deles.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionTitle("Classificação por faixa")
        SurfaceCard {
            Column {
                CriterionMetric.entries.forEachIndexed { index, metric ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    MetricBandRow(metric, state, onEdit = { onEditMetric(metric); onOpen("editar-criterio") })
                }
            }
        }

        SectionTitle("Ganhos mínimos")
        SurfaceCard {
            Column {
                // Valor mínimo, lucro e percentual precisam de especificação própria (modelo de custo pendente).
                PendingRow("Valor mínimo da viagem")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                PendingRow("Lucro estimado mínimo")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                PendingRow("Porcentagem de lucro")
            }
        }

        SectionTitle("Limites e prioridades")
        SurfaceCard {
            Column {
                NavRow(Icons.Outlined.Schedule, "Tempo e distância", "Embarque e viagem") { onOpen("perfis") }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NavRow(Icons.AutoMirrored.Outlined.List, "Como combinar critérios", "Escolher regra de avaliação · pendente") { onOpen("combinacao") }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NavRow(Icons.Outlined.Person, "Perfis de trabalho", "Guardar preferências por contexto") { onOpen("perfis") }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NavRow(Icons.Outlined.LocalGasStation, "Custo operacional", "Combustível, manutenção e outros") { onOpen("custo") }
            }
        }

        InfoNote("Os seus valores ainda não foram definidos. Sem regras confirmadas, nenhuma avaliação Aceitar/Atenção/Recusar é emitida.")
    }
}

@Composable
private fun MetricBandRow(metric: CriterionMetric, state: PreferencesState, onEdit: () -> Unit) {
    val draft = state.draft(metric)
    val validation = CriterionValidator.validate(metric, draft)
    val defined = validation.isValid
    val icon: ImageVector = if (metric == CriterionMetric.PassengerRating) Icons.Outlined.Star else Icons.Outlined.BarChart
    val title = when (metric) {
        CriterionMetric.RevenuePerKm -> "Ganho por km"
        CriterionMetric.RevenuePerHour -> "Ganho por hora"
        CriterionMetric.PassengerRating -> "Nota do passageiro"
    }

    Column(Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, Modifier.weight(1f).padding(start = 10.dp), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Text(
                if (defined) "Editar" else "Definir",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        BandBar(defined)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Ruim", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (defined) "Mín. ${draft.minimum.trim()} · Bom ${draft.good.trim()} ${metric.unit}" else "Não definido",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("Boa", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (defined && !draft.enabled) {
            Text("Valores preenchidos, critério desativado.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Barra de três faixas: cinza quando não definido, colorida só com valores válidos. */
@Composable
private fun BandBar(defined: Boolean) {
    val neutral = Color(0xFFD5DDD8)
    Row(Modifier.fillMaxWidth().height(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(FunctionalColors.Bad, FunctionalColors.Attention, FunctionalColors.Good).forEach { color ->
            Box(Modifier.weight(1f).height(6.dp).background(if (defined) color else neutral, RoundedCornerShape(3.dp)))
        }
    }
}

@Composable
private fun PendingRow(title: String) {
    SettingRow(
        icon = Icons.Outlined.AttachMoney,
        title = title,
        subtitle = "Especificação pendente",
        trailing = {
            Text("Em breve", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        },
    )
}

@Composable
private fun NavRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    SettingRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        trailing = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
    )
}
