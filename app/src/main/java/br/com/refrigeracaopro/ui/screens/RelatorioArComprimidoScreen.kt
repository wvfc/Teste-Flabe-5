package br.com.refrigeracaopro.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.Relatorio
import br.com.refrigeracaopro.data.RelatorioArComprimido
import br.com.refrigeracaopro.data.TipoRelatorio
import br.com.refrigeracaopro.pdf.PdfGenerator
import br.com.refrigeracaopro.ui.components.AssinaturaDialog
import br.com.refrigeracaopro.ui.components.CampoMarcacao
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.LinhaAssinatura
import br.com.refrigeracaopro.ui.components.SecaoFotos
import br.com.refrigeracaopro.ui.components.SeletorOpcoes
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.components.TituloSecao
import br.com.refrigeracaopro.util.Arquivos
import br.com.refrigeracaopro.viewmodel.RelatoriosViewModel
import kotlinx.coroutines.launch

/**
 * Formulário do relatório de inspeção de compressor de AR COMPRIMIDO. Os campos
 * do checklist são guardados como JSON em Relatorio.dadosExtra.
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

    TelaBase(nav, if (relatorioId > 0) numero else "Relatório – Ar comprimido") { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)
            ) {
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

                // Seções do checklist (definidas a partir do modelo do fabricante)
                RelatorioArComprimido.SECOES.forEach { secao ->
                    TituloSecao(secao.titulo)
                    secao.campos.forEach { campo ->
                        when (campo.tipo) {
                            RelatorioArComprimido.Tipo.TEXTO -> {
                                val rotulo = if (campo.sufixo.isBlank()) campo.rotulo else "${campo.rotulo} (${campo.sufixo})"
                                CampoTexto(
                                    valor = valores[campo.chave] ?: "",
                                    aoMudar = { valores[campo.chave] = it },
                                    rotulo = rotulo,
                                )
                            }
                            RelatorioArComprimido.Tipo.MARCACAO -> CampoMarcacao(
                                rotulo = campo.rotulo,
                                opcoes = RelatorioArComprimido.MARCADORES,
                                selecionado = valores[campo.chave] ?: "",
                                aoSelecionar = { valores[campo.chave] = it },
                            )
                            RelatorioArComprimido.Tipo.SIMNAO -> CampoMarcacao(
                                rotulo = campo.rotulo,
                                opcoes = RelatorioArComprimido.OPCOES_SIMNAO,
                                selecionado = valores[campo.chave] ?: "",
                                aoSelecionar = { valores[campo.chave] = it },
                            )
                        }
                    }
                }

                TituloSecao("Recomendações e conclusão")
                CampoTexto(recomendacoes, { recomendacoes = it }, "Recomendações técnicas", linhas = 3)
                CampoTexto(conclusao, { conclusao = it }, "Conclusão", linhas = 3)

                TituloSecao("Fotos")
                SecaoFotos("Fotos da inspeção", fotos) { fotos = it }

                TituloSecao("Assinaturas")
                LinhaAssinatura("Técnico", assinaturaTecnico, { assinarTecnico = true }, { assinaturaTecnico = "" })
                LinhaAssinatura("Cliente", assinaturaCliente, { assinarCliente = true }, { assinaturaCliente = "" })
            }

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
