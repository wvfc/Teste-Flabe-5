package br.com.refrigeracaopro.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import br.com.refrigeracaopro.R

/**
 * Notificações locais para lembrar o técnico das manutenções agendadas.
 * Usa AlarmManager (inexato) + BroadcastReceiver — funciona offline.
 */
object Notificacoes {

    const val CANAL = "manutencoes"

    fun criarCanal(context: Context) {
        val canal = NotificationChannel(
            CANAL,
            "Manutenções agendadas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Lembretes de manutenção preventiva" }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(canal)
    }

    /** Agenda um lembrete para o horário do agendamento. */
    fun agendarLembrete(context: Context, agendamentoId: Long, titulo: String, texto: String, quandoMillis: Long) {
        if (quandoMillis <= System.currentTimeMillis()) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, LembreteReceiver::class.java).apply {
            putExtra("titulo", titulo)
            putExtra("texto", texto)
            putExtra("id", agendamentoId)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            agendamentoId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quandoMillis, pi)
    }

    fun cancelarLembrete(context: Context, agendamentoId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, LembreteReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            agendamentoId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }
}

class LembreteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val titulo = intent.getStringExtra("titulo") ?: "Manutenção agendada"
        val texto = intent.getStringExtra("texto") ?: ""
        val id = intent.getLongExtra("id", 0)

        val notificacao = NotificationCompat.Builder(context, Notificacoes.CANAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // POST_NOTIFICATIONS pode ter sido negada no Android 13+; o sistema apenas ignora
        runCatching { nm.notify(id.toInt(), notificacao) }
    }
}
