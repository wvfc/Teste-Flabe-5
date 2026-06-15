package br.com.refrigeracaopro.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.refrigeracaopro.data.Agendamento
import br.com.refrigeracaopro.data.AppDatabase
import br.com.refrigeracaopro.data.Cliente
import br.com.refrigeracaopro.data.Equipamento
import br.com.refrigeracaopro.data.OrdemServico
import br.com.refrigeracaopro.data.Relatorio
import br.com.refrigeracaopro.data.Servico
import br.com.refrigeracaopro.data.User
import br.com.refrigeracaopro.util.Notificacoes
import br.com.refrigeracaopro.util.Seguranca
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ViewModels da arquitetura MVVM. Cada ViewModel expõe StateFlows para a UI
 * (Compose) e delega persistência aos DAOs do Room.
 */

private fun AndroidViewModel.db() = AppDatabase.get(getApplication<Application>())

// ---------- Login ----------
class LoginViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = db().userDao()

    val temUsuario = MutableStateFlow<Boolean?>(null)
    val usuarioLogado = MutableStateFlow<User?>(null)
    val erro = MutableStateFlow("")

    init {
        viewModelScope.launch { temUsuario.value = dao.contar() > 0 }
    }

    fun criarAdmin(nome: String, usuario: String, senha: String, pergunta: String, resposta: String) {
        viewModelScope.launch {
            if (usuario.isBlank() || senha.length < 4) {
                erro.value = "Informe usuário e senha com ao menos 4 caracteres."
                return@launch
            }
            val salt = Seguranca.gerarSalt()
            val user = User(
                nome = nome.ifBlank { usuario },
                usuario = usuario.trim(),
                senhaHash = Seguranca.hash(senha, salt),
                salt = salt,
                perguntaSeguranca = pergunta.trim(),
                respostaHash = if (resposta.isBlank()) "" else Seguranca.hash(resposta.trim().lowercase(), salt),
            )
            dao.inserir(user)
            usuarioLogado.value = user
            temUsuario.value = true
        }
    }

    fun entrar(usuario: String, senha: String) {
        viewModelScope.launch {
            val user = dao.buscarPorUsuario(usuario.trim())
            if (user != null && Seguranca.conferir(senha, user.salt, user.senhaHash)) {
                erro.value = ""
                usuarioLogado.value = user
            } else {
                erro.value = "Usuário ou senha inválidos."
            }
        }
    }

    /** Redefinição local de senha via pergunta de segurança. */
    fun redefinirSenha(usuario: String, resposta: String, novaSenha: String, aoConcluir: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = dao.buscarPorUsuario(usuario.trim())
            when {
                user == null -> aoConcluir(false, "Usuário não encontrado.")
                user.respostaHash.isBlank() -> aoConcluir(false, "Este usuário não cadastrou pergunta de segurança.")
                !Seguranca.conferir(resposta.trim().lowercase(), user.salt, user.respostaHash) ->
                    aoConcluir(false, "Resposta de segurança incorreta.")
                novaSenha.length < 4 -> aoConcluir(false, "A nova senha deve ter ao menos 4 caracteres.")
                else -> {
                    val salt = Seguranca.gerarSalt()
                    dao.atualizar(
                        user.copy(
                            senhaHash = Seguranca.hash(novaSenha, salt),
                            salt = salt,
                            respostaHash = if (user.respostaHash.isBlank()) "" else
                                Seguranca.hash(resposta.trim().lowercase(), salt),
                        )
                    )
                    aoConcluir(true, "Senha redefinida com sucesso.")
                }
            }
        }
    }

    suspend fun perguntaDe(usuario: String): String? =
        dao.buscarPorUsuario(usuario.trim())?.perguntaSeguranca?.takeIf { it.isNotBlank() }
}

