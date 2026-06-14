package br.com.refrigeracaopro.util

import android.content.Context
import br.com.refrigeracaopro.data.AppDatabase
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Backup e restauração do banco de dados na pasta privada do app no Google
 * Drive (appDataFolder). Usa o escopo OAuth:
 *   https://www.googleapis.com/auth/drive.appdata
 *
 * A pasta appDataFolder é invisível ao usuário e exclusiva do app — ideal para
 * backup. Requer um cliente OAuth Android no Google Cloud configurado com o
 * nome do pacote (applicationId) e o SHA-1 da chave de assinatura do app.
 */
@Suppress("DEPRECATION")
object DriveBackup {

    const val ESCOPO_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
    private const val NOME_BACKUP = "refrigeracao_pro_backup.db"

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /** Opções de login do Google solicitando o escopo do appDataFolder. */
    fun opcoesLogin(): GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(ESCOPO_APPDATA))
            .build()

    sealed class Resultado {
        data class Sucesso(val mensagem: String) : Resultado()
        data class Erro(val mensagem: String) : Resultado()
    }

    private fun arquivoBanco(context: Context): File =
        context.getDatabasePath(AppDatabase.NOME_BANCO)

    /** Obtém um token OAuth para o escopo do appDataFolder (bloqueante; usar em IO). */
    private fun token(context: Context, conta: GoogleSignInAccount): String {
        val account = conta.account ?: error("Conta Google sem informações de acesso.")
        return GoogleAuthUtil.getToken(context, account, "oauth2:$ESCOPO_APPDATA")
    }

    /** Envia (ou atualiza) o backup do banco para o appDataFolder. */
    suspend fun enviar(context: Context, conta: GoogleSignInAccount): Resultado =
        withContext(Dispatchers.IO) {
            try {
                val tkn = token(context, conta)
                // Garante que o WAL seja descarregado antes de ler o arquivo
                AppDatabase.fechar()
                val bytes = arquivoBanco(context).readBytes()

                val idExistente = buscarId(tkn)
                if (idExistente == null) {
                    // Cria metadados na pasta appDataFolder e depois envia o conteúdo
                    val novoId = criarMetadados(tkn) ?: return@withContext Resultado.Erro("Falha ao criar arquivo no Drive.")
                    enviarMidia(tkn, novoId, bytes)
                } else {
                    enviarMidia(tkn, idExistente, bytes)
                }
                Resultado.Sucesso("Backup enviado ao Google Drive (${bytes.size / 1024} KB).")
            } catch (e: Exception) {
                Resultado.Erro("Falha no backup: ${e.message}")
            }
        }

    /** Baixa o backup do appDataFolder e substitui o banco local. */
    suspend fun restaurar(context: Context, conta: GoogleSignInAccount): Resultado =
        withContext(Dispatchers.IO) {
            try {
                val tkn = token(context, conta)
                val id = buscarId(tkn) ?: return@withContext Resultado.Erro("Nenhum backup encontrado no Drive.")
                val bytes = baixar(tkn, id) ?: return@withContext Resultado.Erro("Falha ao baixar o backup.")

                AppDatabase.fechar()
                val destino = arquivoBanco(context)
                File(destino.path + "-wal").delete()
                File(destino.path + "-shm").delete()
                destino.outputStream().use { it.write(bytes) }
                Resultado.Sucesso("Backup restaurado. Reinicie o app para aplicar.")
            } catch (e: Exception) {
                Resultado.Erro("Falha ao restaurar: ${e.message}")
            }
        }

    // ---------- Chamadas à Drive REST API ----------

    /** Procura o id do arquivo de backup no appDataFolder. */
    private fun buscarId(token: String): String? {
        val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder" +
            "&fields=files(id,name,modifiedTime)&orderBy=modifiedTime desc"
        val req = Request.Builder().url(url).header("Authorization", "Bearer $token").get().build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val arr = JSONObject(resp.body?.string().orEmpty()).optJSONArray("files") ?: return null
            for (i in 0 until arr.length()) {
                val f = arr.getJSONObject(i)
                if (f.optString("name") == NOME_BACKUP) return f.optString("id")
            }
        }
        return null
    }

    private fun criarMetadados(token: String): String? {
        val corpo = JSONObject()
            .put("name", NOME_BACKUP)
            .put("parents", org.json.JSONArray().put("appDataFolder"))
        val req = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?fields=id")
            .header("Authorization", "Bearer $token")
            .post(corpo.toString().toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            return JSONObject(resp.body?.string().orEmpty()).optString("id").ifBlank { null }
        }
    }

    private fun enviarMidia(token: String, id: String, bytes: ByteArray) {
        val req = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files/$id?uploadType=media")
            .header("Authorization", "Bearer $token")
            .patch(bytes.toRequestBody("application/octet-stream".toMediaType()))
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code} ao enviar conteúdo.")
        }
    }

    private fun baixar(token: String, id: String): ByteArray? {
        val req = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$id?alt=media")
            .header("Authorization", "Bearer $token").get().build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            return resp.body?.bytes()
        }
    }
}
