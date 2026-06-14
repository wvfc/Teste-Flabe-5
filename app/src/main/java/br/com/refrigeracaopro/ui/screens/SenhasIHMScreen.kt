package br.com.refrigeracaopro.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.SenhasIHM
import br.com.refrigeracaopro.ui.components.AvisoCard
import br.com.refrigeracaopro.ui.components.TelaBase
import kotlinx.coroutines.launch

/** Consulta de senhas/acessos de IHMs, CLPs e inversores. */
@Composable
fun SenhasIHMScreen(nav: NavController) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    val marcas = remember { mutableStateListOf<SenhasIHM.Marca>() }
    var busca by remember { mutableStateOf("") }
    var atualizando by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        marcas.clear(); marcas.addAll(SenhasIHM.carregar(context))
    }

    val filtradas = marcas.mapNotNull { m ->
        if (busca.isBlank()) m
        else {
            val itens = m.itens.filter {
                it.titulo.contains(busca, true) || it.senha.contains(busca, true) ||
                    it.senhaRef.contains(busca, true) || it.observacao.contains(busca, true) ||
                    it.codigos.any { c -> c.nome.contains(busca, true) || c.valor.contains(busca, true) }
            }
            if (m.nome.contains(busca, true)) m
            else if (itens.isNotEmpty()) m.copy(itens = itens)
            else null
        }
    }

    TelaBase(
        nav, "Senhas IHM",
        acoes = {
            IconButton(onClick = {
                atualizando = true
                escopo.launch {
                    when (val r = SenhasIHM.atualizarDaWeb(context)) {
                        is SenhasIHM.Resultado.Sucesso -> {
                            marcas.clear(); marcas.addAll(r.marcas)
                            Toast.makeText(context, "Base atualizada (${r.marcas.size} marcas).", Toast.LENGTH_LONG).show()
                        }
                        is SenhasIHM.Resultado.Erro -> Toast.makeText(context, r.mensagem, Toast.LENGTH_LONG).show()
                    }
                    atualizando = false
                }
            }) { Icon(Icons.Default.Refresh, "Atualizar da web") }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = busca,
                onValueChange = { busca = it },
                label = { Text("Pesquisar marca, modelo ou senha") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            AvisoCard(
                "Senhas e parâmetros variam por modelo e firmware — confirme sempre no manual do fabricante." +
                    if (atualizando) " (atualizando...)" else "",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                items(filtradas, key = { it.slug }) { marca -> CardMarca(marca) }
            }
        }
    }
}

@Composable
private fun CardMarca(marca: SenhasIHM.Marca) {
    var expandido by remember { mutableStateOf(false) }
    Card(onClick = { expandido = !expandido }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(marca.nome, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("${marca.itens.size} item(ns)", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (expandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
            AnimatedVisibility(expandido) {
                Column {
                    if (marca.descricao.isNotBlank()) Text(marca.descricao,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    marca.itens.forEach { item ->
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Text(item.titulo, fontWeight = FontWeight.SemiBold)
                        if (item.usuario.isNotBlank()) Linha("Usuário", item.usuario)
                        if (item.senha.isNotBlank()) Linha("Senha", item.senha)
                        item.codigos.forEach { c -> Linha(c.nome, c.valor) }
                        if (item.senha.isBlank() && item.codigos.isEmpty())
                            Text("Senha: consultar material (ref. ${item.senhaRef})",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        if (item.observacao.isNotBlank()) Linha("Observação", item.observacao)
                    }
                }
            }
        }
    }
}

@Composable
private fun Linha(rotulo: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
        Text("$rotulo: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Text(valor, style = MaterialTheme.typography.bodySmall)
    }
}