// ---------- Clientes ----------
class ClientesViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = db().clienteDao()

    val busca = MutableStateFlow("")

    val clientes: StateFlow<List<Cliente>> = busca
        .flatMapLatest { b -> if (b.isBlank()) dao.listar() else dao.pesquisar(b) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun salvar(cliente: Cliente, aoConcluir: (Long) -> Unit = {}) {
        viewModelScope.launch { aoConcluir(dao.salvar(cliente)) }
    }

    fun excluir(cliente: Cliente) {
        viewModelScope.launch { dao.excluir(cliente) }
    }

    suspend fun buscar(id: Long): Cliente? = dao.buscar(id)

    fun equipamentosDe(clienteId: Long) = db().equipamentoDao().listarPorCliente(clienteId)

    fun historicoDe(clienteId: Long) = db().ordemServicoDao().listarPorCliente(clienteId)

    /** Exporta todos os clientes (independente do filtro de busca) em CSV. */
    fun exportarCsv(aoPronto: (String) -> Unit) {
        viewModelScope.launch {
            val todos = dao.listar().first()
            aoPronto(br.com.refrigeracaopro.util.CsvClientes.exportar(todos))
        }
    }

    /** Importa clientes de um CSV; retorna a quantidade inserida. */
    fun importarCsv(conteudo: String, aoConcluir: (Int) -> Unit) {
        viewModelScope.launch {
            val lista = br.com.refrigeracaopro.util.CsvClientes.importar(conteudo)
            lista.forEach { dao.salvar(it) }
            aoConcluir(lista.size)
        }
    }
}

// ---------- Equipamentos ----------
class EquipamentosViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = db().equipamentoDao()

    val equipamentos: StateFlow<List<Equipamento>> =
        dao.listar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clientes: StateFlow<List<Cliente>> =
        db().clienteDao().listar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun salvar(equipamento: Equipamento, aoConcluir: (Long) -> Unit = {}) {
        viewModelScope.launch { aoConcluir(dao.salvar(equipamento)) }
    }

    fun excluir(equipamento: Equipamento) {
        viewModelScope.launch { dao.excluir(equipamento) }
    }

    suspend fun buscar(id: Long): Equipamento? = dao.buscar(id)

    companion object {
        val TIPOS = listOf(
            "Câmara fria", "Freezer", "Balcão refrigerado", "Ar-condicionado",
            "Chiller", "Compressor", "Secador", "Unidade condensadora", "Evaporador", "Outro"
        )
    }
}

// ---------- Serviços ----------
class ServicosViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = db().servicoDao()

    val servicos: StateFlow<List<Servico>> =
        dao.listar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun salvar(servico: Servico) {
        viewModelScope.launch { dao.salvar(servico) }
    }

    fun excluir(servico: Servico) {
        viewModelScope.launch { dao.excluir(servico) }
    }
}

// ---------- Ordens de Serviço ----------
class OrdensViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = db().ordemServicoDao()

    val ordens: StateFlow<List<OrdemServico>> =
        dao.listar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clientes: StateFlow<List<Cliente>> =
        db().clienteDao().listar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val equipamentos: StateFlow<List<Equipamento>> =
        db().equipamentoDao().listar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val servicos: StateFlow<List<Servico>> =
        db().servicoDao().listar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Número automático no formato OS-AAAA-NNNN. */
    suspend fun proximoNumero(): String {
        val ano = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val sequencia = dao.contar() + 1
        return "OS-$ano-%04d".format(sequencia)
    }

    fun salvar(os: OrdemServico, aoConcluir: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = dao.salvar(os)
            // Sincroniza o financeiro: OS concluída vira receita automaticamente
            sincronizarFinanceiro(if (os.id == 0L) os.copy(id = id) else os)
            aoConcluir(id)
        }
    }

    /**
     * Mantém o lançamento financeiro em dia com o status da OS:
     * - Concluída e com valor: cria/atualiza a Receita correspondente.
     * - Deixou de estar concluída (ex.: cancelada): remove a receita lançada.
     */
    private suspend fun sincronizarFinanceiro(os: OrdemServico) {
        val lancDao = db().lancamentoDao()
        val existente = lancDao.buscarPorOS(os.id)
        if (os.status == br.com.refrigeracaopro.data.StatusOS.CONCLUIDA && os.valorTotal > 0) {
            val base = existente ?: br.com.refrigeracaopro.data.Lancamento(
                tipo = br.com.refrigeracaopro.data.TipoLancamento.RECEITA,
                descricao = "OS ${os.numero}",
                categoria = "Serviço",
                valor = 0.0,
                data = os.dataHora,
                ordemServicoId = os.id,
            )
            lancDao.salvar(base.copy(valor = os.valorTotal, data = os.dataHora, descricao = "OS ${os.numero}"))
        } else if (existente != null) {
            lancDao.excluir(existente)
        }
    }

    fun excluir(os: OrdemServico) {
        viewModelScope.launch {
            // Remove também a receita vinculada, se houver
            db().lancamentoDao().buscarPorOS(os.id)?.let { db().lancamentoDao().excluir(it) }
            dao.excluir(os)
        }
    }

    suspend fun buscar(id: Long): OrdemServico? = dao.buscar(id)
    suspend fun buscarCliente(id: Long): Cliente? = db().clienteDao().buscar(id)
    suspend fun buscarEquipamento(id: Long): Equipamento? = db().equipamentoDao().buscar(id)
}

