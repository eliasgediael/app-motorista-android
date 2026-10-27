package br.app.motorista.domain.journey

enum class JourneyStatus(val label: String) {
    NotStarted("Não iniciada"),
    Running("Em andamento"),
    Paused("Pausada"),
    Ended("Encerrada"),
}

/**
 * Estado serializável da jornada. Tempos em milissegundos de relógio de parede (permite recuperar
 * depois de o processo morrer). [accumulatedMs] é o tempo em andamento já fechado, sem pausas.
 */
data class JourneyState(
    val status: JourneyStatus = JourneyStatus.NotStarted,
    val startedAtMs: Long = 0,
    val segmentStartMs: Long = 0,
    val accumulatedMs: Long = 0,
    val pausedTotalMs: Long = 0,
    val pausedSinceMs: Long = 0,
    val pauseCount: Int = 0,
)

/**
 * Transições da jornada: iniciar, pausar, retomar, encerrar. Cada transição só vale no estado certo e
 * devolve o novo estado, ou null se foi recusada (ação duplicada ou fora de ordem). Pausar e retomar
 * nunca zeram o tempo; encerrar é ação separada.
 */
object JourneyClock {
    fun start(s: JourneyState, now: Long): JourneyState? =
        if (s.status == JourneyStatus.NotStarted || s.status == JourneyStatus.Ended) {
            JourneyState(JourneyStatus.Running, startedAtMs = now, segmentStartMs = now)
        } else {
            null
        }

    fun pause(s: JourneyState, now: Long): JourneyState? {
        if (s.status != JourneyStatus.Running) return null
        return s.copy(
            status = JourneyStatus.Paused,
            accumulatedMs = s.accumulatedMs + (now - s.segmentStartMs).coerceAtLeast(0),
            pausedSinceMs = now,
            pauseCount = s.pauseCount + 1,
        )
    }

    fun resume(s: JourneyState, now: Long): JourneyState? {
        if (s.status != JourneyStatus.Paused) return null
        return s.copy(
            status = JourneyStatus.Running,
            segmentStartMs = now,
            pausedTotalMs = s.pausedTotalMs + (now - s.pausedSinceMs).coerceAtLeast(0),
        )
    }

    fun end(s: JourneyState, now: Long): JourneyState? = when (s.status) {
        JourneyStatus.Running -> s.copy(
            status = JourneyStatus.Ended,
            accumulatedMs = s.accumulatedMs + (now - s.segmentStartMs).coerceAtLeast(0),
        )
        JourneyStatus.Paused -> s.copy(
            status = JourneyStatus.Ended,
            pausedTotalMs = s.pausedTotalMs + (now - s.pausedSinceMs).coerceAtLeast(0),
        )
        else -> null
    }

    /** Tempo em andamento (sem pausas) até [now]. */
    fun workedMs(s: JourneyState, now: Long): Long = when (s.status) {
        JourneyStatus.Running -> s.accumulatedMs + (now - s.segmentStartMs).coerceAtLeast(0)
        JourneyStatus.Paused, JourneyStatus.Ended -> s.accumulatedMs
        JourneyStatus.NotStarted -> 0
    }

    /** Tempo total em pausa até [now]. */
    fun pausedMs(s: JourneyState, now: Long): Long = when (s.status) {
        JourneyStatus.Paused -> s.pausedTotalMs + (now - s.pausedSinceMs).coerceAtLeast(0)
        else -> s.pausedTotalMs
    }

    fun format(ms: Long): String {
        val totalSeconds = ms / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val sec = totalSeconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
    }
}
