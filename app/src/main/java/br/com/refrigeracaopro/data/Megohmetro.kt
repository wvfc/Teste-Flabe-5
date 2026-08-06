package br.com.refrigeracaopro.data

import kotlin.math.pow

/**
 * Painel de diagnóstico de megômetro (megger).
 *
 * Calcula a isolação puntual (60 s), o Índice de Absorção (DAR) e o Índice de
 * Polarização (PI) a partir das leituras de resistência de isolamento, corrige
 * os valores pela temperatura e classifica a condição do equipamento conforme
 * as faixas usuais da **IEEE 43**.
 *
 *  - DAR = R60s / R30s
 *  - PI  = R10min / R1min
 *  - Correção de temperatura: a resistência cai pela metade a cada 10 °C de
 *    aumento, logo R_base = R_medido × 2^((T_medida − T_base) / 10).
 *
 * Os valores são de referência — a norma aplicável e a recomendação do
 * fabricante do equipamento sempre prevalecem.
 */
object Megohmetro {

    /** Temperaturas base usuais para correção das leituras. */
    const val TEMP_BASE_40 = 40.0
    const val TEMP_BASE_20 = 20.0

    /**
     * Acima deste valor (R60s corrigido) a IEEE 43 considera que os índices
     * DAR/PI podem perder o significado — a isolação já é muito alta.
     */
    const val ISOLACAO_ALTA_MOHM = 5000.0

    /** Condição geral do equipamento apresentada no diagnóstico automático. */
    enum class Condicao(val rotulo: String) {
        EXCELENTE("Excelente"),
        BOM("Bom"),
        DUVIDOSO("Duvidoso"),
        POBRE("Pobre"),
        PERIGOSO("Perigoso"),
    }

    /** Leituras do ensaio. Resistências em MΩ; 0 (ou vazio) = não informado. */
    data class Entrada(
        val tensaoV: Double = 0.0,
        val r30s: Double = 0.0,
        val r60s: Double = 0.0,
        val r10min: Double = 0.0,
        val tempC: Double? = null,
        val tempBase: Double = TEMP_BASE_40,
    )

    /** Resultado calculado a partir das leituras. */
    data class Resultado(
        /** Isolação puntual: leitura de 60 s (MΩ). */
        val puntual: Double? = null,
        /** Isolação puntual corrigida para a temperatura base (MΩ). */
        val puntualCorrigido: Double? = null,
        /** Fator aplicado na correção de temperatura. */
        val fatorTemperatura: Double? = null,
        val dar: Double? = null,
        val pi: Double? = null,
        val classeDar: String = "",
        val classePi: String = "",
        val condicao: Condicao? = null,
        val diagnostico: String = "",
        val recomendacao: String = "",
        /** Mínimo de referência (kV + 1) MΩ, a 40 °C. */
        val minimoRecomendado: Double? = null,
        val atendeMinimo: Boolean? = null,
        val avisos: List<String> = emptyList(),
    )

    /** Fator de correção da leitura para a temperatura base. */
    fun fatorCorrecao(tempC: Double, tempBase: Double): Double = 2.0.pow((tempC - tempBase) / 10.0)

    /** Classificação isolada do DAR (Índice de Absorção). */
    fun classificarDar(dar: Double): String = when {
        dar < 1.25 -> "Inadequado"
        dar <= 1.6 -> "Aceitável"
        else -> "Excelente"
    }

    /** Classificação isolada do PI (Índice de Polarização) — IEEE 43. */
    fun classificarPi(pi: Double): String = when {
        pi < 1.0 -> "Perigoso"
        pi < 2.0 -> "Pobre"
        pi < 4.0 -> "Bom"
        else -> "Excelente"
    }

