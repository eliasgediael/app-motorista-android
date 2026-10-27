package br.app.motorista.ui.vehicle

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
import br.app.motorista.domain.calc.valueOrNull
import br.app.motorista.domain.finance.Category
import br.app.motorista.domain.finance.CategoryKind
import br.app.motorista.domain.finance.DateText
import br.app.motorista.domain.finance.EntryKind
import br.app.motorista.domain.finance.FinanceCalc
import br.app.motorista.domain.finance.Transaction
import br.app.motorista.domain.finance.TxSource
import br.app.motorista.domain.vehicle.CostComponent
import br.app.motorista.domain.vehicle.CostKind
import br.app.motorista.domain.vehicle.CostModel
import br.app.motorista.domain.vehicle.CostResult
import br.app.motorista.domain.vehicle.MaintenanceCalc
import br.app.motorista.domain.vehicle.MaintenanceItem
import br.app.motorista.domain.vehicle.MaintenanceRecord
import br.app.motorista.domain.vehicle.MaintenanceStatus
import br.app.motorista.domain.vehicle.Ownership
import br.app.motorista.domain.vehicle.Vehicle
import br.app.motorista.ui.common.ChoiceChips
import br.app.motorista.ui.common.ConfirmDialog
import br.app.motorista.ui.common.DateInput
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.MoneyInput
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SectionTitle
import br.app.motorista.ui.common.SurfaceCard
import br.app.motorista.ui.common.TextInput
import java.math.BigDecimal
import java.time.LocalDate

/** Meus veículos (telas/22-veiculos): cadastro, veículo ativo e arquivamento. */
@Composable
fun VehiclesScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val vehicles by AppData.vehicles.items.collectAsState()
    val settings by AppData.settings.items.collectAsState()
    val activeId = settings.firstOrNull { it.id == "activeVehicle" }?.value
    var name by rememberSaveable { mutableStateOf("") }
    var ownership by rememberSaveable { mutableStateOf(Ownership.Own) }
    var odometer by rememberSaveable { mutableStateOf("") }
    var odoText by rememberSaveable { mutableStateOf<String?>(null) }
    var odoFor by rememberSaveable { mutableStateOf<String?>(null) }
    val odo = OfferParsing.wholeNumber(odometer)
    val odoError = if (odo is Parsed.Invalid) "Use um número inteiro de km" else null

    ScreenScaffold("Meus veículos", onBack, onOpen) {
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Novo veículo", fontWeight = FontWeight.SemiBold)
                TextInput("Nome (ex.: modelo)", name, { name = it })
                ChoiceChips(Ownership.entries, ownership, { it.label }) { ownership = it }
                TextInput("Odômetro atual (km, opcional)", odometer, { odometer = it }, error = odoError, keyboard = androidx.compose.ui.text.input.KeyboardType.Number)
                Button(
                    enabled = name.isNotBlank() && odo !is Parsed.Invalid,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val v = Vehicle(AppData.newId(), name.trim(), ownership, odo.valueOrNull())
                        AppData.vehicles.upsert(v)
                        if (AppData.activeVehicle() == null) AppData.putSetting("activeVehicle", v.id)
                        name = ""
                        odometer = ""
                    },
                ) { Text("Adicionar veículo") }
            }
        }
        SectionTitle("Cadastrados")
        if (vehicles.isEmpty()) Text("Nenhum veículo cadastrado. Odômetro e manutenção ficam desconhecidos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        vehicles.forEach { v ->
            SurfaceCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(v.name + (if (v.id == activeId) " · ATIVO" else "") + (if (v.archived) " · arquivado" else ""), fontWeight = FontWeight.SemiBold)
                    Text(v.ownership.label + " · " + (v.odometerKm?.let { "$it km" } ?: "odômetro desconhecido"), style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (v.id != activeId && !v.archived) TextButton(onClick = { AppData.putSetting("activeVehicle", v.id) }) { Text("Tornar ativo") }
                        TextButton(onClick = {
                            odoFor = v.id
                            odoText = v.odometerKm?.toString() ?: ""
                        }) { Text("Odômetro") }
                        TextButton(onClick = { AppData.vehicles.upsert(v.copy(archived = !v.archived)) }) { Text(if (v.archived) "Restaurar" else "Arquivar") }
                    }
                }
            }
        }
    }

    odoFor?.let { id ->
        val current = AppData.vehicles.get(id)
        val typed = OfferParsing.wholeNumber(odoText ?: "")
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { odoFor = null },
            title = { Text("Odômetro") },
            text = {
                Column {
                    TextInput("Km atual", odoText ?: "", { odoText = it }, keyboard = androidx.compose.ui.text.input.KeyboardType.Number)
                    if (typed is Parsed.Ok && !MaintenanceCalc.acceptOdometer(current?.odometerKm, typed.value)) {
                        Text("Menor que o odômetro registrado (${current?.odometerKm} km): provável erro de digitação.", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = typed is Parsed.Ok && MaintenanceCalc.acceptOdometer(current?.odometerKm, typed.value),
                    onClick = {
                        if (current != null && typed is Parsed.Ok) AppData.vehicles.upsert(current.copy(odometerKm = typed.value))
                        odoFor = null
                    },
                ) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { odoFor = null }) { Text("Cancelar") } },
        )
    }
}

