package br.app.motorista.ui.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.app.motorista.data.AppData
import br.app.motorista.domain.calc.OfferParsing
import br.app.motorista.domain.calc.Parsed
import br.app.motorista.domain.calc.valueOrNull
import br.app.motorista.domain.finance.Bill
import br.app.motorista.domain.finance.BillCalc
import br.app.motorista.domain.finance.BillStatus
import br.app.motorista.domain.finance.DateText
import br.app.motorista.domain.finance.DayClose
import br.app.motorista.domain.finance.EntryKind
import br.app.motorista.domain.finance.FinanceCalc
import br.app.motorista.domain.finance.Goal
import br.app.motorista.domain.finance.GoalCalc
import br.app.motorista.domain.finance.GoalKind
import br.app.motorista.domain.finance.Recurrence
import br.app.motorista.domain.finance.ReserveCalc
import br.app.motorista.domain.finance.ReserveEntry
import br.app.motorista.domain.finance.ReserveKind
import br.app.motorista.domain.finance.Transaction
import br.app.motorista.domain.finance.TxSource
import br.app.motorista.domain.vehicle.MaintenanceCalc
import br.app.motorista.ui.common.ChoiceChips
import br.app.motorista.ui.common.ConfirmDialog
import br.app.motorista.ui.common.DateInput
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.MoneyInput
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SectionTitle
import br.app.motorista.ui.common.SurfaceCard
import br.app.motorista.ui.common.TextInput
import java.time.LocalDate
import java.time.YearMonth

/** Contas fixas (telas/18-contas): recorrência, vencimento e baixa. Uma baixa por conta por mês. */
@Composable
fun BillsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val bills by AppData.bills.items.collectAsState()
    val payments by AppData.billPayments.items.collectAsState()
    val cats by AppData.categories.items.collectAsState()
    var name by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var dueDay by rememberSaveable { mutableStateOf("") }
    var recurrence by rememberSaveable { mutableStateOf(Recurrence.Monthly) }
    var onceDate by rememberSaveable { mutableStateOf("") }
    var removing by rememberSaveable { mutableStateOf<String?>(null) }

    val today = LocalDate.now()
    val ym = YearMonth.from(today)

    ScreenScaffold("Contas fixas", onBack, onOpen) {
        Text("Mês atual: " + "%02d/%d".format(ym.monthValue, ym.year), fontWeight = FontWeight.SemiBold)
        val shown = bills.filter { it.active && BillCalc.dueDate(it, ym) != null }
        if (shown.isEmpty()) Text("Nenhuma conta com vencimento neste mês.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        shown.sortedBy { BillCalc.dueDate(it, ym)?.toEpochDay() ?: 0 }.forEach { b ->
            val status = BillCalc.status(b, ym, payments, today)
            SurfaceCard {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(b.name + " · " + FinanceCalc.formatCents(b.amountCents), fontWeight = FontWeight.SemiBold)
                        Text(
                            "Vence " + DateText.format(BillCalc.dueDate(b, ym)!!) + " · " + when (status) {
                                BillStatus.Paid -> "paga"
                                is BillStatus.Overdue -> "atrasada ${status.daysLate} dia(s)"
                                is BillStatus.DueIn -> if (status.days == 0L) "vence hoje" else "em ${status.days} dia(s)"
                                null -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (status != BillStatus.Paid) {
                        TextButton(onClick = {
                            BillCalc.pay(b, ym, payments, today) { AppData.newId() }?.let { (tx, pay) ->
                                AppData.transactions.upsert(tx)
                                AppData.billPayments.upsert(pay)
                            }
                        }) { Text("Dar baixa") }
                    }
                }
            }
        }
        Text(
            "Dar baixa cria uma única despesa por conta e mês. Lembrete por notificação: não implementado; o vencimento aparece aqui.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionTitle("Nova conta")
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextInput("Nome", name, { name = it })
                MoneyInput("Valor (R$)", amount, { amount = it })
                ChoiceChips(Recurrence.entries, recurrence, { it.label }) { recurrence = it }
                if (recurrence == Recurrence.Monthly) {
                    TextInput("Dia do vencimento (1 a 31)", dueDay, { dueDay = it }, keyboard = KeyboardType.Number)
                } else {
                    DateInput("Data de vencimento", onceDate, { onceDate = it })
                }
                val cents = OfferParsing.cents(amount)
                val day = OfferParsing.wholeNumber(dueDay)
                val once = DateText.parse(onceDate)
                val valid = name.isNotBlank() && cents is Parsed.Ok && cents.value > 0 &&
                    (if (recurrence == Recurrence.Monthly) day is Parsed.Ok && day.value in 1..31 else once != null)
                Button(
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val expenseCat = cats.firstOrNull { it.kind == br.app.motorista.domain.finance.CategoryKind.Expense && !it.archived }
                        AppData.bills.upsert(
                            Bill(
                                AppData.newId(), name.trim(), cents.valueOrNull()!!,
                                if (recurrence == Recurrence.Monthly) day.valueOrNull()!!.toInt() else 1,
                                recurrence, once?.toEpochDay().takeIf { recurrence == Recurrence.Once }, expenseCat?.id,
                            ),
                        )
                        name = ""
                        amount = ""
                        dueDay = ""
                        onceDate = ""
                    },
                ) { Text("Adicionar conta") }
            }
        }
        SectionTitle("Todas as contas")
        bills.forEach { b ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(b.name + " · " + b.recurrence.label + (if (!b.active) " · inativa" else ""), Modifier.weight(1f))
                TextButton(onClick = { AppData.bills.upsert(b.copy(active = !b.active)) }) { Text(if (b.active) "Desativar" else "Ativar") }
                TextButton(onClick = { removing = b.id }) { Text("Remover") }
            }
        }
    }
    removing?.let { id ->
        ConfirmDialog("Remover conta?", "Despesas já lançadas por baixas anteriores não são apagadas.", "Remover", {
            AppData.bills.remove(id)
            removing = null
        }, { removing = null })
    }
}

