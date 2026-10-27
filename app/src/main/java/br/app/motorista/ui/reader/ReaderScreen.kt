package br.app.motorista.ui.reader

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import br.app.motorista.copilot.CopilotController
import br.app.motorista.data.AppData
import br.app.motorista.domain.copilot.FeatureStatus
import br.app.motorista.domain.offer.Platform
import br.app.motorista.reader.OfferReaderService
import br.app.motorista.reader.ReaderState
import br.app.motorista.ui.common.ConfirmDialog
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SectionTitle
import br.app.motorista.ui.common.SurfaceCard

/**
 * Leitura de ofertas: leitor SOMENTE LEITURA por acessibilidade. Mapeia o app de corrida, mostra o que o
 * leitor vê e permite capturar AMOSTRAS de tela para escrever/validar o interpretador de cada plataforma.
 */
@Composable
fun ReaderScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val connected by ReaderState.connected.collectAsState()
    val lastReading by ReaderState.lastReading.collectAsState()
    val lastPkg by ReaderState.lastPackage.collectAsState()
    val state by CopilotController.state.collectAsState()
    val settings by AppData.settings.items.collectAsState()
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    val samples = if (refresh >= 0) OfferReaderService.samples(context) else emptyList()
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    var sharing by rememberSaveable { mutableStateOf<String?>(null) }

    ScreenScaffold("Leitura de ofertas", onBack, onOpen) {
        InfoNote(
            "O leitor só LÊ o texto visível do aplicativo que você mapear. Não toca, não faz gesto, não aceita nem recusa corrida e não envia nada. " +
                "Usar um leitor de tela em outro aplicativo pode contrariar os termos da plataforma (a 99 prevê bloquear a conta); a decisão e o risco são seus.",
        )
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Estado", fontWeight = FontWeight.SemiBold)
                Text("Serviço de acessibilidade: " + if (connected) "conectado" else "desligado")
                Text("Análise: " + state.analysis.status.label + (state.analysis.detail?.let { " · $it" } ?: ""))
                Text("Última leitura: $lastReading", style = MaterialTheme.typography.bodySmall)
                Text("Último app visto: " + (lastPkg ?: "nenhum ainda"), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (!connected) {
            Button(
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Abrir configurações de acessibilidade") }
            Text("Procure por No Ponto, ative e confirme o aviso do Android. Depois volte aqui.", style = MaterialTheme.typography.bodySmall)
        }

        SectionTitle("Mapear o aplicativo de corrida")
        Text(
            "1. Abra o aplicativo de corrida (ele aparece em 'Último app visto'). 2. Volte aqui e toque em Usar como 99Pop ou Usar como Uber.",
            style = MaterialTheme.typography.bodySmall,
        )
        Platform.entries.forEach { p ->
            val mapped = settings.firstOrNull { it.id == OfferReaderService.packageSetting(p) }?.value
            SurfaceCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(p.label + ": " + (mapped ?: "não mapeado"), fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(enabled = lastPkg != null, onClick = { lastPkg?.let { AppData.putSetting(OfferReaderService.packageSetting(p), it) } }) {
                            Text("Usar o último app visto")
                        }
                        if (mapped != null) TextButton(onClick = { AppData.removeSetting(OfferReaderService.packageSetting(p)) }) { Text("Remover") }
                    }
                }
            }
        }

        SectionTitle("Amostras de tela (diagnóstico)")
        Text(
            "Com o aplicativo de corrida aberto numa tela de oferta, use o menu do botão flutuante > Capturar amostra da tela. " +
                "O arquivo guarda os textos visíveis e os identificadores dos elementos, só neste aparelho. Pode conter dados de passageiro: revise e anonimize antes de compartilhar. " +
                "Com amostras reais é possível escrever e validar o interpretador de cada plataforma.",
            style = MaterialTheme.typography.bodySmall,
        )
        if (samples.isEmpty()) Text("Nenhuma amostra.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        samples.forEach { f ->
            SurfaceCard {
                Row(Modifier.padding(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(f.name, fontWeight = FontWeight.SemiBold)
                        Text((f.length() / 1024).toString() + " KB", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { sharing = f.name }) { Text("Compartilhar") }
                    TextButton(onClick = { deleting = f.name }) { Text("Excluir") }
                }
            }
        }
        InfoNote(
            "Interpretadores de 99Pop e Uber: AINDA NÃO validados. Enquanto isso, com a Análise ligada o leitor apenas informa 'layout não validado' e nenhuma oferta é inventada.",
        )
    }

    deleting?.let { name ->
        ConfirmDialog("Excluir amostra?", "$name será apagada.", "Excluir", {
            samples.firstOrNull { it.name == name }?.delete()
            deleting = null
            refresh++
        }, { deleting = null })
    }
    sharing?.let { name ->
        ConfirmDialog(
            "Compartilhar amostra?",
            "O arquivo pode conter nomes, endereços e valores da tela. Revise o conteúdo antes e anonimize o que for pessoal. Ele só sai do aparelho se você escolher um destino.",
            "Escolher destino",
            {
                samples.firstOrNull { it.name == name }?.let { f ->
                    val text = f.readText()
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                    runCatching { context.startActivity(Intent.createChooser(send, "Compartilhar amostra").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                sharing = null
            },
            { sharing = null },
        )
    }
}
