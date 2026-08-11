package br.com.refrigeracaopro.data

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * MCA — Motor Circuit Analysis: ensaio estático de motores de indução
 * trifásicos, com as medições feitas em campo por ponte LCR portátil e
 * digitadas pelo técnico.
 *
 * Este arquivo é o **motor de cálculo puro**: não importa nada de Android e é
 * inteiramente testável por unidade. A regra vale também para os codecs das
 * grades de leitura, que convertem as colunas de texto do banco nos tipos
 * usados aqui.
 *
 * Índices calculados:
 *  - correção da resistência para 40 °C (cobre): R40 = Rt × (234,5 + 40) / (234,5 + t)
 *  - desbalanceamento: (max − min) / média × 100
 *  - delta de ângulo de fase: max(θ) − min(θ)
 *  - índice I/F: (L100Hz − L1kHz) / L100Hz × 100, e o spread entre os pares
 *  - RIC: amplitude pico a pico por par, espalhamento entre pares e desvio
 *    da forma senoidal (erro RMS contra a senoide ajustada por mínimos
 *    quadrados, normalizado pela amplitude)
 *  - índice de polarização: PI = leitura10min / leitura1min
 *
 * Quando falta dado obrigatório para um índice, ele sai como `null` —
 * "não avaliado" — e nunca como zero.
 */
object Mca {

    /** Constante do cobre na correção de resistência por temperatura. */
    const val CONSTANTE_COBRE = 234.5
    const val TEMPERATURA_BASE = 40.0

    /** Desvio máximo aceitável entre as 3 repetições de R do mesmo par. */
    const val DESVIO_REPETICAO_ALERTA = 1.0

    /** Passo angular do RIC e número de posições. */
    const val RIC_PASSO_GRAUS = 30
    const val RIC_POSICOES = 12

    enum class Severidade { OK, ATENCAO, CRITICO }

    /**
     * Limites de alerta. Os defaults são os valores especificados para o
     * módulo; a tela de ajustes permite editá-los (persistidos em `Prefs` por
     * [McaLimites]). Os limites de isolação e PI seguem a IEEE 43 e são
     * **referência**: o critério final é a tendência histórica do próprio motor.
     */
    data class LimitesMca(
        val desbalR40Atencao: Double = 3.0,
        val desbalR40Critico: Double = 5.0,
        val desbalLAtencao: Double = 5.0,
        val desbalLCritico: Double = 7.0,
        val desbalZAtencao: Double = 5.0,
        val desbalZCritico: Double = 7.0,
        val deltaThetaAtencao: Double = 0.7,
        val deltaThetaCritico: Double = 1.0,
        val spreadIfAtencao: Double = 1.5,
        val spreadIfCritico: Double = 2.0,
        val desbalCAtencao: Double = 10.0,
        val desbalCCritico: Double = 20.0,
        val ricAmplitudeAtencao: Double = 10.0,
        val ricAmplitudeCritico: Double = 15.0,
        val ricSenoideAtencao: Double = 5.0,
        val ricSenoideCritico: Double = 10.0,
        val isolacaoAtencaoMOhm: Double = 200.0,
        val isolacaoCriticoMOhm: Double = 100.0,
        val piAtencao: Double = 2.5,
        val piCritico: Double = 2.0,
    )

    /** Pares de fases medidos no ensaio. */
    enum class Par(val rotulo: String) { AB("A-B"), BC("B-C"), CA("C-A") }

    /** Fases medidas contra a carcaça (capacitância para terra). */
    enum class Fase(val rotulo: String) { A("A"), B("B"), C("C") }

    // ------------------------------------------------------------------
    // Entrada
    // ------------------------------------------------------------------

    /** Bloco 1: três repetições de R (Ω) e um ângulo θ (graus), a 1 kHz. */
    data class LeituraResistencia(
        val repeticoes: List<Double?> = listOf(null, null, null),
        val theta: Double? = null,
    )

    /** Blocos 2: indutância (mH) e impedância (Ω) numa frequência. */
    data class LeituraLZ(val l: Double? = null, val z: Double? = null)

    /** Bloco 3: impedância (Ω) e ângulo (graus) a 10 kHz. */
    data class LeituraAltaFrequencia(val z: Double? = null, val theta: Double? = null)

