package br.app.motorista.floating

import br.app.motorista.domain.card.CardStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingConfigTest {
    @Test
    fun buttonAndBalloonOpacityAreIndependent() {
        FloatingConfig.update(CardStyle(opacity = 0.5f), 0.8f)
        assertEquals(0.5f, FloatingConfig.style.opacity, 0f)
        assertEquals(0.8f, FloatingConfig.buttonOpacity, 0f)
        FloatingConfig.update(CardStyle(opacity = 1f), 0.8f)
        assertEquals(0.8f, FloatingConfig.buttonOpacity, 0f)
    }

    @Test
    fun buttonOpacityIsClampedToLegibleRange() {
        FloatingConfig.update(CardStyle(), 0f)
        assertEquals(FloatingConfig.BUTTON_OPACITY_MIN, FloatingConfig.buttonOpacity, 0f)
        FloatingConfig.update(CardStyle(), 5f)
        assertEquals(1f, FloatingConfig.buttonOpacity, 0f)
    }

    @Test
    fun listenerFiresOnlyWhenButtonOpacityChanges() {
        FloatingConfig.update(CardStyle(), 1f)
        var calls = 0
        FloatingConfig.onButtonOpacityChanged = { calls++ }
        FloatingConfig.update(CardStyle(opacity = 0.4f), 1f)
        assertEquals(0, calls)
        FloatingConfig.update(CardStyle(), 0.6f)
        assertEquals(1, calls)
        FloatingConfig.onButtonOpacityChanged = null
        assertTrue(calls == 1)
    }
}
