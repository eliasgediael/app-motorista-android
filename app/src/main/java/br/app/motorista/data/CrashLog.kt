package br.app.motorista.data

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Último erro que derrubou o aplicativo (ou que o aplicativo capturou), guardado só neste aparelho para
 * diagnóstico. Só tem texto técnico (pilha de chamadas); não grava dados de passageiros nem ofertas.
 */
object CrashLog {
    private const val FILE = "ultimo-erro.txt"
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { record(app, "Erro que fechou o aplicativo (thread ${thread.name})", error) }
            previous?.uncaughtException(thread, error)
        }
    }

    /** Erro tratado pelo próprio aplicativo, mas que o usuário precisa poder ver. */
    fun record(context: Context, what: String, error: Throwable) {
        val sw = StringWriter()
        error.printStackTrace(PrintWriter(sw))
        val text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()) + "\n" + what + "\n" + sw.toString().take(6000)
        File(context.applicationContext.filesDir, FILE).writeText(text)
    }

    fun read(context: Context): String? =
        File(context.applicationContext.filesDir, FILE).takeIf { it.exists() }?.readText()

    fun clear(context: Context) {
        File(context.applicationContext.filesDir, FILE).delete()
    }
}
