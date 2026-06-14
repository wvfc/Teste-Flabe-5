package br.com.refrigeracaopro.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Banco de dados local (Room/SQLite). Todos os cadastros, OS, relatórios e
 * agendamentos funcionam 100% offline.
 */
@Database(
    entities = [
        User::class, Cliente::class, Equipamento::class, Servico::class,
        OrdemServico::class, Relatorio::class, Agendamento::class, Lancamento::class,
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun clienteDao(): ClienteDao
    abstract fun equipamentoDao(): EquipamentoDao
    abstract fun servicoDao(): ServicoDao
    abstract fun ordemServicoDao(): OrdemServicoDao
    abstract fun relatorioDao(): RelatorioDao
    abstract fun agendamentoDao(): AgendamentoDao
    abstract fun lancamentoDao(): LancamentoDao

    companion object {
        const val NOME_BANCO = "refrigeracao_pro.db"

        @Volatile
        private var instancia: AppDatabase? = null

        /** Migração 1→2: novos campos de manuais e diagnóstico IA. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE equipamentos ADD COLUMN manuais TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE relatorios ADD COLUMN diagnosticoIA TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE relatorios ADD COLUMN manualAnexado TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Migração 2→3: tabela de lançamentos financeiros (gestão). */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS lancamentos (" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "tipo TEXT NOT NULL, descricao TEXT NOT NULL, categoria TEXT NOT NULL, " +
                        "valor REAL NOT NULL, data INTEGER NOT NULL, formaPagamento TEXT NOT NULL, " +
                        "ordemServicoId INTEGER)"
                )
            }
        }

        /** Migração 3→4: tipo e dados extra (checklist) do relatório. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE relatorios ADD COLUMN tipo TEXT NOT NULL DEFAULT 'Refrigeração'")
                db.execSQL("ALTER TABLE relatorios ADD COLUMN dadosExtra TEXT NOT NULL DEFAULT ''")
            }
        }

        fun get(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NOME_BANCO
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instancia = it }
            }

        /** Fecha o banco (usado antes de backup/restauração). */
        fun fechar() {
            synchronized(this) {
                instancia?.close()
                instancia = null
            }
        }

        /** Serviços pré-cadastrados no primeiro uso. */
        val SERVICOS_PADRAO = listOf(
            Servico(nome = "Troca de compressor", descricao = "Substituição do compressor com solda e vácuo", valorPadrao = 0.0, tempoEstimado = "4h"),
            Servico(nome = "Limpeza de condensador", descricao = "Limpeza química/mecânica do condensador", tempoEstimado = "1h"),
            Servico(nome = "Carga de gás", descricao = "Carga de fluido refrigerante conforme placa", tempoEstimado = "1h30"),
            Servico(nome = "Detecção de vazamento", descricao = "Busca de vazamento com detector eletrônico/espuma", tempoEstimado = "2h"),
            Servico(nome = "Troca de filtro secador", descricao = "Substituição do filtro secador", tempoEstimado = "1h"),
            Servico(nome = "Vácuo no sistema", descricao = "Evacuação do sistema com bomba de vácuo", tempoEstimado = "2h"),
            Servico(nome = "Solda em tubulação", descricao = "Reparo de tubulação com solda oxiacetilênica", tempoEstimado = "1h30"),
            Servico(nome = "Troca de ventilador", descricao = "Substituição de micromotor/ventilador", tempoEstimado = "1h"),
            Servico(nome = "Ajuste de pressostato", descricao = "Regulagem de pressostato de alta/baixa", tempoEstimado = "30min"),
            Servico(nome = "Manutenção preventiva", descricao = "Checklist completo de manutenção preventiva", tempoEstimado = "2h"),
        )
    }
}
