package br.com.refrigeracaopro.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.EnsaioMca
import br.com.refrigeracaopro.data.Mca
import br.com.refrigeracaopro.data.McaExport
import br.com.refrigeracaopro.data.Motor
import br.com.refrigeracaopro.pdf.PdfGenerator
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.util.Arquivos
import br.com.refrigeracaopro.viewmodel.McaViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CORES_PAR = listOf(Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFFEF6C00))

/** Parecer do ensaio: semáforo por índice, achados, gráfico do RIC e exportações. */
@Composable
fun McaParecerScreen(nav: NavController, ensaioId: Long, vm: McaViewModel = viewModel()) {
    val context = LocalContext.current
    val limites by vm.limites.collectAsState()

    var ensaio by remember { mutableStateOf<EnsaioMca?>(null) }
    var motor by remember { mutableStateOf<Motor?>(null) }
    var baseline by remember { mutableStateOf<Mca.Baseline?>(null) }
    var anteriores by remember { mutableStateOf<List<EnsaioMca>>(emptyList()) }

    LaunchedEffect(ensaioId) {
        val e = vm.buscarEnsaio(ensaioId) ?: return@LaunchedEffect
        ensaio = e
        motor = vm.buscarMotor(e.motorId)
        baseline = vm.baselineDe(e)
        anteriores = vm.concluidosDoMotor(e.motorId).filter { it.dataHora < e.dataHora }
    }

    val atual = ensaio
    val formato = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }

    TelaBase(nav, "Parecer do ensaio") { padding ->
        if (atual == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("Carregando o ensaio…", style = MaterialTheme.typography.bodyMedium)
            }
            return@TelaBase
        }

        val leituras = remember(atual) { vm.leiturasDe(atual) }
        val parecer = remember(atual, limites, baseline) { Mca.avaliar(leituras, limites, baseline) }
        val indices = parecer.indices

        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "${atual.numero} • ${formato.format(Date(atual.dataHora))}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp),
            )
            motor?.let { m ->
                Text(
                    listOf(m.tag, m.cliente, m.setor, m.equipamentoAcionado)
                        .filter { it.isNotBlank() }.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ---------------- Semáforo ----------------
            TituloMca("Índices")
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Mca.semaforo(indices, limites).forEachIndexed { indice, linha ->
                        if (indice > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(12.dp)
                                    .background(corSeveridadeMca(linha.severidade), RoundedCornerShape(6.dp))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(linha.nome, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    linha.limiteTexto,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                linha.valorTexto,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = corSeveridadeMca(linha.severidade),
                            )
                        }
                    }
                }
            }

            // ---------------- Achados ----------------
            TituloMca("Achados")
            parecer.achados.forEach { achado ->
                val cor = corSeveridadeMca(achado.severidade)
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = 0.12f)),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            achado.severidade.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.background(cor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(achado.titulo, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        if (achado.evidencia.isNotBlank()) {
                            Text(
                                achado.evidencia,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(achado.detalhe, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (parecer.naoAvaliados.isNotEmpty()) {
                Text(
                    "Não avaliados: " + parecer.naoAvaliados.joinToString("; "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ---------------- Gráfico do RIC ----------------
            TituloMca("RIC — indutância × ângulo")
            GraficoRic(leituras.ric)

            // ---------------- Comparação com o histórico ----------------
            if (anteriores.isNotEmpty()) {
                TituloMca("Comparação com o histórico")
                val anterior = anteriores.maxByOrNull { it.dataHora }
                val primeiro = anteriores.minByOrNull { it.dataHora }
                val indicesAnterior = anterior?.let { Mca.calcularIndices(vm.leiturasDe(it)) }
                val indicesPrimeiro = primeiro?.let { Mca.calcularIndices(vm.leiturasDe(it)) }
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                            Text("Índice", Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("vs. anterior", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("vs. baseline", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        McaExport.indicesEmLinhas(indices).forEachIndexed { posicao, (nome, valor) ->
                            val antes = indicesAnterior?.let { McaExport.indicesEmLinhas(it)[posicao].second }
                            val base = indicesPrimeiro?.let { McaExport.indicesEmLinhas(it)[posicao].second }
                            if (valor != null && (antes != null || base != null)) {
                                HorizontalDivider()
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text(nome, Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall)
                                    Text(variacao(valor, antes), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    Text(variacao(valor, base), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        Text(
                            "Tendência de degradação vale como alerta mesmo com os valores absolutos " +
                                "ainda dentro do limite.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            // ---------------- Exportações ----------------
            TituloMca("Exportar")
            val cabecalho = remember(atual, motor) { cabecalhoDe(atual, motor, formato) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val pdf = PdfGenerator.gerarEnsaioMca(
                            context, atual, motor, leituras, parecer, limites,
                        )
                        Arquivos.compartilhar(context, pdf, "application/pdf", atual.numero)
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("PDF")
                }
                OutlinedButton(
                    onClick = {
                        val arquivo = File(Arquivos.pastaPdfs(context), "${atual.numero}.csv")
                        arquivo.writeText(McaExport.paraCsv(cabecalho, leituras, parecer))
                        Arquivos.compartilhar(context, arquivo, "text/csv", "${atual.numero} (CSV)")
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text("CSV") }
                OutlinedButton(
                    onClick = {
                        val arquivo = File(Arquivos.pastaPdfs(context), "${atual.numero}.json")
                        arquivo.writeText(McaExport.paraJson(cabecalho, leituras, parecer))
                        Arquivos.compartilhar(context, arquivo, "application/json", "${atual.numero} (JSON)")
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("JSON")
                }
            }
            if (atual.rascunho) {
                Text(
                    "Este ensaio ainda está marcado como rascunho.",
                    style = MaterialTheme.typography.bodySmall,
                    color = McaAmarelo,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Gráfico do RIC: três curvas de indutância sobrepostas ao longo do ângulo. */
@Composable
fun GraficoRic(ric: Map<Mca.Par, List<Double?>>) {
    val series = Mca.Par.entries.mapNotNull { par ->
        ric[par]?.takeIf { serie -> serie.count { it != null } >= 2 }?.let { par to it }
    }
    if (series.isEmpty()) {
        Text(
            "Sem leituras de RIC suficientes para o gráfico.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val todos = series.flatMap { it.second }.filterNotNull()
    val minimo = todos.min()
    val maximo = todos.max()
    val faixa = (maximo - minimo).takeIf { it > 0 } ?: 1.0
    val corEixo = MaterialTheme.colorScheme.outline

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(14.dp)) {
            Canvas(Modifier.fillMaxWidth().height(200.dp)) {
                val margem = 24f
                val largura = size.width - margem * 2
                val altura = size.height - margem * 2
                // eixos
                drawLine(corEixo, Offset(margem, margem), Offset(margem, margem + altura), strokeWidth = 2f)
                drawLine(corEixo, Offset(margem, margem + altura), Offset(margem + largura, margem + altura), strokeWidth = 2f)

                series.forEachIndexed { indice, (_, serie) ->
                    val cor = CORES_PAR[indice % CORES_PAR.size]
                    val pontos = serie.mapIndexedNotNull { posicao, valor ->
                        valor?.let {
                            val x = margem + largura * posicao / (Mca.RIC_POSICOES - 1).toFloat()
                            val y = margem + altura - (altura * ((it - minimo) / faixa)).toFloat()
                            Offset(x, y)
                        }
                    }
                    if (pontos.size >= 2) {
                        val caminho = Path().apply {
                            moveTo(pontos.first().x, pontos.first().y)
                            pontos.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(caminho, cor, style = Stroke(width = 3f))
                    }
                    pontos.forEach { drawCircle(cor, radius = 4f, center = it) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                series.forEachIndexed { indice, (par, _) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(10.dp)
                                .background(CORES_PAR[indice % CORES_PAR.size], RoundedCornerShape(5.dp))
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(par.rotulo, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text(
                "0° a 330° • L entre %.2f e %.2f mH".format(minimo, maximo),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------- Auxiliares ----------------

@Composable
private fun TituloMca(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

private fun variacao(atual: Double, referencia: Double?): String {
    if (referencia == null) return "—"
    if (referencia == 0.0) return "—"
    val delta = (atual - referencia) / kotlin.math.abs(referencia) * 100.0
    return (if (delta >= 0) "+" else "") + "%.1f%%".format(delta)
}

internal fun cabecalhoDe(
    ensaio: EnsaioMca,
    motor: Motor?,
    formato: SimpleDateFormat,
): List<Pair<String, String>> = listOf(
    "Ensaio" to ensaio.numero,
    "Data" to formato.format(Date(ensaio.dataHora)),
    "Técnico" to ensaio.tecnico,
    "Tag do motor" to (motor?.tag ?: ""),
    "Cliente" to (motor?.cliente ?: ""),
    "Setor" to (motor?.setor ?: ""),
    "Equipamento acionado" to (motor?.equipamentoAcionado ?: ""),
    "Fabricante/Modelo" to listOfNotNull(
        motor?.fabricante?.takeIf { it.isNotBlank() },
        motor?.modelo?.takeIf { it.isNotBlank() },
    ).joinToString(" / "),
    "Temperatura da carcaça (°C)" to (ensaio.temperaturaCarcacaC?.let { "%.1f".format(it) } ?: ""),
    "Umidade relativa (%)" to (ensaio.umidadeRelativa?.let { "%.0f".format(it) } ?: ""),
)
