package br.app.motorista.domain.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayGeometryTest {
    private val bounds = IntBox(left = 0, top = 100, right = 1000, bottom = 2000)

    @Test
    fun insideStaysPut() {
        assertEquals(IntPoint(200, 500), OverlayGeometry.clamp(200, 500, 150, 150, bounds))
    }

    @Test
    fun clampsAllFourEdges() {
        assertEquals(IntPoint(0, 100), OverlayGeometry.clamp(-50, -50, 150, 150, bounds))
        assertEquals(IntPoint(850, 1850), OverlayGeometry.clamp(5000, 5000, 150, 150, bounds))
    }

    @Test
    fun itemLargerThanBoundsGoesToTopLeft() {
        assertEquals(IntPoint(0, 100), OverlayGeometry.clamp(300, 300, 2000, 3000, bounds))
    }

    @Test
    fun menuGoesRightWhenItFits() {
        val p = OverlayGeometry.placeMenu(IntPoint(100, 300), 150, 20, 400, 600, bounds)
        assertEquals(IntPoint(270, 300), p)
    }

    @Test
    fun menuFlipsLeftNearRightEdge() {
        val p = OverlayGeometry.placeMenu(IntPoint(800, 300), 150, 20, 400, 600, bounds)
        assertEquals(IntPoint(380, 300), p)
    }

    @Test
    fun menuIsClampedVertically() {
        val p = OverlayGeometry.placeMenu(IntPoint(100, 1900), 150, 20, 400, 600, bounds)
        assertEquals(1400, p.y)
        assertTrue(p.y + 600 <= bounds.bottom)
    }

    @Test
    fun menuNeverLeavesBoundsHorizontally() {
        val p = OverlayGeometry.placeMenu(IntPoint(0, 300), 150, 20, 1200, 600, bounds)
        assertEquals(0, p.x)
    }
}