    /** Conjunto completo de leituras de um ensaio. */
    data class Leituras(
        val temperaturaCarcacaC: Double? = null,
        val resistencias: Map<Par, LeituraResistencia> = emptyMap(),
        val lz100Hz: Map<Par, LeituraLZ> = emptyMap(),
        val lz1kHz: Map<Par, LeituraLZ> = emptyMap(),
        val altaFrequencia: Map<Par, LeituraAltaFrequencia> = emptyMap(),
        val capacitanciasNf: Map<Fase, Double> = emptyMap(),
        /** 12 valores de L (mH) por par, de 0° a 330°. */
        val ric: Map<Par, List<Double?>> = emptyMap(),
        val isolacaoMOhm: Double? = null,
        val tensaoEnsaioV: Double? = null,
        val leitura1min: Double? = null,
        val leitura10min: Double? = null,
    )

    /** Índices do ensaio anterior, usados nas regras que dependem de baseline. */
    data class Baseline(
        val ifPorPar: Map<Par, Double> = emptyMap(),
        val capacitanciaMediaNf: Double? = null,
    )

    // ------------------------------------------------------------------
    // Saída
    // ------------------------------------------------------------------

    /** Controle de repetibilidade das 3 medições de R de um par. */
    data class Repeticao(
        val par: Par,
        val media: Double?,
        val desvioPercentual: Double?,
        val alerta: Boolean,
    )

    data class AnaliseRic(
        val amplitudes: Map<Par, Double>,
        val espalhamentoPercentual: Double?,
        val desvioSenoidalPorPar: Map<Par, Double>,
        val desvioSenoidalPercentual: Double?,
    )

    data class Indices(
        val r40: Map<Par, Double> = emptyMap(),
        val repeticoes: List<Repeticao> = emptyList(),
        val desbalR40: Double? = null,
        val desbalL100: Double? = null,
        val desbalL1k: Double? = null,
        val desbalZ100: Double? = null,
        val desbalZ1k: Double? = null,
        val desbalZ10k: Double? = null,
        val deltaTheta1k: Double? = null,
        val deltaTheta10k: Double? = null,
        val ifPorPar: Map<Par, Double> = emptyMap(),
        val spreadIf: Double? = null,
        val desbalCapacitancia: Double? = null,
        val capacitanciaMediaNf: Double? = null,
        val ric: AnaliseRic? = null,
        val isolacaoMOhm: Double? = null,
        val pi: Double? = null,
    )

    data class Achado(
        val titulo: String,
        val severidade: Severidade,
        val evidencia: String,
        val detalhe: String,
    )

    data class Parecer(
        val indices: Indices,
        val achados: List<Achado>,
        /** Índices sem dado suficiente para avaliar. */
        val naoAvaliados: List<String>,
    )

    // ------------------------------------------------------------------
    // Fórmulas elementares
    // ------------------------------------------------------------------

    /** Corrige a resistência medida a `t` °C para 40 °C (cobre). */
    fun corrigirR40(resistencia: Double, temperaturaC: Double): Double? {
        val divisor = CONSTANTE_COBRE + temperaturaC
        if (divisor == 0.0) return null
        return resistencia * (CONSTANTE_COBRE + TEMPERATURA_BASE) / divisor
    }

    /** Desbalanceamento percentual: (max − min) / média × 100. */
    fun desbalanceamento(valores: List<Double>): Double? {
        if (valores.size < 2) return null
        val media = valores.average()
        if (media == 0.0) return null
        return (valores.max() - valores.min()) / media * 100.0
    }

    /** Diferença entre o maior e o menor ângulo de fase (graus). */
    fun deltaTheta(angulos: List<Double>): Double? {
        if (angulos.size < 2) return null
        return angulos.max() - angulos.min()
    }

    /** Índice I/F de um par: (L100 − L1k) / L100 × 100. */
    fun indiceIF(l100: Double, l1k: Double): Double? {
        if (l100 == 0.0) return null
        return (l100 - l1k) / l100 * 100.0
    }

