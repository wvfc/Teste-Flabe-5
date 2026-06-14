package br.com.refrigeracaopro.data

import org.json.JSONObject

/**
 * Definição do relatório de inspeção/partida de COMPRESSOR DE AR COMPRIMIDO
 * (rotativo), baseada no modelo de "Lista de Verificação de Partida".
 *
 * Cada campo é de um tipo:
 *  - TEXTO: preenchimento manual (medições, datas, valores).
 *  - MARCACAO: marcador de 3 estados (Ok / Reparado na visita / Necessita reparo).
 *  - SIMNAO: marcador Sim / Não.
 *
 * Os valores são guardados como JSON (chave -> valor) em Relatorio.dadosExtra.
 */
object RelatorioArComprimido {

    enum class Tipo { TEXTO, MARCACAO, SIMNAO }

    data class Campo(val chave: String, val rotulo: String, val tipo: Tipo = Tipo.MARCACAO, val sufixo: String = "")
    data class Secao(val titulo: String, val campos: List<Campo>)

    val MARCADORES = listOf("Ok", "Reparado na visita", "Necessita reparo")
    val OPCOES_SIMNAO = listOf("Sim", "Não")

    private fun t(chave: String, rotulo: String, sufixo: String = "") = Campo(chave, rotulo, Tipo.TEXTO, sufixo)
    private fun m(chave: String, rotulo: String) = Campo(chave, rotulo, Tipo.MARCACAO)
    private fun sn(chave: String, rotulo: String) = Campo(chave, rotulo, Tipo.SIMNAO)

