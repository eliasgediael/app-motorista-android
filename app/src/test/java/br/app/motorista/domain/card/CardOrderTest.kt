package br.app.motorista.domain.card

import org.junit.Assert.assertEquals
import org.junit.Test

class CardOrderTest {
    @Test
    fun moveSwapsNeighboursAndStopsAtEdges() {
        val s = CardStyle()
        val down = s.moved(CardField.TotalTime, 1)
        assertEquals(CardField.TotalDistance, down.fieldOrder[0])
        assertEquals(CardField.TotalTime, down.fieldOrder[1])
        assertEquals(s, s.moved(CardField.TotalTime, -1))
        assertEquals(s, s.moved(CardField.EstimatedResult, 1))
    }

    @Test
    fun visibleFieldsFollowOrderAndLayoutSet() {
        val s = CardStyle(enabledFields = setOf(CardField.RevenuePerKm, CardField.TotalTime), fieldOrder = CardField.entries.reversed())
        assertEquals(listOf(CardField.RevenuePerKm, CardField.TotalTime), s.visibleFields())
        val sec = s.copy(layout = CardLayout.Secondary, secondaryFields = setOf(CardField.OfferValue))
        assertEquals(listOf(CardField.OfferValue), sec.visibleFields())
    }

    @Test
    fun secondaryFieldsAreIndependentFromFullFields() {
        val s = CardStyle().secondaryToggled(CardField.OfferValue, true)
        assertEquals(true, CardField.OfferValue in s.secondaryFields)
        assertEquals(false, CardField.OfferValue in s.enabledFields)
    }
}
