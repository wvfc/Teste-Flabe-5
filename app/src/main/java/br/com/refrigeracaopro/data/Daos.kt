package br.com.refrigeracaopro.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contar(): Int

    @Query("SELECT * FROM usuarios WHERE usuario = :usuario LIMIT 1")
    suspend fun buscarPorUsuario(usuario: String): User?

    @Insert
    suspend fun inserir(user: User): Long

    @Update
    suspend fun atualizar(user: User)
}

@Dao
interface ClienteDao {
    @Query("SELECT * FROM clientes ORDER BY nome COLLATE NOCASE")
    fun listar(): Flow<List<Cliente>>

    @Query(
        "SELECT * FROM clientes WHERE nome LIKE '%' || :busca || '%' " +
            "OR cpfCnpj LIKE '%' || :busca || '%' OR cidade LIKE '%' || :busca || '%' " +
            "ORDER BY nome COLLATE NOCASE"
    )
    fun pesquisar(busca: String): Flow<List<Cliente>>

    @Query("SELECT * FROM clientes WHERE id = :id")
    suspend fun buscar(id: Long): Cliente?

    @Insert
    suspend fun inserir(cliente: Cliente): Long

    @Update
    suspend fun atualizar(cliente: Cliente)

    /**
     * Insere um novo cliente ou atualiza o existente.
     *
     * IMPORTANTE: não usar @Insert(REPLACE) aqui. No SQLite, "INSERT OR REPLACE"
     * apaga a linha antiga antes de inserir a nova, e esse DELETE dispara o
     * ON DELETE CASCADE de "equipamentos" — ou seja, editar um cliente apagava
     * todos os equipamentos vinculados a ele.
     */
    @Transaction
    suspend fun salvar(cliente: Cliente): Long =
        if (cliente.id == 0L) inserir(cliente) else { atualizar(cliente); cliente.id }

    @Delete
    suspend fun excluir(cliente: Cliente)
}

@Dao
interface EquipamentoDao {
    @Query("SELECT * FROM equipamentos ORDER BY tipo")
    fun listar(): Flow<List<Equipamento>>

    @Query("SELECT * FROM equipamentos WHERE clienteId = :clienteId ORDER BY tipo")
    fun listarPorCliente(clienteId: Long): Flow<List<Equipamento>>

    @Query("SELECT * FROM equipamentos WHERE id = :id")
    suspend fun buscar(id: Long): Equipamento?

    @Insert
    suspend fun inserir(equipamento: Equipamento): Long

    @Update
    suspend fun atualizar(equipamento: Equipamento)

    /** Insere ou atualiza sem apagar a linha (evita disparar CASCADE). */
    @Transaction
    suspend fun salvar(equipamento: Equipamento): Long =
        if (equipamento.id == 0L) inserir(equipamento) else { atualizar(equipamento); equipamento.id }

    @Delete
    suspend fun excluir(equipamento: Equipamento)
}

@Dao
interface ServicoDao {
    @Query("SELECT * FROM servicos ORDER BY nome COLLATE NOCASE")
    fun listar(): Flow<List<Servico>>

    @Query("SELECT COUNT(*) FROM servicos")
    suspend fun contar(): Int

    @Insert
    suspend fun inserir(servico: Servico): Long

    @Update
    suspend fun atualizar(servico: Servico)

    /** Insere ou atualiza sem apagar a linha (evita disparar CASCADE). */
    @Transaction
    suspend fun salvar(servico: Servico): Long =
        if (servico.id == 0L) inserir(servico) else { atualizar(servico); servico.id }

    @Insert
    suspend fun inserirTodos(servicos: List<Servico>)

    @Delete
    suspend fun excluir(servico: Servico)
}

@Dao
interface OrdemServicoDao {
    @Query("SELECT * FROM ordens_servico ORDER BY dataHora DESC")
    fun listar(): Flow<List<OrdemServico>>

    @Query("SELECT * FROM ordens_servico WHERE id = :id")
    suspend fun buscar(id: Long): OrdemServico?

    @Query("SELECT COUNT(*) FROM ordens_servico")
    suspend fun contar(): Int

    @Query("SELECT * FROM ordens_servico WHERE clienteId = :clienteId ORDER BY dataHora DESC")
    fun listarPorCliente(clienteId: Long): Flow<List<OrdemServico>>

    @Insert
    suspend fun inserir(os: OrdemServico): Long

    @Update
    suspend fun atualizar(os: OrdemServico)

    /** Insere ou atualiza sem apagar a linha (evita disparar CASCADE). */
    @Transaction
    suspend fun salvar(os: OrdemServico): Long =
        if (os.id == 0L) inserir(os) else { atualizar(os); os.id }

    @Delete
    suspend fun excluir(os: OrdemServico)
}

@Dao
interface RelatorioDao {
    @Query("SELECT * FROM relatorios ORDER BY dataHora DESC")
    fun listar(): Flow<List<Relatorio>>

