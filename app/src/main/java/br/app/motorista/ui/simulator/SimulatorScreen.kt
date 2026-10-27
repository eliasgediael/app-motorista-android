package br.app.motorista.ui.simulator

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.app.motorista.domain.assessment.Assessment
import br.app.motorista.domain.assessment.Band
import br.app.motorista.domain.assessment.Evaluation
import br.app.motorista.domain.assessment.OfferEvaluator
import br.app.motorista.domain.assist.AlertCalc
import br.app.motorista.domain.assist.PlaceMatcher
import br.app.motorista.domain.calc.Display
import br.app.motorista.domain.calc.MetricResult
import br.app.motorista.domain.calc.OfferCalculator
import br.app.motorista.domain.calc.OfferInput
import br.app.motorista.domain.calc.OfferParsing
import br.app.motorista.domain.calc.Parsed
import br.app.motorista.data.AppData
import br.app.motorista.domain.card.PreviewState
import br.app.motorista.domain.vehicle.CostModel
import br.app.motorista.domain.vehicle.CostResult
import br.app.motorista.domain.criteria.DistanceTimeBasis
import br.app.motorista.domain.history.RuleVersion
import br.app.motorista.domain.history.TripRecord
import br.app.motorista.domain.offer.Freshness
import br.app.motorista.domain.offer.OfferOrigin
import br.app.motorista.domain.offer.OfferSnapshot
import br.app.motorista.domain.offer.Platform
import br.app.motorista.floating.FloatingControlService
import br.app.motorista.offer.BalloonHub
import br.app.motorista.ui.card.PreviewFrame
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SectionTitle
import br.app.motorista.ui.common.SurfaceCard
import br.app.motorista.ui.common.TextInput
import br.app.motorista.ui.assist.currentAlertRules
import br.app.motorista.ui.components.BalloonValues
import br.app.motorista.ui.state.PreferencesState
import br.app.motorista.ui.state.SimulatorForm
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

private data class ParsedOffer(val input: OfferInput, val invalidFields: Set<String>)

private fun <T> field(name: String, parsed: Parsed<T>, invalid: MutableSet<String>): T? = when (parsed) {
    is Parsed.Ok -> parsed.value
    Parsed.Blank -> null
    Parsed.Invalid -> {
        invalid += name
        null
    }
}

private fun parse(f: SimulatorForm): ParsedOffer {
    val bad = mutableSetOf<String>()
    val input = OfferInput(
        fareCents = field("fare", OfferParsing.cents(f.fare), bad),
        pickupMeters = field("pickupKm", OfferParsing.meters(f.pickupKm), bad),
        tripMeters = field("tripKm", OfferParsing.meters(f.tripKm), bad),
        returnMeters = field("returnKm", OfferParsing.meters(f.returnKm), bad),
        pickupSeconds = field("pickupMin", OfferParsing.seconds(f.pickupMin), bad),
        tripSeconds = field("tripMin", OfferParsing.seconds(f.tripMin), bad),
        returnSeconds = field("returnMin", OfferParsing.seconds(f.returnMin), bad),
        passengerRating = field("rating", OfferParsing.rating(f.rating), bad),
    )
    return ParsedOffer(input, bad)
}

