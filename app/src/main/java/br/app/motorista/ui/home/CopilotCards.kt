package br.app.motorista.ui.home

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Traffic
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.app.motorista.copilot.ControllerActions
import br.app.motorista.copilot.CopilotController
import br.app.motorista.domain.copilot.FeatureStatus
import br.app.motorista.domain.journey.JourneyClock
import br.app.motorista.domain.journey.JourneyStatus
import br.app.motorista.ui.common.IconBadge
import br.app.motorista.ui.common.SurfaceCard
import kotlinx.coroutines.delay

/**
 * Cartões do Painel ligados ao mesmo estado do menu flutuante ([CopilotController]).
 * Nenhum interruptor muda sozinho: o estado só vira Ativo quando o serviço confirma.
 */
@Composable
fun CopilotCards(onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val actions = remember { ControllerActions(context) }
    val state by CopilotController.state.collectAsState()
    var confirmShutdown by remember { mutableStateOf(false) }

    val ticking = state.recordingStartedAt != null || state.journey.status == JourneyStatus.Running
    val tick by produceState(0L, ticking) {
        while (ticking) {
            value = SystemClock.elapsedRealtime()
            delay(1000)
        }
    }

    // Copiloto (botão flutuante)
    Card2(
        icon = Icons.Outlined.GridView,
        title = "Copiloto",
        subtitle = if (state.copilotRunning) "Botão flutuante ativo" else "Botão flutuante desligado",
        onSettings = { onOpen("atalhos") },
    ) {
        Switch(
            checked = state.copilotRunning,
            onCheckedChange = { on ->
                if (on) onOpen("atalhos") else if (actions.activeResources().isEmpty()) actions.shutdownCopilot() else confirmShutdown = true
            },
        )
    }

    // Análise de corridas: liga o leitor de ofertas (acessibilidade, somente leitura)
    val an = state.analysis
    Card2(
        icon = Icons.Outlined.Traffic,
        title = "Análise de corridas",
        subtitle = when (an.status) {
            FeatureStatus.Active -> "Ativa · layout dos apps ainda não validado"
            FeatureStatus.Off -> "Desligada"
            else -> an.detail ?: an.status.label
        },
        onSettings = { onOpen("leitura") },
    ) {
        when (an.status) {
            FeatureStatus.PermissionNeeded -> OutlinedButton(onClick = { actions.openAccessibilitySettings() }) { Text("Permitir") }
            FeatureStatus.Starting, FeatureStatus.Stopping -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            else -> Switch(checked = an.status == FeatureStatus.Active, onCheckedChange = { actions.setAnalysis(it) })
        }
    }

    // Gravação
    val rec = state.recording
    Card2(
        icon = Icons.Outlined.PhotoCamera,
        title = "Gravação de segurança",
        subtitle = when (rec.status) {
            FeatureStatus.Active -> {
                val started = state.recordingStartedAt
                val elapsed = if (started != null) (maxOf(tick, SystemClock.elapsedRealtime()) - started).coerceAtLeast(0) else 0
                "Gravando · " + JourneyClock.format(elapsed)
            }
            else -> rec.detail ?: rec.status.label
        },
        onSettings = { onOpen("camera") },
    ) {
        when (rec.status) {
            FeatureStatus.Active -> OutlinedButton(onClick = { actions.stopRecording() }) { Text("Parar") }
            FeatureStatus.Starting, FeatureStatus.Stopping -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            FeatureStatus.PermissionNeeded -> OutlinedButton(onClick = { actions.requestRecording() }) { Text("Permitir") }
            else -> OutlinedButton(onClick = { actions.requestRecording() }) { Text("Iniciar") }
        }
    }

    // Avisos por voz
    val voice = state.voice
    Card2(
        icon = Icons.Outlined.VolumeUp,
        title = "Avisos por voz",
        subtitle = when (voice.status) {
            FeatureStatus.Active -> "Ativados"
            FeatureStatus.Off -> "Desativados"
            else -> voice.detail ?: voice.status.label
        },
        onSettings = { onOpen("atalhos") },
    ) {
        when (voice.status) {
            FeatureStatus.Starting, FeatureStatus.Stopping -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            FeatureStatus.Unavailable, FeatureStatus.Error, FeatureStatus.PermissionNeeded, FeatureStatus.NotImplemented ->
                OutlinedButton(onClick = { actions.setVoice(true) }, enabled = voice.status != FeatureStatus.NotImplemented) { Text("Tentar") }
            else -> Switch(checked = voice.status == FeatureStatus.Active, onCheckedChange = { actions.setVoice(it) })
        }
    }

    // Jornada
    val j = state.journey
    Card2(
        icon = Icons.Outlined.Schedule,
        title = "Jornada",
        subtitle = j.status.label + if (j.status == JourneyStatus.NotStarted) "" else " · " + JourneyClock.format(JourneyClock.workedMs(j, System.currentTimeMillis() + tick * 0)),
        onSettings = { onOpen("jornada") },
    ) {
        when (j.status) {
            JourneyStatus.NotStarted, JourneyStatus.Ended -> OutlinedButton(onClick = { actions.journeyStart() }) { Text("Iniciar") }
            JourneyStatus.Running -> OutlinedButton(onClick = { actions.journeyPause() }) { Text("Pausar") }
            JourneyStatus.Paused -> OutlinedButton(onClick = { actions.journeyResume() }) { Text("Retomar") }
        }
    }

    if (confirmShutdown) {
        AlertDialog(
            onDismissRequest = { confirmShutdown = false },
            title = { Text("Desligar o copiloto?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    actions.activeResources().forEach { Text("• $it") }
                    Text("Nenhum registro será apagado.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmShutdown = false
                    actions.shutdownCopilot()
                }) { Text("Desligar") }
            },
            dismissButton = { TextButton(onClick = { confirmShutdown = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Card2(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onSettings: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    SurfaceCard {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Configurar $title", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing()
        }
    }
}
