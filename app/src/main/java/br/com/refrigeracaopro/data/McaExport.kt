package br.com.refrigeracaopro.data

/**
 * Exportação de um ensaio MCA em CSV e JSON, para consolidação fora do app.
 *
 * Kotlin puro (sem Android e sem biblioteca de JSON), para poder ser testado
 * junto com o motor de cálculo.
 */
object McaExport {

    private fun texto(valor: Double?): String = valor?.let { "%.4f".format(it) } ?: ""

    private fun escaparCsv(valor: String): String {
        val v = valor.replace("\"", "\"\"")
        return if (v.contains(',') || v.contains('"') || v.contains('\n')) "\"$v\"" else v
    }

    private fun escaparJson(valor: String): String = buildString {
        valor.forEach { c ->
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
            }
        }
    }

    private fun campoJson(nome: String, valor: String?): String =
        "\"${escaparJson(nome)}\": " + if (valor == null) "null" else "\"${escaparJson(valor)}\""

    private fun campoJson(nome: String, valor: Double?): String =
        "\"${escaparJson(nome)}\": " + (valor?.let { "%.6f".format(it) } ?: "null")

    /** Linhas de leitura bruta, no formato usado por CSV e JSON. */
    private fun leiturasEmLinhas(leituras: Mca.Leituras): List<Triple<String, String, String>> {
        val linhas = mutableListOf<Triple<String, String, String>>()
        Mca.Par.entries.forEach { par ->
            val r = leituras.resistencias[par]
            r?.repeticoes?.forEachIndexed { i, valor ->
                linhas += Triple("Resistência 1 kHz", "${par.rotulo} R${i + 1} (Ω)", texto(valor))
            }
            linhas += Triple("Resistência 1 kHz", "${par.rotulo} θ (°)", texto(r?.theta))
        }
        Mca.Par.entries.forEach { par ->
            linhas += Triple("Indutância/Impedância", "${par.rotulo} L 100 Hz (mH)", texto(leituras.lz100Hz[par]?.l))
            linhas += Triple("Indutância/Impedância", "${par.rotulo} Z 100 Hz (Ω)", texto(leituras.lz100Hz[par]?.z))
            linhas += Triple("Indutância/Impedância", "${par.rotulo} L 1 kHz (mH)", texto(leituras.lz1kHz[par]?.l))
            linhas += Triple("Indutância/Impedância", "${par.rotulo} Z 1 kHz (Ω)", texto(leituras.lz1kHz[par]?.z))
        }
        Mca.Par.entries.forEach { par ->
            linhas += Triple("Alta frequência 10 kHz", "${par.rotulo} Z (Ω)", texto(leituras.altaFrequencia[par]?.z))
            linhas += Triple("Alta frequência 10 kHz", "${par.rotulo} θ (°)", texto(leituras.altaFrequencia[par]?.theta))
        }
        Mca.Fase.entries.forEach { fase ->
            linhas += Triple("Capacitância p/ terra", "Fase ${fase.rotulo} (nF)", texto(leituras.capacitanciasNf[fase]))
        }
        Mca.Par.entries.forEach { par ->
            leituras.ric[par]?.forEachIndexed { i, valor ->
                linhas += Triple("RIC", "${par.rotulo} ${i * Mca.RIC_PASSO_GRAUS}° L (mH)", texto(valor))
            }
        }
        linhas += Triple("Isolação", "Isolação (MΩ)", texto(leituras.isolacaoMOhm))
        linhas += Triple("Isolação", "Tensão de ensaio (V)", texto(leituras.tensaoEnsaioV))
        linhas += Triple("Isolação", "Leitura 1 min (MΩ)", texto(leituras.leitura1min))
        linhas += Triple("Isolação", "Leitura 10 min (MΩ)", texto(leituras.leitura10min))
        return linhas
    }

    /** Índices calculados, no formato nome → valor. */
    fun indicesEmLinhas(indices: Mca.Indices): List<Pair<String, Double?>> = listOf(
        "Desbalanceamento R40 (%)" to indices.desbalR40,
        "Desbalanceamento L 100 Hz (%)" to indices.desbalL100,
        "Desbalanceamento L 1 kHz (%)" to indices.desbalL1k,
        "Desbalanceamento Z 100 Hz (%)" to indices.desbalZ100,
        "Desbalanceamento Z 1 kHz (%)" to indices.desbalZ1k,
        "Desbalanceamento Z 10 kHz (%)" to indices.desbalZ10k,
        "Delta θ 1 kHz (°)" to indices.deltaTheta1k,
        "Delta θ 10 kHz (°)" to indices.deltaTheta10k,
        "Spread I/F (p.p.)" to indices.spreadIf,
        "Desbalanceamento C p/ terra (%)" to indices.desbalCapacitancia,
        "Espalhamento amplitude RIC (%)" to indices.ric?.espalhamentoPercentual,
        "Desvio senoidal RIC (%)" to indices.ric?.desvioSenoidalPercentual,
        "Isolação (MΩ)" to indices.isolacaoMOhm,
        "Índice de polarização" to indices.pi,
    )

    fun paraCsv(
        cabecalho: List<Pair<String, String>>,
        leituras: Mca.Leituras,
        parecer: Mca.Parecer,
    ): String = buildString {
        appendLine("secao,item,valor")
        cabecalho.forEach { (nome, valor) ->
            appendLine(listOf("Identificação", nome, valor).joinToString(",") { escaparCsv(it) })
        }
        leiturasEmLinhas(leituras).forEach { (secao, item, valor) ->
            appendLine(listOf(secao, item, valor).joinToString(",") { escaparCsv(it) })
        }
        indicesEmLinhas(parecer.indices).forEach { (nome, valor) ->
            appendLine(listOf("Índices", nome, texto(valor)).joinToString(",") { escaparCsv(it) })
        }
        parecer.achados.forEach { achado ->
            appendLine(
                listOf("Achados", "${achado.severidade} — ${achado.titulo}", achado.evidencia)
                    .joinToString(",") { escaparCsv(it) }
            )
        }
        parecer.naoAvaliados.forEach { item ->
            appendLine(listOf("Não avaliados", item, "").joinToString(",") { escaparCsv(it) })
        }
    }

    fun paraJson(
        cabecalho: List<Pair<String, String>>,
        leituras: Mca.Leituras,
        parecer: Mca.Parecer,
    ): String = buildString {
        appendLine("{")
        appendLine("  \"identificacao\": {")
        appendLine(cabecalho.joinToString(",\n") { (n, v) -> "    " + campoJson(n, v) })
        appendLine("  },")
        appendLine("  \"leituras\": [")
        appendLine(
            leiturasEmLinhas(leituras).joinToString(",\n") { (secao, item, valor) ->
                "    { " + campoJson("secao", secao) + ", " + campoJson("item", item) +
                    ", " + campoJson("valor", valor.toDoubleOrNull()) + " }"
            }
        )
        appendLine("  ],")
        appendLine("  \"indices\": {")
        appendLine(indicesEmLinhas(parecer.indices).joinToString(",\n") { (n, v) -> "    " + campoJson(n, v) })
        appendLine("  },")
        appendLine("  \"achados\": [")
        appendLine(
            parecer.achados.joinToString(",\n") { a ->
                "    { " + campoJson("titulo", a.titulo) + ", " + campoJson("severidade", a.severidade.name) +
                    ", " + campoJson("evidencia", a.evidencia) + " }"
            }
        )
        appendLine("  ],")
        appendLine("  \"naoAvaliados\": [")
        appendLine(parecer.naoAvaliados.joinToString(",\n") { "    \"${escaparJson(it)}\"" })
        appendLine("  ]")
        append("}")
    }
}
