package br.com.refrigeracaopro.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.Relatorio
import br.com.refrigeracaopro.data.RelatorioArComprimido
import br.com.refrigeracaopro.data.TipoRelatorio
import br.com.refrigeracaopro.ia.BaseTecnica
import br.com.refrigeracaopro.ia.OpenAiClient
import br.com.refrigeracaopro.pdf.PdfGenerator
import br.com.refrigeracaopro.ui.components.AssinaturaDialog
import br.com.refrigeracaopro.ui.components.CampoMarcacao
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.LinhaAssinatura
import br.com.refrigeracaopro.ui.components.SecaoFotosComObservacao
import br.com.refrigeracaopro.ui.components.SeletorOpcoes
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.components.TituloSecao
import br.com.refrigeracaopro.util.Arquivos
import br.com.refrigeracaopro.viewmodel.RelatoriosViewModel
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.AutoAwesome

/**
 * Formulário do relatório de inspeção de compressor de AR COMPRIMIDO, no padrão
 * de abas por tipo de inspeção, com campos de preenchimento e marcadores
 * (Ok / Reparado / À Reparar / N/A). Os dados são guardados como JSON em
 * Relatorio.dadosExtra.
 */
@Composable
fun RelatorioArComprimidoFormScreen(nav: NavController, relatorioId: Long, vm: RelatoriosViewModel = viewModel()) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    val clientes by vm.clientes.collectAsState()
    val equipamentos by vm.equipamentos.collectAsState()

    var original by remember { mutableStateOf<Relatorio?>(null) }
    var numero by remember { mutableStateOf("") }
    var clienteId by remember { mutableStateOf(0L) }
    var equipamentoId by remember { mutableStateOf<Long?>(null) }
    val valores = remember { mutableStateMapOf<String, String>() }
    var recomendacoes by remember { mutableStateOf("") }
    var conclusao by remember { mutableStateOf("") }
    var fotos by remember { mutableStateOf(listOf<String>()) }
    var assinaturaTecnico by remember { mutableStateOf("") }
    var assinaturaCliente by remember { mutableStateOf("") }
    var assinarTecnico by remember { mutableStateOf(false) }
    var assinarCliente by remember { mutableStateOf(false) }
    var aba by remember { mutableStateOf(0) }
    var carregandoIA by remember { mutableStateOf(false) }

    // Gera a conclusão a partir de tudo que foi preenchido/selecionado no relatório
    fun gerarConclusaoIA() {
        if (!OpenAiClient.disponivel(context)) {
            Toast.makeText(context, "Configure a chave OpenAI e conecte-se à internet.", Toast.LENGTH_LONG).show()
            return
        }
        carregandoIA = true
        escopo.launch {
            val cliente = vm.buscarCliente(clienteId)
            val equip = equipamentoId?.let { vm.buscarEquipamento(it) }
            val dados = buildString {
                append("Cliente: ").append(cliente?.nome ?: "—").append("\n")
                equip?.let { append("Equipamento: ${it.tipo} ${it.marca} ${it.modelo}\n") }
                append("\nDADOS DA INSPEÇÃO DO COMPRESSOR DE AR COMPRIMIDO:\n")
                append(RelatorioArComprimido.resumoParaIA(valores))
                if (recomendacoes.isNotBlank()) append("\nRecomendações do técnico: ").append(recomendacoes)
            }
            val instrucoes = "BASE TÉCNICA:\n" + BaseTecnica.resumoPara(context, "compressor ar comprimido inspeção") +
                "\n\nGere um RELATÓRIO TÉCNICO de conclusão da inspeção, em texto corrido e profissional, " +
                "destacando o estado geral, itens marcados como 'À Reparar' ou 'Reparado', medições relevantes e " +
                "recomendações finais. Não use Markdown."
            val r = OpenAiClient.perguntar(context, listOf("user" to dados), instrucoes)
            when (r) {
                is OpenAiClient.Resultado.Sucesso -> conclusao = r.texto
                is OpenAiClient.Resultado.Erro -> Toast.makeText(context, r.mensagem, Toast.LENGTH_LONG).show()
            }
            carregandoIA = false
        }
    }

    LaunchedEffect(relatorioId) {
        if (relatorioId > 0) {
            vm.buscar(relatorioId)?.let { r ->
                original = r
                numero = r.numero; clienteId = r.clienteId; equipamentoId = r.equipamentoId
                recomendacoes = r.recomendacoes; conclusao = r.conclusao
                fotos = Arquivos.textoParaLista(r.fotosAntes)
                assinaturaTecnico = r.assinaturaTecnico; assinaturaCliente = r.assinaturaCliente
                valores.putAll(RelatorioArComprimido.parse(r.dadosExtra))
            }
        } else {
            numero = vm.proximoNumero()
        }
    }

    fun montar(): Relatorio = (original ?: Relatorio(numero = numero, clienteId = clienteId)).copy(
        numero = numero, clienteId = clienteId, equipamentoId = equipamentoId,
        tipo = TipoRelatorio.AR_COMPRIMIDO,
        dadosExtra = RelatorioArComprimido.toJson(valores),
        recomendacoes = recomendacoes, conclusao = conclusao,
        fotosAntes = Arquivos.listaParaTexto(fotos),
        assinaturaTecnico = assinaturaTecnico, assinaturaCliente = assinaturaCliente,
    )

    val equipsDoCliente = equipamentos.filter { it.clienteId == clienteId }
    // Abas: uma por tipo de inspeção + conclusão/fotos/assinaturas
    val titulosAbas = RelatorioArComprimido.SECOES.map { it.titulo } + listOf("Conclusão", "Fotos", "Assinaturas")

    TelaBase(nav, if (relatorioId > 0) numero else "Inspeção – Ar comprimido") { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Cabeçalho fixo
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(numero, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary)
                SeletorOpcoes(
                    "Cliente *", clientes.map { it.nome },
                    clientes.find { it.id == clienteId }?.nome ?: "",
                    aoSelecionar = { nome -> clienteId = clientes.find { it.nome == nome }?.id ?: 0L; equipamentoId = null }
                )
                SeletorOpcoes(
                    "Equipamento", equipsDoCliente.map { "${it.tipo} ${it.marca}".trim() },
                    equipsDoCliente.find { it.id == equipamentoId }?.let { "${it.tipo} ${it.marca}".trim() } ?: "",
                    aoSelecionar = { texto -> equipamentoId = equipsDoCliente.find { e -> "${e.tipo} ${e.marca}".trim() == texto }?.id }
                )
            }

            ScrollableTabRow(selectedTabIndex = aba.coerceIn(0, titulosAbas.lastIndex), edgePadding = 0.dp) {
                titulosAbas.forEachIndexed { i, titulo ->
                    Tab(selected = aba == i, onClick = { aba = i }, text = { Text(titulo) })
                }
            }

            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)
            ) {
                val secoes = RelatorioArComprimido.SECOES
                when (val titulo = titulosAbas.getOrElse(aba.coerceIn(0, titulosAbas.lastIndex)) { titulosAbas.first() }) {
                    "Conclusão" -> {
                        TituloSecao("Recomendações e conclusão")
                        CampoTexto(recomendacoes, { recomendacoes = it }, "Recomendações técnicas", linhas = 4)
                        OutlinedButton(onClick = { gerarConclusaoIA() }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (carregandoIA) "Gerando relatório..." else "Gerar relatório com IA")
                        }
                        CampoTexto(conclusao, { conclusao = it }, "Conclusão / relatório", linhas = 8)
                    }
                    "Fotos" -> {
                        TituloSecao("Fotos da inspeção")
                        SecaoFotosComObservacao(
                            titulo = "Fotos",
                            fotos = fotos,
                            observacaoDe = { valores["foto::$it"] ?: "" },
                            aoMudarFotos = { fotos = it },
                            aoMudarObservacao = { caminho, obs -> valores["foto::$caminho"] = obs },
                        )
                    }
                    "Assinaturas" -> {
                        TituloSecao("Assinaturas")
                        LinhaAssinatura("Técnico", assinaturaTecnico, { assinarTecnico = true }, { assinaturaTecnico = "" })
                        LinhaAssinatura("Cliente", assinaturaCliente, { assinarCliente = true }, { assinaturaCliente = "" })

                        TituloSecao("Contato do cliente")
                        SeletorOpcoes(
                            "Tipo de contato", listOf("WhatsApp", "E-mail"),
                            valores["contato_tipo"] ?: "",
                            aoSelecionar = { valores["contato_tipo"] = it }
                        )
                        CampoTexto(
                            valores["contato_valor"] ?: "",
                            { valores["contato_valor"] = it },
                            if ((valores["contato_tipo"] ?: "") == "E-mail") "E-mail do cliente" else "WhatsApp do cliente",
                        )
                    }
                    else -> {
                        val secao = secoes.find { it.titulo == titulo } ?: secoes.first()
                        secao.campos.forEachIndexed { idx, campo ->
                            CampoArComprimido(campo, valores)
                            if (idx < secao.campos.lastIndex) HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        }
                    }
                }
            }

            // Ações fixas no rodapé
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Button(
                    onClick = { vm.salvar(montar()) { nav.popBackStack() } },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = clienteId > 0
                ) { Text("Salvar relatório") }
                Row(Modifier.padding(top = 6.dp)) {
                    OutlinedButton(
                        onClick = {
                            escopo.launch {
                                val rel = montar(); vm.salvar(rel) {}
                                val cliente = vm.buscarCliente(clienteId)
                                val equip = equipamentoId?.let { vm.buscarEquipamento(it) }
                                val pdf = PdfGenerator.gerarRelatorioArComprimido(context, rel, cliente, equip)
                                Arquivos.abrirPdf(context, pdf)
                            }
                        },
                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                        enabled = clienteId > 0
                    ) {
                        Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp)); Text("PDF")
                    }
                    OutlinedButton(
                        onClick = {
                            escopo.launch {
                                val rel = montar()
                                val cliente = vm.buscarCliente(clienteId)
                                val equip = equipamentoId?.let { vm.buscarEquipamento(it) }
                                val pdf = PdfGenerator.gerarRelatorioArComprimido(context, rel, cliente, equip)
                                Arquivos.compartilhar(context, pdf, "application/pdf", rel.numero)
                            }
                        },
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                        enabled = clienteId > 0
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp)); Text("Compartilhar")
                    }
                }
            }
        }
    }

    if (assinarTecnico) AssinaturaDialog("Assinatura do técnico", { assinaturaTecnico = it }, { assinarTecnico = false })
    if (assinarCliente) AssinaturaDialog("Assinatura do cliente", { assinaturaCliente = it }, { assinarCliente = false })
}