    val SECOES: List<Secao> = listOf(
        Secao("Identificação do compressor", listOf(
            t("tipo_compressor", "Tipo de compressor"),
            t("modelo", "Modelo"),
            t("potencia_hp", "Potência", "HP"),
            t("n_serie", "Número de série"),
            t("capacidade_cfm", "Capacidade", "CFM"),
            t("pressao_trabalho", "Pressão de trabalho"),
            t("data_inspecao", "Data da inspeção"),
            t("ordem_servico_ref", "Ordem de serviço"),
            t("realizado_por", "Realizado por (técnico)"),
            t("revisado_por", "Revisado por (cliente)"),
        )),
        Secao("Temperaturas e medições", listOf(
            t("i01_horas", "1. Total de horas em operação / energizado"),
            t("i02_temp_descarte_pacote", "2. Temp. de descarte do pacote (plena carga)", "°C"),
            t("i03_temp_descarte_airend", "3. Temp. de descarte do AIREND (plena carga)", "°C"),
            t("i04_temp_injecao_oleo", "4. Temp. de injeção de óleo (plena carga)", "°C"),
            t("i05_temp_ambiente", "5. Temperatura ambiente (tomada de ar)", "°C"),
            t("i06_temp_interna", "6. Temperatura interna do equipamento", "°C"),
            t("i07_temp_valv_termostatica", "7. Temp. válvula termostática (portas A/B/C)", "°C"),
            m("i08_aletas_resfriadores", "8. Inspecionar as aletas dos resfriadores"),
            m("i09_nivel_oleo", "9. Verificar nível de óleo"),
            m("i10_vazamento_oleo", "10. Inspecionar por vazamentos de óleo"),
            m("i11_filtro_oleo_dp", "11. Condição do filtro de óleo (∆P)"),
            t("i12_agua_admissao", "12. Água de resfriamento – admissão (pressão/temp.)"),
            t("i13_agua_descarga", "13. Água de resfriamento – descarga (pressão/temp.)"),
            t("i14_vacuo_admissao", "14. Vácuo na admissão", "PSIG"),
            m("i15_filtro_admissao_cond", "15. Condição do filtro de admissão (encontrado)"),
            t("i16_filtro_admissao_data", "16. Instalação do filtro de admissão (data fab.)"),
            t("i17_dp_separador", "17. ∆P no tanque separador (plena carga)"),
            t("i18_elemento_separador_data", "18. Instalação do elemento separador (data fab.)"),
            m("i19_vazamento_ar", "19. Inspecionar por vazamentos de ar"),
            m("i20_mangueiras", "20. Inspecionar mangueiras"),
        )),
        Secao("Inspeções mecânicas", listOf(
            m("i21_defletores", "21. Inspecionar defletores (espumas) de entrada"),
            m("i22_valv_pressao_minima", "22. Inspecionar válvula de pressão mínima"),
            m("i23_valv_pescador", "23. Inspecionar válvula do pescador (sentido de fluxo)"),
            m("i24_respiro_engrenagens", "24. Inspecionar respiro da caixa de engrenagens"),
            m("i25_valv_dreno_condensado", "25. Inspecionar válvulas de dreno do condensado"),
            m("i26_separador_coalescente", "26. Inspecionar separador Ar-Água / filtro coalescente"),
            m("i27_lubrificacao_motores", "27. Lubrificação dos motores (vide placas)"),
            t("i28_valv_seguranca", "28. Válvula de segurança (PSI / vazão)"),
            m("i29_switch_alta_temp", "29. Atuação do switch de alta temperatura (HAT)"),
            m("i30_reaperto_terminais", "30. Reaperto dos terminais elétricos (potência/comando)"),
        )),
        Secao("Inspeção elétrica", listOf(
            t("i31_tensao_plena_carga", "31. Tensão plena carga (A/B/C – D/E/F)", "V"),
            t("i32_tensao_motor", "32. Tensão do motor (máx. rpm/pressão) (A/B/C – D/E/F)", "V"),
            t("i33_corrente_motor", "33. Corrente do motor (máx. rpm/pressão) (U/V/W)", "A"),
            t("i34_corrente_ventilador", "34. Corrente do motor do ventilador (R/S/T)", "A"),
            t("i35_tensao_dc_bus", "35. Tensão DC BUS (máx. rpm/pressão)", "V"),
            t("i36_corrente_total_pacote", "36. Corrente total do pacote – plena carga (L1/L2/L3)", "A"),
            t("i37_tensao_mcb1", "37. Tensão em MCB 1", "V"),
            t("i38_tensao_t1_a", "38. Tensão em T1 (secundário)", "V"),
            t("i39_tensao_t1_b", "39. Tensão em T1 (secundário)", "V"),
            t("i40_tensao_t1_c", "40. Tensão em T1 (secundário)", "V"),
            t("i41_ligacao_trafo_t1", "41. Ligação na entrada do trafo T1"),
        )),
        Secao("Consumíveis", listOf(
            t("i44_tipo_lubrificante", "44. Tipo de lubrificante"),
            t("i45_abastecimento_lubrificante", "45. Abastecimento de lubrificante (data de fab.)"),
        )),
        Secao("Inspeções de diagnóstico", listOf(
            sn("i46_amostra_lubrificante", "46. Coleta de amostra do lubrificante"),
            sn("i47_amostra_condensado", "47. Coleta de amostra do condensado"),
        )),
        Secao("Vibração", listOf(
            t("i48_dbi", "48. Vibração – Dbi"),
            t("i49_dbm_dbc", "49. Vibração – Dbm/Dbc"),
        )),
        Secao("Infraestrutura", listOf(
            t("i50_tensao_nominal_rede", "50. Tensão nominal da rede", "V"),
            m("i51_duto_ar_quente", "51. Duto de ar quente (área, instalação)"),
            m("i52_ventilacao", "52. Ventilação (temp. ambiente)"),
            t("i53_transiente_cabo_terra", "53. Transiente cabo terra (máx. 0,25 A)", "A"),
            m("i54_poeira_ambiente", "54. Poeira no ambiente"),
            m("i55_instrumentacao_agua", "55. Instrumentação na água de resfriamento"),
            m("i56_secao_condutor", "56. Seção do condutor de alimentação"),
            m("i57_tomada_manutencao", "57. Tomada para manutenção (220 V)"),
            m("i58_tubulacao_purgador", "58. Tubulação do purgador"),
            m("i59_valvulas_isolamento", "59. Válvulas de isolamento e serviço (rede de ar)"),
            m("i60_espaco_equipamento", "60. Espaço ao redor do equipamento"),
            m("i61_iluminacao", "61. Iluminação suficiente / adequada"),
        )),
        Secao("Inspeção geral", listOf(
            sn("g_pintura", "Acabamento de pintura aceitável?"),
            sn("g_pecas_faltando", "Peças ou componentes faltando?"),
            sn("g_metal_danificado", "Metal/tampa danificada?"),
            sn("g_quimicos_poeira", "Área com produtos químicos / muita poeira?"),
            sn("g_unidade_externa", "Unidade externa?"),
            sn("g_modificacao_externa", "Caso externa, houve modificação externa?"),
            sn("g_manutencao_adicional", "Existe manutenção adicional necessária?"),
            sn("g_urgente", "Caso afirmativo, é urgente?"),
        )),
        Secao("Dados de placa – Motor principal", listOf(
            t("mp_hp", "HP"), t("mp_fs", "FS"), t("mp_ip", "IP"), t("mp_rpm", "RPM"),
            t("mp_v", "Tensão (V)"), t("mp_tipo", "Tipo"), t("mp_a", "Corrente (A)"),
            t("mp_frame", "Frame"), t("mp_ns", "N/S"), t("mp_protecao", "Ajuste da proteção"),
        )),
        Secao("Dados de placa – Motor do ventilador", listOf(
            t("mv_hp", "HP"), t("mv_fs", "FS"), t("mv_ip", "IP"), t("mv_rpm", "RPM"),
            t("mv_v", "Tensão (V)"), t("mv_tipo", "Tipo"), t("mv_a", "Corrente (A)"),
            t("mv_frame", "Frame"), t("mv_ns", "N/S"), t("mv_protecao", "Ajuste da proteção"),
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
