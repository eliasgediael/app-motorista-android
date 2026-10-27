package br.app.motorista.ui.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import br.app.motorista.data.AppData
import br.app.motorista.domain.calc.OfferParsing
import br.app.motorista.domain.calc.Parsed
import br.app.motorista.domain.finance.Category
import br.app.motorista.domain.finance.CategoryKind
import br.app.motorista.domain.finance.DateText
import br.app.motorista.domain.finance.EntryKind
import br.app.motorista.domain.finance.FinanceCalc
import br.app.motorista.domain.finance.Transaction
import br.app.motorista.ui.common.ChoiceChips
import br.app.motorista.ui.common.ConfirmDialog
import br.app.motorista.ui.common.DateInput
import br.app.motorista.ui.common.DropdownInput
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.MoneyInput
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SectionTitle
import br.app.motorista.ui.common.SurfaceCard
import br.app.motorista.ui.common.TextInput
import java.time.LocalDate
import java.time.YearMonth

/** Finanças (telas/14-financas): receitas confirmadas, despesas e caixa do mês, em bases separadas. */
@Composable
fun FinanceScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val txs by AppData.transactions.items.collectAsState()
    val cats by AppData.categories.items.collectAsState()
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val ym = runCatching { YearMonth.parse(monthText) }.getOrDefault(YearMonth.now())
    var toDelete by rememberSaveable { mutableStateOf<String?>(null) }

    val monthTxs = FinanceCalc.month(txs, ym).sortedByDescending { it.epochDay }
    val summary = FinanceCalc.summary(monthTxs)

    ScreenScaffold("Finanças", onBack, onOpen) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { monthText = ym.minusMonths(1).toString() }) { Text("‹ Anterior") }
            Text("%02d/%d".format(ym.monthValue, ym.year), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { monthText = ym.plusMonths(1).toString() }) { Text("Próximo ›") }
        }

        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SummaryLine("Receitas confirmadas", FinanceCalc.formatCents(summary.incomeCents))
                SummaryLine("Despesas lançadas", FinanceCalc.formatCents(summary.expenseCents))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SummaryLine("Resultado de caixa", FinanceCalc.formatCents(summary.cashCents), bold = true)
                SummaryLine("Resultado estimado", "indisponível")
            }
        }
        Text(
            "Caixa = receitas confirmadas − despesas lançadas. Resultado estimado exige modelo de custo validado (Custo operacional). " +
                "Reserva e provisões não entram como despesa para não deduzir duas vezes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(onClick = { onOpen("lancamento") }, modifier = Modifier.fillMaxWidth()) { Text("Novo lançamento") }

        SectionTitle("Lançamentos do mês")
        if (monthTxs.isEmpty()) {
            Text("Nenhum lançamento neste mês.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        monthTxs.forEach { t ->
            val catName = cats.firstOrNull { it.id == t.categoryId }?.name ?: "Sem categoria"
            SurfaceCard {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text((if (t.kind == EntryKind.Income) "+ " else "− ") + FinanceCalc.formatCents(t.amountCents), fontWeight = FontWeight.SemiBold)
                        Text(
                            DateText.formatEpochDay(t.epochDay) + " · " + catName + (if (t.note.isNotBlank()) " · " + t.note else ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { toDelete = t.id }) { Text("Excluir") }
                }
            }
        }
    }

    toDelete?.let { id ->
        ConfirmDialog(
            title = "Excluir lançamento?",
            text = "Esta ação não pode ser desfeita.",
            confirmLabel = "Excluir",
            onConfirm = {
                AppData.transactions.remove(id)
                toDelete = null
            },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold)
    }
}

