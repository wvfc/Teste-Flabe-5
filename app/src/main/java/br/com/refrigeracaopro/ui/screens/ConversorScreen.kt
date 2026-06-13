package br.com.refrigeracaopro.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.Conversor
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.SeletorOpcoes
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.theme.Verde

/** Conversor de unidades para diversas grandezas. */
@Composable
fun ConversorScreen(nav: NavController) {
    var categoria by remember { mutableStateOf(Conversor.CATEGORIAS.first()) }
    var de by remember { mutableStateOf(categoria.unidades.first()) }
    var para by remember { mutableStateOf(categoria.unidades[1]) }
    var valor by remember { mutableStateOf("1") }

    val resultado = remember(categoria, de, para, valor) {
        val v = valor.replace(",", ".").toDoubleOrNull() ?: return@remember null
        Conversor.converter(categoria, de, para, v)
    }

    TelaBase(nav, "Conversor de unidades") { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            // Categorias
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
            ) {
                items(Conversor.CATEGORIAS) { cat ->
                    FilterChip(
                        selected = categoria.nome == cat.nome,
                        onClick = {
                            categoria = cat
                            de = cat.unidades.first()
                            para = cat.unidades.getOrElse(1) { cat.unidades.first() }
                        },
                        label = { Text(cat.nome) }
                    )
                }
            }

            Column(Modifier.padding(16.dp)) {
                CampoTexto(valor, { valor = it }, "Valor",
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number))

                SeletorOpcoes("De", categoria.unidades.map { it.nome }, de.nome,
                    { sel -> de = categoria.unidades.first { it.nome == sel } })

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { val t = de; de = para; para = t }) {
                        Icon(Icons.Default.SwapVert, "Inverter", tint = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(Modifier.weight(1f))
                }

                SeletorOpcoes("Para", categoria.unidades.map { it.nome }, para.nome,
                    { sel -> para = categoria.unidades.first { it.nome == sel } })

                resultado?.let { r ->
                    Card(
                        Modifier.fillMaxWidth().padding(top = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Resultado", style = MaterialTheme.typography.labelMedium)
                            Text("${formatar(r)} ${para.nome}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold, color = Verde)
                            Text("$valor ${de.nome} = ${formatar(r)} ${para.nome}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/** Formata o resultado com precisão adequada (evita notação científica feia). */
private fun formatar(v: Double): String {
    val abs = kotlin.math.abs(v)
    return when {
        v == 0.0 -> "0"
        abs >= 1_000_000 || abs < 0.0001 -> "%.4e".format(v)
        abs >= 100 -> "%.2f".format(v)
        else -> "%.4f".format(v).trimEnd('0').trimEnd('.')
    }
}
