package br.app.motorista.ui.history

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.app.motorista.data.AppData
import br.app.motorista.domain.calc.OfferParsing
import br.app.motorista.domain.calc.Parsed
import br.app.motorista.domain.calc.valueOrNull
import br.app.motorista.domain.finance.CategoryKind
import br.app.motorista.domain.finance.DateText
import br.app.motorista.domain.finance.FinanceCalc
import br.app.motorista.domain.history.ReportCalc
import br.app.motorista.domain.history.TimeBuckets
import br.app.motorista.domain.history.TripLogic
import br.app.motorista.domain.history.TripRecord
import br.app.motorista.domain.history.TripStatus
import br.app.motorista.domain.journey.JourneyClock
import br.app.motorista.ui.common.ChoiceChips
import br.app.motorista.ui.common.ConfirmDialog
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.MoneyInput
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SectionTitle
import br.app.motorista.ui.common.SurfaceCard
import java.time.LocalDate
import java.time.ZoneId

private const val SELECTED_TRIP = "selectedTrip"

private fun money(cents: Long?): String = cents?.let { FinanceCalc.formatCents(it) } ?: "—"

private fun csv(trips: List<TripRecord>): String = buildString {
    appendLine("data;plataforma;origem;status;valor_oferta_centavos;metricas;avaliacao;versao_regras")
    trips.sortedBy { it.atMs }.forEach {
        appendLine(
            listOf(
                DateText.formatEpochDay(it.epochDay), it.platform, it.origin, it.status.label, it.input.fareCents?.toString() ?: "",
                it.metricsText.replace(";", ","), it.bandName ?: "sem avaliação", it.ruleVersionId,
            ).joinToString(";"),
        )
    }
}

