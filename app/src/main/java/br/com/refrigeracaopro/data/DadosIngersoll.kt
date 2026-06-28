package br.com.refrigeracaopro.data

import android.content.Context
import org.json.JSONObject
import java.text.Normalizer

/**
 * Base de consumíveis Ingersoll Rand: por máquina, os itens de manutenção
 * (filtros, separador, óleo, kits…) e em quais intervalos de horas devem ser
 * trocados. Sem qualquer informação de valores/preços. Offline (assets).
 */
object DadosIngersoll {

    private const val ASSET = "base_tecnica/ingersoll.json"

    data class Consumivel(
        val consumavel: String,
        val ccn: String,
        val descricao: String,
        val qtd: String,
        val intervalos: List<String>,
    )

    data class Maquina(
        val nome: String,
        val codigo: String,
        val frame: String,
        val frameTipo: String,
        val consumiveis: List<Consumivel>,
    )

    @Volatile private var cache: List<Maquina>? = null

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
            val cons = o.optJSONArray("consumiveis")?.let { ca ->
                (0 until ca.length()).map { j ->
                    val c = ca.getJSONObject(j)
                    val ints = c.optJSONArray("intervalos")?.let { ia ->
                        (0 until ia.length()).map { ia.optString(it) }
                    } ?: emptyList()
                    Consumivel(
                        consumavel = c.optString("consumavel"),
                        ccn = c.optString("ccn"),
                        descricao = c.optString("descricao"),
                        qtd = c.optString("qtd"),
                        intervalos = ints,
                    )
                }
            } ?: emptyList()
            Maquina(
                nome = o.optString("nome"),
                codigo = o.optString("codigo"),
                frame = o.optString("frame"),
                frameTipo = o.optString("frameTipo"),
                consumiveis = cons,
            )
        }
    }

    private fun normalizar(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()

    fun buscar(lista: List<Maquina>, consulta: String): List<Maquina> {
        val termos = normalizar(consulta).split(" ").filter { it.isNotBlank() }
        if (termos.isEmpty()) return lista
        return lista.filter { m ->
            val alvo = normalizar("${m.nome} ${m.codigo} ${m.frame} ${m.frameTipo}")
            termos.all { alvo.contains(it) }
        }
    }
}
