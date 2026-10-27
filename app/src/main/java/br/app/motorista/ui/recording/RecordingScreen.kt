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
import br.app.motorista.domain.calc.valueOrNull
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

        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Gravar áudio", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Text(
                    "Indisponível nesta versão: a gravação é só de vídeo. Áudio exige permissão de microfone, consentimento das pessoas e conferência da política de áudio da plataforma.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val files = br.app.motorista.ui.settings.recordingFiles(context)
        var days by rememberSaveable { mutableStateOf(br.app.motorista.data.AppData.setting("recordingRetentionDays") ?: "") }
        var cleaned by rememberSaveable { mutableStateOf<String?>(null) }
        val daysParsed = br.app.motorista.domain.calc.OfferParsing.wholeNumber(days)
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Armazenamento e retenção", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Text(files.size.toString() + " gravação(ões), " + (files.sumOf { it.length() } / (1024 * 1024)) + " MB neste aparelho.", style = MaterialTheme.typography.bodySmall)
                br.app.motorista.ui.common.TextInput(
                    "Apagar gravações com mais de (dias)", days, { days = it },
                    keyboard = androidx.compose.ui.text.input.KeyboardType.Number,
                    error = if (daysParsed is br.app.motorista.domain.calc.Parsed.Invalid) "Número inteiro" else null,
                )
                OutlinedButton(
                    enabled = daysParsed is br.app.motorista.domain.calc.Parsed.Ok,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val d = daysParsed.valueOrNull() ?: return@OutlinedButton
                        br.app.motorista.data.AppData.putSetting("recordingRetentionDays", d.toString())
                        val limit = System.currentTimeMillis() - d * 86_400_000L
                        val old = files.filter { it.lastModified() < limit }
                        old.forEach { it.delete() }
                        cleaned = old.size.toString() + " gravação(ões) apagada(s)."
                    },
                ) { Text("Apagar as mais antigas agora") }
                cleaned?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text("Nada é apagado sem você tocar no botão. Se não definir os dias, as gravações ficam até você excluir.", style = MaterialTheme.typography.bodySmall)
            }
        }
        OutlinedButton(onClick = { onOpen("gravacoes") }, modifier = Modifier.fillMaxWidth()) { Text("Ver biblioteca") }
        InfoNote("Não validado em aparelho real ainda: abertura da câmera em segundo plano, câmera ocupada e pouco espaço.")
    }
}