/** Reserva de manutenção (telas/19-reserva): provisão, transferência e gasto em bases separadas. */
@Composable
fun ReserveScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val entries by AppData.reserve.items.collectAsState()
    val cats by AppData.categories.items.collectAsState()
    var kind by rememberSaveable { mutableStateOf(ReserveKind.Provision) }
    var amount by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(DateText.format(LocalDate.now())) }
    var note by rememberSaveable { mutableStateOf("") }
    val summary = ReserveCalc.summary(entries)
    val cents = OfferParsing.cents(amount)
    val day = DateText.parse(date)
    val spendTooMuch = kind == ReserveKind.Spend && cents is Parsed.Ok && !ReserveCalc.canSpend(entries, cents.value)

    ScreenScaffold("Reserva de manutenção", onBack, onOpen) {
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Line("Provisionado (planejado)", FinanceCalc.formatCents(summary.provisionedCents))
                Line("Transferido (dinheiro separado)", FinanceCalc.formatCents(summary.transferredCents))
                Line("Gasto da reserva", FinanceCalc.formatCents(summary.spentCents))
                Line("Saldo da reserva", FinanceCalc.formatCents(summary.balanceCents), bold = true)
                Line("Provisão ainda sem transferir", FinanceCalc.formatCents(summary.pendingTransferCents))
            }
        }
        InfoNote("Provisão e transferência não são despesa. O gasto é contado uma única vez, pela despesa de manutenção gerada nele; assim nada é deduzido duas vezes.")

        SectionTitle("Novo registro")
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChips(ReserveKind.entries, kind, { it.label }) { kind = it }
                MoneyInput("Valor (R$)", amount, { amount = it }, if (spendTooMuch) "Maior que o saldo da reserva" else null)
                DateInput("Data", date, { date = it })
                TextInput("Observação (opcional)", note, { note = it })
                Button(
                    enabled = cents is Parsed.Ok && cents.value > 0 && day != null && !spendTooMuch,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val value = cents.valueOrNull()!!
                        var txId: String? = null
                        if (kind == ReserveKind.Spend) {
                            val maintCat = cats.firstOrNull { it.kind == br.app.motorista.domain.finance.CategoryKind.Expense && it.name.equals("Manutenção", true) }
                            val entryId = AppData.newId()
                            txId = AppData.newId()
                            AppData.transactions.upsert(
                                Transaction(txId, EntryKind.Expense, value, day!!.toEpochDay(), maintCat?.id, note = note.trim().ifBlank { "Gasto da reserva" },
                                    source = TxSource.Maintenance, sourceRef = "reserve@$entryId"),
                            )
                            AppData.reserve.upsert(ReserveEntry(entryId, kind, value, day.toEpochDay(), note.trim(), txId))
                        } else {
                            AppData.reserve.upsert(ReserveEntry(AppData.newId(), kind, value, day!!.toEpochDay(), note.trim()))
                        }
                        amount = ""
                        note = ""
                    },
                ) { Text("Registrar") }
            }
        }
        SectionTitle("Histórico")
        entries.sortedByDescending { it.epochDay }.forEach { e ->
            Text(DateText.formatEpochDay(e.epochDay) + " · " + e.kind.label + " · " + FinanceCalc.formatCents(e.amountCents) + (if (e.note.isNotBlank()) " · " + e.note else ""))
        }
    }
}

