package br.com.refrigeracaopro.data

import android.content.Context
import org.json.JSONObject
import java.text.Normalizer

/**
 * Base "Dados de Compressores": informações internas de cada máquina
 * (peças, kits, itens diversos) extraídas do comparativo, com os links
 * (manuais e listas de peças em PDF) relacionados a cada uma.
 *
 * Lida da base embutida em assets (offline).
 */
object DadosCompressores {

    private const val ASSET = "base_tecnica/dados_compressores.json"

    data class Link(val texto: String, val url: String)
    data class Secao(val titulo: String, val colunas: List<String>, val itens: List<List<String>>)
    data class Maquina(
        val nome: String,
        val titulo: String,
        val subtitulo: String,
        val bqd: String,
        val links: List<Link>,
        val secoes: List<Secao>,
    )

    @Volatile
    private var cache: List<Maquina>? = null

    fun carregar(context: Context): List<Maquina> {
        cache?.let { return it }
        val lista = runCatching {
            val texto = context.assets.open(ASSET).bufferedReader().use { it.readText() }
            parse(texto)
        }.getOrDefault(emptyList())
        cache = lista
        return lista
    }

    private fun parse(texto: String): List<Maquina> {
        val obj = JSONObject(texto)
        val arr = obj.optJSONArray("maquinas") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val links = o.optJSONArray("links")?.let { la ->
                (0 until la.length()).map { j ->
                    val lo = la.getJSONObject(j)
                    Link(lo.optString("texto"), lo.optString("url"))
                }
            } ?: emptyList()
            val secoes = o.optJSONArray("secoes")?.let { sa ->
                (0 until sa.length()).map { j ->
                    val so = sa.getJSONObject(j)
                    val cols = so.optJSONArray("colunas")?.let { ca ->
                        (0 until ca.length()).map { ca.optString(it) }
                    } ?: emptyList()
                    val itens = so.optJSONArray("itens")?.let { ia ->
                        (0 until ia.length()).map { k ->
                            val linha = ia.optJSONArray(k)
                            if (linha == null) emptyList() else (0 until linha.length()).map { linha.optString(it) }
                        }
                    } ?: emptyList()
                    Secao(so.optString("titulo"), cols, itens)
                }
            } ?: emptyList()
            Maquina(
                nome = o.optString("nome"),
                titulo = o.optString("titulo"),
                subtitulo = o.optString("subtitulo"),
                bqd = o.optString("bqd"),
                links = links,
                secoes = secoes,
            )
        }
    }

    private fun normalizar(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()

    /** Busca por nome/modelo/BQD; todos os termos da consulta precisam aparecer. */
    fun buscar(lista: List<Maquina>, consulta: String): List<Maquina> {
        val termos = normalizar(consulta).split(" ").filter { it.isNotBlank() }
        if (termos.isEmpty()) return lista
        return lista.filter { m ->
            val alvo = normalizar("${m.nome} ${m.titulo} ${m.subtitulo} ${m.bqd}")
            termos.all { alvo.contains(it) }
        }
    }
}
