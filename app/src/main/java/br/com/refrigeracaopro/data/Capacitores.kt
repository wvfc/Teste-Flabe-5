package br.com.refrigeracaopro.data

import kotlin.math.roundToInt

/**
 * Calculadora estimativa de capacitores (permanente/trabalho e de partida)
 * para motores monofásicos usados em refrigeração e climatização.
 *
 * IMPORTANTE: valores ESTIMADOS. Sempre confira a plaqueta do motor ou o
 * manual do fabricante. Para o capacitor permanente, use tensão igual ou
 * superior à original. Para partida, use sempre relé/PTC/centrífugo adequado.
 */
object Capacitores {

    enum class UnidadePotencia(val rotulo: String, val paraWatts: Double) {
        CV("CV", 735.5),
        HP("HP", 745.7),
        W("W", 1.0),
    }

    enum class Aplicacao(val rotulo: String) {
        COMPRESSOR("Compressor"),
        VENTILADOR("Ventilador"),
        BOMBA("Bomba"),
        EXAUSTOR("Exaustor"),
    }

    data class Entrada(
        val potencia: Double,
        val unidade: UnidadePotencia,
        val tensao: Double,
        val frequencia: Double = 60.0,
        val correnteInformada: Double? = null,
        val aplicacao: Aplicacao = Aplicacao.COMPRESSOR,
    )

    data class Resultado(
        val potenciaWatts: Double,
        val correnteEstimada: Double,
        val capacitorPermanenteUf: Int,
        val capacitorPartidaMinUf: Int,
        val capacitorPartidaMaxUf: Int,
        val tensaoMinimaCapacitor: Int,
        val observacoes: List<String>,
    )

    /**
     * Estima o capacitor permanente a partir da corrente do enrolamento
     * auxiliar. Fórmula usual: C(µF) = (I_aux × 1.000.000) / (2π·f·V).
     *
     * Como geralmente só temos a corrente total do motor, estimamos a corrente
     * do auxiliar como uma fração da corrente nominal, dependendo da aplicação.
     */
    fun calcular(e: Entrada): Resultado {
        val potenciaW = e.potencia * e.unidade.paraWatts
        val obs = mutableListOf<String>()

        // Corrente nominal: usa a informada ou estima por potência/tensão
        // (fator de potência e rendimento típicos ~0,75).
        val corrente = e.correnteInformada ?: run {
            obs += "Corrente não informada: estimada a partir da potência e tensão."
            potenciaW / (e.tensao * 0.75)
        }

        // Fração da corrente atribuída ao enrolamento auxiliar
        val fracaoAuxiliar = when (e.aplicacao) {
            Aplicacao.COMPRESSOR -> 0.55
            Aplicacao.BOMBA -> 0.50
            Aplicacao.VENTILADOR -> 0.45
            Aplicacao.EXAUSTOR -> 0.45
        }
        val correnteAux = corrente * fracaoAuxiliar

        val omega = 2 * Math.PI * e.frequencia
        val permanente = (correnteAux * 1_000_000.0) / (omega * e.tensao)

        // Capacitor de partida: tipicamente 4 a 8× o permanente
        val partidaMin = permanente * 4
        val partidaMax = permanente * 8

        // Tensão mínima do capacitor: margem sobre a tensão de operação
        val tensaoMinima = when {
            e.tensao <= 130 -> 250
            e.tensao <= 260 -> 440
            else -> 600
        }

        obs += "Capacitor permanente: use tensão igual ou superior à original (mín. ${tensaoMinima} VAC)."
        if (e.aplicacao == Aplicacao.COMPRESSOR) {
            obs += "Compressor: capacitor de partida deve ser usado com relé/PTC adequado e retirado após a partida."
        } else {
            obs += "Confirme se o motor usa partida por capacitor (PSC) ou capacitor de partida com chave centrífuga/relé."
        }
        obs += "Capacitor de partida é DIMENSIONADO PARA REGIME INTERMITENTE — nunca o use como permanente."

        return Resultado(
            potenciaWatts = potenciaW,
            correnteEstimada = corrente,
            capacitorPermanenteUf = arredondarComercial(permanente),
            capacitorPartidaMinUf = arredondarComercial(partidaMin),
            capacitorPartidaMaxUf = arredondarComercial(partidaMax),
            tensaoMinimaCapacitor = tensaoMinima,
            observacoes = obs,
        )
    }

    /** Aproxima para um valor comercial próximo de capacitor (µF). */
    private fun arredondarComercial(uf: Double): Int {
        val comerciais = listOf(
            1, 2, 3, 4, 5, 6, 8, 10, 12, 15, 20, 25, 30, 35, 40, 45, 50,
            60, 70, 80, 88, 100, 108, 124, 130, 145, 161, 189, 216, 233, 270, 324, 378
        )
        return comerciais.minByOrNull { kotlin.math.abs(it - uf) } ?: uf.roundToInt()
    }
}
