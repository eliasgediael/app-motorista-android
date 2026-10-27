package br.app.motorista.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.app.motorista.data.AppData
import br.app.motorista.ui.common.ConfirmDialog
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SectionTitle
import br.app.motorista.ui.common.SurfaceCard
import br.app.motorista.ui.nav.ScreenCatalog
import br.app.motorista.ui.nav.ScreenStatus
import org.json.JSONObject

/**
 * Backup e restauração (telas/38-backup): exporta e importa um arquivo JSON escolhido pelo usuário.
 * Nada é enviado a lugar algum; revisar o arquivo antes de compartilhar é responsabilidade de quem exporta.
 */
@Composable
fun BackupScreen(
    prefsJson: () -> JSONObject,
    onRestorePrefs: (JSONObject) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<String?>(null) }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            message = try {
                val text = AppData.exportBackup(JSONObject().put("prefs", prefsJson()))
                context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) } ?: error("Sem acesso ao arquivo")
                "Backup salvo."
            } catch (e: Exception) {
                "Falha ao salvar: " + (e.message ?: e.javaClass.simpleName)
            }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            message = try {
                pendingImport = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: error("Sem acesso ao arquivo")
                null
            } catch (e: Exception) {
                "Falha ao ler: " + (e.message ?: e.javaClass.simpleName)
            }
        }
    }

    ScreenScaffold("Backup e restauração", onBack, onOpen) {
        InfoNote("O backup é um arquivo seu, gerado só quando você pede. Ele inclui lançamentos, cadastros e preferências; não inclui gravações de vídeo.")
        Button(onClick = { export.launch("no-ponto-backup.json") }, modifier = Modifier.fillMaxWidth()) { Text("Exportar backup") }
        OutlinedButton(onClick = { import.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text("Restaurar de um arquivo")
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Text(
            "Restaurar substitui os dados atuais. O arquivo é validado antes: se for inválido ou de versão desconhecida, nada muda. " +
                "Sincronização entre aparelhos e resolução de conflitos: não implementadas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    pendingImport?.let { text ->
        ConfirmDialog(
            title = "Restaurar backup?",
            text = "Os dados atuais serão substituídos pelos do arquivo.",
            confirmLabel = "Restaurar",
            onConfirm = {
                message = try {
                    val extra = AppData.importBackup(text)
                    extra.optJSONObject("prefs")?.let(onRestorePrefs)
                    "Backup restaurado."
                } catch (e: Exception) {
                    e.message ?: "Backup inválido"
                }
                pendingImport = null
            },
            onDismiss = { pendingImport = null },
        )
    }
}

/** Ferramentas (telas/41-ferramentas): catálogo completo com o estado real de cada função. */
@Composable
fun ToolsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    ScreenScaffold("Ferramentas", onBack, onOpen) {
        InfoNote("Cada função mostra seu estado: Pronta (implementada, compilada e com testes automáticos), Bloqueada (depende de decisão ou validação) ou Em desenvolvimento.")
        ScreenCatalog.all.map { it.group }.distinct().forEach { group ->
            SectionTitle(group)
            SurfaceCard {
                Column {
                    ScreenCatalog.all.filter { it.group == group }.forEach { s ->
                        val status = ScreenCatalog.status(s.id)
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpen(s.id) }.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(s.title, fontWeight = FontWeight.SemiBold)
                                if (status == ScreenStatus.Blocked) {
                                    Text(
                                        ScreenCatalog.blockedReason(s.id) ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Text(
                                status.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (status == ScreenStatus.Ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
