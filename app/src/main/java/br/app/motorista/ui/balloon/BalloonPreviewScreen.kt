package br.app.motorista.ui.balloon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.app.motorista.domain.card.PreviewState
import br.app.motorista.ui.card.PreviewFrame
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.state.PreferencesState

/** Telas 42/43/44: balão verde, amarelo e vermelho. Só prévia; não é avaliação de oferta. */
@Composable
fun BalloonPreviewScreen(
    forced: PreviewState,
    state: PreferencesState,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val title = when (forced) {
        PreviewState.Good -> "Balão verde"
        PreviewState.Attention -> "Balão amarelo"
        PreviewState.Bad -> "Balão vermelho"
    }
    ScreenScaffold(title, onBack, onOpen) {
        PreviewFrame(forced, state.cardStyle)

        Text(
            when (forced) {
                PreviewState.Good -> "Aparência do estado bom, com três métricas e explicação."
                PreviewState.Attention -> "Aparência do estado intermediário."
                PreviewState.Bad -> "Aparência do aviso Recusar. É um selo informativo: não existe botão para rejeitar corrida."
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        InfoNote(
            "Esta é uma prévia visual com oferta de exemplo. Sem critérios confirmados e política de combinação, " +
                "o aplicativo não decide verde, amarelo ou vermelho para ofertas reais. Não há integração com 99Pop.",
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onOpen("combinacao") }, modifier = Modifier.weight(1f)) { Text("Combinar critérios") }
            OutlinedButton(onClick = { onOpen("cartao") }, modifier = Modifier.weight(1f)) { Text("Personalizar cartão") }
        }
        Button(onClick = { onOpen("editar-criterio") }, modifier = Modifier.fillMaxWidth()) { Text("Cadastrar preferências") }
    }
}