/** Simulador manual: oferta digitada pelo usuário. Não lê nenhum aplicativo de corrida. */
@Composable
fun SimulatorScreen(
    state: PreferencesState,
    title: String = "Simulador de oferta",
    onChange: ((SimulatorForm) -> SimulatorForm) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val form = state.simulator
    val parsed = parse(form)
    val bad = parsed.invalidFields
    val evaluation = OfferEvaluator.evaluate(parsed.input, state.activeRules(), state.policy)
    val terms by AppData.flaggedTerms.items.collectAsState()
    AppData.settings.items.collectAsState().value
    // Motivos de atenção: alerta com limite do usuário e termos sinalizados. São avisos, não avaliação nem bloqueio.
    val extras = AlertCalc.evaluate(parsed.input, currentAlertRules()) + PlaceMatcher.matches(form.pickupText, form.dropoffText, terms)

    ScreenScaffold(title, onBack, onOpen) {
        InfoNote("Digite uma oferta para ver os cálculos. Entrada manual: nada é lido de 99, Uber ou inDrive. Campo vazio é dado ausente, não zero.")

        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Num("Valor ao motorista (R$)", form.fare, "fare" in bad) { v -> onChange { it.copy(fare = v) } }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Half("Embarque km", form.pickupKm, "pickupKm" in bad, false) { v -> onChange { it.copy(pickupKm = v) } }
                    Half("Embarque min", form.pickupMin, "pickupMin" in bad, true) { v -> onChange { it.copy(pickupMin = v) } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Half("Viagem km", form.tripKm, "tripKm" in bad, false) { v -> onChange { it.copy(tripKm = v) } }
                    Half("Viagem min", form.tripMin, "tripMin" in bad, true) { v -> onChange { it.copy(tripMin = v) } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Half("Retorno km", form.returnKm, "returnKm" in bad, false) { v -> onChange { it.copy(returnKm = v) } }
                    Half("Retorno min", form.returnMin, "returnMin" in bad, true) { v -> onChange { it.copy(returnMin = v) } }
                }
                Num("Nota do passageiro", form.rating, "rating" in bad) { v -> onChange { it.copy(rating = v) } }
                TextInput("Embarque (texto, opcional, não é guardado)", form.pickupText, { v -> onChange { it.copy(pickupText = v) } })
                TextInput("Destino (texto, opcional, não é guardado)", form.dropoffText, { v -> onChange { it.copy(dropoffText = v) } })
                if (bad.isNotEmpty()) {
                    Text(
                        "Há campos inválidos: use números não negativos; minutos inteiros; até 2 casas em R$ e 3 em km.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        SectionTitle("Base para exibir as métricas")
        BasisMenu(form.displayBasis) { b -> onChange { it.copy(displayBasis = b) } }
        Text(
            "Cada critério usa a base cadastrada nele. Esta base vale só para os valores mostrados abaixo e no balão.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionTitle("Métricas calculadas")
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val b = form.displayBasis
                MetricLine("R$/km", OfferCalculator.revenuePerKm(parsed.input, b))
                MetricLine("R$/hora", OfferCalculator.revenuePerHour(parsed.input, b))
                MetricLine("R$/minuto", OfferCalculator.revenuePerMinute(parsed.input, b))
                Text(
                    "Fórmulas candidatas (${OfferCalculator.FORMULA_VERSION}); arredondamento de exibição provisório.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SectionTitle("Avaliação")
        AssessmentSection(evaluation, state, parsed, onOpen)
        if (extras.isNotEmpty()) {
            SectionTitle("Atenção")
            extras.forEach { Text("• $it", color = MaterialTheme.colorScheme.error) }
        }

        SectionTitle("Enviar ao balão flutuante")
        PublishSection(state, parsed, onChange, extras)

        SectionTitle("Diagnóstico do balão flutuante")
        DiagnosticSection()
    }
}

@Composable
private fun AssessmentSection(evaluation: Evaluation, state: PreferencesState, parsed: ParsedOffer, onOpen: (String) -> Unit) {
    SurfaceCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (evaluation.outcomes.isEmpty()) {
                Text("Nenhum critério ativo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            evaluation.outcomes.forEach { o ->
                val text = when (val r = o.result) {
                    is MetricResult.Unavailable -> "indisponível (${r.reason.label})"
                    is MetricResult.Value -> Display.decimal(r.value) + " " + r.unit + (o.band?.let { " · " + bandLabel(it) } ?: " · faixa pendente")
                }
                Text(o.rule.metric.label + ": " + text)
            }
        }
    }
    when (val a = evaluation.assessment) {
        Assessment.NoCriteria -> InfoNote("Sem critérios ativos: nenhuma avaliação é emitida. Cadastre e ative critérios.")
        is Assessment.PolicyPending -> InfoNote("Sem cor geral: falta definir ${a.missing.joinToString(", ")}. Abra Combinar critérios.")
        Assessment.SeparateOnly -> InfoNote("Você escolheu métricas separadas; não há cor geral.")
        Assessment.InsufficientData -> InfoNote("Dados insuficientes para uma cor geral. Dado ausente não é tratado como zero nem como reprovação.")
        is Assessment.Colored -> {
            val preview = when (a.band) {
                Band.Good -> PreviewState.Good
                Band.Attention -> PreviewState.Attention
                Band.Bad -> PreviewState.Bad
            }
            PreviewFrame(preview, state.cardStyle, simulatorValues(state, parsed))
            Text(
                "Selo informativo: o aplicativo não aceita nem recusa corridas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    OutlinedButton(onClick = { onOpen("combinacao") }, modifier = Modifier.fillMaxWidth()) { Text("Combinar critérios") }
}

/** Balões de EXEMPLO no botão flutuante: só diagnóstico, nunca aparecem no menu de operação. */
@Composable
private fun DiagnosticSection() {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    Text(
        "Mostra um balão de exemplo (dados fictícios) pelo botão flutuante, para conferir cores e layout. Não é oferta nem leitura real.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PreviewState.entries.forEach { s ->
            OutlinedButton(
                onClick = {
                    message = if (FloatingControlService.showSample(context, s)) null else "Ligue o copiloto em Controle flutuante antes."
                },
                modifier = Modifier.weight(1f),
            ) { Text(s.label) }
        }
    }
    message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun PublishSection(state: PreferencesState, parsed: ParsedOffer, onChange: ((SimulatorForm) -> SimulatorForm) -> Unit, extras: List<String>) {
    val platform = state.simulator.platform
    var published by remember { mutableStateOf<String?>(null) }
    Text(
        "Plataforma da oferta (cada plataforma guarda a sua; nada se mistura):",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Platform.entries.forEach { p ->
            FilterChip(
                selected = platform == p,
                onClick = {
                    onChange { it.copy(platform = p) }
                    published = null
                },
                label = { Text(p.label) },
            )
        }
    }
    val canPublish = platform != null && parsed.invalidFields.isEmpty()
    OutlinedButton(
        enabled = canPublish,
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            val p = platform ?: return@OutlinedButton
            val now = SystemClock.elapsedRealtime()
            val snapshot = OfferSnapshot(
                id = UUID.randomUUID().toString(),
                platform = p,
                origin = OfferOrigin.Manual,
                capturedAtMs = now,
                input = parsed.input,
            )
            val evaluation = OfferEvaluator.evaluate(parsed.input, state.activeRules(), state.policy)
            val values = simulatorValues(state, parsed, p.label)
            if (BalloonHub.publish(snapshot, evaluation, values, extras)) {
                // Fica no histórico como NÃO confirmada: nunca vira receita sem confirmação do usuário.
                val summary = RuleVersion.summary(state.activeRules(), state.policy)
                AppData.trips.upsert(
                    TripRecord(
                        id = snapshot.id, atMs = System.currentTimeMillis(), epochDay = LocalDate.now().toEpochDay(),
                        platform = p.label, origin = OfferOrigin.Manual.label, input = parsed.input,
                        metricsText = "R$/km " + values.perKm + " · R$/h " + values.perHour + " · R$/min " + values.perMinute +
                            " (base " + (state.simulator.displayBasis?.label ?: "não escolhida") + ")",
                        bandName = (evaluation.assessment as? Assessment.Colored)?.band?.name,
                        note = "", ruleVersionId = RuleVersion.id(summary), ruleSummary = summary,
                    ),
                )
                published = p.label + " · manual · válida por " + (Freshness.PROVISIONAL_MAX_AGE_MS / 1000) + " s (prazo provisório)"
            }
        },
    ) { Text("Publicar oferta") }
    Text(
        published?.let { "Publicada: $it. Use o menu do botão flutuante > Última oferta analisada." }
            ?: "Escolha a plataforma e publique. A oferta vale por tempo limitado e depois aparece como indisponível.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun bandLabel(b: Band): String = when (b) {
    Band.Good -> "verde"
    Band.Attention -> "amarelo"
    Band.Bad -> "vermelho"
}

private fun simulatorValues(state: PreferencesState, parsed: ParsedOffer, tag: String = "Manual"): BalloonValues {
    val basis = state.simulator.displayBasis
    val input = parsed.input
    fun fmt(r: MetricResult): String = if (r is MetricResult.Value) Display.decimal(r.value) else "—"
    val meters = basis?.let { OfferCalculator.totalMeters(input, it) }
    val secs = basis?.let { OfferCalculator.totalSeconds(input, it) }
    return BalloonValues(
        time = secs?.let { (it / 60).toString() + " min" } ?: "— min",
        distance = meters?.let { Display.decimal(BigDecimal(it).movePointLeft(3), 1) + " km" } ?: "— km",
        rating = input.passengerRating?.let { Display.decimal(it) } ?: "—",
        perKm = fmt(OfferCalculator.revenuePerKm(input, basis)),
        perHour = fmt(OfferCalculator.revenuePerHour(input, basis)),
        perMinute = fmt(OfferCalculator.revenuePerMinute(input, basis)),
        fare = input.fareCents?.let { Display.decimal(BigDecimal(it).movePointLeft(2)) } ?: "—",
        result = resultText(input, basis),
        tag = tag,
    )
}

/** Resultado estimado só com modelo de custo confirmado pelo usuário e base escolhida; senão, indisponível. */
private fun resultText(input: OfferInput, basis: DistanceTimeBasis?): String {
    val validated = AppData.setting("costValidated") == "true"
    val cost = if (validated) (CostModel.costPerKm(AppData.costComponents.items.value) as? CostResult.Valid)?.perKm else null
    return when (val r = OfferCalculator.estimatedResult(input, basis, cost)) {
        is MetricResult.Value -> "R$ " + Display.decimal(r.value)
        is MetricResult.Unavailable -> "indisponível (" + r.reason.label + ")"
    }
}

@Composable
private fun MetricLine(label: String, r: MetricResult) {
    val text = when (r) {
        is MetricResult.Value -> Display.decimal(r.value)
        is MetricResult.Unavailable -> "indisponível (" + r.reason.label + ")"
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Num(label: String, value: String, error: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        isError = error,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
private fun RowScope.Half(label: String, value: String, error: Boolean, integer: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.weight(1f),
        label = { Text(label) },
        singleLine = true,
        isError = error,
        keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
    )
}

@Composable
private fun BasisMenu(selected: DistanceTimeBasis?, onSelect: (DistanceTimeBasis) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.label ?: "Escolher base", Modifier.weight(1f))
            Icon(Icons.Outlined.KeyboardArrowDown, null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DistanceTimeBasis.entries.forEach { b ->
                DropdownMenuItem(text = { Text(b.label) }, onClick = {
                    onSelect(b)
                    open = false
                })
            }
        }
    }
}
