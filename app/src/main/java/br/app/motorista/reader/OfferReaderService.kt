package br.app.motorista.reader

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import br.app.motorista.copilot.CopilotController
import br.app.motorista.data.AppData
import br.app.motorista.domain.offer.ParseOutcome
import br.app.motorista.domain.offer.Platform
import br.app.motorista.domain.offer.PlatformOfferParser
import br.app.motorista.domain.offer.ScreenSnapshot
import br.app.motorista.domain.offer.TextNode
import br.app.motorista.domain.offer.UnvalidatedParser
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estado público do leitor, para o Painel, o menu e a tela de leitura. */
object ReaderState {
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _lastReading = MutableStateFlow("Nenhuma leitura ainda")
    val lastReading: StateFlow<String> = _lastReading.asStateFlow()

    private val _lastPackage = MutableStateFlow<String?>(null)
    /** Último app em primeiro plano visto pelo leitor (para você mapear 99Pop/Uber). */
    val lastPackage: StateFlow<String?> = _lastPackage.asStateFlow()

    private val _lastSample = MutableStateFlow<String?>(null)
    /** Resultado da última amostra pedida (caminho do arquivo ou o motivo da falha). */
    val lastSample: StateFlow<String?> = _lastSample.asStateFlow()

    fun setSample(text: String?) {
        _lastSample.value = text
    }

    internal fun setConnected(v: Boolean) {
        _connected.value = v
    }

    internal fun setReading(text: String) {
        _lastReading.value = text
    }

    internal fun setPackage(pkg: String?) {
        _lastPackage.value = pkg
    }
}

/**
 * Leitor de ofertas SOMENTE LEITURA (serviço de acessibilidade, ativado por você nas configurações do Android).
 *
 * Nunca executa ação: não toca, não faz gesto, não aceita nem recusa corrida, não envia mensagem.
 * Só lê texto visível quando a Análise está LIGADA e o app em primeiro plano é um que você mapeou.
 * Campos de senha são ignorados. O texto lido não é gravado, exceto em amostras que VOCÊ pede.
 *
 * Os interpretadores por plataforma ainda não estão validados (faltam amostras reais): hoje a leitura
 * resulta em "layout não validado" e nenhuma oferta é inventada.
 */
class OfferReaderService : AccessibilityService() {
    private var lastEventMs = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        AppData.init(this)
        CopilotController.ensureInit(this)
        ReaderState.setConnected(true)
        CopilotController.onReaderConnectionChanged()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        ReaderState.setConnected(false)
        CopilotController.onReaderConnectionChanged()
        return super.onUnbind(intent)
    }

    override fun onInterrupt() = Unit

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == packageName) return
        ReaderState.setPackage(pkg)
        if (!CopilotController.analysisWanted()) return
        val platform = platformFor(pkg) ?: return
        val now = SystemClock.elapsedRealtime()
        if (now - lastEventMs < THROTTLE_MS) return
        lastEventMs = now
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return
        val nodes = collect(root)
        val snapshot = ScreenSnapshot(pkg, now, nodes)
        when (val out = parserFor(platform).parse(snapshot)) {
            is ParseOutcome.Unsupported -> ReaderState.setReading(platform.label + ": " + out.reason)
            is ParseOutcome.Incomplete -> ReaderState.setReading(platform.label + ": campos faltando (" + out.missing.joinToString(", ") + ")")
            is ParseOutcome.Parsed -> {
                ReaderState.setReading(platform.label + ": oferta interpretada")
                ReaderPublisher.publish(applicationContext, out.snapshot)
            }
        }
    }

    /** Texto visível da tela atual, sem campos de senha. Só leitura. */
    private fun collect(root: AccessibilityNodeInfo): List<TextNode> {
        val out = mutableListOf<TextNode>()
        fun walk(n: AccessibilityNodeInfo?, depth: Int) {
            if (n == null || depth > MAX_DEPTH || out.size > MAX_NODES) return
            if (!n.isPassword) {
                val t = (n.text ?: n.contentDescription)?.toString()
                if (!t.isNullOrBlank()) out += TextNode(t, n.viewIdResourceName)
            }
            for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1)
        }
        walk(root, 0)
        return out
    }

    companion object {
        private const val THROTTLE_MS = 400L
        private const val MAX_DEPTH = 40
        private const val MAX_NODES = 600

        @Volatile
        private var instance: OfferReaderService? = null

        /** Interpretadores por plataforma. Trocar o padrão só depois de amostras reais e teste em aparelho. */
        private val parsers: Map<Platform, PlatformOfferParser> = Platform.entries.associateWith { UnvalidatedParser(it) }

        fun parserFor(p: Platform): PlatformOfferParser = parsers.getValue(p)

        fun packageSetting(p: Platform) = if (p == Platform.P99) "readerPkg99" else "readerPkgUber"

        /** Plataforma mapeada pelo usuário para este pacote, ou null. Pacote não mapeado nunca é lido. */
        fun platformFor(pkg: String): Platform? = Platform.entries.firstOrNull { AppData.setting(packageSetting(it)) == pkg }

        /**
         * Amostra para ESTUDO do layout: texto visível do app em primeiro plano agora, em arquivo local.
         * Só por ação do usuário. Pode conter dados de passageiro: revisar e anonimizar antes de compartilhar.
         * @return arquivo gravado ou mensagem de erro.
         */
        fun captureSample(context: android.content.Context): Result<File> {
            val svc = instance ?: return Result.failure(IllegalStateException("Serviço de acessibilidade desligado"))
            // Todas as janelas visíveis (a oferta pode estar numa janela que não é a "ativa") exceto as deste app.
            val roots = (svc.windows.mapNotNull { it.root } + listOfNotNull(svc.rootInActiveWindow))
                .filter { it.packageName?.toString() != context.packageName }
                .distinctBy { System.identityHashCode(it) }
            if (roots.isEmpty()) return Result.failure(IllegalStateException("Nenhuma tela legível agora. Abra o aplicativo de corrida antes de capturar"))
            val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "amostras").also { it.mkdirs() }
            val file = File(dir, "amostra-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".txt")
            file.writeText(
                buildString {
                    appendLine("janelas=" + roots.size)
                    roots.forEach { root ->
                        val nodes = svc.collect(root)
                        appendLine("=== pacote=" + (root.packageName ?: "desconhecido") + " nos_com_texto=" + nodes.size + " filhos_raiz=" + root.childCount)
                        nodes.forEach { appendLine((it.viewId ?: "-") + " | " + it.text.replace("\n", " ")) }
                    }
                },
            )
            return Result.success(file)
        }

        fun samples(context: android.content.Context): List<File> =
            File(context.getExternalFilesDir(null) ?: context.filesDir, "amostras").listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}
