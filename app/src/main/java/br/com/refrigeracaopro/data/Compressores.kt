package br.com.refrigeracaopro.data

/**
 * Base de referência de compressores/motores para pesquisa de compatibilidade.
 *
 * IMPORTANTE: dados aproximados para orientação. Uma substituição só é "direta"
 * quando coincidem fluido, faixa de aplicação, tensão/fase, óleo e capacidade
 * frigorífica próxima. Sempre confirme com o catálogo do fabricante.
 */
data class Compressor(
    val marca: String,
    val modelo: String,
    val potenciaHp: String,
    val tensao: String,
    val fase: String,            // Monofásico / Trifásico
    val frequencia: String,      // 50/60 Hz
    val correnteNominal: String,
    val fluido: String,
    val aplicacao: String,       // Baixa / Média / Alta temperatura
    val capacidadeFrigorifica: String, // ex.: "1.350 W @ -10°C/45°C"
    val tipoOleo: String,
    val tipoPartida: String,     // RSIR, RSCR, CSR, CSIR, Inverter...
    val tipoConexao: String,     // Solda / Rotalock / Flange
    val faixaEvaporacao: String, // envelope (°C)
    val observacoes: String = "",
)

object Compressores {

    val LISTA = listOf(
        Compressor(
            marca = "Embraco", modelo = "EMIS70HER", potenciaHp = "1/5",
            tensao = "220 V", fase = "Monofásico", frequencia = "60 Hz", correnteNominal = "1,2 A",
            fluido = "R134a", aplicacao = "Média/Alta temperatura",
            capacidadeFrigorifica = "≈ 280 W @ -10°C", tipoOleo = "POE",
            tipoPartida = "RSIR (PTC)", tipoConexao = "Solda",
            faixaEvaporacao = "-25 a +10 °C", observacoes = "Refrigeração comercial leve."
        ),
        Compressor(
            marca = "Embraco", modelo = "NEU6213GK", potenciaHp = "1/3",
            tensao = "220 V", fase = "Monofásico", frequencia = "60 Hz", correnteNominal = "2,3 A",
            fluido = "R404A/R507", aplicacao = "Baixa temperatura",
            capacidadeFrigorifica = "≈ 690 W @ -35°C", tipoOleo = "POE",
            tipoPartida = "CSIR (relé + capacitor de partida)", tipoConexao = "Solda",
            faixaEvaporacao = "-40 a -5 °C", observacoes = "Freezers e baixa temperatura."
        ),
        Compressor(
            marca = "Tecumseh", modelo = "AE4440Y-FZ1A", potenciaHp = "1/2",
            tensao = "220 V", fase = "Monofásico", frequencia = "60 Hz", correnteNominal = "4,1 A",
            fluido = "R404A", aplicacao = "Baixa/Média temperatura",
            capacidadeFrigorifica = "≈ 1.050 W @ -10°C", tipoOleo = "POE",
            tipoPartida = "CSR (capacitor permanente + partida)", tipoConexao = "Rotalock",
            faixaEvaporacao = "-35 a 0 °C", observacoes = "Comercial."
        ),
        Compressor(
            marca = "Tecumseh", modelo = "TFH4540F", potenciaHp = "1/2",
            tensao = "220 V", fase = "Monofásico", frequencia = "60 Hz", correnteNominal = "4,3 A",
            fluido = "R134a", aplicacao = "Média/Alta temperatura",
            capacidadeFrigorifica = "≈ 1.300 W @ +5°C", tipoOleo = "POE",
            tipoPartida = "CSR", tipoConexao = "Rotalock",
            faixaEvaporacao = "-15 a +10 °C", observacoes = "Balcões e câmaras de média."
        ),
        Compressor(
            marca = "Danfoss/Secop", modelo = "SC15G", potenciaHp = "1/2",
            tensao = "220 V", fase = "Monofásico", frequencia = "60 Hz", correnteNominal = "4,0 A",
            fluido = "R134a", aplicacao = "Média temperatura",
            capacidadeFrigorifica = "≈ 1.350 W @ +5°C", tipoOleo = "POE",
            tipoPartida = "CSIR", tipoConexao = "Solda",
            faixaEvaporacao = "-20 a +10 °C", observacoes = "Substituto comum de média temperatura."
        ),
        Compressor(
            marca = "Danfoss", modelo = "NTZ068", potenciaHp = "3/4",
            tensao = "220 V", fase = "Monofásico", frequencia = "60 Hz", correnteNominal = "6,0 A",
            fluido = "R404A/R507", aplicacao = "Baixa temperatura",
            capacidadeFrigorifica = "≈ 1.500 W @ -35°C", tipoOleo = "POE",
            tipoPartida = "CSR", tipoConexao = "Rotalock",
            faixaEvaporacao = "-45 a -5 °C", observacoes = "Baixa temperatura comercial."
        ),
        Compressor(
            marca = "Bitzer", modelo = "4FES-3", potenciaHp = "3",
            tensao = "380 V", fase = "Trifásico", frequencia = "60 Hz", correnteNominal = "6,8 A",
            fluido = "R404A/R134a", aplicacao = "Média/Baixa temperatura",
            capacidadeFrigorifica = "≈ 7.000 W @ -10°C", tipoOleo = "POE",
            tipoPartida = "Direta/Part-winding", tipoConexao = "Rotalock",
            faixaEvaporacao = "-40 a +7 °C", observacoes = "Semi-hermético industrial leve."
        ),
        Compressor(
            marca = "Copeland", modelo = "ZB21KQE", potenciaHp = "2",
            tensao = "380 V", fase = "Trifásico", frequencia = "60 Hz", correnteNominal = "5,2 A",
            fluido = "R404A/R134a", aplicacao = "Média temperatura",
            capacidadeFrigorifica = "≈ 6.300 W @ -5°C", tipoOleo = "POE",
            tipoPartida = "Direta (scroll)", tipoConexao = "Solda",
            faixaEvaporacao = "-20 a +7 °C", observacoes = "Scroll para média temperatura."
        ),
    )

