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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import br.com.refrigeracaopro.data.EnsaioIsolacao
import br.com.refrigeracaopro.data.Equipamento
import br.com.refrigeracaopro.data.Megohmetro
import br.com.refrigeracaopro.pdf.PdfGenerator
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.ConfirmarExclusao
import br.com.refrigeracaopro.ui.components.SeletorOpcoes
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.theme.Verde
import br.com.refrigeracaopro.util.Arquivos
import br.com.refrigeracaopro.viewmodel.MegohmetroViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Vermelho = Color(0xFFC62828)
private val Laranja = Color(0xFFEF6C00)
private val Amarelo = Color(0xFFF9A825)

private val ABAS = listOf("Passo a passo", "Defeitos comuns", "Temperatura")
private const val SEM_VINCULO = "— Não vinculado —"

/**
 * Painel de Diagnóstico de Megômetro: entra com as leituras do megger e o app
 * calcula isolação puntual, DAR e PI, classificando a condição do equipamento
 * conforme a IEEE 43.
 *
 * Os ensaios podem ser salvos por cliente/equipamento, formando o histórico
 * que permite acompanhar a tendência da isolação (manutenção preditiva), e o
 * conjunto pode ser exportado em PDF.
 */
@Composable
fun MegohmetroScreen(nav: NavController, vm: MegohmetroViewModel = viewModel()) {
    val context = LocalContext.current
    val clientes by vm.clientes.collectAsState()
    val equipamentos by vm.equipamentos.collectAsState()

    var clienteId by remember { mutableStateOf(0L) }
    var equipamentoId by remember { mutableStateOf(0L) }
    var tensao by remember { mutableStateOf("") }
    var r30s by remember { mutableStateOf("") }
    var r60s by remember { mutableStateOf("") }
    var r10min by remember { mutableStateOf("") }
    var temp by remember { mutableStateOf("") }
    var tempBase by remember { mutableStateOf("40 °C") }
    var observacoes by remember { mutableStateOf("") }
    var aba by remember { mutableIntStateOf(0) }
    var excluir by remember { mutableStateOf<EnsaioIsolacao?>(null) }

    // Controle de "já gravado": evita duplicar o ensaio ao gerar o relatório
    var idSalvo by remember { mutableStateOf(0L) }
    var numeroSalvo by remember { mutableStateOf("") }
    var chaveSalva by remember { mutableStateOf("") }

    fun num(s: String) = s.replace(",", ".").toDoubleOrNull()
    val base = if (tempBase.startsWith("20")) Megohmetro.TEMP_BASE_20 else Megohmetro.TEMP_BASE_40

    val equipsDoCliente = remember(clienteId, equipamentos) {
        if (clienteId > 0) equipamentos.filter { it.clienteId == clienteId } else equipamentos
    }
    val historicoFlow = remember(clienteId, equipamentoId) { vm.historico(clienteId, equipamentoId) }
    val historico by historicoFlow.collectAsState(initial = emptyList())

    // Recalcula automaticamente a cada mudança nos campos
    val resultado = remember(tensao, r30s, r60s, r10min, temp, tempBase) {
        Megohmetro.calcular(
            Megohmetro.Entrada(
                tensaoV = num(tensao) ?: 0.0,
                r30s = num(r30s) ?: 0.0,
                r60s = num(r60s) ?: 0.0,
                r10min = num(r10min) ?: 0.0,
                tempC = num(temp),
                tempBase = base,
            )
        )
    }

    val chaveAtual = chaveDe(clienteId, equipamentoId, tensao, r30s, r60s, r10min, temp, tempBase, observacoes)
    val jaSalvo = idSalvo > 0 && chaveSalva == chaveAtual

    /** Grava (ou regrava) o ensaio atual e devolve o registro persistido. */
    fun salvarEnsaio(aoConcluir: (EnsaioIsolacao) -> Unit) {
        val ensaio = EnsaioIsolacao(
            id = if (jaSalvo) idSalvo else 0L,
            numero = if (jaSalvo) numeroSalvo else "",
            clienteId = clienteId.takeIf { it > 0 },
            equipamentoId = equipamentoId.takeIf { it > 0 },
            tensaoV = num(tensao) ?: 0.0,
            r30s = num(r30s) ?: 0.0,
            r60s = num(r60s) ?: 0.0,
            r10min = num(r10min) ?: 0.0,
            tempC = num(temp),
            tempBase = base,
            dar = resultado.dar,
            pi = resultado.pi,
            puntualCorrigido = resultado.puntualCorrigido ?: resultado.puntual,
            condicao = resultado.condicao?.rotulo.orEmpty(),
            observacoes = observacoes,
        )
        val chave = chaveAtual
        vm.salvar(ensaio) { salvo ->
            idSalvo = salvo.id
            numeroSalvo = salvo.numero
            chaveSalva = chave
            aoConcluir(salvo)
        }
    }

    /** Recarrega um ensaio do histórico nos campos da tela. */
    fun carregar(e: EnsaioIsolacao) {
        clienteId = e.clienteId ?: 0L
        equipamentoId = e.equipamentoId ?: 0L
        tensao = paraCampo(e.tensaoV)
        r30s = paraCampo(e.r30s)
        r60s = paraCampo(e.r60s)
        r10min = paraCampo(e.r10min)
        temp = e.tempC?.let { paraCampo(it) }.orEmpty()
        tempBase = if (e.tempBase == Megohmetro.TEMP_BASE_20) "20 °C" else "40 °C"
        observacoes = e.observacoes
        idSalvo = e.id
        numeroSalvo = e.numero
        chaveSalva = chaveDe(
            e.clienteId ?: 0L, e.equipamentoId ?: 0L, paraCampo(e.tensaoV), paraCampo(e.r30s),
            paraCampo(e.r60s), paraCampo(e.r10min), e.tempC?.let { paraCampo(it) }.orEmpty(),
            if (e.tempBase == Megohmetro.TEMP_BASE_20) "20 °C" else "40 °C", e.observacoes,
        )
    }

    TelaBase(nav, "Painel de Diagnóstico de Megômetro") { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ---------------- Identificação ----------------
            Titulo("Cliente e equipamento")
            SeletorOpcoes(
                "Cliente",
                listOf(SEM_VINCULO) + clientes.map { it.nome },
                clientes.find { it.id == clienteId }?.nome ?: SEM_VINCULO,
                aoSelecionar = { nome ->
                    clienteId = clientes.find { it.nome == nome }?.id ?: 0L
                    equipamentoId = 0L
                },
            )
            SeletorOpcoes(
                "Equipamento / máquina",
                listOf(SEM_VINCULO) + equipsDoCliente.map { rotuloEquip(it) },
                equipsDoCliente.find { it.id == equipamentoId }?.let { rotuloEquip(it) } ?: SEM_VINCULO,
                aoSelecionar = { texto ->
                    equipamentoId = equipsDoCliente.find { rotuloEquip(it) == texto }?.id ?: 0L
                },
            )
            Text(
                "Vincular o ensaio permite acompanhar a tendência da isolação da máquina ao longo do tempo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ---------------- Entrada de dados ----------------
            Titulo("Entrada de dados")
            CampoTexto(
                tensao, { tensao = it }, "Tensão de teste aplicada (V)",
                teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Row(Modifier.fillMaxWidth()) {
                CampoTexto(
                    r30s, { r30s = it }, "R 30s (MΩ)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.width(8.dp))
                CampoTexto(
                    r60s, { r60s = it }, "R 1min (MΩ)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.width(8.dp))
                CampoTexto(
                    r10min, { r10min = it }, "R 10min (MΩ)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CampoTexto(
                    temp, { temp = it }, "Temperatura do equipamento (°C)", Modifier.weight(1.4f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.width(8.dp))
                SeletorOpcoes("Base", listOf("40 °C", "20 °C"), tempBase, { tempBase = it }, Modifier.weight(1f))
            }
            Text(
                "A temperatura é opcional e serve para corrigir as leituras para a base escolhida.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CampoTexto(observacoes, { observacoes = it }, "Observações do ensaio", linhas = 2)

            // ---------------- Resultados ----------------
            Titulo("Resultados")
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    LinhaResultado(
                        "Isolação puntual (60 s)",
                        resultado.puntual?.let { "%s MΩ".format(formatar(it)) } ?: "—",
                        detalhe = resultado.puntualCorrigido?.let { corrigido ->
                            "Corrigida p/ ${if (tempBase.startsWith("20")) "20" else "40"} °C: " +
                                "${formatar(corrigido)} MΩ (fator %.2f)".format(resultado.fatorTemperatura ?: 1.0)
                        },
                    )
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    LinhaResultado(
                        "Índice de Absorção (DAR)",
                        resultado.dar?.let { "%.2f".format(it) } ?: "—",
                        detalhe = resultado.classeDar.takeIf { it.isNotBlank() }?.let { "R60s ÷ R30s — $it" }
                            ?: "Informe as leituras de 30 s e 1 min",
                        cor = resultado.dar?.let { corDar(it) },
                    )
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    LinhaResultado(
                        "Índice de Polarização (PI)",
                        resultado.pi?.let { "%.2f".format(it) } ?: "—",
                        detalhe = resultado.classePi.takeIf { it.isNotBlank() }?.let { "R10min ÷ R1min — $it" }
                            ?: "Informe as leituras de 1 min e 10 min",
                        cor = resultado.pi?.let { corPi(it) },
                    )
                    resultado.minimoRecomendado?.let { minimo ->
                        HorizontalDivider(Modifier.padding(vertical = 10.dp))
                        Text(
                            "Mínimo de referência (kV + 1): ${formatar(minimo)} MΩ" +
                                when (resultado.atendeMinimo) {
                                    true -> " — atendido"
                                    false -> " — NÃO atendido"
                                    else -> ""
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (resultado.atendeMinimo == false) Vermelho
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ---------------- Diagnóstico automático ----------------
            Titulo("Diagnóstico automático")
            val condicao = resultado.condicao
            if (condicao == null) {
                Text(
                    "Preencha ao menos duas leituras (30 s e 1 min, ou 1 min e 10 min) para " +
                        "o diagnóstico automático.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val cor = corCondicao(condicao)
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = 0.12f)),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            condicao.rotulo.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .background(cor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(resultado.diagnostico, style = MaterialTheme.typography.bodyMedium)
                        if (resultado.recomendacao.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Recomendação: ${resultado.recomendacao}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        resultado.avisos.forEach { aviso ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "• $aviso",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ---------------- Histórico ----------------
            val tituloHistorico = when {
                equipamentoId > 0 -> "Histórico do equipamento"
                clienteId > 0 -> "Histórico do cliente"
                else -> "Últimos ensaios registrados"
            }
            Titulo("$tituloHistorico (${historico.size})")
            SecaoHistorico(
                historico = historico,
                aoAbrir = { carregar(it) },
                aoExcluir = { excluir = it },
            )

            // ---------------- Ações ----------------
            Titulo("Ações")
            if (jaSalvo) {
                Text(
                    "Ensaio salvo como $numeroSalvo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Verde,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        salvarEnsaio { salvo ->
                            Toast.makeText(context, "Ensaio ${salvo.numero} salvo no histórico.", Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = condicao != null,
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Salvar ensaio")
                }
                Button(
                    onClick = {
                        salvarEnsaio { salvo ->
                            val cliente = clientes.find { it.id == salvo.clienteId }
                            val equip = equipamentos.find { it.id == salvo.equipamentoId }
                            val lista = (listOf(salvo) + historico.filter { it.id != salvo.id })
                                .sortedByDescending { it.dataHora }
                            val pdf = PdfGenerator.gerarEnsaioIsolacao(context, salvo, cliente, equip, resultado, lista)
                            Arquivos.compartilhar(context, pdf, "application/pdf", salvo.numero)
                        }
                    },
                    enabled = condicao != null,
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Gerar relatório")
                }
            }
            Text(
                "O relatório salva o ensaio no histórico e gera o PDF com as leituras, o " +
                    "diagnóstico e a evolução das medições anteriores.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )

            // ---------------- Referência técnica ----------------
            Titulo("Referência técnica")
            TabRow(selectedTabIndex = aba, containerColor = MaterialTheme.colorScheme.surface) {
                ABAS.forEachIndexed { indice, titulo ->
                    Tab(
                        selected = aba == indice,
                        onClick = { aba = indice },
                        text = { Text(titulo, style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            when (aba) {
                0 -> AbaPassoAPasso()
                1 -> AbaDefeitos()
                else -> AbaTemperatura()
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    excluir?.let { ensaio ->
        ConfirmarExclusao(
            "Excluir o ensaio ${ensaio.numero.ifBlank { "selecionado" }} do histórico?",
            aoConfirmar = {
                vm.excluir(ensaio)
                if (ensaio.id == idSalvo) { idSalvo = 0L; numeroSalvo = ""; chaveSalva = "" }
                excluir = null
            },
            aoCancelar = { excluir = null },
        )
    }
}

/** Histórico de ensaios com barra proporcional e variação entre medições. */
@Composable
private fun SecaoHistorico(
    historico: List<EnsaioIsolacao>,
    aoAbrir: (EnsaioIsolacao) -> Unit,
    aoExcluir: (EnsaioIsolacao) -> Unit,
) {
    if (historico.isEmpty()) {
        Text(
            "Nenhum ensaio salvo ainda. Preencha as leituras e toque em \"Salvar ensaio\" para " +
                "começar o acompanhamento da isolação desta máquina.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val formato = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
    val valorDe = { e: EnsaioIsolacao -> e.puntualCorrigido ?: e.r60s }
    val maximo = historico.maxOf { valorDe(it) }.coerceAtLeast(0.001)

    // Tendência geral: do ensaio mais antigo ao mais recente
    val pontos = historico.sortedBy { it.dataHora }.map { Megohmetro.Ponto(it.dataHora, valorDe(it)) }
    Megohmetro.tendencia(pontos)?.let { tendencia ->
        val cor = if (tendencia.alerta) Vermelho else if (tendencia.variacao < -0.10) Amarelo else Verde
        Card(
            Modifier.fillMaxWidth().padding(bottom = 10.dp),
            colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = 0.12f)),
        ) {
            Text(
                tendencia.texto,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(14.dp),
            )
        }
    }

    historico.forEachIndexed { indice, ensaio ->
        // O item seguinte da lista é o ensaio imediatamente anterior no tempo
        val anterior = historico.getOrNull(indice + 1)
        val variacao = anterior?.let { Megohmetro.variacao(valorDe(ensaio), valorDe(it)) }
        Card(
            onClick = { aoAbrir(ensaio) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${formato.format(Date(ensaio.dataHora))}  •  ${formatar(valorDe(ensaio))} MΩ",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        val indices = listOfNotNull(
                            ensaio.pi?.let { "PI %.2f".format(it) },
                            ensaio.dar?.let { "DAR %.2f".format(it) },
                            ensaio.condicao.takeIf { it.isNotBlank() },
                            ensaio.numero.takeIf { it.isNotBlank() },
                        ).joinToString(" • ")
                        if (indices.isNotBlank()) {
                            Text(
                                indices,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (variacao != null) {
                        Text(
                            (if (variacao >= 0) "+" else "") + "%.0f%%".format(variacao * 100),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                variacao <= -Megohmetro.QUEDA_ALERTA -> Vermelho
                                variacao < -0.10 -> Laranja
                                else -> Verde
                            },
                        )
                    }
                    IconButton(onClick = { aoExcluir(ensaio) }) {
                        Icon(Icons.Default.Delete, "Excluir ensaio", tint = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.fillMaxWidth().height(6.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth((valorDe(ensaio) / maximo).toFloat().coerceIn(0.02f, 1f))
                            .height(6.dp)
                            .background(corCondicaoTexto(ensaio.condicao), RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

/** Tab 1 — passo a passo dos ensaios (Puntual, DAR e PI). */
@Composable
private fun AbaPassoAPasso() {
    Column {
        CardTexto("Procedimento comum a todos os ensaios") {
            Megohmetro.PASSO_A_PASSO_GERAL.forEach { passo ->
                Text(passo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Segurança: o megômetro aplica alta tensão. Confirme o desligamento, aterre o " +
                    "equipamento e só toque nos terminais após a descarga.",
                style = MaterialTheme.typography.bodySmall,
                color = Vermelho,
            )
        }
        Megohmetro.ENSAIOS.forEach { ensaio ->
            CardTexto(ensaio.nome) {
                ensaio.passos.forEachIndexed { indice, passo ->
                    Text(
                        "${indice + 1}. $passo",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 3.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    ensaio.nota,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        CardTexto("Faixas de interpretação (IEEE 43)") {
            LinhaTabela("PI < 1,0", "Perigoso", cabecalho = false)
            LinhaTabela("PI 1,0 – 2,0", "Pobre", cabecalho = false)
            LinhaTabela("PI 2,0 – 4,0", "Bom", cabecalho = false)
            LinhaTabela("PI > 4,0", "Excelente", cabecalho = false)
            Spacer(Modifier.height(8.dp))
            LinhaTabela("DAR < 1,25", "Inadequado", cabecalho = false)
            LinhaTabela("DAR 1,25 – 1,6", "Aceitável", cabecalho = false)
            LinhaTabela("DAR > 1,6", "Excelente", cabecalho = false)
        }
        CardTexto("Tensão de ensaio usual") {
            LinhaTabela("Tensão nominal", "Tensão de ensaio", cabecalho = true)
            Megohmetro.TENSOES_ENSAIO.forEach { (nominal, ensaio) ->
                LinhaTabela(nominal, ensaio, cabecalho = false)
            }
        }
    }
}

/** Tab 2 — tabela de defeitos comuns e causas. */
@Composable
private fun AbaDefeitos() {
    CardTexto("Defeitos comuns e causas") {
        LinhaTabela("Sintoma no ensaio", "Causas prováveis", cabecalho = true)
        Megohmetro.DEFEITOS.forEach { defeito ->
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            LinhaTabela(defeito.sintoma, defeito.causas, cabecalho = false)
        }
    }
}

/** Tab 3 — fatores de correção de temperatura. */
@Composable
private fun AbaTemperatura() {
    Column {
        CardTexto("Por que corrigir pela temperatura") {
            Text(Megohmetro.CORRECAO_TEMPERATURA_TEXTO, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            Text(
                Megohmetro.CORRECAO_FORMULA,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Verde,
            )
        }
        CardTexto("Fatores de correção") {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("Temp. medida", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Base 40 °C", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Base 20 °C", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Megohmetro.TABELA_CORRECAO.forEach { (temperatura, fator40, fator20) ->
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Text("$temperatura °C", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text("× %.2f".format(fator40), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text("× %.2f".format(fator20), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Exemplo: 100 MΩ medidos a 50 °C equivalem a 200 MΩ corrigidos para 40 °C. " +
                    "Compare sempre leituras corrigidas na mesma base.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------- Componentes auxiliares ----------------

@Composable
private fun Titulo(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun LinhaResultado(rotulo: String, valor: String, detalhe: String? = null, cor: Color? = null) {
    Column(Modifier.fillMaxWidth()) {
        Text(rotulo, style = MaterialTheme.typography.labelMedium)
        Text(
            valor,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = cor ?: Verde,
        )
        if (detalhe != null) {
            Text(detalhe, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CardTexto(titulo: String, conteudo: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            conteudo()
        }
    }
}

@Composable
private fun LinhaTabela(esquerda: String, direita: String, cabecalho: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            esquerda,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (cabecalho) FontWeight.Bold else FontWeight.SemiBold,
        )
        Text(
            direita,
            Modifier.weight(1.3f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (cabecalho) FontWeight.Bold else FontWeight.Normal,
            color = if (cabecalho) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun rotuloEquip(e: Equipamento): String =
    listOf(e.tipo, e.marca, e.modelo).filter { it.isNotBlank() }.joinToString(" ")

/** Chave que identifica o conteúdo do ensaio na tela (detecta alterações). */
private fun chaveDe(
    clienteId: Long, equipamentoId: Long, tensao: String, r30s: String, r60s: String,
    r10min: String, temp: String, tempBase: String, observacoes: String,
): String = listOf(clienteId, equipamentoId, tensao, r30s, r60s, r10min, temp, tempBase, observacoes)
    .joinToString("|")

/** Converte um valor guardado de volta para o campo de texto. */
private fun paraCampo(valor: Double): String = when {
    valor == 0.0 -> ""
    valor == Math.floor(valor) -> "%.0f".format(valor)
    else -> valor.toString()
}

private fun corCondicao(condicao: Megohmetro.Condicao) = when (condicao) {
    Megohmetro.Condicao.EXCELENTE -> Verde
    Megohmetro.Condicao.BOM -> Verde
    Megohmetro.Condicao.DUVIDOSO -> Amarelo
    Megohmetro.Condicao.POBRE -> Laranja
    Megohmetro.Condicao.PERIGOSO -> Vermelho
}

/** Cor a partir do rótulo gravado no histórico. */
private fun corCondicaoTexto(rotulo: String): Color =
    Megohmetro.Condicao.entries.firstOrNull { it.rotulo == rotulo }?.let { corCondicao(it) } ?: Verde

private fun corPi(pi: Double) = when {
    pi < 1.0 -> Vermelho
    pi < 2.0 -> Laranja
    else -> Verde
}

private fun corDar(dar: Double) = when {
    dar < 1.25 -> Laranja
    dar <= 1.6 -> Amarelo
    else -> Verde
}

/** Formata MΩ com casas decimais só quando o valor é pequeno. */
private fun formatar(valor: Double): String = when {
    valor >= 1000 -> "%,.0f".format(valor).replace(",", ".")
    valor >= 10 -> "%.0f".format(valor)
    else -> "%.2f".format(valor)
}
