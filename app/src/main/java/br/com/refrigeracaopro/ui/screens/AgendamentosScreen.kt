package br.com.refrigeracaopro.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.Agendamento
import br.com.refrigeracaopro.data.Periodicidade
import br.com.refrigeracaopro.data.StatusAgendamento
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.ConfirmarExclusao
import br.com.refrigeracaopro.ui.components.SeletorOpcoes
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.viewmodel.AgendamentosViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Agendamento de manutenção preventiva: lista futura/histórico, notificações
 * locais e geração de OS a partir do agendamento.
 */
@Composable
fun AgendamentosScreen(nav: NavController, vm: AgendamentosViewModel = viewModel()) {
    val agendamentos by vm.agendamentos.collectAsState()
    val clientes by vm.clientes.collectAsState()
    val equipamentos by vm.equipamentos.collectAsState()
    var editar by remember { mutableStateOf<Agendamento?>(null) }
    var novo by remember { mutableStateOf(false) }
    var excluir by remember { mutableStateOf<Agendamento?>(null) }
    val formato = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }
    val agora = System.currentTimeMillis()

    val futuros = agendamentos.filter { it.status == StatusAgendamento.AGENDADA }
    val historico = agendamentos.filter { it.status != StatusAgendamento.AGENDADA }

    TelaBase(nav, "Agendamentos", aoAdicionar = { novo = true }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                Text("Próximas manutenções (${futuros.size})",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            }
            items(futuros, key = { it.id }) { ag ->
                CardAgendamento(ag, clientes, equipamentos, formato, agora,
                    aoEditar = { editar = ag },
                    aoExcluir = { excluir = ag },
                    aoConcluir = {
                        val nomeCliente = clientes.find { it.id == ag.clienteId }?.nome ?: ""
                        vm.concluir(ag, nomeCliente)
                    },
                    aoGerarOS = { nav.navigate("ordens/form?id=0&agendamentoCliente=${ag.clienteId}&agendamentoEquip=${ag.equipamentoId ?: 0L}") }
                )
            }
            if (historico.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("Histórico (${historico.size})",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
                items(historico, key = { it.id }) { ag ->
                    CardAgendamento(ag, clientes, equipamentos, formato, agora,
                        aoEditar = { editar = ag }, aoExcluir = { excluir = ag },
                        aoConcluir = null, aoGerarOS = null)
                }
            }
        }
    }

    if (novo || editar != null) {
        DialogoAgendamento(
            agendamento = editar,
            clientes = clientes,
            equipamentos = equipamentos,
            aoSalvar = { ag, nomeCliente -> vm.salvar(ag, nomeCliente); novo = false; editar = null },
            aoFechar = { novo = false; editar = null }
        )
    }

    excluir?.let { ag ->
        ConfirmarExclusao("Excluir este agendamento?",
            aoConfirmar = { vm.excluir(ag); excluir = null },
            aoCancelar = { excluir = null })
    }
}

