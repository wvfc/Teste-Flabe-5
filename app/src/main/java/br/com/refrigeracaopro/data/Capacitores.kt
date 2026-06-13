package br.com.refrigeracaopro.data

import kotlin.math.abs

/**
 * Estimativa de capacitores (permanente/trabalho e de partida) para motores
 * monofásicos de refrigeração e climatização.
 *
 * MÉTODO: usa faixas de REFERÊNCIA por potência (HP), que correspondem aos
 * valores típicos de campo — bem mais confiáveis do que uma fórmula que tende a
 * superestimar (especialmente o capacitor de partida).
 *
 * IMPORTANTE: valores ESTIMADOS. Sempre confira a plaqueta do motor ou o manual
 * do fabricante. Para o capacitor permanente, use tensão igual ou superior à
 * original. Para partida, use sempre relé/PTC/centrífugo adequado.
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
        val potenciaHp: Double,
        val faixaHpReferencia: String,
        val permanenteMinUf: Int,
        val permanenteMaxUf: Int,
        val partidaMinUf: Int,
        val partidaMaxUf: Int,
        val tensaoMinimaCapacitor: Int,
        val observacoes: List<String>,
    )

    /** Faixa de referência por potência (HP) → capacitores típicos (µF). */
    private data class Faixa(
        val hp: Double,
        val permMin: Int, val permMax: Int,
        val partMin: Int, val partMax: Int,
    )

    private val TABELA = listOf(
        Faixa(0.125, 4, 6, 36, 43),
        Faixa(0.25, 5, 8, 43, 53),
        Faixa(0.333, 8, 12, 53, 64),
        Faixa(0.5, 12, 20, 88, 108),
        Faixa(0.75, 20, 30, 108, 130),
        Faixa(1.0, 30, 40, 130, 161),
        Faixa(1.5, 40, 50, 161, 189),
        Faixa(2.0, 50, 70, 189, 233),
        Faixa(3.0, 70, 100, 233, 324),
    )

    fun calcular(e: Entrada): Resultado {
        val potenciaW = e.potencia * e.unidade.paraWatts
        val hp = potenciaW / 745.7
        val faixa = TABELA.minByOrNull { abs(it.hp - hp) } ?: TABELA.first()

        // Capacitores permanentes existem só em motores PSC (ventilador, bomba,
        // alguns compressores). Capacitor de partida em motores CSIR/CSR.
        val obs = mutableListOf<String>()
        obs += "Valores de referência para ~${faixaTexto(faixa.hp)} HP (motor monofásico)."
        obs += "Permanente: use tensão igual ou superior à original (mín. ${tensaoMinima(e.tensao)} VAC)."
        when (e.aplicacao) {
            Aplicacao.COMPRESSOR ->
                obs += "Compressor: o capacitor de partida deve ser usado com relé/PTC adequado e retirado após a partida."
            Aplicacao.VENTILADOR, Aplicacao.EXAUSTOR ->
                obs += "Ventilador/exaustor PSC: geralmente usa apenas capacitor permanente (sem capacitor de partida)."
            Aplicacao.BOMBA ->
                obs += "Bomba: confira se o motor é PSC (permanente) ou CSCR (permanente + partida)."
        }
        obs += "Capacitor de partida é para regime INTERMITENTE — nunca o use como permanente."
        e.correnteInformada?.let { obs += "Corrente informada: %.1f A (confira com a plaqueta).".format(it) }

        return Resultado(
            potenciaHp = hp,
            faixaHpReferencia = faixaTexto(faixa.hp),
            permanenteMinUf = faixa.permMin,
            permanenteMaxUf = faixa.permMax,
            partidaMinUf = faixa.partMin,
            partidaMaxUf = faixa.partMax,
            tensaoMinimaCapacitor = tensaoMinima(e.tensao),
            observacoes = obs,
        )
    }

    private fun tensaoMinima(tensaoOperacao: Double): Int = when {
        tensaoOperacao <= 130 -> 250
        tensaoOperacao <= 260 -> 380
        else -> 440
    }

    private fun faixaTexto(hp: Double): String = when (hp) {
        0.125 -> "1/8"
        0.25 -> "1/4"
        0.333 -> "1/3"
        0.5 -> "1/2"
        0.75 -> "3/4"
        1.0 -> "1"
        1.5 -> "1,5"
        2.0 -> "2"
        3.0 -> "3"
        else -> "%.2f".format(hp)
    }
}
