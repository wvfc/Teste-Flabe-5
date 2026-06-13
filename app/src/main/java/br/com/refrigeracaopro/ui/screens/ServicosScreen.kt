package br.com.refrigeracaopro.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.Servico
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.ConfirmarExclusao
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.viewmodel.ServicosViewModel
import java.text.NumberFormat
import java.util.Locale

/** Catálogo de serviços (CRUD em diálogo). */
@Composable
fun ServicosScreen(nav: NavController, vm: ServicosViewModel = viewModel()) {
    val servicos by vm.servicos.collectAsState()
    var editar by remember { mutableStateOf<Servico?>(null) }
    var novo by remember { mutableStateOf(false) }
    var excluir by remember { mutableStateOf<Servico?>(null) }
    val moeda = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    TelaBase(nav, "Serviços", aoAdicionar = { novo = true }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            items(servicos, key = { it.id }) { servico ->
                Card(
                    onClick = { editar = servico },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(servico.nome, fontWeight = FontWeight.Bold)
                            Text(
                                listOf(
                                    moeda.format(servico.valorPadrao),
                                    servico.tempoEstimado.takeIf { it.isNotBlank() }?.let { "≈ $it" } ?: ""
                                ).filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { excluir = servico }) {
                            Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (novo || editar != null) {
        DialogoServico(
            servico = editar,
            aoSalvar = { vm.salvar(it); novo = false; editar = null },
            aoFechar = { novo = false; editar = null }
        )
    }

    excluir?.let { servico ->
        ConfirmarExclusao(
            "Excluir o serviço \"${servico.nome}\"?",
            aoConfirmar = { vm.excluir(servico); excluir = null },
            aoCancelar = { excluir = null }
        )
    }
}

@Composable
private fun DialogoServico(servico: Servico?, aoSalvar: (Servico) -> Unit, aoFechar: () -> Unit) {
    var nome by remember { mutableStateOf(servico?.nome ?: "") }
    var descricao by remember { mutableStateOf(servico?.descricao ?: "") }
    var valor by remember { mutableStateOf(servico?.valorPadrao?.takeIf { it > 0 }?.toString() ?: "") }
    var tempo by remember { mutableStateOf(servico?.tempoEstimado ?: "") }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (servico == null) "Novo serviço" else "Editar serviço") },
        text = {
            Column {
                CampoTexto(nome, { nome = it }, "Nome do serviço *")
                CampoTexto(descricao, { descricao = it }, "Descrição", linhas = 2)
                CampoTexto(valor, { valor = it }, "Valor padrão (R$)")
                CampoTexto(tempo, { tempo = it }, "Tempo estimado")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (nome.isBlank()) return@TextButton
                    aoSalvar(
                        (servico ?: Servico(nome = "")).copy(
                            nome = nome,
                            descricao = descricao,
                            valorPadrao = valor.replace(",", ".").toDoubleOrNull() ?: 0.0,
                            tempoEstimado = tempo,
                        )
                    )
                },
                enabled = nome.isNotBlank()
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}
