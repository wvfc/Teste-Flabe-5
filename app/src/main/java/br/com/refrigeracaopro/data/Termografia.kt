package br.com.refrigeracaopro.data

import kotlin.math.pow

/**
 * Análise termográfica (inspeção por infravermelho).
 *
 * As temperaturas são digitadas pelo técnico a partir do que a câmera mostra —
 * o app não lê o arquivo radiométrico do equipamento.
 *
 * Cálculos implementados:
 *  - ΔT sobre componente similar e ΔT sobre o ambiente;
 *  - correção do ΔT para a carga nominal: ΔT ∝ I², logo
 *    ΔT_nominal = ΔT_medido × (I_nominal / I_medida)²;
 *  - correção por vento em medição externa;
 *  - reestimativa da temperatura quando a câmera estava com a emissividade
 *    errada;
 *  - menor alvo mensurável a partir da relação D:S e da distância.
 *
 * A classificação de severidade segue as faixas consagradas de inspeção
 * elétrica (NETA MTS / NFPA 70B). Os valores são de referência — a norma
 * aplicável e o critério do responsável técnico sempre prevalecem.
 */
object Termografia {

    const val EMISSIVIDADE_PADRAO = 0.95

    /** Abaixo disso a medição não representa a condição de operação. */
    const val CARGA_MINIMA_PERCENTUAL = 40.0

    /** O alvo deve ser bem maior que o menor ponto que a câmera resolve. */
    const val FATOR_ALVO_SEGURO = 3.0

    // ------------------------------------------------------------------
    // Emissividade por material
    // ------------------------------------------------------------------

    /** Material da superfície medida e sua emissividade típica. */
    data class Material(
        val nome: String,
        val emissividade: Double,
        val faixa: String,
        val grupo: String,
        val dica: String = "",
    )

    const val GRUPO_METAIS = "Metais"
    const val GRUPO_ELETRICO = "Elétrico / industrial"
    const val GRUPO_CONSTRUCAO = "Construção"
    const val GRUPO_DIVERSOS = "Diversos"

    private const val DICA_POLIDO =
        "Superfície polida reflete o ambiente e engana a câmera: cole uma fita isolante fosca " +
            "ou aplique tinta fosca no ponto, espere estabilizar e meça sobre ela com ε = 0,95."

