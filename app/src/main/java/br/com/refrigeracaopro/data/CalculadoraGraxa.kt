package br.com.refrigeracaopro.data

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Calculadora de relubrificação (graxa) para mancais de rolamento.
 *
 * Baseada nas fórmulas usuais de engenharia de lubrificação (SKF/ISO):
 *  - Quantidade de relubrificação: Gp = 0,005 × D × B  (g), com D e B em mm.
 *  - Intervalo de relubrificação:  tf = bf × [ 14·10⁶ / (n·√dm) − 4·dm ]  (h),
 *    com dm = (d + D)/2 e fatores de correção para temperatura, orientação do
 *    eixo e condições ambientais.
 *
 * Os valores são de referência — a recomendação do fabricante do rolamento e
 * da graxa sempre prevalece.
 */
object CalculadoraGraxa {

    const val DENSIDADE = 0.9 // g/cm³ — densidade típica de graxa
    const val BOMBADA_PADRAO = 1.2 // g por bombada de uma pistola de graxa comum

    /** Tipo do rolamento e seu fator bf no intervalo de relubrificação. */
    enum class TipoRolamento(val rotulo: String, val bf: Double) {
        ESFERAS("Rígido de esferas", 1.0),
        CONTATO_ANGULAR("Contato angular", 1.0),
        AUTOCOMP_ESFERAS("Autocompensador de esferas", 1.0),
        ROLOS_CILINDRICOS("Rolos cilíndricos", 0.30),
        ROLOS_CONICOS("Rolos cônicos", 0.20),
        AUTOCOMP_ROLOS("Autocompensador de rolos", 0.20),
        AGULHAS("Agulhas", 0.20),
        AXIAL_ESFERAS("Axial de esferas", 0.35);

        companion object {
            val ROTULOS = values().map { it.rotulo }
            fun porRotulo(r: String) = values().firstOrNull { it.rotulo == r } ?: ESFERAS
        }
    }

    /** Rolamento de catálogo (dimensões em mm): furo, diâmetro externo e largura. */
    data class Rolamento(val designacao: String, val tipo: TipoRolamento, val furo: Int, val externo: Int, val largura: Int)

