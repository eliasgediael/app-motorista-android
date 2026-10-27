package br.app.motorista

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import br.app.motorista.copilot.CopilotController
import br.app.motorista.ui.nav.AppNavHost
import br.app.motorista.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    /** Rota pedida pelo menu flutuante (ex.: etapa visível da gravação). Consumida pela navegação. */
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        CopilotController.ensureInit(this)
        pendingRoute = intent?.getStringExtra(EXTRA_ROUTE)
        setContent {
            AppTheme {
                AppNavHost(pendingRoute = pendingRoute, onRouteConsumed = { pendingRoute = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute = intent.getStringExtra(EXTRA_ROUTE)
    }

    override fun onResume() {
        super.onResume()
        // Permissão concedida ou revogada fora do app: o estado acompanha.
        CopilotController.refreshPermissions()
    }

    companion object {
        const val EXTRA_ROUTE = "route"
    }
}
