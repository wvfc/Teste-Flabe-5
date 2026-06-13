package br.com.refrigeracaopro.ui.screens

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import br.com.refrigeracaopro.data.Equipamento
import br.com.refrigeracaopro.data.PTTable
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.ConfirmarExclusao
import br.com.refrigeracaopro.ui.components.SecaoFotos
import br.com.refrigeracaopro.ui.components.SeletorOpcoes
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.util.Arquivos
import br.com.refrigeracaopro.viewmodel.EquipamentosViewModel

/** Lista de equipamentos cadastrados. */
@Composable
fun EquipamentosScreen(nav: NavController, vm: EquipamentosViewModel = viewModel()) {
    val equipamentos by vm.equipamentos.collectAsState()
    val clientes by vm.clientes.collectAsState()
    var excluir by remember { mutableStateOf<Equipamento?>(null) }

    TelaBase(nav, "Equipamentos", aoAdicionar = { nav.navigate("equipamentos/form?id=0") }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            items(equipamentos, key = { it.id }) { equip ->
                val nomeCliente = clientes.find { it.id == equip.clienteId }?.nome ?: ""
                Card(
                    onClick = { nav.navigate("equipamentos/form?id=${equip.id}") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                listOf(equip.tipo, equip.marca).filter { it.isNotBlank() }.joinToString(" — "),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                listOf(nomeCliente, equip.modelo, equip.fluido).filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { excluir = equip }) {
                            Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    excluir?.let { equip ->
        ConfirmarExclusao(
            "Excluir o equipamento \"${equip.tipo}\"?",
            aoConfirmar = { vm.excluir(equip); excluir = null },
            aoCancelar = { excluir = null }
        )
    }
}

/** Formulário de equipamento, com cliente vinculado, fluido e fotos. */
@Composable
fun EquipamentoFormScreen(
    nav: NavController,
    equipamentoId: Long,
    clientePreSelecionado: Long = 0L,
    vm: EquipamentosViewModel = viewModel(),
) {
    val clientes by vm.clientes.collectAsState()

    var original by remember { mutableStateOf<Equipamento?>(null) }
    var clienteId by remember { mutableStateOf(clientePreSelecionado) }
    var tipo by remember { mutableStateOf(EquipamentosViewModel.TIPOS.first()) }
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var numeroSerie by remember { mutableStateOf("") }
    var fluido by remember { mutableStateOf("") }
    var tensao by remember { mutableStateOf("") }
    var potencia by remember { mutableStateOf("") }
    var local by remember { mutableStateOf("") }
    var dataInstalacao by remember { mutableStateOf("") }
    var observacoes by remember { mutableStateOf("") }
    var fotos by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(equipamentoId) {
        if (equipamentoId > 0) vm.buscar(equipamentoId)?.let { e ->
            original = e
            clienteId = e.clienteId; tipo = e.tipo; marca = e.marca; modelo = e.modelo
            numeroSerie = e.numeroSerie; fluido = e.fluido; tensao = e.tensao; potencia = e.potencia
            local = e.localInstalacao; dataInstalacao = e.dataInstalacao; observacoes = e.observacoes
            fotos = Arquivos.textoParaLista(e.fotos)
        }
    }

    TelaBase(nav, if (equipamentoId > 0) "Editar equipamento" else "Novo equipamento") { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SeletorOpcoes(
                "Cliente vinculado *",
                clientes.map { it.nome },
                clientes.find { it.id == clienteId }?.nome ?: "",
                aoSelecionar = { nome -> clienteId = clientes.find { it.nome == nome }?.id ?: 0L }
            )
            SeletorOpcoes("Tipo de equipamento", EquipamentosViewModel.TIPOS, tipo, { tipo = it })
            Row {
                CampoTexto(marca, { marca = it }, "Marca", modifier = Modifier.weight(1f).padding(end = 4.dp))
                CampoTexto(modelo, { modelo = it }, "Modelo", modifier = Modifier.weight(1f).padding(start = 4.dp))
            }
            CampoTexto(numeroSerie, { numeroSerie = it }, "Número de série")
            SeletorOpcoes("Fluido refrigerante", PTTable.NOMES + "Outro", fluido, { fluido = it })
            Row {
                CampoTexto(tensao, { tensao = it }, "Tensão (V)", modifier = Modifier.weight(1f).padding(end = 4.dp))
                CampoTexto(potencia, { potencia = it }, "Potência", modifier = Modifier.weight(1f).padding(start = 4.dp))
            }
            CampoTexto(local, { local = it }, "Local de instalação")
            CampoTexto(dataInstalacao, { dataInstalacao = it }, "Data de instalação (dd/mm/aaaa)")
            CampoTexto(observacoes, { observacoes = it }, "Observações", linhas = 3)

            Spacer(Modifier.height(8.dp))
            SecaoFotos("Fotos do equipamento", fotos) { fotos = it }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val equip = (original ?: Equipamento(clienteId = clienteId, tipo = tipo)).copy(
                        clienteId = clienteId, tipo = tipo, marca = marca, modelo = modelo,
                        numeroSerie = numeroSerie, fluido = fluido, tensao = tensao, potencia = potencia,
                        localInstalacao = local, dataInstalacao = dataInstalacao, observacoes = observacoes,
                        fotos = Arquivos.listaParaTexto(fotos),
                    )
                    vm.salvar(equip) { nav.popBackStack() }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = clienteId > 0
            ) { Text("Salvar equipamento") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