    /** Índice de polarização. */
    fun indicePolarizacao(leitura1min: Double, leitura10min: Double): Double? {
        if (leitura1min <= 0.0) return null
        return leitura10min / leitura1min
    }

    /**
     * Ajuste senoidal por mínimos quadrados: encontra A, B e C que minimizam
     * o erro de `A·cos(θ) + B·sen(θ) + C` sobre a série medida, em forma
     * fechada pelas equações normais, e devolve o erro RMS normalizado pela
     * amplitude pico a pico (em %).
     *
     * Devolve null quando não há pontos suficientes ou a série é constante.
     */
    fun desvioSenoidal(valores: List<Double?>, passoGraus: Int = RIC_PASSO_GRAUS): Double? {
        val pontos = valores.mapIndexedNotNull { indice, valor ->
            valor?.let { indice * passoGraus * Math.PI / 180.0 to it }
        }
        if (pontos.size < 4) return null

        val n = pontos.size.toDouble()
        var sc = 0.0; var ss = 0.0; var scc = 0.0; var sss = 0.0; var scs = 0.0
        var sy = 0.0; var syc = 0.0; var sys = 0.0
        pontos.forEach { (angulo, y) ->
            val c = cos(angulo); val s = sin(angulo)
            sc += c; ss += s; scc += c * c; sss += s * s; scs += c * s
            sy += y; syc += y * c; sys += y * s
        }
        // Sistema 3x3 [scc scs sc; scs sss ss; sc ss n] · [A B C] = [syc sys sy]
        val m = arrayOf(
            doubleArrayOf(scc, scs, sc, syc),
            doubleArrayOf(scs, sss, ss, sys),
            doubleArrayOf(sc, ss, n, sy),
        )
        val solucao = resolver3x3(m) ?: return null
        val (a, b, c0) = solucao

        val amplitude = amplitude(valores) ?: return null
        if (amplitude == 0.0) return null

        var soma = 0.0
        pontos.forEach { (angulo, y) ->
            val previsto = a * cos(angulo) + b * sin(angulo) + c0
            soma += (y - previsto) * (y - previsto)
        }
        val rms = sqrt(soma / pontos.size)
        return rms / amplitude * 100.0
    }

    /** Eliminação de Gauss com pivotamento parcial para o sistema 3x3. */
    private fun resolver3x3(m: Array<DoubleArray>): Triple<Double, Double, Double>? {
        for (coluna in 0 until 3) {
            var pivo = coluna
            for (linha in coluna + 1 until 3) {
                if (abs(m[linha][coluna]) > abs(m[pivo][coluna])) pivo = linha
            }
            if (abs(m[pivo][coluna]) < 1e-12) return null
            val troca = m[coluna]; m[coluna] = m[pivo]; m[pivo] = troca
            for (linha in 0 until 3) {
                if (linha == coluna) continue
                val fator = m[linha][coluna] / m[coluna][coluna]
                for (k in coluna until 4) m[linha][k] -= fator * m[coluna][k]
            }
        }
        return Triple(m[0][3] / m[0][0], m[1][3] / m[1][1], m[2][3] / m[2][2])
    }

    /** Amplitude pico a pico de uma série (ignora posições não medidas). */
    fun amplitude(valores: List<Double?>): Double? {
        val presentes = valores.filterNotNull()
        if (presentes.size < 2) return null
        return presentes.max() - presentes.min()
    }

    // ------------------------------------------------------------------
    // Cálculo dos índices
    // ------------------------------------------------------------------

