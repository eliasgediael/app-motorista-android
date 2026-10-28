package br.app.motorista.ui.testride

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.app.motorista.data.AppData
import br.app.motorista.domain.assessment.Assessment
import br.app.motorista.domain.assessment.OfferEvaluator
import br.app.motorista.domain.calc.Display
import br.app.motorista.domain.calc.MetricResult
import br.app.motorista.domain.calc.OfferCalculator
import br.app.motorista.domain.calc.OfferInput
import br.app.motorista.domain.criteria.DistanceTimeBasis
import br.app.motorista.domain.finance.CategoryKind
import br.app.motorista.domain.finance.FinanceCalc
import br.app.motorista.domain.history.RuleVersion
import br.app.motorista.domain.history.TripLogic
import br.app.motorista.domain.history.TripRecord
import br.app.motorista.domain.history.TripStatus
import br.app.motorista.domain.offer.OfferOrigin
import br.app.motorista.domain.offer.OfferSnapshot
import br.app.motorista.domain.offer.Platform
import br.app.motorista.offer.BalloonHub
import br.app.motorista.ui.common.ChoiceChips
import br.app.motorista.ui.common.ConfirmDialog
import br.app.motorista.ui.common.DropdownInput
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SectionTitle
import br.app.motorista.ui.common.SurfaceCard
import br.app.motorista.ui.components.BalloonValues
import br.app.motorista.ui.state.PreferencesState
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * Oferta de TESTE pronta, para exercitar o fluxo inteiro sem digitar: cálculo, regras do usuário, balão
 * (no app e no botão flutuante), voz, histórico e confirmação. Os números são fixtures de teste, não preferências.
 */
private data class TestOffer(val name: String, val input: OfferInput, val pickupText: String = "", val dropoffText: String = "")

private val TEST_OFFERS = listOf(
    TestOffer("Boa (2,00 R$/km)", OfferInput(2480, 1_800, 12_400, null, 6 * 60L, 22 * 60L, null, BigDecimal("4.92"))),
    TestOffer("Fraca (0,80 R$/km)", OfferInput(980, 2_500, 11_000, null, 9 * 60L, 28 * 60L, null, BigDecimal("4.70"))),
    TestOffer("Embarque longo", OfferInput(3200, 6_500, 9_000, null, 20 * 60L, 24 * 60L, null, BigDecimal("4.95"))),
    TestOffer("Destino supermercado", OfferInput(2100, 1_200, 8_000, null, 5 * 60L, 18 * 60L, null, BigDecimal("4.85")), dropoffText = "Supermercado Central"),
    TestOffer("Dados faltando", OfferInput(fareCents = 1800, tripMeters = 7_000)),
)

private const val TEST_ORIGIN = "teste"