    val MATERIAIS: List<Material> = listOf(
        // Metais
        Material("Alumínio polido", 0.05, "0,04 – 0,06", GRUPO_METAIS, DICA_POLIDO),
        Material("Alumínio anodizado", 0.55, "0,50 – 0,60", GRUPO_METAIS),
        Material("Alumínio oxidado", 0.25, "0,20 – 0,30", GRUPO_METAIS),
        Material("Cobre polido", 0.03, "0,02 – 0,05", GRUPO_METAIS, DICA_POLIDO),
        Material("Cobre oxidado", 0.70, "0,60 – 0,80", GRUPO_METAIS),
        Material("Latão polido", 0.04, "0,03 – 0,05", GRUPO_METAIS, DICA_POLIDO),
        Material("Latão oxidado", 0.60, "0,50 – 0,70", GRUPO_METAIS),
        Material("Aço polido / laminado", 0.10, "0,07 – 0,15", GRUPO_METAIS, DICA_POLIDO),
        Material("Aço oxidado / enferrujado", 0.80, "0,75 – 0,85", GRUPO_METAIS),
        Material("Aço galvanizado", 0.28, "0,23 – 0,35", GRUPO_METAIS),
        Material("Aço inoxidável polido", 0.17, "0,15 – 0,20", GRUPO_METAIS, DICA_POLIDO),
        Material("Aço inoxidável oxidado", 0.55, "0,50 – 0,60", GRUPO_METAIS),
        Material("Ferro fundido", 0.80, "0,60 – 0,90", GRUPO_METAIS),
        // Elétrico / industrial
        Material("Barramento pintado", 0.93, "0,90 – 0,95", GRUPO_ELETRICO),
        Material("Cabo com isolação de PVC", 0.93, "0,90 – 0,95", GRUPO_ELETRICO),
        Material("Fita isolante fosca", 0.95, "0,93 – 0,97", GRUPO_ELETRICO,
            "Referência prática: é a superfície usada para medir metal polido."),
        Material("Porcelana / isolador", 0.92, "0,90 – 0,95", GRUPO_ELETRICO),
        Material("Baquelite / epóxi", 0.94, "0,92 – 0,96", GRUPO_ELETRICO),
        Material("Carcaça de motor pintada", 0.94, "0,90 – 0,96", GRUPO_ELETRICO),
        Material("Isolamento térmico de tubulação", 0.93, "0,90 – 0,95", GRUPO_ELETRICO),
        // Construção
        Material("Concreto", 0.92, "0,90 – 0,95", GRUPO_CONSTRUCAO),
        Material("Tijolo comum", 0.93, "0,90 – 0,95", GRUPO_CONSTRUCAO),
        Material("Reboco / argamassa", 0.91, "0,89 – 0,93", GRUPO_CONSTRUCAO),
        Material("Telha de fibrocimento", 0.95, "0,93 – 0,96", GRUPO_CONSTRUCAO),
        Material("Vidro", 0.92, "0,90 – 0,94", GRUPO_CONSTRUCAO,
            "Nunca meça através do vidro: ele é opaco ao infravermelho e a câmera lê a " +
                "superfície do próprio vidro, não o que está atrás."),
        Material("Madeira", 0.90, "0,85 – 0,95", GRUPO_CONSTRUCAO),
        // Diversos
        Material("Tinta fosca (qualquer cor)", 0.95, "0,93 – 0,97", GRUPO_DIVERSOS,
            "A cor não altera a emissividade no infravermelho — o acabamento (fosco/brilhante) altera."),
        Material("Tinta brilhante", 0.88, "0,85 – 0,92", GRUPO_DIVERSOS),
        Material("Borracha", 0.95, "0,92 – 0,97", GRUPO_DIVERSOS),
        Material("Plástico (opaco)", 0.94, "0,90 – 0,97", GRUPO_DIVERSOS),
        Material("Água", 0.96, "0,95 – 0,98", GRUPO_DIVERSOS),
        Material("Gelo", 0.97, "0,96 – 0,98", GRUPO_DIVERSOS),
        Material("Pele humana", 0.98, "0,97 – 0,99", GRUPO_DIVERSOS),
        Material("Fuligem / carbono", 0.95, "0,94 – 0,97", GRUPO_DIVERSOS),
    )

    val GRUPOS = listOf(GRUPO_METAIS, GRUPO_ELETRICO, GRUPO_CONSTRUCAO, GRUPO_DIVERSOS)

    fun material(nome: String): Material? = MATERIAIS.firstOrNull { it.nome == nome }

    // ------------------------------------------------------------------
    // Severidade
    // ------------------------------------------------------------------

    /** Classificação da anomalia, do normal ao crítico. */
    enum class Severidade(val rotulo: String, val acao: String) {
        NORMAL("Normal", "Sem ação corretiva. Manter o acompanhamento periódico."),
        INVESTIGAR("Possível deficiência", "Investigar na próxima oportunidade e repetir a medição."),
        DEFICIENCIA("Deficiência provável", "Programar reparo na próxima parada de manutenção."),
        GRAVE("Discrepância grave", "Monitorar de perto e corrigir assim que possível."),
        CRITICO("Crítico", "Reparo imediato. Avaliar desligamento do circuito/equipamento."),
    }

    /** Classes de isolamento de motores e o limite de temperatura de cada uma. */
    val CLASSES_ISOLAMENTO = listOf(
        "—" to 0.0,
        "Classe A (105 °C)" to 105.0,
        "Classe E (120 °C)" to 120.0,
        "Classe B (130 °C)" to 130.0,
        "Classe F (155 °C)" to 155.0,
        "Classe H (180 °C)" to 180.0,
    )

    fun limiteClasse(rotulo: String): Double? =
        CLASSES_ISOLAMENTO.firstOrNull { it.first == rotulo }?.second?.takeIf { it > 0 }

    // ------------------------------------------------------------------
    // Entrada e resultado
    // ------------------------------------------------------------------

