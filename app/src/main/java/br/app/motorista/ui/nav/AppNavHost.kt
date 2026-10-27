package br.app.motorista.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.app.motorista.domain.card.PreviewState
import br.app.motorista.floating.FloatingConfig
import br.app.motorista.ui.balloon.BalloonPreviewScreen
import br.app.motorista.ui.card.CardStyleScreen
import br.app.motorista.ui.combination.CombinationScreen
import br.app.motorista.ui.copilot.CopilotMenuScreen
import br.app.motorista.ui.finance.CategoriesScreen
import br.app.motorista.ui.finance.EntryFormScreen
import br.app.motorista.ui.finance.FinanceScreen
import br.app.motorista.ui.settings.AccountScreen
import br.app.motorista.ui.settings.BackupScreen
import br.app.motorista.ui.settings.DevicesScreen
import br.app.motorista.ui.settings.PermissionsScreen
import br.app.motorista.ui.settings.PreferencesScreen
import br.app.motorista.ui.settings.PrivacyScreen
import br.app.motorista.ui.settings.ToolsScreen
import br.app.motorista.ui.criteria.CriteriaScreen
import br.app.motorista.ui.journey.JourneyScreen
import br.app.motorista.ui.recording.RecordingScreen
import br.app.motorista.ui.floating.FloatingControlScreen
import br.app.motorista.ui.simulator.SimulatorScreen
import br.app.motorista.ui.criteria.CriterionEditorScreen
import br.app.motorista.ui.home.HomeScreen
import br.app.motorista.ui.state.PreferencesViewModel
import br.app.motorista.ui.placeholder.PlaceholderScreen

const val ROUTE_HOME = "painel"
const val ARG_ID = "id"
private const val ROUTE_SCREEN = "tela/{$ARG_ID}"

/** Destinos de navegação inferior documentados em telas/01-painel/FUNCAO.md. */
enum class BottomTab(val screenId: String, val label: String) {
    Inicio("painel", "Início"),
    Financas("financas", "Finanças"),
    Historico("historico", "Histórico"),
    Ajustes("preferencias", "Ajustes"),
}

fun NavHostController.openScreen(id: String) {
    if (id == ROUTE_HOME) {
        popBackStack(ROUTE_HOME, inclusive = false)
    } else {
        navigate("tela/$id") { launchSingleTop = true }
    }
}

@Composable
fun AppNavHost(
    pendingRoute: String? = null,
    onRouteConsumed: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
    preferences: PreferencesViewModel = viewModel(),
) {
    val state by preferences.state.collectAsState()
    // Entrega ao serviço do botão flutuante o mesmo estilo do cartão e a opacidade do botão.
    LaunchedEffect(state.cardStyle, state.buttonOpacity) {
        FloatingConfig.update(state.cardStyle, state.buttonOpacity)
    }
    val back: () -> Unit = { navController.popBackStack() }
    val open: (String) -> Unit = navController::openScreen
    NavHost(navController = navController, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            HomeScreen(onOpen = navController::openScreen)
        }
        composable(ROUTE_SCREEN) { entry ->
            val id = entry.arguments?.getString(ARG_ID).orEmpty()
            when (id) {
                "criterios" -> CriteriaScreen(
                    state = state,
                    onTogglePlatform = preferences::togglePlatform,
                    onEditMetric = preferences::editMetric,
                    onBack = back,
                    onOpen = open,
                )
                "editar-criterio" -> CriterionEditorScreen(
                    state = state,
                    onSelectMetric = preferences::editMetric,
                    onUpdate = preferences::updateDraft,
                    onReview = preferences::markReviewed,
                    onBack = back,
                    onOpen = open,
                )
                "cartao" -> CardStyleScreen(
                    state = state,
                    onPreviewState = preferences::setPreviewState,
                    onStyle = preferences::updateCardStyle,
                    onBack = back,
                    onOpen = open,
                )
                "combinacao" -> CombinationScreen(
                    policy = state.policy,
                    onChange = preferences::updatePolicy,
                    onBack = back,
                    onOpen = open,
                )
                "simulador" -> SimulatorScreen(
                    state = state,
                    onChange = preferences::updateSimulator,
                    onBack = back,
                    onOpen = open,
                )
                "financas" -> FinanceScreen(back, open)
                "lancamento" -> EntryFormScreen(back, open)
                "categorias" -> CategoriesScreen(back, open)
                "preferencias" -> PreferencesScreen(state.themeMode, preferences::setThemeMode, dark = false, onBack = back, onOpen = open)
                "aparencia-escura" -> PreferencesScreen(state.themeMode, preferences::setThemeMode, dark = true, onBack = back, onOpen = open)
                "permissoes" -> PermissionsScreen(back, open)
                "conta" -> AccountScreen(back, open)
                "dispositivos" -> DevicesScreen(back, open)
                "privacidade" -> PrivacyScreen(onWipePrefs = preferences::wipePrefs, onBack = back, onOpen = open)
                "backup" -> BackupScreen(
                    prefsJson = preferences::exportPrefsJson,
                    onRestorePrefs = preferences::importPrefsJson,
                    onBack = back,
                    onOpen = open,
                )
                "ferramentas" -> ToolsScreen(back, open)
                "menu-flutuante" -> CopilotMenuScreen(onBack = back, onOpen = open)
                "camera" -> RecordingScreen(onBack = back, onOpen = open)
                "jornada" -> JourneyScreen(onBack = back, onOpen = open)
                "atalhos" -> FloatingControlScreen(
                    buttonOpacity = state.buttonOpacity,
                    onButtonOpacity = preferences::setButtonOpacity,
                    onBack = back,
                    onOpen = open,
                )
                "balao-boa" -> BalloonPreviewScreen(PreviewState.Good, state, back, open)
                "balao-atencao" -> BalloonPreviewScreen(PreviewState.Attention, state, back, open)
                "balao-ruim" -> BalloonPreviewScreen(PreviewState.Bad, state, back, open)
                else -> PlaceholderScreen(screen = ScreenCatalog.find(id), onBack = back)
            }
        }
    }

    // Rota pedida de fora (menu flutuante). Fica depois do NavHost para o grafo já existir.
    LaunchedEffect(pendingRoute) {
        pendingRoute?.let {
            navController.openScreen(it)
            onRouteConsumed()
        }
    }
}
