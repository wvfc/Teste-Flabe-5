package br.com.refrigeracaopro.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import br.com.refrigeracaopro.util.Arquivos
import java.io.File

/** Campo de texto padrão dos formulários. */
@Composable
fun CampoTexto(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    linhas: Int = 1,
    teclado: androidx.compose.foundation.text.KeyboardOptions =
        androidx.compose.foundation.text.KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        modifier = modifier.padding(vertical = 4.dp),
        minLines = linhas,
        keyboardOptions = teclado,
    )
}

/** Dropdown de seleção simples. */
@Composable
fun SeletorOpcoes(
    rotulo: String,
    opcoes: List<String>,
    selecionado: String,
    aoSelecionar: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    var aberto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = aberto, onExpandedChange = { aberto = it }, modifier = modifier) {
        OutlinedTextField(
            value = selecionado,
            onValueChange = {},
            readOnly = true,
            label = { Text(rotulo) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = aberto) },
            modifier = Modifier
                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        )
        ExposedDropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            opcoes.forEach { opcao ->
                DropdownMenuItem(
                    text = { Text(opcao) },
                    onClick = { aoSelecionar(opcao); aberto = false }
                )
            }
        }
    }
}

/**
 * Seção de fotos (galeria) em que cada imagem tem um campo de observação.
 */