@Composable
private fun Line(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold)
    }
}

/** Metas de ganhos (telas/17-metas): meta do mês e dias de trabalho informados por você. */
@Composable
fun GoalsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val goals by AppData.goals.items.collectAsState()
    val txs by AppData.transactions.items.collectAsState()
    val closes by AppData.dayCloses.items.collectAsState()
    val ym = YearMonth.now()
    val goal = goals.firstOrNull { it.id == ym.toString() }
    var kind by rememberSaveable { mutableStateOf(GoalKind.Income) }
    var target by rememberSaveable { mutableStateOf("") }
    var workDays by rememberSaveable { mutableStateOf("") }

    ScreenScaffold("Metas de ganhos", onBack, onOpen) {
        Text("Mês: " + "%02d/%d".format(ym.monthValue, ym.year), fontWeight = FontWeight.SemiBold)
        if (goal == null) {
            Text("Nenhuma meta definida para este mês. Nenhum valor é sugerido.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val closed = closes.count { YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) == ym }
            val p = GoalCalc.progress(goal, FinanceCalc.month(txs, ym), closed)
            SurfaceCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Line("Meta (" + goal.kind.label + ")", FinanceCalc.formatCents(goal.targetCents))
                    Line("Realizado", FinanceCalc.formatCents(p.earnedCents))
                    Line("Falta", FinanceCalc.formatCents(p.remainingCents), bold = true)
                    Line("Dias trabalhados (dias fechados)", p.workedDays.toString() + " de " + goal.workDays)
                    Line("Necessário por dia restante", p.neededPerDayCents?.let { FinanceCalc.formatCents(it) } ?: if (p.remainingCents == 0L) "meta atingida" else "sem dias restantes")
                }
            }
            TextButton(onClick = { AppData.goals.remove(goal.id) }) { Text("Remover meta do mês") }
        }
        SectionTitle(if (goal == null) "Definir meta" else "Substituir meta")
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChips(GoalKind.entries, kind, { it.label }) { kind = it }
                MoneyInput("Valor da meta (R$)", target, { target = it })
                TextInput("Dias de trabalho no mês", workDays, { workDays = it }, keyboard = KeyboardType.Number)
                val cents = OfferParsing.cents(target)
                val days = OfferParsing.wholeNumber(workDays)
                Button(
                    enabled = cents is Parsed.Ok && cents.value > 0 && days is Parsed.Ok && days.value in 1..31,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        AppData.goals.upsert(Goal(ym.toString(), kind, cents.valueOrNull()!!, days.valueOrNull()!!.toInt()))
                        target = ""
                        workDays = ""
                    },
                ) { Text("Salvar meta") }
            }
        }
        InfoNote("Só receitas confirmadas contam. Os dias trabalhados são os dias que você fechou em Fechar o dia.")
    }
}

