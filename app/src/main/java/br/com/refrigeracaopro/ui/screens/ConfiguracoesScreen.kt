package br.com.refrigeracaopro.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import br.com.refrigeracaopro.data.Prefs
import br.com.refrigeracaopro.data.Prefs.chaveOpenAi
import br.com.refrigeracaopro.data.Prefs.cnpjEmpresa
import br.com.refrigeracaopro.data.Prefs.emailEmpresa
import br.com.refrigeracaopro.data.Prefs.enderecoEmpresa
import br.com.refrigeracaopro.data.Prefs.iaAtiva
import br.com.refrigeracaopro.data.Prefs.logoEmpresa
import br.com.refrigeracaopro.data.Prefs.modeloIa
import br.com.refrigeracaopro.data.Prefs.nomeEmpresa
import br.com.refrigeracaopro.data.Prefs.nomeTecnico
import br.com.refrigeracaopro.data.Prefs.registroTecnico
import br.com.refrigeracaopro.data.Prefs.telefoneEmpresa
import br.com.refrigeracaopro.ia.OpenAiClient
import br.com.refrigeracaopro.ui.components.SeletorOpcoes
import br.com.refrigeracaopro.ui.components.TelaBase
import br.com.refrigeracaopro.ui.components.TituloSecao
import br.com.refrigeracaopro.util.Arquivos
import br.com.refrigeracaopro.util.Backup
import java.io.File
import kotlinx.coroutines.launch

/**
 * Configurações: dados da empresa/técnico, logo, chave OpenAI (criptografada),
 * modelo de IA, teste de conexão e backup/restauração do banco.
 */
@Composable
fun ConfiguracoesScreen(nav: NavController) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()

    var nomeEmp by remember { mutableStateOf(context.nomeEmpresa) }
    var cnpj by remember { mutableStateOf(context.cnpjEmpresa) }
    var telefone by remember { mutableStateOf(context.telefoneEmpresa) }
    var email by remember { mutableStateOf(context.emailEmpresa) }
    var endereco by remember { mutableStateOf(context.enderecoEmpresa) }
    var logo by remember { mutableStateOf(context.logoEmpresa) }
    var tecnico by remember { mutableStateOf(context.nomeTecnico) }
    var registro by remember { mutableStateOf(context.registroTecnico) }
    var chave by remember { mutableStateOf(context.chaveOpenAi) }
    var modelo by remember { mutableStateOf(context.modeloIa) }
    var iaLigada by remember { mutableStateOf(context.iaAtiva) }
    var mostrarChave by remember { mutableStateOf(false) }
    var testando by remember { mutableStateOf(false) }

    val escolherLogo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { Arquivos.copiarImagem(context, it)?.let { caminho -> logo = caminho } }
    }
    val exportarBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let {
            val ok = Backup.exportar(context, it)
            Toast.makeText(context, if (ok) "Backup exportado." else "Falha ao exportar.", Toast.LENGTH_LONG).show()
        }
    }
    val importarBackup = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val ok = Backup.importar(context, it)
            Toast.makeText(
                context,
                if (ok) "Backup restaurado. Reinicie o app." else "Falha ao restaurar.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun salvarTudo() {
        with(Prefs) {
            context.nomeEmpresa = nomeEmp; context.cnpjEmpresa = cnpj; context.telefoneEmpresa = telefone
            context.emailEmpresa = email; context.enderecoEmpresa = endereco; context.logoEmpresa = logo
            context.nomeTecnico = tecnico; context.registroTecnico = registro
            context.chaveOpenAi = chave.trim(); context.modeloIa = modelo; context.iaAtiva = iaLigada
        }
    }

    TelaBase(nav, "Configurações") { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            TituloSecao("Dados da empresa")
            Campo(nomeEmp, { nomeEmp = it }, "Nome da empresa")
            Campo(cnpj, { cnpj = it }, "CNPJ")
            Campo(telefone, { telefone = it }, "Telefone")
            Campo(email, { email = it }, "E-mail")
            Campo(endereco, { endereco = it }, "Endereço")

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                if (logo.isNotBlank() && File(logo).exists()) {
                    AsyncImage(model = File(logo), contentDescription = "Logo", modifier = Modifier.size(64.dp))
                    Spacer(Modifier.width(12.dp))
                }
                OutlinedButton(onClick = { escolherLogo.launch("image/*") }) {
                    Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Selecionar logo")
                }
            }

            TituloSecao("Dados do técnico")
            Campo(tecnico, { tecnico = it }, "Nome do técnico")
            Campo(registro, { registro = it }, "Registro profissional (opcional)")

            TituloSecao("Assistente IA (OpenAI)")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ativar assistente IA", Modifier.weight(1f))
                Switch(checked = iaLigada, onCheckedChange = { iaLigada = it })
            }
            OutlinedTextField(
                value = chave,
                onValueChange = { chave = it },
                label = { Text("Chave da OpenAI (sk-...)") },
                singleLine = true,
                visualTransformation = if (mostrarChave) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { mostrarChave = !mostrarChave }) {
                        Icon(if (mostrarChave) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
            Text(
                "A chave é guardada de forma criptografada no dispositivo (EncryptedSharedPreferences) e " +
                    "nunca é compartilhada. A IA só funciona com internet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SeletorOpcoes("Modelo de IA", OpenAiClient.MODELOS, modelo, { modelo = it })
            OutlinedButton(
                onClick = {
                    salvarTudo()
                    testando = true
                    escopo.launch {
                        when (val r = OpenAiClient.testarConexao(context)) {
                            is OpenAiClient.Resultado.Sucesso ->
                                Toast.makeText(context, "Conexão OK!", Toast.LENGTH_LONG).show()
                            is OpenAiClient.Resultado.Erro ->
                                Toast.makeText(context, r.mensagem, Toast.LENGTH_LONG).show()
                        }
                        testando = false
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) { Text(if (testando) "Testando..." else "Testar conexão") }

            TituloSecao("Backup e restauração")
            Text(
                "Exporte uma cópia do banco de dados local para um arquivo, ou restaure a partir de um backup.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(Modifier.padding(top = 8.dp)) {
                OutlinedButton(
                    onClick = { exportarBackup.launch("refrigeracao_pro_backup.db") },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                ) { Text("Exportar") }
                OutlinedButton(
                    onClick = { importarBackup.launch("*/*") },
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                ) { Text("Restaurar") }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { salvarTudo(); Toast.makeText(context, "Configurações salvas.", Toast.LENGTH_SHORT).show() },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Salvar configurações") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Campo(valor: String, aoMudar: (String) -> Unit, rotulo: String) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}
