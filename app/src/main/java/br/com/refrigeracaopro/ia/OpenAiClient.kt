package br.com.refrigeracaopro.ia

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import br.com.refrigeracaopro.data.Prefs.chaveOpenAi
import br.com.refrigeracaopro.data.Prefs.iaAtiva
import br.com.refrigeracaopro.data.Prefs.modeloIa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP da API da OpenAI (chat completions) usando OkHttp.
 *
 * A chave é cadastrada pelo usuário em Configurações e lida das
 * EncryptedSharedPreferences — nunca fica no código-fonte.
 * A IA só funciona com internet disponível e chave cadastrada.
 */
object OpenAiClient {

    private const val URL = "https://api.openai.com/v1/chat/completions"

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    val MODELOS = listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini", "gpt-4.1")

    const val PROMPT_SISTEMA =
        "Você é um assistente técnico especializado em refrigeração e climatização, " +
            "auxiliando técnicos de campo no Brasil. Responda sempre em português do Brasil, " +
            "em linguagem técnica, clara, objetiva e profissional. Considere normas e práticas " +
            "comuns no Brasil. Quando os dados forem insuficientes, diga quais medições " +
            "adicionais o técnico deve realizar. Inclua alertas de segurança quando relevante."

    fun temInternet(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun disponivel(context: Context): Boolean =
        context.iaAtiva && context.chaveOpenAi.isNotBlank() && temInternet(context)

    sealed class Resultado {
        data class Sucesso(val texto: String) : Resultado()
        data class Erro(val mensagem: String) : Resultado()
    }

    /**
     * Envia uma conversa para a API. [mensagens] são pares (papel, conteúdo),
     * com papel "user" ou "assistant".
     *
     * @param instrucoesExtra texto adicional de sistema (ex.: base técnica local
     *   + formato de diagnóstico). Quando presente, a IA passa a fundamentar a
     *   resposta na base interna em vez de responder genericamente.
     * @param imagens caminhos de imagens locais anexadas à ÚLTIMA mensagem do
     *   usuário (visão). Requer modelo com suporte a imagens (gpt-4o, etc.).
     */
    suspend fun perguntar(
        context: Context,
        mensagens: List<Pair<String, String>>,
        instrucoesExtra: String? = null,
        imagens: List<String> = emptyList(),
    ): Resultado =
        withContext(Dispatchers.IO) {
            val chave = context.chaveOpenAi
            if (chave.isBlank()) return@withContext Resultado.Erro(
                "Chave da OpenAI não cadastrada. Configure em Configurações."
            )
            if (!context.iaAtiva) return@withContext Resultado.Erro("Assistente IA desativado nas configurações.")
            if (!temInternet(context)) return@withContext Resultado.Erro("Sem conexão com a internet.")

            // Índice da última mensagem do usuário (onde anexamos as imagens)
            val idxUltimoUser = mensagens.indexOfLast { it.first == "user" }

            val corpo = JSONObject().apply {
                put("model", context.modeloIa)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", PROMPT_SISTEMA))
                    put(JSONObject().put("role", "system").put("content",
                        "Responda em TEXTO PURO, sem Markdown. Não use asteriscos (*) nem ** para " +
                            "negrito/itálico, nem # para títulos. Use listas com '-' ou '•' quando necessário."))
                    if (!instrucoesExtra.isNullOrBlank()) {
                        put(JSONObject().put("role", "system").put("content", instrucoesExtra))
                    }
                    mensagens.forEachIndexed { i, (papel, conteudo) ->
                        if (i == idxUltimoUser && imagens.isNotEmpty()) {
                            // Conteúdo multimodal: texto + imagens em base64
                            val arr = JSONArray()
                            arr.put(JSONObject().put("type", "text").put("text", conteudo))
                            imagens.forEach { caminho ->
                                imagemBase64(caminho)?.let { dataUrl ->
                                    arr.put(JSONObject().put("type", "image_url").put(
                                        "image_url", JSONObject().put("url", dataUrl)
                                    ))
                                }
                            }
                            put(JSONObject().put("role", papel).put("content", arr))
                        } else {
                            put(JSONObject().put("role", papel).put("content", conteudo))
                        }
                    }
                })
                put("temperature", 0.3)
            }

            val requisicao = Request.Builder()
                .url(URL)
                .header("Authorization", "Bearer $chave")
                .post(corpo.toString().toRequestBody("application/json".toMediaType()))
                .build()

            try {
                http.newCall(requisicao).execute().use { resposta ->
                    val texto = resposta.body?.string().orEmpty()
                    if (!resposta.isSuccessful) {
                        val msg = runCatching {
                            JSONObject(texto).getJSONObject("error").getString("message")
                        }.getOrDefault("HTTP ${resposta.code}")
                        return@withContext Resultado.Erro("Erro da API: $msg")
                    }
                    val conteudo = JSONObject(texto)
                        .getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getString("content")
                    Resultado.Sucesso(limparMarkdown(conteudo))
                }
            } catch (e: Exception) {
                Resultado.Erro("Falha de conexão: ${e.message}")
            }
        }

    /** Lê uma imagem do disco e devolve uma data URL base64 para a API de visão. */
    private fun imagemBase64(caminho: String): String? = runCatching {
        val bytes = File(caminho).readBytes()
        "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }.getOrNull()

    /**
     * Remove marcações Markdown das respostas (o usuário não quer ver "**" no
     * texto). Mantém o conteúdo legível em texto puro.
     */
    fun limparMarkdown(texto: String): String {
        var t = texto.trim()
        t = t.replace("**", "")
        t = t.replace("__", "")
        t = Regex("(?m)^\\s{0,3}#{1,6}\\s*").replace(t, "")      // títulos #, ##, ...
        t = Regex("(?m)^(\\s*)[*+]\\s+").replace(t) { m -> m.groupValues[1] + "• " } // bullets *,+
        t = Regex("(?m)^(\\s*)-\\s+").replace(t) { m -> m.groupValues[1] + "• " }     // bullets -
        t = Regex("(?<![*\\w])\\*(?!\\s)([^*\\n]+?)\\*(?![*\\w])").replace(t) { it.groupValues[1] } // *itálico*
        t = t.replace("`", "")
        return t.trim()
    }

    /** Teste rápido de conexão/chave. */
    suspend fun testarConexao(context: Context): Resultado =
        perguntar(context, listOf("user" to "Responda apenas: OK"))
}