/** Histórico de corridas (telas/11-historico): filtrar, abrir detalhe e exportar. */
@Composable
fun HistoryScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val trips by AppData.trips.items.collectAsState()
    var platform by rememberSaveable { mutableStateOf<String?>(null) }
    var status by rememberSaveable { mutableStateOf<TripStatus?>(null) }
    var exporting by rememberSaveable { mutableStateOf(false) }

    val platforms = trips.map { it.platform }.distinct()
    val shown = trips.filter { (platform == null || it.platform == platform) && (status == null || it.status == status) }.sortedByDescending { it.atMs }

    ScreenScaffold("Histórico de corridas", onBack, onOpen) {
        InfoNote("Aqui ficam as ofertas que você analisou (hoje, entradas manuais do simulador). Só a confirmação sua transforma uma oferta em receita.")
        Text("Plataforma", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { platform = null }, enabled = platform != null) { Text("Todas") }
            platforms.forEach { p -> OutlinedButton(onClick = { platform = p }, enabled = platform != p) { Text(p) } }
        }
        Text("Situação", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChoiceChips(listOf<TripStatus?>(null) + TripStatus.entries, status, { it?.label ?: "Todas" }) { status = it }

        if (shown.isEmpty()) Text("Nenhum registro.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        shown.forEach { t ->
            SurfaceCard(onClick = {
                AppData.putSetting(SELECTED_TRIP, t.id)
                onOpen("detalhe")
            }) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(t.platform + " · " + money(t.input.fareCents), fontWeight = FontWeight.SemiBold)
                    Text(
                        DateText.formatEpochDay(t.epochDay) + " · " + t.status.label + " · origem " + t.origin,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Button(onClick = { exporting = true }, enabled = shown.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Exportar CSV (revisar antes)") }
    }

    if (exporting) {
        val text = csv(shown)
        AlertDialog(
            onDismissRequest = { exporting = false },
            title = { Text("Revisar antes de compartilhar") },
            text = {
                Column {
                    Text("${shown.size} registro(s). O arquivo não tem dados de passageiros. Só sai do aparelho se você compartilhar.")
                    Text(text.lines().take(4).joinToString("\n"), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    exporting = false
                    val send = Intent(Intent.ACTION_SEND).setType("text/csv").putExtra(Intent.EXTRA_TEXT, text)
                    context.startActivity(Intent.createChooser(send, "Compartilhar histórico").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }) { Text("Compartilhar") }
            },
            dismissButton = { TextButton(onClick = { exporting = false }) { Text("Cancelar") } },
        )
    }
}

/** Detalhe da corrida (telas/12-detalhe): métricas, base, regra usada e confirmação manual. */
@Composable
fun TripDetailScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val trips by AppData.trips.items.collectAsState()
    val txs by AppData.transactions.items.collectAsState()
    val settings by AppData.settings.items.collectAsState()
    val id = settings.firstOrNull { it.id == SELECTED_TRIP }?.value
    val trip = trips.firstOrNull { it.id == id }
    var confirming by rememberSaveable { mutableStateOf(false) }
    var amount by rememberSaveable { mutableStateOf("") }
    var deleting by rememberSaveable { mutableStateOf(false) }

    ScreenScaffold("Detalhe da corrida", onBack, onOpen) {
        if (trip == null) {
            Text("Registro não encontrado.")
            return@ScreenScaffold
        }
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(trip.platform + " · " + money(trip.input.fareCents), style = MaterialTheme.typography.titleLarge)
                Text("Situação: " + trip.status.label, fontWeight = FontWeight.SemiBold)
                Text("Origem: " + trip.origin + " · " + DateText.formatEpochDay(trip.epochDay))
                Text(trip.metricsText)
                Text("Avaliação na época: " + (trip.bandName?.let { when (it) { "Good" -> "verde"; "Attention" -> "amarelo"; else -> "vermelho" } } ?: "sem cor geral"))
            }
        }
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Regras usadas (versão " + trip.ruleVersionId + ")", fontWeight = FontWeight.SemiBold)
                Text(trip.ruleSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("O histórico guarda as regras da época; mudar critérios depois não altera este registro.", style = MaterialTheme.typography.bodySmall)
            }
        }
        when (trip.status) {
            TripStatus.NotConfirmed -> {
                Button(
                    onClick = {
                        amount = trip.input.fareCents?.let { FinanceCalc.formatCents(it).removePrefix("R$ ") } ?: ""
                        confirming = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Confirmar viagem realizada") }
                OutlinedButton(onClick = { AppData.trips.upsert(trip.copy(status = TripStatus.NotDone)) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Marcar como não realizada")
                }
            }
            TripStatus.NotDone -> OutlinedButton(onClick = { AppData.trips.upsert(trip.copy(status = TripStatus.NotConfirmed)) }, modifier = Modifier.fillMaxWidth()) {
                Text("Voltar para não confirmada")
            }
            TripStatus.Done -> InfoNote("Receita registrada nas finanças (uma única vez para esta viagem).")
        }
        TextButton(onClick = { deleting = true }) { Text("Excluir registro") }
    }

    if (confirming && trip != null) {
        val cents = OfferParsing.cents(amount)
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Confirmar viagem realizada") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Informe o valor realmente recebido. Ele será lançado como receita confirmada.")
                    MoneyInput("Valor recebido (R$)", amount, { amount = it }, if (cents is Parsed.Invalid) "Valor inválido" else null)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = cents is Parsed.Ok && cents.value > 0,
                    onClick = {
                        val cats = AppData.categories.items.value
                        val income = cats.firstOrNull { it.kind == CategoryKind.Income && it.name.equals("Corridas", true) && !it.archived }
                        val plat = cats.firstOrNull { it.kind == CategoryKind.Platform && it.name.equals(trip.platform, true) }
                        val tx = TripLogic.confirmIncome(trip, cents.valueOrNull() ?: 0, txs, income?.id, plat?.id, { AppData.newId() }, LocalDate.now().toEpochDay())
                        if (tx != null) {
                            AppData.transactions.upsert(tx)
                            AppData.trips.upsert(trip.copy(status = TripStatus.Done, txId = tx.id))
                        }
                        confirming = false
                    },
                ) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancelar") } },
        )
    }
    if (deleting && trip != null) {
        ConfirmDialog("Excluir registro?", "A receita já lançada (se houver) não é apagada.", "Excluir", {
            AppData.trips.remove(trip.id)
            deleting = false
            onBack()
        }, { deleting = false })
    }
}