/** Renderiza um campo do checklist: valor(es) de preenchimento e/ou marcador. */
@Composable
private fun CampoArComprimido(campo: RelatorioArComprimido.Campo, valores: SnapshotStateMap<String, String>) {
    Text(campo.rotulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    if (campo.opcoes.isNotEmpty()) {
        SeletorOpcoes(
            rotulo = "Selecione",
            opcoes = campo.opcoes,
            selecionado = valores[campo.chave] ?: "",
            aoSelecionar = { valores[campo.chave] = it },
        )
        return
    }
    if (campo.temValor) {
        val unico = campo.subRotulos.size == 1 && campo.subRotulos.first().isBlank()
        if (unico) {
            val rotulo = if (campo.sufixo.isBlank()) "Valor" else "Valor (${campo.sufixo})"
            CampoTexto(
                valor = valores[campo.chave] ?: "",
                aoMudar = { valores[campo.chave] = it },
                rotulo = rotulo,
                teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        } else {
            // Subcampos (fases) em pares por linha
            campo.subRotulos.chunked(2).forEach { par ->
                Row {
                    par.forEachIndexed { i, sub ->
                        val chave = RelatorioArComprimido.chaveValor(campo, sub)
                        val rotulo = if (campo.sufixo.isBlank()) sub else "$sub (${campo.sufixo})"
                        CampoTexto(
                            valor = valores[chave] ?: "",
                            aoMudar = { valores[chave] = it },
                            rotulo = rotulo,
                            modifier = Modifier.weight(1f).padding(end = if (i == 0) 4.dp else 0.dp, start = if (i == 1) 4.dp else 0.dp),
                            teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                    if (par.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
    if (campo.temMarcador) {
        val chaveM = RelatorioArComprimido.chaveMarcador(campo)
        CampoMarcacao(
            rotulo = "",
            opcoes = RelatorioArComprimido.MARCADORES,
            selecionado = valores[chaveM] ?: "",
            aoSelecionar = { valores[chaveM] = it },
        )
    }
}
