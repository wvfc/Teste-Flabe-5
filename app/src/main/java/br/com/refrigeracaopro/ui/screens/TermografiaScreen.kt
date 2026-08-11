package br.com.refrigeracaopro.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.InspecaoTermografica
import br.com.refrigeracaopro.data.Termografia
import br.com.refrigeracaopro.pdf.PdfGenerator
import br.com.refrigeracaopro.ui.components.CampoMarcacao
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.ConfirmarExclusao
import br.com.refrigeracaopro.ui.components.SeletorOpcoes
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.theme.Verde
import br.com.refrigeracaopro.util.Arquivos
import br.com.refrigeracaopro.viewmodel.TermografiaViewModel
import coil.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val VermelhoT = Color(0xFFC62828)
private val LaranjaT = Color(0xFFEF6C00)
private val AmareloT = Color(0xFFF9A825)
private val AzulT = Color(0xFF1565C0)

private val ABAS_TERMO = listOf("Como medir", "Emissividade", "Critérios", "O que inspecionar")
private const val SEM_VINCULO_T = "— Não vinculado —"

/**
 * Análise Termográfica: o técnico digita as temperaturas lidas na câmera e o
 * app calcula os ΔT, corrige pela carga e pelo vento, checa a emissividade e
 * classifica a severidade da anomalia (NETA / NFPA 70B).
 *
 * As inspeções ficam guardadas por cliente/equipamento e viram laudo em PDF
 * com as imagens térmica e visível pareadas.
 */
