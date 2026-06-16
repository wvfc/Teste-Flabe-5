package br.com.refrigeracaopro.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.Projeto
import br.com.refrigeracaopro.ui.components.ConfirmarExclusao
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.theme.Verde
import br.com.refrigeracaopro.viewmodel.ProjetosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Tela inicial dos projetos isométricos: novo, abrir, duplicar, favoritos e histórico. */
@Composable
fun ProjetosScreen(nav: NavController, vm: ProjetosViewModel = viewModel()) {
    val projetos by vm.projetos.collectAsState()
    val clientes by vm.clientes.collectAsState()
    var soFavoritos by remember { mutableStateOf(false) }
    var excluir by remember { mutableStateOf<Projeto?>(null) }
    val formato = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }

    val lista = if (soFavoritos) projetos.filter { it.favorito } else projetos

    TelaBase(nav, "CAD 3D", aoAdicionar = { nav.navigate("projetos/cadastro?id=0") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                FilterChip(selected = !soFavoritos, onClick = { soFavoritos = false }, label = { Text("Histórico") })
                Row(Modifier.padding(start = 8.dp)) {
                    FilterChip(selected = soFavoritos, onClick = { soFavoritos = true }, label = { Text("Favoritos") })
                }
            }
            if (lista.isEmpty()) {
                Text("Nenhum projeto ainda. Toque em + para criar um novo.",
                    Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(lista, key = { it.id }) { p ->
                    val cliente = clientes.find { it.id == p.clienteId }?.nome ?: ""
                    Card(
                        onClick = { nav.navigate("projetos/editor?id=${p.id}") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.nome, fontWeight = FontWeight.Bold)
                                Text(listOf(cliente, p.local, formato.format(Date(p.atualizadoEm)))
                                    .filter { it.isNotBlank() }.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { vm.favoritar(p) }) {
                                Icon(if (p.favorito) Icons.Default.Star else Icons.Default.StarBorder,
                                    "Favorito", tint = if (p.favorito) Verde else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { vm.duplicar(p) }) {
                                Icon(Icons.Default.ContentCopy, "Duplicar")
                            }
                            IconButton(onClick = { excluir = p }) {
                                Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    excluir?.let { p ->
        ConfirmarExclusao("Excluir o projeto \"${p.nome}\"?",
            aoConfirmar = { vm.excluir(p); excluir = null }, aoCancelar = { excluir = null })
    }
}
