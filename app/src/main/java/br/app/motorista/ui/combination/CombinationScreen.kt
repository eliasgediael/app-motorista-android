package br.app.motorista.ui.combination

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.app.motorista.domain.assessment.BoundaryPolicy
import br.app.motorista.domain.assessment.CombinationPolicy
import br.app.motorista.domain.assessment.EvaluationPolicy
import br.app.motorista.domain.assessment.MissingDataPolicy
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SectionTitle
import br.app.motorista.ui.common.SurfaceCard

/** Combinar critérios (telas/47-combinacao). Nada vem selecionado: a decisão é do usuário. */
@Composable
fun CombinationScreen(
    policy: EvaluationPolicy,
    onChange: ((EvaluationPolicy) -> EvaluationPolicy) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    ScreenScaffold("Combinar critérios", onBack, onOpen) {
        InfoNote("Já vem com o padrão: no mínimo exato = amarelo; a pior faixa define a cor; critério sem dado é ignorado. Só mude se quiser outra regra.")

        SectionTitle("Como combinar os critérios ativos")
        OptionCard(
            options = CombinationPolicy.entries,
            selected = policy.combination,
            title = { it.label },
            detail = { it.description },
            onSelect = { c -> onChange { it.copy(combination = c) } },
        )

        SectionTitle("Valor exatamente no mínimo")
        OptionCard(
            options = BoundaryPolicy.entries,
            selected = policy.boundary,
            title = { it.label },
            detail = { null },
            onSelect = { b -> onChange { it.copy(boundary = b) } },
        )

        SectionTitle("Critério sem dado na oferta")
        OptionCard(
            options = MissingDataPolicy.entries,
            selected = policy.missing,
            title = { it.label },
            detail = { null },
            onSelect = { m -> onChange { it.copy(missing = m) } },
        )

        if (policy.pending.isNotEmpty()) {
            Text(
                "Pendente: ${policy.pending.joinToString(", ")}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun <T> OptionCard(
    options: List<T>,
    selected: T?,
    title: (T) -> String,
    detail: (T) -> String?,
    onSelect: (T) -> Unit,
) {
    SurfaceCard {
        Column {
            options.forEach { option ->
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.RadioButton) { onSelect(option) }.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = option == selected, onClick = null, modifier = Modifier.padding(8.dp))
                    Column(Modifier.padding(start = 4.dp)) {
                        Text(title(option), fontWeight = FontWeight.SemiBold)
                        detail(option)?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