/** Novo lançamento (telas/15-lancamento): receita ou despesa, com confirmação explícita. */
@Composable
fun EntryFormScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val cats by AppData.categories.items.collectAsState()
    var kind by rememberSaveable { mutableStateOf(EntryKind.Income) }
    var amount by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(DateText.format(LocalDate.now())) }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var platformId by rememberSaveable { mutableStateOf<String?>(null) }
    var note by rememberSaveable { mutableStateOf("") }
    var confirmed by rememberSaveable { mutableStateOf(false) }
    var tried by rememberSaveable { mutableStateOf(false) }

    val cents = OfferParsing.cents(amount)
    val day = DateText.parse(date)
    val amountError = when {
        !tried -> null
        cents is Parsed.Ok && cents.value > 0 -> null
        cents is Parsed.Blank -> "Informe o valor"
        else -> "Valor inválido (use até 2 casas decimais, maior que zero)"
    }
    val dateError = if (tried && day == null) "Data inválida (dd/mm/aaaa)" else null
    val kindCats = cats.filter { !it.archived && it.kind == (if (kind == EntryKind.Income) CategoryKind.Income else CategoryKind.Expense) }
    val platforms = cats.filter { !it.archived && it.kind == CategoryKind.Platform }

    ScreenScaffold("Novo lançamento", onBack, onOpen) {
        ChoiceChips(EntryKind.entries, kind, { it.label }) {
            kind = it
            categoryId = null
        }
        MoneyInput("Valor (R$)", amount, { amount = it }, amountError)
        DateInput("Data", date, { date = it }, dateError)
        Text("Categoria", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        DropdownInput(kindCats, kindCats.firstOrNull { it.id == categoryId }, "Escolher categoria", { it.name }, { categoryId = it?.id })
        if (kind == EntryKind.Income) {
            Text("Plataforma", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            DropdownInput(platforms, platforms.firstOrNull { it.id == platformId }, "Escolher plataforma", { it.name }, { platformId = it?.id })
        }
        TextInput("Observação (opcional)", note, { note = it })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
            Text(
                if (kind == EntryKind.Income) "Confirmo que esta receita foi realmente recebida." else "Confirmo esta despesa.",
                modifier = Modifier.weight(1f),
            )
        }
        Button(
            enabled = confirmed,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                tried = true
                if (cents is Parsed.Ok && cents.value > 0 && day != null) {
                    AppData.transactions.upsert(
                        Transaction(
                            id = AppData.newId(),
                            kind = kind,
                            amountCents = cents.value,
                            epochDay = day.toEpochDay(),
                            categoryId = categoryId,
                            platformId = if (kind == EntryKind.Income) platformId else null,
                            note = note.trim(),
                        ),
                    )
                    onBack()
                }
            },
        ) { Text("Salvar lançamento") }
        InfoNote("Receita só entra nas finanças por confirmação sua. Análise de oferta nunca vira receita sozinha.")
    }
}

/** Categorias e plataformas (telas/20-categorias): criar, renomear e arquivar. Arquivar não apaga lançamentos. */
@Composable
fun CategoriesScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val cats by AppData.categories.items.collectAsState()
    var name by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(CategoryKind.Expense) }
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }

    ScreenScaffold("Categorias e plataformas", onBack, onOpen) {
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nova categoria", fontWeight = FontWeight.SemiBold)
                ChoiceChips(CategoryKind.entries, kind, { it.label }) { kind = it }
                TextInput("Nome", name, { name = it })
                Button(
                    enabled = name.isNotBlank() && cats.none { it.kind == kind && it.name.equals(name.trim(), ignoreCase = true) },
                    onClick = {
                        AppData.categories.upsert(Category(AppData.newId(), name.trim(), kind))
                        name = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Adicionar") }
                Text(
                    "Plataformas aqui são só nomes para classificar lançamentos; não ativam integração com nenhum aplicativo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        CategoryKind.entries.forEach { k ->
            SectionTitle(k.label + "s")
            val list = cats.filter { it.kind == k }
            if (list.isEmpty()) Text("Nenhuma.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            list.forEach { c ->
                SurfaceCard {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            c.name + if (c.archived) " (arquivada)" else "",
                            Modifier.weight(1f),
                            color = if (c.archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                        TextButton(onClick = {
                            renaming = c.id
                            renameText = c.name
                        }) { Text("Renomear") }
                        TextButton(onClick = { AppData.categories.upsert(c.copy(archived = !c.archived)) }) {
                            Text(if (c.archived) "Restaurar" else "Arquivar")
                        }
                    }
                }
            }
        }
    }

    renaming?.let { id ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Renomear") },
            text = { TextInput("Nome", renameText, { renameText = it }) },
            confirmButton = {
                TextButton(
                    enabled = renameText.isNotBlank(),
                    onClick = {
                        AppData.categories.get(id)?.let { AppData.categories.upsert(it.copy(name = renameText.trim())) }
                        renaming = null
                    },
                ) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancelar") } },
        )
    }
}
