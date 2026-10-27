package br.app.motorista.copilot

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.lifecycle.LifecycleService
import br.app.motorista.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor

/**
 * Gravação de segurança VISÍVEL: serviço em primeiro plano do tipo câmera, com notificação e cronômetro.
 * Só inicia por ação do usuário na tela de gravação (o Android exige o app visível para abrir a câmera).
 * Sem áudio. Arquivo salvo na pasta do próprio app. Para ao receber Parar, ao falhar ou se for destruído;
 * a câmera é sempre liberada. Nunca é iniciado em segundo plano nem retomado sozinho.
 */
class RecordingService : LifecycleService() {
    private val handler = Handler(Looper.getMainLooper())
    private val mainExecutor = Executor { handler.post(it) }
    private var provider: ProcessCameraProvider? = null
    private var recording: Recording? = null
    private var file: File? = null
    private var finalized = false
    private var stopRequested = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        CopilotController.ensureInit(this)
        when (intent?.action) {
            ACTION_STOP -> stopRecording()
            else -> startRecording(intent?.getIntExtra(EXTRA_LENS, CameraSelector.LENS_FACING_BACK) ?: CameraSelector.LENS_FACING_BACK)
        }
        return START_NOT_STICKY
    }

    private fun startRecording(lens: Int) {
        if (recording != null) return
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            CopilotController.onCameraPermissionDenied()
            stopSelf()
            return
        }
        try {
            startInForeground()
        } catch (e: Exception) {
            // Ex.: Android recusou iniciar câmera em primeiro plano porque o app não estava visível.
            CopilotController.onRecordingFailed("O Android não permitiu abrir a câmera agora: " + (e.message ?: e.javaClass.simpleName))
            stopSelf()
            return
        }
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val cameraProvider = future.get()
                if (stopRequested) {
                    // Parar chegou antes de a câmera abrir: não abre.
                    stopRecording()
                    return@addListener
                }
                provider = cameraProvider
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
                    .build()
                val capture = VideoCapture.withOutput(recorder)
                val selector = CameraSelector.Builder().requireLensFacing(lens).build()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, selector, capture)

                val dir = getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: filesDir
                dir.mkdirs()
                val target = File(dir, "gravacao-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".mp4")
                file = target
                // Sem withAudioEnabled(): gravação somente de vídeo.
                recording = recorder.prepareRecording(this, FileOutputOptions.Builder(target).build())
                    .start(mainExecutor) { event -> onEvent(event) }
            } catch (e: Exception) {
                fail("Não foi possível abrir a câmera: " + (e.message ?: e.javaClass.simpleName))
            }
        }, mainExecutor)
    }

    private fun onEvent(event: VideoRecordEvent) {
        when (event) {
            is VideoRecordEvent.Start -> CopilotController.onRecordingStarted()
            is VideoRecordEvent.Finalize -> {
                if (finalized) return
                finalized = true
                val error = event.error
                release()
                if (error != VideoRecordEvent.Finalize.ERROR_NONE && error != VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED) {
                    // Arquivo parcial pode existir; nada é apagado. O estado mostra o erro.
                    CopilotController.onRecordingFailed("A gravação terminou com erro (código " + error + "). O arquivo pode estar incompleto.")
                } else {
                    CopilotController.onRecordingFinished(file?.absolutePath)
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> Unit
        }
    }

    private fun stopRecording() {
        stopRequested = true
        val active = recording
        if (active == null) {
            if (finalized) return
            finalized = true
            release()
            CopilotController.onRecordingFinished(null)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        active.stop() // o resultado chega em Finalize
    }

    private fun fail(message: String) {
        release()
        CopilotController.onRecordingFailed(message)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Libera a câmera sempre. */
    private fun release() {
        recording = null
        runCatching { provider?.unbindAll() }
        provider = null
    }

    override fun onDestroy() {
        // Destruído sem Finalize (ex.: sistema): fecha o arquivo e libera a câmera.
        if (!finalized) {
            runCatching { recording?.stop() }
            release()
            CopilotController.onRecordingFinished(file?.absolutePath)
        }
        super.onDestroy()
    }

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Gravação de segurança", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, RecordingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Gravando (visível)")
            .setContentText("Vídeo sem áudio, salvo só neste aparelho.")
            .setSmallIcon(R.drawable.no_ponto_simbolo)
            .setOngoing(true)
            .setUsesChronometer(true)
            .setWhen(System.currentTimeMillis())
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_media_pause),
                    "Parar gravação",
                    stop,
                ).build(),
            )
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val ACTION_STOP = "br.app.motorista.copilot.RECORDING_STOP"
        const val EXTRA_LENS = "lens"
        private const val CHANNEL_ID = "recording"
        private const val NOTIFICATION_ID = 2

        fun start(context: Context, lensFacing: Int) {
            context.startForegroundService(Intent(context, RecordingService::class.java).putExtra(EXTRA_LENS, lensFacing))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, RecordingService::class.java).setAction(ACTION_STOP))
        }
    }
}
