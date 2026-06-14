package br.com.refrigeracaopro.data

import org.json.JSONObject

/**
 * Definição do relatório de inspeção de COMPRESSOR DE AR COMPRIMIDO, no padrão
 * das telas do app de referência: cada item tem campo(s) de preenchimento,
 * seleção e/ou o marcador de 4 estados (Ok / Reparado / À Reparar / N/A),
 * organizados por tipo de inspeção (abas).
 *
 * Armazenamento (JSON em Relatorio.dadosExtra):
 *  - valor de campo simples: chave
 *  - valor de subcampo (fases): "chave_SUB"
 *  - marcador: "chave__m"
 *  - observação de foto: "foto::caminho"
 *  - contato do cliente: "contato_tipo" / "contato_valor"
 */
object RelatorioArComprimido {

    val MARCADORES = listOf("Ok", "Reparado", "À Reparar", "N/A")

    data class Campo(
        val chave: String,
        val rotulo: String,
        val subRotulos: List<String> = listOf(""), // [""] = valor único; senão fases
        val sufixo: String = "",
        val temValor: Boolean = true,
        val temMarcador: Boolean = true,
        val opcoes: List<String> = emptyList(), // quando preenchido, vira seleção
    )

    data class Secao(val titulo: String, val campos: List<Campo>)

    fun chaveValor(campo: Campo, sub: String): String =
        if (sub.isBlank()) campo.chave else "${campo.chave}_$sub"

    fun chaveMarcador(campo: Campo): String = "${campo.chave}__m"

    // Medição: valor + marcador
    private fun med(chave: String, rotulo: String, sufixo: String = "", subs: List<String> = listOf("")) =
        Campo(chave, rotulo, subs, sufixo, temValor = true, temMarcador = true)

    // Medição sem marcador (apenas valor)
    private fun valor(chave: String, rotulo: String, sufixo: String = "") =
        Campo(chave, rotulo, listOf(""), sufixo, temValor = true, temMarcador = false)

    // Seleção (lista de opções)
    private fun sel(chave: String, rotulo: String, opcoes: List<String>) =
        Campo(chave, rotulo, listOf(""), "", temValor = true, temMarcador = false, opcoes = opcoes)

    // Item de inspeção (somente marcador)
    private fun insp(chave: String, rotulo: String) =
        Campo(chave, rotulo, listOf(""), "", temValor = false, temMarcador = true)

