package br.app.motorista.floating

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import br.app.motorista.R
import br.app.motorista.copilot.ControllerActions
import br.app.motorista.copilot.CopilotController
import br.app.motorista.copilot.CopilotUiState
import br.app.motorista.domain.card.PreviewState
import br.app.motorista.domain.offer.Platform
import br.app.motorista.domain.offer.Slot
import br.app.motorista.domain.overlay.IntBox
import br.app.motorista.domain.overlay.IntPoint
import br.app.motorista.domain.overlay.OverlayGeometry
import br.app.motorista.offer.BalloonHub
import br.app.motorista.offer.PublishedBalloon
import br.app.motorista.ui.components.BalloonNeutral
import br.app.motorista.ui.components.OfferBalloon
import br.app.motorista.ui.components.UnavailableBalloon
import br.app.motorista.ui.components.color
import br.app.motorista.ui.components.toPreviewState
import br.app.motorista.ui.copilot.CopilotMenu
import br.app.motorista.ui.theme.AppTheme
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Botão flutuante do PRÓPRIO aplicativo (copiloto No Ponto): botão, menu operacional e balão.
 *
 * Três janelas separadas: o botão (verde fixo, símbolo branco, opacidade própria), o menu (controles
 * ligados a [CopilotController]) e o balão (mesmo componente da prévia do app). A cor do botão nunca
 * acompanha a avaliação. Os controles só pedem ações; o estado muda quando o serviço real confirma.
 *
 * Limites deliberados: não lê outros aplicativos, não conhece o contexto de 99/Uber, não executa
 * toques/gestos, não aceita nem recusa corridas. A área útil é só a da tela (sem barras do sistema):
 * NÃO protege botões de outros aplicativos, o que depende de validação real em aparelho.
 */
class FloatingControlService : Service() {
    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var buttonView: View? = null
    private var buttonParams: WindowManager.LayoutParams? = null

    private var menuView: ComposeView? = null
    private var menuOwner: OverlayOwner? = null

    private var balloonView: ComposeView? = null
    private var balloonOwner: OverlayOwner? = null
    private var balloonModel by mutableStateOf<BalloonModel?>(null)

    /** Oferta em exibição (null quando o balão mostra diagnóstico ou indisponibilidade). */
    private var shownOffer: ShownOffer? = null
    private var lastAutoOfferId: String? = null

    private val hideBalloonRunnable = Runnable { hideBalloon() }
    private val validityTick = object : Runnable {
        override fun run() {
            refreshOfferBalloon()
            if (shownOffer != null) handler.postDelayed(this, TICK_MS)
        }
    }
    private val hubListener: () -> Unit = { handler.post { onHubChanged() } }
    private val opacityListener: () -> Unit = { handler.post { buttonView?.alpha = FloatingConfig.buttonOpacity } }

    private sealed interface BalloonModel {
        data class Sample(val state: PreviewState) : BalloonModel
        data class Offer(val published: PublishedBalloon, val ageSeconds: Long) : BalloonModel
        data class Unavailable(val origin: String?, val message: String) : BalloonModel
    }

    private data class ShownOffer(val platform: Platform, val offerId: String)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        CopilotController.ensureInit(this)
        BalloonHub.addListener(hubListener)
        FloatingConfig.onButtonOpacityChanged = opacityListener
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startInForeground()
        if (!Settings.canDrawOverlays(this)) {
            // Sem permissão concedida pelo usuário não há o que mostrar.
            stopSelf()
            return START_NOT_STICKY
        }
        if (buttonView == null) {
            addButton()
            scope.launch { CopilotController.state.collect { onControllerState(it) } }
        }
        if (intent?.action == ACTION_SAMPLE) {
            val state = runCatching { PreviewState.valueOf(intent.getStringExtra(EXTRA_SAMPLE) ?: "") }.getOrNull()
            if (state != null) showSample(state)
        }
        _running.value = true
        CopilotController.refresh()
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Rotação ou mudança de tela: a geometria antiga deixa de valer.
        removeMenu()
        hideBalloon()
        val params = buttonParams ?: return
        val view = buttonView ?: return
        val p = OverlayGeometry.clamp(params.x, params.y, buttonSize(), buttonSize(), usableBounds())
        params.x = p.x
        params.y = p.y
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    override fun onDestroy() {
        BalloonHub.removeListener(hubListener)
        FloatingConfig.onButtonOpacityChanged = null
        scope.cancel()
        handler.removeCallbacksAndMessages(null)
        hideBalloon()
        removeMenu()
        buttonView?.let { runCatching { windowManager.removeView(it) } }
        buttonView = null
        _running.value = false
        CopilotController.refresh()
        super.onDestroy()
    }