    fun calcularIndices(leituras: Leituras): Indices {
        // Bloco 1 — resistências corrigidas para 40 °C
        val repeticoes = Par.entries.map { par ->
            val medidas = leituras.resistencias[par]?.repeticoes.orEmpty().filterNotNull()
            val media = if (medidas.isEmpty()) null else medidas.average()
            val desvio = if (medidas.size < 2 || media == null || media == 0.0) null
            else (medidas.max() - medidas.min()) / media * 100.0
            Repeticao(par, media, desvio, desvio != null && desvio > DESVIO_REPETICAO_ALERTA)
        }
        val temperatura = leituras.temperaturaCarcacaC
        val r40 = if (temperatura == null) emptyMap() else repeticoes.mapNotNull { rep ->
            rep.media?.let { media -> corrigirR40(media, temperatura)?.let { rep.par to it } }
        }.toMap()

        val desbalR40 = if (r40.size == Par.entries.size) desbalanceamento(r40.values.toList()) else null

        // Blocos 2 e 3 — indutância, impedância e ângulo
        fun desbalDe(mapa: Map<Par, LeituraLZ>, seletor: (LeituraLZ) -> Double?): Double? {
            val valores = Par.entries.mapNotNull { mapa[it]?.let(seletor) }
            return if (valores.size == Par.entries.size) desbalanceamento(valores) else null
        }
        val desbalL100 = desbalDe(leituras.lz100Hz) { it.l }
        val desbalL1k = desbalDe(leituras.lz1kHz) { it.l }
        val desbalZ100 = desbalDe(leituras.lz100Hz) { it.z }
        val desbalZ1k = desbalDe(leituras.lz1kHz) { it.z }

        val z10k = Par.entries.mapNotNull { leituras.altaFrequencia[it]?.z }
        val desbalZ10k = if (z10k.size == Par.entries.size) desbalanceamento(z10k) else null

        val theta1k = Par.entries.mapNotNull { leituras.resistencias[it]?.theta }
        val deltaTheta1k = if (theta1k.size == Par.entries.size) deltaTheta(theta1k) else null
        val theta10k = Par.entries.mapNotNull { leituras.altaFrequencia[it]?.theta }
        val deltaTheta10k = if (theta10k.size == Par.entries.size) deltaTheta(theta10k) else null

        // Índice I/F
        val ifPorPar = Par.entries.mapNotNull { par ->
            val l100 = leituras.lz100Hz[par]?.l
            val l1k = leituras.lz1kHz[par]?.l
            if (l100 == null || l1k == null) null else indiceIF(l100, l1k)?.let { par to it }
        }.toMap()
        val spreadIf = if (ifPorPar.size == Par.entries.size)
            ifPorPar.values.max() - ifPorPar.values.min() else null

        // Bloco 4 — capacitância para terra
        val capacitancias = Fase.entries.mapNotNull { leituras.capacitanciasNf[it] }
        val desbalC = if (capacitancias.size == Fase.entries.size) desbalanceamento(capacitancias) else null

        // Bloco 5 — RIC
        val amplitudes = Par.entries.mapNotNull { par ->
            leituras.ric[par]?.let { serie -> amplitude(serie)?.let { par to it } }
        }.toMap()
        val espalhamento = if (amplitudes.size == Par.entries.size)
            desbalanceamento(amplitudes.values.toList()) else null
        val desvios = Par.entries.mapNotNull { par ->
            leituras.ric[par]?.let { serie -> desvioSenoidal(serie)?.let { par to it } }
        }.toMap()
        val ric = if (amplitudes.isEmpty() && desvios.isEmpty()) null
        else AnaliseRic(amplitudes, espalhamento, desvios, desvios.values.maxOrNull())

        // Bloco 6 — isolação
        val pi = if (leituras.leitura1min != null && leituras.leitura10min != null)
            indicePolarizacao(leituras.leitura1min, leituras.leitura10min) else null

        return Indices(
            r40 = r40,
            repeticoes = repeticoes,
            desbalR40 = desbalR40,
            desbalL100 = desbalL100,
            desbalL1k = desbalL1k,
            desbalZ100 = desbalZ100,
            desbalZ1k = desbalZ1k,
            desbalZ10k = desbalZ10k,
            deltaTheta1k = deltaTheta1k,
            deltaTheta10k = deltaTheta10k,
            ifPorPar = ifPorPar,
            spreadIf = spreadIf,
            desbalCapacitancia = desbalC,
            capacitanciaMediaNf = if (capacitancias.isEmpty()) null else capacitancias.average(),
            ric = ric,
            isolacaoMOhm = leituras.isolacaoMOhm,
            pi = pi,
        )
    }

    /** Severidade de um índice em que valores altos são ruins. */
    fun severidadeAcima(valor: Double?, atencao: Double, critico: Double): Severidade? = when {
        valor == null -> null
        valor >= critico -> Severidade.CRITICO
        valor >= atencao -> Severidade.ATENCAO
        else -> Severidade.OK
    }