    data class Entrada(
        val tempPonto: Double? = null,
        val tempSimilar: Double? = null,
        val tempAmbiente: Double? = null,
        val correnteMedida: Double? = null,
        val correnteNominal: Double? = null,
        val emissividade: Double = EMISSIVIDADE_PADRAO,
        val emissividadeCamera: Double? = null,
        val tempRefletida: Double? = null,
        val distanciaM: Double? = null,
        val relacaoDS: Double? = null,
        val anguloGraus: Double? = null,
        val externo: Boolean = false,
        val ventoMs: Double? = null,
        val classeIsolamento: String = "—",
    )

    data class Resultado(
        val deltaTSimilar: Double? = null,
        val deltaTAmbiente: Double? = null,
        /** ΔT projetado para a corrente nominal. */
        val deltaTCorrigidoCarga: Double? = null,
        val percentualCarga: Double? = null,
        /** ΔT após o fator de vento (medição externa). */
        val deltaTCorrigidoVento: Double? = null,
        /** ΔT efetivamente usado na classificação. */
        val deltaTAvaliado: Double? = null,
        val temperaturaCorrigidaEmissividade: Double? = null,
        val menorAlvoMm: Double? = null,
        val alvoMinimoRecomendadoMm: Double? = null,
        val margemClasse: Double? = null,
        val severidade: Severidade? = null,
        val diagnostico: String = "",
        val recomendacao: String = "",
        val avisos: List<String> = emptyList(),
    )

    // ------------------------------------------------------------------
    // Cálculos
    // ------------------------------------------------------------------

    /**
     * Projeta o ΔT medido para a corrente nominal. O aquecimento por efeito
     * Joule cresce com o quadrado da corrente, então medir com a máquina
     * aliviada subestima muito a anomalia.
     */
    fun corrigirPorCarga(deltaT: Double, correnteMedida: Double, correnteNominal: Double): Double? {
        if (correnteMedida <= 0 || correnteNominal <= 0) return null
        return deltaT * (correnteNominal / correnteMedida).pow(2)
    }

    /** Fator de correção por vento em medição externa (interpolado). */
    fun fatorVento(velocidadeMs: Double): Double {
        val tabela = listOf(
            0.0 to 1.0, 1.0 to 1.00, 2.0 to 1.36, 3.0 to 1.64, 4.0 to 1.86,
            5.0 to 2.06, 6.0 to 2.23, 7.0 to 2.40, 8.0 to 2.54,
        )
        if (velocidadeMs <= 1.0) return 1.0
        if (velocidadeMs >= 8.0) return 2.54
        val proximo = tabela.indexOfFirst { it.first >= velocidadeMs }
        val (v1, f1) = tabela[proximo - 1]
        val (v2, f2) = tabela[proximo]
        return f1 + (f2 - f1) * (velocidadeMs - v1) / (v2 - v1)
    }

    /**
     * Reestima a temperatura quando a câmera mediu com emissividade diferente
     * da real, resolvendo o balanço de radiação em Kelvin:
     *
     *   ε_cam·T_aparente⁴ + (1−ε_cam)·T_refl⁴ = ε_real·T_real⁴ + (1−ε_real)·T_refl⁴
     *
     * É uma aproximação: ignora a transmissão atmosférica e assume o modelo
     * simplificado da câmera. Serve de conferência, não substitui reconfigurar
     * o equipamento e medir de novo.
     */
    fun corrigirEmissividade(
        tempAparenteC: Double,
        emissividadeCamera: Double,
        emissividadeReal: Double,
        tempRefletidaC: Double,
    ): Double? {
        if (emissividadeCamera <= 0 || emissividadeReal <= 0) return null
        if (emissividadeCamera > 1 || emissividadeReal > 1) return null
        val tAp = tempAparenteC + 273.15
        val tRefl = tempRefletidaC + 273.15
        if (tAp <= 0 || tRefl <= 0) return null
        val radiancia = emissividadeCamera * tAp.pow(4) + (1 - emissividadeCamera) * tRefl.pow(4)
        val real4 = (radiancia - (1 - emissividadeReal) * tRefl.pow(4)) / emissividadeReal
        if (real4 <= 0) return null
        return real4.pow(0.25) - 273.15
    }

    /** Menor alvo (mm) que a câmera consegue medir a uma dada distância. */
    fun menorAlvoMm(distanciaM: Double, relacaoDS: Double): Double? {
        if (distanciaM <= 0 || relacaoDS <= 0) return null
        return distanciaM * 1000.0 / relacaoDS
    }

