package br.com.refrigeracaopro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.Megohmetro
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.SeletorOpcoes
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.theme.Verde

private val Vermelho = Color(0xFFC62828)
private val Laranja = Color(0xFFEF6C00)
private val Amarelo = Color(0xFFF9A825)

private val ABAS = listOf("Passo a passo", "Defeitos comuns", "Temperatura")

/**
 * Painel de Diagnóstico de Megômetro: entra com as leituras do megger e o app
 * calcula isolação puntual, DAR e PI, classificando a condição do equipamento
 * conforme a IEEE 43. Traz ainda a referência técnica do ensaio.
 */
@Composable
fun MegohmetroScreen(nav: NavController) {
    var tensao by remember { mutableStateOf("") }
    var r30s by remember { mutableStateOf("") }
    var r60s by remember { mutableStateOf("") }
    var r10min by remember { mutableStateOf("") }
    var temp by remember { mutableStateOf("") }
    var tempBase by remember { mutableStateOf("40 °C") }
    var aba by remember { mutableIntStateOf(0) }

    fun num(s: String) = s.replace(",", ".").toDoubleOrNull()

    // Recalcula automaticamente a cada mudança nos campos
    val resultado = remember(tensao, r30s, r60s, r10min, temp, tempBase) {
        Megohmetro.calcular(
            Megohmetro.Entrada(
                tensaoV = num(tensao) ?: 0.0,
                r30s = num(r30s) ?: 0.0,
                r60s = num(r60s) ?: 0.0,
                r10min = num(r10min) ?: 0.0,
                tempC = num(temp),
                tempBase = if (tempBase.startsWith("20")) Megohmetro.TEMP_BASE_20 else Megohmetro.TEMP_BASE_40,
            )
        )
    }

    TelaBase(nav, "Painel de Diagnóstico de Megômetro") { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ---------------- Entrada de dados ----------------
            Titulo("Entrada de dados")
            CampoTexto(
                tensao, { tensao = it }, "Tensão de teste aplicada (V)",
                teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Row(Modifier.fillMaxWidth()) {
                CampoTexto(
                    r30s, { r30s = it }, "R 30s (MΩ)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.width(8.dp))
                CampoTexto(
                    r60s, { r60s = it }, "R 1min (MΩ)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.width(8.dp))
                CampoTexto(
                    r10min, { r10min = it }, "R 10min (MΩ)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CampoTexto(
                    temp, { temp = it }, "Temperatura do equipamento (°C)", Modifier.weight(1.4f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.width(8.dp))
                SeletorOpcoes("Base", listOf("40 °C", "20 °C"), tempBase, { tempBase = it }, Modifier.weight(1f))
            }
            Text(
                "A temperatura é opcional e serve para corrigir as leituras para a base escolhida.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ---------------- Resultados ----------------
            Titulo("Resultados")
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    LinhaResultado(
                        "Isolação puntual (60 s)",
                        resultado.puntual?.let { "%s MΩ".format(formatar(it)) } ?: "—",
                        detalhe = resultado.puntualCorrigido?.let { corrigido ->
                            "Corrigida p/ ${if (tempBase.startsWith("20")) "20" else "40"} °C: " +
                                "${formatar(corrigido)} MΩ (fator %.2f)".format(resultado.fatorTemperatura ?: 1.0)
                        },
                    )
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    LinhaResultado(
                        "Índice de Absorção (DAR)",
                        resultado.dar?.let { "%.2f".format(it) } ?: "—",
                        detalhe = resultado.classeDar.takeIf { it.isNotBlank() }?.let { "R60s ÷ R30s — $it" }
                            ?: "Informe as leituras de 30 s e 1 min",
                        cor = resultado.dar?.let { corDar(it) },
                    )
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    LinhaResultado(
                        "Índice de Polarização (PI)",
                        resultado.pi?.let { "%.2f".format(it) } ?: "—",
                        detalhe = resultado.classePi.takeIf { it.isNotBlank() }?.let { "R10min ÷ R1min — $it" }
                            ?: "Informe as leituras de 1 min e 10 min",
                        cor = resultado.pi?.let { corPi(it) },
                    )
                    resultado.minimoRecomendado?.let { minimo ->
                        HorizontalDivider(Modifier.padding(vertical = 10.dp))
                        Text(
                            "Mínimo de referência (kV + 1): ${formatar(minimo)} MΩ" +
                                when (resultado.atendeMinimo) {
                                    true -> " — atendido"
                                    false -> " — NÃO atendido"
                                    else -> ""
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (resultado.atendeMinimo == false) Vermelho
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ---------------- Diagnóstico automático ----------------
            Titulo("Diagnóstico automático")
            val condicao = resultado.condicao
            if (condicao == null) {
                Text(
                    "Preencha ao menos duas leituras (30 s e 1 min, ou 1 min e 10 min) para " +
                        "o diagnóstico automático.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val cor = corCondicao(condicao)
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = 0.12f)),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            condicao.rotulo.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .background(cor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(resultado.diagnostico, style = MaterialTheme.typography.bodyMedium)
                        if (resultado.recomendacao.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Recomendação: ${resultado.recomendacao}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        resultado.avisos.forEach { aviso ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "• $aviso",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ---------------- Referência técnica ----------------
            Titulo("Referência técnica")
            TabRow(selectedTabIndex = aba, containerColor = MaterialTheme.colorScheme.surface) {
                ABAS.forEachIndexed { indice, titulo ->
                    Tab(
                        selected = aba == indice,
                        onClick = { aba = indice },
                        text = { Text(titulo, style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            when (aba) {
                0 -> AbaPassoAPasso()
                1 -> AbaDefeitos()
                else -> AbaTemperatura()
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Tab 1 — passo a passo dos ensaios (Puntual, DAR e PI). */
@Composable
private fun AbaPassoAPasso() {
    Column {
        CardTexto("Procedimento comum a todos os ensaios") {
            Megohmetro.PASSO_A_PASSO_GERAL.forEach { passo ->
                Text(passo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Segurança: o megômetro aplica alta tensão. Confirme o desligamento, aterre o " +
                    "equipamento e só toque nos terminais após a descarga.",
                style = MaterialTheme.typography.bodySmall,
                color = Vermelho,
            )
        }
        Megohmetro.ENSAIOS.forEach { ensaio ->
            CardTexto(ensaio.nome) {
                ensaio.passos.forEachIndexed { indice, passo ->
                    Text(
                        "${indice + 1}. $passo",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 3.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    ensaio.nota,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        CardTexto("Faixas de interpretação (IEEE 43)") {
            LinhaTabela("PI < 1,0", "Perigoso", cabecalho = false)
            LinhaTabela("PI 1,0 – 2,0", "Pobre", cabecalho = false)
            LinhaTabela("PI 2,0 – 4,0", "Bom", cabecalho = false)
            LinhaTabela("PI > 4,0", "Excelente", cabecalho = false)
            Spacer(Modifier.height(8.dp))
            LinhaTabela("DAR < 1,25", "Inadequado", cabecalho = false)
            LinhaTabela("DAR 1,25 – 1,6", "Aceitável", cabecalho = false)
            LinhaTabela("DAR > 1,6", "Excelente", cabecalho = false)
        }
        CardTexto("Tensão de ensaio usual") {
            LinhaTabela("Tensão nominal", "Tensão de ensaio", cabecalho = true)
            Megohmetro.TENSOES_ENSAIO.forEach { (nominal, ensaio) ->
                LinhaTabela(nominal, ensaio, cabecalho = false)
            }
        }
    }
}

/** Tab 2 — tabela de defeitos comuns e causas. */
@Composable
private fun AbaDefeitos() {
    CardTexto("Defeitos comuns e causas") {
        LinhaTabela("Sintoma no ensaio", "Causas prováveis", cabecalho = true)
        Megohmetro.DEFEITOS.forEach { defeito ->
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            LinhaTabela(defeito.sintoma, defeito.causas, cabecalho = false)
        }
    }
}

/** Tab 3 — fatores de correção de temperatura. */
@Composable
private fun AbaTemperatura() {
    Column {
        CardTexto("Por que corrigir pela temperatura") {
            Text(Megohmetro.CORRECAO_TEMPERATURA_TEXTO, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            Text(
                Megohmetro.CORRECAO_FORMULA,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Verde,
            )
        }
        CardTexto("Fatores de correção") {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("Temp. medida", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Base 40 °C", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Base 20 °C", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Megohmetro.TABELA_CORRECAO.forEach { (temperatura, fator40, fator20) ->
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Text("$temperatura °C", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text("× %.2f".format(fator40), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text("× %.2f".format(fator20), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Exemplo: 100 MΩ medidos a 50 °C equivalem a 200 MΩ corrigidos para 40 °C. " +
                    "Compare sempre leituras corrigidas na mesma base.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------- Componentes auxiliares ----------------

@Composable
private fun Titulo(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun LinhaResultado(rotulo: String, valor: String, detalhe: String? = null, cor: Color? = null) {
    Column(Modifier.fillMaxWidth()) {
        Text(rotulo, style = MaterialTheme.typography.labelMedium)
        Text(
            valor,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = cor ?: Verde,
        )
        if (detalhe != null) {
            Text(detalhe, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CardTexto(titulo: String, conteudo: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            conteudo()
        }
    }
}

@Composable
private fun LinhaTabela(esquerda: String, direita: String, cabecalho: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            esquerda,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (cabecalho) FontWeight.Bold else FontWeight.SemiBold,
        )
        Text(
            direita,
            Modifier.weight(1.3f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (cabecalho) FontWeight.Bold else FontWeight.Normal,
            color = if (cabecalho) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun corCondicao(condicao: Megohmetro.Condicao) = when (condicao) {
    Megohmetro.Condicao.EXCELENTE -> Verde
    Megohmetro.Condicao.BOM -> Verde
    Megohmetro.Condicao.DUVIDOSO -> Amarelo
    Megohmetro.Condicao.POBRE -> Laranja
    Megohmetro.Condicao.PERIGOSO -> Vermelho
}

private fun corPi(pi: Double) = when {
    pi < 1.0 -> Vermelho
    pi < 2.0 -> Laranja
    else -> Verde
}

private fun corDar(dar: Double) = when {
    dar < 1.25 -> Laranja
    dar <= 1.6 -> Amarelo
    else -> Verde
}

/** Formata MΩ com casas decimais só quando o valor é pequeno. */
private fun formatar(valor: Double): String = when {
    valor >= 1000 -> "%,.0f".format(valor).replace(",", ".")
    valor >= 10 -> "%.0f".format(valor)
    else -> "%.2f".format(valor)
}
