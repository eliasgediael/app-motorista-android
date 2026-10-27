package br.app.motorista.ui.floating

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import br.app.motorista.floating.FloatingConfig
import br.app.motorista.floating.FloatingControlService
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SectionTitle
import br.app.motorista.ui.common.SurfaceCard

/** Controle flutuante (telas/08-atalhos): ativa o botão do próprio app. Sem integração com 99Pop. */
@Composable
fun FloatingControlScreen(
    buttonOpacity: Float,
    onButtonOpacity: (Float) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val context = LocalContext.current
    val running by FloatingControlService.running.collectAsState()

    // Reconsulta a permissão ao voltar da tela de configurações do sistema.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
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
        InfoNote(
            "Este botão pertence ao próprio aplicativo. Ele não lê 99, Uber ou inDrive, não toca em outros aplicativos " +
                "e não aceita nem recusa corridas. Os balões que ele mostra são exemplos.",
        )

        SurfaceCard {
            Column(Modifier.padding(16.dp)) {
                Text("Estado", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (running) "Botão ativo na tela." else "Botão desligado.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (canOverlay) "Permissão de sobreposição: concedida." else "Permissão de sobreposição: não concedida.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!running) {
            Button(onClick = { activate() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (canOverlay) "Ativar botão flutuante" else "Conceder permissão e ativar")
            }
        } else {
            OutlinedButton(onClick = { FloatingControlService.stop(context) }, modifier = Modifier.fillMaxWidth()) {
                Text("Desligar botão flutuante")
            }
        }

        SectionTitle("Aparência do botão")
        SurfaceCard {
            Column(Modifier.padding(16.dp)) {
                Text("Opacidade do botão ${(buttonOpacity * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = buttonOpacity,
                    onValueChange = onButtonOpacity,
                    valueRange = FloatingConfig.BUTTON_OPACITY_MIN..1f,
                )
                Text(
                    "O botão é sempre verde com símbolo branco; a cor não muda com a avaliação. " +
                        "Esta opacidade é só do botão. A opacidade do balão fica em Estilo do cartão.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SectionTitle("Controles do botão")
        Text(
            "Toque no botão para abrir o menu. Arraste para mover.\n" +
                "• Última oferta analisada (some se a oferta expirar ou for trocada)\n" +
                "• Balão verde, amarelo ou vermelho de exemplo (some em 8 s)\n" +
                "• Ocultar balão\n" +
                "• Abrir aplicativo\n" +
                "• Desligar botão",
            style = MaterialTheme.typography.bodyLarge,
        )
        InfoNote(
            "O botão e o menu ficam dentro da área útil da tela (fora das barras do sistema). Isso não protege botões " +
                "de outros aplicativos: nenhuma proteção dos botões do 99Pop foi validada. A permissão de sobreposição " +
                "só é pedida por você, nas configurações do sistema.",
        )
    }
}
