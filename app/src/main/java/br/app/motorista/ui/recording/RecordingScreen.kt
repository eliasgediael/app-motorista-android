package br.app.motorista.ui.recording

import android.Manifest
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.app.motorista.copilot.CopilotController
import br.app.motorista.domain.copilot.FeatureStatus
import br.app.motorista.domain.journey.JourneyClock
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SectionTitle
import br.app.motorista.ui.common.SurfaceCard
import kotlinx.coroutines.delay

/**
 * Gravação de segurança (telas/32-camera). Etapa VISÍVEL exigida pelo Android: o início da gravação
 * acontece aqui, com consentimento e permissão, e só vira Ativo quando o serviço confirma a câmera aberta.
 */
@Composable
fun RecordingScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val state by CopilotController.state.collectAsState()
    val rec = state.recording
    var consent by rememberSaveable { mutableStateOf(false) }
    var lens by rememberSaveable { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) CopilotController.startRecordingService(context, lens) else CopilotController.onCameraPermissionDenied()
    }

    // Saiu sem iniciar: o pedido pendente é cancelado e nada é gravado.
    DisposableEffect(Unit) {
        onDispose { CopilotController.cancelRecordingRequest() }
    }

    val tick by produceState(0L, rec.status) {
        while (rec.status == FeatureStatus.Active) {
            value = SystemClock.elapsedRealtime()
            delay(1000)
        }
    }

    ScreenScaffold("Gravação de segurança", onBack, onOpen) {
        InfoNote(
            "Gravação sempre visível: notificação com cronômetro e botão Parar. Vídeo sem áudio, salvo só neste aparelho, " +
                "na pasta do aplicativo. Nunca grava escondido nem começa sozinha. O Android exige que esta etapa aconteça " +
                "com o aplicativo aberto.",
        )

        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Estado", style = MaterialTheme.typography.titleMedium)
                val started = state.recordingStartedAt
                Text(
                    when (rec.status) {
                        FeatureStatus.Active -> {
                            val elapsed = if (started != null) (maxOf(tick, SystemClock.elapsedRealtime()) - started).coerceAtLeast(0) else 0
                            "Gravando · " + JourneyClock.format(elapsed)
                        }
                        else -> rec.status.label + (rec.detail?.let { " · $it" } ?: "")
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.lastRecordingPath?.let {
                    Text("Último arquivo: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        when (rec.status) {
            FeatureStatus.Active, FeatureStatus.Stopping -> {
                OutlinedButton(
                    onClick = { CopilotController.stopRecording(context) },
                    enabled = rec.status == FeatureStatus.Active,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (rec.status == FeatureStatus.Active) "Parar gravação" else "Parando…") }
            }
            else -> {
                SectionTitle("Câmera")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = lens == CameraSelector.LENS_FACING_BACK,
                        onClick = { lens = CameraSelector.LENS_FACING_BACK },
                        label = { Text("Traseira (via)") },
                    )
                    FilterChip(
                        selected = lens == CameraSelector.LENS_FACING_FRONT,
                        onClick = { lens = CameraSelector.LENS_FACING_FRONT },
                        label = { Text("Frontal (cabine)") },
                    )
                }
                Row(
                    Modifier.fillMaxWidth().clickable { consent = !consent },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = consent, onCheckedChange = { consent = it })
                    Text(
                        "Entendo que a gravação é visível e que devo respeitar as regras da plataforma e a legislação aplicáveis.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rec.status == FeatureStatus.Error || rec.status == FeatureStatus.PermissionNeeded) {
                    Text(rec.detail ?: rec.status.label, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = { permission.launch(Manifest.permission.CAMERA) },
                    enabled = consent && rec.status != FeatureStatus.Starting,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (rec.status == FeatureStatus.Starting) "Abrindo a câmera…" else "Iniciar gravação") }
            }
        }

        InfoNote("Não validado em aparelho real ainda: abertura da câmera em segundo plano, câmera ocupada e pouco espaço.")
    }
}
