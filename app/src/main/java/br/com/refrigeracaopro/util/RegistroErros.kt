package br.com.refrigeracaopro.util

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Captura de falhas para diagnóstico em campo.
 *
 * O app roda em obra, longe de qualquer computador: quando ele fecha sozinho,
 * não há como ler o logcat. Este registro grava a pilha do erro num arquivo
 * interno para que o técnico possa compartilhá-la depois.
 *
 * O tratador anterior continua sendo chamado, então o comportamento do
 * sistema (diálogo de "app parou") não muda.
 */
object RegistroErros {

    private const val ARQUIVO = "ultimo-erro.txt"

    private fun arquivo(context: Context): File =
        File(File(context.filesDir, "logs").apply { mkdirs() }, ARQUIVO)

    /** Instala o tratador global. Chamado uma vez, na criação do app. */
    fun instalar(context: Context) {
        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, erro ->
            runCatching { gravar(context, thread, erro) }
            anterior?.uncaughtException(thread, erro)
        }
    }

    private fun gravar(context: Context, thread: Thread, erro: Throwable) {
        val pilha = StringWriter().also { erro.printStackTrace(PrintWriter(it)) }.toString()
        val quando = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR")).format(Date())
        val versao = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()

        arquivo(context).writeText(
            buildString {
                appendLine("Gestão Pro — relatório de erro")
                appendLine("Data: $quando")
                appendLine("Versão do app: $versao")
                appendLine("Aparelho: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})")
                appendLine("Thread: ${thread.name}")
                appendLine()
                appendLine(pilha)
            }
        )
    }

    /** Existe um relatório de erro guardado? */
    fun existe(context: Context): Boolean = arquivo(context).let { it.exists() && it.length() > 0 }

    /** Primeira linha útil da pilha, para mostrar um resumo na tela. */
    fun resumo(context: Context): String = runCatching {
        arquivo(context).readLines()
            .firstOrNull { it.contains("Exception") || it.contains("Error") }
            ?.trim()
            .orEmpty()
    }.getOrDefault("")

    /** Compartilha o relatório (e-mail, WhatsApp, o que o técnico preferir). */
    fun compartilhar(context: Context) {
        val origem = arquivo(context)
        if (!origem.exists()) return
        val destino = File(Arquivos.pastaPdfs(context), "relatorio-erro.txt")
        origem.copyTo(destino, overwrite = true)
        Arquivos.compartilhar(context, destino, "text/plain", "Relatório de erro do Gestão Pro")
    }

    fun limpar(context: Context) {
        arquivo(context).delete()
    }
}
