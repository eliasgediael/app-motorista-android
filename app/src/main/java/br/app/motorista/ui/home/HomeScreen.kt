package br.app.motorista.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.app.motorista.BuildConfig
import br.app.motorista.floating.FloatingControlService
import br.app.motorista.ui.common.BottomBar
import br.app.motorista.ui.common.IconBadge
import br.app.motorista.ui.common.SurfaceCard
import br.app.motorista.ui.nav.BottomTab
import br.app.motorista.ui.theme.FunctionalColors

/**
 * Painel (telas/01-painel). Copiloto, gravação, voz e jornada vêm do estado real compartilhado
 * ([CopilotCards]); análise de corridas aparece como não implementada, sem interruptor.
 */
@Composable
fun HomeScreen(onOpen: (String) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HomeTopBar(onTools = { onOpen("ferramentas") }) },
        bottomBar = { BottomBar(selected = BottomTab.Inicio, onOpen = onOpen) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AccessCard(onClick = { onOpen("conta") })

            CopilotCards(onOpen)
            RangesCard(onConfigure = { onOpen("criterios") })

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Ferramentas rápidas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Ver todos",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(role = Role.Button) { onOpen("ferramentas") }.padding(8.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickTool(Icons.Outlined.CreditCard, "Estilo do cartão", Modifier.weight(1f)) { onOpen("cartao") }
                QuickTool(Icons.Outlined.LocalGasStation, "Custo operacional", Modifier.weight(1f)) { onOpen("custo") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickTool(Icons.Outlined.BarChart, "Simulador de oferta", Modifier.weight(1f)) { onOpen("simulador") }
                QuickTool(Icons.Outlined.Notifications, "Alertas", Modifier.weight(1f)) { onOpen("alertas") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickTool(Icons.AutoMirrored.Outlined.Message, "Mensagem preparada", Modifier.weight(1f)) { onOpen("mensagem") }
                QuickTool(Icons.Outlined.GridView, "Corrida de teste", Modifier.weight(1f)) { onOpen("corrida-teste") }
            }
            Text(
                "Versão " + BuildConfig.VERSION_NAME + " · " + BuildConfig.GIT_SHA + " · build de teste",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun HomeTopBar(onTools: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp, end = 8.dp, top = 48.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Painel", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        IconButton(onClick = onTools) {
            Icon(Icons.Outlined.GridView, contentDescription = "Abrir ferramentas", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun AccessCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.VerifiedUser, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text("Seu acesso", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                // Acesso comercial indefinido (tela 36): texto neutro, sem plano ou preço.
                Text("Perfil local · acesso comercial a definir", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
        }
    }
}

@Composable
private fun FeatureToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onSettings: () -> Unit,
) {
    SurfaceCard {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Configurar $title", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun ShiftCard(onClick: () -> Unit) {
    SurfaceCard(onClick = onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Outlined.Schedule)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text("Jornada", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Text("Acompanhe seu tempo de trabalho", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "Abrir",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RangesCard(onConfigure: () -> Unit) {
    SurfaceCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Suas faixas", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    "Configurar critérios ›",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(role = Role.Button, onClick = onConfigure).padding(8.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Legend("Ruim", FunctionalColors.Bad)
                Legend("Média", FunctionalColors.Attention)
                Legend("Boa", FunctionalColors.Good)
            }
            // Ausente é desconhecido: nenhum limite foi cadastrado nem presumido.
            Text("Nenhum critério configurado. Nenhuma avaliação é emitida.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(label, Modifier.padding(start = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QuickTool(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    SurfaceCard(modifier = modifier, onClick = onClick) {
        Row(Modifier.height(72.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(label, Modifier.padding(start = 12.dp), fontWeight = FontWeight.SemiBold)
        }
    }
}
