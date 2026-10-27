package br.app.motorista

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import br.app.motorista.copilot.CopilotController
import br.app.motorista.data.AppData
import br.app.motorista.data.ThemeMode
import br.app.motorista.ui.nav.AppNavHost
import br.app.motorista.ui.state.PreferencesViewModel
import br.app.motorista.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private val preferences: PreferencesViewModel by viewModels()

    /** Rota pedida pelo menu flutuante (ex.: etapa visível da gravação). Consumida pela navegação. */
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppData.init(this)
        CopilotController.ensureInit(this)
        pendingRoute = intent?.getStringExtra(EXTRA_ROUTE)
        setContent {
            val state by preferences.state.collectAsState()
            val dark = when (state.themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            AppTheme(darkTheme = dark) {
                AppNavHost(
                    pendingRoute = pendingRoute,
                    onRouteConsumed = { pendingRoute = null },
                    preferences = preferences,
                )
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
