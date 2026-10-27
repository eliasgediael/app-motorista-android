package br.app.motorista.copilot

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import br.app.motorista.MainActivity
import br.app.motorista.domain.copilot.FeatureMachine
import br.app.motorista.domain.copilot.FeatureStatus
import br.app.motorista.domain.journey.JourneyClock
import br.app.motorista.domain.journey.JourneyState
import br.app.motorista.domain.journey.JourneyStatus
import br.app.motorista.domain.offer.Slot
import br.app.motorista.floating.FloatingControlService
import br.app.motorista.offer.BalloonHub
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class MenuEntry(val label: String) { Analysis("Análise de corridas"), Recording("Gravação"), Voice("Avisos por voz"), Journey("Jornada") }

data class FeatureUi(val status: FeatureStatus, val detail: String? = null)

/** Estado único mostrado pelo Painel, pela tela de configuração, pelo menu flutuante e pela notificação. */
data class CopilotUiState(
    val copilotRunning: Boolean = false,
    val buttonVisible: Boolean = true,
    /** Só oculta o balão. Não desliga análise, voz nem gravação. */
    val balloonHidden: Boolean = false,
    val analysis: FeatureUi = FeatureUi(FeatureStatus.Off),
    val recording: FeatureUi = FeatureUi(FeatureStatus.Off),
    /** SystemClock.elapsedRealtime() em que a gravação foi confirmada; base da duração. */
    val recordingStartedAt: Long? = null,
    val lastRecordingPath: String? = null,
    val voice: FeatureUi = FeatureUi(FeatureStatus.Off),
    val journey: JourneyState = JourneyState(),
    val menuEntries: Set<MenuEntry> = MenuEntry.entries.toSet(),
) {
    companion object {
        const val ANALYSIS_REASON = "Sem fonte de leitura de ofertas autorizada e validada"
    }
}

/**
 * Fonte única de verdade dos recursos do copiloto. Cada recurso só muda para Ativo quando o serviço
 * real confirma; falha, negação de permissão, câmera ocupada ou TTS indisponível aparecem como estado,
 * nunca como ON falso. Preferência desejada (itens do menu) é separada do estado em execução.
 *
 * Em memória, exceto a jornada (SharedPreferences). Nada é retomado sozinho depois de o processo morrer.
 */
object CopilotController {
    private val main = Handler(Looper.getMainLooper())
    private lateinit var app: Context
    private var initialized = false

    private val recording = FeatureMachine()
    private val analysis = FeatureMachine()
    private var analysisDesired = false
    private val voice = FeatureMachine()
    private var journey = JourneyState()

    private var recordingStartedAt: Long? = null
    private var lastRecordingPath: String? = null
    private var recordingServiceRequested = false
    private var recordingDetail: String? = null
    private var buttonVisible = true
    private var balloonHidden = false
    private var menuEntries: Set<MenuEntry> = MenuEntry.entries.toSet()

    private var tts: TextToSpeech? = null
    private var lastAnnouncedOffer: String? = null

    private val _state = MutableStateFlow(CopilotUiState())
    val state: StateFlow<CopilotUiState> = _state.asStateFlow()

    /** Idempotente. Chamado por MainActivity e pelos serviços. */
    @Synchronized
    fun ensureInit(context: Context) {
        if (initialized) return
        app = context.applicationContext
        br.app.motorista.data.AppData.init(app)
        journey = JourneyStore.load(app)
        BalloonHub.addListener { main.post { onHubChanged() } }
        // Estado em execução nunca é herdado: gravação e voz começam desligadas em processo novo.
        initialized = true
        refreshPermissions()
    }

    // ---- publicação do estado ----

    private fun publish() {
        main.post {
            _state.value = CopilotUiState(
                copilotRunning = FloatingControlService.running.value,
                buttonVisible = buttonVisible,
                balloonHidden = balloonHidden,
                analysis = FeatureUi(analysis.status, analysis.detail),
                recording = FeatureUi(recording.status, recordingDetail ?: recording.detail),
                recordingStartedAt = recordingStartedAt,
                lastRecordingPath = lastRecordingPath,
                voice = FeatureUi(voice.status, voice.detail),
                journey = journey,
                menuEntries = menuEntries,
            )
        }
    }

    fun refresh() = publish()

    /** Permissão revogada ou concedida fora do app: o estado acompanha. */
    fun refreshPermissions() {
        if (!initialized) return
        val granted = app.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        when {
            !granted && recording.status == FeatureStatus.Off -> recording.fail(FeatureStatus.PermissionNeeded, "Câmera não autorizada")
            granted && recording.status == FeatureStatus.PermissionNeeded -> recording.forceOff()
            !granted && recording.status == FeatureStatus.Active -> Unit // o serviço reporta o fim da gravação
        }
        publish()
    }

    // ---- interface/menu ----

    fun setButtonVisible(visible: Boolean) {
        buttonVisible = visible
        publish()
    }

    fun setBalloonHidden(hidden: Boolean) {
        balloonHidden = hidden
        publish()
    }

