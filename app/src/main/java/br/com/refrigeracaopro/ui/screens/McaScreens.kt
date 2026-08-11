package br.com.refrigeracaopro.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.EnsaioMca
import br.com.refrigeracaopro.data.Mca
import br.com.refrigeracaopro.data.Motor
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.ConfirmarExclusao
import br.com.refrigeracaopro.ui.components.SeletorOpcoes
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.components.TituloSecao
import br.com.refrigeracaopro.viewmodel.McaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val McaVermelho = Color(0xFFC62828)
internal val McaAmarelo = Color(0xFFF9A825)
internal val McaVerde = Color(0xFF2E7D32)
internal val McaCinza = Color(0xFF9E9E9E)

internal const val MCA_SEM_VINCULO = "— Não vinculado —"
internal val CLASSES_MOTOR = listOf("", "B", "F", "H")
internal val LIGACOES = listOf("", "Estrela", "Triângulo")
internal val ACIONAMENTOS = listOf("", "Partida direta", "Soft-starter", "Inversor")

internal fun corSeveridadeMca(severidade: Mca.Severidade?): Color = when (severidade) {
    Mca.Severidade.OK -> McaVerde
    Mca.Severidade.ATENCAO -> McaAmarelo
    Mca.Severidade.CRITICO -> McaVermelho
    null -> McaCinza
}

