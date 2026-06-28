package br.com.refrigeracaopro.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.DadosCompressores
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.theme.Verde

/**
 * Dados de Compressores: busca uma máquina, abre primeiro as informações
 * internas (peças, kits, itens) e, dentro, os links relacionados (manuais e
 * listas de peças em PDF).
 */
@Composable
fun DadosCompressoresScreen(nav: NavController) {
    val context = LocalContext.current
    val todas = remember { DadosCompressores.carregar(context) }
    var consulta by remember { mutableStateOf("") }
    var selecionada by remember { mutableStateOf<DadosCompressores.Maquina?>(null) }

    val resultados = remember(consulta, todas) { DadosCompressores.buscar(todas, consulta) }

    TelaBase(nav, "Dados de Compressores") { padding ->
        val maquina = selecionada
        if (maquina == null) {
            Column(Modifier.fillMaxSize().padding(padding)) {
                OutlinedTextField(
                    value = consulta,
                    onValueChange = { consulta = it },
                    label = { Text("Buscar máquina (ex.: GA 90, GA15, BQD...)") },
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
                                val det = listOf(m.subtitulo, m.bqd).filter { it.isNotBlank() }.joinToString("  •  ")
                                if (det.isNotBlank()) {
                                    Text(det, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    "${m.secoes.sumOf { it.itens.size }} itens  •  ${m.links.size} PDF(s)",
                                    style = MaterialTheme.typography.labelSmall, color = Verde,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        } else {
            DetalheMaquina(maquina, padding) { selecionada = null }
        }
    }
}

@Composable
private fun DetalheMaquina(
    m: DadosCompressores.Maquina,
    padding: androidx.compose.foundation.layout.PaddingValues,
    aoVoltar: () -> Unit,
) {
    val context = LocalContext.current
    fun abrir(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { Toast.makeText(context, "Não foi possível abrir o link.", Toast.LENGTH_SHORT).show() }
    }

    Column(
        Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
    ) {
        TextButton(onClick = aoVoltar, modifier = Modifier.padding(top = 4.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp)); Text("Voltar para a busca")
        }

        // Cabeçalho da máquina
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(m.nome, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Verde)
                if (m.titulo.isNotBlank() && m.titulo != m.nome) Text(m.titulo, style = MaterialTheme.typography.bodyMedium)
                if (m.subtitulo.isNotBlank()) Text(m.subtitulo, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (m.bqd.isNotBlank()) Text("Código: ${m.bqd}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }

        // Informações internas (peças, kits, itens)
        m.secoes.forEach { secao ->
            if (secao.titulo.isNotBlank()) {
                Text(secao.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
            }
            secao.itens.forEach { item -> LinhaItem(secao.colunas, item) }
        }

        // Links relacionados (PDFs)
        if (m.links.isNotEmpty()) {
            Text("Links relacionados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
            m.links.forEach { link ->
                OutlinedButton(
                    onClick = { abrir(link.url) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    Icon(
                        if (link.texto.contains("manual", true)) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.PictureAsPdf,
                        null, Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(link.texto, Modifier.weight(1f))
                }
            }
            Text("Os documentos abrem no navegador (PDF). Requer internet.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Uma linha de item: Descrição em destaque + código e demais colunas. */
@Composable
private fun LinhaItem(colunas: List<String>, item: List<String>) {
    if (item.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(item.getOrElse(0) { "" }, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (item.size > 1) {
                Spacer(Modifier.width(8.dp))
                Text(item[1], style = MaterialTheme.typography.bodyMedium, color = Verde, fontWeight = FontWeight.Bold)
            }
        }
        // Colunas extras (Observação, Intervalo Hrs, etc.)
        val extras = buildList {
            for (i in 2 until item.size) {
                val rotulo = colunas.getOrNull(i)?.takeIf { it.isNotBlank() && !it.startsWith("Descri", true) && !it.startsWith("Cód", true) }
                val valor = item[i]
                if (valor.isNotBlank()) add(if (rotulo != null) "$rotulo: $valor" else valor)
            }
        }
        if (extras.isNotEmpty()) {
            Text(extras.joinToString("  •  "), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
