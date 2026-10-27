package br.app.motorista.copilot

import android.content.Context
import br.app.motorista.floating.FloatingControlService

/** Ações disponíveis ao menu (flutuante e dentro do app) e ao Painel. Nenhuma age sobre apps de terceiros. */
interface CopilotActions {
    fun requestRecording()
    fun stopRecording()
    fun cancelRecording()
    fun setVoice(on: Boolean)
    fun setAnalysis(on: Boolean)
    fun openAccessibilitySettings()
    fun captureSample()
    fun journeyStart()
    fun journeyPause()
    fun journeyResume()
    fun setBalloonHidden(hidden: Boolean)
    fun openApp(route: String? = null)
    fun activeResources(): List<String>

    /** Chamado só depois de o usuário confirmar. Para os recursos ativos e remove as janelas. */
    fun shutdownCopilot()
}

class ControllerActions(private val context: Context) : CopilotActions {
    override fun requestRecording() = CopilotController.requestRecording(context)
    override fun stopRecording() = CopilotController.stopRecording(context)
    override fun cancelRecording() = CopilotController.stopRecording(context)
    override fun setVoice(on: Boolean) = CopilotController.setVoice(on)
    override fun setAnalysis(on: Boolean) = CopilotController.setAnalysis(on)
    override fun openAccessibilitySettings() {
        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    override fun captureSample() {
        val r = br.app.motorista.reader.OfferReaderService.captureSample(context)
        br.app.motorista.reader.ReaderState.setSample(r.fold({ "Amostra salva: " + it.name }, { it.message ?: "Falha ao capturar" }))
    }
    override fun journeyStart() = CopilotController.journeyStart()
    override fun journeyPause() = CopilotController.journeyPause()
    override fun journeyResume() = CopilotController.journeyResume()
    override fun setBalloonHidden(hidden: Boolean) = CopilotController.setBalloonHidden(hidden)
    override fun openApp(route: String?) = CopilotController.openApp(context, route)
    override fun activeResources(): List<String> = CopilotController.activeResources()

    override fun shutdownCopilot() {
        CopilotController.shutdownResources(context)
        FloatingControlService.stop(context)
    }
}