    fun calcular(e: Entrada): Resultado {
        val avisos = mutableListOf<String>()

        val deltaSimilar = if (e.tempPonto != null && e.tempSimilar != null)
            e.tempPonto - e.tempSimilar else null
        val deltaAmbiente = if (e.tempPonto != null && e.tempAmbiente != null)
            e.tempPonto - e.tempAmbiente else null

        // Base da avaliação: o ΔT sobre componente similar é o critério
        // preferencial; sem ele, usa-se a elevação sobre o ambiente.
        val deltaBase = deltaSimilar ?: deltaAmbiente

        val percentualCarga = if (e.correnteMedida != null && e.correnteNominal != null && e.correnteNominal > 0)
            e.correnteMedida / e.correnteNominal * 100.0 else null

        val corrigidoCarga = if (deltaBase != null && e.correnteMedida != null && e.correnteNominal != null)
            corrigirPorCarga(deltaBase, e.correnteMedida, e.correnteNominal) else null

        val fatorVento = if (e.externo && e.ventoMs != null) fatorVento(e.ventoMs) else null
        val corrigidoVento = if (fatorVento != null) (corrigidoCarga ?: deltaBase)?.times(fatorVento) else null

        val avaliado = corrigidoVento ?: corrigidoCarga ?: deltaBase

        // Reavaliação da emissividade
        val corrigidaEmissividade = if (
            e.tempPonto != null && e.emissividadeCamera != null && e.tempRefletida != null &&
            kotlin.math.abs(e.emissividadeCamera - e.emissividade) > 0.001
        ) corrigirEmissividade(e.tempPonto, e.emissividadeCamera, e.emissividade, e.tempRefletida) else null

        val menorAlvo = if (e.distanciaM != null && e.relacaoDS != null)
            menorAlvoMm(e.distanciaM, e.relacaoDS) else null

        val limiteClasse = limiteClasse(e.classeIsolamento)
        val margemClasse = if (limiteClasse != null && e.tempPonto != null) limiteClasse - e.tempPonto else null

        // ---------- Avisos ----------
        if (percentualCarga != null && percentualCarga < CARGA_MINIMA_PERCENTUAL) {
            avisos += "Carga de apenas %.0f%% da nominal. Abaixo de %.0f%% a medição não representa a ".format(
                percentualCarga, CARGA_MINIMA_PERCENTUAL
            ) + "condição de operação: use o ΔT corrigido e, se possível, repita com carga maior."
        }
        if (percentualCarga == null && deltaBase != null) {
            avisos += "Correntes não informadas: o ΔT não foi projetado para a carga nominal. " +
                "Medição com equipamento aliviado subestima a anomalia."
        }
        if (corrigidaEmissividade != null) {
            avisos += "A câmera estava com ε = %.2f e a superfície tem ε = %.2f. A temperatura real ".format(
                e.emissividadeCamera ?: 0.0, e.emissividade
            ) + "estimada é de %.1f °C (valor aproximado — reconfigure a câmera e meça novamente).".format(
                corrigidaEmissividade
            )
        }
        if (e.tempRefletida == null && e.tempPonto != null) {
            avisos += "Temperatura refletida não informada. Meça-a com papel-alumínio amassado " +
                "diante do alvo — sem ela, superfícies de baixa emissividade dão leitura falsa."
        }
        if (e.emissividade < 0.6) {
            avisos += "Emissividade baixa (%.2f): a leitura é pouco confiável. Cole fita isolante ".format(e.emissividade) +
                "fosca no ponto e meça sobre ela com ε = 0,95."
        }
        if (e.anguloGraus != null && e.anguloGraus > 45) {
            avisos += "Ângulo de visada de %.0f° — acima de 45° a emissividade efetiva cai e a ".format(e.anguloGraus) +
                "leitura fica abaixo do real. Reposicione a câmera."
        }
        if (menorAlvo != null) {
            avisos += "A esta distância a câmera resolve no mínimo %.0f mm; o alvo deve ter pelo menos %.0f mm.".format(
                menorAlvo, menorAlvo * FATOR_ALVO_SEGURO
            )
        }
        if (margemClasse != null && margemClasse <= 0) {
            avisos += "Temperatura acima do limite da %s. Risco de perda de vida útil do isolamento.".format(e.classeIsolamento)
        } else if (margemClasse != null && margemClasse < 10) {
            avisos += "Faltam apenas %.0f °C para o limite da %s.".format(margemClasse, e.classeIsolamento)
        }
        if (deltaSimilar != null && deltaAmbiente != null && deltaSimilar < 1.0 && deltaAmbiente > 20.0) {
            avisos += "Todos os componentes estão igualmente quentes: o padrão sugere sobrecarga do " +
                "circuito, e não mau contato em um ponto específico."
        }
        if (e.externo && e.ventoMs == null) {
            avisos += "Medição externa sem velocidade do vento: o vento resfria o ponto e mascara a anomalia."
        }

        val severidade = severidadeDe(
            deltaSimilar = if (deltaSimilar != null) avaliado else null,
            deltaAmbiente = if (deltaSimilar == null) avaliado else deltaAmbiente,
            acimaDaClasse = margemClasse != null && margemClasse <= 0,
        )

        return Resultado(
            deltaTSimilar = deltaSimilar,
            deltaTAmbiente = deltaAmbiente,
            deltaTCorrigidoCarga = corrigidoCarga,
            percentualCarga = percentualCarga,
            deltaTCorrigidoVento = corrigidoVento,
            deltaTAvaliado = avaliado,
            temperaturaCorrigidaEmissividade = corrigidaEmissividade,
            menorAlvoMm = menorAlvo,
            alvoMinimoRecomendadoMm = menorAlvo?.times(FATOR_ALVO_SEGURO),
            margemClasse = margemClasse,
            severidade = severidade,
            diagnostico = severidade?.let { diagnosticoDe(it, deltaSimilar, deltaAmbiente, avaliado) } ?: "",
            recomendacao = severidade?.acao ?: "",
            avisos = avisos,
        )
    }

