package br.app.motorista.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.app.motorista.domain.card.CardField
import br.app.motorista.domain.card.CardFontSize
import br.app.motorista.domain.card.CardLayout
import br.app.motorista.domain.card.CardStyle
import br.app.motorista.domain.card.PreviewState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Teste AUTOMÁTICO em EMULADOR (texto medido de verdade). Varre campos, fonte do app, fonte do sistema
 * ampliada e larguras de tela e confere que nenhum texto transborda, sai da largura ou tem número
 * quebrado em duas linhas. Não é teste no aparelho do usuário.
 */
@RunWith(AndroidJUnit4::class)
class OfferBalloonOverflowTest {
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

    private var current by mutableStateOf<Case?>(null)

    private fun textNodes(root: SemanticsNode): List<SemanticsNode> {
        val found = mutableListOf<SemanticsNode>()
        fun walk(n: SemanticsNode) {
            if (n.config.contains(SemanticsProperties.Text)) found += n
            n.children.forEach { walk(it) }
        }
        walk(root)
        return found
    }

    private fun problems(label: String, widthDp: Int, density: Float): List<String> {
        val widthPx = widthDp * density
        val out = mutableListOf<String>()
        val nodes = textNodes(rule.onRoot().fetchSemanticsNode())
        if (nodes.isEmpty()) out += "$label: nenhum texto"
        nodes.forEach { node ->
            val results = mutableListOf<TextLayoutResult>()
            node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
            results.forEach { r ->
                val text = r.layoutInput.text.text
                if (r.hasVisualOverflow) out += "$label: transbordo em \"$text\""
                if (!text.contains(' ') && r.lineCount != 1) out += "$label: \"$text\" em ${r.lineCount} linhas"
            }
            val b = node.boundsInRoot
            if (!(b.left >= -0.5f && b.right <= widthPx + 0.5f)) out += "$label: fora da largura (${b.left}..${b.right} de $widthPx)"
        }
        return out
    }

    private fun setContent(density: Float) {
        rule.setContent {
            val c = current
            if (c != null) {
                CompositionLocalProvider(LocalDensity provides Density(density, c.systemFontScale)) {
                    Box(Modifier.width(c.widthDp.dp)) { OfferBalloon(c.state, c.style, values = c.values) }
                }
            }
        }
    }

    @Test
    fun noClippingAcrossFieldsFontsAndWidths() {
        val density = 2f
        setContent(density)
        val all = CardField.entries.toSet()
        val bad = mutableListOf<String>()
        for (state in PreviewState.entries)
            for (layout in CardLayout.entries)
                for (font in CardFontSize.entries)
                    for (system in listOf(1.0f, 1.3f, 2.0f))
                        for (width in listOf(320, 360, 411))
                            for (values in listOf(BalloonValues.Sample, longValues)) {
                                current = Case(state, CardStyle(layout = layout, fontSize = font, enabledFields = all), values, system, width)
                                rule.waitForIdle()
                                bad += problems("$state/$layout/${font.label}/sistema x$system/${width}dp", width, density)
                            }
        assertTrue(bad.distinct().take(10).joinToString("\n"), bad.isEmpty())
    }

    @Test
    fun eachSingleFieldAloneDoesNotClip() {
        val density = 2f
        setContent(density)
        val bad = mutableListOf<String>()
        for (field in CardField.entries)
            for (system in listOf(1.0f, 2.0f)) {
                current = Case(PreviewState.Bad, CardStyle(fontSize = CardFontSize.L, enabledFields = setOf(field)), longValues, system, 320)
                rule.waitForIdle()
                bad += problems("campo $field/sistema x$system", 320, density)
            }
        assertTrue(bad.distinct().take(10).joinToString("\n"), bad.isEmpty())
    }
}