/** Manutenção (telas/23-manutencao): itens com agenda configurada por você e registros que geram despesa. */
@Composable
fun MaintenanceScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val vehicle = AppData.activeVehicle()
    val items by AppData.maintenanceItems.items.collectAsState()
    val records by AppData.maintenanceRecords.items.collectAsState()
    val cats by AppData.categories.items.collectAsState()
    AppData.settings.items.collectAsState().value // recompõe quando o veículo ativo muda
    var itemName by rememberSaveable { mutableStateOf("") }
    var interval by rememberSaveable { mutableStateOf("") }
    var lastKm by rememberSaveable { mutableStateOf("") }
    var recName by rememberSaveable { mutableStateOf("") }
    var recKm by rememberSaveable { mutableStateOf("") }
    var recDate by rememberSaveable { mutableStateOf(DateText.format(LocalDate.now())) }
    var recCost by rememberSaveable { mutableStateOf("") }
    var recItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var tried by rememberSaveable { mutableStateOf(false) }

    ScreenScaffold("Manutenção", onBack, onOpen) {
        if (vehicle == null) {
            InfoNote("Cadastre e escolha um veículo ativo em Meus veículos para registrar manutenção.")
            Button(onClick = { onOpen("veiculos") }, modifier = Modifier.fillMaxWidth()) { Text("Abrir Meus veículos") }
            return@ScreenScaffold
        }
        Text("Veículo: ${vehicle.name} · " + (vehicle.odometerKm?.let { "$it km" } ?: "odômetro desconhecido"), fontWeight = FontWeight.SemiBold)

        SectionTitle("Agenda de serviços")
        val myItems = items.filter { it.vehicleId == vehicle.id }
        if (myItems.isEmpty()) Text("Nenhum item. A agenda é configurada por você; nada é presumido.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        myItems.forEach { it2 ->
            SurfaceCard {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(it2.name, fontWeight = FontWeight.SemiBold)
                        val text = when (val st = MaintenanceCalc.status(it2, vehicle.odometerKm)) {
                            is MaintenanceStatus.Unknown -> "Não dá para calcular: falta " + st.missing.joinToString(", ")
                            is MaintenanceStatus.Scheduled ->
                                "Próximo aos ${st.nextDueKm} km · " + if (st.overdue) "atrasado ${-st.remainingKm} km" else "faltam ${st.remainingKm} km"
                        }
                        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { AppData.maintenanceItems.remove(it2.id) }) { Text("Remover") }
                }
            }
        }
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Novo item", fontWeight = FontWeight.SemiBold)
                TextInput("Serviço (ex.: troca de óleo)", itemName, { itemName = it })
                TextInput("Intervalo (km, opcional)", interval, { interval = it }, keyboard = androidx.compose.ui.text.input.KeyboardType.Number)
                TextInput("Km da última execução (opcional)", lastKm, { lastKm = it }, keyboard = androidx.compose.ui.text.input.KeyboardType.Number)
                val i = OfferParsing.wholeNumber(interval)
                val l = OfferParsing.wholeNumber(lastKm)
                Button(
                    enabled = itemName.isNotBlank() && i !is Parsed.Invalid && l !is Parsed.Invalid,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        AppData.maintenanceItems.upsert(
                            MaintenanceItem(AppData.newId(), vehicle.id, itemName.trim(), i.valueOrNull(), l.valueOrNull()),
                        )
                        itemName = ""
                        interval = ""
                        lastKm = ""
                    },
                ) { Text("Adicionar à agenda") }
            }
        }

        SectionTitle("Registrar serviço feito")
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextInput("Serviço", recName, { recName = it })
                TextInput("Km no serviço", recKm, { recKm = it }, keyboard = androidx.compose.ui.text.input.KeyboardType.Number)
                DateInput("Data", recDate, { recDate = it })
                MoneyInput("Custo (R$, opcional)", recCost, { recCost = it })
                if (myItems.isNotEmpty()) {
                    Text("Atualiza o item da agenda:", style = MaterialTheme.typography.bodySmall)
                    ChoiceChips(myItems, myItems.firstOrNull { it.id == recItemId }, { it.name }) { recItemId = it.id }
                }
                val km = OfferParsing.wholeNumber(recKm)
                val cost = OfferParsing.cents(recCost)
                val day = DateText.parse(recDate)
                val kmBad = km is Parsed.Invalid || (km is Parsed.Ok && !MaintenanceCalc.acceptOdometer(vehicle.odometerKm, km.value))
                if (tried && kmBad) Text("Km inválido ou menor que o odômetro registrado.", color = MaterialTheme.colorScheme.error)
                if (tried && day == null) Text("Data inválida.", color = MaterialTheme.colorScheme.error)
                Button(
                    enabled = recName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        tried = true
                        if (!kmBad && cost !is Parsed.Invalid && day != null) {
                            val recId = AppData.newId()
                            val kmValue = km.valueOrNull()
                            val costValue = cost.valueOrNull()
                            AppData.maintenanceRecords.upsert(MaintenanceRecord(recId, vehicle.id, recItemId, recName.trim(), kmValue, day.toEpochDay(), costValue))
                            // A despesa nasce uma vez, ligada ao registro (origem única).
                            if (costValue != null && costValue > 0) {
                                val maintCat = cats.firstOrNull { it.kind == CategoryKind.Expense && it.name.equals("Manutenção", true) }
                                AppData.transactions.upsert(
                                    Transaction(AppData.newId(), EntryKind.Expense, costValue, day.toEpochDay(), maintCat?.id, vehicleId = vehicle.id,
                                        note = recName.trim(), source = TxSource.Maintenance, sourceRef = recId),
                                )
                            }
                            if (kmValue != null) {
                                AppData.vehicles.upsert(vehicle.copy(odometerKm = maxOf(vehicle.odometerKm ?: 0, kmValue)))
                                recItemId?.let { id ->
                                    AppData.maintenanceItems.get(id)?.let { AppData.maintenanceItems.upsert(it.copy(lastDoneKm = kmValue, lastDoneEpochDay = day.toEpochDay())) }
                                }
                            }
                            recName = ""
                            recKm = ""
                            recCost = ""
                            tried = false
                        }
                    },
                ) { Text("Registrar") }
            }
        }
        val myRecords = records.filter { it.vehicleId == vehicle.id }.sortedByDescending { it.epochDay }
        if (myRecords.isNotEmpty()) SectionTitle("Histórico")
        myRecords.forEach { r ->
            SurfaceCard {
                Column(Modifier.padding(12.dp)) {
                    Text(r.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        DateText.formatEpochDay(r.epochDay) + (r.km?.let { " · $it km" } ?: "") + (r.costCents?.let { " · " + FinanceCalc.formatCents(it) } ?: " · sem custo informado"),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

/** Custo operacional (telas/21-custo): componentes informados por você somam o custo por km. Sem valores sugeridos. */
@Composable
fun CostScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val comps by AppData.costComponents.items.collectAsState()
    val settings by AppData.settings.items.collectAsState()
    val validated = settings.firstOrNull { it.id == "costValidated" }?.value == "true"
    var kind by rememberSaveable { mutableStateOf(CostKind.Fuel) }
    var name by rememberSaveable { mutableStateOf("") }
    var a by rememberSaveable { mutableStateOf("") }
    var b by rememberSaveable { mutableStateOf("") }
    var removing by rememberSaveable { mutableStateOf<String?>(null) }

    val result = CostModel.costPerKm(comps)

    ScreenScaffold("Custo operacional", onBack, onOpen) {
        InfoNote("O custo por km é a soma dos componentes que você informar. Nenhum valor é sugerido. Sem componentes completos, o custo é desconhecido (não é zero) e o resultado estimado fica indisponível.")
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Custo por km", fontWeight = FontWeight.SemiBold)
                when (result) {
                    is CostResult.Valid -> {
                        result.parts.forEach { (n, v) -> Text(n + ": R$ " + br.app.motorista.domain.calc.Display.decimal(v, 3) + "/km") }
                        Text("Total: R$ " + br.app.motorista.domain.calc.Display.decimal(result.perKm, 3) + "/km", fontWeight = FontWeight.Bold)
                    }
                    is CostResult.Incomplete -> {
                        Text("Indisponível", fontWeight = FontWeight.Bold)
                        result.reasons.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = validated && result is CostResult.Valid,
                        enabled = result is CostResult.Valid,
                        onCheckedChange = { AppData.putSetting("costValidated", it.toString()) },
                    )
                    Text("Confirmo este modelo de custo para calcular o resultado estimado das ofertas.", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        SectionTitle("Componentes")
        comps.forEach { c ->
            SurfaceCard {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(c.name + " · " + c.kind.label, fontWeight = FontWeight.SemiBold)
                        Text(
                            (c.a?.toPlainString() ?: "—") + (if (c.kind != CostKind.PerKm) " / " + (c.b?.toPlainString() ?: "—") else "") + " · " + c.kind.help,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { removing = c.id }) { Text("Remover") }
                }
            }
        }
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Novo componente", fontWeight = FontWeight.SemiBold)
                ChoiceChips(CostKind.entries, kind, { it.label }) { kind = it }
                Text(kind.help, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextInput("Nome", name, { name = it })
                val labelA = when (kind) { CostKind.Fuel -> "Preço por litro (R$)"; CostKind.PerKm -> "Custo por km (R$)"; CostKind.Monthly -> "Valor mensal (R$)" }
                TextInput(labelA, a, { a = it }, keyboard = androidx.compose.ui.text.input.KeyboardType.Decimal)
                if (kind != CostKind.PerKm) {
                    TextInput(if (kind == CostKind.Fuel) "Consumo (km por litro)" else "Km rodados por mês", b, { b = it }, keyboard = androidx.compose.ui.text.input.KeyboardType.Decimal)
                }
                val pa = OfferParsing.decimal(a)
                val pb = OfferParsing.decimal(b)
                val ok = name.isNotBlank() && pa is Parsed.Ok && (kind == CostKind.PerKm || (pb is Parsed.Ok && pb.value.signum() > 0))
                Button(
                    enabled = ok,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        AppData.costComponents.upsert(
                            CostComponent(AppData.newId(), name.trim(), kind, pa.valueOrNull()!!, pb.valueOrNull()?.takeIf { kind != CostKind.PerKm }),
                        )
                        // Mudou o modelo: a confirmação anterior deixa de valer.
                        AppData.removeSetting("costValidated")
                        name = ""
                        a = ""
                        b = ""
                    },
                ) { Text("Adicionar componente") }
            }
        }
        Text(
            "Não incluído: comissão ou taxa da plataforma (o valor da oferta já é o do motorista), depreciação e impostos, a menos que você os informe como componente.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    removing?.let { id ->
        ConfirmDialog("Remover componente?", "O custo por km será recalculado e a confirmação do modelo será desfeita.", "Remover", {
            AppData.costComponents.remove(id)
            AppData.removeSetting("costValidated")
            removing = null
        }, { removing = null })
    }
}
