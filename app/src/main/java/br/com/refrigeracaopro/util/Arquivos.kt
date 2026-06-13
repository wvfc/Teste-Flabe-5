package br.com.refrigeracaopro.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID

/** Utilitários de arquivos: fotos, assinaturas e compartilhamento. */
object Arquivos {

    /** Lado maior máximo (px) das fotos após compressão. */
    private const val LADO_MAXIMO = 1600
    private const val QUALIDADE_JPEG = 80

    fun pastaFotos(context: Context): File =
        File(context.filesDir, "fotos").apply { mkdirs() }

    fun pastaPdfs(context: Context): File =
        File(context.filesDir, "pdfs").apply { mkdirs() }

    fun pastaManuais(context: Context): File =
        File(context.filesDir, "manuais").apply { mkdirs() }

    /**
     * Copia uma imagem escolhida na galeria para o armazenamento interno,
     * já comprimida. Retorna o caminho do arquivo salvo (ou null em caso de erro).
     */
    fun copiarImagem(context: Context, uri: Uri): String? = runCatching {
        val temporario = File(pastaFotos(context), "tmp-${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)!!.use { entrada ->
            temporario.outputStream().use { saida -> entrada.copyTo(saida) }
        }
        val destino = File(pastaFotos(context), "${UUID.randomUUID()}.jpg")
        val ok = comprimir(temporario, destino)
        temporario.delete()
        if (ok) destino.absolutePath else null
    }.getOrNull()

    /**
     * Comprime/redimensiona uma imagem já capturada pela câmera (no próprio
     * arquivo de destino). Retorna true em caso de sucesso.
     */
    fun comprimirNoLocal(context: Context, caminho: String): Boolean = runCatching {
        val arquivo = File(caminho)
        if (!arquivo.exists() || arquivo.length() == 0L) return false
        val temporario = File(pastaFotos(context), "tmp-${UUID.randomUUID()}.jpg")
        arquivo.copyTo(temporario, overwrite = true)
        val ok = comprimir(temporario, arquivo)
        temporario.delete()
        ok
    }.getOrDefault(false)

    /** Lê o arquivo de origem, corrige a rotação (EXIF), redimensiona e grava JPEG. */
    private fun comprimir(origem: File, destino: File): Boolean {
        val opcoes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(origem.absolutePath, opcoes)
        if (opcoes.outWidth <= 0 || opcoes.outHeight <= 0) return false

        // Amostragem para não estourar memória ao decodificar
        var amostra = 1
        val maiorLado = maxOf(opcoes.outWidth, opcoes.outHeight)
        while (maiorLado / amostra > LADO_MAXIMO * 2) amostra *= 2

        val bitmap = BitmapFactory.decodeFile(
            origem.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = amostra }
        ) ?: return false

        val rotacionado = aplicarRotacaoExif(origem, bitmap)
        val escalado = redimensionar(rotacionado)
        destino.outputStream().use { escalado.compress(Bitmap.CompressFormat.JPEG, QUALIDADE_JPEG, it) }
        if (escalado != bitmap) escalado.recycle()
        bitmap.recycle()
        return true
    }

    private fun redimensionar(bitmap: Bitmap): Bitmap {
        val maiorLado = maxOf(bitmap.width, bitmap.height)
        if (maiorLado <= LADO_MAXIMO) return bitmap
        val fator = LADO_MAXIMO.toFloat() / maiorLado
        return Bitmap.createScaledBitmap(
            bitmap, (bitmap.width * fator).toInt(), (bitmap.height * fator).toInt(), true
        )
    }

    private fun aplicarRotacaoExif(arquivo: File, bitmap: Bitmap): Bitmap {
        val orientacao = runCatching {
            ExifInterface(arquivo.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val graus = when (orientacao) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val matriz = Matrix().apply { postRotate(graus) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
    }

    /** Salva um bitmap (assinatura) como PNG interno e retorna o caminho. */
    fun salvarBitmap(context: Context, bitmap: Bitmap, prefixo: String): String {
        val arquivo = File(pastaFotos(context), "$prefixo-${UUID.randomUUID()}.png")
        arquivo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return arquivo.absolutePath
    }

    /**
     * Cria um arquivo temporário para a foto da câmera e devolve a Uri segura
     * (FileProvider) junto com o caminho físico do arquivo.
     */
    fun uriParaCamera(context: Context): Pair<Uri, String> {
        val arquivo = File(pastaFotos(context), "${UUID.randomUUID()}.jpg")
        arquivo.createNewFile()
        return uriDoArquivo(context, arquivo) to arquivo.absolutePath
    }

    fun uriDoArquivo(context: Context, arquivo: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", arquivo)

    /** Verifica se há algum app de câmera capaz de atender ACTION_IMAGE_CAPTURE. */
    fun temCamera(context: Context): Boolean {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        return intent.resolveActivity(context.packageManager) != null ||
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    /** Copia um arquivo (ex.: manual em PDF) para o armazenamento interno. */
    fun copiarParaManuais(context: Context, uri: Uri, nomeSugerido: String): String? = runCatching {
        val nome = nomeSugerido.ifBlank { "manual" }.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val destino = File(pastaManuais(context), "${UUID.randomUUID()}-$nome.pdf")
        context.contentResolver.openInputStream(uri)!!.use { entrada ->
            destino.outputStream().use { saida -> entrada.copyTo(saida) }
        }
        destino.absolutePath
    }.getOrNull()

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

    /** Abre uma URL no navegador. */
    fun abrirUrl(context: Context, url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    // ----- Conversão entre lista de caminhos e o campo texto do banco -----
    fun listaParaTexto(lista: List<String>): String = lista.joinToString("|")
    fun textoParaLista(texto: String): List<String> =
        texto.split("|").map { it.trim() }.filter { it.isNotEmpty() }
}
