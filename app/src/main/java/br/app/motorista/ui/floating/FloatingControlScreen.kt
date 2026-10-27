package br.app.motorista.ui.floating

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import br.app.motorista.R
import br.app.motorista.copilot.ControllerActions
import br.app.motorista.copilot.CopilotController
import br.app.motorista.copilot.MenuEntry
import br.app.motorista.domain.copilot.FeatureStatus
import br.app.motorista.floating.FloatingConfig
import br.app.motorista.floating.FloatingControlService
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SectionTitle
import br.app.motorista.ui.common.SurfaceCard

/**
 * Controle flutuante (telas/08-atalhos). Liga/desliga o copiloto (serviço real do botão), define o que
 * aparece no menu e a opacidade do botão. Sem integração com 99Pop.
 */
@Composable
fun FloatingControlScreen(
    buttonOpacity: Float,
    onButtonOpacity: (Float) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val context = LocalContext.current
    val actions = remember { ControllerActions(context) }
    val running by FloatingControlService.running.collectAsState()
    val state by CopilotController.state.collectAsState()
    var confirmShutdown by remember { mutableStateOf(false) }

    // Reconsulta a permissão ao voltar da tela de configurações do sistema.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        CopilotController.refreshPermissions()
        onPauseOrDispose { }
    }
    val canOverlay = refresh >= 0 && Settings.canDrawOverlays(context)

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Com ou sem aviso visível, o serviço pode iniciar; a notificação só pode ficar oculta.
        FloatingControlService.start(context)
    }

    fun activate() {
        if (!Settings.canDrawOverlays(context)) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }
        val needsNotification = Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsNotification) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else FloatingControlService.start(context)
    }

    ScreenScaffold("Controle flutuante", onBack, onOpen) {
        SectionTitle("Botão flutuante")
        SurfaceCard {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).background(Color(0xFF159B67), CircleShape), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.no_ponto_simbolo), contentDescription = null, modifier = Modifier.size(56.dp))
                }
                Column(Modifier.padding(start = 14.dp)) {
                    Text("Copiloto No Ponto", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        (if (running) "Ativo na tela" else "Desligado") + " · verde fixo · abre os controles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (canOverlay) "Permissão de sobreposição: concedida" else "Permissão de sobreposição: não concedida",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (!running) {
            Button(onClick = { activate() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (canOverlay) "Ativar copiloto" else "Conceder permissão e ativar")
            }
        } else {
            OutlinedButton(
                onClick = { if (actions.activeResources().isEmpty()) actions.shutdownCopilot() else confirmShutdown = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Desligar copiloto") }
        }

        SurfaceCard {
            Column {
                ToggleRow(
                    title = "Exibir botão",
                    subtitle = "Ocultar o botão não desliga análise, voz, gravação nem jornada",
                    checked = state.buttonVisible,
                    enabled = running,
                    onChange = { CopilotController.setButtonVisible(it) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Posição e tamanho", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Arraste o botão na tela. Ajuste de tamanho: em desenvolvimento.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        SectionTitle("O que aparece no menu")
        SurfaceCard {
            Column {
                MenuEntry.entries.forEachIndexed { index, entry ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val implemented = !(entry == MenuEntry.Analysis && state.analysis.status == FeatureStatus.NotImplemented)
                    ToggleRow(
                        title = entry.label,
                        subtitle = when (entry) {
                            MenuEntry.Analysis -> if (implemented) "Ligar / desligar o serviço de análise" else "Não implementado: " + (state.analysis.detail ?: "")
                            MenuEntry.Recording -> "Iniciar / parar, com estado visível"
                            MenuEntry.Voice -> "Ligar / desligar anúncios por voz"
                            MenuEntry.Journey -> "Iniciar / pausar / retomar"
                        },
                        checked = entry in state.menuEntries,
                        enabled = implemented,
                        onChange = { CopilotController.setMenuEntry(entry, it) },
                    )
                }
            }
        }
        Text(
            "Mostrar um item no menu não inicia o serviço: o item só oferece o controle.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionTitle("Ações do menu")
        SurfaceCard {
            Column {
                ToggleRow(
                    title = "Ocultar balão",
                    subtitle = "Esconde somente o balão, não a análise. Fica oculto até você mostrar de novo",
                    checked = state.balloonHidden,
                    enabled = true,
                    onChange = { CopilotController.setBalloonHidden(it) },
                )
            }
        }

        SurfaceCard {
            Column(Modifier.padding(16.dp)) {
                Text("Opacidade do botão ${(buttonOpacity * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = buttonOpacity,
                    onValueChange = onButtonOpacity,
                    valueRange = FloatingConfig.BUTTON_OPACITY_MIN..1f,
                )
                Text(
                    "Independente da opacidade do balão (Estilo do cartão). A cor do botão é fixa e não muda com a avaliação.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Button(onClick = { onOpen("menu-flutuante") }, modifier = Modifier.fillMaxWidth()) { Text("Ver menu completo") }

        InfoNote(
            "O botão e o menu ficam dentro da área útil da tela (fora das barras do sistema). Isso não protege botões " +
                "de outros aplicativos: nenhuma proteção dos botões do 99Pop foi validada. Serviço ausente aparece como " +
                "não implementado; permissão negada não é estado ligado.",
        )
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
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
