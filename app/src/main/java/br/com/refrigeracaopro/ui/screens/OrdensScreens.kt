package br.com.refrigeracaopro.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.OrdemServico
import br.com.refrigeracaopro.data.StatusOS
import br.com.refrigeracaopro.pdf.PdfGenerator
import br.com.refrigeracaopro.ui.components.AssinaturaDialog
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.ConfirmarExclusao
import br.com.refrigeracaopro.ui.components.LinhaAssinatura
import br.com.refrigeracaopro.ui.components.SecaoFotos
import br.com.refrigeracaopro.ui.components.SeletorOpcoes
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.components.TituloSecao
import br.com.refrigeracaopro.util.Arquivos
import br.com.refrigeracaopro.viewmodel.OrdensViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Lista de Ordens de Serviço, com filtros por status e busca textual. */
@Composable
fun OrdensServicoScreen(nav: NavController, vm: OrdensViewModel = viewModel()) {
    val ordens by vm.ordens.collectAsState()
    val clientes by vm.clientes.collectAsState()
    var busca by remember { mutableStateOf("") }
    var filtroStatus by remember { mutableStateOf("Todas") }
    var excluir by remember { mutableStateOf<OrdemServico?>(null) }
    val formatoData = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }
    val moeda = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    val filtradas = ordens.filter { os ->
        val nomeCliente = clientes.find { it.id == os.clienteId }?.nome ?: ""
        (filtroStatus == "Todas" || os.status == filtroStatus) &&
            (busca.isBlank() || os.numero.contains(busca, true) || nomeCliente.contains(busca, true))
    }

    TelaBase(nav, "Ordens de Serviço", aoAdicionar = { nav.navigate("ordens/form?id=0") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = busca,
                onValueChange = { busca = it },
                label = { Text("Pesquisar por OS ou cliente") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
            ) {
                items(listOf("Todas") + StatusOS.TODOS) { status ->
                    FilterChip(
                        selected = filtroStatus == status,
                        onClick = { filtroStatus = status },
                        label = { Text(status) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(filtradas, key = { it.id }) { os ->
                    val nomeCliente = clientes.find { it.id == os.clienteId }?.nome ?: "—"
                    Card(
                        onClick = { nav.navigate("ordens/form?id=${os.id}") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${os.numero} • $nomeCliente", fontWeight = FontWeight.Bold)
                                Text(
                                    "${formatoData.format(Date(os.dataHora))} • ${moeda.format(os.valorTotal)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                AssistChip(onClick = {}, label = { Text(os.status, style = MaterialTheme.typography.labelSmall) })
                            }
                            IconButton(onClick = { excluir = os }) {
                                Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    excluir?.let { os ->
        ConfirmarExclusao(
            "Excluir a ${os.numero}?",
            aoConfirmar = { vm.excluir(os); excluir = null },
            aoCancelar = { excluir = null }
        )
    }
}

/** Formulário de Ordem de Serviço com geração de PDF, compartilhamento e assinatura. */
@Composable
fun OrdemServicoFormScreen(
    nav: NavController,
    osId: Long,
    clientePreSelecionado: Long = 0L,
    equipamentoPreSelecionado: Long = 0L,
    vm: OrdensViewModel = viewModel(),
) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    val clientes by vm.clientes.collectAsState()
    val equipamentos by vm.equipamentos.collectAsState()
    val servicos by vm.servicos.collectAsState()

    var original by remember { mutableStateOf<OrdemServico?>(null) }
    var numero by remember { mutableStateOf("") }
    var clienteId by remember { mutableStateOf(clientePreSelecionado) }
    var equipamentoId by remember { mutableStateOf(equipamentoPreSelecionado.takeIf { it > 0 }) }
    var tecnico by remember { mutableStateOf("") }
    var defeito by remember { mutableStateOf("") }
    var diagnostico by remember { mutableStateOf("") }
    var servicosExec by remember { mutableStateOf("") }
    var pecas by remember { mutableStateOf("") }
    var valorMo by remember { mutableStateOf("") }
    var valorPecas by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(StatusOS.ABERTA) }
    var assinaturaCliente by remember { mutableStateOf("") }
    var fotosAntes by remember { mutableStateOf(listOf<String>()) }
    var fotosDurante by remember { mutableStateOf(listOf<String>()) }
    var fotosDepois by remember { mutableStateOf(listOf<String>()) }
    var mostrarAssinatura by remember { mutableStateOf(false) }

    LaunchedEffect(osId) {
        if (osId > 0) vm.buscar(osId)?.let { os ->
            original = os
            numero = os.numero; clienteId = os.clienteId; equipamentoId = os.equipamentoId
            tecnico = os.tecnico; defeito = os.defeitoInformado; diagnostico = os.diagnostico
            servicosExec = os.servicosExecutados; pecas = os.pecasUtilizadas
            valorMo = os.valorMaoDeObra.takeIf { it > 0 }?.toString() ?: ""
            valorPecas = os.valorPecas.takeIf { it > 0 }?.toString() ?: ""
            status = os.status; assinaturaCliente = os.assinaturaCliente
            fotosAntes = Arquivos.textoParaLista(os.fotosAntes)
            fotosDurante = Arquivos.textoParaLista(os.fotosDurante)
            fotosDepois = Arquivos.textoParaLista(os.fotosDepois)
        } else {
            numero = vm.proximoNumero()
            // Preenche o técnico padrão a partir das configurações
            tecnico = br.com.refrigeracaopro.data.Prefs.run { context.nomeTecnico }
        }
    }

    fun montar(): OrdemServico = (original ?: OrdemServico(numero = numero, clienteId = clienteId)).copy(
        numero = numero, clienteId = clienteId, equipamentoId = equipamentoId, tecnico = tecnico,
        defeitoInformado = defeito, diagnostico = diagnostico, servicosExecutados = servicosExec,
        pecasUtilizadas = pecas,
        valorMaoDeObra = valorMo.replace(",", ".").toDoubleOrNull() ?: 0.0,
        valorPecas = valorPecas.replace(",", ".").toDoubleOrNull() ?: 0.0,
        status = status, assinaturaCliente = assinaturaCliente,
        fotosAntes = Arquivos.listaParaTexto(fotosAntes),
        fotosDurante = Arquivos.listaParaTexto(fotosDurante),
        fotosDepois = Arquivos.listaParaTexto(fotosDepois),
    )

    val equipsDoCliente = equipamentos.filter { it.clienteId == clienteId }

    TelaBase(nav, if (osId > 0) numero else "Nova OS") { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            Text(numero, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary)

            SeletorOpcoes(
                "Cliente *", clientes.map { it.nome },
                clientes.find { it.id == clienteId }?.nome ?: "",
                aoSelecionar = { nome -> clienteId = clientes.find { it.nome == nome }?.id ?: 0L; equipamentoId = null }
            )
            SeletorOpcoes(
                "Equipamento", equipsDoCliente.map { "${it.tipo} ${it.marca}".trim() },
                equipsDoCliente.find { it.id == equipamentoId }?.let { "${it.tipo} ${it.marca}".trim() } ?: "",
                aoSelecionar = { texto -> equipamentoId = equipsDoCliente.find { e -> "${e.tipo} ${e.marca}".trim() == texto }?.id }
            )
            CampoTexto(tecnico, { tecnico = it }, "Técnico responsável")
            SeletorOpcoes("Status", StatusOS.TODOS, status, { status = it })

            TituloSecao("Atendimento")
            CampoTexto(defeito, { defeito = it }, "Defeito informado pelo cliente", linhas = 2)
            CampoTexto(diagnostico, { diagnostico = it }, "Diagnóstico técnico", linhas = 3)

            if (servicos.isNotEmpty()) {
                Text("Toque para adicionar do catálogo:", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                ) {
                    servicos.take(12).forEach { s ->
                        AssistChip(
                            onClick = {
                                servicosExec = (servicosExec + (if (servicosExec.isBlank()) "" else "\n") + "• ${s.nome}")
                                val mo = valorMo.replace(",", ".").toDoubleOrNull() ?: 0.0
                                if (s.valorPadrao > 0) valorMo = (mo + s.valorPadrao).toString()
                            },
                            label = { Text(s.nome, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
            CampoTexto(servicosExec, { servicosExec = it }, "Serviços executados", linhas = 3)
            CampoTexto(pecas, { pecas = it }, "Peças utilizadas", linhas = 2)

            Row {
                CampoTexto(valorMo, { valorMo = it }, "Mão de obra (R$)", modifier = Modifier.weight(1f).padding(end = 4.dp),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
                CampoTexto(valorPecas, { valorPecas = it }, "Peças (R$)", modifier = Modifier.weight(1f).padding(start = 4.dp),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
            }

            TituloSecao("Fotos")
            SecaoFotos("Antes", fotosAntes) { fotosAntes = it }
            SecaoFotos("Durante", fotosDurante) { fotosDurante = it }
            SecaoFotos("Depois", fotosDepois) { fotosDepois = it }

            TituloSecao("Assinatura do cliente")
            LinhaAssinatura("Cliente", assinaturaCliente, aoAssinar = { mostrarAssinatura = true }, aoLimpar = { assinaturaCliente = "" })

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { vm.salvar(montar()) { nav.popBackStack() } },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = clienteId > 0
            ) { Text("Salvar OS") }

            Spacer(Modifier.height(8.dp))
            Row {
                OutlinedButton(
                    onClick = {
                        escopo.launch {
                            val os = montar()
                            vm.salvar(os) {}
                            val cliente = vm.buscarCliente(clienteId)
                            val equip = equipamentoId?.let { vm.buscarEquipamento(it) }
                            val pdf = PdfGenerator.gerarOrdemServico(context, os, cliente, equip)
                            Arquivos.abrirPdf(context, pdf)
                        }
                    },
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                    enabled = clienteId > 0
                ) {
                    Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp)); Text("Gerar PDF")
                }
                OutlinedButton(
                    onClick = {
                        escopo.launch {
                            val os = montar()
                            val cliente = vm.buscarCliente(clienteId)
                            val equip = equipamentoId?.let { vm.buscarEquipamento(it) }
                            val pdf = PdfGenerator.gerarOrdemServico(context, os, cliente, equip)
                            Arquivos.compartilhar(context, pdf, "application/pdf", os.numero)
                        }
                    },
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    enabled = clienteId > 0
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp)); Text("Compartilhar")
                }
            }
            OutlinedButton(
                onClick = {
                    escopo.launch {
                        vm.salvar(montar()) {
                            // Cria um relatório técnico a partir desta OS
                            nav.navigate("relatorios/form?id=0&osId=$it")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                enabled = clienteId > 0
            ) {
                Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp)); Text("Transformar em relatório técnico")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (mostrarAssinatura) {
        AssinaturaDialog(
            "Assinatura do cliente",
            aoSalvar = { assinaturaCliente = it },
            aoFechar = { mostrarAssinatura = false }
        )
    }
}
