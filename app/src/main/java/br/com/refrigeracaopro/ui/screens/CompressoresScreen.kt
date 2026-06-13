package br.com.refrigeracaopro.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.Compressor
import br.com.refrigeracaopro.data.Compressores
import br.com.refrigeracaopro.ui.components.AvisoCard
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.theme.Verde

/**
 * Pesquisa de motores/compressores e sugestão de equivalentes, destacando
 * diferenças críticas (fluido, óleo, tensão, fase, capacidade, aplicação).
 */
@Composable
fun CompressoresScreen(nav: NavController) {
    var busca by remember { mutableStateOf("") }
    var selecionado by remember { mutableStateOf<Compressor?>(null) }
    val lista = Compressores.pesquisar(busca)

    TelaBase(nav, "Motores e compressores") { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = busca,
                onValueChange = { busca = it; selecionado = null },
                label = { Text("Pesquisar por modelo, marca ou fluido") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            AvisoCard(
                "Substituição direta exige fluido, óleo, tensão/fase, aplicação e capacidade compatíveis. " +
                    "Confirme sempre no catálogo do fabricante.",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            val atual = selecionado
            if (atual == null) {
                LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                    items(lista, key = { it.modelo }) { c ->
                        Card(
                            onClick = { selecionado = c },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("${c.marca} ${c.modelo}", fontWeight = FontWeight.Bold)
                                Text(
                                    "${c.potenciaHp} HP • ${c.fluido} • ${c.aplicacao} • ${c.tensao} ${c.fase}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                DetalheCompressor(atual, aoVoltar = { selecionado = null })
            }
        }
    }
}

@Composable
private fun DetalheCompressor(base: Compressor, aoVoltar: () -> Unit) {
    val equivalentes = remember(base.modelo) { Compressores.equivalentes(base) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("${base.marca} ${base.modelo}", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            FichaCompressor(base)
            Text("Toque novamente em outro modelo na lista para trocar.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SwapHoriz, null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                Text(" Equivalentes sugeridos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
        }
        items(equivalentes, key = { it.compressor.modelo }) { eq ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (eq.substituicaoDireta) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                            null,
                            tint = if (eq.substituicaoDireta) Verde else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "  ${eq.compressor.marca} ${eq.compressor.modelo}",
                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        if (eq.substituicaoDireta) "Possível substituição direta (confirme no catálogo)"
                        else "NÃO é substituição direta — diferenças críticas:",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (eq.substituicaoDireta) Verde else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    eq.diferencas.forEach { d ->
                        Text("• $d", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp))
                    }
                    AnimatedVisibility(true) { FichaCompressor(eq.compressor) }
                }
            }
        }
    }
}

@Composable
private fun FichaCompressor(c: Compressor) {
    Column(Modifier.padding(top = 4.dp)) {
        linha("Potência", "${c.potenciaHp} HP")
        linha("Tensão / Fase / Freq.", "${c.tensao} • ${c.fase} • ${c.frequencia}")
        linha("Corrente nominal", c.correnteNominal)
        linha("Fluido", c.fluido)
        linha("Aplicação", c.aplicacao)
        linha("Capacidade frigorífica", c.capacidadeFrigorifica)
        linha("Óleo", c.tipoOleo)
        linha("Partida", c.tipoPartida)
        linha("Conexão", c.tipoConexao)
        linha("Faixa de evaporação", c.faixaEvaporacao)
        if (c.observacoes.isNotBlank()) linha("Observações", c.observacoes)
    }
}

@Composable
private fun linha(rotulo: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text("$rotulo: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Text(valor, style = MaterialTheme.typography.bodySmall)
    }
}