    /** Severidade de um índice em que valores baixos são ruins (isolação, PI). */
    fun severidadeAbaixo(valor: Double?, atencao: Double, critico: Double): Severidade? = when {
        valor == null -> null
        valor <= critico -> Severidade.CRITICO
        valor <= atencao -> Severidade.ATENCAO
        else -> Severidade.OK
    }

    // ------------------------------------------------------------------
    // Árvore de diagnóstico
    // ------------------------------------------------------------------

    private fun forade(severidade: Severidade?): Boolean =
        severidade == Severidade.ATENCAO || severidade == Severidade.CRITICO

    private fun pior(vararg severidades: Severidade?): Severidade =
        severidades.filterNotNull().maxByOrNull { it.ordinal } ?: Severidade.OK

    /**
     * Avalia os oito ramos na ordem especificada e devolve **todos** os
     * achados aplicáveis, cada um com a evidência numérica que o disparou.
     */
    fun diagnosticar(indices: Indices, limites: LimitesMca, baseline: Baseline? = null): Parecer {
        val achados = mutableListOf<Achado>()
        val naoAvaliados = mutableListOf<String>()

        val sevR40 = severidadeAcima(indices.desbalR40, limites.desbalR40Atencao, limites.desbalR40Critico)
        val sevL100 = severidadeAcima(indices.desbalL100, limites.desbalLAtencao, limites.desbalLCritico)
        val sevL1k = severidadeAcima(indices.desbalL1k, limites.desbalLAtencao, limites.desbalLCritico)
        val sevZ100 = severidadeAcima(indices.desbalZ100, limites.desbalZAtencao, limites.desbalZCritico)
        val sevZ1k = severidadeAcima(indices.desbalZ1k, limites.desbalZAtencao, limites.desbalZCritico)
        val sevZ10k = severidadeAcima(indices.desbalZ10k, limites.desbalZAtencao, limites.desbalZCritico)
        val sevTheta1k = severidadeAcima(indices.deltaTheta1k, limites.deltaThetaAtencao, limites.deltaThetaCritico)
        val sevTheta10k = severidadeAcima(indices.deltaTheta10k, limites.deltaThetaAtencao, limites.deltaThetaCritico)
        val sevSpreadIf = severidadeAcima(indices.spreadIf, limites.spreadIfAtencao, limites.spreadIfCritico)
        val sevC = severidadeAcima(indices.desbalCapacitancia, limites.desbalCAtencao, limites.desbalCCritico)
        val sevAmpRic = severidadeAcima(indices.ric?.espalhamentoPercentual, limites.ricAmplitudeAtencao, limites.ricAmplitudeCritico)
        val sevSenoRic = severidadeAcima(indices.ric?.desvioSenoidalPercentual, limites.ricSenoideAtencao, limites.ricSenoideCritico)
        val sevIsolacao = severidadeAbaixo(indices.isolacaoMOhm, limites.isolacaoAtencaoMOhm, limites.isolacaoCriticoMOhm)
        val sevPi = severidadeAbaixo(indices.pi, limites.piAtencao, limites.piCritico)

        if (indices.desbalR40 == null) naoAvaliados += "Desbalanceamento de R40"
        if (indices.desbalL100 == null && indices.desbalL1k == null) naoAvaliados += "Desbalanceamento de L"
        if (indices.desbalZ1k == null) naoAvaliados += "Desbalanceamento de Z"
        if (indices.deltaTheta1k == null) naoAvaliados += "Delta θ"
        if (indices.spreadIf == null) naoAvaliados += "Spread I/F"
        if (indices.desbalCapacitancia == null) naoAvaliados += "Desbalanceamento de C para terra"
        if (indices.ric?.espalhamentoPercentual == null) naoAvaliados += "Espalhamento de amplitude do RIC"
        if (indices.ric?.desvioSenoidalPercentual == null) naoAvaliados += "Desvio senoidal do RIC"
        if (indices.isolacaoMOhm == null) naoAvaliados += "Isolação"
        if (indices.pi == null) naoAvaliados += "Índice de polarização"

        val indutivosForaDoLimite = listOf(sevL100, sevL1k, sevZ100, sevZ1k, sevZ10k, sevTheta1k, sevTheta10k)
        val algumIndutivoFora = indutivosForaDoLimite.any { forade(it) }
        val todosIndutivosOk = indutivosForaDoLimite.any { it != null } && !algumIndutivoFora

        // 1 — R40 desbalanceada com L, Z e θ dentro do limite
        if (forade(sevR40) && todosIndutivosOk) {
            achados += Achado(
                titulo = "Conexão, cabo ou terminal com alta resistência",
                severidade = sevR40 ?: Severidade.ATENCAO,
                evidencia = "Desbalanceamento de R40 = %.2f%% com L, Z e θ dentro dos limites".format(indices.desbalR40),
                detalhe = "O desequilíbrio aparece só na resistência: procure conexão frouxa ou oxidada, " +
                    "terminal mal crimpado, cabo danificado ou emenda de má qualidade no circuito de força.",
            )
        }

        // 2 — L, Z e θ desbalanceados com R40 dentro do limite
        if (algumIndutivoFora && sevR40 == Severidade.OK) {
            achados += Achado(
                titulo = "Curto entre espiras (falha de estator)",
                severidade = pior(sevL100, sevL1k, sevZ100, sevZ1k, sevZ10k, sevTheta1k, sevTheta10k),
                evidencia = evidenciaIndutiva(indices) + " com R40 em %.2f%%".format(indices.desbalR40 ?: 0.0),
                detalhe = "O desequilíbrio está no comportamento indutivo e não na resistência, padrão " +
                    "típico de espiras em curto no enrolamento do estator.",
            )
        }

        // 3 — desbalanceamento maior a 10 kHz do que a 1 kHz
        val z1k = indices.desbalZ1k
        val z10kValor = indices.desbalZ10k
        if (z1k != null && z10kValor != null && z10kValor > z1k) {
            achados += Achado(
                titulo = "Curto entre espiras incipiente",
                severidade = pior(sevZ10k, Severidade.ATENCAO),
                evidencia = "Desbalanceamento de Z sobe de %.2f%% em 1 kHz para %.2f%% em 10 kHz".format(z1k, z10kValor),
                detalhe = "A anomalia cresce com a frequência, o que reforça a suspeita de falha entre " +
                    "espiras em estágio inicial. Reensaie em intervalo curto para confirmar a tendência.",
            )
        }

        // 4 — todos equilibrados, mas I/F caindo nas três fases ao mesmo tempo
        val ifs = indices.ifPorPar
        if (ifs.size == Par.entries.size && sevSpreadIf == Severidade.OK && !algumIndutivoFora && !forade(sevR40)) {
            val baseIf = baseline?.ifPorPar
            if (baseIf == null || baseIf.size < Par.entries.size) {
                naoAvaliados += "Contaminação do enrolamento (exige ensaio anterior como referência)"
            } else {
                val quedas = Par.entries.mapNotNull { par ->
                    val atual = ifs[par]; val antes = baseIf[par]
                    if (atual == null || antes == null) null else atual - antes
                }
                // "Queda anormal simultânea": as três variam na mesma direção, além
                // do limite de spread — único limite especificado para o índice I/F.
                if (quedas.size == Par.entries.size && quedas.all { it > limites.spreadIfAtencao }) {
                    achados += Achado(
                        titulo = "Contaminação do enrolamento",
                        severidade = Severidade.ATENCAO,
                        evidencia = "I/F subiu nas três fases em relação ao ensaio anterior " +
                            "(%s p.p.)".format(quedas.joinToString(", ") { "%.2f".format(it) }),
                        detalhe = "Os índices seguem equilibrados entre si, mas a resposta em frequência " +
                            "mudou nas três fases ao mesmo tempo — padrão de contaminação ou " +
                            "degradação generalizada do enrolamento, não de falha localizada.",
                    )
                }
            }
        }

        // 5 — capacitância para terra desbalanceada ou acima do baseline histórico
        val mediaC = indices.capacitanciaMediaNf
        val baseC = baseline?.capacitanciaMediaNf
        val acimaDoBaseline = mediaC != null && baseC != null && baseC > 0 && mediaC > baseC
        if (forade(sevC) || acimaDoBaseline) {
            achados += Achado(
                titulo = "Umidade ou contaminação",
                severidade = if (forade(sevC)) sevC!! else Severidade.ATENCAO,
                evidencia = listOfNotNull(
                    indices.desbalCapacitancia?.let { "Desbalanceamento de C para terra = %.2f%%".format(it) },
                    if (acimaDoBaseline) "média subiu de %.2f nF para %.2f nF desde o ensaio de referência"
                        .format(baseC, mediaC) else null,
                ).joinToString(" • "),
                detalhe = "Capacitância para terra desigual entre as fases, ou acima do histórico do " +
                    "próprio motor, indica umidade absorvida ou contaminação depositada no enrolamento. " +
                    "Avalie secagem e limpeza antes de novo ensaio.",
            )
        }

        // 6 — isolação ou PI abaixo do limite
        if (forade(sevIsolacao) || forade(sevPi)) {
            achados += Achado(
                titulo = "Isolação para terra degradada",
                severidade = pior(sevIsolacao, sevPi),
                evidencia = listOfNotNull(
                    indices.isolacaoMOhm?.let { "Isolação = %.0f MΩ".format(it) },
                    indices.pi?.let { "PI = %.2f".format(it) },
                ).joinToString(" • "),
                detalhe = "A isolação contra a carcaça está abaixo do critério adotado. Os limites da " +
                    "IEEE 43 são referência: o que manda é a tendência histórica do próprio motor.",
            )
        }

        // 7 — RIC fora do limite
        if (forade(sevAmpRic) || forade(sevSenoRic)) {
            achados += Achado(
                titulo = "Rotor: excentricidade ou irregularidade de gaiola",
                severidade = pior(sevAmpRic, sevSenoRic),
                evidencia = listOfNotNull(
                    indices.ric?.espalhamentoPercentual?.let { "Espalhamento de amplitude = %.2f%%".format(it) },
                    indices.ric?.desvioSenoidalPercentual?.let { "Desvio senoidal = %.2f%%".format(it) },
                ).joinToString(" • "),
                detalhe = "A variação da indutância com o ângulo do eixo fugiu do padrão esperado, o que " +
                    "aponta excentricidade ou irregularidade na gaiola. Ressalva importante: o RIC por " +
                    "ponte LCR tem baixa sensibilidade a uma ou duas barras quebradas isoladas — a " +
                    "confirmação exige MCSA com o motor carregado.",
            )
        }

        // 8 — nada fora do limite
        if (achados.isEmpty()) {
            val avaliouAlgo = listOf(
                sevR40, sevL100, sevL1k, sevZ100, sevZ1k, sevZ10k, sevTheta1k, sevTheta10k,
                sevSpreadIf, sevC, sevAmpRic, sevSenoRic, sevIsolacao, sevPi,
            ).any { it != null }
            if (avaliouAlgo) {
                achados += Achado(
                    titulo = "Sem anomalia detectada",
                    severidade = Severidade.OK,
                    evidencia = "Todos os índices avaliados estão dentro dos limites adotados",
                    detalhe = "Mantenha o ensaio periódico: a comparação com o histórico do próprio motor " +
                        "detecta degradação antes de qualquer limite absoluto ser ultrapassado.",
                )
            }
        }

        return Parecer(indices, achados, naoAvaliados.distinct())
    }

