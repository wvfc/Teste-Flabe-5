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
import androidx.compose.material3.TabRow
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
import br.com.refrigeracaopro.data.PTTable
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.SeletorOpcoes
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.theme.Verde

/** Calculadora de superaquecimento e subresfriamento. */
@Composable
fun CalculosScreen(nav: NavController) {
    var aba by remember { mutableStateOf(0) }

    TelaBase(nav, "Cálculos de refrigeração") { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = aba) {
                Tab(selected = aba == 0, onClick = { aba = 0 }, text = { Text("Superaquecimento") })
                Tab(selected = aba == 1, onClick = { aba = 1 }, text = { Text("Subresfriamento") })
            }
            if (aba == 0) AbaSuperaquecimento() else AbaSubresfriamento()
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
