package br.com.refrigeracaopro.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.Avisos
import br.com.refrigeracaopro.data.PTTable
import br.com.refrigeracaopro.data.Relatorio
import br.com.refrigeracaopro.ia.BaseTecnica
import br.com.refrigeracaopro.ia.IAContexto
import br.com.refrigeracaopro.ia.OpenAiClient
import br.com.refrigeracaopro.pdf.PdfGenerator
import br.com.refrigeracaopro.ui.components.AssinaturaDialog
import br.com.refrigeracaopro.ui.components.AvisoListaCard
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.ConfirmarExclusao
import br.com.refrigeracaopro.ui.components.LinhaAssinatura
import br.com.refrigeracaopro.ui.components.SecaoFotos
import br.com.refrigeracaopro.ui.components.SeletorOpcoes
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.components.TituloSecao
import br.com.refrigeracaopro.util.Arquivos
import br.com.refrigeracaopro.viewmodel.RelatoriosViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Abre o formulário correto conforme o tipo do relatório. */
private fun abrirRelatorio(nav: NavController, id: Long, tipo: String) {
    when (tipo) {
        br.com.refrigeracaopro.data.TipoRelatorio.AR_COMPRIMIDO -> nav.navigate("relatorios/arcomp?id=$id")
        br.com.refrigeracaopro.data.TipoRelatorio.GERAL -> nav.navigate("relatorios/form?id=$id&osId=0&tipo=geral")
        else -> nav.navigate("relatorios/form?id=$id&osId=0&tipo=refrigeracao")
    }
}

