package br.com.refrigeracaopro.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.CalculadoraGraxa
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.SeletorOpcoes
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.theme.Verde

private const val MANUAL = "— Inserir manualmente —"

/** Calculadora de relubrificação (graxa) para mancais de rolamento. */
@Composable
fun CalculadoraGraxaScreen(nav: NavController) {
    var aplicacao by remember { mutableStateOf(CalculadoraGraxa.APLICACOES.first().nome) }
    var rolamentoSel by remember { mutableStateOf(MANUAL) }

    var d by remember { mutableStateOf("25") }
    var diam by remember { mutableStateOf("52") }
    var larg by remember { mutableStateOf("15") }
    var rpm by remember { mutableStateOf("1750") }
    var tipo by remember { mutableStateOf(CalculadoraGraxa.TipoRolamento.ESFERAS.rotulo) }
    var temp by remember { mutableStateOf("70") }
    var orientacao by remember { mutableStateOf(CalculadoraGraxa.Orientacao.HORIZONTAL.rotulo) }
    var ambiente by remember { mutableStateOf(CalculadoraGraxa.Ambiente.LIMPO.rotulo) }
    var bombada by remember { mutableStateOf(CalculadoraGraxa.BOMBADA_PADRAO.toString()) }

    val recomendacao = remember(aplicacao) {
        CalculadoraGraxa.APLICACOES.firstOrNull { it.nome == aplicacao }?.recomendacao ?: ""
    }

    fun aplicarPreset(nome: String) {
        aplicacao = nome
        CalculadoraGraxa.APLICACOES.firstOrNull { it.nome == nome }?.let { a ->
            orientacao = a.orientacao.rotulo
            ambiente = a.ambiente.rotulo
            temp = a.temperatura.toString()
        }
    }

    fun aplicarRolamento(designacao: String) {
        rolamentoSel = designacao
        CalculadoraGraxa.ROLAMENTOS.firstOrNull { it.designacao == designacao }?.let { r ->
            d = r.furo.toString(); diam = r.externo.toString(); larg = r.largura.toString(); tipo = r.tipo.rotulo
        }
    }

    fun num(s: String) = s.replace(",", ".").toDoubleOrNull() ?: 0.0

    val resultado = remember(d, diam, larg, rpm, tipo, temp, orientacao, ambiente, bombada) {
        CalculadoraGraxa.calcular(
            CalculadoraGraxa.Entrada(
                furo = num(d), externo = num(diam), largura = num(larg), rpm = num(rpm),
                tipo = CalculadoraGraxa.TipoRolamento.porRotulo(tipo),
                tempC = num(temp),
                orientacao = CalculadoraGraxa.Orientacao.porRotulo(orientacao),
                ambiente = CalculadoraGraxa.Ambiente.porRotulo(ambiente),
                gramasPorBombada = num(bombada),
            )
        )
    }

    TelaBase(nav, "Calculadora de Graxa") { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Aplicação / máquina", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            SeletorOpcoes("Tipo de aplicação", CalculadoraGraxa.APLICACOES.map { it.nome }, aplicacao, { aplicarPreset(it) })
            if (recomendacao.isNotBlank()) {
                Text("Graxa sugerida: $recomendacao", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
            }

            Text("Rolamento", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
            SeletorOpcoes("Modelo do rolamento", listOf(MANUAL) + CalculadoraGraxa.ROLAMENTOS.map { it.designacao },
                rolamentoSel, { aplicarRolamento(it) })

            Row(Modifier.fillMaxWidth()) {
                CampoTexto(d, { d = it; rolamentoSel = MANUAL }, "Furo d (mm)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.width(8.dp))
                CampoTexto(diam, { diam = it; rolamentoSel = MANUAL }, "Externo D (mm)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.width(8.dp))
                CampoTexto(larg, { larg = it; rolamentoSel = MANUAL }, "Largura B (mm)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            SeletorOpcoes("Tipo de rolamento", CalculadoraGraxa.TipoRolamento.ROTULOS, tipo, { tipo = it })

            Text("Condições de operação", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
            Row(Modifier.fillMaxWidth()) {
                CampoTexto(rpm, { rpm = it }, "Rotação (rpm)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.width(8.dp))
                CampoTexto(temp, { temp = it }, "Temperatura (°C)", Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            SeletorOpcoes("Orientação do eixo", CalculadoraGraxa.Orientacao.ROTULOS, orientacao, { orientacao = it })
            SeletorOpcoes("Condição ambiental", CalculadoraGraxa.Ambiente.ROTULOS, ambiente, { ambiente = it })
            CampoTexto(bombada, { bombada = it }, "Gramas por bombada da pistola",
                teclado = KeyboardOptions(keyboardType = KeyboardType.Number))

            // Resultados
            Card(
                Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Quantidade de relubrificação", style = MaterialTheme.typography.labelMedium)
                    Text("%.1f g".format(resultado.quantidadeG),
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Verde)
                    Text("≈ %.1f cm³  •  ≈ %.0f bombadas".format(resultado.quantidadeCm3, resultado.bombadas),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(Modifier.padding(top = 10.dp))
                    Text("Intervalo de relubrificação", style = MaterialTheme.typography.labelMedium)
                    val tf = resultado.intervaloHoras
                    if (tf != null) {
                        Text("%,.0f h".format(tf).replace(",", "."),
                            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Verde)
                        Text("≈ %.0f dias (operação contínua)".format(resultado.intervaloDias ?: 0.0),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("—", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Verde)
                    }

                    Spacer(Modifier.padding(top = 10.dp))
                    Text("Diâmetro médio dm = %.0f mm  •  fator n·dm = %,.0f".format(resultado.dm, resultado.fatorVelocidade).replace(",", "."),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    resultado.avisos.forEach { aviso ->
                        Text("• $aviso", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}