    private fun evidenciaIndutiva(indices: Indices): String = listOfNotNull(
        indices.desbalL1k?.let { "L(1 kHz) = %.2f%%".format(it) },
        indices.desbalZ1k?.let { "Z(1 kHz) = %.2f%%".format(it) },
        indices.deltaTheta1k?.let { "Δθ = %.2f°".format(it) },
    ).joinToString(" • ")

    /** Calcula os índices e emite o parecer numa única chamada. */
    fun avaliar(leituras: Leituras, limites: LimitesMca = LimitesMca(), baseline: Baseline? = null): Parecer =
        diagnosticar(calcularIndices(leituras), limites, baseline)

    // ------------------------------------------------------------------
    // Codecs das grades (colunas de texto do banco)
    // ------------------------------------------------------------------
    //
    // Formato: "CHAVE=v1,v2,...|CHAVE=..." — valor vazio significa não medido.
    // Mantido em Kotlin puro para ser testável junto com o motor de cálculo.

    private fun juntar(itens: List<Pair<String, List<Double?>>>): String =
        itens.joinToString("|") { (chave, valores) ->
            "$chave=" + valores.joinToString(",") { it?.toString() ?: "" }
        }

    private fun separar(texto: String): Map<String, List<Double?>> =
        texto.split("|").mapNotNull { item ->
            val corte = item.indexOf('=')
            if (corte <= 0) return@mapNotNull null
            val chave = item.substring(0, corte)
            val valores = item.substring(corte + 1).split(",").map { it.trim().toDoubleOrNull() }
            chave to valores
        }.toMap()

