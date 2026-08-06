package br.com.refrigeracaopro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import br.com.refrigeracaopro.data.Megohmetro
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.components.TituloSecao
import br.com.refrigeracaopro.ui.theme.Verde

/** Painel de Diagnóstico de Megôhmetro: isolação puntual, DAR, PI e diagnóstico. */
@Composable
fun MegohmetroScreen(nav: NavController) {
    var tensao by remember { mutableStateOf("500") }
    var r30 by remember { mutableStateOf("") }
    var r60 by remember { mutableStateOf("") }
    var r10min by remember { mutableStateOf("") }
    var temperatura by remember { mutableStateOf("") }
    var abaRef by remember { mutableStateOf(0) }

    fun num(s: String) = s.replace(",", ".").toDoubleOrNull() ?: 0.0

    // Recalcula automaticamente a cada mudança nos campos
    val resultado = remember(tensao, r30, r60, r10min, temperatura) {
        Megohmetro.calcular(
            Megohmetro.Entrada(
                tensaoV = num(tensao),
                r30s = num(r30),
                r60s = num(r60),
                r10min = num(r10min),
                temperaturaC = temperatura.replace(",", ".").toDoubleOrNull(),
            )
        )
    }

    TelaBase(nav, "Painel de Diagnóstico de Megôhmetro") { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()).imePadding()
        ) {
            // ---------- Entrada de dados ----------
            TituloSecao("Dados do ensaio")
            CampoTexto(tensao, { tensao = it }, "Tensão de teste aplicada (V)",
                teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
            Row(Modifier.fillMaxWidth()) {
                CampoTexto(r30, { r30 = it }, "Leitura 30 s (MΩ)", Modifier.weight(1f).padding(end = 4.dp),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
                CampoTexto(r60, { r60 = it }, "Leitura 1 min (MΩ)", Modifier.weight(1f).padding(start = 4.dp),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Row(Modifier.fillMaxWidth()) {
                CampoTexto(r10min, { r10min = it }, "Leitura 10 min (MΩ)", Modifier.weight(1f).padding(end = 4.dp),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
                CampoTexto(temperatura, { temperatura = it }, "Temperatura (°C) — opcional",
                    Modifier.weight(1f).padding(start = 4.dp),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
            }

            // ---------- Resultados ----------
            TituloSecao("Resultados")
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    LinhaResultado("Isolação puntual (60 s)",
                        resultado.puntual?.let { "%.0f MΩ".format(it) } ?: "—")
                    resultado.puntualCorrigido?.let {
                        LinhaResultado("Corrigido para 40 °C", "%.0f MΩ".format(it), destaque = false)
                    }
                    resultado.minimoRecomendado?.let {
                        LinhaResultado("Mínimo recomendado (kV + 1)", "%.1f MΩ".format(it), destaque = false)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    LinhaResultado("Índice de Absorção (DAR)",
                        resultado.dar?.let { "%.2f".format(it) } ?: "—")
                    LinhaResultado("Índice de Polarização (PI)",
                        resultado.pi?.let { "%.2f".format(it) } ?: "—")
                }
            }

            // ---------- Diagnóstico automático ----------
            TituloSecao("Diagnóstico")
            val condicao = resultado.condicao
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (condicao != null) Color(condicao.cor).copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    if (condicao != null) {
                        Text(condicao.rotulo.uppercase(), style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = Color(condicao.cor))
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(resultado.diagnostico, style = MaterialTheme.typography.bodyMedium)
                    resultado.avisos.forEach { aviso ->
                        Text("• $aviso", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }

            // ---------- Referência técnica ----------
            TituloSecao("Referência técnica")
            val abas = listOf("Passo a passo", "Defeitos comuns", "Correção de temp.")
            TabRow(selectedTabIndex = abaRef) {
                abas.forEachIndexed { i, titulo ->
                    Tab(selected = abaRef == i, onClick = { abaRef = i },
                        text = { Text(titulo, style = MaterialTheme.typography.labelMedium) })
                }
            }
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                when (abaRef) {
                    0 -> {
                        Text("Procedimento do ensaio", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Megohmetro.PASSO_A_PASSO.forEach { passo ->
                            Text(passo, style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 6.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Tipos de ensaio", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Megohmetro.EXPLICACAO_ENSAIOS.forEach { (titulo, desc) ->
                            Column(Modifier.padding(top = 8.dp)) {
                                Text(titulo, fontWeight = FontWeight.SemiBold, color = Verde,
                                    style = MaterialTheme.typography.bodyMedium)
                                Text(desc, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            "Atenção: o ensaio é feito com o equipamento desenergizado e aterrado. " +
                                "Aguarde a descarga completa antes de tocar nos terminais.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                    1 -> {
                        Megohmetro.DEFEITOS.forEach { (sintoma, causa) ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Text(sintoma, fontWeight = FontWeight.SemiBold, color = Verde,
                                    style = MaterialTheme.typography.bodyMedium)
                                Text(causa, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                HorizontalDivider(Modifier.padding(top = 6.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Faixas de referência (IEEE 43)", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        listOf(
                            "PI < 1,0" to "Perigoso",
                            "PI 1,0 – 2,0" to "Pobre",
                            "PI 2,0 – 4,0" to "Bom",
                            "PI > 4,0" to "Excelente",
                            "DAR < 1,25" to "Inadequado",
                            "DAR 1,25 – 1,6" to "Aceitável",
                            "DAR > 1,6" to "Excelente",
                        ).forEach { (faixa, cond) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(faixa, style = MaterialTheme.typography.bodyMedium)
                                Text(cond, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold, color = Verde)
                            }
                        }
                    }
                    else -> {
                        Text(Megohmetro.TEXTO_CORRECAO, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        Text("Fatores para base de 40 °C", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Temperatura", fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall)
                            Text("Multiplicar por", fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Megohmetro.TABELA_CORRECAO.forEach { (t, fator) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("$t °C", style = MaterialTheme.typography.bodyMedium)
                                Text("%.2f×".format(fator), style = MaterialTheme.typography.bodyMedium,
                                    color = Verde, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LinhaResultado(rotulo: String, valor: String, destaque: Boolean = true) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(
            valor,
            style = if (destaque) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (destaque) FontWeight.Bold else FontWeight.Normal,
            color = if (destaque) Verde else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
