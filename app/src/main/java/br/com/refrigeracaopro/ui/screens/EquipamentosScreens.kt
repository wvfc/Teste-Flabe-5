package br.com.refrigeracaopro.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
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
import br.com.refrigeracaopro.ui.components.TituloSecao
import br.com.refrigeracaopro.util.Arquivos
import br.com.refrigeracaopro.viewmodel.EquipamentosViewModel
import java.io.File

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

/**
 * Formulário de equipamento. Para o tipo "Compressor" exibe um modelo de
 * cadastro específico (sem fluido refrigerante, com pressão/tensão nominais,
 * potência em kW, datas, tipo de partida, anexo de PDF e fotos por categoria).
 */
@Composable
fun EquipamentoFormScreen(
    nav: NavController,
    equipamentoId: Long,
    clientePreSelecionado: Long = 0L,
    vm: EquipamentosViewModel = viewModel(),
) {
    val context = LocalContext.current
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
    // Específicos de compressor
    var pressaoNominal by remember { mutableStateOf("") }
    var dataFabricacao by remember { mutableStateOf("") }
    var anoFabricacao by remember { mutableStateOf("") }
    var tipoPartida by remember { mutableStateOf("") }
    var manuais by remember { mutableStateOf(listOf<String>()) }
    var fotosPlaqueta by remember { mutableStateOf(listOf<String>()) }
    var fotosPlaquetaMotor by remember { mutableStateOf(listOf<String>()) }
    var fotosMaquina by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(equipamentoId) {
        if (equipamentoId > 0) vm.buscar(equipamentoId)?.let { e ->
            original = e
            clienteId = e.clienteId; tipo = e.tipo; marca = e.marca; modelo = e.modelo
            numeroSerie = e.numeroSerie; fluido = e.fluido; tensao = e.tensao; potencia = e.potencia
            local = e.localInstalacao; dataInstalacao = e.dataInstalacao; observacoes = e.observacoes
            fotos = Arquivos.textoParaLista(e.fotos)
            pressaoNominal = e.pressaoNominal; dataFabricacao = e.dataFabricacao
            anoFabricacao = e.anoFabricacao; tipoPartida = e.tipoPartida
            manuais = Arquivos.textoParaLista(e.manuais)
            fotosPlaqueta = Arquivos.textoParaLista(e.fotosPlaqueta)
            fotosPlaquetaMotor = Arquivos.textoParaLista(e.fotosPlaquetaMotor)
            fotosMaquina = Arquivos.textoParaLista(e.fotosMaquina)
        }
    }

    val ehCompressor = tipo == "Compressor"

    // Anexar PDF (manual / documento do compressor)
    val anexarPdf = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) Arquivos.copiarParaManuais(context, uri, "$marca-$modelo")?.let { manuais = manuais + it }
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

            if (ehCompressor) {
                // ---- Modelo específico de COMPRESSOR ----
                Row {
                    CampoTexto(tensao, { tensao = it }, "Tensão nominal (V)", modifier = Modifier.weight(1f).padding(end = 4.dp))
                    CampoTexto(potencia, { potencia = it }, "Potência (kW)", modifier = Modifier.weight(1f).padding(start = 4.dp))
                }
                CampoTexto(pressaoNominal, { pressaoNominal = it }, "Pressão nominal")
                SeletorOpcoes(
                    "Tipo de partida",
                    listOf("Estrela-triângulo", "Inversor", "Soft start", "Partida direta"),
                    tipoPartida, { tipoPartida = it }
                )
                Row {
                    CampoTexto(dataFabricacao, { dataFabricacao = it }, "Data de fabricação", modifier = Modifier.weight(1f).padding(end = 4.dp))
                    CampoTexto(anoFabricacao, { anoFabricacao = it }, "Ano de fabricação", modifier = Modifier.weight(1f).padding(start = 4.dp))
                }
                CampoTexto(local, { local = it }, "Local de instalação")
                CampoTexto(observacoes, { observacoes = it }, "Observações", linhas = 3)

                TituloSecao("Documento (PDF)")
                manuais.forEach { caminho ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AttachFile, null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Text(File(caminho).name.substringAfter("-"), Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { manuais = manuais - caminho }) {
                                Icon(Icons.Default.Delete, "Remover", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { anexarPdf.launch("application/pdf") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Anexar PDF")
                }

                TituloSecao("Fotos do compressor")
                SecaoFotos("Foto da plaqueta", fotosPlaqueta) { fotosPlaqueta = it }
                SecaoFotos("Foto da plaqueta do motor", fotosPlaquetaMotor) { fotosPlaquetaMotor = it }
                SecaoFotos("Foto da máquina", fotosMaquina) { fotosMaquina = it }
                SecaoFotos("Diversas", fotos) { fotos = it }
            } else {
                // ---- Modelo padrão (demais equipamentos) ----
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
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val equip = (original ?: Equipamento(clienteId = clienteId, tipo = tipo)).copy(
                        clienteId = clienteId, tipo = tipo, marca = marca, modelo = modelo,
                        numeroSerie = numeroSerie, fluido = if (ehCompressor) "" else fluido,
                        tensao = tensao, potencia = potencia,
                        localInstalacao = local, dataInstalacao = dataInstalacao, observacoes = observacoes,
                        fotos = Arquivos.listaParaTexto(fotos),
                        manuais = Arquivos.listaParaTexto(manuais),
                        pressaoNominal = pressaoNominal, dataFabricacao = dataFabricacao,
                        anoFabricacao = anoFabricacao, tipoPartida = tipoPartida,
                        fotosPlaqueta = Arquivos.listaParaTexto(fotosPlaqueta),
                        fotosPlaquetaMotor = Arquivos.listaParaTexto(fotosPlaquetaMotor),
                        fotosMaquina = Arquivos.listaParaTexto(fotosMaquina),
                    )
                    vm.salvar(equip) {
                        Toast.makeText(context, "Equipamento salvo.", Toast.LENGTH_SHORT).show()
                        nav.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = clienteId > 0
            ) { Text("Salvar equipamento") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