    fun codificarResistencias(dados: Map<Par, LeituraResistencia>): String =
        juntar(Par.entries.map { par ->
            val leitura = dados[par] ?: LeituraResistencia()
            par.name to (List(3) { leitura.repeticoes.getOrNull(it) } + leitura.theta)
        })

    fun decodificarResistencias(texto: String): Map<Par, LeituraResistencia> {
        val mapa = separar(texto)
        return Par.entries.mapNotNull { par ->
            val valores = mapa[par.name] ?: return@mapNotNull null
            par to LeituraResistencia(
                repeticoes = List(3) { valores.getOrNull(it) },
                theta = valores.getOrNull(3),
            )
        }.toMap()
    }

    /** Guarda L e Z das duas frequências numa única coluna. */
    fun codificarLZ(cem: Map<Par, LeituraLZ>, mil: Map<Par, LeituraLZ>): String =
        juntar(Par.entries.map { par ->
            par.name to listOf(cem[par]?.l, cem[par]?.z, mil[par]?.l, mil[par]?.z)
        })

    fun decodificarLZ(texto: String): Pair<Map<Par, LeituraLZ>, Map<Par, LeituraLZ>> {
        val mapa = separar(texto)
        val cem = mutableMapOf<Par, LeituraLZ>()
        val mil = mutableMapOf<Par, LeituraLZ>()
        Par.entries.forEach { par ->
            val v = mapa[par.name] ?: return@forEach
            cem[par] = LeituraLZ(v.getOrNull(0), v.getOrNull(1))
            mil[par] = LeituraLZ(v.getOrNull(2), v.getOrNull(3))
        }
        return cem to mil
    }

