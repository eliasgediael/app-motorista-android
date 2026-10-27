package br.app.motorista.ui.assist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.app.motorista.BuildConfig
import br.app.motorista.copilot.CopilotController
import br.app.motorista.data.AppData
import br.app.motorista.domain.assist.AlertRules
import br.app.motorista.domain.assist.FlaggedTerm
import br.app.motorista.domain.assist.MessageConfig
import br.app.motorista.domain.assist.MessageTemplate
import br.app.motorista.domain.assist.PlaceKind
import br.app.motorista.domain.assist.PlaceMatcher
import br.app.motorista.domain.assist.VoiceItem
import br.app.motorista.domain.assist.VoiceText
import br.app.motorista.domain.calc.OfferParsing
import br.app.motorista.domain.calc.Parsed
import br.app.motorista.domain.calc.valueOrNull
import br.app.motorista.ui.common.ChoiceChips
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SectionTitle
import br.app.motorista.ui.common.SurfaceCard
import br.app.motorista.ui.common.TextInput
import br.app.motorista.ui.nav.ScreenCatalog
import br.app.motorista.ui.nav.ScreenStatus
import java.math.BigDecimal

private fun copyToClipboard(context: Context, label: String, text: String) {
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(label, text))
}

/** Mensagem preparada (telas/26-mensagem): monta o texto e COPIA por ação explícita. Nunca envia. */
@Composable
fun MessageScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val settings by AppData.settings.items.collectAsState()
    fun get(k: String) = settings.firstOrNull { it.id == k }?.value ?: ""
    val greetOn = get("msgGreetOn") == "true"
    val etaOn = get("msgEtaOn") == "true"
    var eta by rememberSaveable { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }
    val etaMin = OfferParsing.wholeNumber(eta)
    val cfg = MessageConfig(greetOn, get("msgGreeting"), get("msgBody"), etaOn, get("msgEtaTemplate"), etaMin.valueOrNull())
    val text = MessageTemplate.render(cfg)
    val problem = MessageTemplate.etaProblem(cfg)

    ScreenScaffold("Mensagem preparada", onBack, onOpen) {
        InfoNote("O app só monta e copia o texto. Você cola e envia no aplicativo que quiser; nenhuma mensagem é enviada automaticamente.")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Incluir saudação", Modifier.weight(1f))
            Switch(checked = greetOn, onCheckedChange = { AppData.putSetting("msgGreetOn", it.toString()) })
        }
        TextInput("Saudação", get("msgGreeting"), { AppData.putSetting("msgGreeting", it) })
        TextInput("Mensagem", get("msgBody"), { AppData.putSetting("msgBody", it) }, singleLine = false)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Incluir previsão de chegada (ETA)", Modifier.weight(1f))
            Switch(checked = etaOn, onCheckedChange = { AppData.putSetting("msgEtaOn", it.toString()) })
        }
        if (etaOn) {
            TextInput("Texto do ETA (use {eta})", get("msgEtaTemplate"), { AppData.putSetting("msgEtaTemplate", it) })
            TextInput("Minutos até o embarque", eta, { eta = it }, keyboard = KeyboardType.Number, error = if (etaMin is Parsed.Invalid) "Número inteiro" else null)
            problem?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        SectionTitle("Prévia")
        SurfaceCard {
            Text(text.ifBlank { "(vazia)" }, Modifier.padding(16.dp))
        }
        Button(
            enabled = text.isNotBlank() && problem == null,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                copyToClipboard(context, "Mensagem", text)
                copied = true
            },
        ) { Text(if (copied) "Copiado" else "Copiar mensagem") }
    }
}

/** Alertas da viagem (telas/27-alertas) e limites de tempo/distância (tela 03): só com limite SEU e dado na oferta. */
@Composable
fun AlertsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val settings by AppData.settings.items.collectAsState()
    var marketHint by rememberSaveable { mutableStateOf(false) }

    ScreenScaffold("Alertas da viagem", onBack, onOpen) {
        InfoNote("Um alerta só existe se você definir o limite e a oferta trouxer o dado. Sem limite, não há alerta. Alertas são avisos no balão e na voz, não mudam a cor nem recusam nada.")
        LimitCard("Tempo e distância até o embarque", listOf(
            Triple("alertPickupKm", "Distância até o embarque (km)", settings),
            Triple("alertPickupMin", "Tempo até o embarque (min)", settings),
        ))
        LimitCard("Tempo e distância da viagem", listOf(
            Triple("alertTripKm", "Distância da viagem (km)", settings),
            Triple("alertTripMin", "Tempo da viagem (min)", settings),
        ))
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Corrida com parada e passageiro iniciante", fontWeight = FontWeight.SemiBold)
                Text("Não implementado: exige fonte confirmada do dado e regra aprovada.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedButton(onClick = { onOpen("locais") }, modifier = Modifier.fillMaxWidth()) { Text("Locais sinalizados e mercados") }
    }
}