    /** Faixas de severidade (NETA MTS / NFPA 70B). */
    private fun severidadeDe(deltaSimilar: Double?, deltaAmbiente: Double?, acimaDaClasse: Boolean): Severidade? {
        if (deltaSimilar == null && deltaAmbiente == null) return if (acimaDaClasse) Severidade.CRITICO else null
        val porSimilar = deltaSimilar?.let {
            when {
                it < 1.0 -> Severidade.NORMAL
                it <= 3.0 -> Severidade.INVESTIGAR
                it <= 15.0 -> Severidade.DEFICIENCIA
                else -> Severidade.CRITICO
            }
        }
        val porAmbiente = deltaAmbiente?.let {
            when {
                it < 1.0 -> Severidade.NORMAL
                it <= 10.0 -> Severidade.INVESTIGAR
                it <= 20.0 -> Severidade.DEFICIENCIA
                it <= 40.0 -> Severidade.GRAVE
                else -> Severidade.CRITICO
            }
        }
        val pior = listOfNotNull(porSimilar, porAmbiente).maxByOrNull { it.ordinal }
        return if (acimaDaClasse) Severidade.CRITICO else pior
    }

    private fun diagnosticoDe(
        severidade: Severidade,
        deltaSimilar: Double?,
        deltaAmbiente: Double?,
        avaliado: Double?,
    ): String {
        val medidas = listOfNotNull(
            deltaSimilar?.let { "ΔT sobre similar = %.1f °C".format(it) },
            deltaAmbiente?.let { "ΔT sobre ambiente = %.1f °C".format(it) },
            avaliado?.takeIf { deltaSimilar != null && kotlin.math.abs(it - deltaSimilar) > 0.1 }
                ?.let { "ΔT corrigido = %.1f °C".format(it) },
        ).joinToString(" • ")
        val texto = when (severidade) {
            Severidade.NORMAL ->
                "Nenhuma anomalia térmica relevante. O ponto está na mesma faixa dos componentes " +
                    "de referência, comportamento esperado para a condição de carga medida."
            Severidade.INVESTIGAR ->
                "Elevação pequena, porém perceptível. Pode ser início de mau contato, desbalanceamento " +
                    "de carga ou variação normal entre componentes — repita a medição para confirmar a tendência."
            Severidade.DEFICIENCIA ->
                "Anomalia térmica confirmada. O padrão é típico de conexão frouxa, oxidada ou " +
                    "subdimensionada, ou de esforço mecânico localizado. Programe a correção."
            Severidade.GRAVE ->
                "Elevação expressiva sobre o ambiente. O componente está trabalhando muito acima do " +
                    "esperado e a degradação tende a acelerar — monitore e corrija na primeira oportunidade."
            Severidade.CRITICO ->
                "Anomalia crítica. Há risco de falha, perda do componente e princípio de incêndio. " +
                    "Trate como reparo imediato e avalie desligar o circuito/equipamento."
        }
        return if (medidas.isBlank()) texto else "$medidas\n\n$texto"
    }

