package br.com.refrigeracaopro.data

/**
 * Calculadora estimativa de carga de fluido refrigerante para sistemas com
 * linha de líquido (ex.: splits e sistemas com linhas longas).
 *
 * IMPORTANTE: o método de referência é a CARGA POR PESO (balança). Esta
 * calculadora apenas estima a carga adicional pelo comprimento extra de linha
 * de líquido, a partir da carga nominal de etiqueta. O valor final depende do
 * fabricante. Para blends (R404A, R407C, R410A) carregue SEMPRE em fase líquida.
 */
object CargaFluido {

    /** Gramas de fluido por metro adicional de linha de líquido, por diâmetro. */
    data class DiametroLinha(val polegada: String, val gramasPorMetro: Int)

    val DIAMETROS_LIQUIDO = listOf(
        DiametroLinha("1/4\"", 20),
        DiametroLinha("5/16\"", 30),
        DiametroLinha("3/8\"", 55),
        DiametroLinha("1/2\"", 110),
        DiametroLinha("5/8\"", 160),
        DiametroLinha("3/4\"", 250),
    )

    /** Comprimento de linha já coberto pela carga de fábrica (padrão típico). */
    const val COMPRIMENTO_PADRAO_M = 5.0

    val BLENDS_FASE_LIQUIDA = listOf("R404A", "R407C", "R410A", "R507", "R448A", "R449A")

    data class Entrada(
        val fluido: String,
        val cargaNominalGramas: Double,
        val comprimentoTotalM: Double,
        val comprimentoPadraoM: Double = COMPRIMENTO_PADRAO_M,
        val diametroLiquido: DiametroLinha,
    )

    data class Resultado(
        val cargaAdicionalGramas: Double,
        val cargaTotalGramas: Double,
        val orientacoes: List<String>,
    )

    fun calcular(e: Entrada): Resultado {
        val excedente = (e.comprimentoTotalM - e.comprimentoPadraoM).coerceAtLeast(0.0)
        val adicional = excedente * e.diametroLiquido.gramasPorMetro
        val total = e.cargaNominalGramas + adicional

        val orient = mutableListOf<String>()
        orient += "Priorize SEMPRE a carga por peso, usando balança de precisão."
        orient += "Registre o peso inicial e final do cilindro para confirmar a carga."
        if (BLENDS_FASE_LIQUIDA.any { e.fluido.startsWith(it, ignoreCase = true) }) {
            orient += "Fluido ${e.fluido}: é um blend — carregue em FASE LÍQUIDA (cilindro invertido / válvula de líquido)."
        }
        orient += "Linha excedente considerada: %.1f m × %d g/m = %.0f g.".format(
            excedente, e.diametroLiquido.gramasPorMetro, adicional
        )
        orient += "O valor final depende do fabricante — confirme na etiqueta/manual do equipamento."

        return Resultado(adicional, total, orient)
    }
}