    fun codificarAltaFrequencia(dados: Map<Par, LeituraAltaFrequencia>): String =
        juntar(Par.entries.map { par -> par.name to listOf(dados[par]?.z, dados[par]?.theta) })

    fun decodificarAltaFrequencia(texto: String): Map<Par, LeituraAltaFrequencia> {
        val mapa = separar(texto)
        return Par.entries.mapNotNull { par ->
            val v = mapa[par.name] ?: return@mapNotNull null
            par to LeituraAltaFrequencia(v.getOrNull(0), v.getOrNull(1))
        }.toMap()
    }

    fun codificarCapacitancias(dados: Map<Fase, Double>): String =
        juntar(Fase.entries.map { fase -> fase.name to listOf(dados[fase]) })

    fun decodificarCapacitancias(texto: String): Map<Fase, Double> {
        val mapa = separar(texto)
        return Fase.entries.mapNotNull { fase ->
            mapa[fase.name]?.getOrNull(0)?.let { fase to it }
        }.toMap()
    }

    fun codificarRic(dados: Map<Par, List<Double?>>): String =
        juntar(Par.entries.map { par ->
            par.name to List(RIC_POSICOES) { dados[par]?.getOrNull(it) }
        })

    fun decodificarRic(texto: String): Map<Par, List<Double?>> {
        val mapa = separar(texto)
        return Par.entries.mapNotNull { par ->
            val v = mapa[par.name] ?: return@mapNotNull null
            par to List(RIC_POSICOES) { v.getOrNull(it) }
        }.toMap()
    }

    /** Ângulo (graus) de cada posição do RIC. */
    fun angulosRic(): List<Int> = List(RIC_POSICOES) { it * RIC_PASSO_GRAUS }
}
