package br.app.motorista.copilot

import android.content.Context
import br.app.motorista.domain.journey.JourneyState
import br.app.motorista.domain.journey.JourneyStatus

/** Persistência local mínima da jornada (SharedPreferences), para sobreviver a o processo encerrar. */
internal object JourneyStore {
    private const val FILE = "journey"

    fun load(context: Context): JourneyState {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val status = runCatching { JourneyStatus.valueOf(p.getString("status", null) ?: "NotStarted") }.getOrDefault(JourneyStatus.NotStarted)
        return JourneyState(
            status = status,
            startedAtMs = p.getLong("startedAtMs", 0),
            segmentStartMs = p.getLong("segmentStartMs", 0),
            accumulatedMs = p.getLong("accumulatedMs", 0),
            pausedTotalMs = p.getLong("pausedTotalMs", 0),
            pausedSinceMs = p.getLong("pausedSinceMs", 0),
            pauseCount = p.getInt("pauseCount", 0),
        )
    }

    fun save(context: Context, s: JourneyState) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString("status", s.status.name)
            .putLong("startedAtMs", s.startedAtMs)
            .putLong("segmentStartMs", s.segmentStartMs)
            .putLong("accumulatedMs", s.accumulatedMs)
            .putLong("pausedTotalMs", s.pausedTotalMs)
            .putLong("pausedSinceMs", s.pausedSinceMs)
            .putInt("pauseCount", s.pauseCount)
            .apply()
    }
}
