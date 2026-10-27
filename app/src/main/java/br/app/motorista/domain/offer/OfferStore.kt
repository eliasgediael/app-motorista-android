package br.app.motorista.domain.offer

/** Prazo de validade do dado. PROVISÓRIO: valor técnico a validar em aparelho; independe da duração visual do balão. */
object Freshness {
    const val PROVISIONAL_MAX_AGE_MS = 15_000L
}

sealed interface Slot {
    data class Fresh(val snapshot: OfferSnapshot, val ageMs: Long) : Slot
    data class Stale(val snapshot: OfferSnapshot, val ageMs: Long) : Slot
    data object Empty : Slot
}

/**
 * Última oferta POR plataforma. Dados de uma plataforma nunca ocupam o espaço de outra.
 * Oferta mais nova substitui a anterior; oferta mais antiga ou repetida é ignorada.
 */
class OfferStore(private val maxAgeMs: Long = Freshness.PROVISIONAL_MAX_AGE_MS) {
    private val slots = mutableMapOf<Platform, OfferSnapshot>()

    /** @return true se a oferta passou a ser a atual da sua plataforma. */
    @Synchronized
    fun accept(snapshot: OfferSnapshot): Boolean {
        val current = slots[snapshot.platform]
        if (current != null && (snapshot.id == current.id || snapshot.capturedAtMs < current.capturedAtMs)) return false
        slots[snapshot.platform] = snapshot
        return true
    }

    @Synchronized
    fun slot(platform: Platform, nowMs: Long): Slot {
        val s = slots[platform] ?: return Slot.Empty
        val age = nowMs - s.capturedAtMs
        return if (age < 0 || age > maxAgeMs) Slot.Stale(s, age) else Slot.Fresh(s, age)
    }

    /** Perda de contexto (troca de app, tela fechada): a oferta deixa de valer. */
    @Synchronized
    fun invalidate(platform: Platform) {
        slots.remove(platform)
    }

    @Synchronized
    fun clear() = slots.clear()
}
