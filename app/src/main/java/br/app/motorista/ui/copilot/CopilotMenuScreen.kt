package br.app.motorista.ui.copilot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import br.app.motorista.copilot.ControllerActions
import br.app.motorista.copilot.CopilotController
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.ScreenScaffold

/** Menu do botão flutuante (telas/45) dentro do app, ligado ao mesmo estado real do menu flutuante. */
@Composable
fun CopilotMenuScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val actions = remember { ControllerActions(context) }
    val state by CopilotController.state.collectAsState()
    ScreenScaffold("Menu do botão flutuante", onBack, onOpen) {
        CopilotMenu(state = state, actions = actions, onSettings = { onOpen("atalhos") }, modifier = Modifier)
        InfoNote("Estes são os mesmos controles e o mesmo estado do menu flutuante. Os estados vêm dos serviços; nada é simulado.")
    }
}