@Composable
private fun LimitCard(title: String, fields: List<Triple<String, String, List<br.app.motorista.domain.finance.Setting>>>) {
    SurfaceCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            fields.forEach { (key, label, settings) ->
                var text by rememberSaveable(key) { mutableStateOf(settings.firstOrNull { it.id == key }?.value ?: "") }
                val parsed = OfferParsing.decimal(text)
                TextInput(label, text, { text = it }, keyboard = KeyboardType.Decimal, error = if (parsed is Parsed.Invalid) "Número inválido" else null)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(enabled = parsed is Parsed.Ok, onClick = { AppData.putSetting(key, text.trim()) }) { Text("Salvar") }
                    OutlinedButton(onClick = {
                        AppData.removeSetting(key)
                        text = ""
                    }) { Text("Desligar") }
                    Text(
                        AppData.setting(key)?.let { "Ligado: acima de $it" } ?: "Desligado",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

fun currentAlertRules(): AlertRules {
    fun d(k: String) = AppData.setting(k)?.let { OfferParsing.decimal(it).valueOrNull() }
    return AlertRules(d("alertPickupKm"), d("alertPickupMin"), d("alertTripKm"), d("alertTripMin"))
}

/** Locais sinalizados (telas/28-locais): termos seus para avisar. Não prova segurança de lugar nenhum. */
@Composable
fun PlacesScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val terms by AppData.flaggedTerms.items.collectAsState()
    var text by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(PlaceKind.Pickup) }

    ScreenScaffold("Locais sinalizados", onBack, onOpen) {
        InfoNote("Termos que você quer notar no embarque ou destino (ex.: um nome de rua ou bairro). Um termo encontrado é só um motivo para olhar com atenção; não indica risco. Endereços digitados não são guardados.")
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChips(PlaceKind.entries, kind, { it.label }) { kind = it }
                TextInput("Termo", text, { text = it })
                Button(
                    enabled = text.isNotBlank() && terms.none { it.kind == kind && it.text.equals(text.trim(), true) },
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        AppData.flaggedTerms.upsert(FlaggedTerm(AppData.newId(), text.trim(), kind))
                        text = ""
                    },
                ) { Text("Adicionar") }
            }
        }
        val settings by AppData.settings.items.collectAsState()
        val marketOn = settings.firstOrNull { it.id == "marketAlert" }?.value == "true"
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Avisar sobre mercados", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Switch(checked = marketOn, onCheckedChange = { AppData.putSetting("marketAlert", it.toString()) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Avisar sobre locais sinalizados", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = settings.firstOrNull { it.id == "placesAlert" }?.value != "false",
                        onCheckedChange = { AppData.putSetting("placesAlert", it.toString()) },
                    )
                }
                Text(
                    "Avisa quando o embarque ou o destino digitado contém uma destas palavras. Edite a lista como quiser (separe por vírgula).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextInput(
                    "Palavras",
                    settings.firstOrNull { it.id == "marketWords" }?.value ?: PlaceMatcher.DEFAULT_MARKET_WORDS.joinToString(", "),
                    { AppData.putSetting("marketWords", it) },
                    singleLine = false,
                )
            }
        }
        SectionTitle("Seus termos")
        if (terms.isEmpty()) Text("Nenhum termo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        terms.forEach { t ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(t.text + " · " + t.kind.label, Modifier.weight(1f))
                TextButton(onClick = { AppData.flaggedTerms.remove(t.id) }) { Text("Remover") }
            }
        }
    }
}

