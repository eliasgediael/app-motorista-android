package br.app.motorista.ui.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.app.motorista.data.AppData
import br.app.motorista.domain.history.Profile
import br.app.motorista.ui.common.ConfirmDialog
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SectionTitle
import br.app.motorista.ui.common.SurfaceCard
import br.app.motorista.ui.common.TextInput
import br.app.motorista.ui.state.PreferencesState
import br.app.motorista.ui.state.RidePlatform

/** Perfis de trabalho (telas/04-perfis): guarda critérios, política e estilo do cartão por contexto. */
@Composable
fun ProfilesScreen(
    exportProfile: () -> String,
    applyProfile: (String) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val profiles by AppData.profiles.items.collectAsState()
    val settings by AppData.settings.items.collectAsState()
    val activeId = settings.firstOrNull { it.id == "activeProfile" }?.value
    var name by rememberSaveable { mutableStateOf("") }
    var removing by rememberSaveable { mutableStateOf<String?>(null) }

    ScreenScaffold("Perfis de trabalho", onBack, onOpen) {
        InfoNote("Um perfil guarda os critérios (valores, bases, ativação), a combinação e o estilo do cartão que estão configurados agora. Nada é criado para você.")
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Salvar configuração atual como perfil", fontWeight = FontWeight.SemiBold)
                TextInput("Nome do perfil", name, { name = it })
                Button(
                    enabled = name.isNotBlank() && profiles.none { it.name.equals(name.trim(), true) },
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val p = Profile(AppData.newId(), name.trim(), exportProfile())
                        AppData.profiles.upsert(p)
                        AppData.putSetting("activeProfile", p.id)
                        name = ""
                    },
                ) { Text("Salvar perfil") }
            }
        }
        SectionTitle("Perfis")
        if (profiles.isEmpty()) Text("Nenhum perfil salvo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        profiles.forEach { p ->
            SurfaceCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(p.name + if (p.id == activeId) " · ATIVO" else "", fontWeight = FontWeight.SemiBold)
                    Row {
                        TextButton(onClick = {
                            applyProfile(p.json)
                            AppData.putSetting("activeProfile", p.id)
                        }) { Text(if (p.id == activeId) "Reaplicar" else "Ativar") }
                        TextButton(onClick = { AppData.profiles.upsert(p.copy(json = exportProfile())) }) { Text("Atualizar com o atual") }
                        TextButton(onClick = {
                            val copyName = p.name + " (cópia)"
                            AppData.profiles.upsert(Profile(AppData.newId(), copyName, p.json))
                        }) { Text("Duplicar") }
                        TextButton(onClick = { removing = p.id }) { Text("Excluir") }
                    }
                }
            }
        }
    }
    removing?.let { id ->
        ConfirmDialog("Excluir perfil?", "Só o perfil salvo é apagado; a configuração atual não muda.", "Excluir", {
            AppData.profiles.remove(id)
            if (AppData.setting("activeProfile") == id) AppData.removeSetting("activeProfile")
            removing = null
        }, { removing = null })
    }
}

/** Aplicativos de corrida (telas/05-plataformas): escolher apps alvo e ver o que realmente existe para cada um. */
@Composable
fun PlatformsScreen(state: PreferencesState, onToggle: (RidePlatform) -> Unit, onBack: () -> Unit, onOpen: (String) -> Unit) {
    ScreenScaffold("Aplicativos de corrida", onBack, onOpen) {
        InfoNote("Escolher um aplicativo registra sua preferência. NÃO liga leitura nem integração. A leitura automática de ofertas não está implementada para nenhum aplicativo.")
        RidePlatform.entries.forEach { p ->
            SurfaceCard {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Text("Entrada manual de ofertas: disponível (simulador)", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Leitura de ofertas: não implementada" + if (p == RidePlatform.InDrive) " · fora do escopo atual" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = p in state.selectedPlatforms, onCheckedChange = { onToggle(p) })
                }
            }
        }
    }
}
