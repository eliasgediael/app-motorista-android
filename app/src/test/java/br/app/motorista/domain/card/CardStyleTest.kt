package br.app.motorista.domain.card

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardStyleTest {
    @Test
    fun toggleAddsAndRemovesField() {
        val base = CardStyle()
        assertFalse(CardField.OfferValue in base.enabledFields)
        val on = base.toggled(CardField.OfferValue, true)
        assertTrue(CardField.OfferValue in on.enabledFields)
        assertFalse(CardField.OfferValue in on.toggled(CardField.OfferValue, false).enabledFields)
    }

    @Test
    fun defaultsMatchReferenceDisplay() {
        val s = CardStyle()
        assertEquals(CardFontSize.M, s.fontSize)
        assertEquals(0.9f, s.opacity, 0f)
        assertEquals(6, s.enabledFields.size)
    }

    @Test
    fun sealsAreInformativeLabels() {
        assertEquals(listOf("ACEITAR", "ATENÇÃO", "RECUSAR"), PreviewState.entries.map { it.seal })
    }
}