    // ------------------------------------------------------------------
    // Legendas das fotos ("caminho::legenda", separadas por "|")
    // ------------------------------------------------------------------

    fun codificarLegendas(legendas: Map<String, String>): String =
        legendas.filterValues { it.isNotBlank() }.entries.joinToString("|") { "${it.key}::${it.value}" }

    fun decodificarLegendas(texto: String): Map<String, String> =
        texto.split("|").mapNotNull { item ->
            val corte = item.indexOf("::")
            if (corte <= 0) null else item.substring(0, corte) to item.substring(corte + 2)
        }.toMap()

    // ------------------------------------------------------------------
    // Referência técnica
    // ------------------------------------------------------------------

    val PASSO_A_PASSO = listOf(
        "1. Confirme a condição de operação: o equipamento deve estar em carga representativa (idealmente acima de 40% da nominal) e em regime térmico estável.",
        "2. Ajuste a emissividade da câmera conforme o material da superfície antes de medir.",
        "3. Meça a temperatura refletida: amasse uma folha de papel-alumínio, alise-a com o lado brilhante para fora, posicione-a diante do alvo e meça com ε = 1,0. O valor lido é a temperatura refletida.",
        "4. Informe também a temperatura ambiente, a umidade relativa e a distância até o alvo.",
        "5. Posicione-se a menos de 45° da perpendicular à superfície — o ideal é abaixo de 30°.",
        "6. Enquadre o alvo ocupando boa parte da imagem, respeitando a relação D:S da câmera.",
        "7. Registre a imagem térmica e a foto visível do mesmo ponto.",
        "8. Meça um componente similar sob a mesma carga (fase vizinha, mancal do outro lado) para obter o ΔT de referência.",
        "9. Anote as correntes medida e nominal para permitir a correção do ΔT para a carga plena.",
    )

    val ERROS_COMUNS = listOf(
        "Medir através de vidro ou acrílico" to
            "O vidro é opaco ao infravermelho: a câmera lê a temperatura do próprio vidro. Abra o painel ou use janela de inspeção de IR.",
        "Deixar a emissividade em 0,95 para tudo" to
            "Em cobre ou alumínio polido o erro chega a dezenas de graus para baixo. Ajuste o ε ou meça sobre fita fosca.",
        "Ignorar a temperatura refletida" to
            "Em superfícies de baixa emissividade a câmera enxerga o reflexo do ambiente (e do próprio operador), não o alvo.",
        "Inspecionar com o equipamento aliviado" to
            "O aquecimento cresce com o quadrado da corrente. Com 30% de carga, um defeito grave parece inofensivo.",
        "Medir com sol direto ou vento" to
            "O sol aquece a superfície e o vento a resfria — os dois destroem o ΔT. Prefira o fim da tarde ou a noite em ambiente externo.",
        "Ângulo de visada muito aberto" to
            "Acima de 45° a emissividade efetiva cai e a leitura fica abaixo do real.",
        "Alvo pequeno demais para a distância" to
            "A câmera mede a média da área do pixel. Um parafuso medido de longe some na média da vizinhança.",
        "Comparar medições de dias diferentes sem contexto" to
            "Carga, ambiente e vento mudam. Compare sempre o ΔT, não a temperatura absoluta.",
    )

    /** Critérios de severidade para exibição em tabela. */
    val CRITERIOS = listOf(
        Triple("Até 1 °C", "Até 1 °C", "Normal — sem ação"),
        Triple("1 – 3 °C", "1 – 10 °C", "Possível deficiência — investigar"),
        Triple("4 – 15 °C", "11 – 20 °C", "Deficiência provável — reparar na próxima parada"),
        Triple("—", "21 – 40 °C", "Discrepância grave — monitorar e corrigir"),
        Triple("Acima de 15 °C", "Acima de 40 °C", "Crítico — reparo imediato"),
    )

