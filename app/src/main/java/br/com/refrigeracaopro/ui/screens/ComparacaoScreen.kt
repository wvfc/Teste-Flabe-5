package br.com.refrigeracaopro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.Componente
import br.com.refrigeracaopro.data.Componentes
import br.com.refrigeracaopro.ui.components.TelaBase

/**
 * Comparação de marcas/modelos de componentes (compressores, condensadoras,
 * evaporadores, ventiladores, controladores, válvulas, pressostatos, filtros).
 * Permite selecionar até 2 itens e compará-los lado a lado.
 */
@Composable
fun ComparacaoScreen(nav: NavController) {
    var categoria by remember { mutableStateOf<String?>(null) }
    var busca by remember { mutableStateOf("") }
    val selecionados = remember { mutableStateListOf<Componente>() }
    val lista = Componentes.pesquisar(categoria, busca)

    TelaBase(nav, "Comparar componentes") { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = busca,
                onValueChange = { busca = it },
                label = { Text("Pesquisar por modelo, marca ou aplicação") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(selected = categoria == null, onClick = { categoria = null }, label = { Text("Todos") })
                }
                items(Componentes.CATEGORIAS) { cat ->
                    FilterChip(selected = categoria == cat, onClick = { categoria = cat }, label = { Text(cat) })
                }
            }

            if (selecionados.isNotEmpty()) {
                TabelaComparacao(selecionados) { selecionados.clear() }
            }

            Text(
                "Selecione até 2 itens para comparar (${selecionados.size}/2):",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(lista, key = { it.categoria + it.modelo }) { comp ->
                    val marcado = selecionados.contains(comp)
                    Card(
                        onClick = {
                            if (marcado) selecionados.remove(comp)
                            else if (selecionados.size < 2) selecionados.add(comp)
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = if (marcado) CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) else CardDefaults.cardColors()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${comp.marca} ${comp.modelo}", fontWeight = FontWeight.Bold)
                            Text("${comp.categoria} • ${comp.aplicacao}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabelaComparacao(itens: List<Componente>, aoLimpar: () -> Unit) {
    val campos: List<Pair<String, (Componente) -> String>> = listOf(
        "Marca" to { it.marca },
        "Modelo" to { it.modelo },
        "Aplicação" to { it.aplicacao },
        "Capacidade" to { it.capacidade },
        "Fluido" to { it.fluido },
        "Tensão" to { it.tensao },
        "Consumo" to { it.consumo },
        "Dimensões" to { it.dimensoes },
        "Conexões" to { it.conexoes },
        "Equivalentes" to { it.equivalentes },
        "Vantagens" to { it.vantagens },
        "Limitações" to { it.limitacoes },
        "Observações" to { it.observacoes },
    )
    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Comparação", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Limpar", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 8.dp).clickable { aoLimpar() })
            }
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            Box(Modifier.horizontalScroll(rememberScrollState())) {
                Column {
                    campos.forEach { (rotulo, extrair) ->
                        Row {
                            Text(rotulo, modifier = Modifier.width(110.dp), fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall)
                            itens.forEach { comp ->
                                Text(extrair(comp).ifBlank { "—" },
                                    modifier = Modifier.width(170.dp).padding(end = 8.dp),
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}