    fun setMenuEntry(entry: MenuEntry, shown: Boolean) {
        menuEntries = if (shown) menuEntries + entry else menuEntries - entry
        publish()
    }

    // ---- análise de corridas (leitor de ofertas) ----

    /** O leitor só trabalha com a Análise LIGADA pelo usuário. */
    fun analysisWanted(): Boolean = analysisDesired

    /**
     * Liga/desliga a Análise. Só vira Ativa quando o serviço de acessibilidade está realmente conectado;
     * sem ele, fica "permissão necessária". Desligar invalida o balão de oferta atual.
     */
    fun setAnalysis(on: Boolean) {
        if (!initialized) return
        analysisDesired = on
        if (!on) {
            analysis.forceOff()
            br.app.motorista.domain.offer.Platform.entries.forEach { BalloonHub.invalidate(it) }
            publish()
            return
        }
        recomputeAnalysis()
    }

    fun onReaderConnectionChanged() {
        if (initialized) recomputeAnalysis()
    }

    private fun recomputeAnalysis() {
        if (!analysisDesired) {
            publish()
            return
        }
        val connected = br.app.motorista.reader.ReaderState.connected.value
        if (connected) {
            if (analysis.status != FeatureStatus.Active) {
                analysis.forceOff()
                analysis.beginStart()
                analysis.confirmActive()
            }
        } else {
            analysis.fail(FeatureStatus.PermissionNeeded, "Ative o serviço de acessibilidade do No Ponto nas configurações do Android")
        }
        publish()
    }

    // ---- gravação ----

    /**
     * Pedido de gravação a partir do menu/Painel. O Android exige etapa visível para a câmera: leva o
     * usuário à tela de gravação no app, onde ele confirma. Estado fica Iniciando até a confirmação real.
     */
    fun requestRecording(context: Context) {
        if (!recording.beginStart("Aguardando confirmação no app")) return
        recordingServiceRequested = false
        recordingDetail = "Aguardando confirmação no app"
        publish()
        openApp(context, "camera")
    }

    /** Usuário saiu da tela sem iniciar, ou cancelou: volta a Desligado sem gravar. */
    fun cancelRecordingRequest() {
        if (recording.status == FeatureStatus.Starting && !recordingServiceRequested) {
            recording.beginStop()
            recordingDetail = null
            publish()
            refreshPermissions()
        }
    }

    /** Chamado pela tela de gravação depois de consentimento e permissão. O estado só vira Ativo em [onRecordingStarted]. */
    fun startRecordingService(context: Context, lensFacing: Int) {
        if (recording.status == FeatureStatus.Off || recording.status == FeatureStatus.Error || recording.status == FeatureStatus.PermissionNeeded) {
            recording.beginStart()
        }
        if (recording.status != FeatureStatus.Starting) return
        recordingServiceRequested = true
        recordingDetail = "Abrindo a câmera"
        publish()
        RecordingService.start(context, lensFacing)
    }

    fun onCameraPermissionDenied() {
        recordingServiceRequested = false
        recordingDetail = null
        recording.fail(FeatureStatus.PermissionNeeded, "Câmera não autorizada")
        publish()
    }

    fun stopRecording(context: Context) {
        when {
            recording.status == FeatureStatus.Active -> {
                recording.beginStop()
                recordingDetail = "Finalizando o arquivo"
                publish()
                RecordingService.stop(context)
            }
            recording.status == FeatureStatus.Starting && recordingServiceRequested -> {
                // O serviço já foi pedido: ele decide o fim e reporta; nada é marcado como desligado antes.
                recordingDetail = "Cancelando"
                publish()
                RecordingService.stop(context)
            }
            recording.status == FeatureStatus.Starting -> {
                // Pedido ainda não confirmado no app: cancelado sem gravar.
                recording.beginStop()
                recordingDetail = null
                publish()
            }
        }
    }

    internal fun onRecordingStarted() {
        recording.confirmActive()
        recordingStartedAt = SystemClock.elapsedRealtime()
        recordingDetail = null
        publish()
    }

    internal fun onRecordingFailed(message: String) {
        recordingServiceRequested = false
        recordingStartedAt = null
        recordingDetail = null
        recording.fail(FeatureStatus.Error, message)
        publish()
    }

    internal fun onRecordingFinished(path: String?) {
        recordingServiceRequested = false
        recordingStartedAt = null
        recordingDetail = null
        if (path != null) lastRecordingPath = path
        recording.confirmOff()
        if (recording.status != FeatureStatus.Off) recording.forceOff()
        publish()
        refreshPermissions()
    }

    // ---- voz ----

    fun setVoice(enabled: Boolean) {
        if (!initialized) return
        if (enabled) startVoice() else stopVoice()
    }

    private fun startVoice() {
        if (!voice.beginStart()) return
        publish()
        tts = TextToSpeech(app) { status -> main.post { onTtsInit(status) } }
    }

