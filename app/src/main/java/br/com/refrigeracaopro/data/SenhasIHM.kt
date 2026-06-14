package br.com.refrigeracaopro.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Base de senhas/acessos de IHMs, CLPs e inversores. Lê de uma base local
 * (assets) e pode sincronizar com o brands.json do repositório do usuário
 * quando disponível (cache offline).
 */
object SenhasIHM {

    // Versão "raw" do brands.json informado pelo usuário
    const val URL_BRANDS =
        "https://raw.githubusercontent.com/wvfc/Ajuda-Tecnico-Web/main/" +
            "Ajuda%20Tecnico%20Web/facilita-tecnico/backend/app/data/brands.json"

    private const val ASSET = "base_tecnica/senhas_ihm.json"
    private const val CACHE = "senhas_ihm_cache.json"

    data class Item(
        val titulo: String,
        val usuario: String,
        val senha: String,
        val senhaRef: String,
        val observacao: String,
    )
    data class Marca(val slug: String, val nome: String, val descricao: String, val itens: List<Item>)

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Carrega do cache (se houver sincronização) ou da base embutida. */
    fun carregar(context: Context): List<Marca> {
        val cache = File(context.filesDir, CACHE)
        val json = if (cache.exists()) cache.readText() else
            runCatching { context.assets.open(ASSET).bufferedReader().use { it.readText() } }.getOrDefault("")
        return parse(json)
    }

    sealed class Resultado {
        data class Sucesso(val marcas: List<Marca>) : Resultado()
        data class Erro(val mensagem: String) : Resultado()
    }

    /** Baixa o brands.json do repositório e atualiza o cache local. */
    suspend fun atualizarDaWeb(context: Context): Resultado = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(URL_BRANDS).header("User-Agent", "GestaoPro").get().build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Resultado.Erro(
                    "Não foi possível baixar (HTTP ${resp.code}). Verifique se o arquivo está público."
                )
                val texto = resp.body?.string().orEmpty()
                val marcas = parse(texto)
                if (marcas.isEmpty()) return@withContext Resultado.Erro("Arquivo baixado, mas sem dados reconhecíveis.")
                File(context.filesDir, CACHE).writeText(texto)
                Resultado.Sucesso(marcas)
            }
        } catch (e: Exception) {
            Resultado.Erro("Falha de conexão: ${e.message}")
        }
    }

    /**
     * Parser flexível: aceita {"marcas":[{marca, itens|equipamentos|modelos:[...]}]},
     * um objeto { "Marca": [ ... ] } ou um array de marcas.
     */
    fun parse(texto: String): List<Marca> {
        if (texto.isBlank()) return emptyList()
        return runCatching {
            val trimmed = texto.trim()
            when {
                trimmed.startsWith("{") -> {
                    val obj = JSONObject(trimmed)
                    when {
                        obj.has("marcas") -> parseArrayMarcas(obj.getJSONArray("marcas"))
                        obj.has("brands") -> parseArrayMarcas(obj.getJSONArray("brands"))
                        else -> parseObjetoMarcas(obj)
                    }
                }
                trimmed.startsWith("[") -> parseArrayMarcas(JSONArray(trimmed))
                else -> emptyList()
            }
        }.getOrDefault(emptyList())
    }

    private fun parseArrayMarcas(arr: JSONArray): List<Marca> {
        val lista = mutableListOf<Marca>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val nome = primeiro(o, "nome", "marca", "brand", "name")
            val slug = primeiro(o, "slug").ifBlank { nome }
            val descricao = primeiro(o, "descricao", "descrição", "desc", "description")
            val itensArr = o.optJSONArray("itens") ?: o.optJSONArray("equipamentos")
                ?: o.optJSONArray("modelos") ?: o.optJSONArray("models") ?: o.optJSONArray("items")
            lista.add(Marca(slug, nome, descricao, parseItens(itensArr)))
        }
        return lista
    }

    private fun parseObjetoMarcas(obj: JSONObject): List<Marca> {
        val lista = mutableListOf<Marca>()
        obj.keys().forEach { chave ->
            val arr = obj.optJSONArray(chave)
            if (arr != null) lista.add(Marca(chave, chave, "", parseItens(arr)))
        }
        return lista
    }

    private fun parseItens(arr: JSONArray?): List<Item> {
        if (arr == null) return emptyList()
        val itens = mutableListOf<Item>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            itens.add(
                Item(
                    titulo = primeiro(o, "titulo", "título", "modelo", "model", "nome", "name"),
                    usuario = primeiro(o, "usuario", "usuário", "user"),
                    senha = primeiro(o, "senha", "password", "code", "codigo"),
                    senhaRef = primeiro(o, "senha_ref", "senharef", "ref"),
                    observacao = primeiro(o, "observacao", "observação", "obs", "nota", "note", "observacoes"),
                )
            )
        }
        return itens
    }

    private fun primeiro(o: JSONObject, vararg chaves: String): String {
        chaves.forEach { k -> if (o.has(k) && !o.isNull(k)) return o.optString(k) }
        return ""
    }
}
