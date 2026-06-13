package br.com.refrigeracaopro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import br.com.refrigeracaopro.ia.OpenAiClient
import br.com.refrigeracaopro.ui.components.TelaBase
import kotlinx.coroutines.launch

private data class Mensagem(val doUsuario: Boolean, val texto: String)

/** Assistente IA técnico: chat com a OpenAI, contextualizado em refrigeração. */
@Composable
fun AssistenteIAScreen(nav: NavController) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    val mensagens = remember { mutableStateListOf<Mensagem>() }
    var entrada by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val disponivel = OpenAiClient.disponivel(context)

    LaunchedEffect(mensagens.size) {
        if (mensagens.isNotEmpty()) listState.animateScrollToItem(mensagens.size - 1)
    }

    fun enviar() {
        val texto = entrada.trim()
        if (texto.isBlank() || carregando) return
        mensagens.add(Mensagem(true, texto))
        entrada = ""
        carregando = true
        escopo.launch {
            // Monta o histórico no formato esperado pela API
            val historico = mensagens.map { (if (it.doUsuario) "user" else "assistant") to it.texto }
            when (val r = OpenAiClient.perguntar(context, historico)) {
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
                        "Quais as causas de superaquecimento alto em um split R410A?",
                        "Como fazer vácuo e carga correta em um sistema R134a?",
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

            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = entrada,
                    onValueChange = { entrada = it },
                    placeholder = { Text("Digite sua pergunta técnica") },
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
            Text(
                msg.texto,
                Modifier.padding(12.dp),
                color = if (msg.doUsuario) androidx.compose.ui.graphics.Color.White
                else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
