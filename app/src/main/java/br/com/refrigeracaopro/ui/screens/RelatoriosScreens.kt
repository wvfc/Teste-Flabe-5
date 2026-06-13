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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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

/** Lista de relatórios técnicos. */
@Composable
fun RelatoriosScreen(nav: NavController, vm: RelatoriosViewModel = viewModel()) {
    val relatorios by vm.relatorios.collectAsState()
    val clientes by vm.clientes.collectAsState()
    var excluir by remember { mutableStateOf<Relatorio?>(null) }
    val formatoData = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }

    TelaBase(nav, "Relatórios técnicos", aoAdicionar = { nav.navigate("relatorios/form?id=0") }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            items(relatorios, key = { it.id }) { rel ->
                val nomeCliente = clientes.find { it.id == rel.clienteId }?.nome ?: "—"
                Card(
                    onClick = { nav.navigate("relatorios/form?id=${rel.id}") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${rel.numero} • $nomeCliente", fontWeight = FontWeight.Bold)
                            Text(formatoData.format(Date(rel.dataHora)),
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

    excluir?.let { rel ->
        ConfirmarExclusao(
            "Excluir o relatório ${rel.numero}?",
            aoConfirmar = { vm.excluir(rel); excluir = null },
            aoCancelar = { excluir = null }
        )
    }
}

/**
 * Formulário do relatório técnico completo: dados, medições, cálculo automático
 * de superaquecimento/subresfriamento, sugestão de IA, fotos, assinaturas e PDF.
 */
@Composable
fun RelatorioFormScreen(nav: NavController, relatorioId: Long, osId: Long = 0L, vm: RelatoriosViewModel = viewModel()) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    val clientes by vm.clientes.collectAsState()
    val equipamentos by vm.equipamentos.collectAsState()

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

    // Recalcula superaquecimento/subresfriamento automaticamente
    fun recalcular() {
        val f = PTTable.porNome(fluido) ?: return
        pSuccao.replace(",", ".").toDoubleOrNull()?.let { p ->
            tSuccao.replace(",", ".").toDoubleOrNull()?.let { t ->
                val r = PTTable.superaquecimento(f, p, t)
                superaq = "%.1f".format(r.valor)
                tEvap = "%.1f".format(r.tempSaturacao)
            }
        }
        pDescarga.replace(",", ".").toDoubleOrNull()?.let { p ->
            tLiquido.replace(",", ".").toDoubleOrNull()?.let { t ->
                val r = PTTable.subresfriamento(f, p, t)
                subresf = "%.1f".format(r.valor)
                tCond = "%.1f".format(r.tempSaturacao)
            }
        }
    }

    fun montar(): Relatorio = (original ?: Relatorio(numero = numero, clienteId = clienteId)).copy(
        numero = numero, clienteId = clienteId, equipamentoId = equipamentoId, ordemServicoId = ordemServicoId,
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

    val abas = listOf("Medições", "Diagnóstico IA", "Fotos", "Peças/Serviços", "Recomendações", "Manual", "Conclusão", "Assinaturas")

    TelaBase(nav, if (relatorioId > 0) numero else "Novo relatório") { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // ---- Cabeçalho fixo: número + cliente + equipamento ----
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
                    aoSelecionar = { texto ->
                        val e = equipsDoCliente.find { e -> "${e.tipo} ${e.marca}".trim() == texto }
                        equipamentoId = e?.id
                        if (e != null && e.fluido.isNotBlank()) fluido = e.fluido
                    }
                )
            }

            ScrollableTabRow(selectedTabIndex = aba, edgePadding = 0.dp) {
                abas.forEachIndexed { i, titulo ->
                    Tab(selected = aba == i, onClick = { aba = i }, text = { Text(titulo) })
                }
            }

            // ---- Conteúdo da aba selecionada (rolável) ----
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)
            ) {
                when (aba) {
                    0 -> {
                        TituloSecao("Motivo e diagnóstico")
                        CampoTexto(motivo, { motivo = it }, "Motivo da visita", linhas = 2)
                        CampoTexto(diagnostico, { diagnostico = it }, "Diagnóstico", linhas = 3)

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
                            CampoTexto(pSuccao, { pSuccao = it; recalcular() }, "Pressão sucção (bar)",
                                modifier = Modifier.weight(1f).padding(end = 4.dp),
                                teclado = KeyboardOptions(keyboardType = KeyboardType.Number))
                            CampoTexto(pDescarga, { pDescarga = it; recalcular() }, "Pressão descarga (bar)",
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
                    }
                    1 -> {
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
                    2 -> {
                        TituloSecao("Fotos antes")
                        SecaoFotos("Antes", fotosAntes) { fotosAntes = it }
                        TituloSecao("Fotos durante")
                        SecaoFotos("Durante", fotosDurante) { fotosDurante = it }
                        TituloSecao("Fotos depois")
                        SecaoFotos("Depois", fotosDepois) { fotosDepois = it }
                    }
                    3 -> {
                        TituloSecao("Peças e serviços")
                        CampoTexto(servicos, { servicos = it }, "Serviços realizados", linhas = 3)
                        CampoTexto(pecas, { pecas = it }, "Peças substituídas", linhas = 3)
                    }
                    4 -> {
                        TituloSecao("Recomendações técnicas")
                        CampoTexto(recomendacoes, { recomendacoes = it }, "Recomendações", linhas = 5)
                    }
                    5 -> {
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
                    6 -> {
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
            }

            // ---- Ações fixas no rodapé ----
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
