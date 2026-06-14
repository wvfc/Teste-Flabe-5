package br.com.refrigeracaopro.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import br.com.refrigeracaopro.ia.BaseTecnica
import br.com.refrigeracaopro.ia.OpenAiClient
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.util.Arquivos
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private data class Mensagem(val doUsuario: Boolean, val texto: String, val imagens: List<String> = emptyList())

/** Assistente IA técnico: chat com a OpenAI, com entrada de texto, imagens e arquivos. */
@Composable
fun AssistenteIAScreen(nav: NavController) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    val mensagens = remember { mutableStateListOf<Mensagem>() }
    var entrada by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Anexos pendentes (antes de enviar)
    val imagensAnexadas = remember { mutableStateListOf<String>() }
    val arquivosAnexados = remember { mutableStateListOf<Pair<String, String>>() } // (nome, conteúdo/nota)

    val disponivel = OpenAiClient.disponivel(context)

    val escolherImagens = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri -> Arquivos.copiarImagem(context, uri)?.let { imagensAnexadas.add(it) } }
    }
    var lendoArquivo by remember { mutableStateOf(false) }
    val escolherArquivo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) escopo.launch {
            lendoArquivo = true
            // Leitura/extração de texto (PDF pode ser pesado) fora da thread principal
            val anexo = withContext(Dispatchers.IO) { Arquivos.lerArquivoTexto(context, uri) }
            arquivosAnexados.add(anexo)
            lendoArquivo = false
        }
    }

    LaunchedEffect(mensagens.size) {
        if (mensagens.isNotEmpty()) listState.animateScrollToItem(mensagens.size - 1)
    }

    fun enviar() {
        val texto = entrada.trim()
        if ((texto.isBlank() && imagensAnexadas.isEmpty() && arquivosAnexados.isEmpty()) || carregando) return

        // Conteúdo dos arquivos de texto é embutido na mensagem enviada à IA
        val anexosTexto = if (arquivosAnexados.isEmpty()) "" else
            "\n\n--- Arquivos anexados ---\n" + arquivosAnexados.joinToString("\n\n") { "Arquivo: ${it.first}\n${it.second}" }
        val textoEnvio = (texto.ifBlank { "Analise o(s) anexo(s)." }) + anexosTexto

        val imagens = imagensAnexadas.toList()
        // Mensagem exibida no chat (com aviso de anexos)
        val rotuloAnexos = buildList {
            if (imagens.isNotEmpty()) add("${imagens.size} imagem(ns)")
            if (arquivosAnexados.isNotEmpty()) add("${arquivosAnexados.size} arquivo(s)")
        }.joinToString(" • ")
        val textoExibido = texto.ifBlank { "(anexos)" } + if (rotuloAnexos.isNotBlank()) "\n📎 $rotuloAnexos" else ""

        mensagens.add(Mensagem(true, textoExibido, imagens))
        entrada = ""
        imagensAnexadas.clear()
        arquivosAnexados.clear()
        carregando = true
        escopo.launch {
            // Histórico textual; a última mensagem do usuário leva o conteúdo de envio
            val historico = mensagens.mapIndexed { i, m ->
                val papel = if (m.doUsuario) "user" else "assistant"
                val conteudo = if (i == mensagens.lastIndex && m.doUsuario) textoEnvio else m.texto
                papel to conteudo
            }
            val instrucoes = "BASE TÉCNICA (consulte antes de responder):\n" +
                BaseTecnica.resumoPara(context, texto) + "\n\n" + BaseTecnica.FORMATO_DIAGNOSTICO
            when (val r = OpenAiClient.perguntar(context, historico, instrucoes, imagens)) {
                is OpenAiClient.Resultado.Sucesso -> mensagens.add(Mensagem(false, r.texto))
                is OpenAiClient.Resultado.Erro -> mensagens.add(Mensagem(false, "⚠️ ${r.mensagem}"))
            }
            carregando = false
        }
    }

    TelaBase(nav, "Assistente IA") { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!disponivel) {
                Card(
                    Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Assistente indisponível", fontWeight = FontWeight.Bold)
                        Text(
                            "Para usar a IA, cadastre sua chave da OpenAI em Configurações, " +
                                "ative o assistente e conecte-se à internet.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (mensagens.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Exemplos de perguntas:", fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary)
                    listOf(
                        "Compressor de câmara fria com R404A não atinge temperatura. Por onde começo?",
                        "Anexe a foto da plaqueta e pergunte: este compressor serve para R404A baixa temperatura?",
                        "Quais as causas de superaquecimento alto em um split R410A?",
                    ).forEach { exemplo ->
                        Card(
                            onClick = { entrada = exemplo },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text(exemplo, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp)) {
                items(mensagens) { msg -> BalaoMensagem(msg) }
                if (carregando) {
                    item {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                            Text("Analisando...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Pré-visualização dos anexos pendentes
            if (imagensAnexadas.isNotEmpty() || arquivosAnexados.isNotEmpty()) {
                LazyRow(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(imagensAnexadas.toList()) { caminho ->
                        Box {
                            AsyncImage(
                                model = File(caminho), contentDescription = null, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(56.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            )
                            IconButton(onClick = { imagensAnexadas.remove(caminho) }, modifier = Modifier.size(20.dp).align(Alignment.TopEnd)) {
                                Icon(Icons.Default.Close, "Remover", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    items(arquivosAnexados.toList()) { arq ->
                        AssistChip(onClick = { arquivosAnexados.remove(arq) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.InsertDriveFile, null, Modifier.size(16.dp)) },
                            label = { Text(arq.first.take(16)) })
                    }
                }
            }

            if (lendoArquivo) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                    Text("Lendo arquivo...", style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { escolherImagens.launch("image/*") }, enabled = disponivel) {
                    Icon(Icons.Default.AddPhotoAlternate, "Anexar imagem", tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = { escolherArquivo.launch("*/*") }, enabled = disponivel) {
                    Icon(Icons.Default.AttachFile, "Anexar arquivo", tint = MaterialTheme.colorScheme.secondary)
                }
                OutlinedTextField(
                    value = entrada,
                    onValueChange = { entrada = it },
                    placeholder = { Text("Pergunte ou anexe imagem/arquivo") },
                    modifier = Modifier.weight(1f),
                    enabled = disponivel,
                    maxLines = 4,
                )
                IconButton(onClick = { enviar() }, enabled = disponivel && !carregando) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Enviar", tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun BalaoMensagem(msg: Mensagem) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (msg.doUsuario) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (msg.doUsuario) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(Modifier.padding(12.dp)) {
                if (msg.imagens.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                        items(msg.imagens) { caminho ->
                            AsyncImage(model = File(caminho), contentDescription = null, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(72.dp).border(1.dp, Color.White, RoundedCornerShape(6.dp)))
                        }
                    }
                }
                Text(
                    msg.texto,
                    color = if (msg.doUsuario) Color.White else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
