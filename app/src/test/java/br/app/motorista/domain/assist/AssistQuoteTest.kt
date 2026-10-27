package br.app.motorista.domain.assist

import br.app.motorista.domain.calc.OfferInput
import br.app.motorista.domain.quote.QuoteCalc
import br.app.motorista.domain.quote.QuoteInput
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistQuoteTest {
    private fun cfg(greet: Boolean = true, eta: Boolean = true, minutes: Long? = 5) =
        MessageConfig(greet, "Olá!", "Estou a caminho.", eta, "Chego em {eta} min.", minutes)

    @Test
    fun messageJoinsOnlyWhatIsEnabledAndFilled() {
        assertEquals("Olá! Estou a caminho. Chego em 5 min.", MessageTemplate.render(cfg()))
        assertEquals("Estou a caminho. Chego em 5 min.", MessageTemplate.render(cfg(greet = false)))
        assertEquals("Olá! Estou a caminho.", MessageTemplate.render(cfg(eta = false)))
    }

    @Test
    fun etaWithoutMinutesIsNotRenderedAndIsReported() {
        val c = cfg(minutes = null)
        assertEquals("Olá! Estou a caminho.", MessageTemplate.render(c))
        assertEquals("Informe os minutos do ETA", MessageTemplate.etaProblem(c))
        assertNull(MessageTemplate.etaProblem(cfg()))
        assertEquals("O texto do ETA precisa conter {eta}", MessageTemplate.etaProblem(cfg().copy(etaTemplate = "Chego logo")))
    }

    @Test
    fun pickupAlertOnlyWithUserLimitAndKnownDistance() {
        val input = OfferInput(pickupMeters = 3_500)
        assertTrue(AlertCalc.evaluate(input, AlertRules(null)).isEmpty())
        assertTrue(AlertCalc.evaluate(OfferInput(), AlertRules(BigDecimal("3"))).isEmpty())
        assertEquals(1, AlertCalc.evaluate(input, AlertRules(BigDecimal("3"))).size)
        assertTrue(AlertCalc.evaluate(input, AlertRules(BigDecimal("3.5"))).isEmpty())
    }

    @Test
    fun placeMatcherIgnoresCaseAndAccentsAndRespectsKind() {
        val terms = listOf(
            FlaggedTerm("1", "Praça", PlaceKind.Pickup),
            FlaggedTerm("2", "mercado", PlaceKind.Market),
            FlaggedTerm("3", "Centro", PlaceKind.Dropoff),
        )
        val r = PlaceMatcher.matches("PRACA da Sé, Mercado Central", "Bairro Centro", terms)
        assertEquals(3, r.size)
        assertTrue(r.any { it.startsWith("Embarque contém \"Praça\"") })
        assertTrue(r.any { it.startsWith("Embarque contém \"mercado\"") })
        assertTrue(r.any { it.startsWith("Destino contém \"Centro\"") })
        // termo de embarque não casa com o destino
        assertTrue(PlaceMatcher.matches("", "Praça", listOf(terms[0])).isEmpty())
    }

    @Test
    fun voiceTextFollowsChosenOrderAndSpeaksNothingWhenEmpty() {
        val text = VoiceText.build(listOf(VoiceItem.PerHour, VoiceItem.Origin), "99Pop", "manual", "2,00", "67,64", "1,13", "avaliação amarela")
        assertEquals("Reais por hora 67,64. 99Pop, entrada manual.", text)
        assertEquals("", VoiceText.build(emptyList(), "x", "y", "", "", "", ""))
        assertEquals(listOf(VoiceItem.PerKm, VoiceItem.Assessment), VoiceText.parse(VoiceText.serialize(listOf(VoiceItem.PerKm, VoiceItem.Assessment))))
        assertTrue(VoiceText.parse(null).isEmpty())
    }

    private fun quote(rateKm: String? = "2", rateMin: String? = "0.5", tolls: Long? = 1000, pickup: Boolean = true, ret: Boolean = false) = QuoteInput(
        BigDecimal("3"), BigDecimal("10"), BigDecimal("10"), pickup, ret, BigDecimal("30"),
        rateKm?.let { BigDecimal(it) }, rateMin?.let { BigDecimal(it) }, tolls,
    )

    @Test
    fun quoteSumsOnlyChosenLegsAndUserRates() {
        val q = QuoteCalc.compute(quote())
        assertEquals(0, BigDecimal("13").compareTo(q.billableKm))
        // 13 km * 2 = 26,00 ; 30 min * 0,5 = 15,00 ; pedágio 10,00
        assertEquals(listOf(2600L, 1500L, 1000L), q.lines.map { it.cents })
        assertEquals(5100, q.totalCents)
        assertEquals(23, QuoteCalc.compute(quote(ret = true)).billableKm!!.toInt())
    }

    @Test
    fun missingRateMeansNoLineAndMissingKmIsReported() {
        val noRates = QuoteCalc.compute(quote(rateKm = null, rateMin = null, tolls = null))
        assertTrue(noRates.lines.isEmpty())
        assertTrue(noRates.notes.isNotEmpty())
        val incomplete = QuoteCalc.compute(quote().copy(returnKm = null, includeReturn = true))
        assertTrue(incomplete.notes.any { it.startsWith("Distância incompleta") })
        assertNull(incomplete.billableKm)
    }

    @Test
    fun internalCostNeverUsesMissingData() {
        assertNull(QuoteCalc.internalCostCents(quote(), null))
        assertEquals(1300L, QuoteCalc.internalCostCents(quote(), BigDecimal("1")))
    }
}
