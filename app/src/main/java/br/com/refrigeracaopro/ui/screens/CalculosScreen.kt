package br.com.refrigeracaopro.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.Avisos
import br.com.refrigeracaopro.data.Capacitores
import br.com.refrigeracaopro.data.CargaFluido
import br.com.refrigeracaopro.data.PTTable
import br.com.refrigeracaopro.ui.components.AvisoCard
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.SeletorOpcoes
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.theme.Verde

/** Calculadora de refrigeração: superaquecimento, subresfriamento, capacitor e carga de fluido. */
@Composable
fun CalculosScreen(nav: NavController) {
    var aba by remember { mutableStateOf(0) }
    val abas = listOf("Superaquecimento", "Subresfriamento", "Capacitor", "Carga de fluido")

    TelaBase(nav, "Cálculos de refrigeração") { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = aba, edgePadding = 0.dp) {
                abas.forEachIndexed { i, titulo ->
                    Tab(selected = aba == i, onClick = { aba = i }, text = { Text(titulo) })
                }
            }
            when (aba) {
                0 -> AbaSuperaquecimento()
                1 -> AbaSubresfriamento()
                2 -> AbaCapacitor()
                else -> AbaCargaFluido()
            }
        }
    }
}

@Composable
private fun AbaSuperaquecimento() {
    var fluido by remember { mutableStateOf(PTTable.NOMES.first()) }
    var pressao by remember { mutableStateOf("") }
    var tempSuccao by remember { mutableStateOf("") }

    val resultado = remember(fluido, pressao, tempSuccao) {
        val f = PTTable.porNome(fluido) ?: return@remember null
        val p = pressao.replace(",", ".").toDoubleOrNull() ?: return@remember null
        val t = tempSuccao.replace(",", ".").toDoubleOrNull() ?: return@remember null
        PTTable.superaquecimento(f, p, t)
    }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Superaquecimento = T. linha de sucção − T. evaporação saturada",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        SeletorOpcoes("Fluido refrigerante", PTTable.NOMES, fluido, { fluido = it })
        CampoTexto(pressao, { pressao = it }, "Pressão de sucção (bar manométrico)",
            teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
        CampoTexto(tempSuccao, { tempSuccao = it }, "Temperatura na linha de sucção (°C)",
            teclado = KeyboardOptions(keyboardType = KeyboardType.Number))

        resultado?.let { r -> CardResultado("Superaquecimento", r) }
    }
}

@Composable
private fun AbaSubresfriamento() {
    var fluido by remember { mutableStateOf(PTTable.NOMES.first()) }
    var pressao by remember { mutableStateOf("") }
    var tempLiquido by remember { mutableStateOf("") }

    val resultado = remember(fluido, pressao, tempLiquido) {
        val f = PTTable.porNome(fluido) ?: return@remember null
        val p = pressao.replace(",", ".").toDoubleOrNull() ?: return@remember null
        val t = tempLiquido.replace(",", ".").toDoubleOrNull() ?: return@remember null
        PTTable.subresfriamento(f, p, t)
    }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Subresfriamento = T. condensação saturada − T. linha de líquido",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        SeletorOpcoes("Fluido refrigerante", PTTable.NOMES, fluido, { fluido = it })
        CampoTexto(pressao, { pressao = it }, "Pressão de descarga/líquido (bar manométrico)",
            teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
        CampoTexto(tempLiquido, { tempLiquido = it }, "Temperatura na linha de líquido (°C)",
            teclado = KeyboardOptions(keyboardType = KeyboardType.Number))

        resultado?.let { r -> CardResultado("Subresfriamento", r) }
    }
}

