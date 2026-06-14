package br.com.refrigeracaopro.util

import br.com.refrigeracaopro.data.Cliente

/** Exportação/importação de clientes em CSV (compatível com Excel/Sheets). */
object CsvClientes {

    private val CABECALHO = listOf(
        "nome", "cpfCnpj", "telefone", "whatsapp", "email",
        "endereco", "cidade", "estado", "observacoes"
    )

    fun exportar(clientes: List<Cliente>): String = buildString {
        appendLine(CABECALHO.joinToString(",") { aspas(it) })
        clientes.forEach { c ->
            appendLine(
                listOf(
                    c.nome, c.cpfCnpj, c.telefone, c.whatsapp, c.email,
                    c.endereco, c.cidade, c.estado, c.observacoes
                ).joinToString(",") { aspas(it) }
            )
        }
    }

    /** Lê o CSV e devolve a lista de clientes (id = 0, para inserção). */
    fun importar(conteudo: String): List<Cliente> {
        val linhas = dividirLinhas(conteudo)
        if (linhas.isEmpty()) return emptyList()
        // Detecta se a primeira linha é cabeçalho
        val inicio = if (linhas.first().firstOrNull()?.equcontains("nome") == true) 1 else 0
        return linhas.drop(inicio).mapNotNull { campos ->
            if (campos.all { it.isBlank() }) return@mapNotNull null
            fun g(i: Int) = campos.getOrElse(i) { "" }.trim()
            val nome = g(0)
            if (nome.isBlank()) return@mapNotNull null
            Cliente(
                nome = nome, cpfCnpj = g(1), telefone = g(2), whatsapp = g(3), email = g(4),
                endereco = g(5), cidade = g(6), estado = g(7), observacoes = g(8)
            )
        }
    }

    private fun String.equcontains(s: String) = this.trim().lowercase().contains(s)

    private fun aspas(valor: String): String {
        val v = valor.replace("\"", "\"\"")
        return if (v.contains(',') || v.contains('"') || v.contains('\n')) "\"$v\"" else v
    }

    /** Parser de CSV simples com suporte a aspas e vírgulas dentro de campos. */
    private fun dividirLinhas(conteudo: String): List<List<String>> {
        val resultado = mutableListOf<List<String>>()
        val campos = mutableListOf<String>()
        val atual = StringBuilder()
        var entreAspas = false
        var i = 0
        val texto = conteudo.replace("\r\n", "\n").replace("\r", "\n")
        fun fecharCampo() { campos.add(atual.toString()); atual.clear() }
        fun fecharLinha() { fecharCampo(); resultado.add(campos.toList()); campos.clear() }
        while (i < texto.length) {
            val ch = texto[i]
            when {
                entreAspas -> {
                    if (ch == '"') {
                        if (i + 1 < texto.length && texto[i + 1] == '"') { atual.append('"'); i++ }
                        else entreAspas = false
                    } else atual.append(ch)
                }
                ch == '"' -> entreAspas = true
                ch == ',' -> fecharCampo()
                ch == '\n' -> fecharLinha()
                else -> atual.append(ch)
            }
            i++
        }
        if (atual.isNotEmpty() || campos.isNotEmpty()) fecharLinha()
        return resultado
    }
}