@Composable
fun SecaoFotosComObservacao(
    titulo: String,
    fotos: List<String>,
    observacaoDe: (String) -> String,
    aoMudarFotos: (List<String>) -> Unit,
    aoMudarObservacao: (String, String) -> Unit,
) {
    val context = LocalContext.current
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val novas = uris.mapNotNull { Arquivos.copiarImagem(context, it) }
        if (novas.isNotEmpty()) aoMudarFotos(fotos + novas)
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(titulo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { galeria.launch("image/*") }, modifier = Modifier.padding(top = 6.dp)) {
            Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Adicionar da galeria")
        }
        fotos.forEach { caminho ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Box {
                    AsyncImage(
                        model = File(caminho),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(84.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    )
                    IconButton(
                        onClick = { aoMudarFotos(fotos - caminho) },
                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                            .background(MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Close, "Remover", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = observacaoDe(caminho),
                    onValueChange = { aoMudarObservacao(caminho, it) },
                    label = { Text("Observação da imagem") },
                    modifier = Modifier.weight(1f),
                    minLines = 2,
                )
            }
        }
    }
}

/** Título de seção dos formulários. */
@Composable
fun TituloSecao(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

/**
 * Campo de marcação: um rótulo e uma linha de opções selecionáveis (chips).
 * Tocar na opção já marcada a desmarca. Usado nos checklists (Ok / Reparado /
 * Necessita reparo, ou Sim / Não).
 */
@Composable
fun CampoMarcacao(
    rotulo: String,
    opcoes: List<String>,
    selecionado: String,
    aoSelecionar: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (rotulo.isNotBlank()) Text(rotulo, style = MaterialTheme.typography.bodyMedium)
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            opcoes.forEach { opcao ->
                androidx.compose.material3.FilterChip(
                    selected = selecionado == opcao,
                    onClick = { aoSelecionar(if (selecionado == opcao) "" else opcao) },
                    label = { Text(opcao, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
    }
}

/** Diálogo de confirmação de exclusão. */
@Composable
fun ConfirmarExclusao(titulo: String, aoConfirmar: () -> Unit, aoCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Excluir") },
        text = { Text(titulo) },
        confirmButton = {
            TextButton(onClick = aoConfirmar) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } }
    )
}

/**
 * Seção de fotos: miniaturas + botões de galeria e câmera.
 * As imagens são copiadas para o armazenamento interno (funciona offline).
 */
@Composable
fun SecaoFotos(
    titulo: String,
    fotos: List<String>,
    aoMudar: (List<String>) -> Unit,
) {
    val context = LocalContext.current

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        // Fotos da galeria: copiadas e comprimidas para o armazenamento interno
        val novas = uris.mapNotNull { Arquivos.copiarImagem(context, it) }
        if (novas.isNotEmpty()) aoMudar(fotos + novas)
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(titulo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(fotos) { caminho ->
                Box {
                    AsyncImage(
                        model = File(caminho),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(84.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    )
                    IconButton(
                        onClick = { aoMudar(fotos - caminho) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Close, "Remover", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            OutlinedButton(onClick = { galeria.launch("image/*") }) {
                Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Adicionar da galeria")
            }
        }
    }
}

/**
 * Diálogo de assinatura na tela: o usuário desenha com o dedo e a assinatura
 * é salva como PNG no armazenamento interno.
 */
@Composable
fun AssinaturaDialog(
    titulo: String,
    aoSalvar: (String) -> Unit,
    aoFechar: () -> Unit,
) {
    val context = LocalContext.current
    // Cada traço é uma lista de pontos
    val tracos = remember { mutableStateOf(listOf<List<Offset>>()) }
    var tracoAtual by remember { mutableStateOf(listOf<Offset>()) }
    var tamanho by remember { mutableStateOf(IntSize(0, 0)) }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(titulo) },
        text = {
            Column {
                Text("Assine no quadro abaixo:", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        .onSizeChanged { tamanho = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset -> tracoAtual = listOf(offset) },
                                onDrag = { change, _ -> tracoAtual = tracoAtual + change.position },
                                onDragEnd = {
                                    tracos.value = tracos.value + listOf(tracoAtual)
                                    tracoAtual = emptyList()
                                }
                            )
                        }
                ) {
                    val todos = tracos.value + listOf(tracoAtual)
                    todos.forEach { pontos ->
                        if (pontos.size > 1) {
                            val path = Path().apply {
                                moveTo(pontos.first().x, pontos.first().y)
                                pontos.drop(1).forEach { lineTo(it.x, it.y) }
                            }
                            drawPath(path, Color.Black, style = Stroke(width = 4f))
                        }
                    }
                }
                TextButton(onClick = { tracos.value = emptyList(); tracoAtual = emptyList() }) {
                    Text("Limpar")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (tracos.value.isNotEmpty() && tamanho.width > 0) {
                        // Renderiza os traços em um Bitmap e salva como PNG
                        val bitmap = Bitmap.createBitmap(tamanho.width, tamanho.height, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        val paint = Paint().apply {
                            color = android.graphics.Color.BLACK
                            strokeWidth = 4f
                            style = Paint.Style.STROKE
                            isAntiAlias = true
                            strokeJoin = Paint.Join.ROUND
                            strokeCap = Paint.Cap.ROUND
                        }
                        tracos.value.forEach { pontos ->
                            if (pontos.size > 1) {
                                val path = android.graphics.Path().apply {
                                    moveTo(pontos.first().x, pontos.first().y)
                                    pontos.drop(1).forEach { lineTo(it.x, it.y) }
                                }
                                canvas.drawPath(path, paint)
                            }
                        }
                        aoSalvar(Arquivos.salvarBitmap(context, bitmap, "assinatura"))
                    }
                    aoFechar()
                }
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}

/** Linha de assinatura no formulário (mostra miniatura ou botão para assinar). */
@Composable
fun LinhaAssinatura(rotulo: String, caminho: String, aoAssinar: () -> Unit, aoLimpar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(rotulo, Modifier.weight(1f))
        if (caminho.isNotBlank() && File(caminho).exists()) {
            AsyncImage(
                model = File(caminho),
                contentDescription = null,
                modifier = Modifier
                    .height(40.dp)
                    .width(110.dp)
                    .background(Color.White)
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .graphicsLayer { },
            )
            TextButton(onClick = aoLimpar) { Text("Limpar") }
        } else {
            OutlinedButton(onClick = aoAssinar) { Text("Assinar") }
        }
    }
}