/** Fechar o dia (telas/16-fechamento): conferir receitas e despesas do dia, informar odômetro e concluir. */
@Composable
fun DayCloseScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val txs by AppData.transactions.items.collectAsState()
    val closes by AppData.dayCloses.items.collectAsState()
    AppData.settings.items.collectAsState().value
    var date by rememberSaveable { mutableStateOf(DateText.format(LocalDate.now())) }
    var odometer by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    val day = DateText.parse(date)
    val vehicle = AppData.activeVehicle()
    val existing = day?.let { d -> closes.firstOrNull { it.epochDay == d.toEpochDay() } }
    val summary = day?.let { FinanceCalc.summary(FinanceCalc.day(txs, it)) }
    val odo = OfferParsing.wholeNumber(odometer)
    val odoBad = odo is Parsed.Invalid || (odo is Parsed.Ok && !MaintenanceCalc.acceptOdometer(vehicle?.odometerKm, odo.value))

    ScreenScaffold("Fechar o dia", onBack, onOpen) {
        DateInput("Dia", date, { date = it }, if (day == null) "Data inválida" else null)
        if (summary != null) {
            SurfaceCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Line("Receitas confirmadas", FinanceCalc.formatCents(summary.incomeCents))
                    Line("Despesas", FinanceCalc.formatCents(summary.expenseCents))
                    Line("Resultado de caixa", FinanceCalc.formatCents(summary.cashCents), bold = true)
                }
            }
        }
        if (existing != null) {
            InfoNote("Este dia já foi fechado: receitas " + FinanceCalc.formatCents(existing.incomeCents) + ", despesas " + FinanceCalc.formatCents(existing.expenseCents) +
                (existing.odometerKm?.let { ", odômetro $it km" } ?: "") + ". Os totais do fechamento ficam guardados como estavam.")
            TextButton(onClick = { AppData.dayCloses.remove(existing.id) }) { Text("Reabrir o dia") }
        } else {
            Text(
                if (vehicle != null) "Veículo ativo: " + vehicle.name + " (odômetro " + (vehicle.odometerKm?.let { "$it km" } ?: "desconhecido") + ")" else "Sem veículo ativo: o odômetro não será atualizado.",
                style = MaterialTheme.typography.bodySmall,
            )
            TextInput("Odômetro ao fim do dia (km, opcional)", odometer, { odometer = it }, keyboard = KeyboardType.Number,
                error = if (odoBad) "Inválido ou menor que o odômetro registrado" else null)
            TextInput("Observação (opcional)", note, { note = it })
            Button(
                enabled = day != null && !odoBad,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val d = day!!
                    val s = FinanceCalc.summary(FinanceCalc.day(txs, d))
                    val km = odo.valueOrNull()
                    AppData.dayCloses.upsert(DayClose(d.toString(), d.toEpochDay(), km, vehicle?.id, s.incomeCents, s.expenseCents, note.trim(), System.currentTimeMillis()))
                    if (km != null && vehicle != null) AppData.vehicles.upsert(vehicle.copy(odometerKm = maxOf(vehicle.odometerKm ?: 0, km)))
                    odometer = ""
                    note = ""
                },
            ) { Text("Concluir fechamento") }
        }
        InfoNote("Conferir antes de concluir: receitas só entram se confirmadas. Fechar o dia não altera lançamentos.")
    }
}
