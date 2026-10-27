package br.app.motorista.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.unit.dp
import br.app.motorista.domain.card.CardField
import br.app.motorista.domain.card.CardStyle
import br.app.motorista.domain.card.PreviewState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes AUTOMÁTICOS de comportamento do balão (JVM + Robolectric, sem aparelho nem emulador).
 * Robolectric não mede texto de verdade; por isso transbordo/corte de fonte NÃO é verificado aqui.
 * Isso é feito em emulador por androidTest/OfferBalloonOverflowTest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfferBalloonLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun unavailableBalloonShowsNoValues() {
        rule.setContent {
            Box(Modifier.width(320.dp)) {
                UnavailableBalloon(
                    origin = "99Pop · manual",
                    message = "A última oferta expirou ou foi substituída. Nenhum resultado antigo é mostrado.",
                    style = CardStyle(),
                )
            }
        }
        rule.onAllNodesWithText("R$/km").assertCountEquals(0)
        rule.onAllNodesWithText("INDISPONÍVEL").assertCountEquals(1)
    }

    @Test
    fun sealFollowsStyleSetting() {
        rule.setContent {
            Box(Modifier.width(360.dp)) {
                OfferBalloon(PreviewState.Good, CardStyle(highlightSeal = false))
            }
        }
        rule.onAllNodesWithText("ACEITAR").assertCountEquals(0)
    }

    @Test
    fun reasonFollowsStyleSetting() {
        rule.setContent {
            Box(Modifier.width(360.dp)) {
                OfferBalloon(PreviewState.Good, CardStyle(showReason = false))
            }
        }
        rule.onAllNodesWithText("decisão manual", substring = true).assertCountEquals(0)
    }

    @Test
    fun deselectedFieldsAreNotShown() {
        rule.setContent {
            Box(Modifier.width(360.dp)) {
                OfferBalloon(PreviewState.Good, CardStyle(enabledFields = setOf(CardField.RevenuePerKm)))
            }
        }
        rule.onAllNodesWithText("R$/km").assertCountEquals(1)
        rule.onAllNodesWithText("R$/h").assertCountEquals(0)
        rule.onAllNodesWithText("R$/min").assertCountEquals(0)
    }
}
