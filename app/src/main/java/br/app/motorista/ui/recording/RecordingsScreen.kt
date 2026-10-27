package br.app.motorista.ui.recording

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import br.app.motorista.ui.common.ConfirmDialog
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SurfaceCard
import br.app.motorista.ui.settings.recordingFiles
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Biblioteca de gravações (telas/33-gravacoes): lista, reprodução, compartilhamento voluntário e exclusão. */
@Composable
fun RecordingsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    val files = if (refresh >= 0) recordingFiles(context) else emptyList()
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    var sharing by rememberSaveable { mutableStateOf<String?>(null) }

    fun uriFor(f: File) = FileProvider.getUriForFile(context, context.packageName + ".files", f)

    ScreenScaffold("Biblioteca de gravações", onBack, onOpen) {
        InfoNote("Vídeos gravados por você na tela de gravação, salvos só neste aparelho e sem áudio. Compartilhar é voluntário e pede revisão antes.")
        if (files.isEmpty()) Text("Nenhuma gravação.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        files.forEach { f ->
            SurfaceCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(f.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(f.lastModified())) + " · " + (f.length() / 1024) + " KB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row {
                        TextButton(onClick = {
                            val view = Intent(Intent.ACTION_VIEW).setDataAndType(uriFor(f), "video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            runCatching { context.startActivity(view) }
                        }) { Text("Reproduzir") }
                        TextButton(onClick = { sharing = f.name }) { Text("Compartilhar") }
                        TextButton(onClick = { deleting = f.name }) { Text("Excluir") }
                    }
                }
            }
        }
    }

    deleting?.let { name ->
        ConfirmDialog("Excluir gravação?", "$name será apagada deste aparelho. Não há como desfazer.", "Excluir", {
            files.firstOrNull { it.name == name }?.delete()
            deleting = null
            refresh++
        }, { deleting = null })
    }
    sharing?.let { name ->
        ConfirmDialog(
            "Compartilhar gravação?",
            "O vídeo pode mostrar pessoas e lugares. Revise antes: ao compartilhar, ele sai do aparelho para o aplicativo que você escolher.",
            "Escolher destino",
            {
                files.firstOrNull { it.name == name }?.let { f ->
                    val send = Intent(Intent.ACTION_SEND).setType("video/mp4").putExtra(Intent.EXTRA_STREAM, uriFor(f)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    runCatching { context.startActivity(Intent.createChooser(send, "Compartilhar gravação").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                sharing = null
            },
            { sharing = null },
        )
    }
}
