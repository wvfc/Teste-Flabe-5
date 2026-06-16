package br.com.refrigeracaopro.data

import kotlin.math.PI
import kotlin.math.sqrt

/**
 * Cálculos aproximados do projeto isométrico (auxiliares — não substituem
 * dimensionamento de engenharia).
 */
object CalculosProjeto {

    data class ItemMaterial(val descricao: String, val quantidade: Int, val detalhe: String)

    data class Resultado(
        val comprimentoTotal: Double,       // m (tubos + trechos)
        val qtdCurvas: Int,
        val qtdTees: Int,
        val qtdValvulas: Int,
        val qtdRegistros: Int,
        val qtdFiltros: Int,
        val qtdConexoes: Int,                // links entre componentes
        val comprimentoEquivalente: Double,  // m
        val velocidade: Double?,             // m/s
        val perdaCarga: Double?,             // bar (aprox.)
        val pressaoEstimada: Double?,        // bar
        val sugestaoDiametro: String,
        val materiais: List<ItemMaterial>,
    )

    // Comprimento equivalente aproximado por tipo de conexão (m), valor médio
    private fun equivalente(tipo: String): Double = when {
        tipo.contains("Curva 90", true) || tipo.contains("Curva curta", true) -> 1.5
        tipo.contains("Curva 45", true) -> 0.8
        tipo.contains("Curva longa", true) -> 1.0
        tipo.contains("Tee", true) || tipo.contains("Cruzeta", true) -> 2.0
        tipo.contains("Registro", true) || tipo.contains("Válvula", true) -> 3.0
        tipo.contains("Redução", true) -> 0.5
        else -> 0.3
    }

    fun calcular(estado: EstadoProjeto, cadastro: Projeto): Resultado {
        val comps = estado.componentes
        fun num(s: String) = s.replace(",", ".").filter { it.isDigit() || it == '.' || it == '-' }.toDoubleOrNull()

        // Comprimento dos tubos (componentes "Tubo") + trechos (conexões)
        val compTubos = comps.filter { it.tipo.startsWith("Tubo", true) || it.categoria == "Tubulações" }.sumOf { num(it.comprimento) ?: 0.0 }
        val compTrechos = estado.conexoes.sumOf { num(it.comprimento) ?: 0.0 }
        val comprimentoTotal = compTubos + compTrechos

        val curvas = comps.count { it.tipo.contains("Curva", true) }
        val tees = comps.count { it.tipo.contains("Tee", true) || it.tipo.contains("Cruzeta", true) }
        val valvulas = comps.count { it.categoria == "Válvulas" }
        val registros = comps.count { it.tipo.contains("Registro", true) }
        val filtros = comps.count { it.tipo.startsWith("Filtro", true) || it.tipo.startsWith("Separador", true) }
        val conexoesLinks = estado.conexoes.size

        val equivAcessorios = comps.filter {
            it.categoria == "Conexões" || it.categoria == "Válvulas" ||
                it.tipo.startsWith("Filtro", true) || it.tipo.startsWith("Separador", true)
        }.sumOf { equivalente(it.tipo) }
        val comprimentoEquivalente = comprimentoTotal + equivAcessorios

        // Diâmetro de referência (mm): menor diâmetro informado entre tubos/trechos
        val diametros = (comps.mapNotNull { num(it.diametro) } + estado.conexoes.mapNotNull { num(it.diametroInterno) })
            .filter { it > 0 }
        val dRefMm = diametros.minOrNull()

        // Vazão (assume L/s se número simples; converte de m³/h se o texto indicar)
        val vazaoTxt = cadastro.vazao.lowercase()
        val vazaoNum = num(cadastro.vazao)
        val vazaoLs = when {
            vazaoNum == null -> null
            vazaoTxt.contains("m³/h") || vazaoTxt.contains("m3/h") -> vazaoNum * 1000.0 / 3600.0
            vazaoTxt.contains("l/min") -> vazaoNum / 60.0
            else -> vazaoNum // assume L/s
        }

        var velocidade: Double? = null
        if (vazaoLs != null && dRefMm != null && dRefMm > 0) {
            val area = PI * (dRefMm / 1000.0) * (dRefMm / 1000.0) / 4.0 // m²
            velocidade = (vazaoLs / 1000.0) / area // m/s
        }

        // Perda de carga MUITO aproximada (proporcional ao comprimento equivalente
        // e ao quadrado da velocidade) — apenas indicativa.
        var perda: Double? = null
        if (velocidade != null && dRefMm != null && dRefMm > 0) {
            val fator = 0.02
            val perdaPa = fator * (comprimentoEquivalente / (dRefMm / 1000.0)) * (1.2 * velocidade * velocidade / 2.0)
            perda = perdaPa / 100000.0 // Pa -> bar
        }

        val pTrab = num(cadastro.pressaoTrabalho)
        val pressaoEstimada = if (pTrab != null && perda != null) (pTrab - perda).coerceAtLeast(0.0) else pTrab

        // Sugestão de diâmetro para velocidade-alvo (ar ≈ 6 m/s, líquido ≈ 1,5 m/s)
        val alvo = if (cadastro.fluido.contains("líquido", true) || cadastro.fluido.startsWith("R", true)) 1.5 else 6.0
        val sugestao = if (vazaoLs != null && vazaoLs > 0) {
            val d = sqrt((4.0 * (vazaoLs / 1000.0)) / (PI * alvo)) * 1000.0 // mm
            "≈ %.0f mm (para %.1f m/s)".format(d, alvo)
        } else "Informe a vazão para calcular"

        // Lista de materiais (agrupa por tipo)
        val materiais = comps.groupBy { it.tipo }.map { (tipo, lista) ->
            val det = lista.firstOrNull { it.diametro.isNotBlank() }?.diametro?.let { "Ø $it" } ?: ""
            ItemMaterial(tipo, lista.size, det)
        }.sortedBy { it.descricao } +
            if (comprimentoTotal > 0) listOf(ItemMaterial("Tubo (comprimento total)", 1, "%.1f m".format(comprimentoTotal))) else emptyList()

        return Resultado(
            comprimentoTotal, curvas, tees, valvulas, registros, filtros, conexoesLinks,
            comprimentoEquivalente, velocidade, perda, pressaoEstimada, sugestao, materiais
        )
    }

    /** Resumo textual (para a IA e para o PDF). */
    fun resumo(r: Resultado): String = buildString {
        appendLine("Comprimento total: %.1f m".format(r.comprimentoTotal))
        appendLine("Comprimento equivalente: %.1f m".format(r.comprimentoEquivalente))
        appendLine("Curvas: ${r.qtdCurvas} | Tees: ${r.qtdTees} | Válvulas: ${r.qtdValvulas} | Registros: ${r.qtdRegistros} | Filtros: ${r.qtdFiltros} | Conexões: ${r.qtdConexoes}")
        r.velocidade?.let { appendLine("Velocidade do fluido: %.2f m/s".format(it)) }
        r.perdaCarga?.let { appendLine("Perda de carga aprox.: %.2f bar".format(it)) }
        r.pressaoEstimada?.let { appendLine("Pressão estimada: %.2f bar".format(it)) }
        appendLine("Sugestão de diâmetro: ${r.sugestaoDiametro}")
    }
}
