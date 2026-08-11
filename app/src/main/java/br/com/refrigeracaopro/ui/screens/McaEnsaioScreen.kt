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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.EnsaioMca
import br.com.refrigeracaopro.data.Mca
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.viewmodel.McaViewModel

private const val ULTIMO_BLOCO = 6

private val TITULOS_BLOCO = listOf(
    "Bloco 0 — Segurança e identificação",
    "Bloco 1 — Resistência (1 kHz)",
    "Bloco 2 — Indutância e impedância",
    "Bloco 3 — Alta frequência (10 kHz)",
    "Bloco 4 — Capacitância para terra",
    "Bloco 5 — RIC (Rotor Influence Check)",
    "Bloco 6 — Isolação (opcional)",
)

/**
 * Wizard de coleta do ensaio MCA: um bloco por tela, com rascunho salvo
 * automaticamente a cada campo preenchido e tela mantida ligada durante a
 * coleta.
 */
@Composable
fun McaEnsaioScreen(nav: NavController, motorId: Long, ensaioId: Long, vm: McaViewModel = viewModel()) {
    // Tela sempre ligada enquanto o técnico coleta
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    var carregado by remember { mutableStateOf(false) }
    var idAtual by remember { mutableStateOf(ensaioId) }
    var numeroEnsaio by remember { mutableStateOf("") }
    var bloco by remember { mutableIntStateOf(0) }
    var versao by remember { mutableIntStateOf(0) }

    var tecnico by remember { mutableStateOf("") }
    var temperatura by remember { mutableStateOf("") }
    var umidade by remember { mutableStateOf("") }
    var observacoes by remember { mutableStateOf("") }
    val seguranca = remember { mutableStateMapOf<Int, Boolean>() }
    val campos = remember { mutableStateMapOf<String, String>() }

    fun mudou() { versao++ }

    // Carrega o ensaio existente (ou o rascunho pendente do motor)
    LaunchedEffect(ensaioId, motorId) {
        val existente = if (ensaioId > 0) vm.buscarEnsaio(ensaioId) else vm.rascunhoDoMotor(motorId)
        if (existente != null) {
            idAtual = existente.id
            numeroEnsaio = existente.numero
            bloco = existente.blocoAtual.coerceIn(0, ULTIMO_BLOCO)
            tecnico = existente.tecnico
            temperatura = existente.temperaturaCarcacaC?.let { "%.1f".format(it) } ?: ""
            umidade = existente.umidadeRelativa?.let { "%.0f".format(it) } ?: ""
            observacoes = existente.observacoes
            seguranca[0] = existente.segDesligado
            seguranca[1] = existente.segCabosDesconectados
            seguranca[2] = existente.segCapacitorRemovido
            seguranca[3] = existente.segAterrado
            seguranca[4] = existente.segCalibracao
            seguranca[5] = existente.segAquecimento
            carregarCampos(campos, existente)
        }
        carregado = true
    }

    fun montar(concluir: Boolean = false) = EnsaioMca(
        id = idAtual,
        numero = numeroEnsaio,
        motorId = motorId,
        tecnico = tecnico,
        temperaturaCarcacaC = numero(temperatura),
        umidadeRelativa = numero(umidade),
        rascunho = !concluir,
        blocoAtual = bloco,
        segDesligado = seguranca[0] == true,
        segCabosDesconectados = seguranca[1] == true,
        segCapacitorRemovido = seguranca[2] == true,
        segAterrado = seguranca[3] == true,
        segCalibracao = seguranca[4] == true,
        segAquecimento = seguranca[5] == true,
        resistencias = Mca.codificarResistencias(lerResistencias(campos)),
        indutancias = lerLZ(campos).let { (cem, mil) -> Mca.codificarLZ(cem, mil) },
        altaFrequencia = Mca.codificarAltaFrequencia(lerAltaFrequencia(campos)),
        capacitancias = Mca.codificarCapacitancias(lerCapacitancias(campos)),
        ric = Mca.codificarRic(lerRic(campos)),
        isolacaoMOhm = numero(campos["ISO_R"].orEmpty()),
        tensaoEnsaioV = numero(campos["ISO_V"].orEmpty()),
        leitura1min = numero(campos["ISO_1MIN"].orEmpty()),
        leitura10min = numero(campos["ISO_10MIN"].orEmpty()),
        observacoes = observacoes,
    )

    // Salvamento automático do rascunho, com pequena espera para não gravar a
    // cada tecla — o técnico não perde dados se o app for para segundo plano.
    LaunchedEffect(versao, bloco) {
        if (!carregado || versao == 0) return@LaunchedEffect
        kotlinx.coroutines.delay(500)
        vm.salvarEnsaio(montar()) { salvo ->
            idAtual = salvo.id
            numeroEnsaio = salvo.numero
        }
    }

    val segurancaOk = (0..5).all { seguranca[it] == true }
    val temperaturaOk = numero(temperatura) != null

    TelaBase(nav, "Ensaio MCA") { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()).imePadding()
        ) {
            LinearProgressIndicator(
                progress = { (bloco + 1) / (ULTIMO_BLOCO + 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            Text(
                TITULOS_BLOCO[bloco],
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
            )
            if (numeroEnsaio.isNotBlank()) {
                Text(
                    "$numeroEnsaio • rascunho salvo automaticamente",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when (bloco) {
                0 -> BlocoSeguranca(
                    tecnico, { tecnico = it; mudou() },
                    temperatura, { temperatura = it; mudou() },
                    umidade, { umidade = it; mudou() },
                    seguranca, { indice, valor -> seguranca[indice] = valor; mudou() },
                )
                1 -> BlocoResistencia(campos) { mudou() }
                2 -> BlocoIndutancia(campos) { mudou() }
                3 -> BlocoAltaFrequencia(campos) { mudou() }
                4 -> BlocoCapacitancia(campos) { mudou() }
                5 -> BlocoRic(campos) { mudou() }
                else -> BlocoIsolacao(campos, observacoes, { observacoes = it; mudou() }) { mudou() }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { if (bloco > 0) bloco-- },
                    enabled = bloco > 0,
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text("Anterior") }
                if (bloco < ULTIMO_BLOCO) {
                    Button(
                        onClick = { bloco++ },
                        enabled = bloco != 0 || (segurancaOk && temperaturaOk),
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text("Próximo") }
                } else {
                    Button(
                        onClick = {
                            vm.salvarEnsaio(montar(concluir = true)) { salvo ->
                                nav.navigate("mca/parecer?id=${salvo.id}") {
                                    popUpTo("mca/ensaio?motorId=$motorId&id=$ensaioId") { inclusive = true }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text("Concluir e ver parecer") }
                }
            }
            if (bloco == 0 && !(segurancaOk && temperaturaOk)) {
                Text(
                    "Marque todos os itens de segurança e informe a temperatura da carcaça para liberar a coleta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = McaVermelho,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ---------------- Blocos ----------------

private val ITENS_SEGURANCA = listOf(
    "Motor desligado, bloqueado e sinalizado",
    "Cabos desconectados da partida/inversor",
    "Capacitor de correção removido, se houver",
    "Terminais aterrados e descarregados",
    "Calibração OPEN e SHORT do LC1020E feita com a garra Kelvin montada",
    "Instrumento aquecido por 30 min",
)

@Composable
private fun BlocoSeguranca(
    tecnico: String, aoMudarTecnico: (String) -> Unit,
    temperatura: String, aoMudarTemperatura: (String) -> Unit,
    umidade: String, aoMudarUmidade: (String) -> Unit,
    marcados: Map<Int, Boolean>, aoMarcar: (Int, Boolean) -> Unit,
) {
    Column {
        CampoTexto(tecnico, aoMudarTecnico, "Técnico responsável")
        Row(Modifier.fillMaxWidth()) {
            CampoTexto(
                temperatura, aoMudarTemperatura, "Temperatura da carcaça (°C) *", Modifier.weight(1f),
                teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Spacer(Modifier.width(8.dp))
            CampoTexto(
                umidade, aoMudarUmidade, "Umidade relativa (%)", Modifier.weight(1f),
                teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        Text(
            "A temperatura é obrigatória: sem ela não há como corrigir a resistência para 40 °C.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        ITENS_SEGURANCA.forEachIndexed { indice, item ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = marcados[indice] == true,
                    onCheckedChange = { aoMarcar(indice, it) },
                )
                Text(item, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun BlocoResistencia(campos: MutableMap<String, String>, aoMudar: () -> Unit) {
    Column {
        Ajuda("1 kHz • 0,6 V • faixa travada em 100 Ω. Três medições de R por par, reposicionando a garra a cada uma.")
        Mca.Par.entries.forEach { par ->
            CardBloco(par.rotulo) {
                Row(Modifier.fillMaxWidth()) {
                    (1..3).forEach { n ->
                        CampoNumero(campos, "R_${par.name}_$n", "R$n (Ω)", Modifier.weight(1f), aoMudar)
                        if (n < 3) Spacer(Modifier.width(6.dp))
                    }
                }
                CampoNumero(campos, "TH_${par.name}", "θ (graus)", Modifier.fillMaxWidth(), aoMudar)

                val medidas = (1..3).mapNotNull { numero(campos["R_${par.name}_$it"].orEmpty()) }
                if (medidas.size >= 2) {
                    val media = medidas.average()
                    val desvio = if (media == 0.0) null else (medidas.max() - medidas.min()) / media * 100.0
                    Text(
                        "Média %.4f Ω • desvio %.2f%%".format(media, desvio ?: 0.0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (desvio != null && desvio > Mca.DESVIO_REPETICAO_ALERTA) {
                        Text(
                            "Reposicione a garra e repita — desvio acima de 1%.",
                            style = MaterialTheme.typography.bodySmall,
                            color = McaVermelho,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlocoIndutancia(campos: MutableMap<String, String>, aoMudar: () -> Unit) {
    Column {
        Ajuda("Indutância (mH) e impedância (Ω) de cada par, nas duas frequências.")
        Mca.Par.entries.forEach { par ->
            CardBloco(par.rotulo) {
                Text("100 Hz", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth()) {
                    CampoNumero(campos, "L100_${par.name}", "L (mH)", Modifier.weight(1f), aoMudar)
                    Spacer(Modifier.width(6.dp))
                    CampoNumero(campos, "Z100_${par.name}", "Z (Ω)", Modifier.weight(1f), aoMudar)
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                Text("1 kHz", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth()) {
                    CampoNumero(campos, "L1K_${par.name}", "L (mH)", Modifier.weight(1f), aoMudar)
                    Spacer(Modifier.width(6.dp))
                    CampoNumero(campos, "Z1K_${par.name}", "Z (Ω)", Modifier.weight(1f), aoMudar)
                }
            }
        }
    }
}

@Composable
private fun BlocoAltaFrequencia(campos: MutableMap<String, String>, aoMudar: () -> Unit) {
    Column {
        Ajuda("10 kHz: é nesta faixa que a falha entre espiras incipiente aparece primeiro.")
        Mca.Par.entries.forEach { par ->
            CardBloco(par.rotulo) {
                Row(Modifier.fillMaxWidth()) {
                    CampoNumero(campos, "Z10K_${par.name}", "Z (Ω)", Modifier.weight(1f), aoMudar)
                    Spacer(Modifier.width(6.dp))
                    CampoNumero(campos, "TH10K_${par.name}", "θ (graus)", Modifier.weight(1f), aoMudar)
                }
            }
        }
    }
}

@Composable
private fun BlocoCapacitancia(campos: MutableMap<String, String>, aoMudar: () -> Unit) {
    Column {
        Ajuda("100 Hz: cada fase contra a carcaça. Diferença entre as fases indica umidade ou contaminação.")
        CardBloco("Capacitância para terra") {
            Row(Modifier.fillMaxWidth()) {
                Mca.Fase.entries.forEachIndexed { indice, fase ->
                    CampoNumero(campos, "C_${fase.name}", "Fase ${fase.rotulo} (nF)", Modifier.weight(1f), aoMudar)
                    if (indice < Mca.Fase.entries.size - 1) Spacer(Modifier.width(6.dp))
                }
            }
        }
    }
}

@Composable
private fun BlocoRic(campos: MutableMap<String, String>, aoMudar: () -> Unit) {
    val angulos = Mca.angulosRic()
    val total = angulos.size * Mca.Par.entries.size
    val preenchidos = angulos.sumOf { angulo ->
        Mca.Par.entries.count { par -> numero(campos["RIC_${par.name}_$angulo"].orEmpty()) != null }
    }
    Column {
        Ajuda(
            "Modo L, 1 kHz. Gire o eixo de 30° em 30° e meça os três pares em cada posição. " +
                "Marque a posição inicial no eixo para não perder a referência."
        )
        Card(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("$preenchidos de $total leituras", fontWeight = FontWeight.SemiBold)
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else preenchidos / total.toFloat() },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text("Ângulo", Modifier.weight(0.7f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Mca.Par.entries.forEach {
                Text(it.rotulo, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
        angulos.forEach { angulo ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("$angulo°", Modifier.weight(0.7f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Mca.Par.entries.forEach { par ->
                    CampoNumero(campos, "RIC_${par.name}_$angulo", "mH", Modifier.weight(1f), aoMudar)
                    Spacer(Modifier.width(4.dp))
                }
            }
        }
    }
}

@Composable
private fun BlocoIsolacao(
    campos: MutableMap<String, String>,
    observacoes: String,
    aoMudarObservacoes: (String) -> Unit,
    aoMudar: () -> Unit,
) {
    Column {
        Ajuda("Opcional: valores lidos em megôhmetro externo. Deixe em branco se o ensaio não foi feito.")
        CardBloco("Isolação para terra") {
            Row(Modifier.fillMaxWidth()) {
                CampoNumero(campos, "ISO_R", "Isolação (MΩ)", Modifier.weight(1f), aoMudar)
                Spacer(Modifier.width(6.dp))
                CampoNumero(campos, "ISO_V", "Tensão de ensaio (V)", Modifier.weight(1f), aoMudar)
            }
            Row(Modifier.fillMaxWidth()) {
                CampoNumero(campos, "ISO_1MIN", "Leitura 1 min (MΩ)", Modifier.weight(1f), aoMudar)
                Spacer(Modifier.width(6.dp))
                CampoNumero(campos, "ISO_10MIN", "Leitura 10 min (MΩ)", Modifier.weight(1f), aoMudar)
            }
        }
        CampoTexto(observacoes, aoMudarObservacoes, "Observações do ensaio", linhas = 3)
    }
}

// ---------------- Auxiliares ----------------

@Composable
private fun Ajuda(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

@Composable
private fun CardBloco(titulo: String, conteudo: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(bottom = 10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            conteudo()
        }
    }
}

@Composable
private fun CampoNumero(
    campos: MutableMap<String, String>,
    chave: String,
    rotulo: String,
    modifier: Modifier,
    aoMudar: () -> Unit,
) {
    CampoTexto(
        campos[chave].orEmpty(),
        { novo -> campos[chave] = novo; aoMudar() },
        rotulo,
        modifier,
        teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

private fun numero(texto: String): Double? = texto.replace(",", ".").toDoubleOrNull()

// ----- Conversão entre os campos da tela e as grades codificadas -----

private fun lerResistencias(campos: Map<String, String>): Map<Mca.Par, Mca.LeituraResistencia> =
    Mca.Par.entries.associateWith { par ->
        Mca.LeituraResistencia(
            repeticoes = (1..3).map { numero(campos["R_${par.name}_$it"].orEmpty()) },
            theta = numero(campos["TH_${par.name}"].orEmpty()),
        )
    }

private fun lerLZ(campos: Map<String, String>): Pair<Map<Mca.Par, Mca.LeituraLZ>, Map<Mca.Par, Mca.LeituraLZ>> {
    val cem = Mca.Par.entries.associateWith {
        Mca.LeituraLZ(numero(campos["L100_${it.name}"].orEmpty()), numero(campos["Z100_${it.name}"].orEmpty()))
    }
    val mil = Mca.Par.entries.associateWith {
        Mca.LeituraLZ(numero(campos["L1K_${it.name}"].orEmpty()), numero(campos["Z1K_${it.name}"].orEmpty()))
    }
    return cem to mil
}

private fun lerAltaFrequencia(campos: Map<String, String>): Map<Mca.Par, Mca.LeituraAltaFrequencia> =
    Mca.Par.entries.associateWith {
        Mca.LeituraAltaFrequencia(
            numero(campos["Z10K_${it.name}"].orEmpty()),
            numero(campos["TH10K_${it.name}"].orEmpty()),
        )
    }

private fun lerCapacitancias(campos: Map<String, String>): Map<Mca.Fase, Double> =
    Mca.Fase.entries.mapNotNull { fase ->
        numero(campos["C_${fase.name}"].orEmpty())?.let { fase to it }
    }.toMap()

private fun lerRic(campos: Map<String, String>): Map<Mca.Par, List<Double?>> =
    Mca.Par.entries.associateWith { par ->
        Mca.angulosRic().map { numero(campos["RIC_${par.name}_$it"].orEmpty()) }
    }

/** Preenche os campos da tela a partir do registro salvo. */
private fun carregarCampos(campos: MutableMap<String, String>, ensaio: EnsaioMca) {
    fun por(valor: Double?) = valor?.toString().orEmpty()

    Mca.decodificarResistencias(ensaio.resistencias).forEach { (par, leitura) ->
        leitura.repeticoes.forEachIndexed { i, v -> campos["R_${par.name}_${i + 1}"] = por(v) }
        campos["TH_${par.name}"] = por(leitura.theta)
    }
    val (cem, mil) = Mca.decodificarLZ(ensaio.indutancias)
    cem.forEach { (par, lz) ->
        campos["L100_${par.name}"] = por(lz.l); campos["Z100_${par.name}"] = por(lz.z)
    }
    mil.forEach { (par, lz) ->
        campos["L1K_${par.name}"] = por(lz.l); campos["Z1K_${par.name}"] = por(lz.z)
    }
    Mca.decodificarAltaFrequencia(ensaio.altaFrequencia).forEach { (par, af) ->
        campos["Z10K_${par.name}"] = por(af.z); campos["TH10K_${par.name}"] = por(af.theta)
    }
    Mca.decodificarCapacitancias(ensaio.capacitancias).forEach { (fase, v) ->
        campos["C_${fase.name}"] = por(v)
    }
    Mca.decodificarRic(ensaio.ric).forEach { (par, serie) ->
        serie.forEachIndexed { i, v -> campos["RIC_${par.name}_${i * Mca.RIC_PASSO_GRAUS}"] = por(v) }
    }
    campos["ISO_R"] = por(ensaio.isolacaoMOhm)
    campos["ISO_V"] = por(ensaio.tensaoEnsaioV)
    campos["ISO_1MIN"] = por(ensaio.leitura1min)
    campos["ISO_10MIN"] = por(ensaio.leitura10min)
}