/** Lista de motores cadastrados para ensaio MCA, com busca por tag/cliente. */
@Composable
fun McaMotoresScreen(nav: NavController, vm: McaViewModel = viewModel()) {
    val motores by vm.lista.collectAsState()
    val busca by vm.busca.collectAsState()
    var excluir by remember { mutableStateOf<Motor?>(null) }

    TelaBase(
        nav, "Análise de Motores (MCA)",
        aoAdicionar = { nav.navigate("mca/motor?id=0") },
        acoes = {
            IconButton(onClick = { nav.navigate("mca/ajustes") }) {
                Icon(Icons.Default.Tune, "Limites de alerta")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = busca,
                onValueChange = { vm.busca.value = it },
                label = { Text("Pesquisar por tag, cliente, setor ou equipamento") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            if (motores.isEmpty()) {
                Text(
                    "Nenhum motor cadastrado. Toque em + para cadastrar o primeiro e iniciar o " +
                        "acompanhamento por ensaio estático.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn {
                items(motores, key = { it.id }) { motor ->
                    Card(
                        onClick = { nav.navigate("mca/motor?id=${motor.id}") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(motor.tag, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                val linha = listOf(motor.cliente, motor.setor, motor.equipamentoAcionado)
                                    .filter { it.isNotBlank() }.joinToString(" • ")
                                if (linha.isNotBlank()) {
                                    Text(
                                        linha,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                val ficha = listOfNotNull(
                                    motor.potenciaCV?.let { "%.0f CV".format(it) },
                                    motor.tensaoNominalV?.let { "%.0f V".format(it) },
                                    motor.polos?.let { "$it polos" },
                                ).joinToString(" • ")
                                if (ficha.isNotBlank()) {
                                    Text(
                                        ficha,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            IconButton(onClick = { excluir = motor }) {
                                Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    excluir?.let { motor ->
        ConfirmarExclusao(
            "Excluir o motor \"${motor.tag}\"? Os ensaios ficam órfãos no histórico.",
            aoConfirmar = { vm.excluirMotor(motor); excluir = null },
            aoCancelar = { excluir = null },
        )
    }
}

/** Cadastro do motor e histórico dos ensaios daquele motor. */
@Composable
fun McaMotorFormScreen(nav: NavController, motorId: Long, vm: McaViewModel = viewModel()) {
    val context = LocalContext.current
    val clientes by vm.clientes.collectAsState()
    val equipamentos by vm.equipamentos.collectAsState()

    var original by remember { mutableStateOf<Motor?>(null) }
    var tag by remember { mutableStateOf("") }
    var clienteId by remember { mutableStateOf(0L) }
    var equipamentoId by remember { mutableStateOf(0L) }
    var cliente by remember { mutableStateOf("") }
    var setor by remember { mutableStateOf("") }
    var acionado by remember { mutableStateOf("") }
    var fabricante by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var serie by remember { mutableStateOf("") }
    var potencia by remember { mutableStateOf("") }
    var tensao by remember { mutableStateOf("") }
    var corrente by remember { mutableStateOf("") }
    var polos by remember { mutableStateOf("") }
    var rpm by remember { mutableStateOf("") }
    var frequencia by remember { mutableStateOf("60") }
    var classe by remember { mutableStateOf("") }
    var ligacao by remember { mutableStateOf("") }
    var acionamento by remember { mutableStateOf("") }
    var observacoes by remember { mutableStateOf("") }
    var rascunho by remember { mutableStateOf<EnsaioMca?>(null) }

    LaunchedEffect(motorId) {
        if (motorId > 0) vm.buscarMotor(motorId)?.let { m ->
            original = m
            tag = m.tag; clienteId = m.clienteId ?: 0L; equipamentoId = m.equipamentoId ?: 0L
            cliente = m.cliente; setor = m.setor; acionado = m.equipamentoAcionado
            fabricante = m.fabricante; modelo = m.modelo; serie = m.numeroSerie
            potencia = m.potenciaCV?.let { "%.0f".format(it) } ?: ""
            tensao = m.tensaoNominalV?.let { "%.0f".format(it) } ?: ""
            corrente = m.correnteNominalA?.let { "%.1f".format(it) } ?: ""
            polos = m.polos?.toString() ?: ""; rpm = m.rpmNominal?.toString() ?: ""
            frequencia = m.frequenciaHz?.let { "%.0f".format(it) } ?: "60"
            classe = m.classeIsolamento; ligacao = m.tipoLigacao; acionamento = m.acionamento
            observacoes = m.observacoes
        }
        if (motorId > 0) rascunho = vm.rascunhoDoMotor(motorId)
    }

    val ensaios by (if (motorId > 0) vm.ensaiosDoMotor(motorId) else kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    val formato = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }
    val limites by vm.limites.collectAsState()

    fun num(s: String) = s.replace(",", ".").toDoubleOrNull()

    TelaBase(nav, if (motorId > 0) "Motor ${tag.ifBlank { "" }}".trim() else "Novo motor") { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()).imePadding()
        ) {
            CampoTexto(tag, { tag = it }, "Tag do motor * (única)")
            SeletorOpcoes(
                "Cliente cadastrado",
                listOf(MCA_SEM_VINCULO) + clientes.map { it.nome },
                clientes.find { it.id == clienteId }?.nome ?: MCA_SEM_VINCULO,
                aoSelecionar = { nome ->
                    val c = clientes.find { it.nome == nome }
                    clienteId = c?.id ?: 0L
                    equipamentoId = 0L
                    if (c != null) cliente = c.nome
                },
            )
            val equipsDoCliente = equipamentos.filter { clienteId == 0L || it.clienteId == clienteId }
            SeletorOpcoes(
                "Equipamento cadastrado",
                listOf(MCA_SEM_VINCULO) + equipsDoCliente.map { "${it.tipo} ${it.marca}".trim() },
                equipsDoCliente.find { it.id == equipamentoId }?.let { "${it.tipo} ${it.marca}".trim() }
                    ?: MCA_SEM_VINCULO,
                aoSelecionar = { texto ->
                    equipamentoId = equipsDoCliente.find { "${it.tipo} ${it.marca}".trim() == texto }?.id ?: 0L
                },
            )
            CampoTexto(cliente, { cliente = it }, "Cliente (texto livre)")
            Row(Modifier.fillMaxWidth()) {
                CampoTexto(setor, { setor = it }, "Setor", Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                CampoTexto(acionado, { acionado = it }, "Equipamento acionado", Modifier.weight(1f))
            }

            TituloSecao("Dados de placa")
            Row(Modifier.fillMaxWidth()) {
                CampoTexto(fabricante, { fabricante = it }, "Fabricante", Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                CampoTexto(modelo, { modelo = it }, "Modelo", Modifier.weight(1f))
            }
            CampoTexto(serie, { serie = it }, "Número de série")
            Row(Modifier.fillMaxWidth()) {
                CampoTexto(potencia, { potencia = it }, "Potência (CV)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.width(8.dp))
                CampoTexto(tensao, { tensao = it }, "Tensão (V)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.width(8.dp))
                CampoTexto(corrente, { corrente = it }, "Corrente (A)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Row(Modifier.fillMaxWidth()) {
                CampoTexto(polos, { polos = it }, "Polos", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.width(8.dp))
                CampoTexto(rpm, { rpm = it }, "RPM nominal", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.width(8.dp))
                CampoTexto(frequencia, { frequencia = it }, "Frequência (Hz)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            SeletorOpcoes("Classe de isolamento", CLASSES_MOTOR, classe, { classe = it })
            SeletorOpcoes("Tipo de ligação", LIGACOES, ligacao, { ligacao = it })
            SeletorOpcoes("Acionamento", ACIONAMENTOS, acionamento, { acionamento = it })
            CampoTexto(observacoes, { observacoes = it }, "Observações", linhas = 2)

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val motor = (original ?: Motor(tag = "")).copy(
                        tag = tag.trim(),
                        clienteId = clienteId.takeIf { it > 0 },
                        equipamentoId = equipamentoId.takeIf { it > 0 },
                        cliente = cliente, setor = setor, equipamentoAcionado = acionado,
                        fabricante = fabricante, modelo = modelo, numeroSerie = serie,
                        potenciaCV = num(potencia), tensaoNominalV = num(tensao),
                        correnteNominalA = num(corrente), polos = polos.toIntOrNull(),
                        rpmNominal = rpm.toIntOrNull(), frequenciaHz = num(frequencia),
                        classeIsolamento = classe, tipoLigacao = ligacao, acionamento = acionamento,
                        observacoes = observacoes,
                    )
                    vm.salvarMotor(motor) { id ->
                        if (id == null) {
                            Toast.makeText(context, "Já existe um motor com a tag \"${tag.trim()}\".", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Motor salvo.", Toast.LENGTH_SHORT).show()
                            nav.popBackStack()
                        }
                    }
                },
                enabled = tag.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Salvar motor") }

            if (motorId > 0) {
                TituloSecao("Ensaios (${ensaios.size})")
                rascunho?.let { pendente ->
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = McaAmarelo.copy(alpha = 0.15f)
                        ),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Há um ensaio em andamento (bloco ${pendente.blocoAtual}).", fontWeight = FontWeight.SemiBold)
                            OutlinedButton(
                                onClick = { nav.navigate("mca/ensaio?motorId=$motorId&id=${pendente.id}") },
                                modifier = Modifier.padding(top = 6.dp),
                            ) { Text("Retomar rascunho") }
                        }
                    }
                }
                Button(
                    onClick = { nav.navigate("mca/ensaio?motorId=$motorId&id=0") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(48.dp),
                ) { Text("Novo ensaio") }

                ensaios.filter { !it.rascunho }.forEach { ensaio ->
                    val parecer = remember(ensaio.id, limites) {
                        Mca.avaliar(vm.leiturasDe(ensaio), limites)
                    }
                    val pior = parecer.achados.maxByOrNull { it.severidade.ordinal }?.severidade
                    Card(
                        onClick = { nav.navigate("mca/parecer?id=${ensaio.id}") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(12.dp)
                                    .background(corSeveridadeMca(pior), RoundedCornerShape(6.dp))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${formato.format(Date(ensaio.dataHora))} • ${ensaio.numero}",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    parecer.achados.firstOrNull()?.titulo ?: "Sem parecer",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Tela de limites de alerta, com os defaults do módulo. */
@Composable
fun McaAjustesScreen(nav: NavController, vm: McaViewModel = viewModel()) {
    val context = LocalContext.current
    val limites by vm.limites.collectAsState()
    val campos = remember(limites) {
        mutableStateOf(
            listOf(
                Triple("Desbal. R40 (%)", limites.desbalR40Atencao, limites.desbalR40Critico),
                Triple("Desbal. L (%)", limites.desbalLAtencao, limites.desbalLCritico),
                Triple("Desbal. Z (%)", limites.desbalZAtencao, limites.desbalZCritico),
                Triple("Delta θ (°)", limites.deltaThetaAtencao, limites.deltaThetaCritico),
                Triple("Spread I/F (p.p.)", limites.spreadIfAtencao, limites.spreadIfCritico),
                Triple("Desbal. C p/ terra (%)", limites.desbalCAtencao, limites.desbalCCritico),
                Triple("Espalhamento RIC (%)", limites.ricAmplitudeAtencao, limites.ricAmplitudeCritico),
                Triple("Desvio senoidal RIC (%)", limites.ricSenoideAtencao, limites.ricSenoideCritico),
                Triple("Isolação (MΩ)", limites.isolacaoAtencaoMOhm, limites.isolacaoCriticoMOhm),
                Triple("Índice de polarização", limites.piAtencao, limites.piCritico),
            ).map { (nome, a, c) -> nome to (("%.2f".format(a)) to ("%.2f".format(c))) }
        )
    }

    TelaBase(nav, "Limites de alerta") { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()).imePadding()
        ) {
            Text(
                br.com.refrigeracaopro.data.McaLimites.AVISO_REFERENCIA,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Text("Índice", Modifier.weight(1.4f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Atenção", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Crítico", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            campos.value.forEachIndexed { indice, (nome, valores) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(nome, Modifier.weight(1.4f), style = MaterialTheme.typography.bodySmall)
                    CampoTexto(
                        valores.first,
                        { novo -> campos.value = campos.value.toMutableList().also { it[indice] = nome to (novo to valores.second) } },
                        "", Modifier.weight(1f),
                        teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Spacer(Modifier.width(6.dp))
                    CampoTexto(
                        valores.second,
                        { novo -> campos.value = campos.value.toMutableList().also { it[indice] = nome to (valores.first to novo) } },
                        "", Modifier.weight(1f),
                        teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }

            fun v(indice: Int, critico: Boolean, padrao: Double): Double {
                val par = campos.value[indice].second
                return (if (critico) par.second else par.first).replace(",", ".").toDoubleOrNull() ?: padrao
            }

            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        vm.restaurarLimites()
                        Toast.makeText(context, "Limites restaurados.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                ) { Text("Restaurar padrão") }
                Button(
                    onClick = {
                        val padrao = Mca.LimitesMca()
                        vm.salvarLimites(
                            Mca.LimitesMca(
                                desbalR40Atencao = v(0, false, padrao.desbalR40Atencao),
                                desbalR40Critico = v(0, true, padrao.desbalR40Critico),
                                desbalLAtencao = v(1, false, padrao.desbalLAtencao),
                                desbalLCritico = v(1, true, padrao.desbalLCritico),
                                desbalZAtencao = v(2, false, padrao.desbalZAtencao),
                                desbalZCritico = v(2, true, padrao.desbalZCritico),
                                deltaThetaAtencao = v(3, false, padrao.deltaThetaAtencao),
                                deltaThetaCritico = v(3, true, padrao.deltaThetaCritico),
                                spreadIfAtencao = v(4, false, padrao.spreadIfAtencao),
                                spreadIfCritico = v(4, true, padrao.spreadIfCritico),
                                desbalCAtencao = v(5, false, padrao.desbalCAtencao),
                                desbalCCritico = v(5, true, padrao.desbalCCritico),
                                ricAmplitudeAtencao = v(6, false, padrao.ricAmplitudeAtencao),
                                ricAmplitudeCritico = v(6, true, padrao.ricAmplitudeCritico),
                                ricSenoideAtencao = v(7, false, padrao.ricSenoideAtencao),
                                ricSenoideCritico = v(7, true, padrao.ricSenoideCritico),
                                isolacaoAtencaoMOhm = v(8, false, padrao.isolacaoAtencaoMOhm),
                                isolacaoCriticoMOhm = v(8, true, padrao.isolacaoCriticoMOhm),
                                piAtencao = v(9, false, padrao.piAtencao),
                                piCritico = v(9, true, padrao.piCritico),
                            )
                        )
                        Toast.makeText(context, "Limites salvos.", Toast.LENGTH_SHORT).show()
                        nav.popBackStack()
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                ) { Text("Salvar") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
