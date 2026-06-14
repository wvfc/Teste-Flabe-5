package br.com.refrigeracaopro.data

import org.json.JSONObject

/**
 * Definição do relatório de inspeção de COMPRESSOR DE AR COMPRIMIDO, baseada no
 * modelo de checklist do fabricante (campos de inspeção, elétricos, pressões,
 * temperaturas e horímetro). Os valores são guardados como JSON em
 * Relatorio.dadosExtra (chave -> valor).
 */
object RelatorioArComprimido {

    data class Campo(val chave: String, val rotulo: String, val sufixo: String = "")
    data class Secao(val titulo: String, val campos: List<Campo>)

    val SECOES: List<Secao> = listOf(
        Secao("Horímetro e manutenção", listOf(
            Campo("horas_total", "Total de horas de funcionamento / em carga"),
            Campo("prox_filtro_admissao", "Próxima substituição do filtro de admissão", "hrs"),
            Campo("prox_filtro_oleo", "Próxima substituição do filtro de óleo", "hrs"),
            Campo("prox_elemento_separador", "Próxima substituição do elemento separador", "hrs"),
            Campo("prox_oleo", "Próxima substituição do óleo", "hrs"),
            Campo("prox_lubrificacao_motor", "Próxima lubrificação do motor principal", "hrs"),
            Campo("ultima_troca_rolamento_vent", "Última troca do rolamento do ventilador", "hrs"),
        )),
        Secao("Pressões e temperaturas", listOf(
            Campo("pressao_descarga_pacote", "Pressão de descarga do pacote (carga/alívio)", "bar"),
            Campo("temp_descarga_pacote", "Temp. de descarga do pacote a plena carga", "°C"),
            Campo("temp_descarga_unidade", "Temp. de descarga da unidade compressora a plena carga", "°C"),
            Campo("temp_injecao_oleo", "Temp. de injeção do óleo a plena carga", "°C"),
            Campo("pressao_carter_alivio", "Pressão do cárter em alívio", "bar"),
            Campo("vacuo_admissao_alivio", "Vácuo na admissão em alívio", "bar"),
            Campo("queda_pressao_separador", "Queda de pressão do separador a plena carga", "bar"),
            Campo("temp_ambiente", "Temperatura ambiente da instalação", "°C"),
            Campo("temp_interna", "Temperatura interna do equipamento", "°C"),
            Campo("temp_valvula_termostatica", "Temp. da válvula de controle termostático (A/B/C)", "°C"),
        )),
        Secao("Óleo, ar e filtros", listOf(
            Campo("tipo_oleo", "Tipo do óleo lubrificante"),
            Campo("inspecao_nivel_oleo", "Inspecionar nível de óleo"),
            Campo("inspecao_vazamento_oleo", "Inspecionar vazamento de óleo"),
            Campo("inspecao_vazamento_ar", "Inspecionar por vazamentos de ar"),
            Campo("condicao_filtro_admissao", "Condição do filtro de admissão"),
        )),
        Secao("Inspeção mecânica", listOf(
            Campo("inspecao_mangueiras", "Inspecionar mangueiras"),
            Campo("inspecao_valvula_pressao_minima", "Inspecionar válvula de pressão mínima"),
            Campo("inspecao_orificio_pescador", "Inspecionar orifício e tela do pescador"),
            Campo("inspecao_dreno_condensado", "Inspecionar e limpar o dreno de condensado"),
            Campo("tipo_transmissao", "Tipo de transmissão"),
            Campo("inspecao_trocadores_calor", "Inspecionar os núcleos dos trocadores de calor"),
            Campo("valvula_seguranca", "Válvula de segurança instalada e operacional"),
        )),
        Secao("Medições elétricas", listOf(
            Campo("tensao_carga", "Tensão em carga (A/B/C/D/E/F)", "V"),
            Campo("tensao_alivio", "Tensão em alívio (A/B/C/D/E/F)", "V"),
            Campo("corrente_motor_plena", "Corrente do motor – plena carga (T1/T2/T3)", "A"),
            Campo("corrente_motor_sem", "Corrente do motor – sem carga (T1/T2/T3)", "A"),
            Campo("queda_tensao_partida", "Queda de tensão na chave de partida (L1/L2/L3)", "V"),
            Campo("corrente_total_pacote", "Corrente total do pacote – plena carga (L1/L2/L3)", "A"),
            Campo("inspecao_contatores", "Inspecionar contatores e conexões elétricas"),
            Campo("verificar_corte_alta_temp", "Verificar corte por alta temperatura"),
            Campo("corrente_aterramento", "Corrente do cabo de aterramento", "A"),
            Campo("corrente_neutro", "Corrente do cabo neutro", "A"),
        )),
        Secao("Motor hermético e secador", listOf(
            Campo("temp_ponto_orvalho", "Temperatura do ponto de orvalho", "°C"),
            Campo("temp_descarga_hermetico", "Temperatura de descarga do motor hermético", "°C"),
            Campo("temp_succao_hermetico", "Temperatura de sucção do motor hermético", "°C"),
            Campo("tensao_alim_hermetico", "Tensão de alimentação do motor hermético (L1-L2)", "V"),
            Campo("temp_entrada_ar_secador", "Temperatura de entrada do ar no secador", "°C"),
        )),
    )

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
}
