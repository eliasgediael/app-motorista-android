package br.app.motorista.ui.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import br.app.motorista.data.PrefsStore
import br.app.motorista.data.StoredPrefs
import br.app.motorista.data.ThemeMode
import br.app.motorista.domain.card.CardStyle
import br.app.motorista.domain.card.PreviewState
import br.app.motorista.domain.assessment.CriterionRule
import br.app.motorista.domain.assessment.EvaluationPolicy
import br.app.motorista.domain.criteria.CriterionDraft
import br.app.motorista.domain.criteria.CriterionMetric
import br.app.motorista.domain.criteria.CriterionValidator
import br.app.motorista.domain.criteria.DistanceTimeBasis
import br.app.motorista.domain.offer.Platform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class RidePlatform(val label: String) { P99("99"), Uber("Uber"), InDrive("inDrive") }

/** Campos de texto do simulador manual. Vazio = dado ausente. */
data class SimulatorForm(
    val fare: String = "",
    val pickupKm: String = "",
    val pickupMin: String = "",
    val tripKm: String = "",
    val tripMin: String = "",
    val returnKm: String = "",
    val returnMin: String = "",
    val rating: String = "",
    /** Base usada só para EXIBIR métricas do cartão; cada critério usa a base própria. */
    val displayBasis: DistanceTimeBasis? = null,
    /** Plataforma da oferta digitada. Sem escolha, não há como publicar (origem desconhecida). */
    val platform: Platform? = null,
)

data class PreferencesState(
    val drafts: Map<CriterionMetric, CriterionDraft> = emptyMap(),
    val editingMetric: CriterionMetric = CriterionMetric.RevenuePerKm,
    /** Seleção de apps é só preferência; não ativa leitura nem integração. */
    val selectedPlatforms: Set<RidePlatform> = emptySet(),
    val cardStyle: CardStyle = CardStyle(),
    val previewState: PreviewState = PreviewState.Good,
    /** Métrica cujo rascunho foi "revisado" na tela 46. Revisão não confirma regra. */
    val reviewedMetric: CriterionMetric? = null,
    val policy: EvaluationPolicy = EvaluationPolicy(),
    val simulator: SimulatorForm = SimulatorForm(),
    /** Opacidade do BOTÃO flutuante; independente da opacidade do balão (cardStyle.opacity). */
    val buttonOpacity: Float = 1f,
    val themeMode: ThemeMode = ThemeMode.System,
) {
    /** Critérios ativos e válidos. Rascunho incompleto nunca vira regra. */
    fun activeRules(): List<CriterionRule> = CriterionMetric.entries.mapNotNull { metric ->
        val draft = draft(metric)
        val v = CriterionValidator.validate(metric, draft)
        if (draft.enabled && v.isValid) CriterionRule(metric, v.minimum!!, v.good!!, draft.basis) else null
    }

    fun draft(metric: CriterionMetric): CriterionDraft = drafts[metric] ?: CriterionDraft()
}

/**
 * Estado em memória compartilhado entre telas de critérios e cartão. Sobrevive a rotação,
 * mas NÃO persiste em disco (Room/DataStore ficam para a etapa de persistência).
 */
class PreferencesViewModel(app: Application) : AndroidViewModel(app) {
    private val store = PrefsStore(app)

    private val _state = MutableStateFlow(fromStored(store.load()))
    val state: StateFlow<PreferencesState> = _state.asStateFlow()

    private fun fromStored(p: StoredPrefs) = PreferencesState(
        drafts = p.drafts,
        selectedPlatforms = RidePlatform.entries.filter { it.name in p.selectedPlatforms }.toSet(),
        cardStyle = p.cardStyle,
        policy = p.policy,
        buttonOpacity = p.buttonOpacity,
        themeMode = p.themeMode,
    )

    /** Alteração de preferência durável: atualiza o estado e grava em disco. */
    private fun persistChange(f: (PreferencesState) -> PreferencesState) {
        _state.update(f)
        val s = _state.value
        store.save(StoredPrefs(s.drafts, s.selectedPlatforms.map { it.name }.toSet(), s.cardStyle, s.policy, s.buttonOpacity, s.themeMode))
    }

    /** Recarrega do disco (depois de restaurar um backup). */
    fun reload() {
        _state.update { fromStored(store.load()).copy(simulator = it.simulator) }
    }

    /** Preferências atuais como JSON (para o backup). */
    fun exportPrefsJson(): org.json.JSONObject {
        val s = _state.value
        return br.app.motorista.data.PrefsCodec.toJson(StoredPrefs(s.drafts, s.selectedPlatforms.map { it.name }.toSet(), s.cardStyle, s.policy, s.buttonOpacity, s.themeMode))
    }

    /** Restaura preferências de um backup já validado. */
    fun importPrefsJson(o: org.json.JSONObject) {
        store.save(br.app.motorista.data.PrefsCodec.fromJson(o))
        reload()
    }

    /** Apaga as preferências gravadas e volta ao padrão (sem valores pessoais). */
    fun wipePrefs() {
        store.clear()
        _state.update { fromStored(StoredPrefs()).copy(simulator = SimulatorForm()) }
    }

    fun setThemeMode(mode: ThemeMode) = persistChange { it.copy(themeMode = mode) }

    fun editMetric(metric: CriterionMetric) = _state.update { it.copy(editingMetric = metric, reviewedMetric = null) }

    fun updateDraft(metric: CriterionMetric, change: (CriterionDraft) -> CriterionDraft) = persistChange {
        it.copy(drafts = it.drafts + (metric to change(it.draft(metric))), reviewedMetric = null)
    }

    fun markReviewed(metric: CriterionMetric) = _state.update { it.copy(reviewedMetric = metric) }

    fun togglePlatform(platform: RidePlatform) = persistChange {
        val next = if (platform in it.selectedPlatforms) it.selectedPlatforms - platform else it.selectedPlatforms + platform
        it.copy(selectedPlatforms = next)
    }

    fun updateCardStyle(change: (CardStyle) -> CardStyle) = persistChange { it.copy(cardStyle = change(it.cardStyle)) }

    fun setPreviewState(state: PreviewState) = _state.update { it.copy(previewState = state) }

    fun updatePolicy(change: (EvaluationPolicy) -> EvaluationPolicy) = persistChange { it.copy(policy = change(it.policy)) }

    fun updateSimulator(change: (SimulatorForm) -> SimulatorForm) = _state.update { it.copy(simulator = change(it.simulator)) }

    fun setButtonOpacity(value: Float) = persistChange { it.copy(buttonOpacity = value) }
}