@Composable
fun TermografiaScreen(nav: NavController, vm: TermografiaViewModel = viewModel()) {
    val context = LocalContext.current
    val clientes by vm.clientes.collectAsState()
    val equipamentos by vm.equipamentos.collectAsState()

    var clienteId by remember { mutableStateOf(0L) }
    var equipamentoId by remember { mutableStateOf(0L) }
    var ponto by remember { mutableStateOf("") }

    var material by remember { mutableStateOf(Termografia.MATERIAIS.first { it.emissividade >= 0.9 }.nome) }
    var emissividade by remember { mutableStateOf("0.95") }
    var emissividadeCamera by remember { mutableStateOf("") }
    var tempRefletida by remember { mutableStateOf("") }
    var tempAmbiente by remember { mutableStateOf("") }
    var umidade by remember { mutableStateOf("") }
    var distancia by remember { mutableStateOf("") }
    var relacaoDS by remember { mutableStateOf("") }
    var angulo by remember { mutableStateOf("") }
    var local by remember { mutableStateOf("Interno") }
    var vento by remember { mutableStateOf("") }

    var correnteMedida by remember { mutableStateOf("") }
    var correnteNominal by remember { mutableStateOf("") }
    var classe by remember { mutableStateOf(Termografia.CLASSES_ISOLAMENTO.first().first) }

    var tempPonto by remember { mutableStateOf("") }
    var tempSimilar by remember { mutableStateOf("") }

    var fotosTermicas by remember { mutableStateOf(listOf<String>()) }
    var fotosVisiveis by remember { mutableStateOf(listOf<String>()) }
    var legendas by remember { mutableStateOf(mapOf<String, String>()) }
    var observacoes by remember { mutableStateOf("") }

    var aba by remember { mutableIntStateOf(0) }
    var excluir by remember { mutableStateOf<InspecaoTermografica?>(null) }

    var idSalvo by remember { mutableStateOf(0L) }
    var numeroSalvo by remember { mutableStateOf("") }
    var chaveSalva by remember { mutableStateOf("") }

    fun num(s: String) = s.replace(",", ".").toDoubleOrNull()
    val externo = local == "Externo"

    val equipsDoCliente = remember(clienteId, equipamentos) {
        if (clienteId > 0) equipamentos.filter { it.clienteId == clienteId } else equipamentos
    }
    val historicoFlow = remember(clienteId, equipamentoId) { vm.historico(clienteId, equipamentoId) }
    val historico by historicoFlow.collectAsState(initial = emptyList())

    val entrada = Termografia.Entrada(
        tempPonto = num(tempPonto),
        tempSimilar = num(tempSimilar),
        tempAmbiente = num(tempAmbiente),
        correnteMedida = num(correnteMedida),
        correnteNominal = num(correnteNominal),
        emissividade = num(emissividade) ?: Termografia.EMISSIVIDADE_PADRAO,
        emissividadeCamera = num(emissividadeCamera),
        tempRefletida = num(tempRefletida),
        distanciaM = num(distancia),
        relacaoDS = num(relacaoDS),
        anguloGraus = num(angulo),
        externo = externo,
        ventoMs = num(vento),
        classeIsolamento = classe,
    )
    val resultado = remember(entrada) { Termografia.calcular(entrada) }

    val chaveAtual = listOf(
        clienteId, equipamentoId, ponto, material, emissividade, emissividadeCamera, tempRefletida,
        tempAmbiente, umidade, distancia, relacaoDS, angulo, local, vento, correnteMedida,
        correnteNominal, classe, tempPonto, tempSimilar, observacoes,
        fotosTermicas.joinToString(","), fotosVisiveis.joinToString(","), legendas.toString(),
    ).joinToString("|")
    val jaSalvo = idSalvo > 0 && chaveSalva == chaveAtual

    fun salvarInspecao(aoConcluir: (InspecaoTermografica) -> Unit) {
        val inspecao = InspecaoTermografica(
            id = if (jaSalvo) idSalvo else 0L,
            numero = if (jaSalvo) numeroSalvo else "",
            clienteId = clienteId.takeIf { it > 0 },
            equipamentoId = equipamentoId.takeIf { it > 0 },
            ponto = ponto,
            material = material,
            emissividade = entrada.emissividade,
            emissividadeCamera = entrada.emissividadeCamera,
            tempRefletida = entrada.tempRefletida,
            tempAmbiente = entrada.tempAmbiente,
            umidade = num(umidade),
            distanciaM = entrada.distanciaM,
            relacaoDS = entrada.relacaoDS,
            anguloGraus = entrada.anguloGraus,
            externo = externo,
            ventoMs = entrada.ventoMs,
            correnteMedida = entrada.correnteMedida,
            correnteNominal = entrada.correnteNominal,
            classeIsolamento = classe,
            tempPonto = entrada.tempPonto,
            tempSimilar = entrada.tempSimilar,
            deltaTSimilar = resultado.deltaTSimilar,
            deltaTAmbiente = resultado.deltaTAmbiente,
            deltaTCorrigido = resultado.deltaTAvaliado,
            severidade = resultado.severidade?.rotulo.orEmpty(),
            diagnostico = resultado.diagnostico,
            recomendacao = resultado.recomendacao,
            fotosTermicas = Arquivos.listaParaTexto(fotosTermicas),
            fotosVisiveis = Arquivos.listaParaTexto(fotosVisiveis),
            legendasFotos = Termografia.codificarLegendas(legendas),
            observacoes = observacoes,
        )
        val chave = chaveAtual
        vm.salvar(inspecao) { salva ->
            idSalvo = salva.id
            numeroSalvo = salva.numero
            chaveSalva = chave
            aoConcluir(salva)
        }
    }

    TelaBase(nav, "Análise Termográfica") { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()).imePadding()
        ) {
            // ---------------- Identificação ----------------
            TituloT("Cliente, equipamento e ponto")
            SeletorOpcoes(
                "Cliente",
                listOf(SEM_VINCULO_T) + clientes.map { it.nome },
                clientes.find { it.id == clienteId }?.nome ?: SEM_VINCULO_T,
                aoSelecionar = { nome ->
                    clienteId = clientes.find { it.nome == nome }?.id ?: 0L
                    equipamentoId = 0L
                },
            )
            SeletorOpcoes(
                "Equipamento / máquina",
                listOf(SEM_VINCULO_T) + equipsDoCliente.map { rotuloEquipT(it.tipo, it.marca, it.modelo) },
                equipsDoCliente.find { it.id == equipamentoId }
                    ?.let { rotuloEquipT(it.tipo, it.marca, it.modelo) } ?: SEM_VINCULO_T,
                aoSelecionar = { texto ->
                    equipamentoId = equipsDoCliente
                        .find { rotuloEquipT(it.tipo, it.marca, it.modelo) == texto }?.id ?: 0L
                },
            )
            CampoTexto(ponto, { ponto = it }, "Ponto inspecionado")
            Text(
                "Ex.: \"Disjuntor da fase R do QGBT\", \"Mancal lado acoplamento\", \"Contator K1\". " +
                    "Use sempre o mesmo nome para acompanhar a evolução do ponto.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ---------------- Condições do ensaio ----------------
            TituloT("Condições do ensaio")
            SeletorOpcoes(
                "Material da superfície",
                Termografia.MATERIAIS.map { it.nome },
                material,
                aoSelecionar = { nome ->
                    material = nome
                    Termografia.material(nome)?.let { emissividade = it.emissividade.toString() }
                },
            )
            Termografia.material(material)?.let { m ->
                Text(
                    "${m.grupo} • faixa típica ${m.faixa}" + if (m.dica.isNotBlank()) "\n${m.dica}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (m.emissividade < 0.6) LaranjaT else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(Modifier.fillMaxWidth()) {
                CampoTexto(
                    emissividade, { emissividade = it }, "Emissividade (ε)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.width(8.dp))
                CampoTexto(
                    emissividadeCamera, { emissividadeCamera = it }, "ε usado na câmera", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Text(
                "Preencha \"ε usado na câmera\" só se ele foi diferente do material — o app reestima a temperatura real.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth()) {
                CampoTexto(
                    tempRefletida, { tempRefletida = it }, "T. refletida (°C)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.width(8.dp))
                CampoTexto(
                    tempAmbiente, { tempAmbiente = it }, "T. ambiente (°C)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.width(8.dp))
                CampoTexto(
                    umidade, { umidade = it }, "Umidade (%)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Row(Modifier.fillMaxWidth()) {
                CampoTexto(
                    distancia, { distancia = it }, "Distância (m)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.width(8.dp))
                CampoTexto(
                    relacaoDS, { relacaoDS = it }, "Relação D:S", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.width(8.dp))
                CampoTexto(
                    angulo, { angulo = it }, "Ângulo (°)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            CampoMarcacao("Local da medição", listOf("Interno", "Externo"), local, {
                if (it.isNotBlank()) local = it
            })
            if (externo) {
                CampoTexto(
                    vento, { vento = it }, "Velocidade do vento (m/s)",
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // ---------------- Carga ----------------
            TituloT("Carga no momento da medição")
            Row(Modifier.fillMaxWidth()) {
                CampoTexto(
                    correnteMedida, { correnteMedida = it }, "Corrente medida (A)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.width(8.dp))
                CampoTexto(
                    correnteNominal, { correnteNominal = it }, "Corrente nominal (A)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            SeletorOpcoes(
                "Classe de isolamento (motores)",
                Termografia.CLASSES_ISOLAMENTO.map { it.first },
                classe,
                { classe = it },
            )

            // ---------------- Medições ----------------
            TituloT("Temperaturas medidas")
            Row(Modifier.fillMaxWidth()) {
                CampoTexto(
                    tempPonto, { tempPonto = it }, "Ponto quente (°C)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.width(8.dp))
                CampoTexto(
                    tempSimilar, { tempSimilar = it }, "Componente similar (°C)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Text(
                "O componente similar é a referência sob a mesma carga: a fase vizinha, o mancal do " +
                    "outro lado, a máquina gêmea. É o critério mais confiável de avaliação.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ---------------- Fotos ----------------
            TituloT("Registro fotográfico")
            SecaoFotosTermo(
                "Imagens térmicas", fotosTermicas, legendas,
                aoMudarFotos = { fotosTermicas = it },
                aoMudarLegenda = { caminho, texto -> legendas = legendas + (caminho to texto) },
            )
            SecaoFotosTermo(
                "Fotos visíveis (mesmo ponto)", fotosVisiveis, legendas,
                aoMudarFotos = { fotosVisiveis = it },
                aoMudarLegenda = { caminho, texto -> legendas = legendas + (caminho to texto) },
            )
            Text(
                "As imagens são normalizadas em 1080 × 900 para o laudo sair sempre com a mesma qualidade.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CampoTexto(observacoes, { observacoes = it }, "Observações da inspeção", linhas = 2)

            // ---------------- Resultados ----------------
            TituloT("Resultados")
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    LinhaValor(
                        "ΔT sobre componente similar",
                        resultado.deltaTSimilar?.let { "%.1f °C".format(it) } ?: "—",
                        detalhe = if (resultado.deltaTSimilar == null)
                            "Informe o ponto quente e o componente similar" else null,
                    )
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    LinhaValor(
                        "ΔT sobre o ambiente",
                        resultado.deltaTAmbiente?.let { "%.1f °C".format(it) } ?: "—",
                        detalhe = if (resultado.deltaTAmbiente == null)
                            "Informe o ponto quente e a temperatura ambiente" else null,
                    )
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    LinhaValor(
                        "ΔT projetado para a carga nominal",
                        resultado.deltaTCorrigidoCarga?.let { "%.1f °C".format(it) } ?: "—",
                        detalhe = resultado.percentualCarga?.let { "Medido com %.0f%% da carga nominal".format(it) }
                            ?: "Informe as correntes medida e nominal",
                        cor = resultado.percentualCarga?.let {
                            if (it < Termografia.CARGA_MINIMA_PERCENTUAL) LaranjaT else null
                        },
                    )
                    resultado.deltaTCorrigidoVento?.let {
                        HorizontalDivider(Modifier.padding(vertical = 10.dp))
                        LinhaValor("ΔT corrigido pelo vento", "%.1f °C".format(it))
                    }
                    resultado.temperaturaCorrigidaEmissividade?.let {
                        HorizontalDivider(Modifier.padding(vertical = 10.dp))
                        LinhaValor(
                            "Temperatura reestimada pela emissividade", "%.1f °C".format(it),
                            detalhe = "Valor aproximado — reconfigure a câmera e meça novamente",
                            cor = AzulT,
                        )
                    }
                    resultado.menorAlvoMm?.let {
                        HorizontalDivider(Modifier.padding(vertical = 10.dp))
                        Text(
                            "Menor alvo mensurável: %.0f mm • alvo recomendado a partir de %.0f mm".format(
                                it, resultado.alvoMinimoRecomendadoMm ?: 0.0
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    resultado.margemClasse?.let {
                        HorizontalDivider(Modifier.padding(vertical = 10.dp))
                        Text(
                            if (it > 0) "Faltam %.0f °C para o limite da %s".format(it, classe)
                            else "Temperatura %.0f °C ACIMA do limite da %s".format(-it, classe),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (it <= 0) VermelhoT else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ---------------- Diagnóstico ----------------
            TituloT("Diagnóstico automático")
            val severidade = resultado.severidade
            if (severidade == null) {
                Text(
                    "Informe a temperatura do ponto e ao menos uma referência (componente similar ou " +
                        "temperatura ambiente) para o diagnóstico automático.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val cor = corSeveridade(severidade)
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = 0.12f)),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            severidade.rotulo.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .background(cor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(resultado.diagnostico, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Ação: ${resultado.recomendacao}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
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
                else -> "Últimas inspeções registradas"
            }
            TituloT("$tituloHistorico (${historico.size})")
            HistoricoTermografia(historico, ponto) { excluir = it }

            // ---------------- Ações ----------------
            TituloT("Ações")
            if (jaSalvo) {
                Text(
                    "Inspeção salva como $numeroSalvo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Verde,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        salvarInspecao { salva ->
                            Toast.makeText(context, "Inspeção ${salva.numero} salva.", Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = severidade != null,
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Salvar inspeção")
                }
                Button(
                    onClick = {
                        salvarInspecao { salva ->
                            val cliente = clientes.find { it.id == salva.clienteId }
                            val equip = equipamentos.find { it.id == salva.equipamentoId }
                            val lista = (listOf(salva) + historico.filter { it.id != salva.id })
                                .sortedByDescending { it.dataHora }
                            val pdf = PdfGenerator.gerarInspecaoTermografica(
                                context, salva, cliente, equip, resultado, lista
                            )
                            Arquivos.compartilhar(context, pdf, "application/pdf", salva.numero)
                        }
                    },
                    enabled = severidade != null,
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Gerar relatório")
                }
            }
            Text(
                "O relatório salva a inspeção e gera o laudo em PDF com as condições do ensaio, os " +
                    "cálculos, o diagnóstico, as imagens e o histórico do ponto.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )

            // ---------------- Referência técnica ----------------
            TituloT("Referência técnica")
            TabRow(selectedTabIndex = aba, containerColor = MaterialTheme.colorScheme.surface) {
                ABAS_TERMO.forEachIndexed { indice, titulo ->
                    Tab(
                        selected = aba == indice,
                        onClick = { aba = indice },
                        text = { Text(titulo, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            when (aba) {
                0 -> AbaComoMedir()
                1 -> AbaEmissividade()
                2 -> AbaCriterios()
                else -> AbaAplicacoes()
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    excluir?.let { inspecao ->
        ConfirmarExclusao(
            "Excluir a inspeção ${inspecao.numero.ifBlank { "selecionada" }} do histórico?",
            aoConfirmar = {
                vm.excluir(inspecao)
                if (inspecao.id == idSalvo) { idSalvo = 0L; numeroSalvo = ""; chaveSalva = "" }
                excluir = null
            },
            aoCancelar = { excluir = null },
        )
    }
}

/** Fotos da termografia: normalizadas em 1080 × 900 e com legenda por imagem. */
@Composable
private fun SecaoFotosTermo(
    titulo: String,
    fotos: List<String>,
    legendas: Map<String, String>,
    aoMudarFotos: (List<String>) -> Unit,
    aoMudarLegenda: (String, String) -> Unit,
) {
    val context = LocalContext.current
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val novas = uris.mapNotNull { Arquivos.copiarImagemTermografica(context, it) }
        if (novas.isNotEmpty()) aoMudarFotos(fotos + novas)
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(titulo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { galeria.launch("image/*") }, modifier = Modifier.padding(top = 6.dp)) {
            Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Adicionar da galeria")
        }
        fotos.forEach { caminho ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Box {
                    AsyncImage(
                        model = File(caminho),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(84.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    )
                    IconButton(
                        onClick = { aoMudarFotos(fotos - caminho) },
                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                            .background(MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Close, "Remover", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = legendas[caminho].orEmpty(),
                    onValueChange = { aoMudarLegenda(caminho, it) },
                    label = { Text("Legenda da imagem") },
                    modifier = Modifier.weight(1f),
                    minLines = 2,
                )
            }
        }
    }
}

/** Histórico das inspeções, destacando as do mesmo ponto. */
@Composable
private fun HistoricoTermografia(
    historico: List<InspecaoTermografica>,
    pontoAtual: String,
    aoExcluir: (InspecaoTermografica) -> Unit,
) {
    if (historico.isEmpty()) {
        Text(
            "Nenhuma inspeção salva ainda. Preencha as medições e toque em \"Salvar inspeção\" para " +
                "começar o acompanhamento deste ponto.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val formato = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }

    // Evolução do ΔT do mesmo ponto (o que importa para a tendência)
    val doPonto = historico.filter { pontoAtual.isNotBlank() && it.ponto.equals(pontoAtual, ignoreCase = true) }
    if (doPonto.size >= 2) {
        val recente = doPonto.first().deltaTCorrigido ?: doPonto.first().deltaTSimilar
        val antigo = doPonto.last().deltaTCorrigido ?: doPonto.last().deltaTSimilar
        if (recente != null && antigo != null) {
            val subiu = recente - antigo
            val cor = if (subiu > 3) VermelhoT else if (subiu > 0) AmareloT else Verde
            Card(
                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = 0.12f)),
            ) {
                Text(
                    when {
                        subiu > 3 -> "Atenção: o ΔT deste ponto subiu %.1f °C desde a primeira inspeção (%.1f → %.1f °C). Degradação em curso.".format(subiu, antigo, recente)
                        subiu > 0 -> "O ΔT deste ponto subiu %.1f °C desde a primeira inspeção (%.1f → %.1f °C). Acompanhe.".format(subiu, antigo, recente)
                        else -> "O ΔT deste ponto caiu %.1f °C desde a primeira inspeção (%.1f → %.1f °C).".format(-subiu, antigo, recente)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }
    }

    historico.forEach { inspecao ->
        Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${formato.format(Date(inspecao.dataHora))}  •  ${inspecao.ponto.ifBlank { "sem ponto" }}",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    val dados = listOfNotNull(
                        inspecao.tempPonto?.let { "%.1f °C".format(it) },
                        inspecao.deltaTCorrigido?.let { "ΔT %.1f °C".format(it) },
                        inspecao.severidade.takeIf { it.isNotBlank() },
                        inspecao.numero.takeIf { it.isNotBlank() },
                    ).joinToString(" • ")
                    Text(
                        dados,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    Modifier.size(12.dp)
                        .background(corSeveridadeTexto(inspecao.severidade), RoundedCornerShape(6.dp))
                )
                IconButton(onClick = { aoExcluir(inspecao) }) {
                    Icon(Icons.Default.Delete, "Excluir inspeção", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ---------------- Abas de referência ----------------

@Composable
private fun AbaComoMedir() {
    Column {
        CardTextoT("Passo a passo da medição") {
            Termografia.PASSO_A_PASSO.forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
        CardTextoT("Erros comuns") {
            Termografia.ERROS_COMUNS.forEachIndexed { indice, (erro, explicacao) ->
                if (indice > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp))
                Text(erro, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    explicacao,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        CardTextoT("Por que a carga importa") {
            Text(Termografia.TEXTO_CARGA, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AbaEmissividade() {
    Column {
        CardTextoT("O que é emissividade") {
            Text(Termografia.TEXTO_EMISSIVIDADE, style = MaterialTheme.typography.bodyMedium)
        }
        Termografia.GRUPOS.forEach { grupo ->
            CardTextoT(grupo) {
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    Text("Material", Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("ε", Modifier.weight(0.5f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("Faixa", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Termografia.MATERIAIS.filter { it.grupo == grupo }.forEach { m ->
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Text(m.nome, Modifier.weight(1.5f), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "%.2f".format(m.emissividade), Modifier.weight(0.5f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (m.emissividade < 0.6) LaranjaT else Verde,
                        )
                        Text(
                            m.faixa, Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AbaCriterios() {
    Column {
        CardTextoT("Faixas de severidade (NETA MTS / NFPA 70B)") {
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Text("ΔT similar", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("ΔT ambiente", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Classificação", Modifier.weight(1.6f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Termografia.CRITERIOS.forEach { (similar, ambiente, classificacao) ->
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(similar, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text(ambiente, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text(classificacao, Modifier.weight(1.6f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        CardTextoT("Leitura do padrão térmico") {
            Text(
                "Ponto quente isolado em uma fase indica conexão frouxa, oxidada ou subdimensionada.\n\n" +
                    "As três fases igualmente quentes indicam sobrecarga do circuito — trocar o parafuso " +
                    "não resolve.\n\n" +
                    "Aquecimento que cresce em direção à carga sugere problema a jusante; aquecimento no " +
                    "próprio corpo do dispositivo aponta falha interna.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        CardTextoT("Classes de isolamento") {
            Termografia.CLASSES_ISOLAMENTO.drop(1).forEach { (rotulo, limite) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(rotulo, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "limite %.0f °C".format(limite), Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AbaAplicacoes() {
    Column {
        Termografia.APLICACOES.forEach { aplicacao ->
            CardTextoT(aplicacao.equipamento) {
                Text("O que olhar: ${aplicacao.oQueOlhar}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Sintoma: ${aplicacao.sintoma}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------- Auxiliares ----------------

@Composable
private fun TituloT(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun LinhaValor(rotulo: String, valor: String, detalhe: String? = null, cor: Color? = null) {
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
private fun CardTextoT(titulo: String, conteudo: @Composable () -> Unit) {
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

private fun rotuloEquipT(tipo: String, marca: String, modelo: String): String =
    listOf(tipo, marca, modelo).filter { it.isNotBlank() }.joinToString(" ")

private fun corSeveridade(severidade: Termografia.Severidade) = when (severidade) {
    Termografia.Severidade.NORMAL -> Verde
    Termografia.Severidade.INVESTIGAR -> AmareloT
    Termografia.Severidade.DEFICIENCIA -> LaranjaT
    Termografia.Severidade.GRAVE -> LaranjaT
    Termografia.Severidade.CRITICO -> VermelhoT
}

private fun corSeveridadeTexto(rotulo: String): Color =
    Termografia.Severidade.entries.firstOrNull { it.rotulo == rotulo }?.let { corSeveridade(it) }
        ?: Color(0xFF9E9E9E)
