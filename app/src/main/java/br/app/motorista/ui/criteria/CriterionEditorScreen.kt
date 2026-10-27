package br.app.motorista.ui.criteria

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.app.motorista.domain.criteria.CriterionDraft
import br.app.motorista.domain.criteria.CriterionIssue
import br.app.motorista.domain.criteria.CriterionMetric
import br.app.motorista.domain.criteria.CriterionValidation
import br.app.motorista.domain.criteria.CriterionValidator
import br.app.motorista.domain.criteria.DistanceTimeBasis
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SettingRow
import br.app.motorista.ui.common.SurfaceCard
import br.app.motorista.ui.state.PreferencesState
import br.app.motorista.ui.theme.FunctionalColors

/** Cadastrar preferência (telas/46-editar-criterio). Nada vem preenchido; nenhum valor é sugerido. */
@Composable
fun CriterionEditorScreen(
    state: PreferencesState,
    onSelectMetric: (CriterionMetric) -> Unit,
    onUpdate: (CriterionMetric, (CriterionDraft) -> CriterionDraft) -> Unit,
    onReview: (CriterionMetric) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val metric = state.editingMetric
    val draft = state.draft(metric)
    val validation = CriterionValidator.validate(metric, draft)
    // Erros só aparecem depois de "Revisar", para não acusar campo ainda não preenchido.
    val reviewed = state.reviewedMetric == metric

    ScreenScaffold("Cadastrar preferência", onBack, onOpen) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CriterionMetric.entries.forEach { m ->
                FilterChip(selected = m == metric, onClick = { onSelectMetric(m) }, label = { Text(m.label) })
            }
        }

        SurfaceCard {
            SettingRow(
                icon = Icons.Outlined.BarChart,
                title = "Usar este critério",
                subtitle = if (validation.isValid) "Desativado até você decidir" else "Preencha os campos para poder ativar",
                trailing = {
                    Switch(
                        checked = draft.enabled && validation.isValid,
                        enabled = validation.isValid,
                        onCheckedChange = { on -> onUpdate(metric) { it.copy(enabled = on) } },
                    )
                },
            )
        }

        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(
                    label = "Valor mínimo",
                    value = draft.minimum,
                    error = if (reviewed) fieldError(validation, CriterionIssue.MinimumMissing, CriterionIssue.MinimumInvalid) else null,
                    onChange = { text -> onUpdate(metric) { it.copy(minimum = text, enabled = false) } },
                )
                NumberField(
                    label = "Considerado bom a partir de",
                    value = draft.good,
                    error = if (reviewed) fieldError(validation, CriterionIssue.GoodMissing, CriterionIssue.GoodInvalid, CriterionIssue.GoodBelowMinimum) else null,
                    onChange = { text -> onUpdate(metric) { it.copy(good = text, enabled = false) } },
                )
                if (metric.needsBasis) {
                    BasisPicker(
                        selected = draft.basis,
                        showError = reviewed && CriterionIssue.BasisMissing in validation.issues,
                        onSelect = { basis -> onUpdate(metric) { it.copy(basis = basis, enabled = false) } },
                    )
                }
            }
        }

        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BandLegend(FunctionalColors.Bad, "Abaixo do mínimo")
                BandLegend(FunctionalColors.Attention, "Entre mínimo e bom")
                BandLegend(FunctionalColors.Good, "A partir de bom")
            }
        }

        InfoNote("Confirmar o tratamento de valores exatamente no limite. Nenhum número do vídeo foi importado como sua preferência.")

        if (reviewed && validation.isValid) {
            InfoNote(
                buildString {
                    append("Rascunho revisado: mínimo ${draft.minimum.trim()}, bom ${draft.good.trim()} ${metric.unit}")
                    draft.basis?.let { append(", base ${it.label.lowercase()}") }
                    append(". Ainda não é regra confirmada e não gera avaliação.")
                    if (validation.bandsCollapsed) append(" Atenção: mínimo igual a bom deixa a faixa amarela vazia.")
                },
            )
        }

        OutlinedButton(onClick = { onOpen("combinacao") }, modifier = Modifier.fillMaxWidth()) { Text("Definir combinação") }
        Button(onClick = { onReview(metric) }, modifier = Modifier.fillMaxWidth()) { Text("Revisar preferência") }
    }
}

private fun fieldError(validation: CriterionValidation, vararg issues: CriterionIssue): String? {
    val issue = issues.firstOrNull { it in validation.issues } ?: return null
    return when (issue) {
        CriterionIssue.MinimumMissing, CriterionIssue.GoodMissing -> "Informe um valor"
        CriterionIssue.MinimumInvalid, CriterionIssue.GoodInvalid -> "Use um número maior ou igual a zero"
        CriterionIssue.GoodBelowMinimum -> "Não pode ser menor que o valor mínimo"
        CriterionIssue.BasisMissing -> null
    }
}

@Composable
private fun NumberField(label: String, value: String, error: String?, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text("Não definido") },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
private fun BasisPicker(selected: DistanceTimeBasis?, showError: Boolean, onSelect: (DistanceTimeBasis) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text("Base de distância/tempo", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(selected?.label ?: "Escolher base", Modifier.weight(1f))
                Icon(Icons.Outlined.KeyboardArrowDown, null)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DistanceTimeBasis.entries.forEach { basis ->
                    DropdownMenuItem(text = { Text(basis.label) }, onClick = { onSelect(basis); open = false })
                }
            }
        }
        if (showError) {
            Text("Escolha a base do cálculo", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun BandLegend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(text, Modifier.padding(start = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
