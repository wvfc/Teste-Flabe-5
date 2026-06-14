package br.com.refrigeracaopro.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidades do banco Room.
 *
 * Observações de modelagem:
 * - Listas de fotos são armazenadas como caminhos de arquivo separados por "|"
 *   (os arquivos ficam no armazenamento interno do app, funcionando offline).
 * - Valores monetários em centavos? Não: usamos Double por simplicidade do MVP.
 */

/** Usuário local do app (login offline). Senha guardada como hash SHA-256 + salt. */
@Entity(tableName = "usuarios")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val usuario: String,
    val senhaHash: String,
    val salt: String,
    val perguntaSeguranca: String = "",
    val respostaHash: String = "",
    val admin: Boolean = true,
)

@Entity(tableName = "clientes")
data class Cliente(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val cpfCnpj: String = "",
    val telefone: String = "",
    val whatsapp: String = "",
    val email: String = "",
    val endereco: String = "",
    val cidade: String = "",
    val estado: String = "",
    val observacoes: String = "",
    val criadoEm: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "equipamentos",
    foreignKeys = [ForeignKey(
        entity = Cliente::class,
        parentColumns = ["id"],
        childColumns = ["clienteId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("clienteId")]
)
data class Equipamento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clienteId: Long,
    val tipo: String, // câmara fria, freezer, balcão, ar-condicionado, chiller...
    val marca: String = "",
    val modelo: String = "",
    val numeroSerie: String = "",
    val fluido: String = "",
    val tensao: String = "",
    val potencia: String = "",
    val localInstalacao: String = "",
    val dataInstalacao: String = "",
    val observacoes: String = "",
    val fotos: String = "", // caminhos separados por "|"
    val manuais: String = "", // manuais técnicos anexados (caminhos/URLs separados por "|")
)

/** Serviço de catálogo (ex.: troca de compressor, carga de gás). */
@Entity(tableName = "servicos")
data class Servico(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val descricao: String = "",
    val valorPadrao: Double = 0.0,
    val tempoEstimado: String = "",
)

@Entity(
    tableName = "ordens_servico",
    indices = [Index("clienteId"), Index("equipamentoId")]
)
data class OrdemServico(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val numero: String, // gerado automaticamente: OS-AAAA-NNNN
    val dataHora: Long = System.currentTimeMillis(),
    val clienteId: Long,
    val equipamentoId: Long? = null,
    val tecnico: String = "",
    val defeitoInformado: String = "",
    val diagnostico: String = "",
    val servicosExecutados: String = "",
    val pecasUtilizadas: String = "",
    val valorMaoDeObra: Double = 0.0,
    val valorPecas: Double = 0.0,
    val status: String = StatusOS.ABERTA,
    val assinaturaCliente: String = "", // caminho do PNG da assinatura
    val fotosAntes: String = "",
    val fotosDurante: String = "",
    val fotosDepois: String = "",
) {
    val valorTotal: Double get() = valorMaoDeObra + valorPecas
}

object StatusOS {
    const val ABERTA = "Aberta"
    const val EM_ANDAMENTO = "Em andamento"
    const val AGUARDANDO_PECA = "Aguardando peça"
    const val CONCLUIDA = "Concluída"
    const val CANCELADA = "Cancelada"
    val TODOS = listOf(ABERTA, EM_ANDAMENTO, AGUARDANDO_PECA, CONCLUIDA, CANCELADA)
}

/** Relatório técnico completo, com medições e fotos. */
@Entity(
    tableName = "relatorios",
    indices = [Index("clienteId"), Index("equipamentoId")]
)
data class Relatorio(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val numero: String, // RT-AAAA-NNNN
    val dataHora: Long = System.currentTimeMillis(),
    val clienteId: Long,
    val equipamentoId: Long? = null,
    val ordemServicoId: Long? = null, // quando gerado a partir de uma OS
    val tipo: String = TipoRelatorio.REFRIGERACAO, // tipo do relatório
    val dadosExtra: String = "", // campos do checklist (JSON) p/ relatório de ar comprimido
    val motivoVisita: String = "",
    val diagnostico: String = "",
    val diagnosticoIA: String = "", // sugestão de diagnóstico gerada pela IA
    val manualAnexado: String = "", // manual técnico vinculado ao relatório
    // Medições elétricas
    val correnteEletrica: String = "",
    val tensaoEletrica: String = "",
    // Medições de pressão (bar manométrico)
    val pressaoSuccao: String = "",
    val pressaoDescarga: String = "",
    // Temperaturas (°C)
    val tempLinhaSuccao: String = "",
    val tempLinhaLiquido: String = "",
    val tempAmbiente: String = "",
    val tempInterna: String = "",
    val tempEvaporacao: String = "",
    val tempCondensacao: String = "",
    val fluido: String = "",
    // Calculados
    val superaquecimento: String = "",
    val subresfriamento: String = "",
    val servicosRealizados: String = "",
    val pecasSubstituidas: String = "",
    val recomendacoes: String = "",
    val conclusao: String = "",
    val fotosAntes: String = "",
    val fotosDurante: String = "",
    val fotosDepois: String = "",
    val assinaturaTecnico: String = "",
    val assinaturaCliente: String = "",
)

@Entity(
    tableName = "agendamentos",
    indices = [Index("clienteId"), Index("equipamentoId")]
)
data class Agendamento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clienteId: Long,
    val equipamentoId: Long? = null,
    val tipoManutencao: String = "Preventiva",
    val dataHora: Long,
    val periodicidade: String = Periodicidade.UNICA,
    val observacoes: String = "",
    val status: String = StatusAgendamento.AGENDADA,
)

object Periodicidade {
    const val UNICA = "Única"
    val TODAS = listOf(UNICA, "Semanal", "Mensal", "Trimestral", "Semestral", "Anual")
}

object TipoRelatorio {
    const val REFRIGERACAO = "Refrigeração"
    const val AR_COMPRIMIDO = "Ar comprimido"
    const val GERAL = "Geral"
    val TODOS = listOf(REFRIGERACAO, AR_COMPRIMIDO, GERAL)
}

object StatusAgendamento {
    const val AGENDADA = "Agendada"
    const val REALIZADA = "Realizada"
    const val CANCELADA = "Cancelada"
    val TODOS = listOf(AGENDADA, REALIZADA, CANCELADA)
}

/** Lançamento financeiro (gestão de receitas e despesas). */
@Entity(tableName = "lancamentos")
data class Lancamento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: String = TipoLancamento.RECEITA, // Receita ou Despesa
    val descricao: String,
    val categoria: String = "",
    val valor: Double = 0.0,
    val data: Long = System.currentTimeMillis(),
    val formaPagamento: String = "",
    val ordemServicoId: Long? = null, // quando gerado a partir de uma OS concluída
)

object TipoLancamento {
    const val RECEITA = "Receita"
    const val DESPESA = "Despesa"
    val TODOS = listOf(RECEITA, DESPESA)

    val CATEGORIAS_RECEITA = listOf("Serviço", "Venda de peça", "Contrato", "Outros")
    val CATEGORIAS_DESPESA = listOf("Peças/Materiais", "Combustível", "Ferramentas", "Impostos", "Salários", "Outros")
}
