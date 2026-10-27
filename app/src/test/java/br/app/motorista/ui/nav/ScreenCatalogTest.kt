package br.app.motorista.ui.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenCatalogTest {
    @Test
    fun hasFiftyUniqueScreens() {
        assertEquals(50, ScreenCatalog.all.size)
        assertEquals(50, ScreenCatalog.all.map { it.id }.toSet().size)
    }

    @Test
    fun homeAndBottomTabsResolve() {
        assertNotNull(ScreenCatalog.find(ROUTE_HOME))
        BottomTab.entries.forEach { assertNotNull(it.label, ScreenCatalog.find(it.screenId)) }
    }

    @Test
    fun homeActionTargetsExist() {
        val targets = listOf(
            "ferramentas", "conta", "atalhos", "criterios", "camera", "jornada",
            "cartao", "custo", "alertas", "mensagem",
        )
        targets.forEach { assertNotNull(it, ScreenCatalog.find(it)) }
    }

    @Test
    fun implementedIdsAreInCatalog() {
        assertTrue(ScreenCatalog.implemented.all { ScreenCatalog.find(it) != null })
    }
}
