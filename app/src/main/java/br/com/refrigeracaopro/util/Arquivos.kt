package br.com.refrigeracaopro.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** Utilitários de arquivos: fotos, assinaturas e compartilhamento. */
object Arquivos {

    fun pastaFotos(context: Context): File =
        File(context.filesDir, "fotos").apply { mkdirs() }

    fun pastaPdfs(context: Context): File =
        File(context.filesDir, "pdfs").apply { mkdirs() }

    /** Copia uma imagem escolhida (galeria/câmera) para o armazenamento interno. */
    fun copiarImagem(context: Context, uri: Uri): String? = runCatching {
        val destino = File(pastaFotos(context), "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)!!.use { entrada ->
            destino.outputStream().use { saida -> entrada.copyTo(saida) }
        }
        destino.absolutePath
    }.getOrNull()

    /** Salva um bitmap (assinatura) como PNG interno e retorna o caminho. */
    fun salvarBitmap(context: Context, bitmap: Bitmap, prefixo: String): String {
        val arquivo = File(pastaFotos(context), "$prefixo-${UUID.randomUUID()}.png")
        arquivo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return arquivo.absolutePath
    }

    /** Cria um arquivo temporário para foto da câmera e retorna a Uri do FileProvider. */
    fun uriParaCamera(context: Context): Pair<Uri, String> {
        val arquivo = File(pastaFotos(context), "${UUID.randomUUID()}.jpg")
        arquivo.createNewFile()
        return uriDoArquivo(context, arquivo) to arquivo.absolutePath
    }

    fun uriDoArquivo(context: Context, arquivo: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", arquivo)

    /** Compartilha um arquivo via WhatsApp, e-mail etc. (seletor do sistema). */
    fun compartilhar(context: Context, arquivo: File, mime: String, titulo: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uriDoArquivo(context, arquivo))
            putExtra(Intent.EXTRA_SUBJECT, titulo)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, titulo))
    }

    /** Abre um PDF em um visualizador externo. */
    fun abrirPdf(context: Context, arquivo: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uriDoArquivo(context, arquivo), "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(intent) }
    }

    // ----- Conversão entre lista de caminhos e o campo texto do banco -----
    fun listaParaTexto(lista: List<String>): String = lista.joinToString("|")
    fun textoParaLista(texto: String): List<String> =
        texto.split("|").map { it.trim() }.filter { it.isNotEmpty() }
}
