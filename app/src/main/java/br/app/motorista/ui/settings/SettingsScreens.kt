package br.app.motorista.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import br.app.motorista.BuildConfig
import br.app.motorista.copilot.CopilotController
import br.app.motorista.data.AppData
import br.app.motorista.data.ThemeMode
import br.app.motorista.domain.copilot.FeatureStatus
import br.app.motorista.ui.common.ChoiceChips
import br.app.motorista.ui.common.ConfirmDialog
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SectionTitle
import br.app.motorista.ui.common.SurfaceCard
import java.io.File
import java.time.ZoneId

/** Aparência e preferências (telas 35 e 50). O tema escuro preserva as cores funcionais do balão. */
@Composable
fun PreferencesScreen(themeMode: ThemeMode, onTheme: (ThemeMode) -> Unit, dark: Boolean, onBack: () -> Unit, onOpen: (String) -> Unit) {
    ScreenScaffold(if (dark) "Aparência escura" else "Aparência e preferências", onBack, onOpen) {
        SectionTitle("Tema")
        ChoiceChips(ThemeMode.entries, themeMode, { it.label }, onTheme)
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Prévia da superfície", fontWeight = FontWeight.SemiBold)
                Text("Texto principal sobre a superfície atual.")
                Text("Texto secundário.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "As cores verde, amarela e vermelha do balão não mudam com o tema.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SectionTitle("Atalhos")
        OutlinedButton(onClick = { onOpen("plataformas") }, modifier = Modifier.fillMaxWidth()) { Text("Plataformas de corrida") }
        OutlinedButton(onClick = { onOpen("conta") }, modifier = Modifier.fillMaxWidth()) { Text("Conta e dispositivos") }
        OutlinedButton(onClick = { onOpen("permissoes") }, modifier = Modifier.fillMaxWidth()) { Text("Permissões") }
        SectionTitle("Outras preferências")
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Fuso horário: " + ZoneId.systemDefault().id + " (do sistema)")
                Text("Tamanho do texto: segue a configuração do sistema.")
                Text("Lembretes e fechamento diário configurável: não implementado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun Context.has(permission: String) = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

/** Permissões e funcionamento (telas/34-permissoes): finalidade, efeito de negar e como revogar, com estado real. */
@Composable
fun PermissionsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        CopilotController.refreshPermissions()
        onPauseOrDispose { }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    val state by CopilotController.state.collectAsState()

    fun appSettings() {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    ScreenScaffold("Permissões e funcionamento", onBack, onOpen) {
        val overlay = tick >= 0 && Settings.canDrawOverlays(context)
        PermissionCard(
            title = "Sobreposição (botão flutuante)",
            granted = overlay,
            purpose = "Mostrar o botão, o menu e o balão sobre outros aplicativos.",
            ifDenied = "O botão flutuante não aparece. O restante do app funciona.",
            action = if (overlay) "Gerenciar" else "Permitir",
        ) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        val camera = tick >= 0 && context.has(Manifest.permission.CAMERA)
        PermissionCard(
            title = "Câmera (gravação de segurança)",
            granted = camera,
            purpose = "Gravar vídeo visível, somente por ação sua. Sem áudio.",
            ifDenied = "A gravação não inicia. Nada é gravado em segundo plano nem escondido.",
            action = if (camera) "Gerenciar" else "Permitir",
        ) { if (camera) appSettings() else cameraLauncher.launch(Manifest.permission.CAMERA) }
        if (Build.VERSION.SDK_INT >= 33) {
            val notif = tick >= 0 && context.has(Manifest.permission.POST_NOTIFICATIONS)
            PermissionCard(
                title = "Notificações",
                granted = notif,
                purpose = "Mostrar o aviso obrigatório do copiloto e da gravação (com cronômetro e botão Parar).",
                ifDenied = "Os serviços podem funcionar, mas o aviso fica oculto. Prefira permitir para ver a gravação.",
                action = if (notif) "Gerenciar" else "Permitir",
            ) { if (notif) appSettings() else notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
        PermissionCard(
            title = "Voz (síntese de fala)",
            granted = state.voice.status == FeatureStatus.Active,
            purpose = "Falar avisos de análise. Usa o mecanismo de voz do aparelho; não precisa de permissão.",
            ifDenied = state.voice.detail ?: "Estado atual: " + state.voice.status.label,
            action = "Abrir ajustes de voz",
            statusText = state.voice.status.label,
        ) {
            runCatching {
                context.startActivity(Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure { appSettings() }
        }
        PermissionCard(
            title = "Leitura da tela de outros aplicativos",
            granted = false,
            purpose = "Ler ofertas de 99Pop/Uber automaticamente.",
            ifDenied = "Não implementado e não solicitado. Depende de decisão sua e dos termos de cada plataforma.",
            action = null,
            statusText = "Não solicitada",
        ) {}
        PermissionCard(
            title = "Microfone, localização, mídia, contatos",
            granted = false,
            purpose = "O aplicativo não usa.",
            ifDenied = "Nada a revogar.",
            action = null,
            statusText = "Não usada",
        ) {}
        OutlinedButton(onClick = { appSettings() }, modifier = Modifier.fillMaxWidth()) { Text("Abrir configurações do aplicativo") }
        InfoNote("Revogar uma permissão nas configurações do sistema atualiza o estado aqui; a gravação e o voz nunca retomam sozinhos.")
    }
}

@Composable
private fun PermissionCard(
    title: String,
    granted: Boolean,
    purpose: String,
    ifDenied: String,
    action: String?,
    statusText: String? = null,
    onAction: () -> Unit,
) {
    SurfaceCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(
                    statusText ?: if (granted) "Concedida" else "Não concedida",
                    color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text("Para quê: $purpose", style = MaterialTheme.typography.bodySmall)
            Text("Se negar: $ifDenied", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (action != null) OutlinedButton(onClick = onAction) { Text(action) }
        }
    }
}

/** Privacidade e dados (telas/39-privacidade): o que é guardado, onde, e como apagar. */
@Composable
fun PrivacyScreen(onWipePrefs: () -> Unit, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    var confirmWipe by remember { mutableStateOf(false) }
    var confirmRecordings by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val txs by AppData.transactions.items.collectAsState()
    val files = recordingFiles(context)

    ScreenScaffold("Privacidade e dados", onBack, onOpen) {
        InfoNote("Tudo fica só neste aparelho. Não há conta, nuvem nem telemetria. Dados pessoais de passageiros não são guardados.")
        SectionTitle("O que é guardado")
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Lançamentos financeiros: ${txs.size}")
                Text("Preferências e critérios: valores que você digitou.")
                Text("Jornada: tempos de início, pausa e fim.")
                Text("Gravações: ${files.size} arquivo(s) de vídeo na pasta do aplicativo, sem áudio.")
                Text("Ofertas digitadas no simulador: só em memória; não são gravadas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        SectionTitle("Consentimento por recurso")
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Gravação: só inicia por ação sua, com consentimento na tela de gravação.")
                Text("Voz: só quando você liga.")
                Text("Botão flutuante: só com a permissão de sobreposição que você concede.")
                Text("Leitura de outros aplicativos: não existe.")
            }
        }
        SectionTitle("Imagens e gravações")
        OutlinedButton(onClick = { onOpen("gravacoes") }, modifier = Modifier.fillMaxWidth()) { Text("Biblioteca de gravações") }
        OutlinedButton(onClick = { onOpen("permissoes") }, modifier = Modifier.fillMaxWidth()) { Text("Permissões concedidas") }
        SectionTitle("Apagar")
        OutlinedButton(onClick = { confirmRecordings = true }, enabled = files.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Apagar gravações") }
        Button(onClick = { confirmWipe = true }, modifier = Modifier.fillMaxWidth()) { Text("Apagar todos os dados do aplicativo") }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }

    if (confirmWipe) {
        ConfirmDialog(
            title = "Apagar todos os dados?",
            text = "Lançamentos, contas, veículos, histórico, jornada e preferências serão apagados. Gravações não são apagadas aqui. Não há como desfazer.",
            confirmLabel = "Apagar",
            onConfirm = {
                AppData.wipeAll()
                onWipePrefs()
                context.getSharedPreferences("journey", Context.MODE_PRIVATE).edit().clear().apply()
                CopilotController.resetJourney()
                confirmWipe = false
                message = "Dados apagados."
            },
            onDismiss = { confirmWipe = false },
        )
    }
    if (confirmRecordings) {
        ConfirmDialog(
            title = "Apagar gravações?",
            text = "${files.size} arquivo(s) de vídeo serão apagados deste aparelho. Não há como desfazer.",
            confirmLabel = "Apagar",
            onConfirm = {
                files.forEach { it.delete() }
                confirmRecordings = false
                message = "Gravações apagadas."
            },
            onDismiss = { confirmRecordings = false },
        )
    }
}

/** Arquivos de vídeo gravados pelo app (pasta própria; sem permissão de armazenamento). */
fun recordingFiles(context: Context): List<File> {
    val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES) ?: return emptyList()
    return dir.listFiles { f -> f.isFile && f.extension.equals("mp4", ignoreCase = true) }?.sortedByDescending { it.lastModified() } ?: emptyList()
}

/** Dispositivos (telas/37): este aparelho. Limite comercial e sincronização ainda não existem. */
@Composable
fun DevicesScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    ScreenScaffold("Dispositivos", onBack, onOpen) {
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Este aparelho", fontWeight = FontWeight.SemiBold)
                Text(Build.MANUFACTURER + " " + Build.MODEL)
                Text("Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")")
                Text("Aplicativo " + BuildConfig.VERSION_NAME + " · " + BuildConfig.GIT_SHA)
            }
        }
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Outros aparelhos", fontWeight = FontWeight.SemiBold)
                Text("Sincronização entre aparelhos: não implementada.")
                Text("Limite de aparelhos: acesso comercial ainda a definir.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        InfoNote("Para levar seus dados a outro aparelho, use Backup e restauração (exportar e importar arquivo).")
    }
}

/** Conta e acesso (telas/36): perfil local. Acesso comercial, plano e preço ainda não definidos. */
@Composable
fun AccountScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    ScreenScaffold("Conta e acesso", onBack, onOpen) {
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Perfil local", fontWeight = FontWeight.SemiBold)
                Text("Os dados ficam neste aparelho. Não há login, nuvem nem assinatura.")
                Text("Acesso comercial (plano, preço, período de teste): a definir.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        OutlinedButton(onClick = { onOpen("privacidade") }, modifier = Modifier.fillMaxWidth()) { Text("Privacidade e dados") }
        OutlinedButton(onClick = { onOpen("backup") }, modifier = Modifier.fillMaxWidth()) { Text("Backup e restauração") }
    }
}
