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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.Cliente
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.ConfirmarExclusao
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.components.TituloSecao
import br.com.refrigeracaopro.util.Arquivos
import br.com.refrigeracaopro.viewmodel.ClientesViewModel
import kotlinx.coroutines.flow.flowOf
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Lista de clientes com pesquisa, edição e exclusão. */
@Composable
fun ClientesScreen(nav: NavController, vm: ClientesViewModel = viewModel()) {
    val context = LocalContext.current
    val clientes by vm.clientes.collectAsState()
    val busca by vm.busca.collectAsState()
    var excluir by remember { mutableStateOf<Cliente?>(null) }

    // Importar clientes de um CSV escolhido pelo usuário
    val importar = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val conteudo = runCatching {
                context.contentResolver.openInputStream(uri)!!.use { it.bufferedReader().readText() }
            }.getOrNull()
            if (conteudo != null) vm.importarCsv(conteudo) { n ->
                Toast.makeText(context, "$n cliente(s) importado(s).", Toast.LENGTH_LONG).show()
            } else Toast.makeText(context, "Não foi possível ler o arquivo.", Toast.LENGTH_LONG).show()
        }
    }

    TelaBase(
        nav, "Clientes",
        aoAdicionar = { nav.navigate("clientes/form?id=0") },
        acoes = {
            IconButton(onClick = {
                vm.exportarCsv { csv ->
                    val arquivo = File(Arquivos.pastaPdfs(context), "clientes.csv")
                    arquivo.writeText(csv)
                    Arquivos.compartilhar(context, arquivo, "text/csv", "Clientes (CSV)")
                }
            }) { Icon(Icons.Default.Upload, "Exportar clientes") }
            IconButton(onClick = { importar.launch("*/*") }) {
                Icon(Icons.Default.Download, "Importar clientes")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = busca,
                onValueChange = { vm.busca.value = it },
                label = { Text("Pesquisar por nome, CPF/CNPJ ou cidade") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn {
                items(clientes, key = { it.id }) { cliente ->
                    Card(
                        onClick = { nav.navigate("clientes/form?id=${cliente.id}") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(cliente.nome, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                if (cliente.telefone.isNotBlank() || cliente.cidade.isNotBlank()) {
                                    Text(
                                        listOf(cliente.telefone, cliente.cidade).filter { it.isNotBlank() }.joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            IconButton(onClick = { excluir = cliente }) {
                                Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    excluir?.let { cliente ->
        ConfirmarExclusao(
            "Excluir o cliente \"${cliente.nome}\"? Os equipamentos vinculados também serão removidos.",
            aoConfirmar = { vm.excluir(cliente); excluir = null },
            aoCancelar = { excluir = null }
        )
    }
}

/** Formulário de cliente (criar/editar) + equipamentos vinculados + histórico. */
@Composable
fun ClienteFormScreen(nav: NavController, clienteId: Long, vm: ClientesViewModel = viewModel()) {
    var original by remember { mutableStateOf<Cliente?>(null) }
    var nome by remember { mutableStateOf("") }
    var cpfCnpj by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var endereco by remember { mutableStateOf("") }
    var cidade by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf("") }
    var observacoes by remember { mutableStateOf("") }

    LaunchedEffect(clienteId) {
        if (clienteId > 0) vm.buscar(clienteId)?.let { c ->
            original = c
            nome = c.nome; cpfCnpj = c.cpfCnpj; telefone = c.telefone; whatsapp = c.whatsapp
            email = c.email; endereco = c.endereco; cidade = c.cidade; estado = c.estado
            observacoes = c.observacoes
        }
    }

    val equipamentos by (if (clienteId > 0) vm.equipamentosDe(clienteId) else flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    val historico by (if (clienteId > 0) vm.historicoDe(clienteId) else flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    val formatoData = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }

    TelaBase(nav, if (clienteId > 0) "Editar cliente" else "Novo cliente") { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            CampoTexto(nome, { nome = it }, "Nome / Razão social *")
            CampoTexto(cpfCnpj, { cpfCnpj = it }, "CPF / CNPJ")
            Row {
                CampoTexto(
                    telefone, { telefone = it }, "Telefone",
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                CampoTexto(
                    whatsapp, { whatsapp = it }, "WhatsApp",
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
            CampoTexto(email, { email = it }, "E-mail", teclado = KeyboardOptions(keyboardType = KeyboardType.Email))
            CampoTexto(endereco, { endereco = it }, "Endereço completo")
            Row {
                CampoTexto(cidade, { cidade = it }, "Cidade", modifier = Modifier.weight(2f).padding(end = 4.dp))
                CampoTexto(estado, { estado = it }, "UF", modifier = Modifier.weight(1f).padding(start = 4.dp))
            }
            CampoTexto(observacoes, { observacoes = it }, "Observações", linhas = 3)

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    if (nome.isBlank()) return@Button
                    val cliente = (original ?: Cliente(nome = "")).copy(
                        nome = nome, cpfCnpj = cpfCnpj, telefone = telefone, whatsapp = whatsapp,
                        email = email, endereco = endereco, cidade = cidade, estado = estado,
                        observacoes = observacoes,
                    )
                    vm.salvar(cliente) { nav.popBackStack() }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = nome.isNotBlank()
            ) { Text("Salvar cliente") }

            if (clienteId > 0) {
                TituloSecao("Equipamentos vinculados (${equipamentos.size})")
                equipamentos.forEach { equip ->
                    Card(
                        onClick = { nav.navigate("equipamentos/form?id=${equip.id}") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${equip.tipo} ${equip.marca}".trim(), fontWeight = FontWeight.SemiBold)
                                if (equip.modelo.isNotBlank()) Text(equip.modelo, style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                TextButton(onClick = { nav.navigate("equipamentos/form?id=0&clienteId=$clienteId") }) {
                    Text("+ Vincular novo equipamento")
                }

                TituloSecao("Histórico de atendimentos (${historico.size})")
                historico.forEach { os ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(os.numero, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(formatoData.format(Date(os.dataHora)), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.weight(0.2f))
                        Text(os.status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    HorizontalDivider()
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
