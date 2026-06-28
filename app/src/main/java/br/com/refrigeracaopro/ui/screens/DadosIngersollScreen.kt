package br.com.refrigeracaopro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.DadosIngersoll
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.theme.Verde

/**
 * Dashboard Ingersoll Rand: busca a máquina e abre os consumíveis de
 * manutenção (filtros, separador, óleo, kits) com os intervalos de troca
 * em horas. Sem informações de valores.
 */
@Composable
fun DadosIngersollScreen(nav: NavController) {
    val context = LocalContext.current
    val todas = remember { DadosIngersoll.carregar(context) }
    var consulta by remember { mutableStateOf("") }
    var selecionada by remember { mutableStateOf<DadosIngersoll.Maquina?>(null) }

    val resultados = remember(consulta, todas) { DadosIngersoll.buscar(todas, consulta) }

    TelaBase(nav, "Ingersoll Rand") { padding ->
        val maquina = selecionada
        if (maquina == null) {
            Column(Modifier.fillMaxSize().padding(padding)) {
                OutlinedTextField(
                    value = consulta,
                    onValueChange = { consulta = it },
                    label = { Text("Buscar máquina (ex.: UP6, R55, Small Rotary)") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
                Text(
                    "${resultados.size} de ${todas.size} máquinas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(resultados) { m ->
                        Card(
                            onClick = { selecionada = m },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(m.nome, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                val det = listOf(m.frame, m.frameTipo).filter { it.isNotBlank() }.joinToString("  •  ")
                                if (det.isNotBlank()) {
                                    Text(det, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${m.consumiveis.size} consumíveis",
                                    style = MaterialTheme.typography.labelSmall, color = Verde,
                                    modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        } else {
            DetalheIngersoll(maquina, padding) { selecionada = null }
        }
    }
}

@Composable
private fun DetalheIngersoll(
    m: DadosIngersoll.Maquina,
    padding: androidx.compose.foundation.layout.PaddingValues,
    aoVoltar: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
    ) {
        TextButton(onClick = aoVoltar, modifier = Modifier.padding(top = 4.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp)); Text("Voltar para a busca")
        }

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(m.nome, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Verde)
                if (m.frame.isNotBlank()) Text(m.frame, style = MaterialTheme.typography.bodyMedium)
                if (m.frameTipo.isNotBlank()) Text(m.frameTipo, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (m.codigo.isNotBlank()) Text("Código: ${m.codigo}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Text("Consumíveis de manutenção", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp, bottom = 2.dp))
        Text("Intervalos em milhares de horas (ex.: 8K = 8.000 h).",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        m.consumiveis.forEach { c -> ItemConsumivel(c) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ItemConsumivel(c: DadosIngersoll.Consumivel) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(c.consumavel.ifBlank { c.descricao }, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (c.ccn.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Text(c.ccn.uppercase(), style = MaterialTheme.typography.bodyMedium, color = Verde, fontWeight = FontWeight.Bold)
            }
        }
        if (c.descricao.isNotBlank() && c.descricao != c.consumavel) {
            Text(c.descricao, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.padding(top = 2.dp)) {
            if (c.qtd.isNotBlank()) {
                Text("Qtd: ${c.qtd}", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
            }
        }
        if (c.intervalos.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                c.intervalos.forEach { iv ->
                    AssistChip(
                        onClick = {}, enabled = false,
                        label = { Text(iv, style = MaterialTheme.typography.labelMedium) },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledLabelColor = Verde,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    )
                }
            }
        }
        HorizontalDivider(Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}
