package br.app.motorista.data

import android.content.Context
import br.app.motorista.domain.assessment.BoundaryPolicy
import br.app.motorista.domain.assessment.CombinationPolicy
import br.app.motorista.domain.assessment.EvaluationPolicy
import br.app.motorista.domain.assessment.MissingDataPolicy
import br.app.motorista.domain.card.CardField
import br.app.motorista.domain.card.CardFontSize
import br.app.motorista.domain.card.CardLayout
import br.app.motorista.domain.card.CardStyle
import br.app.motorista.domain.criteria.CriterionDraft
import br.app.motorista.domain.criteria.CriterionMetric
import br.app.motorista.domain.criteria.DistanceTimeBasis
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode(val label: String) { System("Seguir o sistema"), Light("Claro"), Dark("Escuro") }

/**
 * Preferências do usuário que valem entre sessões: critérios (valores digitados por ele), política de
 * combinação, estilo do cartão e do botão. Nenhum valor padrão pessoal é inventado.
 */
data class StoredPrefs(
    val drafts: Map<CriterionMetric, CriterionDraft> = emptyMap(),
    val selectedPlatforms: Set<String> = emptySet(),
    val cardStyle: CardStyle = CardStyle(),
    val policy: EvaluationPolicy = EvaluationPolicy(),
    val buttonOpacity: Float = 1f,
    val themeMode: ThemeMode = ThemeMode.System,
)

object PrefsCodec {
    fun toJson(p: StoredPrefs): JSONObject {
        val drafts = JSONObject()
        p.drafts.forEach { (m, d) ->
            drafts.put(
                m.name,
                JSONObject().put("enabled", d.enabled).put("minimum", d.minimum).put("good", d.good)
                    .put("basis", d.basis?.name ?: JSONObject.NULL),
            )
        }
        val c = p.cardStyle
        return JSONObject()
            .put("drafts", drafts)
            .put("platforms", JSONArray(p.selectedPlatforms.toList()))
            .put(
                "card",
                JSONObject()
                    .put("layout", c.layout.name).put("highlightSeal", c.highlightSeal).put("showReason", c.showReason)
                    .put("fontSize", c.fontSize.name).put("opacity", c.opacity.toDouble())
                    .put("enabled", JSONArray(c.enabledFields.map { it.name }))
                    .put("order", JSONArray(c.fieldOrder.map { it.name }))
                    .put("secondary", JSONArray(c.secondaryFields.map { it.name })),
            )
            .put(
                "policy",
                JSONObject()
                    .put("boundary", p.policy.boundary?.name ?: JSONObject.NULL)
                    .put("combination", p.policy.combination?.name ?: JSONObject.NULL)
                    .put("missing", p.policy.missing?.name ?: JSONObject.NULL),
            )
            .put("buttonOpacity", p.buttonOpacity.toDouble())
            .put("theme", p.themeMode.name)
    }

    private inline fun <reified E : Enum<E>> enumOrNull(o: JSONObject, key: String): E? =
        if (o.has(key) && !o.isNull(key)) runCatching { enumValueOf<E>(o.getString(key)) }.getOrNull() else null

    private inline fun <reified E : Enum<E>> enumList(a: JSONArray?): List<E> =
        if (a == null) emptyList() else (0 until a.length()).mapNotNull { runCatching { enumValueOf<E>(a.getString(it)) }.getOrNull() }

    fun fromJson(o: JSONObject): StoredPrefs {
        val drafts = mutableMapOf<CriterionMetric, CriterionDraft>()
        o.optJSONObject("drafts")?.let { d ->
            CriterionMetric.entries.forEach { m ->
                d.optJSONObject(m.name)?.let { x ->
                    drafts[m] = CriterionDraft(
                        enabled = x.optBoolean("enabled", false),
                        minimum = x.optString("minimum", ""),
                        good = x.optString("good", ""),
                        basis = enumOrNull<DistanceTimeBasis>(x, "basis"),
                    )
                }
            }
        }
        val defaults = CardStyle()
        val card = o.optJSONObject("card")
        val style = if (card == null) defaults else {
            val order = enumList<CardField>(card.optJSONArray("order"))
            CardStyle(
                layout = enumOrNull<CardLayout>(card, "layout") ?: defaults.layout,
                highlightSeal = card.optBoolean("highlightSeal", defaults.highlightSeal),
                showReason = card.optBoolean("showReason", defaults.showReason),
                fontSize = enumOrNull<CardFontSize>(card, "fontSize") ?: defaults.fontSize,
                opacity = card.optDouble("opacity", defaults.opacity.toDouble()).toFloat(),
                enabledFields = if (card.has("enabled")) enumList<CardField>(card.optJSONArray("enabled")).toSet() else defaults.enabledFields,
                // Ordem salva só vale se contiver todos os campos exatamente uma vez.
                fieldOrder = if (order.size == CardField.entries.size && order.toSet().size == order.size) order else defaults.fieldOrder,
                secondaryFields = if (card.has("secondary")) enumList<CardField>(card.optJSONArray("secondary")).toSet() else defaults.secondaryFields,
            )
        }
        val pol = o.optJSONObject("policy")
        val policy = if (pol == null) EvaluationPolicy() else EvaluationPolicy(
            boundary = enumOrNull<BoundaryPolicy>(pol, "boundary"),
            combination = enumOrNull<CombinationPolicy>(pol, "combination"),
            missing = enumOrNull<MissingDataPolicy>(pol, "missing"),
        )
        val plats = o.optJSONArray("platforms")
        return StoredPrefs(
            drafts = drafts,
            selectedPlatforms = if (plats == null) emptySet() else (0 until plats.length()).map { plats.getString(it) }.toSet(),
            cardStyle = style,
            policy = policy,
            buttonOpacity = o.optDouble("buttonOpacity", 1.0).toFloat(),
            themeMode = enumOrNull<ThemeMode>(o, "theme") ?: ThemeMode.System,
        )
    }
}

/** Persistência em SharedPreferences (um JSON). */
class PrefsStore(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("preferences", Context.MODE_PRIVATE)

    fun load(): StoredPrefs = runCatching { PrefsCodec.fromJson(JSONObject(sp.getString("json", "{}") ?: "{}")) }.getOrDefault(StoredPrefs())

    fun save(p: StoredPrefs) {
        sp.edit().putString("json", PrefsCodec.toJson(p).toString()).apply()
    }

    fun clear() {
        sp.edit().clear().apply()
    }
}