    /** Catálogo de rolamentos comuns (deep groove, cilíndricos, cônicos e autocompensadores). */
    val ROLAMENTOS: List<Rolamento> = listOf(
        // Rígidos de esferas — série 60
        Rolamento("6000", TipoRolamento.ESFERAS, 10, 26, 8),
        Rolamento("6001", TipoRolamento.ESFERAS, 12, 28, 8),
        Rolamento("6002", TipoRolamento.ESFERAS, 15, 32, 9),
        Rolamento("6003", TipoRolamento.ESFERAS, 17, 35, 10),
        Rolamento("6004", TipoRolamento.ESFERAS, 20, 42, 12),
        Rolamento("6005", TipoRolamento.ESFERAS, 25, 47, 12),
        Rolamento("6006", TipoRolamento.ESFERAS, 30, 55, 13),
        Rolamento("6007", TipoRolamento.ESFERAS, 35, 62, 14),
        Rolamento("6008", TipoRolamento.ESFERAS, 40, 68, 15),
        Rolamento("6009", TipoRolamento.ESFERAS, 45, 75, 16),
        Rolamento("6010", TipoRolamento.ESFERAS, 50, 80, 16),
        // Rígidos de esferas — série 62
        Rolamento("6200", TipoRolamento.ESFERAS, 10, 30, 9),
        Rolamento("6201", TipoRolamento.ESFERAS, 12, 32, 10),
        Rolamento("6202", TipoRolamento.ESFERAS, 15, 35, 11),
        Rolamento("6203", TipoRolamento.ESFERAS, 17, 40, 12),
        Rolamento("6204", TipoRolamento.ESFERAS, 20, 47, 14),
        Rolamento("6205", TipoRolamento.ESFERAS, 25, 52, 15),
        Rolamento("6206", TipoRolamento.ESFERAS, 30, 62, 16),
        Rolamento("6207", TipoRolamento.ESFERAS, 35, 72, 17),
        Rolamento("6208", TipoRolamento.ESFERAS, 40, 80, 18),
        Rolamento("6209", TipoRolamento.ESFERAS, 45, 85, 19),
        Rolamento("6210", TipoRolamento.ESFERAS, 50, 90, 20),
        Rolamento("6211", TipoRolamento.ESFERAS, 55, 100, 21),
        Rolamento("6212", TipoRolamento.ESFERAS, 60, 110, 22),
        // Rígidos de esferas — série 63
        Rolamento("6300", TipoRolamento.ESFERAS, 10, 35, 11),
        Rolamento("6301", TipoRolamento.ESFERAS, 12, 37, 12),
        Rolamento("6302", TipoRolamento.ESFERAS, 15, 42, 13),
        Rolamento("6303", TipoRolamento.ESFERAS, 17, 47, 14),
        Rolamento("6304", TipoRolamento.ESFERAS, 20, 52, 15),
        Rolamento("6305", TipoRolamento.ESFERAS, 25, 62, 17),
        Rolamento("6306", TipoRolamento.ESFERAS, 30, 72, 19),
        Rolamento("6307", TipoRolamento.ESFERAS, 35, 80, 21),
        Rolamento("6308", TipoRolamento.ESFERAS, 40, 90, 23),
        Rolamento("6309", TipoRolamento.ESFERAS, 45, 100, 25),
        Rolamento("6310", TipoRolamento.ESFERAS, 50, 110, 27),
        Rolamento("6311", TipoRolamento.ESFERAS, 55, 120, 29),
        Rolamento("6312", TipoRolamento.ESFERAS, 60, 130, 31),
        // Rolos cilíndricos — NU
        Rolamento("NU204", TipoRolamento.ROLOS_CILINDRICOS, 20, 47, 14),
        Rolamento("NU205", TipoRolamento.ROLOS_CILINDRICOS, 25, 52, 15),
        Rolamento("NU206", TipoRolamento.ROLOS_CILINDRICOS, 30, 62, 16),
        Rolamento("NU207", TipoRolamento.ROLOS_CILINDRICOS, 35, 72, 17),
        Rolamento("NU208", TipoRolamento.ROLOS_CILINDRICOS, 40, 80, 18),
        Rolamento("NU209", TipoRolamento.ROLOS_CILINDRICOS, 45, 85, 19),
        Rolamento("NU210", TipoRolamento.ROLOS_CILINDRICOS, 50, 90, 20),
        Rolamento("NU308", TipoRolamento.ROLOS_CILINDRICOS, 40, 90, 23),
        Rolamento("NU309", TipoRolamento.ROLOS_CILINDRICOS, 45, 100, 25),
        Rolamento("NU310", TipoRolamento.ROLOS_CILINDRICOS, 50, 110, 27),
        // Rolos cônicos — 302/322
        Rolamento("30205", TipoRolamento.ROLOS_CONICOS, 25, 52, 16),
        Rolamento("30206", TipoRolamento.ROLOS_CONICOS, 30, 62, 17),
        Rolamento("30207", TipoRolamento.ROLOS_CONICOS, 35, 72, 18),
        Rolamento("30208", TipoRolamento.ROLOS_CONICOS, 40, 80, 20),
        Rolamento("30210", TipoRolamento.ROLOS_CONICOS, 50, 90, 22),
        Rolamento("32208", TipoRolamento.ROLOS_CONICOS, 40, 80, 25),
        Rolamento("32210", TipoRolamento.ROLOS_CONICOS, 50, 90, 25),
        // Autocompensadores de rolos — 222
        Rolamento("22205", TipoRolamento.AUTOCOMP_ROLOS, 25, 52, 18),
        Rolamento("22206", TipoRolamento.AUTOCOMP_ROLOS, 30, 62, 20),
        Rolamento("22208", TipoRolamento.AUTOCOMP_ROLOS, 40, 80, 23),
        Rolamento("22210", TipoRolamento.AUTOCOMP_ROLOS, 50, 90, 23),
        Rolamento("22212", TipoRolamento.AUTOCOMP_ROLOS, 60, 110, 28),
        Rolamento("22215", TipoRolamento.AUTOCOMP_ROLOS, 75, 130, 31),
        Rolamento("22308", TipoRolamento.AUTOCOMP_ROLOS, 40, 90, 33),
        Rolamento("22310", TipoRolamento.AUTOCOMP_ROLOS, 50, 110, 40),
    )

    /** Orientação do eixo (fator no intervalo). */
    enum class Orientacao(val rotulo: String, val fator: Double) {
        HORIZONTAL("Horizontal", 1.0),
        VERTICAL("Vertical", 0.5);

        companion object {
            val ROTULOS = values().map { it.rotulo }
            fun porRotulo(r: String) = values().firstOrNull { it.rotulo == r } ?: HORIZONTAL
        }
    }

    /** Condição ambiental (fator no intervalo). */
    enum class Ambiente(val rotulo: String, val fator: Double) {
        LIMPO("Limpo / seco", 1.0),
        MODERADO("Moderado (poeira/umidade)", 0.6),
        SEVERO("Severo (poeira, umidade, vibração)", 0.3);