    private fun onTtsInit(status: Int) {
        val engine = tts
        if (voice.status != FeatureStatus.Starting || engine == null) {
            // Desligado enquanto iniciava.
            engine?.shutdown()
            return
        }
        if (status != TextToSpeech.SUCCESS) {
            fail("Mecanismo de voz indisponível neste aparelho", FeatureStatus.Unavailable)
            return
        }
        val lang = engine.setLanguage(Locale("pt", "BR"))
        if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
            fail("Voz em português (Brasil) não está instalada", FeatureStatus.Unavailable)
            return
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = Unit

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                main.post { fail("Falha ao falar um aviso", FeatureStatus.Error) }
            }
        })
        voice.confirmActive()
        publish()
    }

    private fun fail(message: String, to: FeatureStatus) {
        tts?.shutdown()
        tts = null
        voice.fail(to, message)
        publish()
    }

    private fun stopVoice() {
        if (!voice.beginStop()) return
        // Cancela o que estiver falando ou pendente antes de liberar o mecanismo.
        tts?.stop()
        tts?.shutdown()
        tts = null
        voice.confirmOff()
        publish()
    }

    /** Fala um aviso, substituindo o que ainda estava pendente. Só se a voz está Ativa. */
    fun announce(text: String) {
        val engine = tts ?: return
        if (voice.status != FeatureStatus.Active) return
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "aviso-" + SystemClock.elapsedRealtime())
    }

    private fun onHubChanged() {
        val published = BalloonHub.latestAny() ?: return
        val slot = BalloonHub.store.slot(published.platform, SystemClock.elapsedRealtime())
        if (slot !is Slot.Fresh || slot.snapshot.id != published.offerId) return
        if (published.offerId == lastAnnouncedOffer) return
        lastAnnouncedOffer = published.offerId
        val v = published.values
        val colour = when (published.band) {
            br.app.motorista.domain.assessment.Band.Good -> "avaliação verde"
            br.app.motorista.domain.assessment.Band.Attention -> "avaliação amarela"
            br.app.motorista.domain.assessment.Band.Bad -> "avaliação vermelha"
            null -> "sem avaliação geral"
        }
        // O que falar e em que ordem vem das escolhas do usuário (Voz e notificações).
        val text = br.app.motorista.domain.assist.VoiceText.build(
            br.app.motorista.ui.assist.currentVoiceItems(), published.platform.label, published.origin.label, v.perKm, v.perHour, v.perMinute, colour,
        )
        if (text.isNotBlank()) announce(text)
    }

    // ---- jornada ----

    private fun journeyChange(next: JourneyState?) {
        if (next == null) return // ação duplicada ou fora de ordem: recusada
        journey = next
        JourneyStore.save(app, next)
        publish()
    }

    /** Depois de apagar todos os dados: a jornada volta ao estado inicial. */
    fun resetJourney() {
        journey = JourneyState()
        if (initialized) JourneyStore.save(app, journey)
        publish()
    }

    fun journeyStart() = journeyChange(JourneyClock.start(journey, System.currentTimeMillis()))
    fun journeyPause() = journeyChange(JourneyClock.pause(journey, System.currentTimeMillis()))
    fun journeyResume() = journeyChange(JourneyClock.resume(journey, System.currentTimeMillis()))
    fun journeyEnd() {
        val before = journey
        val now = System.currentTimeMillis()
        val ended = JourneyClock.end(before, now) ?: return
        journeyChange(ended)
        // Registro permanente da jornada encerrada, para os relatórios.
        br.app.motorista.data.AppData.journeys.upsert(
            br.app.motorista.domain.history.JourneyRecord(
                id = ended.startedAtMs.toString(), startedAtMs = ended.startedAtMs, endedAtMs = now,
                workedMs = JourneyClock.workedMs(ended, now), pausedMs = JourneyClock.pausedMs(ended, now), pauseCount = ended.pauseCount,
            ),
        )
    }

    // ---- copiloto ----

    /** Recursos que o desligamento do copiloto vai afetar, para o usuário confirmar. */
    fun activeResources(): List<String> = buildList {
        val s = _state.value
        if (s.recording.status == FeatureStatus.Active || s.recording.status == FeatureStatus.Starting) add("Gravação em andamento: será encerrada e o arquivo salvo")
        if (s.voice.status == FeatureStatus.Active || s.voice.status == FeatureStatus.Starting) add("Avisos por voz: serão desligados")
        if (s.journey.status == JourneyStatus.Running) add("Jornada em andamento: será pausada (nada é apagado)")
    }

    /** Para o que estiver ativo, de forma coerente. Nunca apaga registros. Não encerra a jornada, só pausa. */
    fun shutdownResources(context: Context) {
        stopRecording(context)
        setVoice(false)
        if (journey.status == JourneyStatus.Running) journeyPause()
    }

    fun openApp(context: Context, route: String? = null) {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        if (route != null) intent.putExtra(MainActivity.EXTRA_ROUTE, route)
        context.startActivity(intent)
    }
}
