package br.com.refrigeracaopro.util

import android.content.Context
import android.net.Uri
import br.com.refrigeracaopro.data.AppDatabase
import java.io.File

/**
 * Backup e restauração manuais do banco de dados local (arquivo SQLite).
 * Permite ao técnico guardar uma cópia de segurança dos dados.
 */
object Backup {

    private fun arquivoBanco(context: Context): File =
        context.getDatabasePath(AppDatabase.NOME_BANCO)

    /** Copia o banco atual para a Uri escolhida pelo usuário. */
    fun exportar(context: Context, destino: Uri): Boolean = runCatching {
        // Garante que o WAL seja descarregado antes de copiar
        AppDatabase.fechar()
        context.contentResolver.openOutputStream(destino)!!.use { saida ->
            arquivoBanco(context).inputStream().use { it.copyTo(saida) }
        }
        true
    }.getOrDefault(false)

    /** Substitui o banco atual pelo conteúdo da Uri escolhida. */
    fun importar(context: Context, origem: Uri): Boolean = runCatching {
        AppDatabase.fechar()
        val destino = arquivoBanco(context)
        // Remove arquivos auxiliares do WAL para evitar inconsistência
        File(destino.path + "-wal").delete()
        File(destino.path + "-shm").delete()
        context.contentResolver.openInputStream(origem)!!.use { entrada ->
            destino.outputStream().use { entrada.copyTo(it) }
        }
        true
    }.getOrDefault(false)
}
