package br.app.motorista.ui.quote

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.app.motorista.data.AppData
import br.app.motorista.domain.calc.OfferParsing
import br.app.motorista.domain.calc.Parsed
import br.app.motorista.domain.calc.valueOrNull
import br.app.motorista.domain.finance.DateText
import br.app.motorista.domain.finance.FinanceCalc
import br.app.motorista.domain.quote.Quote
import br.app.motorista.domain.quote.QuoteCalc
import br.app.motorista.domain.quote.QuoteInput
import br.app.motorista.domain.vehicle.CostModel
import br.app.motorista.domain.vehicle.CostResult
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SectionTitle
import br.app.motorista.ui.common.SurfaceCard
import br.app.motorista.ui.common.TextInput
import java.time.LocalDate

/** Orçamento em memória, repassado ao recibo. Nada é gravado em disco. */
object QuoteDraft {
    var quote: Quote? = null
    var input: QuoteInput? = null
}

/** Viagem particular (telas/24-particular): orçamento com preços digitados por você. Custo e margem ficam só aqui. */
@Composable
fun PrivateTripScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    var pickup by rememberSaveable { mutableStateOf("") }
    var trip by rememberSaveable { mutableStateOf("") }
    var ret by rememberSaveable { mutableStateOf("") }
    var inclPickup by rememberSaveable { mutableStateOf(true) }
    var inclReturn by rememberSaveable { mutableStateOf(false) }
    var minutes by rememberSaveable { mutableStateOf("") }
    var rateKm by rememberSaveable { mutableStateOf("") }
    var rateMin by rememberSaveable { mutableStateOf("") }
    var tolls by rememberSaveable { mutableStateOf("") }

    val fields = listOf(pickup, trip, ret, minutes, rateKm, rateMin).map { OfferParsing.decimal(it) }
    val tollsP = OfferParsing.cents(tolls)
    val invalid = fields.any { it is Parsed.Invalid } || tollsP is Parsed.Invalid
    val input = QuoteInput(
        fields[0].valueOrNull(), fields[1].valueOrNull(), fields[2].valueOrNull(), inclPickup, inclReturn,
        fields[3].valueOrNull(), fields[4].valueOrNull(), fields[5].valueOrNull(), tollsP.valueOrNull(),
    )
    val quote = QuoteCalc.compute(input)
    val validated = AppData.setting("costValidated") == "true"
    val costPerKm = if (validated) (CostModel.costPerKm(AppData.costComponents.items.value) as? CostResult.Valid)?.perKm else null
    val internal = QuoteCalc.internalCostCents(input, costPerKm)

    ScreenScaffold("Viagem particular", onBack, onOpen) {
        InfoNote("Os preços por km, por minuto e pedágios são seus: o app não sugere nenhum valor. Campo de preço vazio = não cobrar essa linha.")
        TextInput("Km até o embarque", pickup, { pickup = it }, keyboard = KeyboardType.Decimal)
        TextInput("Km do percurso", trip, { trip = it }, keyboard = KeyboardType.Decimal)
        TextInput("Km de retorno", ret, { ret = it }, keyboard = KeyboardType.Decimal)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = inclPickup, onCheckedChange = { inclPickup = it })
            Text("Cobrar o embarque", Modifier.weight(1f))
            Checkbox(checked = inclReturn, onCheckedChange = { inclReturn = it })
            Text("Cobrar o retorno", Modifier.weight(1f))
        }
        TextInput("Tempo total (minutos)", minutes, { minutes = it }, keyboard = KeyboardType.Decimal)
        TextInput("Preço por km (R$, opcional)", rateKm, { rateKm = it }, keyboard = KeyboardType.Decimal)
        TextInput("Preço por minuto (R$, opcional)", rateMin, { rateMin = it }, keyboard = KeyboardType.Decimal)
        TextInput("Pedágios (R$, opcional)", tolls, { tolls = it }, keyboard = KeyboardType.Decimal)
        if (invalid) Text("Há campos inválidos (números não negativos).", color = MaterialTheme.colorScheme.error)

        SectionTitle("Orçamento")
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                quote.lines.forEach { Row2(it.label, FinanceCalc.formatCents(it.cents)) }
                Row2("Total", FinanceCalc.formatCents(quote.totalCents), bold = true)
                quote.notes.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        SectionTitle("Interno (não vai ao recibo)")
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (internal == null) {
                    Text("Custo e margem indisponíveis: faltam km completos ou modelo de custo confirmado (Custo operacional).")
                } else {
                    Row2("Custo estimado", FinanceCalc.formatCents(internal))
                    Row2("Margem (orçamento − custo)", FinanceCalc.formatCents(quote.totalCents - internal))
                }
            }
        }
        Button(
            enabled = !invalid && quote.lines.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                QuoteDraft.quote = quote
                QuoteDraft.input = input
                onOpen("recibo")
            },
        ) { Text("Gerar orçamento e recibo") }
    }
}

@Composable
private fun Row2(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold)
    }
}

/** Orçamento e recibo (telas/25-recibo): documento revisável para compartilhar, sem custo nem margem. */
@Composable
fun ReceiptScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val quote = QuoteDraft.quote
    var title by rememberSaveable { mutableStateOf("Orçamento de viagem") }
    var client by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    ScreenScaffold("Orçamento e recibo", onBack, onOpen) {
        if (quote == null) {
            Text("Nenhum orçamento gerado. Calcule em Viagem particular.")
            OutlinedButton(onClick = { onOpen("particular") }, modifier = Modifier.fillMaxWidth()) { Text("Abrir Viagem particular") }
            return@ScreenScaffold
        }
        TextInput("Título", title, { title = it })
        TextInput("Para (opcional; não é guardado)", client, { client = it })
        TextInput("Observações (opcional)", notes, { notes = it }, singleLine = false)
        val text = buildString {
            appendLine(title.ifBlank { "Orçamento" })
            appendLine("Data: " + DateText.format(LocalDate.now()))
            if (client.isNotBlank()) appendLine("Para: " + client.trim())
            appendLine()
            quote.lines.forEach { appendLine(it.label + ": " + FinanceCalc.formatCents(it.cents)) }
            appendLine("Total: " + FinanceCalc.formatCents(quote.totalCents))
            if (notes.isNotBlank()) {
                appendLine()
                appendLine(notes.trim())
            }
        }
        SectionTitle("Prévia (é exatamente o que será compartilhado)")
        SurfaceCard { Text(text, Modifier.padding(16.dp)) }
        Text(
            "Não inclui custo, margem nem dados internos. Só sai do aparelho se você compartilhar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                context.startActivity(Intent.createChooser(send, "Compartilhar").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            },
        ) { Text("Compartilhar") }
    }
}
