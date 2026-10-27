package br.app.motorista.domain.assist

import br.app.motorista.domain.calc.OfferInput
import java.text.Normalizer

// ---------- Mensagem preparada ----------

data class MessageConfig(
    val greetingEnabled: Boolean,
    val greeting: String,
    val body: String,
    val etaEnabled: Boolean,
    /** Texto do aviso de chegada; use {eta} onde entra o número de minutos. */
    val etaTemplate: String,
    val etaMinutes: Long?,
)

/** Mensagem montada só com o que o usuário escreveu. Nada é enviado: o app apenas copia por ação explícita. */
object MessageTemplate {
    fun render(c: MessageConfig): String {
        val parts = mutableListOf<String>()
        if (c.greetingEnabled && c.greeting.isNotBlank()) parts += c.greeting.trim()
        if (c.body.isNotBlank()) parts += c.body.trim()
        if (c.etaEnabled && c.etaMinutes != null && c.etaTemplate.isNotBlank()) {
            parts += c.etaTemplate.trim().replace("{eta}", c.etaMinutes.toString())
        }
        return parts.joinToString(" ")
    }

    /** Aviso quando o ETA está ligado mas falta o valor: o texto não sai com "{eta}" solto. */
    fun etaProblem(c: MessageConfig): String? = when {
        !c.etaEnabled -> null
        c.etaMinutes == null -> "Informe os minutos do ETA"
        c.etaTemplate.isNotBlank() && !c.etaTemplate.contains("{eta}") -> "O texto do ETA precisa conter {eta}"
        else -> null
    }
}

// ---------- Alertas da viagem ----------

/** Limites definidos pelo usuário. null = alerta desligado (nunca um valor padrão). */
data class AlertRules(val maxPickupKm: java.math.BigDecimal?)

object AlertCalc {
    fun evaluate(input: OfferInput, rules: AlertRules): List<String> = buildList {
        val limit = rules.maxPickupKm
        val pickup = input.pickupMeters
        if (limit != null && pickup != null) {
            val limitMeters = limit.multiply(java.math.BigDecimal(1000))
            if (java.math.BigDecimal(pickup) > limitMeters) add("Embarque distante: acima do seu limite de " + limit.stripTrailingZeros().toPlainString() + " km")
        }
    }
}

// ---------- Locais sinalizados ----------

enum class PlaceKind(val label: String) { Pickup("Embarque"), Dropoff("Destino"), Market("Mercado/local")}

data class FlaggedTerm(val id: String, val text: String, val kind: PlaceKind)

/**
 * Correspondência de termos pessoais com texto digitado. Ignora maiúsculas e acentos. Texto de endereço
 * é usado só na hora: não é guardado. Um termo encontrado é um motivo para olhar com atenção, não prova de risco.
 */
object PlaceMatcher {
    /** Palavras iniciais do aviso de mercados. São editáveis pelo usuário e aparecem na tela; não são regra de risco. */
    val DEFAULT_MARKET_WORDS = listOf("mercado", "supermercado", "hipermercado", "atacado", "atacadista", "feira", "hortifruti", "açougue")

    fun parseWords(csv: String?): List<String> = (csv ?: DEFAULT_MARKET_WORDS.joinToString(", ")).split(',').map { it.trim() }.filter { it.isNotBlank() }

    /** Aviso de mercados: procura as palavras no texto de embarque e destino. Só um motivo para olhar com atenção. */
    fun marketMatches(pickupText: String, dropoffText: String, words: List<String>): List<String> = buildList {
        val p = normalize(pickupText)
        val d = normalize(dropoffText)
        words.forEach { w ->
            val n = normalize(w)
            if (n.isBlank()) return@forEach
            if (p.contains(n)) add("Embarque parece ser mercado (contém \"" + w + "\")")
            if (d.contains(n)) add("Destino parece ser mercado (contém \"" + w + "\")")
        }
    }

    fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "").trim()

    fun matches(pickupText: String, dropoffText: String, terms: List<FlaggedTerm>): List<String> = buildList {
        val p = normalize(pickupText)
        val d = normalize(dropoffText)
        terms.forEach { t ->
            val needle = normalize(t.text)
            if (needle.isBlank()) return@forEach
            val inPickup = p.contains(needle) && t.kind != PlaceKind.Dropoff
            val inDropoff = d.contains(needle) && t.kind != PlaceKind.Pickup
            if (inPickup) add("Embarque contém \"" + t.text + "\" (" + t.kind.label + ")")
            if (inDropoff) add("Destino contém \"" + t.text + "\" (" + t.kind.label + ")")
        }
    }
}

// ---------- Voz ----------

enum class VoiceItem(val label: String) {
    Origin("Plataforma e origem"),
    PerKm("Reais por quilômetro"),
    PerHour("Reais por hora"),
    PerMinute("Reais por minuto"),
    Assessment("Avaliação (cor)"),
}

object VoiceText {
    /** Itens escolhidos, na ordem escolhida, separados por ponto. Vazio se nada foi escolhido (nada é falado). */
    fun build(items: List<VoiceItem>, platform: String, origin: String, perKm: String, perHour: String, perMinute: String, assessment: String): String =
        items.joinToString(" ") {
            when (it) {
                VoiceItem.Origin -> "$platform, entrada $origin."
                VoiceItem.PerKm -> "Reais por quilômetro $perKm."
                VoiceItem.PerHour -> "Reais por hora $perHour."
                VoiceItem.PerMinute -> "Reais por minuto $perMinute."
                VoiceItem.Assessment -> "$assessment."
            }
        }

    fun parse(csv: String?): List<VoiceItem> =
        csv?.split(',')?.mapNotNull { n -> VoiceItem.entries.firstOrNull { it.name == n.trim() } }?.distinct() ?: emptyList()

    fun serialize(items: List<VoiceItem>): String = items.joinToString(",") { it.name }
}
