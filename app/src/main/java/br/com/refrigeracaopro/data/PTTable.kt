package br.com.refrigeracaopro.data

import kotlin.math.exp
import kotlin.math.ln

/**
 * Tabela P/T aproximada (pressão x temperatura de saturação).
 *
 * A curva de saturação de cada fluido é aproximada pela correlação de
 * Clausius-Clapeyron de dois parâmetros: ln(P) = A - B/T, ajustada pelo
 * ponto de ebulição normal e pelo ponto crítico do fluido.
 *
 * Precisão típica: ±0,5 a ±2 °C na faixa usual de trabalho — suficiente para
 * diagnóstico de campo, mas o técnico deve sempre conferir a tabela P/T
 * oficial do fabricante do fluido.
 */
object PTTable {

    private const val PRESSAO_ATM_BAR = 1.01325

    /**
     * @param tbC ponto de ebulição normal (°C, a 1 atm)
     * @param tcC temperatura crítica (°C)
     * @param pcBar pressão crítica (bar absoluto)
     */
    data class Fluido(val nome: String, val tbC: Double, val tcC: Double, val pcBar: Double) {
        private val a: Double
        private val b: Double

        init {
            // Ajuste de ln(P) = A - B/T pelos pontos (Tb, 1 atm) e (Tc, Pc)
            val tb = tbC + 273.15
            val tc = tcC + 273.15
            b = ln(pcBar / PRESSAO_ATM_BAR) / (1.0 / tb - 1.0 / tc)
            a = ln(PRESSAO_ATM_BAR) + b / tb
        }

        /** Pressão de saturação absoluta (bar) para uma temperatura em °C. */
        fun pressaoSaturacaoAbs(tempC: Double): Double = exp(a - b / (tempC + 273.15))

        /** Temperatura de saturação (°C) para uma pressão absoluta em bar. */
        fun tempSaturacao(pressaoAbsBar: Double): Double = b / (a - ln(pressaoAbsBar)) - 273.15
    }

    // Constantes termodinâmicas aproximadas (Tb a 1 atm, ponto crítico)
    val FLUIDOS = listOf(
        Fluido("R22", -40.8, 96.1, 49.9),
        Fluido("R134a", -26.1, 101.1, 40.6),
        Fluido("R404A", -46.6, 72.0, 37.3),
        Fluido("R407C", -43.8, 86.0, 46.3),
        Fluido("R410A", -51.4, 71.3, 49.0),
        Fluido("R507", -47.1, 70.6, 37.1),
        Fluido("R32", -51.7, 78.1, 57.8),
        Fluido("R290", -42.1, 96.7, 42.5),
        Fluido("R600a", -11.7, 134.7, 36.3),
        Fluido("R1234yf", -29.5, 94.7, 33.8),
        Fluido("R1234ze", -19.0, 109.4, 36.3),
        // CO2: ajustado pelo ponto triplo (-56,6 °C / 5,18 bar) e crítico
        recalibrar("CO2 / R744", -56.6, 5.18, 31.0, 73.8),
        Fluido("Amônia / R717", -33.3, 132.3, 113.3),
    )

    val NOMES = FLUIDOS.map { it.nome }

    fun porNome(nome: String): Fluido? = FLUIDOS.find { it.nome == nome }

    /** Cria um fluido calibrado por dois pontos (T1,P1) e (T2,P2) em bar absoluto. */
    private fun recalibrar(nome: String, t1C: Double, p1Bar: Double, t2C: Double, p2Bar: Double): Fluido {
        // Resolve o Tb equivalente para que a curva passe pelos dois pontos dados
        val tb1 = t1C + 273.15
        val tc = t2C + 273.15
        val b = ln(p2Bar / p1Bar) / (1.0 / tb1 - 1.0 / tc)
        val a = ln(p1Bar) + b / tb1
        // Tb "virtual" no qual P = 1 atm segundo essa curva
        val tbVirtual = b / (a - ln(PRESSAO_ATM_BAR)) - 273.15
        return Fluido(nome, tbVirtual, t2C, p2Bar)
    }

    /** Converte pressão manométrica (bar) em absoluta (bar). */
    fun manometricaParaAbsoluta(pManBar: Double): Double = pManBar + PRESSAO_ATM_BAR

    /** Converte psi para bar. */
    fun psiParaBar(psi: Double): Double = psi / 14.5038

    // ---------- Cálculos técnicos ----------

    data class ResultadoCalculo(
        val tempSaturacao: Double,
        val valor: Double,
        val interpretacao: String,
        val causas: String,
    )

    /**
     * Superaquecimento = T linha de sucção - T evaporação saturada.
     * Pressão de sucção em bar MANOMÉTRICO.
     */
    fun superaquecimento(fluido: Fluido, pressaoSuccaoManBar: Double, tempSuccaoC: Double): ResultadoCalculo {
        val tSat = fluido.tempSaturacao(manometricaParaAbsoluta(pressaoSuccaoManBar))
        val sa = tempSuccaoC - tSat
        val (interp, causas) = when {
            sa < 4 -> "MUITO BAIXO" to
                "Risco de retorno de líquido ao compressor. Possíveis causas: excesso de carga de fluido, " +
                "válvula de expansão muito aberta, bulbo da TEV mal posicionado/solto, baixa carga térmica no evaporador."
            sa <= 12 -> "NORMAL" to
                "Superaquecimento dentro da faixa típica (4–12 K para expansão por TEV). " +
                "Confirme com a especificação do fabricante do equipamento."
            else -> "ALTO" to
                "Evaporador 'faminto' de fluido. Possíveis causas: falta de carga de fluido, filtro secador ou " +
                "tubo capilar obstruído, válvula de expansão subdimensionada/travada, perda de carga excessiva na linha de líquido."
        }
        return ResultadoCalculo(tSat, sa, interp, causas)
    }

    /**
     * Subresfriamento = T condensação saturada - T linha de líquido.
     * Pressão de descarga/líquido em bar MANOMÉTRICO.
     */
    fun subresfriamento(fluido: Fluido, pressaoDescargaManBar: Double, tempLiquidoC: Double): ResultadoCalculo {
        val tSat = fluido.tempSaturacao(manometricaParaAbsoluta(pressaoDescargaManBar))
        val sr = tSat - tempLiquidoC
        val (interp, causas) = when {
            sr < 2 -> "BAIXO" to
                "Pouco líquido no condensador. Possíveis causas: falta de carga de fluido, vazamento, " +
                "condensador subdimensionado, flash gas na linha de líquido."
            sr <= 10 -> "NORMAL" to
                "Subresfriamento dentro da faixa típica (2–10 K). " +
                "Confirme com a especificação do fabricante do equipamento."
            else -> "ALTO" to
                "Acúmulo de líquido no condensador. Possíveis causas: excesso de carga de fluido, " +
                "condensador parcialmente bloqueado, não-condensáveis no sistema, restrição na linha de líquido."
        }
        return ResultadoCalculo(tSat, sr, interp, causas)
    }
}