private enum class Period(val label: String) { Today("Hoje"), Week("7 dias"), Month("Mês atual") }

/** Relatórios da jornada (telas/10-relatorios): ganho online separado do ganho da oferta. */
@Composable
fun ReportsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val txs by AppData.transactions.items.collectAsState()
    val trips by AppData.trips.items.collectAsState()
    val journeys by AppData.journeys.items.collectAsState()
    val cats by AppData.categories.items.collectAsState()
    var period by rememberSaveable { mutableStateOf(Period.Week) }
    val today = LocalDate.now()
    val (from, to) = when (period) {
        Period.Today -> today to today
        Period.Week -> today.minusDays(6) to today
        Period.Month -> today.withDayOfMonth(1) to today
    }
    val zone = ZoneId.systemDefault()
    val r = ReportCalc.period(txs, trips, journeys, from.toEpochDay(), to.toEpochDay(), zone)
    val inRange = txs.filter { it.kind == br.app.motorista.domain.finance.EntryKind.Income && it.epochDay in from.toEpochDay()..to.toEpochDay() }

    ScreenScaffold("Relatórios da jornada", onBack, onOpen) {
        ChoiceChips(Period.entries, period, { it.label }) { period = it }
        Text(DateText.format(from) + " a " + DateText.format(to), style = MaterialTheme.typography.bodySmall)
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row2("Receita confirmada", money(r.incomeCents))
                Row2("Viagens realizadas", r.doneTrips.toString())
                Row2("Ofertas analisadas", r.analysedOffers.toString())
            }
        }
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Jornada (tempo online)", fontWeight = FontWeight.SemiBold)
                Row2("Em andamento", JourneyClock.format(r.workedMs))
                Row2("Em pausa", JourneyClock.format(r.pausedMs))
                Row2("Ganho por hora online", money(r.incomePerOnlineHourCents).let { if (r.incomePerOnlineHourCents == null) "indisponível (sem jornada encerrada no período)" else "$it/h" })
                Text(
                    "Usa a duração da jornada, não só o tempo das viagens. É diferente do R$/h estimado de uma oferta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SectionTitle("Receita por plataforma")
        val byPlatform = inRange.groupBy { t -> cats.firstOrNull { it.id == t.platformId }?.name ?: "Sem plataforma" }
        if (byPlatform.isEmpty()) Text("Sem receitas no período.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        byPlatform.forEach { (name, list) -> Row2(name, money(list.sumOf { it.amountCents }) + " (" + list.size + ")") }
    }
}

@Composable
private fun Row2(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

/** Melhores horários (telas/13-horarios): viagens realizadas por dia da semana e hora, com a amostra explícita. */
@Composable
fun BestTimesScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val trips by AppData.trips.items.collectAsState()
    val txs by AppData.transactions.items.collectAsState()
    val done = trips.filter { it.status == TripStatus.Done }
    val items = done.map { t -> t.atMs to (txs.firstOrNull { it.id == t.txId }?.amountCents ?: t.input.fareCents ?: 0L) }
    val zone = ZoneId.systemDefault()

    ScreenScaffold("Melhores horários", onBack, onOpen) {
        InfoNote("Baseado só nas viagens que você confirmou como realizadas (${done.size}). O método de ranking e a amostra mínima ainda não foram aprovados: os grupos abaixo mostram contagem e receita, ordenados por receita total, sem afirmar tendência.")
        if (done.isEmpty()) {
            Text("Ainda não há viagens confirmadas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            SectionTitle("Dia da semana")
            TimeBuckets.byWeekday(items, zone).forEach { Row2(it.key + " · " + it.count + " viagem(ns)", money(it.incomeCents)) }
            SectionTitle("Hora do dia")
            TimeBuckets.byHour(items, zone).forEach { Row2(it.key + " · " + it.count + " viagem(ns)", money(it.incomeCents)) }
        }
    }
}