    fun calcular(e: Entrada): Resultado {
        val r30 = e.r30s.takeIf { it > 0 }
        val r60 = e.r60s.takeIf { it > 0 }
        val r10 = e.r10min.takeIf { it > 0 }

        val dar = if (r30 != null && r60 != null) r60 / r30 else null
        val pi = if (r60 != null && r10 != null) r10 / r60 else null

        val fator = e.tempC?.let { fatorCorrecao(it, e.tempBase) }
        val puntualCorrigido = if (r60 != null && fator != null) r60 * fator else null

        // Mínimo clássico de referência da IEEE 43: (kV do ensaio + 1) MΩ a 40 °C
        val minimo = e.tensaoV.takeIf { it > 0 }?.let { it / 1000.0 + 1.0 }
        val referencia = puntualCorrigido ?: r60
        val atendeMinimo = if (minimo != null && referencia != null) referencia >= minimo else null

        // Resistência que cai ao longo do ensaio: sinal de ruptura dielétrica
        val quedaDurante = (r30 != null && r60 != null && r60 < r30) ||
            (r60 != null && r10 != null && r10 < r60)

        val avisos = mutableListOf<String>()
        if (r30 != null && r60 != null && r60 < r30) {
            avisos += "A leitura caiu de 30 s para 60 s. Queda de resistência durante o ensaio indica " +
                "ruptura dielétrica ou rachadura na isolação — interrompa e investigue."
        }
        if (r60 != null && r10 != null && r10 < r60) {
            avisos += "A leitura caiu de 1 min para 10 min. Isolação instável sob tensão: suspeita de " +
                "umidade, contaminação ou defeito dielétrico em evolução."
        }
        if (atendeMinimo == false && minimo != null) {
            avisos += "Isolação abaixo do mínimo de referência de %.1f MΩ (kV do ensaio + 1) da IEEE 43."
                .format(minimo)
        }
        if (referencia != null && referencia > ISOLACAO_ALTA_MOHM) {
            avisos += "Com isolação acima de 5.000 MΩ os índices DAR e PI podem perder o significado " +
                "(IEEE 43); nesse caso prevalece a leitura puntual."
        }
        if (e.tempC == null && r60 != null) {
            avisos += "Temperatura não informada: os valores não foram corrigidos e não devem ser " +
                "comparados diretamente com ensaios anteriores."
        }
        if (e.tensaoV > 0 && e.tensaoV < 250) {
            avisos += "Tensão de ensaio baixa (%.0f V) para máquinas industriais — confira a tensão nominal do equipamento."
                .format(e.tensaoV)
        }

        val condicao = condicaoDe(pi, dar, atendeMinimo, quedaDurante)

        return Resultado(
            puntual = r60,
            puntualCorrigido = puntualCorrigido,
            fatorTemperatura = fator,
            dar = dar,
            pi = pi,
            classeDar = dar?.let { classificarDar(it) } ?: "",
            classePi = pi?.let { classificarPi(it) } ?: "",
            condicao = condicao,
            diagnostico = condicao?.let { diagnosticoDe(it, pi, dar) } ?: "",
            recomendacao = condicao?.let { RECOMENDACOES[it] ?: "" } ?: "",
            minimoRecomendado = minimo,
            atendeMinimo = atendeMinimo,
            avisos = avisos,
        )
    }

    /**
     * Condição geral: o PI manda quando existe; sem ele, usa-se o DAR. Um DAR
     * inadequado com PI aceitável rebaixa o quadro para "Duvidoso" (indício de
     * contaminação superficial), assim como uma isolação abaixo do mínimo.
     */
    private fun condicaoDe(pi: Double?, dar: Double?, atendeMinimo: Boolean?, quedaDurante: Boolean): Condicao? {
        if (quedaDurante) return Condicao.PERIGOSO
        var condicao = when {
            pi != null -> when {
                pi < 1.0 -> Condicao.PERIGOSO
                pi < 2.0 -> Condicao.POBRE
                pi < 4.0 -> Condicao.BOM
                else -> Condicao.EXCELENTE
            }
            dar != null -> when {
                dar < 1.25 -> Condicao.POBRE
                dar <= 1.6 -> Condicao.DUVIDOSO
                else -> Condicao.BOM
            }
            else -> return null
        }
        if (pi != null && dar != null && dar < 1.25 && condicao.ordinal < Condicao.DUVIDOSO.ordinal) {
            condicao = Condicao.DUVIDOSO
        }
        if (atendeMinimo == false && condicao.ordinal < Condicao.DUVIDOSO.ordinal) {
            condicao = Condicao.DUVIDOSO
        }
        return condicao
    }

    private fun diagnosticoDe(condicao: Condicao, pi: Double?, dar: Double?): String {
        val indices = buildString {
            if (pi != null) append("PI = %.2f (%s)".format(pi, classificarPi(pi)))
            if (pi != null && dar != null) append(" • ")
            if (dar != null) append("DAR = %.2f (%s)".format(dar, classificarDar(dar)))
        }
        val base = DIAGNOSTICOS[condicao] ?: ""
        return if (indices.isBlank()) base else "$indices\n\n$base"
    }

    private val DIAGNOSTICOS = mapOf(
        Condicao.EXCELENTE to
            "Isolação em excelente estado. A resistência cresce bem ao longo do ensaio, " +
            "indicando isolação seca, limpa e com boa capacidade de polarização.",
        Condicao.BOM to
            "Isolação em bom estado, dentro do esperado para operação. Mantenha o " +
            "acompanhamento periódico para observar a tendência entre as medições.",
        Condicao.DUVIDOSO to
            "Isolação duvidosa: os índices não se confirmam entre si ou o valor está no " +
            "limite. Há indício de umidade ou contaminação superficial. Repita o ensaio " +
            "após limpeza e secagem antes de liberar o equipamento.",
        Condicao.POBRE to
            "Isolação pobre. A resistência quase não evolui com o tempo, comportamento " +
            "típico de umidade, sujeira condutiva ou envelhecimento da isolação. " +
            "Não recomendável operar sem tratamento.",
        Condicao.PERIGOSO to
            "Condição perigosa. A resistência não cresce (ou cai) durante o ensaio, o que " +
            "aponta isolação comprometida — umidade severa, contaminação ou ruptura " +
            "dielétrica. Não energize o equipamento.",
    )