@Composable
private fun CardResultado(titulo: String, r: PTTable.ResultadoCalculo) {
    val cor = when (r.interpretacao) {
        "NORMAL" -> Verde
        else -> MaterialTheme.colorScheme.error
    }
    Card(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Temperatura de saturação: %.1f °C".format(r.tempSaturacao),
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("$titulo: %.1f K".format(r.valor),
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Interpretação: ${r.interpretacao}",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cor)
            Spacer(Modifier.height(8.dp))
            Text("Possíveis causas:", fontWeight = FontWeight.SemiBold)
            Text(r.causas, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ----- Capacitor -----
@Composable
private fun AbaCapacitor() {
    var potencia by remember { mutableStateOf("") }
    var unidade by remember { mutableStateOf(Capacitores.UnidadePotencia.CV) }
    var tensao by remember { mutableStateOf("220") }
    var corrente by remember { mutableStateOf("") }
    var frequencia by remember { mutableStateOf("60") }
    var aplicacao by remember { mutableStateOf(Capacitores.Aplicacao.COMPRESSOR) }

    val resultado = remember(potencia, unidade, tensao, corrente, frequencia, aplicacao) {
        val p = potencia.replace(",", ".").toDoubleOrNull() ?: return@remember null
        val v = tensao.replace(",", ".").toDoubleOrNull() ?: return@remember null
        if (p <= 0 || v <= 0) return@remember null
        Capacitores.calcular(
            Capacitores.Entrada(
                potencia = p, unidade = unidade, tensao = v,
                frequencia = frequencia.replace(",", ".").toDoubleOrNull() ?: 60.0,
                correnteInformada = corrente.replace(",", ".").toDoubleOrNull(),
                aplicacao = aplicacao,
            )
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Estimativa de capacitor permanente e de partida para motores monofásicos.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        CampoTexto(potencia, { potencia = it }, "Potência do motor",
            teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
        SeletorOpcoes("Unidade de potência", Capacitores.UnidadePotencia.entries.map { it.rotulo },
            unidade.rotulo, { sel -> unidade = Capacitores.UnidadePotencia.entries.first { it.rotulo == sel } })
        CampoTexto(tensao, { tensao = it }, "Tensão (V)",
            teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
        CampoTexto(corrente, { corrente = it }, "Corrente nominal (A) — opcional",
            teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
        CampoTexto(frequencia, { frequencia = it }, "Frequência (Hz)",
            teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
        SeletorOpcoes("Aplicação", Capacitores.Aplicacao.entries.map { it.rotulo },
            aplicacao.rotulo, { sel -> aplicacao = Capacitores.Aplicacao.entries.first { it.rotulo == sel } })

        resultado?.let { r ->
            Card(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Capacitor permanente: ≈ ${r.capacitorPermanenteUf} µF",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Verde)
                    Text("Capacitor de partida: ≈ ${r.capacitorPartidaMinUf}–${r.capacitorPartidaMaxUf} µF",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Tensão mínima do capacitor: ${r.tensaoMinimaCapacitor} VAC")
                    Text("Corrente considerada: %.1f A".format(r.correnteEstimada),
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    r.observacoes.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        AvisoCard(Avisos.CALCULO_AUXILIAR)
    }
}

// ----- Carga de fluido -----
@Composable
private fun AbaCargaFluido() {
    var fluido by remember { mutableStateOf(PTTable.NOMES.first()) }
    var cargaNominal by remember { mutableStateOf("") }
    var comprimento by remember { mutableStateOf("") }
    var diametro by remember { mutableStateOf(CargaFluido.DIAMETROS_LIQUIDO[2]) }
    var pesoInicial by remember { mutableStateOf("") }
    var pesoFinal by remember { mutableStateOf("") }

    val resultado = remember(fluido, cargaNominal, comprimento, diametro) {
        val carga = cargaNominal.replace(",", ".").toDoubleOrNull() ?: return@remember null
        val comp = comprimento.replace(",", ".").toDoubleOrNull() ?: return@remember null
        CargaFluido.calcular(
            CargaFluido.Entrada(
                fluido = fluido, cargaNominalGramas = carga,
                comprimentoTotalM = comp, diametroLiquido = diametro,
            )
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Estimativa da carga adicional pelo comprimento extra da linha de líquido.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SeletorOpcoes("Fluido", PTTable.NOMES, fluido, { fluido = it })
        CampoTexto(cargaNominal, { cargaNominal = it }, "Carga nominal de etiqueta (g)",
            teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
        CampoTexto(comprimento, { comprimento = it }, "Comprimento total da linha de líquido (m)",
            teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
        SeletorOpcoes("Diâmetro da linha de líquido", CargaFluido.DIAMETROS_LIQUIDO.map { it.polegada },
            diametro.polegada, { sel -> diametro = CargaFluido.DIAMETROS_LIQUIDO.first { it.polegada == sel } })

        Text("Carga por balança (registro):", style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 8.dp))
        androidx.compose.foundation.layout.Row {
            CampoTexto(pesoInicial, { pesoInicial = it }, "Peso inicial cilindro (g)",
                modifier = Modifier.weight(1f).padding(end = 4.dp),
                teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
            CampoTexto(pesoFinal, { pesoFinal = it }, "Peso final cilindro (g)",
                modifier = Modifier.weight(1f).padding(start = 4.dp),
                teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        val retirado = run {
            val pi = pesoInicial.replace(",", ".").toDoubleOrNull()
            val pf = pesoFinal.replace(",", ".").toDoubleOrNull()
            if (pi != null && pf != null) pi - pf else null
        }
        if (retirado != null) {
            Text("Fluido transferido (balança): %.0f g".format(retirado),
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Verde)
        }

        resultado?.let { r ->
            Card(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Carga adicional estimada: %.0f g".format(r.cargaAdicionalGramas),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Carga total estimada: %.0f g".format(r.cargaTotalGramas),
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Verde)
                    Spacer(Modifier.height(8.dp))
                    r.orientacoes.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        AvisoCard(Avisos.CALCULO_AUXILIAR)
    }
}