/** Corrida de teste (extra): dispara ofertas de teste e acompanha a corrida até a confirmação. Tudo marcado TESTE e apagável. */
@Composable
fun TestRideScreen(state: PreferencesState, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val trips by AppData.trips.items.collectAsState()
    val txs by AppData.transactions.items.collectAsState()
    AppData.settings.items.collectAsState().value
    var platform by rememberSaveable { mutableStateOf(Platform.P99) }
    var basis by rememberSaveable { mutableStateOf<DistanceTimeBasis?>(state.simulator.displayBasis ?: DistanceTimeBasis.PickupAndTrip) }
    var message by remember { mutableStateOf<Pair<String, String>?>(null) }
    var clearing by rememberSaveable { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var lastError by remember { mutableStateOf(br.app.motorista.data.CrashLog.read(context)) }
    val testTrips = trips.filter { it.origin == TEST_ORIGIN }.sortedByDescending { it.atMs }

    ScreenScaffold("Corrida de teste", onBack, onOpen) {
        InfoNote(
            "Dispara uma oferta de TESTE pelo mesmo caminho da oferta real: usa SEUS critérios e a SUA combinação, aparece no balão (ligue o copiloto para ver no botão flutuante) e na voz, " +
                "e entra no histórico marcada como teste. Os números abaixo são exemplos de teste, não são sugestão de valores.",
        )
        lastError?.let { err ->
            SurfaceCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Último erro do aplicativo (diagnóstico)", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                    androidx.compose.foundation.text.selection.SelectionContainer { Text(err.take(1500), style = MaterialTheme.typography.bodySmall) }
                    TextButton(onClick = { br.app.motorista.data.CrashLog.clear(context); lastError = null }) { Text("Apagar este aviso") }
                }
            }
        }
        Text("Plataforma da oferta", style = MaterialTheme.typography.bodySmall)
        ChoiceChips(Platform.entries, platform, { it.label }) { platform = it }
        Text("Base para exibir as métricas", style = MaterialTheme.typography.bodySmall)
        DropdownInput(DistanceTimeBasis.entries, basis, "Escolher base", { it.label }, { basis = it }, allowNone = false)

        SectionTitle("Ofertas de teste")
        TEST_OFFERS.forEach { t ->
            SurfaceCard {
              Column {
                Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            FinanceCalc.formatCents(t.input.fareCents ?: 0) + (t.input.tripMeters?.let { " · " + Display.decimal(BigDecimal(it).movePointLeft(3), 1) + " km" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(onClick = {
                        message = t.name to try {
                            fire(state, t, platform, basis)
                        } catch (e: Exception) {
                            br.app.motorista.data.CrashLog.record(context, "Erro ao disparar oferta de teste", e)
                            "ERRO: " + (e.message ?: e.javaClass.simpleName)
                        }
                    }) { Text("Disparar") }
                }
                // Resultado logo abaixo da oferta tocada, onde o usuário está olhando.
                message?.takeIf { it.first == t.name }?.let {
                    Text(it.second, Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
              }
            }
        }

        SectionTitle("Corridas de teste")
        if (testTrips.isEmpty()) Text("Nenhuma corrida de teste ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        testTrips.forEach { t ->
            SurfaceCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(t.platform + " · " + FinanceCalc.formatCents(t.input.fareCents ?: 0) + " · " + t.status.label, fontWeight = FontWeight.SemiBold)
                    Text(t.metricsText, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        when (t.status) {
                            TripStatus.NotConfirmed -> {
                                OutlinedButton(onClick = {
                                    val cats = AppData.categories.items.value
                                    val income = cats.firstOrNull { it.kind == CategoryKind.Income && !it.archived }
                                    val tx = TripLogic.confirmIncome(t, t.input.fareCents ?: 0, txs, income?.id, null, { AppData.newId() }, LocalDate.now().toEpochDay())
                                    if (tx != null) {
                                        AppData.transactions.upsert(tx.copy(note = "TESTE · " + tx.note))
                                        AppData.trips.upsert(t.copy(status = TripStatus.Done, txId = tx.id))
                                    }
                                }) { Text("Concluir corrida (realizada)") }
                                TextButton(onClick = { AppData.trips.upsert(t.copy(status = TripStatus.NotDone)) }) { Text("Não realizada") }
                            }
                            else -> Text("Ver detalhes no Histórico", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        if (testTrips.isNotEmpty()) OutlinedButton(onClick = { clearing = true }, modifier = Modifier.fillMaxWidth()) { Text("Apagar dados de teste") }
    }

    if (clearing) {
        ConfirmDialog(
            "Apagar dados de teste?",
            "As corridas de teste e as receitas de teste lançadas por elas serão apagadas. Seus dados reais não são tocados.",
            "Apagar",
            {
                testTrips.forEach { t ->
                    t.txId?.let { AppData.transactions.remove(it) }
                    AppData.trips.remove(t.id)
                }
                clearing = false
            },
            { clearing = false },
        )
    }
}

/** Mesmo caminho da oferta real: regras e política do usuário -> avaliação -> hub (balão, voz) + histórico. */
private fun fire(state: PreferencesState, t: TestOffer, platform: Platform, basis: DistanceTimeBasis?): String {
    val now = SystemClock.elapsedRealtime()
    val snapshot = OfferSnapshot(UUID.randomUUID().toString(), platform, OfferOrigin.Manual, now, t.input)
    val rules = state.activeRules()
    val evaluation = OfferEvaluator.evaluate(t.input, rules, state.policy)
    fun fmt(r: MetricResult) = if (r is MetricResult.Value) Display.decimal(r.value) else "—"
    val km = OfferCalculator.revenuePerKm(t.input, basis)
    val hour = OfferCalculator.revenuePerHour(t.input, basis)
    val min = OfferCalculator.revenuePerMinute(t.input, basis)
    val values = BalloonValues(
        time = basis?.let { OfferCalculator.totalSeconds(t.input, it) }?.let { (it / 60).toString() + " min" } ?: "— min",
        distance = basis?.let { OfferCalculator.totalMeters(t.input, it) }?.let { Display.decimal(BigDecimal(it).movePointLeft(3), 1) + " km" } ?: "— km",
        rating = t.input.passengerRating?.let { Display.decimal(it) } ?: "—",
        perKm = fmt(km), perHour = fmt(hour), perMinute = fmt(min),
        fare = t.input.fareCents?.let { Display.decimal(BigDecimal(it).movePointLeft(2)) } ?: "—",
        result = "indisponível",
        tag = "TESTE · " + platform.label,
    )
    val extras = br.app.motorista.domain.assist.AlertCalc.evaluate(t.input, br.app.motorista.ui.assist.currentAlertRules()) +
        br.app.motorista.domain.assist.PlaceMatcher.matches("", t.dropoffText, AppData.flaggedTerms.items.value) +
        br.app.motorista.ui.assist.marketMatchesNow(t.pickupText, t.dropoffText)
    if (!BalloonHub.publish(snapshot, evaluation, values, extras)) return "Oferta não publicada."
    val summary = RuleVersion.summary(rules, state.policy)
    AppData.trips.upsert(
        TripRecord(
            id = snapshot.id, atMs = System.currentTimeMillis(), epochDay = LocalDate.now().toEpochDay(), platform = platform.label, origin = TEST_ORIGIN,
            input = t.input, metricsText = "TESTE · R$/km " + values.perKm + " · R$/h " + values.perHour + " · R$/min " + values.perMinute,
            bandName = (evaluation.assessment as? Assessment.Colored)?.band?.name, note = extras.joinToString(" "),
            ruleVersionId = RuleVersion.id(summary), ruleSummary = summary,
        ),
    )
    val verdict = when ((evaluation.assessment as? Assessment.Colored)?.band) {
        br.app.motorista.domain.assessment.Band.Good -> "VERDE (boa)"
        br.app.motorista.domain.assessment.Band.Attention -> "AMARELO (atenção)"
        br.app.motorista.domain.assessment.Band.Bad -> "VERMELHO (fraca)"
        null -> "SEM COR: " + (BalloonHub.latest(platform)?.note ?: "")
    }
    return "Resultado: $verdict" + if (extras.isNotEmpty()) " · atenção: " + extras.joinToString("; ") else ""
}