/** Voz e notificações (telas/29-voz): o que falar e em que ordem. Cada oferta é falada uma vez e a fala pendente é substituída. */
@Composable
fun VoiceSettingsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val settings by AppData.settings.items.collectAsState()
    val chosen = VoiceText.parse(settings.firstOrNull { it.id == "voiceItems" }?.value).ifEmpty {
        if (settings.any { it.id == "voiceItems" }) emptyList() else DEFAULT_VOICE_ITEMS
    }
    fun save(list: List<VoiceItem>) = AppData.putSetting("voiceItems", VoiceText.serialize(list))
    val ordered = chosen + VoiceItem.entries.filter { it !in chosen }

    ScreenScaffold("Voz e notificações", onBack, onOpen) {
        InfoNote("Ligue ou desligue a voz no menu do botão flutuante ou no Painel. Aqui você escolhe o que é falado e a ordem. Sem itens escolhidos, nada é falado.")
        SurfaceCard {
            Column {
                ordered.forEachIndexed { index, item ->
                    val on = item in chosen
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = on, onCheckedChange = { checked -> save(if (checked) chosen + item else chosen - item) })
                        Text(item.label, Modifier.weight(1f))
                        if (on) {
                            val i = chosen.indexOf(item)
                            TextButton(enabled = i > 0, onClick = { save(chosen.toMutableList().also { it.add(i - 1, it.removeAt(i)) }) }) { Text("▲") }
                            TextButton(enabled = i < chosen.size - 1, onClick = { save(chosen.toMutableList().also { it.add(i + 1, it.removeAt(i)) }) }) { Text("▼") }
                        }
                    }
                }
            }
        }
        val preview = VoiceText.build(chosen, "99Pop", "manual", "2,00", "67,64", "1,13", "avaliação amarela", "24,80", "Embarque distante: acima do seu limite.")
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Prévia do aviso (dados de exemplo)", fontWeight = FontWeight.SemiBold)
                Text(preview.ifBlank { "(nada será falado)" }, style = MaterialTheme.typography.bodyMedium)
                var hint by remember { mutableStateOf<String?>(null) }
                OutlinedButton(
                    enabled = preview.isNotBlank(),
                    onClick = {
                        hint = if (CopilotController.state.value.voice.status == br.app.motorista.domain.copilot.FeatureStatus.Active) {
                            CopilotController.announce(preview)
                            null
                        } else {
                            "Ligue os avisos por voz no Painel ou no menu do botão flutuante."
                        }
                    },
                ) { Text("Falar prévia") }
                hint?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Como funciona", fontWeight = FontWeight.SemiBold)
                Text("• Cada oferta é falada no máximo uma vez.", style = MaterialTheme.typography.bodySmall)
                Text("• Uma nova oferta interrompe a fala anterior ainda pendente.", style = MaterialTheme.typography.bodySmall)
                Text("• Oferta já expirada não é falada.", style = MaterialTheme.typography.bodySmall)
                Text("• Desligar a voz cancela o que estiver pendente.", style = MaterialTheme.typography.bodySmall)
                Text("Notificações de análise: não implementadas (não há fonte real de ofertas).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Itens falados enquanto o usuário não escolher: os curtos. Reais por hora, valor e motivo ficam a critério dele. */
private val DEFAULT_VOICE_ITEMS = listOf(VoiceItem.Origin, VoiceItem.PerKm, VoiceItem.PerHour, VoiceItem.Assessment)

fun currentVoiceItems(): List<VoiceItem> {
    val raw = AppData.setting("voiceItems") ?: return DEFAULT_VOICE_ITEMS
    return VoiceText.parse(raw)
}

private enum class NavApp(val label: String) { Ask("Perguntar sempre"), Maps("Google Maps"), Waze("Waze") }

/** Navegação (telas/30-navegacao): abre o endereço que você digitou no aplicativo que escolher. Só por ação sua. */
@Composable
fun NavigationScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val settings by AppData.settings.items.collectAsState()
    val app = NavApp.entries.firstOrNull { it.name == settings.firstOrNull { s -> s.id == "navApp" }?.value }
    var address by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    ScreenScaffold("Navegação", onBack, onOpen) {
        InfoNote("Escolha o aplicativo de mapas e digite o endereço conferido por você. O app só abre o mapa quando você toca em Abrir. Ver a rua no mapa não comprova segurança de rua nem de passageiro. Endereços não são guardados.")
        Text("Aplicativo preferido", fontWeight = FontWeight.SemiBold)
        ChoiceChips(NavApp.entries, app, { it.label }) { AppData.putSetting("navApp", it.name) }
        TextInput("Endereço", address, { address = it })
        Button(
            enabled = app != null && address.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val q = Uri.encode(address.trim())
                val intent = when (app) {
                    NavApp.Maps -> Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$q")).setPackage("com.google.android.apps.maps")
                    NavApp.Waze -> Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?q=$q&navigate=yes")).setPackage("com.waze")
                    NavApp.Ask -> Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$q")), "Abrir endereço com")
                    null -> null
                }
                message = if (intent == null) {
                    "Escolha um aplicativo."
                } else if (app != NavApp.Ask && intent.resolveActivity(context.packageManager) == null) {
                    "${app?.label} não está instalado neste aparelho."
                } else {
                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    null
                }
            },
        ) { Text("Abrir no " + (app?.label ?: "mapa")) }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

/** Ajuda e primeiros passos (telas/40-ajuda): como usar e o que é real, simulação ou ainda inexistente. */
@Composable
fun HelpScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    ScreenScaffold("Ajuda e primeiros passos", onBack, onOpen) {
        SectionTitle("Primeiros passos")
        listOf(
            "1. Cadastre seus critérios (Critérios de análise): você digita o mínimo, o patamar bom e a base. Nada vem preenchido.",
            "2. Escolha como combinar os critérios (Combinar critérios). Sem isso, não há cor geral.",
            "3. Teste no Simulador de oferta: digite uma oferta e veja os cálculos e o balão.",
            "4. Ative o copiloto (Controle flutuante) para ter o botão, o menu e o balão sobre outros apps.",
            "5. Registre ganhos e despesas em Finanças; confirme as viagens realizadas no Histórico.",
        ).forEach { Text(it, Modifier.padding(vertical = 2.dp)) }

        SectionTitle("Perguntas frequentes")
        listOf(
            "Como funciona o balão?" to "Ele mostra os números da oferta (R$/km, R$/h, R$/min), calculados com a base que você escolheu, e um selo colorido: verde, amarelo ou vermelho pelas SUAS faixas. Sem critérios, combinação definida e dados completos, não há cor. O balão fica separado do botão e some quando a oferta expira ou muda.",
            "Como funciona o botão flutuante?" to "Botão verde fixo com o símbolo. Toque para abrir os controles (análise, gravação, voz, jornada); arraste para mover. A cor dele nunca muda com a avaliação. Precisa da permissão de sobreposição que você concede nas configurações.",
            "Quando aparece Recusar?" to "O selo RECUSAR aparece quando as suas regras põem a oferta abaixo do mínimo, com a sua combinação já escolhida. É só um aviso: o app NUNCA recusa corrida nem toca em outro app. Sem regras, aparece sem cor geral.",
            "Permissões e compatibilidade" to "Sobreposição (botão), câmera (gravação visível, sem áudio), notificações (aviso dos serviços) e acessibilidade (leitor de ofertas, só se você ligar). Android mínimo 8.0. Veja o estado real em Permissões.",
        ).forEach { (q, a) ->
            var open by androidx.compose.runtime.saveable.rememberSaveable(q) { androidx.compose.runtime.mutableStateOf(false) }
            SurfaceCard(onClick = { open = !open }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(q, fontWeight = FontWeight.SemiBold)
                    if (open) Text(a, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        SectionTitle("O que é real e o que não é")
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Real: cálculos, critérios, finanças, histórico, jornada, voz, gravação visível, botão flutuante, backup.")
                Text("Simulação: ofertas vêm da entrada manual; balões de exemplo são só diagnóstico.")
                Text("Não existe: leitura automática de ofertas do 99Pop ou da Uber, captura de tela, sincronização, aceite ou recusa de corridas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        SectionTitle("Estado das funções")
        val ready = ScreenCatalog.all.count { ScreenCatalog.status(it.id) == ScreenStatus.Ready }
        val blocked = ScreenCatalog.all.filter { ScreenCatalog.status(it.id) == ScreenStatus.Blocked }
        Text("$ready de ${ScreenCatalog.all.size} telas implementadas; " + blocked.size + " bloqueada(s).")
        blocked.forEach { Text("• " + it.title + ": " + (ScreenCatalog.blockedReason(it.id) ?: ""), style = MaterialTheme.typography.bodySmall) }
        OutlinedButton(onClick = { onOpen("ferramentas") }, modifier = Modifier.fillMaxWidth()) { Text("Ver todas as ferramentas") }
        OutlinedButton(onClick = { onOpen("permissoes") }, modifier = Modifier.fillMaxWidth()) { Text("Ver permissões") }
        Text("Versão " + BuildConfig.VERSION_NAME + " · " + BuildConfig.GIT_SHA, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Texto do aplicativo de outra tela precisa do veículo; aqui só auxiliar de compra de preço. */
internal fun parseDecimalOrNull(s: String): BigDecimal? = OfferParsing.decimal(s).valueOrNull()

/** Avisos de mercado atuais: vazio se o aviso está desligado. */
fun marketMatchesNow(pickup: String, dropoff: String): List<String> =
    if (AppData.setting("marketAlert") == "true") PlaceMatcher.marketMatches(pickup, dropoff, PlaceMatcher.parseWords(AppData.setting("marketWords"))) else emptyList()
