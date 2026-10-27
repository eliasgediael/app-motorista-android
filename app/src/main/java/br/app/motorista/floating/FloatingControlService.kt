package br.app.motorista.floating

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import br.app.motorista.R
import br.app.motorista.domain.assessment.Band
import br.app.motorista.domain.card.PreviewState
import br.app.motorista.domain.card.SampleOffer
import br.app.motorista.domain.offer.Slot
import br.app.motorista.offer.BalloonHub
import br.app.motorista.ui.theme.FunctionalColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Botão flutuante do PRÓPRIO aplicativo, com menu de controles e balão de EXEMPLO.
 *
 * Limites deliberados: não lê outros aplicativos, não conhece o contexto de 99/Uber/inDrive,
 * não executa toques/gestos, não aceita nem recusa corridas. O balão é uma janela separada
 * do botão, não toca a tela (FLAG_NOT_TOUCHABLE) e desaparece sozinho.
 */
class FloatingControlService : Service() {
    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var buttonView: View? = null
    private var menuView: View? = null
    private var balloonView: View? = null
    private val hideBalloon = Runnable { removeBalloon() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
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

    override fun onDestroy() {
        handler.removeCallbacks(hideBalloon)
        removeBalloon()
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
            .setContentText("Simulação do próprio aplicativo. Sem integração com 99Pop.")
            .setSmallIcon(android.R.drawable.ic_menu_view)
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

    // ---- botão ----

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun overlayParams(width: Int, height: Int, flags: Int): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT,
        )

    private fun addButton() {
        val size = dp(56)
        val label = TextView(this).apply {
            text = getString(R.string.floating_button_label)
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 12f
            gravity = Gravity.CENTER
            contentDescription = "Botão flutuante. Toque para abrir os controles."
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF1B8A5A.toInt())
            }
            elevation = dp(4).toFloat()
        }
        val params = overlayParams(size, size, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(220)
        }
        label.setOnTouchListener(DragOrTapListener(params) { toggleMenu(params) })
        windowManager.addView(label, params)
        buttonView = label
    }

    /** Arrasta o botão; toque curto (sem arrasto) abre/fecha o menu. */
    private inner class DragOrTapListener(
        private val params: WindowManager.LayoutParams,
        private val onTap: () -> Unit,
    ) : View.OnTouchListener {
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
                    if (!moved && (kotlin.math.abs(dx) > dp(6) || kotlin.math.abs(dy) > dp(6))) moved = true
                    if (moved) {
                        params.x = startX + dx
                        params.y = startY + dy
                        windowManager.updateViewLayout(view, params)
                        removeMenu()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) onTap()
                }
            }
            return true
        }
    }

    // ---- menu de controles ----

    private fun toggleMenu(buttonParams: WindowManager.LayoutParams) {
        if (menuView != null) removeMenu() else showMenu(buttonParams)
    }

    private fun showMenu(buttonParams: WindowManager.LayoutParams) {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), 0xFFDDE5E0.toInt())
            }
            elevation = dp(8).toFloat()
        }
        column.addView(menuTitle("Controles · simulação"))
        column.addView(menuItem("Última oferta analisada", 0xFF15211B.toInt()) { showLatestOffer() })
        column.addView(menuItem("Balão verde (exemplo)", FunctionalColors.Good.toArgb()) { showBalloon(PreviewState.Good) })
        column.addView(menuItem("Balão amarelo (exemplo)", FunctionalColors.Attention.toArgb()) { showBalloon(PreviewState.Attention) })
        column.addView(menuItem("Balão vermelho (exemplo)", FunctionalColors.Bad.toArgb()) { showBalloon(PreviewState.Bad) })
        column.addView(menuItem("Ocultar balão", 0xFF15211B.toInt()) { removeBalloon() })
        column.addView(menuItem("Abrir aplicativo", 0xFF15211B.toInt()) { openApp() })
        column.addView(menuItem("Desligar botão", 0xFFB3261E.toInt()) { stopSelf() })

        val params = overlayParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = buttonParams.x + dp(64)
            y = buttonParams.y
        }
        column.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) removeMenu()
            false
        }
        windowManager.addView(column, params)
        menuView = column
    }

    private fun menuTitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 12f
        setTextColor(0xFF5F6E66.toInt())
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

    // ---- balão de exemplo (janela separada do botão) ----

    private fun showBalloon(state: PreviewState) {
        val accent = state.accent()
        showCard(
            state.seal,
            accent,
            listOf(
                Line("SIMULAÇÃO · dados de exemplo · sem 99Pop", 11f, GREY),
                Line(SampleOffer.MINUTES + " · " + SampleOffer.DISTANCE + " · ★ " + SampleOffer.RATING, 13f, GREY),
                Line("R$/km " + SampleOffer.PER_KM + "   R$/h " + SampleOffer.PER_HOUR + "   R$/min " + SampleOffer.PER_MINUTE, 17f, INK),
                Line("Selo informativo. Decisão manual; nada é aceito ou recusado.", 12f, accent),
            ),
        )
    }

    /**
     * Balão da última oferta publicada. Só mostra cores/valores se a oferta ainda é a atual E está
     * dentro do prazo; caso contrário mostra indisponibilidade, nunca o resultado antigo.
     */
    private fun showLatestOffer() {
        val published = BalloonHub.latestAny()
        if (published == null) {
            showCard("INDISPONÍVEL", GREY, listOf(Line("Nenhuma oferta analisada ainda.", 14f, INK)))
            return
        }
        val now = SystemClock.elapsedRealtime()
        val slot = BalloonHub.store.slot(published.platform, now)
        val origin = published.platform.label + " · " + published.origin.label
        if (slot !is Slot.Fresh || slot.snapshot.id != published.offerId) {
            showCard(
                "INDISPONÍVEL",
                GREY,
                listOf(
                    Line(origin, 11f, GREY),
                    Line("A última oferta é antiga e não vale mais. Nenhum resultado é mostrado.", 14f, INK),
                ),
            )
            return
        }
        val v = published.values
        val band = published.band
        val accent = band?.let { it.accent() } ?: GREY
        val seal = when (band) {
            Band.Good -> PreviewState.Good.seal
            Band.Attention -> PreviewState.Attention.seal
            Band.Bad -> PreviewState.Bad.seal
            null -> "SEM COR GERAL"
        }
        showCard(
            seal,
            accent,
            listOf(
                Line(origin + " · há " + (slot.ageMs / 1000) + " s", 11f, GREY),
                Line(v.time + " · " + v.distance + " · ★ " + v.rating, 13f, GREY),
                Line("R$/km " + v.perKm + "   R$/h " + v.perHour + "   R$/min " + v.perMinute, 17f, INK),
                Line(published.note, 12f, accent),
            ),
        )
    }

    private class Line(val text: String, val size: Float, val color: Int)

    private fun PreviewState.accent(): Int = when (this) {
        PreviewState.Good -> FunctionalColors.Good.toArgb()
        PreviewState.Attention -> FunctionalColors.Attention.toArgb()
        PreviewState.Bad -> FunctionalColors.Bad.toArgb()
    }

    private fun Band.accent(): Int = when (this) {
        Band.Good -> PreviewState.Good.accent()
        Band.Attention -> PreviewState.Attention.accent()
        Band.Bad -> PreviewState.Bad.accent()
    }

    private fun showCard(sealText: String, accent: Int, lines: List<Line>) {
        removeBalloon()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(
            TextView(this).apply {
                text = sealText
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 12f
                setPadding(dp(12), dp(4), dp(12), dp(4))
                background = GradientDrawable().apply {
                    setColor(accent)
                    cornerRadii = floatArrayOf(dp(10).toFloat(), dp(10).toFloat(), dp(10).toFloat(), dp(10).toFloat(), 0f, 0f, 0f, 0f)
                }
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dp(14)
            },
        )
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                cornerRadius = dp(16).toFloat()
                setStroke(dp(2), accent)
            }
        }
        lines.forEach { card.addView(label(it.text, it.size, it.color)) }
        root.addView(card)

        val params = overlayParams(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(96)
        }
        windowManager.addView(root, params)
        balloonView = root
        handler.postDelayed(hideBalloon, BALLOON_SECONDS * 1000L)
    }

    private fun label(text: String, size: Float, color: Int) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
    }

    private fun removeBalloon() {
        handler.removeCallbacks(hideBalloon)
        balloonView?.let { runCatching { windowManager.removeView(it) } }
        balloonView = null
    }

    companion object {
        const val ACTION_STOP = "br.app.motorista.floating.STOP"
        private const val CHANNEL_ID = "floating_control"
        private const val NOTIFICATION_ID = 1
        private const val BALLOON_SECONDS = 8
        private const val GREY = 0xFF5F6E66.toInt()
        private const val INK = 0xFF15211B.toInt()

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running.asStateFlow()

        fun start(context: Context) {
            context.startForegroundService(Intent(context, FloatingControlService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingControlService::class.java))
        }
    }
}
