package br.com.refrigeracaopro.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.refrigeracaopro.ui.theme.AzulEscuro
import br.com.refrigeracaopro.ui.theme.AzulMedio
import br.com.refrigeracaopro.ui.theme.Verde
import br.com.refrigeracaopro.viewmodel.LoginViewModel
import kotlinx.coroutines.launch
import android.widget.Toast

/**
 * Tela de login. O acesso principal é pela conta Google (que também concede o
 * escopo de backup no Drive). Há ainda um login local (offline) opcional, com
 * criação de administrador no primeiro acesso e recuperação de senha.
 */
@Suppress("DEPRECATION")
@Composable
fun LoginScreen(aoEntrar: () -> Unit, vm: LoginViewModel = viewModel()) {
    val context = LocalContext.current
    val temUsuario by vm.temUsuario.collectAsState()
    val logado by vm.usuarioLogado.collectAsState()
    val erro by vm.erro.collectAsState()

    if (logado != null) {
        aoEntrar()
        return
    }

    var usuario by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var nome by remember { mutableStateOf("") }
    var pergunta by remember { mutableStateOf("") }
    var resposta by remember { mutableStateOf("") }
    var mostrarRecuperacao by remember { mutableStateOf(false) }
    var mostrarLocal by remember { mutableStateOf(false) }

    // Login direto pela conta Google (também concede o escopo de backup no Drive)
    val loginGoogle = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn
            .getSignedInAccountFromIntent(resultado.data)
        try {
            task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            aoEntrar()
        } catch (e: com.google.android.gms.common.api.ApiException) {
            Toast.makeText(context, "Não foi possível entrar com o Google (código ${e.statusCode}).", Toast.LENGTH_LONG).show()
        }
    }
    fun clienteGoogle() = com.google.android.gms.auth.api.signin.GoogleSignIn
        .getClient(context, br.com.refrigeracaopro.util.DriveBackup.opcoesLogin())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AzulEscuro, AzulMedio)))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.AcUnit, null, tint = Verde, modifier = Modifier.size(72.dp))
        Text(
            "Refrigeração Pro",
            style = MaterialTheme.typography.headlineMedium,
            color = androidx.compose.ui.graphics.Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Assistência técnica em refrigeração",
            style = MaterialTheme.typography.bodyMedium,
            color = androidx.compose.ui.graphics.Color(0xFFB8C7DB),
        )
        Spacer(Modifier.height(24.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                // ----- Login principal: conta Google -----
                Text("Entrar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { loginGoogle.launch(clienteGoogle().signInIntent) },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Entrar com Google")
                }
                TextButton(
                    onClick = { mostrarLocal = !mostrarLocal },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) { Text(if (mostrarLocal) "Ocultar login local" else "Entrar com usuário local (offline)") }

                if (!mostrarLocal) return@Column

                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                if (temUsuario == false) {
                    // ----- Primeiro acesso: criar administrador -----
                    Text("Primeiro acesso", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Crie o usuário administrador.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(nome, { nome = it }, label = { Text("Seu nome") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(usuario, { usuario = it }, label = { Text("Usuário") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        senha, { senha = it }, label = { Text("Senha") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        pergunta, { pergunta = it },
                        label = { Text("Pergunta de segurança (para recuperar senha)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(resposta, { resposta = it }, label = { Text("Resposta") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { vm.criarAdmin(nome, usuario, senha, pergunta, resposta) },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Text("Criar e entrar") }
                } else {
                    // ----- Login local (offline) -----
                    Text("Login local", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(usuario, { usuario = it }, label = { Text("Usuário") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        senha, { senha = it }, label = { Text("Senha") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (erro.isNotBlank()) {
                        Text(erro, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { vm.entrar(usuario, senha) },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Text("Entrar") }
                    TextButton(onClick = { mostrarRecuperacao = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Esqueci minha senha")
                    }
                }
            }
        }
    }

    if (mostrarRecuperacao) {
        DialogoRecuperacao(
            vm = vm,
            aoFechar = { mostrarRecuperacao = false },
            aoMensagem = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        )
    }
}

@Composable
private fun DialogoRecuperacao(vm: LoginViewModel, aoFechar: () -> Unit, aoMensagem: (String) -> Unit) {
    val escopo = rememberCoroutineScope()
    var usuario by remember { mutableStateOf("") }
    var perguntaExibida by remember { mutableStateOf<String?>(null) }
    var resposta by remember { mutableStateOf("") }
    var novaSenha by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Redefinir senha") },
        text = {
            Column {
                OutlinedTextField(usuario, { usuario = it }, label = { Text("Usuário") }, modifier = Modifier.fillMaxWidth())
                if (perguntaExibida == null) {
                    TextButton(onClick = {
                        escopo.launch {
                            val p = vm.perguntaDe(usuario)
                            if (p == null) aoMensagem("Usuário não encontrado ou sem pergunta de segurança.")
                            else perguntaExibida = p
                        }
                    }) { Text("Buscar pergunta de segurança") }
                } else {
                    Text("Pergunta: $perguntaExibida", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(resposta, { resposta = it }, label = { Text("Resposta") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        novaSenha, { novaSenha = it }, label = { Text("Nova senha") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    vm.redefinirSenha(usuario, resposta, novaSenha) { ok, msg ->
                        aoMensagem(msg)
                        if (ok) aoFechar()
                    }
                },
                enabled = perguntaExibida != null
            ) { Text("Redefinir") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}