// ---------- Relatórios ----------
class RelatoriosViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = db().relatorioDao()

    val relatorios: StateFlow<List<Relatorio>> =
        dao.listar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clientes: StateFlow<List<Cliente>> =
        db().clienteDao().listar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val equipamentos: StateFlow<List<Equipamento>> =
        db().equipamentoDao().listar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Número automático no formato RT-AAAA-NNNN. */
    suspend fun proximoNumero(): String {
        val ano = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        return "RT-$ano-%04d".format(dao.contar() + 1)
    }

    fun salvar(relatorio: Relatorio, aoConcluir: (Long) -> Unit = {}) {
        viewModelScope.launch { aoConcluir(dao.salvar(relatorio)) }
    }

    fun excluir(relatorio: Relatorio) {
        viewModelScope.launch { dao.excluir(relatorio) }
    }

    suspend fun buscar(id: Long): Relatorio? = dao.buscar(id)
    suspend fun buscarCliente(id: Long): Cliente? = db().clienteDao().buscar(id)
    suspend fun buscarEquipamento(id: Long): Equipamento? = db().equipamentoDao().buscar(id)
    suspend fun buscarOS(id: Long): OrdemServico? = db().ordemServicoDao().buscar(id)
}

// ---------- Projetos isométricos ----------
class ProjetosViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = db().projetoDao()

    val projetos: StateFlow<List<br.com.refrigeracaopro.data.Projeto>> =
        dao.listar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clientes: StateFlow<List<Cliente>> =
        db().clienteDao().listar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun salvar(projeto: br.com.refrigeracaopro.data.Projeto, aoConcluir: (Long) -> Unit = {}) {
        viewModelScope.launch { aoConcluir(dao.salvar(projeto.copy(atualizadoEm = System.currentTimeMillis()))) }
    }

    fun duplicar(projeto: br.com.refrigeracaopro.data.Projeto, aoConcluir: () -> Unit = {}) {
        viewModelScope.launch {
            dao.salvar(projeto.copy(id = 0, nome = projeto.nome + " (cópia)", favorito = false,
                criadoEm = System.currentTimeMillis(), atualizadoEm = System.currentTimeMillis()))
            aoConcluir()
        }
    }

    fun favoritar(projeto: br.com.refrigeracaopro.data.Projeto) {
        viewModelScope.launch { dao.salvar(projeto.copy(favorito = !projeto.favorito)) }
    }

    fun excluir(projeto: br.com.refrigeracaopro.data.Projeto) {
        viewModelScope.launch { dao.excluir(projeto) }
    }

    suspend fun buscar(id: Long): br.com.refrigeracaopro.data.Projeto? = dao.buscar(id)
}

