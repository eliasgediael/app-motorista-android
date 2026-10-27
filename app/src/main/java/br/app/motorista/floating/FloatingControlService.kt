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
import android.graphics.drawable.GradientDrawable
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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import br.app.motorista.R
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
import br.app.motorista.ui.theme.AppTheme
import kotlin.math.abs

/**
 * Botão flutuante do PRÓPRIO aplicativo, com menu de controles e balão.
 *
 * Três elementos separados: o botão (verde fixo, símbolo branco, opacidade própria), o menu e o balão
 * (mesmo componente da prévia do app, com o mesmo estilo e a opacidade do balão). A cor do botão não
 * acompanha a avaliação.
 *
 * Limites deliberados: não lê outros aplicativos, não conhece o contexto de 99/Uber, não executa
 * toques/gestos, não aceita nem recusa corridas. A área útil usada aqui é só a da tela (sem barras do
 * sistema); NÃO protege botões de outros aplicativos, o que depende de validação real em aparelho.
 */
class FloatingControlService : Service() {
    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())

    private var buttonView: View? = null
    private var buttonParams: WindowManager.LayoutParams? = null
    private var menuView: View? = null

    private var balloonView: ComposeView? = null
    private var balloonOwner: OverlayOwner? = null
    private var balloonModel by mutableStateOf<BalloonModel?>(null)

    /** Oferta real em exibição (null quando o balão mostra exemplo ou indisponibilidade). */
    private var shownOffer: ShownOffer? = null

    private val hideBalloonRunnable = Runnable { hideBalloon() }
    private val validityTick = object : Runnable {
        override fun run() {
            refreshOfferBalloon()
            if (shownOffer != null) handler.postDelayed(this, TICK_MS)
        }
    }

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
        BalloonHub.listener = { handler.post { refreshOfferBalloon() } }
        FloatingConfig.onButtonOpacityChanged = { handler.post { buttonView?.alpha = FloatingConfig.buttonOpacity } }
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
        if (buttonView == null) addButton()
        _running.value = true
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
        BalloonHub.listener = null
        FloatingConfig.onButtonOpacityChanged = null
        handler.removeCallbacksAndMessages(null)
        hideBalloon()
        removeMenu()
        buttonView?.let { runCatching { windowManager.removeView(it) } }
        buttonView = null
        _running.value = false
        super.onDestroy()
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
            .setContentTitle("Botão flutuante ativo")
            .setContentText("Ferramenta do próprio aplicativo. Sem integração com 99Pop.")
            .setSmallIcon(R.drawable.ic_symbol)
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

    // ---- botão: verde fixo, símbolo branco, opacidade própria ----

    private fun addButton() {
        val size = buttonSize()
        val symbol = ImageView(this).apply {
            setImageResource(R.drawable.ic_symbol)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val button = FrameLayout(this).apply {
            contentDescription = getString(R.string.floating_button_description)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(getColor(R.color.brand_green))
            }
            alpha = FloatingConfig.buttonOpacity
            elevation = dp(4).toFloat()
            addView(symbol, FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER))
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

    // ---- menu de controles, limitado à área útil ----

    private fun toggleMenu() {
        if (menuView != null) removeMenu() else showMenu()
    }

    private fun showMenu() {
        val anchor = buttonParams ?: return
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        column.addView(menuTitle("Controles"))
        column.addView(menuItem("Última oferta analisada", INK) { showLatestOffer() })
        column.addView(menuItem("Balão verde (exemplo)", PreviewState.Good.color().toArgb()) { showSample(PreviewState.Good) })
        column.addView(menuItem("Balão amarelo (exemplo)", PreviewState.Attention.color().toArgb()) { showSample(PreviewState.Attention) })
        column.addView(menuItem("Balão vermelho (exemplo)", PreviewState.Bad.color().toArgb()) { showSample(PreviewState.Bad) })
        column.addView(menuItem("Ocultar balão", INK) { hideBalloon() })
        column.addView(menuItem("Abrir aplicativo", INK) { openApp() })
        column.addView(menuItem("Desligar botão", 0xFFB3261E.toInt()) { stopSelf() })

        val scroll = ScrollView(this).apply {
            addView(column)
            background = GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), 0xFFDDE5E0.toInt())
            }
            elevation = dp(8).toFloat()
        }

        val bounds = usableBounds()
        val menuW = dp(240).coerceAtMost(bounds.width)
        column.measure(
            View.MeasureSpec.makeMeasureSpec(menuW, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        val menuH = column.measuredHeight.coerceAtMost(bounds.height)
        val p = OverlayGeometry.placeMenu(IntPoint(anchor.x, anchor.y), buttonSize(), dp(8), menuW, menuH, bounds)

        val params = overlayParams(menuW, menuH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH).apply {
            gravity = Gravity.TOP or Gravity.START
            x = p.x
            y = p.y
        }
        scroll.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) removeMenu()
            false
        }
        windowManager.addView(scroll, params)
        menuView = scroll
    }

    private fun menuTitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 12f
        setTextColor(GREY)
        setPadding(dp(12), dp(6), dp(12), dp(6))
    }

    private fun menuItem(text: String, color: Int, action: () -> Unit) = TextView(this).apply {
        this.text = text
        textSize = 15f
        setTextColor(color)
        minHeight = dp(48)
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), 0, dp(12), 0)
        setOnClickListener {
            removeMenu()
            action()
        }
    }

    private fun removeMenu() {
        menuView?.let { runCatching { windowManager.removeView(it) } }
        menuView = null
    }

    private fun openApp() {
        packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let { startActivity(it) }
    }

    // ---- balão: mesmo componente e mesmas configurações da prévia ----

    private fun showSample(state: PreviewState) {
        stopOfferTracking()
        display(BalloonModel.Sample(state))
    }

    /**
     * Mostra a última oferta publicada. Só exibe valores se ela ainda é a atual e está dentro do prazo;
     * caso contrário mostra indisponibilidade. Enquanto visível, [refreshOfferBalloon] a invalida.
     */
    private fun showLatestOffer() {
        val published = BalloonHub.latestAny()
        if (published == null) {
            stopOfferTracking()
            display(BalloonModel.Unavailable(null, "Nenhuma oferta analisada ainda."))
            return
        }
        val slot = BalloonHub.store.slot(published.platform, SystemClock.elapsedRealtime())
        if (slot !is Slot.Fresh || slot.snapshot.id != published.offerId) {
            stopOfferTracking()
            display(BalloonModel.Unavailable(originOf(published), "A última oferta expirou ou foi substituída. Nenhum resultado antigo é mostrado."))
            return
        }
        shownOffer = ShownOffer(published.platform, published.offerId)
        display(BalloonModel.Offer(published, slot.ageMs / 1000))
        handler.removeCallbacks(validityTick)
        handler.postDelayed(validityTick, TICK_MS)
    }

    /**
     * Revalida a oferta exibida (a cada tick e a cada mudança no hub): expirou, sumiu ou foi trocada
     * por outra. Troca válida atualiza o balão; qualquer outro caso o remove na hora.
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
            ViewTreeLifecycleOwner.set(this, owner)
            ViewTreeSavedStateRegistryOwner.set(this, owner)
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
        balloonView?.let {
            runCatching { windowManager.removeView(it) }
        }
        balloonView = null
        balloonOwner?.destroy()
        balloonOwner = null
        balloonModel = null
    }

    @androidx.compose.runtime.Composable
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
        private const val CHANNEL_ID = "floating_control"
        private const val NOTIFICATION_ID = 1
        private const val BALLOON_SECONDS = 8
        private const val TICK_MS = 500L
        private const val GREY = 0xFF5F6E66.toInt()
        private const val INK = 0xFF15211B.toInt()

        private val _running = kotlinx.coroutines.flow.MutableStateFlow(false)
        val running: kotlinx.coroutines.flow.StateFlow<Boolean> = _running

        fun start(context: Context) {
            context.startForegroundService(Intent(context, FloatingControlService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingControlService::class.java))
        }
    }
}
