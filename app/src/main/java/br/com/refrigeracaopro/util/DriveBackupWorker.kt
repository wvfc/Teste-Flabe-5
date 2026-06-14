@file:Suppress("DEPRECATION")

package br.com.refrigeracaopro.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.com.refrigeracaopro.data.Prefs.backupAutomatico
import br.com.refrigeracaopro.data.Prefs.backupFrequencia
import br.com.refrigeracaopro.data.Prefs.backupSomenteWifi
import com.google.android.gms.auth.api.signin.GoogleSignIn
import java.util.concurrent.TimeUnit

/**
 * Worker que envia o backup do banco para o Google Drive (appDataFolder) em
 * segundo plano. Executa apenas se o backup automático estiver ligado e houver
 * conta Google conectada e internet (garantida pela constraint).
 */
class DriveBackupWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!ctx.backupAutomatico) return Result.success()
        val conta = GoogleSignIn.getLastSignedInAccount(ctx) ?: return Result.success()
        return when (DriveBackup.enviar(ctx, conta)) {
            is DriveBackup.Resultado.Sucesso -> Result.success()
            is DriveBackup.Resultado.Erro -> Result.retry()
        }
    }
}

/** Agenda/cancela o backup periódico conforme as preferências do usuário. */
object AgendadorBackup {

    private const val NOME = "backup_drive_periodico"

    /** Aplica o agendamento atual (chamar ao iniciar o app e ao mudar as preferências). */
    fun aplicar(context: Context) {
        val wm = WorkManager.getInstance(context)
        if (!context.backupAutomatico) {
            wm.cancelUniqueWork(NOME)
            return
        }
        val horas = if (context.backupFrequencia == "Semanal") 24L * 7 else 24L
        // Wi-Fi (rede não tarifada) ou qualquer conexão, conforme a preferência
        val rede = if (context.backupSomenteWifi) NetworkType.UNMETERED else NetworkType.CONNECTED
        val requisicao = PeriodicWorkRequestBuilder<DriveBackupWorker>(horas, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(rede).build())
            .build()
        // UPDATE mantém o histórico mas aplica o novo intervalo/constraints
        wm.enqueueUniquePeriodicWork(NOME, ExistingPeriodicWorkPolicy.UPDATE, requisicao)
    }
}
