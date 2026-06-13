package br.com.refrigeracaopro.ia

import android.content.Context

/**
 * Base técnica interna consultável OFFLINE (arquivos JSON em assets/base_tecnica).
 *
 * Antes de chamar a IA, selecionamos os trechos relevantes desta base para
 * injetar no prompt, fazendo a IA responder com fundamento técnico em vez de
 * respostas genéricas.
 */
object BaseTecnica {

    private const val PASTA = "base_tecnica"

    private val ARQUIVOS = listOf(
        "gases_refrigerantes.json",
        "compressores_referencia.json",
        "diagnosticos_refrigeracao.json",
        "falhas_comuns.json",
        "procedimentos_tecnicos.json",
        "tabela_capacitores.json",
        "tabela_carga_fluido.json",
    )

    // Cache em memória do conteúdo dos arquivos
    private var cache: Map<String, String>? = null

    private fun carregar(context: Context): Map<String, String> {
        cache?.let { return it }
        val mapa = ARQUIVOS.associateWith { nome ->
            runCatching {
                context.assets.open("$PASTA/$nome").bufferedReader().use { it.readText() }
            }.getOrDefault("")
        }
        cache = mapa
        return mapa
    }

    /**
     * Monta um resumo da base relevante para a [consulta]. Sempre inclui as
     * regras de diagnóstico e falhas comuns (núcleo do raciocínio) e adiciona
     * trechos extras conforme as palavras-chave da consulta.
     */
    fun resumoPara(context: Context, consulta: String): String {
        val arquivos = carregar(context)
        val texto = consulta.lowercase()
        val partes = mutableListOf<String>()

        // Núcleo de diagnóstico sempre presente
        partes += rotulo("DIAGNÓSTICOS", arquivos["diagnosticos_refrigeracao.json"])
        partes += rotulo("FALHAS COMUNS", arquivos["falhas_comuns.json"])

        // Gases: se algum fluido for citado
        if (Regex("r\\d|gás|gas|fluido|amônia|amonia|co2").containsMatchIn(texto)) {
            partes += rotulo("GASES", arquivos["gases_refrigerantes.json"])
        }
        if (Regex("compressor|motor|substitui|equivalent").containsMatchIn(texto)) {
            partes += rotulo("COMPRESSORES", arquivos["compressores_referencia.json"])
        }
        if (Regex("vácuo|vacuo|carga|solda|recolh|procedimento|vazament").containsMatchIn(texto)) {
            partes += rotulo("PROCEDIMENTOS", arquivos["procedimentos_tecnicos.json"])
        }
        if (Regex("capacitor|partida|µf|uf|microfarad").containsMatchIn(texto)) {
            partes += rotulo("CAPACITORES", arquivos["tabela_capacitores.json"])
        }
        if (Regex("carga|fluido|linha|metros|gramas").containsMatchIn(texto)) {
            partes += rotulo("CARGA DE FLUIDO", arquivos["tabela_carga_fluido.json"])
        }

        // Limita o tamanho total para não estourar o contexto do modelo
        return partes.filter { it.isNotBlank() }.joinToString("\n\n").take(6000)
    }

    private fun rotulo(titulo: String, json: String?): String =
        if (json.isNullOrBlank()) "" else "### $titulo\n$json"

    /** Instrução de formato de resposta de diagnóstico estruturado. */
    const val FORMATO_DIAGNOSTICO =
        "Use a BASE TÉCNICA fornecida, os dados do técnico, o histórico e as medições. " +
            "EVITE respostas genéricas. Estruture SEMPRE a resposta nos tópicos:\n" +
            "1) Diagnóstico provável\n2) Possíveis causas\n3) Testes recomendados\n" +
            "4) Riscos\n5) Correção sugerida\n6) Observação de segurança\n" +
            "7) Grau de confiança (baixo, médio ou alto).\n" +
            "Se faltarem medições essenciais, indique quais coletar."
}