    /** Aplicação simplificada (baixa/média/alta) extraída do texto. */
    private fun faixas(aplicacao: String): Set<String> =
        Regex("baixa|m[ée]dia|alta", RegexOption.IGNORE_CASE)
            .findAll(aplicacao.lowercase()).map { it.value }.toSet()

    /** Pesquisa por modelo ou marca. */
    fun pesquisar(termo: String): List<Compressor> {
        if (termo.isBlank()) return LISTA
        return LISTA.filter {
            it.modelo.contains(termo, true) || it.marca.contains(termo, true) ||
                it.fluido.contains(termo, true)
        }
    }

    data class Equivalente(val compressor: Compressor, val substituicaoDireta: Boolean, val diferencas: List<String>)

    /**
     * Sugere equivalentes ao [base], destacando diferenças críticas e se a
     * substituição é direta.
     */
    fun equivalentes(base: Compressor): List<Equivalente> =
        LISTA.filter { it.modelo != base.modelo }
            .map { candidato ->
                val dif = mutableListOf<String>()
                if (!fluidoCompativel(base.fluido, candidato.fluido))
                    dif += "Fluido diferente (${base.fluido} → ${candidato.fluido})"
                if (faixas(base.aplicacao).intersect(faixas(candidato.aplicacao)).isEmpty())
                    dif += "Faixa de aplicação diferente (${base.aplicacao} → ${candidato.aplicacao})"
                if (base.fase != candidato.fase)
                    dif += "Fase elétrica diferente (${base.fase} → ${candidato.fase})"
                if (base.tensao != candidato.tensao)
                    dif += "Tensão diferente (${base.tensao} → ${candidato.tensao})"
                if (base.tipoOleo != candidato.tipoOleo)
                    dif += "Óleo diferente (${base.tipoOleo} → ${candidato.tipoOleo})"
                if (base.potenciaHp != candidato.potenciaHp)
                    dif += "Potência diferente (${base.potenciaHp} HP → ${candidato.potenciaHp} HP)"
                Equivalente(candidato, dif.isEmpty(), dif)
            }
            // Ordena dos mais compatíveis para os menos
            .sortedBy { it.diferencas.size }

    private fun fluidoCompativel(a: String, b: String): Boolean {
        val fa = a.split("/").map { it.trim().uppercase() }.toSet()
        val fb = b.split("/").map { it.trim().uppercase() }.toSet()
        return fa.intersect(fb).isNotEmpty()
    }
}
