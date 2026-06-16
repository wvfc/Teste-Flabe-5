package br.com.refrigeracaopro.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.refrigeracaopro.data.CalculosProjeto
import br.com.refrigeracaopro.data.CatalogoComponentes
import br.com.refrigeracaopro.data.CompIso
import br.com.refrigeracaopro.data.ConexaoIso
import br.com.refrigeracaopro.data.EstadoProjeto
import br.com.refrigeracaopro.data.FluidosLinha
import br.com.refrigeracaopro.data.Projeto
import br.com.refrigeracaopro.data.ProjetoJson
import br.com.refrigeracaopro.ia.OpenAiClient
import br.com.refrigeracaopro.pdf.PdfGenerator
import br.com.refrigeracaopro.projeto.RenderizadorProjeto
import br.com.refrigeracaopro.ui.components.CampoTexto
import br.com.refrigeracaopro.ui.components.SeletorOpcoes
import br.com.refrigeracaopro.util.Arquivos
import br.com.refrigeracaopro.viewmodel.ProjetosViewModel
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorIsoScreen(nav: NavController, projetoId: Long, vm: ProjetosViewModel = viewModel()) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    val clientes by vm.clientes.collectAsState()

    var projeto by remember { mutableStateOf<Projeto?>(null) }
    var estado by remember { mutableStateOf(EstadoProjeto()) }
    val desfazer = remember { ArrayDeque<EstadoProjeto>() }
    val refazer = remember { ArrayDeque<EstadoProjeto>() }

    var escala by remember { mutableStateOf(1.2f) }
    var offX by remember { mutableStateOf(0f) }
    var offY by remember { mutableStateOf(0f) }
    var grade by remember { mutableStateOf(true) }
    var selecionado by remember { mutableStateOf<Long?>(null) }
    var tamanho by remember { mutableStateOf(IntSize(1, 1)) }
    var modoConexao by remember { mutableStateOf(false) }
    var fluidoAtual by remember { mutableStateOf(FluidosLinha.AR_COMPRIMIDO) }
    var modo3d by remember { mutableStateOf(false) }
    var yaw by remember { mutableStateOf(45f) }
    var pitch by remember { mutableStateOf(30f) }

    var mostrarBiblioteca by remember { mutableStateOf(false) }
    var mostrarCalculos by remember { mutableStateOf(false) }
    var editarComp by remember { mutableStateOf<CompIso?>(null) }
    var menuExport by remember { mutableStateOf(false) }
    var menuCamera by remember { mutableStateOf(false) }
    var respostaIA by remember { mutableStateOf<String?>(null) }
    var carregandoIA by remember { mutableStateOf(false) }

    LaunchedEffect(projetoId) {
        vm.buscar(projetoId)?.let { p ->
            projeto = p
            estado = ProjetoJson.desserializar(p.estado)
            offX = tamanho.width / 2f; offY = tamanho.height / 2f
        }
    }

    fun salvarAuto() {
        val p = projeto ?: return
        vm.salvar(p.copy(estado = ProjetoJson.serializar(estado)))
    }

    // Aplica uma mudança com suporte a desfazer + autosave
    fun aplicar(novo: EstadoProjeto) {
        desfazer.addLast(estado)
        refazer.clear()
        estado = novo
        salvarAuto()
    }

    fun proximoId(): Long = (estado.componentes.maxOfOrNull { it.id } ?: 0L) + 1L

    fun centroMundoX() = (tamanho.width / 2f - offX) / escala
    fun centroMundoY() = (tamanho.height / 2f - offY) / escala

    fun adicionar(categoria: String, tipo: String) {
        val novo = CompIso(proximoId(), categoria, tipo, centroMundoX(), centroMundoY(), nome = tipo)
        aplicar(estado.copy(componentes = estado.componentes + novo))
        selecionado = novo.id
    }

    fun atualizarComp(c: CompIso) {
        aplicar(estado.copy(componentes = estado.componentes.map { if (it.id == c.id) c else it }))
    }

    fun excluirSelecionado() {
        val id = selecionado ?: return
        aplicar(
            estado.copy(
                componentes = estado.componentes.filter { it.id != id },
                conexoes = estado.conexoes.filter { it.deId != id && it.paraId != id },
            )
        )
        selecionado = null
    }

    fun duplicarSelecionado() {
        val c = estado.componentes.find { it.id == selecionado } ?: return
        val novo = c.copy(id = proximoId(), x = c.x + 30f, y = c.y + 30f)
        aplicar(estado.copy(componentes = estado.componentes + novo))
        selecionado = novo.id
    }

    fun rotacionarSelecionado() {
        val c = estado.componentes.find { it.id == selecionado } ?: return
        atualizarComp(c.copy(rotacao = (c.rotacao + 45f) % 360f))
    }

    fun elevarSelecionado(d: Float) {
        val c = estado.componentes.find { it.id == selecionado } ?: return
        atualizarComp(c.copy(z = (c.z + d).coerceIn(-200f, 400f)))
    }

    fun conectar(aId: Long, bId: Long) {
        if (aId == bId) return
        if (estado.conexoes.any { (it.deId == aId && it.paraId == bId) || (it.deId == bId && it.paraId == aId) }) return
        val nova = ConexaoIso((estado.conexoes.maxOfOrNull { it.id } ?: 0L) + 1L, aId, bId, fluidoAtual)
        aplicar(estado.copy(conexoes = estado.conexoes + nova))
    }

    fun desconectarSelecionado() {
        val id = selecionado ?: return
        aplicar(estado.copy(conexoes = estado.conexoes.filter { it.deId != id && it.paraId != id }))
    }

    // Hit-test em coordenadas de tela
    fun acharComponente(px: Float, py: Float): CompIso? {
        val hw = RenderizadorProjeto.COMP_W * escala / 2
        val hh = RenderizadorProjeto.COMP_H * escala / 2
        return estado.componentes.lastOrNull { c ->
            val cx = c.x * escala + offX; val cy = c.y * escala + offY
            abs(px - cx) <= hw && abs(py - cy) <= hh
        }
    }

    // Conexão automática por proximidade ao soltar um componente
    fun encaixarPorProximidade(id: Long) {
        val c = estado.componentes.find { it.id == id } ?: return
        val alvo = estado.componentes.filter { it.id != id }.minByOrNull {
            val dx = it.x - c.x; val dy = it.y - c.y; dx * dx + dy * dy
        } ?: return
        val dist = kotlin.math.hypot((alvo.x - c.x).toDouble(), (alvo.y - c.y).toDouble())
        if (dist <= RenderizadorProjeto.COMP_W * 1.2) conectar(id, alvo.id)
    }

    val bitmap = remember(estado, escala, offX, offY, grade, selecionado, tamanho, modo3d, yaw, pitch) {
        if (modo3d)
            RenderizadorProjeto.render3d(estado, tamanho.width, tamanho.height, escala, offX, offY, grade, yaw, pitch).asImageBitmap()
        else
            RenderizadorProjeto.render(estado, tamanho.width, tamanho.height, escala, offX, offY, grade, selecionado).asImageBitmap()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(projeto?.nome ?: "CAD 3D", fontWeight = FontWeight.Bold, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { salvarAuto(); nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { if (desfazer.isNotEmpty()) { refazer.addLast(estado); estado = desfazer.removeLast(); salvarAuto() } }) {
                        Icon(Icons.AutoMirrored.Filled.Undo, "Desfazer", tint = Color.White)
                    }
                    IconButton(onClick = { if (refazer.isNotEmpty()) { desfazer.addLast(estado); estado = refazer.removeLast(); salvarAuto() } }) {
                        Icon(Icons.AutoMirrored.Filled.Redo, "Refazer", tint = Color.White)
                    }
                    if (modo3d) {
                        IconButton(onClick = { menuCamera = true }) {
                            Icon(Icons.Default.ScreenRotation, "Câmera", tint = Color.White)
                        }
                        DropdownMenu(expanded = menuCamera, onDismissRequest = { menuCamera = false }) {
                            Text("Câmera", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                            RenderizadorProjeto.PRESETS.forEach { (nome, cam) ->
                                DropdownMenuItem(text = { Text(nome) }, onClick = { yaw = cam.yaw; pitch = cam.pitch; menuCamera = false })
                            }
                        }
                    }
                    IconButton(onClick = { modo3d = !modo3d; selecionado = null; modoConexao = false }) {
                        Icon(Icons.Default.ViewInAr, if (modo3d) "Ver em 2D" else "Ver em 3D",
                            tint = if (modo3d) Color(0xFF7BD3A8) else Color.White)
                    }
                    IconButton(onClick = { grade = !grade }) { Icon(Icons.Default.GridOn, "Grade", tint = Color.White) }
                    IconButton(onClick = { menuExport = true }) { Icon(Icons.Default.MoreVert, "Mais", tint = Color.White) }
                    DropdownMenu(expanded = menuExport, onDismissRequest = { menuExport = false }) {
                        DropdownMenuItem(text = { Text("Dados do projeto") }, onClick = { menuExport = false; nav.navigate("projetos/cadastro?id=$projetoId") })
                        DropdownMenuItem(text = { Text("Exportar PNG (${if (modo3d) "3D" else "2D"})") }, onClick = { menuExport = false; exportarPng(context, estado, projeto, modo3d, yaw, pitch) })
                        DropdownMenuItem(text = { Text("Exportar PDF (${if (modo3d) "3D" else "2D"})") }, onClick = {
                            menuExport = false
                            escopo.launch { exportarPdf(context, estado, projeto, clientes, modo3d, yaw, pitch) }
                        })
                        DropdownMenuItem(text = { Text("Exportar CSV (materiais)") }, onClick = { menuExport = false; exportarCsv(context, estado, projeto ?: Projeto()) })
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White,
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Área de desenho
            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .onSizeChanged {
                        if (tamanho.width <= 1) { offX = it.width / 2f; offY = it.height / 2f }
                        tamanho = it
                    }
                    .pointerInput(estado, escala, offX, offY, modoConexao, modo3d) {
                        detectTapGestures { tap ->
                            if (modo3d) return@detectTapGestures
                            val c = acharComponente(tap.x, tap.y)
                            if (modoConexao && c != null && selecionado != null && c.id != selecionado) {
                                conectar(selecionado!!, c.id); modoConexao = false
                            } else {
                                selecionado = c?.id
                            }
                        }
                    }
                    .pointerInput(escala, modo3d) {
                        var arrastandoId: Long? = null
                        var preDrag: EstadoProjeto? = null
                        detectDragGestures(
                            onDragStart = { pos ->
                                val c = if (modo3d) null else acharComponente(pos.x, pos.y)
                                arrastandoId = c?.id
                                if (c != null) { selecionado = c.id; preDrag = estado }
                            },
                            onDrag = { change, delta ->
                                change.consume()
                                val id = arrastandoId
                                if (id != null) {
                                    estado = estado.copy(componentes = estado.componentes.map {
                                        if (it.id == id) it.copy(x = it.x + delta.x / escala, y = it.y + delta.y / escala) else it
                                    })
                                } else if (modo3d) {
                                    // Órbita livre da câmera 3D
                                    yaw = (yaw + delta.x * 0.4f).let { ((it % 360f) + 360f) % 360f }
                                    pitch = (pitch - delta.y * 0.4f).coerceIn(2f, 89f)
                                } else {
                                    offX += delta.x; offY += delta.y
                                }
                            },
                            onDragEnd = {
                                val id = arrastandoId
                                if (id != null) {
                                    val anterior = preDrag
                                    if (anterior != null) { desfazer.addLast(anterior); refazer.clear() }
                                    encaixarPorProximidade(id)
                                    salvarAuto()
                                }
                                arrastandoId = null; preDrag = null
                            }
                        )
                    }
            ) {
                Image(bitmap = bitmap, contentDescription = "Desenho do projeto", modifier = Modifier.fillMaxSize())

                // Controles de zoom
                Column(Modifier.padding(8.dp)) {
                    IconButton(onClick = { escala = (escala * 1.2f).coerceAtMost(6f) }) { Icon(Icons.Default.ZoomIn, "Mais zoom") }
                    IconButton(onClick = { escala = (escala / 1.2f).coerceAtLeast(0.2f) }) { Icon(Icons.Default.ZoomOut, "Menos zoom") }
                }
            }

            // Barra do componente selecionado (apenas na edição 2D)
            if (selecionado != null && !modo3d) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    BotaoBarra(Icons.Default.ArrowUpward, "Subir") { elevarSelecionado(20f) }
                    BotaoBarra(Icons.Default.ArrowDownward, "Descer") { elevarSelecionado(-20f) }
                    BotaoBarra(Icons.Default.Rotate90DegreesCw, "Girar") { rotacionarSelecionado() }
                    BotaoBarra(Icons.Default.ContentCopy, "Duplicar") { duplicarSelecionado() }
                    BotaoBarra(Icons.Default.Link, if (modoConexao) "Destino" else "Conectar") { modoConexao = !modoConexao }
                    BotaoBarra(Icons.Default.Tune, "Editar") { editarComp = estado.componentes.find { it.id == selecionado } }
                    BotaoBarra(Icons.Default.Delete, "Excluir") { excluirSelecionado() }
                }
            }

            // Barra inferior: fluido + adicionar + cálculos + IA
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    SeletorOpcoes("Linha/fluido", FluidosLinha.TODOS, fluidoAtual, { fluidoAtual = it })
                }
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = { mostrarCalculos = true }) { Icon(Icons.Default.Calculate, "Cálculos", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = {
                    if (!OpenAiClient.disponivel(context)) { Toast.makeText(context, "Configure a chave OpenAI.", Toast.LENGTH_LONG).show(); return@IconButton }
                    carregandoIA = true
                    escopo.launch {
                        val calc = CalculosProjeto.calcular(estado, projeto ?: Projeto())
                        val ctx = "Projeto de tubulação. Fluido: ${projeto?.fluido}; pressão: ${projeto?.pressaoTrabalho}; vazão: ${projeto?.vazao}.\n" +
                            CalculosProjeto.resumo(calc)
                        val r = OpenAiClient.perguntar(context, listOf("user" to
                            "Analise este projeto de tubulação e sugira melhor diâmetro, melhor rota, redução de perdas, economia de material, reposicionamento de válvulas e melhor distribuição:\n\n$ctx"))
                        respostaIA = when (r) { is OpenAiClient.Resultado.Sucesso -> r.texto; is OpenAiClient.Resultado.Erro -> r.mensagem }
                        carregandoIA = false
                    }
                }) { Icon(Icons.Default.AutoAwesome, "Otimizar com IA", tint = MaterialTheme.colorScheme.secondary) }
                FilledTonalButton(onClick = { mostrarBiblioteca = true }) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Componente")
                }
            }
        }
    }

    // Biblioteca de componentes
    if (mostrarBiblioteca) {
        ModalBottomSheet(onDismissRequest = { mostrarBiblioteca = false }, sheetState = rememberModalBottomSheetState()) {
            Column(Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("Biblioteca de componentes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                CatalogoComponentes.CATEGORIAS.forEach { (categoria, tipos) ->
                    Text(categoria, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tipos.forEach { tipo ->
                            AssistChip(onClick = { adicionar(categoria, tipo); mostrarBiblioteca = false },
                                label = { Text(tipo, style = MaterialTheme.typography.labelMedium) })
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Painel de cálculos + materiais
    if (mostrarCalculos) {
        val calc = remember(estado) { CalculosProjeto.calcular(estado, projeto ?: Projeto()) }
        ModalBottomSheet(onDismissRequest = { mostrarCalculos = false }, sheetState = rememberModalBottomSheetState()) {
            Column(Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("Inteligência do projeto", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(CalculosProjeto.resumo(calc), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                Text("Lista de materiais", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
                calc.materiais.forEach { m ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text("${m.quantidade}×", fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                        Text(m.descricao, Modifier.weight(1f))
                        Text(m.detalhe, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("Valores aproximados — auxiliares ao dimensionamento.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Propriedades do componente
    editarComp?.let { comp ->
        PropriedadesDialog(comp, aoSalvar = { atualizarComp(it); editarComp = null }, aoFechar = { editarComp = null })
    }

    // Resposta da IA
    respostaIA?.let { texto ->
        AlertDialog(
            onDismissRequest = { respostaIA = null },
            confirmButton = { TextButton(onClick = { respostaIA = null }) { Text("Fechar") } },
            title = { Text("Otimização sugerida") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(if (carregandoIA) "Analisando..." else texto) } }
        )
    }
}

@Composable
private fun BotaoBarra(icone: androidx.compose.ui.graphics.vector.ImageVector, rotulo: String, onClick: () -> Unit) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) { Icon(icone, rotulo, tint = MaterialTheme.colorScheme.primary) }
        Text(rotulo, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun PropriedadesDialog(comp: CompIso, aoSalvar: (CompIso) -> Unit, aoFechar: () -> Unit) {
    var nome by remember { mutableStateOf(comp.nome) }
    var codigo by remember { mutableStateOf(comp.codigo) }
    var fabricante by remember { mutableStateOf(comp.fabricante) }
    var modelo by remember { mutableStateOf(comp.modelo) }
    var diametro by remember { mutableStateOf(comp.diametro) }
    var material by remember { mutableStateOf(comp.material) }
    var comprimento by remember { mutableStateOf(comp.comprimento) }
    var peso by remember { mutableStateOf(comp.peso) }
    var pressaoMax by remember { mutableStateOf(comp.pressaoMax) }
    var tempMax by remember { mutableStateOf(comp.tempMax) }
    var etiqueta by remember { mutableStateOf(comp.etiqueta) }
    var elev by remember { mutableStateOf(comp.z.toInt().toString()) }
    var obs by remember { mutableStateOf(comp.observacoes) }

    AlertDialog(
        onDismissRequest = aoFechar,
        confirmButton = {
            TextButton(onClick = {
                aoSalvar(comp.copy(nome = nome, codigo = codigo, fabricante = fabricante, modelo = modelo,
                    diametro = diametro, material = material, comprimento = comprimento, peso = peso,
                    pressaoMax = pressaoMax, tempMax = tempMax, etiqueta = etiqueta, observacoes = obs,
                    z = elev.replace(",", ".").toFloatOrNull() ?: comp.z))
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
        title = { Text(comp.tipo) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                CampoTexto(nome, { nome = it }, "Nome")
                CampoTexto(etiqueta, { etiqueta = it }, "Etiqueta")
                CampoTexto(elev, { elev = it }, "Elevação / nível")
                CampoTexto(codigo, { codigo = it }, "Código")
                CampoTexto(fabricante, { fabricante = it }, "Fabricante")
                CampoTexto(modelo, { modelo = it }, "Modelo")
                CampoTexto(diametro, { diametro = it }, "Diâmetro (mm)")
                CampoTexto(material, { material = it }, "Material")
                CampoTexto(comprimento, { comprimento = it }, "Comprimento (m)")
                CampoTexto(peso, { peso = it }, "Peso")
                CampoTexto(pressaoMax, { pressaoMax = it }, "Pressão máxima")
                CampoTexto(tempMax, { tempMax = it }, "Temperatura máxima")
                CampoTexto(obs, { obs = it }, "Observações", linhas = 2)
            }
        }
    )
}

// ---------- Exportações ----------

private fun renderExport(estado: EstadoProjeto, modo3d: Boolean, yaw: Float, pitch: Float): android.graphics.Bitmap {
    val w = 1600; val h = 1100
    return if (modo3d) {
        val e = RenderizadorProjeto.enquadrar3d(estado, w, h, yaw, pitch)
        RenderizadorProjeto.render3d(estado, w, h, e, 0f, 0f, true, yaw, pitch)
    } else {
        val v = RenderizadorProjeto.enquadrar(estado, w, h)
        RenderizadorProjeto.render(estado, w, h, v.escala, v.offX, v.offY, true, null)
    }
}

private fun exportarPng(context: android.content.Context, estado: EstadoProjeto, projeto: Projeto?, modo3d: Boolean, yaw: Float, pitch: Float) {
    val bmp = renderExport(estado, modo3d, yaw, pitch)
    val caminho = Arquivos.salvarBitmap(context, bmp, "projeto")
    Arquivos.compartilhar(context, File(caminho), "image/png", projeto?.nome ?: "Projeto")
}

private suspend fun exportarPdf(
    context: android.content.Context, estado: EstadoProjeto, projeto: Projeto?,
    clientes: List<br.com.refrigeracaopro.data.Cliente>, modo3d: Boolean, yaw: Float, pitch: Float,
) {
    val p = projeto ?: return
    val bmp = renderExport(estado, modo3d, yaw, pitch)
    val calc = CalculosProjeto.calcular(estado, p)
    val cliente = clientes.find { it.id == p.clienteId }
    val pdf = PdfGenerator.gerarProjetoIso(context, p, cliente, bmp, calc)
    Arquivos.compartilhar(context, pdf, "application/pdf", p.nome)
}

private fun exportarCsv(context: android.content.Context, estado: EstadoProjeto, projeto: Projeto) {
    val calc = CalculosProjeto.calcular(estado, projeto)
    val csv = buildString {
        appendLine("Quantidade,Descrição,Detalhe")
        calc.materiais.forEach { appendLine("${it.quantidade},\"${it.descricao}\",\"${it.detalhe}\"") }
    }
    val arquivo = File(Arquivos.pastaPdfs(context), "materiais.csv")
    arquivo.writeText(csv)
    Arquivos.compartilhar(context, arquivo, "text/csv", "Lista de materiais")
}
