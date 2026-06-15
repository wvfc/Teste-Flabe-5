package br.com.refrigeracaopro.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.Projeto
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.SeletorOpcoes
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.viewmodel.ProjetosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Cadastro/edição dos dados do projeto. Ao criar, abre o editor em seguida. */
@Composable
fun ProjetoCadastroScreen(nav: NavController, projetoId: Long, vm: ProjetosViewModel = viewModel()) {
    val clientes by vm.clientes.collectAsState()
    var original by remember { mutableStateOf<Projeto?>(null) }
    var nome by remember { mutableStateOf("") }
    var clienteId by remember { mutableStateOf<Long?>(null) }
    var equipamento by remember { mutableStateOf("") }
    var local by remember { mutableStateOf("") }
    var responsavel by remember { mutableStateOf("") }
    var data by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())) }
    var pressao by remember { mutableStateOf("") }
    var fluido by remember { mutableStateOf("") }
    var vazao by remember { mutableStateOf("") }
    var temperatura by remember { mutableStateOf("") }
    var observacoes by remember { mutableStateOf("") }

    LaunchedEffect(projetoId) {
        if (projetoId > 0) vm.buscar(projetoId)?.let { p ->
            original = p
            nome = p.nome; clienteId = p.clienteId; equipamento = p.equipamento; local = p.local
            responsavel = p.responsavel; data = p.data; pressao = p.pressaoTrabalho; fluido = p.fluido
            vazao = p.vazao; temperatura = p.temperatura; observacoes = p.observacoes
        }
    }

    TelaBase(nav, if (projetoId > 0) "Dados do projeto" else "Novo projeto") { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            CampoTexto(nome, { nome = it }, "Nome do projeto *")
            SeletorOpcoes("Cliente", clientes.map { it.nome },
                clientes.find { it.id == clienteId }?.nome ?: "",
                aoSelecionar = { sel -> clienteId = clientes.find { it.nome == sel }?.id })
            CampoTexto(equipamento, { equipamento = it }, "Equipamento")
            CampoTexto(local, { local = it }, "Local")
            CampoTexto(responsavel, { responsavel = it }, "Responsável")
            CampoTexto(data, { data = it }, "Data")
            Row {
                CampoTexto(pressao, { pressao = it }, "Pressão de trabalho", modifier = Modifier.weight(1f).padding(end = 4.dp))
                CampoTexto(vazao, { vazao = it }, "Vazão (ex.: 50 m³/h)", modifier = Modifier.weight(1f).padding(start = 4.dp))
            }
            Row {
                CampoTexto(fluido, { fluido = it }, "Fluido refrigerante", modifier = Modifier.weight(1f).padding(end = 4.dp))
                CampoTexto(temperatura, { temperatura = it }, "Temperatura", modifier = Modifier.weight(1f).padding(start = 4.dp))
            }
            CampoTexto(observacoes, { observacoes = it }, "Observações", linhas = 3)

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val p = (original ?: Projeto()).copy(
                        nome = nome.ifBlank { "Projeto" }, clienteId = clienteId, equipamento = equipamento,
                        local = local, responsavel = responsavel, data = data, pressaoTrabalho = pressao,
                        fluido = fluido, vazao = vazao, temperatura = temperatura, observacoes = observacoes,
                    )
                    vm.salvar(p) { novoId ->
                        if (projetoId > 0) nav.popBackStack()
                        else {
                            // Abre o editor do projeto recém-criado
                            nav.popBackStack()
                            nav.navigate("projetos/editor?id=$novoId")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = nome.isNotBlank()
            ) { Text(if (projetoId > 0) "Salvar dados" else "Criar e abrir editor") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
