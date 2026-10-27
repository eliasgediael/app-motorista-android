package br.app.motorista.data

import br.app.motorista.domain.assessment.BoundaryPolicy
import br.app.motorista.domain.assessment.EvaluationPolicy
import br.app.motorista.domain.card.CardField
import br.app.motorista.domain.card.CardFontSize
import br.app.motorista.domain.card.CardStyle
import br.app.motorista.domain.criteria.CriterionDraft
import br.app.motorista.domain.criteria.CriterionMetric
import br.app.motorista.domain.criteria.DistanceTimeBasis
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrefsCodecTest {
    @Test
    fun roundTripKeepsUserValuesOnly() {
        val p = StoredPrefs(
            drafts = mapOf(CriterionMetric.RevenuePerKm to CriterionDraft(true, "1,5", "2,5", DistanceTimeBasis.TripOnly)),
            selectedPlatforms = setOf("P99"),
            cardStyle = CardStyle(fontSize = CardFontSize.L, opacity = 0.6f, fieldOrder = CardField.entries.reversed()),
            policy = EvaluationPolicy(boundary = BoundaryPolicy.MinimumIsBad),
            buttonOpacity = 0.7f,
            themeMode = ThemeMode.Dark,
        )
        val back = PrefsCodec.fromJson(PrefsCodec.toJson(p))
        assertEquals(p.drafts, back.drafts)
        assertEquals(p.cardStyle, back.cardStyle)
        assertEquals(BoundaryPolicy.MinimumIsBad, back.policy.boundary)
        assertNull(back.policy.combination)
        assertEquals(0.7f, back.buttonOpacity, 0.0001f)
        assertEquals(ThemeMode.Dark, back.themeMode)
    }

    @Test
    fun emptyJsonGivesPendingDefaults() {
        val p = PrefsCodec.fromJson(JSONObject())
        assertEquals(emptyMap<CriterionMetric, CriterionDraft>(), p.drafts)
        assertEquals(3, p.policy.pending.size)
        assertEquals(ThemeMode.System, p.themeMode)
    }

    @Test
    fun brokenFieldOrderFallsBackToDefault() {
        val o = JSONObject("{\"card\":{\"order\":[\"TotalTime\",\"TotalTime\"]}}")
        assertEquals(CardField.entries, PrefsCodec.fromJson(o).cardStyle.fieldOrder)
    }
}
