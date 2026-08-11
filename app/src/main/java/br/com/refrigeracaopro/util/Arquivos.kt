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
    private const val LADO_MAXIMO = 2200
    private const val QUALIDADE_JPEG = 92

    /**
     * Formato das imagens de termografia: normalizadas em 1080 × 900 para que
     * o laudo em PDF saia sempre com a mesma qualidade. A imagem é encaixada
     * na moldura preservando a proporção — nunca esticada, porque deformar a
     * imagem térmica falseia a leitura da escala de cores.
     */
    private const val TERMO_LARGURA = 1080
    private const val TERMO_ALTURA = 900
    private const val TERMO_QUALIDADE_JPEG = 95

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
     * Copia uma imagem para a análise termográfica, normalizando-a em
     * 1080 × 900 (encaixada na moldura, sem distorcer) com qualidade JPEG
     * mais alta — as imagens térmicas vão para o laudo em PDF e a escala de
     * cores precisa sair limpa.
     */
    fun copiarImagemTermografica(context: Context, uri: Uri): String? = runCatching {
        val temporario = File(pastaFotos(context), "tmp-${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)!!.use { entrada ->
            temporario.outputStream().use { saida -> entrada.copyTo(saida) }
        }
        val destino = File(pastaFotos(context), "termo-${UUID.randomUUID()}.jpg")
        val ok = normalizarTermografica(temporario, destino)
        temporario.delete()
        if (ok) destino.absolutePath else null
    }.getOrNull()

    /** Lê, corrige a rotação (EXIF) e grava a imagem encaixada em 1080 × 900. */
    private fun normalizarTermografica(origem: File, destino: File): Boolean {
        val opcoes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(origem.absolutePath, opcoes)
        if (opcoes.outWidth <= 0 || opcoes.outHeight <= 0) return false

        var amostra = 1
        while (opcoes.outWidth / amostra > TERMO_LARGURA * 2 && opcoes.outHeight / amostra > TERMO_ALTURA * 2) {
            amostra *= 2
        }

        val bitmap = BitmapFactory.decodeFile(
            origem.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = amostra }
        ) ?: return false

        val rotacionado = aplicarRotacaoExif(origem, bitmap)
        // Encaixa na moldura preservando a proporção (escala para cima quando a
        // imagem da câmera térmica é pequena, para manter o padrão do laudo).
        val fator = minOf(
            TERMO_LARGURA.toFloat() / rotacionado.width,
            TERMO_ALTURA.toFloat() / rotacionado.height,
        )
        val largura = (rotacionado.width * fator).toInt().coerceAtLeast(1)
        val altura = (rotacionado.height * fator).toInt().coerceAtLeast(1)
        val escalado = Bitmap.createScaledBitmap(rotacionado, largura, altura, true)

        destino.outputStream().use { escalado.compress(Bitmap.CompressFormat.JPEG, TERMO_QUALIDADE_JPEG, it) }
        if (escalado != rotacionado) escalado.recycle()
        if (rotacionado != bitmap) rotacionado.recycle()
        bitmap.recycle()
        return true
    }

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

    /** Nome de exibição de um conteúdo (uri), com fallback. */
    fun nomeDoConteudo(context: Context, uri: Uri): String = runCatching {
        var nome = "arquivo"
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) nome = c.getString(idx) ?: nome
        }
        nome
    }.getOrDefault("arquivo")

    /**
     * Lê um arquivo escolhido para anexar ao Assistente IA. Extrai o texto de
     * PDFs e de arquivos de texto, para que a IA possa resumir/analisar. Quando
     * não há texto extraível (ex.: PDF digitalizado/imagem), devolve uma nota.
     */
    fun lerArquivoTexto(context: Context, uri: Uri): Pair<String, String> {
        val nome = nomeDoConteudo(context, uri)
        val ext = nome.substringAfterLast('.', "").lowercase()
        val extensoesTexto = listOf("txt", "csv", "log", "json", "xml", "md", "kt", "java", "ini", "cfg")

        if (ext == "pdf") {
            val texto = extrairTextoPdf(context, uri)
            return if (!texto.isNullOrBlank()) {
                nome to texto.take(15000)
            } else {
                nome to "[O PDF \"$nome\" parece ser digitalizado/imagem (sem texto extraível). " +
                    "Não foi possível ler o conteúdo. Se possível, envie um PDF com texto ou tire uma foto da página.]"
            }
        }

        if (ext in extensoesTexto) {
            val conteudo = runCatching {
                context.contentResolver.openInputStream(uri)!!.use { it.bufferedReader().readText() }
            }.getOrNull()
            if (!conteudo.isNullOrBlank()) return nome to conteudo.take(15000)
        }

        return nome to "[Arquivo \"$nome\" anexado — tipo não suportado para leitura de texto. Descreva o que deseja analisar.]"
    }

    /** Extrai o texto de um PDF usando PdfBox-Android. */
    private fun extrairTextoPdf(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)!!.use { entrada ->
            com.tom_roush.pdfbox.pdmodel.PDDocument.load(entrada).use { doc ->
                com.tom_roush.pdfbox.text.PDFTextStripper().getText(doc).trim()
            }
        }
    }.getOrNull()

    // ----- Conversão entre lista de caminhos e o campo texto do banco -----
    fun listaParaTexto(lista: List<String>): String = lista.joinToString("|")
    fun textoParaLista(texto: String): List<String> =
        texto.split("|").map { it.trim() }.filter { it.isNotEmpty() }
}
