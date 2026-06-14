package br.com.refrigeracaopro.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(cliente: Cliente): Long

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(equipamento: Equipamento): Long

    @Delete
    suspend fun excluir(equipamento: Equipamento)
}

@Dao
interface ServicoDao {
    @Query("SELECT * FROM servicos ORDER BY nome COLLATE NOCASE")
    fun listar(): Flow<List<Servico>>

    @Query("SELECT COUNT(*) FROM servicos")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(servico: Servico): Long

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(os: OrdemServico): Long

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(relatorio: Relatorio): Long

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(lancamento: Lancamento): Long

    @Delete
    suspend fun excluir(lancamento: Lancamento)
}

@Dao
interface AgendamentoDao {
    @Query("SELECT * FROM agendamentos ORDER BY dataHora")
    fun listar(): Flow<List<Agendamento>>

    @Query("SELECT * FROM agendamentos WHERE id = :id")
    suspend fun buscar(id: Long): Agendamento?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(agendamento: Agendamento): Long

    @Delete
    suspend fun excluir(agendamento: Agendamento)
}
