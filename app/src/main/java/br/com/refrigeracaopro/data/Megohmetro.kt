package br.com.refrigeracaopro.data

import kotlin.math.pow

/**
 * Diagnóstico de ensaio com megôhmetro (resistência de isolamento).
 *
 * Índices calculados:
 *  - Isolação puntual: leitura de 60 s (valor de referência do ensaio).
 *  - DAR (Índice de Absorção Dielétrica) = R60s / R30s.
 *  - PI (Índice de Polarização) = R10min / R1min.
 *
 * Interpretação baseada na prática da IEEE 43 (Recommended Practice for
 * Testing Insulation Resistance of Electric Machinery).
 */
object Megohmetro {

    /** Condição do isolamento, do pior para o melhor. */
    enum class Condicao(val rotulo: String, val cor: Long) {
        PERIGOSO("Perigoso", 0xFFC62828),
        POBRE("Pobre", 0xFFEF6C00),
        DUVIDOSO("Duvidoso", 0xFFF9A825),
        BOM("Bom", 0xFF2E7D32),
        EXCELENTE("Excelente", 0xFF1565C0),
    }

    data class Entrada(
        val tensaoV: Double,
        val r30s: Double,
        val r60s: Double,
        val r10min: Double,
        val temperaturaC: Double?,
    )

    data class Resultado(
        val puntual: Double?,          // MΩ em 60 s
        val puntualCorrigido: Double?, // MΩ corrigido para 40 °C
        val dar: Double?,
        val pi: Double?,
        val condicao: Condicao?,
        val diagnostico: String,
        val avaliacaoDar: String?,
        val avaliacaoPi: String?,
        val minimoRecomendado: Double?, // MΩ (IEEE 43 / regra kV+1)
        val avisos: List<String>,
    )

    /** Temperatura base para correção das leituras (prática usual: 40 °C). */
    const val TEMPERATURA_BASE = 40.0

    /**
     * Fator de correção de temperatura: a resistência de isolamento cai pela
     * metade a cada 10 °C de aumento. Para corrigir uma leitura feita a T para
     * a temperatura base, multiplica-se por 2^((T - base)/10).
     */
    fun fatorCorrecao(temperaturaC: Double, base: Double = TEMPERATURA_BASE): Double =
        2.0.pow((temperaturaC - base) / 10.0)

    /** Tabela de fatores de correção para exibição (referência rápida). */
    val TABELA_CORRECAO: List<Pair<Int, Double>> =
        listOf(10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60).map { t -> t to fatorCorrecao(t.toDouble()) }

    /** Avaliação do PI conforme faixas usuais (IEEE 43). */
    fun avaliarPi(pi: Double): Pair<Condicao, String> = when {
        pi < 1.0 -> Condicao.PERIGOSO to "PI < 1,0 — Perigoso: a resistência cai ao longo do ensaio, indicando isolamento comprometido."
        pi < 2.0 -> Condicao.POBRE to "PI entre 1,0 e 2,0 — Pobre: isolamento com umidade ou contaminação relevante."
        pi < 4.0 -> Condicao.BOM to "PI entre 2,0 e 4,0 — Bom: isolamento em condição adequada."
        else -> Condicao.EXCELENTE to "PI acima de 4,0 — Excelente: isolamento seco e em ótimo estado."
    }

    /** Avaliação do DAR conforme faixas usuais. */
    fun avaliarDar(dar: Double): Pair<Condicao, String> = when {
        dar < 1.25 -> Condicao.POBRE to "DAR < 1,25 — Inadequado: sugere umidade ou contaminação."
        dar <= 1.6 -> Condicao.BOM to "DAR entre 1,25 e 1,6 — Aceitável."
        else -> Condicao.EXCELENTE to "DAR acima de 1,6 — Excelente."
    }

    /**
     * Mínimo recomendado de isolação (MΩ) pela regra prática kV + 1,
     * onde kV é a tensão nominal do equipamento em kV.
     */
    fun minimoRecomendado(tensaoV: Double): Double? =
        if (tensaoV <= 0) null else (tensaoV / 1000.0) + 1.0