/** Lista de relatórios, com escolha do tipo ao criar um novo. */
@Composable
fun RelatoriosScreen(nav: NavController, vm: RelatoriosViewModel = viewModel()) {
    val relatorios by vm.relatorios.collectAsState()
    val clientes by vm.clientes.collectAsState()
    var excluir by remember { mutableStateOf<Relatorio?>(null) }
    var escolherTipo by remember { mutableStateOf(false) }
    val formatoData = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }

    TelaBase(nav, "Relatórios", aoAdicionar = { escolherTipo = true }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            items(relatorios, key = { it.id }) { rel ->
                val nomeCliente = clientes.find { it.id == rel.clienteId }?.nome ?: "—"
                Card(
                    onClick = { abrirRelatorio(nav, rel.id, rel.tipo) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${rel.numero} • $nomeCliente", fontWeight = FontWeight.Bold)
                            Text("${rel.tipo} • ${formatoData.format(Date(rel.dataHora))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { excluir = rel }) {
                            Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (escolherTipo) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { escolherTipo = false },
            title = { Text("Novo relatório") },
            text = {
                Column {
                    Text("Escolha o tipo de relatório:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    OpcaoTipo("Técnico de refrigeração", "Pressões, temperaturas, superaquecimento/subresfriamento e diagnóstico IA.") {
                        escolherTipo = false; nav.navigate("relatorios/form?id=0&osId=0&tipo=refrigeracao")
                    }
                    OpcaoTipo("Compressor de ar comprimido", "Checklist de inspeção (horímetro, óleo, elétrica, pressões e temperaturas).") {
                        escolherTipo = false; nav.navigate("relatorios/arcomp?id=0")
                    }
                    OpcaoTipo("Geral (simples)", "Relatório enxuto: dados, serviços, recomendações, fotos e assinaturas.") {
                        escolherTipo = false; nav.navigate("relatorios/form?id=0&osId=0&tipo=geral")
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { escolherTipo = false }) { Text("Cancelar") } }
        )
    }

    excluir?.let { rel ->
        ConfirmarExclusao(
            "Excluir o relatório ${rel.numero}?",
            aoConfirmar = { vm.excluir(rel); excluir = null },
            aoCancelar = { excluir = null }
        )
    }
}

@Composable
private fun OpcaoTipo(titulo: String, descricao: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(titulo, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(descricao, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Formulário do relatório técnico completo: dados, medições, cálculo automático
 * de superaquecimento/subresfriamento, sugestão de IA, fotos, assinaturas e PDF.
 */
@Composable
fun RelatorioFormScreen(
    nav: NavController,
    relatorioId: Long,
    osId: Long = 0L,
    tipoArg: String = "refrigeracao",
    vm: RelatoriosViewModel = viewModel(),
) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    val clientes by vm.clientes.collectAsState()
    val equipamentos by vm.equipamentos.collectAsState()
    // "refrigeracao" (completo) ou "geral" (simples)
    val refrig = tipoArg != "geral"
    val tipoRelatorio = if (refrig) br.com.refrigeracaopro.data.TipoRelatorio.REFRIGERACAO
        else br.com.refrigeracaopro.data.TipoRelatorio.GERAL

    var original by remember { mutableStateOf<Relatorio?>(null) }
    var numero by remember { mutableStateOf("") }
    var clienteId by remember { mutableStateOf(0L) }
    var equipamentoId by remember { mutableStateOf<Long?>(null) }
    var ordemServicoId by remember { mutableStateOf<Long?>(null) }
    var motivo by remember { mutableStateOf("") }
    var diagnostico by remember { mutableStateOf("") }
    var corrente by remember { mutableStateOf("") }
    var tensao by remember { mutableStateOf("") }
    var pSuccao by remember { mutableStateOf("") }
    var pDescarga by remember { mutableStateOf("") }
    var tSuccao by remember { mutableStateOf("") }
    var tLiquido by remember { mutableStateOf("") }
    var tAmbiente by remember { mutableStateOf("") }
    var tInterna by remember { mutableStateOf("") }
    var fluido by remember { mutableStateOf("") }
    var tEvap by remember { mutableStateOf("") }
    var tCond by remember { mutableStateOf("") }
    var superaq by remember { mutableStateOf("") }
    var subresf by remember { mutableStateOf("") }
    var servicos by remember { mutableStateOf("") }
    var pecas by remember { mutableStateOf("") }
    var recomendacoes by remember { mutableStateOf("") }
    var conclusao by remember { mutableStateOf("") }
    var fotosAntes by remember { mutableStateOf(listOf<String>()) }
    var fotosDurante by remember { mutableStateOf(listOf<String>()) }
    var fotosDepois by remember { mutableStateOf(listOf<String>()) }
    var assinaturaTecnico by remember { mutableStateOf("") }
    var assinaturaCliente by remember { mutableStateOf("") }
    var assinarTecnico by remember { mutableStateOf(false) }
    var assinarCliente by remember { mutableStateOf(false) }
    var carregandoIA by remember { mutableStateOf(false) }
    var diagnosticoIA by remember { mutableStateOf("") }
    var manualAnexado by remember { mutableStateOf("") }
    var carregandoDiag by remember { mutableStateOf(false) }
    var aba by remember { mutableStateOf(0) }

    LaunchedEffect(relatorioId, osId) {
        if (relatorioId > 0) {
            vm.buscar(relatorioId)?.let { r ->
                original = r
                numero = r.numero; clienteId = r.clienteId; equipamentoId = r.equipamentoId
                ordemServicoId = r.ordemServicoId
                motivo = r.motivoVisita; diagnostico = r.diagnostico
                corrente = r.correnteEletrica; tensao = r.tensaoEletrica
                pSuccao = r.pressaoSuccao; pDescarga = r.pressaoDescarga
                tSuccao = r.tempLinhaSuccao; tLiquido = r.tempLinhaLiquido
                tAmbiente = r.tempAmbiente; tInterna = r.tempInterna
                fluido = r.fluido; tEvap = r.tempEvaporacao; tCond = r.tempCondensacao
                superaq = r.superaquecimento; subresf = r.subresfriamento
                servicos = r.servicosRealizados; pecas = r.pecasSubstituidas
                recomendacoes = r.recomendacoes; conclusao = r.conclusao
                diagnosticoIA = r.diagnosticoIA; manualAnexado = r.manualAnexado
                fotosAntes = Arquivos.textoParaLista(r.fotosAntes)
                fotosDurante = Arquivos.textoParaLista(r.fotosDurante)
                fotosDepois = Arquivos.textoParaLista(r.fotosDepois)
                assinaturaTecnico = r.assinaturaTecnico; assinaturaCliente = r.assinaturaCliente
            }
        } else {
            numero = vm.proximoNumero()
            // Quando criado a partir de uma OS, herda os dados
            if (osId > 0) vm.buscarOS(osId)?.let { os ->
                ordemServicoId = os.id
                clienteId = os.clienteId
                equipamentoId = os.equipamentoId
                motivo = os.defeitoInformado
                diagnostico = os.diagnostico
                servicos = os.servicosExecutados
                pecas = os.pecasUtilizadas
                os.equipamentoId?.let { eid -> vm.buscarEquipamento(eid)?.let { fluido = it.fluido } }
            }
        }
    }

    // Recalcula superaquecimento/subresfriamento automaticamente (pressões em psi)
    fun recalcular() {
        val f = PTTable.porNome(fluido) ?: return
        pSuccao.replace(",", ".").toDoubleOrNull()?.let { psi ->
            tSuccao.replace(",", ".").toDoubleOrNull()?.let { t ->
                val r = PTTable.superaquecimento(f, PTTable.psiParaBar(psi), t)
                superaq = "%.1f".format(r.valor)
                tEvap = "%.1f".format(r.tempSaturacao)
            }
        }
        pDescarga.replace(",", ".").toDoubleOrNull()?.let { psi ->
            tLiquido.replace(",", ".").toDoubleOrNull()?.let { t ->
                val r = PTTable.subresfriamento(f, PTTable.psiParaBar(psi), t)
                subresf = "%.1f".format(r.valor)
                tCond = "%.1f".format(r.tempSaturacao)
            }
        }
    }

    fun montar(): Relatorio = (original ?: Relatorio(numero = numero, clienteId = clienteId)).copy(
        numero = numero, clienteId = clienteId, equipamentoId = equipamentoId, ordemServicoId = ordemServicoId,
        tipo = tipoRelatorio,
        motivoVisita = motivo, diagnostico = diagnostico, correnteEletrica = corrente, tensaoEletrica = tensao,
        pressaoSuccao = pSuccao, pressaoDescarga = pDescarga, tempLinhaSuccao = tSuccao, tempLinhaLiquido = tLiquido,
        tempAmbiente = tAmbiente, tempInterna = tInterna, fluido = fluido, tempEvaporacao = tEvap, tempCondensacao = tCond,
        superaquecimento = superaq, subresfriamento = subresf, servicosRealizados = servicos, pecasSubstituidas = pecas,
        recomendacoes = recomendacoes, conclusao = conclusao,
        diagnosticoIA = diagnosticoIA, manualAnexado = manualAnexado,
        fotosAntes = Arquivos.listaParaTexto(fotosAntes), fotosDurante = Arquivos.listaParaTexto(fotosDurante),
        fotosDepois = Arquivos.listaParaTexto(fotosDepois),
        assinaturaTecnico = assinaturaTecnico, assinaturaCliente = assinaturaCliente,
    )

    val equipsDoCliente = equipamentos.filter { it.clienteId == clienteId }

    // Anexa um manual técnico (PDF) ao relatório
    val anexarManual = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) Arquivos.copiarParaManuais(context, uri, numero)?.let { manualAnexado = it }
    }

    // Gera a sugestão de diagnóstico da IA com base nas medições + base técnica
    fun gerarDiagnosticoIA() {
        if (!OpenAiClient.disponivel(context)) {
            Toast.makeText(context, "Configure a chave OpenAI e conecte-se à internet.", Toast.LENGTH_LONG).show()
            return
        }
        carregandoDiag = true
        escopo.launch {
            val ctx = IAContexto.deRelatorio(montar(), equipsDoCliente.find { it.id == equipamentoId })
            val instrucoes = "BASE TÉCNICA (consulte antes de responder):\n" +
                BaseTecnica.resumoPara(context, "$diagnostico $motivo $fluido diagnóstico") +
                "\n\n" + BaseTecnica.FORMATO_DIAGNOSTICO
            val r = OpenAiClient.perguntar(
                context,
                listOf("user" to "Analise o caso e gere o DIAGNÓSTICO no formato pedido:\n\n$ctx"),
                instrucoes,
            )
            when (r) {
                is OpenAiClient.Resultado.Sucesso -> diagnosticoIA = r.texto
                is OpenAiClient.Resultado.Erro -> Toast.makeText(context, r.mensagem, Toast.LENGTH_LONG).show()
            }
            carregandoDiag = false
        }
    }

    // No relatório "geral" (simples) ocultamos medições, diagnóstico IA e manual
    val abas = buildList {
        add("Dados")
        if (refrig) add("Diagnóstico IA")
        add("Fotos"); add("Peças/Serviços"); add("Recomendações")
        if (refrig) add("Manual")
        add("Conclusão"); add("Assinaturas")
    }

    TelaBase(nav, if (relatorioId > 0) numero else "Novo relatório") { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Abas fixas para trocar de seção; o restante rola (mais espaço ao digitar)
            ScrollableTabRow(selectedTabIndex = aba, edgePadding = 0.dp) {
                abas.forEachIndexed { i, titulo ->
                    Tab(selected = aba == i, onClick = { aba = i }, text = { Text(titulo) })
                }
            }

            // ---- Cabeçalho + conteúdo da aba (rolável) ----
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(16.dp)
            ) {
                // Cabeçalho: número, cliente e equipamento (rolam junto com o formulário)
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
                    aoSelecionar = { texto ->
                        val e = equipsDoCliente.find { e -> "${e.tipo} ${e.marca}".trim() == texto }
                        equipamentoId = e?.id
                        if (e != null && e.fluido.isNotBlank()) fluido = e.fluido
                    }
                )
                HorizontalDivider(Modifier.padding(vertical = 10.dp))

                when (abas.getOrElse(aba.coerceIn(0, abas.lastIndex)) { abas.first() }) {
                    "Dados" -> {
                        TituloSecao("Motivo e diagnóstico")
                        CampoTexto(motivo, { motivo = it }, "Motivo da visita", linhas = 2)
                        CampoTexto(diagnostico, { diagnostico = it }, "Diagnóstico", linhas = 3)

                        if (refrig) {
                        TituloSecao("Medições elétricas")
                        Row {
                            CampoTexto(corrente, { corrente = it }, "Corrente (A)", modifier = Modifier.weight(1f).padding(end = 4.dp),
                                teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
                            CampoTexto(tensao, { tensao = it }, "Tensão (V)", modifier = Modifier.weight(1f).padding(start = 4.dp),
                                teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }

                        TituloSecao("Fluido e pressões")
                        SeletorOpcoes("Fluido refrigerante", PTTable.NOMES, fluido, { fluido = it; recalcular() })
                        Row {
                            CampoTexto(pSuccao, { pSuccao = it; recalcular() }, "Pressão sucção (psi)",
                                modifier = Modifier.weight(1f).padding(end = 4.dp),
                                teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
                            CampoTexto(pDescarga, { pDescarga = it; recalcular() }, "Pressão descarga (psi)",
                                modifier = Modifier.weight(1f).padding(start = 4.dp),
                                teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }

                        TituloSecao("Temperaturas")
                        Row {
                            CampoTexto(tSuccao, { tSuccao = it; recalcular() }, "Linha sucção (°C)",
                                modifier = Modifier.weight(1f).padding(end = 4.dp),
                                teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
                            CampoTexto(tLiquido, { tLiquido = it; recalcular() }, "Linha líquido (°C)",
                                modifier = Modifier.weight(1f).padding(start = 4.dp),
                                teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                        Row {
                            CampoTexto(tAmbiente, { tAmbiente = it }, "Ambiente (°C)", modifier = Modifier.weight(1f).padding(end = 4.dp),
                                teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
                            CampoTexto(tInterna, { tInterna = it }, "Interna (°C)", modifier = Modifier.weight(1f).padding(start = 4.dp),
                                teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                        Row {
                            CampoTexto(tEvap, { tEvap = it }, "T. evaporação (°C)", modifier = Modifier.weight(1f).padding(end = 4.dp),
                                teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
                            CampoTexto(tCond, { tCond = it }, "T. condensação (°C)", modifier = Modifier.weight(1f).padding(start = 4.dp),
                                teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                        Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Calculate, null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Superaquecimento: ${superaq.ifBlank { "—" }} K", fontWeight = FontWeight.SemiBold)
                                    Text("Subresfriamento: ${subresf.ifBlank { "—" }} K", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        } // fim das medições (somente refrigeração)
                    }
                    "Diagnóstico IA" -> {
                        TituloSecao("Diagnóstico assistido por IA")
                        Text("Gera diagnóstico provável, causas, testes, riscos, correção, segurança e grau de confiança, " +
                            "usando as medições e a base técnica interna.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = { gerarDiagnosticoIA() }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (carregandoDiag) "Gerando diagnóstico..." else "Gerar diagnóstico com IA")
                        }
                        CampoTexto(diagnosticoIA, { diagnosticoIA = it }, "Diagnóstico da IA (editável)", linhas = 10)
                    }
                    "Fotos" -> {
                        TituloSecao("Fotos antes")
                        SecaoFotos("Antes", fotosAntes) { fotosAntes = it }
                        TituloSecao("Fotos durante")
                        SecaoFotos("Durante", fotosDurante) { fotosDurante = it }
                        TituloSecao("Fotos depois")
                        SecaoFotos("Depois", fotosDepois) { fotosDepois = it }
                    }
                    "Peças/Serviços" -> {
                        TituloSecao("Peças e serviços")
                        CampoTexto(servicos, { servicos = it }, "Serviços realizados", linhas = 3)
                        CampoTexto(pecas, { pecas = it }, "Peças substituídas", linhas = 3)
                    }
                    "Recomendações" -> {
                        TituloSecao("Recomendações técnicas")
                        CampoTexto(recomendacoes, { recomendacoes = it }, "Recomendações", linhas = 5)
                    }
                    "Manual" -> {
                        TituloSecao("Manual anexado")
                        if (manualAnexado.isNotBlank() && File(manualAnexado).exists()) {
                            Card(onClick = { Arquivos.abrirPdf(context, File(manualAnexado)) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AttachFile, null, tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(File(manualAnexado).name.substringAfter("-"), Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium)
                                    IconButton(onClick = { manualAnexado = "" }) {
                                        Icon(Icons.Default.Delete, "Remover", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        } else {
                            Text("Nenhum manual anexado.", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = { anexarManual.launch("application/pdf") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp)); Text("Anexar manual (PDF)")
                        }
                        Text("Use o módulo \"Buscar Manual\" no menu para localizar o PDF do fabricante.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                    "Conclusão" -> {
                        TituloSecao("Conclusão técnica")
                        CampoTexto(conclusao, { conclusao = it }, "Conclusão", linhas = 5)
                        OutlinedButton(
                            onClick = {
                                if (!OpenAiClient.disponivel(context)) {
                                    Toast.makeText(context, "Configure a chave OpenAI e conecte-se à internet.", Toast.LENGTH_LONG).show()
                                    return@OutlinedButton
                                }
                                carregandoIA = true
                                escopo.launch {
                                    val ctx = IAContexto.deRelatorio(montar(), equipsDoCliente.find { it.id == equipamentoId })
                                    val r = OpenAiClient.perguntar(context, listOf(
                                        "user" to "Com base nos dados técnicos a seguir, gere uma CONCLUSÃO TÉCNICA " +
                                            "objetiva e profissional para o relatório (máx. 6 linhas):\n\n$ctx"
                                    ))
                                    when (r) {
                                        is OpenAiClient.Resultado.Sucesso -> conclusao = r.texto
                                        is OpenAiClient.Resultado.Erro -> Toast.makeText(context, r.mensagem, Toast.LENGTH_LONG).show()
                                    }
                                    carregandoIA = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (carregandoIA) "Gerando..." else "Gerar conclusão com IA")
                        }
                    }
                    else -> {
                        TituloSecao("Assinaturas")
                        LinhaAssinatura("Técnico", assinaturaTecnico, { assinarTecnico = true }, { assinaturaTecnico = "" })
                        LinhaAssinatura("Cliente", assinaturaCliente, { assinarCliente = true }, { assinaturaCliente = "" })
                        AvisoListaCard("Segurança e avisos técnicos", Avisos.SEGURANCA)
                    }
                }

                // Ações no fim do formulário (rolam junto para liberar a área de edição)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { vm.salvar(montar()) { nav.popBackStack() } },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = clienteId > 0
                ) { Text("Salvar relatório") }
                Row(Modifier.padding(top = 6.dp)) {
                    OutlinedButton(
                        onClick = {
                            escopo.launch {
                                val rel = montar()
                                vm.salvar(rel) {}
                                val cliente = vm.buscarCliente(clienteId)
                                val equip = equipamentoId?.let { vm.buscarEquipamento(it) }
                                val pdf = PdfGenerator.gerarRelatorio(context, rel, cliente, equip)
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
                                val pdf = PdfGenerator.gerarRelatorio(context, rel, cliente, equip)
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

    if (assinarTecnico) {
        AssinaturaDialog("Assinatura do técnico", { assinaturaTecnico = it }, { assinarTecnico = false })
    }
    if (assinarCliente) {
        AssinaturaDialog("Assinatura do cliente", { assinaturaCliente = it }, { assinarCliente = false })
    }
}