@Composable
private fun CardAgendamento(
    ag: Agendamento,
    clientes: List<br.com.refrigeracaopro.data.Cliente>,
    equipamentos: List<br.com.refrigeracaopro.data.Equipamento>,
    formato: SimpleDateFormat,
    agora: Long,
    aoEditar: () -> Unit,
    aoExcluir: () -> Unit,
    aoConcluir: (() -> Unit)?,
    aoGerarOS: (() -> Unit)?,
) {
    val nomeCliente = clientes.find { it.id == ag.clienteId }?.nome ?: "—"
    val equip = equipamentos.find { it.id == ag.equipamentoId }
    val atrasado = ag.status == StatusAgendamento.AGENDADA && ag.dataHora < agora

    Card(onClick = aoEditar, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("$nomeCliente — ${ag.tipoManutencao}", fontWeight = FontWeight.Bold)
                    Text(
                        listOfNotNull(formato.format(Date(ag.dataHora)), equip?.tipo, ag.periodicidade)
                            .filter { it.isNotBlank() }.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (atrasado) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = aoExcluir) {
                    Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error)
                }
            }
            AssistChip(onClick = {}, label = { Text(ag.status, style = MaterialTheme.typography.labelSmall) })
            if (aoConcluir != null || aoGerarOS != null) {
                Row {
                    aoConcluir?.let {
                        OutlinedButton(onClick = it, modifier = Modifier.padding(end = 4.dp)) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.height(16.dp))
                            Text(" Concluir", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    aoGerarOS?.let {
                        OutlinedButton(onClick = it) {
                            Icon(Icons.Default.Engineering, null, modifier = Modifier.height(16.dp))
                            Text(" Gerar OS", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogoAgendamento(
    agendamento: Agendamento?,
    clientes: List<br.com.refrigeracaopro.data.Cliente>,
    equipamentos: List<br.com.refrigeracaopro.data.Equipamento>,
    aoSalvar: (Agendamento, String) -> Unit,
    aoFechar: () -> Unit,
) {
    val context = LocalContext.current
    var clienteId by remember { mutableStateOf(agendamento?.clienteId ?: 0L) }
    var equipamentoId by remember { mutableStateOf(agendamento?.equipamentoId) }
    var tipo by remember { mutableStateOf(agendamento?.tipoManutencao ?: "Preventiva") }
    var periodicidade by remember { mutableStateOf(agendamento?.periodicidade ?: Periodicidade.UNICA) }
    var status by remember { mutableStateOf(agendamento?.status ?: StatusAgendamento.AGENDADA) }
    var observacoes by remember { mutableStateOf(agendamento?.observacoes ?: "") }
    val calendario = remember {
        Calendar.getInstance().apply {
            timeInMillis = agendamento?.dataHora ?: (System.currentTimeMillis() + 3600_000)
        }
    }
    var dataHora by remember { mutableStateOf(calendario.timeInMillis) }
    val formato = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }
    val equipsDoCliente = equipamentos.filter { it.clienteId == clienteId }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (agendamento == null) "Novo agendamento" else "Editar agendamento") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SeletorOpcoes("Cliente *", clientes.map { it.nome },
                    clientes.find { it.id == clienteId }?.nome ?: "",
                    aoSelecionar = { nome -> clienteId = clientes.find { it.nome == nome }?.id ?: 0L; equipamentoId = null })
                SeletorOpcoes("Equipamento", equipsDoCliente.map { "${it.tipo} ${it.marca}".trim() },
                    equipsDoCliente.find { it.id == equipamentoId }?.let { "${it.tipo} ${it.marca}".trim() } ?: "",
                    aoSelecionar = { texto -> equipamentoId = equipsDoCliente.find { e -> "${e.tipo} ${e.marca}".trim() == texto }?.id })
                CampoTexto(tipo, { tipo = it }, "Tipo de manutenção")
                OutlinedButton(
                    onClick = {
                        val c = Calendar.getInstance().apply { timeInMillis = dataHora }
                        DatePickerDialog(context, { _, ano, mes, dia ->
                            TimePickerDialog(context, { _, hora, min ->
                                c.set(ano, mes, dia, hora, min)
                                dataHora = c.timeInMillis
                            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) { Text("Data/Hora: ${formato.format(Date(dataHora))}") }
                SeletorOpcoes("Periodicidade", Periodicidade.TODAS, periodicidade, { periodicidade = it })
                SeletorOpcoes("Status", StatusAgendamento.TODOS, status, { status = it })
                CampoTexto(observacoes, { observacoes = it }, "Observações", linhas = 2)
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    if (clienteId <= 0) return@TextButton
                    val ag = (agendamento ?: Agendamento(clienteId = clienteId, dataHora = dataHora)).copy(
                        clienteId = clienteId, equipamentoId = equipamentoId, tipoManutencao = tipo,
                        dataHora = dataHora, periodicidade = periodicidade, status = status, observacoes = observacoes
                    )
                    aoSalvar(ag, clientes.find { it.id == clienteId }?.nome ?: "")
                },
                enabled = clienteId > 0
            ) { Text("Salvar") }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}