// ---------- Gestão financeira ----------
class GestaoViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = db().lancamentoDao()

    val lancamentos: StateFlow<List<br.com.refrigeracaopro.data.Lancamento>> =
        dao.listar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun salvar(lancamento: br.com.refrigeracaopro.data.Lancamento) {
        viewModelScope.launch { dao.salvar(lancamento) }
    }

    fun excluir(lancamento: br.com.refrigeracaopro.data.Lancamento) {
        viewModelScope.launch { dao.excluir(lancamento) }
    }

    /**
     * Importa como Receita as OS concluídas que ainda não possuem lançamento,
     * usando o valor total da OS. Evita duplicar pelo ordemServicoId.
     */
    fun importarReceitasDeOS(aoConcluir: (Int) -> Unit) {
        viewModelScope.launch {
            // Snapshot único da lista de OS
            val ordens = db().ordemServicoDao().listar().first()
            var criados = 0
            ordens.filter { it.status == br.com.refrigeracaopro.data.StatusOS.CONCLUIDA && it.valorTotal > 0 }
                .forEach { os ->
                    if (dao.contarPorOS(os.id) == 0) {
                        dao.salvar(
                            br.com.refrigeracaopro.data.Lancamento(
                                tipo = br.com.refrigeracaopro.data.TipoLancamento.RECEITA,
                                descricao = "OS ${os.numero}",
                                categoria = "Serviço",
                                valor = os.valorTotal,
                                data = os.dataHora,
                                ordemServicoId = os.id,
                            )
                        )
                        criados++
                    }
                }
            aoConcluir(criados)
        }
    }
}

// ---------- Agendamentos ----------
class AgendamentosViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = db().agendamentoDao()

    val agendamentos: StateFlow<List<Agendamento>> =
        dao.listar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clientes: StateFlow<List<Cliente>> =
        db().clienteDao().listar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val equipamentos: StateFlow<List<Equipamento>> =
        db().equipamentoDao().listar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun salvar(agendamento: Agendamento, nomeCliente: String) {
        viewModelScope.launch {
            val id = dao.salvar(agendamento)
            // Agenda a notificação local para o horário marcado
            if (agendamento.status == br.com.refrigeracaopro.data.StatusAgendamento.AGENDADA) {
                Notificacoes.agendarLembrete(
                    getApplication(),
                    id,
                    "Manutenção: ${agendamento.tipoManutencao}",
                    "Cliente: $nomeCliente — ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(agendamento.dataHora))}",
                    agendamento.dataHora
                )
            } else {
                Notificacoes.cancelarLembrete(getApplication(), id)
            }
        }
    }

    /**
     * Marca como realizada e, se houver periodicidade, cria automaticamente
     * a próxima ocorrência.
     */
    fun concluir(agendamento: Agendamento, nomeCliente: String) {
        viewModelScope.launch {
            dao.salvar(agendamento.copy(status = br.com.refrigeracaopro.data.StatusAgendamento.REALIZADA))
            Notificacoes.cancelarLembrete(getApplication(), agendamento.id)
            val proximaData = proximaOcorrencia(agendamento.dataHora, agendamento.periodicidade) ?: return@launch
            val proxima = agendamento.copy(id = 0, dataHora = proximaData, status = br.com.refrigeracaopro.data.StatusAgendamento.AGENDADA)
            salvar(proxima, nomeCliente)
        }
    }

    fun excluir(agendamento: Agendamento) {
        viewModelScope.launch {
            dao.excluir(agendamento)
            Notificacoes.cancelarLembrete(getApplication(), agendamento.id)
        }
    }

    private fun proximaOcorrencia(dataHora: Long, periodicidade: String): Long? {
        val cal = Calendar.getInstance().apply { timeInMillis = dataHora }
        when (periodicidade) {
            "Semanal" -> cal.add(Calendar.DAY_OF_YEAR, 7)
            "Mensal" -> cal.add(Calendar.MONTH, 1)
            "Trimestral" -> cal.add(Calendar.MONTH, 3)
            "Semestral" -> cal.add(Calendar.MONTH, 6)
            "Anual" -> cal.add(Calendar.YEAR, 1)
            else -> return null
        }
        return cal.timeInMillis
    }
}
