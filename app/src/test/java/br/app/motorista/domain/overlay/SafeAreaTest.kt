package br.app.motorista.domain.overlay

import br.app.motorista.domain.overlay.SafeAreaPlanner.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeAreaTest {
    private val screen = IntBox(0, 0, 1000, 2000)
    private val full = Size(800, 400)
    private val compact = Size(800, 150)

    @Test
    fun unknownContextHides() {
        val p = SafeAreaPlanner.plan(screen, null, full, compact, 20)
        assertTrue(p is Placement.Hidden)
    }

    @Test
    fun emptyExclusionsUseTopCentered() {
        val p = SafeAreaPlanner.plan(screen, emptyList(), full, compact, 20) as Placement.At
        assertEquals(IntBox(100, 0, 900, 400), p.box)
        assertFalse(p.compact)
    }

    @Test
    fun neverOverlapsAnExclusionPlusMargin() {
        val ex = listOf(IntBox(0, 0, 1000, 300), IntBox(0, 1700, 1000, 2000))
        val p = SafeAreaPlanner.plan(screen, ex, full, compact, 20) as Placement.At
        ex.forEach { assertFalse(SafeAreaPlanner.overlaps(p.box, SafeAreaPlanner.grow(it, 20))) }
        assertTrue(SafeAreaPlanner.inside(screen, p.box))
    }

    @Test
    fun fallsBackToCompactWhenFullDoesNotFit() {
        // sobram 200px livres no meio: cabe o compacto (150), não o completo (400)
        val ex = listOf(IntBox(0, 0, 1000, 900), IntBox(0, 1180, 1000, 2000))
        val p = SafeAreaPlanner.plan(screen, ex, full, compact, 20) as Placement.At
        assertTrue(p.compact)
    }

    @Test
    fun hidesWhenNothingFits() {
        val ex = listOf(IntBox(0, 0, 1000, 2000))
        assertTrue(SafeAreaPlanner.plan(screen, ex, full, compact, 20) is Placement.Hidden)
    }

    @Test
    fun marginIsRespected() {
        // faixa livre de 150 entre as exclusões, mas a margem de 20 de cada lado a reduz para 110: nem o compacto cabe
        val ex = listOf(IntBox(0, 0, 1000, 925), IntBox(0, 1075, 1000, 2000))
        assertTrue(SafeAreaPlanner.plan(screen, ex, full, compact, 20) is Placement.Hidden)
        assertTrue(SafeAreaPlanner.plan(screen, ex, full, compact, 0) is Placement.At)
    }
}