    val SECOES: List<Secao> = listOf(
        Secao("Horímetros", listOf(
            med("horas_total", "Total de horas de funcionamento / em carga", "hrs", listOf("Funcionamento", "Carga")),
            med("prox_filtro_admissao", "Próxima substituição do filtro de admissão", "hrs"),
            med("prox_filtro_oleo", "Próxima substituição do filtro de óleo", "hrs"),
            med("prox_elemento_separador", "Próxima substituição do elemento separador", "hrs"),
            med("prox_oleo", "Próxima substituição do óleo", "hrs"),
            med("ultima_lubrificacao_motor", "Última lubrificação do motor", "hrs"),
            med("ultima_troca_rolamento_vent", "Última troca do rolamento do ventilador", "hrs"),
        )),
        Secao("Medições", listOf(
            med("pressao_descarga_pacote", "Pressão de descarga do pacote (carga/alívio)", "bar", listOf("Carga", "Alívio")),
            med("temp_descarga_pacote", "Temperatura de saída do ar", "°C"),
            med("temp_descarga_unidade", "Temp. de descarga da unidade compressora a plena carga", "°C"),
            med("temp_injecao_oleo", "Temp. de injeção do óleo a plena carga", "°C"),
            med("pressao_carter_alivio", "Pressão do cárter em alívio", "bar"),
            med("vacuo_admissao_alivio", "Vácuo na admissão em alívio", "bar"),
            med("queda_pressao_separador", "Queda de pressão do separador a plena carga", "bar"),
            med("temp_ambiente", "Temperatura ambiente da instalação", "°C"),
            med("temp_interna", "Temperatura interna do equipamento", "°C"),
            med("temp_valv_termostatica", "Temp. da válvula de controle termostático", "°C",
                listOf("Entrada no radiador", "Saída do radiador", "Injeção na unidade")),
            med("temp_ponto_orvalho", "Temperatura do ponto de orvalho", "°C"),
        )),
        Secao("Inspeção mecânica", listOf(
            sel("tipo_oleo", "Tipo do óleo lubrificante", listOf("Mineral", "Semissintético", "Sintético")),
            insp("nivel_oleo", "Inspecionar nível de óleo"),
            insp("vazamento_oleo", "Inspecionar vazamento de óleo"),
            insp("vazamento_ar", "Inspecionar por vazamentos de ar"),
            insp("cond_filtro_admissao", "Condição do filtro de admissão"),
            insp("mangueiras", "Inspecionar mangueiras"),
            insp("valv_pressao_minima", "Inspecionar válvula de pressão mínima"),
            insp("orificio_pescador", "Inspecionar orifício e tela do pescador"),
            insp("dreno_condensado", "Inspecionar e limpar o dreno de condensado"),
            sel("tipo_transmissao", "Tipo de transmissão", listOf("Correia", "Engrenagem", "Acoplamento", "HPM")),
            insp("trocadores_calor", "Inspecionar os núcleos de trocadores de calor"),
            insp("valv_seguranca", "Válvula de segurança instalada e operacional"),
        )),
        Secao("Inspeção elétrica", listOf(
            // Tensões medidas entre fases (linha a linha)
            med("tensao_carga", "Tensão em carga (entre fases)", "V", listOf("U-V", "U-W", "V-W", "U-T", "V-T", "W-T")),
            med("tensao_alivio", "Tensão em alívio (entre fases)", "V", listOf("U-V", "U-W", "V-W", "U-T", "V-T", "W-T")),
            med("corrente_motor_plena", "Corrente do motor (plena carga)", "A", listOf("T1", "T2", "T3")),
            med("corrente_motor_sem", "Corrente do motor (sem carga)", "A", listOf("T1", "T2", "T3")),
            med("corrente_ventilador", "Corrente do motor do ventilador", "A", listOf("R", "S", "T")),
            med("queda_tensao_partida", "Queda de tensão na chave de partida", "V", listOf("L1", "L2", "L3")),
            med("corrente_total_pacote", "Corrente total do pacote (plena carga)", "A", listOf("L1", "L2", "L3")),
            insp("contatores", "Inspecionar os contatores e conexões elétricas"),
            insp("corte_alta_temp", "Verificar corte por alta temperatura"),
            med("corrente_aterramento", "Corrente do cabo de aterramento", "A"),
            med("corrente_neutro", "Corrente do cabo neutro", "A"),
        )),
        Secao("Vibração", listOf(
            valor("vibracao", "Vibração", "mm/s"),
        )),
    )

    /** Monta o texto do valor de um campo (juntando subcampos com suas fases). */
    fun valorTexto(campo: Campo, valores: Map<String, String>): String {
        if (campo.subRotulos.size == 1 && campo.subRotulos.first().isBlank()) {
            val v = valores[campo.chave].orEmpty()
            return if (v.isBlank()) "" else if (campo.sufixo.isBlank()) v else "$v ${campo.sufixo}"
        }
        val partes = campo.subRotulos.mapNotNull { sub ->
            val v = valores[chaveValor(campo, sub)].orEmpty()
            if (v.isBlank()) null else "$sub: $v"
        }
        if (partes.isEmpty()) return ""
        return partes.joinToString("  ") + if (campo.sufixo.isBlank()) "" else " ${campo.sufixo}"
    }

    fun parse(json: String): MutableMap<String, String> {
        val mapa = mutableMapOf<String, String>()
        if (json.isBlank()) return mapa
        runCatching {
            val obj = JSONObject(json)
            obj.keys().forEach { k -> mapa[k] = obj.optString(k) }
        }
        return mapa
    }

    fun toJson(valores: Map<String, String>): String {
        val obj = JSONObject()
        valores.forEach { (k, v) -> if (v.isNotBlank()) obj.put(k, v) }
        return obj.toString()
    }

    /** Resumo textual de todos os campos preenchidos (para a IA gerar o relatório). */
    fun resumoParaIA(valores: Map<String, String>): String = buildString {
        SECOES.forEach { secao ->
            val linhas = secao.campos.mapNotNull { campo ->
                val v = valorTexto(campo, valores)
                val m = valores[chaveMarcador(campo)].orEmpty()
                val texto = when {
                    v.isNotBlank() && m.isNotBlank() -> "$v [$m]"
                    m.isNotBlank() -> m
                    else -> v
                }
                if (texto.isBlank()) null else "- ${campo.rotulo}: $texto"
            }
            if (linhas.isNotEmpty()) {
                append("## ").append(secao.titulo).append("\n")
                append(linhas.joinToString("\n")).append("\n\n")
            }
        }
    }
}