    // ---- reação ao estado único do copiloto ----

    private fun onControllerState(s: CopilotUiState) {
        buttonView?.visibility = if (s.buttonVisible) View.VISIBLE else View.GONE
        if (!s.buttonVisible) removeMenu()
        if (s.balloonHidden && balloonView != null) hideBalloon()
    }

    // ---- notificação obrigatória do serviço em primeiro plano ----

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Botão flutuante", NotificationManager.IMPORTANCE_LOW),
        )
        val stop = PendingIntent.getService(
            this, 0,
            Intent(this, FloatingControlService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Copiloto No Ponto ativo")
            .setContentText("Botão flutuante do próprio aplicativo. Sem integração com 99Pop.")
            .setSmallIcon(R.drawable.no_ponto_simbolo)
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
                    "Desligar",
                    stop,
                ).build(),
            )
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // ---- geometria ----

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun buttonSize(): Int = dp(56)

    /** Tela menos barras do sistema e recorte. Mesma referência (tela inteira) das janelas FLAG_LAYOUT_IN_SCREEN. */
    private fun usableBounds(): IntBox {
        if (Build.VERSION.SDK_INT >= 30) {
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
            )
            val b = metrics.bounds
            return IntBox(b.left + insets.left, b.top + insets.top, b.right - insets.right, b.bottom - insets.bottom)
        }
        val dm = resources.displayMetrics
        val statusId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val status = if (statusId > 0) resources.getDimensionPixelSize(statusId) else 0
        return IntBox(0, status, dm.widthPixels, dm.heightPixels)
    }

    private fun overlayParams(width: Int, height: Int, extraFlags: Int): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or extraFlags,
            PixelFormat.TRANSLUCENT,
        )

    // ---- botão: verde fixo com símbolo branco (arte do projeto), opacidade própria ----

    private fun addButton() {
        val size = buttonSize()
        val button = View(this).apply {
            contentDescription = getString(R.string.floating_button_description)
            // Verde fixo e símbolo branco vêm do drawable; nada aqui depende da avaliação da corrida.
            background = getDrawable(R.drawable.no_ponto_botao)
            alpha = FloatingConfig.buttonOpacity
            elevation = dp(4).toFloat()
        }
        val bounds = usableBounds()
        val start = OverlayGeometry.clamp(bounds.left + dp(12), bounds.top + dp(160), size, size, bounds)
        val params = overlayParams(size, size, 0).apply {
            gravity = Gravity.TOP or Gravity.START
            x = start.x
            y = start.y
        }
        button.setOnTouchListener(DragOrTapListener(params))
        windowManager.addView(button, params)
        buttonView = button
        buttonParams = params
    }

    /** Arrasta o botão dentro da área útil; toque curto (sem arrasto) abre/fecha o menu. */
    private inner class DragOrTapListener(private val params: WindowManager.LayoutParams) : View.OnTouchListener {
        private var startX = 0
        private var startY = 0
        private var touchX = 0f
        private var touchY = 0f
        private var moved = false

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (!moved && (abs(dx) > dp(6) || abs(dy) > dp(6))) moved = true
                    if (moved) {
                        val p = OverlayGeometry.clamp(startX + dx, startY + dy, buttonSize(), buttonSize(), usableBounds())
                        params.x = p.x
                        params.y = p.y
                        windowManager.updateViewLayout(view, params)
                        removeMenu()
                    }
                }
                MotionEvent.ACTION_UP -> if (!moved) toggleMenu()
            }
            return true
        }
    }

    // ---- menu operacional, limitado à área útil ----

    private fun toggleMenu() {
        if (menuView != null) removeMenu() else showMenu()
    }

    private fun showMenu() {
        val anchor = buttonParams ?: return
        CopilotController.refreshPermissions()
        val bounds = usableBounds()
        val menuW = dp(340).coerceAtMost(bounds.width)
        val maxHeightDp = (bounds.height / resources.displayMetrics.density).dp
        val owner = OverlayOwner()
        val actions = ControllerActions(this)
        val view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                AppTheme {
                    val state by CopilotController.state.collectAsState()
                    CopilotMenu(
                        state = state,
                        actions = actions,
                        onSettings = {
                            removeMenu()
                            CopilotController.openApp(this@FloatingControlService, "atalhos")
                        },
                        maxHeight = maxHeightDp,
                    )
                }
            }
        }
        val start = OverlayGeometry.placeMenu(IntPoint(anchor.x, anchor.y), buttonSize(), dp(8), menuW, dp(240), bounds)
        val params = overlayParams(menuW, ViewGroup.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH).apply {
            gravity = Gravity.TOP or Gravity.START
            x = start.x
            y = start.y
        }
        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) removeMenu()
            false
        }
        // Com a altura real conhecida (e a cada mudança de conteúdo), reposiciona dentro da área útil.
        view.viewTreeObserver.addOnGlobalLayoutListener { placeMenuNow(view, menuW) }
        windowManager.addView(view, params)
        menuView = view
        menuOwner = owner
    }

    private fun placeMenuNow(view: ComposeView, menuW: Int) {
        val anchor = buttonParams ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        if (view.height <= 0 || view.parent == null) return
        val bounds = usableBounds()
        val p = OverlayGeometry.placeMenu(
            IntPoint(anchor.x, anchor.y), buttonSize(), dp(8), menuW, view.height.coerceAtMost(bounds.height), bounds,
        )
        if (p.x != params.x || p.y != params.y) {
            params.x = p.x
            params.y = p.y
            runCatching { windowManager.updateViewLayout(view, params) }
        }
    }

    private fun removeMenu() {
        menuView?.let { runCatching { windowManager.removeView(it) } }
        menuView = null
        menuOwner?.destroy()
        menuOwner = null
    }

    // ---- balão: mesmo componente e mesmas configurações da prévia ----

    /** Diagnóstico: balão de exemplo, pedido pela tela do simulador. Nunca aparece no menu de operação. */
    private fun showSample(state: PreviewState) {
        stopOfferTracking()
        display(BalloonModel.Sample(state))
    }

    private fun onHubChanged() {
        if (shownOffer != null) {
            refreshOfferBalloon()
            return
        }
        if (CopilotController.state.value.balloonHidden || buttonView == null) return
        val latest = BalloonHub.latestAny() ?: return
        if (latest.offerId == lastAutoOfferId) return
        val slot = BalloonHub.store.slot(latest.platform, SystemClock.elapsedRealtime())
        if (slot !is Slot.Fresh || slot.snapshot.id != latest.offerId) return
        lastAutoOfferId = latest.offerId
        showOffer(latest, slot.ageMs / 1000)
    }

    private fun showOffer(published: PublishedBalloon, ageSeconds: Long) {
        shownOffer = ShownOffer(published.platform, published.offerId)
        display(BalloonModel.Offer(published, ageSeconds))
        handler.removeCallbacks(validityTick)
        handler.postDelayed(validityTick, TICK_MS)
    }

    /**
     * Revalida a oferta exibida (a cada tick e a cada mudança no hub): expirou, sumiu ou foi trocada.
     * Troca válida atualiza o balão; qualquer outro caso o remove na hora.
     */
    private fun refreshOfferBalloon() {
        val shown = shownOffer ?: return
        if (balloonView == null) {
            stopOfferTracking()
            return
        }
        val slot = BalloonHub.store.slot(shown.platform, SystemClock.elapsedRealtime())
        val latest = BalloonHub.latest(shown.platform)
        if (slot is Slot.Fresh && latest != null && latest.offerId == slot.snapshot.id) {
            shownOffer = ShownOffer(shown.platform, latest.offerId)
            balloonModel = BalloonModel.Offer(latest, slot.ageMs / 1000)
        } else {
            hideBalloon()
        }
    }

    private fun stopOfferTracking() {
        shownOffer = null
        handler.removeCallbacks(validityTick)
    }

    private fun originOf(p: PublishedBalloon): String = p.platform.label + " · " + p.origin.label

    private fun display(model: BalloonModel) {
        balloonModel = model
        handler.removeCallbacks(hideBalloonRunnable)
        if (balloonView == null) addBalloonWindow()
        // Duração visual é independente da validade do dado: some em BALLOON_SECONDS ou antes, se invalidado.
        handler.postDelayed(hideBalloonRunnable, BALLOON_SECONDS * 1000L)
    }

    private fun addBalloonWindow() {
        val owner = OverlayOwner()
        val view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent { BalloonContent(balloonModel) }
        }
        val bounds = usableBounds()
        val width = (bounds.width * 0.92f).toInt().coerceAtMost(dp(560))
        val params = overlayParams(
            width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = bounds.top + dp(12)
        }
        windowManager.addView(view, params)
        balloonView = view
        balloonOwner = owner
    }

    private fun hideBalloon() {
        handler.removeCallbacks(hideBalloonRunnable)
        stopOfferTracking()
        balloonView?.let { runCatching { windowManager.removeView(it) } }
        balloonView = null
        balloonOwner?.destroy()
        balloonOwner = null
        balloonModel = null
    }

    @Composable
    private fun BalloonContent(model: BalloonModel?) {
        val style = FloatingConfig.style
        AppTheme {
            when (model) {
                null -> Unit
                is BalloonModel.Sample -> OfferBalloon(model.state, style)
                is BalloonModel.Unavailable -> UnavailableBalloon(model.origin, model.message, style)
                is BalloonModel.Offer -> {
                    val p = model.published
                    val band = p.band
                    OfferBalloon(
                        seal = band?.toPreviewState()?.seal ?: "SEM COR GERAL",
                        accent = band?.toPreviewState()?.color() ?: BalloonNeutral,
                        style = style,
                        values = p.values.copy(tag = originOf(p)),
                        reason = p.note + " (há " + model.ageSeconds + " s)",
                    )
                }
            }
        }
    }

    /** Dono de ciclo de vida mínimo para hospedar Compose em uma janela de serviço. */
    private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val controller = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

        init {
            controller.performAttach()
            controller.performRestore(Bundle())
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun destroy() {
            registry.currentState = Lifecycle.State.DESTROYED
        }
    }

    companion object {
        const val ACTION_STOP = "br.app.motorista.floating.STOP"
        const val ACTION_SAMPLE = "br.app.motorista.floating.SAMPLE"
        const val EXTRA_SAMPLE = "state"
        private const val CHANNEL_ID = "floating_control"
        private const val NOTIFICATION_ID = 1
        private const val BALLOON_SECONDS = 8
        private const val TICK_MS = 500L

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running

        fun start(context: Context) {
            context.startForegroundService(Intent(context, FloatingControlService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingControlService::class.java))
        }

        /** Diagnóstico: mostra um balão de exemplo se o copiloto estiver ligado. */
        fun showSample(context: Context, state: PreviewState): Boolean {
            if (!_running.value) return false
            context.startService(
                Intent(context, FloatingControlService::class.java).setAction(ACTION_SAMPLE).putExtra(EXTRA_SAMPLE, state.name),
            )
            return true
        }
    }
}