        companion object {
            val ROTULOS = values().map { it.rotulo }
            fun porRotulo(r: String) = values().firstOrNull { it.rotulo == r } ?: LIMPO
        }
    }

    /** Preset de aplicação: ajusta as condições e recomenda o tipo de graxa. */
    data class Aplicacao(
        val nome: String,
        val orientacao: Orientacao,
        val ambiente: Ambiente,
        val temperatura: Int,
        val recomendacao: String,
    )

    val APLICACOES: List<Aplicacao> = listOf(
        Aplicacao("Motor elétrico", Orientacao.HORIZONTAL, Ambiente.LIMPO, 70,
            "Graxa de lítio ou poliureia NLGI 2. Acima de ~1800 rpm, prefira poliureia."),
        Aplicacao("Ventilador / exaustor", Orientacao.HORIZONTAL, Ambiente.MODERADO, 80,
            "Graxa de lítio EP2. Atenção à temperatura do mancal e ao desbalanceamento."),
        Aplicacao("Bomba centrífuga", Orientacao.HORIZONTAL, Ambiente.MODERADO, 70,
            "Graxa de lítio EP2 resistente à água."),
        Aplicacao("Compressor parafuso (motor)", Orientacao.HORIZONTAL, Ambiente.LIMPO, 80,
            "Siga o manual; em geral graxa de poliureia para alta rotação/temperatura."),
        Aplicacao("Britador / peneira vibratória", Orientacao.HORIZONTAL, Ambiente.SEVERO, 80,
            "Graxa EP2/EP3 com aditivo. A vibração reduz muito o intervalo — relubrifique com frequência."),
        Aplicacao("Esteira transportadora", Orientacao.HORIZONTAL, Ambiente.SEVERO, 60,
            "Graxa de lítio EP2 resistente a água e contaminação."),
        Aplicacao("Mancal de eixo vertical", Orientacao.VERTICAL, Ambiente.MODERADO, 70,
            "Use graxa mais consistente (NLGI 3) e reduza o intervalo (eixo vertical retém menos graxa)."),
        Aplicacao("Alta temperatura (estufa/secador)", Orientacao.HORIZONTAL, Ambiente.LIMPO, 120,
            "Graxa de complexo de lítio ou poliureia para alta temperatura."),
    )

    data class Entrada(
        val furo: Double, val externo: Double, val largura: Double,
        val rpm: Double, val tipo: TipoRolamento,
        val tempC: Double, val orientacao: Orientacao, val ambiente: Ambiente,
        val gramasPorBombada: Double,
    )

    data class Resultado(
        val dm: Double,
        val fatorVelocidade: Double,
        val quantidadeG: Double,
        val quantidadeCm3: Double,
        val bombadas: Double,
        val intervaloHoras: Double?,
        val intervaloDias: Double?,
        val avisos: List<String>,
    )

    fun calcular(e: Entrada): Resultado {
        val dm = (e.furo + e.externo) / 2.0
        val fatorVel = e.rpm * dm
        val qG = 0.005 * e.externo * e.largura
        val qCm3 = if (DENSIDADE > 0) qG / DENSIDADE else 0.0
        val bombadas = if (e.gramasPorBombada > 0) qG / e.gramasPorBombada else 0.0

        val fatorTemp = if (e.tempC <= 70) 1.0 else 0.5.pow((e.tempC - 70) / 15.0)

        val avisos = mutableListOf<String>()
        var tf: Double? = null
        if (e.rpm > 0 && dm > 0) {
            val base = (14_000_000.0 / (e.rpm * sqrt(dm))) - 4 * dm
            if (base <= 0) {
                avisos.add("Rotação muito alta para este diâmetro: a relubrificação com graxa fica inviável — considere lubrificação a óleo ou graxa especial de alta rotação.")
            } else {
                tf = e.tipo.bf * base * fatorTemp * e.orientacao.fator * e.ambiente.fator
            }
        } else if (e.rpm <= 0) {
            avisos.add("Sem rotação informada: o intervalo não pôde ser calculado. Relubrifique por calendário (ex.: a cada 6–12 meses).")
        }

        if (e.tempC > 70) avisos.add("Temperatura acima de 70 °C: o intervalo foi reduzido e exige graxa apropriada para a faixa de trabalho.")
        if (fatorVel > 500_000) avisos.add("Fator de velocidade (n·dm) elevado: confirme se a graxa suporta a rotação.")
        avisos.add("Valores de referência. A recomendação do fabricante do rolamento e da graxa prevalece.")

        return Resultado(dm, fatorVel, qG, qCm3, bombadas, tf, tf?.div(24.0), avisos)
    }
}
