package br.com.refrigeracaopro.util

import android.content.Context
import br.com.refrigeracaopro.data.AppDatabase
import br.com.refrigeracaopro.data.Prefs.backupHash
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
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup e restauração COMPLETOS na pasta privada do app no Google Drive
 * (appDataFolder, escopo https://www.googleapis.com/auth/drive.appdata).
 *
 * O backup é um único .zip contendo o banco de dados + as pastas de fotos,
 * manuais e PDFs gerados, de modo que a restauração devolve tudo (cadastros,
 * imagens e anexos). Reaproveitado tanto pelo botão manual quanto pelo backup
 * automático (WorkManager).
 */
@Suppress("DEPRECATION")
object DriveBackup {

    const val ESCOPO_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
    private const val NOME_ZIP = "gestao_pro_backup.zip"
    private const val NOME_ANTIGO = "refrigeracao_pro_backup.db" // backups antigos (só banco)
    private val PASTAS = listOf("fotos", "manuais", "pdfs")

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

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

    private fun token(context: Context, conta: GoogleSignInAccount): String {
        val account = conta.account ?: error("Conta Google sem informações de acesso.")
        return GoogleAuthUtil.getToken(context, account, "oauth2:$ESCOPO_APPDATA")
    }

    /**
     * Envia o backup completo. Quando [forcar] é false (caso do backup
     * automático), pula o envio se nada mudou desde o último backup.
     */
    suspend fun enviar(context: Context, conta: GoogleSignInAccount, forcar: Boolean = false): Resultado =
        withContext(Dispatchers.IO) {
            try {
                AppDatabase.fechar() // descarrega o WAL antes de empacotar
                val assinatura = assinatura(context)
                if (!forcar && assinatura == context.backupHash) {
                    return@withContext Resultado.Sucesso("Backup já atualizado (nada mudou).")
                }

                val tkn = token(context, conta)
                val zip = construirZip(context)
                val bytes = zip.readBytes()
                zip.delete()

                val id = buscarId(tkn, NOME_ZIP)
                if (id == null) {
                    val novoId = criarMetadados(tkn, NOME_ZIP) ?: return@withContext Resultado.Erro("Falha ao criar arquivo no Drive.")
                    enviarMidia(tkn, novoId, bytes)
                } else {
                    enviarMidia(tkn, id, bytes)
                }
                context.backupHash = assinatura
                Resultado.Sucesso("Backup completo enviado (${bytes.size / 1024} KB).")
            } catch (e: Exception) {
                Resultado.Erro("Falha no backup: ${e.message}")
            }
        }

    /** Baixa o backup e restaura banco + arquivos. Compatível com backups antigos (só .db). */
    suspend fun restaurar(context: Context, conta: GoogleSignInAccount): Resultado =
        withContext(Dispatchers.IO) {
            try {
                val tkn = token(context, conta)
                val idZip = buscarId(tkn, NOME_ZIP)
                val idAntigo = if (idZip == null) buscarId(tkn, NOME_ANTIGO) else null
                val id = idZip ?: idAntigo ?: return@withContext Resultado.Erro("Nenhum backup encontrado no Drive.")
                val bytes = baixar(tkn, id) ?: return@withContext Resultado.Erro("Falha ao baixar o backup.")

                AppDatabase.fechar()
                val db = arquivoBanco(context)
                File(db.path + "-wal").delete()
                File(db.path + "-shm").delete()

                // Detecta zip (assinatura "PK") ou banco puro (backup antigo)
                if (bytes.size > 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) {
                    extrairZip(context, bytes)
                } else {
                    db.parentFile?.mkdirs()
                    db.outputStream().use { it.write(bytes) }
                }
                Resultado.Sucesso("Backup restaurado. Reinicie o app para aplicar.")
            } catch (e: Exception) {
                Resultado.Erro("Falha ao restaurar: ${e.message}")
            }
        }

    // ---------- Empacotamento ----------

    /** Assinatura (hash) do conteúdo atual, para detectar mudanças. */
    private fun assinatura(context: Context): String {
        val sb = StringBuilder()
        val db = arquivoBanco(context)
        if (db.exists()) sb.append("db:${db.length()}:${db.lastModified()};")
        PASTAS.forEach { sub ->
            File(context.filesDir, sub).listFiles()?.sortedBy { it.name }?.forEach { f ->
                if (f.isFile) sb.append("$sub/${f.name}:${f.length()}:${f.lastModified()};")
            }
        }
        val md = MessageDigest.getInstance("MD5").digest(sb.toString().toByteArray())
        return md.joinToString("") { "%02x".format(it) }
    }

    private fun construirZip(context: Context): File {
        val zip = File(context.cacheDir, "backup_temp.zip")
        ZipOutputStream(zip.outputStream().buffered()).use { zos ->
            val db = arquivoBanco(context)
            if (db.exists()) adicionar(zos, db, "databases/${db.name}")
            PASTAS.forEach { sub ->
                File(context.filesDir, sub).listFiles()?.forEach { f ->
                    if (f.isFile) adicionar(zos, f, "$sub/${f.name}")
                }
            }
        }
        return zip
    }

    private fun adicionar(zos: ZipOutputStream, arquivo: File, nome: String) {
        zos.putNextEntry(ZipEntry(nome))
        arquivo.inputStream().use { it.copyTo(zos) }
        zos.closeEntry()
    }

    private fun extrairZip(context: Context, bytes: ByteArray) {
        val dbDir = arquivoBanco(context).parentFile
        ZipInputStream(bytes.inputStream()).use { zis ->
            var entrada: ZipEntry? = zis.nextEntry
            while (entrada != null) {
                val nome = entrada.name
                val destino = if (nome.startsWith("databases/"))
                    File(dbDir, nome.removePrefix("databases/"))
                else File(context.filesDir, nome)
                if (!entrada.isDirectory) {
                    destino.parentFile?.mkdirs()
                    destino.outputStream().use { zis.copyTo(it) }
                }
                zis.closeEntry()
                entrada = zis.nextEntry
            }
        }
    }

    // ---------- Drive REST API ----------

    private fun buscarId(token: String, nome: String): String? {
        val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder" +
            "&fields=files(id,name,modifiedTime)&orderBy=modifiedTime desc"
        val req = Request.Builder().url(url).header("Authorization", "Bearer $token").get().build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val arr = JSONObject(resp.body?.string().orEmpty()).optJSONArray("files") ?: return null
            for (i in 0 until arr.length()) {
                val f = arr.getJSONObject(i)
                if (f.optString("name") == nome) return f.optString("id")
            }
        }
        return null
    }

    private fun criarMetadados(token: String, nome: String): String? {
        val corpo = JSONObject()
            .put("name", nome)
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
            .patch(bytes.toRequestBody("application/zip".toMediaType()))
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
