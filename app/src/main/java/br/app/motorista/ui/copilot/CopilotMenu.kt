package br.app.motorista.ui.copilot

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Traffic
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.app.motorista.copilot.CopilotActions
import br.app.motorista.copilot.CopilotUiState
import br.app.motorista.copilot.FeatureUi
import br.app.motorista.copilot.MenuEntry
import br.app.motorista.domain.copilot.FeatureStatus
import br.app.motorista.domain.journey.JourneyClock
import br.app.motorista.domain.journey.JourneyStatus
import kotlinx.coroutines.delay

private val Danger = Color(0xFFB3261E)
private val DangerSoft = Color(0xFFFFEFEE)

/**
 * Menu operacional do copiloto (telas 45 e 51–56). Os estados vêm do serviço real via [CopilotUiState];
 * nada aqui liga um recurso por conta própria: cada botão pede a ação e o estado só muda na confirmação.
 */
@Composable
fun CopilotMenu(
    state: CopilotUiState,
    actions: CopilotActions,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp? = null,
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    val needsTick = state.recordingStartedAt != null || state.journey.status == JourneyStatus.Running || state.journey.status == JourneyStatus.Paused
    val tick by produceState(0L, needsTick) {
        while (needsTick) {
            value = SystemClock.elapsedRealtime()
            delay(1000)
        }
    }

    val scroll = rememberScrollState()
    Column(
        modifier
            .background(Color.White, RoundedCornerShape(24.dp))
            // Rolagem própria só no menu flutuante (altura limitada); dentro de uma tela já rolável não pode haver rolagem aninhada.
            .let { if (maxHeight != null) it.heightIn(max = maxHeight).verticalScroll(scroll) else it }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Copiloto", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
                Text("No Ponto", style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
            IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Configurar o menu", tint = Ink) }
        }

        if (state.recording.status == FeatureStatus.Active) {
            Text(
                "● GRAVANDO",
                color = Danger,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth().background(DangerSoft, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        val entries = MenuEntry.entries.filter { it in state.menuEntries }
        entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { entry ->
                    Box2(Modifier.weight(1f)) {
                        when (entry) {
                            MenuEntry.Analysis -> AnalysisCard(state.analysis)
                            MenuEntry.Recording -> RecordingCard(state, actions, tick)
                            MenuEntry.Voice -> VoiceCard(state.voice, actions)
                            MenuEntry.Journey -> JourneyCard(state, actions, tick)
                        }
                    }
                }
                if (pair.size == 1) Column(Modifier.weight(1f)) {}
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { actions.setBalloonHidden(!state.balloonHidden) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
            ) { Text(if (state.balloonHidden) "Mostrar balão" else "Ocultar balão", color = Ink) }
            OutlinedButton(
                onClick = { actions.openApp() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Abrir aplicativo", color = Ink) }
        }

        if (!confirming) {
            TextButton(onClick = { confirming = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Desligar copiloto", color = Danger, fontWeight = FontWeight.Bold)
            }
        } else {
            ShutdownConfirmation(actions, onCancel = { confirming = false })
        }
    }
}

private val Ink = Color(0xFF15211B)
private val Muted = Color(0xFF5F6E66)

@Composable
private fun Box2(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .background(Color(0xFFFAFCFB), RoundedCornerShape(18.dp))
            .padding(12.dp),
    ) { content() }
}

@Composable
private fun CardHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(title, fontWeight = FontWeight.Bold, color = Ink, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun StatusLine(text: String, color: Color = Muted) {
    Text(text, color = color, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
}

@Composable
private fun DisabledChip(text: String) {
    Text(
        text,
        color = Muted,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.background(Color(0xFFEDEFEE), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

@Composable
private fun ActionButton(text: String, enabled: Boolean = true, danger: Boolean = false, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (danger) Danger.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (danger) Danger else Ink),
        modifier = Modifier.heightIn(min = 48.dp),
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun AnalysisCard(f: FeatureUi) {
    CardHeader(Icons.Outlined.Traffic, "Análise de corridas")
    // Sem fonte real de ofertas, não existe serviço a ligar: nenhum switch é oferecido.
    StatusLine("Serviço indisponível")
    DisabledChip(if (f.status == FeatureStatus.NotImplemented) "Não implementado" else f.status.label)
    if (f.detail != null) Text(f.detail, color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun RecordingCard(state: CopilotUiState, actions: CopilotActions, tick: Long) {
    val f = state.recording
    CardHeader(Icons.Outlined.PhotoCamera, "Gravação")
    when (f.status) {
        FeatureStatus.Off -> {
            StatusLine("Desligada")
            ActionButton("Iniciar gravação") { actions.requestRecording() }
        }
        FeatureStatus.PermissionNeeded -> {
            StatusLine(f.detail ?: "Câmera não autorizada")
            ActionButton("Permitir") { actions.requestRecording() }
        }
        FeatureStatus.Starting -> {
            StatusLine(f.detail ?: "Iniciando")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                ActionButton("Cancelar") { actions.cancelRecording() }
            }
        }
        FeatureStatus.Active -> {
            val started = state.recordingStartedAt
            val elapsed = if (started != null) (maxOf(tick, SystemClock.elapsedRealtime()) - started).coerceAtLeast(0) else 0
            StatusLine("Gravando · " + JourneyClock.format(elapsed), Danger)
            ActionButton("Parar gravação", danger = true) { actions.stopRecording() }
        }
        FeatureStatus.Stopping -> {
            StatusLine(f.detail ?: "Parando")
            ActionButton("Parando…", enabled = false) {}
        }
        FeatureStatus.Error, FeatureStatus.Unavailable -> {
            StatusLine(f.detail ?: f.status.label, Danger)
            ActionButton("Tentar de novo") { actions.requestRecording() }
        }
        FeatureStatus.NotImplemented -> {
            StatusLine("Serviço indisponível")
            DisabledChip("Não implementada")
        }
    }
}

@Composable
private fun VoiceCard(f: FeatureUi, actions: CopilotActions) {
    CardHeader(Icons.Outlined.VolumeUp, "Avisos por voz")
    when (f.status) {
        FeatureStatus.Off -> {
            StatusLine("Desativados")
            Switch(checked = false, onCheckedChange = { actions.setVoice(true) })
        }
        FeatureStatus.Starting -> {
            StatusLine("Iniciando")
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        }
        FeatureStatus.Active -> {
            StatusLine("Ativados")
            Switch(checked = true, onCheckedChange = { actions.setVoice(false) })
        }
        FeatureStatus.Stopping -> {
            StatusLine("Parando")
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        }
        FeatureStatus.Unavailable, FeatureStatus.Error, FeatureStatus.PermissionNeeded -> {
            StatusLine(f.detail ?: f.status.label, Danger)
            ActionButton("Tentar de novo") { actions.setVoice(true) }
        }
        FeatureStatus.NotImplemented -> {
            StatusLine("Serviço indisponível")
            DisabledChip("Não implementado")
        }
    }
}

@Composable
private fun JourneyCard(state: CopilotUiState, actions: CopilotActions, tick: Long) {
    val j = state.journey
    CardHeader(Icons.Outlined.Schedule, "Jornada")
    // tick força a recomposição a cada segundo enquanto a jornada corre.
    val worked = JourneyClock.format(JourneyClock.workedMs(j, System.currentTimeMillis() + tick * 0))
    when (j.status) {
        JourneyStatus.NotStarted, JourneyStatus.Ended -> {
            StatusLine(if (j.status == JourneyStatus.Ended) "Encerrada · " + worked else "Não iniciada")
            ActionButton("Iniciar") { actions.journeyStart() }
        }
        JourneyStatus.Running -> {
            StatusLine("Em andamento · " + worked)
            ActionButton("Pausar") { actions.journeyPause() }
        }
        JourneyStatus.Paused -> {
            StatusLine("Pausada · " + worked)
            ActionButton("Retomar") { actions.journeyResume() }
        }
    }
}

@Composable
private fun ShutdownConfirmation(actions: CopilotActions, onCancel: () -> Unit) {
    val active = actions.activeResources()
    Column(
        Modifier.fillMaxWidth().background(DangerSoft, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Desligar o copiloto?", fontWeight = FontWeight.Bold, color = Ink)
        if (active.isEmpty()) {
            Text("Nenhum recurso ativo. O botão e o menu serão removidos.", color = Muted, style = MaterialTheme.typography.bodySmall)
        } else {
            active.forEach { Text("• $it", color = Ink, style = MaterialTheme.typography.bodySmall) }
            Text("Nenhum registro será apagado.", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancelar", color = Ink) }
            OutlinedButton(
                onClick = {
                    actions.shutdownCopilot()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger),
            ) { Text("Desligar", fontWeight = FontWeight.Bold) }
        }
    }
}