    fun calcular(e: Entrada): Resultado {
        val avisos = mutableListOf<String>()

        val puntual = e.r60s.takeIf { it > 0 }
        val dar = if (e.r30s > 0 && e.r60s > 0) e.r60s / e.r30s else null
        val pi = if (e.r60s > 0 && e.r10min > 0) e.r10min / e.r60s else null

        val corrigido = if (puntual != null && e.temperaturaC != null)
            puntual * fatorCorrecao(e.temperaturaC) else null

        val avalDar = dar?.let { avaliarDar(it) }
        val avalPi = pi?.let { avaliarPi(it) }

        // A condição final segue o PI (índice mais confiável); sem PI, usa o DAR.
        var condicao = avalPi?.first ?: avalDar?.first

        val minimo = minimoRecomendado(e.tensaoV)
        val referencia = corrigido ?: puntual
        if (minimo != null && referencia != null && referencia < minimo) {
            avisos.add(
                "Isolação de %.0f MΩ abaixo do mínimo recomendado (%.1f MΩ pela regra kV + 1). Investigar antes de energizar."
                    .format(referencia, minimo)
            )
            // Isolação abaixo do mínimo rebaixa o diagnóstico
            if (condicao == null || condicao.ordinal > Condicao.DUVIDOSO.ordinal) condicao = Condicao.DUVIDOSO
        }

        if (pi != null && puntual != null && puntual > 5000) {
            avisos.add(
                "Isolação muito alta (> 5000 MΩ): conforme a IEEE 43, o PI perde significado nesses casos — " +
                    "considere apenas o valor puntual."
            )
        }
        if (e.temperaturaC == null) {
            avisos.add("Sem temperatura informada: os valores não foram corrigidos. Compare leituras sempre na mesma temperatura base.")
        }
        if (e.tensaoV <= 0) avisos.add("Informe a tensão de teste para verificar o mínimo recomendado.")

        val diagnostico = montarDiagnostico(condicao, avalPi?.second, avalDar?.second, pi, dar)

        return Resultado(
            puntual = puntual,
            puntualCorrigido = corrigido,
            dar = dar,
            pi = pi,
            condicao = condicao,
            diagnostico = diagnostico,
            avaliacaoDar = avalDar?.second,
            avaliacaoPi = avalPi?.second,
            minimoRecomendado = minimo,
            avisos = avisos,
        )
    }

    private fun montarDiagnostico(
        condicao: Condicao?, textoPi: String?, textoDar: String?, pi: Double?, dar: Double?,
    ): String {
        if (condicao == null) return "Preencha as leituras para obter o diagnóstico."
        val base = when (condicao) {
            Condicao.EXCELENTE -> "Isolamento em excelente estado. Equipamento apto à operação."
            Condicao.BOM -> "Isolamento em boas condições. Manter o acompanhamento periódico."
            Condicao.DUVIDOSO -> "Isolamento duvidoso. Reavaliar o ensaio e investigar umidade/contaminação antes de liberar."
            Condicao.POBRE -> "Isolamento pobre. Recomenda-se secagem/limpeza e novo ensaio antes de energizar."
            Condicao.PERIGOSO -> "Condição perigosa. NÃO energize o equipamento: há forte indício de falha de isolamento."
        }
        // Combinação típica de defeito: DAR baixo com PI aceitável
        val nota = if (dar != null && pi != null && dar < 1.25 && pi >= 2.0)
            "\n\nObservação: DAR baixo com PI aceitável sugere contaminação superficial ou umidade localizada inicial."
        else ""
        return listOfNotNull(base, textoPi, textoDar).joinToString("\n\n") + nota
    }

    // ---------- Conteúdo técnico de referência ----------

    val PASSO_A_PASSO = listOf(
        "1. Desenergize e aterre o equipamento.",
        "2. Conecte a garra EARTH na carcaça e LINE no condutor.",
        "3. Aplique a tensão.",
        "4. Anote a leitura em 30s, 60s e 10min.",
        "5. Desligue e aguarde a descarga automática antes de tocar.",
    )

    val EXPLICACAO_ENSAIOS = listOf(
        "Puntual (spot test)" to
            "Leitura única, normalmente em 60 s, usada para comparar com o mínimo recomendado e com o histórico do equipamento.",
        "DAR (Índice de Absorção)" to
            "Relação entre as leituras de 60 s e 30 s (R60s / R30s). Ensaio rápido, útil quando não há tempo para os 10 minutos.",
        "PI (Índice de Polarização)" to
            "Relação entre as leituras de 10 min e 1 min (R10min / R1min). É o índice mais confiável para avaliar umidade e envelhecimento.",
    )

    /** Defeitos comuns e suas causas prováveis. */
    val DEFEITOS = listOf(
        "Baixa isolação em todos os tempos" to
            "Umidade geral, contaminação severa, envelhecimento térmico.",
        "DAR baixo mas PI aceitável" to
            "Contaminação superficial ou umidade localizada inicial.",
        "Queda abrupta de resistência durante o teste" to
            "Ruptura dielétrica, rachadura na isolação.",
    )

    val TEXTO_CORRECAO =
        "A resistência de isolamento cai pela metade a cada 10 °C de aumento de temperatura. " +
            "Para comparar leituras, é necessário corrigir para uma temperatura base (geralmente 20 °C ou 40 °C).\n\n" +
            "Neste painel a correção usa 40 °C como base: o valor corrigido é a leitura multiplicada por " +
            "2^((T − 40)/10). Assim, ensaios feitos em dias e temperaturas diferentes podem ser comparados entre si."
}