    private val RECOMENDACOES = mapOf(
        Condicao.EXCELENTE to "Liberar para operação e manter o histórico das leituras para análise de tendência.",
        Condicao.BOM to "Liberar para operação e repetir o ensaio no próximo período de manutenção preventiva.",
        Condicao.DUVIDOSO to "Limpar, secar e repetir o ensaio. Comparar com leituras anteriores corrigidas na mesma temperatura.",
        Condicao.POBRE to "Programar secagem/limpeza da isolação e reensaiar. Investigar infiltração e contaminação.",
        Condicao.PERIGOSO to "Não energizar. Isolar o equipamento, investigar a falha e encaminhar para reparo/rebobinamento.",
    )

    // ------------------------------------------------------------------
    // Referência técnica
    // ------------------------------------------------------------------

    /** Procedimento comum a todos os ensaios (texto de referência). */
    val PASSO_A_PASSO_GERAL = listOf(
        "1. Desenergize e aterre o equipamento.",
        "2. Conecte a garra EARTH na carcaça e LINE no condutor.",
        "3. Aplique a tensão.",
        "4. Anote a leitura em 30s, 60s e 10min.",
        "5. Desligue e aguarde a descarga automática antes de tocar.",
    )

    /** Ensaio: o que fazer e como ler em cada um dos três testes. */
    data class Ensaio(val nome: String, val passos: List<String>, val nota: String)

    val ENSAIOS = listOf(
        Ensaio(
            nome = "Isolação Puntual (spot test)",
            passos = listOf(
                "Siga o procedimento comum acima.",
                "Selecione a tensão de ensaio conforme a tensão nominal do equipamento.",
                "Aplique a tensão e mantenha por 60 segundos.",
                "Anote a leitura de 60 s: esse é o valor de isolação puntual.",
                "Corrija a leitura para a temperatura base antes de comparar com ensaios anteriores.",
            ),
            nota = "Referência clássica da IEEE 43: mínimo de (kV do ensaio + 1) MΩ a 40 °C.",
        ),
        Ensaio(
            nome = "DAR — Índice de Absorção",
            passos = listOf(
                "Siga o procedimento comum acima.",
                "Aplique a tensão e dispare o cronômetro no mesmo instante.",
                "Anote a leitura aos 30 segundos (R30s).",
                "Sem interromper o ensaio, anote a leitura aos 60 segundos (R60s).",
                "Calcule DAR = R60s ÷ R30s.",
            ),
            nota = "Ensaio rápido (1 minuto), útil quando não há tempo para os 10 minutos do PI.",
        ),
        Ensaio(
            nome = "PI — Índice de Polarização",
            passos = listOf(
                "Siga o procedimento comum acima.",
                "Aplique a tensão e mantenha o ensaio por 10 minutos ininterruptos.",
                "Anote a leitura em 1 minuto (R1min).",
                "Anote a leitura em 10 minutos (R10min).",
                "Calcule PI = R10min ÷ R1min.",
            ),
            nota = "É o ensaio mais confiável para avaliar umidade e envelhecimento da isolação.",
        ),
    )

    /** Defeito comum observado no ensaio e suas causas prováveis. */
    data class Defeito(val sintoma: String, val causas: String)

    val DEFEITOS = listOf(
        Defeito(
            sintoma = "Baixa isolação em todos os tempos",
            causas = "Umidade geral, contaminação severa, envelhecimento térmico.",
        ),
        Defeito(
            sintoma = "DAR baixo mas PI aceitável",
            causas = "Contaminação superficial ou umidade localizada inicial.",
        ),
        Defeito(
            sintoma = "Queda abrupta de resistência durante o teste",
            causas = "Ruptura dielétrica, rachadura na isolação.",
        ),
    )

    val CORRECAO_TEMPERATURA_TEXTO =
        "A resistência de isolamento cai pela metade a cada 10 °C de aumento de temperatura. " +
            "Para comparar leituras, é necessário corrigir para uma temperatura base " +
            "(geralmente 20 °C ou 40 °C).\n\n" +
            "Ou seja: uma medição feita a 50 °C vale o dobro quando corrigida para 40 °C, e uma " +
            "medição feita a 30 °C vale a metade. Sem essa correção, o mesmo equipamento parece " +
            "piorar no verão e melhorar no inverno."

    const val CORRECAO_FORMULA = "R_base = R_medido × 2^((T_medida − T_base) ÷ 10)"

    /** Temperaturas de ensaio e os fatores de correção para 40 °C e 20 °C. */
    val TABELA_CORRECAO: List<Triple<Int, Double, Double>> =
        listOf(10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60).map { t ->
            Triple(t, fatorCorrecao(t.toDouble(), TEMP_BASE_40), fatorCorrecao(t.toDouble(), TEMP_BASE_20))
        }

    /** Tensões de ensaio usuais por faixa de tensão nominal do equipamento. */
    val TENSOES_ENSAIO = listOf(
        "Até 100 V" to "100 a 250 V",
        "440 a 550 V" to "500 a 1000 V",
        "2400 V" to "1000 a 2500 V",
        "4160 V ou mais" to "2500 a 5000 V",
    )
}
