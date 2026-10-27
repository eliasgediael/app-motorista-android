package br.app.motorista.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import br.app.motorista.domain.card.CardField
import br.app.motorista.domain.card.CardFontSize
import br.app.motorista.domain.card.CardLayout
import br.app.motorista.domain.card.CardStyle
import br.app.motorista.domain.card.PreviewState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Teste AUTOMÁTICO de layout do balão (JVM + Robolectric, sem aparelho nem emulador).
 * Varre combinações de campos, fonte do app, fonte do sistema ampliada e larguras de tela e confere
 * que nenhum texto transborda, é cortado ou tem número quebrado em duas linhas.
 * Não substitui teste em aparelho real.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfferBalloonLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    private data class Case(
        val state: PreviewState,
        val style: CardStyle,
        val values: BalloonValues,
        val systemFontScale: Float,
        val widthDp: Int,
    )

    private val longValues = BalloonValues(
        time = "125 min", distance = "1234,5 km", rating = "4,92",
        perKm = "123,45", perHour = "1234,56", perMinute = "12,34",
        fare = "9999,99", result = "Indisponível", tag = "99Pop · manual",
    )

    private val allFields = CardField.entries.toSet()

    private var current by mutableStateOf<Case?>(null)

    private fun checkNoOverflow(label: String, widthDp: Int, density: Float) {
        val rootWidthPx = widthDp * density
        val nodes = rule.onRoot().fetchSemanticsNode().let { root ->
            val found = mutableListOf<androidx.compose.ui.semantics.SemanticsNode>()
            fun walk(n: androidx.compose.ui.semantics.SemanticsNode) {
                if (n.config.contains(SemanticsProperties.Text)) found += n
                n.children.forEach { walk(it) }
            }
            walk(root)
            found
        }
        assertTrue("$label: nenhum texto encontrado", nodes.isNotEmpty())
        val problems = mutableListOf<String>()
        nodes.forEach { node ->
            val results = mutableListOf<TextLayoutResult>()
            node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
            results.forEach { r ->
                val text = r.layoutInput.text.text
                if (r.hasVisualOverflow) problems += "transbordo em \"$text\" size=${r.size} lines=${r.lineCount} w=${r.multiParagraph.width}"
                if (!text.contains(' ') && r.lineCount != 1) problems += "\"$text\" quebrou em ${r.lineCount} linhas"
            }
            val b = node.boundsInRoot
            if (!(b.left >= -0.5f && b.right <= rootWidthPx + 0.5f)) problems += "fora da largura (${b.left}..${b.right} de $rootWidthPx)"
        }
        assertTrue("$label: " + problems.distinct().joinToString(" | "), problems.isEmpty())
    }

    @Test
    fun noClippingAcrossFieldsFontsAndWidths() {
        val density = 2f
        rule.setContent {
            val c = current
            if (c != null) {
                CompositionLocalProvider(LocalDensity provides Density(density, c.systemFontScale)) {
                    Box(Modifier.width(c.widthDp.dp).testTag("root")) {
                        OfferBalloon(c.state, c.style, values = c.values)
                    }
                }
            }
        }

        var cases = 0
        for (state in PreviewState.entries)
            for (layout in CardLayout.entries)
                for (font in CardFontSize.entries)
                    for (system in listOf(1.0f, 1.3f, 2.0f))
                        for (width in listOf(320, 360, 411))
                            for (values in listOf(BalloonValues.Sample, longValues)) {
                                val style = CardStyle(layout = layout, fontSize = font, enabledFields = allFields)
                                current = Case(state, style, values, system, width)
                                rule.waitForIdle()
                                checkNoOverflow("$state/$layout/${font.label}/sistema x$system/${width}dp", width, density)
                                cases++
                            }
        assertTrue(cases > 300)
    }

    @Test
    fun eachSingleFieldAloneDoesNotClip() {
        val density = 2f
        rule.setContent {
            val c = current
            if (c != null) {
                CompositionLocalProvider(LocalDensity provides Density(density, c.systemFontScale)) {
                    Box(Modifier.width(c.widthDp.dp).testTag("root")) {
                        OfferBalloon(c.state, c.style, values = c.values)
                    }
                }
            }
        }
        for (field in CardField.entries)
            for (system in listOf(1.0f, 2.0f)) {
                val style = CardStyle(fontSize = CardFontSize.L, enabledFields = setOf(field))
                current = Case(PreviewState.Bad, style, longValues, system, 320)
                rule.waitForIdle()
                checkNoOverflow("campo $field/sistema x$system", 320, density)
            }
    }

    @Test
    fun unavailableBalloonDoesNotClipAndShowsNoValues() {
        val density = 2f
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density, 2.0f)) {
                Box(Modifier.width(320.dp).testTag("root")) {
                    UnavailableBalloon(
                        origin = "99Pop · manual",
                        message = "A última oferta expirou ou foi substituída. Nenhum resultado antigo é mostrado.",
                        style = CardStyle(fontSize = CardFontSize.L),
                    )
                }
            }
        }
        rule.waitForIdle()
        checkNoOverflow("indisponível", 320, density)
        rule.onAllNodesWithText("R$/km").assertCountEquals(0)
    }

    @Test
    fun sealFollowsStyleSetting() {
        rule.setContent {
            Box(Modifier.width(360.dp).testTag("root")) {
                OfferBalloon(PreviewState.Good, CardStyle(highlightSeal = false))
            }
        }
        rule.onAllNodesWithText("ACEITAR").assertCountEquals(0)
    }

    @Test
    fun reasonFollowsStyleSetting() {
        rule.setContent {
            Box(Modifier.width(360.dp).testTag("root")) {
                OfferBalloon(PreviewState.Good, CardStyle(showReason = false))
            }
        }
        rule.onAllNodesWithText("decisão manual", substring = true).assertCountEquals(0)
    }

    @Test
    fun deselectedFieldsAreNotShown() {
        rule.setContent {
            Box(Modifier.width(360.dp).testTag("root")) {
                OfferBalloon(PreviewState.Good, CardStyle(enabledFields = setOf(CardField.RevenuePerKm)))
            }
        }
        rule.onAllNodesWithText("R$/km").assertCountEquals(1)
        rule.onAllNodesWithText("R$/h").assertCountEquals(0)
        rule.onAllNodesWithText("R$/min").assertCountEquals(0)
    }
}
