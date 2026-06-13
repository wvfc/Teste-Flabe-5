package br.com.refrigeracaopro.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
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
import br.com.refrigeracaopro.data.GasRefrigerante
import br.com.refrigeracaopro.data.GasesRefrigerantes
import br.com.refrigeracaopro.ui.components.TelaBase

/** Consulta técnica de gases refrigerantes (referência geral). */
@Composable
fun GasesScreen(nav: NavController) {
    var busca by remember { mutableStateOf("") }
    val lista = GasesRefrigerantes.LISTA.filter {
        busca.isBlank() || it.nome.contains(busca, true) || it.tipo.contains(busca, true) ||
            it.aplicacao.contains(busca, true)
    }

    TelaBase(nav, "Consulta de gases") { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = busca,
                onValueChange = { busca = it },
                label = { Text("Pesquisar fluido") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            // Aviso técnico obrigatório
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.padding(4.dp))
                    Text(GasesRefrigerantes.AVISO, style = MaterialTheme.typography.bodySmall)
                }
            }
            LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                items(lista, key = { it.nome }) { gas -> CardGas(gas) }
            }
        }
    }
}

@Composable
private fun CardGas(gas: GasRefrigerante) {
    var expandido by remember { mutableStateOf(false) }
    Card(
        onClick = { expandido = !expandido },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(gas.nome, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("${gas.tipo} • Classe ${gas.classeSeguranca} • GWP ${gas.gwp}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (expandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
            AnimatedVisibility(expandido) {
                Column {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    LinhaInfo("Aplicação comum", gas.aplicacao)
                    LinhaInfo("Pressões de trabalho", gas.pressoesTrabalho)
                    LinhaInfo("Temp. de evaporação", gas.tempEvaporacao)
                    LinhaInfo("Temp. de condensação", gas.tempCondensacao)
                    LinhaInfo("Óleo compatível", gas.oleoCompativel)
                    LinhaInfo("Classe de segurança", gas.classeSeguranca)
                    LinhaInfo("GWP aproximado", gas.gwp)
                    LinhaInfo("Observações técnicas", gas.observacoes)
                    if (gas.substitutos != "—") LinhaInfo("Substitutos comuns", gas.substitutos)
                }
            }
        }
    }
}

@Composable
private fun LinhaInfo(rotulo: String, valor: String) {
    Column(Modifier.padding(vertical = 3.dp)) {
        Text(rotulo.uppercase(), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
        Text(valor, style = MaterialTheme.typography.bodyMedium)
    }
}