    @Query("SELECT * FROM relatorios WHERE id = :id")
    suspend fun buscar(id: Long): Relatorio?

    @Query("SELECT COUNT(*) FROM relatorios")
    suspend fun contar(): Int

    @Insert
    suspend fun inserir(relatorio: Relatorio): Long

    @Update
    suspend fun atualizar(relatorio: Relatorio)

    /** Insere ou atualiza sem apagar a linha (evita disparar CASCADE). */
    @Transaction
    suspend fun salvar(relatorio: Relatorio): Long =
        if (relatorio.id == 0L) inserir(relatorio) else { atualizar(relatorio); relatorio.id }

    @Delete
    suspend fun excluir(relatorio: Relatorio)
}

@Dao
interface LancamentoDao {
    @Query("SELECT * FROM lancamentos ORDER BY data DESC")
    fun listar(): Flow<List<Lancamento>>

    @Query("SELECT * FROM lancamentos WHERE data BETWEEN :inicio AND :fim ORDER BY data DESC")
    fun listarPeriodo(inicio: Long, fim: Long): Flow<List<Lancamento>>

    @Query("SELECT COUNT(*) FROM lancamentos WHERE ordemServicoId = :osId")
    suspend fun contarPorOS(osId: Long): Int

    @Query("SELECT * FROM lancamentos WHERE ordemServicoId = :osId LIMIT 1")
    suspend fun buscarPorOS(osId: Long): Lancamento?

    @Insert
    suspend fun inserir(lancamento: Lancamento): Long

    @Update
    suspend fun atualizar(lancamento: Lancamento)

    /** Insere ou atualiza sem apagar a linha (evita disparar CASCADE). */
    @Transaction
    suspend fun salvar(lancamento: Lancamento): Long =
        if (lancamento.id == 0L) inserir(lancamento) else { atualizar(lancamento); lancamento.id }

    @Delete
    suspend fun excluir(lancamento: Lancamento)
}

@Dao
interface ProjetoDao {
    @Query("SELECT * FROM projetos ORDER BY atualizadoEm DESC")
    fun listar(): Flow<List<Projeto>>

    @Query("SELECT * FROM projetos WHERE id = :id")
    suspend fun buscar(id: Long): Projeto?

    @Insert
    suspend fun inserir(projeto: Projeto): Long

    @Update
    suspend fun atualizar(projeto: Projeto)

    /** Insere ou atualiza sem apagar a linha (evita disparar CASCADE). */
    @Transaction
    suspend fun salvar(projeto: Projeto): Long =
        if (projeto.id == 0L) inserir(projeto) else { atualizar(projeto); projeto.id }

    @Delete
    suspend fun excluir(projeto: Projeto)
}

@Dao
interface AgendamentoDao {
    @Query("SELECT * FROM agendamentos ORDER BY dataHora")
    fun listar(): Flow<List<Agendamento>>

    @Query("SELECT * FROM agendamentos WHERE id = :id")
    suspend fun buscar(id: Long): Agendamento?

    @Insert
    suspend fun inserir(agendamento: Agendamento): Long

    @Update
    suspend fun atualizar(agendamento: Agendamento)

    /** Insere ou atualiza sem apagar a linha (evita disparar CASCADE). */
    @Transaction
    suspend fun salvar(agendamento: Agendamento): Long =
        if (agendamento.id == 0L) inserir(agendamento) else { atualizar(agendamento); agendamento.id }

    @Delete
    suspend fun excluir(agendamento: Agendamento)
}

@Dao
interface EnsaioIsolacaoDao {
    @Query("SELECT * FROM ensaios_isolacao ORDER BY dataHora DESC")
    fun listar(): Flow<List<EnsaioIsolacao>>

    @Query("SELECT * FROM ensaios_isolacao WHERE equipamentoId = :equipamentoId ORDER BY dataHora DESC")
    fun listarPorEquipamento(equipamentoId: Long): Flow<List<EnsaioIsolacao>>

    @Query("SELECT * FROM ensaios_isolacao WHERE clienteId = :clienteId ORDER BY dataHora DESC")
    fun listarPorCliente(clienteId: Long): Flow<List<EnsaioIsolacao>>

    @Query("SELECT * FROM ensaios_isolacao WHERE id = :id")
    suspend fun buscar(id: Long): EnsaioIsolacao?

    @Query("SELECT COUNT(*) FROM ensaios_isolacao")
    suspend fun contar(): Int

    @Insert
    suspend fun inserir(ensaio: EnsaioIsolacao): Long

    @Update
    suspend fun atualizar(ensaio: EnsaioIsolacao)

    /** Insere ou atualiza sem apagar a linha (evita disparar CASCADE). */
    @Transaction
    suspend fun salvar(ensaio: EnsaioIsolacao): Long =
        if (ensaio.id == 0L) inserir(ensaio) else { atualizar(ensaio); ensaio.id }

    @Delete
    suspend fun excluir(ensaio: EnsaioIsolacao)
}
