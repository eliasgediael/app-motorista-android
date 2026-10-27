package br.app.motorista.ui.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.app.motorista.copilot.CopilotController
import br.app.motorista.domain.journey.JourneyClock
import br.app.motorista.domain.journey.JourneyStatus
import br.app.motorista.ui.common.InfoNote
import br.app.motorista.ui.common.ScreenScaffold
import br.app.motorista.ui.common.SurfaceCard
import kotlinx.coroutines.delay

/** Jornada (telas/09-jornada): iniciar, pausar, retomar e encerrar (ação separada, com confirmação). */
@Composable
fun JourneyScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val state by CopilotController.state.collectAsState()
    val j = state.journey
    var confirmEnd by androidx.compose.runtime.remember { mutableStateOf(false) }
    val now by produceState(System.currentTimeMillis(), j.status) {
        while (j.status == JourneyStatus.Running || j.status == JourneyStatus.Paused) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }

    ScreenScaffold("Jornada", onBack, onOpen) {
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(j.status.label, style = MaterialTheme.typography.headlineSmall)
                Text("Em andamento: " + JourneyClock.format(JourneyClock.workedMs(j, now)))
                Text("Em pausa: " + JourneyClock.format(JourneyClock.pausedMs(j, now)) + " · pausas: " + j.pauseCount)
            }
        }
        when (j.status) {
            JourneyStatus.NotStarted, JourneyStatus.Ended -> Button(onClick = { CopilotController.journeyStart() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (j.status == JourneyStatus.Ended) "Iniciar nova jornada" else "Iniciar jornada")
            }
            JourneyStatus.Running -> OutlinedButton(onClick = { CopilotController.journeyPause() }, modifier = Modifier.fillMaxWidth()) { Text("Pausar") }
            JourneyStatus.Paused -> Button(onClick = { CopilotController.journeyResume() }, modifier = Modifier.fillMaxWidth()) { Text("Retomar") }
        }
        if (j.status == JourneyStatus.Running || j.status == JourneyStatus.Paused) {
            OutlinedButton(onClick = { confirmEnd = true }, modifier = Modifier.fillMaxWidth()) { Text("Encerrar jornada") }
        }
        val trips by br.app.motorista.data.AppData.trips.items.collectAsState()
        val pending = trips.count { it.status == br.app.motorista.domain.history.TripStatus.NotConfirmed && it.epochDay == java.time.LocalDate.now().toEpochDay() }
        SurfaceCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Confirmar viagens", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Text(if (pending == 0) "Nenhuma oferta de hoje aguardando confirmação." else "$pending oferta(s) de hoje ainda não confirmadas como realizadas.")
                OutlinedButton(onClick = { onOpen("historico") }, modifier = Modifier.fillMaxWidth()) { Text("Abrir histórico para confirmar") }
            }
        }
        InfoNote("Pausar e retomar não zeram o tempo. Encerrar é uma ação separada. Fases e relatórios (tela 10) ainda não existem.")
    }

    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text("Encerrar a jornada?") },
            text = { Text("O tempo em andamento e o de pausa ficam registrados e a jornada não poderá ser retomada.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmEnd = false
                    CopilotController.journeyEnd()
                }) { Text("Encerrar") }
            },
            dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("Cancelar") } },
        )
    }
}