    /** Ponto de inspeção típico e o que observar nele. */
    data class Aplicacao(val equipamento: String, val oQueOlhar: String, val sintoma: String)

    val APLICACOES = listOf(
        Aplicacao(
            "Quadro elétrico / partida do compressor",
            "Bornes, contatores, disjuntores, emendas e barramentos, com o painel aberto e em carga.",
            "Ponto quente isolado em uma fase = conexão frouxa ou oxidada. As três fases igualmente quentes = sobrecarga.",
        ),
        Aplicacao(
            "Motor elétrico",
            "Carcaça, caixa de ligação e tampas, comparando com o histórico e com a classe de isolamento.",
            "Elevação geral aponta sobrecarga, ventilação obstruída ou desbalanceamento de tensão.",
        ),
        Aplicacao(
            "Mancais e rolamentos",
            "Cada mancal comparado com o do lado oposto e com máquinas iguais.",
            "Mancal mais quente que o par indica falta ou excesso de graxa, desalinhamento ou início de falha.",
        ),
        Aplicacao(
            "Acoplamento, correias e polias",
            "Região do acoplamento e das polias com a máquina em operação.",
            "Aquecimento localizado sugere desalinhamento, correia patinando ou tensão incorreta.",
        ),
        Aplicacao(
            "Condensador",
            "Superfície da serpentina e diferença entre entrada e saída.",
            "Aquecimento desigual indica sujeira, aletas obstruídas ou ventilador parado — sobe a pressão de descarga.",
        ),
        Aplicacao(
            "Evaporador",
            "Distribuição de temperatura ao longo da serpentina.",
            "Regiões frias e quentes alternadas indicam má distribuição de fluido, falta de carga ou obstrução.",
        ),
        Aplicacao(
            "Linha de descarga e de sucção",
            "Toda a extensão da tubulação e o isolamento térmico.",
            "Descarga muito quente sugere sobrecarga ou condensação deficiente; falhas no isolamento aparecem como manchas quentes.",
        ),
        Aplicacao(
            "Filtro secador e válvulas",
            "Corpo do filtro e das válvulas, comparando entrada e saída.",
            "Queda brusca de temperatura ao longo do componente indica restrição/entupimento.",
        ),
        Aplicacao(
            "Purgadores e linhas de ar comprimido",
            "Corpo do purgador e trechos da rede.",
            "Purgador quente permanentemente indica passagem contínua de ar (vazamento); frio demais pode indicar entupimento.",
        ),
        Aplicacao(
            "Secador de ar",
            "Trocador de calor e linha de saída.",
            "Trocador com temperatura fora do padrão aponta perda de eficiência e risco de condensado na rede.",
        ),
    )

    val TEXTO_EMISSIVIDADE =
        "Emissividade (ε) é a fração da radiação que a superfície emite em relação a um corpo negro " +
            "ideal. É o parâmetro que mais afeta a leitura: com ε errado, a temperatura sai errada.\n\n" +
            "Superfícies foscas e não metálicas (tinta, borracha, concreto, isolação de cabo) ficam " +
            "entre 0,90 e 0,96 e são fáceis de medir. Metais polidos ficam abaixo de 0,10 e refletem o " +
            "ambiente — nesses casos a saída prática é colar fita isolante fosca ou aplicar tinta fosca " +
            "no ponto, esperar estabilizar e medir sobre ela com ε = 0,95.\n\n" +
            "A cor da superfície não muda a emissividade no infravermelho; o acabamento, sim."

    val TEXTO_CARGA =
        "O aquecimento por efeito Joule é proporcional ao quadrado da corrente. Por isso o ΔT medido " +
            "só vale para a carga do momento da medição, e a projeção para a carga nominal é " +
            "ΔT_nominal = ΔT_medido × (I_nominal ÷ I_medida)².\n\n" +
            "Na prática: um ΔT de 10 °C medido com 50% da carga equivale a cerca de 40 °C na carga " +
            "plena — de \"deficiência provável\" para \"crítico\". Medir com o equipamento aliviado e " +
            "concluir que está tudo bem é o erro mais comum em laudo termográfico."
}
